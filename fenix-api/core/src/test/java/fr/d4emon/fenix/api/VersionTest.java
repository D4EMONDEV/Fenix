package fr.d4emon.fenix.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VersionTest {

    @Nested
    @DisplayName("parsing")
    class Parsing {

        @Test
        void readsEveryComponent() {
            Version version = Version.parse("1.2.3-rc.1+build.7");

            assertEquals(1, version.major());
            assertEquals(2, version.minor());
            assertEquals(3, version.patch());
            assertEquals("rc.1", version.preRelease());
            assertEquals("build.7", version.build());
        }

        @Test
        @DisplayName("fills in a missing minor or patch, so Minecraft versions parse")
        void toleratesMissingComponents() {
            assertEquals(new Version(26, 2, 0), Version.parse("26.2"));
            assertEquals(new Version(26, 0, 0), Version.parse("26"));
        }

        @Test
        void toleratesSurroundingWhitespace() {
            assertEquals(new Version(1, 0, 0), Version.parse("  1.0.0  "));
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "x", "1.2.3.4", "1.-2.3", "01.2.3", "1.2.3-", "1.2.3+", "v1.2.3"})
        void rejectsAnythingElse(String text) {
            assertThrows(IllegalArgumentException.class, () -> Version.parse(text));
        }

        @Test
        void rejectsNull() {
            assertThrows(NullPointerException.class, () -> Version.parse(null));
        }

        @Test
        void rejectsNegativeComponents() {
            assertThrows(IllegalArgumentException.class, () -> new Version(1, -1, 0));
        }

        @Test
        void roundTripsThroughToString() {
            for (String text : List.of("1.2.3", "0.1.0-SNAPSHOT", "1.0.0-rc.1+build.7", "26.2.0")) {
                assertEquals(text, Version.parse(text).toString());
            }
        }

        @Test
        void normalisesShorthandOnTheWayOut() {
            assertEquals("26.2.0", Version.parse("26.2").toString());
        }
    }

    @Nested
    @DisplayName("ordering")
    class Ordering {

        @Test
        void comparesNumbersBeforeAnythingElse() {
            assertOrdered("1.0.0", "1.0.1", "1.1.0", "2.0.0");
        }

        @Test
        @DisplayName("a pre-release sorts below the release it leads to")
        void preReleaseIsOlderThanRelease() {
            assertTrue(Version.parse("1.0.0-rc.1").compareTo(Version.parse("1.0.0")) < 0);
            assertTrue(Version.parse("0.1.0-SNAPSHOT").compareTo(Version.parse("0.1.0")) < 0);
        }

        @Test
        @DisplayName("the ordering example from the semver specification")
        void followsTheSpecification() {
            assertOrdered(
                    "1.0.0-alpha",
                    "1.0.0-alpha.1",
                    "1.0.0-alpha.beta",
                    "1.0.0-beta",
                    "1.0.0-beta.2",
                    "1.0.0-beta.11",
                    "1.0.0-rc.1",
                    "1.0.0");
        }

        @Test
        @DisplayName("numeric identifiers compare by value, not as text")
        void comparesNumericIdentifiersNumerically() {
            // Lexically "10" sorts before "2"; numerically it must not.
            assertTrue(Version.parse("1.0.0-2").compareTo(Version.parse("1.0.0-10")) < 0);
        }

        @Test
        void numericIdentifiersRankBelowAlphanumericOnes() {
            assertTrue(Version.parse("1.0.0-1").compareTo(Version.parse("1.0.0-alpha")) < 0);
        }

        @Test
        @DisplayName("build metadata is ignored, unlike in equals")
        void ignoresBuildMetadata() {
            Version left = Version.parse("1.0.0+a");
            Version right = Version.parse("1.0.0+b");

            assertEquals(0, left.compareTo(right));
            assertNotEquals(left, right);
        }

        /**
         * Asserts that the given versions are in strictly ascending order, and
         * that sorting a shuffled copy puts them back.
         */
        private void assertOrdered(String... texts) {
            List<Version> expected = new ArrayList<>();
            for (String text : texts) {
                expected.add(Version.parse(text));
            }

            for (int i = 0; i < expected.size() - 1; i++) {
                Version lower = expected.get(i);
                Version higher = expected.get(i + 1);
                assertTrue(lower.compareTo(higher) < 0, lower + " should sort below " + higher);
                assertTrue(higher.compareTo(lower) > 0, higher + " should sort above " + lower);
            }

            List<Version> shuffled = new ArrayList<>(expected);
            java.util.Collections.reverse(shuffled);
            java.util.Collections.sort(shuffled);
            assertEquals(expected, shuffled);
        }
    }

    @Nested
    class PreReleaseFlag {

        @Test
        void isSetWhenIdentifiersArePresent() {
            assertTrue(Version.parse("1.0.0-rc.1").isPreRelease());
            assertFalse(Version.parse("1.0.0").isPreRelease());
            assertFalse(Version.parse("1.0.0+build.7").isPreRelease());
        }
    }
}
