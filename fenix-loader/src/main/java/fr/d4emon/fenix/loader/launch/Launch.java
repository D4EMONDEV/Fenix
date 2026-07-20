package fr.d4emon.fenix.loader.launch;

import fr.d4emon.fenix.api.Side;
import fr.d4emon.fenix.api.Version;
import fr.d4emon.fenix.loader.classloader.FenixClassLoader;
import fr.d4emon.fenix.loader.discovery.DiscoveryResult;
import fr.d4emon.fenix.loader.discovery.ModCandidate;
import fr.d4emon.fenix.loader.discovery.ModDiscoverer;
import fr.d4emon.fenix.loader.log.ConsoleLogger;
import fr.d4emon.fenix.loader.metadata.InvalidMetadataException;
import fr.d4emon.fenix.loader.resolve.ModResolver;
import fr.d4emon.fenix.loader.resolve.ResolutionException;
import fr.d4emon.fenix.loader.resolve.ResolutionResult;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The loader's entry point: what the launcher starts instead of the game.
 *
 * <p>The pipeline is discovery, resolution, classloading, instantiation,
 * {@code onPreLaunch}, then the game's own {@code main}. The later phases fire
 * from inside the game through {@link FenixHooks}.
 */
public final class Launch {

    private static final String USAGE = """
            Usage: Launch --gameMain <class> [options] [-- game arguments...]
              --gameMain <class>   the game's main class (required)
              --gameJar <jar>      the game jar, loaded in the transformable scope
              --gameDir <dir>      the game directory (default: current directory)
              --mods <dir>         the mods directory (default: <gameDir>/mods)
              --side client|server the side to launch (default: client)""";

    private Launch() {
    }

    /**
     * Launches the game through Fenix.
     *
     * @param args see {@code --help} in the class documentation
     */
    public static void main(String[] args) {
        try {
            run(args);
        } catch (LaunchException | ResolutionException | InvalidMetadataException e) {
            // Diagnosed failures: the message is the whole story. The cause, if
            // any, is genuinely useful context — print it compactly.
            System.err.println();
            System.err.println(e.getMessage());
            if (e.getCause() != null) {
                e.getCause().printStackTrace();
            }
            System.exit(1);
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * The pipeline, separated from {@link #main} so failures stay throwable.
     */
    static void run(String[] args) throws Throwable {
        Options options = Options.parse(args);
        ConsoleLogger log = new ConsoleLogger("fenix");
        Version loaderVersion = FenixVersion.current();

        log.info("Fenix Loader {} — {} side, game directory {}",
                loaderVersion, options.side().toString().toLowerCase(Locale.ROOT), options.gameDir());

        // 1. What is installed?
        DiscoveryResult discovered = ModDiscoverer.scan(options.modsDir());
        if (discovered.hasProblems()) {
            StringBuilder message = new StringBuilder("some files in ")
                    .append(options.modsDir()).append(" cannot be loaded:");
            for (String problem : discovered.problems()) {
                message.append(System.lineSeparator()).append("  - ").append(problem);
            }
            throw new LaunchException(message.toString());
        }

        // 2. Can it all load together, and in what order?
        ResolutionResult resolved = ModResolver.resolve(
                discovered.mods(), options.side(), Map.of("fenix", loaderVersion));
        for (ModCandidate skipped : resolved.skipped()) {
            log.info("skipping {} — it only loads on the {}",
                    skipped, skipped.metadata().side().toString().toLowerCase(Locale.ROOT));
        }
        for (ModCandidate mod : resolved.loadOrder()) {
            log.info("loading {}", mod);
        }

        // 3. Build the world mods and the game live in.
        FenixClassLoader loader = new FenixClassLoader(Launch.class.getClassLoader());
        if (options.gameJar() != null) {
            loader.addPath(options.gameJar());
        }
        for (ModCandidate mod : resolved.loadOrder()) {
            loader.addPath(mod.path());
        }

        // 4. Wake the mods up, before any game class exists.
        List<LoadedMod> mods = ModInstantiator.instantiate(loader, resolved.loadOrder());
        FenixRuntime runtime = new FenixRuntime(options.side(), options.gameDir(), mods);
        FenixHooks.bind(runtime);
        runtime.firePreLaunch();

        // 5. Hand over to the game, inside the transformable scope.
        Class<?> gameMain;
        try {
            gameMain = loader.loadClass(options.gameMain());
        } catch (ClassNotFoundException e) {
            throw new LaunchException("the game main class " + options.gameMain()
                    + " was not found — check --gameMain and --gameJar", e);
        }

        log.info("handing over to {}", options.gameMain());
        Thread.currentThread().setContextClassLoader(loader);
        try {
            gameMain.getMethod("main", String[].class).invoke(null, (Object) options.gameArgs());
        } catch (InvocationTargetException e) {
            // The game's own failure is the story, not the reflection frame.
            throw e.getCause();
        }
    }

    /**
     * The command line, parsed.
     *
     * @param gameMain the game's main class, required
     * @param gameJar  the game jar for the child scope, or {@code null}
     * @param gameDir  the game directory
     * @param modsDir  the mods directory
     * @param side     the side to launch
     * @param gameArgs everything after {@code --}, passed to the game untouched
     */
    record Options(String gameMain, Path gameJar, Path gameDir, Path modsDir, Side side, String[] gameArgs) {

        static Options parse(String[] args) {
            String gameMain = null;
            Path gameJar = null;
            Path gameDir = Path.of(".");
            Path modsDir = null;
            Side side = Side.CLIENT;
            String[] gameArgs = new String[0];

            int i = 0;
            while (i < args.length) {
                String arg = args[i];
                switch (arg) {
                    case "--gameMain" -> gameMain = value(args, i++);
                    case "--gameJar" -> gameJar = Path.of(value(args, i++));
                    case "--gameDir" -> gameDir = Path.of(value(args, i++));
                    case "--mods" -> modsDir = Path.of(value(args, i++));
                    case "--side" -> side = parseSide(value(args, i++));
                    case "--" -> {
                        gameArgs = java.util.Arrays.copyOfRange(args, i + 1, args.length);
                        i = args.length - 1;
                    }
                    default -> throw new LaunchException(
                            "unknown option '" + arg + "'" + System.lineSeparator() + USAGE);
                }
                i++;
            }

            if (gameMain == null) {
                throw new LaunchException("--gameMain is required" + System.lineSeparator() + USAGE);
            }
            if (modsDir == null) {
                modsDir = gameDir.resolve("mods");
            }
            return new Options(gameMain, gameJar, gameDir, modsDir, side, gameArgs);
        }

        private static String value(String[] args, int index) {
            if (index + 1 >= args.length) {
                throw new LaunchException(
                        "option '" + args[index] + "' needs a value" + System.lineSeparator() + USAGE);
            }
            return args[index + 1];
        }

        private static Side parseSide(String text) {
            return switch (text.toLowerCase(Locale.ROOT)) {
                case "client" -> Side.CLIENT;
                case "server" -> Side.SERVER;
                default -> throw new LaunchException(
                        "--side must be 'client' or 'server', not '" + text + "'");
            };
        }
    }
}
