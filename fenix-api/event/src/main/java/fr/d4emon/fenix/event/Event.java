package fr.d4emon.fenix.event;

import java.util.Objects;

/**
 * Something that happened, which listeners are told about.
 *
 * <p>An event carries a single <em>context</em> — normally a record holding
 * everything the listener needs. That is what keeps declaring an event to two
 * lines instead of a hand-written functional interface and a combiner per
 * event:
 *
 * <pre>{@code
 * public record ClientTick(Minecraft client) { }
 *
 * public static final Event<ClientTick> TICK_END = Event.create();
 * }</pre>
 *
 * and listening to one line:
 *
 * <pre>{@code
 * ClientEvents.TICK_END.register(tick -> doSomething(tick.client()));
 * }</pre>
 *
 * <p>Adding a parameter later is a change to the record, not a break in every
 * listener's signature.
 *
 * <p>This event <strong>cannot be cancelled</strong> — every listener always
 * runs. That is a promise in the type: if an event can be stopped, it is a
 * {@link CancellableEvent} and its listeners must say so by returning a
 * {@link Flow}. There is no way to "return CANCEL" here and quietly have
 * nothing happen.
 *
 * <p>Safe to register, unsubscribe and fire from any thread. Listeners run on
 * whichever thread fired the event — usually the game thread, so treat them as
 * game code.
 *
 * @param <C> the context handed to listeners
 */
public final class Event<C> {

    /**
     * A listener on an {@link Event}.
     *
     * @param <C> the context type
     */
    @FunctionalInterface
    public interface Listener<C> {

        /**
         * Called when the event fires.
         *
         * @param context what happened
         */
        void on(C context);
    }

    private final Listeners listeners = new Listeners();

    private Event() {
    }

    /**
     * Creates an event.
     *
     * @param <C> the context handed to listeners
     * @return a new event with no listeners
     */
    public static <C> Event<C> create() {
        return new Event<>();
    }

    /**
     * Registers a listener at {@link Priority#NORMAL}.
     *
     * @param listener the listener
     * @return the handle that removes it again
     * @throws NullPointerException if the listener is {@code null}
     */
    public Subscription register(Listener<C> listener) {
        return register(Priority.NORMAL, listener);
    }

    /**
     * Registers a listener.
     *
     * @param priority higher runs earlier; see {@link Priority}
     * @param listener the listener
     * @return the handle that removes it again
     * @throws NullPointerException if the listener is {@code null}
     */
    public Subscription register(int priority, Listener<C> listener) {
        Objects.requireNonNull(listener, "listener");
        return listeners.add(priority, listener);
    }

    /**
     * Tells every listener, in priority order.
     *
     * @param context what happened
     */
    public void fire(C context) {
        for (Object listener : listeners.active()) {
            @SuppressWarnings("unchecked") // only register() puts listeners in here
            Listener<C> typed = (Listener<C>) listener;
            typed.on(context);
        }
    }

    /**
     * {@return whether anything is listening}
     *
     * <p>For skipping expensive work needed only to build the context.
     */
    public boolean hasListeners() {
        return listeners.active().length > 0;
    }
}
