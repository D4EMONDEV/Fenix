package fr.d4emon.fenix.loader.log;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsoleLoggerTest {

    private final ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errBytes = new ByteArrayOutputStream();

    private ConsoleLogger logger(boolean verbose) {
        return new ConsoleLogger("testmod",
                new PrintStream(outBytes, true, StandardCharsets.UTF_8),
                new PrintStream(errBytes, true, StandardCharsets.UTF_8),
                verbose);
    }

    private String out() {
        return outBytes.toString(StandardCharsets.UTF_8);
    }

    private String err() {
        return errBytes.toString(StandardCharsets.UTF_8);
    }

    @Test
    void substitutesPlaceholdersInOrder() {
        logger(false).info("loaded {} blocks in {} ms", 42, 7);

        assertEquals("[testmod/INFO] loaded 42 blocks in 7 ms", out().strip());
    }

    @Test
    @DisplayName("missing arguments leave their placeholders visible rather than crash")
    void toleratesMissingArguments() {
        logger(false).info("value is {}");

        assertEquals("[testmod/INFO] value is {}", out().strip());
    }

    @Test
    @DisplayName("a trailing throwable gets its stack trace")
    void printsATrailingThrowable() {
        logger(false).error("something broke in {}", "startup", new IllegalStateException("boom"));

        String error = err();
        assertTrue(error.contains("[testmod/ERROR] something broke in startup"), error);
        assertTrue(error.contains("IllegalStateException"), error);
        assertTrue(error.contains("boom"), error);
    }

    @Test
    @DisplayName("a throwable with its own placeholder is formatted, not traced")
    void aThrowableCanAlsoBeAnArgument() {
        logger(false).info("caught: {}", new IllegalStateException("expected"));

        String output = out();
        assertTrue(output.contains("caught: java.lang.IllegalStateException: expected"), output);
        assertTrue(!output.contains("at "), "no stack trace expected, got: " + output);
    }

    @Test
    void routesLevelsToTheRightStream() {
        ConsoleLogger logger = logger(false);
        logger.info("fine");
        logger.warn("worrying");
        logger.error("broken");

        assertTrue(out().contains("fine"));
        assertTrue(err().contains("worrying"));
        assertTrue(err().contains("broken"));
        assertTrue(!out().contains("worrying"));
    }

    @Test
    @DisplayName("debug and trace stay silent unless verbose")
    void gatesDebugBehindVerbosity() {
        logger(false).debug("hidden");
        logger(false).trace("hidden too");
        assertEquals("", out());

        logger(true).debug("visible");
        assertTrue(out().contains("[testmod/DEBUG] visible"));
    }
}
