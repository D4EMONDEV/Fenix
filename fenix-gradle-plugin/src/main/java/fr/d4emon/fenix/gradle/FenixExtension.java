package fr.d4emon.fenix.gradle;

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
     * {@return the Minecraft version to build and run against}
     */
    public abstract Property<String> getMinecraft();

    /**
     * {@return the Fenix loader version to compile and launch with}
     */
    public abstract Property<String> getLoaderVersion();
}
