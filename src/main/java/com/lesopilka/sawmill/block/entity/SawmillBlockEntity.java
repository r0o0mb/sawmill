package com.lesopilka.sawmill.block.entity;

import com.lesopilka.sawmill.SawmillMod;
import com.lesopilka.sawmill.block.SawmillBlock;
import com.lesopilka.sawmill.config.SawmillConfig;
import com.lesopilka.sawmill.registry.ModBlockEntities;
import com.lesopilka.sawmill.upgrade.UpgradeItem;
import com.lesopilka.sawmill.upgrade.UpgradeType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import java.util.Map;

public class SawmillBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer, Repairable {

    public static final int INVENTORY_SIZE = 6;
    public static final int FUEL_SLOT = 0;
    public static final int INPUT_SLOT = 1;
    public static final int OUTPUT_SLOT = 2;
    public static final int UPGRADE_SLOT_START = 3;

    private static final Map<Item, Item> LOG_TO_PLANKS = Map.ofEntries(
            Map.entry(Items.OAK_LOG, Items.OAK_PLANKS),
            Map.entry(Items.SPRUCE_LOG, Items.SPRUCE_PLANKS),
            Map.entry(Items.BIRCH_LOG, Items.BIRCH_PLANKS),
            Map.entry(Items.JUNGLE_LOG, Items.JUNGLE_PLANKS),
            Map.entry(Items.ACACIA_LOG, Items.ACACIA_PLANKS),
            Map.entry(Items.DARK_OAK_LOG, Items.DARK_OAK_PLANKS),
            Map.entry(Items.MANGROVE_LOG, Items.MANGROVE_PLANKS),
            Map.entry(Items.CHERRY_LOG, Items.CHERRY_PLANKS),
            Map.entry(Items.PALE_OAK_LOG, Items.PALE_OAK_PLANKS),
            Map.entry(Items.BAMBOO_BLOCK, Items.BAMBOO_PLANKS),
            Map.entry(Items.CRIMSON_STEM, Items.CRIMSON_PLANKS),
            Map.entry(Items.WARPED_STEM, Items.WARPED_PLANKS)
    );

    private NonNullList<ItemStack> items = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY);

    private int burnTime;

    private int burnTimeTotal;

    private int progress;

    private int processTime = SawmillConfig.BASE_PROCESS_TICKS;

    private long nextAllowedTick;

    private boolean jammed;

    private int maintenanceTicks;

    private int autoPushTimer;

    private int autoUnjamTimer;

    public static final int DATA_BURN_TIME = 0;
    public static final int DATA_BURN_TIME_TOTAL = 1;
    public static final int DATA_PROGRESS = 2;
    public static final int DATA_PROCESS_TIME = 3;
    public static final int DATA_JAMMED = 4;
    public static final int DATA_COUNT = 5;

    public final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_BURN_TIME -> burnTime;
                case DATA_BURN_TIME_TOTAL -> burnTimeTotal;
                case DATA_PROGRESS -> progress;
                case DATA_PROCESS_TIME -> processTime;
                case DATA_JAMMED -> jammed ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {

            switch (index) {
                case DATA_BURN_TIME -> burnTime = value;
                case DATA_BURN_TIME_TOTAL -> burnTimeTotal = value;
                case DATA_PROGRESS -> progress = value;
                case DATA_PROCESS_TIME -> processTime = value;
                case DATA_JAMMED -> jammed = value != 0;
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public SawmillBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SAWMILL_BE, pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, SawmillBlockEntity blockEntity) {
        boolean changed = false;

        if (blockEntity.jammed
                && blockEntity.hasUpgrade(UpgradeType.SELF_DIAGNOSIS)
                && blockEntity.hasUpgrade(UpgradeType.AUTOMATION_2)) {
            if (++blockEntity.autoUnjamTimer >= SawmillConfig.AUTO_UNJAM_INTERVAL) {
                blockEntity.autoUnjamTimer = 0;
                if (blockEntity.consumeIronForSelfRepair()) {
                    blockEntity.jammed = false;
                    blockEntity.maintenanceTicks = SawmillConfig.MAINTENANCE_GRACE_TICKS;
                    SawmillMod.LOGGER.debug("Sawmill at {} self-repaired", pos);
                }
            }
        }

        if (blockEntity.hasUpgrade(UpgradeType.AUTOMATION_2)
                && ++blockEntity.autoPushTimer >= SawmillConfig.AUTO_PUSH_INTERVAL) {
            blockEntity.autoPushTimer = 0;
            if (blockEntity.pushOutput()) {
                changed = true;
            }
        }

        if (!level.isClientSide()) {
            boolean wasLit = state.getValue(SawmillBlock.LIT);

            if (blockEntity.burnTime > 0) {
                blockEntity.burnTime--;
            }

            boolean canProcess = blockEntity.canStartProcessing();

            if (blockEntity.burnTime <= 0 && canProcess) {
                changed |= blockEntity.consumeFuel();
            }

            if (blockEntity.burnTime > 0 && canProcess) {
                if (blockEntity.progress == 0) {

                    blockEntity.processTime = blockEntity.calcProcessTicks();
                    blockEntity.nextAllowedTick = level.getGameTime() + SawmillConfig.RATE_LIMIT_INTERVAL;
                }
                blockEntity.progress++;

                if (blockEntity.progress % SawmillConfig.WORK_SOUND_INTERVAL == 0) {
                    level.playSound(null, pos, SoundEvents.WOOD_HIT, SoundSource.BLOCKS, 0.6f, 1.1f);
                }
                if (blockEntity.progress >= blockEntity.processTime) {
                    blockEntity.finishLog(level);
                    changed = true;
                }
            } else if (blockEntity.progress > 0) {

                blockEntity.progress = 0;
                changed = true;
            }

            boolean lit = blockEntity.burnTime > 0;
            if (lit != wasLit || state.getValue(SawmillBlock.CLOGGED) != blockEntity.jammed) {
                level.setBlock(pos, state
                        .setValue(SawmillBlock.LIT, lit)
                        .setValue(SawmillBlock.CLOGGED, blockEntity.jammed), 3);
            }

            if (blockEntity.maintenanceTicks > 0) {
                blockEntity.maintenanceTicks--;
            }
        }

        if (changed) {
            blockEntity.setChanged();
        }
    }

    private boolean canStartProcessing() {
        if (jammed) {
            return false;
        }

        if (progress == 0 && level != null && level.getGameTime() < nextAllowedTick) {
            return false;
        }
        return isLog(items.get(INPUT_SLOT))
                && canOutputAccept();
    }

    private boolean canOutputAccept() {
        ItemStack out = items.get(OUTPUT_SLOT);
        if (out.isEmpty()) {
            return true;
        }
        Item planks = planksFor(items.get(INPUT_SLOT));
        return out.is(planks) && out.getCount() + SawmillConfig.PLANKS_PER_LOG <= out.getMaxStackSize();
    }

    private int calcProcessTicks() {
        int ticks = SawmillConfig.BASE_PROCESS_TICKS;
        for (int i = 0; i < speedUpgradeCount(); i++) {
            ticks = (int) Math.max(SawmillConfig.MIN_PROCESS_TICKS, Math.round(ticks * SawmillConfig.SPEED_LEVEL_MULTIPLIER));
        }
        return Math.max(SawmillConfig.MIN_PROCESS_TICKS, ticks);
    }

    private void finishLog(Level lvl) {
        ItemStack input = items.get(INPUT_SLOT);
        Item planks = planksFor(input);
        int amount = SawmillConfig.PLANKS_PER_LOG;
        net.minecraft.util.RandomSource random = lvl.getRandom();

        double bonusChance = SawmillConfig.BONUS_CHANCE_PER_LEVEL * efficiencyUpgradeCount();
        if (random.nextDouble() < Math.min(bonusChance, 0.9)) {
            amount *= 2;
        }

        ItemStack out = items.get(OUTPUT_SLOT);
        if (out.isEmpty()) {
            items.set(OUTPUT_SLOT, new ItemStack(planks, amount));
        } else if (out.is(planks)) {
            out.grow(amount);
        }

        lvl.playSound(null, worldPosition, SoundEvents.UI_STONECUTTER_TAKE_RESULT,
                SoundSource.BLOCKS, 0.7f, 1.1f);

        input.shrink(1);
        if (input.isEmpty()) {
            items.set(INPUT_SLOT, ItemStack.EMPTY);
        }

        if (maintenanceTicks <= 0 && random.nextDouble() < SawmillConfig.JAM_CHANCE) {
            jammed = true;

            lvl.playSound(null, worldPosition, SoundEvents.ANVIL_LAND,
                    SoundSource.BLOCKS, 0.4f, 1.5f);
        }

        progress = 0;
    }

    private boolean consumeFuel() {
        ItemStack fuel = items.get(FUEL_SLOT);
        if (fuel.isEmpty()) {
            return false;
        }
        int ticks = fuelTicks(fuel);
        if (ticks <= 0) {
            return false;
        }
        burnTime = ticks;
        burnTimeTotal = ticks;
        if (fuel.is(Items.LAVA_BUCKET)) {
            items.set(FUEL_SLOT, new ItemStack(Items.BUCKET));
        } else {
            fuel.shrink(1);
            if (fuel.isEmpty()) {
                items.set(FUEL_SLOT, ItemStack.EMPTY);
            }
        }
        return true;
    }

    private static int fuelTicks(ItemStack stack) {
        if (stack.is(Items.COAL) || stack.is(Items.CHARCOAL)) {
            return SawmillConfig.FUEL_COAL;
        }
        if (stack.is(Items.LAVA_BUCKET)) {
            return SawmillConfig.FUEL_LAVA_BUCKET;
        }
        if (stack.getItem().builtInRegistryHolder().is(net.minecraft.tags.ItemTags.PLANKS)) {
            return SawmillConfig.FUEL_PLANKS;
        }
        return 0;
    }

    private boolean consumeIronForSelfRepair() {
        ItemStack fuel = items.get(FUEL_SLOT);
        if (fuel.is(Items.IRON_INGOT) && fuel.getCount() >= SawmillConfig.SELF_DIAG_IRON_COST) {
            fuel.shrink(SawmillConfig.SELF_DIAG_IRON_COST);
            if (fuel.isEmpty()) {
                items.set(FUEL_SLOT, ItemStack.EMPTY);
            }
            return true;
        }
        return false;
    }

    public int countUpgrades(UpgradeType type) {
        int count = 0;
        for (int i = UPGRADE_SLOT_START; i < INVENTORY_SIZE; i++) {
            if (UpgradeItem.is(items.get(i), type)) {
                count++;
            }
        }
        return count;
    }

    public boolean hasUpgrade(UpgradeType type) {
        return countUpgrades(type) > 0;
    }

    private int speedUpgradeCount() {
        return countUpgrades(UpgradeType.SPEED);
    }

    private int efficiencyUpgradeCount() {
        return countUpgrades(UpgradeType.EFFICIENCY);
    }

    public boolean isJammed() {
        return jammed;
    }

    public void repair(@Nullable Player player) {
        jammed = false;
        maintenanceTicks = SawmillConfig.MAINTENANCE_GRACE_TICKS;
        setChanged();
    }

    private boolean pushOutput() {
        if (level == null) {
            return false;
        }
        ItemStack out = items.get(OUTPUT_SLOT);
        if (out.isEmpty()) {
            return false;
        }
        for (Direction side : Direction.values()) {
            if (level.getBlockEntity(worldPosition.relative(side)) instanceof net.minecraft.world.Container target) {
                ItemStack leftover = com.lesopilka.sawmill.util.InventoryHelper.insertStack(target, out);
                if (leftover.getCount() < out.getCount()) {
                    items.set(OUTPUT_SLOT, leftover);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {

        if (!hasUpgrade(UpgradeType.AUTOMATION_1)) {
            return new int[0];
        }
        return new int[]{ FUEL_SLOT, INPUT_SLOT, OUTPUT_SLOT };
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction dir) {
        if (slot == FUEL_SLOT) {
            return fuelTicks(stack) > 0 || stack.is(Items.IRON_INGOT);
        }
        if (slot == INPUT_SLOT) {
            return isLog(stack);
        }
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction dir) {
        return slot == OUTPUT_SLOT;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case FUEL_SLOT -> fuelTicks(stack) > 0 || stack.is(Items.IRON_INGOT);
            case INPUT_SLOT -> isLog(stack);
            case UPGRADE_SLOT_START, UPGRADE_SLOT_START + 1, UPGRADE_SLOT_START + 2 ->
                    stack.getItem() instanceof UpgradeItem;
            default -> true;
        };
    }

    private static boolean isLog(ItemStack stack) {
        return LOG_TO_PLANKS.containsKey(stack.getItem());
    }

    private static Item planksFor(ItemStack log) {
        Item planks = LOG_TO_PLANKS.get(log.getItem());
        return planks != null ? planks : Items.OAK_PLANKS;
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> list) {
        this.items = list;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.sawmill.sawmill");
    }

    @Override
    protected AbstractContainerMenu createMenu(int syncId, net.minecraft.world.entity.player.Inventory inventory) {
        return new com.lesopilka.sawmill.screen.SawmillMenu(syncId, inventory, this, this.dataAccess);
    }

    @Override
    public int getContainerSize() {
        return INVENTORY_SIZE;
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public void drops() {
        if (level != null) {
            Containers.dropContents(level, worldPosition, items);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        output.putInt("BurnTime", burnTime);
        output.putInt("BurnTimeTotal", burnTimeTotal);
        output.putInt("Progress", progress);
        output.putInt("ProcessTime", processTime);
        output.putLong("NextAllowedTick", nextAllowedTick);
        output.putBoolean("Jammed", jammed);
        output.putInt("MaintenanceTicks", maintenanceTicks);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
        burnTime = input.getIntOr("BurnTime", 0);
        burnTimeTotal = input.getIntOr("BurnTimeTotal", 0);
        progress = input.getIntOr("Progress", 0);
        processTime = input.getIntOr("ProcessTime", SawmillConfig.BASE_PROCESS_TICKS);
        nextAllowedTick = input.getLongOr("NextAllowedTick", 0);
        jammed = input.getBooleanOr("Jammed", false);
        maintenanceTicks = input.getIntOr("MaintenanceTicks", 0);
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }
}
