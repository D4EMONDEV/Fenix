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
        extension.getLibrary().convention(false);

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

        // The mod compiles against real Minecraft names.
        dependencies.add("compileOnly", project.files(game.clientJar()));
        game.compileLibs().forEach(lib -> dependencies.add("compileOnly", lib));

        // A library is a piece of Fenix itself: it gets Minecraft and stops
        // there. Depending on the API from inside the API would be circular, and
        // there is no mod here to index or launch.
        boolean library = extension.getLibrary().get();
        if (!library) {
            dependencies.add("compileOnly", "fr.d4emon.fenix:fenix-api:" + loaderVersion);
            dependencies.add("annotationProcessor", "fr.d4emon.fenix:fenix-processor:" + loaderVersion);
        }

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

        if (library) {
            // Nothing to run, and nothing to write run configurations for.
            return;
        }

        // The launch process needs the loader, the API and every Minecraft
        // library (natives included — LWJGL extracts them from the classpath).
        dependencies.add(clientClasspath.getName(), "fr.d4emon.fenix:fenix-loader:" + loaderVersion);
        dependencies.add(clientClasspath.getName(), "fr.d4emon.fenix:fenix-api:" + loaderVersion);
        game.compileLibs().forEach(lib -> dependencies.add(clientClasspath.getName(), lib));
        game.nativeLibs().forEach(lib -> dependencies.add(clientClasspath.getName(), lib));

        // The server launch classpath: the loader and API only. The server's own
        // libraries come from the bundle, added when runServer actually runs, so
        // the 60 MB download is not paid on every configure.
        Configuration serverClasspath = project.getConfigurations().create("fenixServerClasspath");
        dependencies.add(serverClasspath.getName(), "fr.d4emon.fenix:fenix-loader:" + loaderVersion);
        dependencies.add(serverClasspath.getName(), "fr.d4emon.fenix:fenix-api:" + loaderVersion);

        Configuration vineflower = project.getConfigurations().create("fenixVineflower");
        dependencies.add(vineflower.getName(),
                "org.vineflower:vineflower:" + readPluginProperties().getProperty("vineflower"));

        registerRunClient(project, game, clientClasspath, fenixMod);
        registerRunServer(project, minecraft, cacheRoot, serverClasspath, fenixMod);
        registerGenSources(project, game, vineflower);
        writeRunConfigs(project);
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

    private void registerRunServer(Project project, String minecraft, Path cacheRoot,
                                   Configuration serverClasspath, Configuration fenixMod) {
        Directory runDir = project.getLayout().getProjectDirectory().dir("run-server");
        var jar = project.getTasks().named("jar");

        var syncServerMods = project.getTasks().register("syncServerMods", Copy.class, task -> {
            task.setDescription("Copies this mod and its Fenix mod dependencies into run-server/mods");
            task.from(jar);
            task.from(fenixMod);
            task.into(runDir.dir("mods"));
        });

        boolean dryRun = project.hasProperty("fenix.dryRun");
        project.getTasks().register("runServer", JavaExec.class, task -> {
            task.setGroup("fenix");
            task.setDescription("Launches the Minecraft dedicated server through Fenix, with this mod installed");
            task.dependsOn(syncServerMods);
            task.getMainClass().set(LAUNCH_MAIN);
            task.setWorkingDir(runDir);

            // The server bundle is large and only needed here, so it is resolved
            // when the task runs rather than at configuration time.
            task.doFirst(unused -> {
                MinecraftServer server = new MinecraftDownloader(cacheRoot).resolveServer(minecraft);
                task.setClasspath(project.files(serverClasspath, server.libraries()));

                List<String> args = new ArrayList<>(List.of(
                        "--fenix.gameJar", server.serverJar().toAbsolutePath().toString(),
                        "--fenix.gameMain", server.mainClass(),
                        "--fenix.side", "server",
                        "--fenix.gameDir", runDir.getAsFile().getAbsolutePath()));
                // Fenix never writes eula=true; accepting the licence is the
                // user's act. Without run-server/eula.txt the server exits early
                // by its own choice, which a dry run sidesteps entirely.
                args.add(dryRun ? "--fenix.dryRun" : "nogui");
                task.setArgs(args);
            });
        });
    }

    private void registerGenSources(Project project, MinecraftLibraries game, Configuration vineflower) {
        var outputDir = project.getLayout().getBuildDirectory().dir("fenix/minecraft-sources");

        project.getTasks().register("genSources", JavaExec.class, task -> {
            task.setGroup("fenix");
            task.setDescription("Decompiles Minecraft with Vineflower for navigation");
            task.setClasspath(vineflower);
            task.getMainClass().set("org.jetbrains.java.decompiler.main.decompiler.ConsoleDecompiler");
            task.getOutputs().dir(outputDir);
            task.doFirst(unused -> outputDir.get().getAsFile().mkdirs());
            task.setArgs(List.of(
                    "-jrt=1", // resolve JDK types from the runtime image, not a stub
                    game.clientJar().toAbsolutePath().toString(),
                    outputDir.get().getAsFile().getAbsolutePath()));
        });
    }

    /**
     * Writes IntelliJ run configurations for the Fenix tasks during a Gradle
     * sync, so they appear in the IDE's run menu without hand-editing. They are
     * Gradle-type configurations: the launch classpath is assembled by the
     * tasks, which an Application configuration could not reproduce.
     */
    private void writeRunConfigs(Project project) {
        if (!Boolean.getBoolean("idea.sync.active")) {
            return;
        }
        for (String taskName : List.of("runClient", "runServer", "genSources")) {
            writeRunConfig(project, taskName);
        }
    }

    private static void writeRunConfig(Project project, String taskName) {
        java.nio.file.Path runConfigDir = project.getProjectDir().toPath().resolve(".run");
        String taskPath = project.getPath().equals(":") ? ":" + taskName : project.getPath() + ":" + taskName;
        String xml = """
                <component name="ProjectRunConfigurationManager">
                  <configuration default="false" name="%s" type="GradleRunConfiguration" factoryName="Gradle">
                    <ExternalSystemSettings>
                      <option name="externalProjectPath" value="$PROJECT_DIR$" />
                      <option name="taskNames">
                        <list>
                          <option value="%s" />
                        </list>
                      </option>
                    </ExternalSystemSettings>
                    <method v="2" />
                  </configuration>
                </component>
                """.formatted(taskName, taskPath);
        try {
            java.nio.file.Files.createDirectories(runConfigDir);
            java.nio.file.Files.writeString(runConfigDir.resolve(taskName + ".run.xml"), xml);
        } catch (IOException e) {
            project.getLogger().warn("Fenix could not write the {} run configuration: {}",
                    taskName, e.getMessage());
        }
    }

    /** The public Fenix Maven repository, served from GitHub Pages. */
    private static final String FENIX_REPO = "https://d4emondev.github.io/Fenix/";

    private static void addRepositories(Project project) {
        var repositories = project.getRepositories();
        // The Fenix artifacts: locally from a developer's ~/.m2, or publicly from
        // GitHub Pages. mavenLocal comes first so an in-development loader wins.
        repositories.mavenLocal();
        repositories.maven(repo -> {
            repo.setName("Fenix");
            repo.setUrl(FENIX_REPO);
        });
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
