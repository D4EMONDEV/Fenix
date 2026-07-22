package fr.d4emon.fenix.registry.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Binds a key.
 *
 * <pre>{@code
 * public static final KeyMapping OPEN_SAFE =
 *         KeyBindings.register(Identifier.parse("mymod:open_safe"), InputConstants.KEY_K);
 * }</pre>
 *
 * <p>Then read it on the client tick, in a loop — a key pressed twice between
 * two ticks reports twice, and asking once would drop the second press:
 *
 * <pre>{@code
 * ClientEvents.TICK.register(tick -> {
 *     while (OPEN_SAFE.consumeClick()) {
 *         // …
 *     }
 * });
 * }</pre>
 *
 * <p>The name is a translation key: {@code mymod:open_safe} is shown as
 * whatever {@code key.mymod.open_safe} says, under the category's own key.
 * Without a translation the player sees the raw key in their controls screen.
 *
 * <p>Client-only, and registered from {@code onRegister} — which runs before
 * the game builds its options, which is exactly when the list has to be
 * complete.
 */
public final class KeyBindings {

    /**
     * What has been registered, in registration order.
     *
     * <p>Held rather than pushed straight into the game's options because
     * those do not exist yet: {@code onRegister} runs during bootstrap, and
     * {@code Options} is built later, in Minecraft's constructor.
     */
    private static final List<KeyMapping> REGISTERED = new ArrayList<>();

    private KeyBindings() {
    }

    /**
     * Binds a key, in the miscellaneous category.
     *
     * @param id      the mod's id and a name, as {@code mymod:open_safe}
     * @param keyCode the default key — an {@code InputConstants.KEY_*} constant
     * @return the mapping, to ask whether it was pressed
     * @throws NullPointerException  if {@code id} is {@code null}
     * @throws IllegalStateException if the same id is registered twice
     */
    public static KeyMapping register(Identifier id, int keyCode) {
        return register(id, keyCode, KeyMapping.Category.MISC);
    }

    /**
     * Binds a key.
     *
     * @param id       the mod's id and a name, as {@code mymod:open_safe}
     * @param keyCode  the default key — an {@code InputConstants.KEY_*} constant
     * @param category which group it appears under in the controls screen
     * @return the mapping, to ask whether it was pressed
     * @throws NullPointerException  if any argument is {@code null}
     * @throws IllegalStateException if the same id is registered twice
     */
    public static KeyMapping register(Identifier id, int keyCode, KeyMapping.Category category) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(category, "category");

        String name = id.toLanguageKey("key");
        // Vanilla's own constructor puts the mapping in a static table keyed by
        // name, and silently replaces whatever was there. Two mods choosing the
        // same name would leave one of them holding a mapping the game no
        // longer knows about — bound in the controls screen, dead in play.
        if (KeyMapping.get(name) != null) {
            throw new IllegalStateException("a key binding named " + name + " already exists");
        }

        KeyMapping mapping = new KeyMapping(name, InputConstants.Type.KEYSYM, keyCode, category);
        REGISTERED.add(mapping);
        return mapping;
    }

    /**
     * Makes a group of the mod's own for the controls screen.
     *
     * <p>Only worth it for a mod with several keys; one key is better placed in
     * a category the player already scrolls past. The label comes from
     * {@code key.category.<namespace>.<path>}.
     *
     * @param id the mod's id and a name, as {@code mymod:safes}
     * @return the category, to pass to {@link #register(Identifier, int, KeyMapping.Category)}
     * @throws NullPointerException     if {@code id} is {@code null}
     * @throws IllegalArgumentException if the category already exists
     */
    public static KeyMapping.Category category(Identifier id) {
        Objects.requireNonNull(id, "id");
        return KeyMapping.Category.register(id);
    }

    /**
     * {@return every mapping registered, in order}
     *
     * <p>For the mixin that appends them to the game's options. Not part of
     * what a mod is meant to call.
     */
    public static List<KeyMapping> fenix$registered() {
        return List.copyOf(REGISTERED);
    }
}
