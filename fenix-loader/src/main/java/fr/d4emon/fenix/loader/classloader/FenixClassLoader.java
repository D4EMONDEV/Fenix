package fr.d4emon.fenix.loader.classloader;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.CodeSource;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * The classloader the game and every mod run inside.
 *
 * <p><strong>Child-first:</strong> a class is looked up in the game and mod
 * jars before the application classpath. That inversion is what makes
 * transformation possible at all — if the parent were asked first, it would
 * define game classes from the original jar and this loader would never see
 * them.
 *
 * <p>Several package prefixes are exceptions, always taken from the parent:
 *
 * <ul>
 * <li>{@code fr.d4emon.fenix.loader.} — the loader itself;</li>
 * <li>{@code fr.d4emon.fenix.api.} — the contracts the loader shares with
 *     mods. If a mod jar carried its own copy and this loader defined it, the
 *     JVM would hold two {@code Class} objects with the same name, and every
 *     cast across the boundary would fail. The parent's copy is the only
 *     one that can exist.</li>
 * <li>{@code org.objectweb.asm.} and {@code org.spongepowered.asm.} — the
 *     transformation stack. A transformed game class holds references to Mixin
 *     runtime types like {@code CallbackInfo}; those must resolve to the same
 *     copy the transformer used, so exactly one copy can exist, on the
 *     parent. The one exception is {@code org.spongepowered.asm.synthetic.},
 *     which Mixin generates at runtime against game classes and which
 *     therefore has to be defined here (see {@link ClassGenerator}).</li>
 * </ul>
 *
 * <p>({@code java.} is short-circuited to the parent as well, as everywhere:
 * the JVM refuses user-defined classes in it.)
 *
 * <p>A parent-only class that the parent does not have is a
 * {@link ClassNotFoundException} even if a mod jar contains it — falling back
 * to the child copy would quietly recreate the split it exists to prevent.
 *
 * <p>Resources follow the same child-first rule, so a mod's
 * {@code assets/} shadow the classpath's on lookup, and
 * {@link #getResources(String)} lists child results first.
 *
 * <p><strong>Performance contract:</strong> each jar added with
 * {@link #addPath(Path)} is opened exactly once and stays open until
 * {@link #close()}; class bytes are read from that open, indexed
 * {@link JarFile}. The game defines tens of thousands of classes at startup —
 * anything per-class that reopens a jar (in particular the JDK's
 * {@code jar:} URL machinery, cached or not) turns launch time from seconds
 * into minutes. Keeping the jars open ourselves is also what lets
 * {@link #close()} actually release the file locks, which Windows needs before
 * a player can update their mods folder.
 */
public final class FenixClassLoader extends URLClassLoader {

    /**
     * Produces the bytes of a class no jar contains — Mixin's runtime-generated
     * synthetic classes. Returns {@code null} when it cannot generate the name,
     * which becomes a {@link ClassNotFoundException}.
     */
    @FunctionalInterface
    public interface ClassGenerator {

        /**
         * Generates one class.
         *
         * @param binaryName the class being defined
         * @return the class bytes, or {@code null} if not generatable
         */
        byte[] generate(String binaryName);
    }

    static {
        registerAsParallelCapable();
    }

    /** Mixin generates these against game classes, so they must be child-defined. */
    private static final String SYNTHETIC_PREFIX = "org.spongepowered.asm.synthetic.";

    private static final List<String> PARENT_ONLY = List.of(
            "java.",
            "fr.d4emon.fenix.loader.",
            "fr.d4emon.fenix.api.",
            "org.objectweb.asm.",
            "org.spongepowered.asm.");

    private final List<ClassTransformer> transformers = new CopyOnWriteArrayList<>();
    private final List<ChildSource> sources = new CopyOnWriteArrayList<>();
    private final AtomicReference<ClassGenerator> classGenerator = new AtomicReference<>();

    /**
     * Creates an empty loader; jars are added afterwards with {@link #addPath(Path)}.
     *
     * @param parent the loader holding the application classpath — the loader
     *               itself, the API, and the libraries they use
     * @throws NullPointerException if the parent is {@code null}
     */
    public FenixClassLoader(ClassLoader parent) {
        super("fenix", new URL[0], Objects.requireNonNull(parent, "parent"));
    }

    /**
     * Adds a jar — or, in a development environment, a directory of classes —
     * to the child scope.
     *
     * <p>A jar is opened here, once, and stays open until {@link #close()}.
     *
     * @param path the jar file or class directory
     * @throws IllegalArgumentException if the path cannot be opened
     * @throws NullPointerException     if the path is {@code null}
     */
    public void addPath(Path path) {
        Objects.requireNonNull(path, "path");
        try {
            // Registered with the URL machinery too, so findResource and
            // findResources keep working for URL-based lookups.
            addURL(path.toUri().toURL());
            sources.add(Files.isDirectory(path)
                    ? new DirectorySource(path)
                    : new JarSource(path));
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(path + " cannot be expressed as a URL", e);
        } catch (IOException e) {
            throw new IllegalArgumentException(path + " cannot be opened as a jar: " + e.getMessage(), e);
        }
    }

    /**
     * Registers a transformer for every class defined from now on.
     *
     * <p>Classes defined before registration stay as they are; there is no
     * retroactive transformation. See {@link ClassTransformer} for the timing
     * this imposes.
     *
     * @param transformer the transformer, appended after any already registered
     * @throws NullPointerException if the transformer is {@code null}
     */
    public void addTransformer(ClassTransformer transformer) {
        transformers.add(Objects.requireNonNull(transformer, "transformer"));
    }

    /**
     * Installs the generator for classes no jar contains.
     *
     * <p>There is one, Mixin's, so a second call is a wiring bug and is refused.
     *
     * @param generator the generator
     * @throws NullPointerException  if the generator is {@code null}
     * @throws IllegalStateException if a generator is already installed
     */
    public void setClassGenerator(ClassGenerator generator) {
        Objects.requireNonNull(generator, "generator");
        if (!classGenerator.compareAndSet(null, generator)) {
            throw new IllegalStateException("a class generator is already installed");
        }
    }

    /**
     * Reads a class's bytes without transforming them, child scope before parent.
     *
     * <p>This is what the Mixin service reads mixin classes and target-class
     * hierarchies through: it wants the original bytecode, never the
     * post-transformation result.
     *
     * @param binaryName the class name, for example {@code net.minecraft.client.Minecraft}
     * @return the raw class bytes, or {@code null} if nothing has them
     * @throws IOException          if a source fails to read
     * @throws NullPointerException if the name is {@code null}
     */
    public byte[] readClassBytes(String binaryName) throws IOException {
        Objects.requireNonNull(binaryName, "binaryName");
        String path = binaryName.replace('.', '/') + ".class";
        for (ChildSource source : sources) {
            byte[] bytes = source.readClass(path);
            if (bytes != null) {
                return bytes;
            }
        }
        try (InputStream in = getParent().getResourceAsStream(path)) {
            return in == null ? null : in.readAllBytes();
        }
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            Class<?> loaded = findLoadedClass(name);
            if (loaded == null) {
                if (name.startsWith(SYNTHETIC_PREFIX)) {
                    loaded = findClass(name);          // generated here, against game classes
                } else if (isParentOnly(name)) {
                    loaded = getParent().loadClass(name);
                } else {
                    loaded = loadChildFirst(name);
                }
            }
            if (resolve) {
                resolveClass(loaded);
            }
            return loaded;
        }
    }

    private Class<?> loadChildFirst(String name) throws ClassNotFoundException {
        try {
            return findClass(name);
        } catch (ClassNotFoundException notInChild) {
            // Not in any game or mod jar; the application classpath is the last
            // resort. Only ClassNotFoundException may take this path — a
            // ClassTransformationException must propagate, because the parent's
            // copy would be the untransformed class, running silently.
            return getParent().loadClass(name);
        }
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        String path = name.replace('.', '/') + ".class";
        for (ChildSource source : sources) {
            byte[] bytes;
            try {
                bytes = source.readClass(path);
            } catch (IOException e) {
                throw new ClassNotFoundException(name, e);
            }
            if (bytes != null) {
                bytes = transform(name, bytes);
                return defineClass(name, bytes, 0, bytes.length, source.codeSource());
            }
        }

        // No jar has it: it may be a class Mixin generates on demand.
        ClassGenerator generator = classGenerator.get();
        if (generator != null) {
            byte[] generated = generator.generate(name);
            if (generated != null) {
                return defineClass(name, generated, 0, generated.length, (CodeSource) null);
            }
        }
        throw new ClassNotFoundException(name);
    }

    @Override
    public URL getResource(String name) {
        URL resource = findResource(name);
        return resource != null ? resource : getParent().getResource(name);
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        // Served from our own open jars: the default implementation would go
        // through a jar: URL connection per call, which either reopens the jar
        // every time or parks it in the JDK's global cache past close().
        for (ChildSource source : sources) {
            try {
                InputStream in = source.openResource(name);
                if (in != null) {
                    return in;
                }
            } catch (IOException e) {
                return null;
            }
        }
        return getParent().getResourceAsStream(name);
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        List<URL> resources = new ArrayList<>(Collections.list(findResources(name)));
        getParent().getResources(name).asIterator().forEachRemaining(resources::add);
        return Collections.enumeration(resources);
    }

    @Override
    public void close() throws IOException {
        IOException failure = null;
        for (ChildSource source : sources) {
            try {
                source.close();
            } catch (IOException e) {
                failure = e;
            }
        }
        try {
            super.close();
        } catch (IOException e) {
            failure = e;
        }
        if (failure != null) {
            throw failure;
        }
    }

    private static boolean isParentOnly(String name) {
        for (String prefix : PARENT_ONLY) {
            if (name.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private byte[] transform(String name, byte[] bytes) {
        for (ClassTransformer transformer : transformers) {
            byte[] transformed;
            try {
                transformed = transformer.transform(name, bytes);
            } catch (RuntimeException e) {
                throw new ClassTransformationException(
                        "transforming " + name + " failed in " + transformer.getClass().getName(), e);
            }
            if (transformed == null) {
                throw new ClassTransformationException(
                        transformer.getClass().getName() + " returned null for " + name
                                + " — a transformer that changes nothing must return its input");
            }
            bytes = transformed;
        }
        return bytes;
    }

    /**
     * One entry of the child scope, with its bytes directly reachable — no URL
     * connections, no per-read jar opening.
     */
    private sealed interface ChildSource permits JarSource, DirectorySource {

        /** {@return the class bytes, or {@code null} when this source lacks the class} */
        byte[] readClass(String path) throws IOException;

        /** {@return a stream over the resource, or {@code null} when absent} */
        InputStream openResource(String path) throws IOException;

        /** {@return the location tooling sees on classes defined from here} */
        CodeSource codeSource();

        void close() throws IOException;
    }

    private static final class JarSource implements ChildSource {

        private final JarFile jar;
        private final CodeSource codeSource;

        JarSource(Path path) throws IOException {
            this.jar = new JarFile(path.toFile());
            this.codeSource = new CodeSource(path.toUri().toURL(), (Certificate[]) null);
        }

        @Override
        public byte[] readClass(String path) throws IOException {
            JarEntry entry = jar.getJarEntry(path);
            if (entry == null) {
                return null;
            }
            try (InputStream in = jar.getInputStream(entry)) {
                return in.readAllBytes();
            }
        }

        @Override
        public InputStream openResource(String path) throws IOException {
            JarEntry entry = jar.getJarEntry(path);
            return entry == null ? null : jar.getInputStream(entry);
        }

        @Override
        public CodeSource codeSource() {
            return codeSource;
        }

        @Override
        public void close() throws IOException {
            jar.close();
        }
    }

    private static final class DirectorySource implements ChildSource {

        private final Path root;
        private final CodeSource codeSource;

        DirectorySource(Path root) throws MalformedURLException {
            this.root = root;
            this.codeSource = new CodeSource(root.toUri().toURL(), (Certificate[]) null);
        }

        @Override
        public byte[] readClass(String path) throws IOException {
            Path file = root.resolve(path);
            return Files.isRegularFile(file) ? Files.readAllBytes(file) : null;
        }

        @Override
        public InputStream openResource(String path) throws IOException {
            Path file = root.resolve(path);
            return Files.isRegularFile(file) ? Files.newInputStream(file) : null;
        }

        @Override
        public CodeSource codeSource() {
            return codeSource;
        }

        @Override
        public void close() {
        }
    }
}
