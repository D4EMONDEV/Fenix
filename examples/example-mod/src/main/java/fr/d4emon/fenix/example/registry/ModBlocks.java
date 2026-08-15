package fr.d4emon.fenix.example.registry;

import fr.d4emon.fenix.example.block.RubyReforgingBlock;
import fr.d4emon.fenix.example.block.RubySafeBlock;
import fr.d4emon.fenix.example.block.RubyTallyBlock;

import fr.d4emon.fenix.registry.Holder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.sounds.SoundEvents;

/**
 * The mod's blocks.
 *
 * <p>Each is a {@code static final} field describing what it is; the actual
 * registration happens later, when {@link ModContent#register()} runs. That is
 * why the type is {@link Holder} rather than {@link Block}: the block does not
 * exist yet at the moment this class is initialised.
 */
public final class ModBlocks {

    /** A decorative ore-style block, as hard as iron and needing a pickaxe. */
    public static final Holder<Block> RUBY_BLOCK = ModContent.REGISTRAR.newBlock("ruby_block")
            .strength(3f, 6f)
            .requiresTool()
            .sound(SoundType.METAL)
            .withItem()
            .register();

    /** The same, but glowing — enough to show a second block works. */
    public static final Holder<Block> GLOWING_RUBY_BLOCK = ModContent.REGISTRAR.newBlock("glowing_ruby_block")
            .strength(3f, 6f)
            .requiresTool()
            .lightLevel(10)
            .sound(SoundType.METAL)
            .withItem()
            .register();

    /** Counts how often it is used, which is what its block entity stores. */
    public static final Holder<Block> RUBY_TALLY = ModContent.REGISTRAR.newBlock("ruby_tally")
            .strength(3f, 6f)
            .requiresTool()
            .sound(SoundType.METAL)
            .from(RubyTallyBlock::new)
            .withItem()
            .register();

    /** Holds things, which is the point of a menu. */
    public static final Holder<Block> RUBY_SAFE = ModContent.REGISTRAR.newBlock("ruby_safe")
            .strength(4f, 12f)
            .requiresTool()
            .sound(SoundType.METAL)
            .from(RubySafeBlock::new)
            .withItem()
            .register();

    /** Runs a recipe of the mod's own type: put an item in, take a reforged one out. */
    public static final Holder<Block> RUBY_REFORGING = ModContent.REGISTRAR.newBlock("ruby_reforging")
            .strength(4f, 12f)
            .requiresTool()
            .sound(SoundType.METAL)
            .from(RubyReforgingBlock::new)
            .withItem()
            .register();

    /**
     * A log, so there is something for an axe to strip and a fire to catch.
     *
     * <p>{@code RotatedPillarBlock} is what gives it an axis, which stripping
     * carries across so a sideways log stays sideways.
     */
    public static final Holder<Block> RUBY_LOG = ModContent.REGISTRAR.newBlock("ruby_log")
            .strength(2f)
            .sound(SoundType.WOOD)
            .from(RotatedPillarBlock::new)
            .withItem()
            .register();

    /** What it strips into. */
    public static final Holder<Block> STRIPPED_RUBY_LOG =
            ModContent.REGISTRAR.newBlock("stripped_ruby_log")
                    .strength(2f)
                    .sound(SoundType.WOOD)
                    .from(RotatedPillarBlock::new)
                    .withItem()
                    .register();

    /** Ruby ore as it appears in stone. */
    public static final Holder<Block> RUBY_ORE = ModContent.REGISTRAR.newBlock("ruby_ore")
            .strength(3f, 3f)
            .requiresTool()
            .withItem()
            .register();

    /**
     * And as it appears in deepslate.
     *
     * <p>A separate block, not a variant: the two replace different blocks
     * during generation, and an ore that skips this one shows stone-textured
     * lumps below y=0.
     */
    public static final Holder<Block> DEEPSLATE_RUBY_ORE =
            ModContent.REGISTRAR.newBlock("deepslate_ruby_ore")
                    .strength(4.5f, 3f)
                    .requiresTool()
                    .sound(SoundType.DEEPSLATE)
                    .withItem()
                    .register();

    /**
     * The four shapes a decorative block usually grows into.
     *
     * <p>All of them take their texture from {@link #RUBY_BLOCK}, which is how
     * vanilla does it — a slab is cut from planks and uses the planks texture —
     * and means a family of blocks costs no new artwork.
     *
     * <p>They are here to be generated for: a slab is the simple case, a fence
     * is multipart with boolean sides, a wall is multipart with three-valued
     * ones, and a gate is sixteen rotated variants. Between them they cover
     * every shape the blockstate writer has to get right.
     */
    public static final Holder<Block> RUBY_SLAB = ModContent.REGISTRAR
            .newBlock("ruby_slab")
            .strength(3.0f, 6.0f)
            .requiresTool()
            .sound(SoundType.METAL)
            .from(SlabBlock::new)
            .withItem()
            .register();

    /** A fence of ruby, which connects to its neighbours. */
    public static final Holder<Block> RUBY_FENCE = ModContent.REGISTRAR
            .newBlock("ruby_fence")
            .strength(3.0f, 6.0f)
            .requiresTool()
            .sound(SoundType.METAL)
            .from(FenceBlock::new)
            .withItem()
            .register();

    /** A wall of ruby: like a fence, but it grows tall to meet what is above. */
    public static final Holder<Block> RUBY_WALL = ModContent.REGISTRAR
            .newBlock("ruby_wall")
            .strength(3.0f, 6.0f)
            .requiresTool()
            .sound(SoundType.METAL)
            .from(WallBlock::new)
            .withItem()
            .register();

    /** A gate of ruby. The wood type decides the sound it makes, and nothing else. */
    public static final Holder<Block> RUBY_GATE = ModContent.REGISTRAR
            .newBlock("ruby_gate")
            .strength(3.0f, 6.0f)
            .requiresTool()
            .sound(SoundType.METAL)
            .from(properties -> new FenceGateBlock(WoodType.OAK, properties))
            .withItem()
            .register();

    /**
     * The character the door, trapdoor, button and plate share.
     *
     * <p>Declared here rather than borrowed: every vanilla set belongs to a
     * wood or a metal that is not ruby, and the two that sound right — iron and
     * copper — carry opinions with them. Iron's is that a hand cannot open it.
     *
     * <p>Registered eagerly, because the four blocks below are built from it
     * while this class is still loading.
     */
    public static final BlockSetType RUBY_SET = ModContent.REGISTRAR.blockSetType(
            "ruby", true, SoundType.METAL,
            SoundEvents.IRON_DOOR_CLOSE, SoundEvents.IRON_DOOR_OPEN,
            SoundEvents.IRON_TRAPDOOR_CLOSE, SoundEvents.IRON_TRAPDOOR_OPEN,
            SoundEvents.STONE_PRESSURE_PLATE_CLICK_OFF,
            SoundEvents.STONE_PRESSURE_PLATE_CLICK_ON,
            SoundEvents.STONE_BUTTON_CLICK_OFF, SoundEvents.STONE_BUTTON_CLICK_ON);

    /**
     * The four shapes whose constructors Minecraft keeps to itself.
     *
     * <p>{@code StairBlock}, {@code ButtonBlock}, {@code TrapDoorBlock} and
     * {@code PressurePlateBlock} are all {@code protected}, so a mod cannot
     * call them without either subclassing each one for no reason or opening
     * the door — which is what the {@code accessible} entries in this mod's
     * manifest do. The loader widens them at run time and the Gradle plugin
     * widens the copy compiled against, so javac and the game agree.
     *
     * <p>They are here because between them and the four above, every shape the
     * blockstate writer produces is generated and compared against the vanilla
     * block it is modelled on.
     */
    public static final Holder<Block> RUBY_STAIRS = ModContent.REGISTRAR
            .newBlock("ruby_stairs")
            .strength(3.0f, 6.0f)
            .requiresTool()
            .sound(SoundType.METAL)
            // Safe to read here: content is built in declaration order when the
            // registrar is applied, and the ruby block is declared above.
            .from(properties -> new StairBlock(RUBY_BLOCK.get().defaultBlockState(), properties))
            .withItem()
            .register();

    /**
     * A trapdoor of ruby.
     *
     * <p>Built from the mod's own set type, so it opens by hand and sounds
     * like ruby rather than like whichever vanilla metal was nearest.
     */
    public static final Holder<Block> RUBY_TRAPDOOR = ModContent.REGISTRAR
            .newBlock("ruby_trapdoor")
            .strength(3.0f, 6.0f)
            .requiresTool()
            .noOcclusion()
            .sound(SoundType.METAL)
            .from(properties -> new TrapDoorBlock(RUBY_SET, properties))
            .withItem()
            .register();

    /** A button of ruby, which stays pressed for the ticks given here. */
    public static final Holder<Block> RUBY_BUTTON = ModContent.REGISTRAR
            .newBlock("ruby_button")
            .instabreak()
            .noOcclusion()
            .sound(SoundType.METAL)
            .from(properties -> new ButtonBlock(RUBY_SET, 20, properties))
            .withItem()
            .register();

    /** A pressure plate of ruby. */
    public static final Holder<Block> RUBY_PLATE = ModContent.REGISTRAR
            .newBlock("ruby_plate")
            .instabreak()
            .noOcclusion()
            .sound(SoundType.METAL)
            .from(properties -> new PressurePlateBlock(RUBY_SET, properties))
            .withItem()
            .register();

    /**
     * A door of ruby: two blocks tall, and the last shape the model provider
     * had no answer for.
     *
     * <p>{@code COPPER} for the same reason as the trapdoor: it opens by hand.
     *
     * <p>Unlike the shapes above it does not borrow a texture. A door is drawn
     * from a top half, a bottom half and a flat picture for the item, none of
     * which look like a full block — so it is here with those three textures
     * rather than cut from the ruby block.
     */
    public static final Holder<Block> RUBY_DOOR = ModContent.REGISTRAR
            .newBlock("ruby_door")
            .strength(3.0f, 6.0f)
            .requiresTool()
            .noOcclusion()
            .sound(SoundType.METAL)
            .from(properties -> new DoorBlock(RUBY_SET, properties))
            .withItem()
            .register();

    private ModBlocks() {
    }

    /** Loads this class, which is what runs the declarations above. */
    static void load() {
    }
}
