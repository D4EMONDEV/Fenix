package fr.d4emon.fenix.gradle;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;

/**
 * Unpacks Mojang's server bundler jar into a plain server jar and its libraries.
 *
 * <p>The bundle's layout, stable since 1.18:
 *
 * <ul>
 * <li>{@code META-INF/main-class} — one line, the real server main class;</li>
 * <li>{@code META-INF/versions.list} and {@code META-INF/libraries.list} —
 *     tab-separated {@code <sha256>\t<id>\t<path>} rows;</li>
 * <li>the payloads themselves under {@code META-INF/versions/} and
 *     {@code META-INF/libraries/}, keyed by the {@code <path>} column.</li>
 * </ul>
 *
 * <p>Extraction is idempotent: a file already present is left as is, so a second
 * {@code runServer} does no work.
 */
final class ServerUnbundler {

    private final Path outputDir;

    ServerUnbundler(Path outputDir) {
        this.outputDir = outputDir;
    }

    MinecraftServer unbundle(Path bundle) throws IOException {
        Files.createDirectories(outputDir);

        try (JarFile jar = new JarFile(bundle.toFile())) {
            String mainClass = readLine(jar, "META-INF/main-class");

            Path serverJar = extractSingle(jar, "versions", readList(jar, "META-INF/versions.list"));
            List<Path> libraries = new ArrayList<>();
            Path libraryDir = Files.createDirectories(outputDir.resolve("libraries"));
            for (String[] row : readList(jar, "META-INF/libraries.list")) {
                libraries.add(extract(jar, "META-INF/libraries/" + row[2], libraryDir.resolve(row[2])));
            }
            return new MinecraftServer(serverJar, libraries, mainClass);
        }
    }

    /** The versions list has exactly one row — the server jar. */
    private Path extractSingle(JarFile jar, String area, List<String[]> rows) throws IOException {
        if (rows.size() != 1) {
            throw new IOException("expected exactly one " + area + " entry in the server bundle, found "
                    + rows.size());
        }
        String path = rows.get(0)[2];
        Path target = outputDir.resolve(path.substring(path.lastIndexOf('/') + 1));
        return extract(jar, "META-INF/" + area + "/" + path, target);
    }

    private static Path extract(JarFile jar, String entry, Path target) throws IOException {
        if (Files.isRegularFile(target)) {
            return target;
        }
        var jarEntry = jar.getJarEntry(entry);
        if (jarEntry == null) {
            throw new IOException("the server bundle is missing " + entry);
        }
        Files.createDirectories(target.getParent());
        try (InputStream in = jar.getInputStream(jarEntry)) {
            Files.copy(in, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
        return target;
    }

    private static String readLine(JarFile jar, String entry) throws IOException {
        return read(jar, entry).strip();
    }

    private static List<String[]> readList(JarFile jar, String entry) throws IOException {
        List<String[]> rows = new ArrayList<>();
        for (String line : read(jar, entry).split("\n")) {
            if (!line.isBlank()) {
                rows.add(line.strip().split("\t"));
            }
        }
        return rows;
    }

    private static String read(JarFile jar, String entry) throws IOException {
        var jarEntry = jar.getJarEntry(entry);
        if (jarEntry == null) {
            throw new IOException("not a server bundle: missing " + entry);
        }
        try (InputStream in = jar.getInputStream(jarEntry)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
