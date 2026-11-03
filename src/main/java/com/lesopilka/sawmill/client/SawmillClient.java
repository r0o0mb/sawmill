package com.lesopilka.sawmill.client;

import com.lesopilka.sawmill.SawmillMod;
import com.lesopilka.sawmill.registry.ModBlockEntities;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;

public class SawmillClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        MenuScreens.register(ModBlockEntities.SAWMILL_MENU, SawmillScreen::new);
        MenuScreens.register(ModBlockEntities.INCUBATOR_MENU, IncubatorScreen::new);
        SawmillMod.LOGGER.info("Клиентская часть лесопилки зарегистрирована");
    }
}
