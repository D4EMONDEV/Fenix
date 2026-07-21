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
