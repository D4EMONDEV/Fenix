package fr.d4emon.fenix.event;

import net.minecraft.server.MinecraftServer;

/**
 * The server's lifecycle and heartbeat.
 *
 * <p>These fire on a dedicated server and on the integrated server inside a
 * single-player game — the server is where the world actually lives, so
 * anything authoritative belongs here rather than in
 * {@code fr.d4emon.fenix.event.client.ClientEvents}.
 */
public final class ServerEvents {

    /**
     * A server that has just come up.
     *
     * @param server the server
     */
    public record Started(MinecraftServer server) {
    }

    /**
     * One server tick.
     *
     * @param server the server
     */
    public record Tick(MinecraftServer server) {
    }

    /**
     * Fires once per server, on its first tick — the world is loaded and the
     * server is running. Loading a second world fires it again for that server.
     */
    public static final Event<Started> STARTED = Event.create();

    /** Fires before each server tick. */
    public static final Event<Tick> TICK_START = Event.create();

    /** Fires after each server tick. */
    public static final Event<Tick> TICK_END = Event.create();

    private ServerEvents() {
    }
}
