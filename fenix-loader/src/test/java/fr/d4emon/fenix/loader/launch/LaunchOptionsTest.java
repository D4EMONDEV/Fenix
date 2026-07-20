package fr.d4emon.fenix.loader.launch;

import fr.d4emon.fenix.api.Side;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LaunchOptionsTest {

    @Test
    void appliesDefaults() {
        Launch.Options options = Launch.Options.parse(new String[] {"--gameMain", "com.example.Game"});

        assertEquals("com.example.Game", options.gameMain());
        assertNull(options.gameJar());
        assertEquals(Path.of("."), options.gameDir());
        assertEquals(Path.of(".").resolve("mods"), options.modsDir());
        assertEquals(Side.CLIENT, options.side());
        assertArrayEquals(new String[0], options.gameArgs());
    }

    @Test
    void readsEveryOption() {
        Launch.Options options = Launch.Options.parse(new String[] {
                "--gameMain", "com.example.Game",
                "--gameJar", "game.jar",
                "--gameDir", "run",
                "--mods", "elsewhere/mods",
                "--side", "server",
                "--", "--gameFlag", "value",
        });

        assertEquals(Path.of("game.jar"), options.gameJar());
        assertEquals(Path.of("run"), options.gameDir());
        assertEquals(Path.of("elsewhere/mods"), options.modsDir());
        assertEquals(Side.SERVER, options.side());
        assertArrayEquals(new String[] {"--gameFlag", "value"}, options.gameArgs());
    }

    @Test
    @DisplayName("the mods directory defaults to <gameDir>/mods, not <cwd>/mods")
    void modsDefaultFollowsGameDir() {
        Launch.Options options = Launch.Options.parse(new String[] {
                "--gameMain", "com.example.Game", "--gameDir", "run"});

        assertEquals(Path.of("run").resolve("mods"), options.modsDir());
    }

    @Test
    void rejectsBadInput() {
        assertTrue(assertThrows(LaunchException.class, () -> Launch.Options.parse(new String[0]))
                .getMessage().contains("--gameMain is required"));

        assertTrue(assertThrows(LaunchException.class, () -> Launch.Options.parse(
                new String[] {"--gameMain", "X", "--sideways"}))
                .getMessage().contains("unknown option"));

        assertTrue(assertThrows(LaunchException.class, () -> Launch.Options.parse(
                new String[] {"--gameMain"}))
                .getMessage().contains("needs a value"));

        assertTrue(assertThrows(LaunchException.class, () -> Launch.Options.parse(
                new String[] {"--gameMain", "X", "--side", "everywhere"}))
                .getMessage().contains("--side"));
    }
}
