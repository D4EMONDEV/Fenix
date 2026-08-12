package fr.d4emon.fenix.event;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The listener storage behind both event types.
 *
 * <p>Registration is rare; dispatch is constant. So registration takes a lock
 * and rebuilds a flat, priority-sorted array, which dispatch then reads from a
 * single {@code volatile} field and walks by index. Firing therefore takes no
 * lock, allocates nothing, and never sees a half-updated list — a listener
 * registering or unsubscribing while an event is being dispatched is safe, and
 * simply takes effect from the next dispatch.
 *
 * <p>The array is {@code Object[]} because the two event types hold different
 * listener interfaces; each casts its own elements back on the way out. The
 * cast is safe by construction: nothing else can put anything in here.
 */
final class Listeners {

    private static final Object[] NONE = new Object[0];

    /**
     * Reports a listener that threw, and {@return nothing}.
     *
     * <p>An event is fired from inside Minecraft. Letting a listener's exception
     * out of {@code fire} sends it up through the game's own call stack, which
     * usually ends the game — with a crash report that names a Minecraft method
     * and no mention of the mod that actually failed. One broken listener would
     * also stop every listener registered after it, none of which did anything
     * wrong.
     *
     * <p>So it is caught here, logged with the listener named, and the event
     * carries on. The log line is the loud part: swallowing a failure silently
     * would trade one bad outcome for a worse one, a mod that simply stops
     * working with nothing anywhere saying why.
     *
     * <p>{@code Error} is deliberately not caught. An {@code OutOfMemoryError}
     * or a {@code StackOverflowError} is not a listener misbehaving, it is the
     * JVM in trouble, and carrying on around it helps nobody.
     *
     * @param listener the listener that threw
     * @param context  what was being dispatched, named in the message
     * @param failure  what it threw
     */
    static void failed(Object listener, Object context, Exception failure) {
        // A lambda's class name is the owning class plus a generated suffix, so
        // this still says which mod it came from — the best available, since a
        // lambda has no name of its own.
        LOGGER.log(System.Logger.Level.ERROR,
                () -> "Fenix: a listener for "
                        + (context == null ? "an event" : context.getClass().getName())
                        + " threw and was skipped; the event carried on to the rest. Listener: "
                        + listener.getClass().getName(),
                failure);
    }

    /**
     * The JDK's own logger, and not Minecraft's.
     *
     * <p>Everything else in this file is plain Java, which is what lets the
     * event bus be unit-tested in milliseconds without a game. Reaching for
     * {@code com.mojang.logging.LogUtils} for one line would put Minecraft on
     * the test runtime classpath, and the tests fail to load without it — which
     * is exactly what happened when this was written that way.
     */
    private static final System.Logger LOGGER = System.getLogger("fenix.event");

    /** Sorted, immutable once published. Read without locking. */
    private volatile Object[] active = NONE;

    /** The registration record, guarded by {@code this}. */
    private final List<Entry> entries = new ArrayList<>();

    private long sequence;

    private record Entry(int priority, long sequence, Object listener) {
    }

    /**
     * Registers a listener and returns the handle that removes it.
     */
    synchronized Subscription add(int priority, Object listener) {
        Entry entry = new Entry(priority, sequence++, listener);
        entries.add(entry);
        rebuild();
        return () -> remove(entry);
    }

    private synchronized void remove(Entry entry) {
        if (entries.remove(entry)) {
            rebuild();
        }
    }

    /** Must be called while holding the lock. */
    private void rebuild() {
        List<Entry> sorted = new ArrayList<>(entries);
        // Higher priority first; same priority keeps registration order.
        sorted.sort(Comparator.comparingInt(Entry::priority).reversed()
                .thenComparingLong(Entry::sequence));

        Object[] built = new Object[sorted.size()];
        for (int i = 0; i < built.length; i++) {
            built[i] = sorted.get(i).listener();
        }
        active = built;
    }

    /**
     * {@return the current listeners, in dispatch order}
     *
     * <p>Never modify the returned array.
     */
    Object[] active() {
        return active;
    }
}
