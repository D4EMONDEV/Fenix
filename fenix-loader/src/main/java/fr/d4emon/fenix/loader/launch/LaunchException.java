package fr.d4emon.fenix.loader.launch;

/**
 * A fatal, already-explained launch failure.
 *
 * <p>The message is written for the person reading the crash: it names the mod
 * or the file at fault and, where possible, what to do about it. {@code main}
 * prints it without a stack trace — the trace belongs to unexpected failures,
 * not diagnosed ones.
 */
public final class LaunchException extends RuntimeException {

    /**
     * Creates the exception.
     *
     * @param message the full, player-readable explanation
     */
    public LaunchException(String message) {
        super(message);
    }

    /**
     * Creates the exception, keeping the underlying failure for the log.
     *
     * @param message the full, player-readable explanation
     * @param cause   what actually went wrong underneath
     */
    public LaunchException(String message, Throwable cause) {
        super(message, cause);
    }
}
