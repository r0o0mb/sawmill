package com.lesopilka.sawmill.block.entity;

import com.lesopilka.sawmill.SawmillMod;
import com.lesopilka.sawmill.block.IncubatorBlock;
import com.lesopilka.sawmill.config.IncubatorConfig;
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
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
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

public class IncubatorBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer, Repairable {
    public static final int INVENTORY_SIZE = 8;
    public static final int FUEL_SLOT = 0;
    public static final int SAPLING_SLOT = 1;
    public static final int CATALYST_SLOT = 2;
    public static final int LOG_OUTPUT_SLOT = 3;
    public static final int SAPLING_OUTPUT_SLOT = 4;
    public static final int UPGRADE_SLOT_START = 5;

    private static final Map<Item, Item[]> SAPLING_TO_OUTPUT = Map.ofEntries(
            Map.entry(Items.OAK_SAPLING, new Item[]{ Items.OAK_LOG, Items.OAK_SAPLING }),
            Map.entry(Items.SPRUCE_SAPLING, new Item[]{ Items.SPRUCE_LOG, Items.SPRUCE_SAPLING }),
            Map.entry(Items.BIRCH_SAPLING, new Item[]{ Items.BIRCH_LOG, Items.BIRCH_SAPLING }),
            Map.entry(Items.JUNGLE_SAPLING, new Item[]{ Items.JUNGLE_LOG, Items.JUNGLE_SAPLING }),
            Map.entry(Items.ACACIA_SAPLING, new Item[]{ Items.ACACIA_LOG, Items.ACACIA_SAPLING }),
            Map.entry(Items.DARK_OAK_SAPLING, new Item[]{ Items.DARK_OAK_LOG, Items.DARK_OAK_SAPLING }),
            Map.entry(Items.MANGROVE_PROPAGULE, new Item[]{ Items.MANGROVE_LOG, Items.MANGROVE_PROPAGULE }),
            Map.entry(Items.CHERRY_SAPLING, new Item[]{ Items.CHERRY_LOG, Items.CHERRY_SAPLING }),
            Map.entry(Items.PALE_OAK_SAPLING, new Item[]{ Items.PALE_OAK_LOG, Items.PALE_OAK_SAPLING }),

            Map.entry(Items.AZALEA, new Item[]{ Items.OAK_LOG, Items.OAK_SAPLING }),
            Map.entry(Items.FLOWERING_AZALEA, new Item[]{ Items.OAK_LOG, Items.OAK_SAPLING })
    );

    private NonNullList<ItemStack> items = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY);

    private int burnTime;

    private int burnTimeTotal;

    private int progress;

    private int processTime = IncubatorConfig.BASE_PROCESS_TICKS;

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

    public IncubatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.INCUBATOR_BE, pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, IncubatorBlockEntity blockEntity) {
        boolean changed = false;

        if (blockEntity.jammed
                && blockEntity.hasUpgrade(UpgradeType.SELF_DIAGNOSIS)
                && blockEntity.hasUpgrade(UpgradeType.AUTOMATION_2)) {
            if (++blockEntity.autoUnjamTimer >= IncubatorConfig.AUTO_UNJAM_INTERVAL) {
                blockEntity.autoUnjamTimer = 0;
                if (blockEntity.consumeIronForSelfRepair()) {
                    blockEntity.jammed = false;
                    blockEntity.maintenanceTicks = IncubatorConfig.MAINTENANCE_GRACE_TICKS;
                    SawmillMod.LOGGER.debug("Incubator at {} self-repaired", pos);
                }
            }
        }

        if (blockEntity.hasUpgrade(UpgradeType.AUTOMATION_2)
                && ++blockEntity.autoPushTimer >= IncubatorConfig.AUTO_PUSH_INTERVAL) {
            blockEntity.autoPushTimer = 0;
            if (blockEntity.pushOutput(LOG_OUTPUT_SLOT) | blockEntity.pushOutput(SAPLING_OUTPUT_SLOT)) {
                changed = true;
            }
        }

        if (!level.isClientSide()) {
            boolean wasLit = state.getValue(IncubatorBlock.LIT);

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
                    blockEntity.nextAllowedTick = level.getGameTime() + IncubatorConfig.RATE_LIMIT_INTERVAL;
                }
                blockEntity.progress++;

                if (blockEntity.progress % SawmillConfig.WORK_SOUND_INTERVAL == 0) {
                    level.playSound(null, pos, SoundEvents.BONE_MEAL_USE,
                            SoundSource.BLOCKS, 0.5f, 0.9f);
                }
                if (blockEntity.progress >= blockEntity.processTime) {
                    blockEntity.finishSapling(level);
                    changed = true;
                }
            } else if (blockEntity.progress > 0) {

                blockEntity.progress = 0;
                changed = true;
            }

            boolean lit = blockEntity.burnTime > 0;
            if (lit != wasLit || state.getValue(IncubatorBlock.CLOGGED) != blockEntity.jammed) {
                level.setBlock(pos, state
                        .setValue(IncubatorBlock.LIT, lit)
                        .setValue(IncubatorBlock.CLOGGED, blockEntity.jammed), 3);
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
        return isSapling(items.get(SAPLING_SLOT))
                && items.get(CATALYST_SLOT).is(Items.BONE_MEAL)
                && items.get(CATALYST_SLOT).getCount() >= IncubatorConfig.BONE_MEAL_MIN
                && canOutputsAccept();
    }

    private boolean canOutputsAccept() {
        ItemStack sapling = items.get(SAPLING_SLOT);
        Item[] out = outputsFor(sapling);
        if (out == null) {
            return false;
        }
        return canAccept(items.get(LOG_OUTPUT_SLOT), out[0], IncubatorConfig.LOGS_MIN)
                && canAccept(items.get(SAPLING_OUTPUT_SLOT), out[1], IncubatorConfig.SAPLINGS_MAX);
    }

    private static boolean canAccept(ItemStack out, Item produced, int amount) {
        if (out.isEmpty()) {
            return true;
        }
        return out.is(produced) && out.getCount() + amount <= out.getMaxStackSize();
    }

    private int calcProcessTicks() {
        int ticks = IncubatorConfig.BASE_PROCESS_TICKS;
        for (int i = 0; i < speedUpgradeCount(); i++) {
            ticks = (int) Math.max(IncubatorConfig.MIN_PROCESS_TICKS, Math.round(ticks * SawmillConfig.SPEED_LEVEL_MULTIPLIER));
        }
        return Math.max(IncubatorConfig.MIN_PROCESS_TICKS, ticks);
    }

    private void finishSapling(Level lvl) {
        ItemStack sapling = items.get(SAPLING_SLOT);
        Item[] out = outputsFor(sapling);
        if (out == null) {
            progress = 0;
            return;
        }
        RandomSource random = lvl.getRandom();

        ItemStack catalyst = items.get(CATALYST_SLOT);
        int mealCost = IncubatorConfig.BONE_MEAL_MIN + random.nextInt(
                IncubatorConfig.BONE_MEAL_MAX - IncubatorConfig.BONE_MEAL_MIN + 1);
        catalyst.shrink(Math.min(mealCost, catalyst.getCount()));
        if (catalyst.isEmpty()) {
            items.set(CATALYST_SLOT, ItemStack.EMPTY);
        }

        int logs = IncubatorConfig.LOGS_MIN + random.nextInt(
                IncubatorConfig.LOGS_MAX - IncubatorConfig.LOGS_MIN + 1);
        double bonusChance = SawmillConfig.BONUS_CHANCE_PER_LEVEL * efficiencyUpgradeCount();
        while (logs < IncubatorConfig.LOGS_HARD_CAP
                && random.nextDouble() < Math.min(bonusChance, 0.9)) {
            logs = Math.min(IncubatorConfig.LOGS_HARD_CAP, logs + 2);
        }
        addToSlot(LOG_OUTPUT_SLOT, out[0], logs);

        int saplings = random.nextInt(IncubatorConfig.SAPLINGS_MAX + 1);
        if (saplings > 0) {
            addToSlot(SAPLING_OUTPUT_SLOT, out[1], saplings);
        }

        lvl.playSound(null, worldPosition, SoundEvents.COMPOSTER_READY,
                SoundSource.BLOCKS, 0.8f, 1.0f);

        sapling.shrink(1);
        if (sapling.isEmpty()) {
            items.set(SAPLING_SLOT, ItemStack.EMPTY);
        }

        if (maintenanceTicks <= 0 && random.nextDouble() < SawmillConfig.JAM_CHANCE) {
            jammed = true;

            lvl.playSound(null, worldPosition, SoundEvents.ANVIL_LAND,
                    SoundSource.BLOCKS, 0.4f, 1.5f);
        }

        progress = 0;
    }

    private void addToSlot(int slot, Item item, int amount) {
        ItemStack out = items.get(slot);
        if (out.isEmpty()) {
            items.set(slot, new ItemStack(item, amount));
        } else if (out.is(item)) {
            out.grow(amount);
        }
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
            return IncubatorConfig.FUEL_COAL;
        }
        if (stack.is(Items.LAVA_BUCKET)) {
            return IncubatorConfig.FUEL_LAVA_BUCKET;
        }
        if (stack.getItem().builtInRegistryHolder().is(ItemTags.PLANKS)) {
            return IncubatorConfig.FUEL_PLANKS;
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

    @Override
    public boolean isJammed() {
        return jammed;
    }

    @Override
    public void repair(@Nullable Player player) {
        jammed = false;
        maintenanceTicks = IncubatorConfig.MAINTENANCE_GRACE_TICKS;
        setChanged();
    }

    private boolean pushOutput(int slot) {
        if (level == null) {
            return false;
        }
        ItemStack out = items.get(slot);
        if (out.isEmpty()) {
            return false;
        }
        for (Direction side : Direction.values()) {
            if (level.getBlockEntity(worldPosition.relative(side)) instanceof net.minecraft.world.Container target) {
                ItemStack leftover = com.lesopilka.sawmill.util.InventoryHelper.insertStack(target, out);
                if (leftover.getCount() < out.getCount()) {
                    items.set(slot, leftover);
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
        return new int[]{ FUEL_SLOT, SAPLING_SLOT, CATALYST_SLOT, LOG_OUTPUT_SLOT, SAPLING_OUTPUT_SLOT };
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction dir) {
        if (slot == FUEL_SLOT) {
            return fuelTicks(stack) > 0 || stack.is(Items.IRON_INGOT);
        }
        if (slot == SAPLING_SLOT) {
            return isSapling(stack);
        }
        if (slot == CATALYST_SLOT) {
            return stack.is(Items.BONE_MEAL);
        }
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction dir) {
        return slot == LOG_OUTPUT_SLOT || slot == SAPLING_OUTPUT_SLOT;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case FUEL_SLOT -> fuelTicks(stack) > 0 || stack.is(Items.IRON_INGOT);
            case SAPLING_SLOT -> isSapling(stack);
            case CATALYST_SLOT -> stack.is(Items.BONE_MEAL);
            case UPGRADE_SLOT_START, UPGRADE_SLOT_START + 1, UPGRADE_SLOT_START + 2 ->
                    stack.getItem() instanceof UpgradeItem;
            default -> false;
        };
    }

    private static boolean isSapling(ItemStack stack) {
        return SAPLING_TO_OUTPUT.containsKey(stack.getItem());
    }

    private static Item[] outputsFor(ItemStack sapling) {
        return SAPLING_TO_OUTPUT.get(sapling.getItem());
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
        return Component.translatable("container.sawmill.forest_incubator");
    }

    @Override
    protected AbstractContainerMenu createMenu(int syncId, net.minecraft.world.entity.player.Inventory inventory) {
        return new com.lesopilka.sawmill.screen.IncubatorMenu(syncId, inventory, this, this.dataAccess);
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
        processTime = input.getIntOr("ProcessTime", IncubatorConfig.BASE_PROCESS_TICKS);
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
