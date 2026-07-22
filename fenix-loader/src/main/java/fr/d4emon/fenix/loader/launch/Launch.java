package fr.d4emon.fenix.loader.launch;

import fr.d4emon.fenix.api.Side;
import fr.d4emon.fenix.api.Version;
import fr.d4emon.fenix.loader.access.AccessTransformer;
import fr.d4emon.fenix.loader.access.AccessWidener;
import fr.d4emon.fenix.loader.classloader.FenixClassLoader;
import fr.d4emon.fenix.loader.discovery.DiscoveryResult;
import fr.d4emon.fenix.loader.discovery.ModCandidate;
import fr.d4emon.fenix.loader.discovery.ModDiscoverer;
import fr.d4emon.fenix.loader.game.GameLocator;
import fr.d4emon.fenix.loader.log.ConsoleLogger;
import fr.d4emon.fenix.loader.metadata.InvalidMetadataException;
import fr.d4emon.fenix.loader.mixin.MixinSetup;
import fr.d4emon.fenix.loader.resolve.ModResolver;
import fr.d4emon.fenix.loader.resolve.ResolutionException;
import fr.d4emon.fenix.loader.resolve.ResolutionResult;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The loader's entry point: what the launcher starts instead of the game.
 *
 * <p>A Fenix launcher profile inherits from the vanilla version, so the
 * launcher builds the vanilla classpath — game jar included — and starts this
 * class with the vanilla arguments. Fenix's own options are namespaced
 * {@code --fenix.*}; every other argument belongs to the game and passes
 * through untouched. The game directory is peeked from the vanilla
 * {@code --gameDir} so the loader and the game always agree on it.
 *
 * <p>The pipeline is discovery, resolution, classloading, instantiation,
 * {@code onPreLaunch}, then the game's own {@code main}. The later phases fire
 * from inside the game through {@link FenixHooks}.
 */
public final class Launch {

    private static final String USAGE = """
            Fenix options (everything else is handed to the game):
              --fenix.gameMain <class>  main class, when the game is not Minecraft on the classpath
              --fenix.gameJar <jar>     game jar to load in the transformable scope
              --fenix.gameDir <dir>     game directory (default: the game's own --gameDir, else '.')
              --fenix.mods <dir>        mods directory (default: <gameDir>/mods)
              --fenix.side client|server  the side (default: detected from the game jar)
              --fenix.dryRun            stop after proving the game and mods resolve""";

    private Launch() {
    }

    /**
     * Launches the game through Fenix.
     *
     * @param args Fenix's {@code --fenix.*} options plus the game's arguments
     */
    public static void main(String[] args) {
        try {
            run(args);
        } catch (LaunchException | ResolutionException | InvalidMetadataException e) {
            // Diagnosed: the message is the whole story, and the cause is
            // context rather than the report.
            FailureReport.publish(e, true, reportDirectory(args));
            System.exit(1);
        } catch (Throwable t) {
            FailureReport.publish(t, false, reportDirectory(args));
            System.exit(1);
        }
    }

    /**
     * {@return where a failure report should go, guessed from the arguments}
     *
     * <p>Guessed rather than taken, because the arguments are what failed to
     * parse in some of the cases this exists for. A report next to the game is
     * worth a second attempt at reading them; a report nowhere is not.
     */
    private static Path reportDirectory(String[] args) {
        try {
            return Options.parse(args).gameDir();
        } catch (RuntimeException unparseable) {
            return Path.of(".");
        }
    }

    /**
     * The pipeline, separated from {@link #main} so failures stay throwable.
     *
     * <p>Public because {@link #main} is a dead end for anything that wants to
     * know how the launch went: it prints and calls {@link System#exit}. Fenix's
     * own conformance tests drive this instead, and so could anything embedding
     * the loader.
     *
     * @param args the same arguments {@link #main} takes
     * @throws Throwable whatever the launch, or the game itself, threw
     */
    public static void run(String[] args) throws Throwable {
        Options options = Options.parse(args);
        ConsoleLogger log = new ConsoleLogger("fenix");
        Version loaderVersion = FenixVersion.current();

        // 1. What are we launching?
        GameLocator.Game located = null;
        String mainClass = options.gameMain();
        Path gameJar = options.gameJar();
        if (mainClass == null) {
            Path explicitJar = gameJar;
            located = (explicitJar != null
                    ? GameLocator.inspect(explicitJar)
                    : GameLocator.locate(System.getProperty("java.class.path")))
                    .orElseThrow(() -> new LaunchException(explicitJar != null
                            ? explicitJar + " is not a Minecraft jar — no known main class inside"
                            : "Minecraft was not found on the classpath — launch through a Fenix profile, "
                                    + "or pass --fenix.gameMain" + System.lineSeparator() + USAGE));
            mainClass = located.mainClass();
            gameJar = located.jar();
        } else if (gameJar != null) {
            // An explicit main can still point at a real Minecraft jar; probing
            // it is what feeds the "minecraft" version into resolution.
            located = GameLocator.inspect(gameJar).orElse(null);
        }

        Side side = options.side() != null ? options.side()
                : located != null ? located.side()
                : Side.CLIENT;

        Map<String, Version> builtins = new HashMap<>();
        builtins.put("fenix", loaderVersion);
        if (located != null && located.version().isPresent()) {
            builtins.put("minecraft", located.version().get());
        }

        log.info("Fenix Loader {} — {} side, game directory {}",
                loaderVersion, side.toString().toLowerCase(Locale.ROOT), options.gameDir());
        if (located != null) {
            log.info("game: {} ({})", located.jar().getFileName(),
                    located.version().map(Version::toString).orElse("unknown version"));
        }

        // 2. What is installed?
        // Unpacked beside the mods rather than among them, so a player can
        // delete the directory to force a clean re-unpack and cannot mistake
        // its contents for something they installed.
        DiscoveryResult discovered = ModDiscoverer.scan(options.modsDir(),
                options.gameDir().resolve(".fenix").resolve("jars"));
        if (discovered.hasProblems()) {
            StringBuilder message = new StringBuilder("some files in ")
                    .append(options.modsDir()).append(" cannot be loaded:");
            for (String problem : discovered.problems()) {
                message.append(System.lineSeparator()).append("  - ").append(problem);
            }
            throw new LaunchException(message.toString());
        }

        // 3. Can it all load together, and in what order?
        ResolutionResult resolved = ModResolver.resolve(discovered.mods(), side, Map.copyOf(builtins));
        for (ModCandidate skipped : resolved.skipped()) {
            log.info("skipping {} — it only loads on the {}",
                    skipped, skipped.metadata().side().toString().toLowerCase(Locale.ROOT));
        }
        for (ModCandidate mod : resolved.loadOrder()) {
            log.info("loading {}", mod);
        }

        // 4. Build the world the game and mods live in.
        FenixClassLoader loader = new FenixClassLoader(Launch.class.getClassLoader());
        if (gameJar != null) {
            loader.addPath(gameJar);
        }
        for (ModCandidate mod : resolved.loadOrder()) {
            loader.addPath(mod.path());
        }

        // 5. Widen what mods asked to reach, before anything can load a class
        // in a state they cannot use. Registered ahead of Mixin, which is what
        // puts it first: a mixin targeting a widened member has to find it
        // already widened.
        AccessWidener widener = new AccessWidener();
        for (ModCandidate mod : resolved.loadOrder()) {
            widener.add(mod.metadata().accessible(), mod.fileName());
        }
        if (!widener.isEmpty()) {
            loader.addTransformer(new AccessTransformer(widener));
        }

        // 6. Bring up Mixin and register every config, before any game class
        // can load. The loader's own config carries the lifecycle hooks; mods
        // add theirs. On a non-Minecraft game the configs are simply inert.
        List<String> mixinConfigs = new ArrayList<>();
        mixinConfigs.add("fenix-loader.mixins.json");
        for (ModCandidate mod : resolved.loadOrder()) {
            mixinConfigs.addAll(mod.metadata().mixins());
        }
        MixinSetup.bootstrap(loader, side, mixinConfigs);

        // 7. Wake the mods up, before any game class exists.
        List<LoadedMod> mods = ModInstantiator.instantiate(loader, resolved.loadOrder(), side);
        FenixRuntime runtime = new FenixRuntime(side, options.gameDir(), mods);
        FenixHooks.bind(runtime);
        runtime.firePreLaunch();

        // 7. Hand over to the game, inside the transformable scope.
        Class<?> gameMain;
        try {
            gameMain = loader.loadClass(mainClass);
        } catch (ClassNotFoundException e) {
            throw new LaunchException("the game main class " + mainClass
                    + " was not found — check --fenix.gameMain and --fenix.gameJar", e);
        }

        if (options.dryRun()) {
            log.info("dry run — {} resolves through the Fenix classloader, {} mod(s) ready; "
                    + "stopping before the game starts", mainClass, mods.size());
            return;
        }

        log.info("handing over to {}", mainClass);
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
     * @param gameMain the game's main class, or {@code null} to locate Minecraft
     * @param gameJar  the game jar for the child scope, or {@code null}
     * @param gameDir  the game directory
     * @param modsDir  the mods directory
     * @param side     the explicit side, or {@code null} to detect it
     * @param dryRun   whether to stop before invoking the game
     * @param gameArgs every non-Fenix argument, forwarded to the game untouched
     */
    record Options(String gameMain, Path gameJar, Path gameDir, Path modsDir,
                   Side side, boolean dryRun, String[] gameArgs) {

        static Options parse(String[] args) {
            String gameMain = null;
            Path gameJar = null;
            Path fenixGameDir = null;
            Path modsDir = null;
            Side side = null;
            boolean dryRun = false;
            List<String> gameArgs = new ArrayList<>(args.length);
            Path peekedGameDir = null;

            int i = 0;
            while (i < args.length) {
                String arg = args[i];
                switch (arg) {
                    case "--fenix.gameMain" -> gameMain = value(args, i++);
                    case "--fenix.gameJar" -> gameJar = Path.of(value(args, i++));
                    case "--fenix.gameDir" -> fenixGameDir = Path.of(value(args, i++));
                    case "--fenix.mods" -> modsDir = Path.of(value(args, i++));
                    case "--fenix.side" -> side = parseSide(value(args, i++));
                    case "--fenix.dryRun" -> dryRun = true;
                    default -> {
                        if (arg.startsWith("--fenix.")) {
                            throw new LaunchException(
                                    "unknown option '" + arg + "'" + System.lineSeparator() + USAGE);
                        }
                        // The game's argument — forwarded, but --gameDir is also
                        // peeked so the loader agrees with the game on where the
                        // game directory (and thus mods/) is.
                        gameArgs.add(arg);
                        if (arg.equals("--gameDir") && i + 1 < args.length) {
                            peekedGameDir = Path.of(args[i + 1]);
                        }
                    }
                }
                i++;
            }

            Path gameDir = fenixGameDir != null ? fenixGameDir
                    : peekedGameDir != null ? peekedGameDir
                    : Path.of(".");
            if (modsDir == null) {
                modsDir = gameDir.resolve("mods");
            }
            return new Options(gameMain, gameJar, gameDir, modsDir, side, dryRun,
                    gameArgs.toArray(String[]::new));
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
                        "--fenix.side must be 'client' or 'server', not '" + text + "'");
            };
        }
    }
}
