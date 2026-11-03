package com.lesopilka.sawmill.client;

import com.lesopilka.sawmill.SawmillMod;
import com.lesopilka.sawmill.screen.SawmillMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class SawmillScreen extends AbstractContainerScreen<SawmillMenu> {
    private static final Identifier BACKGROUND =
            Identifier.fromNamespaceAndPath(SawmillMod.MOD_ID, "textures/gui/sawmill.png");
    private static final Identifier FURNACE_TEXTURE =
            Identifier.withDefaultNamespace("textures/gui/container/furnace.png");

    private static final int FLAME_WIDTH = 14;
    private static final int FLAME_HEIGHT = 14;

    private static final int ARROW_WIDTH = 24;
    private static final int ARROW_HEIGHT = 16;

    public SawmillScreen(SawmillMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 166);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractBackground(graphics, mouseX, mouseY, delta);

        graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos, topPos,
                0f, 0f, imageWidth, imageHeight, 176, 166);

        int burnTime = menu.getBurnTime();
        int burnTotal = Math.max(1, menu.getBurnTimeTotal());
        if (burnTime > 0) {
            int flameHeight = FLAME_HEIGHT - (FLAME_HEIGHT * burnTime / burnTotal);
            graphics.blit(RenderPipelines.GUI_TEXTURED, FURNACE_TEXTURE,
                    leftPos + 57, topPos + 36 + (FLAME_HEIGHT - flameHeight),
                    176f, 0f, FLAME_WIDTH, flameHeight, 256, 256);
        }

        int progress = menu.getProgress();
        int processTime = Math.max(1, menu.getProcessTime());
        int arrowWidth = ARROW_WIDTH * progress / processTime;
        if (arrowWidth > 0) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, FURNACE_TEXTURE,
                    leftPos + 79, topPos + 34,
                    176f, 14f, arrowWidth, ARROW_HEIGHT, 256, 256);
        }

        if (menu.isJammed()) {
            graphics.text(this.font,
                    Component.translatable("message.sawmill.jammed"),
                    leftPos + 8, topPos + 62, 0xFFAA0000, true);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);
        graphics.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);
    }
}
