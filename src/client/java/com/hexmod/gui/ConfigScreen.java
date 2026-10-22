package com.hexmod.gui;

import com.hexmod.module.L;
import com.hexmod.module.ModuleManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.List;

/** Configi: zapis wlasnych ustawien pod nazwa, wczytywanie, usuwanie, eksport i import ze schowka. */
public class ConfigScreen extends Screen {
    private final Screen parent;
    private TextFieldWidget nameField;
    private List<String> names = List.of();
    private String status = "";
    private String filter = "";

    public ConfigScreen(Screen parent) {
        super(Text.literal(L.t("Configs")));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int cx = width / 2;

        nameField = new TextFieldWidget(textRenderer, cx - 140, 40, 180, 20, Text.literal(L.t("Name")));
        nameField.setPlaceholder(Text.literal(L.t("Config name")));
        nameField.setMaxLength(24);
        addDrawableChild(nameField);

        addDrawableChild(ButtonWidget.builder(Text.literal(L.t("Save")), b -> {
            status = ModuleManager.saveConfig(nameField.getText())
                    ? L.t("Config saved.") : L.t("Enter a name (letters, digits, - and _).");
            clearAndInit();
        }).dimensions(cx + 46, 40, 94, 20).build());

        names = ModuleManager.listConfigs();
        if (!filter.isEmpty()) {
            String f = filter.toLowerCase();
            names = names.stream().filter(n -> n.toLowerCase().contains(f)).toList();
        }
        addDrawableChild(ButtonWidget.builder(
                Text.literal(filter.isEmpty() ? L.t("Search (uses the name field)") : L.t("Filter: ") + filter + " [x]"), b -> {
            filter = filter.isEmpty() ? nameField.getText().trim() : "";
            clearAndInit();
        }).dimensions(cx - 20, 16, 160, 16).build());
        String[] presetNames = {"Minimal PvP", "Full PvP", "Clean"};
        for (int i = 0; i < presetNames.length; i++) {
            final int pi = i;
            addDrawableChild(ButtonWidget.builder(Text.literal(L.t(presetNames[i])), b -> {
                applyPreset(pi);
                status = L.t("Preset applied: ") + L.t(presetNames[pi]);
            }).dimensions(cx - 140 + i * 94, 64, 92, 18).build());
        }
        int y = 94;
        for (int i = 0; i < Math.min(6, names.size()); i++) {
            final String n = names.get(i);
            addDrawableChild(ButtonWidget.builder(Text.literal(L.t("Load")), b -> {
                status = ModuleManager.loadConfig(n) ? L.t("Loaded: ") + n : L.t("Could not load.");
            }).dimensions(cx + 10, y, 70, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal(L.t("Remove")), b -> {
                ModuleManager.deleteConfig(n);
                status = L.t("Removed: ") + n;
                clearAndInit();
            }).dimensions(cx + 84, y, 56, 20).build());
            y += 24;
        }

        int by = height - 52;
        addDrawableChild(ButtonWidget.builder(Text.literal(L.t("Copy to clipboard")), b -> {
            client.keyboard.setClipboard(ModuleManager.exportJson());
            status = L.t("Config copied to the clipboard. Send it to a friend.");
        }).dimensions(cx - 140, by, 135, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal(L.t("Paste from clipboard")), b -> {
            status = ModuleManager.importJson(client.keyboard.getClipboard())
                    ? L.t("Config imported from the clipboard.") : L.t("The clipboard does not contain a valid config.");
        }).dimensions(cx + 5, by, 135, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal(L.t("Back")), b -> client.setScreen(parent))
                .dimensions(cx - 50, by + 26, 100, 20).build());
    }

    private static final java.util.Set<String> MINIMAL = java.util.Set.of("enemy_tags", "cooldowns", "totems", "effects",
            "armor", "saturation", "pearl_timer", "fps", "ping");
    private static final java.util.Set<String> CLEAN = java.util.Set.of("fps", "cps", "ping");
    private static final java.util.Set<String> NOT_IN_FULL = java.util.Set.of("spotify", "memory", "badge", "waypoints", "speed");

    /** Gotowe zestawy widgetow: 0 = Minimal PvP, 1 = Full PvP, 2 = Clean. */
    private static void applyPreset(int p) {
        switch (p) {
            case 0 -> ModuleManager.applyPreset(MINIMAL::contains);
            case 1 -> ModuleManager.applyPreset(id -> !NOT_IN_FULL.contains(id));
            default -> ModuleManager.applyPreset(CLEAN::contains);
        }
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
        ctx.drawTextWithShadow(textRenderer, L.t("CONFIGS"), cx - 140, 22, HexTheme.RED);

        int y = 94;
        for (int i = 0; i < Math.min(6, names.size()); i++) {
            ctx.drawTextWithShadow(textRenderer, names.get(i), cx - 140, y + 6, HexTheme.TEXT);
            y += 24;
        }
        if (names.isEmpty()) {
            ctx.drawTextWithShadow(textRenderer, L.t("No saved configs."), cx - 140, 100, HexTheme.TEXT_DIM);
        }
        if (!status.isEmpty()) {
            ctx.drawCenteredTextWithShadow(textRenderer, status, cx, height - 66, HexTheme.TEXT);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
