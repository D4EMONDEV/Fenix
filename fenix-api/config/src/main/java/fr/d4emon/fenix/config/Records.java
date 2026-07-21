package fr.d4emon.fenix.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Turns records into JSON and back.
 *
 * <p>Gson can read records itself, and does the one thing that matters wrongly:
 * a field missing from the file becomes {@code null} or zero, in silence. A
 * player who deletes a line, or who upgrades to a version that added one, gets
 * a mod behaving as though they had asked for nothing.
 *
 * <p>So the record is built here, component by component, taking the default
 * for anything absent. That is also what makes it possible to name an unknown
 * key rather than drop it — a mistyped setting that quietly does nothing is the
 * classic configuration bug, and it costs an evening every time.
 */
final class Records {

    private Records() {
    }

    /**
     * {@return a record of the same shape, from JSON, falling back to defaults}
     *
     * @param <T>      the record type
     * @param json     what was in the file
     * @param defaults the values to use for anything missing
     * @param unknown  collects keys the record has no component for
     * @param path     where we are, for messages
     */
    @SuppressWarnings("unchecked")
    static <T extends Record> T read(JsonObject json, T defaults, List<String> unknown, String path) {
        Class<T> type = (Class<T>) defaults.getClass();
        RecordComponent[] components = type.getRecordComponents();

        List<String> known = new ArrayList<>();
        Object[] values = new Object[components.length];

        for (int i = 0; i < components.length; i++) {
            RecordComponent component = components[i];
            known.add(component.getName());
            Object fallback = valueOf(component, defaults);
            JsonElement element = json.get(component.getName());
            values[i] = element == null || element.isJsonNull()
                    ? fallback
                    : convert(element, component.getGenericType(), fallback, unknown,
                            path + component.getName());
        }
        for (String key : json.keySet()) {
            if (!known.contains(key)) {
                unknown.add(path + key);
            }
        }
        return construct(type, components, values, path);
    }

    /** {@return the record as JSON, in declaration order} */
    static JsonObject write(Record value) {
        JsonObject json = new JsonObject();
        for (RecordComponent component : value.getClass().getRecordComponents()) {
            json.add(component.getName(), toJson(valueOf(component, value)));
        }
        return json;
    }

    private static JsonElement toJson(Object value) {
        return switch (value) {
            case null -> com.google.gson.JsonNull.INSTANCE;
            case Record record -> write(record);
            case Enum<?> constant -> new JsonPrimitive(constant.name());
            case Boolean bool -> new JsonPrimitive(bool);
            case Number number -> new JsonPrimitive(number);
            case String string -> new JsonPrimitive(string);
            case List<?> list -> {
                JsonArray array = new JsonArray();
                list.forEach(item -> array.add(toJson(item)));
                yield array;
            }
            default -> throw new ConfigException(value.getClass().getSimpleName()
                    + " is not a type configuration can hold");
        };
    }

    private static Object valueOf(RecordComponent component, Record from) {
        try {
            return component.getAccessor().invoke(from);
        } catch (ReflectiveOperationException e) {
            throw new ConfigException("cannot read " + component.getName(), e);
        }
    }

    private static <T extends Record> T construct(Class<T> type, RecordComponent[] components,
                                                  Object[] values, String path) {
        Class<?>[] parameters = new Class<?>[components.length];
        for (int i = 0; i < components.length; i++) {
            parameters[i] = components[i].getType();
        }
        try {
            Constructor<T> constructor = type.getDeclaredConstructor(parameters);
            constructor.setAccessible(true);
            return constructor.newInstance(values);
        } catch (InvocationTargetException e) {
            // A compact constructor is where a mod validates its own settings,
            // so this is a rejected value rather than a bug. It deserves the
            // message its author wrote, not a stack trace.
            Throwable cause = e.getCause();
            String detail = cause == null ? e.toString() : cause.getMessage();
            throw new ConfigException(path.isEmpty() ? detail : path + ": " + detail, cause);
        } catch (ReflectiveOperationException e) {
            throw new ConfigException("cannot build " + type.getSimpleName(), e);
        }
    }

    private static Object convert(JsonElement element, Type target, Object fallback,
                                  List<String> unknown, String path) {
        if (target instanceof ParameterizedType parameterized
                && parameterized.getRawType() == List.class) {
            Type item = parameterized.getActualTypeArguments()[0];
            JsonArray array = expect(element, JsonElement::isJsonArray, "a list", path)
                    .getAsJsonArray();
            List<Object> list = new ArrayList<>();
            for (int i = 0; i < array.size(); i++) {
                list.add(convert(array.get(i), item, null, unknown, path + "[" + i + "]"));
            }
            return List.copyOf(list);
        }
        Class<?> raw = (Class<?>) target;

        if (Record.class.isAssignableFrom(raw)) {
            JsonObject nested = expect(element, JsonElement::isJsonObject, "an object", path)
                    .getAsJsonObject();
            if (!(fallback instanceof Record nestedDefaults)) {
                throw new ConfigException(path + ": nothing to fall back on — a record inside a "
                        + "list needs every field present");
            }
            return read(nested, nestedDefaults, unknown, path + ".");
        }
        if (raw.isEnum()) {
            String name = element.getAsString();
            for (Object constant : raw.getEnumConstants()) {
                if (((Enum<?>) constant).name().equalsIgnoreCase(name)) {
                    return constant;
                }
            }
            throw new ConfigException(path + ": " + name + " is not one of "
                    + List.of(raw.getEnumConstants()));
        }
        JsonPrimitive primitive = expect(element, JsonElement::isJsonPrimitive, "a value", path)
                .getAsJsonPrimitive();

        if (raw == boolean.class || raw == Boolean.class) {
            return primitive.getAsBoolean();
        }
        if (raw == int.class || raw == Integer.class) {
            return primitive.getAsInt();
        }
        if (raw == long.class || raw == Long.class) {
            return primitive.getAsLong();
        }
        if (raw == double.class || raw == Double.class) {
            return primitive.getAsDouble();
        }
        if (raw == float.class || raw == Float.class) {
            return primitive.getAsFloat();
        }
        if (raw == String.class) {
            return primitive.getAsString();
        }
        throw new ConfigException(path + ": " + raw.getSimpleName()
                + " is not a type configuration can hold — use a number, a boolean, a string, "
                + "an enum, a list, or another record");
    }

    private static JsonElement expect(JsonElement element, Predicate<JsonElement> shape,
                                      String what, String path) {
        if (!shape.test(element)) {
            throw new ConfigException(path + ": expected " + what + ", found " + element);
        }
        return element;
    }
}
