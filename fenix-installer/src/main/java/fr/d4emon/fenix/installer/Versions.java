package fr.d4emon.fenix.installer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Which versions can actually be installed into, and with what.
 *
 * <p>Typing a version by hand was the installer's one remaining way to fail:
 * a name that does not match a folder produces an error the player then has to
 * go and check. Offering only what is there removes the question instead of
 * answering it.
 */
final class Versions {

    private Versions() {
    }

    /**
     * {@return the game versions present in a folder <em>and</em> supported,
     * newest first}
     *
     * <p>A version counts as present when the launcher has written its
     * manifest, which it does the first time the version is actually run. That
     * is the same condition the install itself needs, so anything offered here
     * will work.
     *
     * @param minecraftDir the {@code .minecraft} folder
     * @param supported    what this installer was built for
     */
    static List<String> installable(Path minecraftDir, List<String> supported) {
        Path versions = minecraftDir.resolve("versions");
        if (!Files.isDirectory(versions)) {
            return List.of();
        }
        List<String> present = new ArrayList<>();
        try (Stream<Path> entries = Files.list(versions)) {
            entries.filter(Files::isDirectory).forEach(directory -> {
                String name = directory.getFileName().toString();
                if (Files.isRegularFile(directory.resolve(name + ".json"))) {
                    present.add(name);
                }
            });
        } catch (IOException e) {
            // An unreadable folder is a folder with nothing to offer, and the
            // install itself will say so in terms the player can act on.
            return List.of();
        }
        present.retainAll(supported);
        present.sort(Versions::newestFirst);
        return List.copyOf(present);
    }

    /**
     * {@return the Fenix versions installable for a game version, newest first}
     *
     * <p>One, today, and honestly so: the loader, the API core and Mixin all
     * travel inside this installer, so the only version it can lay down is the
     * one it is carrying. The shape is a list because that is what it will
     * become once an installer can fetch what it does not carry.
     *
     * @param minecraftVersion the game version
     * @param fenixVersion     what this installer carries
     * @param supported        the game versions it carries it for
     */
    static List<String> forMinecraft(String minecraftVersion, String fenixVersion,
                                     List<String> supported) {
        return supported.contains(minecraftVersion) ? List.of(fenixVersion) : List.of();
    }

    /**
     * Compares two version names so the newest sorts first.
     *
     * <p>Numeric segments compare as numbers: otherwise "26.10" sorts before
     * "26.2", which is the sort of thing a player notices immediately and
     * trusts the installer less for.
     */
    private static int newestFirst(String left, String right) {
        String[] a = left.split("[._-]");
        String[] b = right.split("[._-]");
        for (int i = 0; i < Math.max(a.length, b.length); i++) {
            String x = i < a.length ? a[i] : "";
            String y = i < b.length ? b[i] : "";
            int comparison = numeric(x) && numeric(y)
                    ? Long.compare(Long.parseLong(y), Long.parseLong(x))
                    : y.compareTo(x);
            if (comparison != 0) {
                return comparison;
            }
        }
        return 0;
    }

    private static boolean numeric(String part) {
        return !part.isEmpty() && part.chars().allMatch(Character::isDigit);
    }
}
