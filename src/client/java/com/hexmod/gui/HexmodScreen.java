package com.hexmod.gui;

import com.hexmod.module.Module;
import com.hexmod.module.ModuleManager;
import com.hexmod.module.Remote;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.List;

/** Glowne menu Hexmoda (Prawy Shift). */
public class HexmodScreen extends Screen {
    private static final int PANEL_W = 340;
    private static final int CELL_W = 160;
    private static final int ROW_H = 22;
    private static final int HEADER_H = 28;
    private static final int FOOTER_H = 82;

    private int panelX, panelY, panelH;

    public HexmodScreen() {
        super(Text.literal("Hexmod"));
    }

    private int rows() {
        return (ModuleManager.all().size() + 1) / 2;
    }

    private int cellX(int i) {
        return panelX + 6 + (i % 2) * (CELL_W + 8);
    }

    private int cellY(int i) {
        return panelY + HEADER_H + (i / 2) * ROW_H;
    }

    @Override
    protected void init() {
        List<Module> modules = ModuleManager.all();
        panelH = HEADER_H + rows() * ROW_H + FOOTER_H;
        panelX = (width - PANEL_W) / 2;
        panelY = Math.max(2, (height - panelH) / 2);

        for (int i = 0; i < modules.size(); i++) {
            Module m = modules.get(i);
            addDrawableChild(new ToggleSwitch(cellX(i) + CELL_W - 36 - 4, cellY(i) + 2, m.isEnabled(), m::setEnabled));
        }

        int half = (PANEL_W - 24) / 2;
        int lx = panelX + 8;
        int rx = panelX + 16 + half;
        int by = panelY + panelH - FOOTER_H + 6;

        addDrawableChild(ButtonWidget.builder(Text.literal("Edytuj HUD"),
                b -> client.setScreen(new HudEditorScreen(this))).dimensions(lx, by, half, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Configi"),
                b -> client.setScreen(new ConfigScreen(this))).dimensions(rx, by, half, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Sojusznicy"),
                b -> client.setScreen(new AlliesScreen(this))).dimensions(lx, by + 24, half, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Katalog modow"),
                b -> client.setScreen(new ModCatalogScreen(this))).dimensions(rx, by + 24, half, 20).build());

        ButtonWidget off = ButtonWidget.builder(Text.literal("Wylacz wszystkie moduly"), b -> {
            ModuleManager.disableAll();
            clearAndInit();
        }).dimensions(lx, by + 48, Remote.isAdmin() ? half : PANEL_W - 16, 20).build();
        addDrawableChild(off);

        if (Remote.isAdmin()) {
            addDrawableChild(ButtonWidget.builder(Text.literal("Panel admina"),
                    b -> client.setScreen(new AdminScreen(this))).dimensions(rx, by + 48, half, 20).build());
        }
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, width, height, HexTheme.OVERLAY);
        ctx.fill(panelX, panelY, panelX + PANEL_W, panelY + panelH, HexTheme.PANEL);
        ctx.fill(panelX, panelY, panelX + PANEL_W, panelY + 3, HexTheme.RED);
        for (int i = 0; i < ModuleManager.all().size(); i++) {
            ctx.fill(cellX(i), cellY(i), cellX(i) + CELL_W, cellY(i) + ROW_H - 2, HexTheme.PANEL_ROW);
        }
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawTextWithShadow(textRenderer, "HEXMOD", panelX + 10, panelY + 12, HexTheme.RED);
        if (Remote.isBlocked()) {
            String t = "DOSTEP ZABLOKOWANY";
            ctx.drawTextWithShadow(textRenderer, t, panelX + PANEL_W - 10 - textRenderer.getWidth(t), panelY + 12, HexTheme.RED);
        }
        List<Module> modules = ModuleManager.all();
        for (int i = 0; i < modules.size(); i++) {
            ctx.drawTextWithShadow(textRenderer, modules.get(i).name, cellX(i) + 6, cellY(i) + 6, HexTheme.TEXT);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
