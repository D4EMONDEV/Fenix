package fr.d4emon.fenix.registry;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.CreativeModeTab;

import java.util.ArrayList;
import java.util.List;

/**
 * Pages of creative tabs.
 *
 * <p>Vanilla has no such thing: the tab strip is a fixed grid of two rows by
 * seven columns, and vanilla's own tabs fill all fourteen. A mod tab has
 * literally nowhere to go, so Fenix adds the pages that make room.
 *
 * <p>Page 0 is vanilla's, untouched — someone who installs a mod should still
 * find the menu exactly where they left it. Mod tabs start on page 1.
 */
public final class CreativePages {

    /** Two rows of seven, as vanilla lays them out. */
    public static final int TABS_PER_PAGE = 14;

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
        int pages = count();
        current = Math.floorMod(current + delta, pages);
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
        List<CreativeModeTab> mods = tabs.stream().filter(CreativePages::isModTab).toList();
        int from = (current - 1) * TABS_PER_PAGE;
        int to = Math.min(from + TABS_PER_PAGE, mods.size());
        return from >= mods.size() ? List.of() : new ArrayList<>(mods.subList(from, to));
    }

    /**
     * {@return which page a tab sits on}
     *
     * <p>Vanilla's are all on page 0. Mod tabs follow in registration order,
     * fourteen to a page — the same order {@link #onCurrentPage} slices, so the
     * two agree by construction.
     *
     * @param tab the tab to place
     */
    public static int pageOf(CreativeModeTab tab) {
        if (!isModTab(tab)) {
            return 0;
        }
        List<CreativeModeTab> mods = allTabs().stream().filter(CreativePages::isModTab).toList();
        int index = mods.indexOf(tab);
        return index < 0 ? 0 : 1 + index / TABS_PER_PAGE;
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

    private static List<CreativeModeTab> allTabs() {
        return BuiltInRegistries.CREATIVE_MODE_TAB.stream().toList();
    }

    private static int ceilDiv(int value, int divisor) {
        return (value + divisor - 1) / divisor;
    }
}
