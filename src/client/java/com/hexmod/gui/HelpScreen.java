package com.hexmod.gui;

import com.hexmod.module.L;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/** Krotka sciaga: komendy czatu i skroty Hexmoda. */
public class HelpScreen extends Screen {
    private static final String[][] LINES = {
            {"MENU", "Right Shift opens the Hex Client menu (changeable in Controls). Modules are split into PvP, Status and Other."},
            {"HUD", "HUD Editor: drag widgets, click a widget to set its scale, color, options and key."},
            {"STEALTH", "Widget option 'Hide until key': the widget stays invisible until you press its key."},
            {".wp add [name]", "Saves a waypoint where you are standing."},
            {".wp del name", "Removes a waypoint. .wp list shows all of them."},
            {".wp death", "Turns the automatic death point on or off."},
            {".lock", "Locks the item in your hand. .lock name (e.g. ender_pearl) locks by name."},
            {".lock offhand", "Locks or unlocks the offhand (free by default)."},
            {".lock list / clear", "Shows or clears the list of locked items."},
            {".kd", "Resets the K/D counter."},
            {".lyrics 0.5", "Shifts the Spotify lyrics (positive = earlier)."},
    };

    private static final int PER_PAGE = 5;
    private final Screen parent;
    private int page = 0;

    public HelpScreen(Screen parent) {
        super(Text.literal(L.t("Commands & Help")));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int pages = (LINES.length + PER_PAGE - 1) / PER_PAGE;
        addDrawableChild(ButtonWidget.builder(Text.literal("<"), b -> {
            page = (page + pages - 1) % pages;
        }).dimensions(width / 2 - 110, height - 28, 50, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal(L.t("Back")), b -> client.setScreen(parent))
                .dimensions(width / 2 - 50, height - 28, 100, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal(">"), b -> {
            page = (page + 1) % pages;
        }).dimensions(width / 2 + 60, height - 28, 50, 20).build());
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, width, height, HexTheme.OVERLAY);
        ctx.fill(width / 2 - 170, 4, width / 2 + 170, height - 2, HexTheme.PANEL);
        ctx.fill(width / 2 - 170, 4, width / 2 + 170, 7, HexTheme.RED);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        int x = width / 2 - 160;
        ctx.drawTextWithShadow(textRenderer, L.t("COMMANDS & HELP"), x, 14, HexTheme.RED);
        int pages = (LINES.length + PER_PAGE - 1) / PER_PAGE;
        ctx.drawTextWithShadow(textRenderer, L.t("Page ") + (page + 1) + "/" + pages, x + 240, 14, HexTheme.TEXT_DIM);
        int y = 30;
        for (int i = page * PER_PAGE; i < Math.min(LINES.length, (page + 1) * PER_PAGE); i++) {
            String[] l = LINES[i];
            ctx.drawTextWithShadow(textRenderer, L.t(l[0]), x, y, HexTheme.RED);
            for (var line : textRenderer.wrapLines(Text.literal(L.t(l[1])), 320)) {
                ctx.drawTextWithShadow(textRenderer, line, x, y + 10, HexTheme.TEXT);
                y += 10;
            }
            y += 18;
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
