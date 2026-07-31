---
title: Ember
description: Generate the resource files your mod needs, from the Java declarations it owns.
order: 3
---

Ember is the Fenix generator API. It writes resource and data files into
`src/main/generated`; the directory is part of your build and should be
committed like any other source output.

```bash
./gradlew ember
```

## Add a provider when you need one

The template's Ember option creates an empty `ModResources` class. It produces
nothing until you add a provider that matches a real need in your mod.

For example, if your mod owns `Content.COPPER_TOKEN`, this provider creates its
English translation:

```java
import fr.d4emon.fenix.ember.EmberLanguageProvider;
import fr.d4emon.fenix.ember.Generator;

@Generator
public final class ModLanguage extends EmberLanguageProvider {
    @Override
    protected void translations() {
        add(Content.COPPER_TOKEN, "Copper Token");
    }
}
```

`Content` is still your class. Ember receives its `Holder` values through the
public Fenix API; it does not require a class named `ModContent`.

## Providers

| Provider | Generates |
|---|---|
| `EmberLanguageProvider` | translations such as `lang/en_us.json` |
| `EmberModelProvider` | blockstates, models and item definitions |
| `EmberLootTableProvider` | block loot tables |
| `EmberRecipeProvider` | crafting and smelting recipes |
| `EmberTagsProvider` | block and item tags |
| `EmberSoundProvider` | `sounds.json` |
| `EmberOreProvider` | configured and placed ore features |

Textures and `.ogg` sound files remain files you create yourself. Ember creates
the JSON around them, not the artwork or audio.

Browse the [`ember` API reference](/docs/0.2.0/api/fr-d4emon-fenix-ember) for
every provider and method.
