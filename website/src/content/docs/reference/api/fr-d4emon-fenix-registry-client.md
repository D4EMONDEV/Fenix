---
title: "registry.client"
description: "Types in fr.d4emon.fenix.registry.client"
sidebar:
  order: 21
tableOfContents:
  maxHeadingLevel: 2
---

<p class="api-package"><code>fr.d4emon.fenix.registry.client</code></p>

| Type | What it is |
|---|---|
| [`CreativePageButton`](#creativepagebutton) | One of the two arrows that turn creative pages. |
| [`EntityRendering`](#entityrendering) | Says how an entity looks. |
| [`KeyBindings`](#keybindings) | Binds a key. |
| [`MenuScreens`](#menuscreens) | Says what a menu looks like. |
| [`ParticleRendering`](#particlerendering) | Says what a particle looks like. |
| [`ParticleRendering.Registration`](#particlerendering-registration) | One pending registration. |
| [`ScreenFactory`](#screenfactory) | Builds the screen for a menu, when the server says a window has opened. |
| [`SpriteParticleFactory`](#spriteparticlefactory) | Builds the provider that draws a particle, given its textures. |

## CreativePageButton

One of the two arrows that turn creative pages.

A real `Button` rather than something drawn by hand, which is what
buys the hover state, the click handling, the keyboard focus and the screen
reader narration without any of it being written here.

Its sprites are Fenix's own, drawn in the palette the creative panel
already uses — the same white bevel and grey face as the scroll bar directly
below it — so the pair reads as part of the screen rather than bolted onto
it.

**Constants**

| Name | What it is |
|---|---|
| `WIDTH` | Matching the scroll bar's width, which sits directly underneath. |
| `HEIGHT` | As tall as the strip above the item grid allows. |

### `void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick)`

Draws the arrow and nothing else.

This is the hook rather than `extractWidgetRenderState` because
that one is final; overriding here is also what skips the vanilla button
plate, which would show through underneath.

## EntityRendering

Says how an entity looks.

```java
EntityRendering.register(ModEntities.WISP, ThrownItemRenderer::new);
```

An entity with no renderer is invisible: it is there, it ticks, it can be
hit, and nothing is drawn. Vanilla only warns about it once, in the log,
where it is easy to miss.

Client-only, so call it behind a side check — `fenix.side()` — or
from a class the server never loads. Registering renderers from
`onRegister` is early enough; the client builds its renderers later.

### `static void register(Holder<EntityType<T>> type, EntityRendererProvider<T> renderer)`

Registers the renderer for an entity type.

## KeyBindings

Binds a key.

```java
public static final KeyMapping OPEN_SAFE =
       KeyBindings.register(Identifier.parse("mymod:open_safe"), InputConstants.KEY_K);
```

Then read it on the client tick, in a loop — a key pressed twice between
two ticks reports twice, and asking once would drop the second press:

```java
ClientEvents.TICK.register(tick -> {
   while (OPEN_SAFE.consumeClick()) {
       // …
   }
});
```

The name is a translation key: `mymod:open_safe` is shown as
whatever `key.mymod.open_safe` says, under the category's own key.
Without a translation the player sees the raw key in their controls screen.

Client-only, and registered from `onRegister` — which runs before
the game builds its options, which is exactly when the list has to be
complete.

### `static KeyMapping.Category category(Identifier id)`

Makes a group of the mod's own for the controls screen.

Only worth it for a mod with several keys; one key is better placed in
a category the player already scrolls past. The label comes from
`key.category.<namespace>.<path>`.

### `static KeyMapping register(Identifier id, int keyCode)`

Binds a key, in the miscellaneous category.

### `static KeyMapping register(Identifier id, int keyCode, KeyMapping.Category category)`

Binds a key.

## MenuScreens

Says what a menu looks like.

```java
MenuScreens.register(ModContent.SAFE_MENU, SafeScreen::new);
```

A menu type with no screen fails quietly and confusingly: the server opens
the window, the client finds nothing to draw, and the player watches their
inventory close again with no error anywhere.

Client-only, so call it from a `@Mod` class in `src/client`.

### `static void register(Holder<MenuType<M>> type, ScreenFactory<M,S> screen)`

Registers the screen for a menu type.

## ParticleRendering

Says what a particle looks like.

```java
ParticleRendering.register(ModContent.SPARK, GlowParticle.ElectricSparkProvider::new);
```

A particle type with no provider is spawned and never drawn: the server
sends it, the client looks up a provider, finds none, and returns. Nothing
crashes and nothing is logged — the effect simply does not happen, on some
machines and not others if a mod is only installed on one side.

The textures come from
`assets/<namespace>/particles/<name>.json`, a file listing sprite names;
without it the particle is drawn as the missing texture.

Client-only, and registered from `onRegister` — the game builds its
provider table once, while loading resources, and that is where these are
added.

### `static void register(Holder<SimpleParticleType> type, SpriteParticleFactory factory)`

Registers the provider for a particle type.

## ParticleRendering.Registration

One pending registration.

### `boolean equals(Object o)`

### `SpriteParticleFactory factory()`

### `int hashCode()`

### `String toString()`

### `Holder<SimpleParticleType> type()`

## ScreenFactory

Builds the screen for a menu, when the server says a window has opened.

Fenix's own, and public, for the same reason `MenuFactory` is:
vanilla's equivalent is private, and the widening that lets this module reach
it stays this module's business. A mod passing `MyScreen::new` would
otherwise have to name a type it cannot see.

### `S create(M menu, Inventory inventory, Component title)`

Builds one.

## SpriteParticleFactory

Builds the provider that draws a particle, given its textures.

Fenix's own, and public, for the same reason `ScreenFactory` is:
vanilla's equivalent is a private nested interface, and a mod passing
`MyProvider::new` would otherwise have to name a type it cannot see.

The sprite set arrives already loaded from
`assets/<namespace>/particles/<name>.json`, which is what lists the
textures.

### `ParticleProvider<SimpleParticleType> create(SpriteSet sprites)`

Builds one.

