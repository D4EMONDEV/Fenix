package fr.d4emon.fenix.conformance;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that everything the demo registers can actually be found in game.
 *
 * <p>Content in no creative tab is the quietest failure this repository has.
 * The block registers, its model loads, its loot table works, every other
 * conformance check passes, and the only symptom is that a player opening the
 * tab cannot see it. Nothing logs. Nothing throws. It looks exactly like a
 * block that was never added.
 *
 * <p>It has already happened twice — the logs and ores, then the nine cut
 * shapes — because adding content and listing it are two edits and only the
 * first one is load-bearing. So this reads the declarations rather than
 * trusting them: every {@code Holder<Block>} and {@code Holder<Item>} the demo
 * declares has to appear in the demo's own tab.
 *
 * <p>Source text, not a running game, because the failure is an omission in
 * source and the fix is in source. Booting the game would prove the same thing
 * far more slowly, and only for whatever the boot happened to reach.
 */
class CreativeTabConformanceTest {

    /** Where the demo keeps its content. */
    private static final List<String> SOURCES =
            List.of("ModBlocks.java", "ModItems.java", "ModContent.java");

    /** {@code public static final Holder<Block> RUBY_BLOCK = …} */
    private static final Pattern DECLARATION =
            Pattern.compile("Holder<(?:Block|Item)>\\s+([A-Z][A-Z0-9_]*)\\s*=");

    @Test
    @DisplayName("every block and item the demo declares is in the demo's tab")
    void everythingIsReachable() throws IOException {
        Path content = source("ModContent.java");
        String modContent = Files.readString(content, StandardCharsets.UTF_8);

        Set<String> declared = new LinkedHashSet<>();
        for (String file : SOURCES) {
            Matcher matcher = DECLARATION.matcher(Files.readString(source(file), StandardCharsets.UTF_8));
            while (matcher.find()) {
                declared.add(matcher.group(1));
            }
        }
        assertTrue(declared.size() > 10,
                "found only " + declared.size() + " declarations; the pattern has stopped "
                        + "matching how the demo declares content, so this check proves nothing");

        String tab = ownTabCall(modContent);
        List<String> missing = new ArrayList<>();
        for (String name : declared) {
            // Word boundaries: RUBY_LOG must not be satisfied by STRIPPED_RUBY_LOG,
            // which is exactly the pair that would hide a real omission.
            if (!Pattern.compile("\\b" + name + "\\b").matcher(tab).find()) {
                missing.add(name);
            }
        }

        assertTrue(missing.isEmpty(),
                "declared but in no creative tab, so unreachable except by /give: "
                        + String.join(", ", missing));
    }

    /**
     * The argument list of the {@code CreativeTabs.addTo(TAB, …)} call.
     *
     * <p>Read by counting brackets rather than by regex, because the call spans
     * a dozen lines and holds calls of its own.
     */
    private static String ownTabCall(String text) {
        int start = text.indexOf("CreativeTabs.addTo(TAB");
        assertTrue(start >= 0, "the demo should add its content to its own tab");

        int open = text.indexOf('(', start);
        int depth = 0;
        for (int i = open; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '(') {
                depth++;
            } else if (c == ')' && --depth == 0) {
                return text.substring(open + 1, i);
            }
        }
        throw new AssertionError("the addTo(TAB, …) call is never closed");
    }

    private static Path source(String name) {
        Path path = Path.of("..", "..", "examples", "example-mod", "src", "main", "java", "fr",
                "d4emon", "fenix", "example", "registry", name).toAbsolutePath().normalize();
        assertTrue(Files.isRegularFile(path), "the demo source should be readable at " + path);
        return path;
    }
}
