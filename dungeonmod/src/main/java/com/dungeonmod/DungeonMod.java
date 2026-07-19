package com.dungeonmod;

import com.dungeonmod.test.TestGenerator;
import com.dungeonmod.util.FlecheComboData;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.LightBlock;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.scoreboard.Team;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class DungeonMod implements ModInitializer {
    public static final String MOD_ID = "dungeonmod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final List<DungeonData> dungeons = new ArrayList<>();
    public static BlockPos lastDepartPos = null;
    public static final Map<UUID, BlockPos> lastAnchorSpawn = new HashMap<>();

    public static final String FLASK_NAME = "§aFiole";
    public static final String FLASK_BLESSED_NAME = "§9Fiole d'eau bénite";

    public static final Map<UUID, List<ItemStack>> sacBackup = new HashMap<>();

    public static final Set<UUID> customZombies = new HashSet<>();
    public static final Map<UUID, Identifier> zombieTextures = new HashMap<>();
    public static final Map<UUID, BlockPos> zombieSpawns = new HashMap<>();
    private static final Set<UUID> alertedGoblins = new HashSet<>(); // permanently hostile
    public static final Identifier TEXTURE_GOBELIN_1 = Identifier.of("dungeonmod", "textures/entity/gobelin_1.png");
    public static final Identifier TEXTURE_GOBELIN_2 = Identifier.of("dungeonmod", "textures/entity/gobelin_2.png");

    @Override
    public void onInitialize() {

        ModItems.addToCreativeTabs();
        com.dungeonmod.screen.ModScreenHandlers.register();
        FabricDefaultAttributeRegistry.register(com.dungeonmod.entity.StoneThrowerGoblinEntity.THROWER_TYPE,
            net.minecraft.entity.mob.ZombieEntity.createZombieAttributes()
                .add(net.minecraft.entity.attribute.EntityAttributes.MAX_HEALTH, 14.0)
                .add(net.minecraft.entity.attribute.EntityAttributes.FOLLOW_RANGE, 8.0)
                .add(net.minecraft.entity.attribute.EntityAttributes.ATTACK_DAMAGE, 2.0));
        com.dungeonmod.entity.OgreEntity.registerAttributes();

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            TestGenerator.loadFromDisk(server);
            if (TestGenerator.getLastSeed() != 0) {
                ServerWorld world = server.getOverworld();
                addDungeon("LastGen",
                    new BlockPos(TestGenerator.getLastOriginX(), TestGenerator.getLastOriginY(), TestGenerator.getLastOriginZ()),
                    10, 10,
                    world.getRegistryKey().getValue(), 0, null);
            }
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            DungeonCommand.register(dispatcher);
        });

        // Nettoyage de l'armure lourde à la mort réelle, et respawn sur l'ancre
        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
                if (!alive) {
                UUID u = newPlayer.getUuid();
                heavyPieceGiven.remove(u);
                heavyPieceStored.remove(u);
                pendingDamage.remove(u);
                // Restaurer le Sac dans l'inventaire après la mort
                List<ItemStack> savedSacs = sacBackup.remove(u);
                if (savedSacs != null) {
                    for (ItemStack sac : savedSacs) {
                        newPlayer.getInventory().offerOrDrop(sac);
                    }
                }
                // Forcer le spawn sur l'ancre si elle a été utilisée
                BlockPos myAnchor = lastAnchorSpawn.get(u);
                if (myAnchor != null) {
                    // Téléporter le joueur au puit après le respawn
                    BlockPos anchorPos = myAnchor;
                    var worldKey = oldPlayer.getWorld().getRegistryKey();
                    newPlayer.getServer().execute(() -> {
                        ServerWorld targetWorld = newPlayer.getServer().getWorld(worldKey);
                        if (targetWorld != null) {
                            newPlayer.teleport(targetWorld,
                                anchorPos.getX(), anchorPos.getY() + 1.0, anchorPos.getZ(),
                                java.util.Set.of(), 0, 0, true);
                            newPlayer.sendMessage(Text.literal(""), true);
                            newPlayer.sendMessage(Text.literal("§6Vous réapparaissez au puits !"), true);
                            LOGGER.info("[Ancre] Téléporté au puits après respawn: {}", anchorPos);
                        }
                    });
                }
            }
        });

        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            if (!world.isClient()) {
                LOGGER.info("[AttackBlock] pos={}, hand={}", pos, hand);
                if (com.dungeonmod.util.AncreGrappling.onAttackBlock(player, world, hand, pos))
                    return ActionResult.SUCCESS;
            }
            return ActionResult.PASS;
        });

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            ItemStack stack = player.getStackInHand(hand);
            BlockPos pos = hitResult.getBlockPos();
            BlockState state = world.getBlockState(pos);

            // Porte pale du jardin — vérouillée, seule la clé blanche ouvre
            if (state.isOf(Blocks.PALE_OAK_DOOR) && !player.isCreative() && !player.isSpectator()) {
                if (world.isClient()) {
                    // Client : laisser passer le paquet si on a la clé, sinon bloquer
                    return isWhiteKey(stack) ? ActionResult.PASS : ActionResult.FAIL;
                }
                // Serveur
                if (isWhiteKey(stack)) {
                    if (tryOpenPaleOakDoor((ServerWorld) world, pos)) {
                        if (!player.isCreative()) stack.decrement(1);
                        return ActionResult.SUCCESS;
                    }
                    return ActionResult.FAIL;
                }
                player.sendMessage(Text.literal("§cCette porte est verrouillée."), true);
                return ActionResult.FAIL;
            }

            if (!world.isClient() && world instanceof ServerWorld sw) {
                // Ancre : poser son spawn sur l'eau du puits (clic droit)
                if (!stack.isEmpty() && stack.isOf(Items.CONDUIT)
                    && stack.contains(DataComponentTypes.CUSTOM_NAME)
                    && stack.get(DataComponentTypes.CUSTOM_NAME).getString().contains("Ancre")
                    && player instanceof ServerPlayerEntity sp) {
                    LOGGER.info("[Ancre] Right-click detected, pos={}", pos);
                    boolean isWater = isNearWater(world, pos);
                    LOGGER.info("[Ancre] isNearWater={}", isWater);
                    if (isWater) {
                        LOGGER.info("[Ancre] lastPuitPositions count={}", com.dungeonmod.test.TestGenerator.lastPuitPositions.size());
                        boolean inPuit = false;
                        for (BlockPos p : com.dungeonmod.test.TestGenerator.lastPuitPositions) {
                            boolean inRange = pos.getX() >= p.getX() && pos.getX() < p.getX() + 10
                                && pos.getZ() >= p.getZ() && pos.getZ() < p.getZ() + 10;
                            LOGGER.info("[Ancre] Checking puit pos p={}, pos={}, inRange={}", p, pos, inRange);
                            if (inRange) {
                                inPuit = true; break;
                            }
                        }
                        LOGGER.info("[Ancre] inPuit={}", inPuit);
                        if (inPuit) {
                            BlockPos waterPos = findWaterPos(world, pos);
                            BlockPos spawnPos = waterPos.up();
                            sp.setSpawnPoint(net.minecraft.registry.RegistryKey.of(
                                net.minecraft.registry.RegistryKeys.WORLD,
                                world.getRegistryKey().getValue()),
                                spawnPos, 0.0f, true, false);
                            lastAnchorSpawn.put(sp.getUuid(), waterPos);
                            sp.sendMessage(Text.literal("§6Point de réapparition défini sur l'eau du puits !"), true);
                            LOGGER.info("[Ancre] Spawn set at {} (water at {})", spawnPos, waterPos);
                            return ActionResult.SUCCESS;
                        } else {
                            LOGGER.info("[Ancre] Not in puit area");
                        }
                    }
                }

                if (isKey(stack)) {
                    if (state.isOf(Blocks.IRON_DOOR)) {
                        if (tryOpenIronDoor(sw, pos)) {
                            if (!player.isCreative()) stack.decrement(1);
                            return ActionResult.SUCCESS;
                        }
                        return ActionResult.FAIL;
                    }
                }
                boolean toggled = TestGenerator.toggleBib2(sw, pos);
                if (toggled) {
                    return ActionResult.SUCCESS;
                }
                if (state.isOf(Blocks.CAMPFIRE) && isBaton(stack)) {
                    ItemStack torche = Items.STICK.getDefaultStack();
                    torche.set(DataComponentTypes.CUSTOM_NAME, net.minecraft.text.Text.literal("§9Torche"));
                    torche.set(DataComponentTypes.ITEM_MODEL, Identifier.of("dungeonmod", "torche"));
                    player.getInventory().setStack(player.getInventory().selectedSlot, torche);
                    return ActionResult.SUCCESS;
                }
                if (com.dungeonmod.util.TorcheHelper.isTorche(stack) && isNearWater(world, pos)) {
                    ItemStack baton = Items.STICK.getDefaultStack();
                    baton.set(DataComponentTypes.CUSTOM_NAME, net.minecraft.text.Text.literal("§9Bâton"));
                    player.getInventory().setStack(player.getInventory().selectedSlot, baton);
                    return ActionResult.SUCCESS;
                }
            }
            return ActionResult.PASS;
        });

        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getStackInHand(hand);

            if (isFlask(stack) && !isBlessedFlask(stack)) {
                HitResult hit = player.raycast(5.0, 1.0f, false);
                if (hit.getType() == HitResult.Type.BLOCK && hit instanceof BlockHitResult blockHit) {
                    if (isNearWater(world, blockHit.getBlockPos())) {
                        if (!world.isClient()) {
                            transformFlaskToBlessed(player, hand, stack);
                        }
                        return ActionResult.SUCCESS;
                    }
                }
            }

            if (isFlask(stack) && isBlessedFlask(stack)) {
                if (!world.isClient()) {
                    if (player instanceof ServerPlayerEntity sp) {
                        holyWaterTimers.put(sp.getUuid(), System.currentTimeMillis() + 8000);
                    }
                    transformFlaskToNormal(player, hand, stack);
                }
                return ActionResult.SUCCESS;
            }

            if (isApple(stack)) {
                if (!world.isClient()) {
                    player.heal(1.0f);
                    if (!player.isCreative()) stack.decrement(1);
                }
                return ActionResult.SUCCESS;
            }

            if (isPotato(stack)) {
                if (!world.isClient() && player instanceof ServerPlayerEntity sp) {
                    sp.heal(2.0f);
                    sp.addStatusEffect(new StatusEffectInstance(
                        StatusEffects.NAUSEA, 200, 0, false, false));
                    if (!sp.isCreative()) stack.decrement(1);
                }
                return ActionResult.SUCCESS;
            }

            if (isSteak(stack)) {
                if (!world.isClient() && player instanceof ServerPlayerEntity sp) {
                    sp.heal(6.0f);
                    if (!sp.isCreative()) stack.decrement(1);
                }
                return ActionResult.SUCCESS;
            }

            if (isBiere(stack)) {
                if (!world.isClient() && player instanceof ServerPlayerEntity sp) {
                    if (stack.isOf(Items.POTION)) {
                        sp.addStatusEffect(new StatusEffectInstance(
                            StatusEffects.NAUSEA, 400, 0, false, false));
                        com.dungeonmod.util.BeerStrengthData.applyBoost(sp, "brune", 1.5f, 400);
                    } else {
                        com.dungeonmod.util.BeerStrengthData.applyBoost(sp, "viking", 2.5f, 600);
                    }
                    if (!sp.isCreative()) {
                        ItemStack chope = new ItemStack(Items.GLASS_BOTTLE);
                        chope.set(DataComponentTypes.CUSTOM_NAME, net.minecraft.text.Text.literal("§7Chope de bière"));
                        chope.set(DataComponentTypes.ITEM_MODEL, Identifier.of("dungeonmod", "chope_biere"));
                        if (hand.equals(net.minecraft.util.Hand.MAIN_HAND)) {
                            player.getInventory().setStack(player.getInventory().selectedSlot, chope);
                        } else {
                            player.getInventory().setStack(40, chope);
                        }
                    }
                }
                return ActionResult.SUCCESS;
            }

            if (isEgg(stack)) {
                if (!world.isClient() && player instanceof ServerPlayerEntity sp) {
                    var hit = sp.raycast(10.0, 1.0f, false);
                    double x = hit.getPos().x;
                    double y = hit.getPos().y + 1;
                    double z = hit.getPos().z;
                    Identifier tex = getTextureForEgg(stack);
                    String name = stack.get(DataComponentTypes.CUSTOM_NAME).getString();
                    if (name.contains("ogre")) {
                        var ogre = new com.dungeonmod.entity.OgreEntity(
                            com.dungeonmod.entity.OgreEntity.TYPE, world);
                        ogre.setPosition(x, y, z);
                        ogre.setPersistent();
                        ogre.setCustomName(net.minecraft.text.Text.literal("§eCyclope"));
                        ogre.setCustomNameVisible(false);
                        ogre.throwTestMode = true;
                        world.spawnEntity(ogre);
                    } else if (name.contains("lanceur")) {
                        var goblin = new com.dungeonmod.entity.StoneThrowerGoblinEntity(
                            com.dungeonmod.entity.StoneThrowerGoblinEntity.THROWER_TYPE, world);
                        goblin.setPosition(x, y, z);
                        goblin.setPersistent();
                        if (tex != null) zombieTextures.put(goblin.getUuid(), tex);
                        customZombies.add(goblin.getUuid());
                        world.spawnEntity(goblin);
                    } else {
                        var zombie = new net.minecraft.entity.mob.ZombieEntity(
                            net.minecraft.entity.EntityType.ZOMBIE, world);
                        zombie.setPosition(x, y, z);
                        if (tex != null) zombieTextures.put(zombie.getUuid(), tex);
                        customZombies.add(zombie.getUuid());
                        world.spawnEntity(zombie);
                    }
                    if (!sp.isCreative()) stack.decrement(1);
                }
                return ActionResult.SUCCESS;
            }

            if (isHeart(stack)) {
                if (!world.isClient() && player instanceof ServerPlayerEntity sp) {
                    var attr = sp.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.MAX_HEALTH);
                    if (attr != null) {
                        double current = attr.getBaseValue();
                        attr.setBaseValue(current + 2.0);
                    }
                    sp.setHealth(sp.getHealth() + 2.0f);
                    if (!sp.isCreative()) stack.decrement(1);
                }
                return ActionResult.SUCCESS;
            }

            if (isBaton(stack)) {
                if (!world.isClient() && player instanceof ServerPlayerEntity sp && canThrow(sp) && !com.dungeonmod.util.CraftingHelper.wasJustCrafted(sp)) {
                    var snowball = new net.minecraft.entity.projectile.thrown.SnowballEntity(world, sp, stack);
                    snowball.setVelocity(sp, sp.getPitch(), sp.getYaw(), 0.0f, 1.5f, 0.0f);
                    world.spawnEntity(snowball);
                    if (!sp.isCreative()) stack.decrement(1);
                }
                return ActionResult.SUCCESS;
            }

            if (isOs(stack)) {
                if (!world.isClient() && player instanceof ServerPlayerEntity sp && canThrow(sp)) {
                    var snowball = new net.minecraft.entity.projectile.thrown.SnowballEntity(world, sp, stack);
                    snowball.setVelocity(sp, sp.getPitch(), sp.getYaw(), 0.0f, 1.5f, 0.0f);
                    world.spawnEntity(snowball);
                    if (!sp.isCreative()) stack.decrement(1);
                }
                return ActionResult.SUCCESS;
            }

            if (com.dungeonmod.util.TorcheHelper.isTorche(stack)) {
                if (!world.isClient() && player instanceof ServerPlayerEntity sp && canThrow(sp)) {
                    var snowball = new net.minecraft.entity.projectile.thrown.SnowballEntity(world, sp, stack);
                    snowball.setVelocity(sp, sp.getPitch(), sp.getYaw(), 0.0f, 1.5f, 0.0f);
                    world.spawnEntity(snowball);
                    if (!sp.isCreative()) stack.decrement(1);
                }
                return ActionResult.SUCCESS;
            }

            if (com.dungeonmod.util.BoomerangHelper.isBoomerang(stack)) {
                if (!world.isClient() && player instanceof ServerPlayerEntity sp) {
                    com.dungeonmod.util.BoomerangHelper.throwBoomerang(sp, stack);
                }
                return ActionResult.SUCCESS;
            }

            if (com.dungeonmod.util.FlecheComboData.isFleche(stack)) {
                if (!world.isClient()) {
                    player.setCurrentHand(hand);
                }
                return ActionResult.SUCCESS;
            }

            return ActionResult.PASS;
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerWorld world : server.getWorlds()) {
                for (ServerPlayerEntity player : world.getPlayers()) {
                    handleHeavyHelmet(player);
                    handleMinerHelmet(player);
                    handleTorchLight(player);
                    preventAzaleaGrowth(player);
                    handleHeavyChestplate(player);
                    handleVoyageurLeggings(player);
                    showHunterCooldown(player);
                    handleHunterLeggings(player);
                    handleDentDeLoup(player);
                    checkFlecheTimers(player, System.currentTimeMillis());
                    handleTetralame(player);
                    handleDungeonFood(player);
                }
            }
            // Grappin de l'ancre
            for (ServerWorld world : server.getWorlds()) {
                com.dungeonmod.util.AncreGrappling.tickHooks(world);
            }
            spawnBoneStars(server);
            processHolyWater(server);
            processIceDot(server);
            com.dungeonmod.entity.BoomerangEntity.processPending();
            if (!dungeons.isEmpty()) {


                guideGoblinsHome(server);
            }
        });

        // Register cyclops sounds
        net.minecraft.registry.Registry.register(net.minecraft.registry.Registries.SOUND_EVENT,
            Identifier.of("dungeonmod", "entity.cyclops.growl1"),
            net.minecraft.sound.SoundEvent.of(Identifier.of("dungeonmod", "entity.cyclops.grognement_cyclops_1")));
        net.minecraft.registry.Registry.register(net.minecraft.registry.Registries.SOUND_EVENT,
            Identifier.of("dungeonmod", "entity.cyclops.growl2"),
            net.minecraft.sound.SoundEvent.of(Identifier.of("dungeonmod", "entity.cyclops.grognement_cyclops_2")));

        LOGGER.info("Dungeon Mod loaded!");
    }

    public static boolean canThrow(ServerPlayerEntity player) {
        return !com.dungeonmod.util.EpeeBlockData.get(player).isOnCooldown();
    }

    public static boolean isFlask(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (!stack.isOf(Items.GLASS_BOTTLE) && !stack.isOf(Items.POTION)) return false;
        if (!stack.contains(DataComponentTypes.CUSTOM_NAME)) return false;
        String name = stack.get(DataComponentTypes.CUSTOM_NAME).getString();
        return name.contains("Fiole");
    }

    public static boolean isApple(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (!stack.isOf(Items.APPLE)) return false;
        if (!stack.contains(DataComponentTypes.CUSTOM_NAME)) return false;
        String name = stack.get(DataComponentTypes.CUSTOM_NAME).getString();
        return name.contains("Pomme rouge");
    }

    public static boolean isPotato(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (!stack.isOf(Items.POISONOUS_POTATO)) return false;
        if (!stack.contains(DataComponentTypes.CUSTOM_NAME)) return false;
        String name = stack.get(DataComponentTypes.CUSTOM_NAME).getString();
        return name.contains("Patate douce");
    }

    public static boolean isSteak(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (!stack.isOf(Items.BEEF)) return false;
        if (!stack.contains(DataComponentTypes.CUSTOM_NAME)) return false;
        String name = stack.get(DataComponentTypes.CUSTOM_NAME).getString();
        return name.contains("Steack cru");
    }

    public static boolean isBiere(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (!stack.isOf(Items.POTION) && !stack.isOf(Items.HONEY_BOTTLE)) return false;
        if (!stack.contains(DataComponentTypes.CUSTOM_NAME)) return false;
        String name = stack.get(DataComponentTypes.CUSTOM_NAME).getString();
        return name.contains("Bière");
    }

    public static boolean isEgg(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (!stack.isOf(Items.STICK)) return false;
        if (!stack.contains(DataComponentTypes.CUSTOM_NAME)) return false;
        String name = stack.get(DataComponentTypes.CUSTOM_NAME).getString();
        return name.contains("Oeuf");
    }

    public static boolean isBaton(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (!stack.isOf(Items.STICK)) return false;
        if (!stack.contains(DataComponentTypes.CUSTOM_NAME)) return false;
        String name = stack.get(DataComponentTypes.CUSTOM_NAME).getString();
        return name.contains("Bâton");
    }

    public static boolean isKey(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (!stack.isOf(Items.TRIAL_KEY)) return false;
        if (!stack.contains(DataComponentTypes.CUSTOM_NAME)) return false;
        String name = stack.get(DataComponentTypes.CUSTOM_NAME).getString();
        return name.contains("Clé");
    }

    public static boolean isDentDeLoup(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (!stack.isOf(Items.STICK)) return false;
        if (!stack.contains(DataComponentTypes.CUSTOM_NAME)) return false;
        String name = stack.get(DataComponentTypes.CUSTOM_NAME).getString();
        return name.contains("Dent de loup");
    }

    public static boolean isWhiteKey(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (!stack.isOf(Items.TRIAL_KEY)) return false;
        if (!stack.contains(DataComponentTypes.CUSTOM_NAME)) return false;
        String name = stack.get(DataComponentTypes.CUSTOM_NAME).getString();
        return name.contains("Clé blanche");
    }

    public static boolean isOs(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (!stack.isOf(Items.BONE)) return false;
        if (!stack.contains(DataComponentTypes.CUSTOM_NAME)) return false;
        String name = stack.get(DataComponentTypes.CUSTOM_NAME).getString();
        return name.contains("Os");
    }

    private static final Set<BlockPos> openedDoors = new HashSet<>();
    public static final Set<UUID> stunnedEntities = new HashSet<>();

    private static boolean tryOpenIronDoor(ServerWorld world, BlockPos pos) {
        var state = world.getBlockState(pos);
        if (!state.isOf(Blocks.IRON_DOOR)) return false;

        // Get the lower part of the door
        if (state.get(DoorBlock.HALF) == net.minecraft.block.enums.DoubleBlockHalf.UPPER) {
            pos = pos.down();
            state = world.getBlockState(pos);
        }

        if (!state.isOf(Blocks.IRON_DOOR)) return false;
        if (openedDoors.contains(pos)) return false;
        if (state.get(DoorBlock.OPEN)) return false;

        // Open this door
        ((DoorBlock) state.getBlock()).setOpen(null, world, state, pos, true);
        openedDoors.add(pos);

        // Check for adjacent iron door (double door)
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                if (Math.abs(dx) + Math.abs(dz) > 1) continue;
                BlockPos adj = pos.add(dx, 0, dz);
                if (world.getBlockState(adj).isOf(Blocks.IRON_DOOR)) {
                    ((DoorBlock) world.getBlockState(adj).getBlock()).setOpen(null, world, world.getBlockState(adj), adj, true);
                    openedDoors.add(adj);
                }
            }
        }

        return true;
    }

    private static boolean tryOpenPaleOakDoor(ServerWorld world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (!state.isOf(Blocks.PALE_OAK_DOOR)) return false;

        if (state.get(DoorBlock.HALF) == DoubleBlockHalf.UPPER) {
            pos = pos.down();
            state = world.getBlockState(pos);
        }

        if (!state.isOf(Blocks.PALE_OAK_DOOR)) return false;
        if (state.get(DoorBlock.OPEN)) return false;

        ((DoorBlock) state.getBlock()).setOpen(null, world, state, pos, true);

        // Double porte adjacente
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                if (Math.abs(dx) + Math.abs(dz) > 1) continue;
                BlockPos adj = pos.add(dx, 0, dz);
                BlockState adjState = world.getBlockState(adj);
                if (adjState.isOf(Blocks.PALE_OAK_DOOR) && !adjState.get(DoorBlock.OPEN)) {
                    ((DoorBlock) adjState.getBlock()).setOpen(null, world, adjState, adj, true);
                }
            }
        }

        return true;
    }

    private static void handleDentDeLoup(ServerPlayerEntity player) {
        boolean hasDent = isDentDeLoup(player.getMainHandStack())
            || isDentDeLoup(player.getOffHandStack());

        ServerWorld world = player.getServerWorld();
        var scoreboard = world.getScoreboard();

        Team redTeam = scoreboard.getTeam("red_glowing");
        if (redTeam == null) {
            redTeam = scoreboard.addTeam("red_glowing");
            redTeam.setColor(net.minecraft.util.Formatting.RED);
        }

        double radius = 10.0;
        Box box = player.getBoundingBox().expand(radius);

        for (var entity : world.getOtherEntities(player, box)) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (living instanceof PlayerEntity) continue;
            String uuidStr = living.getUuidAsString();

            if (hasDent && living.getHealth() / living.getMaxHealth() <= 0.4f
                && player.squaredDistanceTo(living) <= radius * radius) {
                living.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
                    net.minecraft.entity.effect.StatusEffects.GLOWING, 10, 0, true, false, false));
                if (scoreboard.getScoreHolderTeam(uuidStr) != redTeam) {
                    scoreboard.addScoreHolderToTeam(uuidStr, redTeam);
                }
            } else {
                if (living.hasStatusEffect(net.minecraft.entity.effect.StatusEffects.GLOWING)) {
                    living.removeStatusEffect(net.minecraft.entity.effect.StatusEffects.GLOWING);
                }
                if (scoreboard.getScoreHolderTeam(uuidStr) == redTeam) {
                    scoreboard.removeScoreHolderFromTeam(uuidStr, redTeam);
                }
            }
        }

        // Nettoyage : retirer de l'équipe les entités mortes/disparues
        for (String member : java.util.List.copyOf(redTeam.getPlayerList())) {
            try {
                java.util.UUID uuid = java.util.UUID.fromString(member);
                Entity memberEntity = world.getEntity(uuid);
                if (memberEntity == null || !memberEntity.isAlive()
                    || (memberEntity instanceof LivingEntity l && !l.hasStatusEffect(net.minecraft.entity.effect.StatusEffects.GLOWING))) {
                    scoreboard.removeScoreHolderFromTeam(member, redTeam);
                }
            } catch (IllegalArgumentException ignored) {}
        }
    }

    private static boolean hasTetralame(ServerPlayerEntity player) {
        return isTetralame(player.getMainHandStack()) || isTetralame(player.getOffHandStack());
    }

    private static boolean isTetralame(ItemStack stack) {
        return !stack.isEmpty() && stack.isOf(net.minecraft.item.Items.NETHERITE_SWORD)
            && stack.contains(net.minecraft.component.DataComponentTypes.CUSTOM_NAME)
            && stack.get(net.minecraft.component.DataComponentTypes.CUSTOM_NAME).getString().contains("Tétralame mort subite");
    }

    public static final Map<UUID, Double> tetralameSavedHealth = new HashMap<>();

    private static void handleTetralame(ServerPlayerEntity player) {
        UUID u = player.getUuid();
        var attr = player.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.MAX_HEALTH);
        if (attr == null) return;

        if (hasTetralame(player)) {
            if (attr.getBaseValue() > 1.0) {
                tetralameSavedHealth.put(u, attr.getBaseValue());
                attr.setBaseValue(1.0);
            }
            if (player.getHealth() > 1.0f) {
                player.setHealth(1.0f);
            }
        } else {
            Double saved = tetralameSavedHealth.remove(u);
            if (saved != null) {
                double prevMax = saved;
                float prevHealth = player.getHealth();
                attr.setBaseValue(prevMax);
                player.setHealth((float)(prevHealth > prevMax ? prevMax : Math.max(0.5f, prevHealth)));
            }
        }
    }

    public static boolean isHeart(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (!stack.isOf(Items.HEART_OF_THE_SEA)) return false;
        if (!stack.contains(DataComponentTypes.CUSTOM_NAME)) return false;
        String name = stack.get(DataComponentTypes.CUSTOM_NAME).getString();
        return name.contains("Coeur");
    }

    public static Identifier getTextureForEgg(ItemStack stack) {
        String name = stack.get(DataComponentTypes.CUSTOM_NAME).getString();
        if (name.contains("gobelin 1")) return TEXTURE_GOBELIN_1;
        if (name.contains("gobelin 2")) return TEXTURE_GOBELIN_2;
        if (name.contains("lanceur")) return TEXTURE_GOBELIN_2;
        return null;
    }

    public static boolean isBlessedFlask(ItemStack stack) {
        return stack.isOf(Items.GLASS_BOTTLE) && stack.contains(DataComponentTypes.CUSTOM_NAME) && 
            stack.get(DataComponentTypes.CUSTOM_NAME).getString().contains("Fiole d'eau bénite");
    }

    private static boolean isNearWater(World world, BlockPos pos) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos check = pos.add(dx, dy, dz);
                    if (world.getFluidState(check).isOf(net.minecraft.fluid.Fluids.WATER)) return true;
                    if (world.getBlockState(check).isOf(net.minecraft.block.Blocks.WATER)) return true;
                }
            }
        }
        return false;
    }

    private static BlockPos findWaterPos(World world, BlockPos pos) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos check = pos.add(dx, dy, dz);
                    if (world.getFluidState(check).isOf(net.minecraft.fluid.Fluids.WATER)) return check;
                    if (world.getBlockState(check).isOf(net.minecraft.block.Blocks.WATER)) return check;
                }
            }
        }
        return pos;
    }

    private static LoreComponent flaskLore(String... lines) {
        java.util.List<Text> lore = new java.util.ArrayList<>();
        for (String line : lines) lore.add(Text.literal(line));
        return new LoreComponent(lore);
    }

    public static void transformFlaskToBlessed(PlayerEntity player, Hand hand, ItemStack stack) {
        ItemStack blessed = new ItemStack(Items.GLASS_BOTTLE);
        blessed.set(DataComponentTypes.CUSTOM_NAME, Text.literal(FLASK_BLESSED_NAME));
        blessed.set(DataComponentTypes.ITEM_MODEL, Identifier.of("dungeonmod", "fiole_benite"));
        blessed.set(DataComponentTypes.LORE, flaskLore("§7Une eau bénite.", "§7Régénère 0,5 coeur/s pendant 8s."));
        player.setStackInHand(hand, blessed);
    }

    public static void transformFlaskToNormal(PlayerEntity player, Hand hand, ItemStack stack) {
        ItemStack normal = new ItemStack(Items.GLASS_BOTTLE);
        normal.set(DataComponentTypes.CUSTOM_NAME, Text.literal(FLASK_NAME));
        normal.set(DataComponentTypes.LORE, flaskLore("§7Une fiole banale.", "§7Peut contenir de l'eau."));
        player.setStackInHand(hand, normal);
    }

    public static ItemStack createFlask() {
        ItemStack stack = new ItemStack(Items.GLASS_BOTTLE);
        stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(FLASK_NAME));
        stack.set(DataComponentTypes.LORE, flaskLore("§7Une fiole banale.", "§7Peut contenir de l'eau."));
        return stack;
    }

    private static final Set<UUID> healthSet = new HashSet<>();

    public static Set<UUID> getHealthSet() { return healthSet; }
    private static final Map<UUID, BlockPos> minerLightPositions = new HashMap<>();
    private static final Set<UUID> voyageurSpeedSet = new HashSet<>();
    private static final Identifier VOYAGEUR_SPEED_ID = Identifier.of("dungeonmod", "voyageur_speed_boost");
    public static final Map<UUID, Long> hunterCooldown = new java.util.concurrent.ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> hadHunterLegs = new java.util.concurrent.ConcurrentHashMap<>();

    private static void showHunterCooldown(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        var legs = player.getInventory().getArmorStack(1);
        boolean hasLegs = !legs.isEmpty() && legs.isOf(Items.CHAINMAIL_LEGGINGS)
            && legs.contains(DataComponentTypes.CUSTOM_NAME)
            && legs.get(DataComponentTypes.CUSTOM_NAME).getString().contains("Jambière du chasseur");

        // Detect equip → set cooldown
        if (hasLegs && !Boolean.TRUE.equals(hadHunterLegs.get(uuid))) {
            hunterCooldown.put(uuid, System.currentTimeMillis());
        }
        hadHunterLegs.put(uuid, hasLegs);

        if (!hasLegs) {
            hunterCooldown.remove(uuid);
            return;
        }

        Long start = hunterCooldown.get(uuid);
        if (start != null) {
            long remaining = 2000 - (System.currentTimeMillis() - start);
            if (remaining > 0) {
                return;
            }
            hunterCooldown.remove(uuid);
        }

    }

    public static boolean isHunterProne(PlayerEntity player) {
        var legs = player.getInventory().getArmorStack(1);
        if (legs.isEmpty() || !legs.isOf(Items.CHAINMAIL_LEGGINGS)) return false;
        if (!legs.contains(DataComponentTypes.CUSTOM_NAME)) return false;
        if (!legs.get(DataComponentTypes.CUSTOM_NAME).getString().contains("Jambière du chasseur")) return false;
        if (!player.isSneaking()) return false;

        Long start = hunterCooldown.get(player.getUuid());
        if (start != null && System.currentTimeMillis() - start < 2000) return false;
        return true;
    }

    private static boolean hasSkull(ServerPlayerEntity player) {
        ItemStack helmet = player.getInventory().getArmorStack(3);
        if (helmet.isEmpty() || !helmet.isOf(Items.SKELETON_SKULL)) return false;
        if (!helmet.contains(DataComponentTypes.CUSTOM_NAME)) return false;
        return helmet.get(DataComponentTypes.CUSTOM_NAME).getString().contains("Crâne de squelette");
    }

    private static boolean hasHeavyHelmet(ServerPlayerEntity player) {
        ItemStack helmet = player.getInventory().getArmorStack(3);
        if (helmet.isEmpty() || !helmet.isOf(Items.IRON_HELMET)) return false;
        if (!helmet.contains(DataComponentTypes.CUSTOM_NAME)) return false;
        return helmet.get(DataComponentTypes.CUSTOM_NAME).getString().contains("Casque lourd");
    }

    private static void handleHeavyHelmet(ServerPlayerEntity player) {
        // Effect replaced by camera overlay (EQUIPPABLE component)
    }

    private static boolean hasMinerHelmet(ServerPlayerEntity player) {
        ItemStack helmet = player.getInventory().getArmorStack(3);
        if (helmet.isEmpty() || !helmet.isOf(Items.LEATHER_HELMET)) return false;
        if (!helmet.contains(DataComponentTypes.CUSTOM_NAME)) return false;
        return helmet.get(DataComponentTypes.CUSTOM_NAME).getString().contains("Casque du mineur");
    }

    private static void handleMinerHelmet(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        BlockPos prevPos = minerLightPositions.get(uuid);
        if (hasMinerHelmet(player)) {
            BlockPos headPos = player.getBlockPos().up(1);
            if (!headPos.equals(prevPos)) {
                if (prevPos != null && player.getServerWorld().getBlockState(prevPos).isOf(Blocks.LIGHT)) {
                    player.getServerWorld().setBlockState(prevPos, Blocks.AIR.getDefaultState(), 3);
                }
                if (player.getServerWorld().getBlockState(headPos).isAir()) {
                    player.getServerWorld().setBlockState(headPos, Blocks.LIGHT.getDefaultState().with(LightBlock.LEVEL_15, 14), 3);
                }
                minerLightPositions.put(uuid, headPos);
            }
        } else if (prevPos != null) {
            if (player.getServerWorld().getBlockState(prevPos).isOf(Blocks.LIGHT)) {
                player.getServerWorld().setBlockState(prevPos, Blocks.AIR.getDefaultState(), 3);
            }
            minerLightPositions.remove(uuid);
        }
    }

    private static final Map<UUID, BlockPos> torchLightPositions = new HashMap<>();

    private static void handleTorchLight(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        BlockPos prevPos = torchLightPositions.get(uuid);
        boolean hasTorch = com.dungeonmod.util.TorcheHelper.isTorche(player.getMainHandStack())
            || com.dungeonmod.util.TorcheHelper.isTorche(player.getOffHandStack());
        if (hasTorch) {
            BlockPos handPos = player.getBlockPos().up();
            if (!handPos.equals(prevPos)) {
                if (prevPos != null && player.getServerWorld().getBlockState(prevPos).isOf(Blocks.LIGHT)) {
                    player.getServerWorld().setBlockState(prevPos, Blocks.AIR.getDefaultState(), 3);
                }
                if (player.getServerWorld().getBlockState(handPos).isAir()) {
                    player.getServerWorld().setBlockState(handPos, Blocks.LIGHT.getDefaultState().with(LightBlock.LEVEL_15, 14), 3);
                }
                torchLightPositions.put(uuid, handPos);
            }
        } else if (prevPos != null) {
            if (player.getServerWorld().getBlockState(prevPos).isOf(Blocks.LIGHT)) {
                player.getServerWorld().setBlockState(prevPos, Blocks.AIR.getDefaultState(), 3);
            }
            torchLightPositions.remove(uuid);
        }
    }

    private static int azaleaTick = 0;
    private static void preventAzaleaGrowth(ServerPlayerEntity player) {
        if (++azaleaTick % 100 != 0) return; // tous les ~5 secondes
        BlockPos center = player.getBlockPos();
        ServerWorld world = player.getServerWorld();
        int r = 8;
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                for (int dy = -3; dy <= 5; dy++) {
                    BlockPos p = center.add(dx, dy, dz);
                    BlockState bs = world.getBlockState(p);
                    if (bs.isOf(Blocks.AZALEA) || bs.isOf(Blocks.FLOWERING_AZALEA)) {
                        world.setBlockState(p, bs, 3);
                    }
                }
            }
        }
    }

    private static boolean isHeavyPiece(ItemStack stack, Item baseItem, String name) {
        if (stack.isEmpty()) return false;
        if (!stack.isOf(baseItem)) return false;
        if (!stack.contains(DataComponentTypes.CUSTOM_NAME)) return false;
        return stack.get(DataComponentTypes.CUSTOM_NAME).getString().contains(name);
    }

    private static float getHeavyAbsorption(ServerPlayerEntity player) {
        float total = 0.0f;
        if (isHeavyPiece(player.getInventory().getArmorStack(3), Items.IRON_HELMET, "Casque lourd")) total += 2.0f;
        if (isHeavyPiece(player.getInventory().getArmorStack(2), Items.IRON_CHESTPLATE, "Plastron lourd")) total += 10.0f;
        if (isHeavyPiece(player.getInventory().getArmorStack(1), Items.IRON_LEGGINGS, "Jambière lourde")) total += 6.0f;
        return total;
    }

    private static final Map<UUID, Set<String>> heavyPieceGiven = new HashMap<>();
    private static final Map<UUID, Map<String, Float>> heavyPieceStored = new HashMap<>();
    private static final Map<UUID, Float> pendingDamage = new HashMap<>();

    public static Set<String> getHeavyClaimed(UUID uuid) {
        return heavyPieceGiven.get(uuid);
    }

    public static Set<String> getOrCreateHeavyClaimed(UUID uuid) {
        return heavyPieceGiven.computeIfAbsent(uuid, k -> new HashSet<>());
    }

    public static Map<String, Float> getHeavyStored(UUID uuid) {
        return heavyPieceStored.get(uuid);
    }

    public static Map<String, Float> getOrCreateHeavyStored(UUID uuid) {
        return heavyPieceStored.computeIfAbsent(uuid, k -> new HashMap<>());
    }

    private static void handleHeavyChestplate(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();

        // S'assurer que MAX_ABSORPTION est assez grand pour ne pas clamer setAbsorptionAmount
        var maxAbsAttr = player.getAttributeInstance(EntityAttributes.MAX_ABSORPTION);
        if (maxAbsAttr != null && maxAbsAttr.getBaseValue() < 100.0) {
            maxAbsAttr.setBaseValue(100.0);
        }

        // Détection mort réelle (health = 0) → on ne fait rien mais on NE RESET PAS
        if (player.getHealth() <= 0.0f) return;

        // Nettoyage uniquement si joueur n'est plus dans le monde (spectateur/déconnecté)
        if (player.isSpectator()) {
            heavyPieceGiven.remove(uuid);
            heavyPieceStored.remove(uuid);
            pendingDamage.remove(uuid);
            return;
        }

        Set<String> given = heavyPieceGiven.computeIfAbsent(uuid, k -> new HashSet<>());

        ItemStack head = player.getInventory().getArmorStack(3);
        ItemStack chest = player.getInventory().getArmorStack(2);
        ItemStack legs = player.getInventory().getArmorStack(1);
        ItemStack feet = player.getInventory().getArmorStack(0);

        boolean hasHelmet = !head.isEmpty() && head.isOf(Items.IRON_HELMET)
            && head.contains(DataComponentTypes.CUSTOM_NAME)
            && head.get(DataComponentTypes.CUSTOM_NAME).getString().contains("Casque lourd");
        boolean hasChestplate = !chest.isEmpty() && chest.isOf(Items.IRON_CHESTPLATE)
            && chest.contains(DataComponentTypes.CUSTOM_NAME)
            && chest.get(DataComponentTypes.CUSTOM_NAME).getString().contains("Plastron lourd");
        boolean hasLeggings = !legs.isEmpty() && legs.isOf(Items.IRON_LEGGINGS)
            && legs.contains(DataComponentTypes.CUSTOM_NAME)
            && legs.get(DataComponentTypes.CUSTOM_NAME).getString().contains("Jambière lourde");
        boolean hasBoots = !feet.isEmpty() && feet.isOf(Items.IRON_BOOTS)
            && feet.contains(DataComponentTypes.CUSTOM_NAME)
            && feet.get(DataComponentTypes.CUSTOM_NAME).getString().contains("Bottes lourdes");

        Map<String, Float> stored = heavyPieceStored.computeIfAbsent(uuid, k -> new HashMap<>());

        // Mettre à jour pendingDamage (dégâts pris depuis la dernière modif d'équipement)
        float newAbs = player.getAbsorptionAmount();
        Float lastAbs = stored.get("_last_abs");
        if (lastAbs != null && lastAbs > newAbs) {
            pendingDamage.merge(uuid, lastAbs - newAbs, Float::sum);
        }
        stored.put("_last_abs", newAbs);

        // État précédent
        boolean prevHelmet = given.contains("prev_helmet");
        boolean prevChestplate = given.contains("prev_chestplate");
        boolean prevLeggings = given.contains("prev_leggings");
        boolean prevBoots = given.contains("prev_boots");

        // Mettre à jour l'état précédent
        if (hasHelmet) given.add("prev_helmet"); else given.remove("prev_helmet");
        if (hasChestplate) given.add("prev_chestplate"); else given.remove("prev_chestplate");
        if (hasLeggings) given.add("prev_leggings"); else given.remove("prev_leggings");
        if (hasBoots) given.add("prev_boots"); else given.remove("prev_boots");

        // Pièces enlevées → paient les dégâts en attente
        if (prevHelmet && !hasHelmet) {
            float pd = pendingDamage.getOrDefault(uuid, 0f);
            float s = stored.getOrDefault("helmet", 2.0f);
            float pay = Math.min(s, pd);
            pendingDamage.put(uuid, pd - pay);
            stored.put("helmet", s - pay);
            float sub = s - pay;
            if (sub > 0) {
                player.setAbsorptionAmount(Math.max(0, player.getAbsorptionAmount() - sub));
                stored.put("_last_abs", player.getAbsorptionAmount());
            }
        }
        if (prevChestplate && !hasChestplate) {
            float pd = pendingDamage.getOrDefault(uuid, 0f);
            float s = stored.getOrDefault("chestplate", 10.0f);
            float pay = Math.min(s, pd);
            pendingDamage.put(uuid, pd - pay);
            stored.put("chestplate", s - pay);
            float sub = s - pay;
            if (sub > 0) {
                player.setAbsorptionAmount(Math.max(0, player.getAbsorptionAmount() - sub));
                stored.put("_last_abs", player.getAbsorptionAmount());
            }
        }
        if (prevLeggings && !hasLeggings) {
            float pd = pendingDamage.getOrDefault(uuid, 0f);
            float s = stored.getOrDefault("leggings", 6.0f);
            float pay = Math.min(s, pd);
            pendingDamage.put(uuid, pd - pay);
            stored.put("leggings", s - pay);
            float sub = s - pay;
            if (sub > 0) {
                player.setAbsorptionAmount(Math.max(0, player.getAbsorptionAmount() - sub));
                stored.put("_last_abs", player.getAbsorptionAmount());
            }
        }
        if (prevBoots && !hasBoots) {
            float pd = pendingDamage.getOrDefault(uuid, 0f);
            float s = stored.getOrDefault("boots", 2.0f);
            float pay = Math.min(s, pd);
            pendingDamage.put(uuid, pd - pay);
            stored.put("boots", s - pay);
            float sub = s - pay;
            if (sub > 0) {
                player.setAbsorptionAmount(Math.max(0, player.getAbsorptionAmount() - sub));
                stored.put("_last_abs", player.getAbsorptionAmount());
            }
        }

        // Pièces remises → restaurent leur stock (sans réinitialiser la valeur)
        if (hasHelmet && !prevHelmet && given.contains("helmet")) {
            float s = stored.getOrDefault("helmet", 0f);
            if (s > 0) {
                player.setAbsorptionAmount(player.getAbsorptionAmount() + s);
                stored.put("_last_abs", player.getAbsorptionAmount());
            }
        }
        if (hasChestplate && !prevChestplate && given.contains("chestplate")) {
            float s = stored.getOrDefault("chestplate", 0f);
            if (s > 0) {
                player.setAbsorptionAmount(player.getAbsorptionAmount() + s);
                stored.put("_last_abs", player.getAbsorptionAmount());
            }
        }
        if (hasLeggings && !prevLeggings && given.contains("leggings")) {
            float s = stored.getOrDefault("leggings", 0f);
            if (s > 0) {
                player.setAbsorptionAmount(player.getAbsorptionAmount() + s);
                stored.put("_last_abs", player.getAbsorptionAmount());
            }
        }
        if (hasBoots && !prevBoots && given.contains("boots")) {
            float s = stored.getOrDefault("boots", 0f);
            if (s > 0) {
                player.setAbsorptionAmount(player.getAbsorptionAmount() + s);
                stored.put("_last_abs", player.getAbsorptionAmount());
            }
        }

        // Pièces nouvellement équipées (jamais réclamées cette vie)
        boolean helmetNew = hasHelmet && given.add("helmet");
        boolean chestplateNew = hasChestplate && given.add("chestplate");
        boolean leggingsNew = hasLeggings && given.add("leggings");
        boolean bootsNew = hasBoots && given.add("boots");

        if (helmetNew || chestplateNew || leggingsNew || bootsNew) {
            float added = 0;
            if (helmetNew) { added += 2; stored.put("helmet", 2.0f); }
            if (chestplateNew) { added += 10; stored.put("chestplate", 10.0f); }
            if (leggingsNew) { added += 6; stored.put("leggings", 6.0f); }
            if (bootsNew) { added += 2; stored.put("boots", 2.0f); }
            player.setAbsorptionAmount(player.getAbsorptionAmount() + added);
            stored.put("_last_abs", player.getAbsorptionAmount());
        }
    }

    private static void handleHunterLeggings(ServerPlayerEntity player) {
        var chest = player.getInventory().getArmorStack(2);
        if (chest.isEmpty() || !chest.isOf(Items.CHAINMAIL_CHESTPLATE)) return;
        if (!chest.contains(DataComponentTypes.CUSTOM_NAME)) return;
        if (!chest.get(DataComponentTypes.CUSTOM_NAME).getString().contains("Plastron du chasseur")) return;

        player.addStatusEffect(new StatusEffectInstance(
            StatusEffects.SLOW_FALLING, 15, 0, true, false, false));
    }

    private static boolean hasVoyageurLeggings(ServerPlayerEntity player) {
        var legs = player.getInventory().getArmorStack(1);
        if (legs.isEmpty() || !legs.isOf(Items.LEATHER_LEGGINGS)) return false;
        if (!legs.contains(DataComponentTypes.CUSTOM_NAME)) return false;
        return legs.get(DataComponentTypes.CUSTOM_NAME).getString().contains("Jambière du voyageur");
    }

    private static void handleVoyageurLeggings(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        boolean lowHealth = player.getHealth() <= 4.0f;
        boolean hasLegs = hasVoyageurLeggings(player);
        var attr = player.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED);

        if (hasLegs && lowHealth) {
            if (voyageurSpeedSet.add(uuid) && attr != null) {
                attr.addPersistentModifier(new EntityAttributeModifier(VOYAGEUR_SPEED_ID, 0.5, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            }
        } else if (voyageurSpeedSet.remove(uuid) && attr != null) {
            attr.removeModifier(VOYAGEUR_SPEED_ID);
        }
    }

    private static void handleDungeonFood(ServerPlayerEntity player) {
        boolean inDungeon = false;
        for (DungeonData d : dungeons) {
            if (!d.worldKey.equals(player.getWorld().getRegistryKey().getValue())) continue;
            int px = player.getBlockX(), pz = player.getBlockZ();
            int halfGrid = 320;
            if (px >= d.origin.getX() - halfGrid && px < d.origin.getX() + halfGrid
                && pz >= d.origin.getZ() - halfGrid && pz < d.origin.getZ() + halfGrid) {
                inDungeon = true; break;
            }
        }
        UUID uuid = player.getUuid();
        if (inDungeon) {
            player.getHungerManager().setFoodLevel(17);
            player.getHungerManager().setSaturationLevel(5.0f);
            if (healthSet.add(uuid)) {
                var attr = player.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.MAX_HEALTH);
                if (attr != null) {
                    double ratio = player.getHealth() / attr.getBaseValue();
                    attr.setBaseValue(10.0);
                    player.setHealth((float)(ratio * 10.0));
                }
            }
            BlockPos spawnTarget = lastAnchorSpawn.getOrDefault(uuid, lastDepartPos);
            if (spawnTarget != null) {
                player.setSpawnPoint(net.minecraft.registry.RegistryKey.of(
                    net.minecraft.registry.RegistryKeys.WORLD,
                    player.getWorld().getRegistryKey().getValue()),
                    spawnTarget, 0.0f, true, false);
            }
        } else if (healthSet.remove(uuid)) {
            var attr = player.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.MAX_HEALTH);
            if (attr != null) {
                double ratio = player.getHealth() / attr.getBaseValue();
                attr.setBaseValue(20.0);
                player.setHealth((float)(ratio * 20.0));
            }
        }
    }

    private static void guideGoblinsHome(net.minecraft.server.MinecraftServer server) {
        for (ServerWorld world : server.getWorlds()) {
            for (java.util.Map.Entry<UUID, BlockPos> entry : zombieSpawns.entrySet()) {
                net.minecraft.entity.Entity entity = world.getEntity(entry.getKey());
                if (!(entity instanceof net.minecraft.entity.mob.ZombieEntity zombie)) continue;
                BlockPos spawn = entry.getValue();

                var attr = zombie.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.FOLLOW_RANGE);
                var target = zombie.getTarget();

                // Determine follow ranges based on skull
                double idleRange, chaseRange;
                if (target instanceof ServerPlayerEntity sp && hasSkull(sp)) {
                    idleRange = 4.0;
                    chaseRange = 6.0;
                } else {
                    idleRange = 5.0;
                    chaseRange = 15.0;
                }

                if (target != null) {
                    // Prone hunters are invisible to goblins
                    if (target instanceof ServerPlayerEntity sp && isHunterProne(sp)) {
                        zombie.setTarget(null);
                    }
                    if (zombie.getTarget() == null) {
                        if (attr != null) attr.setBaseValue(idleRange);
                        // Return to spawn if too far
                        if (zombie.squaredDistanceTo(spawn.getX(), spawn.getY(), spawn.getZ()) > 25.0) {
                            var nav = zombie.getNavigation();
                            if (nav != null) nav.startMovingTo(spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5, 1.0);
                        }
                    } else {
                        if (attr != null) attr.setBaseValue(chaseRange);
                        if (target.squaredDistanceTo(spawn.getX(), spawn.getY(), spawn.getZ()) > chaseRange * chaseRange * 4) {
                            zombie.setTarget(null);
                            if (attr != null) attr.setBaseValue(idleRange);
                        }
                    }
                } else {
                    if (attr != null) attr.setBaseValue(idleRange);
                    // Return to spawn if too far
                    if (zombie.squaredDistanceTo(spawn.getX(), spawn.getY(), spawn.getZ()) > 25.0) {
                        var nav = zombie.getNavigation();
                        if (nav != null) nav.startMovingTo(spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5, 1.0);
                    }
                }
            }
        }
    }

    public static boolean isInDungeon(BlockPos pos, DungeonData d, ServerWorld world) {
        if (!d.worldKey.equals(world.getRegistryKey().getValue())) return false;
        return pos.getX() >= d.origin.getX() && pos.getX() < d.origin.getX() + d.gridW * 10 + 10
            && pos.getZ() >= d.origin.getZ() && pos.getZ() < d.origin.getZ() + d.gridH * 10 + 10
            && pos.getY() >= d.origin.getY() && pos.getY() < d.origin.getY() + 20;
    }

    private static final java.util.Map<java.util.UUID, Long> flechePause = new java.util.HashMap<>();
    private static final java.util.Map<java.util.UUID, Long> holyWaterTimers = new java.util.HashMap<>();
    private static final java.util.Map<java.util.UUID, Long> holyWaterLastHeal = new java.util.HashMap<>();

    private static void checkFlecheTimers(ServerPlayerEntity player, long now) {
        java.util.UUID uid = player.getUuid();
        boolean isUsingFleche = player.isUsingItem() && com.dungeonmod.util.FlecheComboData.isFleche(player.getActiveItem());
        var activeStack = player.getActiveItem();

        // Pause ou reprise du timer Sang
        if (isUsingFleche) {
            if (!flechePause.containsKey(uid)) {
                flechePause.put(uid, now); // début de la pause
            }
        } else {
            Long pauseStart = flechePause.remove(uid);
            if (pauseStart != null) {
                long pausedMs = now - pauseStart;
                // Décaler la date d'expiration du temps de pause
                for (int i = 0; i < player.getInventory().size(); i++) {
                    var stack = player.getInventory().getStack(i);
                    if (com.dungeonmod.util.FlecheComboData.isFleche(stack)) {
                        com.dungeonmod.util.FlecheComboData.extendEndTime(stack, pausedMs);
                    }
                }
            }
        }

        // Vérifier expiration (sauf pour la flèche active)
        for (int i = 0; i < player.getInventory().size(); i++) {
            var stack = player.getInventory().getStack(i);
            if (!com.dungeonmod.util.FlecheComboData.isFleche(stack)) continue;
            if (isUsingFleche && stack == activeStack) continue;
            com.dungeonmod.util.FlecheComboData.checkExpired(stack);
        }
    }

    private static void processHolyWater(net.minecraft.server.MinecraftServer server) {
        long now = System.currentTimeMillis();
        var it2 = holyWaterTimers.entrySet().iterator();
        while (it2.hasNext()) {
            var entry = it2.next();
            UUID uuid = entry.getKey();
            long expireAt = entry.getValue();
            if (now >= expireAt) { it2.remove(); holyWaterLastHeal.remove(uuid); continue; }
            Long lastHeal = holyWaterLastHeal.get(uuid);
            if (lastHeal != null && now - lastHeal < 1000) continue;
            for (ServerWorld world : server.getWorlds()) {
                net.minecraft.entity.player.PlayerEntity player = world.getPlayerByUuid(uuid);
                if (player == null || !player.isAlive()) { it2.remove(); holyWaterLastHeal.remove(uuid); break; }
                player.heal(1.0f);
                holyWaterLastHeal.put(uuid, now);
                break;
            }
        }
    }

    private static void processIceDot(net.minecraft.server.MinecraftServer server) {
        long now = System.currentTimeMillis();
        processDotMap(com.dungeonmod.util.BaguetteData.iceDot, com.dungeonmod.util.BaguetteData.iceDotLastDamage, server, now,
            net.minecraft.particle.ParticleTypes.SNOWFLAKE);
        processDotMap(com.dungeonmod.util.BaguetteData.darkDot, com.dungeonmod.util.BaguetteData.darkDotLastDamage, server, now,
            net.minecraft.particle.ParticleTypes.HAPPY_VILLAGER);
    }

    private static void processDotMap(java.util.Map<java.util.UUID, Long> dotMap,
                                       java.util.Map<java.util.UUID, Long> lastDmgMap,
                                       net.minecraft.server.MinecraftServer server, long now,
                                       net.minecraft.particle.ParticleEffect particle) {
        var it = dotMap.entrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            UUID eUuid = entry.getKey();
            long expireAt = entry.getValue();
            if (now >= expireAt) { it.remove(); lastDmgMap.remove(eUuid); continue; }
            Long lastDmg = lastDmgMap.get(eUuid);
            if (lastDmg != null && now - lastDmg < 1000) continue;
            for (ServerWorld world : server.getWorlds()) {
                Entity entity = world.getEntity(eUuid);
                if (entity == null || !entity.isAlive()) { it.remove(); lastDmgMap.remove(eUuid); break; }
                if (entity instanceof net.minecraft.entity.LivingEntity living) {
                    living.damage(world, living.getDamageSources().magic(), 2.0f);
                    lastDmgMap.put(eUuid, now);
                }
                world.spawnParticles(particle, entity.getX(), entity.getY() + entity.getHeight() * 0.8, entity.getZ(), 2, 0.4, 0.3, 0.4, 0.03);
                break;
            }
        }
    }

    private static void spawnBoneStars(net.minecraft.server.MinecraftServer server) {
        long gameTime = server.getOverworld().getTime();
        for (ServerWorld world : server.getWorlds()) {
            var it = stunnedEntities.iterator();
            while (it.hasNext()) {
                UUID uuid = it.next();
                Entity entity = world.getEntity(uuid);
                if (entity == null || !entity.isAlive()) {
                    it.remove();
                    continue;
                }
                if (entity instanceof LivingEntity living && living.hasStatusEffect(StatusEffects.SLOWNESS)) {
                    var pos = entity.getPos();
                    double y = pos.y + entity.getHeight() + 0.5;
                    for (int i = 0; i < 4; i++) {
                        double angle = (gameTime * 0.3 + i * Math.PI / 2) % (Math.PI * 2);
                        double radius = 0.5;
                        double px = pos.x + Math.cos(angle) * radius;
                        double pz = pos.z + Math.sin(angle) * radius;
                        double py = y + Math.sin(gameTime * 0.2 + i) * 0.15;
                        world.spawnParticles(net.minecraft.particle.ParticleTypes.FIREWORK, px, py, pz, 1, 0.0, 0.0, 0.0, 0.0);
                    }
                } else {
                    it.remove();
                }
            }
        }
    }

    public static void addDungeon(String name, BlockPos origin, int gridW, int gridH, Identifier worldKey, int entryRotation, List<BlockPos> extraPositions) {
        dungeons.add(new DungeonData(name, origin, gridW, gridH, worldKey, entryRotation, extraPositions));
    }

    public static void removeDungeon(String name) {
        dungeons.removeIf(d -> d.name.equals(name));
    }

    public static DungeonData getDungeon(String name) {
        for (DungeonData d : dungeons) {
            if (d.name.equals(name)) return d;
        }
        return null;
    }

    public static class DungeonData {
        public String name;
        public BlockPos origin;
        public int gridW, gridH;
        public Identifier worldKey;
        public int entryRotation;
        public List<BlockPos> extraPositions;

        public DungeonData(String name, BlockPos origin, int gridW, int gridH, Identifier worldKey, int entryRotation, List<BlockPos> extraPositions) {
            this.name = name;
            this.origin = origin;
            this.gridW = gridW;
            this.gridH = gridH;
            this.worldKey = worldKey;
            this.entryRotation = entryRotation;
            this.extraPositions = extraPositions != null ? extraPositions : new ArrayList<>();
        }
    }
}
