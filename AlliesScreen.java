package com.hexmod.gui;

import com.hexmod.module.AlliesManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;

import java.util.List;

/** Lista sojusznikow: dodawanie po nicku albo z celownika, usuwanie. */
public class AlliesScreen extends Screen {
    private final Screen parent;
    private TextFieldWidget nameField;
    private List<String> names = List.of();
    private String status = "";

    public AlliesScreen(Screen parent) {
        super(Text.literal("Sojusznicy"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int cx = width / 2;
        nameField = new TextFieldWidget(textRenderer, cx - 140, 40, 180, 20, Text.literal("Nick"));
        nameField.setPlaceholder(Text.literal("Nick gracza"));
        nameField.setMaxLength(16);
        addDrawableChild(nameField);

        addDrawableChild(ButtonWidget.builder(Text.literal("Dodaj"), b -> {
            status = AlliesManager.add(nameField.getText()) ? "Dodano sojusznika." : "Wpisz nick (lub juz jest na liscie).";
            clearAndInit();
        }).dimensions(cx + 46, 40, 94, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Dodaj gracza z celownika"), b -> {
            Entity t = client.targetedEntity;
            if (t instanceof PlayerEntity p) {
                status = AlliesManager.add(p.getName().getString()) ? "Dodano: " + p.getName().getString() : "Juz jest na liscie.";
            } else {
                status = "Nie patrzysz na gracza.";
            }
            clearAndInit();
        }).dimensions(cx - 140, 66, 280, 20).build());

        names = AlliesManager.list();
        int y = 96;
        for (int i = 0; i < Math.min(7, names.size()); i++) {
            final String n = names.get(i);
            addDrawableChild(ButtonWidget.builder(Text.literal("Usun"), b -> {
                AlliesManager.remove(n);
                status = "Usunieto: " + n;
                clearAndInit();
            }).dimensions(cx + 84, y, 56, 20).build());
            y += 24;
        }

        addDrawableChild(ButtonWidget.builder(Text.literal("Wstecz"), b -> client.setScreen(parent))
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
        ctx.drawTextWithShadow(textRenderer, "SOJUSZNICY", cx - 140, 22, HexTheme.RED);
        int y = 96;
        for (int i = 0; i < Math.min(7, names.size()); i++) {
            ctx.drawTextWithShadow(textRenderer, "\u2605 " + names.get(i), cx - 140, y + 6, HexTheme.TEXT);
            y += 24;
        }
        if (names.isEmpty()) {
            ctx.drawTextWithShadow(textRenderer, "Brak sojusznikow.", cx - 140, 102, HexTheme.TEXT_DIM);
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
