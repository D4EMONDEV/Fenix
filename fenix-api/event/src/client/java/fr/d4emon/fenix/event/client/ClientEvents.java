package fr.d4emon.fenix.event.client;

import fr.d4emon.fenix.event.Event;
import net.minecraft.client.Minecraft;

/**
 * The client's heartbeat.
 *
 * <p>Client only — this package is never loaded on a dedicated server. For
 * anything authoritative, use {@code fr.d4emon.fenix.event.ServerEvents}
 * instead; a client tick is for rendering, input and local state.
 */
public final class ClientEvents {

    /**
     * One client tick.
     *
     * @param client the game client
     */
    public record Tick(Minecraft client) {
    }

    /** Fires before each client tick. */
    public static final Event<Tick> TICK_START = Event.create();

    /** Fires after each client tick. */
    public static final Event<Tick> TICK_END = Event.create();

    private ClientEvents() {
    }
}
