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
 * <p>Both properties have defaults — the Minecraft version the plugin was built
 * against, and the plugin's own version as the loader version — so the common
 * case needs no configuration at all.
 */
public abstract class FenixExtension {

    /**
     * The Fenix API version, defaulted from the plugin.
     *
     * <p>Separate from {@link #getLoaderVersion()} because they say different
     * things and move at different speeds: the loader version is the platform
     * contract a mod's {@code depends.fenix} names, while this one is a release
     * of the API set — and unlike the loader, it carries the game version,
     * because it is built against it.
     *
     * @return the property
     */
    public abstract Property<String> getApiVersion();

    /**
     * Whether the whole API is a dependency of this mod, on by default.
     *
     * <p>Set it to {@code false} to name the modules you use instead:
     *
     * <pre>{@code
     * fenix { api = false }
     * dependencies { fenixMod("fr.d4emon.fenix:fenix-api-event:0.1.0") }
     * }</pre>
     *
     * @return the property
     */
    public abstract Property<Boolean> getApi();

    /**
     * {@return the Minecraft version to build and run against}
     */
    public abstract Property<String> getMinecraft();

    /**
     * {@return the Fenix loader version to compile and launch with}
     */
    public abstract Property<String> getLoaderVersion();

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
