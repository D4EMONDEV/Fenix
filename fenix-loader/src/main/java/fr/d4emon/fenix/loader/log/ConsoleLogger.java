package fr.d4emon.fenix.loader.log;

import fr.d4emon.fenix.api.log.FenixLogger;

import java.io.PrintStream;
import java.util.Objects;

/**
 * The fallback logging backend: plain lines on the standard streams.
 *
 * <p>Used when nothing better is available — once the loader runs inside the
 * real game, an slf4j-backed logger takes over so mod output lands in the
 * game's log files. The format is {@code [name/LEVEL] message}.
 *
 * <p>{@code trace} and {@code debug} are silent unless the {@code fenix.debug}
 * system property is set.
 */
public final class ConsoleLogger implements FenixLogger {

    private final String name;
    private final PrintStream out;
    private final PrintStream err;
    private final boolean verbose;

    /**
     * Creates a logger writing to {@link System#out} and {@link System#err}.
     *
     * @param name the name shown in every line — the mod id, or {@code fenix}
     *             for the loader itself
     * @throws NullPointerException if the name is {@code null}
     */
    public ConsoleLogger(String name) {
        this(name, System.out, System.err, Boolean.getBoolean("fenix.debug"));
    }

    /**
     * Visible for tests: inject the streams and the verbosity.
     */
    ConsoleLogger(String name, PrintStream out, PrintStream err, boolean verbose) {
        this.name = Objects.requireNonNull(name, "name");
        this.out = Objects.requireNonNull(out, "out");
        this.err = Objects.requireNonNull(err, "err");
        this.verbose = verbose;
    }

    @Override
    public void trace(String message, Object... arguments) {
        if (verbose) {
            write(out, "TRACE", message, arguments);
        }
    }

    @Override
    public void debug(String message, Object... arguments) {
        if (verbose) {
            write(out, "DEBUG", message, arguments);
        }
    }

    @Override
    public void info(String message, Object... arguments) {
        write(out, "INFO", message, arguments);
    }

    @Override
    public void warn(String message, Object... arguments) {
        write(err, "WARN", message, arguments);
    }

    @Override
    public void error(String message, Object... arguments) {
        write(err, "ERROR", message, arguments);
    }

    private void write(PrintStream stream, String level, String message, Object[] arguments) {
        int placeholders = countPlaceholders(message);

        // A trailing throwable with no placeholder of its own gets its stack
        // trace, matching what every logging API has taught people to expect.
        Throwable throwable = null;
        int usable = arguments.length;
        if (arguments.length > placeholders && arguments[arguments.length - 1] instanceof Throwable last) {
            throwable = last;
            usable--;
        }

        StringBuilder line = new StringBuilder(message.length() + 32)
                .append('[').append(name).append('/').append(level).append("] ");
        int argument = 0;
        int index = 0;
        while (index < message.length()) {
            if (argument < usable && index + 1 < message.length()
                    && message.charAt(index) == '{' && message.charAt(index + 1) == '}') {
                line.append(arguments[argument++]);
                index += 2;
            } else {
                line.append(message.charAt(index++));
            }
        }

        stream.println(line);
        if (throwable != null) {
            throwable.printStackTrace(stream);
        }
    }

    private static int countPlaceholders(String message) {
        int count = 0;
        for (int i = 0; i + 1 < message.length(); i++) {
            if (message.charAt(i) == '{' && message.charAt(i + 1) == '}') {
                count++;
                i++;
            }
        }
        return count;
    }
}
