package com.lesopilka.sawmill.registry;

import com.lesopilka.sawmill.SawmillMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;

public final class ModIds {
    private ModIds() {}

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(SawmillMod.MOD_ID, path);
    }

    public static ResourceKey<net.minecraft.world.level.block.Block> blockKey(String path) {
        return ResourceKey.create(Registries.BLOCK, id(path));
    }

    public static ResourceKey<net.minecraft.world.item.Item> itemKey(String path) {
        return ResourceKey.create(Registries.ITEM, id(path));
    }
}
