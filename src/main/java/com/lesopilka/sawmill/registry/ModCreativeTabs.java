package com.lesopilka.sawmill.registry;

import com.lesopilka.sawmill.SawmillMod;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public final class ModCreativeTabs {
    private ModCreativeTabs() {}

    public static final CreativeModeTab SAWMILL_TAB = Registry.register(
            BuiltInRegistries.CREATIVE_MODE_TAB,
            ModIds.id("main"),
            FabricCreativeModeTab.builder()
                    .icon(() -> new ItemStack(ModBlocks.SAWMILL_ITEM))
                    .title(Component.translatable("creativemodetab.sawmill.main"))
                    .displayItems((parameters, output) -> {
                        output.accept(ModBlocks.SAWMILL_ITEM);
                        output.accept(ModBlocks.INCUBATOR_ITEM);
                        output.accept(ModBlocks.WRENCH);
                        output.accept(ModBlocks.SPEED_UPGRADE);
                        output.accept(ModBlocks.EFFICIENCY_UPGRADE);
                        output.accept(ModBlocks.AUTOMATION_UPGRADE_1);
                        output.accept(ModBlocks.AUTOMATION_UPGRADE_2);
                        output.accept(ModBlocks.SELF_DIAGNOSIS_UPGRADE);
                    })
                    .build()
    );

    public static void init() {
        SawmillMod.LOGGER.info("Registering creative tab for {}", SawmillMod.MOD_ID);
    }
}
