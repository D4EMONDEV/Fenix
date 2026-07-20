package fr.d4emon.fenix.loader.launch;

import fr.d4emon.fenix.api.Side;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LaunchOptionsTest {

    @Test
    void appliesDefaults() {
        Launch.Options options = Launch.Options.parse(new String[0]);

        assertNull(options.gameMain());
        assertNull(options.gameJar());
        assertNull(options.side());
        assertFalse(options.dryRun());
        assertEquals(Path.of("."), options.gameDir());
        assertEquals(Path.of(".").resolve("mods"), options.modsDir());
        assertArrayEquals(new String[0], options.gameArgs());
    }

    @Test
    void readsEveryFenixOption() {
        Launch.Options options = Launch.Options.parse(new String[] {
                "--fenix.gameMain", "com.example.Game",
                "--fenix.gameJar", "game.jar",
                "--fenix.gameDir", "run",
                "--fenix.mods", "elsewhere/mods",
                "--fenix.side", "server",
                "--fenix.dryRun",
        });

        assertEquals("com.example.Game", options.gameMain());
        assertEquals(Path.of("game.jar"), options.gameJar());
        assertEquals(Path.of("run"), options.gameDir());
        assertEquals(Path.of("elsewhere/mods"), options.modsDir());
        assertEquals(Side.SERVER, options.side());
        assertTrue(options.dryRun());
        assertArrayEquals(new String[0], options.gameArgs());
    }

    @Test
    @DisplayName("everything the launcher passes goes to the game, order intact")
    void forwardsVanillaArgumentsUntouched() {
        String[] vanilla = {
                "--username", "Player", "--version", "fenix-0.1.0-26.2",
                "--gameDir", "C:/mc", "--assetsDir", "assets", "--accessToken", "token",
        };

        Launch.Options options = Launch.Options.parse(vanilla);

        assertArrayEquals(vanilla, options.gameArgs());
    }

    @Test
    @DisplayName("the vanilla --gameDir is peeked, so loader and game agree on it")
    void peeksTheVanillaGameDir() {
        Launch.Options options = Launch.Options.parse(new String[] {
                "--username", "Player", "--gameDir", "C:/mc",
        });

        assertEquals(Path.of("C:/mc"), options.gameDir());
        assertEquals(Path.of("C:/mc").resolve("mods"), options.modsDir());
        // Still forwarded: the game needs it too.
        assertArrayEquals(new String[] {"--username", "Player", "--gameDir", "C:/mc"}, options.gameArgs());
    }

    @Test
    @DisplayName("--fenix.gameDir wins over the peeked vanilla value")
    void explicitGameDirBeatsThePeek() {
        Launch.Options options = Launch.Options.parse(new String[] {
                "--gameDir", "C:/mc", "--fenix.gameDir", "C:/elsewhere",
        });

        assertEquals(Path.of("C:/elsewhere"), options.gameDir());
    }

    @Test
    void rejectsBadFenixInput() {
        assertTrue(assertThrows(LaunchException.class, () -> Launch.Options.parse(
                new String[] {"--fenix.sideways"}))
                .getMessage().contains("unknown option"));

        assertTrue(assertThrows(LaunchException.class, () -> Launch.Options.parse(
                new String[] {"--fenix.gameMain"}))
                .getMessage().contains("needs a value"));

        assertTrue(assertThrows(LaunchException.class, () -> Launch.Options.parse(
                new String[] {"--fenix.side", "everywhere"}))
                .getMessage().contains("--fenix.side"));
    }
}
