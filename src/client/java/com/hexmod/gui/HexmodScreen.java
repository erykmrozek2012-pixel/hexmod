package com.hexmod.gui;

import com.hexmod.module.Module;
import com.hexmod.module.ModuleManager;
import com.hexmod.module.Remote;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.List;

/** Glowne menu Hexmoda (Prawy Shift). Zablokowany gracz widzi tylko komunikat. */
public class HexmodScreen extends Screen {
    private static final int PANEL_W = 340;
    private static final int CELL_W = 160;
    private static final int ROW_H = 20;
    private static final int HEADER_H = 24;
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

    private void button(String label, int col, int row, Screen target) {
        int bw = (PANEL_W - 16 - 8) / 3;
        int x = panelX + 8 + col * (bw + 4);
        int y = panelY + panelH - FOOTER_H + 6 + row * 24;
        addDrawableChild(ButtonWidget.builder(Text.literal(label), b -> client.setScreen(target))
                .dimensions(x, y, bw, 20).build());
    }

    @Override
    protected void init() {
        if (Remote.isLocked()) {
            panelH = 80;
            panelX = (width - PANEL_W) / 2;
            panelY = (height - panelH) / 2;
            addDrawableChild(ButtonWidget.builder(Text.literal("Zamknij"), b -> close())
                    .dimensions(panelX + PANEL_W / 2 - 50, panelY + panelH - 28, 100, 20).build());
            return;
        }

        List<Module> modules = ModuleManager.all();
        panelH = HEADER_H + rows() * ROW_H + FOOTER_H;
        panelX = (width - PANEL_W) / 2;
        panelY = Math.max(2, (height - panelH) / 2);

        for (int i = 0; i < modules.size(); i++) {
            Module m = modules.get(i);
            addDrawableChild(new ToggleSwitch(cellX(i) + CELL_W - 36 - 4, cellY(i) + 1, m.isEnabled(), m::setEnabled));
        }

        button("Edytuj HUD", 0, 0, new HudEditorScreen(this));
        button("Configi", 1, 0, new ConfigScreen(this));
        button("Sojusznicy", 2, 0, new AlliesScreen(this));
        button("Katalog modow", 0, 1, new ModCatalogScreen(this));
        button("Spotify", 1, 1, new SpotifyScreen(this));
        if (Remote.isAdmin()) {
            button("Panel admina", 2, 1, new AdminScreen(this));
        }

        int by = panelY + panelH - FOOTER_H + 6 + 48;
        addDrawableChild(ButtonWidget.builder(Text.literal("Wylacz wszystkie moduly"), b -> {
            ModuleManager.disableAll();
            clearAndInit();
        }).dimensions(panelX + 8, by, PANEL_W - 16, 20).build());
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, width, height, HexTheme.OVERLAY);
        ctx.fill(panelX, panelY, panelX + PANEL_W, panelY + panelH, HexTheme.PANEL);
        ctx.fill(panelX, panelY, panelX + PANEL_W, panelY + 3, HexTheme.RED);
        if (Remote.isLocked()) return;
        for (int i = 0; i < ModuleManager.all().size(); i++) {
            ctx.fill(cellX(i), cellY(i), cellX(i) + CELL_W, cellY(i) + ROW_H - 2, HexTheme.PANEL_ROW);
        }
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        int cx = width / 2;
        if (Remote.isLocked()) {
            ctx.drawTextWithShadow(textRenderer, "HEXMOD", panelX + 10, panelY + 9, HexTheme.RED);
            if (Remote.isReady()) {
                ctx.drawCenteredTextWithShadow(textRenderer, "DOSTEP ZABLOKOWANY", cx, panelY + 28, HexTheme.RED);
                ctx.drawCenteredTextWithShadow(textRenderer, "Administrator zablokowal Ci dostep do Hexmoda.", cx, panelY + 42, HexTheme.TEXT_DIM);
            } else {
                ctx.drawCenteredTextWithShadow(textRenderer, "Sprawdzam dostep...", cx, panelY + 34, HexTheme.TEXT_DIM);
            }
            return;
        }
        ctx.drawTextWithShadow(textRenderer, "HEXMOD", panelX + 10, panelY + 9, HexTheme.RED);
        List<Module> modules = ModuleManager.all();
        for (int i = 0; i < modules.size(); i++) {
            ctx.drawTextWithShadow(textRenderer, modules.get(i).name, cellX(i) + 6, cellY(i) + 5, HexTheme.TEXT);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
