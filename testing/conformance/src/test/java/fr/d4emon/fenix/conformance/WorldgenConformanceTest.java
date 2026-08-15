package fr.d4emon.fenix.conformance;

import fr.d4emon.fenix.loader.launch.Launch;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that the worldgen files Ember writes are files the game accepts.
 *
 * <p>Worldgen is the one part of a mod that is pure data, and data is where a
 * generator's mistakes hide: a misspelled field or a renamed enum fails no
 * build, fails no startup and logs nothing — the entry is dropped and the ore
 * is never anywhere. The player reports bad luck, and the author looks at their
 * spawn weights.
 *
 * <p>So the files are parsed with Minecraft's own codecs, which is both the
 * only honest check and the only thing that would notice the format changing
 * in a game update.
 */
class WorldgenConformanceTest {

    /** A real launch keeps its classloader open for the life of the process. */
    @TempDir(cleanup = CleanupMode.NEVER)
    Path gameDir;

    @Test
    @DisplayName("the ore files Ember wrote parse with Minecraft's own codecs")
    void generatedWorldgenParses() throws Exception {
        Path clientJar = requiredFile("fenix.test.clientJar");
        Path worldgen = Path.of(requiredProperty("fenix.test.worldgenDir"));

        Path configured = worldgen.resolve("configured_feature").resolve("ruby_ore.json");
        Path placed = worldgen.resolve("placed_feature").resolve("ruby_ore.json");
        assertTrue(Files.isRegularFile(configured), configured + " — run :example-mod:ember");
        assertTrue(Files.isRegularFile(placed), placed + " — run :example-mod:ember");

        Files.createDirectories(gameDir.resolve("mods"));

        assertDoesNotThrow(() -> Launch.run(new String[] {
                "--fenix.gameJar", clientJar.toAbsolutePath().toString(),
                "--fenix.gameMain", "fr.d4emon.fenix.probe.WorldgenProbe",
                "--fenix.gameDir", gameDir.toAbsolutePath().toString(),
                configured.toAbsolutePath().toString(),
                placed.toAbsolutePath().toString(),
        }), "the probe reports a failed check by throwing");
    }

    @Test
    @DisplayName("every data file Ember wrote parses with Minecraft's own codec")
    void generatedLootTablesParse() throws Exception {
        Path clientJar = requiredFile("fenix.test.clientJar");
        Path tables = Path.of(requiredProperty("fenix.test.exampleGenerated"))
                .resolve("data/example-mod/loot_table");
        assertTrue(Files.isDirectory(tables), tables + " — run :example-mod:ember");

        // Advancements go to the same probe: both are datapack JSON whose
        // mistakes are dropped entries rather than failures, and booting the
        // game twice to check two directories would cost a minute for nothing.
        Path advancements = Path.of(requiredProperty("fenix.test.exampleGenerated"))
                .resolve("data/example-mod/advancement");
        assertTrue(Files.isDirectory(advancements),
                advancements + " — run :example-mod:ember");

        Path damageTypes = Path.of(requiredProperty("fenix.test.exampleGenerated"))
                .resolve("data/example-mod/damage_type");
        assertTrue(Files.isDirectory(damageTypes),
                damageTypes + " — run :example-mod:ember");

        Path enchantments = Path.of(requiredProperty("fenix.test.exampleGenerated"))
                .resolve("data/example-mod/enchantment");
        assertTrue(Files.isDirectory(enchantments),
                enchantments + " — run :example-mod:ember");

        Path generated = Path.of(requiredProperty("fenix.test.exampleGenerated"));
        Path trades = generated.resolve("data/example-mod/villager_trade");
        Path tradeSets = generated.resolve("data/example-mod/trade_set");
        assertTrue(Files.isDirectory(trades), trades + " — run :example-mod:ember");
        assertTrue(Files.isDirectory(tradeSets), tradeSets + " — run :example-mod:ember");

        Files.createDirectories(gameDir.resolve("mods"));

        // A loot table is pure data, and the mistakes it can carry are the kind
        // that drop the entry rather than fail: a block that quietly drops
        // nothing, which reads as a missing table rather than a malformed one.
        assertDoesNotThrow(() -> Launch.run(new String[] {
                "--fenix.gameJar", clientJar.toAbsolutePath().toString(),
                "--fenix.gameMain", "fr.d4emon.fenix.probe.LootTableFilesProbe",
                "--fenix.gameDir", gameDir.toAbsolutePath().toString(),
                tables.toAbsolutePath().toString(),
                advancements.toAbsolutePath().toString(),
                damageTypes.toAbsolutePath().toString(),
                enchantments.toAbsolutePath().toString(),
                trades.toAbsolutePath().toString(),
                tradeSets.toAbsolutePath().toString(),
                generated.resolve("data/example-mod").toAbsolutePath().toString(),
        }), "the probe reports a failed check by throwing");
    }

    private static String requiredProperty(String name) {
        String value = System.getProperty(name);
        assertNotNull(value, "the build must set -D" + name);
        return value;
    }

    private static Path requiredFile(String property) {
        Path path = Path.of(requiredProperty(property));
        assertTrue(Files.isRegularFile(path), path + " does not exist");
        return path;
    }
}
