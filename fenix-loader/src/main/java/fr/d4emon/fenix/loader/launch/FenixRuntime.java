package fr.d4emon.fenix.loader.launch;

import fr.d4emon.fenix.api.Fenix;
import fr.d4emon.fenix.api.FenixMod;
import fr.d4emon.fenix.api.ModInfo;
import fr.d4emon.fenix.api.Side;
import fr.d4emon.fenix.loader.log.ConsoleLogger;

import java.nio.file.Path;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * The state behind every {@link Fenix} context, and the thing that fires the
 * lifecycle.
 *
 * <p>Each phase runs at most once — the game may reach a hook twice (a second
 * {@code MinecraftServer} in the same process, say) but mods must not — and
 * always walks the mods in load order, so a mod can rely on its dependencies
 * having been through a phase before it enters it.
 */
public final class FenixRuntime {

    private enum Phase {
        PRE_LAUNCH("onPreLaunch"), REGISTER("onRegister"), INIT("onInit");

        final String methodName;

        Phase(String methodName) {
            this.methodName = methodName;
        }
    }

    private final Side side;
    private final Path gameDir;
    private final List<LoadedMod> mods;
    private final Map<String, ModInfo> infoById;
    private final List<ModInfo> infoView;
    private final Map<String, Fenix> contexts = new HashMap<>();
    private final Set<Phase> fired = EnumSet.noneOf(Phase.class);
    private final ConsoleLogger logger = new ConsoleLogger("fenix");

    /**
     * Creates the runtime for one launch.
     *
     * @param side    the side this process is running on
     * @param gameDir the game directory
     * @param mods    every loaded mod, in load order
     * @throws NullPointerException if any argument is {@code null}
     */
    public FenixRuntime(Side side, Path gameDir, List<LoadedMod> mods) {
        this.side = Objects.requireNonNull(side, "side");
        this.gameDir = Objects.requireNonNull(gameDir, "gameDir");
        this.mods = List.copyOf(mods);

        Map<String, ModInfo> byId = new LinkedHashMap<>();
        for (LoadedMod mod : this.mods) {
            byId.put(mod.id(), mod.metadata().toModInfo());
        }
        this.infoById = Map.copyOf(byId);
        this.infoView = List.copyOf(byId.values());
    }

    /**
     * Fires {@code onPreLaunch}. Called by {@link Launch} before any game class
     * is loaded.
     */
    public void firePreLaunch() {
        fire(Phase.PRE_LAUNCH, FenixMod::onPreLaunch);
    }

    /**
     * Fires {@code onRegister}. Called from inside the game, while its
     * registries are still open.
     */
    public void fireRegister() {
        fire(Phase.REGISTER, FenixMod::onRegister);
    }

    /**
     * Fires {@code onInit}. Called from inside the game once it is up.
     */
    public void fireInit() {
        fire(Phase.INIT, FenixMod::onInit);
    }

    private void fire(Phase phase, BiConsumer<FenixMod, Fenix> call) {
        if (!fired.add(phase)) {
            logger.debug("{} already ran; ignoring the repeat", phase.methodName);
            return;
        }
        logger.debug("firing {}", phase.methodName);

        for (LoadedMod mod : mods) {
            Fenix context = contextFor(mod);
            for (FenixMod entry : mod.entries()) {
                try {
                    call.accept(entry, context);
                } catch (RuntimeException e) {
                    throw new LaunchException(
                            "mod '" + mod.id() + "' failed in " + phase.methodName, e);
                }
            }
        }
    }

    private Fenix contextFor(LoadedMod mod) {
        return contexts.computeIfAbsent(mod.id(), id -> new ModContext(this, infoById.get(id)));
    }

    /**
     * {@return every loaded mod, in load order}
     */
    public List<LoadedMod> mods() {
        return mods;
    }

    // What the per-mod contexts read.

    Side side() {
        return side;
    }

    Path gameDir() {
        return gameDir;
    }

    List<ModInfo> modInfos() {
        return infoView;
    }

    Optional<ModInfo> findInfo(String id) {
        return Optional.ofNullable(infoById.get(id));
    }
}
