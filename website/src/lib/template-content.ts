/**
 * The starter content a generated project can come with: a block, an item, a
 * creative tab, and either Ember generators or the resource files written by
 * hand.
 *
 * Its own file because it is most of what the generator writes and none of what
 * the generator decides. A project without it is the entry point and the build
 * files, which is the whole of {@link ./template} once this is out of the way.
 */
import { texture } from './png';
import type { ZipEntry } from './zip';

/** What the caller already worked out, rather than working it out again here. */
export interface ContentContext {
  /** Adds a text file to the project. */
  add: (path: string, text: string) => void;
  /** Adds a binary file. */
  addBytes: (path: string, data: Uint8Array) => void;
  /** The mod id, e.g. `my-mod`. */
  id: string;
  /** The id as a resource path segment, e.g. `my_mod`. */
  ns: string;
  /** `ns`, upper-cased: the constant names. */
  upper: string;
  /** The mod's Java package. */
  pkg: string;
  /** That package as a directory path. */
  path: string;
  /** The entry point's class name, which owns MODID. */
  main: string;
  /** The mod's display name, for the strings a player reads. */
  modName: string;
  /** Whether Ember writes the resources, or the template does. */
  ember: boolean;
}

/** Writes the block, the item, and everything that describes them. */
export function writeStarterContent(context: ContentContext): ZipEntry[] {
  const { add, addBytes, id, ns, upper, pkg, path, main, modName, ember } = context;
  const options = { modName };
  const features = { ember };

  // ---------------------------------------------------------------- content

  add(`src/main/java/${path}/content/ModContent.java`, `package ${pkg}.content;

import ${pkg}.${main};
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
    public static final Registrar REGISTRAR = Registrar.of(${main}.MODID);

    /** The creative tab holding this mod's items. */
    public static final ResourceKey<CreativeModeTab> TAB =
            REGISTRAR.creativeTab("${ns}", ModItems.${upper}_INGOT);

    private ModContent() {
    }

    /** Hands every declaration above to the game. Called once, from onRegister. */
    public static void register() {
        // Touching the classes runs their static initialisers, which is what
        // fills the registrar. The order does not matter; nothing reaches the
        // game until apply().
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
    public static final Holder<Block> ${upper}_BLOCK = ModContent.REGISTRAR
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
    public static final Holder<Item> ${upper}_INGOT = ModContent.REGISTRAR
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

  // ---------------------------------------------------------------- resources

  if (features.ember) {
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
        cubeAll(ModBlocks.${upper}_BLOCK);
        flatItem(ModItems.${upper}_INGOT);
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
        add(ModBlocks.${upper}_BLOCK, "${options.modName} Block");
        add(ModItems.${upper}_INGOT, "${options.modName} Ingot");
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
        dropsSelf(ModBlocks.${upper}_BLOCK);
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
        // Nine ingots into a block, and the block back into nine. The second
        // needs a name of its own: both would otherwise write a file named
        // after the result, and two recipes cannot share one.
        shaped(ModBlocks.${upper}_BLOCK)
                .pattern("###", "###", "###")
                .define('#', ModItems.${upper}_INGOT)
                .save();

        shapeless(ModItems.${upper}_INGOT, 9)
                .ingredient(ModBlocks.${upper}_BLOCK)
                .named("${ns}_ingot_from_block")
                .save();
    }
}
`);
  } else {
    // Written once, by hand: a block with no model is invisible and a block
    // with no loot table drops nothing, both without a word in the log.
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
  return [];
}
