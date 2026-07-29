# Dungeon Mod - Documentation pour IA

## Architecture

Mod Fabric 1.21.4. Tous les items sont des items vanilla renommés (pas de `Items.register()`). Les items utilisent `DataComponentTypes.CUSTOM_NAME`, `DataComponentTypes.LORE`, `DataComponentTypes.ATTRIBUTE_MODIFIERS`, `DataComponentTypes.ITEM_MODEL`, `DataComponentTypes.EQUIPPABLE`.

**Fichiers source** : `src/main/java/com/dungeonmod/`

### Classes principales

| Fichier | Rôle |
|---------|------|
| `DungeonMod.java` | Initialisation, tick handlers, helpers |
| `ModItems.java` | Enregistrement de tous les items |
| `DungeonCommand.java` | Commandes `/teste`, `/lobby`, etc. |
| `TestGenerator.java` | Génération du donjon, spawn gobelins |

### Mixins

| Fichier | Cible | Effet |
|---------|-------|-------|
| `ZombieEntityMixin` | `ZombieEntity` | Silence sons zombie (ambient, hurt, death, step) |
| `AbstractZombieModelMixin` | `AbstractZombieModel` | Bras en position neutre |
| `ZombieEntityRenderStateMixin` | `ZombieEntityRenderState` | Skin custom gobelin |
| `ZombieBaseEntityRendererMixin` | `ZombieBaseEntityRenderer` | Texture custom gobelin |
| `InGameHudMixin` | `InGameHud` | Barre de vie centrée, armure cachée + texte "Protection = X%", barre food cachée |
| `LivingEntityRendererMixin` (client) | `LivingEntityRenderer` | Affiche PV/dégâts des monstres ≤5 blocs avec Casque du chasseur |
| `ItemStackMixin` | `ItemStack` | Cache le tooltip d'attributs pour tous les `ArmorItem` + crâne |
| `DamageUtilMixin` | `DamageUtil` | Remplace formule dégâts : `dégâts × (1 - armure/100)` (1% = 1% réduction) |
| `LivingEntityDamageMixin` | `ServerPlayerEntity` | Annule dégâts si `isHunterProne`, reflète dégâts avec Plastron du héros |
| `PlayerAttackMixin` | `PlayerEntity` | Annule attaque si `isHunterProne` |
| `LivingEntityPoseMixin` | `Entity.getPose()` | Force `SWIMMING` si sneak + Jambière du chasseur |
| `SnowballDamageMixin` | `SnowballEntity` | Dégâts sur bâton/os lancés (STICK→4 dégâts+knockback, BONE→4 dégâts+slowness) |
| `BackstabMixin` | `LivingEntity.damage` | Double les dégâts de la Dague si attaque de dos (dot < -0.3) |
| `ComboMixin` | `LivingEntity.damage` | Système de combo pour Hache en fer (1→1.5→2→4 coeurs, réinitialisé si changement cible) |

## Items

Tous les items sont enregistrés dans `ModItems.java` avec la méthode `register(id, displayName, vanillaItem, modifier?, lore...)`.

### Casques (slot HEAD)

| ID | Nom | Item | Armure | Effet |
|----|-----|------|--------|-------|
| `casque_chasseur` | Casque du chasseur | CHAINMAIL_HELMET | 4 | Montre PV/dégâts des monstres ≤5 blocs |
| `crane_squelette` | Crâne de squelette | SKELETON_SKULL | 2 | Gobelins moins agressifs (follow_range réduit) |
| `casque_lourd` | Casque lourd | IRON_HELMET | 8 | Overlay visière (caméra), -10% vitesse, -10% saut |
| `casque_mineur` | Casque du mineur | LEATHER_HELMET | 2 | Place un Light block (luminosité 14) au-dessus du joueur |

### Plastrons (slot CHEST)

| ID | Nom | Item | Armure | Effet |
|----|-----|------|--------|-------|
| `plastron_lourd` | Plastron lourd | IRON_CHESTPLATE | 12 | -30% vitesse, -30% saut, +10 absorption max (set 10 HP à l'équipement) |
| `plastron_heros` | Plastron du héros | GOLDEN_CHESTPLATE | 8 | Reflette les dégâts subis à l'attaquant |
| `plastron_chasseur` | Plastron du chasseur | CHAINMAIL_CHESTPLATE | 23 | Kill → +0,5 cœur ; Attaque +125% |
| `cape_du_voyageur` | Cape du voyageur | LEATHER_CHESTPLATE | 18 | Plane / chute nulle ; Attaque +70% |

### Jambières (slot LEGS)

| ID | Nom | Item | Armure | Effet |
|----|-----|------|--------|-------|
| `jambiere_voyageur` | Jambière du voyageur | LEATHER_LEGGINGS | 13 | +50% vitesse quand ≤4 PV ; Attaque +65% |
| `jambiere_chasseur` | Jambière du chasseur | CHAINMAIL_LEGGINGS | 17 | Sneak→mode caché ; Attaque +80% |
| `jambiere_lourde` | Jambière lourde | IRON_LEGGINGS | 23 | −20% vitesse/saut ; Attaque +95% |

### Bottes (slot FEET)

| ID | Nom | Item | Armure | Effet |
|----|-----|------|--------|-------|
| `bottes_sept_lieues` | Bottes de sept lieues | LEATHER_BOOTS | 7 | +100% vitesse ; Attaque +30% |
| `bottes_lourdes` | Bottes lourdes | IRON_BOOTS | 14 | Annule le recul ; Attaque +70% |
| `bottes_apollon` | Bottes de Mercure | GOLDEN_BOOTS | 9 | Double saut, chute nulle ; Attaque +40% |

### Armes (slot MAINHAND)

| ID | Nom | Item | ATK | Effet spécifique |
|----|-----|------|-----|------------------|
| `baton` | Bâton | STICK | +1 (total 1 cœur) | Clic droit→lance le bâton (4 dégâts+knockback), perdu |
| `dague` | Dague | FLINT | +2 (total 1.5 cœurs) | Backstab (par derrière) : dégâts doublés (3 cœurs) |
| `os` | Os | BONE | +1 (total 1 cœur) | Clic droit→lance l'os (4 dégâts+Slowness 255 2s+particules FIREWORK), perdu |
| `hache_fer` | Hache en fer | IRON_AXE | 0 (total 0.5 cœur de base) | Combo : 1→1.5→2→4 cœurs, réinitialisé si changement cible |

### Autres items

| ID | Nom | Item | Effet |
|----|-----|------|-------|
| `fiole` | Fiole | GLASS_BOTTLE | - |
| `fiole_benite` | Fiole d'eau bénite | POTION (orange) | Régénération III 5s |
| `pomme_rouge` | Pomme rouge | APPLE | Soigne 0.5 cœur |
| `patate_douce` | Patate douce | POISONOUS_POTATO | Soigne 1 cœur + nausée 10s |
| `steack_cru` | Steack cru | BEEF | Soigne 3 cœurs |
| `biere_brune` | Bière périmée | POTION (couleur orange) | Nausée 10s |
| `biere_blonde` | Bière blonde | HONEY_BOTTLE | Force 10s |
| `oeuf_*` | Oeufs | STICK | Spawn zombie/gobelin custom |
| `coeur` | Coeur | HEART_OF_THE_SEA | +1 cœur max + soigne 1 cœur |
| `cle` | Clé | TRIAL_KEY | Ouvre les portes en fer (une utilisation), ouvre aussi les doubles portes |
| `denier` | Denier | GOLD_NUGGET | Monnaie du jeu |

## Gobelins

- Zombies custom avec skins (`gobelin_1.png`, `gobelin_2.png`)
- Spawnés dans les salles M1/M2 (3 par salle)
- **HP** : 20 (10 cœurs)
- **ATK** : 2 (1 cœur)
- **Vitesse** : base × 1.4
- **Follow range** : idle=5, chase=15 (réduit à idle=4, chase=6 si joueur a le crâne)

## Règles du donjon

- **PV max** : 10 (5 cœurs)
- **Nourriture** : foodLevel=17, saturation=5 (pas de régénération naturelle)
- **Formule dégâts** : `dégâts × (1 - armure/100)` (remplace la formule vanilla)
- **Pas de barre food** (cancel via mixin)
- **Barre armure** : remplacée par texte "Protection = X%"
- **Barre vie** : centrée dynamiquement

## Systèmes complexes

### Camouflage (Jambière du chasseur)
- `isHunterProne(player)` : vérifie sneak + jambière porte + cooldown 2s
- `LivingEntityPoseMixin` : force `EntityPose.SWIMMING`
- `guideGoblinsHome` : `zombie.setTarget(null)` si cible prone
- `PlayerAttackMixin` : cancel `attack` si prone
- `LivingEntityDamageMixin` : cancel `damage` si prone
- Cooldown : `ConcurrentHashMap<UUID, Long>` pour gérer le timer entre threads
- Détection équipement : `showHunterCooldown()` vérifie `hadHunterLegs` transition

### Combo Hache
- `Map<UUID, UUID> currentTargets` (player→target) et `Map<UUID, Integer> combos` (player→combo)
- `@Inject` HEAD de `LivingEntity.damage` avec `cancellable = true`
- Appelle récursivement `living.damage(world, source, comboDamage)` avec flag `comboProcessing`
- Dégâts : combo 0→2, 1→3, 2→4, 3→8 (cap à 3)
- Changement de cible → combo remis à 0

### Clé Portes
- `UseBlockCallback` : détecte clic droit avec `isKey(stack)` sur `Blocks.IRON_DOOR`
- `tryOpenIronDoor()` : ouvre porte via `DoorBlock.setOpen()`, vérifie `openedDoors` set
- Double porte : vérifie les blocs adjacents (±1 horizontal) pour une autre porte en fer
- Consomme la clé (`stack.decrement(1)` en survie)

### Overlay Casque lourd
- `EquippableComponent.cameraOverlay` : texture `assets/dungeonmod/textures/misc/helmet_overlay.png`
- Générée via script PowerShell (carrés noirs semi-transparents, yeux rectangulaires avec dégradé)

### Miner Helmet (Light blocks)
- Place `Blocks.LIGHT` (level 14) à `player.getBlockPos().up(2)` chaque tick
- Supprime l'ancien bloc lumineux quand le joueur bouge ou enlève le casque
- Flags `Block.NOTIFY_LISTENERS | Block.REDRAW_ON_MAIN_THREAD` (3)

### Os Stun
- `stunnedEntities` (Set<UUID>) : ajouté via `SnowballDamageMixin` quand l'os touche
- `spawnBoneStars()` : chaque tick serveur, spawn 4 particules `FIREWORK` tournantes au-dessus de la tête
- Rotation basée sur `gameTime * 0.3` pour effet fluide
- Slowness amplifier 255, durée 40 ticks (2s)

## Slots d'armure
- 0 = FEET (bottes)
- 1 = LEGS (jambières)
- 2 = CHEST (plastron)
- 3 = HEAD (casque)

## Conventions de code
- Modifier les attributs : `stack.set(DataComponentTypes.ATTRIBUTE_MODIFIERS, AttributeModifiersComponent.builder().add(...).build().withShowInTooltip(false))`
- Identifiants uniques UUID : `Identifier.of("dungeonmod", "item_slot_effect")` (éviter doublons entre pièces)
- Items lancés : `SnowballEntity` avec `setVelocity(player, pitch, yaw, 0, 1.5, 0)`
- Détection d'item custom : `isOf(vanillaItem) && contains(CUSTOM_NAME) && get(CUSTOM_NAME).getString().contains("Nom")`
- Tick handlers : `ServerTickEvents.END_SERVER_TICK` pour food, casques, jambières, particules

## Ressources
- **Textures** : `assets/dungeonmod/textures/item/*.png` (16×16)
- **Models** : `assets/dungeonmod/models/item/*.json`
- **Item definitions** : `assets/dungeonmod/items/*.json`
- **Textures entité** : `assets/dungeonmod/textures/entity/*.png`
- **Lang** : `assets/dungeonmod/lang/fr_fr.json`, `en_us.json`

## Outils qualité de l'algo (ajout juil. 2026)

### Scripts à la racine de `dungeonmod/`
| Script | Effet |
|---|---|
| `run_client.py` | `gradlew.bat runClient` (client dev) |
| `clean_build.py` | `gradlew.bat clean build` — recompile TOUT à neuf |
| `ouvrir_donjon.py [seed]` | Génère le HTML de viz (`DungeonViz`) + validation auto en console |
| `test_algo.py [nb]` | Harnais de régression (`SeedHarness`) : N **échantillons joueur** (seed=0) + seeds dorées + vérifs auto |
| `test_algo.py --seed S` | Rejoue une seed de SORTIE précise (bug joueur) |
| `test_algo.py --range [start]` | DEBUG seulement : ancienne plage d'entrée 1..N (ne reflète PAS le joueur) |

⚠️ **Règle d'or du workflow** : `ouvrir_donjon.py` et `test_algo.py` exécutent les classes de `build/classes/java/main`. **Toujours lancer `clean_build.py` après un pull** — sinon le HTML/harnais tournent sur les VIEILLES classes et "prouvent" à tort que des bugs corrigés existent encore (arrivé 2× en juillet 2026).

### Harnais de régression (`debug/SeedHarness.java`)
- Classe pure sans dépendance Minecraft ; compilée par `gradlew build` ; exécutée via `test_algo.py`.
- **Sémantique des seeds (IMPORTANT)** : `generateDungeon(seed)` rejette en interne les layouts invalides (retry jusqu'à 100× en mode joueur `seed=0`, 20× en mode seed fixe). Seules les seeds de **SORTIE** (`DungeonResult.seed` / `getLastSeed()`) sont livrées au joueur. Tester la plage d'entrée `1..N` est trompeur (beaucoup de null / layouts jamais donnés au joueur). Le mode par défaut échantillonne donc comme `/teste` : `generateDungeon(0)`, puis valide le résultat réellement produit.
- Teste toujours les **seeds dorées** (régressions historiques = seeds de SORTIE) :
  - `224237267600147` — couloir à 3-4 connexions (raccords P2/P3 partagés)
  - `827324799543570426` — virage IJ2 à 3 connexions au sud de la Centrale (P4)
  - `227471353010315` — virage IJ2 à 4 connexions au sud (fusion des 2 arbres sud adjacents)
- Vérifie par donjon : génération non nulle · cohérence labels ↔ adjacence (`DungeonAlgo.validateStructure`) · connexité BFS des 2 étages · garanties gameplay (Prison, loot, Ogre, Centrale, PorteGob, MarchandNoir, PuitDJ, lootdj P4).
- Exit code 0/1 → chainable. **Objectif permanent : SUCCESS 100 % sur les échantillons joueur.** Tout nouveau bug d'algo → ajouter sa seed de SORTIE dans `GOLDEN_SEEDS`.

### Invariants structurels de l'algo (RÈGLE D'OR DU LABEL)
Un label structurel générique (`C1-3`/`I2`/`I3`/`I4`/`cul`, `CJ1-3`/`IJ2-4`/`culDJ`, `CG1`/`GI2-4`/`CDG`) se déduit **uniquement** de l'adjacence finale du nœud :
- Source unique : `DungeonAlgo.shapeOf(voisins)` → `Shape`, puis `shapeLabel(shape, theme, rng)` (thèmes `P12`, `DJ`, `GOBLIN`).
- Après TOUTE mutation du graphe suivant une classification : `reclassifyGeneric(labels, adj, rng)` (passe finale en fin P4 et fin de `generateDungeon`).
- Vérification : `DungeonAlgo.validateStructure(labels, adj, scope)` (appelée par DungeonViz et SeedHarness).
- **P4 : les chaînes initiales (`f1`/`f2`) des 5 arbres sont réservées dans `globalOccupied` AVANT toute croissance** (`reservedChains`). Ne JAMAIS poser une cellule sans vérifier/réserver l'occupation : c'est la cause corrigée de la "fusion du sud" (deux arbres partageant une cellule → voisinages fusionnés au merge `addAll`). Garde anti-fusion : retry si deux arbres partagent une cellule.

## Note pour les agents IA (Arena.ai) — récupération du sandbox

Le sandbox Arena peut être **re-cloné entre les tours** (déjà 2 occurrences, juillet 2026). Symptômes : branche locale revenue sur le vieux commit `a471d63`, travail récent présent en "modifications non commitées", refspec fetch limité à `main`, reflog = clone frais. **C'EST NORMAL — ne pas paniquer** : le serveur GitHub garde le bon état, et un commit greffé sur le vieux socle serait refusé par le push de toute façon (`fetch first`).

Procédure éprouvée :
1. Sauvegarde : `tar czf /home/user/worktree_backup.tgz --exclude=.git .`
2. `git config remote.origin.fetch "+refs/heads/*:refs/remotes/origin/*"` puis `git fetch origin`
3. `git log --oneline origin/arena/019fa7b9-donjonmod` (la branche de session officielle)
4. `git reset origin/arena/019fa7b9-donjonmod` (mode mixed — préserve le working tree intact)
5. `git status` : ne doivent rester que les vraies nouvelles différences ; restaurer le reste via `git restore <fichier>`
6. Committer UNIQUEMENT le delta réel voulu, pusher seulement sur `arena/019fa7b9-donjonmod` (jamais ailleurs). En cas de doute : `git ls-remote origin`.

Contraintes sandbox connues : **pas de JDK**, apt vide pour Java, réseau externe bloqué (seul GitHub passe) → aucune compilation/exécution Java côté agent. Validations de substitution : équilibre des accolades/parenthèses (Python), greps croisés, audit structurel du `dungeon_viz.html` (tooltips = coordonnées, cellSize=80, +23 offset, Centrale +40), puis demander à l'utilisateur : `python clean_build.py`.
