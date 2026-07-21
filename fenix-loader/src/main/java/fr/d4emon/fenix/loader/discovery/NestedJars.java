package fr.d4emon.fenix.loader.discovery;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Unpacks the jars a mod carries inside itself.
 *
 * <p>A mod that is really several — the Fenix API is four, and will be more —
 * would otherwise ask a player to drop four files in {@code mods} and keep
 * their versions in step by hand. Carrying them inside one jar makes that the
 * build's problem rather than theirs.
 *
 * <p>Anything under {@code META-INF/jars/} is taken, with no manifest listing
 * to keep in sync: a list that can disagree with the jar is a list that
 * eventually does, and the directory is already the truth.
 *
 * <p>Unpacked rather than read in place because a nested jar has to become a
 * real file for the classloader to open, and because a directory of extracted
 * jars is something a person can look at when a mod does not load.
 */
final class NestedJars {

    private static final String PREFIX = "META-INF/jars/";

    private NestedJars() {
    }

    /**
     * {@return the jars unpacked out of a container, in the order it holds them}
     *
     * @param container where to look
     * @param into      the directory to unpack into
     * @param problems  collects anything that could not be unpacked
     */
    static List<Path> unpack(Path container, Path into, List<String> problems) {
        String containerName = container.getFileName().toString();
        List<Path> unpacked = new ArrayList<>();

        try (JarFile jar = new JarFile(container.toFile())) {
            // Named after the container, so two mods carrying a jar of the same
            // name cannot overwrite each other's copy.
            Path target = into.resolve(stripExtension(containerName));

            for (Enumeration<JarEntry> entries = jar.entries(); entries.hasMoreElements(); ) {
                JarEntry entry = entries.nextElement();
                if (!isNestedJar(entry)) {
                    continue;
                }
                Path file = target.resolve(fileName(entry));
                if (!isCurrent(file, entry)) {
                    Files.createDirectories(target);
                    write(jar, entry, file);
                }
                unpacked.add(file);
            }
        } catch (IOException e) {
            problems.add(containerName + ": its nested jars cannot be unpacked: " + e.getMessage());
        }
        return unpacked;
    }

    private static boolean isNestedJar(JarEntry entry) {
        String name = entry.getName();
        return !entry.isDirectory()
                && name.startsWith(PREFIX)
                && name.lastIndexOf('/') == PREFIX.length() - 1
                && name.toLowerCase(Locale.ROOT).endsWith(".jar");
    }

    /**
     * An unpacked jar is reused when it is the same size as the one inside.
     *
     * <p>Not a hash: this runs on every launch, for every mod, and the cost of
     * reading two jars in full to compare them would be paid by every player on
     * every start. Size catches a version change, which is the case that
     * matters — the same bytes at the same size are the same jar.
     */
    private static boolean isCurrent(Path file, JarEntry entry) throws IOException {
        return Files.isRegularFile(file) && Files.size(file) == entry.getSize();
    }

    private static void write(JarFile jar, JarEntry entry, Path file) throws IOException {
        // Through a temporary name, so a launch interrupted mid-copy cannot
        // leave a truncated jar that every later launch then trusts.
        Path partial = file.resolveSibling(file.getFileName() + ".partial");
        try (InputStream in = jar.getInputStream(entry)) {
            Files.copy(in, partial, StandardCopyOption.REPLACE_EXISTING);
        }
        Files.move(partial, file, StandardCopyOption.REPLACE_EXISTING);
    }

    private static String fileName(JarEntry entry) {
        return entry.getName().substring(PREFIX.length());
    }

    private static String stripExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? fileName : fileName.substring(0, dot);
    }
}
