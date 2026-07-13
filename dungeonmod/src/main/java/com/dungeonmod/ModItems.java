package com.dungeonmod;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.*;
import java.util.function.Consumer;

public class ModItems {

    private static final Map<String, CustomItem> ITEMS = new LinkedHashMap<>();

    public static class CustomItem {
        public final String id;
        public final String displayName;
        public final Item vanillaItem;
        public final String[] lore;
        public final Consumer<ItemStack> modifier;

        public CustomItem(String id, String displayName, Item vanillaItem, Consumer<ItemStack> modifier, String... lore) {
            this.id = id;
            this.displayName = displayName;
            this.vanillaItem = vanillaItem;
            this.modifier = modifier;
            this.lore = lore;
        }

        public ItemStack createStack() {
            ItemStack stack = new ItemStack(vanillaItem);
            stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(displayName));
            stack.set(DataComponentTypes.MAX_STACK_SIZE, 1);
            stack.set(DataComponentTypes.UNBREAKABLE, new net.minecraft.component.type.UnbreakableComponent(false));
            if (modifier != null) modifier.accept(stack);
            if (lore.length > 0) {
                List<Text> loreList = new ArrayList<>();
                for (String line : lore) loreList.add(Text.literal(line));
                stack.set(DataComponentTypes.LORE, new LoreComponent(loreList));
            }
            return stack;
        }
    }

    static {
        register("fiole", "§aFiole", Items.GLASS_BOTTLE, "§7Une fiole banale.", "§7Peut contenir de l'eau.");
        register("fiole_benite", "§9Fiole d'eau bénite", Items.POTION,
            stack -> {
                stack.set(DataComponentTypes.ITEM_MODEL, Identifier.of("dungeonmod", "fiole_benite"));
                stack.remove(DataComponentTypes.POTION_CONTENTS);
            },
            "§7Une eau bénite.", "§7Régénère 0,5 coeur/s pendant 8s.");
        register("pomme_rouge", "§aPomme rouge", Items.APPLE, "§7Une pomme rouge.", "§7Restaure un demi-coeur.");
        register("patate_douce", "§aPatate douce", Items.POISONOUS_POTATO, "§7Une patate douce.", "§7Restaure un coeur mais donne la nausée.");
        register("steack_cru", "§aSteack cru", Items.BEEF, "§7Un steack cru.", "§7Restaure trois coeurs.");
        ItemStack chopeStack = new ItemStack(Items.GLASS_BOTTLE);
        chopeStack.set(DataComponentTypes.CUSTOM_NAME, net.minecraft.text.Text.literal("§7Chope de bière"));
        chopeStack.set(DataComponentTypes.ITEM_MODEL, Identifier.of("dungeonmod", "chope_biere"));

        register("biere_brune", "§9Bière périmée", Items.POTION,
            stack -> {
                stack.set(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(Optional.empty(), Optional.of(0xE49A3A), List.of(), Optional.empty()));
                stack.set(DataComponentTypes.ITEM_MODEL, Identifier.of("dungeonmod", "biere_perimee"));
                stack.set(DataComponentTypes.USE_REMAINDER, new net.minecraft.component.type.UseRemainderComponent(chopeStack.copy()));
            },
            "§7Une bière périmée.", "§7Nausée + Force 50% (20s).");
        register("biere_viking", "§9Bière de Viking", Items.HONEY_BOTTLE,
            stack -> {
                stack.set(DataComponentTypes.ITEM_MODEL, Identifier.of("dungeonmod", "biere_blonde"));
                stack.set(DataComponentTypes.USE_REMAINDER, new net.minecraft.component.type.UseRemainderComponent(chopeStack.copy()));
            },
            "§7Une bière de Viking.", "§7Force 150% pendant 30s.");
        register("chope_biere", "§7Chope de bière", Items.GLASS_BOTTLE,
            stack -> stack.set(DataComponentTypes.ITEM_MODEL, Identifier.of("dungeonmod", "chope_biere")),
            "§7Une bière déjà bue.");
        register("oeuf_zombie", "§eOeuf de zombie", Items.STICK, "§7Un oeuf étrange.", "§7Fait apparaître un zombie custom.");
        register("oeuf_gobelin_1", "§eOeuf de gobelin 1", Items.STICK, "§7Un oeuf de gobelin.", "§7Fait apparaître un gobelin 1.");
        register("oeuf_gobelin_2", "§eOeuf de gobelin 2", Items.STICK, "§7Un oeuf de gobelin.", "§7Fait apparaître un gobelin 2.");
        register("casque_chasseur", "§9Casque du chasseur", Items.CHAINMAIL_HELMET,
            stack -> {
                stack.set(DataComponentTypes.ITEM_MODEL, Identifier.of("dungeonmod", "casque_chasseur"));
                stack.set(DataComponentTypes.ATTRIBUTE_MODIFIERS,
                    AttributeModifiersComponent.builder()
                        .add(EntityAttributes.ARMOR,
                            new EntityAttributeModifier(Identifier.of("dungeonmod", "casque_chasseur_armor"), 4.0, EntityAttributeModifier.Operation.ADD_VALUE),
                            AttributeModifierSlot.HEAD)
                        .build()
                        .withShowInTooltip(false));
                stack.set(DataComponentTypes.EQUIPPABLE,
                    EquippableComponent.builder(EquipmentSlot.HEAD)
                        .equipSound(SoundEvents.ITEM_ARMOR_EQUIP_CHAIN)
                        .swappable(true)
                        .build());
            },
            "§7Un casque léger.", "§7Portée : voir les PV des monstres à 5 blocs.", "§7Protection: +4");
        register("plastron_chasseur", "§9Plastron du chasseur", Items.CHAINMAIL_CHESTPLATE,
            stack -> stack.set(DataComponentTypes.ATTRIBUTE_MODIFIERS,
                AttributeModifiersComponent.builder()
                    .add(EntityAttributes.ARMOR,
                        new EntityAttributeModifier(Identifier.of("dungeonmod", "plastron_chasseur_armor"), 7.0, EntityAttributeModifier.Operation.ADD_VALUE),
                        AttributeModifierSlot.CHEST)
                    .build()
                    .withShowInTooltip(false)),
            "§7Un plastron de chasseur.", "§7Permet au fouet d'être utilisé", "§7comme un grappin.", "§7Ralentit la chute.", "§7Protection: +7");
        register("coeur", "§cCoeur", Items.HEART_OF_THE_SEA,
            stack -> stack.set(DataComponentTypes.ITEM_MODEL, net.minecraft.util.Identifier.of("dungeonmod", "coeur")),
            "§7Un coeur mystique.", "§7Utilisez pour gagner un coeur max supplémentaire.");
        register("crane_squelette", "§9Crâne de squelette", Items.SKELETON_SKULL,
            stack -> {
                stack.set(DataComponentTypes.ATTRIBUTE_MODIFIERS,
                    AttributeModifiersComponent.builder()
                        .add(EntityAttributes.ARMOR,
                            new EntityAttributeModifier(Identifier.of("dungeonmod", "crane_squelette_armor"), 2.0, EntityAttributeModifier.Operation.ADD_VALUE),
                            AttributeModifierSlot.HEAD)
                        .build()
                        .withShowInTooltip(false));
                stack.set(DataComponentTypes.EQUIPPABLE,
                    EquippableComponent.builder(EquipmentSlot.HEAD)
                        .equipSound(SoundEvents.ITEM_ARMOR_EQUIP_CHAIN)
                        .swappable(true)
                        .build());
            },
            "§7Un crâne qui protège des regards.", "§7Protection: +2");
        register("casque_lourd", "§9Casque lourd", Items.IRON_HELMET,
            stack -> {
                stack.set(DataComponentTypes.ATTRIBUTE_MODIFIERS,
                    AttributeModifiersComponent.builder()
                        .add(EntityAttributes.ARMOR,
                            new EntityAttributeModifier(Identifier.of("dungeonmod", "casque_lourd_armor"), 8.0, EntityAttributeModifier.Operation.ADD_VALUE),
                            AttributeModifierSlot.HEAD)
                        .add(EntityAttributes.MOVEMENT_SPEED,
                            new EntityAttributeModifier(Identifier.of("dungeonmod", "casque_lourd_speed"), -0.1, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL),
                            AttributeModifierSlot.HEAD)
                        .add(EntityAttributes.JUMP_STRENGTH,
                            new EntityAttributeModifier(Identifier.of("dungeonmod", "casque_lourd_jump"), -0.1, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL),
                            AttributeModifierSlot.HEAD)
                        .add(EntityAttributes.MAX_ABSORPTION,
                            new EntityAttributeModifier(Identifier.of("dungeonmod", "casque_lourd_absorption"), 2.0, EntityAttributeModifier.Operation.ADD_VALUE),
                            AttributeModifierSlot.HEAD)
                        .build()
                        .withShowInTooltip(false));
                stack.set(DataComponentTypes.EQUIPPABLE,
                    EquippableComponent.builder(EquipmentSlot.HEAD)
                        .equipSound(SoundEvents.ITEM_ARMOR_EQUIP_IRON)
                        .swappable(true)
                        .cameraOverlay(Identifier.of("dungeonmod", "misc/helmet_overlay"))
                        .build());
            },
            "§7Un casque lourd en fer.", "§7Protège beaucoup mais réduit la vision.", "§7Protection: +8");
        register("casque_mineur", "§9Casque du mineur", Items.LEATHER_HELMET,
            stack -> stack.set(DataComponentTypes.ATTRIBUTE_MODIFIERS,
                AttributeModifiersComponent.builder()
                    .add(EntityAttributes.ARMOR,
                        new EntityAttributeModifier(Identifier.of("dungeonmod", "casque_mineur_armor"), 2.0, EntityAttributeModifier.Operation.ADD_VALUE),
                        AttributeModifierSlot.HEAD)
                    .build()
                    .withShowInTooltip(false)),
            "§7Un casque qui éclaire les environs.", "§7Protection: +2");
        register("plastron_lourd", "§9Plastron lourd", Items.IRON_CHESTPLATE,
            stack -> stack.set(DataComponentTypes.ATTRIBUTE_MODIFIERS,
                AttributeModifiersComponent.builder()
                    .add(EntityAttributes.ARMOR,
                        new EntityAttributeModifier(Identifier.of("dungeonmod", "plastron_lourd_armor"), 12.0, EntityAttributeModifier.Operation.ADD_VALUE),
                        AttributeModifierSlot.CHEST)
                    .add(EntityAttributes.MOVEMENT_SPEED,
                        new EntityAttributeModifier(Identifier.of("dungeonmod", "plastron_lourd_speed"), -0.3, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL),
                        AttributeModifierSlot.CHEST)
                    .add(EntityAttributes.MAX_ABSORPTION,
                        new EntityAttributeModifier(Identifier.of("dungeonmod", "plastron_lourd_absorption"), 10.0, EntityAttributeModifier.Operation.ADD_VALUE),
                        AttributeModifierSlot.CHEST)
                    .add(EntityAttributes.JUMP_STRENGTH,
                        new EntityAttributeModifier(Identifier.of("dungeonmod", "plastron_lourd_jump"), -0.3, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL),
                        AttributeModifierSlot.CHEST)
                    .build()
                    .withShowInTooltip(false)),
            "§7Un plastron en fer très résistant.", "§7Ralentit le porteur mais augmente l'endurance.", "§7Protection: +12");
        register("jambiere_lourde", "§9Jambière lourde", Items.IRON_LEGGINGS,
            stack -> stack.set(DataComponentTypes.ATTRIBUTE_MODIFIERS,
                AttributeModifiersComponent.builder()
                    .add(EntityAttributes.ARMOR,
                        new EntityAttributeModifier(Identifier.of("dungeonmod", "jambiere_lourde_armor"), 6.0, EntityAttributeModifier.Operation.ADD_VALUE),
                        AttributeModifierSlot.LEGS)
                    .add(EntityAttributes.MOVEMENT_SPEED,
                        new EntityAttributeModifier(Identifier.of("dungeonmod", "jambiere_lourde_speed"), -0.2, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL),
                        AttributeModifierSlot.LEGS)
                    .add(EntityAttributes.JUMP_STRENGTH,
                        new EntityAttributeModifier(Identifier.of("dungeonmod", "jambiere_lourde_jump"), -0.2, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL),
                        AttributeModifierSlot.LEGS)
                    .add(EntityAttributes.MAX_ABSORPTION,
                        new EntityAttributeModifier(Identifier.of("dungeonmod", "jambiere_lourde_absorption"), 6.0, EntityAttributeModifier.Operation.ADD_VALUE),
                        AttributeModifierSlot.LEGS)
                    .build()
                    .withShowInTooltip(false)),
            "§7Des jambières en fer renforcées.", "§7Protection: +6");
        register("plastron_heros", "§ePlastron du héros", Items.GOLDEN_CHESTPLATE,
            stack -> stack.set(DataComponentTypes.ATTRIBUTE_MODIFIERS,
                AttributeModifiersComponent.builder()
                    .add(EntityAttributes.ARMOR,
                        new EntityAttributeModifier(Identifier.of("dungeonmod", "plastron_heros_armor"), 8.0, EntityAttributeModifier.Operation.ADD_VALUE),
                        AttributeModifierSlot.CHEST)
                    .build()
                    .withShowInTooltip(false)),
            "§7Un plastron légendaire.", "§7Reflette les dégâts reçus à l'attaquant.", "§7Protection: +8");
        register("plastron_voyageur", "§9Plastron du voyageur", Items.LEATHER_CHESTPLATE,
            stack -> stack.set(DataComponentTypes.ATTRIBUTE_MODIFIERS,
                AttributeModifiersComponent.builder()
                    .add(EntityAttributes.ARMOR,
                        new EntityAttributeModifier(Identifier.of("dungeonmod", "plastron_voyageur_armor"), 6.0, EntityAttributeModifier.Operation.ADD_VALUE),
                        AttributeModifierSlot.CHEST)
                    .build()
                    .withShowInTooltip(false)),
            "§7Un plastron de voyageur.", "§7Tuer un ennemi régénère 0.5 coeur.", "§7Protection: +6");
        register("jambiere_voyageur", "§9Jambière du voyageur", Items.LEATHER_LEGGINGS,
            stack -> stack.set(DataComponentTypes.ATTRIBUTE_MODIFIERS,
                AttributeModifiersComponent.builder()
                    .add(EntityAttributes.ARMOR,
                        new EntityAttributeModifier(Identifier.of("dungeonmod", "jambiere_voyageur_armor"), 5.0, EntityAttributeModifier.Operation.ADD_VALUE),
                        AttributeModifierSlot.LEGS)
                    .build()
                    .withShowInTooltip(false)),
            "§7Des jambières légères.", "§7Accélèrent quand la vie est basse.", "§7Protection: +5");
        register("jambiere_chasseur", "§9Jambière du chasseur", Items.CHAINMAIL_LEGGINGS,
            stack -> stack.set(DataComponentTypes.ATTRIBUTE_MODIFIERS,
                AttributeModifiersComponent.builder()
                    .add(EntityAttributes.ARMOR,
                        new EntityAttributeModifier(Identifier.of("dungeonmod", "jambiere_chasseur_armor"), 7.0, EntityAttributeModifier.Operation.ADD_VALUE),
                        AttributeModifierSlot.LEGS)
                    .build()
                    .withShowInTooltip(false)),
            "§7Des jambières de chasseur.", "§7S'accroupir permet", "§7de se cacher des monstres.", "§7Protection: +7");
        register("bottes_sept_lieues", "§9Bottes de sept lieues", Items.LEATHER_BOOTS,
            stack -> stack.set(DataComponentTypes.ATTRIBUTE_MODIFIERS,
                AttributeModifiersComponent.builder()
                    .add(EntityAttributes.ARMOR,
                        new EntityAttributeModifier(Identifier.of("dungeonmod", "bottes_armor"), 3.0, EntityAttributeModifier.Operation.ADD_VALUE),
                        AttributeModifierSlot.FEET)
                    .add(EntityAttributes.MOVEMENT_SPEED,
                        new EntityAttributeModifier(Identifier.of("dungeonmod", "bottes_speed"), 1.0, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL),
                        AttributeModifierSlot.FEET)
                    .build()
                    .withShowInTooltip(false)),
            "§7Des bottes légendaires.", "§7Permet de courir très vite.", "§7Protection: +3");
        register("baton", "§9Bâton", Items.STICK,
            stack -> stack.set(DataComponentTypes.ATTRIBUTE_MODIFIERS,
                AttributeModifiersComponent.builder()
                    .add(EntityAttributes.ATTACK_DAMAGE,
                        new EntityAttributeModifier(Identifier.of("dungeonmod", "baton_attack"), 1.0, EntityAttributeModifier.Operation.ADD_VALUE),
                        AttributeModifierSlot.MAINHAND)
                    .add(EntityAttributes.ATTACK_SPEED,
                        new EntityAttributeModifier(Identifier.of("dungeonmod", "baton_speed"), -1.5, EntityAttributeModifier.Operation.ADD_VALUE),
                        AttributeModifierSlot.MAINHAND)
                    .add(EntityAttributes.ENTITY_INTERACTION_RANGE,
                        new EntityAttributeModifier(Identifier.of("dungeonmod", "baton_range"), -0.2, EntityAttributeModifier.Operation.ADD_VALUE),
                        AttributeModifierSlot.MAINHAND)
                    .build()
                    .withShowInTooltip(false)),
            "§7Un bâton ordinaire.", "§7Clic droit pour le lancer.");
        register("dague", "§9Dague", Items.FLINT,
            stack -> {
                stack.set(DataComponentTypes.ITEM_MODEL, Identifier.of("dungeonmod", "dague"));
                stack.set(DataComponentTypes.ATTRIBUTE_MODIFIERS,
                    AttributeModifiersComponent.builder()
                        .add(EntityAttributes.ATTACK_DAMAGE,
                            new EntityAttributeModifier(Identifier.of("dungeonmod", "dague_attack"), 2.0, EntityAttributeModifier.Operation.ADD_VALUE),
                            AttributeModifierSlot.MAINHAND)
                        .add(EntityAttributes.ATTACK_SPEED,
                            new EntityAttributeModifier(Identifier.of("dungeonmod", "dague_speed"), 0.0, EntityAttributeModifier.Operation.ADD_VALUE),
                            AttributeModifierSlot.MAINHAND)
                        .add(EntityAttributes.ENTITY_INTERACTION_RANGE,
                            new EntityAttributeModifier(Identifier.of("dungeonmod", "dague_range"), -0.5, EntityAttributeModifier.Operation.ADD_VALUE),
                            AttributeModifierSlot.MAINHAND)
                        .build()
                        .withShowInTooltip(false));
            },
            "§7Une dague légère.", "§7Inflige 1.5 coeurs (3 coeurs par derrière).");
        register("cle", "§eClé", Items.TRIAL_KEY,
            stack -> stack.set(DataComponentTypes.ITEM_MODEL, Identifier.of("dungeonmod", "key")),
            "§7Une clé mystérieuse.", "§7Permet d'ouvrir les portes en fer.");
        register("cle_blanche", "§fClé blanche", Items.TRIAL_KEY,
            stack -> stack.set(DataComponentTypes.ITEM_MODEL, Identifier.of("dungeonmod", "key_white")),
            "§7Une clé blanche immaculée.", "§7Ouvre la porte pale du jardin.");
        register("os", "§aOs", Items.BONE,
            stack -> stack.set(DataComponentTypes.ATTRIBUTE_MODIFIERS,
                AttributeModifiersComponent.builder()
                    .add(EntityAttributes.ATTACK_DAMAGE,
                        new EntityAttributeModifier(Identifier.of("dungeonmod", "os_attack"), 4.0, EntityAttributeModifier.Operation.ADD_VALUE),
                        AttributeModifierSlot.MAINHAND)
                    .add(EntityAttributes.ATTACK_SPEED,
                        new EntityAttributeModifier(Identifier.of("dungeonmod", "os_speed"), -2.75, EntityAttributeModifier.Operation.ADD_VALUE),
                        AttributeModifierSlot.MAINHAND)
                    .build()
                    .withShowInTooltip(false)),
            "§7n'attend qu'à être fracassé sur un ennemi", "§7(clic droit pour fracasser)");
        register("hache_fer", "§9Hache en fer", Items.IRON_AXE,
            stack -> {
                stack.set(DataComponentTypes.ITEM_MODEL, Identifier.of("dungeonmod", "hache_fer"));
                stack.set(DataComponentTypes.ATTRIBUTE_MODIFIERS,
                    AttributeModifiersComponent.builder()
                        .add(EntityAttributes.ATTACK_SPEED,
                            new EntityAttributeModifier(Identifier.of("dungeonmod", "hache_fer_speed"), -2.0, EntityAttributeModifier.Operation.ADD_VALUE),
                            AttributeModifierSlot.MAINHAND)
                        .build()
                        .withShowInTooltip(false));
            },
            "§7Une hache en fer.", "§7Les dégâts augmentent si vous frappez", "§7le même ennemi (1→1.5→2.5→5.5 coeurs)");
        register("denier", "§7Denier", Items.GOLD_NUGGET,
            stack -> stack.set(DataComponentTypes.ITEM_MODEL, Identifier.of("dungeonmod", "denier")),
            "§7Une pièce de monnaie ancienne.");
        register("faux_fer", "§9Faux de fer", Items.IRON_HOE,
            stack -> {
                stack.set(DataComponentTypes.ITEM_MODEL, Identifier.of("dungeonmod", "faux_fer"));
                stack.set(DataComponentTypes.ATTRIBUTE_MODIFIERS,
                    AttributeModifiersComponent.builder()
                        .add(EntityAttributes.ATTACK_SPEED,
                            new EntityAttributeModifier(Identifier.of("dungeonmod", "faux_fer_speed"), -2.333, EntityAttributeModifier.Operation.ADD_VALUE),
                            AttributeModifierSlot.MAINHAND)
                        .add(EntityAttributes.ENTITY_INTERACTION_RANGE,
                            new EntityAttributeModifier(Identifier.of("dungeonmod", "faux_fer_range"), 0.5, EntityAttributeModifier.Operation.ADD_VALUE),
                            AttributeModifierSlot.MAINHAND)
                        .build()
                        .withShowInTooltip(false));
            },
            "§7Une faux en fer.", "§7Inflige 1.5 coeurs de zone.", "§7Portée: 3.5 blocs.");
        register("epee", "§9Epée", Items.STONE_SWORD,
            stack -> {
                stack.set(DataComponentTypes.ITEM_MODEL, Identifier.of("dungeonmod", "epee"));
                var builder = AttributeModifiersComponent.builder();
                builder.add(EntityAttributes.ATTACK_DAMAGE,
                    new EntityAttributeModifier(Identifier.of("dungeonmod", "epee_attack"), 3.0, EntityAttributeModifier.Operation.ADD_VALUE),
                    AttributeModifierSlot.MAINHAND);
                builder.add(EntityAttributes.ATTACK_SPEED,
                    new EntityAttributeModifier(Identifier.of("dungeonmod", "epee_speed"), -2.0, EntityAttributeModifier.Operation.ADD_VALUE),
                    AttributeModifierSlot.MAINHAND);
                builder.add(EntityAttributes.ENTITY_INTERACTION_RANGE,
                    new EntityAttributeModifier(Identifier.of("dungeonmod", "epee_range"), 0.2, EntityAttributeModifier.Operation.ADD_VALUE),
                    AttributeModifierSlot.MAINHAND);
                stack.set(DataComponentTypes.ATTRIBUTE_MODIFIERS, builder.build().withShowInTooltip(false));
                stack.set(DataComponentTypes.FOOD, new net.minecraft.component.type.FoodComponent.Builder().nutrition(0).saturationModifier(0).alwaysEdible().build());
            },
            "§7Une épée en pierre.", "§7Inflige 2 coeurs.", "§7Clic droit pour bloquer.");
        register("lance", "§9Lance", Items.TRIDENT,
            stack -> {
                stack.set(DataComponentTypes.ITEM_MODEL, Identifier.of("dungeonmod", "lance"));
                var builder = AttributeModifiersComponent.builder();
                builder.add(EntityAttributes.ATTACK_DAMAGE,
                    new EntityAttributeModifier(Identifier.of("dungeonmod", "lance_attack"), 2.0, EntityAttributeModifier.Operation.ADD_VALUE),
                    AttributeModifierSlot.MAINHAND);
                builder.add(EntityAttributes.ATTACK_SPEED,
                    new EntityAttributeModifier(Identifier.of("dungeonmod", "lance_speed"), -2.182, EntityAttributeModifier.Operation.ADD_VALUE),
                    AttributeModifierSlot.MAINHAND);
                builder.add(EntityAttributes.ENTITY_INTERACTION_RANGE,
                    new EntityAttributeModifier(Identifier.of("dungeonmod", "lance_reach"), 1.2, EntityAttributeModifier.Operation.ADD_VALUE),
                    AttributeModifierSlot.MAINHAND);
                stack.set(DataComponentTypes.ATTRIBUTE_MODIFIERS, builder.build().withShowInTooltip(false));
            },
            "§7Une lance en bois.", "§7Inflige 1.5 coeurs.", "§7Portée: 4.2 blocs.", "§7Clic droit pour lancer.");
        register("fleche", "§9Flèche", Items.ARROW,
            stack -> {
                stack.set(DataComponentTypes.MAX_STACK_SIZE, 1);
                stack.set(DataComponentTypes.ATTRIBUTE_MODIFIERS,
                    AttributeModifiersComponent.builder()
                        .add(EntityAttributes.ATTACK_SPEED,
                            new EntityAttributeModifier(Identifier.of("dungeonmod", "fleche_speed"), -1.143, EntityAttributeModifier.Operation.ADD_VALUE),
                            AttributeModifierSlot.MAINHAND)
                        .add(EntityAttributes.ENTITY_INTERACTION_RANGE,
                            new EntityAttributeModifier(Identifier.of("dungeonmod", "fleche_range"), -0.2, EntityAttributeModifier.Operation.ADD_VALUE),
                            AttributeModifierSlot.MAINHAND)
                        .build()
                        .withShowInTooltip(false));
            },
            "§7Une flèche simple.", "§7Utilisable comme poignard ou projectile.");
        register("arc_heros", "§eArc du héros", Items.BOW,
            stack -> {
                stack.set(DataComponentTypes.ITEM_MODEL, Identifier.of("dungeonmod", "arc_heros"));
                stack.set(DataComponentTypes.ATTRIBUTE_MODIFIERS,
                    AttributeModifiersComponent.builder()
                        .add(EntityAttributes.ATTACK_DAMAGE,
                            new EntityAttributeModifier(Identifier.of("dungeonmod", "arc_heros_damage"), 0.0, EntityAttributeModifier.Operation.ADD_VALUE),
                            AttributeModifierSlot.MAINHAND)
                        .build()
                        .withShowInTooltip(false));
            },
            "§7Un arc légendaire.", "§7Base: 2 coeurs, Sang I: 3.33 coeurs, Sang II: 4.66 coeurs.", "§7Seulement compatible avec les flèches custom.");
        register("fouet", "§9Fouet", Items.STICK,
            stack -> {
                stack.set(DataComponentTypes.ITEM_MODEL, Identifier.of("dungeonmod", "fouet"));
                stack.set(DataComponentTypes.ATTRIBUTE_MODIFIERS,
                    AttributeModifiersComponent.builder()
                        .add(EntityAttributes.ATTACK_SPEED,
                            new EntityAttributeModifier(Identifier.of("dungeonmod", "fouet_speed"), -2.75, EntityAttributeModifier.Operation.ADD_VALUE),
                            AttributeModifierSlot.MAINHAND)
                        .add(EntityAttributes.ENTITY_INTERACTION_RANGE,
                            new EntityAttributeModifier(Identifier.of("dungeonmod", "fouet_range"), 4.0, EntityAttributeModifier.Operation.ADD_VALUE),
                            AttributeModifierSlot.MAINHAND)
                        .build()
                        .withShowInTooltip(false));
            },
            "§7Un fouet en cuir.", "§70.5 cœur en mêlée.", "§7Clic droit: charge + ralentit.");
        register("torche", "§9Torche", Items.STICK,
            stack -> {
                stack.set(DataComponentTypes.ITEM_MODEL, Identifier.of("dungeonmod", "torche"));
                stack.set(DataComponentTypes.ATTRIBUTE_MODIFIERS,
                    AttributeModifiersComponent.builder()
                        .add(EntityAttributes.ATTACK_SPEED,
                            new EntityAttributeModifier(Identifier.of("dungeonmod", "torche_speed"), -2.182, EntityAttributeModifier.Operation.ADD_VALUE),
                            AttributeModifierSlot.MAINHAND)
                        .build()
                        .withShowInTooltip(false));
            },
            "§7Une torche qui éclaire.", "§7Enflamme les ennemis (0.5 cœur/s 3s).", "§7Clic droit pour lancer.");
        register("boomerang", "§aBoomerang", Items.STICK,
            stack -> {
                stack.set(DataComponentTypes.ITEM_MODEL, Identifier.of("dungeonmod", "boomerang"));
                stack.set(DataComponentTypes.ATTRIBUTE_MODIFIERS,
                    AttributeModifiersComponent.builder()
                        .add(EntityAttributes.ATTACK_DAMAGE,
                            new EntityAttributeModifier(Identifier.of("dungeonmod", "boomerang_attack"), 1.0, EntityAttributeModifier.Operation.ADD_VALUE),
                            AttributeModifierSlot.MAINHAND)
                        .add(EntityAttributes.ATTACK_SPEED,
                            new EntityAttributeModifier(Identifier.of("dungeonmod", "boomerang_speed"), -1.143, EntityAttributeModifier.Operation.ADD_VALUE),
                            AttributeModifierSlot.MAINHAND)
                        .add(EntityAttributes.ENTITY_INTERACTION_RANGE,
                            new EntityAttributeModifier(Identifier.of("dungeonmod", "boomerang_range"), -0.2, EntityAttributeModifier.Operation.ADD_VALUE),
                            AttributeModifierSlot.MAINHAND)
                        .build()
                        .withShowInTooltip(false));
            },
            "§7Un boomerang en bois.", "§7Clic gauche pour lancer (1 cœur).", "§7Revient après 1.5s.");
        register("sabre", "§9Sabre", Items.IRON_SWORD,
            stack -> {
                stack.set(DataComponentTypes.ITEM_MODEL, Identifier.of("dungeonmod", "sabre"));
                stack.set(DataComponentTypes.ATTRIBUTE_MODIFIERS,
                    AttributeModifiersComponent.builder()
                        .add(EntityAttributes.ATTACK_DAMAGE,
                            new EntityAttributeModifier(Identifier.of("dungeonmod", "sabre_attack"), 3.0, EntityAttributeModifier.Operation.ADD_VALUE),
                            AttributeModifierSlot.MAINHAND)
                        .add(EntityAttributes.ATTACK_SPEED,
                            new EntityAttributeModifier(Identifier.of("dungeonmod", "sabre_speed"), -1.14, EntityAttributeModifier.Operation.ADD_VALUE),
                            AttributeModifierSlot.MAINHAND)
                        .add(EntityAttributes.ENTITY_INTERACTION_RANGE,
                            new EntityAttributeModifier(Identifier.of("dungeonmod", "sabre_range"), 0.2, EntityAttributeModifier.Operation.ADD_VALUE),
                            AttributeModifierSlot.MAINHAND)
                        .build()
                        .withShowInTooltip(false));
            },
            "§7Un sabre léger.", "§7Attaquer dans le vide charge", "§7le combo (max 3).", "§7Clic droit : décharge le combo.", "§7Dégâts: 2 coeurs.");
        register("rune_sombre", "§5Rune sombre", Items.STICK,
            stack -> {
                stack.set(DataComponentTypes.ITEM_MODEL, Identifier.of("dungeonmod", "rune_sombre"));
                stack.set(DataComponentTypes.ATTRIBUTE_MODIFIERS, AttributeModifiersComponent.builder().build());
            },
            "§7Une rune sombre.", "§7Glissez-la sur une baguette", "§7dans l'inventaire pour l'appliquer.");
        register("rune_glace", "§bRune de glace", Items.STICK,
            stack -> {
                stack.set(DataComponentTypes.ITEM_MODEL, Identifier.of("dungeonmod", "rune_glace"));
                stack.set(DataComponentTypes.ATTRIBUTE_MODIFIERS, AttributeModifiersComponent.builder().build());
            },
            "§7Une rune gelée.", "§7Glissez-la sur une baguette", "§7dans l'inventaire pour l'appliquer.");
        var baguetteBuilder = AttributeModifiersComponent.builder()
            .add(EntityAttributes.ATTACK_SPEED,
                new EntityAttributeModifier(Identifier.of("dungeonmod", "baguette_speed"), -3.0, EntityAttributeModifier.Operation.ADD_VALUE),
                AttributeModifierSlot.MAINHAND)
            .add(EntityAttributes.ATTACK_DAMAGE,
                new EntityAttributeModifier(Identifier.of("dungeonmod", "baguette_damage"), 0.0, EntityAttributeModifier.Operation.ADD_VALUE),
                AttributeModifierSlot.MAINHAND);
        var baguetteModifiers = baguetteBuilder.build().withShowInTooltip(false);
        register("baguette_feu", "§cBaguette de feu", Items.STICK,
            stack -> {
                stack.set(DataComponentTypes.ITEM_MODEL, Identifier.of("dungeonmod", "baguette_feu"));
                stack.set(DataComponentTypes.ATTRIBUTE_MODIFIERS, baguetteModifiers);
            },
            "§7Une baguette enflammee.", "§7Clic gauche : tire une boule de feu (0,5 coeur + brulee 2,5s).", "§7Clic droit : change de forme.");
        register("baguette_glace", "§bBaguette de glace", Items.STICK,
            stack -> {
                stack.set(DataComponentTypes.ITEM_MODEL, Identifier.of("dungeonmod", "baguette_glace"));
                stack.set(DataComponentTypes.ATTRIBUTE_MODIFIERS, baguetteModifiers);
            },
            "§7Une baguette gelee.", "§7Clic gauche : glace l'ennemi (1 coeur + ralentissement).", "§70,5 coeur/s pendant 5s.");
        register("baguette_sombre", "§5Baguette sombre", Items.STICK,
            stack -> {
                stack.set(DataComponentTypes.ITEM_MODEL, Identifier.of("dungeonmod", "baguette_sombre"));
                stack.set(DataComponentTypes.ATTRIBUTE_MODIFIERS, baguetteModifiers);
            },
            "§7Une baguette des tenebres.", "§7Clic gauche : drain de vie (2 coeurs + 0,5 coeur/s 5s).", "§7Lien : les degats sur la cible soignent.");
        register("anneau_sang", "§cAnneau de sang", Items.STICK,
            stack -> {
                stack.set(DataComponentTypes.ITEM_MODEL, Identifier.of("dungeonmod", "anneau_sang"));
                stack.set(DataComponentTypes.ATTRIBUTE_MODIFIERS, AttributeModifiersComponent.builder().build());
            },
            "§7Un anneau ensanglante.", "§7Augmente les degats de 150%.", "§7Mais vous prenez 2x plus de degats.");
        register("bottes_apollon", "§6Bottes d'Apollon", Items.GOLDEN_BOOTS,
            stack -> {
                stack.set(DataComponentTypes.ATTRIBUTE_MODIFIERS,
                    AttributeModifiersComponent.builder()
                        .add(EntityAttributes.ARMOR,
                            new EntityAttributeModifier(Identifier.of("dungeonmod", "bottes_apollon_armor"), 1.0, EntityAttributeModifier.Operation.ADD_VALUE),
                            AttributeModifierSlot.FEET)
                        .add(EntityAttributes.FALL_DAMAGE_MULTIPLIER,
                            new EntityAttributeModifier(Identifier.of("dungeonmod", "bottes_apollon_fall"), -1.0, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL),
                            AttributeModifierSlot.FEET)
                        .build()
                        .withShowInTooltip(false));
            },
            "§7Des bottes légères.", "§7Permettent d'effectuer un double saut.", "§7Annule les dégâts de chute.", "§7Protection: +1");
        register("dent_de_loup", "§7Dent de loup", Items.STICK,
            stack -> stack.set(DataComponentTypes.ITEM_MODEL, Identifier.of("dungeonmod", "dent_de_loup")),
            "§7Une dent de loup.", "§7Révèle la protection et la force du porteur.", "§7Les ennemis affaiblis (< 40%) sont soulignés.");
        register("glaive", "§9Glaive", Items.STICK,
            stack -> {
                stack.set(DataComponentTypes.ITEM_MODEL, Identifier.of("dungeonmod", "glaive"));
                stack.set(DataComponentTypes.ATTRIBUTE_MODIFIERS,
                    AttributeModifiersComponent.builder()
                        .add(EntityAttributes.ATTACK_DAMAGE,
                            new EntityAttributeModifier(Identifier.of("dungeonmod", "glaive_attack"), 5.0, EntityAttributeModifier.Operation.ADD_VALUE),
                            AttributeModifierSlot.MAINHAND)
                        .add(EntityAttributes.ATTACK_SPEED,
                            new EntityAttributeModifier(Identifier.of("dungeonmod", "glaive_speed"), -3.0, EntityAttributeModifier.Operation.ADD_VALUE),
                            AttributeModifierSlot.MAINHAND)
                        .build()
                        .withShowInTooltip(false));
            },
            "§7Un glaive tranchant.", "§7Enchaînez les coups pour accelerer.", "§7Clic droit : attaque tournoyante.");
    }

    private static void register(String id, String displayName, Item vanillaItem, String... lore) {
        ITEMS.put(id, new CustomItem(id, displayName, vanillaItem, null, lore));
    }

    private static void register(String id, String displayName, Item vanillaItem, Consumer<ItemStack> modifier, String... lore) {
        ITEMS.put(id, new CustomItem(id, displayName, vanillaItem, modifier, lore));
    }

    public static CustomItem get(String id) {
        return ITEMS.get(id);
    }

    public static Collection<String> getIds() {
        return ITEMS.keySet();
    }

    public static List<CustomItem> getAll() {
        return new ArrayList<>(ITEMS.values());
    }

    public static void addToCreativeTabs() {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(entries -> {
            for (CustomItem item : ITEMS.values()) {
                entries.add(item.createStack());
            }
        });
    }
}
