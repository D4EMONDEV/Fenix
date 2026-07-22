---
title: "network.client"
description: "Types in fr.d4emon.fenix.network.client"
sidebar:
  order: 41
tableOfContents:
  maxHeadingLevel: 2
---

<p class="api-package"><code>fr.d4emon.fenix.network.client</code></p>

| Type | What it is |
|---|---|
| [`ClientChannels`](#clientchannels) | Teaches the common half how a client sends. |

## ClientChannels

Teaches the common half how a client sends.

`ToServer.send` has to work from common code — a mod's send site is
usually right beside the button that triggers it — but reaching the
connection means naming `Minecraft`, which common code cannot. So the
client half installs the one method that does.

### `static void install()`

Installs it. Called by this module's client entry point.

