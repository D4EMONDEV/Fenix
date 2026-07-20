package fr.d4emon.fenix.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ModInfoTest {

    private static final Version VERSION = new Version(1, 0, 0);

    @Test
    @DisplayName("an absent name falls back to the id")
    void fillsInDefaults() {
        ModInfo info = new ModInfo("example-mod", VERSION);

        assertEquals("example-mod", info.name());
        assertEquals("", info.description());
        assertEquals("", info.license());
        assertEquals(List.of(), info.authors());
    }

    @Test
    void treatsABlankNameAsAbsent() {
        assertEquals("example-mod", new ModInfo("example-mod", VERSION, "  ", null, null, null).name());
    }

    @ParameterizedTest
    @ValueSource(strings = {"fenix", "fenix-api-core", "example-mod", "a1"})
    void acceptsValidIds(String id) {
        assertEquals(id, new ModInfo(id, VERSION).id());
    }

    @ParameterizedTest
    @DisplayName("rejects ids that would be ambiguous in a path or a log line")
    @ValueSource(strings = {"A", "Example-Mod", "1mod", "-mod", "mod_name", "a", "mod name", "mod.name", ""})
    void rejectsInvalidIds(String id) {
        assertThrows(IllegalArgumentException.class, () -> new ModInfo(id, VERSION));
    }

    @Test
    void rejectsMissingIdOrVersion() {
        assertThrows(NullPointerException.class, () -> new ModInfo(null, VERSION));
        assertThrows(NullPointerException.class, () -> new ModInfo("example-mod", null));
    }

    @Test
    @DisplayName("the author list is copied, so the caller cannot mutate it afterwards")
    void copiesAuthors() {
        List<String> authors = new ArrayList<>(List.of("D4EMON"));
        ModInfo info = new ModInfo("example-mod", VERSION, null, null, authors, null);

        authors.add("someone else");

        assertEquals(List.of("D4EMON"), info.authors());
        assertThrows(UnsupportedOperationException.class, () -> info.authors().add("nor this way"));
    }
}
