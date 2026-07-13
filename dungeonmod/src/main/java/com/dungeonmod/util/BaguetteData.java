package com.dungeonmod.util;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import java.util.*;
import net.minecraft.component.type.LoreComponent;

public class BaguetteData {
    private static final Map<UUID, Long> cooldowns = new HashMap<>();
    public static final Set<UUID> fireTagged = new HashSet<>();
    public static final Map<UUID, Long> iceDot = new HashMap<>();
    public static final Map<UUID, Long> iceDotLastDamage = new HashMap<>();
    public static final Map<UUID, Long> darkDot = new HashMap<>();
    public static final Map<UUID, Long> darkDotLastDamage = new HashMap<>();
    public static final Map<UUID, java.util.AbstractMap.SimpleEntry<UUID, Long>> darkLink = new HashMap<>(); // joueur → (ennemi, expireAt)

    public static boolean tryWandAttack(PlayerEntity player, Entity target) {
        if (player.getWorld().isClient()) return false;
        ItemStack stack = player.getMainHandStack();
        if (!isBaguette(stack)) return false;

        UUID uid = player.getUuid();
        long now = System.currentTimeMillis();
        Long last = cooldowns.get(uid);
        if (last != null && now - last < 1000) return true; // cooldown 1s

        String type = getWandType(stack);
        if (!(player.getWorld() instanceof ServerWorld sw)) return false;

        var snowball = new SnowballEntity(sw, player, createProjectileStack(type));
        snowball.setVelocity(player, player.getPitch(), player.getYaw(), 0.0f, 2.0f, 0.0f);
        if (type.equals("feu")) {
            snowball.setOnFireFor(100); // visuel de feu, n'enflamme pas les blocs
        }
        sw.spawnEntity(snowball);
        cooldowns.put(uid, now);
        return true;
    }

    public static void resetCooldown(PlayerEntity player) {
        cooldowns.put(player.getUuid(), System.currentTimeMillis());
    }

    public static boolean isOnCooldown(PlayerEntity player) {
        Long last = cooldowns.get(player.getUuid());
        return last != null && System.currentTimeMillis() - last < 1000;
    }

    public static boolean isBaguette(ItemStack stack) {
        if (stack.isEmpty() || !stack.isOf(Items.STICK)) return false;
        if (!stack.contains(DataComponentTypes.CUSTOM_NAME)) return false;
        String name = stack.get(DataComponentTypes.CUSTOM_NAME).getString();
        return name.contains("Baguette");
    }

    public static String getWandType(ItemStack stack) {
        if (!stack.contains(DataComponentTypes.CUSTOM_NAME)) return "feu";
        String name = stack.get(DataComponentTypes.CUSTOM_NAME).getString();
        if (name.contains("glace")) return "glace";
        if (name.contains("sombre")) return "sombre";
        return "feu";
    }

    public static void setWandType(ItemStack stack, String type) {
        String name = type.equals("glace") ? "§bBaguette de glace" : type.equals("sombre") ? "§5Baguette sombre" : "§cBaguette de feu";
        String desc = type.equals("glace") ? "Une baguette gelee." : type.equals("sombre") ? "Une baguette des tenebres." : "Une baguette enflammee.";
        stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(name));
        java.util.List<Text> lore = new java.util.ArrayList<>();
        lore.add(Text.literal("§7" + desc));
        if (type.equals("feu")) {
            lore.add(Text.literal("§7Clic gauche : tire une boule de feu (0,5 coeur + brulee 2,5s)."));
        } else if (type.equals("glace")) {
            lore.add(Text.literal("§7Clic gauche : glace l'ennemi (1 coeur + ralentissement)."));
            lore.add(Text.literal("§70,5 coeur/s pendant 5s."));
        } else {
            lore.add(Text.literal("§7Clic gauche : drain de vie (2 coeurs + 0,5 coeur/s 5s)."));
            lore.add(Text.literal("§7Lien : les degats sur la cible soignent."));
        }
        lore.add(Text.literal("§7Clic droit : change de forme."));
        stack.set(DataComponentTypes.LORE, new LoreComponent(lore));
    }

    public static void setWandModel(ItemStack stack, String type) {
        String model = type.equals("glace") ? "baguette_glace" : type.equals("sombre") ? "baguette_sombre" : "baguette_feu";
        stack.set(DataComponentTypes.ITEM_MODEL, Identifier.of("dungeonmod", model));
    }

    public static boolean hasRune(ItemStack stack, String type) {
        var comp = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (comp == null) return false;
        String runes = comp.getNbt().getString("runes");
        return runes.contains(type);
    }

    public static void appliquerRune(ItemStack wand, String runeType, PlayerEntity player) {
        var comp = wand.get(DataComponentTypes.CUSTOM_DATA);
        net.minecraft.nbt.NbtCompound nbt = comp != null ? comp.getNbt().copy() : new net.minecraft.nbt.NbtCompound();
        String runes = nbt.getString("runes");
        if (!runes.contains(runeType)) {
            runes = runes.isEmpty() ? runeType : runes + "," + runeType;
            nbt.putString("runes", runes);
            wand.set(DataComponentTypes.CUSTOM_DATA, net.minecraft.component.type.NbtComponent.of(nbt));
        }
        // Transformation immédiate dans la forme correspondant à la rune
        String targetType = runeType.equals("dark") ? "sombre" : runeType.equals("ice") ? "glace" : "feu";
        setWandType(wand, targetType);
        setWandModel(wand, targetType);
        player.sendMessage(Text.literal("§aRune appliquée !"), true);
    }

    public static String getRuneType(ItemStack stack) {
        if (stack.isEmpty() || !stack.contains(DataComponentTypes.CUSTOM_NAME)) return null;
        String name = stack.get(DataComponentTypes.CUSTOM_NAME).getString();
        if (name.contains("Rune sombre")) return "dark";
        if (name.contains("Rune de glace")) return "ice";
        return null;
    }

    // Appelé depuis le SERVEUR (multiplayer) pour tirer sur entité
    public static void tryWandAttackServer(PlayerEntity player) {
        long now = System.currentTimeMillis();
        UUID uid = player.getUuid();
        Long last = cooldowns.get(uid);
        if (last != null && now - last < 1000) return;

        ItemStack stack = player.getMainHandStack();
        if (!isBaguette(stack)) return;
        if (!(player.getWorld() instanceof ServerWorld sw)) return;

        String type = getWandType(stack);
        var snowball = new SnowballEntity(sw, player, createProjectileStack(type));
        snowball.setVelocity(player, player.getPitch(), player.getYaw(), 0.0f, 2.0f, 0.0f);
        if (type.equals("feu")) {
            snowball.setOnFireFor(100.0F);
            snowball.setFireTicks(2000);
        }
        sw.spawnEntity(snowball);
        cooldowns.put(uid, now);
    }

    // Appelé depuis le CLIENT (doAttack) pour tirer même dans le vide
    public static void tryWandAttackClient(PlayerEntity player) {
        long now = System.currentTimeMillis();
        UUID uid = player.getUuid();
        Long last = cooldowns.get(uid);
        if (last != null && now - last < 1000) return; // cooldown 1s
        cooldowns.put(uid, now);

        ItemStack stack = player.getMainHandStack();
        if (!isBaguette(stack)) return;
        String type = getWandType(stack);

        // En solo, le serveur intégré est accessible
        var mc = net.minecraft.client.MinecraftClient.getInstance();
        if (mc.getServer() == null) return;
        var sw = mc.getServer().getWorld(player.getWorld().getRegistryKey());
        if (sw == null) return;
        // Utiliser le ServerPlayer (pas le ClientPlayer) pour éviter les erreurs de packet
        var serverPlayer = sw.getPlayerByUuid(player.getUuid());
        if (serverPlayer == null) return;
        var snowball = new SnowballEntity(sw, serverPlayer, createProjectileStack(type));
        snowball.setVelocity(serverPlayer, serverPlayer.getPitch(), serverPlayer.getYaw(), 0.0f, 2.0f, 0.0f);
        if (type.equals("feu")) {
            snowball.setOnFireFor(100.0F);
            snowball.setFireTicks(2000);
        }
        sw.spawnEntity(snowball);
    }

    private static ItemStack createProjectileStack(String type) {
        ItemStack proj;
        if (type.equals("feu")) proj = new ItemStack(Items.FIRE_CHARGE);
        else if (type.equals("sombre")) {
            proj = new ItemStack(Items.ENDER_PEARL);
            proj.set(DataComponentTypes.ITEM_MODEL, Identifier.of("dungeonmod", "black_ender_pearl"));
        }
        else proj = new ItemStack(Items.SNOWBALL);
        String name = type.equals("glace") ? "§bBoule de glace" : type.equals("sombre") ? "§5Boule sombre" : "§cBoule de feu";
        proj.set(DataComponentTypes.CUSTOM_NAME, Text.literal(name));
        return proj;
    }
}
