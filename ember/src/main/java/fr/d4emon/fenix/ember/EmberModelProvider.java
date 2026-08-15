package fr.d4emon.fenix.ember;

import fr.d4emon.fenix.registry.Holder;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    // ------------------------------------------------------------------
    // The everyday shapes
    //
    // Each of these borrows its textures from another block — a slab is cut
    // from planks, stairs from the same. That is how vanilla does it, and it
    // means adding a whole family of blocks costs no new artwork.
    //
    // The rotations below are vanilla's own, read out of the game's blockstate
    // files rather than remembered. A block turned the wrong way is not a
    // crash: it renders, facing somewhere else, and the mistake is only
    // visible to somebody who looks at it from the right side.
    // ------------------------------------------------------------------

    /**
     * A cube with a top, a bottom and a shared side — the shape grass has.
     *
     * <p>Expects {@code <name>_top}, {@code <name>_bottom} and {@code <name>_side}.
     *
     * @param block the block
     */
    protected final void cubeBottomTop(Holder<Block> block) {
        String name = block.id().getPath();
        String base = modId() + ":block/" + name;

        writeModel("block/" + name, "minecraft:block/cube_bottom_top", Map.of(
                "top", base + "_top",
                "bottom", base + "_bottom",
                "side", base + "_side"));
        variants(name, List.of(new Variant("", base, 0, 0, false)));
        itemDefinition(name, base);
    }

    /**
     * A block with a face — a furnace, a machine, anything with a front.
     *
     * <p>Expects {@code <name>_front}, {@code <name>_side} and {@code <name>_top}.
     * The block needs a {@code facing} property, which it has if it is built on
     * {@code HorizontalDirectionalBlock}; without one it keeps its default state,
     * matches no variant, and renders as the missing model.
     *
     * @param block the block
     */
    protected final void orientable(Holder<Block> block) {
        String name = block.id().getPath();
        String base = modId() + ":block/" + name;

        writeModel("block/" + name, "minecraft:block/orientable", Map.of(
                "front", base + "_front",
                "side", base + "_side",
                "top", base + "_top"));

        List<Variant> variants = new ArrayList<>();
        // north is the model's own direction, so it is the one with no turn.
        for (String facing : HORIZONTAL) {
            variants.add(new Variant("facing=" + facing, base, 0, yawFrom("north", facing), false));
        }
        variants(name, variants);
        itemDefinition(name, base);
    }

    /**
     * A plant: two crossed squares, no collision, drawn from one texture.
     *
     * @param block the block
     */
    protected final void cross(Holder<Block> block) {
        String name = block.id().getPath();
        String base = modId() + ":block/" + name;

        writeModel("block/" + name, "minecraft:block/cross", Map.of("cross", base));
        variants(name, List.of(new Variant("", base, 0, 0, false)));
        // A plant is placed from a flat item, not from the block model.
        writeModel("item/" + name, "minecraft:item/generated", Map.of("layer0", base));
        itemDefinition(name, modId() + ":item/" + name);
    }

    /**
     * A slab, cut from {@code from}.
     *
     * @param block the slab
     * @param from  the full block it is cut from, whose textures and whole-block
     *              model the doubled slab uses
     */
    protected final void slab(Holder<Block> block, Holder<Block> from) {
        String name = block.id().getPath();
        String base = modId() + ":block/" + name;
        String source = modId() + ":block/" + from.id().getPath();
        Map<String, String> textures = Map.of("bottom", source, "top", source, "side", source);

        writeModel("block/" + name, "minecraft:block/slab", textures);
        writeModel("block/" + name + "_top", "minecraft:block/slab_top", textures);

        variants(name, List.of(
                new Variant("type=bottom", base, 0, 0, false),
                new Variant("type=double", source, 0, 0, false),
                new Variant("type=top", base + "_top", 0, 0, false)));
        itemDefinition(name, base);
    }

    /**
     * Stairs, cut from {@code from}.
     *
     * <p>Forty variants: four directions, two halves, and five shapes for the
     * corners stairs make against each other.
     *
     * @param block the stairs
     * @param from  the full block whose textures they take
     */
    protected final void stairs(Holder<Block> block, Holder<Block> from) {
        String name = block.id().getPath();
        String base = modId() + ":block/" + name;
        String source = modId() + ":block/" + from.id().getPath();
        Map<String, String> textures = Map.of("bottom", source, "top", source, "side", source);

        writeModel("block/" + name, "minecraft:block/stairs", textures);
        writeModel("block/" + name + "_inner", "minecraft:block/inner_stairs", textures);
        writeModel("block/" + name + "_outer", "minecraft:block/outer_stairs", textures);

        List<Variant> variants = new ArrayList<>();
        for (String facing : HORIZONTAL) {
            int base360 = yawFrom("east", facing);
            for (String half : List.of("bottom", "top")) {
                boolean top = half.equals("top");
                for (String shape : List.of("straight", "inner_left", "inner_right",
                        "outer_left", "outer_right")) {
                    String suffix = shape.startsWith("inner") ? "_inner"
                            : shape.startsWith("outer") ? "_outer" : "";
                    // Bottom turns back for a left corner; top turns on for a
                    // right one. Straight follows whichever side it is on.
                    boolean left = shape.endsWith("_left");
                    boolean right = shape.endsWith("_right");
                    int turn = top ? (right ? 90 : 0) : (left ? -90 : 0);
                    int y = Math.floorMod(base360 + turn, 360);
                    int x = top ? 180 : 0;
                    variants.add(new Variant(
                            "facing=" + facing + ",half=" + half + ",shape=" + shape,
                            base + suffix, x, y, x != 0 || y != 0));
                }
            }
        }
        variants(name, variants);
        itemDefinition(name, base);
    }

    /**
     * A fence, built from {@code from}.
     *
     * <p>Multipart rather than variants: a fence is a post plus an arm for each
     * neighbour it connects to, and listing every combination would be sixteen
     * entries saying the same four things.
     *
     * @param block the fence
     * @param from  the block whose texture it takes
     */
    protected final void fence(Holder<Block> block, Holder<Block> from) {
        String name = block.id().getPath();
        String base = modId() + ":block/" + name;
        String source = modId() + ":block/" + from.id().getPath();

        writeModel("block/" + name + "_post", "minecraft:block/fence_post", Map.of("texture", source));
        writeModel("block/" + name + "_side", "minecraft:block/fence_side", Map.of("texture", source));
        // The item is the inventory shape, which is neither the post nor a side.
        writeModel("block/" + name + "_inventory", "minecraft:block/fence_inventory",
                Map.of("texture", source));

        List<Part> parts = new ArrayList<>();
        parts.add(new Part("", base + "_post", 0, false));
        for (String side : HORIZONTAL) {
            parts.add(new Part("\"" + side + "\": \"true\"", base + "_side",
                    yawFrom("north", side), true));
        }
        multipart(name, parts);
        itemDefinition(name, base + "_inventory");
    }

    /**
     * A fence gate, built from {@code from}.
     *
     * @param block the gate
     * @param from  the block whose texture it takes
     */
    protected final void fenceGate(Holder<Block> block, Holder<Block> from) {
        String name = block.id().getPath();
        String base = modId() + ":block/" + name;
        Map<String, String> texture = Map.of("texture", modId() + ":block/" + from.id().getPath());

        writeModel("block/" + name, "minecraft:block/template_fence_gate", texture);
        writeModel("block/" + name + "_open", "minecraft:block/template_fence_gate_open", texture);
        writeModel("block/" + name + "_wall", "minecraft:block/template_fence_gate_wall", texture);
        writeModel("block/" + name + "_wall_open", "minecraft:block/template_fence_gate_wall_open",
                texture);

        List<Variant> variants = new ArrayList<>();
        for (String facing : HORIZONTAL) {
            int y = yawFrom("south", facing);
            for (boolean inWall : List.of(false, true)) {
                for (boolean open : List.of(false, true)) {
                    String suffix = (inWall ? "_wall" : "") + (open ? "_open" : "");
                    variants.add(new Variant(
                            "facing=" + facing + ",in_wall=" + inWall + ",open=" + open,
                            base + suffix, 0, y, true));
                }
            }
        }
        variants(name, variants);
        itemDefinition(name, base);
    }

    /**
     * A wall, built from {@code from}.
     *
     * @param block the wall
     * @param from  the block whose texture it takes
     */
    protected final void wall(Holder<Block> block, Holder<Block> from) {
        String name = block.id().getPath();
        String base = modId() + ":block/" + name;
        Map<String, String> texture = Map.of("wall", modId() + ":block/" + from.id().getPath());

        writeModel("block/" + name + "_post", "minecraft:block/template_wall_post", texture);
        writeModel("block/" + name + "_side", "minecraft:block/template_wall_side", texture);
        writeModel("block/" + name + "_side_tall", "minecraft:block/template_wall_side_tall", texture);
        writeModel("block/" + name + "_inventory", "minecraft:block/wall_inventory", texture);

        List<Part> parts = new ArrayList<>();
        parts.add(new Part("\"up\": \"true\"", base + "_post", 0, false));
        // A wall's sides are three-valued — none, low, tall — rather than the
        // boolean a fence has, because a wall grows to meet what is above it.
        for (String height : List.of("low", "tall")) {
            String suffix = height.equals("low") ? "_side" : "_side_tall";
            for (String side : HORIZONTAL) {
                parts.add(new Part("\"" + side + "\": \"" + height + "\"", base + suffix,
                        yawFrom("north", side), true));
            }
        }
        multipart(name, parts);
        itemDefinition(name, base + "_inventory");
    }

    /**
     * A trapdoor, built from its own texture.
     *
     * <p>Expects {@code textures/block/<name>.png}.
     *
     * @param block the trapdoor
     */
    protected final void trapdoor(Holder<Block> block) {
        trapdoor(block, block);
    }

    /**
     * A trapdoor drawn from another block's texture.
     *
     * <p>Vanilla's trapdoors have a texture of their own, with the slats and
     * hinges drawn in, which is why {@link #trapdoor(Holder)} looks for one
     * under the trapdoor's own name. This overload is for a trapdoor cut from a
     * material it shares with a full block, the way every other shape here
     * borrows.
     *
     * @param block the trapdoor
     * @param from  the block whose texture it takes
     */
    protected final void trapdoor(Holder<Block> block, Holder<Block> from) {
        String name = block.id().getPath();
        String base = modId() + ":block/" + name;
        Map<String, String> texture = Map.of("texture", modId() + ":block/" + from.id().getPath());

        writeModel("block/" + name + "_bottom", "minecraft:block/template_trapdoor_bottom", texture);
        writeModel("block/" + name + "_top", "minecraft:block/template_trapdoor_top", texture);
        writeModel("block/" + name + "_open", "minecraft:block/template_trapdoor_open", texture);

        List<Variant> variants = new ArrayList<>();
        for (String facing : HORIZONTAL) {
            for (String half : List.of("bottom", "top")) {
                // Shut, a trapdoor is a flat panel: vanilla leaves it unturned
                // whichever way it faces, and only the open one is rotated.
                variants.add(new Variant("facing=" + facing + ",half=" + half + ",open=false",
                        base + "_" + half, 0, 0, false));
                variants.add(new Variant("facing=" + facing + ",half=" + half + ",open=true",
                        base + "_open", 0, yawFrom("north", facing), false));
            }
        }
        variants(name, variants);
        itemDefinition(name, base + "_bottom");
    }

    /**
     * A button, built from {@code from}.
     *
     * @param block the button
     * @param from  the block whose texture it takes
     */
    protected final void button(Holder<Block> block, Holder<Block> from) {
        String name = block.id().getPath();
        String base = modId() + ":block/" + name;
        Map<String, String> texture = Map.of("texture", modId() + ":block/" + from.id().getPath());

        writeModel("block/" + name, "minecraft:block/button", texture);
        writeModel("block/" + name + "_pressed", "minecraft:block/button_pressed", texture);
        writeModel("block/" + name + "_inventory", "minecraft:block/button_inventory", texture);

        List<Variant> variants = new ArrayList<>();
        for (String face : List.of("ceiling", "floor", "wall")) {
            for (String facing : HORIZONTAL) {
                for (boolean powered : List.of(false, true)) {
                    int x = switch (face) {
                        case "ceiling" -> 180;
                        case "wall" -> 90;
                        default -> 0;
                    };
                    // A button on a ceiling is upside down, so its facing turns
                    // the other way round.
                    // Upside down on a ceiling, so its facing turns the other
                    // way round.
                    int y = face.equals("ceiling")
                            ? Math.floorMod(yawFrom("north", facing) + 180, 360)
                            : yawFrom("north", facing);
                    boolean uvlock = face.equals("wall");
                    variants.add(new Variant(
                            "face=" + face + ",facing=" + facing + ",powered=" + powered,
                            base + (powered ? "_pressed" : ""), x, y, uvlock));
                }
            }
        }
        variants(name, variants);
        itemDefinition(name, base + "_inventory");
    }

    /**
     * A pressure plate, built from {@code from}.
     *
     * @param block the plate
     * @param from  the block whose texture it takes
     */
    protected final void pressurePlate(Holder<Block> block, Holder<Block> from) {
        String name = block.id().getPath();
        String base = modId() + ":block/" + name;
        Map<String, String> texture = Map.of("texture", modId() + ":block/" + from.id().getPath());

        writeModel("block/" + name, "minecraft:block/pressure_plate_up", texture);
        writeModel("block/" + name + "_down", "minecraft:block/pressure_plate_down", texture);

        variants(name, List.of(
                new Variant("powered=false", base, 0, 0, false),
                new Variant("powered=true", base + "_down", 0, 0, false)));
        itemDefinition(name, base);
    }

    /**
     * A door: two blocks tall, hinged on either side, and open or shut.
     *
     * <p>Thirty-two states, and eight models to answer them. Expects three
     * textures: {@code <name>_bottom} and {@code <name>_top} for the two halves
     * of the block, and {@code textures/item/<name>.png} for the item, which is
     * flat and does not look like either.
     *
     * <p>The block needs {@code facing}, {@code half}, {@code hinge} and
     * {@code open}, which it has if it is built on {@code DoorBlock}.
     *
     * @param block the door
     */
    protected final void door(Holder<Block> block) {
        String name = block.id().getPath();
        String base = modId() + ":block/" + name;
        Map<String, String> textures = Map.of("bottom", base + "_bottom", "top", base + "_top");

        for (String half : List.of("bottom", "top")) {
            for (String hinge : List.of("left", "right")) {
                for (String open : List.of("", "_open")) {
                    String part = half + "_" + hinge + open;
                    writeModel("block/" + name + "_" + part,
                            "minecraft:block/door_" + part, textures);
                }
            }
        }

        List<Variant> variants = new ArrayList<>();
        for (String facing : HORIZONTAL) {
            int shut = yawFrom("east", facing);
            for (String half : List.of("lower", "upper")) {
                // The blockstate says lower and upper; the models say bottom
                // and top. Vanilla's own naming, and not worth ironing out —
                // the model names have to match the templates being parented.
                String modelHalf = half.equals("lower") ? "bottom" : "top";
                for (String hinge : List.of("left", "right")) {
                    for (boolean open : List.of(false, true)) {
                        // Swinging left turns one way, right the other, from
                        // wherever the shut door faces.
                        int turn = !open ? 0 : hinge.equals("left") ? 90 : 270;
                        variants.add(new Variant(
                                "facing=" + facing + ",half=" + half + ",hinge=" + hinge
                                        + ",open=" + open,
                                base + "_" + modelHalf + "_" + hinge + (open ? "_open" : ""),
                                0, Math.floorMod(shut + turn, 360), false));
                    }
                }
            }
        }
        variants(name, variants);

        // A door in the hand is a flat picture of one, from a texture of its
        // own: neither half of the block reads as a door at item size.
        String item = modId() + ":item/" + name;
        writeModel("item/" + name, "minecraft:item/generated", Map.of("layer0", item));
        itemDefinition(name, item);
    }

    /**
     * An item held like a tool, drawn at an angle in the hand.
     *
     * <p>The difference from {@link #flatItem} is the pose, not the texture: a
     * pickaxe drawn as a flat item is held flat, which reads as a bug.
     *
     * @param item the item
     */
    protected final void handheldItem(Holder<Item> item) {
        String name = item.id().getPath();
        String texture = modId() + ":item/" + name;

        writeModel("item/" + name, "minecraft:item/handheld", Map.of("layer0", texture));
        itemDefinition(name, texture);
    }

    /**
     * The four horizontal directions, clockwise from north.
     *
     * <p>Clockwise matters: every rotation below is worked out as the turn from
     * one of these to another, and that arithmetic only holds if the order is
     * the one the game's own y rotations follow.
     */
    private static final List<String> HORIZONTAL = List.of("north", "east", "south", "west");

    /**
     * {@return the y rotation that turns a model facing {@code zero} to face
     * {@code facing}}
     *
     * <p>Models are drawn facing one particular way and the blockstate turns
     * them, so which way counts as no turn differs per shape: stairs are drawn
     * facing east, a fence gate facing south, a furnace facing north. Those are
     * read out of the game's own files, not remembered — two of them were
     * remembered wrongly first.
     *
     * @param zero   the direction the model is drawn facing
     * @param facing the direction it should end up facing
     */
    private static int yawFrom(String zero, String facing) {
        return Math.floorMod((HORIZONTAL.indexOf(facing) - HORIZONTAL.indexOf(zero)) * 90, 360);
    }

    /** One line of a {@code variants} blockstate: a state, and how to turn the model. */
    private record Variant(String state, String model, int x, int y, boolean uvlock) {
    }

    /** One case of a {@code multipart} blockstate: when to add a model, and how to turn it. */
    private record Part(String when, String model, int y, boolean uvlock) {
    }

    /** Writes a model file: a parent, and the textures that fill it in. */
    private void writeModel(String path, String parent, Map<String, String> textures) {
        String entries = textures.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> "    \"%s\": \"%s\"".formatted(entry.getKey(), entry.getValue()))
                .collect(Collectors.joining(",\n"));
        output().asset("models/" + path + ".json", """
                {
                  "parent": "%s",
                  "textures": {
                %s
                  }
                }
                """.formatted(parent, entries));
    }

    /** Writes a blockstate that picks one model per state. */
    private void variants(String name, List<Variant> variants) {
        String body = variants.stream()
                .map(variant -> "    \"%s\": {\n%s\n    }".formatted(
                        variant.state(), placement(variant.model(), variant.x(), variant.y(),
                                variant.uvlock(), "      ")))
                .collect(Collectors.joining(",\n"));
        output().asset("blockstates/" + name + ".json", """
                {
                  "variants": {
                %s
                  }
                }
                """.formatted(body));
    }

    /** Writes a blockstate that adds up models — a post, and an arm per neighbour. */
    private void multipart(String name, List<Part> parts) {
        String body = parts.stream()
                .map(part -> {
                    String apply = "      \"apply\": {\n%s\n      }".formatted(
                            placement(part.model(), 0, part.y(), part.uvlock(), "        "));
                    return part.when().isEmpty()
                            ? "    {\n%s\n    }".formatted(apply)
                            : "    {\n%s,\n      \"when\": { %s }\n    }".formatted(apply, part.when());
                })
                .collect(Collectors.joining(",\n"));
        output().asset("blockstates/" + name + ".json", """
                {
                  "multipart": [
                %s
                  ]
                }
                """.formatted(body));
    }

    /**
     * {@return the model, and any turn it needs, as blockstate fields}
     *
     * <p>A zero rotation is left out rather than written as {@code 0}: the game
     * treats a missing field and a zero the same, and writing it would make
     * every generated file differ from the vanilla one it was modelled on for
     * no reason.
     */
    private static String placement(String model, int x, int y, boolean uvlock, String indent) {
        List<String> fields = new ArrayList<>();
        fields.add(indent + "\"model\": \"" + model + "\"");
        if (x != 0) {
            fields.add(indent + "\"x\": " + x);
        }
        if (y != 0) {
            fields.add(indent + "\"y\": " + y);
        }
        if (uvlock) {
            fields.add(indent + "\"uvlock\": true");
        }
        return String.join(",\n", fields);
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
