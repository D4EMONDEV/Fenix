package fr.d4emon.fenix.ember;

import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Where generated files go, and the small amount of shared machinery for
 * writing them.
 *
 * <p>JSON is written by hand rather than through a serialiser. These files have
 * a handful of fixed shapes, and writing them directly keeps the output stable
 * and diffable — a generated file that reorders itself between runs is a
 * generated file nobody can review.
 */
public final class EmberOutput {

    private final String modId;
    private final Path root;

    EmberOutput(String modId, Path root) {
        this.modId = modId;
        this.root = root;
    }

    /**
     * {@return the mod everything is being written for}
     */
    public String modId() {
        return modId;
    }

    /**
     * Writes a client resource, under this mod's namespace.
     *
     * @param path relative to {@code assets/<mod>/}
     * @param json the file's contents
     */
    public void asset(String path, String json) {
        write(root.resolve("assets").resolve(modId).resolve(path), json);
    }

    /**
     * Writes server data, under this mod's namespace.
     *
     * @param path relative to {@code data/<mod>/}
     * @param json the file's contents
     */
    public void data(String path, String json) {
        data(modId, path, json);
    }

    /**
     * Writes server data under any namespace.
     *
     * <p>Needed for tags: adding to {@code minecraft:mineable/pickaxe} means
     * writing a file in <em>Minecraft's</em> namespace, which the game then
     * merges with vanilla's. A tag file belongs to the tag, not to the mod
     * contributing to it.
     *
     * @param namespace whose data directory to write into
     * @param path      relative to {@code data/<namespace>/}
     * @param json      the file's contents
     */
    public void data(String namespace, String path, String json) {
        write(root.resolve("data").resolve(namespace).resolve(path), json);
    }

    /**
     * {@return a number as JSON wants it, whatever the machine's language}
     *
     * <p>{@code "%.1f".formatted(2.0)} is {@code "2,0"} on a French machine,
     * and that is not JSON. Every number written into a generated file goes
     * through here, so the output does not depend on where it was generated.
     *
     * @param value the number
     */
    /**
     * {@return a float as JSON wants it}
     *
     * <p>Separate from the double overload because widening loses the point of
     * a float: {@code 0.05f} as a double is 0.05000000074505806, and that is
     * what would be written into the file.
     *
     * @param value the number
     */
    public static String decimal(float value) {
        return Float.toString(value);
    }

    public static String decimal(double value) {
        // Double.toString, not %.1f. Both are locale-independent, but the
        // format rounds: a reputation discount of 0.05 came out as 0.1, which
        // is a number the game accepts and a mod author never wrote. This
        // keeps the value and still writes 12 as "12.0", which is what the
        // codecs want.
        return Double.toString(value);
    }

    /**
     * {@return the registered id of a block, item, effect or entity type}
     *
     * @param content what to name
     * @throws IllegalArgumentException if it is none of those
     */
    public static Identifier idOf(Object content) {
        Objects.requireNonNull(content, "content");
        if (content instanceof net.minecraft.core.Holder<?> ignored) {
            throw new IllegalArgumentException("pass the Fenix Holder, not Minecraft's");
        }
        return switch (content) {
            case Block block -> net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block);
            case Item item -> net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item);
            case MobEffect effect ->
                    net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.getKey(effect);
            // Entity types, for the tables that say what a mob drops.
            case net.minecraft.world.entity.EntityType<?> type ->
                    net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(type);
            // Sound events, for the files that name one: a jukebox song, an
            // instrument, sounds.json itself.
            case net.minecraft.sounds.SoundEvent sound ->
                    net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.getKey(sound);
            case net.minecraft.world.level.material.Fluid fluid ->
                    net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(fluid);
            default -> throw new IllegalArgumentException(
                    content.getClass().getName()
                            + " is not something with a registered id that Ember knows");
        };
    }

    /** {@return the translation key of a block, item or status effect} */
    static String descriptionId(Object content) {
        return switch (content) {
            case Block block -> block.getDescriptionId();
            case Item item -> item.getDescriptionId();
            case MobEffect effect -> effect.getDescriptionId();
            default -> throw new IllegalArgumentException(
                    content.getClass().getName() + " has no translation key");
        };
    }

    /** Escapes a string for JSON. */
    static String quote(String text) {
        StringBuilder out = new StringBuilder(text.length() + 2).append('"');
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                default -> out.append(c);
            }
        }
        return out.append('"').toString();
    }

    private static void write(Path file, String json) {
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, json, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("cannot write " + file, e);
        }
    }
}
