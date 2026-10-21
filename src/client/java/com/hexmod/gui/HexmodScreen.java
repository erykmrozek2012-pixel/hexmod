package com.hexmod.gui;

import com.hexmod.module.L;
import com.hexmod.module.Module;
import com.hexmod.module.ModuleManager;
import com.hexmod.module.Remote;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Map;

/** Glowne menu Hex Client (Prawy Shift): zakladki Modules i Tools, kategorie PvP / Status / Other, wybor jezyka. */
public class HexmodScreen extends Screen {
    private static final int PANEL_W = 340;
    private static final int CELL_W = 160;
    private static final int ROW_H = 24;
    private static final int HEADER_H = 40;
    private static final int CAT_H = 36;
    private static final int FOOTER_H = 30;
    private static final int TAB_W = 90;

    private static final String[] CATS = {"PvP", "Status", "Other"};
    private static final String[] CAT_DESC = {
            "Combat widgets about enemies and fights",
            "Your own stats, items and cooldowns",
            "Everything else: FPS, waypoints, music"};
    private static int cat = 0; // 0 = PvP, 1 = Status, 2 = Other

    private static int tab = 0; // 0 = Modules, 1 = Tools

    /** Krotkie podpisy pod nazwami modulow (po angielsku, tlumaczone przez L.t). */
    private static final Map<String, String> SUB = Map.ofEntries(
            Map.entry("allies", "Friends near you"),
            Map.entry("apples", "Gapples held by enemies"),
            Map.entry("armor", "Armor durability"),
            Map.entry("combo", "Hits in a row"),
            Map.entry("cooldowns", "Item cooldowns"),
            Map.entry("coords", "Your position"),
            Map.entry("cps", "Clicks per second"),
            Map.entry("damage_dealt", "Damage you deal"),
            Map.entry("eat_timer", "Eating countdown"),
            Map.entry("effects", "Active potion effects"),
            Map.entry("enemy_shield", "Enemy shield state"),
            Map.entry("enemy_tags", "Info above enemies"),
            Map.entry("fps", "Frames per second"),
            Map.entry("item_count", "PvP item counters"),
            Map.entry("item_lock", "Protect items from drops"),
            Map.entry("memory", "Game memory usage"),
            Map.entry("nearest_enemy", "Closest enemy info"),
            Map.entry("pearl_timer", "Pearl flight time"),
            Map.entry("ping", "Latency to server"),
            Map.entry("reach", "Last hit distance"),
            Map.entry("saturation", "Saturation in combat"),
            Map.entry("shield_break", "Shield break alerts"),
            Map.entry("spotify", "Current song & lyrics"),
            Map.entry("totems", "Totems you carry"),
            Map.entry("waypoints", "Saved locations"));

    private int panelX, panelY, panelH;

    public HexmodScreen() {
        super(Text.literal("Hex Client"));
    }

    private int headerH() {
        return tab == 0 ? HEADER_H + CAT_H : HEADER_H;
    }

    private List<Module> inCategory(int c) {
        List<Module> out = new java.util.ArrayList<>();
        for (Module m : ModuleManager.all()) {
            if ((!Module.extrasHidden || m.isBasic()) && m.category().equals(CATS[c])) out.add(m);
        }
        return out;
    }

    private List<Module> shown() {
        return inCategory(cat);
    }

    private int rows() {
        return (shown().size() + 1) / 2;
    }

    private int cellX(int i) {
        return panelX + 6 + (i % 2) * (CELL_W + 8);
    }

    private int cellY(int i) {
        return panelY + headerH() + (i / 2) * ROW_H;
    }

    private int catW() {
        return (PANEL_W - 16 - 8) / 3;
    }

    private int catX(int i) {
        return panelX + 8 + i * (catW() + 4);
    }

    private int tabX(int i) {
        return panelX + 8 + i * (TAB_W + 4);
    }

    private void tool(String label, int index, Screen target) {
        int bw = (PANEL_W - 24) / 2;
        int x = panelX + 8 + (index % 2) * (bw + 8);
        int y = panelY + HEADER_H + 4 + (index / 2) * 26;
        addDrawableChild(ButtonWidget.builder(Text.literal(L.t(label)), b -> client.setScreen(target))
                .dimensions(x, y, bw, 20).build());
    }

    @Override
    protected void init() {
        if (Remote.isLocked()) {
            panelH = 80;
            panelX = (width - PANEL_W) / 2;
            panelY = (height - panelH) / 2;
            addDrawableChild(ButtonWidget.builder(Text.literal(L.t("Close")), b -> close())
                    .dimensions(panelX + PANEL_W / 2 - 50, panelY + panelH - 28, 100, 20).build());
            return;
        }

        List<Module> modules = shown();
        panelX = (width - PANEL_W) / 2;
        panelH = tab == 0 ? headerH() + rows() * ROW_H + FOOTER_H : HEADER_H + 5 * 26 + 10;
        panelY = Math.max(2, (height - panelH) / 2);

        // Przelacznik jezyka (prawy gorny rog)
        addDrawableChild(ButtonWidget.builder(Text.literal(L.t("Language") + ": " + L.languageName()), b -> {
            L.next();
            clearAndInit();
        }).dimensions(panelX + PANEL_W - 112, panelY + 5, 106, 14).build());

        String[] tabs = {"Modules", "Tools"};
        for (int i = 0; i < tabs.length; i++) {
            final int target = i;
            addDrawableChild(ButtonWidget.builder(Text.literal(L.t(tabs[i])), b -> {
                tab = target;
                clearAndInit();
            }).dimensions(tabX(i), panelY + 20, TAB_W, 16).build());
        }

        if (tab == 0) {
            for (int i = 0; i < CATS.length; i++) {
                final int c = i;
                String label = L.t(CATS[i]) + " (" + inCategory(i).size() + ")";
                addDrawableChild(ButtonWidget.builder(Text.literal(label), b -> {
                    cat = c;
                    clearAndInit();
                }).dimensions(catX(i), panelY + HEADER_H, catW(), 16).build());
            }
            for (int i = 0; i < modules.size(); i++) {
                Module m = modules.get(i);
                addDrawableChild(new ToggleSwitch(cellX(i) + CELL_W - 36 - 2, cellY(i) + 3, m.isEnabled(), m::setEnabled));
            }
            int by = panelY + panelH - FOOTER_H + 6;
            addDrawableChild(ButtonWidget.builder(Text.literal(L.t("Disable all modules")), b -> {
                ModuleManager.disableAll();
                clearAndInit();
            }).dimensions(panelX + 8, by, PANEL_W - 16, 18).build());
        } else {
            boolean full = !Module.extrasHidden;
            int n = 0;
            tool("HUD Editor", n++, new HudEditorScreen(this));
            tool("Configs", n++, new ConfigScreen(this));
            if (full) tool("Friends", n++, new AlliesScreen(this));
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
        ctx.fill(tabX(tab), panelY + 36, tabX(tab) + TAB_W, panelY + 39, HexTheme.RED);
        if (tab == 0) {
            int cy = panelY + HEADER_H + 16;
            ctx.fill(catX(cat), cy, catX(cat) + catW(), cy + 3, HexTheme.RED);
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
            ctx.drawTextWithShadow(textRenderer, "HEX CLIENT", panelX + 10, panelY + 9, HexTheme.RED);
            if (Remote.isReady()) {
                ctx.drawCenteredTextWithShadow(textRenderer, L.t("ACCESS BLOCKED"), cx, panelY + 28, HexTheme.RED);
                ctx.drawCenteredTextWithShadow(textRenderer, L.t("An administrator has blocked your access to Hex Client."), cx, panelY + 42, HexTheme.TEXT_DIM);
            } else {
                ctx.drawCenteredTextWithShadow(textRenderer, L.t("Checking access..."), cx, panelY + 34, HexTheme.TEXT_DIM);
            }
            return;
        }
        HexLogo.draw(ctx, textRenderer, panelX + 8, panelY + 5);
        if (tab == 0) {
            // opis wybranej kategorii (maly, szary) pod przyciskami kategorii
            String desc = textRenderer.trimToWidth(L.t(CAT_DESC[cat]), PANEL_W - 16);
            ctx.drawTextWithShadow(textRenderer, desc, panelX + 10, panelY + HEADER_H + 22, HexTheme.TEXT_DIM);

            List<Module> modules = shown();
            int textW = CELL_W - 46;
            for (int i = 0; i < modules.size(); i++) {
                Module mod = modules.get(i);
                ctx.drawTextWithShadow(textRenderer, textRenderer.trimToWidth(L.t(mod.name), textW), cellX(i) + 6, cellY(i) + 3, HexTheme.TEXT);
                var mx = ctx.getMatrices();
                mx.pushMatrix();
                mx.translate((float) (cellX(i) + 6), (float) (cellY(i) + 14));
                mx.scale(0.75f, 0.75f);
                String sub = L.t(SUB.getOrDefault(mod.id, ""));
                ctx.drawTextWithShadow(textRenderer, textRenderer.trimToWidth(sub, (int) (textW / 0.75f)), 0, 0, HexTheme.TEXT_DIM);
                mx.popMatrix();
            }
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
