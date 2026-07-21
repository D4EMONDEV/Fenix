package fr.d4emon.fenix.ember;

import fr.d4emon.fenix.registry.Holder;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * Writes models, blockstates and item model definitions.
 *
 * <pre>{@code
 * @Generator
 * public final class ModModels extends EmberModelProvider {
 *     @Override
 *     protected void models() {
 *         cubeAll(ModBlocks.RUBY_BLOCK);
 *         flatItem(ModItems.RUBY);
 *     }
 * }
 * }</pre>
 *
 * <p>The methods describe an intent — "a cube with one texture" — rather than a
 * file. Which files that becomes is Minecraft's business and moves between
 * versions: 26.x wants a model definition under {@code items/} separate from
 * the model itself, and a block's item points straight at the block model with
 * no item model of its own.
 */
public abstract class EmberModelProvider extends EmberProvider {

    /** For subclasses. */
    protected EmberModelProvider() {
    }

    /** Describes the models. */
    protected abstract void models();

    @Override
    protected final void run() {
        models();
    }

    /**
     * A solid cube with the same texture on every face.
     *
     * <p>Expects a texture at {@code assets/<mod>/textures/block/<name>.png},
     * the one thing that cannot be generated.
     *
     * @param block the block
     */
    protected final void cubeAll(Holder<Block> block) {
        String name = block.id().getPath();
        String model = modId() + ":block/" + name;

        output().asset("models/block/" + name + ".json", """
                {
                  "parent": "minecraft:block/cube_all",
                  "textures": {
                    "all": "%s"
                  }
                }
                """.formatted(model));

        output().asset("blockstates/" + name + ".json", """
                {
                  "variants": {
                    "": {
                      "model": "%s"
                    }
                  }
                }
                """.formatted(model));

        itemDefinition(name, model);
    }

    /**
     * A flat item drawn from one texture, like most crafting materials.
     *
     * <p>Expects a texture at {@code assets/<mod>/textures/item/<name>.png}.
     *
     * @param item the item
     */
    protected final void flatItem(Holder<Item> item) {
        String name = item.id().getPath();
        String model = modId() + ":item/" + name;

        output().asset("models/item/" + name + ".json", """
                {
                  "parent": "minecraft:item/generated",
                  "textures": {
                    "layer0": "%s"
                  }
                }
                """.formatted(model));

        itemDefinition(name, model);
    }

    private void itemDefinition(String name, String model) {
        output().asset("items/" + name + ".json", """
                {
                  "model": {
                    "type": "minecraft:model",
                    "model": "%s"
                  }
                }
                """.formatted(model));
    }
}
