package fr.d4emon.fenix.gradle;

import java.nio.file.Path;
import java.util.List;

/**
 * The parts of a Minecraft version a mod build needs.
 *
 * @param clientJar   the client jar, downloaded and verified in the Fenix cache
 * @param compileLibs vanilla library coordinates for the compile classpath, in
 *                    {@code group:artifact:version} form
 * @param nativeLibs  native library coordinates ({@code …:natives-<os>}); on the
 *                    runtime classpath only, where LWJGL extracts them itself
 * @param assetIndex  the asset index id, for {@code --assetIndex} at run time
 * @param javaVersion the major Java version the game targets, for the toolchain
 */
public record MinecraftLibraries(
        Path clientJar,
        List<String> compileLibs,
        List<String> nativeLibs,
        String assetIndex,
        int javaVersion) {

    /**
     * Copies the coordinate lists so the record cannot change under its reader.
     */
    public MinecraftLibraries {
        compileLibs = List.copyOf(compileLibs);
        nativeLibs = List.copyOf(nativeLibs);
    }
}
