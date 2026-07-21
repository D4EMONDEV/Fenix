---
title: Comparé aux autres chargeurs
description: Ce que Fenix fait autrement que Fabric, Forge et NeoForge — et ce qu'ils font mieux.
---

Fabric, Forge et NeoForge sont plus anciens, plus gros et bien mieux éprouvés
que Fenix. Cette page parle de ce qui est *différent*, pas de qui gagne.

## D'un coup d'œil

| | Fenix | Fabric | Forge | NeoForge |
|---|---|---|---|---|
| Mods trouvés par | compilation | scan | scan | scan |
| Remapping | <span class="no">aucun</span> | au build & au lancement | au build | au build |
| Côté client/serveur imposé par | <span class="yes">le compilateur</span> | source sets | `@OnlyIn` | `@OnlyIn` |
| Génération de ressources | <span class="yes">intégrée</span> | intégrée | intégrée | intégrée |
| Configuration | <span class="yes">intégrée</span> | <span class="no">bibliothèque tierce</span> | intégrée | intégrée |
| Registres désaccordés | <span class="yes">refus nommé</span> | <span class="part">partiel</span> | refus | refus |
| Installation de l'API | <span class="yes">un jar</span> | un jar | intégrée | intégrée |
| Maturité | <span class="no">avant 1.0</span> | <span class="yes">des années</span> | <span class="yes">une décennie</span> | <span class="yes">des années</span> |

## Là où Fenix est vraiment différent

### Le côté est une erreur de compilation

Tous les chargeurs ont la même règle : une classe client ne doit pas se charger
sur un serveur. Forge et NeoForge l'expriment avec `@OnlyIn` et te font
confiance. Fabric sépare les source sets, ce qui est une vraie contrainte, et
qui s'active à la demande.

Fenix compile le code commun contre un Minecraft dont la moitié client a été
**retirée** :

```java title="src/main/java — commun"
Minecraft.getInstance();
// error: package net.minecraft.client does not exist
```

La panne que ça évite est la pire qui soit : ça marche chez toi, parce que tu
développes côté client, et ça casse sur le serveur dédié de quelqu'un d'autre.

### Aucun mapping

Les chargeurs antérieurs à la 26.1 traînent une chaîne de remapping pour un jeu
obfusqué. Fenix vise un jeu livré déobfusqué depuis la 26.1 : pas de remapping,
pas de mappings à télécharger, pas de refmaps qui se périment.

Ce n'est pas de l'astuce — c'est être arrivé assez tard pour que le problème ait
disparu.

### Une configuration sans bibliothèque tierce

Fabric laisse la configuration à des bibliothèques communautaires, alors chaque
mod en choisit une différente et un modpack finit avec quatre. Fenix en a une,
typée, adossée à des records.

### Le registrar absorbe la comptabilité de vanilla

Vanilla fait un travail *autour* de l'enregistrement de son propre contenu qu'un
mod contourne : ids réseau des états de blocs, `Item.BY_BLOCK`, validité des
block entities. En sauter un et le plantage arrive plus tard, dans le code de
vanilla, sans rien nommer d'utile.

Fenix fait ce travail pour ton contenu, et chaque cas est tenu par une
vérification qui démarre le vrai jeu et échoue si ça régresse.

## Là où les autres sont devant

**Tout le reste.** Fabric a des milliers de mods, Forge une décennie, et tous
deux ont résolu des problèmes que Fenix n'a pas encore rencontrés. Ici il n'y a
pas d'écosystème, pas de mod avec lequel tu aies envie de jouer, et pas de
version publiée.

Fenix ne vise aussi qu'une seule version de Minecraft. Les autres suivent le jeu
de version en version, ce qui est l'essentiel du travail et rien du plaisir.

**Choisis Fenix** si tu écris un mod pour la 26.2 et que tu veux que le
compilateur attrape ce que les autres découvrent à l'exécution. **Choisis les
autres** pour jouer.
