package fr.d4emon.fenix.installer;

/**
 * A diagnosed installation failure. The message tells the user what to do —
 * usually "install and run vanilla Minecraft once first".
 */
public final class InstallException extends RuntimeException {

    /**
     * Creates the exception.
     *
     * @param message the full, user-readable explanation
     */
    public InstallException(String message) {
        super(message);
    }

    /**
     * Creates the exception, keeping the underlying failure.
     *
     * @param message the full, user-readable explanation
     * @param cause   what actually went wrong underneath
     */
    public InstallException(String message, Throwable cause) {
        super(message, cause);
    }
}
