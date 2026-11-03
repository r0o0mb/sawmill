package com.lesopilka.sawmill.screen;

import com.lesopilka.sawmill.block.entity.SawmillBlockEntity;
import com.lesopilka.sawmill.registry.ModBlockEntities;
import com.lesopilka.sawmill.upgrade.UpgradeItem;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public class SawmillMenu extends AbstractContainerMenu {
    public static final int FUEL_SLOT = SawmillBlockEntity.FUEL_SLOT;
    public static final int INPUT_SLOT = SawmillBlockEntity.INPUT_SLOT;
    public static final int OUTPUT_SLOT = SawmillBlockEntity.OUTPUT_SLOT;

    private final Container container;
    private final ContainerData data;

    public SawmillMenu(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainer(SawmillBlockEntity.INVENTORY_SIZE), new SimpleContainerData(SawmillBlockEntity.DATA_COUNT));
    }

    public SawmillMenu(int syncId, Inventory playerInventory, Container container, ContainerData data) {
        super(ModBlockEntities.SAWMILL_MENU, syncId);
        this.container = container;
        this.data = data;

        this.addDataSlots(data);

        this.addSlot(new Slot(container, FUEL_SLOT, 56, 53) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return container.canPlaceItem(FUEL_SLOT, stack);
            }
        });

        this.addSlot(new Slot(container, INPUT_SLOT, 56, 17) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return container.canPlaceItem(INPUT_SLOT, stack);
            }
        });

        this.addSlot(new Slot(container, OUTPUT_SLOT, 116, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        for (int i = 0; i < 3; i++) {
            int slotIndex = SawmillBlockEntity.UPGRADE_SLOT_START + i;
            int x = 8 + i * 18;
            this.addSlot(new Slot(container, slotIndex, x, 35) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return stack.getItem() instanceof UpgradeItem;
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }
            });
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, 9 + row * 9 + col, 8 + col * 18, 84 + row * 18));
            }
        }

        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();

        int containerEnd = SawmillBlockEntity.INVENTORY_SIZE;
        int playerInvStart = containerEnd;
        int playerInvEnd = playerInvStart + 27;
        int hotbarEnd = playerInvEnd + 9;

        if (index < playerInvStart) {

            if (!this.moveItemStackTo(stack, playerInvStart, hotbarEnd, true)) {
                return ItemStack.EMPTY;
            }
        } else {

            if (stack.getItem() instanceof UpgradeItem) {
                if (!this.moveItemStackTo(stack, SawmillBlockEntity.UPGRADE_SLOT_START, containerEnd, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (this.isFuel(stack)) {
                if (!this.moveItemStackTo(stack, FUEL_SLOT, INPUT_SLOT, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (this.isLog(stack)) {
                if (!this.moveItemStackTo(stack, INPUT_SLOT, OUTPUT_SLOT, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index < hotbarEnd) {
                if (!this.moveItemStackTo(stack, playerInvStart, playerInvEnd, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(stack, playerInvEnd, hotbarEnd, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    private boolean isFuel(ItemStack stack) {
        return container.canPlaceItem(FUEL_SLOT, stack);
    }

    private boolean isLog(ItemStack stack) {
        return container.canPlaceItem(INPUT_SLOT, stack);
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }

    public int getBurnTime() {
        return this.data.get(SawmillBlockEntity.DATA_BURN_TIME);
    }

    public int getBurnTimeTotal() {
        return this.data.get(SawmillBlockEntity.DATA_BURN_TIME_TOTAL);
    }

    public int getProgress() {
        return this.data.get(SawmillBlockEntity.DATA_PROGRESS);
    }

    public int getProcessTime() {
        return this.data.get(SawmillBlockEntity.DATA_PROCESS_TIME);
    }

    public boolean isJammed() {
        return this.data.get(SawmillBlockEntity.DATA_JAMMED) != 0;
    }
}
