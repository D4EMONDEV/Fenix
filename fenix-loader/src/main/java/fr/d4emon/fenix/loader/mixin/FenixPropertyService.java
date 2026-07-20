package fr.d4emon.fenix.loader.mixin;

import org.spongepowered.asm.service.IGlobalPropertyService;
import org.spongepowered.asm.service.IPropertyKey;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mixin's global blackboard, backed by a plain map.
 *
 * <p>Mixin keeps a handful of process-wide values here (its selected transformer
 * among them). The built-in implementations lean on LaunchWrapper or
 * ModLauncher classes that are absent under Fenix, so a self-contained map is
 * the whole requirement. Discovered through {@code META-INF/services}.
 */
public final class FenixPropertyService implements IGlobalPropertyService {

    /** A key that is just its name — value identity is by string equality. */
    private record Key(String name) implements IPropertyKey {
    }

    private final ConcurrentHashMap<String, Object> values = new ConcurrentHashMap<>();

    /** Instantiated by Mixin through {@code META-INF/services}. */
    public FenixPropertyService() {
    }

    @Override
    public IPropertyKey resolveKey(String name) {
        return new Key(name);
    }

    private static String name(IPropertyKey key) {
        return ((Key) Objects.requireNonNull(key, "key")).name();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getProperty(IPropertyKey key) {
        return (T) values.get(name(key));
    }

    @Override
    public void setProperty(IPropertyKey key, Object value) {
        if (value == null) {
            values.remove(name(key));
        } else {
            values.put(name(key), value);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getProperty(IPropertyKey key, T defaultValue) {
        Object value = values.get(name(key));
        return value != null ? (T) value : defaultValue;
    }

    @Override
    public String getPropertyString(IPropertyKey key, String defaultValue) {
        Object value = values.get(name(key));
        return value != null ? value.toString() : defaultValue;
    }
}
