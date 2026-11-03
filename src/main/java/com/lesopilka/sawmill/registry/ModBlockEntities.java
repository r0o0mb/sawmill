package com.lesopilka.sawmill.registry;

import com.lesopilka.sawmill.SawmillMod;
import com.lesopilka.sawmill.block.entity.IncubatorBlockEntity;
import com.lesopilka.sawmill.block.entity.SawmillBlockEntity;
import com.lesopilka.sawmill.screen.IncubatorMenu;
import com.lesopilka.sawmill.screen.SawmillMenu;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class ModBlockEntities {
    private ModBlockEntities() {}

    public static final BlockEntityType<SawmillBlockEntity> SAWMILL_BE =
            Registry.register(
                    BuiltInRegistries.BLOCK_ENTITY_TYPE,
                    ModIds.id("sawmill"),
                    FabricBlockEntityTypeBuilder.create(SawmillBlockEntity::new, ModBlocks.SAWMILL).build()
            );

    public static final MenuType<SawmillMenu> SAWMILL_MENU =
            Registry.register(
                    BuiltInRegistries.MENU,
                    ModIds.id("sawmill"),
                    new MenuType<>(SawmillMenu::new, FeatureFlags.VANILLA_SET)
            );

    public static final BlockEntityType<IncubatorBlockEntity> INCUBATOR_BE =
            Registry.register(
                    BuiltInRegistries.BLOCK_ENTITY_TYPE,
                    ModIds.id("forest_incubator"),
                    FabricBlockEntityTypeBuilder.create(IncubatorBlockEntity::new, ModBlocks.INCUBATOR).build()
            );

    public static final MenuType<IncubatorMenu> INCUBATOR_MENU =
            Registry.register(
                    BuiltInRegistries.MENU,
                    ModIds.id("forest_incubator"),
                    new MenuType<>(IncubatorMenu::new, FeatureFlags.VANILLA_SET)
            );

    public static void init() {
        SawmillMod.LOGGER.info("Registering block entities and menus for {}", SawmillMod.MOD_ID);
    }
}
