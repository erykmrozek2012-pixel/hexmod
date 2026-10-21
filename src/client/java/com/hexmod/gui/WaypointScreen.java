package com.hexmod.gui;

import com.hexmod.module.L;
import com.hexmod.module.WaypointManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.List;

/** Lista waypointow: dodaj w miejscu, w ktorym stoisz, zmien kolor, usun. */
public class WaypointScreen extends Screen {
    private static final int ROWS = 8;
    private final Screen parent;
    private TextFieldWidget nameField;
    private String status = "";

    public WaypointScreen(Screen parent) {
        super(Text.literal(L.t("Waypoints")));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int cx = width / 2;
        nameField = new TextFieldWidget(textRenderer, cx - 140, 40, 180, 20, Text.literal(L.t("Name")));
        nameField.setPlaceholder(Text.literal(L.t("Waypoint name")));
        nameField.setMaxLength(24);
        addDrawableChild(nameField);

        addDrawableChild(ButtonWidget.builder(Text.literal(L.t("Add here")), b -> {
            status = WaypointManager.addHere(nameField.getText()) ? L.t("Waypoint added.") : L.t("Cannot add (limit ")
                    + WaypointManager.MAX + L.t(" or no player).");
            clearAndInit();
        }).dimensions(cx + 46, 40, 94, 20).build());

        List<WaypointManager.Waypoint> list = WaypointManager.list();
        int y = 70;
        for (int i = 0; i < Math.min(ROWS, list.size()); i++) {
            final WaypointManager.Waypoint w = list.get(i);
            addDrawableChild(ButtonWidget.builder(Text.literal(L.t("Color")), b -> {
                WaypointManager.cycleColor(w);
            }).dimensions(cx + 26, y, 54, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal(L.t("Remove")), b -> {
                WaypointManager.remove(w);
                status = L.t("Removed: ") + w.name;
                clearAndInit();
            }).dimensions(cx + 84, y, 56, 20).build());
            y += 24;
        }

        addDrawableChild(ButtonWidget.builder(Text.literal(L.t("Back")), b -> client.setScreen(parent))
                .dimensions(cx - 50, height - 30, 100, 20).build());
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, width, height, HexTheme.OVERLAY);
        ctx.fill(width / 2 - 150, 8, width / 2 + 150, height - 4, HexTheme.PANEL);
        ctx.fill(width / 2 - 150, 8, width / 2 + 150, 11, HexTheme.RED);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        int cx = width / 2;
        ctx.drawTextWithShadow(textRenderer, L.t("WAYPOINTS"), cx - 140, 22, HexTheme.RED);
        List<WaypointManager.Waypoint> list = WaypointManager.list();
        int y = 70;
        for (int i = 0; i < Math.min(ROWS, list.size()); i++) {
            WaypointManager.Waypoint w = list.get(i);
            ctx.fill(cx - 140, y + 5, cx - 132, y + 13, 0xFF000000 | w.color);
            String t = textRenderer.trimToWidth(w.name + " " + w.x + " " + w.y + " " + w.z, 160);
            ctx.drawTextWithShadow(textRenderer, t, cx - 128, y + 6, HexTheme.TEXT);
            y += 24;
        }
        if (list.isEmpty()) {
            ctx.drawTextWithShadow(textRenderer, L.t("No waypoints. Enter a name and click 'Add here'."), cx - 140, 76, HexTheme.TEXT_DIM);
        } else if (list.size() > ROWS) {
            ctx.drawTextWithShadow(textRenderer, L.t("Showing ") + ROWS + L.t(" of ") + list.size() + L.t(" (the rest in .wp list)."), cx - 140, y + 4, HexTheme.TEXT_DIM);
        }
        if (!status.isEmpty()) {
            ctx.drawCenteredTextWithShadow(textRenderer, status, cx, height - 44, HexTheme.TEXT);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
