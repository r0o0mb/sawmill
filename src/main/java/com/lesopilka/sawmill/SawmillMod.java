package com.lesopilka.sawmill;

import com.lesopilka.sawmill.registry.ModBlockEntities;
import com.lesopilka.sawmill.registry.ModBlocks;
import com.lesopilka.sawmill.registry.ModCreativeTabs;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SawmillMod implements ModInitializer {
    public static final String MOD_ID = "sawmill";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {

        ModBlocks.init();
        ModBlockEntities.init();
        ModCreativeTabs.init();

        LOGGER.info("Лесопилка инициализирована");
    }
}
