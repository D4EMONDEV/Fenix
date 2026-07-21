package fr.d4emon.fenix.config;

import fr.d4emon.fenix.api.Fenix;
import fr.d4emon.fenix.api.log.FenixLogger;
import fr.d4emon.fenix.api.ModInfo;
import fr.d4emon.fenix.api.Side;
import fr.d4emon.fenix.api.Version;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The file half: reading it, writing it back, and saying what was ignored. */
class ConfigTest {

    record Settings(boolean enabled, int count) {
    }

    private static final Settings DEFAULTS = new Settings(true, 20);

    @TempDir
    Path configDir;

    private final List<String> warnings = new ArrayList<>();
    private Fenix fenix;

    @BeforeEach
    void setUp() {
        fenix = new StubFenix(configDir, warnings);
    }

    private Path file() {
        return configDir.resolve("config.json");
    }

    @Test
    @DisplayName("a first launch writes the file, filled in")
    void firstLaunchWritesTheFile() throws IOException {
        Config<Settings> config = Config.of(fenix, DEFAULTS);

        assertEquals(DEFAULTS, config.get());
        assertTrue(Files.isRegularFile(file()), "the file should have been created");
        // Written out rather than left empty, so a player has something to edit
        // instead of having to guess the shape from documentation.
        String written = Files.readString(file());
        assertTrue(written.contains("\"enabled\""), written);
        assertTrue(written.contains("\"count\""), written);
    }

    @Test
    @DisplayName("what the player wrote wins, and the rest keeps its default")
    void readsWhatIsThere() throws IOException {
        Files.writeString(file(), """
                { "count": 5 }
                """);

        Settings settings = Config.of(fenix, DEFAULTS).get();

        assertEquals(5, settings.count());
        assertEquals(true, settings.enabled());
    }

    @Test
    @DisplayName("a setting added by an update appears in the file on next launch")
    void addedSettingBecomesVisible() throws IOException {
        Files.writeString(file(), """
                { "enabled": false }
                """);

        Config.of(fenix, DEFAULTS);

        // The point of rewriting after every load: otherwise the new setting
        // exists, has a value, and the player has no way of knowing either.
        assertTrue(Files.readString(file()).contains("\"count\""), Files.readString(file()));
    }

    @Test
    @DisplayName("an unknown key is warned about by name")
    void unknownKeyIsWarned() throws IOException {
        Files.writeString(file(), """
                { "enabledd": false }
                """);

        Config.of(fenix, DEFAULTS);

        assertEquals(1, warnings.size(), warnings.toString());
        assertTrue(warnings.getFirst().contains("enabledd"), warnings.toString());
    }

    @Test
    @DisplayName("a file that is not JSON says so, naming the file")
    void brokenFileIsReported() throws IOException {
        Files.writeString(file(), "{ not json");

        ConfigException thrown =
                assertThrows(ConfigException.class, () -> Config.of(fenix, DEFAULTS));

        assertTrue(thrown.getMessage().contains("config.json"), thrown.getMessage());
    }

    @Test
    @DisplayName("reload picks up an edit made while the game was running")
    void reloadRereads() throws IOException {
        Config<Settings> config = Config.of(fenix, DEFAULTS);
        Files.writeString(file(), """
                { "count": 77 }
                """);

        config.reload();

        assertEquals(77, config.get().count());
    }

    @Test
    @DisplayName("nothing is left behind when a write finishes")
    void noPartialFileSurvives() throws IOException {
        Config.of(fenix, DEFAULTS);

        // The write goes through a temporary name so an interrupted one cannot
        // leave a truncated file that the next launch refuses.
        try (var entries = Files.list(configDir)) {
            assertEquals(List.of("config.json"),
                    entries.map(path -> path.getFileName().toString()).sorted().toList());
        }
    }

    /** Just enough of a context to point at a directory and collect warnings. */
    private record StubFenix(Path configDir, List<String> warnings) implements Fenix {

        @Override
        public ModInfo mod() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Collection<ModInfo> mods() {
            return List.of();
        }

        @Override
        public Optional<ModInfo> findMod(String id) {
            return Optional.empty();
        }

        @Override
        public Version loaderVersion() {
            return Version.parse("0.1.0");
        }

        @Override
        public Side side() {
            return Side.SERVER;
        }

        @Override
        public Path gameDir() {
            return configDir;
        }

        @Override
        public FenixLogger logger() {
            return new FenixLogger() {
                @Override
                public void trace(String message, Object... arguments) {
                }

                @Override
                public void debug(String message, Object... arguments) {
                }

                @Override
                public void info(String message, Object... arguments) {
                }

                @Override
                public void warn(String message, Object... arguments) {
                    warnings.add(message + " " + List.of(arguments));
                }

                @Override
                public void error(String message, Object... arguments) {
                }
            };
        }
    }
}
