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
     * A pillar: one texture on the ends, another around the sides — the shape a
     * log has.
     *
     * <p>Expects {@code textures/block/<name>.png} for the sides and
     * {@code textures/block/<name>_top.png} for the ends.
     *
     * <p>Writes two models and a three-way blockstate, because a pillar laid on
     * its side is the upright model turned rather than a model of its own. The
     * block has to carry an {@code axis} property for that blockstate to match —
     * which it does if it is built on {@code RotatedPillarBlock}. A block
     * without one keeps its default state, finds no matching variant, and
     * renders as the missing model.
     *
     * @param block the block
     */
    protected final void cubeColumn(Holder<Block> block) {
        String name = block.id().getPath();
        String upright = modId() + ":block/" + name;
        String horizontal = upright + "_horizontal";

        String textures = """
                  "textures": {
                    "end": "%s_top",
                    "side": "%s"
                  }
                """.formatted(upright, upright);

        output().asset("models/block/" + name + ".json", """
                {
                  "parent": "minecraft:block/cube_column",
                %s}
                """.formatted(textures));

        output().asset("models/block/" + name + "_horizontal.json", """
                {
                  "parent": "minecraft:block/cube_column_horizontal",
                %s}
                """.formatted(textures));

        // The rotations are vanilla's own: a pillar along x is the horizontal
        // model turned twice, along z once, and upright needs none.
        output().asset("blockstates/" + name + ".json", """
                {
                  "variants": {
                    "axis=x": {
                      "model": "%s",
                      "x": 90,
                      "y": 90
                    },
                    "axis=y": {
                      "model": "%s"
                    },
                    "axis=z": {
                      "model": "%s",
                      "x": 90
                    }
                  }
                }
                """.formatted(horizontal, upright, horizontal));

        itemDefinition(name, upright);
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
