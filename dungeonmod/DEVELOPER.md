# Guide du développeur — Dungeon Mod

## Créer un item modé

### 1. Enregistrer l'item

Dans `ModItems.java`, utiliser la méthode `register()` :

```java
register("id_item", "§9NomAffiché", Items.BASE_ITEM,
    stack -> {
        stack.set(DataComponentTypes.ITEM_MODEL, Identifier.of("dungeonmod", "id_item"));
        stack.set(DataComponentTypes.ATTRIBUTE_MODIFIERS,
            AttributeModifiersComponent.builder()
                .add(EntityAttributes.ATTACK_DAMAGE,
                    new EntityAttributeModifier(Identifier.of("dungeonmod", "id_item_attack"), VALEUR, EntityAttributeModifier.Operation.ADD_VALUE),
                    AttributeModifierSlot.MAINHAND)
                .add(EntityAttributes.ATTACK_SPEED,
                    new EntityAttributeModifier(Identifier.of("dungeonmod", "id_item_speed"), VALEUR, EntityAttributeModifier.Operation.ADD_VALUE),
                    AttributeModifierSlot.MAINHAND)
                .add(EntityAttributes.ENTITY_INTERACTION_RANGE,
                    new EntityAttributeModifier(Identifier.of("dungeonmod", "id_item_range"), VALEUR, EntityAttributeModifier.Operation.ADD_VALUE),
                    AttributeModifierSlot.MAINHAND)
                .build()
                .withShowInTooltip(false));
    },
    "§7Ligne de description 1.", "§7Ligne de description 2.");
```

### 2. Calcul des attributs

Les attributs utilisent **ADD_VALUE** sur les valeurs de base du joueur :

| Attribut | Base | Calcul |
|---|---|---|
| `ATTACK_DAMAGE` | 1.0 | `1.0 + modificateur` = dégâts finaux (2.0 = 1 cœur) |
| `ATTACK_SPEED` | 4.0 | `4.0 + modificateur` = attaques/seconde (1.0 = 1s cooldown) |
| `ENTITY_INTERACTION_RANGE` | 3.0 | `3.0 + modificateur` = portée en blocs |

**Exemples :**

- **Dague** : `attack = +2.0` → 3.0 (1.5 coeurs), `speed = 0.0` → 4.0 (250ms), `range = -0.5` → 2.5 blocs
- **Épée** : `attack = +3.0` → 4.0 (2 coeurs), `speed = -2.0` → 2.0 (500ms), `range = +0.2` → 3.2 blocs
- **Sabre** : `attack = +3.0` → 4.0 (2 coeurs), `speed = -1.14` → 2.86 (350ms), `range = +0.2` → 3.2 blocs

**Important :** `stack.set(DataComponentTypes.ATTRIBUTE_MODIFIERS, ...)` **remplace** entièrement les attributs par défaut de l'item de base. Si vous utilisez `Items.IRON_SWORD`, son bonus de dégâts +6.0 est perdu.

### 3. Knockback

Ajouter l'item dans `WeaponKnockbackMixin.java` :

```java
boolean isMonItem = stack.isOf(Items.BASE_ITEM) && name.contains("Nom");
// ...
else if (isMonItem) kb = VALEUR;
```

Les valeurs standards : dague = 0.1, épée/bâton/lance = 0.2, os = 0.5.

### 4. Fichiers JSON

Deux fichiers sont nécessaires :

**`assets/dungeonmod/models/item/mon_item.json`** — le modèle visuel :
```json
{
    "parent": "minecraft:item/handheld",
    "textures": {
        "layer0": "dungeonmod:item/mon_item"
    }
}
```

**`assets/dungeonmod/items/mon_item.json`** — la définition d'item :
```json
{
  "model": {
    "type": "minecraft:model",
    "model": "dungeonmod:item/mon_item"
  }
}
```

Le fichier PNG de la texture va dans `assets/dungeonmod/textures/item/mon_item.png`.

---

## Créer une animation (PlayerAnimator)

### 1. Fichier d'animation

Placer dans `assets/dungeonmod/player_animation/mon_anim.animation.json` :

```json
{
	"format_version": "1.8.0",
	"animations": {
		"animation.mon_anim.nom": {
			"loop": false,
			"animation_length": 0.3,
			"bones": {
				"right_arm": {
					"rotation": {
						"0.0": [-140, 0, 0],
						"0.1": [-60, 0, 0],
						"0.2": [-20, 0, 0],
						"0.3": [0, 0, 0]
					}
				}
			}
		}
	}
}
```

**Os disponibles :** `head`, `torso`, `right_arm`, `left_arm`, `right_leg`, `left_leg`, `body` (le corps entier)

**Propriétés animables :** `rotation` (angles), `position` (décalage)
**Easings supportés :** `LINEAR`, `IN_SINE`, `OUT_SINE`, `INOUTSINE`, `IN_QUAD`, `OUT_QUAD`, etc.

### 2. Enregistrer la couche d'animation (côté client)

Dans `DungeonModClient.java` :

```java
ClientTickEvents.END_CLIENT_TICK.register(client -> {
    if (client.world == null || client.player == null) return;
    if (maAnimDejaEnregistree) return;
    maAnimDejaEnregistree = true;

    var animStack = PlayerAnimationAccess.getPlayerAnimLayer(client.player);
    ModifierLayer<IAnimation> layer = new ModifierLayer<>();
    animStack.addAnimLayer(42, layer); // 42 = priorité
    MaClasseData.animLayers.put(client.player.getUuid(), layer);
});
```

### 3. Déclencher l'animation (côté serveur)

```java
ModifierLayer<IAnimation> layer = MaClasseData.animLayers.get(player.getUuid());
if (layer == null) return;
var playable = PlayerAnimationRegistry.getAnimation(Identifier.of("dungeonmod", "nom_mon_anim"));
if (playable == null) return;
layer.replaceAnimationWithFade(
    AbstractFadeModifier.standardFadeIn(2, Ease.LINEAR),
    playable.playAnimation()
        .setFirstPersonMode(FirstPersonMode.THIRD_PERSON_MODEL)
        .setFirstPersonConfiguration(new FirstPersonConfiguration()
            .setShowRightItem(true).setShowRightArm(true))
);
```

### 4. Règles importantes

- Le **dossier** est `player_animation/` (pas `animations/`)
- **Tous les mixins** doivent utiliser `@Unique private` ou `private` pour leurs méthodes statiques. Les méthodes `public static` sont **interdites** dans les classes mixin car elles sont injectées dans la classe cible
- Les données partagées entre client et serveur doivent être dans une **classe utilitaire** (package `util/`), jamais dans un mixin
- La clé de l'animation dans `getAnimation()` est le nom complet dans le JSON : `"animation.mon_anim.nom"` — et le `Identifier` est `Identifier.of("dungeonmod", "animation.mon_anim.nom")`

---

## Créer un mixin avec données partagées

**Ne JAMAIS mettre de `public static` dans un mixin.** La bonne pratique :

```java
// Dans le mixin
@Mixin(Item.class)
public class MonMixin {
    @Inject(method = "use", ...)
    private void onUse(...) {
        MaClasseUtilitaire.maMethode();
    }
}

// Dans util/MaClasseUtilitaire.java
public class MaClasseUtilitaire {
    private static final Map<UUID, Donnees> mesDonnees = new HashMap<>();
    
    public static void maMethode() { ... }
}
```

---

## Comment tester une animation

1. Lancer le jeu après un `./gradlew build`
2. Utiliser `/teste item nom_item` pour obtenir l'item
3. Faire clic droit pour déclencher l'animation
4. Les logs montrent `[SABRE] playable=...` si l'animation est trouvée
5. Modifier le JSON dans `player_animation/` et faire **F3+T** pour recharger (uniquement après le premier restart ayant chargé le dossier)
