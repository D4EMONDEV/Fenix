import type { ZipEntry } from './zip';

export const FENIX_VERSION = '0.1.5';
export const MINECRAFT_VERSION = '26.2';

const LOADER_VERSION = '0.1.1';
const API_VERSION = '0.3.0';

/** The choices exposed by the project generator. Deliberately small: a new
 * project should describe the author's mod, not spend its first commit deleting ours. */
export interface Options {
  modId: string;
  name: string;
  description: string;
  packageName: string;
  className: string;
  author: string;
  license: string;
  client: boolean;
  ember: boolean;
}

export const DEFAULTS: Options = {
  modId: 'my-mod',
  name: 'My Mod',
  description: 'A Fenix mod.',
  packageName: 'com.example.mymod',
  className: 'MyMod',
  author: '',
  license: 'Apache-2.0',
  client: false,
  ember: false,
};

export function idFromName(name: string): string {
  const id = name
    .trim()
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '');
  return /^[a-z]/.test(id) ? id : id ? `mod-${id}` : 'my-mod';
}

export function classFromName(name: string): string {
  const joined = name
    .trim()
    .split(/[^A-Za-z0-9]+/)
    .filter(Boolean)
    .map((part) => part[0].toUpperCase() + part.slice(1))
    .join('');
  return /^[A-Za-z]/.test(joined) ? joined : `Mod${joined}`;
}

export function problems(options: Options): Partial<Record<keyof Options, string>> {
  const found: Partial<Record<keyof Options, string>> = {};
  if (!/^[a-z][a-z0-9-]*$/.test(options.modId)) {
    found.modId = 'Lowercase letters, digits and hyphens, starting with a letter.';
  }
  if (!/^[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)+$/.test(options.packageName)) {
    found.packageName = 'A Java package: at least two lowercase parts, like com.example.mymod.';
  }
  if (!/^[A-Z][A-Za-z0-9_]*$/.test(options.className)) {
    found.className = 'A Java class name, starting with a capital.';
  }
  if (!options.name.trim()) found.name = 'Give the mod a name.';
  return found;
}

const json = (value: string) => JSON.stringify(value);

function settings(options: Options): string {
  return `pluginManagement {
    repositories {
        maven("https://d4emondev.github.io/Fenix/")
        gradlePluginPortal()
    }
}

rootProject.name = ${json(options.modId)}
`;
}

function build(options: Options): string {
  return `plugins {
    id("fr.d4emon.fenix.dev") version "${FENIX_VERSION}"
}

group = ${json(options.packageName)}
version = "1.0.0"
description = ${json(options.description)}

fenix {
    minecraft = "${MINECRAFT_VERSION}"
}
`;
}

function manifest(options: Options): string {
  const author = options.author ? `,\n  "authors": [${json(options.author)}]` : '';
  const license = options.license === 'none' ? '' : `,\n  "license": ${json(options.license)}`;
  return `{
  "schema": 1,
  "id": ${json(options.modId)},
  "version": "\${version}",
  "name": ${json(options.name)},
  "description": ${json(options.description)}${author}${license},
  "side": "both",
  "depends": {
    "fenix": ">=${LOADER_VERSION}",
    "minecraft": "~\${minecraft_version}",
    "fenix-api": ">=${API_VERSION}"
  }
}
`;
}

function mainClass(options: Options): string {
  return `package ${options.packageName};

import fr.d4emon.fenix.api.Fenix;
import fr.d4emon.fenix.api.FenixMod;
import fr.d4emon.fenix.api.Mod;

@Mod(${json(options.modId)})
public final class ${options.className} implements FenixMod {

    @Override
    public void onInit(Fenix fenix) {
        fenix.logger().info("${options.name} loaded on the {} side", fenix.side());
    }
}
`;
}

function clientClass(options: Options): string {
  return `package ${options.packageName}.client;

import fr.d4emon.fenix.api.Fenix;
import fr.d4emon.fenix.api.FenixMod;
import fr.d4emon.fenix.api.Mod;

/** Client-only setup belongs here. A dedicated server never loads this class. */
@Mod(${json(options.modId)})
public final class ${options.className}Client implements FenixMod {

    @Override
    public void onInit(Fenix fenix) {
    }
}
`;
}

function emberGenerator(options: Options): string {
  return `package ${options.packageName};

import fr.d4emon.fenix.ember.EmberGenerator;
import fr.d4emon.fenix.ember.EmberOutput;
import fr.d4emon.fenix.ember.Generator;

/**
 * Resource generation for ${options.name}.
 * Add only the generators your mod needs; this class intentionally emits nothing yet.
 */
@Generator
public final class ModResources implements EmberGenerator {

    @Override
    public void generate(EmberOutput output) {
    }
}
`;
}

function readme(options: Options): string {
  return `# ${options.name}

${options.description}

A mod for **Minecraft ${MINECRAFT_VERSION}**, built with
[Fenix](https://github.com/D4EMONDEV/Fenix) ${FENIX_VERSION}.

## Run it

\`\`\`bash
./gradlew runClient
\`\`\`

That downloads the game, builds this mod and launches it. Use \`runServer\`
for a dedicated server.

## Build it

\`\`\`bash
./gradlew build
\`\`\`

The jar lands in \`build/libs/\`.
${options.ember ? `
## Generate resources

\`\`\`bash
./gradlew ember
\`\`\`

\`ModResources\` is ready for Ember generators. Its empty method writes no
files, so the project starts cleanly; add only the assets and data your mod needs.
` : ''}
## Project layout

\`\`\`
src/main/java/         common mod code
src/main/resources/    fenix.mod.json and the assets you add${options.client ? '\nsrc/client/java/       client-only code' : ''}${options.ember ? '\nsrc/main/generated/    Ember output (after running ember)' : ''}
\`\`\`

## Documentation

<https://d4emondev.github.io/Fenix/docs/${API_VERSION}/guides/getting-started>
`;
}

const GITIGNORE = `.gradle/
build/
run/
run-server/

.idea/
*.iml
.vscode/
.DS_Store
`;

const GRADLE_PROPERTIES = `org.gradle.jvmargs=-Xmx2G
org.gradle.parallel=true
org.gradle.caching=true
`;

export const WRAPPER: { path: string; source: string; executable?: boolean }[] = [
  { path: 'gradlew', source: 'wrapper/gradlew', executable: true },
  { path: 'gradlew.bat', source: 'wrapper/gradlew.bat' },
  { path: 'gradle/wrapper/gradle-wrapper.jar', source: 'wrapper/gradle/wrapper/gradle-wrapper.jar' },
  { path: 'gradle/wrapper/gradle-wrapper.properties', source: 'wrapper/gradle/wrapper/gradle-wrapper.properties' },
];

export async function wrapper(root: string): Promise<ZipEntry[]> {
  return Promise.all(
    WRAPPER.map(async (file) => {
      const response = await fetch(`${import.meta.env.BASE_URL}${file.source}`);
      if (!response.ok) throw new Error(`${file.source} — ${response.status} ${response.statusText}`);
      return {
        path: `${root}/${file.path}`,
        data: new Uint8Array(await response.arrayBuffer()),
        executable: file.executable,
      };
    }),
  );
}

export function generate(options: Options): ZipEntry[] {
  const root = options.modId;
  const pkg = options.packageName.replace(/\./g, '/');
  const files: ZipEntry[] = [
    { path: `${root}/README.md`, text: readme(options) },
    { path: `${root}/settings.gradle.kts`, text: settings(options) },
    { path: `${root}/build.gradle.kts`, text: build(options) },
    { path: `${root}/gradle.properties`, text: GRADLE_PROPERTIES },
    { path: `${root}/.gitignore`, text: GITIGNORE },
    { path: `${root}/src/main/resources/fenix.mod.json`, text: manifest(options) },
    { path: `${root}/src/main/java/${pkg}/${options.className}.java`, text: mainClass(options) },
  ];
  if (options.ember) files.push({ path: `${root}/src/main/java/${pkg}/ModResources.java`, text: emberGenerator(options) });
  if (options.client) files.push({ path: `${root}/src/client/java/${pkg}/client/${options.className}Client.java`, text: clientClass(options) });
  return files;
}
