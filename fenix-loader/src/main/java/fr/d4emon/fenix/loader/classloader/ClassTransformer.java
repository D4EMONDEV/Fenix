package fr.d4emon.fenix.loader.classloader;

/**
 * Rewrites a class on its way into the JVM.
 *
 * <p>Transformers only ever see classes the {@link FenixClassLoader} defines
 * itself — game and mod classes. Anything the parent loads, including the
 * loader and the API, is out of reach by construction.
 *
 * <p>A class is transformed exactly once, at definition. A transformer
 * registered after a class was defined never sees it, so anything that wants to
 * touch game classes has to be registered before the first game class loads —
 * in practice, during {@code onPreLaunch}.
 */
@FunctionalInterface
public interface ClassTransformer {

    /**
     * Transforms one class.
     *
     * <p>Transformers run in registration order, each receiving the previous
     * one's output.
     *
     * @param className the binary name being defined, such as {@code net.minecraft.client.Minecraft}
     * @param classBytes the class file as it stands; treat it as read-only
     * @return the class file to use — the input itself to leave the class
     *         alone, never {@code null}
     */
    byte[] transform(String className, byte[] classBytes);
}
