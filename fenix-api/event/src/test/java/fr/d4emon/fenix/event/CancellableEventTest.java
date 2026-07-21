package fr.d4emon.fenix.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CancellableEventTest {

    private record BlockBreak(String block) {
    }

    @Test
    @DisplayName("with nobody objecting, the action proceeds")
    void continuesWhenNoListenerCancels() {
        CancellableEvent<BlockBreak> event = CancellableEvent.create();
        event.register(context -> Flow.CONTINUE);
        event.register(context -> Flow.CONTINUE);

        Flow result = event.fire(new BlockBreak("stone"));

        assertEquals(Flow.CONTINUE, result);
        assertFalse(result.isCancelled());
    }

    @Test
    void anEmptyEventContinues() {
        CancellableEvent<BlockBreak> event = CancellableEvent.create();

        assertEquals(Flow.CONTINUE, event.fire(new BlockBreak("stone")));
    }

    @Test
    @DisplayName("one refusal cancels the whole thing")
    void cancelsWhenAListenerRefuses() {
        CancellableEvent<BlockBreak> event = CancellableEvent.create();
        event.register(context -> Flow.CONTINUE);
        event.register(context -> Flow.CANCEL);

        Flow result = event.fire(new BlockBreak("bedrock"));

        assertTrue(result.isCancelled());
    }

    @Test
    @DisplayName("cancelling stops the listeners after it — the decision is made")
    void stopsDispatchAtTheFirstCancel() {
        CancellableEvent<BlockBreak> event = CancellableEvent.create();
        List<String> seen = new ArrayList<>();
        event.register(Priority.HIGH, context -> {
            seen.add("first");
            return Flow.CANCEL;
        });
        event.register(context -> {
            seen.add("second");
            return Flow.CONTINUE;
        });

        event.fire(new BlockBreak("bedrock"));

        assertEquals(List.of("first"), seen);
    }

    @Test
    @DisplayName("a HIGHEST listener sees events nobody has cancelled yet")
    void priorityDecidesWhoGetsToDecideFirst() {
        CancellableEvent<BlockBreak> event = CancellableEvent.create();
        List<String> seen = new ArrayList<>();
        event.register(Priority.LOW, context -> {
            seen.add("low");
            return Flow.CONTINUE;
        });
        event.register(Priority.HIGHEST, context -> {
            seen.add("highest");
            return Flow.CONTINUE;
        });

        event.fire(new BlockBreak("stone"));

        assertEquals(List.of("highest", "low"), seen);
    }

    @Test
    void unsubscribingWorksTheSameWay() {
        CancellableEvent<BlockBreak> event = CancellableEvent.create();
        Subscription subscription = event.register(context -> Flow.CANCEL);

        assertTrue(event.fire(new BlockBreak("stone")).isCancelled());
        subscription.close();
        assertFalse(event.fire(new BlockBreak("stone")).isCancelled());
    }

    @Test
    @DisplayName("returning null is a bug, and says so")
    void rejectsANullFlow() {
        CancellableEvent<BlockBreak> event = CancellableEvent.create();
        event.register(context -> null);

        NullPointerException failure = assertThrows(NullPointerException.class,
                () -> event.fire(new BlockBreak("stone")));

        assertTrue(failure.getMessage().contains("Flow.CONTINUE"), failure.getMessage());
    }

    @Test
    void rejectsANullListener() {
        CancellableEvent<BlockBreak> event = CancellableEvent.create();

        assertThrows(NullPointerException.class, () -> event.register(null));
    }
}
