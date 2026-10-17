package com.hexmod.gui;

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

    public ConfigScreen(Screen parent) {
        super(Text.literal("Configi"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int cx = width / 2;

        nameField = new TextFieldWidget(textRenderer, cx - 140, 40, 180, 20, Text.literal("Nazwa"));
        nameField.setPlaceholder(Text.literal("Nazwa configu"));
        nameField.setMaxLength(24);
        addDrawableChild(nameField);

        addDrawableChild(ButtonWidget.builder(Text.literal("Zapisz"), b -> {
            status = ModuleManager.saveConfig(nameField.getText())
                    ? "Zapisano config." : "Wpisz nazwe (litery, cyfry, - i _).";
            clearAndInit();
        }).dimensions(cx + 46, 40, 94, 20).build());

        names = ModuleManager.listConfigs();
        int y = 72;
        for (int i = 0; i < Math.min(7, names.size()); i++) {
            final String n = names.get(i);
            addDrawableChild(ButtonWidget.builder(Text.literal("Wczytaj"), b -> {
                status = ModuleManager.loadConfig(n) ? "Wczytano: " + n : "Nie udalo sie wczytac.";
            }).dimensions(cx + 10, y, 70, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("Usun"), b -> {
                ModuleManager.deleteConfig(n);
                status = "Usunieto: " + n;
                clearAndInit();
            }).dimensions(cx + 84, y, 56, 20).build());
            y += 24;
        }

        int by = height - 52;
        addDrawableChild(ButtonWidget.builder(Text.literal("Kopiuj do schowka"), b -> {
            client.keyboard.setClipboard(ModuleManager.exportJson());
            status = "Skopiowano config do schowka. Wyslij go znajomemu.";
        }).dimensions(cx - 140, by, 135, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Wklej ze schowka"), b -> {
            status = ModuleManager.importJson(client.keyboard.getClipboard())
                    ? "Zaimportowano config ze schowka." : "Schowek nie zawiera poprawnego configu.";
        }).dimensions(cx + 5, by, 135, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Wstecz"), b -> client.setScreen(parent))
                .dimensions(cx - 50, by + 26, 100, 20).build());
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
        ctx.drawTextWithShadow(textRenderer, "CONFIGI", cx - 140, 22, HexTheme.RED);

        int y = 72;
        for (int i = 0; i < Math.min(7, names.size()); i++) {
            ctx.drawTextWithShadow(textRenderer, names.get(i), cx - 140, y + 6, HexTheme.TEXT);
            y += 24;
        }
        if (names.isEmpty()) {
            ctx.drawTextWithShadow(textRenderer, "Brak zapisanych configow.", cx - 140, 78, HexTheme.TEXT_DIM);
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
