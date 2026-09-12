# Dungeon Mod - Documentation pour IA

## Architecture

Mod Fabric 1.21.4. **182 fichiers Java**, **85 mixins**, **75 items**. Les items sont des items vanilla renommés via `DataComponentTypes.CUSTOM_NAME`, `DataComponentTypes.LORE`, `DataComponentTypes.ATTRIBUTE_MODIFIERS`, `DataComponentTypes.ITEM_MODEL`, `DataComponentTypes.EQUIPPABLE` (pas de `Items.register()`), **sauf** `sac` (item custom réel `SacItem` enregistré via `Registry.register`).

**Fichiers source** : `src/main/java/com/dungeonmod/`

### Organisation du code (182 fichiers Java)

| Package | Fichiers | Rôle |
|---------|----------|------|
| `com.dungeonmod` | 4 | `DungeonMod` (init, ticks, helpers), `ModItems`, `DungeonCommand`, `DungeonModClient` |
| `debug/` | 14 | **Logique pure, zéro Minecraft** : `DungeonAlgo`, `DungeonPart1..4`, `DungeonTreeBuilder`, `DungeonConstraints`, `DungeonLabels`, `DungeonLabelState`, `DungeonCompositeRooms`, `RoomType`, `DungeonViz`, `SeedHarness`, `DungeonFailureLog` |
| `test/` | 1 | `TestGenerator` (placement des `.nbt`) |
| `mixin/` | 85 | Cœur du gameplay custom (catalogue complet plus bas) |
| `entity/` | 23 | Gobelins, `OgreEntity`, miniboss, PNJ (`BaseNpcEntity`, `Elias`, `Gaspard`, `Barman`, `NpcMerchant`), modèles/renderers |
| `entity/boss/` | 4 | `BossEntity`, `BossPhase`, `BossAnimation`, `BossRoom` |
| `entity/boss/capability/` | 7 | Capacités boss par composition (`BossHasCombos`, `BossEnrages`, `BossSummonsAdds`, `BossHasWeakPoint`, `BossBecomesNpc`, `BossHasDeathSequence`, `BossCapability`) |
| `util/` | 18 | Helpers par arme (`EpeeHelper`, `LanceHelper`, `FouetHelper`…), `DungeonLoot`, `RoomRewardManager`, `CraftingHelper`, grappin |
| `network/` | 7 | Payloads client/serveur (`Buy`, `Sell`, `Trade`, `Trades`, `OpenShop`, `Subtitle`, `JumpState`) |
| `screen/` | 6 | Boutiques et sac : handlers + écrans |
| `client/` | 5 | Rendu/overlays client |
| `client/dialogue/` | 4 | Système de dialogue PNJ |
| `village/` | 2 | `NpcMerchant`, `SellTradeRegistry` |
| `accessor/` | 1 | `CustomSkinAccessor` |
| `item/` | 1 | `SacItem` |

> ⚠️ **Le découpage en dossiers ne reflète pas l'architecture réelle.** Le graphe d'appels
> (codebase-memory) fait apparaître ~12 *clusters* (communautés de code) qui recoupent les
> dossiers différemment. Voir « Clusters (graphe) » ci-dessous.

### Clusters (graphe codebase-memory)
Communautés détectées sur le graphe d'appels/imports (Leiden) — utiles pour cibler un changement :

| # | Taille | Cohésion | Nœuds représentatifs | Thème |
|---|-------|----------|----------------------|-------|
| 2 | 156 | 0.48 | `get`, `contains`, `isEmpty`, `onInitialize` | Init / shop / registres |
| 5 | 117 | 0.58 | `put`, `remove`, `generatePart4Tree`, `analyzePart3`, `placeChapelAndCrypt` | Algo P3/P4 + viz |
| 30 | 91 | 0.76 | `damage`, `getPhase`, `tickCombat`, `getAttackState`, `stop` | Boss / combat |
| 3 | 86 | 0.57 | `generateRandomCave`, `spawnGoblins`, `register`, `convertGraphToCells` | Grotte / spawn / placement |
| 20 | 77 | 0.51 | `size`, `mouseClicked`, `getStack`, `startDialogue`, `getName` | Screens / UI |
| 4 | 67 | 0.58 | `get`, `isFleche`, `isBaguette`, `checkFlecheTimers` | Items / armes |
| 17 | 63 | 0.68 | `register`, `registerArmures`, `onInitializeClient`, `isGlaive` | Enregistrement items |
| 67 | 43 | 0.52 | `generateDungeon`, `validateResult`, `validateStructure` | Harnais / validation (+ `main` Python) |
| 40 | 28 | 0.85 | `buildSvg`, `renderToHtml`, `append` | Viz Python |
| 33 | 25 | 0.60 | `SubtitlePayload`, `openTradeShop`, `startDialogue` | PNJ / dialogues |
| 147 | 22 | 0.47 | `checkSpacingRules`, `findM5WithGapBeforeDoor`, `placeMonster5OnDoorPaths` | Contraintes de placement |
| 1 | 20 | 0.49 | `tryCraft`, `isAncreGrappling`, `onSwing`, `scanRoom` | Craft / grappin |

### Mixins (catalogue complet — 85 fichiers)

Cible réelle extraite de `@Mixin(...)` de chaque fichier. Un mixin peut modifier plusieurs
méthodes ; l'effet est résumé à une phrase.

#### Combat / dégâts (27)

| Fichier | Cible @Mixin | Effet |
|---------|--------------|-------|
| `AncreImmunityMixin` | `PlayerEntity` | Annule les dégâts d'une entité dont le joueur est immunisé après l'avoir traversée avec l'ancre |
| `ArcHerosHitMixin` | `PersistentProjectileEntity` | Les flèches marquées `SANG2_ARROWS` infligent Wither |
| `ArmorAttributeCapMixin` | `ClampedEntityAttribute` | Relève le plafond de l'attribut armure de 30 à 100 |
| `AttackCooldownMixin` | `PlayerEntity` | Annule l'attaque si la barre de recharge est incomplète (seuil 0,68 en double dague, sinon 1,0) |
| `BackstabMixin` | `LivingEntity` | Double les dégâts de la dague dans le dos ; ajoute un knockback dague |
| `BeerStrengthMixin` | `LivingEntity` | Multiplie les dégâts selon le bonus de force de la bière |
| `ComboMixin` | `LivingEntity` | Combo à la hache en fer (dégâts croissants, son d'enclume) |
| `DagueDamageMixin` | `LivingEntity` | Réduit de moitié les dégâts subis par un joueur en garde (parry) à la dague |
| `DamageUtilMixin` | `DamageUtil` | `@Overwrite` : remplace la formule vanilla par `dégâts × (1 − armure/100)` |
| `DarkBuffMixin` | `PlayerEntity` | Soigne 1 HP si un lien sombre actif sur la cible vient d'être frappé |
| `EpeeDamageMixin` | `LivingEntity` | Bloque les dégâts avec l'épée en garde, gère le guard break, annule le recul pendant la garde |
| `FauxDeFerMixin` | `PlayerEntity` | La faux de fer inflige une attaque de zone |
| `FireballDamageMixin` | `SmallFireballEntity` | Petite boule de feu : 1 dégât + 2 s de feu, marque la cible |
| `FlecheMeleeMixin` | `PlayerEntity` | Attaque mêlée à la flèche, combo de sang (dégâts, Wither, passage en sang 2) |
| `FlecheMixin` | `PersistentProjectileEntity` | Flèches custom : dégâts ramenés à 1, Wither si niveau sang 2 |
| `FouetMixin` | `PlayerEntity` | Le fouet tire l'ennemi vers le joueur (portée 12 blocs) |
| `GlaiveAttackMixin` | `PlayerEntity` | Attaque perforante en cône avec combo |
| `IdoleBonheurMixin` | `ServerPlayerEntity` | 25 % de chance d'annuler les dégâts (Idole du bonheur en main) |
| `LivingEntityDamageMixin` | `ServerPlayerEntity` | Annule les dégâts si le chasseur est accroupi ; renvoie les dégâts avec le Plastron du héros |
| `NoHitInvulnerabilityMixin` | `LivingEntity` | Remet `timeUntilRegen` à 0 pour les entités marquées sans invulnérabilité |
| `OgreEyeMixin` | `LivingEntity` | Détecte une touche à la tête de l'Ogre (mêlée ou projectile) |
| `PlayerAttackMixin` | `PlayerEntity` | Annule l'attaque si le chasseur est camouflé |
| `RingOfBloodMixin` | `LivingEntity` | Anneau de sang : ×2 dégâts entrants, ×2,5 dégâts sortants |
| `SnowballDamageMixin` | `SnowballEntity` | Dégâts/effets custom des projectiles renommés (bâton, os, torche, dague, flèche, caillou, boules) |
| `TorcheMixin` | `PlayerEntity` | La torche met le feu 3 s + knockback (portée 3 blocs) |
| `VoyageurMixin` | `LivingEntity` | Soigne 1 HP au tueur portant le Plastron du chasseur |
| `WeaponKnockbackMixin` | `PlayerEntity` | Knockback propre à chaque arme custom (dague, bâton, os, épée, lance, sabre) |

#### Items / armes (18)

| Fichier | Cible @Mixin | Effet |
|---------|--------------|-------|
| `ArcHerosMixin` | `BowItem` | Tir custom de l'Arc du héros (dégâts 6/10/14, perçage et marquage selon le niveau de sang) |
| `BaguetteAttackMixin` | `PlayerEntity` | Baguette : annule l'attaque vanilla et déclenche le tir |
| `BaguetteMixin` | `Item` | Fait cycler le type de baguette (feu/sombre/glace) selon les runes appliquées |
| `DagueDualMixin` | `Item` | Parry à la double dague (modèle blocking, Slowness, anti-toggle) |
| `DungeonConsumableMixin` | `Item` | Animations/effets des consommables (nourriture, bière, fiole), bonus ×2 du Plastron du glouton |
| `EpeeMixin` | `Item` | Permet de lever l'épée en garde (durée 72000, refus si cooldown du block) |
| `FlecheUsingMixin` | `Item` | Change le modèle de la flèche pendant/après l'utilisation selon le niveau de sang |
| `FouetChargeMixin` | `Item` | Charge le fouet puis déclenche l'attaque au relâcher |
| `GlaiveMixin` | `Item` | Attaque tournoyante du glaive (cône, particules, sons) |
| `ItemEntityMixin` | `ItemEntity` | Restaure à l'état normal une flèche de sang lâchée au sol |
| `LanceBlockHitMixin` | `ProjectileEntity` | La lance fait apparaître son item au point d'impact |
| `LanceTridentMixin` | `TridentEntity` | Force les dégâts de la lance à 6 |
| `NoVanillaRecipeMixin` | `Ingredient` | Empêche tout item renommé d'être utilisé dans une recette vanilla |
| `RuneApplyMixin` | `ScreenHandler` | Applique une rune sur une baguette lors d'un clic dans l'inventaire |
| `SabreComboMixin` | `PlayerEntity` | Mixin vide (injection retirée, conservé pour la refmap) |
| `SabreMixin` | `Item` | Combo du sabre, animation de slash et particules |
| `SyrinxMixin` | `Item` | Rend le Syrinx oublié utilisable comme un arc |
| `ThrownItemChargeMixin` | `Item` | Charge 1 s puis lance bâton, caillou, os ou torche |

#### Grappin / ancre (5)

| Fichier | Cible @Mixin | Effet |
|---------|--------------|-------|
| `AnchorSpawnMixin` | `PlayerEntity` | Persiste/restaure la position de spawn d'ancre |
| `AncreGrapplingMixin` | `PlayerEntity` | Notifie `AncreGrappling.onAttackEntity` lors d'une attaque au grappin |
| `BlockActionMixin` | `ServerPlayNetworkHandler` | Bloque la destruction de bloc pendant l'utilisation du grappin |
| `NoRespawnBlockMessageMixin` | `ServerCommonNetworkHandler` | Supprime le message « no respawn block » pour les joueurs ayant une ancre |
| `SwingAirMixin` | `ServerPlayNetworkHandler` | Déclenche le grappin ou l'attaque de zone de la faux au swing dans le vide |

#### HUD / UI / Client (11)

| Fichier | Cible @Mixin | Effet |
|---------|--------------|-------|
| `AdvancementMixin` | `AdvancementDisplay` | Les avancements ne sont plus annoncés dans le chat |
| `ArmorHudMixin` | `InGameHud` | `@Redirect` `getArmor` : masque la barre d'armure |
| `ClientAttackCooldownMixin` | `MinecraftClient` | Bloque l'attaque si cooldown incomplet, charge le combo sabre dans le vide, gère le tir de baguette |
| `DialogueHudMixin` | `InGameHud` | Masque hotbar/status/crosshair/XP pendant un dialogue |
| `DoubleJumpMixin` | `ClientPlayerEntity` | Double saut avec les bottes de Mercure/Apollon (son, particules, boost sprint) |
| `InGameHudMixin` | `InGameHud` | Recentre la barre de vie selon le max ; annule l'affichage d'armure |
| `InventoryScreenMixin` | `InventoryScreen` | Cache le bouton du livre de recettes ; vide le titre de craft |
| `KeyboardInputMixin` | `KeyboardInput` | Annule les déplacements pendant un dialogue |
| `MerchantScreenMixin` | `MerchantScreen` | Dessine des slots de trade supplémentaires pour le marchand « Cyclope » |
| `NoCraftInventoryMixin` | `InventoryScreen` | Retire le livre de recettes et masque la zone de craft hors créatif |
| `PauseMenuMixin` | `MinecraftClient` | Bloque l'ouverture du menu pause pendant un dialogue |

#### Entités / skins (8)

| Fichier | Cible @Mixin | Effet |
|---------|--------------|-------|
| `AbstractZombieModelMixin` | `AbstractZombieModel` | Remet à zéro pitch/roll des deux bras (bras neutres) |
| `LanceTridentModelMixin` | `TridentEntityModel` | `@Overwrite` : modèle simplifié de la lance (pôle + base) |
| `LivingEntityPoseMixin` | `Entity` | Force la pose SWIMMING du joueur accroupi portant la Jambière du chasseur |
| `LivingEntityRendererMixin` | `LivingEntityRenderer` | Ajoute PV et dégâts au nom des monstres si le Casque du chasseur est porté |
| `ZombieBaseEntityRendererMixin` | `ZombieBaseEntityRenderer` | Applique la texture custom des zombies marqués |
| `ZombieEntityMixin` | `ZombieEntity` | Supprime tous les sons des zombies |
| `ZombieEntityRenderStateMixin` | `ZombieEntityRenderState` | Ajoute les champs `customSkin`/`customTexture` (implémente `CustomSkinAccessor`) |
| `ZombieGoblinLootMixin` | `LivingEntity` | Les zombies custom font tomber du loot et incrémentent le compteur de kills gobelin |

#### Inventaire / craft (5)

| Fichier | Cible @Mixin | Effet |
|---------|--------------|-------|
| `CraftingMixin` | `ServerPlayNetworkHandler` | Craft custom bâton ↔ torche près d'un feu de camp ou de l'eau |
| `PlayerScreenHandlerMixin` | `PlayerScreenHandler` | Décale/masque les slots de craft hors créatif et réimplémente `quickMove` |
| `RecipeBookMixin` | `RecipeBookWidget` | `isOpen` renvoie toujours false (désactive le livre de recettes) |
| `SacKeepMixin` | `ServerPlayerEntity` | Retire/sauvegarde les sacs avant le drop de mort |
| `ScreenHandlerMixin` | `ScreenHandler` | Tronque la liste des stacks synchronisés à la taille réelle des slots |

#### Persistance / PV (4)

| Fichier | Cible @Mixin | Effet |
|---------|--------------|-------|
| `HeavyPersistenceMixin` | `PlayerEntity` | Persiste les pièces « heavy », l'absorption et les valeurs stockées |
| `MaxAbsorptionMixin` | `PlayerEntity` | Ajoute un attribut MAX_ABSORPTION de 100 |
| `MaxHealthPersistMixin` | `ServerPlayerEntity` | Persiste la vie maximale et l'état « dans le donjon » |
| `TetralameHealthMixin` | `PlayerEntity` | Persiste la vie sauvegardée liée à la Tétralame |

#### Accessors / Invokers (3)

| Fichier | Cible @Mixin | Effet |
|---------|--------------|-------|
| `PersistentProjectileEntityAccessor` | `PersistentProjectileEntity` | Expose `setPierceLevel` (Invoker) et `pickupType` (Accessor) |
| `ScreenHandlerInvoker` | `ScreenHandler` | Expose `insertItem` et `addSlot` en Invoker |
| `SlotAccessor` | `Slot` | Expose les setters mutables `x` et `y` du slot |

#### Divers (4)

| Fichier | Cible @Mixin | Effet |
|---------|--------------|-------|
| `FireSnowballMixin` | `SnowballEntity` | Particules selon le nom de la boule (feu, glace, sombre) |
| `InteractionBlockerMixin` | `AbstractBlock` | `onUse`/`onUseWithItem` renvoient FAIL pour les blocs bloqués |
| `ItemStackMixin` | `ItemStack` | Masque le tooltip d'attributs des armures et du crâne de squelette |
| `NoXpMixin` | `LivingEntity` | Aucune entité ne drop d'XP |

## Items

**75 items** au total : 74 via `ModItems.register(id, displayName, vanillaItem, modifier?, lore...)`
+ `sac` (item custom `SacItem`, enregistré via `Registry.register`). Les valeurs d'armure/attaque
ci-dessous proviennent des `ATTRIBUTE_MODIFIERS` réels du code. Les bonus « Attaque +X % » des
armures sont posés par `BeerStrengthData.registerArmorAttackBonus(...)` à l'enregistrement.
Tout item reçoit `MAX_STACK_SIZE = 1`, `UNBREAKABLE`, et un `CUSTOM_NAME`.

### Casques (slot HEAD)

| ID | Nom | Item | Armure | Effet |
|----|-----|------|--------|-------|
| `casque_chasseur` | Casque du chasseur | CHAINMAIL_HELMET | 10 | Voir PV/dégâts des monstres ≤5 blocs ; Attaque +65% |
| `crane_squelette` | Crâne de squelette | SKELETON_SKULL | 6 | Gobelins moins agressifs (follow_range réduit) ; Attaque +45% |
| `casque_lourd` | Casque lourd | IRON_HELMET | 17 | Overlay visière (caméra), −10% vitesse, −10% saut ; Attaque +75% |
| `casque_mineur` | Casque du mineur | LEATHER_HELMET | 6 | Place un Light block (luminosité 14) au-dessus du joueur ; Attaque +25% |

### Plastrons (slot CHEST)

| ID | Nom | Item | Armure | Effet |
|----|-----|------|--------|-------|
| `plastron_lourd` | Plastron lourd | IRON_CHESTPLATE | 12 | −30% vitesse/saut ; +10 absorption (set « lourd », cf. `resetHeavyAbsorption`) |
| `plastron_heros` | Plastron du héros | GOLDEN_CHESTPLATE | 8 | Renvoie les dégâts subis à l'attaquant |
| `plastron_chasseur` | Plastron du chasseur | CHAINMAIL_CHESTPLATE | 23 | Kill → +0,5 cœur ; Attaque +125% |
| `plastron_glouton` | Plastron du glouton | LEATHER_CHESTPLATE | 15 | Manger/boire instantané ; soins aliments ×2 ; durée potions/bières ×2 ; Attaque +40% |
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

| ID | Nom | Item | Attributs (ADD_VALUE sauf mention) | Effet spécifique |
|----|-----|------|-----------------------------------|------------------|
| `tetralame_mort_subite` | Tétralame mort subite | NETHERITE_SWORD | damage +39, range +0,5 | 0,5 cœur max tant qu'elle est en main ; ~20 cœurs de dégâts, portée 3,5 blocs |
| `baton` | Bâton | STICK | damage +1, speed −1,5, range −0,2 | Clic droit → lance le bâton (4 dégâts + knockback), perdu |
| `dague` | Dague | FLINT | damage +2, speed 0, range −0,5 | Backstab (par derrière) : dégâts doublés ; parry en double dague |
| `os` | Os | BONE | damage +4, speed −2,75 | Clic droit → lance l'os (4 dégâts + Slowness 255 2 s + particules), perdu |
| `hache_fer` | Hache en fer | IRON_AXE | speed −2 | Combo croissant sur la même cible, réinitialisé si changement de cible |
| `faux_fer` | Faux de fer | IRON_HOE | speed −2,333, range +0,5 | Attaque de zone (1,5 cœur), portée 3,5 blocs |
| `epee` | Épée | STONE_SWORD | damage +3, speed −2, range +0,2 | Blocage (clic droit), guard break |
| `lance` | Lance | TRIDENT | damage +2, speed −2,182, range +1,2 | Portée 4,2 blocs ; clic droit → lancer |
| `fleche` | Flèche | ARROW | speed −1,143, range −0,2 | Poignard mêlée ou projectile ; combo de sang |
| `arc_heros` | Arc du héros | BOW | damage +0 (base) | 2 → 4,66 cœurs selon le niveau de sang ; flèches custom uniquement |
| `fouet` | Fouet | STICK | speed −2,75, range +4,0 | Clic droit : charge puis tire l'ennemi (portée 12 blocs) |
| `torche` | Torche | STICK | speed −2,182 | Enflamme (0,5 cœur/s, 3 s) ; clic droit → lancer |
| `boomerang` | Boomerang | STICK | damage +1, speed −1,143, range −0,2 | Clic gauche → lancer (1 cœur), revient après 1,5 s |
| `sabre` | Sabre | IRON_SWORD | damage +3, speed −1,14, range +0,2 | Combo via attaques dans le vide (max 3), clic droit décharge |
| `glaive` | Glaive | STICK | damage +5, speed −3 | Combo perforant en cône, clic droit → attaque tournoyante |

### Baguettes magiques (slot MAINHAND)

`baguette_feu`, `baguette_glace`, `baguette_sombre` : STICK, speed −3, damage +0.
Clic gauche → projectile ; clic droit → change de forme. Runes (`rune_sombre`, `rune_glace`)
appliquées par glisser-déposer sur la baguette dans l'inventaire (`RuneApplyMixin`).
- **Feu** : boule de feu (0,5 cœur + brûlure 2,5 s).
- **Glace** : 1 cœur + ralentissement (0,5 cœur/s pendant 5 s).
- **Sombre** : drain de vie (2 cœurs + 0,5 cœur/s 5 s ; les dégâts sur la cible soignent).

### Consommables

| ID | Nom | Item | Effet |
|----|-----|------|-------|
| `fiole` | Fiole | GLASS_BOTTLE | Récipient vide |
| `fiole_benite` | Fiole d'eau bénite | POTION | Régénère 0,5 cœur/s pendant 8 s |
| `pomme_rouge` | Pomme rouge | APPLE | Soigne 0,5 cœur |
| `patate_douce` | Patate douce | POISONOUS_POTATO | Soigne 1 cœur + nausée |
| `steack_cru` | Steack cru | BEEF | Soigne 3 cœurs |
| `chair_gobelin_crue` | Chair de gobelin crue | BEEF | Soigne 2 cœurs |
| `chair_gobelin_cuite` | Chair de gobelin cuite | COOKED_BEEF | Soigne 5 cœurs |
| `biere_brune` | Bière périmée | POTION | Nausée + Force 50 % (20 s) ; reste une chope |
| `biere_viking` | Bière de Viking | HONEY_BOTTLE | Force 150 % (30 s) ; reste une chope |
| `chope_biere` | Chope de bière | GLASS_BOTTLE | Reste de bière bue |

### Objets divers / quête

| ID | Nom | Item | Effet |
|----|-----|------|-------|
| `sac` | Sac | `SacItem` | Conteneur (item custom, `item/SacItem.java`) |
| `totem_immortalite` | Totem d'immortalité | TOTEM_OF_UNDYING | Protège de la mort |
| `syrinx_oublie` | Syrinx oublié | STICK | Clic droit : joue des notes (utilisable comme un arc) |
| `idole_du_bonheur` | Idole du bonheur | ECHO_SHARD | 25 % de chance d'annuler les dégâts ennemis (main droite ou gauche) |
| `ancre` | Ancre | CONDUIT | Clic droit : point de réapparition sur l'eau des puits ; clic gauche : grappin ≤15 blocs |
| `coeur` | Cœur | HEART_OF_THE_SEA | +1 cœur max |
| `montre` | Montre | CLOCK | Décor / horloge arrêtée |
| `tablette_de_pierre` | Tablette de pierre | PAPER | Décor / support de runes |
| `poussiere_de_pierre` | Poussière de pierre | GUNPOWDER | Matériau |
| `sablier` | Sablier | CLOCK | Décor |
| `compas_casse` | Compas cassé | COMPASS | Aiguille qui tourne sans fin |
| `compas_repare` | Compas réparé | COMPASS | Pointe le **centre** du puits le plus proche du même étage |
| `anneau_basique` | Anneau basique | IRON_NUGGET | Anneau de fer |
| `croix` | Croix | STICK | Décor |
| `fragment_de_fer` | Fragment de fer | IRON_NUGGET | Matériau |
| `cle` | Clé | TRIAL_KEY | Ouvre les portes en fer (une utilisation, doubles portes incluses) |
| `cle_blanche` | Clé blanche | TRIAL_KEY | Ouvre la porte pale du jardin |
| `denier` | Denier | GOLD_NUGGET | Monnaie du jeu |
| `rune_sombre` | Rune sombre | STICK | À glisser sur une baguette (change la forme) |
| `rune_glace` | Rune de glace | STICK | À glisser sur une baguette (change la forme) |
| `anneau_sang` | Anneau de sang | STICK | Dégâts infligés +150 %, mais dégâts subis ×2 |
| `dent_de_loup` | Dent de loup | STICK | Révèle protection/force du porteur ; ennemis <40 % soulignés |
| `conseil` | Conseil | PAPER | Objet de quête (Gaspard) |
| `ongle_cyclope` | Ongle du Cyclope | BONE | Objet de quête |
| `caillou` | Caillou | SNOWBALL | Clic droit → lancer |
| `web` | Toile d'araignée | STRING | Matériau |
| `leather` | Cuir | LEATHER | Matériau |

### Œufs d'apparition (STICK)

| ID | Invoque |
|----|---------|
| `oeuf_zombie` | Zombie custom |
| `oeuf_gobelin_1` / `oeuf_gobelin_2` | Gobelins custom (skins 1 et 2) |
| `oeuf_lanceur_gobelin` | Gobelin lanceur de pierre |
| `oeuf_ogre` | Ogre lanceur de pierres |

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
- **Formule dégâts** : `dégâts × (1 - armure/100)` (remplace la formule vanilla) — 1 pt d'attribut ARMOR = 1 % de réduction, cap **100 %**
- **Plafond ARMOR vanilla = 30** : levé à 100 via `ArmorAttributeCapMixin` (sinon Dent de loup + réduction réelle bloquées à 30 %)
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
