package com.hexmod.gui;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hexmod.module.Remote;
import com.hexmod.module.Web;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Panel admina (tylko nick er__is). Przygotowuje pliki blocklist.json i catalog.json,
 * ktore kopiujesz do repozytorium hexmod-data na GitHubie. Zmieniac je moze tylko wlasciciel repo.
 */
public class AdminScreen extends Screen {
    private final Screen parent;
    private int mode = 0; // 0 = blokady, 1 = katalog
    private final List<Remote.BlockedEntry> blocks = new CopyOnWriteArrayList<>(Remote.blocklist().blocked);
    private final List<Remote.CatalogEntry> mods = new CopyOnWriteArrayList<>(Remote.catalog().mods);
    private volatile boolean needsInit;
    private volatile String status = "";
    private TextFieldWidget fieldA, fieldB;

    public AdminScreen(Screen parent) {
        super(Text.literal("Panel admina"));
        this.parent = parent;
    }

    private static String dashed(String id) {
        if (id.length() != 32) return id;
        return id.substring(0, 8) + "-" + id.substring(8, 12) + "-" + id.substring(12, 16)
                + "-" + id.substring(16, 20) + "-" + id.substring(20);
    }

    @Override
    protected void init() {
        if (!Remote.isAdmin()) {
            client.setScreen(parent);
            return;
        }
        int cx = width / 2;

        addDrawableChild(ButtonWidget.builder(Text.literal("Blokady"), b -> { mode = 0; status = ""; clearAndInit(); })
                .dimensions(cx - 140, 28, 135, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Katalog modow"), b -> { mode = 1; status = ""; clearAndInit(); })
                .dimensions(cx + 5, 28, 135, 20).build());

        if (mode == 0) {
            fieldA = new TextFieldWidget(textRenderer, cx - 140, 56, 180, 20, Text.literal("Nick"));
            fieldA.setPlaceholder(Text.literal("Nick do zablokowania"));
            fieldA.setMaxLength(16);
            addDrawableChild(fieldA);
            addDrawableChild(ButtonWidget.builder(Text.literal("Zablokuj"), b -> block(fieldA.getText().trim()))
                    .dimensions(cx + 46, 56, 94, 20).build());

            int y = 84;
            for (int i = 0; i < Math.min(6, blocks.size()); i++) {
                final Remote.BlockedEntry e = blocks.get(i);
                addDrawableChild(ButtonWidget.builder(Text.literal("Odblokuj"), b -> {
                    blocks.remove(e);
                    clearAndInit();
                }).dimensions(cx + 70, y, 70, 20).build());
                y += 24;
            }
            addDrawableChild(ButtonWidget.builder(Text.literal("Kopiuj blocklist.json"), b -> {
                Remote.Blocklist out = new Remote.Blocklist();
                out.blocked = List.copyOf(blocks);
                client.keyboard.setClipboard(new GsonBuilder().setPrettyPrinting().create().toJson(out));
                status = "Skopiowano. Wklej do blocklist.json w repozytorium hexmod-data.";
            }).dimensions(cx - 140, height - 54, 280, 20).build());
        } else {
            fieldA = new TextFieldWidget(textRenderer, cx - 140, 56, 120, 20, Text.literal("Modrinth"));
            fieldA.setPlaceholder(Text.literal("Nazwa na Modrinth"));
            fieldA.setMaxLength(48);
            addDrawableChild(fieldA);
            fieldB = new TextFieldWidget(textRenderer, cx - 16, 56, 96, 20, Text.literal("Nazwa"));
            fieldB.setPlaceholder(Text.literal("Nazwa w menu"));
            fieldB.setMaxLength(24);
            addDrawableChild(fieldB);
            addDrawableChild(ButtonWidget.builder(Text.literal("Dodaj"), b -> {
                String slug = fieldA.getText().trim();
                if (slug.isEmpty()) { status = "Wpisz nazwe moda z Modrinth (z adresu strony)."; return; }
                Remote.CatalogEntry e = new Remote.CatalogEntry();
                e.modrinth = slug;
                e.id = slug;
                e.name = fieldB.getText().trim().isEmpty() ? slug : fieldB.getText().trim();
                mods.add(e);
                clearAndInit();
            }).dimensions(cx + 84, 56, 56, 20).build());

            int y = 84;
            for (int i = 0; i < Math.min(6, mods.size()); i++) {
                final Remote.CatalogEntry e = mods.get(i);
                addDrawableChild(ButtonWidget.builder(Text.literal("Usun"), b -> {
                    mods.remove(e);
                    clearAndInit();
                }).dimensions(cx + 84, y, 56, 20).build());
                y += 24;
            }
            addDrawableChild(ButtonWidget.builder(Text.literal("Kopiuj catalog.json"), b -> {
                Remote.Catalog out = new Remote.Catalog();
                out.mods = List.copyOf(mods);
                client.keyboard.setClipboard(new GsonBuilder().setPrettyPrinting().create().toJson(out));
                status = "Skopiowano. Wklej do catalog.json w repozytorium hexmod-data.";
            }).dimensions(cx - 140, height - 54, 280, 20).build());
        }

        addDrawableChild(ButtonWidget.builder(Text.literal("Wstecz"), b -> client.setScreen(parent))
                .dimensions(cx - 50, height - 30, 100, 20).build());
    }

    private void block(String nick) {
        if (nick.isEmpty()) { status = "Wpisz nick."; return; }
        status = "Szukam gracza " + nick + "...";
        CompletableFuture.runAsync(() -> {
            try {
                JsonObject o = JsonParser.parseString(
                        Web.get("https://api.mojang.com/users/profiles/minecraft/" + nick)).getAsJsonObject();
                Remote.BlockedEntry e = new Remote.BlockedEntry();
                e.uuid = dashed(o.get("id").getAsString());
                e.nick = o.get("name").getAsString();
                blocks.add(e);
                status = "Dodano do listy: " + e.nick;
            } catch (IOException | RuntimeException ex) {
                status = "Nie znaleziono gracza (" + nick + ").";
            }
            needsInit = true;
        });
    }

    @Override
    public void tick() {
        if (needsInit) {
            needsInit = false;
            clearAndInit();
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
        ctx.drawTextWithShadow(textRenderer, "PANEL ADMINA", cx - 140, 14, HexTheme.RED);
        int y = 84;
        if (mode == 0) {
            for (int i = 0; i < Math.min(6, blocks.size()); i++) {
                ctx.drawTextWithShadow(textRenderer, blocks.get(i).nick, cx - 140, y + 6, HexTheme.TEXT);
                y += 24;
            }
            if (blocks.isEmpty()) ctx.drawTextWithShadow(textRenderer, "Nikt nie jest zablokowany.", cx - 140, 90, HexTheme.TEXT_DIM);
        } else {
            for (int i = 0; i < Math.min(6, mods.size()); i++) {
                Remote.CatalogEntry e = mods.get(i);
                ctx.drawTextWithShadow(textRenderer, e.name + " (" + e.modrinth + ")", cx - 140, y + 6, HexTheme.TEXT);
                y += 24;
            }
            if (mods.isEmpty()) ctx.drawTextWithShadow(textRenderer, "Katalog jest pusty.", cx - 140, 90, HexTheme.TEXT_DIM);
        }
        if (!status.isEmpty()) {
            ctx.drawCenteredTextWithShadow(textRenderer, status, cx, height - 68, HexTheme.TEXT);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
