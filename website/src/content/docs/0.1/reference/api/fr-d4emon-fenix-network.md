---
title: "network"
description: "Types in fr.d4emon.fenix.network"
sidebar:
  order: 40
tableOfContents:
  maxHeadingLevel: 2
---

<p class="api-package"><code>fr.d4emon.fenix.network</code></p>

| Type | What it is |
|---|---|
| [`Channel`](#channel) | What a payload has in common whichever way it travels. |
| [`Channels`](#channels) | Fenix's own payload table, and the dispatch into it. |
| [`Channels.ClientSender`](#channels-clientsender) | How the client reaches its connection, without common code naming it. |
| [`Envelope`](#envelope) | The one payload type Fenix registers with vanilla, in each direction. |
| [`RegistryCheck`](#registrycheck) | Tells a joining player when their game and the server's do not match. |
| [`RegistrySummary`](#registrysummary) | What a side has registered, small enough to send on every join. |
| [`ToClient`](#toclient) | A payload the server sends to a client. |
| [`ToServer`](#toserver) | A payload a client sends to the server. |

## Channel

What a payload has in common whichever way it travels.

Sealed rather than extensible: a payload goes one way or the other, and
the two are not interchangeable. Which way it goes is in the type — a
`ToServer` cannot be sent to a client, and that is a compile error
rather than a packet nobody handles.

### `StreamCodec<FriendlyByteBuf,T> codec()`

{@return how its payloads are written and read}

### `Identifier id()`

{@return what this channel is called on the wire}

### `String toString()`

## Channels

Fenix's own payload table, and the dispatch into it.

This is the table vanilla's would have been, had adding to it been
possible — see `Envelope` for why it is not. Registration happens
whenever a channel is declared, with no ordering constraint at all.

### `static boolean deliver(Envelope envelope, Object context, CustomPacketPayload.Type<Envelope> direction)`

Hands an arriving envelope to its channel.

An unknown channel is ignored rather than fatal: a server may well run
mods its players do not have, and the reverse. Being able to name the
channel in a log line is the whole reason Fenix wraps payloads at all —
vanilla would have discarded it without a word.

### `static void sender(Channels.ClientSender clientSender)`

Installs the client's way of sending. Called by the client half.

## Channels.ClientSender

How the client reaches its connection, without common code naming it.

### `void toServer(Envelope envelope)`

Sends an envelope to the server.

## Envelope

The one payload type Fenix registers with vanilla, in each direction.

Every mod payload travels inside one of these rather than as a vanilla
payload of its own, and that is a deliberate choice with a concrete reason.

Vanilla builds its payload dispatch table once, eagerly, from a list
captured when `ClientboundCustomPayloadPacket` is first loaded. A mod
wanting a type in that table would have to have registered it before that
moment — a moment decided by vanilla's own class-loading order, which is not
something a loader should bet on and which could change under it on any
Minecraft update. The failure would be silent, too: the packet decodes as a
discarded payload and nothing is ever heard from again.

These two types are constants, so the injection that adds them carries no
such bet — it runs when the class is transformed, which is always before its
static initialiser. Mods then register into Fenix's own table whenever they
like, and the ordering question disappears rather than being answered.

The cost is one identifier on the wire per packet. The gain is that an
unknown channel can be reported by name instead of vanishing.

**Constants**

| Name | What it is |
|---|---|
| `TO_SERVER` | Client to server. |
| `TO_CLIENT` | Server to client. |
| `TO_SERVER_CODEC` | Reads and writes an envelope travelling towards the server. |
| `TO_CLIENT_CODEC` | Reads and writes an envelope travelling towards a client. |

### `Identifier channel()`

### `byte[] data()`

### `CustomPacketPayload.Type<Envelope> direction()`

### `boolean equals(Object o)`

### `int hashCode()`

### `String toString()`

### `CustomPacketPayload.Type<? extends CustomPacketPayload> type()`

## RegistryCheck

Tells a joining player when their game and the server's do not match.

Without this, a client missing one of the server's mods is not refused —
it is admitted, and then falls apart in ways that name nothing useful.
Network ids are assigned per registry, so one absent block shifts every id
after it: the player sees the wrong blocks, or is kicked by vanilla with
"Can't find id for Block{…}", or watches chunks arrive as nonsense. None of
those mention the mod that is actually missing.

So the server states what it has on join, the client compares and reports
back, and the server refuses with a sentence naming what is wrong. Detection
and a clear refusal — never quietly remapping ids to paper over it, which
trades a confusing disconnect for a world that corrupts slowly.

A client without Fenix answers nothing and is left alone. It is no worse
off than before, and a server that wants to insist can say so itself.

**Constants**

| Name | What it is |
|---|---|
| `SERVER_SUMMARY` | What the server has, sent to every joining player. |
| `MISMATCH` | What the client found wrong with it, if anything. |

### `static void greet(ServerPlayer player)`

Tells a joining player what this server has. Called from the join mixin.

### `static void listen()`

Loads this class on a client, so its handlers are registered.

## RegistrySummary

What a side has registered, small enough to send on every join.

Sending the ids themselves would be honest and useless: a large modpack
has tens of thousands, which is megabytes on a channel capped at one, on
every connection. So each registry is reduced to a digest, and the mod
namespaces are sent alongside in full.

That pairing is what lets the refusal be specific. A digest alone can only
say "these differ"; the namespaces turn the common case — someone is missing
a mod — into a sentence naming it. When the namespaces match and the digests
do not, the mods are the same and their versions are not, which is a
different sentence and just as useful.

**Constants**

| Name | What it is |
|---|---|
| `CODEC` | How a summary is written and read. |

### `List<String> differencesFrom(RegistrySummary theirs)`

{@return what is wrong, in sentences, or empty when the two agree}

### `Map<String,String> digests()`

### `boolean equals(Object o)`

### `int hashCode()`

### `static RegistrySummary local()`

{@return what this side has registered}

### `Set<String> namespaces()`

### `String toString()`

## ToClient

A payload the server sends to a client.

```java
public static final ToClient<SafeContents> CONTENTS =
       ToClient.of(Identifier.parse("mymod:contents"), SafeContents.CODEC);

// client, once
CONTENTS.receive(contents -> show(contents));

// server, whenever
CONTENTS.send(player, new SafeContents(items));
```

The handler runs on the client thread. A client that has no handler for
the channel simply drops it, which is what lets a server run a mod its
players do not have.

### `static ToClient<T> of(Identifier id, StreamCodec<FriendlyByteBuf,T> codec)`

Declares a channel towards clients.

### `void receive(Consumer<T> handler)`

Says what the client does when one arrives. Client only.

### `void send(ServerPlayer player, T payload)`

Sends one to a player.

### `void sendAll(MinecraftServer server, T payload)`

Sends one to everybody on the server.

## ToServer

A payload a client sends to the server.

```java
public static final ToServer<OpenSafe> OPEN_SAFE =
       ToServer.of(Identifier.parse("mymod:open_safe"), OpenSafe.CODEC);

// server, once
OPEN_SAFE.receive((open, player) -> open(player, open.pos()));

// client, whenever
OPEN_SAFE.send(new OpenSafe(pos));
```

The handler is given the player who sent it, and runs on the server
thread — anything reached from it is safe to touch. Treat what arrives as
untrusted: a client can send anything at any time, so check that the player
is near the block they claim to be opening.

### `static ToServer<T> of(Identifier id, StreamCodec<FriendlyByteBuf,T> codec)`

Declares a channel towards the server.

### `void receive(BiConsumer<T,ServerPlayer> handler)`

Says what the server does when one arrives.

### `void send(T payload)`

Sends one to the server. Client only.

