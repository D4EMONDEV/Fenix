package fr.d4emon.fenix.gradle;

import java.nio.file.Path;
import java.util.List;

/**
 * A dedicated server, un-bundled from Mojang's launcher jar.
 *
 * <p>Since 1.18 the server download is a <em>bundler</em>: a jar whose main
 * class extracts the real server and its libraries at runtime. Fenix has to
 * launch the real server through its own loader, so the bundle is unpacked
 * ahead of time into the cache.
 *
 * @param serverJar the extracted server jar, handed to the loader as the game
 * @param libraries the extracted server libraries, for the launch classpath
 * @param mainClass the server's real main class, from the bundle manifest
 */
public record MinecraftServer(Path serverJar, List<Path> libraries, String mainClass) {

    /**
     * Copies the library list so the record cannot change under its reader.
     */
    public MinecraftServer {
        libraries = List.copyOf(libraries);
    }
}
