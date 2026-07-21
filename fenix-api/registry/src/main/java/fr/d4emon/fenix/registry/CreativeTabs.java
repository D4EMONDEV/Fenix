package fr.d4emon.fenix.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Puts a mod's content in the creative menu.
 *
 * <pre>{@code
 * CreativeTabs.addTo(CreativeTabs.BUILDING_BLOCKS, ModBlocks.RUBY_BLOCK);
 * CreativeTabs.addTo(CreativeTabs.INGREDIENTS, ModItems.RUBY);
 * }</pre>
 *
 * <p>Without this, content is registered but unreachable in game except through
 * {@code /give} — which is the difference between a mod a player can use and
 * one they cannot.
 *
 * <p>Call it from {@code onRegister}, alongside the content itself. The
 * additions are recorded and applied whenever the game builds a tab's contents,
 * which it does on every resource reload rather than once.
 *
 * <p>Vanilla's own tab keys are private, so the ones below are rebuilt from
 * their ids. Any other tab — including another mod's — works by passing its
 * key directly.
 */
public final class CreativeTabs {

    /** Stone, planks, and the rest of what you build with. */
    public static final ResourceKey<CreativeModeTab> BUILDING_BLOCKS = vanilla("building_blocks");

    /** Decorative blocks: glass, carpets, plants. */
    public static final ResourceKey<CreativeModeTab> COLORED_BLOCKS = vanilla("colored_blocks");

    /** Natural blocks: dirt, ore, wood. */
    public static final ResourceKey<CreativeModeTab> NATURAL_BLOCKS = vanilla("natural_blocks");

    /** Blocks that do something: chests, hoppers, furnaces. */
    public static final ResourceKey<CreativeModeTab> FUNCTIONAL_BLOCKS = vanilla("functional_blocks");

    /** Redstone components. */
    public static final ResourceKey<CreativeModeTab> REDSTONE_BLOCKS = vanilla("redstone_blocks");

    /** Tools and utilities. */
    public static final ResourceKey<CreativeModeTab> TOOLS_AND_UTILITIES = vanilla("tools_and_utilities");

    /** Weapons and armour. */
    public static final ResourceKey<CreativeModeTab> COMBAT = vanilla("combat");

    /** Food and drink. */
    public static final ResourceKey<CreativeModeTab> FOOD_AND_DRINKS = vanilla("food_and_drinks");

    /** Crafting materials — where most new items belong. */
    public static final ResourceKey<CreativeModeTab> INGREDIENTS = vanilla("ingredients");

    /** Spawn eggs and operator tools. */
    public static final ResourceKey<CreativeModeTab> SPAWN_EGGS = vanilla("spawn_eggs");

    /** Declared additions, in the order they were declared. */
    private static final Map<ResourceKey<CreativeModeTab>, List<Holder<?>>> ADDITIONS =
            new LinkedHashMap<>();

    private CreativeTabs() {
    }

    /**
     * Adds content to a creative tab.
     *
     * @param tab     which tab
     * @param content registered blocks or items; a block contributes the item
     *                that places it
     * @throws NullPointerException if anything is {@code null}
     */
    public static void addTo(ResourceKey<CreativeModeTab> tab, Holder<?>... content) {
        Objects.requireNonNull(tab, "tab");
        List<Holder<?>> entries = ADDITIONS.computeIfAbsent(tab, key -> new ArrayList<>());
        for (Holder<?> entry : content) {
            entries.add(Objects.requireNonNull(entry, "content"));
        }
    }

    /**
     * {@return what has been added to a tab, in declaration order}
     *
     * <p>Read by the mixin that applies these. Not meant for mods.
     *
     * @param tab which tab
     */
    public static List<Holder<?>> additionsFor(ResourceKey<CreativeModeTab> tab) {
        return ADDITIONS.getOrDefault(tab, List.of());
    }

    private static ResourceKey<CreativeModeTab> vanilla(String name) {
        return ResourceKey.create(Registries.CREATIVE_MODE_TAB,
                Identifier.fromNamespaceAndPath("minecraft", name));
    }
}
