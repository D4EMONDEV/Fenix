package fr.d4emon.fenix.ember;

import fr.d4emon.fenix.registry.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;

/**
 * The {@link Ember} that actually writes files.
 *
 * <p>JSON is emitted by hand rather than through a serialiser: these files have
 * a handful of fixed shapes, and writing them directly keeps the output stable,
 * diffable, and free of whatever Gson version the game happens to ship.
 *
 * <p>Translations accumulate and are written once at the end, since they all
 * share a single file.
 */
final class EmberWriter implements Ember {

    private final String modId;
    private final Path assets;
    private final Map<String, String> translations = new TreeMap<>();

    EmberWriter(String modId, Path outputRoot) {
        this.modId = modId;
        this.assets = outputRoot.resolve("assets").resolve(modId);
    }

    @Override
    public String modId() {
        return modId;
    }

    @Override
    public void cubeAll(Holder<Block> block) {
        String name = path(block.id());
        String model = modId + ":block/" + name;

        write(assets.resolve("models/block/" + name + ".json"), """
                {
                  "parent": "minecraft:block/cube_all",
                  "textures": {
                    "all": "%s"
                  }
                }
                """.formatted(model));

        write(assets.resolve("blockstates/" + name + ".json"), """
                {
                  "variants": {
                    "": {
                      "model": "%s"
                    }
                  }
                }
                """.formatted(model));

        // A block's item points straight at the block model — there is no
        // separate item model for it, as vanilla's own blocks show.
        writeItemDefinition(name, model);
    }

    @Override
    public void flatItem(Holder<Item> item) {
        String name = path(item.id());
        String model = modId + ":item/" + name;

        write(assets.resolve("models/item/" + name + ".json"), """
                {
                  "parent": "minecraft:item/generated",
                  "textures": {
                    "layer0": "%s"
                  }
                }
                """.formatted(model));

        writeItemDefinition(name, model);
    }

    @Override
    public void cubeAll(Holder<Block> block, String english) {
        cubeAll(block);
        blockName(block, english);
    }

    @Override
    public void flatItem(Holder<Item> item, String english) {
        flatItem(item);
        itemName(item, english);
    }

    @Override
    public void blockName(Holder<Block> block, String english) {
        translate(block.get().getDescriptionId(), english);
    }

    @Override
    public void itemName(Holder<Item> item, String english) {
        translate(item.get().getDescriptionId(), english);
    }

    @Override
    public void translate(String key, String english) {
        translations.put(key, english);
    }

    /**
     * The model definition every item needs since 26.x, separate from the model
     * itself: {@code assets/<mod>/items/<name>.json}.
     */
    private void writeItemDefinition(String name, String model) {
        write(assets.resolve("items/" + name + ".json"), """
                {
                  "model": {
                    "type": "minecraft:model",
                    "model": "%s"
                  }
                }
                """.formatted(model));
    }

    /** Writes the accumulated translations. Called once, after collection. */
    void finish() {
        if (translations.isEmpty()) {
            return;
        }
        StringBuilder json = new StringBuilder("{\n");
        String separator = "";
        for (Map.Entry<String, String> entry : translations.entrySet()) {
            json.append(separator).append("  ")
                    .append(quote(entry.getKey())).append(": ").append(quote(entry.getValue()));
            separator = ",\n";
        }
        json.append("\n}\n");
        write(assets.resolve("lang/en_us.json"), json.toString());
    }

    /**
     * The path part of an id. A holder is used rather than a bare name so the
     * file and the registered object cannot disagree.
     */
    private static String path(Identifier id) {
        return id.getPath();
    }

    private static void write(Path file, String content) {
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("cannot write " + file, e);
        }
    }

    private static String quote(String text) {
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
}
