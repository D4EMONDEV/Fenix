package fr.d4emon.fenix.mixin.resource;

import fr.d4emon.fenix.resource.ModPackSource;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.FolderRepositorySource;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.RepositorySource;
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
 * mentions. So the type is read off the {@link FolderRepositorySource} that
 * every repository is built with (the player's {@code resourcepacks} or
 * {@code datapacks} folder), which is common to both sides.
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
            if (source instanceof FolderRepositorySource folder) {
                return ((FolderRepositorySourceAccessor) folder).fenix$packType();
            }
        }
        return null;
    }
}
