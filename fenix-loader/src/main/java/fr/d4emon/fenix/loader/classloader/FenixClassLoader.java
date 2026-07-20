package fr.d4emon.fenix.loader.classloader;

import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.security.CodeSource;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The classloader the game and every mod run inside.
 *
 * <p><strong>Child-first:</strong> a class is looked up in the game and mod
 * jars before the application classpath. That inversion is what makes
 * transformation possible at all — if the parent were asked first, it would
 * define game classes from the original jar and this loader would never see
 * them.
 *
 * <p>Two package prefixes are exceptions, always taken from the parent:
 *
 * <ul>
 * <li>{@code fr.d4emon.fenix.loader.} — the loader itself;</li>
 * <li>{@code fr.d4emon.fenix.api.} — the contracts the loader shares with
 *     mods. If a mod jar carried its own copy and this loader defined it, the
 *     JVM would hold two {@code Class} objects with the same name, and every
 *     cast across the boundary would fail. The parent's copy is the only
 *     one that can exist.</li>
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
 */
public final class FenixClassLoader extends URLClassLoader {

    static {
        registerAsParallelCapable();
    }

    private static final List<String> PARENT_ONLY = List.of(
            "java.",
            "fr.d4emon.fenix.loader.",
            "fr.d4emon.fenix.api.");

    private final List<ClassTransformer> transformers = new CopyOnWriteArrayList<>();

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
     * @param path the jar file or class directory
     * @throws NullPointerException if the path is {@code null}
     */
    public void addPath(Path path) {
        Objects.requireNonNull(path, "path");
        try {
            addURL(path.toUri().toURL());
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(path + " cannot be expressed as a URL", e);
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

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            Class<?> loaded = findLoadedClass(name);
            if (loaded == null) {
                loaded = isParentOnly(name) ? getParent().loadClass(name) : loadChildFirst(name);
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
        URL resource = findResource(path);
        if (resource == null) {
            throw new ClassNotFoundException(name);
        }

        byte[] bytes;
        try (InputStream in = openUncached(resource)) {
            bytes = in.readAllBytes();
        } catch (IOException e) {
            throw new ClassNotFoundException(name, e);
        }

        bytes = transform(name, bytes);
        return defineClass(name, bytes, 0, bytes.length, codeSource(resource, path));
    }

    @Override
    public URL getResource(String name) {
        URL resource = findResource(name);
        return resource != null ? resource : getParent().getResource(name);
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        URL resource = getResource(name);
        if (resource == null) {
            return null;
        }
        try {
            return openUncached(resource);
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Opens a resource while bypassing the JDK's global {@code jar:} connection
     * cache. The cache holds jar files open past {@link #close()}, which on
     * Windows means the files cannot be deleted or replaced for as long as the
     * JVM lives — exactly what a player updating their mods folder would hit.
     */
    private static InputStream openUncached(URL resource) throws IOException {
        var connection = resource.openConnection();
        connection.setUseCaches(false);
        return connection.getInputStream();
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        List<URL> resources = new ArrayList<>(Collections.list(findResources(name)));
        getParent().getResources(name).asIterator().forEachRemaining(resources::add);
        return Collections.enumeration(resources);
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
     * Derives the location tooling sees on the defined class — the jar it came
     * from, or the class directory root — so that "which file did this class
     * load from?" stays answerable inside a transformed process.
     */
    private static CodeSource codeSource(URL resource, String path) {
        try {
            if (resource.openConnection() instanceof JarURLConnection jarConnection) {
                return new CodeSource(jarConnection.getJarFileURL(), (Certificate[]) null);
            }
        } catch (IOException ignored) {
            // The class still loads; only the reported location is affected.
        }

        String text = resource.toString();
        if (text.endsWith(path)) {
            try {
                URL root = URI.create(text.substring(0, text.length() - path.length())).toURL();
                return new CodeSource(root, (Certificate[]) null);
            } catch (MalformedURLException | IllegalArgumentException ignored) {
                // Fall through to the resource itself.
            }
        }
        return new CodeSource(resource, (Certificate[]) null);
    }
}
