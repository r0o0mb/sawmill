package com.lesopilka.sawmill.block.entity;

import net.minecraft.world.entity.player.Player;

public interface Repairable {
    boolean isJammed();

    void repair(Player player);
}
