package fr.d4emon.fenix.mixin.resource;

import fr.d4emon.fenix.resource.ModPackSource;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.FolderRepositorySource;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.RepositorySource;
import net.minecraft.server.packs.repository.ServerPacksSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Adds the mod pack source to every pack repository the game builds.
 *
 * <p>The constructor is the one place both repositories go through — the
 * client's resource packs and the server's datapacks — so injecting here covers
 * a client, a dedicated server and the integrated server in one.
 *
 * <p><strong>The catch:</strong> the constructor takes only sources, and never
 * says which kind of repository it is building. The obvious test —
 * {@code instanceof ClientPackSource} — is a trap: that class is client-only,
 * and merely naming it here would be a {@code NoClassDefFoundError} on a
 * dedicated server, because loading a class resolves every type its code
 * mentions. So the type is read off the sources themselves. A
 * {@link ServerPacksSource} means datapacks and is nameable on both sides; a
 * repository without one is the client's, and is told apart by the folder it
 * watches.
 */
@Mixin(PackRepository.class)
public class PackRepositoryMixin {

    /** Vanilla keeps this immutable; Fenix needs one more entry in it. */
    @Shadow
    @Final
    @Mutable
    private Set<RepositorySource> sources;

    /** Matched by Mixin from the config; not called directly. */
    public PackRepositoryMixin() {
    }

    @Inject(method = "<init>", at = @At("TAIL"), remap = false)
    private void fenix$addModPacks(RepositorySource[] given, CallbackInfo ci) {
        PackType type = inferType(given);
        if (type == null) {
            // Some other repository — a test, or something vanilla builds
            // without a folder source. Nothing to contribute.
            return;
        }
        Set<RepositorySource> withMods = new LinkedHashSet<>(sources);
        withMods.add(new ModPackSource(type));
        sources = withMods;
    }

    private static PackType inferType(RepositorySource[] given) {
        for (RepositorySource source : given) {
            // A datapack repository, whether or not it has a folder behind it.
            //
            // This is the one that mattered and the one the folder test missed.
            // The create-world screen builds `new PackRepository(new
            // ServerPacksSource(...))` — vanilla and nothing else, no folder —
            // so mod datapacks were absent from the load that decides a new
            // world's worldgen registries. The world was then created without
            // them, and the ore a mod adds to a biome had no feature to point
            // at. The server noticed the pack afterwards ("Found new data pack
            // …, loading it automatically") and reloaded recipes and
            // advancements, which is too late: worldgen is frozen at world
            // load.
            //
            // Safe to name on both sides, unlike ClientPackSource: the
            // dedicated server is built on this class.
            if (source instanceof ServerPacksSource) {
                return PackType.SERVER_DATA;
            }
        }
        for (RepositorySource source : given) {
            // The client's resource-pack repository, which has no
            // ServerPacksSource and is told apart by its folder.
            if (source instanceof FolderRepositorySource folder) {
                return ((FolderRepositorySourceAccessor) folder).fenix$packType();
            }
        }
        return null;
    }
}
