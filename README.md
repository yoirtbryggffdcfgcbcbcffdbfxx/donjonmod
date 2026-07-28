# ⚔️ DonjonCraft — Dungeon Mod

Mod **Fabric 1.21.4** : donjons procéduraux en 4 phases, PNJ avec dialogues et boutiques, boss à mécaniques (le Cyclope), armures et armes à effets custom, économie en deniers.

Vision long terme : un serveur **DonjonCraft** où le but est de devenir **maire du village**, avec 3 métiers (mineur, bûcheron, aventurier), des quêtes, des donjons en party et des boss liés à une histoire de péchés originels. Voir `docs/donjoncraft-brainstorming.odt`.

---

## 🚀 Lancer le projet

**Prérequis : JDK 21** (c'est tout — le wrapper Gradle 8.12 est inclus, il s'auto-télécharge).

```bat
:: Windows
gradlew.bat runClient        :: lance le client de dev
gradlew.bat build            :: compile le jar (dans build/libs)
python run_client.py         :: raccourci = même chose que runClient
```

| Composant | Version |
|---|---|
| Minecraft | 1.21.4 |
| Fabric Loader | 0.16.9 |
| Fabric API | 0.119.4+1.21.4 |
| GeckoLib | 4.8.5 |
| player-animation-lib | 2.0.5+1.21.4 |
| Java | 21 |

---

## 🎮 Commandes (dev/test)

| Commande | Effet |
|---|---|
| `/teste <1-40>` | Génère un donjon (nombre max de salles), TP au départ, vie mise à 10 |
| `/testeseed <seed>` | Génère avec une seed donnée *(⚠️ déterminisme partiel, cf. note dans `LLM.md`)* |
| `/teste test` | Régénère au même endroit (nettoie les entités + reset vie) |
| `/teste tp <salle>` | Téléporte vers un type de salle du donjon courant |
| `/teste item <id>` | Donne un item custom (ids dans `ModItems.java`) |
| `/teste stats` | Affiche dégâts/vitesse/portée/knockback de l'item en main |
| `/teste feed` | Met la vie à 0,5 cœur (test) |
| `/testehealth` / `/testebeer` | Outils de debug (vie / bière) |
| `/testehub` | Place le hub |

---

## 🗺️ Le donjon — génération

Pipeline : `debug/DungeonAlgo.java` (logique pure, sans dépendance Minecraft) → `test/TestGenerator.java` (placement de **~72 structures `.nbt`**) → `debug/DungeonViz.java` (rendu HTML `dungeon_viz.html`).

**4 phases enchaînées** (graphe en arbre, sans cycles) :

1. **Grotte** (20-26 salles) : départ, taverne (PNJ), prison, loot, salle de monstres, puit
2. **Donjon** (18-25 salles) : monstres donjon, loots donjon, campement
3. **Grande zone** (45 salles) : jardin, crypte, chapelle, statue, **salle du boss Ogre**
4. **Étage P4** : Centrale, Marchand Noir, quartier gobelin…

**Garanties** : ≥1 loot, 1 puit, prison atteignable, loot manquant de P3 reporté en P4. Contraintes de lisibilité : max 3-4 intersections, pas de 3 couloirs colinéaires consécutifs. En cas d'échec : nouvel essai (jusqu'à ~100 tentatives) puis abandon propre (log `ECHEC`).

---

## ⚙️ Règles de gameplay

- **PV max : 10** (5 cœurs), **pas de régénération naturelle** (nourriture neutralisée)
- **Dégâts custom** : `dégâts × (1 - armure/100)` — 1 point d'armure = 1 % de réduction (remplace la formule vanilla)
- HUD custom : barre de vie centrée, texte « Protection = X% », barre de faim cachée
- Consommables qui soignent (pomme, patate douce, steak, fiole d'eau bénite…)

## 🛡️ Items & armures (extrait)

Tous sont des **items vanilla renommés** via `DataComponentTypes` (pas de registry custom). Référence complète : `LLM.md`.

- **Chasseur** : casque (affiche PV/dégâts des mobs ≤5 blocs), jambière (sneak = camouflage total, cooldown 2 s), plastron (**Chute lente permanente**)
- **Voyageur** : plastron (portée +1,5 bloc), jambière (+50 % vitesse sous 4 PV)
- **Lourd** : −10 à −30 % vitesse/saut, visière (overlay caméra), absorption
- **Héros** : renvoie les dégâts subis ; **Bottes 7 lieues** : +30 % vitesse, zéro dégât de chute
- **Armes** : dague (backstab ×2), hache (combo 1→1.5→2→4 cœurs), bâton/os lançables, sabre…
- **Armure lourde (texture custom)** : via `EquippableComponent` + equipment asset — recette complète dans `ARMOR_REGISTRATION.md`

## 👥 PNJ & économie

| PNJ | Rôle | Lieu |
|---|---|---|
| **Mira** (Barman) | Vend des bières : **2 deniers** → bière | Taverne (P1) |
| **Gaspard** | Achète des bières contre des **conseils** (3 uniques, une fois chacun) | Taverne |
| **Elias** | Donne une fiole (1ʳᵉ rencontre) | Taverne |
| **Cyclope** | Après sa mort : troc **bout de tissu ↔ bières**, 4 max à vie | Salle du boss |

Le **denier** (= pépite d'or remaniée) est la monnaie. Shops via écran custom (`ShopScreen`, 825 lignes : achat/vente, slot de dépôt) ou écran marchand vanilla étendu (`NpcMerchant`). Réseau dédié (`TradesPayload`, `TradeData`).

## 👁️ Boss : le Cyclope (`OgreEntity`)

Machine à états (phases synchronisées client/serveur) : statue → intro → combat → **mort scénarisée** (marche au centre, anim, devient PNJ marchand).

- **Combos contextuels à la distance** : lancer de pierre balistique, charge (5 dégâts + Slowness), clap AoE, coup de tête (Nausée + Cécité)
- **Mécanique d'œil** : frapper la tête (mêlée ou projectile) → attaque interrompue + dégâts **×4** ; le boss se protège ensuite (−50 % dégâts, cooldown 10 s, fenêtres d'interruptibilité par attaque)
- Bossbar dynamique (couleur selon invulnérabilité)

---

## 📁 Structure du dépôt

```
donjonmod/
├── README.md                     ← vous êtes ici
├── docs/
│   └── donjoncraft-brainstorming.odt   ← game design original
└── dungeonmod/                   ← le mod
    ├── build.gradle / gradlew.bat / gradle.properties
    ├── run_client.py · ouvrir_donjon.py · check_nbt.py
    ├── *.md                      ← docs internes (table ci-dessous)
    ├── dungeon_viz.html          ← dernier donjon généré (visualisation)
    └── src/main/
        ├── java/com/dungeonmod/
        │   ├── DungeonMod.java          # init, tick handlers
        │   ├── ModItems.java            # tous les items/armures
        │   ├── DungeonCommand.java      # commandes
        │   ├── debug/                   # DungeonAlgo (génération), DungeonViz
        │   ├── test/                    # TestGenerator (placement des salles)
        │   ├── entity/                  # PNJ, boss (OgreEntity), gobelins…
        │   ├── client/ + client/dialogue/   # overlays, dialogues PNJ
        │   ├── mixin/                   # ~50 mixins (combat, HUD, items…)
        │   ├── network/                 # payloads (shop, sous-titres…)
        │   ├── screen/                  # ShopScreen + handler
        │   ├── util/  item/  accessor/  village/
        └── resources/
            ├── assets/dungeonmod/       # textures, models, equipment, lang, geckolib
            ├── test_structures/         # ~72 structures .nbt de salles
            └── algorithme_de_generation-python/   ← prototypes originels de l'algo
```

## 📚 Documentation interne

| Fichier | Contenu |
|---|---|
| `dungeonmod/LLM.md` | ⭐ Doc pour IA : architecture, mixins, items, conventions — **à lire en premier** |
| `dungeonmod/DEVELOPER.md` | Recettes : créer un item, attributs, knockback, JSON |
| `dungeonmod/ARMOR_REGISTRATION.md` | Ajouter un set d'armure à texture custom (equipment assets) |
| `dungeonmod/GUIDE_ITEMS_MODDES.md` | Guide items moddés |
| `dungeonmod/BLOCKBENCH_GECKOLIB.md` | Pipeline Blockbench → GeckoLib (modèles/anims) |

---

## 🔀 Workflow Git

- `main` = version stable en jeu
- Branches de travail → merge sur `main` quand validé
- **Committer souvent** (petits commits nommés) — toute l'histoire du projet y gagne

## 🗓️ Roadmap

- [ ] 3 nouveaux boss : **Cerbère** (chien à 3 têtes), **Méduse** (endort le joueur), **Mangemorts** — liés à une histoire de péchés originels
- [ ] Nouvelles salles & gameplay associés
- [ ] Contenu du Marchand Noir (salle déjà générée en P4)
- [ ] Scaling des boss en party (multi-joueurs)
