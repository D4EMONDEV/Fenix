package fr.d4emon.fenix.gradle;

import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;

/**
 * The {@code fenix { }} block a mod build configures.
 *
 * <pre>{@code
 * fenix {
 *     minecraft = "26.2"
 * }
 * }</pre>
 *
 * <p>Naming the game version is enough. Every other version is looked up for
 * that game in the table the plugin carries, so the API a mod compiles against
 * is the one built for the game it asked for — see {@link Platforms}.
 *
 * <p>Each of those lookups can be overridden, one line each, which is what
 * testing an unreleased loader or a locally built Ember looks like:
 *
 * <pre>{@code
 * fenix {
 *     minecraft = "26.2"
 *     loader = "0.1.2"
 *     ember = "0.2.1"
 * }
 * }</pre>
 *
 * <p>An override is taken exactly as written, so overriding one does not move
 * the others — which is the point, and also the risk: a loader and an API from
 * different releases are a pair nobody tested.
 */
public abstract class FenixExtension {

    /**
     * {@return the Minecraft version to build and run against}
     *
     * <p>Defaults to the current line in the plugin's platform table. A version
     * Fenix has no release for fails at configuration rather than silently
     * pairing the game with an API built for a different one.
     */
    public abstract Property<String> getMinecraft();

    /**
     * {@return the Fenix loader version to compile and launch with}
     *
     * <p>Separate from {@link #getApi()} because they say different things and
     * move at different speeds: this is the platform contract a mod's
     * {@code depends.fenix} names, and it moves when the loader's promises to
     * mods change rather than when its internals do. It carries no game version,
     * because almost nothing in the loader touches a Minecraft class.
     */
    public abstract Property<String> getLoader();

    /**
     * {@return the Fenix API version}
     *
     * <p>A release of the API set. Unlike the loader it belongs to one game
     * version, because it is compiled against it: {@code 0.3.0+mc26.2}. Write
     * either form — a version with no {@code +mc} suffix gets the one for
     * {@link #getMinecraft()} appended, so the common case is just the number.
     */
    public abstract Property<String> getApi();

    /**
     * {@return the Ember version, for a project that runs data generators}
     *
     * <p>Built against the game like the API, and accepts the same two forms.
     */
    public abstract Property<String> getEmber();

    /**
     * Whether the whole API bundle is a dependency of this mod, on by default.
     *
     * <p>Set it to {@code false} to name the modules you use instead:
     *
     * <pre>{@code
     * fenix { bundle = false }
     * dependencies { fenixMod("fr.d4emon.fenix:fenix-api-event:0.2.0") }
     * }</pre>
     *
     * <p>Named for what it turns off — {@code fenix-api} is a bundle jar
     * carrying every module — rather than for the API, which a mod that sets
     * this to {@code false} is still very much using.
     *
     * @return the property
     */
    public abstract Property<Boolean> getBundle();

    /**
     * {@return whether this project is a Fenix building block rather than a mod}
     *
     * <p>A library gets Minecraft on its compile classpath and nothing else: no
     * API dependency, no annotation processor, and no run tasks. Fenix's own API
     * modules set this — they <em>are</em> the API, so depending on it would be
     * circular, and there is nothing to launch. Defaults to {@code false}.
     */
    public abstract Property<Boolean> getLibrary();

    /**
     * {@return the Minecraft client jar the plugin resolved}
     *
     * <p>Set by the plugin, not by the build. Useful to a build that needs the
     * game as a <em>file</em> rather than on a classpath — Fenix's own
     * conformance tests hand it to the loader.
     */
    public abstract RegularFileProperty getClientJar();
}
