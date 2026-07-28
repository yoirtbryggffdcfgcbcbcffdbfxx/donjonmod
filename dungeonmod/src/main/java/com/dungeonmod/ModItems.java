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
import net.minecraft.item.equipment.EquipmentAssetKeys;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import com.dungeonmod.item.SacItem;

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

    /**
     * Petit utilitaire pour réduire le boilerplate des AttributeModifiersComponent.
     * IMPORTANT : ne modifie AUCUN identifiant existant. Chaque appelant doit fournir
     * exactement le même suffixe d'Identifier que dans le code d'origine, pour garantir
     * qu'aucune référence externe (combo, runes, etc.) ne soit cassée.
     */
    private static class AttrBuilder {
        private final AttributeModifiersComponent.Builder builder = AttributeModifiersComponent.builder();

        AttrBuilder damage(String identifierPath, double value) {
            builder.add(EntityAttributes.ATTACK_DAMAGE,
                new EntityAttributeModifier(Identifier.of("dungeonmod", identifierPath), value, EntityAttributeModifier.Operation.ADD_VALUE),
                AttributeModifierSlot.MAINHAND);
            return this;
        }

        AttrBuilder speed(String identifierPath, double value) {
            builder.add(EntityAttributes.ATTACK_SPEED,
                new EntityAttributeModifier(Identifier.of("dungeonmod", identifierPath), value, EntityAttributeModifier.Operation.ADD_VALUE),
                AttributeModifierSlot.MAINHAND);
            return this;
        }

        AttrBuilder range(String identifierPath, double value) {
            builder.add(EntityAttributes.ENTITY_INTERACTION_RANGE,
                new EntityAttributeModifier(Identifier.of("dungeonmod", identifierPath), value, EntityAttributeModifier.Operation.ADD_VALUE),
                AttributeModifierSlot.MAINHAND);
            return this;
        }

        AttrBuilder armor(String identifierPath, double value, AttributeModifierSlot slot) {
            builder.add(EntityAttributes.ARMOR,
                new EntityAttributeModifier(Identifier.of("dungeonmod", identifierPath), value, EntityAttributeModifier.Operation.ADD_VALUE),
                slot);
            return this;
        }

        AttrBuilder speedMult(String identifierPath, double value, AttributeModifierSlot slot) {
            builder.add(EntityAttributes.MOVEMENT_SPEED,
                new EntityAttributeModifier(Identifier.of("dungeonmod", identifierPath), value, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL),
                slot);
            return this;
        }

        AttrBuilder jumpMult(String identifierPath, double value, AttributeModifierSlot slot) {
            builder.add(EntityAttributes.JUMP_STRENGTH,
                new EntityAttributeModifier(Identifier.of("dungeonmod", identifierPath), value, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL),
                slot);
            return this;
        }

        AttrBuilder knockback(String identifierPath, double value, AttributeModifierSlot slot) {
            builder.add(EntityAttributes.KNOCKBACK_RESISTANCE,
                new EntityAttributeModifier(Identifier.of("dungeonmod", identifierPath), value, EntityAttributeModifier.Operation.ADD_VALUE),
                slot);
            return this;
        }

        AttrBuilder fallDamageMult(String identifierPath, double value, AttributeModifierSlot slot) {
            builder.add(EntityAttributes.FALL_DAMAGE_MULTIPLIER,
                new EntityAttributeModifier(Identifier.of("dungeonmod", identifierPath), value, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL),
                slot);
            return this;
        }

        AttributeModifiersComponent build() {
            return builder.build().withShowInTooltip(false);
        }

        /** build() sans withShowInTooltip(false), pour les cas (rares) où l'original ne le posait pas. */
        AttributeModifiersComponent buildVisible() {
            return builder.build();
        }
    }

    static {
        registerConsommables();
        registerSac();
        registerObjetsDivers();
        registerOeufs();
        registerArmures();
        registerArmes();
        registerBaguettes();
    }

    // ===================== Consommables =====================

    private static void registerConsommables() {
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
        register("chair_gobelin_crue", "§cChair de gobelin crue", Items.BEEF,
            stack -> {
                stack.set(DataComponentTypes.ITEM_MODEL, Identifier.of("dungeonmod", "chair_gobelin_crue"));
                stack.set(DataComponentTypes.FOOD, new net.minecraft.component.type.FoodComponent.Builder().nutrition(8).saturationModifier(0.3f).build());
            },
            "§7Une chair de gobelin crue.", "§7Restaure 2 coeurs.");
        register("chair_gobelin_cuite", "§aChair de gobelin cuite", Items.COOKED_BEEF,
            stack -> {
                stack.set(DataComponentTypes.ITEM_MODEL, Identifier.of("dungeonmod", "chair_gobelin_cuite"));
                stack.set(DataComponentTypes.FOOD, new net.minecraft.component.type.FoodComponent.Builder().nutrition(20).saturationModifier(0.6f).build());
            },
            "§7Une chair de gobelin cuite.", "§7Restaure 5 coeurs.");

        // La chope doit exister AVANT d'être référencée par biere_brune / biere_viking.
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
    }

    // ===================== Sac (enregistrement spécial, item custom) =====================

    private static void registerSac() {
        var sacId = Identifier.of("dungeonmod", "sac");
        var sacKey = net.minecraft.registry.RegistryKey.of(Registries.ITEM.getKey(), sacId);
        Item sac = Registry.register(Registries.ITEM, sacKey,
            new SacItem(new Item.Settings().registryKey(sacKey).maxCount(1)));
        putItem(new CustomItem("sac", "§6Sac", sac, null));
    }

    // ===================== Objets divers / quête =====================

    private static void registerObjetsDivers() {
        register("totem_immortalite", "§6Totem d'immortalité", Items.TOTEM_OF_UNDYING,
            stack -> stack.set(DataComponentTypes.ITEM_MODEL, Identifier.of("dungeonmod", "totem_immortalite")),
            "§7Vous protège de la mort.");
        register("syrinx_oublie", "§bSyrinx oublié", Items.STICK,
            stack -> stack.set(DataComponentTypes.ITEM_MODEL, Identifier.of("dungeonmod", "syrinx_oublie")),
            "§7Une flûte mystérieuse.", "§7Clic droit : joue des notes aléatoires.");
        register("idole_du_bonheur", "§dIdole du bonheur", Items.ECHO_SHARD,
            stack -> stack.set(DataComponentTypes.ITEM_MODEL, Identifier.of("dungeonmod", "idole_du_bonheur")),
            "§725% de chance d'annuler les dégâts ennemis.", "§7Fonctionne en main droite ou gauche.");
        register("ancre", "§6Ancre", Items.CONDUIT,
            stack -> stack.set(DataComponentTypes.ITEM_MODEL, Identifier.of("dungeonmod", "ancre")),
            "§7Une ancre mystique.", "§7Permet de redéfinir son point de réapparition.", "§7Clic droit : définit le point de spawn sur l'eau des puits.", "§7Clic gauche : grappin vers les murs/plafonds (≤15 blocs).");
        register("coeur", "§cCoeur", Items.HEART_OF_THE_SEA,
            stack -> stack.set(DataComponentTypes.ITEM_MODEL, net.minecraft.util.Identifier.of("dungeonmod", "coeur")),
            "§7Un coeur mystique.", "§7Utilisez pour gagner un coeur max supplémentaire.");
        register("montre", "§6Montre", Items.CLOCK,
            stack -> stack.set(DataComponentTypes.ITEM_MODEL, Identifier.of("dungeonmod", "montre")),
            "§7Une vieille montre arrêtée.");
        register("tablette_de_pierre", "§7Tablette de pierre", Items.PAPER,
            stack -> stack.set(DataComponentTypes.ITEM_MODEL, Identifier.of("dungeonmod", "tablette_de_pierre")),
            "§7Une tablette couverte de runes.");
        register("poussiere_de_pierre", "§7Poussière de pierre", Items.GUNPOWDER,
            stack -> stack.set(DataComponentTypes.ITEM_MODEL, Identifier.of("dungeonmod", "poussiere_de_pierre")),
            "§7Une fine poussière minérale.");
        register("sablier", "§6Sablier", Items.CLOCK,
            stack -> stack.set(DataComponentTypes.ITEM_MODEL, Identifier.of("dungeonmod", "sablier")),
            "§7Un sablier dont le sable ne coule plus.");
        register("compas_casse", "§7Compas cassé", Items.COMPASS,
            stack -> stack.set(DataComponentTypes.ITEM_MODEL, Identifier.of("dungeonmod", "compas_casse")),
            "§7Une aiguille qui tourne sans fin...");
        register("anneau_basique", "§7Anneau basique", Items.IRON_NUGGET,
            stack -> stack.set(DataComponentTypes.ITEM_MODEL, Identifier.of("dungeonmod", "anneau_basique")),
            "§7Un simple anneau de fer.");
        register("croix", "§7Croix", Items.STICK,
            stack -> stack.set(DataComponentTypes.ITEM_MODEL, Identifier.of("dungeonmod", "croix")),
            "§7Une croix en bois grossière.");
        register("fragment_de_fer", "§7Fragment de fer", Items.IRON_NUGGET,
            stack -> stack.set(DataComponentTypes.ITEM_MODEL, Identifier.of("dungeonmod", "fragment_de_fer")),
            "§7Un éclat de métal rouillé.");
        register("cle", "§eClé", Items.TRIAL_KEY,
            stack -> stack.set(DataComponentTypes.ITEM_MODEL, Identifier.of("dungeonmod", "key")),
            "§7Une clé mystérieuse.", "§7Permet d'ouvrir les portes en fer.");
        register("cle_blanche", "§fClé blanche", Items.TRIAL_KEY,
            stack -> stack.set(DataComponentTypes.ITEM_MODEL, Identifier.of("dungeonmod", "key_white")),
            "§7Une clé blanche immaculée.", "§7Ouvre la porte pale du jardin.");
        register("denier", "§7Denier", Items.GOLD_NUGGET,
            stack -> stack.set(DataComponentTypes.ITEM_MODEL, Identifier.of("dungeonmod", "denier")),
            "§7Une pièce de monnaie ancienne.");
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
        register("anneau_sang", "§cAnneau de sang", Items.STICK,
            stack -> {
                stack.set(DataComponentTypes.ITEM_MODEL, Identifier.of("dungeonmod", "anneau_sang"));
                stack.set(DataComponentTypes.ATTRIBUTE_MODIFIERS, AttributeModifiersComponent.builder().build());
            },
            "§7Un anneau ensanglante.", "§7Augmente les degats de 150%.", "§7Mais vous prenez 2x plus de degats.");
        register("dent_de_loup", "§7Dent de loup", Items.STICK,
            stack -> stack.set(DataComponentTypes.ITEM_MODEL, Identifier.of("dungeonmod", "dent_de_loup")),
            "§7Une dent de loup.", "§7Révèle la protection et la force du porteur.", "§7Les ennemis affaiblis (< 40%) sont soulignés.");
        register("conseil", "§aConseil", Items.PAPER,
            stack -> stack.set(DataComponentTypes.ITEM_MODEL, Identifier.of("dungeonmod", "conseil")),
            "§7Un précieux conseil de Gaspard.");
        register("ongle_cyclope", "§7Ongle du Cyclope", Items.BONE,
            stack -> stack.set(DataComponentTypes.ITEM_MODEL, Identifier.of("dungeonmod", "ongle_cyclope")),
            "§7Un ongle tranchant de cyclope.");
        register("caillou", "§8Caillou", Items.SNOWBALL,
            stack -> stack.set(DataComponentTypes.ITEM_MODEL, Identifier.of("dungeonmod", "caillou")),
            "§7Un petit caillou.", "§7Clic droit pour le lancer.");
        register("web", "§7Toile d'araignée", Items.STRING,
            stack -> stack.set(DataComponentTypes.ITEM_MODEL, Identifier.of("dungeonmod", "web")),
            "§7Une toile d'araignée collante.");
        register("leather", "§8Cuir", Items.LEATHER,
            stack -> stack.set(DataComponentTypes.ITEM_MODEL, Identifier.of("dungeonmod", "leather")),
            "§7Un morceau de cuir brut.");
    }

    // ===================== Œufs d'apparition =====================

    private static void registerOeufs() {
        register("oeuf_zombie", "§eOeuf de zombie", Items.STICK, "§7Un oeuf étrange.", "§7Fait apparaître un zombie custom.");
        register("oeuf_gobelin_1", "§eOeuf de gobelin 1", Items.STICK, "§7Un oeuf de gobelin.", "§7Fait apparaître un gobelin 1.");
        register("oeuf_gobelin_2", "§eOeuf de gobelin 2", Items.STICK, "§7Un oeuf de gobelin.", "§7Fait apparaître un gobelin 2.");
        register("oeuf_lanceur_gobelin", "§eOeuf de lanceur", Items.STICK, "§7Un oeuf de lanceur.", "§7Fait apparaître un gobelin lanceur de pierre.");
        register("oeuf_ogre", "§cOeuf d'ogre", Items.STICK, "§7Un oeuf d'ogre.", "§7Fait apparaître un ogre lanceur de pierres.");
    }

    // ===================== Armures =====================

    private static void registerArmures() {
        register("casque_chasseur", "§9Casque du chasseur", Items.CHAINMAIL_HELMET,
            stack -> {
                stack.set(DataComponentTypes.ITEM_MODEL, Identifier.of("dungeonmod", "casque_chasseur"));
                stack.set(DataComponentTypes.ATTRIBUTE_MODIFIERS,
                    new AttrBuilder().armor("casque_chasseur_armor", 4.0, AttributeModifierSlot.HEAD).build());
                stack.set(DataComponentTypes.EQUIPPABLE,
                    EquippableComponent.builder(EquipmentSlot.HEAD)
                        .equipSound(SoundEvents.ITEM_ARMOR_EQUIP_CHAIN)
                        .swappable(true)
                        .build());
            },
            "§7Un casque léger.", "§7Portée : voir les PV des monstres à 5 blocs.", "§7Protection: +4");
        register("plastron_chasseur", "§9Plastron du chasseur", Items.CHAINMAIL_CHESTPLATE,
            stack -> stack.set(DataComponentTypes.ATTRIBUTE_MODIFIERS,
                new AttrBuilder().armor("plastron_chasseur_armor", 7.0, AttributeModifierSlot.CHEST).build()),
            "§7Un plastron de chasseur.", "§7Ralentit la chute.", "§7Protection: +7");
        register("crane_squelette", "§9Crâne de squelette", Items.SKELETON_SKULL,
            stack -> {
                stack.set(DataComponentTypes.ATTRIBUTE_MODIFIERS,
                    new AttrBuilder().armor("crane_squelette_armor", 2.0, AttributeModifierSlot.HEAD).build());
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
                    new AttrBuilder()
                        .armor("casque_lourd_armor", 8.0, AttributeModifierSlot.HEAD)
                        .speedMult("casque_lourd_speed", -0.1, AttributeModifierSlot.HEAD)
                        .jumpMult("casque_lourd_jump", -0.1, AttributeModifierSlot.HEAD)
                        .build());
                stack.set(DataComponentTypes.ITEM_MODEL, Identifier.of("dungeonmod", "casque_lourd"));
                stack.set(DataComponentTypes.EQUIPPABLE,
                    EquippableComponent.builder(EquipmentSlot.HEAD)
                        .model(net.minecraft.registry.RegistryKey.of(net.minecraft.item.equipment.EquipmentAssetKeys.REGISTRY_KEY, Identifier.of("dungeonmod", "armure_lourde")))
                        .equipSound(SoundEvents.ITEM_ARMOR_EQUIP_IRON)
                        .swappable(true)
                        .cameraOverlay(Identifier.of("dungeonmod", "misc/helmet_overlay"))
                        .build());
            },
            "§7Un casque lourd en fer.", "§7Protège beaucoup mais réduit la vision.", "§7Protection: +8");
        register("casque_mineur", "§9Casque du mineur", Items.LEATHER_HELMET,
            stack -> stack.set(DataComponentTypes.ATTRIBUTE_MODIFIERS,
                new AttrBuilder().armor("casque_mineur_armor", 2.0, AttributeModifierSlot.HEAD).build()),
            "§7Un casque qui éclaire les environs.", "§7Protection: +2");
        register("plastron_lourd", "§9Plastron lourd", Items.IRON_CHESTPLATE,
            stack -> {
                stack.set(DataComponentTypes.ATTRIBUTE_MODIFIERS, new AttrBuilder()
                    .armor("plastron_lourd_armor", 12.0, AttributeModifierSlot.CHEST)
                    .speedMult("plastron_lourd_speed", -0.3, AttributeModifierSlot.CHEST)
                    .jumpMult("plastron_lourd_jump", -0.3, AttributeModifierSlot.CHEST).build());
                stack.set(DataComponentTypes.ITEM_MODEL, Identifier.of("dungeonmod", "plastron_lourd"));
                stack.set(DataComponentTypes.EQUIPPABLE, EquippableComponent.builder(EquipmentSlot.CHEST)
                    .model(RegistryKey.of(EquipmentAssetKeys.REGISTRY_KEY, Identifier.of("dungeonmod", "armure_lourde")))
                    .equipSound(SoundEvents.ITEM_ARMOR_EQUIP_IRON).swappable(true).build());
            },
            "§7Un plastron en fer très résistant.", "§7Ralentit le porteur mais augmente l'endurance.", "§7Protection: +12");
        register("jambiere_lourde", "§9Jambière lourde", Items.IRON_LEGGINGS,
            stack -> {
                stack.set(DataComponentTypes.ATTRIBUTE_MODIFIERS, new AttrBuilder()
                    .armor("jambiere_lourde_armor", 6.0, AttributeModifierSlot.LEGS)
                    .speedMult("jambiere_lourde_speed", -0.2, AttributeModifierSlot.LEGS)
                    .jumpMult("jambiere_lourde_jump", -0.2, AttributeModifierSlot.LEGS).build());
                stack.set(DataComponentTypes.ITEM_MODEL, Identifier.of("dungeonmod", "jambiere_lourde"));
                stack.set(DataComponentTypes.EQUIPPABLE, EquippableComponent.builder(EquipmentSlot.LEGS)
                    .model(RegistryKey.of(EquipmentAssetKeys.REGISTRY_KEY, Identifier.of("dungeonmod", "armure_lourde")))
                    .equipSound(SoundEvents.ITEM_ARMOR_EQUIP_IRON).swappable(true).build());
            },
            "§7Des jambières en fer renforcées.", "§7Protection: +6");
        register("bottes_lourdes", "§9Bottes lourdes", Items.IRON_BOOTS,
            stack -> {
                stack.set(DataComponentTypes.ATTRIBUTE_MODIFIERS, new AttrBuilder()
                    .armor("bottes_lourdes_armor", 6.0, AttributeModifierSlot.FEET)
                    .knockback("bottes_lourdes_kb", 1.0, AttributeModifierSlot.FEET).build());
                stack.set(DataComponentTypes.ITEM_MODEL, Identifier.of("dungeonmod", "bottes_lourdes"));
                stack.set(DataComponentTypes.EQUIPPABLE, EquippableComponent.builder(EquipmentSlot.FEET)
                    .model(RegistryKey.of(EquipmentAssetKeys.REGISTRY_KEY, Identifier.of("dungeonmod", "armure_lourde")))
                    .equipSound(SoundEvents.ITEM_ARMOR_EQUIP_IRON).swappable(true).build());
            },
            "§7Des bottes en fer renforcées.", "§7Annule le recul.", "§7Protection: +6");
        register("plastron_heros", "§ePlastron du héros", Items.GOLDEN_CHESTPLATE,
            stack -> stack.set(DataComponentTypes.ATTRIBUTE_MODIFIERS,
                new AttrBuilder().armor("plastron_heros_armor", 8.0, AttributeModifierSlot.CHEST).build()),
            "§7Un plastron légendaire.", "§7Reflette les dégâts reçus à l'attaquant.", "§7Protection: +8");
        register("plastron_voyageur", "§9Plastron du voyageur", Items.LEATHER_CHESTPLATE,
            stack -> stack.set(DataComponentTypes.ATTRIBUTE_MODIFIERS,
                new AttrBuilder().armor("plastron_voyageur_armor", 6.0, AttributeModifierSlot.CHEST).build()),
            "§7Un plastron de voyageur.", "§7Tuer un ennemi régénère 0.5 coeur.", "§7Protection: +6");
        register("jambiere_voyageur", "§9Jambière du voyageur", Items.LEATHER_LEGGINGS,
            stack -> stack.set(DataComponentTypes.ATTRIBUTE_MODIFIERS,
                new AttrBuilder().armor("jambiere_voyageur_armor", 5.0, AttributeModifierSlot.LEGS).build()),
            "§7Des jambières légères.", "§7Accélèrent quand la vie est basse.", "§7Protection: +5");
        register("jambiere_chasseur", "§9Jambière du chasseur", Items.CHAINMAIL_LEGGINGS,
            stack -> stack.set(DataComponentTypes.ATTRIBUTE_MODIFIERS,
                new AttrBuilder().armor("jambiere_chasseur_armor", 7.0, AttributeModifierSlot.LEGS).build()),
            "§7Des jambières de chasseur.", "§7S'accroupir permet", "§7de se cacher des monstres.", "§7Protection: +7");
        register("bottes_sept_lieues", "§9Bottes de sept lieues", Items.LEATHER_BOOTS,
            stack -> stack.set(DataComponentTypes.ATTRIBUTE_MODIFIERS,
                new AttrBuilder()
                    .armor("bottes_armor", 3.0, AttributeModifierSlot.FEET)
                    .speedMult("bottes_speed", 1.0, AttributeModifierSlot.FEET)
                    .build()),
            "§7Des bottes légendaires.", "§7Permet de courir très vite.", "§7Protection: +3");
        register("bottes_apollon", "§6Bottes d'Apollon", Items.GOLDEN_BOOTS,
            stack -> stack.set(DataComponentTypes.ATTRIBUTE_MODIFIERS,
                new AttrBuilder()
                    .armor("bottes_apollon_armor", 1.0, AttributeModifierSlot.FEET)
                    .fallDamageMult("bottes_apollon_fall", -1.0, AttributeModifierSlot.FEET)
                    .build()),
            "§7Des bottes légères.", "§7Permettent d'effectuer un double saut.", "§7Annule les dégâts de chute.", "§7Protection: +1");
    }

    // ===================== Armes (corps-à-corps / distance) =====================

    private static void registerArmes() {
        register("tetralame_mort_subite", "§4Tétralame mort subite", Items.NETHERITE_SWORD,
            stack -> {
                stack.set(DataComponentTypes.ITEM_MODEL, Identifier.of("dungeonmod", "tetralame_mort_subite"));
                stack.set(DataComponentTypes.ATTRIBUTE_MODIFIERS,
                    new AttrBuilder()
                        .damage("tetralame_damage", 39.0)
                        .range("tetralame_range", 0.5)
                        .build());
            },
            "§4Attention: 0,5 coeur max tant qu'elle est en main.", "§7Inflige 20 coeurs de dégâts.", "§7Portée de 3,5 blocs.");
        register("baton", "§9Bâton", Items.STICK,
            stack -> stack.set(DataComponentTypes.ATTRIBUTE_MODIFIERS,
                new AttrBuilder()
                    .damage("baton_attack", 1.0)
                    .speed("baton_speed", -1.5)
                    .range("baton_range", -0.2)
                    .build()),
            "§7Un bâton ordinaire.", "§7Clic droit pour le lancer.");
        register("dague", "§9Dague", Items.FLINT,
            stack -> {
                stack.set(DataComponentTypes.ITEM_MODEL, Identifier.of("dungeonmod", "dague"));
                stack.set(DataComponentTypes.ATTRIBUTE_MODIFIERS,
                    new AttrBuilder()
                        .damage("dague_attack", 2.0)
                        .speed("dague_speed", 0.0)
                        .range("dague_range", -0.5)
                        .build());
            },
            "§7Une dague légère.", "§7Inflige 1.5 coeurs (3 coeurs par derrière).");
        register("os", "§aOs", Items.BONE,
            stack -> stack.set(DataComponentTypes.ATTRIBUTE_MODIFIERS,
                new AttrBuilder()
                    .damage("os_attack", 4.0)
                    .speed("os_speed", -2.75)
                    .build()),
            "§7n'attend qu'à être fracassé sur un ennemi", "§7(clic droit pour fracasser)");
        register("hache_fer", "§9Hache en fer", Items.IRON_AXE,
            stack -> {
                stack.set(DataComponentTypes.ITEM_MODEL, Identifier.of("dungeonmod", "hache_fer"));
                stack.set(DataComponentTypes.ATTRIBUTE_MODIFIERS,
                    new AttrBuilder().speed("hache_fer_speed", -2.0).build());
            },
            "§7Une hache en fer.", "§7Les dégâts augmentent si vous frappez", "§7le même ennemi (1→1.5→2.5→5.5 coeurs)");
        register("faux_fer", "§9Faux de fer", Items.IRON_HOE,
            stack -> {
                stack.set(DataComponentTypes.ITEM_MODEL, Identifier.of("dungeonmod", "faux_fer"));
                stack.set(DataComponentTypes.ATTRIBUTE_MODIFIERS,
                    new AttrBuilder()
                        .speed("faux_fer_speed", -2.333)
                        .range("faux_fer_range", 0.5)
                        .build());
            },
            "§7Une faux en fer.", "§7Inflige 1.5 coeurs de zone.", "§7Portée: 3.5 blocs.");
        register("epee", "§9Epée", Items.STONE_SWORD,
            stack -> {
                stack.set(DataComponentTypes.ITEM_MODEL, Identifier.of("dungeonmod", "epee"));
                stack.set(DataComponentTypes.ATTRIBUTE_MODIFIERS,
                    new AttrBuilder()
                        .damage("epee_attack", 3.0)
                        .speed("epee_speed", -2.0)
                        .range("epee_range", 0.2)
                        .build());
                stack.set(DataComponentTypes.FOOD, new net.minecraft.component.type.FoodComponent.Builder().nutrition(0).saturationModifier(0).alwaysEdible().build());
            },
            "§7Une épée en pierre.", "§7Inflige 2 coeurs.", "§7Clic droit pour bloquer.");
        register("lance", "§9Lance", Items.TRIDENT,
            stack -> {
                stack.set(DataComponentTypes.ITEM_MODEL, Identifier.of("dungeonmod", "lance"));
                stack.set(DataComponentTypes.ATTRIBUTE_MODIFIERS,
                    new AttrBuilder()
                        .damage("lance_attack", 2.0)
                        .speed("lance_speed", -2.182)
                        .range("lance_reach", 1.2)
                        .build());
            },
            "§7Une lance en bois.", "§7Inflige 1.5 coeurs.", "§7Portée: 4.2 blocs.", "§7Clic droit pour lancer.");
        register("fleche", "§9Flèche", Items.ARROW,
            stack -> {
                stack.set(DataComponentTypes.MAX_STACK_SIZE, 1);
                stack.set(DataComponentTypes.ATTRIBUTE_MODIFIERS,
                    new AttrBuilder()
                        .speed("fleche_speed", -1.143)
                        .range("fleche_range", -0.2)
                        .build());
            },
            "§7Une flèche simple.", "§7Utilisable comme poignard ou projectile.");
        register("arc_heros", "§eArc du héros", Items.BOW,
            stack -> {
                stack.set(DataComponentTypes.ITEM_MODEL, Identifier.of("dungeonmod", "arc_heros"));
                stack.set(DataComponentTypes.ATTRIBUTE_MODIFIERS,
                    new AttrBuilder().damage("arc_heros_damage", 0.0).build());
            },
            "§7Un arc légendaire.", "§7Base: 2 coeurs, Sang I: 3.33 coeurs, Sang II: 4.66 coeurs.", "§7Seulement compatible avec les flèches custom.");
        register("fouet", "§9Fouet", Items.STICK,
            stack -> {
                stack.set(DataComponentTypes.ITEM_MODEL, Identifier.of("dungeonmod", "fouet"));
                stack.set(DataComponentTypes.ATTRIBUTE_MODIFIERS,
                    new AttrBuilder()
                        .speed("fouet_speed", -2.75)
                        .range("fouet_range", 4.0)
                        .build());
            },
            "§7Un fouet en cuir.", "§70.5 cœur en mêlée.", "§7Clic droit: charge + ralentit.");
        register("torche", "§9Torche", Items.STICK,
            stack -> {
                stack.set(DataComponentTypes.ITEM_MODEL, Identifier.of("dungeonmod", "torche"));
                stack.set(DataComponentTypes.ATTRIBUTE_MODIFIERS,
                    new AttrBuilder().speed("torche_speed", -2.182).build());
            },
            "§7Une torche qui éclaire.", "§7Enflamme les ennemis (0.5 cœur/s 3s).", "§7Clic droit pour lancer.");
        register("boomerang", "§aBoomerang", Items.STICK,
            stack -> {
                stack.set(DataComponentTypes.ITEM_MODEL, Identifier.of("dungeonmod", "boomerang"));
                stack.set(DataComponentTypes.ATTRIBUTE_MODIFIERS,
                    new AttrBuilder()
                        .damage("boomerang_attack", 1.0)
                        .speed("boomerang_speed", -1.143)
                        .range("boomerang_range", -0.2)
                        .build());
            },
            "§7Un boomerang en bois.", "§7Clic gauche pour lancer (1 cœur).", "§7Revient après 1.5s.");
        register("sabre", "§9Sabre", Items.IRON_SWORD,
            stack -> {
                stack.set(DataComponentTypes.ITEM_MODEL, Identifier.of("dungeonmod", "sabre"));
                stack.set(DataComponentTypes.ATTRIBUTE_MODIFIERS,
                    new AttrBuilder()
                        .damage("sabre_attack", 3.0)
                        .speed("sabre_speed", -1.14)
                        .range("sabre_range", 0.2)
                        .build());
            },
            "§7Un sabre léger.", "§7Attaquer dans le vide charge", "§7le combo (max 3).", "§7Clic droit : décharge le combo.", "§7Dégâts: 2 coeurs.");
        register("glaive", "§9Glaive", Items.STICK,
            stack -> {
                stack.set(DataComponentTypes.ITEM_MODEL, Identifier.of("dungeonmod", "glaive"));
                stack.set(DataComponentTypes.ATTRIBUTE_MODIFIERS,
                    new AttrBuilder()
                        .damage("glaive_attack", 5.0)
                        .speed("glaive_speed", -3.0)
                        .build());
            },
            "§7Un glaive tranchant.", "§7Enchaînez les coups pour accelerer.", "§7Clic droit : attaque tournoyante.");
    }

    // ===================== Baguettes magiques =====================

    private static void registerBaguettes() {
        // Modificateurs partagés entre les 3 baguettes (comportement identique à l'original).
        var baguetteModifiers = new AttrBuilder()
            .speed("baguette_speed", -3.0)
            .damage("baguette_damage", 0.0)
            .build();

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
    }

    // ===================== Enregistrement générique =====================

    private static void register(String id, String displayName, Item vanillaItem, String... lore) {
        putItem(new CustomItem(id, displayName, vanillaItem, null, lore));
    }

    private static void register(String id, String displayName, Item vanillaItem, Consumer<ItemStack> modifier, String... lore) {
        putItem(new CustomItem(id, displayName, vanillaItem, modifier, lore));
    }

    /** Centralise l'insertion + détecte les doublons d'id (au lieu de les écraser silencieusement). */
    private static void putItem(CustomItem item) {
        if (ITEMS.containsKey(item.id)) {
            System.out.println("[ModItems] ATTENTION: id dupliqué '" + item.id + "' — l'entrée précédente est écrasée.");
        }
        ITEMS.put(item.id, item);
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
            // Sac item
            entries.add(Registries.ITEM.get(Identifier.of("dungeonmod", "sac")));
        });
    }
}