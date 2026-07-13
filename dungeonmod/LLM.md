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
| `plastron_voyageur` | Plastron du voyageur | LEATHER_CHESTPLATE | 6 | Portée blocs +1.5 (→6), portée entité +1 (→4) |

### Jambières (slot LEGS)

| ID | Nom | Item | Armure | Effet |
|----|-----|------|--------|-------|
| `jambiere_voyageur` | Jambière du voyageur | LEATHER_LEGGINGS | 5 | +50% vitesse quand ≤4 PV |
| `jambiere_chasseur` | Jambière du chasseur | CHAINMAIL_LEGGINGS | 7 | Sneak→mode caché (pose SWIMMING), gobelins ignorent, dégâts annulés. Cooldown 2s. |

### Bottes (slot FEET)

| ID | Nom | Item | Armure | Effet |
|----|-----|------|--------|-------|
| `bottes_sept_lieues` | Bottes de sept lieues | LEATHER_BOOTS | 3 | +30% vitesse, +70% saut, 0 dégâts de chute |

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
