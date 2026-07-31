package fr.d4emon.fenix.resource;

import fr.d4emon.fenix.api.ModInfo;
import fr.d4emon.fenix.loader.launch.FenixHooks;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.FilePackResources;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.KnownPack;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackCompatibility;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.RepositorySource;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.jar.JarFile;

/**
 * Hands every mod jar to the game as a resource pack.
 *
 * <p>Minecraft only reads assets and data from packs. A mod jar is not one — it
 * has no {@code pack.mcmeta} — so without this a mod's models, textures and
 * translations sit in the jar and are never looked at. That is the difference
 * between a registered block and a <em>visible</em> one.
 *
 * <p>The pack is therefore built in code rather than read from the jar, which
 * also means mod authors never have to write a {@code pack.mcmeta} or keep its
 * format version current.
 *
 * <p>Packs are added at the top and forced on: a mod's own resources are not
 * something a player should have to enable, and a resource pack they *do*
 * enable still sits above and can override them.
 */
public final class ModPackSource implements RepositorySource {

    private final PackType type;

    /**
     * Creates a source for one kind of repository.
     *
     * @param type which repository this is feeding — client resources or server data
     */
    public ModPackSource(PackType type) {
        this.type = type;
    }

    @Override
    public void loadPacks(Consumer<Pack> consumer) {
        Map<String, String> versions = new LinkedHashMap<>();
        for (ModInfo mod : FenixHooks.loadedMods()) {
            versions.put(mod.id(), mod.version().toString());
        }

        for (Map.Entry<String, Path> mod : FenixHooks.modJars().entrySet()) {
            String modId = mod.getKey();
            Path jar = mod.getValue();

            if (!hasResources(jar, type)) {
                continue;
            }
            Pack pack = create(modId, jar, versions.getOrDefault(modId, "0.0.0"));
            if (pack != null) {
                consumer.accept(pack);
            }
        }
    }

    private Pack create(String modId, Path jar, String version) {
        Component title = Component.literal(modId);
        PackLocationInfo location = new PackLocationInfo(
                // Namespaced so a mod can never collide with a player's pack.
                "fenix/" + modId, title, PackSource.BUILT_IN,
                // A mod jar is exactly what a KnownPack describes: a pack both
                // sides identify by name and version, so the server need not
                // send its contents to a client that already has the mod —
                // which Fenix's registry sync has already insisted upon.
                //
                // Declaring it also settles the "Experimental Features" warning
                // every world creation used to raise. Vanilla decides the
                // lifecycle of a datapack registry entry by whether the pack it
                // came from is known: no KnownPack means Lifecycle.experimental,
                // one experimental registry makes allRegistriesLifecycle()
                // experimental, and CreateWorldScreen warns on anything that is
                // not stable. A mod with one worldgen file was enough to trip
                // it, and nothing in the message pointed at a mod.
                Optional.of(new KnownPack(modId, "fenix/" + modId, version)));

        Pack.Metadata metadata = new Pack.Metadata(
                Component.literal("Resources of " + modId),
                // A mod ships with the version it was built for; treating it as
                // incompatible would only hide it behind a warning.
                PackCompatibility.COMPATIBLE,
                net.minecraft.world.flag.FeatureFlagSet.of(),
                List.of());

        PackSelectionConfig selection = new PackSelectionConfig(
                true, Pack.Position.TOP, /* fixed position */ false);

        return new Pack(location, new FilePackResources.FileResourcesSupplier(jar), metadata, selection);
    }

    /**
     * Whether a jar has anything for this repository, so that mods shipping only
     * code do not clutter the pack list.
     */
    private static boolean hasResources(Path jar, PackType type) {
        String directory = type.getDirectory() + "/";
        try (JarFile jarFile = new JarFile(jar.toFile())) {
            return jarFile.stream().anyMatch(entry -> entry.getName().startsWith(directory));
        } catch (IOException e) {
            // Discovery already vetted these jars; an unreadable one here just
            // has nothing to contribute.
            return false;
        }
    }
}
