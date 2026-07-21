---
title: Installer Fenix
description: Ajouter Fenix au launcher Minecraft.
tableOfContents: false
slug: fr/0.1/play/install
---

:::caution[Tout début]
Fenix est avant la 1.0. Ça fonctionne, et l'API changera encore sans préavis.
:::

## Joueurs

Télécharge le dernier **fenix-installer-…-windows.zip** depuis
[la page des releases](https://github.com/D4EMONDEV/Fenix/releases), décompresse-le,
et lance **Fenix Installer**.

Il ajoute un profil Fenix au launcher Minecraft. Ouvre le launcher, choisis le
profil, appuie sur Jouer. Les mods vont dans `.minecraft/mods`, comme d'habitude.

L'installeur embarque son propre Java : il n'y a rien d'autre à installer.

### Avant de commencer

Installe Minecraft **26.2** depuis le launcher et lance-le une fois. L'installeur
construit son profil par-dessus la version que le launcher a déjà téléchargée, et
te le dira franchement si elle manque.

### Depuis les sources

```bash
git clone https://github.com/D4EMONDEV/Fenix
cd Fenix
./gradlew :fenix-installer:distInstaller
```

L'application arrive dans `fenix-installer/build/distributions/`.

Avec des arguments, il reste un outil en ligne de commande — ce qu'on veut sur un
serveur sans écran :

```bash
"Fenix Installer" --dir /srv/minecraft --minecraft 26.2
```

## Auteurs de mods

Tu n'as pas besoin de l'installeur pour développer : le plugin Gradle télécharge
et lance le jeu lui-même.

```kotlin title="settings.gradle.kts"
pluginManagement {
    repositories {
        maven("https://d4emondev.github.io/Fenix/")
        gradlePluginPortal()
    }
}
```

```kotlin title="build.gradle.kts"
plugins { id("fr.d4emon.fenix.dev") version "0.1.0" }

fenix { minecraft = "26.2" }
```

Puis `./gradlew runClient`. Le [guide de démarrage](/fr/0.1/guides/getting-started/)
prend la suite.

## Ce qui est installé

| Où | Quoi |
|---|---|
| `versions/fenix-<version>-<mc>/` | Un manifeste de version qui hérite de vanilla |
| `libraries/fr/d4emon/fenix/` | Le loader et le cœur de l'API |
| `libraries/net/fabricmc/`, `libraries/org/ow2/asm/` | Mixin et ASM |
| `launcher_profiles.json` | Un profil, nommé Fenix |

Rien dans `versions/26.2/` n'est touché. Vanilla continue de fonctionner, et
supprimer le profil Fenix supprime Fenix.
