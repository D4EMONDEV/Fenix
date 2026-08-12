---
title: Networking
description: Sending your own messages between the client and the server.
order: 9
---

Two types, one for each direction. Declare a record, give it a codec, and both
ends read the same file — so a field added to the record is added to both sides
at once.

```java title="src/main/java/com/example/mymod/ModPayloads.java"
package com.example.mymod;

import fr.d4emon.fenix.network.ToClient;
import fr.d4emon.fenix.network.ToServer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

public final class ModPayloads {

    public record Tally(BlockPos pos, int count) {
        static final StreamCodec<FriendlyByteBuf, Tally> CODEC = StreamCodec.of(
                (buffer, value) -> {
                    buffer.writeBlockPos(value.pos());
                    buffer.writeVarInt(value.count());
                },
                buffer -> new Tally(buffer.readBlockPos(), buffer.readVarInt()));
    }

    public record Reset(BlockPos pos) {
        static final StreamCodec<FriendlyByteBuf, Reset> CODEC = StreamCodec.of(
                (buffer, value) -> buffer.writeBlockPos(value.pos()),
                buffer -> new Reset(buffer.readBlockPos()));
    }

    public static final ToClient<Tally> TALLY =
            ToClient.of(Identifier.fromNamespaceAndPath("my-mod", "tally"), Tally.CODEC);

    public static final ToServer<Reset> RESET =
            ToServer.of(Identifier.fromNamespaceAndPath("my-mod", "reset"), Reset.CODEC);

    private ModPayloads() {
    }
}
```

The write and the read must mirror each other exactly, in the same order. They
are two lambdas beside each other for that reason: a mismatch is a desync that
shows up as garbage several fields later, and putting them side by side is the
cheapest way to notice.

## Sending

```java
// Server → one client
ModPayloads.TALLY.send(player, new ModPayloads.Tally(pos, 3));

// Server → everyone
ModPayloads.TALLY.sendAll(server, new ModPayloads.Tally(pos, 3));

// Client → server
ModPayloads.RESET.send(new ModPayloads.Reset(pos));
```

`ToServer.send` takes no player: there is only one server, and it already knows
who you are. That asymmetry is deliberate — a client cannot claim to be somebody
else by construction.

## Receiving

Register the handlers once, in `onInit`.

```java
// On the server. The handler runs on the server thread.
ModPayloads.RESET.receive((reset, player) -> {
    if (!player.blockPosition().closerThan(reset.pos(), 8)) {
        return;
    }
    // …
});
```

```java title="in the client half"
ModPayloads.TALLY.receive(tally -> {
    // Runs on the client thread.
});
```

<div class="admonition danger">
<p class="admonition-title">A payload is whatever arrived on a socket</p>
<p>The <code>player</code> handed to a server handler is trustworthy; the record
is not. Check every distance, every amount, every position against what that
player could actually do. A mod that acts on a position without checking the
player is near it is a mod that lets anyone edit the world from anywhere.</p>
</div>

## What happens on a vanilla client

Fenix declares its payloads to the connection. A client that does not have your
mod never receives them, and the send is a no-op rather than a disconnect.

`RegistryCheck` runs when a player joins: the server sends a summary of what it
has registered, the client compares it against its own and reports back anything
missing, and the connection is refused with those entries named. A client
without a block the server has finds out at the door, rather than joining and
desyncing the first time it sees one.

## When not to use this

A block entity that only needs its contents shown does not need a payload:
Minecraft already synchronises block entity data to clients that can see it.
Reach for a payload when you need something the game does not already send — a
one-off, a request, or state nobody else has a reason to know about.
