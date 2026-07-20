package fr.d4emon.fenix.loader.launch;

import fr.d4emon.fenix.api.Fenix;
import fr.d4emon.fenix.api.ModInfo;
import fr.d4emon.fenix.api.Side;
import fr.d4emon.fenix.api.Version;
import fr.d4emon.fenix.api.log.FenixLogger;
import fr.d4emon.fenix.loader.log.ConsoleLogger;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;

/**
 * One mod's view of the loader.
 *
 * <p>Knowing which mod it belongs to is the whole design: the logger carries
 * the mod's id and the config directory is resolved per mod, with no global
 * lookup anywhere.
 */
final class ModContext implements Fenix {

    private final FenixRuntime runtime;
    private final ModInfo info;
    private final FenixLogger logger;

    ModContext(FenixRuntime runtime, ModInfo info) {
        this.runtime = runtime;
        this.info = info;
        this.logger = new ConsoleLogger(info.id());
    }

    @Override
    public ModInfo mod() {
        return info;
    }

    @Override
    public Collection<ModInfo> mods() {
        return runtime.modInfos();
    }

    @Override
    public Optional<ModInfo> findMod(String id) {
        return runtime.findInfo(id);
    }

    @Override
    public Version loaderVersion() {
        return FenixVersion.current();
    }

    @Override
    public Side side() {
        return runtime.side();
    }

    @Override
    public Path gameDir() {
        return runtime.gameDir();
    }

    @Override
    public Path configDir() {
        Path dir = runtime.gameDir().resolve("config").resolve(info.id());
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new UncheckedIOException("cannot create the config directory " + dir, e);
        }
        return dir;
    }

    @Override
    public FenixLogger logger() {
        return logger;
    }
}
