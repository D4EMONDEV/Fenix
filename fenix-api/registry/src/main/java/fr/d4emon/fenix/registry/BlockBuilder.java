package fr.d4emon.fenix.registry;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * Describes a block, then registers it.
 *
 * <pre>{@code
 * public static final Holder<Block> RUBY_BLOCK = REGISTRAR.newBlock("ruby_block")
 *         .strength(3f)
 *         .requiresTool()
 *         .withItem()
 *         .register();
 * }</pre>
 *
 * <p>The methods here cover what most blocks need. Anything else is reachable
 * through {@link #properties(UnaryOperator)}, which hands you vanilla's own
 * builder — this is a shortcut over that API, never a wall in front of it.
 */
public final class BlockBuilder {

    private final Registrar registrar;
    private final String name;

    private UnaryOperator<BlockBehaviour.Properties> properties = UnaryOperator.identity();
    private Function<BlockBehaviour.Properties, Block> factory = Block::new;
    private boolean withItem;

    BlockBuilder(Registrar registrar, String name) {
        this.registrar = registrar;
        this.name = name;
    }

    /**
     * Sets how long it takes to break, and how well it resists explosions.
     *
     * @param destroyTime         mining time; higher is slower
     * @param explosionResistance blast resistance
     * @return this builder
     */
    public BlockBuilder strength(float destroyTime, float explosionResistance) {
        return properties(props -> props.strength(destroyTime, explosionResistance));
    }

    /**
     * Sets breaking time and explosion resistance to the same value.
     *
     * @param strength both values
     * @return this builder
     */
    public BlockBuilder strength(float strength) {
        return properties(props -> props.strength(strength));
    }

    /**
     * Makes the block break instantly, like grass.
     *
     * @return this builder
     */
    public BlockBuilder instabreak() {
        return properties(BlockBehaviour.Properties::instabreak);
    }

    /**
     * Requires the right tool to drop anything, like ore.
     *
     * @return this builder
     */
    public BlockBuilder requiresTool() {
        return properties(BlockBehaviour.Properties::requiresCorrectToolForDrops);
    }

    /**
     * Emits light.
     *
     * @param level 0 to 15
     * @return this builder
     */
    public BlockBuilder lightLevel(int level) {
        return properties(props -> props.lightLevel(state -> level));
    }

    /**
     * Lets you see through it, like glass.
     *
     * @return this builder
     */
    public BlockBuilder noOcclusion() {
        return properties(BlockBehaviour.Properties::noOcclusion);
    }

    /**
     * Sets the sounds it makes.
     *
     * @param sound the sound type
     * @return this builder
     */
    public BlockBuilder sound(SoundType sound) {
        return properties(props -> props.sound(sound));
    }

    /**
     * Applies anything else vanilla's builder offers.
     *
     * @param step what to do to the properties
     * @return this builder
     */
    public BlockBuilder properties(UnaryOperator<BlockBehaviour.Properties> step) {
        Objects.requireNonNull(step, "step");
        UnaryOperator<BlockBehaviour.Properties> previous = properties;
        properties = props -> step.apply(previous.apply(props));
        return this;
    }

    /**
     * Builds a subclass instead of a plain {@link Block}.
     *
     * @param blockFactory builds the block from the finished properties
     * @return this builder
     */
    public BlockBuilder from(Function<BlockBehaviour.Properties, Block> blockFactory) {
        this.factory = Objects.requireNonNull(blockFactory, "blockFactory");
        return this;
    }

    /**
     * Also registers the item that places this block — what you want unless the
     * block is only ever placed by code.
     *
     * @return this builder
     */
    public BlockBuilder withItem() {
        this.withItem = true;
        return this;
    }

    /**
     * Declares the block. Registration itself happens at {@code apply()}.
     *
     * @return a handle on the block
     */
    public Holder<Block> register() {
        Function<BlockBehaviour.Properties, Block> built = props -> factory.apply(properties.apply(props));
        return withItem
                ? registrar.blockWithItem(name, built)
                : registrar.block(name, built);
    }
}
