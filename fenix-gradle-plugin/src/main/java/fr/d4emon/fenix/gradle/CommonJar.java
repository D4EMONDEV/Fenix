package fr.d4emon.fenix.gradle;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Builds the jar that common code compiles against: Minecraft with the
 * client-only half removed.
 *
 * <p>This is what turns the sidedness rule from a convention into a compile
 * error. Naming {@code net.minecraft.client.Minecraft} from {@code src/main}
 * stops being something a reviewer has to notice and becomes a {@code javac}
 * error with a line number — which matters because the alternative surfaces as
 * a {@code NoClassDefFoundError} on somebody else's dedicated server, long
 * after the mod was written and tested on a client.
 *
 * <p>Derived from the client jar rather than downloaded: the client jar is a
 * strict superset of the server's — every package the server has, the client
 * has too — so stripping the four client-only roots leaves exactly what both
 * sides share, at the cost of one pass over a jar already on disk.
 */
final class CommonJar {

    /**
     * The roots the dedicated server does not ship.
     *
     * <p>Not guessed: this is the set difference between the two published
     * jars, and a conformance check keeps it honest as Minecraft moves.
     */
    private static final List<String> CLIENT_ONLY = List.of(
            "net/minecraft/client/",
            "net/minecraft/realms/",
            "com/mojang/blaze3d/",
            "com/mojang/realmsclient/");

    private CommonJar() {
    }

    /**
     * {@return the common jar, built on first use and cached beside the client}
     *
     * @param clientJar the full client jar
     * @param version   the Minecraft version, for the file name
     */
    static Path of(Path clientJar, String version) {
        Path common = clientJar.getParent().resolve("common-" + version + ".jar");
        if (Files.isRegularFile(common)) {
            return common;
        }
        // Written beside and moved into place, so an interrupted build cannot
        // leave a half-written jar that every later build then trusts.
        Path partial = common.resolveSibling(common.getFileName() + ".partial");
        try (ZipFile source = new ZipFile(clientJar.toFile());
             ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(partial))) {

            for (Enumeration<? extends ZipEntry> entries = source.entries(); entries.hasMoreElements(); ) {
                ZipEntry entry = entries.nextElement();
                if (isClientOnly(entry.getName())) {
                    continue;
                }
                out.putNextEntry(new ZipEntry(entry.getName()));
                if (!entry.isDirectory()) {
                    try (InputStream in = source.getInputStream(entry)) {
                        copy(in, out);
                    }
                }
                out.closeEntry();
            }
        } catch (IOException e) {
            throw new UncheckedIOException("could not build the common Minecraft jar", e);
        }
        try {
            Files.move(partial, common, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("could not put the common Minecraft jar in place", e);
        }
        return common;
    }

    private static boolean isClientOnly(String entry) {
        return CLIENT_ONLY.stream().anyMatch(entry::startsWith);
    }

    private static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[16 * 1024];
        for (int read = in.read(buffer); read >= 0; read = in.read(buffer)) {
            out.write(buffer, 0, read);
        }
    }
}
