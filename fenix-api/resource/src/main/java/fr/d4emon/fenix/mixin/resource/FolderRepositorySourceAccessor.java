package fr.d4emon.fenix.mixin.resource;

import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.FolderRepositorySource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Reads the pack type off a folder source.
 *
 * <p>This is how {@link PackRepositoryMixin} tells a resource repository from a
 * datapack one. See there for why the obvious alternative is a trap.
 */
@Mixin(FolderRepositorySource.class)
public interface FolderRepositorySourceAccessor {

    /**
     * {@return whether this source feeds client resources or server data}
     */
    @Accessor("packType")
    PackType fenix$packType();
}
