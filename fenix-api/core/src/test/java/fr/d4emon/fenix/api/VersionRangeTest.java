package fr.d4emon.fenix.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VersionRangeTest {

    private static void assertMatches(String constraint, String version) {
        assertTrue(VersionRange.parse(constraint).contains(Version.parse(version)),
                constraint + " should accept " + version);
    }

    private static void assertRejects(String constraint, String version) {
        assertFalse(VersionRange.parse(constraint).contains(Version.parse(version)),
                constraint + " should reject " + version);
    }

    @Nested
    @DisplayName("exact and open constraints")
    class Simple {

        @Test
        void anyMatchesEverything() {
            assertMatches("*", "0.0.1");
            assertMatches("*", "999.0.0");
            assertMatches("*", "1.0.0-rc.1");
        }

        @Test
        void aBareVersionIsExact() {
            assertMatches("1.2.3", "1.2.3");
            assertRejects("1.2.3", "1.2.4");
            assertRejects("1.2.3", "1.2.2");
        }

        @Test
        void comparisonOperatorsRespectTheirBoundary() {
            assertMatches(">=1.2.0", "1.2.0");
            assertRejects(">1.2.0", "1.2.0");
            assertMatches("<=1.2.0", "1.2.0");
            assertRejects("<1.2.0", "1.2.0");

            assertMatches(">1.2.0", "1.2.1");
            assertMatches("<1.2.0", "1.1.9");
        }

        @Test
        @DisplayName("'>=' is not read as '>' of '=1.0.0'")
        void prefersTheLongerOperator() {
            assertEquals(VersionRange.atLeast(new Version(1, 0, 0)), VersionRange.parse(">=1.0.0"));
        }
    }

    @Nested
    @DisplayName("caret — compatible updates")
    class Caret {

        @Test
        void allowsMinorAndPatchAboveOne() {
            assertMatches("^1.2.0", "1.2.0");
            assertMatches("^1.2.0", "1.9.9");
            assertRejects("^1.2.0", "2.0.0");
            assertRejects("^1.2.0", "1.1.9");
        }

        @Test
        @DisplayName("below 1.0.0 a minor bump is breaking, so the range tightens")
        void tightensForZeroMajor() {
            assertMatches("^0.2.0", "0.2.9");
            assertRejects("^0.2.0", "0.3.0");
        }

        @Test
        @DisplayName("below 0.1.0 even a patch bump is breaking")
        void tightensFurtherForZeroMinor() {
            assertMatches("^0.0.3", "0.0.3");
            assertRejects("^0.0.3", "0.0.4");
        }
    }

    @Nested
    @DisplayName("tilde — patch updates")
    class Tilde {

        @Test
        void staysOnTheMinorLine() {
            assertMatches("~1.2.0", "1.2.0");
            assertMatches("~1.2.0", "1.2.99");
            assertRejects("~1.2.0", "1.3.0");
            assertRejects("~1.2.0", "1.1.0");
        }

        @Test
        @DisplayName("a shorthand Minecraft version works, since the patch defaults to zero")
        void acceptsShorthand() {
            assertMatches("~26.2", "26.2.0");
            assertMatches("~26.2", "26.2.4");
            assertRejects("~26.2", "26.3.0");
        }
    }

    @Nested
    class PreReleases {

        @Test
        @DisplayName("a pre-release falls inside a range that spans it")
        void areIncludedWhenInRange() {
            assertMatches("^1.0.0", "1.5.0-rc.1");
        }

        @Test
        void stillSortBelowTheirRelease() {
            assertRejects(">=1.0.0", "1.0.0-rc.1");
        }
    }

    @Nested
    class Parsing {

        @ParameterizedTest
        @ValueSource(strings = {"", "  ", "^", ">=", "~", ">=x", "1.2.3 || 2.0.0", "abc"})
        void rejectsAnythingUnrecognised(String constraint) {
            assertThrows(IllegalArgumentException.class, () -> VersionRange.parse(constraint));
        }

        @Test
        void rejectsNull() {
            assertThrows(NullPointerException.class, () -> VersionRange.parse(null));
        }

        @Test
        void toleratesWhitespaceAroundTheOperator() {
            assertMatches(" >= 1.2.0 ", "1.3.0");
        }
    }

    @Nested
    @DisplayName("rendering, which is what a player sees when a dependency fails")
    class Rendering {

        @ParameterizedTest
        @CsvSource({
                "*,        *",
                "1.2.3,    1.2.3",
                "=1.2.3,   1.2.3",
                ">=1.2.0,  >=1.2.0",
                ">1.2.0,   >1.2.0",
                "<=1.2.0,  <=1.2.0",
                "^1.2.0,   >=1.2.0 <2.0.0",
                "^0.2.0,   >=0.2.0 <0.3.0",
                "~1.2.0,   >=1.2.0 <1.3.0",
        })
        void spellsOutTheActualBounds(String constraint, String expected) {
            assertEquals(expected, VersionRange.parse(constraint).toString());
        }
    }
}
