package fr.d4emon.fenix.conformance;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that Fenix's log messages survive the console they land in.
 *
 * <p>The loader logs through {@code System.out} before anything better exists,
 * and on Windows that stream carries the console's own code page. An em dash
 * is not in cp850, so it does not arrive mangled — it does not arrive at all,
 * and the encoder puts a replacement character where the character was. The
 * very first line Fenix prints read {@code Fenix Loader 0.1.1 <?> client side}.
 *
 * <p>Nothing can be done about the console. What can be done is not to depend
 * on it: a message written in ASCII prints identically everywhere, and Fenix's
 * logs are the one place where prose is worth giving up for that.
 *
 * <p>Only the message literals. Comments, docs and translations are read by
 * people through tools that handle UTF-8, and are left in proper English.
 */
class LogTextConformanceTest {

    /** The first string argument of any log call. */
    private static final Pattern LOG_CALL = Pattern.compile(
            "\\.(?:trace|debug|info|warn|error)\\s*\\(\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");

    /** Every module that logs. */
    private static final List<String> MODULES = List.of(
            "fenix-loader", "fenix-api", "ember", "fenix-installer",
            "examples", "testing/demo-mod");

    @Test
    @DisplayName("every log message is ASCII, so every console can print it")
    void logMessagesArePrintable() throws IOException {
        List<String> problems = new ArrayList<>();
        int scanned = 0;

        for (String module : MODULES) {
            Path root = Path.of("..", "..", module).toAbsolutePath().normalize();
            assertTrue(Files.isDirectory(root), "expected a module at " + root);

            try (Stream<Path> files = Files.walk(root)) {
                for (Path file : files.filter(LogTextConformanceTest::isSource).toList()) {
                    String text = Files.readString(file, StandardCharsets.UTF_8);
                    Matcher matcher = LOG_CALL.matcher(text);
                    while (matcher.find()) {
                        scanned++;
                        String message = matcher.group(1);
                        for (int i = 0; i < message.length(); i++) {
                            char c = message.charAt(i);
                            if (c > 127) {
                                problems.add(root.relativize(file) + ": U+"
                                        + String.format("%04X", (int) c) + " in \"" + message + "\"");
                                break;
                            }
                        }
                    }
                }
            }
        }

        assertTrue(scanned > 20,
                "found only " + scanned + " log calls; the pattern has stopped matching how "
                        + "Fenix logs, so this check proves nothing");
        assertTrue(problems.isEmpty(),
                "log messages a cp850 console turns into replacement characters:\n  "
                        + String.join("\n  ", problems));
    }

    private static boolean isSource(Path path) {
        // build/ holds copies of the same files, which would report twice.
        return path.toString().endsWith(".java")
                && !path.toString().replace('\\', '/').contains("/build/");
    }
}
