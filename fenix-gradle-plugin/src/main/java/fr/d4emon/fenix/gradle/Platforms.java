package fr.d4emon.fenix.gradle;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Which Fenix versions go with which Minecraft version.
 *
 * <p>Fenix's API is compiled against the game, so an API release belongs to one
 * Minecraft version and to no other. Minecraft renames and moves things every
 * release — in 26.2 alone the text-drawing call, the creative tab structure and
 * villager trades all changed shape — so one API jar cannot serve two game
 * versions, and a mod author must never be handed one that was built for a
 * different game than the one they asked for.
 *
 * <p>Before this table the plugin knew exactly one pairing: the one baked in
 * when the plugin itself was built. Setting {@code fenix { minecraft = "26.3" }}
 * changed the game that was downloaded and left the API coordinate pointing at
 * the release for 26.2. Nothing failed at configuration time. The mod compiled
 * against a jar for the wrong game and broke later, at class loading, naming a
 * missing Minecraft method — which reads as a Fenix bug rather than as a
 * mismatched pair.
 *
 * <p>So the pairings are a table, every known one is carried in the plugin jar,
 * and asking for a game version that is not in it is an error that names the
 * ones that are. A table baked into the jar rather than fetched keeps the build
 * offline, reproducible and free of a network call at configuration time; the
 * cost is that a new game version needs a new plugin release, which is no cost
 * at all — a new game version needs a new API release anyway, and the two ship
 * together.
 *
 * @see <a href="https://github.com/D4EMONDEV/Fenix/blob/main/platforms.json">platforms.json</a>
 */
final class Platforms {

    /** One game version and the Fenix release built for it. */
    record Platform(String minecraft, String branch, String status, int java,
                    String loader, String api, String ember, String processor) {
    }

    private final Map<String, Platform> byMinecraft;

    private Platforms(Map<String, Platform> byMinecraft) {
        this.byMinecraft = byMinecraft;
    }

    /**
     * {@return the table carried in the plugin jar}
     */
    static Platforms load() {
        try (InputStream in = Platforms.class.getResourceAsStream("/platforms.json")) {
            if (in == null) {
                throw new IllegalStateException("platforms.json is missing from the plugin jar");
            }
            JsonObject root = new Gson().fromJson(
                    new InputStreamReader(in, StandardCharsets.UTF_8), JsonObject.class);
            Map<String, Platform> table = new LinkedHashMap<>();
            for (var element : root.getAsJsonArray("platforms")) {
                JsonObject entry = element.getAsJsonObject();
                Platform platform = new Platform(
                        entry.get("minecraft").getAsString(),
                        entry.get("branch").getAsString(),
                        entry.get("status").getAsString(),
                        entry.get("java").getAsInt(),
                        entry.get("loader").getAsString(),
                        entry.get("api").getAsString(),
                        entry.get("ember").getAsString(),
                        entry.get("processor").getAsString());
                table.put(platform.minecraft(), platform);
            }
            if (table.isEmpty()) {
                throw new IllegalStateException("platforms.json lists no platforms");
            }
            return new Platforms(table);
        } catch (IOException e) {
            throw new IllegalStateException("cannot read platforms.json", e);
        }
    }

    /**
     * {@return the Fenix release built for {@code minecraft}}
     *
     * @param minecraft the game version a mod asked for
     * @throws IllegalArgumentException if no Fenix release was built for it
     */
    Platform forMinecraft(String minecraft) {
        Platform platform = byMinecraft.get(minecraft);
        if (platform == null) {
            throw new IllegalArgumentException(
                    "Fenix has no release for Minecraft " + minecraft + "."
                            + " This plugin knows: " + String.join(", ", byMinecraft.keySet())
                            + ". A newer plugin may know more —"
                            + " see https://github.com/D4EMONDEV/Fenix/blob/main/platforms.json");
        }
        return platform;
    }

    /**
     * {@return every game version this plugin can build for, newest entry first}
     */
    List<String> supported() {
        return new ArrayList<>(byMinecraft.keySet());
    }

    /**
     * {@return the version the plugin builds for when a mod names none}
     *
     * <p>The first entry, which the table keeps as the current one.
     */
    Platform current() {
        return byMinecraft.values().iterator().next();
    }
}
