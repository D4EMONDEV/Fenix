---
title: Generating resources
description: Ember writes your models, translations, loot tables, recipes, tags
  and sounds from Java.
slug: 0.1/guides/ember
---

A block needs a model, a blockstate, a translation and a loot table before it is
a block anyone can use. Written by hand that is four JSON files per block, in
four places, that no compiler checks — and renaming a block leaves all four
behind.

Ember writes them from Java, against the content you registered.

```java title="ModModels.java"
@Generator
public final class ModModels extends EmberModelProvider {
    @Override
    protected void models() {
        cubeAll(ModBlocks.RUBY_BLOCK);
        flatItem(ModItems.RUBY);
    }
}
```

```bash
./gradlew ember
```

Output lands in `src/main/generated`, which is part of your resources — so the
files are reviewable in a diff and shipped in your jar.

## What it writes

| Provider | Writes |
|---|---|
| `EmberModelProvider` | Block models, blockstates, item models |
| `EmberLanguageProvider` | `en_us.json` |
| `EmberLootTableProvider` | What a block drops |
| `EmberRecipeProvider` | Shaped and shapeless recipes |
| `EmberTagsProvider` | Block and item tags |
| `EmberSoundProvider` | `sounds.json` |

Textures and `.ogg` files are what it cannot generate for you.

## Why it is worth the indirection

**Renaming is safe.** The generators name `ModBlocks.RUBY_BLOCK`, not
`"mymod:ruby_block"`. Rename the field and the compiler follows; the JSON is
rewritten on the next run.

**Keys cannot drift.** A creative tab's translation key is derived from the tab
itself:

```java
add(ModContent.TAB, "Example Mod");
```

Nobody types `itemGroup.example-mod.example_mod` twice and gets it right twice.

**Missing things are loud.** A generator in the wrong source set is a compile
error naming the fix, rather than files that silently never appear.

## Generators live in `src/main`

Never in `src/client`. Generators describe files against registered content and
write into `src/main/generated`, so a client-side one would produce common
output from the client half. Ember does not look there, and the annotation
processor says so rather than letting it be found later as a missing model.
