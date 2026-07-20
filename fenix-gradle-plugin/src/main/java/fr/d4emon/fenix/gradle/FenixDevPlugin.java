package fr.d4emon.fenix.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.Directory;
import org.gradle.api.tasks.Copy;
import org.gradle.api.tasks.JavaExec;

import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.jvm.toolchain.JavaLanguageVersion;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

/**
 * The plugin a mod author applies: {@code id("fr.d4emon.fenix.dev")}.
 *
 * <p>It turns a plain Java project into a Fenix mod project — Minecraft on the
 * compile classpath under its real names, the Fenix API and annotation
 * processor wired in, and a {@code runClient} task that launches the game
 * through the loader with the mod installed.
 *
 * <p>Minecraft is downloaded once into the Fenix cache; its libraries are
 * ordinary Gradle dependencies. Inside this repository the Fenix coordinates
 * resolve to the sibling projects automatically; for an external mod they come
 * from a Maven repository (today, {@code mavenLocal()} after {@code installFenix}).
 */
public final class FenixDevPlugin implements Plugin<Project> {

    private static final String LAUNCH_MAIN = "fr.d4emon.fenix.loader.launch.Launch";

    @Override
    public void apply(Project project) {
        project.getPluginManager().apply("java-library");

        Properties pluginProperties = readPluginProperties();
        FenixExtension extension = project.getExtensions().create("fenix", FenixExtension.class);
        extension.getMinecraft().convention(pluginProperties.getProperty("minecraft"));
        extension.getLoaderVersion().convention(pluginProperties.getProperty("version"));

        addRepositories(project);

        // The launch classpath: the loader and API, plus Minecraft's libraries.
        // The game jar is not here — it is handed to the loader as a mod-scope
        // path so Mixin can transform it.
        Configuration clientClasspath = project.getConfigurations().create("fenixClientClasspath");
        // Mod-on-mod dependencies: compiled against, and synced into run/mods.
        Configuration fenixMod = project.getConfigurations().create("fenixMod");
        project.getConfigurations().getByName("compileOnly").extendsFrom(fenixMod);

        project.afterEvaluate(unused -> configure(project, extension, clientClasspath, fenixMod));
    }

    private void configure(Project project, FenixExtension extension,
                           Configuration clientClasspath, Configuration fenixMod) {
        String minecraft = extension.getMinecraft().get();
        String loaderVersion = extension.getLoaderVersion().get();

        Path cacheRoot = project.getGradle().getGradleUserHomeDir().toPath().resolve("caches").resolve("fenix");
        MinecraftLibraries game = new MinecraftDownloader(cacheRoot).resolve(minecraft);

        // Compile and run on the Java version the game targets, whatever the
        // Gradle daemon happens to run on.
        var javaExtension = project.getExtensions().getByType(JavaPluginExtension.class);
        javaExtension.getToolchain().getLanguageVersion().set(JavaLanguageVersion.of(game.javaVersion()));

        var dependencies = project.getDependencies();

        // The mod compiles against real Minecraft names and the Fenix API.
        dependencies.add("compileOnly", project.files(game.clientJar()));
        game.compileLibs().forEach(lib -> dependencies.add("compileOnly", lib));
        dependencies.add("compileOnly", "fr.d4emon.fenix:fenix-api:" + loaderVersion);
        dependencies.add("annotationProcessor", "fr.d4emon.fenix:fenix-processor:" + loaderVersion);

        // Let a mod template its metadata: ${version} and ${minecraft_version}
        // in fenix.mod.json are filled in from the build, so the declared
        // version can never drift from the project's.
        var tokens = Map.of(
                "version", project.getVersion().toString(),
                "minecraft_version", minecraft);
        project.getTasks().named("processResources", Copy.class, task -> {
            task.getInputs().properties(tokens);
            task.filesMatching("fenix.mod.json", copy -> copy.expand(tokens));
        });

        // The launch process needs the loader, the API and every Minecraft
        // library (natives included — LWJGL extracts them from the classpath).
        dependencies.add(clientClasspath.getName(), "fr.d4emon.fenix:fenix-loader:" + loaderVersion);
        dependencies.add(clientClasspath.getName(), "fr.d4emon.fenix:fenix-api:" + loaderVersion);
        game.compileLibs().forEach(lib -> dependencies.add(clientClasspath.getName(), lib));
        game.nativeLibs().forEach(lib -> dependencies.add(clientClasspath.getName(), lib));

        registerRunClient(project, game, clientClasspath, fenixMod);
    }

    private void registerRunClient(Project project, MinecraftLibraries game,
                                   Configuration clientClasspath, Configuration fenixMod) {
        Directory runDir = project.getLayout().getProjectDirectory().dir("run");
        var jar = project.getTasks().named("jar");

        // Every mod that will be present at run time: this mod plus its fenixMod
        // dependencies, copied into run/mods where the loader discovers them.
        var syncMods = project.getTasks().register("syncMods", Copy.class, task -> {
            task.setDescription("Copies this mod and its Fenix mod dependencies into run/mods");
            task.from(jar);
            task.from(fenixMod);
            task.into(runDir.dir("mods"));
        });

        project.getTasks().register("runClient", JavaExec.class, task -> {
            task.setGroup("fenix");
            task.setDescription("Launches the Minecraft client through Fenix, with this mod installed");
            task.dependsOn(syncMods);
            task.setClasspath(clientClasspath);
            task.getMainClass().set(LAUNCH_MAIN);
            task.setWorkingDir(runDir);

            List<String> args = new ArrayList<>(List.of(
                    "--fenix.gameJar", game.clientJar().toAbsolutePath().toString(),
                    "--fenix.gameDir", runDir.getAsFile().getAbsolutePath()));
            if (project.hasProperty("fenix.dryRun")) {
                args.add("--fenix.dryRun");
            } else {
                // Reuse the vanilla launcher's already-downloaded assets rather
                // than fetching them again.
                Path assets = defaultMinecraftDir().resolve("assets");
                args.addAll(List.of(
                        "--username", "Dev",
                        "--version", "fenix-dev",
                        "--gameDir", runDir.getAsFile().getAbsolutePath(),
                        "--assetsDir", assets.toString(),
                        "--assetIndex", game.assetIndex(),
                        "--accessToken", "0",
                        "--userType", "legacy"));
            }
            task.setArgs(args);
        });
    }

    private static void addRepositories(Project project) {
        var repositories = project.getRepositories();
        repositories.mavenLocal();
        repositories.mavenCentral();
        repositories.maven(repo -> {
            repo.setName("FabricMC");
            repo.setUrl("https://maven.fabricmc.net/");
        });
        repositories.maven(repo -> {
            repo.setName("MinecraftLibraries");
            repo.setUrl("https://libraries.minecraft.net/");
        });
    }

    private static Path defaultMinecraftDir() {
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

    private static Properties readPluginProperties() {
        try (InputStream in = FenixDevPlugin.class.getResourceAsStream("/fenix-plugin.properties")) {
            if (in == null) {
                throw new IllegalStateException("fenix-plugin.properties is missing from the plugin jar");
            }
            Properties properties = new Properties();
            properties.load(in);
            return properties;
        } catch (IOException e) {
            throw new IllegalStateException("cannot read fenix-plugin.properties", e);
        }
    }
}
