---
title: Vitesse
description: Où Fenix passe du temps, et où il refuse d'en passer.
---

Fenix n'est pas rapide parce qu'il est optimisé. Il est rapide parce qu'un jeu
livré déobfusqué, et un chargeur qui décide des choses pendant que ton code
compile, laissent beaucoup moins à faire au démarrage.

<ul class="figures">
  <li><strong>0,4 s</strong><span>pour charger 2 000 classes</span></li>
  <li><strong>0</strong><span>classe scannée au démarrage</span></li>
  <li><strong>0</strong><span>étape de remapping</span></li>
</ul>

## Rien n'est découvert à l'exécution

Les autres chargeurs trouvent ton mod en scannant : chaque classe de chaque jar
est ouverte et lue à la recherche d'une annotation, avant que le jeu démarre. Ce
coût grandit avec chaque mod installé, et il est payé à chaque lancement.

Le processeur d'annotations de Fenix écrit la réponse dans ton jar pendant qu'il
compile :

```json title="fenix.index.json"
{ "schema": 1, "mods": { "example-mod": "com.example.ExampleMod" } }
```

Le chargeur lit un petit fichier par mod. Le démarrage cesse de se soucier de la
taille des mods, et une classe de mod que le processeur n'a pas pu valider
devient une erreur `javac` plutôt qu'un mod qui ne se charge jamais.

## Aucun mapping, nulle part

Minecraft est livré déobfusqué depuis la 26.1. Les chargeurs conçus pour les
années d'avant portent une chaîne de remapping : les jars de mods sont réécrits
à l'installation ou au lancement, les mappings sont téléchargés et mis en cache,
et les refmaps de mixin traduisent les noms à l'exécution.

Fenix n'a rien de tout ça. Tu écris `BlockBehaviour.Properties`, le fichier de
classe dit `BlockBehaviour.Properties`, et rien ne traduit quoi que ce soit.
Cela supprime une étape de build, un cache, un téléchargement, et toute une
catégorie de « ça marche en dev, ça casse en production ».

## Un classloader qui garde ses fichiers ouverts

Une version précoce refermait chaque jar après lecture, ce qui est propre et,
sous Windows, catastrophique : rouvrir l'archive pour chaque classe transformait
un lancement en **163 secondes**. Garder les descripteurs ouverts a ramené le
même travail à **0,4 seconde** — un facteur 400 tenant à une seule décision sur
des descripteurs de fichiers.

Le prix, c'est que les jars restent verrouillés pendant que le jeu tourne, ce
qui est correct : remplacer un jar de mod sous un jeu en cours n'est pas une
fonctionnalité.

## Une distribution qui n'alloue rien

Le bus d'événements ne prend aucun verrou et n'alloue rien par émission.
L'inscription reconstruit un tableau trié derrière un `volatile`, donc on peut
ajouter ou retirer un écouteur pendant une distribution : cela prend effet à la
suivante.

Les événements se déclenchent à chaque tick, vingt fois par seconde, pendant
toute une session. Un bloc `synchronized` à cet endroit est une promesse d'être
lent pendant des heures.

## Ce que Fenix refuse de faire pour être rapide

**Remapper les ids en silence.** Quand un client et un serveur ne s'accordent
pas sur les registres, la connexion est refusée en nommant le mod. Remapper pour
masquer le problème échange une déconnexion incompréhensible contre un monde qui
se corrompt lentement.

**Sauter la comptabilité de vanilla.** Enregistrer un bloc rejoue les passes que
vanilla fait sur les siens — ids d'états, association à l'item, validité des
block entities. Les sauter est plus rapide et produit des plantages loin de leur
cause.
