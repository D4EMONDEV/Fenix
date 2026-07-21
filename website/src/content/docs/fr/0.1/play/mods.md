---
title: Ajouter des mods
description: Où vont les mods, et quoi faire quand l'un d'eux refuse de se charger.
slug: fr/0.1/play/mods
---

Dépose les jars de mods dans `.minecraft/mods`. Fenix lit le dossier au
démarrage ; il n'y a rien à activer.

## Ce qui doit s'y trouver

| Fichier | Ce que c'est |
|---|---|
| `example-mod-1.0.0.jar` | Un mod |
| `fenix-api-0.1.0+mc26.2.jar` | L'API dont la plupart des mods ont besoin, en **un** fichier |

L'API transporte ses modules à l'intérieur d'elle-même : l'installer ou la
mettre à jour reste un seul fichier même quand elle grossit. Tu n'as pas de
versions de modules à accorder à la main.

## Quand un mod ne se charge pas

Fenix dit pourquoi, par son nom, avant même que la fenêtre du jeu s'ouvre. Il ne
laisse jamais tomber un jar en silence — un mod présent et muet, c'est la panne
qui coûte une soirée.

**`contains no fenix.mod.json`** — ce jar est fait pour un autre chargeur. Un
mod Fabric ou Forge ne tournera pas sur Fenix, et le renommer n'y changera rien.

**`requires fenix >=x, but y is installed`** — mets Fenix à jour, ou prends une
version plus ancienne du mod.

**`requires minecraft ~26.2`** — le mod a été construit pour une autre version
du jeu. Tout ce qui est compilé contre Minecraft le dit dans son nom de
fichier : `+mc26.2`.

**Deux mods avec le même id** — tu as le même mod en double, sans doute en deux
versions. Supprimes-en un.

## Quand le serveur te refuse

> This server and your game do not have the same mods — missing: example-mod

Le serveur vérifie à la connexion et nomme ce qui manque, plutôt que de te
laisser entrer et casser doucement. Installe ce qu'il nomme, ou demande à qui
l'administre.

C'est ainsi parce que les ids de blocs et d'items sont attribués dans l'ordre :
un seul mod manquant décale tout ce qui suit, et le résultat est des mauvais
blocs et des plantages qui ne parlent de rien d'utile.

## Désinstaller Fenix

Supprime le profil Fenix dans le launcher. Rien dans `versions/26.2/` n'a jamais
été touché : vanilla est exactement où tu l'as laissé.
