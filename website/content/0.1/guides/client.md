---
title: Client-side code
description: Rendering, key bindings and screens — and why they live apart.
order: 8
---

A dedicated server has no rendering, no keyboard and no screens. Half of
Minecraft's classes simply are not there. A mod that touches one from code the
server runs crashes on somebody else's server, at the moment a player does the
thing that reaches it — which is a crash report with your mod's name on it and
no way to reproduce it locally.

Fenix makes that a compile error instead.

## Two source sets

`src/main` compiles against a Minecraft with the client half **removed**.
Naming `Minecraft`, `Screen` or any renderer there does not compile.

`src/client` gets the whole game, and the loader only reads it on a client.

```
src/main/java/com/example/mymod/MyMod.java          both sides
src/client/java/com/example/mymod/client/MyModClient.java   client only
```

The client half is a second entry point with the **same mod id**:

```java title="src/client/java/com/example/mymod/client/MyModClient.java"
package com.example.mymod.client;

import fr.d4emon.fenix.api.Fenix;
import fr.d4emon.fenix.api.FenixMod;
import fr.d4emon.fenix.api.Mod;

@Mod("my-mod")
public final class MyModClient implements FenixMod {

    public MyModClient() {
    }

    @Override
    public void onRegister(Fenix fenix) {
        // Renderers, screens, particle factories.
    }
}
```

Both halves are found by the annotation processor and both run on a client; only
the common one runs on a server. There is nothing to configure.

## Renderers

Everything registered from the client half is registered in `onRegister`, beside
the content it draws.

```java
import fr.d4emon.fenix.registry.client.BlockEntityRendering;
import fr.d4emon.fenix.registry.client.EntityRendering;
import fr.d4emon.fenix.registry.client.MenuScreens;
import fr.d4emon.fenix.registry.client.ParticleRendering;

@Override
public void onRegister(Fenix fenix) {
    EntityRendering.register(ModContent.WISP, ThrownItemRenderer::new);
    BlockEntityRendering.register(ModContent.TALLY, TallyRenderer::new);
    MenuScreens.register(ModContent.SAFE_MENU, SafeScreen::new);
    ParticleRendering.register(ModContent.SPARK, GlowParticle.ElectricSparkProvider::new);
}
```

| Class | Registers |
|---|---|
| `EntityRendering` | How an entity is drawn. |
| `BlockEntityRendering` | A renderer for a block entity — for anything a static model cannot do. |
| `EntityModels` | A model layer, for a renderer that needs its own geometry. |
| `MenuScreens` | The screen that opens for a menu. |
| `ParticleRendering` | The factory that makes a particle. |
| `FluidRendering` | The textures and tint of a fluid. |
| `KeyBindings` | A key the player can rebind. |
| `CreativePageButton` | An extra page in the creative inventory. |

## Key bindings

A binding is registered once and then polled; it is not an event.

```java title="src/client/java/com/example/mymod/client/ModKeys.java"
import com.mojang.blaze3d.platform.InputConstants;
import fr.d4emon.fenix.event.client.ClientEvents;
import fr.d4emon.fenix.registry.client.KeyBindings;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

public final class ModKeys {

    private static final KeyMapping.Category CATEGORY =
            KeyBindings.category(Identifier.parse("my-mod:my_mod"));

    public static final KeyMapping OPEN = KeyBindings.register(
            Identifier.parse("my-mod:open"), InputConstants.KEY_G, CATEGORY);

    private ModKeys() {
    }

    public static void listen() {
        ClientEvents.TICK_END.register(tick -> {
            while (OPEN.consumeClick()) {
                // …
            }
        });
    }
}
```

`consumeClick` in a `while`, not an `if`: a key pressed twice between two ticks
reports twice, and asking once would drop the second press.

Both the binding and the category are named by `Identifier`, and both show in
the controls screen through a translation key derived from it — so both belong
in your language file, or the screen shows the raw id.

## The HUD

```java
import fr.d4emon.fenix.event.client.HudRenderEvents;

HudRenderEvents.RENDER.register(hud -> {
    // hud carries the graphics context and the frame's delta.
});
```

This fires every frame. Do no work here that could be done once and cached: at
120 frames a second, anything allocated here is allocated 120 times a second.

## Tooltips

```java
import fr.d4emon.fenix.event.client.ItemTooltipEvents;

ItemTooltipEvents.BUILD.register(tooltip -> {
    if (tooltip.stack().is(ModItems.RUBY.get())) {
        tooltip.lines().add(1, Component.translatable("tooltip.my-mod.ruby")
                .withStyle(ChatFormatting.DARK_RED));
    }
});
```

`lines()` is mutable, and index 1 is just under the item's name. Appending puts
your line below everything, including the enchantment list.

## Per-world client state

`ClientEvents.CONNECTED` and `DISCONNECTED` bracket a world. Anything cached
about the world belongs built in the first and thrown away in the second — a
cache that survives a disconnect is carried into the next world and is quietly
wrong there.

## What still belongs on the server

The client can be lied to. Anything that decides an outcome — whether a block
breaks, whether an item is consumed, what a container holds — belongs in
`src/main`, and the client only shows the result. `ClientBlockEvents` exists for
prediction and for cancelling a click before it is sent, not for deciding
anything.
