package fr.d4emon.fenix.event;

/**
 * A registered listener, which can be taken back off the event.
 *
 * <p>Every {@code register} call returns one. Being able to unsubscribe is
 * deliberate: a listener that is only relevant while a screen is open, a world
 * is loaded, or a feature is enabled should not have to guard itself on every
 * single dispatch forever.
 *
 * <p>It extends {@link AutoCloseable} with no checked exception, so it works in
 * try-with-resources and as a method reference. Closing twice is harmless.
 */
@FunctionalInterface
public interface Subscription extends AutoCloseable {

    /**
     * Removes the listener from its event. Idempotent.
     */
    @Override
    void close();
}
