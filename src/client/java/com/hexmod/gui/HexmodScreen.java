package com.hexmod.gui;

import com.hexmod.module.Module;
import com.hexmod.module.ModuleManager;
import com.hexmod.module.Remote;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.List;

/** Glowne menu Hexmoda (Prawy Shift) z zakladkami: Moduly i Narzedzia. Zablokowany gracz widzi tylko komunikat. */
public class HexmodScreen extends Screen {
    private static final int PANEL_W = 340;
    private static final int CELL_W = 160;
    private static final int ROW_H = 18;
    private static final int HEADER_H = 40;
    private static final int FOOTER_H = 30;
    private static final int TAB_W = 90;

    private static int tab = 0; // zapamietana zakladka: 0 = Moduly, 1 = Narzedzia

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

    private int tabX(int i) {
        return panelX + 8 + i * (TAB_W + 4);
    }

    private void tool(String label, int index, Screen target) {
        int bw = (PANEL_W - 24) / 2;
        int x = panelX + 8 + (index % 2) * (bw + 8);
        int y = panelY + HEADER_H + 4 + (index / 2) * 26;
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
        panelX = (width - PANEL_W) / 2;
        panelH = tab == 0 ? HEADER_H + rows() * ROW_H + FOOTER_H : HEADER_H + 4 * 26 + 10;
        panelY = Math.max(2, (height - panelH) / 2);

        String[] tabs = {"Moduly", "Narzedzia"};
        for (int i = 0; i < tabs.length; i++) {
            final int target = i;
            addDrawableChild(ButtonWidget.builder(Text.literal(tabs[i]), b -> {
                tab = target;
                clearAndInit();
            }).dimensions(tabX(i), panelY + 18, TAB_W, 16).build());
        }

        if (tab == 0) {
            for (int i = 0; i < modules.size(); i++) {
                Module m = modules.get(i);
                addDrawableChild(new ToggleSwitch(cellX(i) + CELL_W - 36 - 2, cellY(i), m.isEnabled(), m::setEnabled));
            }
            int by = panelY + panelH - FOOTER_H + 6;
            int half = (PANEL_W - 20) / 2;
            addDrawableChild(ButtonWidget.builder(Text.literal("Wylacz wszystkie moduly"), b -> {
                ModuleManager.disableAll();
                clearAndInit();
            }).dimensions(panelX + 8, by, half, 18).build());
            addDrawableChild(ButtonWidget.builder(
                    Text.literal("Ukryj dodatkowe: " + (Module.extrasHidden ? "ON" : "OFF")), b -> {
                Module.extrasHidden = !Module.extrasHidden;
                clearAndInit();
            }).dimensions(panelX + 12 + half, by, half, 18).build());
        } else {
            tool("Edytuj HUD", 0, new HudEditorScreen(this));
            tool("Configi", 1, new ConfigScreen(this));
            tool("Sojusznicy", 2, new AlliesScreen(this));
            tool("Katalog modow", 3, new ModCatalogScreen(this));
            tool("Spotify", 4, new SpotifyScreen(this));
            tool("Edytor tekstur", 5, new TexturePackScreen(this));
            if (Remote.isAdmin()) tool("Panel admina", 6, new AdminScreen(this));
        }
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, width, height, HexTheme.OVERLAY);
        ctx.fill(panelX, panelY, panelX + PANEL_W, panelY + panelH, HexTheme.PANEL);
        ctx.fill(panelX, panelY, panelX + PANEL_W, panelY + 3, HexTheme.RED);
        if (Remote.isLocked()) return;
        ctx.fill(tabX(tab), panelY + 34, tabX(tab) + TAB_W, panelY + 37, HexTheme.RED);
        if (tab == 0) {
            for (int i = 0; i < ModuleManager.all().size(); i++) {
                ctx.fill(cellX(i), cellY(i), cellX(i) + CELL_W, cellY(i) + ROW_H - 1, HexTheme.PANEL_ROW);
            }
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
        ctx.drawTextWithShadow(textRenderer, "HEXMOD", panelX + 10, panelY + 6, HexTheme.RED);
        if (tab == 0) {
            List<Module> modules = ModuleManager.all();
            for (int i = 0; i < modules.size(); i++) {
                ctx.drawTextWithShadow(textRenderer, modules.get(i).name, cellX(i) + 6, cellY(i) + 5, HexTheme.TEXT);
            }
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
