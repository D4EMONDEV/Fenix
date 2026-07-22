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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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
                id, Version.parse(version), null, null, null, null, null, side, List.of(depends), null, null, null, null);
        return new ModCandidate(metadata, Path.of(id + ".jar"));
    }

    /** A mod as it arrives from inside another mod's jar. */
    private static ModCandidate bundled(String id, String version) {
        ModMetadata metadata = new ModMetadata(id, Version.parse(version), null, null, null, null,
                null, ModSide.BOTH, null, null, null, null, null);
        return new ModCandidate(metadata, Path.of(id + "-" + version + ".jar"), true);
    }

    /** A mod that refuses to run alongside something. */
    private static ModCandidate breaker(String id, ModDependency... broken) {
        ModMetadata metadata = new ModMetadata(id, Version.parse("1.0.0"), null, null, null, null,
                null, ModSide.BOTH, null, List.of(broken), null, null, null);
        return new ModCandidate(metadata, Path.of(id + ".jar"));
    }

    /** A mod that wants to load after something, without requiring it. */
    private static ModCandidate follower(String id, ModDependency... after) {
        ModMetadata metadata = new ModMetadata(id, Version.parse("1.0.0"), null, null, null, null,
                null, ModSide.BOTH, null, null, List.of(after), null, null);
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
                    "twin", Version.parse("1.0.0"), null, null, null, null, null, ModSide.BOTH, null, null, null, null, null);

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

    @Nested
    @DisplayName("breaks")
    class Breaks {

        @Test
        @DisplayName("a mod refuses to load beside something it declares broken")
        void refusesABrokenCombination() {
            ResolutionException thrown = assertThrows(ResolutionException.class, () ->
                    ModResolver.resolve(
                            List.of(breaker("patch", dep("legacy", "<2.0.0")),
                                    mod("legacy", "1.4.0", ModSide.BOTH)),
                            Side.CLIENT, NO_BUILTINS));

            // Naming both, and the constraint: "it crashed" is what this
            // exists to replace.
            assertTrue(thrown.getMessage().contains("legacy"), thrown.getMessage());
            assertTrue(thrown.getMessage().contains("cannot run with"), thrown.getMessage());
        }

        @Test
        @DisplayName("a version outside the broken range is fine")
        void allowsAVersionThatIsNotBroken() {
            assertDoesNotThrow(() -> ModResolver.resolve(
                    List.of(breaker("patch", dep("legacy", "<2.0.0")),
                            mod("legacy", "2.0.0", ModSide.BOTH)),
                    Side.CLIENT, NO_BUILTINS));
        }

        @Test
        @DisplayName("what is not installed cannot break anything")
        void allowsAnAbsentMod() {
            assertDoesNotThrow(() -> ModResolver.resolve(
                    List.of(breaker("patch", dep("legacy", "*"))), Side.CLIENT, NO_BUILTINS));
        }

        @Test
        @DisplayName("a builtin can be declared broken too")
        void checksBuiltins() {
            assertThrows(ResolutionException.class, () -> ModResolver.resolve(
                    List.of(breaker("patch", dep("minecraft", "<26.2"))), Side.CLIENT,
                    Map.of("minecraft", Version.parse("26.1.0"))));
        }
    }

    @Nested
    @DisplayName("after")
    class After {

        @Test
        @DisplayName("a mod loads after what it names, without requiring it")
        void ordersWithoutRequiring() {
            // Alphabetically "aaa" comes first, so only the edge can put it last.
            ResolutionResult result = ModResolver.resolve(
                    List.of(follower("aaa", dep("zzz", "*")), mod("zzz", "1.0.0", ModSide.BOTH)),
                    Side.CLIENT, NO_BUILTINS);

            assertEquals(List.of("zzz", "aaa"), order(result));
        }

        @Test
        @DisplayName("naming something absent is not an error")
        void toleratesAnAbsentMod() {
            ResolutionResult result = ModResolver.resolve(
                    List.of(follower("patch", dep("never-installed", "*"))),
                    Side.CLIENT, NO_BUILTINS);

            // The whole point: an optional integration must not become a
            // requirement just to be ordered correctly.
            assertEquals(List.of("patch"), order(result));
        }

        @Test
        @DisplayName("a mod named by both depends and after is one edge, not two")
        void doesNotCountTheSameEdgeTwice() {
            ModMetadata both = new ModMetadata("patch", Version.parse("1.0.0"), null, null, null,
                    null, null, ModSide.BOTH, List.of(dep("lib", "*")), null,
                    List.of(dep("lib", "*")), null, null);

            ResolutionResult result = ModResolver.resolve(
                    List.of(new ModCandidate(both, Path.of("patch.jar")),
                            mod("lib", "1.0.0", ModSide.BOTH)),
                    Side.CLIENT, NO_BUILTINS);

            // Counted twice, the mod is blocked by one satisfied edge forever
            // and never reaches the load order at all.
            assertEquals(List.of("lib", "patch"), order(result));
        }
    }

    @Nested
    @DisplayName("bundled duplicates")
    class Bundled {

        @Test
        @DisplayName("two mods carrying the same library keep the newer copy")
        void keepsTheNewerBundledCopy() {
            ResolutionResult result = ModResolver.resolve(
                    List.of(bundled("lib", "1.0.0"), bundled("lib", "2.1.0")),
                    Side.CLIENT, NO_BUILTINS);

            // Refusing here would mean any two mods sharing a dependency could
            // not be installed together, which neither author could fix.
            assertEquals(List.of("lib"), order(result));
            assertEquals(Version.parse("2.1.0"), result.loadOrder().get(0).version());
        }

        @Test
        @DisplayName("order does not decide which copy wins")
        void picksTheNewerWhicheverCameFirst() {
            ResolutionResult result = ModResolver.resolve(
                    List.of(bundled("lib", "2.1.0"), bundled("lib", "1.0.0")),
                    Side.CLIENT, NO_BUILTINS);

            assertEquals(Version.parse("2.1.0"), result.loadOrder().get(0).version());
        }

        @Test
        @DisplayName("a loose jar and a bundled one still resolve")
        void reconcilesLooseWithBundled() {
            ResolutionResult result = ModResolver.resolve(
                    List.of(mod("lib", "1.0.0", ModSide.BOTH), bundled("lib", "2.0.0")),
                    Side.CLIENT, NO_BUILTINS);

            assertEquals(Version.parse("2.0.0"), result.loadOrder().get(0).version());
        }

        @Test
        @DisplayName("two loose jars are still the player's to sort out")
        void refusesTwoLooseCopies() {
            ResolutionException thrown = assertThrows(ResolutionException.class, () ->
                    ModResolver.resolve(
                            List.of(mod("lib", "1.0.0", ModSide.BOTH),
                                    mod("lib", "2.0.0", ModSide.BOTH)),
                            Side.CLIENT, NO_BUILTINS));

            assertTrue(thrown.getMessage().contains("remove one of them"), thrown.getMessage());
        }
    }
}
