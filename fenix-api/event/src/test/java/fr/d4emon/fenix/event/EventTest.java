package fr.d4emon.fenix.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventTest {

    /** The kind of record a real event carries. */
    private record Tick(int number) {
    }

    @Nested
    @DisplayName("dispatch")
    class Dispatch {

        @Test
        void tellsEveryListener() {
            Event<Tick> event = Event.create();
            List<String> seen = new ArrayList<>();
            event.register(tick -> seen.add("a" + tick.number()));
            event.register(tick -> seen.add("b" + tick.number()));

            event.fire(new Tick(1));

            assertEquals(List.of("a1", "b1"), seen);
        }

        @Test
        void firingWithNoListenersDoesNothing() {
            Event<Tick> event = Event.create();

            event.fire(new Tick(1));

            assertFalse(event.hasListeners());
        }

        @Test
        void reportsWhetherAnythingIsListening() {
            Event<Tick> event = Event.create();
            assertFalse(event.hasListeners());

            Subscription subscription = event.register(tick -> { });
            assertTrue(event.hasListeners());

            subscription.close();
            assertFalse(event.hasListeners());
        }
    }

    @Nested
    @DisplayName("ordering")
    class Ordering {

        @Test
        @DisplayName("higher priority runs first, whatever the registration order")
        void sortsByPriority() {
            Event<Tick> event = Event.create();
            List<String> seen = new ArrayList<>();
            event.register(Priority.LOW, tick -> seen.add("low"));
            event.register(Priority.HIGHEST, tick -> seen.add("highest"));
            event.register(tick -> seen.add("normal"));
            event.register(Priority.HIGH, tick -> seen.add("high"));

            event.fire(new Tick(1));

            assertEquals(List.of("highest", "high", "normal", "low"), seen);
        }

        @Test
        @DisplayName("the same priority keeps registration order")
        void isStableWithinAPriority() {
            Event<Tick> event = Event.create();
            List<String> seen = new ArrayList<>();
            event.register(tick -> seen.add("first"));
            event.register(tick -> seen.add("second"));
            event.register(tick -> seen.add("third"));

            event.fire(new Tick(1));

            assertEquals(List.of("first", "second", "third"), seen);
        }

        @Test
        void acceptsArbitraryPriorities() {
            Event<Tick> event = Event.create();
            List<String> seen = new ArrayList<>();
            event.register(Priority.NORMAL + 1, tick -> seen.add("just above normal"));
            event.register(Priority.NORMAL, tick -> seen.add("normal"));

            event.fire(new Tick(1));

            assertEquals(List.of("just above normal", "normal"), seen);
        }
    }

    @Nested
    @DisplayName("unsubscribing")
    class Unsubscribing {

        @Test
        void removesTheListener() {
            Event<Tick> event = Event.create();
            AtomicInteger calls = new AtomicInteger();
            Subscription subscription = event.register(tick -> calls.incrementAndGet());

            event.fire(new Tick(1));
            subscription.close();
            event.fire(new Tick(2));

            assertEquals(1, calls.get());
        }

        @Test
        @DisplayName("closing twice is harmless")
        void isIdempotent() {
            Event<Tick> event = Event.create();
            Subscription subscription = event.register(tick -> { });

            subscription.close();
            subscription.close();

            assertFalse(event.hasListeners());
        }

        @Test
        @DisplayName("removing one listener leaves the others alone")
        void removesOnlyItsOwn() {
            Event<Tick> event = Event.create();
            List<String> seen = new ArrayList<>();
            event.register(tick -> seen.add("kept"));
            Subscription dropped = event.register(tick -> seen.add("dropped"));
            event.register(tick -> seen.add("also kept"));

            dropped.close();
            event.fire(new Tick(1));

            assertEquals(List.of("kept", "also kept"), seen);
        }

        @Test
        @DisplayName("two identical listeners are separate registrations")
        void treatsIdenticalListenersSeparately() {
            Event<Tick> event = Event.create();
            AtomicInteger calls = new AtomicInteger();
            Event.Listener<Tick> listener = tick -> calls.incrementAndGet();

            Subscription first = event.register(listener);
            event.register(listener);
            first.close();
            event.fire(new Tick(1));

            assertEquals(1, calls.get(), "only the closed registration should be gone");
        }

        @Test
        @SuppressWarnings("try") // the unused resource is the point: it closes at the end of the block
        void worksInTryWithResources() {
            Event<Tick> event = Event.create();
            AtomicInteger calls = new AtomicInteger();

            try (Subscription ignored = event.register(tick -> calls.incrementAndGet())) {
                event.fire(new Tick(1));
            }
            event.fire(new Tick(2));

            assertEquals(1, calls.get());
        }
    }

    @Nested
    @DisplayName("concurrency")
    class Concurrency {

        @Test
        @DisplayName("registering during a dispatch does not disturb it")
        void toleratesRegistrationWhileFiring() {
            Event<Tick> event = Event.create();
            List<String> seen = new ArrayList<>();
            event.register(tick -> {
                seen.add("first");
                event.register(later -> seen.add("added mid-dispatch"));
            });
            event.register(tick -> seen.add("second"));

            event.fire(new Tick(1));

            assertEquals(List.of("first", "second"), seen, "the new listener joins from the next dispatch");

            seen.clear();
            event.fire(new Tick(2));
            assertTrue(seen.contains("added mid-dispatch"));
        }

        @Test
        @DisplayName("unsubscribing during a dispatch does not disturb it")
        void toleratesUnsubscribeWhileFiring() {
            Event<Tick> event = Event.create();
            List<String> seen = new ArrayList<>();
            Subscription[] second = new Subscription[1];
            event.register(Priority.HIGH, tick -> {
                seen.add("first");
                second[0].close();
            });
            second[0] = event.register(tick -> seen.add("second"));

            event.fire(new Tick(1));

            assertEquals(List.of("first", "second"), seen, "the dispatch in flight is unaffected");

            seen.clear();
            event.fire(new Tick(2));
            assertEquals(List.of("first"), seen);
        }

        @Test
        @DisplayName("concurrent registration loses nothing")
        void survivesConcurrentRegistration() throws InterruptedException {
            Event<Tick> event = Event.create();
            int threads = 8;
            int perThread = 100;
            CountDownLatch start = new CountDownLatch(1);
            CountDownLatch done = new CountDownLatch(threads);

            for (int i = 0; i < threads; i++) {
                Thread.ofPlatform().start(() -> {
                    try {
                        start.await();
                        for (int j = 0; j < perThread; j++) {
                            event.register(tick -> { });
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        done.countDown();
                    }
                });
            }
            start.countDown();
            assertTrue(done.await(30, TimeUnit.SECONDS), "registration threads should finish");

            AtomicInteger calls = new AtomicInteger();
            event.register(tick -> calls.incrementAndGet());
            event.fire(new Tick(1));

            assertEquals(1, calls.get());
            assertTrue(event.hasListeners());
        }
    }

    @Test
    void rejectsANullListener() {
        Event<Tick> event = Event.create();

        assertThrows(NullPointerException.class, () -> event.register(null));
        assertThrows(NullPointerException.class, () -> event.register(Priority.HIGH, null));
    }
}
