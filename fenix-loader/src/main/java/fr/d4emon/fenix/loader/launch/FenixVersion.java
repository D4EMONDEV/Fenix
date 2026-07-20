package fr.d4emon.fenix.loader.launch;

import fr.d4emon.fenix.api.Version;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Properties;

/**
 * The loader's own version, stamped into {@code fenix-loader.properties} at
 * build time. This is what satisfies a mod's {@code "fenix"} dependency.
 */
public final class FenixVersion {

    private static final Version CURRENT = load();

    private FenixVersion() {
    }

    /**
     * {@return the version of the running loader}
     */
    public static Version current() {
        return CURRENT;
    }

    private static Version load() {
        try (InputStream in = FenixVersion.class.getResourceAsStream("/fenix-loader.properties")) {
            if (in == null) {
                throw new IllegalStateException(
                        "fenix-loader.properties is missing from the loader jar — the build is broken");
            }
            Properties properties = new Properties();
            properties.load(in);

            String version = properties.getProperty("version");
            if (version == null || version.startsWith("${")) {
                throw new IllegalStateException(
                        "fenix-loader.properties was not expanded at build time — the build is broken");
            }
            return Version.parse(version);
        } catch (IOException e) {
            throw new UncheckedIOException("cannot read fenix-loader.properties", e);
        }
    }
}
