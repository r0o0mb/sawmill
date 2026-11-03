package com.lesopilka.sawmill.upgrade;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.network.chat.Component;

import java.util.List;

public class UpgradeItem extends Item {
    private final UpgradeType type;

    public UpgradeItem(Properties properties, UpgradeType type) {
        super(properties);
        this.type = type;
    }

    public UpgradeType getType() {
        return type;
    }

    public static boolean is(ItemStack stack, UpgradeType type) {
        return stack.getItem() instanceof UpgradeItem upgrade && upgrade.getType() == type;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                java.util.function.Consumer<Component> builder, TooltipFlag flag) {
        builder.accept(Component.translatable("tooltip.sawmill.upgrade." + type.name().toLowerCase()));
        super.appendHoverText(stack, context, display, builder, flag);
    }
}
