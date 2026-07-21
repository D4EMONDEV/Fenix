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

    /** Spawn eggs. */
    public static final ResourceKey<CreativeModeTab> SPAWN_EGGS = vanilla("spawn_eggs");

    /** The search tab. Present on every page — see {@link CreativePages}. */
    public static final ResourceKey<CreativeModeTab> SEARCH = vanilla("search");

    /** The survival inventory tab. Present on every page. */
    public static final ResourceKey<CreativeModeTab> INVENTORY = vanilla("inventory");

    /** Saved hotbars. Present on every page. */
    public static final ResourceKey<CreativeModeTab> HOTBAR = vanilla("hotbar");

    /** Operator blocks, shown only to operators. Present on every page. */
    public static final ResourceKey<CreativeModeTab> OP_BLOCKS = vanilla("op_blocks");

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

    /**
     * {@return the translation key for a tab's title}
     *
     * <p>The one place this is worked out. {@link Registrar#creativeTab} names
     * the tab with it and {@code EmberLanguageProvider} translates it with it,
     * so the two cannot drift apart — a tab showing its raw key in game is the
     * sort of thing that ships unnoticed.
     *
     * @param tab the tab
     */
    public static String titleKey(ResourceKey<CreativeModeTab> tab) {
        Identifier id = Objects.requireNonNull(tab, "tab").identifier();
        return "itemGroup." + id.getNamespace() + "." + id.getPath();
    }

    private static ResourceKey<CreativeModeTab> vanilla(String name) {
        return ResourceKey.create(Registries.CREATIVE_MODE_TAB,
                Identifier.fromNamespaceAndPath("minecraft", name));
    }
}
