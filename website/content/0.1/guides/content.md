---
title: Content and registries
description: Keep your declarations in your mod and let Registrar perform the game registration.
order: 2
---

`Registrar` is Fenix API. A class named `Content`, `ModContent` or `MyItems` is
your own code. It simply holds the declarations for your mod in one predictable
place.

## Declare, then apply

Declare `Holder` fields before the game opens its registries. The holder gives
you the final object after `apply()` runs, while still letting other declarations
refer to it safely.

```java
public final class Content {
    public static final Registrar REGISTRAR = Registrar.of("my-mod");

    public static final Holder<Item> COPPER_TOKEN = REGISTRAR.item("copper_token");
    public static final Holder<Block> COPPER_CRATE = REGISTRAR.blockWithItem("copper_crate");

    private Content() {
    }
}
```

```java
@Override
public void onRegister(Fenix fenix) {
    Content.REGISTRAR.apply();
}
```

`blockWithItem` adds both the block and its placing item. Use `block` when a
block should have no item form, and `item` for a plain item. `newBlock` and
`newItem` open builders instead, for anything that needs properties set.

## Put content in a creative tab

Registration makes content exist; a creative tab makes it discoverable:

```java
import fr.d4emon.fenix.registry.CreativeTabs;

CreativeTabs.addTo(CreativeTabs.INGREDIENTS, Content.COPPER_TOKEN);
CreativeTabs.addTo(CreativeTabs.BUILDING_BLOCKS, Content.COPPER_CRATE);
```

Call this while registering, after declaring the holder. For a tab of your own,
use `Registrar.creativeTab` and name it with `EmberLanguageProvider`.

## Models and names are separate

`Registrar` only registers Java objects. A texture, model, translation, loot
table or recipe is a resource file. You may write those files yourself, or use
[Ember](/docs/@latest/guides/ember) to generate them from the `Holder` fields you
declared.
