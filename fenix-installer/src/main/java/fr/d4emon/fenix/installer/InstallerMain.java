package fr.d4emon.fenix.installer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
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

    /** Lists the embedded jars and their coordinates; written by the build. */
    private static final String PAYLOAD_INDEX = "/fenix-payload/payload.index";

    private InstallerMain() {
    }

    /**
     * Runs the installer.
     *
     * @param args see the usage text
     */
    public static void main(String[] args) {
        try {
            // Double-clicked, with a screen to draw on: show the window. Given
            // arguments, or run over ssh, it stays the command-line tool it was
            // — a headless machine is exactly where scripting an install
            // matters, and popping up a window there would just hang.
            if (args.length == 0 && !java.awt.GraphicsEnvironment.isHeadless()) {
                Properties build = readBuildProperties();
                InstallerWindow.open(build.getProperty("version"), build.getProperty("minecraft"),
                        defaultMinecraftDir(), InstallerMain::payloadOrFail);
                return;
            }
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
                minecraftDir, minecraftVersion, fenixVersion, extractPayload());

        System.out.println();
        System.out.println("Installed:");
        report.libraries().forEach(path -> System.out.println("  " + path));
        System.out.println("  " + report.versionJson());
        System.out.println("  " + report.profiles() + " (profile '" + Installer.PROFILE_KEY + "')");
        System.out.println();
        System.out.println("Open the Minecraft Launcher, pick the \"Fenix " + minecraftVersion
                + "\" profile, and press Play.");
    }

    /** The window cannot throw a checked exception at us, so this wraps it. */
    private static List<Installer.Library> payloadOrFail() {
        try {
            return extractPayload();
        } catch (IOException e) {
            throw new InstallException("the installer's payload cannot be read: " + e.getMessage());
        }
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
     * Extracts every embedded jar to a temporary file, reading their Maven
     * coordinates from the payload index. Missing payload means a hand-built or
     * IDE-run installer.
     */
    private static List<Installer.Library> extractPayload() throws IOException {
        List<Installer.Library> libraries = new ArrayList<>();
        for (String line : readPayloadIndex()) {
            // group:artifact:version:filename
            String[] fields = line.split(":", 4);
            if (fields.length != 4) {
                throw new InstallException("the installer's payload index is malformed: '" + line + "'");
            }
            String resource = "/fenix-payload/" + fields[3];
            try (InputStream in = InstallerMain.class.getResourceAsStream(resource)) {
                if (in == null) {
                    throw new InstallException("the installer is missing its embedded " + resource
                            + " — build it with gradlew :fenix-installer:jar");
                }
                Path temp = Files.createTempFile(fields[1] + "-", ".jar");
                temp.toFile().deleteOnExit();
                Files.copy(in, temp, StandardCopyOption.REPLACE_EXISTING);
                libraries.add(new Installer.Library(fields[0], fields[1], fields[2], temp));
            }
        }
        return libraries;
    }

    private static List<String> readPayloadIndex() throws IOException {
        try (InputStream in = InstallerMain.class.getResourceAsStream(PAYLOAD_INDEX)) {
            if (in == null) {
                throw new InstallException("the installer is missing its payload index — "
                        + "build it with gradlew :fenix-installer:jar");
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).lines()
                    .filter(line -> !line.isBlank())
                    .toList();
        }
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
