package com.lesopilka.sawmill.item;

import com.lesopilka.sawmill.block.entity.Repairable;
import com.lesopilka.sawmill.config.SawmillConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.item.context.UseOnContext;
import org.jspecify.annotations.Nullable;

public class WrenchItem extends Item {

    public WrenchItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        BlockEntity be = level.getBlockEntity(pos);

        if (be instanceof Repairable machine) {
            if (level.isClientSide()) {
                return machine.isJammed() ? InteractionResult.SUCCESS : InteractionResult.PASS;
            }
            if (!machine.isJammed()) {
                return InteractionResult.PASS;
            }

            if (player != null && !player.getAbilities().instabuild) {
                if (!consumeIron(player)) {
                    player.sendSystemMessage(Component.translatable("message.sawmill.wrench_no_iron"));
                    return InteractionResult.FAIL;
                }
            }

            machine.repair(player);
            level.playSound(null, pos, SoundEvents.DECORATED_POT_INSERT, SoundSource.BLOCKS, 0.8f, 1.2f);
            if (player != null) {
                player.sendSystemMessage(Component.translatable("message.sawmill.repaired"));
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    private boolean consumeIron(Player player) {
        int needed = SawmillConfig.WRENCH_REPAIR_COST;

        for (InteractionHand hand : new InteractionHand[]{ InteractionHand.OFF_HAND, InteractionHand.MAIN_HAND }) {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.is(Items.IRON_INGOT)) {
                stack.shrink(needed);
                if (stack.isEmpty()) {
                    player.setItemInHand(hand, ItemStack.EMPTY);
                }
                return true;
            }
        }
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(Items.IRON_INGOT)) {
                stack.shrink(needed);
                if (stack.isEmpty()) {
                    player.getInventory().setItem(i, ItemStack.EMPTY);
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                java.util.function.Consumer<Component> builder, TooltipFlag flag) {
        builder.accept(Component.translatable("tooltip.sawmill.wrench"));
        super.appendHoverText(stack, context, display, builder, flag);
    }
}
