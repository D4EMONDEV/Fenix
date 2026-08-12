/**
 * Builds a Fenix mod project, as a set of files ready to be zipped.
 *
 * Everything here is written to compile and run as generated. That is the whole
 * standard this file is held to: a project that opens with an error, or that
 * launches into the missing-texture checker, teaches a first-time mod author
 * that Fenix is broken — and they have no way to tell that it was the template
 * rather than something they did.
 *
 * Mixins, networking, commands and configuration are not options here. A
 * generator with a checkbox per feature produces a project that is a tour of
 * the API rather than a starting point, and every box left ticked is code
 * somebody has to read before deleting. Those are what the guides are for; this
 * is what a first `runClient` is for.
 *
 * The starter block and item are an option, because they are the one piece
 * somebody either wants to learn from or wants gone before they write a line.
 *
 * The version numbers come from `platforms.json` by way of {@link ./platforms},
 * so they are the ones actually published for the game version chosen.
 */
import { currentPlatform, platforms, pluginVersion, type Platform } from './platforms';
import { writeStarterContent } from './template-content';
import type { ZipEntry } from './zip';

/** The choices that change the shape of the project rather than its text. */
export interface Features {
  /**
   * A block, an item, a creative tab and the classes that own them.
   *
   * <p>Off, the project is its entry point and nothing else — no `ModContent`,
   * no `ModBlocks`, no `ModItems`, no generators, no placeholder art. The
   * resource folders are still there and still empty, so nobody has to work out
   * where a texture goes.
   */
  starterContent: boolean;
  /** Ember generators for models, names, loot tables and recipes. */
  ember: boolean;
  /** A `src/client` source set, kept apart from the code a server runs. */
  splitClient: boolean;
  /** Kotlin build scripts rather than Groovy. */
  kotlin: boolean;
}

export interface Options {
  modName: string;
  modId: string;
  /** Whether {@link modId} follows {@link modName} rather than being typed. */
  autoModId: boolean;
  group: string;
  version: string;
  author: string;
  description: string;
  license: LicenseId;
  minecraft: string;
  features: Features;
}

export type LicenseId = 'Apache-2.0' | 'MIT' | 'GPL-3.0-or-later' | 'ARR';

export const LICENSES: { id: LicenseId; label: string }[] = [
  { id: 'Apache-2.0', label: 'Apache 2.0' },
  { id: 'MIT', label: 'MIT' },
  { id: 'GPL-3.0-or-later', label: 'GPL v3 or later' },
  { id: 'ARR', label: 'All rights reserved' },
];

export const DEFAULT_OPTIONS: Options = {
  modName: 'My Mod',
  modId: 'my-mod',
  autoModId: true,
  group: 'com.example',
  version: '1.0.0',
  author: '',
  description: 'A Minecraft mod built with Fenix.',
  license: 'Apache-2.0',
  minecraft: currentPlatform.minecraft,
  features: { starterContent: true, ember: true, splitClient: true, kotlin: true },
};

/**
 * A mod id is a namespace: it ends up in every resource path the mod owns, and
 * Minecraft's own parser accepts exactly this shape. Rejecting it here beats
 * finding out from a resource that silently fails to load.
 */
export const MOD_ID_PATTERN = /^[a-z][a-z0-9_-]{1,63}$/;

/** A Java package: dot-separated lowercase identifiers. */
export const GROUP_PATTERN = /^[a-z_][a-z0-9_]*(\.[a-z_][a-z0-9_]*)*$/;

/** `My Mod` becomes `my-mod`: what the id field holds until somebody edits it. */
export function idFromName(name: string): string {
  return name
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '')
    .slice(0, 64);
}

/** {@return the errors that would stop the project building, by field} */
export function validate(options: Options): Partial<Record<keyof Options, string>> {
  const errors: Partial<Record<keyof Options, string>> = {};
  if (!options.modName.trim()) {
    errors.modName = 'A name is what the mod list shows.';
  }
  if (!MOD_ID_PATTERN.test(options.modId)) {
    errors.modId = options.autoModId
      ? 'That name leaves nothing usable as an id. Set one by hand.'
      : 'Lowercase letters, digits, underscore and dash; starting with a letter.';
  }
  if (!GROUP_PATTERN.test(options.group)) {
    errors.group = 'A Java package: lowercase words separated by dots.';
  }
  if (!/^\d+(\.\d+)*(-[0-9A-Za-z.-]+)?$/.test(options.version)) {
    errors.version = 'Numbers separated by dots, optionally with a -suffix.';
  }
  return errors;
}

/** The package a mod's classes live in: the group, then the id with dashes dropped. */
export function packageOf(options: Options): string {
  return `${options.group}.${options.modId.replace(/[-_]/g, '')}`;
}

/** `My Mod` becomes `MyMod`; the class name the entry point gets. */
export function classOf(options: Options): string {
  const name = options.modName
    .replace(/[^A-Za-z0-9 ]/g, ' ')
    .split(/\s+/)
    .filter(Boolean)
    .map((word) => word[0].toUpperCase() + word.slice(1))
    .join('');
  // A Java class cannot start with a digit, and a mod called "2Much" is a name
  // somebody will type.
  return /^[A-Za-z]/.test(name) ? name : `Mod${name}`;
}

function platformFor(minecraft: string): Platform {
  return platforms.find((entry) => entry.minecraft === minecraft) ?? currentPlatform;
}

/** The id, as a resource path segment: `my-mod` blocks become `my_mod_block`. */
function snake(modId: string): string {
  return modId.replace(/-/g, '_');
}

const APACHE_NOTICE = `Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.`;

function licenseText(options: Options): string {
  const year = new Date().getFullYear();
  const holder = options.author.trim() || options.modName;
  switch (options.license) {
    case 'Apache-2.0':
      return `Copyright ${year} ${holder}\n\n${APACHE_NOTICE}\n`;
    case 'MIT':
      return `MIT License

Copyright (c) ${year} ${holder}

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
`;
    case 'GPL-3.0-or-later':
      return `Copyright (C) ${year} ${holder}

This program is free software: you can redistribute it and/or modify it under
the terms of the GNU General Public License as published by the Free Software
Foundation, either version 3 of the License, or (at your option) any later
version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY
WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
PARTICULAR PURPOSE. See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with
this program. If not, see <https://www.gnu.org/licenses/>.

The full text of the GNU General Public License version 3 belongs in this file;
it was left out of the generated project because it is long and unmodified. Copy
it from https://www.gnu.org/licenses/gpl-3.0.txt before publishing.
`;
    case 'ARR':
      return `Copyright ${year} ${holder}. All rights reserved.

No permission is granted to use, copy, modify or distribute this software.
`;
  }
}

/**
 * Builds every file of the project.
 *
 * The Gradle wrapper is fetched rather than written: it is four files, one of
 * them a jar, and they are served from this site's own `public/wrapper`. A
 * project without it is one that cannot be built by someone who has no Gradle
 * installed, which is most people.
 */
export async function generate(options: Options): Promise<ZipEntry[]> {
  const platform = platformFor(options.minecraft);
  const pkg = packageOf(options);
  const main = classOf(options);
  const path = pkg.replace(/\./g, '/');
  const id = options.modId;
  const ns = snake(id);
  const upper = ns.toUpperCase();
  const { features } = options;

  const files: ZipEntry[] = [];
  const add = (p: string, text: string) => files.push({ path: p, text });
  const addBytes = (p: string, data: Uint8Array) => files.push({ path: p, data });

  // ---------------------------------------------------------------- build

  const script = features.kotlin ? '.kts' : '';
  const quote = features.kotlin ? '"' : "'";

  // `pluginManagement` first, because Gradle requires it before any other
  // statement in a settings script. The Kotlin DSL tolerates it coming later
  // and the Groovy one does not — it fails the build with "must appear before
  // any other statements" — so writing it second worked for exactly as long as
  // Kotlin was the only option.
  //
  // Assignment rather than Groovy's bare `name 'value'` throughout: the fenix
  // block's members are Gradle `Property` objects, which have no such method,
  // and `minecraft '26.2'` fails with a missing-method error naming the
  // extension rather than the line.
  add(`settings.gradle${script}`, features.kotlin
    ? `// Where the Fenix Gradle plugin itself comes from. The API and the loader
// are added by the plugin; this block is only about the plugin.
pluginManagement {
    repositories {
        maven("https://d4emondev.github.io/Fenix/") { name = "Fenix" }
        gradlePluginPortal()
    }
}

rootProject.name = "${id}"
`
    : `// Where the Fenix Gradle plugin itself comes from. The API and the loader
// are added by the plugin; this block is only about the plugin.
pluginManagement {
    repositories {
        maven {
            name = 'Fenix'
            url = 'https://d4emondev.github.io/Fenix/'
        }
        gradlePluginPortal()
    }
}

rootProject.name = '${id}'
`);

  add(`build.gradle${script}`, `plugins {
    id(${quote}fr.d4emon.fenix.dev${quote}) version ${quote}${pluginVersion}${quote}
}

group = ${quote}${options.group}${quote}
version = ${quote}${options.version}${quote}
description = ${quote}${options.description.replace(/(['"])/g, '\\$1')}${quote}

// The game version, and nothing else. Every other Fenix version is looked up
// for it, so the API this compiles against is the one built for the game it
// asked for. Override a single one — loader, api, ember — only to test a
// release that is not out yet.
fenix {
    minecraft = ${quote}${options.minecraft}${quote}
}
`);

  add('gradle.properties', `# Minecraft ${options.minecraft} needs Java ${platform.java}; the Gradle daemon
# selects that toolchain itself, so this is only about the daemon's own heap.
org.gradle.jvmargs=-Xmx3G
org.gradle.parallel=true
org.gradle.caching=true
`);

  // ---------------------------------------------------------------- metadata

  const metadata = {
    schema: 1,
    id,
    version: '${version}',
    name: options.modName,
    description: options.description,
    authors: options.author.trim() ? [options.author.trim()] : [],
    license: options.license === 'ARR' ? 'All rights reserved' : options.license,
    side: 'both',
    depends: {
      fenix: `>=${platform.loader}`,
      minecraft: `~${options.minecraft}`,
      'fenix-api': `>=${platform.api}`,
    },
  };
  add('src/main/resources/fenix.mod.json', `${JSON.stringify(metadata, null, 2)}\n`);

  // ---------------------------------------------------------------- mod class

  add(`src/main/java/${path}/${main}.java`, `package ${pkg};

${features.starterContent ? `import ${pkg}.content.ModContent;
` : ''}import fr.d4emon.fenix.api.Fenix;
import fr.d4emon.fenix.api.FenixMod;
import fr.d4emon.fenix.api.Mod;

/**
 * The mod's entry point.
 *
 * <p>Nothing in {@code fenix.mod.json} points at this class — the {@link Mod}
 * annotation is the declaration, and the annotation processor records it while
 * this compiles. Mistype the id and the build fails rather than the launch.
 */
@Mod(${main}.MODID)
public final class ${main} implements FenixMod {

    /**
     * The mod id, in one place.
     *
     * <p>The annotation above needs it, the registrar needs it, every resource
     * path is built from it and the client half names it too. A constant is
     * what makes all of them the same string by construction rather than by
     * care. It has to be a compile-time constant to sit in an annotation, which
     * {@code static final String} with a literal is.
     */
    public static final String MODID = "${id}";

    /** Instantiated by the loader from the compile-time index. */
    public ${main}() {
    }
${features.starterContent ? `
    /**
     * Runs once, before the game's registries are frozen. Everything a mod adds
     * to the world is registered from here and from nowhere later.
     */
    @Override
    public void onRegister(Fenix fenix) {
        ModContent.register();
    }
` : ''}
    /** Runs after registration, when a server exists and config can be read. */
    @Override
    public void onInit(Fenix fenix) {
        fenix.logger().info("${options.modName} ready");
    }
}
`);

  if (features.starterContent) {
    writeStarterContent({
      add, addBytes, id, ns, upper, pkg, path, main,
      modName: options.modName, ember: features.ember,
    });
  } else {
    // Ember is wired into every mod project by the Gradle plugin — it is on the
    // compile classpath and `./gradlew ember` exists whatever is ticked here —
    // so wanting it without the starter block is an ordinary thing to want. All
    // that is missing is somewhere to put a generator.
    if (features.ember) {
      files.push({ path: `src/main/java/${path}/data`, directory: true });
    }
    // No starter content, so no resources to describe — but the folders a mod
    // will need are marked out all the same. That a texture belongs under
    // `assets/<id>/textures/block` is a thing to look up once; an empty folder
    // says it without a word.
    //
    // Real directory entries, not a placeholder file: the request was folders
    // and nothing in them. Git does not track an empty directory, so the first
    // commit drops them again — that is git's business, and a file put there to
    // outwit it is exactly the content that was not wanted.
    files.push({ path: `src/main/resources/assets/${id}`, directory: true });
    files.push({ path: `src/main/resources/data/${id}`, directory: true });
  }


  // ---------------------------------------------------------------- client

  if (features.splitClient) {
    add(`src/client/java/${path}/client/${main}Client.java`, `package ${pkg}.client;

import ${pkg}.${main};
import fr.d4emon.fenix.api.Fenix;
import fr.d4emon.fenix.api.FenixMod;
import fr.d4emon.fenix.api.Mod;
import fr.d4emon.fenix.event.client.ClientEvents;

/**
 * The client half of the mod.
 *
 * <p>A second entry point with the same id: the loader runs this one only on a
 * client, so anything here may name a client-only class. The common half may
 * not, and the build enforces that — {@code src/main} compiles against a
 * Minecraft with the client removed, so reaching for a renderer there is a
 * compile error rather than a crash on somebody else's dedicated server.
 */
@Mod(${main}.MODID)
public final class ${main}Client implements FenixMod {

    /** Instantiated by the loader from the compile-time index. */
    public ${main}Client() {
    }

    @Override
    public void onInit(Fenix fenix) {
        ClientEvents.CONNECTED.register(joined ->
                fenix.logger().info("${options.modName}: joined a world"));
    }
}
`);
  }

  // ---------------------------------------------------------------- chrome

  add('.gitignore', `# Gradle
.gradle/
build/

# The plugin's working directories: a launched game, and its logs.
run/
run-server/
logs/
${features.ember ? `
# Ember writes here. Committing it is a choice — see the README.
# src/main/generated/
` : ''}
# IDEs
.idea/
*.iml
.vscode/
`);

  add('LICENSE', licenseText(options));
  add('README.md', readme(options, platform, main, script));

  // ---------------------------------------------------------------- wrapper

  files.push(...await fetchWrapper());
  return files;
}

function readme(options: Options, platform: Platform, main: string, script: string): string {
  const { features } = options;
  const lines: string[] = [
    `# ${options.modName}`,
    '',
    options.description,
    '',
    `A Minecraft ${options.minecraft} mod, built with [Fenix](https://github.com/D4EMONDEV/Fenix).`,
    '',
    '## Running it',
    '',
    '```',
    './gradlew runClient',
    '```',
    '',
    'The first run downloads Minecraft and the Fenix loader into a shared cache,',
    `so it takes a few minutes. Java ${platform.java} is needed to run the game; Gradle`,
    'selects that toolchain itself.',
    '',
    '`./gradlew build` writes the mod jar into `build/libs`. Drop it into a `mods`',
    'folder beside a Fenix installation to play with it.',
    '',
    '## What is here',
    '',
    `- \`build.gradle${script}\` — the game version, and nothing else.`,
    `- \`src/main/java/…/${main}.java\` — the entry point. \`@Mod\` is what the loader finds.`,
    "- `src/main/resources/fenix.mod.json` — the mod's name, version and dependencies.",
  ];

  if (features.starterContent) {
    lines.push('- `…/content/` — a block, an item, a creative tab, and the registrar that owns them.');
  } else {
    lines.push(
      `- \`src/main/resources/assets/${options.modId}/\` and \`data/${options.modId}/\` — empty, and`,
      "  where this mod's resources go. Git does not track an empty directory, so they",
      '  will not survive a commit until something is in them.',
    );
  }

  if (features.ember) {
    lines.push(
      features.starterContent
        ? '- `…/data/` — Ember generators. Run `./gradlew ember` after changing one.'
        : '- `…/data/` — empty, and where an Ember generator goes. Run `./gradlew ember`.',
    );
  }
  if (features.splitClient) {
    lines.push(
      '- `src/client/java/` — the client half. It may name client-only classes;',
      '  `src/main` may not, and the compiler enforces it.',
    );
  }

  if (features.starterContent) {
    lines.push(
      '',
      '## The placeholder art',
      '',
      'The textures under `src/main/resources/assets/` are flat colours the generator',
      'drew, so the first launch shows a block rather than the magenta-and-black',
      'checker. Replace them with 16×16 PNGs of your own.',
    );
  }

  if (features.ember) {
    lines.push(
      '',
      '## Generated resources',
      '',
      'Ember writes models, names, loot tables and recipes into `src/main/generated`.',
      'It is on the compile classpath of every Fenix mod and `./gradlew ember` always',
      'exists, so a generator can be added at any point — a class extending one of the',
      'Ember providers and annotated `@Generator`.',
      'Whether to commit that directory is a real choice: committing it makes a diff',
      'show what a generator change actually produced, and ignoring it keeps the',
      'history smaller. `.gitignore` has the line, commented out.',
    );
  }

  lines.push(
    '',
    '## Versions',
    '',
    '| | |',
    '|---|---|',
    `| Minecraft | ${options.minecraft} |`,
    `| Java | ${platform.java} |`,
    `| Fenix loader | ${platform.loader} |`,
    `| Fenix API | ${platform.api} |`,
    '',
    'The build names only the Minecraft version. The Fenix Gradle plugin looks the',
    'rest up, so the API a mod compiles against is always the one built for the game',
    'it asked for.',
    '',
  );

  return lines.join('\n');
}

/**
 * Fetches the Gradle wrapper this site serves.
 *
 * A generated project has to be buildable by somebody with no Gradle installed,
 * which is most people — and a wrapper is four files, one of them a jar, so it
 * cannot be written from source here.
 */
async function fetchWrapper(): Promise<ZipEntry[]> {
  const parts: { from: string; to: string; binary?: boolean; executable?: boolean }[] = [
    { from: '/wrapper/gradle/wrapper/gradle-wrapper.jar', to: 'gradle/wrapper/gradle-wrapper.jar', binary: true },
    { from: '/wrapper/gradle/wrapper/gradle-wrapper.properties', to: 'gradle/wrapper/gradle-wrapper.properties' },
    { from: '/wrapper/gradlew', to: 'gradlew', executable: true },
    { from: '/wrapper/gradlew.bat', to: 'gradlew.bat' },
  ];

  return Promise.all(parts.map(async (part) => {
    const response = await fetch(part.from);
    if (!response.ok) {
      throw new Error(`could not read ${part.from}: ${response.status}`);
    }
    if (part.binary) {
      return { path: part.to, data: new Uint8Array(await response.arrayBuffer()) };
    }
    return { path: part.to, text: await response.text(), executable: part.executable };
  }));
}
