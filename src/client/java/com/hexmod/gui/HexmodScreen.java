package com.hexmod.gui;

import com.hexmod.module.Module;
import com.hexmod.module.ModuleManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.List;

/** Glowne menu Hexmoda (Prawy Shift): lista modulow z przelacznikami ON/OFF. */
public class HexmodScreen extends Screen {
    private static final int PANEL_W = 280;
    private static final int ROW_H = 26;
    private static final int HEADER_H = 30;
    private static final int FOOTER_H = 34;

    private int panelX, panelY, panelH;

    public HexmodScreen() {
        super(Text.literal("Hexmod"));
    }

    @Override
    protected void init() {
        List<Module> modules = ModuleManager.all();
        panelH = HEADER_H + modules.size() * ROW_H + FOOTER_H;
        panelX = (width - PANEL_W) / 2;
        panelY = (height - panelH) / 2;

        int y = panelY + HEADER_H;
        for (Module m : modules) {
            addDrawableChild(new ToggleSwitch(
                    panelX + PANEL_W - 36 - 10, y + 4, m.isEnabled(), m::setEnabled));
            y += ROW_H;
        }

        addDrawableChild(ButtonWidget.builder(Text.literal("Wylacz wszystkie moduly"), b -> {
            ModuleManager.disableAll();
            clearAndInit();
        }).dimensions(panelX + 10, panelY + panelH - FOOTER_H + 6, PANEL_W - 20, 20).build());
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, width, height, HexTheme.OVERLAY);
        ctx.fill(panelX, panelY, panelX + PANEL_W, panelY + panelH, HexTheme.PANEL);
        ctx.fill(panelX, panelY, panelX + PANEL_W, panelY + 3, HexTheme.RED);

        int y = panelY + HEADER_H;
        List<Module> modules = ModuleManager.all();
        for (int i = 0; i < modules.size(); i++) {
            if (i % 2 == 0) {
                ctx.fill(panelX + 4, y, panelX + PANEL_W - 4, y + ROW_H, HexTheme.PANEL_ROW);
            }
            y += ROW_H;
        }
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);

        ctx.drawTextWithShadow(textRenderer, "HEXMOD", panelX + 10, panelY + 12, HexTheme.RED);

        int y = panelY + HEADER_H;
        for (Module m : ModuleManager.all()) {
            ctx.drawTextWithShadow(textRenderer, m.name, panelX + 10, y + 4, HexTheme.TEXT);
            ctx.drawTextWithShadow(textRenderer, m.description, panelX + 10, y + 14, HexTheme.TEXT_DIM);
            y += ROW_H;
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
