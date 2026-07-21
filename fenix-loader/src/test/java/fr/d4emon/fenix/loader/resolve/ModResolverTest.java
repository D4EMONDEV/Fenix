package fr.d4emon.fenix.loader.resolve;

import fr.d4emon.fenix.api.Side;
import fr.d4emon.fenix.api.Version;
import fr.d4emon.fenix.api.VersionRange;
import fr.d4emon.fenix.loader.discovery.ModCandidate;
import fr.d4emon.fenix.loader.metadata.ModDependency;
import fr.d4emon.fenix.loader.metadata.ModMetadata;
import fr.d4emon.fenix.loader.metadata.ModSide;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModResolverTest {

    private static final Map<String, Version> NO_BUILTINS = Map.of();

    private static ModCandidate mod(String id, String version, ModDependency... depends) {
        return mod(id, version, ModSide.BOTH, depends);
    }

    private static ModCandidate mod(String id, String version, ModSide side, ModDependency... depends) {
        ModMetadata metadata = new ModMetadata(
                id, Version.parse(version), null, null, null, null, null, side, List.of(depends), null, null);
        return new ModCandidate(metadata, Path.of(id + ".jar"));
    }

    private static ModDependency dep(String id, String constraint) {
        return new ModDependency(id, VersionRange.parse(constraint));
    }

    private static List<String> order(ResolutionResult result) {
        return result.loadOrder().stream().map(ModCandidate::id).toList();
    }

    @Nested
    @DisplayName("ordering")
    class Ordering {

        @Test
        void anEmptyModsFolderResolvesToNothing() {
            ResolutionResult result = ModResolver.resolve(List.of(), Side.CLIENT, NO_BUILTINS);

            assertEquals(List.of(), result.loadOrder());
            assertEquals(List.of(), result.skipped());
        }

        @Test
        @DisplayName("unrelated mods load alphabetically, so the order is reproducible")
        void unconstrainedModsAreAlphabetical() {
            ResolutionResult result = ModResolver.resolve(
                    List.of(mod("charlie", "1.0.0"), mod("alpha", "1.0.0"), mod("bravo", "1.0.0")),
                    Side.CLIENT, NO_BUILTINS);

            assertEquals(List.of("alpha", "bravo", "charlie"), order(result));
        }

        @Test
        @DisplayName("a dependency beats the alphabet")
        void dependenciesComeFirst() {
            ResolutionResult result = ModResolver.resolve(
                    List.of(
                            mod("aaa-addon", "1.0.0", dep("zzz-library", "*")),
                            mod("zzz-library", "1.0.0")),
                    Side.CLIENT, NO_BUILTINS);

            assertEquals(List.of("zzz-library", "aaa-addon"), order(result));
        }

        @Test
        void chainsResolveEndToEnd() {
            ResolutionResult result = ModResolver.resolve(
                    List.of(
                            mod("top", "1.0.0", dep("middle", "*")),
                            mod("middle", "1.0.0", dep("base", "*")),
                            mod("base", "1.0.0")),
                    Side.CLIENT, NO_BUILTINS);

            assertEquals(List.of("base", "middle", "top"), order(result));
        }

        @Test
        @DisplayName("input order never changes the outcome")
        void isDeterministicWhateverTheFileSystemSaid() {
            List<ModCandidate> mods = List.of(
                    mod("one", "1.0.0", dep("three", "*")),
                    mod("two", "1.0.0"),
                    mod("three", "1.0.0"));

            List<String> forward = order(ModResolver.resolve(mods, Side.CLIENT, NO_BUILTINS));
            List<String> backward = order(ModResolver.resolve(mods.reversed(), Side.CLIENT, NO_BUILTINS));

            assertEquals(forward, backward);
        }
    }

    @Nested
    @DisplayName("sides")
    class Sides {

        @Test
        @DisplayName("a client-only mod on a server is set aside, not an error")
        void skipsModsDeclaringTheOtherSide() {
            ResolutionResult result = ModResolver.resolve(
                    List.of(mod("shaders", "1.0.0", ModSide.CLIENT), mod("common", "1.0.0")),
                    Side.SERVER, NO_BUILTINS);

            assertEquals(List.of("common"), order(result));
            assertEquals("shaders", result.skipped().getFirst().id());
        }

        @Test
        @DisplayName("depending on a mod that sat out names the side mismatch")
        void reportsADependencyOnASkippedMod() {
            ResolutionException failure = assertThrows(ResolutionException.class, () -> ModResolver.resolve(
                    List.of(
                            mod("shaders", "1.0.0", ModSide.CLIENT),
                            mod("shader-addon", "1.0.0", dep("shaders", "*"))),
                    Side.SERVER, NO_BUILTINS));

            String problem = failure.problems().getFirst();
            assertTrue(problem.contains("shader-addon"), problem);
            assertTrue(problem.contains("only loads on the client"), problem);
            assertTrue(problem.contains("this is the server"), problem);
        }
    }

    @Nested
    @DisplayName("dependency problems")
    class Problems {

        @Test
        void reportsAMissingDependency() {
            ResolutionException failure = assertThrows(ResolutionException.class, () -> ModResolver.resolve(
                    List.of(mod("example-mod", "1.0.0", dep("fenix-api", "^0.1.0"))),
                    Side.CLIENT, NO_BUILTINS));

            String problem = failure.problems().getFirst();
            assertTrue(problem.contains("example-mod"), problem);
            assertTrue(problem.contains("fenix-api"), problem);
            assertTrue(problem.contains("not installed"), problem);
        }

        @Test
        @DisplayName("a version mismatch shows what was wanted and what is there")
        void reportsAVersionMismatch() {
            ResolutionException failure = assertThrows(ResolutionException.class, () -> ModResolver.resolve(
                    List.of(
                            mod("example-mod", "1.0.0", dep("library", ">=2.0.0")),
                            mod("library", "1.5.0")),
                    Side.CLIENT, NO_BUILTINS));

            String problem = failure.problems().getFirst();
            assertTrue(problem.contains(">=2.0.0"), problem);
            assertTrue(problem.contains("library 1.5.0 is present"), problem);
        }

        @Test
        void reportsDuplicateIdsWithBothFiles() {
            ModMetadata metadata = new ModMetadata(
                    "twin", Version.parse("1.0.0"), null, null, null, null, null, ModSide.BOTH, null, null, null);

            ResolutionException failure = assertThrows(ResolutionException.class, () -> ModResolver.resolve(
                    List.of(
                            new ModCandidate(metadata, Path.of("twin-1.0.0.jar")),
                            new ModCandidate(metadata, Path.of("twin-copy.jar"))),
                    Side.CLIENT, NO_BUILTINS));

            String problem = failure.problems().getFirst();
            assertTrue(problem.contains("twin-1.0.0.jar"), problem);
            assertTrue(problem.contains("twin-copy.jar"), problem);
        }

        @Test
        void reportsAModClaimingABuiltInId() {
            ResolutionException failure = assertThrows(ResolutionException.class, () -> ModResolver.resolve(
                    List.of(mod("minecraft", "99.0.0")),
                    Side.CLIENT, Map.of("minecraft", Version.parse("26.2"))));

            assertTrue(failure.problems().getFirst().contains("built-in id"));
        }

        @Test
        @DisplayName("every problem is reported in one go")
        void collectsEveryProblemBeforeFailing() {
            ResolutionException failure = assertThrows(ResolutionException.class, () -> ModResolver.resolve(
                    List.of(
                            mod("one", "1.0.0", dep("ghost", "*")),
                            mod("two", "1.0.0", dep("phantom", "*"))),
                    Side.CLIENT, NO_BUILTINS));

            assertEquals(2, failure.problems().size());
            assertTrue(failure.getMessage().contains("2 mod problems"), failure.getMessage());
        }
    }

    @Nested
    @DisplayName("built-in versions")
    class Builtins {

        private static final Map<String, Version> BUILTINS = Map.of(
                "fenix", Version.parse("0.1.0"),
                "minecraft", Version.parse("26.2"));

        @Test
        void satisfyDependenciesWithoutBeingMods() {
            ResolutionResult result = ModResolver.resolve(
                    List.of(mod("example-mod", "1.0.0",
                            dep("fenix", ">=0.1.0"), dep("minecraft", "~26.2"))),
                    Side.CLIENT, BUILTINS);

            assertEquals(List.of("example-mod"), order(result));
        }

        @Test
        void stillEnforceTheirRanges() {
            ResolutionException failure = assertThrows(ResolutionException.class, () -> ModResolver.resolve(
                    List.of(mod("example-mod", "1.0.0", dep("minecraft", "~26.3"))),
                    Side.CLIENT, BUILTINS));

            String problem = failure.problems().getFirst();
            assertTrue(problem.contains("minecraft 26.2.0 is present"), problem);
        }
    }

    @Nested
    @DisplayName("cycles")
    class Cycles {

        @Test
        void aTwoModCycleNamesItsPath() {
            ResolutionException failure = assertThrows(ResolutionException.class, () -> ModResolver.resolve(
                    List.of(
                            mod("alpha", "1.0.0", dep("bravo", "*")),
                            mod("bravo", "1.0.0", dep("alpha", "*"))),
                    Side.CLIENT, NO_BUILTINS));

            String problem = failure.problems().getFirst();
            assertTrue(problem.contains("circle"), problem);
            assertTrue(problem.contains("alpha -> bravo -> alpha"), problem);
        }

        @Test
        void aSelfDependencyIsACycleOfOne() {
            ResolutionException failure = assertThrows(ResolutionException.class, () -> ModResolver.resolve(
                    List.of(mod("narcissus", "1.0.0", dep("narcissus", "*"))),
                    Side.CLIENT, NO_BUILTINS));

            assertTrue(failure.problems().getFirst().contains("narcissus -> narcissus"));
        }

        @Test
        @DisplayName("a mod that merely depends on the cycle is not blamed for it")
        void namesOnlyTheCycleItself() {
            ResolutionException failure = assertThrows(ResolutionException.class, () -> ModResolver.resolve(
                    List.of(
                            mod("innocent", "1.0.0", dep("looper", "*")),
                            mod("looper", "1.0.0", dep("returner", "*")),
                            mod("returner", "1.0.0", dep("looper", "*"))),
                    Side.CLIENT, NO_BUILTINS));

            String problem = failure.problems().getFirst();
            assertTrue(problem.contains("looper -> returner -> looper"), problem);
        }
    }
}
