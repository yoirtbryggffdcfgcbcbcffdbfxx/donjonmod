# Armor Registration in Dungeon Mod (1.21.4)

## Files required per armor set

```
src/main/resources/assets/dungeonmod/
├── equipment/armure_lourde.json          # Equipment asset definition
├── textures/entity/equipment/
│   ├── humanoid/armure_lourde.png        # Layer 1 texture (helmet/chest/feet)
│   └── humanoid_leggings/armure_lourde.png # Layer 2 texture (leggings)
├── textures/item/casque_lourd.png        # Icon in inventory
├── models/item/casque_lourd.json         # Item model
└── items/casque_lourd.json              # 1.21.4 item definition
```

## Step 1: Equipment JSON

`assets/dungeonmod/equipment/armure_lourde.json`:
```json
{
  "layers": {
    "humanoid": [{ "texture": "dungeonmod:armure_lourde" }],
    "humanoid_leggings": [{ "texture": "dungeonmod:armure_lourde" }],
    "horse_body": [{ "texture": "minecraft:iron" }]
  }
}
```

## Step 2: Armor textures

- `textures/entity/equipment/humanoid/armure_lourde.png` (64x64)
- `textures/entity/equipment/humanoid_leggings/armure_lourde.png` (64x64)

## Step 3: Item registration in ModItems.java

Each armor piece needs:
- `DataComponentTypes.ITEM_MODEL` → custom icon model
- `DataComponentTypes.ATTRIBUTE_MODIFIERS` → armor stats
- `DataComponentTypes.EQUIPPABLE` with `.model(key)` → links to equipment asset

```java
register("casque_lourd", "§9Casque lourd", Items.IRON_HELMET,
    stack -> {
        stack.set(DataComponentTypes.ITEM_MODEL, Identifier.of("dungeonmod", "casque_lourd"));
        stack.set(DataComponentTypes.ATTRIBUTE_MODIFIERS, new AttrBuilder()
            .armor("key_armor", 8.0, AttributeModifierSlot.HEAD).build());
        stack.set(DataComponentTypes.EQUIPPABLE,
            EquippableComponent.builder(EquipmentSlot.HEAD)
                .model(RegistryKey.of(EquipmentAssetKeys.REGISTRY_KEY,
                    Identifier.of("dungeonmod", "armure_lourde")))
                .equipSound(SoundEvents.ITEM_ARMOR_EQUIP_IRON)
                .swappable(true)
                .build());
    },
    "Lore line 1", "Lore line 2");
```

## Equipment slots mapping

| Slot | Vanilla base | Equipment slot |
|------|-------------|----------------|
| Helmet | `Items.IRON_HELMET` | `EquipmentSlot.HEAD` |
| Chestplate | `Items.IRON_CHESTPLATE` | `EquipmentSlot.CHEST` |
| Leggings | `Items.IRON_LEGGINGS` | `EquipmentSlot.LEGS` |
| Boots | `Items.IRON_BOOTS` | `EquipmentSlot.FEET` |
