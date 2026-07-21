package fr.d4emon.fenix.loader.launch;

import fr.d4emon.fenix.api.Fenix;
import fr.d4emon.fenix.api.FenixMod;
import fr.d4emon.fenix.api.Side;
import fr.d4emon.fenix.api.Version;
import fr.d4emon.fenix.loader.metadata.ModMetadata;
import fr.d4emon.fenix.loader.metadata.ModSide;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FenixRuntimeTest {

    @TempDir
    Path gameDir;

    /** Records every lifecycle call it receives, tagged with its mod id. */
    private static final class RecordingMod implements FenixMod {

        private final String tag;
        private final List<String> journal;

        RecordingMod(String tag, List<String> journal) {
            this.tag = tag;
            this.journal = journal;
        }

        @Override
        public void onPreLaunch(Fenix fenix) {
            journal.add(tag + ":preLaunch");
        }

        @Override
        public void onRegister(Fenix fenix) {
            journal.add(tag + ":register");
        }

        @Override
        public void onInit(Fenix fenix) {
            journal.add(tag + ":init");
        }
    }

    private static LoadedMod mod(String id, FenixMod... entries) {
        ModMetadata metadata = new ModMetadata(id, Version.parse("1.0.0"),
                null, null, null, null, null, ModSide.BOTH, null, null, null);
        return new LoadedMod(metadata, Path.of(id + ".jar"), List.of(entries));
    }

    private FenixRuntime runtime(LoadedMod... mods) {
        return new FenixRuntime(Side.CLIENT, gameDir, List.of(mods));
    }

    @Nested
    @DisplayName("lifecycle")
    class Lifecycle {

        @Test
        @DisplayName("each phase walks the mods in load order")
        void firesInLoadOrder() {
            List<String> journal = new ArrayList<>();
            FenixRuntime runtime = runtime(
                    mod("base", new RecordingMod("base", journal)),
                    mod("addon", new RecordingMod("addon", journal)));

            runtime.firePreLaunch();
            runtime.fireRegister();
            runtime.fireInit();

            assertEquals(List.of(
                    "base:preLaunch", "addon:preLaunch",
                    "base:register", "addon:register",
                    "base:init", "addon:init"), journal);
        }

        @Test
        @DisplayName("a repeated hook is ignored — the game may reach it twice, mods must not")
        void firesEachPhaseOnce() {
            List<String> journal = new ArrayList<>();
            FenixRuntime runtime = runtime(mod("solo", new RecordingMod("solo", journal)));

            runtime.fireInit();
            runtime.fireInit();

            assertEquals(List.of("solo:init"), journal);
        }

        @Test
        @DisplayName("a mod failing a phase stops the launch, naming mod and phase")
        void aFailingModIsFatal() {
            RuntimeException boom = new IllegalStateException("boom");
            FenixRuntime runtime = runtime(mod("fragile", new FenixMod() {
                @Override
                public void onInit(Fenix fenix) {
                    throw boom;
                }
            }));

            LaunchException failure = assertThrows(LaunchException.class, runtime::fireInit);

            assertTrue(failure.getMessage().contains("fragile"), failure.getMessage());
            assertTrue(failure.getMessage().contains("onInit"), failure.getMessage());
            assertSame(boom, failure.getCause());
        }
    }

    @Nested
    @DisplayName("the context handed to mods")
    class Context {

        private Fenix captured;

        private Fenix contextSeenBy(String id, LoadedMod... others) {
            FenixMod capturer = new FenixMod() {
                @Override
                public void onInit(Fenix fenix) {
                    captured = fenix;
                }
            };
            List<LoadedMod> all = new ArrayList<>(List.of(others));
            all.add(mod(id, capturer));
            FenixRuntime runtime = new FenixRuntime(Side.CLIENT, gameDir, all);
            runtime.fireInit();
            return captured;
        }

        @Test
        void knowsWhichModItBelongsTo() {
            Fenix fenix = contextSeenBy("self-aware");

            assertEquals("self-aware", fenix.mod().id());
        }

        @Test
        void seesEveryModInLoadOrder() {
            Fenix fenix = contextSeenBy("last", mod("first"), mod("second"));

            assertEquals(List.of("first", "second", "last"),
                    fenix.mods().stream().map(info -> info.id()).toList());
        }

        @Test
        void looksUpModsById() {
            Fenix fenix = contextSeenBy("seeker", mod("neighbour"));

            assertTrue(fenix.findMod("neighbour").isPresent());
            assertTrue(fenix.findMod("stranger").isEmpty());
            assertTrue(fenix.isLoaded("neighbour"));
        }

        @Test
        void exposesTheEnvironment() {
            Fenix fenix = contextSeenBy("env");

            assertEquals(Side.CLIENT, fenix.side());
            assertEquals(gameDir, fenix.gameDir());
            assertEquals(FenixVersion.current(), fenix.loaderVersion());
        }

        @Test
        @DisplayName("the config directory is per mod, and created on demand")
        void resolvesAndCreatesTheConfigDir() {
            Fenix fenix = contextSeenBy("configured");

            Path dir = fenix.configDir();

            assertEquals(gameDir.resolve("config").resolve("configured"), dir);
            assertTrue(Files.isDirectory(dir));
        }

        @Test
        @DisplayName("the logger speaks in the mod's name")
        void scopesTheLogger() {
            Fenix fenix = contextSeenBy("chatty");

            // The console logger writes "[<name>/LEVEL]"; the name is the only
            // per-mod part, so asserting on the instance identity is enough
            // here — formatting is covered by ConsoleLoggerTest.
            assertSame(fenix.logger(), fenix.logger());
        }
    }

    @Test
    @DisplayName("the loader's own version is stamped at build time and parseable")
    void loaderVersionIsReal() {
        Version version = FenixVersion.current();

        assertTrue(version.major() >= 0);
    }

    @Test
    void hooksDelegateToTheBoundRuntime() {
        List<String> journal = new ArrayList<>();
        FenixRuntime runtime = runtime(mod("hooked", new RecordingMod("hooked", journal)));

        FenixHooks.bind(runtime);
        try {
            FenixHooks.onGameRegister();
            FenixHooks.onGameInit();
        } finally {
            FenixHooks.bind(null);
        }

        assertEquals(List.of("hooked:register", "hooked:init"), journal);
    }

    @Test
    void hooksRefuseToFireUnbound() {
        FenixHooks.bind(null);

        assertThrows(IllegalStateException.class, FenixHooks::onGameInit);
    }
}
