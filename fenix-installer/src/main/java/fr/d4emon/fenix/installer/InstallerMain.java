package fr.d4emon.fenix.installer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

/**
 * The installer's command line.
 *
 * <p>The loader jars travel <em>inside</em> the installer jar (packed at build
 * time under {@code fenix-payload/}), so the installer is a single
 * self-contained file with nothing to download.
 */
public final class InstallerMain {

    private static final String USAGE = """
            Usage: java -jar fenix-installer.jar [options]
              --dir <path>          the .minecraft directory (default: the standard location)
              --minecraft <version> the vanilla version to install on top of""";

    /** The artifacts embedded in the installer, in classpath order. */
    private static final List<String> PAYLOAD_ARTIFACTS = List.of("fenix-loader", "fenix-api-core");

    private InstallerMain() {
    }

    /**
     * Runs the installer.
     *
     * @param args see the usage text
     */
    public static void main(String[] args) {
        try {
            run(args);
        } catch (InstallException e) {
            System.err.println();
            System.err.println(e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void run(String[] args) throws IOException {
        Properties build = readBuildProperties();
        String fenixVersion = build.getProperty("version");
        String minecraftVersion = build.getProperty("minecraft");
        Path minecraftDir = defaultMinecraftDir();

        int i = 0;
        while (i < args.length) {
            switch (args[i]) {
                case "--dir" -> minecraftDir = Path.of(value(args, i++));
                case "--minecraft" -> minecraftVersion = value(args, i++);
                default -> throw new InstallException("unknown option '" + args[i] + "'"
                        + System.lineSeparator() + USAGE);
            }
            i++;
        }

        System.out.println("Installing Fenix " + fenixVersion + " for Minecraft " + minecraftVersion
                + " into " + minecraftDir);

        Installer.Report report = Installer.install(
                minecraftDir, minecraftVersion, fenixVersion, extractPayload(fenixVersion));

        System.out.println();
        System.out.println("Installed:");
        report.libraries().forEach(path -> System.out.println("  " + path));
        System.out.println("  " + report.versionJson());
        System.out.println("  " + report.profiles() + " (profile '" + Installer.PROFILE_KEY + "')");
        System.out.println();
        System.out.println("Open the Minecraft Launcher, pick the \"Fenix " + minecraftVersion
                + "\" profile, and press Play.");
    }

    private static Properties readBuildProperties() throws IOException {
        try (InputStream in = InstallerMain.class.getResourceAsStream("/fenix-installer.properties")) {
            if (in == null) {
                throw new InstallException("fenix-installer.properties is missing — this installer "
                        + "was not built by Gradle; run gradlew :fenix-installer:jar");
            }
            Properties properties = new Properties();
            properties.load(in);
            return properties;
        }
    }

    /**
     * Extracts the embedded loader jars to temporary files the installer can
     * copy from. Missing payload means a hand-built or IDE-run installer.
     */
    private static List<Installer.Library> extractPayload(String fenixVersion) throws IOException {
        List<Installer.Library> libraries = new ArrayList<>();
        for (String artifact : PAYLOAD_ARTIFACTS) {
            String resource = "/fenix-payload/" + artifact + "-" + fenixVersion + ".jar";
            try (InputStream in = InstallerMain.class.getResourceAsStream(resource)) {
                if (in == null) {
                    throw new InstallException("the installer is missing its embedded " + resource
                            + " — build it with gradlew :fenix-installer:jar");
                }
                Path temp = Files.createTempFile(artifact + "-", ".jar");
                temp.toFile().deleteOnExit();
                Files.copy(in, temp, StandardCopyOption.REPLACE_EXISTING);
                libraries.add(new Installer.Library("fr.d4emon.fenix", artifact, fenixVersion, temp));
            }
        }
        return libraries;
    }

    /**
     * {@return the platform's standard {@code .minecraft} location}
     */
    static Path defaultMinecraftDir() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        Path home = Path.of(System.getProperty("user.home", "."));
        if (os.contains("win")) {
            String appData = System.getenv("APPDATA");
            return (appData != null ? Path.of(appData) : home.resolve("AppData").resolve("Roaming"))
                    .resolve(".minecraft");
        }
        if (os.contains("mac")) {
            return home.resolve("Library").resolve("Application Support").resolve("minecraft");
        }
        return home.resolve(".minecraft");
    }

    private static String value(String[] args, int index) {
        if (index + 1 >= args.length) {
            throw new InstallException("option '" + args[index] + "' needs a value"
                    + System.lineSeparator() + USAGE);
        }
        return args[index + 1];
    }
}
