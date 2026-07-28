# Dungeon Mod

Génération de donjons semi-procéduraux sous Minecraft. Génère un réseau de salles connectées avec monstres, PNJ marchands, coffres et butin.

## Commandes

| Commande | Description |
|----------|-------------|
| `/donjon` | Génère un nouveau donjon aléatoire |
| `/test` | Réinitialise et regénère le donjon depuis le hub |
| `/testhub` | Place le hub + couloir d'entrée |
| `/config` | Voir/modifier les paramètres de génération |
| `/base` | Retour au hub |

## Structure des salles

Chaque salle est un fichier `.nbt` dans `src/main/resources/test_structures/`. Les salles sont connectées via des ports (N/S/E/O). Types de salles :

- **T1-T4** : Salles de taverne (contiennent les PNJ : Barman, Gaspard, Elias)
- **C1, C2, C3** : Couloirs droits
- **IJ1-IJ5** : Intersections / virages
- **CJ1-CJ5** : Couloirs de jonction (Partie 4)
- **MJ1-MJ5** : Salles de monstres
- **MG1-MG3** : Salles gobelins spéciales
- **LootDj1-LootDj3** : Salles de butin
- **Prison, PrisonCentrale1-3** : Salles de prison
- **TaverneCoin1-4** : Salles de taverne avec coins
- **MarchandNoir** : Boutique spéciale
- **Ogre** : Salle du boss Cyclope
- **Centrale** : Salle de départ
- **Speciales** : Fontaine, Jardin, Statue, Puit, Chapelle, Crypte, PorteGob, GobelinDoor

## PNJ

| PNJ | Rôle | Bloc |
|-----|------|------|
| Barman (Mira) | Achat de bières | Terre cuite émaillée blanche |
| Gaspard | Vente/échange conseils | Terre cuite émaillée gris clair |
| Elias | Don de fiole (première rencontre) | Terre cuite émaillée grise |
| Cyclope (Ogre) | Boss | Spawné via oeuf |

### Gaspard - Système de trade

Gaspard propose un shop avec :
- **Mode Achat** (désactivé pour Gaspard)
- **Mode Vente** : offres préfabriquées (bière viking → conseil)
- **Mode Vente custom** : déposer bière blonde/viking, choisir récompense (conseil)
- 3 conseils uniques aléatoires, chacun ne peut être donné qu'une fois
- Dialogue `"Je n'ai plus rien à t'apprendre"` une fois tous donnés

## Armure lourde (grimdark)

Texture custom via `EquippableComponent.model()` référençant `dungeonmod:armure_lourde`.
Voir `ARMOR_REGISTRATION.md` pour ajouter un nouveau set.

## Loot

Les tonneaux et coffres sont remplis aléatoirement à la génération via `DungeonLoot.java` :
- **Tonneaux** : bières, bâtons, patates, cailloux, toiles, cuir, poussière
- **Coffres** : items génériques + items spécifiques par salle (ex: clé en salle Loot1 haut)
- **OneTime** : items qui n'apparaissent qu'une seule fois par donjon

## Architecture du projet

```
src/main/java/com/dungeonmod/
├── DungeonMod.java          # Initialisation principale
├── DungeonCommand.java      # Commandes /donjon, /test, etc.
├── ModItems.java            # Enregistrement des items/armures
├── DungeonModClient.java    # Initialisation client (rendu, réseaux)
├── accessor/                # Accesseurs pour mixins
├── client/                  # Overlay dialogues, handlers
├── client/dialogue/         # Dialogues des PNJ (Gaspard, Barman, Cyclope, Elias)
├── debug/                   # DungeonAlgo (génération), DungeonViz (visualisation)
├── entity/                  # Entités + renderers
├── item/                    # Items customs
├── mixin/                   # ~50 mixins (gameplay, combat, UI)
├── network/                 # Payloads réseau (shop, trades)
├── screen/                  # ShopScreen + handler
├── test/                    # TestGenerator (placement salles)
├── util/                    # Helpers (Boomerang, DungeonLoot, etc.)
└── village/                 # SellTradeRegistry

src/main/resources/
├── assets/dungeonmod/
│   ├── equipment/           # Equipment assets (armure)
│   ├── items/               # Définitions d'items 1.21.4
│   ├── geckolib/models/     # Modèles GeckoLib
│   ├── models/              # Modèles d'items
│   ├── textures/            # Textures (items, entity, gui, armor)
│   ├── sounds/              # Sons
│   └── lang/                # Traductions
├── test_structures/         # ~70 structures NBT de salles
└── data/                    # Datapack (recettes désactivées)
```

## Build

```
./gradlew build
```
