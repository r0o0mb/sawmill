package com.lesopilka.sawmill.registry;

import com.lesopilka.sawmill.SawmillMod;
import com.lesopilka.sawmill.block.IncubatorBlock;
import com.lesopilka.sawmill.block.SawmillBlock;
import com.lesopilka.sawmill.item.WrenchItem;
import com.lesopilka.sawmill.upgrade.UpgradeItem;
import com.lesopilka.sawmill.upgrade.UpgradeType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public final class ModBlocks {
    private ModBlocks() {}

    public static final Block SAWMILL = Registry.register(
            BuiltInRegistries.BLOCK,
            ModIds.id("sawmill"),
            new SawmillBlock(BlockBehaviour.Properties.of()
                    .setId(ModIds.blockKey("sawmill"))
                    .mapColor(MapColor.WOOD)
                    .strength(3.5f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.WOOD)
                    .noOcclusion())
    );

    public static final Block INCUBATOR = Registry.register(
            BuiltInRegistries.BLOCK,
            ModIds.id("forest_incubator"),
            new IncubatorBlock(BlockBehaviour.Properties.of()
                    .setId(ModIds.blockKey("forest_incubator"))
                    .mapColor(MapColor.PLANT)
                    .strength(3.5f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.WOOD)
                    .noOcclusion())
    );

    public static final Item INCUBATOR_ITEM = Registry.register(
            BuiltInRegistries.ITEM,
            ModIds.id("forest_incubator"),
            new net.minecraft.world.item.BlockItem(INCUBATOR,
                    new Item.Properties().setId(ModIds.itemKey("forest_incubator")))
    );

    public static final Item SAWMILL_ITEM = Registry.register(
            BuiltInRegistries.ITEM,
            ModIds.id("sawmill"),
            new net.minecraft.world.item.BlockItem(SAWMILL,
                    new Item.Properties().setId(ModIds.itemKey("sawmill")))
    );

    public static final Item SPEED_UPGRADE = registerUpgrade("speed_upgrade", UpgradeType.SPEED);
    public static final Item EFFICIENCY_UPGRADE = registerUpgrade("efficiency_upgrade", UpgradeType.EFFICIENCY);
    public static final Item AUTOMATION_UPGRADE_1 = registerUpgrade("automation_upgrade_1", UpgradeType.AUTOMATION_1);
    public static final Item AUTOMATION_UPGRADE_2 = registerUpgrade("automation_upgrade_2", UpgradeType.AUTOMATION_2);
    public static final Item SELF_DIAGNOSIS_UPGRADE = registerUpgrade("self_diagnosis_upgrade", UpgradeType.SELF_DIAGNOSIS);

    public static final Item WRENCH = Registry.register(
            BuiltInRegistries.ITEM,
            ModIds.id("wrench"),
            new WrenchItem(new Item.Properties().setId(ModIds.itemKey("wrench")).durability(64))
    );

    private static Item registerUpgrade(String name, UpgradeType type) {
        return Registry.register(
                BuiltInRegistries.ITEM,
                ModIds.id(name),
                new UpgradeItem(new Item.Properties().setId(ModIds.itemKey(name)).stacksTo(1), type)
        );
    }

    public static void init() {
        SawmillMod.LOGGER.info("Registering blocks and items for {}", SawmillMod.MOD_ID);
    }
}
