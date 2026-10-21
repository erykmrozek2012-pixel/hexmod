package com.hexmod.gui;

import com.hexmod.module.L;
import com.hexmod.module.ModCatalog;
import com.hexmod.module.Remote;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/** Katalog modow prowadzony przez admina: pobierz z Modrinth, potem wlaczaj/wylaczaj przelacznikiem. */
public class ModCatalogScreen extends Screen {
    private final Screen parent;
    private final Set<String> busy = ConcurrentHashMap.newKeySet();
    private volatile boolean needsInit;
    private volatile String status = "";
    private List<Remote.CatalogEntry> entries = List.of();

    public ModCatalogScreen(Screen parent) {
        super(Text.literal(L.t("Mod Catalog")));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int cx = width / 2;
        entries = Remote.catalog().mods;
        int y = 44;
        for (int i = 0; i < Math.min(7, entries.size()); i++) {
            Remote.CatalogEntry e = entries.get(i);
            final String slug = e.modrinth;
            if (busy.contains(slug)) { y += 26; continue; }
            ModCatalog.State st = ModCatalog.state(slug);
            if (st == ModCatalog.State.NOT_INSTALLED) {
                addDrawableChild(ButtonWidget.builder(Text.literal(L.t("Download")), b -> download(slug))
                        .dimensions(cx + 70, y, 70, 20).build());
            } else {
                addDrawableChild(new ToggleSwitch(cx + 104, y + 1, st == ModCatalog.State.ENABLED, on -> {
                    try {
                        ModCatalog.setEnabled(slug, on);
                        status = L.t("The change takes effect after restarting the game.");
                    } catch (IOException ex) {
                        status = L.t("Error: ") + ex.getMessage();
                    }
                }));
            }
            y += 26;
        }

        addDrawableChild(ButtonWidget.builder(Text.literal(L.t("Refresh catalog")), b -> {
            Remote.refresh();
            status = L.t("Refreshing... come back in a moment.");
        }).dimensions(cx - 140, height - 54, 135, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal(L.t("Back")), b -> client.setScreen(parent))
                .dimensions(cx + 5, height - 54, 135, 20).build());
    }

    private void download(String slug) {
        busy.add(slug);
        status = L.t("Downloading: ") + slug + "...";
        needsInit = true;
        CompletableFuture.runAsync(() -> {
            try {
                ModCatalog.install(slug);
                status = L.t("Downloaded. Restart the game to activate the mod.");
            } catch (IOException | RuntimeException ex) {
                status = L.t("Download error: ") + ex.getMessage();
            } finally {
                busy.remove(slug);
                needsInit = true;
            }
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
        ctx.drawTextWithShadow(textRenderer, L.t("MOD CATALOG"), cx - 140, 22, HexTheme.RED);
        int y = 44;
        for (int i = 0; i < Math.min(7, entries.size()); i++) {
            Remote.CatalogEntry e = entries.get(i);
            ctx.drawTextWithShadow(textRenderer, e.name.isEmpty() ? e.modrinth : e.name, cx - 140, y + 6, HexTheme.TEXT);
            if (busy.contains(e.modrinth)) {
                ctx.drawTextWithShadow(textRenderer, L.t("Downloading..."), cx + 60, y + 6, HexTheme.TEXT_DIM);
            }
            y += 26;
        }
        if (entries.isEmpty()) {
            ctx.drawTextWithShadow(textRenderer, L.t("The catalog is empty or could not be downloaded."), cx - 140, 50, HexTheme.TEXT_DIM);
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
