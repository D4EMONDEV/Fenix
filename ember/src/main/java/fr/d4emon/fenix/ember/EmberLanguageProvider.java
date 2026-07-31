package fr.d4emon.fenix.ember;

import fr.d4emon.fenix.registry.CreativeTabs;
import fr.d4emon.fenix.registry.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;

import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.regex.Pattern;

/**
 * Writes a language file.
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
 * <p>English unless told otherwise. Another language is a second generator that
 * names its code — generators are built through their no-argument constructor,
 * so the code is passed up rather than taken as a parameter:
 *
 * <pre>{@code
 * @Generator
 * public final class ModLanguageFr extends EmberLanguageProvider {
 *     public ModLanguageFr() {
 *         super("fr_fr");
 *     }
 *
 *     @Override
 *     protected void translations() {
 *         add(ModBlocks.RUBY_BLOCK, "Bloc de rubis");
 *     }
 * }
 * }</pre>
 *
 * <p>Entries are sorted, so the file does not reshuffle itself between runs and
 * a diff shows only what actually changed.
 */
public abstract class EmberLanguageProvider extends EmberProvider {

    /**
     * What Minecraft calls a language: lowercase, with an underscore.
     *
     * <p>Checked because the mistake is silent and easy — {@code fr_FR}, the
     * shape Java's own {@code Locale} prints, produces a file the game never
     * looks for. Nothing fails; the mod is simply untranslated, in a language
     * the author cannot see is missing because they wrote it.
     */
    private static final Pattern LANGUAGE = Pattern.compile("[a-z0-9_]+");

    private final Map<String, String> entries = new TreeMap<>();
    private final String language;

    /** For subclasses writing English. */
    protected EmberLanguageProvider() {
        this("en_us");
    }

    /**
     * For subclasses writing any other language.
     *
     * @param language the language code, as Minecraft spells it — {@code fr_fr},
     *                 {@code pt_br}, {@code zh_cn}
     * @throws IllegalArgumentException if it is not a language code Minecraft
     *                                  would look for
     */
    protected EmberLanguageProvider(String language) {
        Objects.requireNonNull(language, "language");
        if (!LANGUAGE.matcher(language).matches()) {
            throw new IllegalArgumentException("'" + language + "' is not a Minecraft language code"
                    + " — they are lowercase with an underscore, like fr_fr, and a file named"
                    + " anything else is never read");
        }
        this.language = language;
    }

    /** {@return the language this writes} */
    protected final String language() {
        return language;
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
        output().asset("lang/" + language + ".json", json.append("\n}\n").toString());
    }

    /**
     * Names a block or an item.
     *
     * <p>One method for both: generics erase, so a pair of overloads taking
     * {@code Holder<Block>} and {@code Holder<Item>} would be the same method.
     * The kind is checked when this runs, at build time.
     *
     * @param content what to name
     * @param text    what to call it, in this file's language
     */
    protected final void add(Holder<?> content, String text) {
        add(EmberOutput.descriptionId(content.get()), text);
    }

    /**
     * Names a creative tab.
     *
     * <p>The key comes from the tab itself rather than being written out, so a
     * renamed tab cannot leave its translation behind — which in game shows up
     * as a tab titled {@code itemGroup.your-mod.something}.
     *
     * @param tab  the tab, as returned by {@code Registrar.creativeTab}
     * @param text what to call it, in this file's language
     */
    protected final void add(ResourceKey<CreativeModeTab> tab, String text) {
        add(CreativeTabs.titleKey(tab), text);
    }

    /**
     * Adds any translation.
     *
     * @param key  the translation key
     * @param text what it reads, in this file's language
     */
    protected final void add(String key, String text) {
        entries.put(key, text);
    }
}
