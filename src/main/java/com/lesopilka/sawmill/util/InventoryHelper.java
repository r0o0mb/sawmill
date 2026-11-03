package com.lesopilka.sawmill.util;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

public final class InventoryHelper {
    private InventoryHelper() {}

    public static ItemStack insertStack(Container container, ItemStack stack) {
        if (stack.isEmpty()) {
            return stack;
        }
        ItemStack remaining = stack.copy();

        for (int i = 0; i < container.getContainerSize() && !remaining.isEmpty(); i++) {
            ItemStack slot = container.getItem(i);
            if (!slot.isEmpty() && ItemStack.isSameItemSameComponents(slot, remaining)) {
                int space = slot.getMaxStackSize() - slot.getCount();
                if (space > 0) {
                    int toMove = Math.min(space, remaining.getCount());
                    slot.grow(toMove);
                    remaining.shrink(toMove);
                }
            }
        }

        for (int i = 0; i < container.getContainerSize() && !remaining.isEmpty(); i++) {
            if (container.getItem(i).isEmpty()) {
                int toMove = Math.min(container.getMaxStackSize(), remaining.getCount());
                container.setItem(i, remaining.split(toMove));
            }
        }

        container.setChanged();
        return remaining;
    }
}
