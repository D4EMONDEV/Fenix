package fr.d4emon.fenix.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What a settings file does when it is not exactly what the mod expected —
 * which is the normal case, since people edit these by hand.
 */
class RecordsTest {

    enum Mode { QUIET, LOUD }

    record Inner(int depth, String label) {
    }

    record Settings(boolean enabled, int count, double ratio, String name,
                    Mode mode, List<String> tags, Inner nested) {
    }

    record Checked(int count) {
        Checked {
            if (count < 1) {
                throw new IllegalArgumentException("count must be at least 1");
            }
        }
    }

    private static final Settings DEFAULTS = new Settings(
            true, 20, 0.5, "fenix", Mode.QUIET, List.of("a"), new Inner(1, "inner"));

    private final List<String> unknown = new ArrayList<>();

    private Settings read(String json) {
        return Records.read(parse(json), DEFAULTS, unknown, "");
    }

    private static JsonObject parse(String json) {
        return JsonParser.parseString(json).getAsJsonObject();
    }

    @Nested
    @DisplayName("what the file leaves out")
    class Missing {

        @Test
        @DisplayName("an empty file is every default, not every zero")
        void emptyFileIsAllDefaults() {
            assertEquals(DEFAULTS, read("{}"));
        }

        @Test
        @DisplayName("one missing setting takes its default and leaves the rest alone")
        void oneMissingSetting() {
            // The failure this guards against: a player deletes a line, or
            // updates to a version that added one, and gets `false` and `0`
            // for everything they did not mention.
            Settings settings = read("""
                    { "count": 99 }
                    """);

            assertEquals(99, settings.count());
            assertEquals(true, settings.enabled());
            assertEquals("fenix", settings.name());
            assertEquals(Mode.QUIET, settings.mode());
        }

        @Test
        @DisplayName("a null reads as absent rather than as null")
        void explicitNullIsAbsent() {
            assertEquals("fenix", read("""
                    { "name": null }
                    """).name());
        }

        @Test
        @DisplayName("a partly written nested record keeps the rest of its defaults")
        void nestedFallsBackPerField() {
            assertEquals(new Inner(7, "inner"), read("""
                    { "nested": { "depth": 7 } }
                    """).nested());
        }
    }

    @Nested
    @DisplayName("what the file gets wrong")
    class Wrong {

        @Test
        @DisplayName("an unknown key is reported, never silently dropped")
        void unknownKeyIsReported() {
            // A mistyped setting that quietly does nothing is the classic
            // configuration bug: the player has no way to tell it was ignored.
            read("""
                    { "enabledd": false }
                    """);

            assertEquals(List.of("enabledd"), unknown);
        }

        @Test
        @DisplayName("an unknown key inside a nested record says where it is")
        void unknownNestedKeyIsQualified() {
            read("""
                    { "nested": { "dpeth": 3 } }
                    """);

            assertEquals(List.of("nested.dpeth"), unknown);
        }

        @Test
        @DisplayName("a value of the wrong shape names the field")
        void wrongShapeNamesTheField() {
            ConfigException thrown = assertThrows(ConfigException.class, () -> read("""
                    { "tags": "not-a-list" }
                    """));

            assertTrue(thrown.getMessage().startsWith("tags:"), thrown.getMessage());
        }

        @Test
        @DisplayName("an enum that is not one of the choices lists them")
        void badEnumListsTheChoices() {
            ConfigException thrown = assertThrows(ConfigException.class, () -> read("""
                    { "mode": "SCREAMING" }
                    """));

            assertTrue(thrown.getMessage().contains("QUIET"), thrown.getMessage());
        }

        @Test
        @DisplayName("an enum is matched whatever case it was typed in")
        void enumIsCaseInsensitive() {
            assertEquals(Mode.LOUD, read("""
                    { "mode": "loud" }
                    """).mode());
        }
    }

    @Nested
    @DisplayName("what the mod itself refuses")
    class Validation {

        @Test
        @DisplayName("a compact constructor's message reaches the player")
        void validationMessageSurvives() {
            // Written once, in the record, and impossible to skip. What matters
            // is that the author's sentence arrives rather than a stack trace.
            ConfigException thrown = assertThrows(ConfigException.class,
                    () -> Records.read(parse("""
                            { "count": 0 }
                            """), new Checked(5), unknown, ""));

            assertEquals("count must be at least 1", thrown.getMessage());
        }
    }

    @Nested
    @DisplayName("writing it back")
    class Writing {

        @Test
        @DisplayName("a record survives being written and read again")
        void roundTrips() {
            Settings original = new Settings(false, 3, 1.25, "x", Mode.LOUD,
                    List.of("p", "q"), new Inner(9, "deep"));

            assertEquals(original, Records.read(Records.write(original), DEFAULTS, unknown, ""));
            assertEquals(List.of(), unknown);
        }

        @Test
        @DisplayName("every setting is written, so an added one becomes visible")
        void writesEverySetting() {
            // The file is rewritten after each load precisely so a setting
            // added by an update appears with its default, rather than staying
            // invisible until somebody reads a changelog.
            assertEquals(
                    List.of("enabled", "count", "ratio", "name", "mode", "tags", "nested"),
                    List.copyOf(Records.write(DEFAULTS).keySet()));
        }
    }
}
