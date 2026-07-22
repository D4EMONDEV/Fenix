package fr.d4emon.fenix.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.Directory;
import org.gradle.api.attributes.LibraryElements;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.jvm.tasks.Jar;
import org.gradle.api.file.FileTreeElement;
import org.gradle.api.specs.Spec;
import org.gradle.api.tasks.Copy;
import org.gradle.api.tasks.Sync;
import org.gradle.api.tasks.JavaExec;

import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.jvm.toolchain.JavaLanguageVersion;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicReference;
import java.util.Map;
import java.util.Set;
import java.util.Properties;
import java.util.jar.JarFile;

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
        extension.getApiVersion().convention(pluginProperties.getProperty("api"));
        extension.getLibrary().convention(false);
        extension.getApi().convention(true);

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
        String apiVersion = extension.getApiVersion().get();

        Path cacheRoot = project.getGradle().getGradleUserHomeDir().toPath().resolve("caches").resolve("fenix");
        MinecraftLibraries game = new MinecraftDownloader(cacheRoot).resolve(minecraft);

        // Compile and run on the Java version the game targets, whatever the
        // Gradle daemon happens to run on.
        var javaExtension = project.getExtensions().getByType(JavaPluginExtension.class);
        javaExtension.getToolchain().getLanguageVersion().set(JavaLanguageVersion.of(game.javaVersion()));

        var dependencies = project.getDependencies();

        extension.getClientJar().fileValue(game.clientJar().toFile());

        // Common code compiles against Minecraft with the client half removed,
        // which is what makes reaching for a client class from src/main a
        // javac error rather than a NoClassDefFoundError on somebody else's
        // dedicated server. Client code gets the whole jar, in its own source
        // set — see clientSourceSet below.
        // A project that declares `accessible` compiles against a Minecraft
        // with those doors already open, so what the loader will allow at run
        // time and what javac allows now cannot disagree.
        List<String> widen = Widening.declarations(project.file("src/main/resources/fenix.mod.json"));
        dependencies.add("compileOnly",
                project.files(CommonJar.of(game.clientJar(), minecraft, widen)));
        game.compileLibs().forEach(lib -> dependencies.add("compileOnly", lib));

        // A library is a piece of Fenix itself: it gets Minecraft and stops
        // there. Depending on the API from inside the API would be circular, and
        // there is no mod here to index or launch.
        boolean library = extension.getLibrary().get();
        clientSourceSet(project, game, loaderVersion, apiVersion, library, minecraft, widen);
        if (!library) {
            if (extension.getApi().get()) {
                // fenixMod and not compileOnly, so what a mod compiles against
                // is also what is there when it runs. The two disagreeing is
                // how you get a mod that builds and then cannot find the class
                // it was written against — the exact failure Fenix exists to
                // move earlier. fenixMod feeds compileOnly, so this covers both.
                dependencies.add(fenixMod.getName(), "fr.d4emon.fenix:fenix-api:" + apiVersion);
            }
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
            // A literal replacement rather than `expand`, which runs a Groovy
            // template: a manifest naming a nested class — `MenuType$MenuSupplier`
            // — would have its `$` read as a variable and fail the build.
            // Escaping it would leave the source file invalid JSON, and
            // everything else reads that file as JSON.
            task.filesMatching("fenix.mod.json", copy -> copy.filter(line -> {
                String text = line;
                for (Map.Entry<String, String> token : tokens.entrySet()) {
                    text = text.replace("${" + token.getKey() + "}", token.getValue());
                }
                return text;
            }));
        });

        if (library) {
            // Nothing to run, and nothing to write run configurations for.
            return;
        }

        // The launch process needs the loader, the API and every Minecraft
        // library (natives included — LWJGL extracts them from the classpath).
        dependencies.add(clientClasspath.getName(), "fr.d4emon.fenix:fenix-loader:" + loaderVersion);
        dependencies.add(clientClasspath.getName(), "fr.d4emon.fenix:fenix-api:" + apiVersion);
        game.compileLibs().forEach(lib -> dependencies.add(clientClasspath.getName(), lib));
        game.nativeLibs().forEach(lib -> dependencies.add(clientClasspath.getName(), lib));

        // The server launch classpath: the loader and API only. The server's own
        // libraries come from the bundle, added when runServer actually runs, so
        // the 60 MB download is not paid on every configure.
        Configuration serverClasspath = project.getConfigurations().create("fenixServerClasspath");
        dependencies.add(serverClasspath.getName(), "fr.d4emon.fenix:fenix-loader:" + loaderVersion);
        dependencies.add(serverClasspath.getName(), "fr.d4emon.fenix:fenix-api:" + apiVersion);

        Configuration vineflower = project.getConfigurations().create("fenixVineflower");
        dependencies.add(vineflower.getName(),
                "org.vineflower:vineflower:" + readPluginProperties().getProperty("vineflower"));

        // Ember generates this mod's resources into a source directory that is
        // part of the build, so generated files are reviewable and shipped.
        Directory generated = project.getLayout().getProjectDirectory().dir("src/main/generated");
        project.getExtensions().getByType(org.gradle.api.tasks.SourceSetContainer.class)
                .getByName("main").getResources().srcDir(generated);
        // Ember is a build-time generator, not something a mod runs with. Its
        // own configuration keeps it out of run/mods, where it was being copied
        // into every client and server launch for nothing.
        Configuration ember = project.getConfigurations().create("fenixEmber");
        dependencies.add(ember.getName(), "fr.d4emon.fenix:ember:" + apiVersion);
        // Still on the compile classpath: a mod writes @Generator classes
        // against it. It is only the runtime copy that was pointless.
        dependencies.add("compileOnly", "fr.d4emon.fenix:ember:" + apiVersion);

        registerRunClient(project, game, clientClasspath, fenixMod);
        registerEmber(project, game, clientClasspath, fenixMod, ember, generated);
        registerRunServer(project, minecraft, cacheRoot, serverClasspath, fenixMod);
        registerGenSources(project, game, vineflower);
        writeRunConfigs(project);
    }

    /**
     * Adds the {@code client} source set, when the project has one.
     *
     * <p>The arrangement is one-way on purpose: client code sees common code,
     * common code cannot see client code. That is what a mod author actually
     * wants — write the mod in {@code src/main}, reach into {@code src/client}
     * only to draw it — and it is also the only arrangement the compiler can
     * enforce, since the reverse would need the client jar on the common
     * classpath and the enforcement would evaporate.
     *
     * <p>Its entry class is indexed into a file of its own, so a dedicated
     * server is never told to load a class it cannot resolve.
     */
    private void clientSourceSet(Project project, MinecraftLibraries game, String loaderVersion,
                                 String apiVersion, boolean library, String minecraft,
                                 List<String> widen) {
        Directory clientJava = project.getLayout().getProjectDirectory().dir("src/client/java");
        if (!clientJava.getAsFile().isDirectory()) {
            // Nothing to configure for a mod with no client half, which is most
            // of them. Creating an empty source set would only slow the build.
            return;
        }

        var sourceSets = project.getExtensions().getByType(org.gradle.api.tasks.SourceSetContainer.class);
        SourceSet main = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME);
        SourceSet client = sourceSets.create("client");

        // Whatever common code compiles against, client code compiles against
        // too — Mixin, the mod's own library dependencies, all of it. The
        // client half is a superset of the common half, never a sibling.
        project.getConfigurations().getByName(client.getCompileOnlyConfigurationName())
                .extendsFrom(project.getConfigurations().getByName(main.getCompileOnlyConfigurationName()));
        project.getConfigurations().getByName(client.getImplementationConfigurationName())
                .extendsFrom(project.getConfigurations().getByName(main.getImplementationConfigurationName()));

        var dependencies = project.getDependencies();
        dependencies.add(client.getCompileOnlyConfigurationName(),
                project.files(CommonJar.widened(game.clientJar(), minecraft, widen)));
        game.compileLibs().forEach(lib ->
                dependencies.add(client.getCompileOnlyConfigurationName(), lib));
        if (!library) {
            dependencies.add(client.getCompileOnlyConfigurationName(),
                    "fr.d4emon.fenix:fenix-api:" + apiVersion);
            dependencies.add(client.getAnnotationProcessorConfigurationName(),
                    "fr.d4emon.fenix:fenix-processor:" + loaderVersion);
        }

        client.setCompileClasspath(client.getCompileClasspath().plus(main.getOutput()));
        client.setRuntimeClasspath(client.getOutput()
                .plus(main.getOutput())
                .plus(client.getRuntimeClasspath()));

        project.getTasks().named(client.getCompileJavaTaskName(), JavaCompile.class, task ->
                task.getOptions().getCompilerArgs().add("-Afenix.indexFile=fenix.index.client.json"));

        // Both halves ship in the one jar, and both are visible to whatever
        // depends on this project. The server simply never loads the client
        // half, which is what the separate index arranges.
        //
        project.getTasks().named("jar", Jar.class, task -> task.from(client.getOutput()));

        // Resolve dependencies as jars rather than as classes directories, but
        // only here. Gradle hands a project dependency its main classes
        // directory by default, which is faster and which would hide the client
        // half of another Fenix module entirely — the client classes live in
        // that module's jar, not in its main output. Common compilation keeps
        // the fast path, and keeps not seeing the client half, which is the
        // whole point of the split.
        project.getConfigurations().getByName(client.getCompileClasspathConfigurationName())
                .getAttributes()
                .attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE,
                        project.getObjects().named(LibraryElements.class, LibraryElements.JAR));
    }

    private void registerRunClient(Project project, MinecraftLibraries game,
                                   Configuration clientClasspath, Configuration fenixMod) {
        Directory runDir = project.getLayout().getProjectDirectory().dir("run");
        var jar = project.getTasks().named("jar");

        // Every mod that will be present at run time: this mod plus its fenixMod
        // dependencies, copied into run/mods where the loader discovers them.
        //
        // Sync, not Copy: a copy leaves last build's jars behind, so the first
        // version bump puts two of everything in the directory and the loader
        // refuses to start over duplicate ids — correctly, and about something
        // the author did nothing to cause. The directory mirrors the build.
        var syncMods = project.getTasks().register("syncMods", Sync.class, task -> {
            task.setDescription("Mirrors this mod and its Fenix mod dependencies into run/mods");
            task.from(jar);
            task.from(fenixMod);
            task.exclude(notCarriedInside(fenixMod));
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

    /**
     * Runs Ember: the game boots headlessly, the mod registers its content, and
     * the generators write the resource files that content needs.
     *
     * <p>It uses its own game directory so a generation run cannot disturb a
     * real one, and runs the game through the loader like everything else —
     * Ember has to be inside the transformed scope to see the same Minecraft
     * the mod does.
     */
    private void registerEmber(Project project, MinecraftLibraries game, Configuration clientClasspath,
                               Configuration fenixMod, Configuration ember, Directory generated) {
        Directory runDir = project.getLayout().getBuildDirectory().dir("ember-run").get();
        var jar = project.getTasks().named("jar");

        var syncEmberMods = project.getTasks().register("syncEmberMods", Sync.class, task -> {
            task.setDescription("Mirrors this mod, its Fenix mod dependencies and Ember into the Ember game directory");
            task.from(jar);
            task.from(fenixMod);
            // Only generation needs it, so only generation gets it.
            task.from(ember);
            task.exclude(notCarriedInside(fenixMod));
            task.into(runDir.dir("mods"));
        });

        project.getTasks().register("ember", JavaExec.class, task -> {
            task.setGroup("fenix");
            task.setDescription("Generates this mod's assets and data into src/main/generated");
            task.dependsOn(syncEmberMods);
            task.setClasspath(clientClasspath);
            task.getMainClass().set(LAUNCH_MAIN);
            task.getOutputs().dir(generated);

            task.setArgs(List.of(
                    "--fenix.gameJar", game.clientJar().toAbsolutePath().toString(),
                    "--fenix.gameMain", "fr.d4emon.fenix.ember.EmberRunner",
                    "--fenix.gameDir", runDir.getAsFile().getAbsolutePath(),
                    generated.getAsFile().getAbsolutePath()));
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
            Set<String> carried = carriedInside(fenixMod);
            task.exclude(element -> carried.contains(element.getName())
                    || !isFenixMod(element.getFile()));
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

    /**
     * Whether a file is a Fenix mod, and so belongs in {@code mods/}.
     *
     * <p>A {@code fenixMod} dependency drags its own dependencies along, and
     * some of those are plain libraries — {@code fenix-api-core} above all,
     * which the loader supplies on the parent classpath and which carries no
     * {@code fenix.mod.json}. Copying one into {@code mods/} makes the loader
     * refuse to start, so the sync filters them out.
     */
    /**
     * {@return the names of every jar carried inside the given jars}
     *
     * <p>The API resolves to its bundle <em>and</em> the modules the bundle
     * already carries, because that is what a transitive dependency means to
     * Gradle. Copying both would put each module in {@code mods} twice — once
     * on its own and once unpacked out of the bundle — and the loader would
     * rightly refuse two mods with one id.
     */
    /**
     * {@return a rule excluding jars the bundle already carries, and anything
     * that is not a Fenix mod}
     *
     * <p>The list is read when the task runs, not when it is configured. A
     * bundle is a file, and at configuration time it is last build's file: the
     * first build after a version bump would exclude the previous version's
     * names and let this version's modules through, so every module would land
     * in the directory twice — once loose, once unpacked from the bundle — and
     * the loader would refuse to start over duplicate ids.
     *
     * @param bundles the jars that may carry others inside them
     */
    private static Spec<FileTreeElement> notCarriedInside(Iterable<File> bundles) {
        // Read once per run rather than once per file: opening every bundle
        // for each of its own entries would be quadratic in the module count.
        AtomicReference<Set<String>> carried = new AtomicReference<>();
        return element -> {
            Set<String> names = carried.updateAndGet(
                    known -> known != null ? known : carriedInside(bundles));
            return names.contains(element.getName()) || !isFenixMod(element.getFile());
        };
    }

    private static Set<String> carriedInside(Iterable<File> candidates) {
        Set<String> carried = new HashSet<>();
        for (File file : candidates) {
            if (!file.getName().endsWith(".jar")) {
                continue;
            }
            try (JarFile jar = new JarFile(file)) {
                jar.stream()
                        .map(java.util.zip.ZipEntry::getName)
                        .filter(name -> name.startsWith("META-INF/jars/") && name.endsWith(".jar"))
                        .map(name -> name.substring("META-INF/jars/".length()))
                        .forEach(carried::add);
            } catch (IOException e) {
                // Not readable as a jar, so it carries nothing we can miss.
            }
        }
        return carried;
    }

    private static boolean isFenixMod(File file) {
        if (!file.getName().endsWith(".jar")) {
            return false;
        }
        try (JarFile jar = new JarFile(file)) {
            return jar.getJarEntry("fenix.mod.json") != null;
        } catch (IOException e) {
            return false;
        }
    }

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
