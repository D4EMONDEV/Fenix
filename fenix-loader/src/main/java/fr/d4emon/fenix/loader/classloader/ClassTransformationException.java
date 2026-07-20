package fr.d4emon.fenix.loader.classloader;

/**
 * Thrown when a {@link ClassTransformer} fails, so the launch stops.
 *
 * <p>Deliberately never swallowed by the classloader's fallback path: letting
 * the parent supply its copy after a failed transformation would silently run
 * the <em>untransformed</em> class, which is a far worse failure than a crash —
 * a mixin that did not apply, with no error anywhere.
 */
public final class ClassTransformationException extends RuntimeException {

    /**
     * Creates the exception; the message should name the class being transformed.
     *
     * @param message what failed, naming the class
     */
    public ClassTransformationException(String message) {
        super(message);
    }

    /**
     * Creates the exception; the message should name the class being transformed.
     *
     * @param message what failed, naming the class
     * @param cause   the transformer's own failure
     */
    public ClassTransformationException(String message, Throwable cause) {
        super(message, cause);
    }
}
