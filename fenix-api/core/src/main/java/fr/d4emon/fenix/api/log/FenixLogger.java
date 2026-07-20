package fr.d4emon.fenix.api.log;

/**
 * A logger scoped to a single mod.
 *
 * <p>Obtained from {@code Fenix#logger()}. Every mod gets its own instance, so
 * its output is attributed to it without the mod having to name itself in every
 * message.
 *
 * <p>Messages use {@code {}} as a placeholder and are only formatted if the
 * level is enabled:
 *
 * <pre>{@code
 * logger.info("Loaded {} blocks in {} ms", count, elapsed);
 * }</pre>
 *
 * <p>If the last argument is a {@link Throwable} and has no matching
 * placeholder, its stack trace is logged after the message.
 */
public interface FenixLogger {

    /**
     * Logs a message useful only when tracing execution step by step.
     *
     * @param message   the message, with {@code {}} placeholders
     * @param arguments values for the placeholders, optionally followed by a {@link Throwable}
     */
    void trace(String message, Object... arguments);

    /**
     * Logs a message useful when diagnosing a problem.
     *
     * @param message   the message, with {@code {}} placeholders
     * @param arguments values for the placeholders, optionally followed by a {@link Throwable}
     */
    void debug(String message, Object... arguments);

    /**
     * Logs a message worth seeing during a normal run.
     *
     * @param message   the message, with {@code {}} placeholders
     * @param arguments values for the placeholders, optionally followed by a {@link Throwable}
     */
    void info(String message, Object... arguments);

    /**
     * Logs something unexpected that did not stop the mod from working.
     *
     * @param message   the message, with {@code {}} placeholders
     * @param arguments values for the placeholders, optionally followed by a {@link Throwable}
     */
    void warn(String message, Object... arguments);

    /**
     * Logs a failure.
     *
     * @param message   the message, with {@code {}} placeholders
     * @param arguments values for the placeholders, optionally followed by a {@link Throwable}
     */
    void error(String message, Object... arguments);
}
