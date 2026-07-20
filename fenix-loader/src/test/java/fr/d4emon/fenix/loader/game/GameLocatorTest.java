package fr.d4emon.fenix.loader.game;

import fr.d4emon.fenix.api.Side;
import fr.d4emon.fenix.api.Version;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameLocatorTest {

    @TempDir
    Path tempDir;

    /** Entry existence is what detection reads, so dummy bytes are enough. */
    private Path writeJar(String name, Map<String, String> entries) throws IOException {
        Path jar = tempDir.resolve(name);
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(jar))) {
            for (Map.Entry<String, String> entry : entries.entrySet()) {
                out.putNextEntry(new ZipEntry(entry.getKey()));
                out.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                out.closeEntry();
            }
        }
        return jar;
    }

    private Path clientJar() throws IOException {
        return writeJar("client.jar", Map.of(
                "net/minecraft/client/main/Main.class", "dummy",
                "net/minecraft/server/Main.class", "dummy",
                "version.json", """
                        {"id": "26.2", "name": "26.2", "world_version": 4903}
                        """));
    }

    @Test
    @DisplayName("a client jar is recognised, with its version read from version.json")
    void recognisesAClientJar() throws IOException {
        Optional<GameLocator.Game> game = GameLocator.inspect(clientJar());

        assertTrue(game.isPresent());
        assertEquals("net.minecraft.client.main.Main", game.get().mainClass());
        assertEquals(Side.CLIENT, game.get().side());
        assertEquals(Optional.of(Version.parse("26.2")), game.get().version());
    }

    @Test
    @DisplayName("the client jar contains server classes too, so client is checked first")
    void neverMistakesAClientJarForAServer() throws IOException {
        assertEquals(Side.CLIENT, GameLocator.inspect(clientJar()).orElseThrow().side());
    }

    @Test
    void recognisesADedicatedServerJar() throws IOException {
        Path jar = writeJar("server.jar", Map.of("net/minecraft/server/Main.class", "dummy"));

        GameLocator.Game game = GameLocator.inspect(jar).orElseThrow();

        assertEquals("net.minecraft.server.Main", game.mainClass());
        assertEquals(Side.SERVER, game.side());
        assertEquals(Optional.empty(), game.version());
    }

    @Test
    void ignoresJarsThatAreNotTheGame() throws IOException {
        Path jar = writeJar("library.jar", Map.of("com/example/Library.class", "dummy"));

        assertEquals(Optional.empty(), GameLocator.inspect(jar));
    }

    @Test
    @DisplayName("a broken version.json degrades to 'version unknown', not a refusal")
    void toleratesABrokenVersionFile() throws IOException {
        Path jar = writeJar("odd-client.jar", Map.of(
                "net/minecraft/client/main/Main.class", "dummy",
                "version.json", "{ not json"));

        GameLocator.Game game = GameLocator.inspect(jar).orElseThrow();

        assertEquals(Optional.empty(), game.version());
        assertEquals(Side.CLIENT, game.side());
    }

    @Test
    @DisplayName("classpath scanning skips junk and finds the game among libraries")
    void locatesTheGameOnAClasspath() throws IOException {
        Path library = writeJar("gson.jar", Map.of("com/google/gson/Gson.class", "dummy"));
        Path game = clientJar();
        String classpath = String.join(File.pathSeparator,
                "does-not-exist.jar",
                tempDir.resolve("a-directory").toString(),
                library.toString(),
                game.toString());

        Optional<GameLocator.Game> located = GameLocator.locate(classpath);

        assertEquals(Optional.of(game), located.map(GameLocator.Game::jar));
    }

    @Test
    void anEmptyClasspathHasNoGame() {
        assertEquals(Optional.empty(), GameLocator.locate(""));
    }
}
