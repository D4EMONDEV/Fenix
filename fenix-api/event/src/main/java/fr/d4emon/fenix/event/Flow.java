package fr.d4emon.fenix.event;

/**
 * What a listener wants to happen next.
 *
 * <p>Returned by listeners of a {@link CancellableEvent}. A listener that
 * cancels stops the game's own action <em>and</em> the remaining listeners:
 * once something has decided the action must not happen, asking the rest is
 * meaningless.
 */
public enum Flow {

    /** Let the action proceed, and let the remaining listeners see the event. */
    CONTINUE,

    /** Stop the action, and stop dispatching to further listeners. */
    CANCEL;

    /**
     * {@return whether this is {@link #CANCEL}}
     */
    public boolean isCancelled() {
        return this == CANCEL;
    }
}
