package fr.d4emon.fenix.conformance;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that the demo still uses everything a mod can call.
 *
 * <p>example-mod is how this repository knows Fenix works: the conformance
 * probes prove pieces in isolation, and the demo is the only place the pieces
 * are used together, by hand, the way somebody would actually write a mod. An
 * API added without a use here is one nobody has ever called.
 *
 * <p>That is not hypothetical. Block entity rendering shipped, was documented,
 * had its own task marked done, and went four releases without the demo ever
 * registering one — so nothing would have noticed if it had never worked. The
 * same was true of level events and client block events.
 *
 * <p>What counts is a static entry point: a {@code public final class} with a
 * private constructor and public static members, which is the shape Fenix uses
 * everywhere a mod is meant to call in. Builders, functional interfaces and
 * value types are left out, because a mod uses those without naming them.
 */
class DemoCoverageConformanceTest {

    /**
     * Entry points a mod never calls, each for a reason.
     *
     * <p>Anything added here should be a class Fenix drives itself. If it is
     * one a mod would call, the demo wants a use rather than a line here.
     */
    private static final Set<String> INTERNAL = Set.of(
            // The event machinery. A mod registers on an Event; only the API
            // modules create them.
            "Event", "CancellableEvent",
            // The channel table, reached through ToClient and ToServer.
            "Channels", "ClientChannels",
            // Behind Attachments, which is the door a mod uses.
            "AttachmentStorage",
            // Built by Commands, handed to a mod as an argument.
            "FenixCommand",
            // Fenix's own: paging the creative menu, checking a connecting
            // player's registries, and warning about unreachable professions.
            "CreativePages", "RegistryCheck", "VillagerJobSites");

    @Test
    @DisplayName("every entry point a mod can call is called by the demo")
    void demoUsesTheWholeApi() throws IOException {
        String demo = read(Path.of("..", "..", "examples", "example-mod", "src"));
        assertTrue(demo.length() > 10_000, "the demo sources should be readable, got "
                + demo.length() + " characters");

        Set<String> entryPoints = new TreeSet<>();
        List<String> unused = new ArrayList<>();

        for (Path file : sources(Path.of("..", "..", "fenix-api"))) {
            String name = file.getFileName().toString().replace(".java", "");
            String text = Files.readString(file, StandardCharsets.UTF_8);

            boolean isEntryPoint =
                    find(text, "^public final class " + name + "\\b")
                            && find(text, "^\\s+private " + name + "\\(\\)")
                            && find(text, "^\\s+public static ");
            if (!isEntryPoint) {
                continue;
            }
            entryPoints.add(name);
            if (!INTERNAL.contains(name) && !find(demo, "\\b" + name + "\\b")) {
                unused.add(name);
            }
        }

        assertTrue(entryPoints.size() > 25,
                "found only " + entryPoints.size() + " entry points; the shape this looks for "
                        + "has changed, so the check proves nothing");
        assertTrue(unused.isEmpty(),
                "API a mod can call that the demo never calls, so nothing exercises it "
                        + "end to end:\n  " + String.join("\n  ", unused)
                        + "\nEither use it in example-mod, or say in INTERNAL why a mod would not.");
    }

    private static boolean find(String text, String regex) {
        return Pattern.compile(regex, Pattern.MULTILINE).matcher(text).find();
    }

    private static List<Path> sources(Path root) throws IOException {
        assertTrue(Files.isDirectory(root), "expected sources at " + root.toAbsolutePath());
        try (Stream<Path> files = Files.walk(root)) {
            return files.filter(path -> {
                String as = path.toString().replace('\\', '/');
                // Mixins are not API, and build/ is a second copy of everything.
                return as.endsWith(".java") && !as.contains("/build/")
                        && !as.contains("/mixin/") && !as.contains("/test/");
            }).toList();
        }
    }

    /**
     * Every source under a root, with the import lines removed.
     *
     * <p>Without that this check passes on an import alone. Deleting the one
     * call to {@code BlockEntityRendering} left the import behind, javac said
     * nothing because an unused import is legal, and the name was still in the
     * file — so the check went on reporting an API that nothing called.
     */
    private static String read(Path root) throws IOException {
        StringBuilder all = new StringBuilder();
        for (Path file : sources(root)) {
            all.append(Files.readString(file, StandardCharsets.UTF_8)
                    .replaceAll("(?m)^import .*$", "")).append('\n');
        }
        return all.toString();
    }
}
