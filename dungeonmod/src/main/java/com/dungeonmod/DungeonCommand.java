package com.dungeonmod;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import com.dungeonmod.test.TestGenerator;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameRules;
import net.minecraft.world.TeleportTarget;

import java.util.*;

public class DungeonCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        // /teste
        dispatcher.register(CommandManager.literal("teste")
            .then(CommandManager.argument("salles", IntegerArgumentType.integer(1, 40))
                .executes(ctx -> testGenerated(ctx.getSource(),
                    IntegerArgumentType.getInteger(ctx, "salles"))))
            .executes(ctx -> testGenerated(ctx.getSource(), 40))
        );

        // /teste tp <salle>
        dispatcher.register(CommandManager.literal("teste")
            .then(CommandManager.literal("tp")
                .then(CommandManager.argument("salle", StringArgumentType.word())
                    .suggests((ctx, builder) -> {
                        for (String s : TestGenerator.getRoomTypeNames()) builder.suggest(s);
                        return builder.buildFuture();
                    })
                    .executes(ctx -> testTeleport(ctx.getSource(),
                        StringArgumentType.getString(ctx, "salle")))))
        );

        // /testeseed <seed>
        dispatcher.register(CommandManager.literal("testeseed")
            .then(CommandManager.argument("seed", IntegerArgumentType.integer())
                .executes(ctx -> testSeed(ctx.getSource(),
                    (long) IntegerArgumentType.getInteger(ctx, "seed"))))
        );

        // /teste item <item>
        dispatcher.register(CommandManager.literal("teste")
            .then(CommandManager.literal("item")
                .then(CommandManager.argument("item", StringArgumentType.word())
                    .suggests((ctx, builder) -> {
                        for (String id : ModItems.getIds()) builder.suggest(id);
                        return builder.buildFuture();
                    })
                    .executes(ctx -> giveItem(ctx.getSource(),
                        StringArgumentType.getString(ctx, "item")))))
        );

        // /teste stats
        dispatcher.register(CommandManager.literal("teste")
            .then(CommandManager.literal("stats")
                .executes(ctx -> showStats(ctx.getSource())))
        );

        // /teste test (regénère au même endroit)
        dispatcher.register(CommandManager.literal("teste")
            .then(CommandManager.literal("test")
                .executes(ctx -> testRegenerate(ctx.getSource())))
        );

        // /teste feed
        dispatcher.register(CommandManager.literal("teste")
            .then(CommandManager.literal("feed")
                .executes(ctx -> showFeed(ctx.getSource())))
        );

        // /testehealth
        dispatcher.register(CommandManager.literal("testehealth")
            .executes(ctx -> testeHealth(ctx.getSource()))
        );

        // /testebeer
        dispatcher.register(CommandManager.literal("testebeer")
            .executes(ctx -> testBeer(ctx.getSource()))
        );

        // /testehub
        dispatcher.register(CommandManager.literal("testehub")
            .executes(ctx -> testHub(ctx.getSource()))
        );

        // /lobby url, /lobby check
        dispatcher.register(CommandManager.literal("lobby")
            .then(CommandManager.literal("url")
                .then(CommandManager.argument("url", StringArgumentType.string())
                    .executes(ctx -> lobbyUrl(ctx.getSource(), StringArgumentType.getString(ctx, "url"))))
            )
            .then(CommandManager.literal("check")
                .executes(ctx -> lobbyCheck(ctx.getSource()))
            )
        );
    }

    // ===================== /teste =====================

    private static int testIndex = 0;
    private static int testBaseX, testBaseZ;
    private static boolean testBaseSet = false;

    private static int testGenerated(ServerCommandSource source, int maxRooms) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("§cRéservé aux joueurs."));
            return 0;
        }
        ServerWorld world = source.getWorld();

        if (!testBaseSet) {
            testBaseX = player.getBlockX();
            testBaseZ = player.getBlockZ();
            testBaseSet = true;
            testIndex = 0;
        }

        int genX = testBaseX + (testIndex + 1) * 2000;
        int genZ = testBaseZ;
        int genY = 100;
        testIndex++;

        BlockPos genPos = new BlockPos(genX, genY, genZ);
        try {
            TestGenerator.generateRandomCave(world, genPos, maxRooms);

            // Nettoyer les sacs des joueurs dans le donjon
            for (ServerPlayerEntity p : source.getServer().getPlayerManager().getPlayerList()) {
                boolean inDungeon = false;
                for (var d : com.dungeonmod.DungeonMod.dungeons) {
                    if (!d.worldKey.equals(p.getWorld().getRegistryKey().getValue())) continue;
                    int halfGrid = 320;
                    int px = p.getBlockX(), pz = p.getBlockZ();
                    if (px >= d.origin.getX() - halfGrid && px < d.origin.getX() + halfGrid
                        && pz >= d.origin.getZ() - halfGrid && pz < d.origin.getZ() + halfGrid) {
                        inDungeon = true; break;
                    }
                }
                if (!inDungeon) continue;
                var inv = p.getInventory();
                for (int i = 0; i < inv.main.size(); i++) {
                    if (inv.main.get(i).getItem() instanceof com.dungeonmod.item.SacItem) {
                        inv.main.set(i, net.minecraft.item.ItemStack.EMPTY);
                    }
                }
            }
            // Apply dungeon gamerules
            GameRules rules = world.getGameRules();
            rules.get(GameRules.DO_MOB_SPAWNING).set(false, null);
            rules.get(GameRules.DO_MOB_LOOT).set(false, null);
            rules.get(GameRules.DO_TILE_DROPS).set(false, null);
            rules.get(GameRules.DO_ENTITY_DROPS).set(false, null);
            rules.get(GameRules.DO_DAYLIGHT_CYCLE).set(false, null);
            rules.get(GameRules.DO_WEATHER_CYCLE).set(false, null);
            rules.get(GameRules.DO_INSOMNIA).set(false, null);
            rules.get(GameRules.DO_MOB_GRIEFING).set(false, null);
            rules.get(GameRules.DO_FIRE_TICK).set(false, null);
            rules.get(GameRules.DO_VINES_SPREAD).set(false, null);
            rules.get(GameRules.DROWNING_DAMAGE).set(false, null);
            rules.get(GameRules.FALL_DAMAGE).set(true, null);
            rules.get(GameRules.FIRE_DAMAGE).set(true, null);
            rules.get(GameRules.FREEZE_DAMAGE).set(false, null);
            world.setTimeOfDay(18000);
            world.setWeather(0, 0, false, false);
            rules.get(GameRules.KEEP_INVENTORY).set(false, null);
            rules.get(GameRules.NATURAL_REGENERATION).set(false, null);
            rules.get(GameRules.DISABLE_RAIDS).set(true, null);
            rules.get(GameRules.DO_PATROL_SPAWNING).set(false, null);
            rules.get(GameRules.DO_TRADER_SPAWNING).set(false, null);
            rules.get(GameRules.DO_WARDEN_SPAWNING).set(false, null);
            player.getAttributeInstance(EntityAttributes.MAX_HEALTH).setBaseValue(10.0);
            player.setHealth(10.0f);
            long usedSeed = TestGenerator.getLastSeed();
            int departX = TestGenerator.getLastDepartX();
            int departZ = TestGenerator.getLastDepartZ();
            player.teleportTo(new TeleportTarget(world,
                new Vec3d(departX + 0.5, genY + 4, departZ + 0.5),
                Vec3d.ZERO, 0.0f, 0.0f, TeleportTarget.NO_OP));
            source.sendFeedback(() -> Text.literal("§7Réseau n°" + testIndex + " généré (§e" + maxRooms + " salles§7) — Seed: §e" + usedSeed), true);
        } catch (Exception e) {
            source.sendError(Text.literal("§cErreur: " + e.getMessage()));
            return 0;
        }
        return 1;
    }

    // ===================== /testeseed =====================

    private static int testSeed(ServerCommandSource source, long seed) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("§cRéservé aux joueurs."));
            return 0;
        }
        ServerWorld world = source.getWorld();
        BlockPos rawPos = BlockPos.ofFloored(source.getPosition());
        int cx = TestGenerator.getStructSizeX();
        int cz = TestGenerator.getStructSizeZ();
        BlockPos pos = rawPos.add(-cx / 2, 0, -cz / 2);

        try {
            TestGenerator.generateRandomCave(world, pos, 40, seed);
            long usedSeed = TestGenerator.getLastSeed();
            source.sendFeedback(() -> Text.literal("§eSeed " + usedSeed + " §7— Généré !"), true);
        } catch (Exception e) {
            source.sendError(Text.literal("§cErreur: " + e.getMessage()));
            return 0;
        }
        return 1;
    }

    // ===================== /teste tp =====================

    private static int testTeleport(ServerCommandSource source, String roomType) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("§cRéservé aux joueurs."));
            return 0;
        }
        ServerWorld world = source.getWorld();

        // Construire le typeMap dynamiquement depuis LABEL_TO_TYPE
        Map<String, Integer> typeMap = new HashMap<>(TestGenerator.getLabelToType());
        // Ajouter les types speciaux qui ne sont pas dans LABEL_TO_TYPE
        typeMap.put("Centrale", TestGenerator.CENTRALE);

        Integer targetType = typeMap.get(roomType);
        if (targetType == null) {
            // Chercher par nom partiel
            for (var e : typeMap.entrySet()) {
                if (e.getKey().toLowerCase().contains(roomType.toLowerCase())) { targetType = e.getValue(); break; }
            }
            if (targetType == null) {
                source.sendError(Text.literal("§cType inconnu: " + roomType + "."));
                return 0;
            }
        }

        // Rassembler toutes les salles de ce type
        List<TestGenerator.SpecialRoomEntry> candidates = new ArrayList<>();
        for (TestGenerator.SpecialRoomEntry sr : TestGenerator.lastSpecialRooms) {
            if (sr.type == targetType) candidates.add(sr);
        }
        if (!candidates.isEmpty()) {
            TestGenerator.SpecialRoomEntry chosen = candidates.get(new Random().nextInt(candidates.size()));
            int baseY = TestGenerator.getLastOriginY();
            // Determiner le Y: Centrale=+24, P4=+10, autres=+4
            boolean isCentrale = targetType == TestGenerator.CENTRALE;
            String tn = null;
            for (var e : typeMap.entrySet()) if (e.getValue() == targetType) { tn = e.getKey(); break; }
            boolean isP4 = tn != null && (targetType > 50 || tn.equals("Chapelle1") || tn.equals("Chapelle2")
                || tn.equals("Crypte1") || tn.equals("Crypte2")
                || tn.equals("PrisonC1") || tn.equals("PrisonC2") || tn.equals("PrisonC3") || tn.equals("PrisonC4")
                || tn.startsWith("PorteGob") || tn.startsWith("PuitG") || tn.startsWith("MarchG") || tn.startsWith("ArmG") || tn.startsWith("TresorG")
                || tn.startsWith("MG") || tn.startsWith("CDG") || tn.startsWith("CG1") || tn.startsWith("GI"));
            int finalTy = isCentrale ? baseY + 15 : isP4 ? baseY + 14 : baseY + 4;
            player.teleportTo(new TeleportTarget(world,
                new Vec3d(chosen.worldX + 5, finalTy, chosen.worldZ + 5),
                Vec3d.ZERO, 0.0f, 0.0f, net.minecraft.world.TeleportTarget.NO_OP));
            source.sendFeedback(() -> Text.literal("§7TP vers §e" + roomType + " §7(×" + candidates.size() + ") à (" + chosen.worldX + ", " + finalTy + ", " + chosen.worldZ + ")"), true);
            return 1;
        }

        source.sendError(Text.literal("§cSalle " + roomType + " non trouvée dans ce donjon."));
        return 0;
    }

    private static int testeHealth(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("§cRéservé aux joueurs."));
            return 0;
        }
        player.setHealth(1.0f);
        source.sendFeedback(() -> Text.literal("§c§lVie mise à 0,5 coeur !"), true);
        return 1;
    }

    // ===================== /teste item =====================

    private static int giveItem(ServerCommandSource source, String itemId) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("§cRéservé aux joueurs."));
            return 0;
        }

        ModItems.CustomItem item = ModItems.get(itemId);
        if (item == null) {
            source.sendError(Text.literal("§cItem inconnu: " + itemId + ". Items: " + String.join(", ", ModItems.getIds())));
            return 0;
        }

        player.giveItemStack(item.createStack());
        source.sendFeedback(() -> Text.literal("§a" + item.displayName + " §7donné !"), true);
        return 1;
    }

    // ===================== /teste stats =====================

    private static int showStats(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("§cRéservé aux joueurs."));
            return 0;
        }

        var stack = player.getMainHandStack();
        if (stack.isEmpty()) {
            source.sendError(Text.literal("§cTu n'as rien en main."));
            return 0;
        }

        double totalDamage = 1.0;
        double totalSpeed = 4.0;
        double totalRange = 3.0;

        var attrs = stack.get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
        if (attrs != null) {
            for (var entry : attrs.modifiers()) {
                if (entry.slot() != AttributeModifierSlot.MAINHAND) continue;
                var attrEntry = entry.attribute();
                var mod = entry.modifier();
                double val = mod.value();

                if (mod.operation() != EntityAttributeModifier.Operation.ADD_VALUE) continue;

                if (attrEntry.matches(EntityAttributes.ATTACK_DAMAGE)) {
                    totalDamage += val;
                } else if (attrEntry.matches(EntityAttributes.ATTACK_SPEED)) {
                    totalSpeed += val;
                } else if (attrEntry.matches(EntityAttributes.ENTITY_INTERACTION_RANGE)) {
                    totalRange += val;
                }
            }
        }

        int kbLevel = 0;
        var ench = stack.get(DataComponentTypes.ENCHANTMENTS);
        if (ench != null) {
            for (var entry : ench.getEnchantments()) {
                Optional<Identifier> optId = entry.getKey().map(k -> k.getValue());
                if (optId.isPresent() && optId.get().getPath().equals("knockback")) {
                    kbLevel = ench.getLevel(entry);
                    break;
                }
            }
        }

        double cooldownSec = totalSpeed > 0 ? 1.0 / totalSpeed : 0;
        double hearts = totalDamage / 2.0;

        String nameStr = stack.getName().getString();
        String dmgStr = String.format("§7Dégâts: §e%.1f §8(%.1f coeurs)", totalDamage, hearts);
        String spdStr = String.format("§7Vitesse: §e%.2f §8attaques/s §7(cooldown: §e%.3fs§7)", totalSpeed, cooldownSec);
        String rngStr = String.format("§7Portée: §e%.1f §8blocs", totalRange);
        String kbStr = kbLevel > 0 ? "§7Knockback enchant: §eNiveau " + kbLevel : "§7Knockback enchant: §7Aucun";

        source.sendFeedback(() -> Text.literal("§6§lStats de " + nameStr), false);
        source.sendFeedback(() -> Text.literal(dmgStr), false);
        source.sendFeedback(() -> Text.literal(spdStr), false);
        source.sendFeedback(() -> Text.literal(rngStr), false);
        source.sendFeedback(() -> Text.literal(kbStr), false);
        return 1;
    }

    // ===================== /teste test =====================

    private static int testRegenerate(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) { source.sendError(Text.literal("§cRéservé aux joueurs.")); return 0; }
        ServerWorld world = source.getWorld();

        int ox = com.dungeonmod.test.TestGenerator.getLastOriginX();
        int oy = com.dungeonmod.test.TestGenerator.getLastOriginY();
        int oz = com.dungeonmod.test.TestGenerator.getLastOriginZ();
        if (ox == 0 && oz == 0) {
            source.sendError(Text.literal("§cAucun donjon précédent. Fait d'abord /teste."));
            return 0;
        }

        // Nettoyer les entites (items, mobs, etc.) dans la zone du donjon
        net.minecraft.util.math.Box box = new net.minecraft.util.math.Box(
            ox - 30, oy - 10, oz - 30,
            ox + 300, oy + 40, oz + 300);
        for (var e : world.getOtherEntities(null, box)) {
            e.discard();
        }

        BlockPos genPos = new BlockPos(ox, oy, oz);
        try {
            TestGenerator.generateRandomCave(world, genPos, 40);
            player.getAttributeInstance(EntityAttributes.MAX_HEALTH).setBaseValue(10.0);
            player.setHealth(10.0f);
            long usedSeed = TestGenerator.getLastSeed();
            int departX = TestGenerator.getLastDepartX();
            int departZ = TestGenerator.getLastDepartZ();
            player.teleportTo(new TeleportTarget(world,
                new Vec3d(departX + 0.5, oy + 4, departZ + 0.5),
                Vec3d.ZERO, 0.0f, 0.0f, TeleportTarget.NO_OP));
            source.sendFeedback(() -> Text.literal("§7Régénéré au même endroit — Seed: §e" + usedSeed), true);
        } catch (Exception e) {
            source.sendError(Text.literal("§cErreur: " + e.getMessage()));
            return 0;
        }
        return 1;
    }

    // ===================== /teste feed =====================

    private static int showFeed(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) return 0;
        int food = player.getHungerManager().getFoodLevel();
        source.sendFeedback(() -> Text.literal("§7Nourriture: §e" + food + "/20"), true);
        return 1;
    }

    // ===================== /lobby =====================

    private static int lobbyUrl(ServerCommandSource source, String url) {
        LobbyClient.setBaseUrl(url);
        source.sendFeedback(() -> Text.literal("§aURL du lobby changée: §f" + LobbyClient.getBaseUrl()), true);
        return 1;
    }

    private static int testBeer(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) return 0;
        float mult = com.dungeonmod.util.BeerStrengthData.getMultiplier(player);
        if (mult != 1.0f) {
            source.sendFeedback(() -> Text.literal("§aBoost actif: §e" + (int)((mult - 1) * 100) + "%"), true);
        } else {
            source.sendFeedback(() -> Text.literal("§7Aucun boost actif."), true);
        }
        return 1;
    }

    // ===================== /testehub =====================

    private static int testHub(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) { source.sendError(Text.literal("§cRéservé aux joueurs.")); return 0; }
        ServerWorld world = source.getWorld();
        BlockPos pos = player.getBlockPos();

        TestGenerator.initTestHub();
        if (TestGenerator.getCentraleData() == null) {
            source.sendError(Text.literal("§cCentrale non chargee."));
            return 0;
        }

        // Lire la direction du port blanc detecte
        int[] white = TestGenerator.getRoomPorts(TestGenerator.CENTRALE);
        source.sendFeedback(() -> Text.literal("§7Ports blancs: " + java.util.Arrays.toString(white)), false);

        // Direction du port blanc = entree
        int entreeDir = (white != null && white.length > 0) ? white[0] : 3;
        String[] dirNames = {"Est", "Sud", "Ouest", "Nord"};
        source.sendFeedback(() -> Text.literal("§aEntree detectee: " + dirNames[entreeDir]), true);

        // Placer la Centrale + 1 couloir dans la direction de l'entree
        int ox = pos.getX(), oy = pos.getY(), oz = pos.getZ();
        TestGenerator.placeCentraleTest(world, ox, oy, oz, entreeDir);

        source.sendFeedback(() -> Text.literal("§7Centrale placee. Entree vers le " + dirNames[entreeDir] + "."), true);
        return 1;
    }

    private static int lobbyCheck(ServerCommandSource source) {
        source.sendFeedback(() -> Text.literal("§7Vérification du lobby " + LobbyClient.getBaseUrl() + "..."), false);
        boolean ok = LobbyClient.healthCheck();
        if (ok) {
            source.sendFeedback(() -> Text.literal("§a§l✔ Lobby répond !"), true);
        } else {
            source.sendError(Text.literal("§c§l✘ Lobby injoignable"));
        }
        return 1;
    }
}
