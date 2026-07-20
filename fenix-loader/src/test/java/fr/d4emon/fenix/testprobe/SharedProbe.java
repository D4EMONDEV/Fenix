package fr.d4emon.fenix.testprobe;

/**
 * Exists on the test classpath so the classloader tests can shadow it from a
 * generated jar. Deliberately outside {@code fr.d4emon.fenix.loader} and
 * {@code fr.d4emon.fenix.api}: those prefixes are parent-only, and this class
 * has to be loadable by the child.
 */
public final class SharedProbe {

    private SharedProbe() {
    }

    /**
     * {@return where this copy of the class lives}
     *
     * <p>The generated shadow returns {@code "child"} instead, which is how the
     * tests tell the two copies apart.
     */
    public static String source() {
        return "parent";
    }
}
