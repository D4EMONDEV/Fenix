package fr.d4emon.fenix.event;

import java.util.Objects;

/**
 * An event whose listeners can stop the thing from happening.
 *
 * <p>Same shape as {@link Event}, except a listener returns a {@link Flow}:
 *
 * <pre>{@code
 * public record BlockBreak(Player player, BlockPos pos) { }
 *
 * public static final CancellableEvent<BlockBreak> BREAK = CancellableEvent.create();
 * }</pre>
 *
 * <pre>{@code
 * BlockEvents.BREAK.register(break -> isProtected(break.pos()) ? Flow.CANCEL : Flow.CONTINUE);
 * }</pre>
 *
 * <p>The first listener to cancel ends the dispatch — the remaining listeners
 * are not called, because once the action is off the table there is nothing
 * left for them to decide. A listener that only wants to <em>observe</em>
 * every occurrence should sit on a plain {@link Event}, or register at
 * {@link Priority#HIGHEST} to run before anyone can cancel.
 *
 * <p>Firing returns the outcome, which is what the caller acts on:
 *
 * <pre>{@code
 * if (BlockEvents.BREAK.fire(new BlockBreak(player, pos)).isCancelled()) {
 *     callback.cancel();
 * }
 * }</pre>
 *
 * @param <C> the context handed to listeners
 */
public final class CancellableEvent<C> {

    /**
     * A listener on a {@link CancellableEvent}.
     *
     * @param <C> the context type
     */
    @FunctionalInterface
    public interface Listener<C> {

        /**
         * Called when the event fires.
         *
         * @param context what is about to happen
         * @return {@link Flow#CONTINUE} to allow it, {@link Flow#CANCEL} to stop it
         */
        Flow on(C context);
    }

    private final Listeners listeners = new Listeners();

    private CancellableEvent() {
    }

    /**
     * Creates an event.
     *
     * @param <C> the context handed to listeners
     * @return a new event with no listeners
     */
    public static <C> CancellableEvent<C> create() {
        return new CancellableEvent<>();
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
     * Asks every listener, in priority order, stopping at the first refusal.
     *
     * @param context what is about to happen
     * @return {@link Flow#CANCEL} if a listener cancelled, {@link Flow#CONTINUE} otherwise
     */
    public Flow fire(C context) {
        for (Object listener : listeners.active()) {
            @SuppressWarnings("unchecked") // only register() puts listeners in here
            Listener<C> typed = (Listener<C>) listener;

            Flow flow = typed.on(context);
            if (flow == null) {
                throw new NullPointerException(typed.getClass().getName()
                        + " returned null; a listener must return Flow.CONTINUE or Flow.CANCEL");
            }
            if (flow.isCancelled()) {
                return Flow.CANCEL;
            }
        }
        return Flow.CONTINUE;
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
