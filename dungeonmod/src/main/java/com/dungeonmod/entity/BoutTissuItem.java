package com.dungeonmod.entity;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

public class BoutTissuItem extends Item {
    public BoutTissuItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
        if (entity instanceof OgreEntity ogre && ogre.getPhase() == 4 && ogre.deathStage == 3) {
            return use(user.getWorld(), user, hand);
        }
        return ActionResult.PASS;
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if (world.isClient) return ActionResult.SUCCESS;

        OgreEntity cyclops = null;
        for (var e : world.getOtherEntities(user, user.getBoundingBox().expand(10.0))) {
            if (e instanceof OgreEntity ogre && ogre.getPhase() == 4 && ogre.deathStage == 3) {
                cyclops = ogre;
                break;
            }
        }

        if (cyclops == null) return ActionResult.FAIL;

        String rewardId = cyclops.getRandomUngivenReward();
        if (rewardId == null) {
            user.sendMessage(Text.literal("§7Le cyclope a déjà tout donné..."), true);
            return ActionResult.FAIL;
        }

        ItemStack reward = cyclops.createReward(rewardId);
        if (reward.isEmpty()) {

            return ActionResult.FAIL;
        }

        user.giveItemStack(reward.copy());
        cyclops.markItemGiven(rewardId);
        user.getStackInHand(hand).decrement(1);

        return ActionResult.CONSUME;
    }
}
