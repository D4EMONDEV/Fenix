package fr.d4emon.fenix.event.client;

import fr.d4emon.fenix.event.Event;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientPacketListener;

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

    /**
     * This client has joined a world — a server's, or its own.
     *
     * <p>Fires once the login packet has been handled, so the level and the
     * player exist. A single-player world is a server too, so this fires there
     * as well; that is the point, since a mod's client state has to be set up
     * the same way either way.
     *
     * @param client     the game client
     * @param connection what it is talking to
     */
    public record Connected(Minecraft client, ClientPacketListener connection) {
    }

    /**
     * This client has left a world.
     *
     * <p>The counterpart to {@link Connected}, and where per-world client state
     * belongs discarded — a mod that keeps a cache keyed by the world it is in
     * will otherwise carry it into the next one and be quietly wrong.
     *
     * @param client the game client
     */
    public record Disconnected(Minecraft client) {
    }

    /** Fires when this client has joined a world. */
    /**
     * A screen is about to be shown, or closed.
     *
     * @param screen what is being opened, or {@code null} when the last one is
     *               closing and the player returns to the world
     */
    public record ScreenChange(@Nullable Screen screen) {
    }

    public static final Event<Connected> CONNECTED = Event.create();

    /**
     * Fires whenever the client changes screen, including to nothing.
     *
     * <p>Where a mod adds a button to somebody else's screen, or notices that an
     * inventory has been closed. It fires before the screen is initialised, so
     * its widgets are not there yet — which is the moment a mod wanting to add
     * one has to act.
     */
    public static final Event<ScreenChange> SCREEN = Event.create();

    /** Fires when this client has left one. */
    public static final Event<Disconnected> DISCONNECTED = Event.create();

    private ClientEvents() {
    }
}
