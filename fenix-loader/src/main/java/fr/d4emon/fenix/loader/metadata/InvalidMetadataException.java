package fr.d4emon.fenix.loader.metadata;

/**
 * Thrown when a {@code fenix.mod.json} cannot be understood.
 *
 * <p>Always carries the source it came from, because the person who has to fix
 * it is usually looking at a folder of jars and needs to know which one is at
 * fault before anything else.
 *
 * <p>Unchecked on purpose: discovery catches these at its own boundary so it can
 * report every broken mod at once, rather than stopping at the first.
 */
public final class InvalidMetadataException extends RuntimeException {

    private final transient String source;

    /**
     * Creates an exception whose message reads {@code source: detail}.
     *
     * @param source where the metadata came from, such as a jar file name
     * @param detail what is wrong with it
     */
    public InvalidMetadataException(String source, String detail) {
        super(source + ": " + detail);
        this.source = source;
    }

    /**
     * Creates an exception whose message reads {@code source: detail}, keeping
     * the failure that caused it.
     *
     * @param source where the metadata came from, such as a jar file name
     * @param detail what is wrong with it
     * @param cause  the underlying failure
     */
    public InvalidMetadataException(String source, String detail, Throwable cause) {
        super(source + ": " + detail, cause);
        this.source = source;
    }

    /**
     * {@return where the offending metadata came from}
     */
    public String source() {
        return source;
    }
}
