package com.lesopilka.sawmill.screen;

import com.lesopilka.sawmill.block.entity.IncubatorBlockEntity;
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

public class IncubatorMenu extends AbstractContainerMenu {
    public static final int FUEL_SLOT = IncubatorBlockEntity.FUEL_SLOT;
    public static final int SAPLING_SLOT = IncubatorBlockEntity.SAPLING_SLOT;
    public static final int CATALYST_SLOT = IncubatorBlockEntity.CATALYST_SLOT;
    public static final int LOG_OUTPUT_SLOT = IncubatorBlockEntity.LOG_OUTPUT_SLOT;
    public static final int SAPLING_OUTPUT_SLOT = IncubatorBlockEntity.SAPLING_OUTPUT_SLOT;

    private final Container container;
    private final ContainerData data;

    public IncubatorMenu(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainer(IncubatorBlockEntity.INVENTORY_SIZE), new SimpleContainerData(IncubatorBlockEntity.DATA_COUNT));
    }

    public IncubatorMenu(int syncId, Inventory playerInventory, Container container, ContainerData data) {
        super(ModBlockEntities.INCUBATOR_MENU, syncId);
        this.container = container;
        this.data = data;

        this.addDataSlots(data);

        this.addSlot(new Slot(container, FUEL_SLOT, 56, 53) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return container.canPlaceItem(FUEL_SLOT, stack);
            }
        });

        this.addSlot(new Slot(container, SAPLING_SLOT, 56, 17) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return container.canPlaceItem(SAPLING_SLOT, stack);
            }
        });

        this.addSlot(new Slot(container, CATALYST_SLOT, 38, 17) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return container.canPlaceItem(CATALYST_SLOT, stack);
            }
        });

        this.addSlot(new Slot(container, LOG_OUTPUT_SLOT, 116, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        this.addSlot(new Slot(container, SAPLING_OUTPUT_SLOT, 134, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        for (int i = 0; i < 3; i++) {
            int slotIndex = IncubatorBlockEntity.UPGRADE_SLOT_START + i;
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

        int containerEnd = IncubatorBlockEntity.INVENTORY_SIZE;
        int playerInvStart = containerEnd;
        int playerInvEnd = playerInvStart + 27;
        int hotbarEnd = playerInvEnd + 9;

        if (index < playerInvStart) {

            if (!this.moveItemStackTo(stack, playerInvStart, hotbarEnd, true)) {
                return ItemStack.EMPTY;
            }
        } else {

            if (stack.getItem() instanceof UpgradeItem) {
                if (!this.moveItemStackTo(stack, IncubatorBlockEntity.UPGRADE_SLOT_START, containerEnd, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (stack.is(net.minecraft.world.item.Items.BONE_MEAL)) {
                if (!this.moveItemStackTo(stack, CATALYST_SLOT, LOG_OUTPUT_SLOT, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (this.isFuel(stack)) {
                if (!this.moveItemStackTo(stack, FUEL_SLOT, SAPLING_SLOT, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (this.isSapling(stack)) {
                if (!this.moveItemStackTo(stack, SAPLING_SLOT, CATALYST_SLOT, false)) {
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

    private boolean isSapling(ItemStack stack) {
        return container.canPlaceItem(SAPLING_SLOT, stack);
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }

    public int getBurnTime() {
        return this.data.get(IncubatorBlockEntity.DATA_BURN_TIME);
    }

    public int getBurnTimeTotal() {
        return this.data.get(IncubatorBlockEntity.DATA_BURN_TIME_TOTAL);
    }

    public int getProgress() {
        return this.data.get(IncubatorBlockEntity.DATA_PROGRESS);
    }

    public int getProcessTime() {
        return this.data.get(IncubatorBlockEntity.DATA_PROCESS_TIME);
    }

    public boolean isJammed() {
        return this.data.get(IncubatorBlockEntity.DATA_JAMMED) != 0;
    }
}
