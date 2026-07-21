package fr.d4emon.fenix.registry;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Pages of creative tabs.
 *
 * <p>Vanilla has no such thing: the tab strip is a fixed grid of two rows by
 * seven columns, and vanilla's own tabs fill all fourteen. A mod tab has
 * literally nowhere to go, so Fenix adds the pages that make room.
 *
 * <p>Page 0 is vanilla's, untouched — someone who installs a mod should still
 * find the menu exactly where they left it. Mod tabs start on page 1.
 *
 * <p>Four of vanilla's tabs come along to every page: search, inventory,
 * hotbar and op blocks. They are tools rather than categories, and a player who
 * turns the page to find a mod's blocks should not lose the search box to do
 * it. They sit in columns 5 and 6 of both rows, which is exactly why mod tabs
 * get {@value #TABS_PER_PAGE} slots and not fourteen.
 */
public final class CreativePages {

    /** Columns 0 to 4 of both rows — what is left once the tools are seated. */
    public static final int TABS_PER_PAGE = 10;

    /** Half a page: the point where slots move from the top row to the bottom. */
    private static final int SLOTS_PER_ROW = TABS_PER_PAGE / 2;

    /**
     * The tabs that follow the player from page to page.
     *
     * <p>Not a category between them: they are how you search, what you are
     * carrying, and what an operator can reach.
     */
    private static final Set<ResourceKey<CreativeModeTab>> ALWAYS_VISIBLE = Set.of(
            CreativeTabs.SEARCH,
            CreativeTabs.INVENTORY,
            CreativeTabs.HOTBAR,
            CreativeTabs.OP_BLOCKS);

    private static int current;
    private static int claimed;

    private CreativePages() {
    }

    /**
     * {@return the next free slot within a page, for a tab being registered}
     *
     * <p>Slots are handed out in declaration order and wrap every page, so two
     * mod tabs never land on the same square. Called by {@link Registrar}.
     */
    static int claimSlot() {
        return claimed++ % TABS_PER_PAGE;
    }

    /**
     * {@return the row a slot belongs to} Top row first, then the bottom.
     *
     * @param slot a slot from {@link #claimSlot()}
     */
    static CreativeModeTab.Row rowOf(int slot) {
        return slot < SLOTS_PER_ROW ? CreativeModeTab.Row.TOP : CreativeModeTab.Row.BOTTOM;
    }

    /**
     * {@return the column a slot belongs to} Never 5 or 6 — those are the
     * tools', on every page.
     *
     * @param slot a slot from {@link #claimSlot()}
     */
    static int columnOf(int slot) {
        return slot % SLOTS_PER_ROW;
    }

    /**
     * {@return the page being shown}
     */
    public static int current() {
        return current;
    }

    /**
     * {@return how many pages there are, at least one}
     */
    public static int count() {
        int modTabs = (int) allTabs().stream().filter(CreativePages::isModTab).count();
        return modTabs == 0 ? 1 : 1 + ceilDiv(modTabs, TABS_PER_PAGE);
    }

    /**
     * Moves to another page, wrapping at both ends.
     *
     * @param delta how far to move; negative goes back
     */
    public static void turn(int delta) {
        current = Math.floorMod(current + delta, count());
    }

    /**
     * {@return which page a tab sits on}
     *
     * <p>Vanilla's are all on page 0 — where they are <em>registered</em>, which
     * is what decides whether two of them collide. The four that also travel to
     * later pages are not registered twice, so they still answer 0.
     *
     * @param tab the tab to place
     */
    public static int pageOf(CreativeModeTab tab) {
        if (!isModTab(tab)) {
            return 0;
        }
        int index = modTabs().indexOf(tab);
        return index < 0 ? 0 : 1 + index / TABS_PER_PAGE;
    }

    /**
     * {@return the tabs belonging to the page being shown}
     *
     * @param tabs every tab the game would otherwise offer
     */
    public static List<CreativeModeTab> onCurrentPage(List<CreativeModeTab> tabs) {
        if (current == 0) {
            return tabs.stream().filter(tab -> !isModTab(tab)).toList();
        }

        List<CreativeModeTab> page = new ArrayList<>(tabs.stream().filter(CreativePages::isAlwaysVisible).toList());
        List<CreativeModeTab> mods = modTabs();
        int from = (current - 1) * TABS_PER_PAGE;
        if (from < mods.size()) {
            page.addAll(mods.subList(from, Math.min(from + TABS_PER_PAGE, mods.size())));
        }
        return page;
    }

    private static boolean isAlwaysVisible(CreativeModeTab tab) {
        return BuiltInRegistries.CREATIVE_MODE_TAB.getResourceKey(tab)
                .map(ALWAYS_VISIBLE::contains)
                .orElse(Boolean.FALSE);
    }

    /**
     * A tab is a mod's when it is not Minecraft's.
     *
     * <p>Deciding by namespace rather than by anything Fenix records means
     * another loader's tabs, or a tab added some other way, are paged too
     * instead of being silently dropped off the end.
     */
    private static boolean isModTab(CreativeModeTab tab) {
        return BuiltInRegistries.CREATIVE_MODE_TAB.getResourceKey(tab)
                .map(key -> !key.identifier().getNamespace().equals("minecraft"))
                .orElse(Boolean.FALSE);
    }

    private static List<CreativeModeTab> modTabs() {
        return allTabs().stream().filter(CreativePages::isModTab).toList();
    }

    private static List<CreativeModeTab> allTabs() {
        return BuiltInRegistries.CREATIVE_MODE_TAB.stream().toList();
    }

    private static int ceilDiv(int value, int divisor) {
        return (value + divisor - 1) / divisor;
    }
}
