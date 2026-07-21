package fr.d4emon.fenix.config;

/**
 * A configuration file that cannot be read, naming the file and the field.
 *
 * <p>Unchecked on purpose: a mod cannot sensibly recover from its own
 * configuration being wrong, and the useful thing is a message a player can act
 * on rather than a catch block that hides it.
 */
public final class ConfigException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Reports a file a player can go and fix.
     *
     * @param message what is wrong, in terms of the file they edited
     */
    public ConfigException(String message) {
        super(message);
    }

    /**
     * Reports a file a player can go and fix, over something lower down.
     *
     * @param message what is wrong
     * @param cause   what went wrong underneath
     */
    public ConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}
