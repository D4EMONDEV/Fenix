package fr.d4emon.fenix.ember;

import fr.d4emon.fenix.registry.CreativeTabs;
import fr.d4emon.fenix.registry.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;

import java.util.Map;
import java.util.TreeMap;

/**
 * Writes {@code en_us.json}.
 *
 * <pre>{@code
 * @Generator
 * public final class ModLanguage extends EmberLanguageProvider {
 *     @Override
 *     protected void translations() {
 *         add(ModBlocks.RUBY_BLOCK, "Ruby Block");
 *         add(ModItems.RUBY, "Ruby");
 *     }
 * }
 * }</pre>
 *
 * <p>Entries are sorted, so the file does not reshuffle itself between runs and
 * a diff shows only what actually changed.
 */
public abstract class EmberLanguageProvider extends EmberProvider {

    private final Map<String, String> entries = new TreeMap<>();

    /** For subclasses. */
    protected EmberLanguageProvider() {
    }

    /** Describes the translations. */
    protected abstract void translations();

    @Override
    protected final void run() {
        translations();
        if (entries.isEmpty()) {
            return;
        }
        StringBuilder json = new StringBuilder("{\n");
        String separator = "";
        for (Map.Entry<String, String> entry : entries.entrySet()) {
            json.append(separator).append("  ")
                    .append(EmberOutput.quote(entry.getKey()))
                    .append(": ")
                    .append(EmberOutput.quote(entry.getValue()));
            separator = ",\n";
        }
        output().asset("lang/en_us.json", json.append("\n}\n").toString());
    }

    /**
     * Names a block or an item.
     *
     * <p>One method for both: generics erase, so a pair of overloads taking
     * {@code Holder<Block>} and {@code Holder<Item>} would be the same method.
     * The kind is checked when this runs, at build time.
     *
     * @param content what to name
     * @param english what to call it
     */
    protected final void add(Holder<?> content, String english) {
        add(EmberOutput.descriptionId(content.get()), english);
    }

    /**
     * Names a creative tab.
     *
     * <p>The key comes from the tab itself rather than being written out, so a
     * renamed tab cannot leave its translation behind — which in game shows up
     * as a tab titled {@code itemGroup.your-mod.something}.
     *
     * @param tab     the tab, as returned by {@code Registrar.creativeTab}
     * @param english what to call it
     */
    protected final void add(ResourceKey<CreativeModeTab> tab, String english) {
        add(CreativeTabs.titleKey(tab), english);
    }

    /**
     * Adds any translation.
     *
     * @param key     the translation key
     * @param english what it reads
     */
    protected final void add(String key, String english) {
        entries.put(key, english);
    }
}
