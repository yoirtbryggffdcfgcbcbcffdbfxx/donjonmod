# Guide : Items dans DungeonMod

## Principe
**Aucun item custom.** On utilise des items vanilla renommés. La texture est déjà celle de l'item vanilla.

## Comment créer un item

### 1. Ajouter dans `ModItems.java`

```java
register("id_de_litem", "§eNom Affiché", Items.ITEM_VANILLA);
```

Exemples :
```java
register("fiole", "Fiole", Items.GLASS_BOTTLE);
register("fiole_benite", "§9Fiole d'eau bénite", Items.POTION);
register("bouclier_sacré", "§6Bouclier Sacré", Items.SHIELD);
register("clé_dungeon", "§cClé du Donjon", Items.TRIPWIRE_HOOK);
```

C'est tout. Rien d'autre à faire. L'item :
- Apparaît dans le tab créatif (onglet Combat)
- Est donnable via `/teste item <id>`
- A la texture de l'item vanilla choisi

### 2. Donner l'item au joueur (dans le code Java)

```java
ModItems.CustomItem item = ModItems.get("fiole");
player.giveItemStack(item.createStack());
```

### 3. Vérifier si un item en main est "le nôtre"

```java
ModItems.CustomItem item = ModItems.get("fiole");
if (stack.isOf(item.vanillaItem) && stack.contains(DataComponentTypes.CUSTOM_NAME)) {
    String name = stack.get(DataComponentTypes.CUSTOM_NAME).getString();
    if (name.contains("Fiole")) {
        // C'est notre fiole
    }
}
```

### 4. Transformer un item en un autre (changer d'apparence)

```java
// La fiole normale → bénite (changement de texture automatique)
ModItems.CustomItem blessed = ModItems.get("fiole_benite");
player.setStackInHand(hand, blessed.createStack());
```

---

## Commandes disponibles

| Commande | Effet |
|----------|-------|
| `/teste item fiole` | Donne une fiole normale |
| `/teste item fiole_benite` | Donne une fiole bénite |
| `/teste item` + Tab | Autocomplete de tous les items |

---

## Liste des items actuels

| ID | Nom | Item vanilla | Texture |
|----|-----|-------------|---------|
| `fiole` | Fiole | `Items.GLASS_BOTTLE` | glass_bottle |
| `fiole_benite` | §9Fiole d'eau bénite | `Items.POTION` | potion |
