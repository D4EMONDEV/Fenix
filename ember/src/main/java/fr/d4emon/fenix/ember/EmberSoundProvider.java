package fr.d4emon.fenix.ember;

import fr.d4emon.fenix.registry.Holder;
import net.minecraft.sounds.SoundEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Writes {@code sounds.json}.
 *
 * <pre>{@code
 * @Generator
 * public final class ModSounds extends EmberSoundProvider {
 *     @Override
 *     protected void sounds() {
 *         add(ModContent.ANVIL_CRACK, "anvil_crack");
 *     }
 * }
 * }</pre>
 *
 * <p>Registering a sound event is only half of a sound. The other half is this
 * file, which says which ogg files the event actually plays; without it the
 * event exists, the code that plays it runs, and nothing is heard.
 *
 * <p>Entries are sorted, so the file does not reshuffle itself between runs and
 * a diff shows only what actually changed.
 */
public abstract class EmberSoundProvider extends EmberProvider {

    private final Map<String, List<String>> entries = new TreeMap<>();

    /** For subclasses. */
    protected EmberSoundProvider() {
    }

    /** Describes the sounds. */
    protected abstract void sounds();

    @Override
    protected final void run() {
        sounds();
        if (entries.isEmpty()) {
            return;
        }
        StringBuilder json = new StringBuilder("{\n");
        String separator = "";
        for (Map.Entry<String, List<String>> entry : entries.entrySet()) {
            json.append(separator).append("  ")
                    .append(EmberOutput.quote(entry.getKey()))
                    .append(": {\n    \"sounds\": [");
            String comma = "";
            for (String file : entry.getValue()) {
                json.append(comma).append('\n').append("      ").append(EmberOutput.quote(file));
                comma = ",";
            }
            json.append("\n    ]\n  }");
            separator = ",\n";
        }
        // Not under assets/<ns>/lang or /models: sounds.json sits at the root of
        // the namespace, which is why this writes a bare path.
        output().asset("sounds.json", json.append("\n}\n").toString());
    }

    /**
     * Says which files a sound event plays.
     *
     * <p>More than one file makes the game pick between them at random, which
     * is how vanilla keeps repeated sounds from grating.
     *
     * @param sound the event, as returned by {@code Registrar.sound}
     * @param files ogg file names under {@code assets/<mod id>/sounds/}, without
     *              the extension; at least one
     * @throws IllegalArgumentException if no file is given
     */
    protected final void add(Holder<SoundEvent> sound, String... files) {
        Objects.requireNonNull(sound, "sound");
        if (files.length == 0) {
            throw new IllegalArgumentException(sound.id() + " has no files, so it would play nothing");
        }
        List<String> qualified = new ArrayList<>();
        for (String file : files) {
            // Namespaced, or the game looks for the file under minecraft's own
            // assets and silently finds nothing.
            qualified.add(sound.id().getNamespace() + ":" + Objects.requireNonNull(file, "file"));
        }
        entries.put(sound.id().getPath(), qualified);
    }
}
