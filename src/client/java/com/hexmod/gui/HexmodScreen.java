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

    private static int cat = 0; // 0 = PvP, 1 = Inne
    private static int tab = 0; // zapamietana zakladka: 0 = Moduly, 1 = Narzedzia

    private int panelX, panelY, panelH;

    public HexmodScreen() {
        super(Text.literal("Hexmod"));
    }

    /** Moduly widoczne w menu: po .hexoff tylko podstawowe (reszta znika takze z listy). */
    private List<Module> shown() {
        List<Module> out = new java.util.ArrayList<>();
        for (Module m : ModuleManager.all()) {
            if ((!Module.extrasHidden || m.isBasic()) && m.category().equals(cat == 0 ? "PvP" : "Inne")) out.add(m);
        }
        return out;
    }

    private int rows() {
        return (shown().size() + 1) / 2;
    }

    private int cellX(int i) {
        return panelX + 6 + (i % 2) * (CELL_W + 8);
    }

    private int cellY(int i) {
        return panelY + HEADER_H + (i / 2) * ROW_H;
    }

    private int catX(int i) {
        return panelX + PANEL_W - 140 + i * 70;
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

        List<Module> modules = shown();
        panelX = (width - PANEL_W) / 2;
        panelH = tab == 0 ? HEADER_H + rows() * ROW_H + FOOTER_H : HEADER_H + 5 * 26 + 10;
        panelY = Math.max(2, (height - panelH) / 2);

        String[] tabs = {"Modules", "Tools"};
        for (int i = 0; i < tabs.length; i++) {
            final int target = i;
            addDrawableChild(ButtonWidget.builder(Text.literal(tabs[i]), b -> {
                tab = target;
                clearAndInit();
            }).dimensions(tabX(i), panelY + 18, TAB_W, 16).build());
        }

        if (tab == 0) {
            String[] cats = {"PvP", "Other"};
            for (int i = 0; i < cats.length; i++) {
                final int c = i;
                addDrawableChild(ButtonWidget.builder(Text.literal(cats[i]), b -> {
                    cat = c;
                    clearAndInit();
                }).dimensions(catX(i), panelY + 18, 66, 16).build());
            }
            for (int i = 0; i < modules.size(); i++) {
                Module m = modules.get(i);
                addDrawableChild(new ToggleSwitch(cellX(i) + CELL_W - 36 - 2, cellY(i), m.isEnabled(), m::setEnabled));
            }
            int by = panelY + panelH - FOOTER_H + 6;
            addDrawableChild(ButtonWidget.builder(Text.literal("Disable all modules"), b -> {
                ModuleManager.disableAll();
                clearAndInit();
            }).dimensions(panelX + 8, by, PANEL_W - 16, 18).build());
        } else {
            boolean full = !Module.extrasHidden;
            int n = 0;
            tool("HUD Editor", n++, new HudEditorScreen(this));
            tool("Configs", n++, new ConfigScreen(this));
            if (full) tool("Allies", n++, new AlliesScreen(this));
            if (full) tool("Mod Catalog", n++, new ModCatalogScreen(this));
            tool("Spotify", n++, new SpotifyScreen(this));
            if (full) tool("Texture Editor", n++, new TexturePackScreen(this));
            if (full) tool("Macros", n++, new MacroScreen(this));
            if (full) tool("Waypoints", n++, new WaypointScreen(this));
            if (full) tool("Commands & Help", n++, new HelpScreen(this));
            if (full && Remote.isAdmin()) tool("Admin Panel", n++, new AdminScreen(this));
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
            ctx.fill(catX(cat), panelY + 34, catX(cat) + 66, panelY + 37, HexTheme.RED);
            for (int i = 0; i < shown().size(); i++) {
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
        HexLogo.draw(ctx, textRenderer, panelX + 8, panelY + 5);
        if (tab == 0) {
            List<Module> modules = shown();
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
