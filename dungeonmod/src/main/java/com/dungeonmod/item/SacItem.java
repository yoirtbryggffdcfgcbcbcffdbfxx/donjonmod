package com.dungeonmod.item;

import com.dungeonmod.screen.ModScreenHandlers;
import com.dungeonmod.screen.SacScreenHandler;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class SacItem extends Item {

    private static final int SAC_SIZE = 4;
    private static final Text TITLE = Text.literal("Sac");

    public SacItem(Settings settings) {
        super(settings);
    }

    @Override
    public Text getName(ItemStack stack) {
        return Text.literal("§6Sac");
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (!world.isClient()) {
            user.openHandledScreen(new NamedScreenHandlerFactory() {
                @Override
                public Text getDisplayName() {
                    return TITLE;
                }

                @Override
                public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
                    ContainerComponent container = stack.getOrDefault(DataComponentTypes.CONTAINER, ContainerComponent.DEFAULT);
                    SimpleInventory inventory = new SimpleInventory(SAC_SIZE) {
                        @Override
                        public void markDirty() {
                            super.markDirty();
                            List<ItemStack> stacks = new ArrayList<>();
                            for (int i = 0; i < this.size(); i++) {
                                stacks.add(this.getStack(i));
                            }
                            net.minecraft.util.collection.DefaultedList<ItemStack> defaulted = net.minecraft.util.collection.DefaultedList.ofSize(SAC_SIZE, ItemStack.EMPTY);
                            for (int i = 0; i < SAC_SIZE; i++) defaulted.set(i, stacks.get(i));
                            stack.set(DataComponentTypes.CONTAINER, ContainerComponent.fromStacks(defaulted));
                        }
                    };

                    net.minecraft.util.collection.DefaultedList<ItemStack> storedStacks = net.minecraft.util.collection.DefaultedList.ofSize(SAC_SIZE, ItemStack.EMPTY);
                    container.copyTo(storedStacks);
                    for (int i = 0; i < Math.min(storedStacks.size(), SAC_SIZE); i++) {
                        inventory.setStack(i, storedStacks.get(i));
                    }

                    return new SacScreenHandler(syncId, playerInventory, inventory);
                }
            });
        }

        return ActionResult.SUCCESS;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.literal("§7Ouvre un inventaire de 4 slots."));
        tooltip.add(Text.literal("§7Les items ne sont pas perdus à la mort."));
    }
}
