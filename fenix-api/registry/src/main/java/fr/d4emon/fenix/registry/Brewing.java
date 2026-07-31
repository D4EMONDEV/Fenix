package fr.d4emon.fenix.registry;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionBrewing;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * What a brewing stand can make.
 *
 * <pre>{@code
 * Brewing.mix(Potions.AWKWARD, ModItems.RUBY, ModPotions.GLIMMERING);
 * }</pre>
 *
 * <p>A potion that nothing brews into can only be given by command, so
 * {@code Registrar.potion} is half of a potion and this is the other half.
 *
 * <p>Vanilla builds its brewing table once per server, from a fixed list, and
 * throws the builder away. There is nothing left to add to afterwards — so a
 * mod's mixes are recorded here and handed to that builder while it is still
 * open, through the same public methods vanilla uses on it. Declaring them
 * before the potions exist is fine: nothing is resolved until the builder asks.
 */
public final class Brewing {

    /** One brewing step, in the shape vanilla's builder takes it. */
    private sealed interface Step {

        void applyTo(PotionBrewing.Builder builder);

        record Mix(Supplier<Potion> from, Supplier<Item> ingredient, Supplier<Potion> to) implements Step {
            @Override
            public void applyTo(PotionBrewing.Builder builder) {
                builder.addMix(BuiltInRegistries.POTION.wrapAsHolder(from.get()), ingredient.get(),
                        BuiltInRegistries.POTION.wrapAsHolder(to.get()));
            }
        }

        record StartMix(Supplier<Item> ingredient, Supplier<Potion> to) implements Step {
            @Override
            public void applyTo(PotionBrewing.Builder builder) {
                builder.addStartMix(ingredient.get(), BuiltInRegistries.POTION.wrapAsHolder(to.get()));
            }
        }

        record Container(Supplier<Item> item) implements Step {
            @Override
            public void applyTo(PotionBrewing.Builder builder) {
                builder.addContainer(item.get());
            }
        }

        record ContainerRecipe(Supplier<Item> from, Supplier<Item> ingredient, Supplier<Item> to)
                implements Step {
            @Override
            public void applyTo(PotionBrewing.Builder builder) {
                builder.addContainerRecipe(from.get(), ingredient.get(), to.get());
            }
        }
    }

    private static final List<Step> STEPS = new ArrayList<>();

    private Brewing() {
    }

    /**
     * Brews one potion into another.
     *
     * <p>The usual shape: an awkward potion plus an ingredient makes something.
     * Vanilla's own potions are reachable through {@code Potions}.
     *
     * @param from       the potion in the bottle
     * @param ingredient what goes in the top slot
     * @param to         what the bottle becomes
     */
    public static void mix(net.minecraft.core.Holder<Potion> from, Holder<Item> ingredient,
                           Holder<Potion> to) {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(ingredient, "ingredient");
        Objects.requireNonNull(to, "to");
        add(new Step.Mix(from::value, ingredient::get, to::get));
    }

    /**
     * Brews a mod's potion from one of a mod's own.
     *
     * @param from       the potion in the bottle
     * @param ingredient what goes in the top slot
     * @param to         what the bottle becomes
     */
    public static void mix(Holder<Potion> from, Holder<Item> ingredient, Holder<Potion> to) {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(ingredient, "ingredient");
        Objects.requireNonNull(to, "to");
        add(new Step.Mix(from::get, ingredient::get, to::get));
    }

    /**
     * Brews a potion straight from water, the way a breeze rod does.
     *
     * @param ingredient what goes in the top slot, over a water bottle
     * @param to         the potion it makes
     */
    public static void startMix(Holder<Item> ingredient, Holder<Potion> to) {
        Objects.requireNonNull(ingredient, "ingredient");
        Objects.requireNonNull(to, "to");
        add(new Step.StartMix(ingredient::get, to::get));
    }

    /**
     * Lets an item hold a potion at all, as a bottle does.
     *
     * @param item the container item
     */
    public static void container(Holder<Item> item) {
        Objects.requireNonNull(item, "item");
        add(new Step.Container(item::get));
    }

    /**
     * Turns one container into another, the way gunpowder makes a splash potion.
     *
     * @param from       the container in the bottle slot
     * @param ingredient what goes in the top slot
     * @param to         what it becomes, keeping the potion inside
     */
    public static void containerRecipe(Holder<Item> from, Holder<Item> ingredient, Holder<Item> to) {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(ingredient, "ingredient");
        Objects.requireNonNull(to, "to");
        add(new Step.ContainerRecipe(from::get, ingredient::get, to::get));
    }

    /**
     * Hands every declared step to a builder that is still open.
     *
     * <p>Called by the mixin on {@code PotionBrewing.Builder.build}. The table is
     * rebuilt whenever the server reloads, and this runs each time, so a mod's
     * mixes survive a reload the way vanilla's do.
     *
     * @param builder the builder about to be built
     */
    public static void applyTo(PotionBrewing.Builder builder) {
        for (Step step : List.copyOf(STEPS)) {
            step.applyTo(builder);
        }
    }

    private static void add(Step step) {
        synchronized (STEPS) {
            STEPS.add(step);
        }
    }
}
