/**
 * Builds a complete Fenix mod project, as a set of files ready to be zipped.
 *
 * Everything here is written to compile and run as generated. That is the whole
 * standard this file is held to: a project that opens with an error, or that
 * launches into the missing-texture checker, teaches a first-time mod author
 * that Fenix is broken — and they have no way to tell that it was the template
 * rather than something they did.
 *
 * The version numbers come from `platforms.json` by way of {@link ./platforms},
 * so they are the ones actually published for the game version chosen.
 */
import { currentPlatform, platforms, pluginVersion, type Platform } from './platforms';
import { texture } from './png';
import type { ZipEntry } from './zip';

/** What the generator can add to a project, beyond the mod class itself. */
export interface Features {
  /** A `src/client` source set: rendering, key bindings, screens. */
  client: boolean;
  /** A block and an item, registered, with textures and a creative tab. */
  content: boolean;
  /** Ember generators for models, language, loot tables and recipes. */
  ember: boolean;
  /** A mixin, its config file, and the metadata entry that loads it. */
  mixins: boolean;
  /** A record-backed configuration file. */
  config: boolean;
  /** A command, registered through the event bus. */
  commands: boolean;
  /** A payload in each direction. */
  networking: boolean;
  /** A GitHub Actions workflow that builds the mod on every push. */
  ci: boolean;
}

export interface Options {
  modId: string;
  modName: string;
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
  modId: 'my-mod',
  modName: 'My Mod',
  group: 'com.example',
  version: '1.0.0',
  author: '',
  description: 'A Minecraft mod built with Fenix.',
  license: 'Apache-2.0',
  minecraft: currentPlatform.minecraft,
  features: {
    client: true,
    content: true,
    ember: true,
    mixins: false,
    config: false,
    commands: false,
    networking: false,
    ci: true,
  },
};

/**
 * A mod id is a namespace: it ends up in every resource path the mod owns, and
 * Minecraft's own parser accepts exactly this shape. Rejecting it here beats
 * finding out from a resource that silently fails to load.
 */
export const MOD_ID_PATTERN = /^[a-z][a-z0-9_-]{1,63}$/;

/** A Java package: dot-separated lowercase identifiers. */
export const GROUP_PATTERN = /^[a-z_][a-z0-9_]*(\.[a-z_][a-z0-9_]*)*$/;

/** {@return the errors that would stop the project building, by field} */
export function validate(options: Options): Partial<Record<keyof Options, string>> {
  const errors: Partial<Record<keyof Options, string>> = {};
  if (!MOD_ID_PATTERN.test(options.modId)) {
    errors.modId = 'Lowercase letters, digits, underscore and dash; starting with a letter.';
  }
  if (!GROUP_PATTERN.test(options.group)) {
    errors.group = 'A Java package: lowercase words separated by dots.';
  }
  if (!options.modName.trim()) {
    errors.modName = 'A name is what the mod list shows.';
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

function licenseText(options: Options): string | null {
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
  const { features } = options;

  const files: ZipEntry[] = [];
  const add = (p: string, text: string) => files.push({ path: p, text });
  const addBytes = (p: string, data: Uint8Array) => files.push({ path: p, data });

  // ---------------------------------------------------------------- build

  add('settings.gradle.kts', `rootProject.name = "${id}"

// Where the Fenix Gradle plugin itself is resolved from. The Fenix API and
// loader are added by the plugin; this block is only about the plugin.
pluginManagement {
    repositories {
        maven("https://d4emondev.github.io/Fenix/") { name = "Fenix" }
        gradlePluginPortal()
    }
}
`);

  const dependencyLines: string[] = [];
  if (features.mixins) {
    dependencyLines.push(
      '',
      '// Mixin is on the compile classpath because this mod writes one. It is',
      '// already at run time — the loader starts it — so this is compileOnly.',
      'dependencies {',
      '    compileOnly("net.fabricmc:sponge-mixin:0.17.3+mixin.0.8.7")',
      '}',
    );
  }

  add('build.gradle.kts', `plugins {
    id("fr.d4emon.fenix.dev") version "${pluginVersion}"
}

group = "${options.group}"
version = "${options.version}"
description = "${options.description.replace(/"/g, '\\"')}"

fenix {
    minecraft = "${options.minecraft}"
}
${dependencyLines.join('\n')}
`);

  add('gradle.properties', `# Minecraft ${options.minecraft} needs Java ${platform.java}; the Gradle daemon
# selects that toolchain itself, so this is only about the daemon's own heap.
org.gradle.jvmargs=-Xmx3G
org.gradle.parallel=true
org.gradle.caching=true
`);

  // ---------------------------------------------------------------- metadata

  const depends: Record<string, string> = {
    fenix: `>=${platform.loader}`,
    minecraft: `~${options.minecraft}`,
    'fenix-api': `>=${platform.api}`,
  };

  const metadata: Record<string, unknown> = {
    schema: 1,
    id,
    version: '${version}',
    name: options.modName,
    description: options.description,
    authors: options.author.trim() ? [options.author.trim()] : [],
    license: options.license === 'ARR' ? 'All rights reserved' : options.license,
    side: 'both',
    depends,
  };
  if (features.mixins) {
    metadata.mixins = [`${id}.mixins.json`];
  }

  add('src/main/resources/fenix.mod.json', `${JSON.stringify(metadata, null, 2)}\n`);

  // ---------------------------------------------------------------- mod class

  const imports = new Set<string>([
    'fr.d4emon.fenix.api.Fenix',
    'fr.d4emon.fenix.api.FenixMod',
    'fr.d4emon.fenix.api.Mod',
  ]);
  const registerBody: string[] = [];
  const initBody: string[] = [];
  const fields: string[] = [];

  if (features.content) {
    imports.add(`${pkg}.content.ModContent`);
    registerBody.push('        ModContent.register();');
  }
  if (features.config) {
    imports.add('fr.d4emon.fenix.config.Config');
    fields.push(`    /** Read once, when the mod starts. */
    private Config<${main}Config> config;
`);
    initBody.push(`        config = Config.of(fenix, ${main}Config.DEFAULTS);`);
  }
  if (features.commands) {
    initBody.push(`        ${main}Commands.register();`);
  }
  if (features.networking) {
    initBody.push(`        ${main}Payloads.listen();`);
  }

  initBody.push(
    features.config
      ? `        fenix.logger().info("${options.modName} ready — greeting is {}", config.get().greeting());`
      : `        fenix.logger().info("${options.modName} ready");`,
  );

  add(`src/main/java/${path}/${main}.java`, `package ${pkg};

${[...imports].sort().map((i) => `import ${i};`).join('\n')}

/**
 * The mod's entry point.
 *
 * <p>Nothing in {@code fenix.mod.json} points at this class — the {@link Mod}
 * annotation is the declaration, and the annotation processor records it while
 * this compiles. Mistype the id and the build fails rather than the launch.
 */
@Mod("${id}")
public final class ${main} implements FenixMod {

${fields.join('\n')}    /** Instantiated by the loader from the compile-time index. */
    public ${main}() {
    }
${
  registerBody.length
    ? `
    /**
     * Runs once, before the game's registries are frozen. Everything a mod adds
     * to the world is registered from here and from nowhere later.
     */
    @Override
    public void onRegister(Fenix fenix) {
${registerBody.join('\n')}
    }
`
    : ''
}
    /** Runs after registration, when it is safe to read config and listen. */
    @Override
    public void onInit(Fenix fenix) {
${initBody.join('\n')}
    }
}
`);

  // ---------------------------------------------------------------- content

  if (features.content) {
    add(`src/main/java/${path}/content/ModContent.java`, `package ${pkg}.content;

import fr.d4emon.fenix.registry.Registrar;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;

/**
 * Everything this mod adds to the game.
 *
 * <p>The fields are declared here and registered by {@link #register()}, which
 * the mod calls from {@code onRegister}. Declaring and registering are separate
 * on purpose: a static field initialises the first time its class is touched,
 * and a mod that registered from a field initialiser would register at whatever
 * moment something first read one — which is not a moment anybody chose.
 */
public final class ModContent {

    /** Owns the mod's namespace, and every id derived from it. */
    public static final Registrar REGISTRAR = Registrar.of("${id}");

    /** The creative tab holding this mod's items. */
    public static final ResourceKey<CreativeModeTab> TAB =
            REGISTRAR.creativeTab("${ns}", ModItems.${ns.toUpperCase()}_INGOT);

    private ModContent() {
    }

    /** Hands every declaration above to the game. Called once, from onRegister. */
    public static void register() {
        // Touching the classes runs their static initialisers, which is what
        // fills the registrar. The order does not matter; nothing is added to
        // the game until apply() below.
        ModBlocks.touch();
        ModItems.touch();
        REGISTRAR.apply();
    }
}
`);

    add(`src/main/java/${path}/content/ModBlocks.java`, `package ${pkg}.content;

import fr.d4emon.fenix.registry.Holder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;

/** The blocks this mod adds. */
public final class ModBlocks {

    /**
     * A plain decorative block that drops itself.
     *
     * <p>{@code withItem} is what puts it in an inventory: a block and the item
     * that places it are two registrations in Minecraft, and a block without the
     * second one exists in the world and cannot be picked up.
     */
    public static final Holder<Block> ${ns.toUpperCase()}_BLOCK = ModContent.REGISTRAR
            .newBlock("${ns}_block")
            .strength(3.0f, 6.0f)
            .requiresTool()
            .sound(SoundType.METAL)
            .withItem()
            .register();

    private ModBlocks() {
    }

    /** Loads this class, and with it every declaration above. */
    static void touch() {
    }
}
`);

    add(`src/main/java/${path}/content/ModItems.java`, `package ${pkg}.content;

import fr.d4emon.fenix.registry.Holder;
import net.minecraft.world.item.Item;

/** The items this mod adds. */
public final class ModItems {

    /** A crafting material. */
    public static final Holder<Item> ${ns.toUpperCase()}_INGOT = ModContent.REGISTRAR
            .newItem("${ns}_ingot")
            .stacksTo(64)
            .register();

    private ModItems() {
    }

    /** Loads this class, and with it every declaration above. */
    static void touch() {
    }
}
`);

    // Placeholder art, so the first launch shows a block rather than the
    // missing-texture checker.
    addBytes(`src/main/resources/assets/${id}/textures/block/${ns}_block.png`, texture(0x8a6a3f));
    addBytes(`src/main/resources/assets/${id}/textures/item/${ns}_ingot.png`, texture(0xd8a44a, true));

    if (!features.ember) {
      // Without Ember these files are written by hand, so the template writes
      // them once — a block with no model is invisible, and a block with no
      // loot table drops nothing, both silently.
      add(`src/main/resources/assets/${id}/blockstates/${ns}_block.json`,
        `{\n  "variants": {\n    "": { "model": "${id}:block/${ns}_block" }\n  }\n}\n`);
      add(`src/main/resources/assets/${id}/models/block/${ns}_block.json`,
        `{\n  "parent": "minecraft:block/cube_all",\n  "textures": { "all": "${id}:block/${ns}_block" }\n}\n`);
      add(`src/main/resources/assets/${id}/items/${ns}_block.json`,
        `{\n  "model": { "type": "minecraft:model", "model": "${id}:block/${ns}_block" }\n}\n`);
      add(`src/main/resources/assets/${id}/models/item/${ns}_ingot.json`,
        `{\n  "parent": "minecraft:item/generated",\n  "textures": { "layer0": "${id}:item/${ns}_ingot" }\n}\n`);
      add(`src/main/resources/assets/${id}/items/${ns}_ingot.json`,
        `{\n  "model": { "type": "minecraft:model", "model": "${id}:item/${ns}_ingot" }\n}\n`);
      add(`src/main/resources/assets/${id}/lang/en_us.json`, `${JSON.stringify({
        [`block.${id}.${ns}_block`]: `${options.modName} Block`,
        [`item.${id}.${ns}_ingot`]: `${options.modName} Ingot`,
        [`itemGroup.${id}.${ns}`]: options.modName,
      }, null, 2)}\n`);
      add(`src/main/resources/data/${id}/loot_table/blocks/${ns}_block.json`, `{
  "type": "minecraft:block",
  "pools": [
    {
      "rolls": 1.0,
      "conditions": [ { "condition": "minecraft:survives_explosion" } ],
      "entries": [ { "type": "minecraft:item", "name": "${id}:${ns}_block" } ]
    }
  ],
  "random_sequence": "${id}:blocks/${ns}_block"
}
`);
    }
  }

  // ---------------------------------------------------------------- ember

  if (features.ember && features.content) {
    add(`src/main/java/${path}/data/ModModels.java`, `package ${pkg}.data;

import ${pkg}.content.ModBlocks;
import ${pkg}.content.ModItems;
import fr.d4emon.fenix.ember.EmberModelProvider;
import fr.d4emon.fenix.ember.Generator;

/** Block and item models, and the blockstate files that point at them. */
@Generator
public final class ModModels extends EmberModelProvider {

    /** Instantiated by Ember. */
    public ModModels() {
    }

    @Override
    protected void models() {
        cubeAll(ModBlocks.${ns.toUpperCase()}_BLOCK);
        flatItem(ModItems.${ns.toUpperCase()}_INGOT);
    }
}
`);

    add(`src/main/java/${path}/data/ModLanguage.java`, `package ${pkg}.data;

import ${pkg}.content.ModBlocks;
import ${pkg}.content.ModItems;
import fr.d4emon.fenix.ember.EmberLanguageProvider;
import fr.d4emon.fenix.ember.Generator;

/**
 * English names.
 *
 * <p>For another language, subclass again and pass its code to the constructor:
 * {@code super("fr_fr")}.
 */
@Generator
public final class ModLanguage extends EmberLanguageProvider {

    /** Instantiated by Ember. */
    public ModLanguage() {
    }

    @Override
    protected void translations() {
        add(ModBlocks.${ns.toUpperCase()}_BLOCK, "${options.modName} Block");
        add(ModItems.${ns.toUpperCase()}_INGOT, "${options.modName} Ingot");
        add("itemGroup.${id}.${ns}", "${options.modName}");
    }
}
`);

    add(`src/main/java/${path}/data/ModLootTables.java`, `package ${pkg}.data;

import ${pkg}.content.ModBlocks;
import fr.d4emon.fenix.ember.EmberLootTableProvider;
import fr.d4emon.fenix.ember.Generator;

/**
 * What blocks drop.
 *
 * <p>A block with no loot table drops nothing at all, silently — the single
 * most common surprise when adding a block by hand.
 */
@Generator
public final class ModLootTables extends EmberLootTableProvider {

    /** Instantiated by Ember. */
    public ModLootTables() {
    }

    @Override
    protected void lootTables() {
        dropsSelf(ModBlocks.${ns.toUpperCase()}_BLOCK);
    }
}
`);

    add(`src/main/java/${path}/data/ModRecipes.java`, `package ${pkg}.data;

import ${pkg}.content.ModBlocks;
import ${pkg}.content.ModItems;
import fr.d4emon.fenix.ember.EmberRecipeProvider;
import fr.d4emon.fenix.ember.Generator;

/** Crafting recipes. */
@Generator
public final class ModRecipes extends EmberRecipeProvider {

    /** Instantiated by Ember. */
    public ModRecipes() {
    }

    @Override
    protected void recipes() {
        // Nine ingots into a block, and the block back into nine ingots. The
        // second needs a name of its own: both produce a recipe file named
        // after the result, and two recipes cannot share one.
        shaped(ModBlocks.${ns.toUpperCase()}_BLOCK)
                .pattern("###", "###", "###")
                .define('#', ModItems.${ns.toUpperCase()}_INGOT)
                .save();

        shapeless(ModItems.${ns.toUpperCase()}_INGOT, 9)
                .ingredient(ModBlocks.${ns.toUpperCase()}_BLOCK)
                .named("${ns}_ingot_from_block")
                .save();
    }
}
`);
  }

  // ---------------------------------------------------------------- config

  if (features.config) {
    add(`src/main/java/${path}/${main}Config.java`, `package ${pkg};

/**
 * This mod's settings.
 *
 * <p>A record, not a builder and not a map: the field names are the JSON keys,
 * the types are the validation, and a setting that is read anywhere is a field
 * the compiler knows about. The compact constructor is where a value that
 * parses but makes no sense is rejected — that happens once, at load, with the
 * file named, rather than at the point of use with nothing to point at.
 */
public record ${main}Config(boolean enabled, int limit, String greeting) {

    /** Written to disk the first time the mod runs. */
    public static final ${main}Config DEFAULTS =
            new ${main}Config(true, 10, "${options.modName} is loaded.");

    /** @throws IllegalArgumentException if the file holds a value that cannot work */
    public ${main}Config {
        if (limit < 1) {
            throw new IllegalArgumentException("limit must be at least 1");
        }
    }
}
`);
  }

  // ---------------------------------------------------------------- commands

  if (features.commands) {
    add(`src/main/java/${path}/${main}Commands.java`, `package ${pkg};

import com.mojang.brigadier.arguments.IntegerArgumentType;
import fr.d4emon.fenix.command.CommandEvents;
import net.minecraft.network.chat.Component;

import static fr.d4emon.fenix.command.Commands.argument;
import static fr.d4emon.fenix.command.Commands.literal;
import static fr.d4emon.fenix.command.Commands.operator;
import static fr.d4emon.fenix.command.Commands.run;

/** The commands this mod adds. */
public final class ${main}Commands {

    private ${main}Commands() {
    }

    /**
     * Listens for the registration event.
     *
     * <p>Registering the listener once is enough: the server fires it on start
     * and again on every datapack reload, which is when a dispatcher is rebuilt
     * and a command registered any other way would quietly disappear.
     */
    public static void register() {
        CommandEvents.REGISTER.register(registration -> registration.dispatcher().register(
                literal("${ns}")
                        .requires(operator())
                        .then(argument("times", IntegerArgumentType.integer(1, 64))
                                .executes(run(context -> {
                                    int times = IntegerArgumentType.getInteger(context, "times");
                                    context.getSource().sendSuccess(
                                            () -> Component.literal("Hello ".repeat(times)), false);
                                })))
                        .executes(run(context -> context.getSource().sendSuccess(
                                () -> Component.literal("Hello from ${options.modName}"), false)))));
    }
}
`);
  }

  // ---------------------------------------------------------------- network

  if (features.networking) {
    add(`src/main/java/${path}/${main}Payloads.java`, `package ${pkg};

import fr.d4emon.fenix.network.ToClient;
import fr.d4emon.fenix.network.ToServer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

/**
 * The messages this mod sends between the client and the server.
 *
 * <p>One record per message, with the codec that puts it on the wire. Both
 * sides read this file, so a field added to a record is added to both ends at
 * once — which is the failure this shape exists to prevent.
 */
public final class ${main}Payloads {

    /** Server to client: something happened worth showing. */
    public record Ping(String message) {
        static final StreamCodec<FriendlyByteBuf, Ping> CODEC = StreamCodec.of(
                (buffer, value) -> buffer.writeUtf(value.message()),
                buffer -> new Ping(buffer.readUtf()));
    }

    /** Client to server: the player asked for something. */
    public record Request(int amount) {
        static final StreamCodec<FriendlyByteBuf, Request> CODEC = StreamCodec.of(
                (buffer, value) -> buffer.writeVarInt(value.amount()),
                buffer -> new Request(buffer.readVarInt()));
    }

    /** Sent with {@code PING.send(player, new Ping("…"))}. */
    public static final ToClient<Ping> PING =
            ToClient.of(Identifier.fromNamespaceAndPath("${id}", "ping"), Ping.CODEC);

    /** Sent from the client with {@code REQUEST.send(new Request(1))}. */
    public static final ToServer<Request> REQUEST =
            ToServer.of(Identifier.fromNamespaceAndPath("${id}", "request"), Request.CODEC);

    private ${main}Payloads() {
    }

    /**
     * Starts listening for what the client sends.
     *
     * <p>The handler runs on the server thread, and {@code player} is the one
     * that sent it. Never trust the contents: a payload is whatever arrived on
     * a socket, and checking it here is the only place it gets checked.
     */
    public static void listen() {
        REQUEST.receive((request, player) -> {
            if (request.amount() < 1 || request.amount() > 64) {
                return;
            }
            PING.send(player, new Ping("asked for " + request.amount()));
        });
    }
}
`);
  }

  // ---------------------------------------------------------------- mixins

  if (features.mixins) {
    add(`src/main/resources/${id}.mixins.json`, `{
  "required": true,
  "minVersion": "0.8.7",
  "package": "${pkg}.mixin",
  "compatibilityLevel": "JAVA_${platform.java}",
  "injectors": {
    "_comment": "Every injection must land. An injection that stopped matching should fail loudly rather than leave a mixin that silently does nothing.",
    "defaultRequire": 1
  },
  "mixins": [
    "MinecraftServerMixin"
  ]
}
`);

    add(`src/main/java/${path}/mixin/MinecraftServerMixin.java`, `package ${pkg}.mixin;

import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

/**
 * A worked example: says something the first time the server ticks.
 *
 * <p>Three rules this file is shaped by.
 *
 * <p>Everything in this package belongs to Mixin. A config owns every class
 * under the package it declares, so an ordinary helper class here fails to
 * load — put those anywhere else.
 *
 * <p>Members a mixin adds are prefixed and marked {@link Unique}. Two mods
 * mixing into the same class would otherwise collide on a field called
 * {@code announced}, and the one that lost would be silently overwritten.
 *
 * <p>{@code remap = false} because Minecraft ships unobfuscated since 26.1.
 * The name in {@code method} is the real one, and there is no mapping step
 * that could translate it.
 *
 * <p>Reach for a mixin only when the API has no event for what you need. An
 * injection is bound to a method under the name it has today, and Minecraft
 * renames things every release — an event survives that, an injection does not.
 */
@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {

    @Unique
    private boolean ${ns}$announced;

    /** Merged into MinecraftServer; never constructed directly. */
    public MinecraftServerMixin() {
    }

    @Inject(method = "tickServer", at = @At("HEAD"), remap = false)
    private void ${ns}$onFirstTick(BooleanSupplier hasTimeLeft, CallbackInfo ci) {
        if (${ns}$announced) {
            return;
        }
        ${ns}$announced = true;
        LogUtils.getLogger().info("${options.modName}: the server is ticking");
    }
}
`);
  }

  // ---------------------------------------------------------------- client

  if (features.client) {
    const clientImports = new Set<string>([
      'fr.d4emon.fenix.api.Fenix',
      'fr.d4emon.fenix.api.FenixMod',
      'fr.d4emon.fenix.api.Mod',
      'fr.d4emon.fenix.event.client.ClientEvents',
    ]);
    add(`src/client/java/${path}/client/${main}Client.java`, `package ${pkg}.client;

${[...clientImports].sort().map((i) => `import ${i};`).join('\n')}

/**
 * The client half of the mod.
 *
 * <p>A second entry point with the same id: the loader runs this one only on a
 * client, so anything here may name a client-only class. The common half may
 * not, and the build enforces that — {@code src/main} compiles against a
 * Minecraft with the client removed, so reaching for one there is a compile
 * error rather than a crash on somebody else's dedicated server.
 */
@Mod("${id}")
public final class ${main}Client implements FenixMod {

    /** Instantiated by the loader from the compile-time index. */
    public ${main}Client() {
    }

    @Override
    public void onInit(Fenix fenix) {
        // Fires each time this client joins a world — the moment per-world
        // client state should be built, and DISCONNECTED the moment it should
        // be thrown away. Keeping a cache past a disconnect is how a mod
        // carries one world's state into the next and is quietly wrong.
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

# Ember writes here. Committing it is a choice — see the README.
# src/main/generated/

# IDEs
.idea/
*.iml
.vscode/
`);

  const licence = licenseText(options);
  if (licence) {
    add('LICENSE', licence);
  }

  add('README.md', readme(options, platform, main));

  if (features.ci) {
    add('.github/workflows/build.yml', `name: Build

on:
  push:
    branches: ["**"]
  pull_request:

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      # Minecraft ${options.minecraft} runs on Java ${platform.java}; the Gradle
      # plugin selects that toolchain for compiling the mod itself.
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '${platform.java}'

      - uses: gradle/actions/setup-gradle@v4

      - run: ./gradlew build

      - uses: actions/upload-artifact@v4
        with:
          name: ${id}
          path: build/libs/*.jar
`);
  }

  // ---------------------------------------------------------------- wrapper

  const wrapper = await fetchWrapper();
  files.push(...wrapper);

  return files;
}

function readme(options: Options, platform: Platform, main: string): string {
  const { features } = options;
  const lines: string[] = [];
  lines.push(`# ${options.modName}`, '', options.description, '');
  lines.push(
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
    '```',
    './gradlew build',
    '```',
    '',
    'writes the mod jar into `build/libs`. Drop it into a `mods` folder beside a',
    'Fenix installation to play with it.',
    '',
    '## What is here',
    '',
  );

  const map: [boolean, string][] = [
    [true, `\`src/main/java/…/${main}.java\` — the entry point. \`@Mod\` is what the loader finds.`],
    [true, '`src/main/resources/fenix.mod.json` — the mod\'s name, version and dependencies.'],
    [features.content, '`…/content/` — the blocks and items, and the registrar that owns them.'],
    [features.ember, '`…/data/` — Ember generators. Run `./gradlew ember` after changing them.'],
    [features.config, `\`${main}Config.java\` — settings, as a record. Written to \`run/config/${options.modId}\`.`],
    [features.commands, `\`${main}Commands.java\` — a command, registered through the event bus.`],
    [features.networking, `\`${main}Payloads.java\` — one message each way, with its codec.`],
    [features.mixins, `\`…/mixin/\` — a worked mixin. Everything in that package belongs to Mixin.`],
    [features.client, `\`src/client/java/\` — the client half. It may name client-only classes; \`src/main\` may not.`],
  ];
  for (const [on, text] of map) {
    if (on) {
      lines.push(`- ${text}`);
    }
  }

  if (features.content) {
    lines.push(
      '',
      '## The placeholder art',
      '',
      'The textures under `src/main/resources/assets/` are flat colours the',
      'generator drew, so the first launch shows a block rather than the',
      'magenta-and-black checker. Replace them with 16×16 PNGs of your own.',
    );
  }

  if (features.ember) {
    lines.push(
      '',
      '## Generated resources',
      '',
      'Ember writes models, language files, loot tables and recipes into',
      '`src/main/generated`. Run `./gradlew ember` after changing a generator.',
      '',
      'Whether to commit that directory is a real choice: committing it makes a',
      'diff show what a generator change actually produced, and ignoring it keeps',
      'the history smaller. `.gitignore` has the line, commented out.',
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
    'The build names only the Minecraft version. The Fenix Gradle plugin looks up',
    'the rest, so the API a mod compiles against is always the one built for the',
    'game it asked for.',
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
