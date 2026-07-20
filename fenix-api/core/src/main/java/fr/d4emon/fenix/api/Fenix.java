package fr.d4emon.fenix.api;

import fr.d4emon.fenix.api.log.FenixLogger;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;

/**
 * A mod's view of the loader, handed to it at every {@link FenixMod} lifecycle
 * phase.
 *
 * <p>There is no way to obtain one statically, and that is the point: because
 * the context is passed in, it can know <em>which</em> mod it belongs to. That
 * is what makes {@link #logger()} attribute output automatically and
 * {@link #configDir()} resolve per mod, neither of which a singleton could do.
 *
 * <p>Instances are supplied by the loader. Mods implement this only in tests.
 */
public interface Fenix {

    /**
     * {@return the mod this context belongs to}
     */
    ModInfo mod();

    /**
     * {@return every loaded mod, in the order they were initialised}
     *
     * <p>Dependencies always come before the mods that declare them.
     */
    Collection<ModInfo> mods();

    /**
     * Looks up a loaded mod.
     *
     * @param id the mod id to look for
     * @return the mod, or empty if nothing with that id is loaded
     */
    Optional<ModInfo> findMod(String id);

    /**
     * Checks whether a mod is loaded.
     *
     * <p>For optional integration with another mod. A hard requirement belongs
     * in {@code depends} instead, so the player is told what is missing before
     * the game starts rather than after something silently did nothing.
     *
     * @param id the mod id to look for
     * @return whether a mod with that id is loaded
     */
    default boolean isLoaded(String id) {
        return findMod(id).isPresent();
    }

    /**
     * {@return the version of the loader itself}
     */
    Version loaderVersion();

    /**
     * {@return the side this process is running on}
     *
     * @see Side
     */
    Side side();

    /**
     * {@return the game directory, holding the world saves, resource packs and mods}
     */
    Path gameDir();

    /**
     * {@return the directory this mod should keep its configuration in}
     *
     * <p>Resolved per mod and created on demand, so two mods cannot collide.
     */
    Path configDir();

    /**
     * {@return a logger that attributes its output to this mod}
     */
    FenixLogger logger();
}
