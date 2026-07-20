# `fenix.mod.json`

Every mod ships one, at the root of its jar. It describes the mod; it does
**not** point at code — that is what the `@Mod` annotation and the compile-time
index are for.

## Example

```json
{
  "schema": 1,
  "id": "example-mod",
  "version": "1.0.0",
  "name": "Example Mod",
  "description": "The smallest thing a Fenix mod author has to write.",
  "authors": ["D4emon"],
  "license": "Apache-2.0",
  "contact": {
    "homepage": "https://example.com",
    "issues": "https://example.com/issues"
  },
  "side": "both",
  "depends": {
    "fenix": ">=0.1.0",
    "minecraft": "~26.2",
    "fenix-api-event": "^0.1.0"
  },
  "mixins": ["example-mod.mixins.json"]
}
```

## Fields

| Field         | Required | Meaning                                                        |
|---------------|----------|----------------------------------------------------------------|
| `schema`      | yes      | Metadata format version. Currently `1`.                         |
| `id`          | yes      | Unique id. Lowercase, `a-z0-9-`.                                |
| `version`     | yes      | Semantic version of this mod.                                   |
| `name`        | no       | Human-readable name. Defaults to `id`.                          |
| `description` | no       | One or two sentences.                                           |
| `authors`     | no       | List of names.                                                  |
| `license`     | no       | SPDX identifier.                                                |
| `contact`     | no       | Free-form map; `homepage` and `issues` are conventional.        |
| `side`        | no       | `both` (default), `client`, or `server`.                        |
| `depends`     | no       | Map of mod id to version constraint. `minecraft` and `fenix` are valid ids. |
| `mixins`      | no       | Mixin config files to load.                                     |

## Version constraints

Standard semantic-version range syntax:

| Constraint | Matches                                     |
|------------|---------------------------------------------|
| `1.2.3`    | exactly that version                        |
| `>=1.2.0`  | that version or newer                       |
| `^1.2.0`   | `>=1.2.0` and `<2.0.0` — compatible updates |
| `~1.2.0`   | `>=1.2.0` and `<1.3.0` — patch updates      |
| `*`        | any version                                 |

`depends` drives **initialisation order** as well as validation: a mod is always
initialised after everything it depends on.

## In this repository

Modules build their metadata with `${version}` and `${minecraft_version}`
placeholders, substituted at build time by the `fenix.java-conventions` plugin.
A module's declared version can therefore never drift from its Gradle version.
