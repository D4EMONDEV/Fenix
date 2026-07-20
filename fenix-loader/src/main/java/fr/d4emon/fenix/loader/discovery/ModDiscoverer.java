package fr.d4emon.fenix.loader.discovery;

import fr.d4emon.fenix.loader.metadata.InvalidMetadataException;
import fr.d4emon.fenix.loader.metadata.ModMetadata;
import fr.d4emon.fenix.loader.metadata.ModMetadataReader;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

/**
 * Finds mods on disk.
 *
 * <p>A scan looks at the top level of the mods directory only, and considers
 * every {@code .jar} file there. Renaming a jar to {@code .jar.disabled} is
 * therefore enough to keep it out of a launch, which players already expect
 * from other loaders.
 *
 * <p>One bad jar never hides the others: every failure becomes an entry in
 * {@link DiscoveryResult#problems()} and the scan carries on. A jar with no
 * {@code fenix.mod.json} at all is reported too, because a file in the mods
 * directory that is not a Fenix mod is almost always a mod for a different
 * loader — silence would leave the player wondering why nothing happened.
 */
public final class ModDiscoverer {

    private ModDiscoverer() {
    }

    /**
     * Scans a mods directory.
     *
     * <p>Candidates are returned in file-name order, compared case-insensitively
     * so the result does not depend on the file system. Load order is decided
     * later, by resolution — this order only makes logs and error lists stable.
     *
     * @param modsDirectory the directory to scan; a missing directory is an empty
     *                      result, not an error, because that is what a first
     *                      launch looks like
     * @return what was found, and what could not be read
     * @throws NullPointerException if the directory is {@code null}
     */
    public static DiscoveryResult scan(Path modsDirectory) {
        Objects.requireNonNull(modsDirectory, "modsDirectory");

        if (!Files.exists(modsDirectory)) {
            return new DiscoveryResult(List.of(), List.of());
        }
        if (!Files.isDirectory(modsDirectory)) {
            return new DiscoveryResult(List.of(),
                    List.of(modsDirectory + " is not a directory"));
        }

        List<Path> jars;
        try (Stream<Path> entries = Files.list(modsDirectory)) {
            jars = entries
                    .filter(ModDiscoverer::isJar)
                    .sorted(Comparator.comparing(path -> path.getFileName().toString().toLowerCase(Locale.ROOT)))
                    .toList();
        } catch (IOException e) {
            return new DiscoveryResult(List.of(),
                    List.of("the mods directory " + modsDirectory + " cannot be listed: " + e.getMessage()));
        }

        List<ModCandidate> mods = new ArrayList<>();
        List<String> problems = new ArrayList<>();
        for (Path jar : jars) {
            readJar(jar, mods, problems);
        }
        return new DiscoveryResult(mods, problems);
    }

    private static boolean isJar(Path path) {
        return Files.isRegularFile(path)
                && path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".jar");
    }

    private static void readJar(Path path, List<ModCandidate> mods, List<String> problems) {
        String fileName = path.getFileName().toString();

        try (JarFile jar = new JarFile(path.toFile())) {
            JarEntry entry = jar.getJarEntry(ModMetadataReader.FILE_NAME);
            if (entry == null) {
                problems.add(fileName + ": contains no " + ModMetadataReader.FILE_NAME
                        + " — is it a mod for a different loader?");
                return;
            }

            ModMetadata metadata;
            try (Reader reader = new InputStreamReader(jar.getInputStream(entry), StandardCharsets.UTF_8)) {
                metadata = ModMetadataReader.read(reader, fileName);
            }
            mods.add(new ModCandidate(metadata, path));
        } catch (InvalidMetadataException e) {
            // Already prefixed with the file name.
            problems.add(e.getMessage());
        } catch (IOException e) {
            problems.add(fileName + ": cannot be opened as a jar: " + e.getMessage());
        }
    }
}
