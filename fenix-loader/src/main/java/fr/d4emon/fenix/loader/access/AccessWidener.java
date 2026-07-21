package fr.d4emon.fenix.loader.access;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * What a mod has asked to reach inside Minecraft.
 *
 * <p>Mixin covers most of this already: {@code @Accessor} and {@code @Invoker}
 * reach a private field or method without touching its declaration. What they
 * cannot do is make a type <em>nameable</em> — and some of vanilla's doors are
 * shut that way. Creating a {@code MenuType} needs a private constructor whose
 * parameter is a private interface, so there is nothing a mod can write down at
 * all, in any package.
 *
 * <p>Hence widening: the declared access of named members is raised to public
 * before anything loads them. Fabric and Forge both carry the same mechanism,
 * under different names, for the same reason.
 *
 * <p>Declarations are written in {@code fenix.mod.json}:
 *
 * <pre>{@code
 * "accessible": [
 *   "class net.minecraft.world.inventory.MenuType$MenuSupplier",
 *   "method net.minecraft.world.inventory.MenuType <init>"
 * ]
 * }</pre>
 *
 * <p>Members are named without a descriptor, so every overload of a name is
 * widened together. Naming one overload out of several is a precision nobody
 * has wanted and a signature that would rot on the next Minecraft update.
 */
public final class AccessWidener {

    /** Starts empty; mods add to it as they are read. */
    public AccessWidener() {
    }

    private final Set<String> classes = new LinkedHashSet<>();
    private final Map<String, Set<String>> methods = new LinkedHashMap<>();
    private final Map<String, Set<String>> fields = new LinkedHashMap<>();

    /**
     * Reads a mod's declarations.
     *
     * @param declarations the {@code accessible} entries
     * @param source       which mod they came from, for error messages
     * @throws IllegalArgumentException if a declaration cannot be understood
     */
    public void add(List<String> declarations, String source) {
        for (String declaration : Objects.requireNonNull(declarations, "declarations")) {
            String[] parts = declaration.trim().split("\\s+");
            switch (parts.length == 0 ? "" : parts[0]) {
                case "class" -> {
                    expect(parts.length == 2, declaration, source, "class <name>");
                    classes.add(internal(parts[1]));
                }
                case "method" -> {
                    expect(parts.length == 3, declaration, source, "method <class> <name>");
                    methods.computeIfAbsent(internal(parts[1]), key -> new LinkedHashSet<>())
                            .add(parts[2]);
                }
                case "field" -> {
                    expect(parts.length == 3, declaration, source, "field <class> <name>");
                    fields.computeIfAbsent(internal(parts[1]), key -> new LinkedHashSet<>())
                            .add(parts[2]);
                }
                default -> throw new IllegalArgumentException(source
                        + ": '" + declaration + "' should start with class, method or field");
            }
        }
    }

    /**
     * {@return whether anything was declared at all}
     *
     * <p>Most mods declare nothing, and the transformer is worth skipping
     * entirely rather than running over every class the game loads.
     */
    public boolean isEmpty() {
        return classes.isEmpty() && methods.isEmpty() && fields.isEmpty();
    }

    /**
     * {@return whether this class is named by any declaration}
     *
     * <p>Includes classes named only as the owner of a member, and classes that
     * merely <em>contain</em> a widened nested class: a nested type is nameable
     * only if the outer class says so too, in its {@code InnerClasses} entry.
     *
     * @param internalName the class, in {@code a/b/C} form
     */
    public boolean touches(String internalName) {
        if (classes.contains(internalName)
                || methods.containsKey(internalName)
                || fields.containsKey(internalName)) {
            return true;
        }
        String nested = internalName + "$";
        return classes.stream().anyMatch(name -> name.startsWith(nested));
    }

    /**
     * {@return whether this class should be made public}
     *
     * @param internalName the class, in {@code a/b/C} form
     */
    public boolean widensClass(String internalName) {
        return classes.contains(internalName);
    }

    /**
     * {@return whether this method should be made public}
     *
     * @param owner the declaring class, in {@code a/b/C} form
     * @param name  the method name; {@code <init>} for a constructor
     */
    public boolean widensMethod(String owner, String name) {
        return methods.getOrDefault(owner, Set.of()).contains(name);
    }

    /**
     * {@return whether this field should be made public}
     *
     * @param owner the declaring class, in {@code a/b/C} form
     * @param name  the field name
     */
    public boolean widensField(String owner, String name) {
        return fields.getOrDefault(owner, Set.of()).contains(name);
    }

    private static void expect(boolean condition, String declaration, String source, String shape) {
        if (!condition) {
            throw new IllegalArgumentException(source
                    + ": '" + declaration + "' should read '" + shape + "'");
        }
    }

    /** Declarations are written with dots, because that is how Java reads. */
    private static String internal(String name) {
        return name.replace('.', '/');
    }
}
