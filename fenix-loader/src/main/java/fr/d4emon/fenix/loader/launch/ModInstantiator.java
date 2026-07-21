package fr.d4emon.fenix.loader.launch;

import fr.d4emon.fenix.api.FenixMod;
import fr.d4emon.fenix.api.Side;
import fr.d4emon.fenix.loader.classloader.FenixClassLoader;
import fr.d4emon.fenix.loader.discovery.ModCandidate;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Turns resolved candidates into {@link LoadedMod}s by reading each jar's
 * compile-time index and instantiating the classes it names — through the
 * {@link FenixClassLoader}, so mod code lives in the child scope from its very
 * first instruction.
 *
 * <p>The annotation processor already rejected most ways these classes can be
 * wrong, so a failure here means the jar was assembled by hand or manipulated
 * after compilation; the errors still name the jar and the class, because
 * hand-assembled jars are exactly the ones that need good errors.
 */
public final class ModInstantiator {

    private ModInstantiator() {
    }

    /**
     * Instantiates every mod's entry classes.
     *
     * @param loader     the classloader mods run inside; their jars must
     *                   already have been added to it
     * @param loadOrder  the resolved mods, dependencies first
     * @param side       which side is running; a server skips client entry classes
     * @return the loaded mods, in the same order
     * @throws LaunchException      if an entry class cannot be loaded or instantiated
     * @throws NullPointerException if either argument is {@code null}
     */
    public static List<LoadedMod> instantiate(FenixClassLoader loader, List<ModCandidate> loadOrder, Side side) {
        Objects.requireNonNull(loader, "loader");
        Objects.requireNonNull(loadOrder, "loadOrder");
        Objects.requireNonNull(side, "side");

        // A mod's client entry class is indexed apart from its common one, and
        // a server never even hears about it. That is the whole point: the
        // class names types a server jar does not contain, so being told to
        // load it would be fatal rather than merely wrong.
        List<String> indexes = side == Side.CLIENT
                ? List.of(ModIndexReader.FILE_NAME, ModIndexReader.CLIENT_FILE_NAME)
                : List.of(ModIndexReader.FILE_NAME);

        List<LoadedMod> mods = new ArrayList<>(loadOrder.size());
        for (ModCandidate candidate : loadOrder) {
            List<FenixMod> entries = new ArrayList<>();
            for (String index : indexes) {
                for (Map.Entry<String, String> indexed
                        : ModIndexReader.readFromJar(candidate.path(), index).entrySet()) {
                    if (!indexed.getKey().equals(candidate.id())) {
                        throw new LaunchException(candidate.fileName() + " indexes mod '" + indexed.getKey()
                                + "' but its fenix.mod.json declares '" + candidate.id()
                                + "' — the jar was assembled inconsistently");
                    }
                    entries.add(instantiate(candidate, indexed.getValue(), loader));
                }
            }
            mods.add(new LoadedMod(candidate.metadata(), candidate.path(), entries));
        }
        return mods;
    }

    private static FenixMod instantiate(ModCandidate candidate, String className, FenixClassLoader loader) {
        Class<?> type;
        try {
            type = loader.loadClass(className);
        } catch (ClassNotFoundException e) {
            throw new LaunchException("mod '" + candidate.id() + "' (" + candidate.fileName()
                    + ") declares its entry class as " + className
                    + ", which cannot be loaded — was the jar repackaged after compilation?", e);
        }

        Object instance;
        try {
            instance = type.getConstructor().newInstance();
        } catch (NoSuchMethodException e) {
            throw new LaunchException("the entry class " + className + " of mod '" + candidate.id()
                    + "' has no public no-argument constructor", e);
        } catch (InvocationTargetException e) {
            throw new LaunchException("the constructor of " + className + " (mod '" + candidate.id()
                    + "') threw — a mod must not do real work before its lifecycle starts", e.getCause());
        } catch (ReflectiveOperationException e) {
            throw new LaunchException("the entry class " + className + " of mod '" + candidate.id()
                    + "' cannot be instantiated", e);
        }

        if (!(instance instanceof FenixMod mod)) {
            throw new LaunchException("the entry class " + className + " of mod '" + candidate.id()
                    + "' does not implement " + FenixMod.class.getName());
        }
        return mod;
    }
}
