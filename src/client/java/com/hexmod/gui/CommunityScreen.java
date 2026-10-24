package com.hexmod.gui;

import com.hexmod.module.Community;
import com.hexmod.module.L;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Spolecznosc: configi i paczki tekstur z listy w repozytorium hexmod-data (aktualizuje sie bez nowej wersji moda). */
public class CommunityScreen extends Screen {
    private static final int PER_PAGE = 5;
    private static int tab = 0; // 0 = configi, 1 = paczki tekstur

    private final Screen parent;
    private volatile Community.Data data = new Community.Data();
    private volatile boolean loading = false;
    private volatile boolean needsInit = false;
    private volatile String status = "";
    private int page = 0;

    public CommunityScreen(Screen parent) {
        super(Text.literal(L.t("Community")));
        this.parent = parent;
        reload();
    }

    private void reload() {
        if (loading) return;
        loading = true;
        CompletableFuture.runAsync(() -> {
            try {
                data = Community.load();
            } catch (IOException | RuntimeException ex) {
                status = L.t("Error: ") + ex.getMessage();
            } finally {
                loading = false;
                needsInit = true;
            }
        });
    }

    private List<Community.Entry> entries() {
        if (tab == 2) return List.of();
        return tab == 0 ? data.configs : data.packs;
    }

    @Override
    protected void init() {
        int cx = width / 2;
        String[] tabs = {"Configs", "Texture packs", "Guide"};
        for (int i = 0; i < tabs.length; i++) {
            final int t = i;
            addDrawableChild(ButtonWidget.builder(Text.literal(L.t(tabs[i])), b -> {
                tab = t;
                page = 0;
                status = "";
                clearAndInit();
            }).dimensions(cx - 140 + i * 94, 34, 92, 18).build());
        }

        List<Community.Entry> list = entries();
        int pages = Math.max(1, (list.size() + PER_PAGE - 1) / PER_PAGE);
        if (page >= pages) page = pages - 1;
        for (int i = 0; i < PER_PAGE; i++) {
            int idx = page * PER_PAGE + i;
            if (idx >= list.size()) break;
            final Community.Entry e = list.get(idx);
            addDrawableChild(ButtonWidget.builder(Text.literal(L.t(tab == 0 ? "Install" : "Download")), b -> act(e))
                    .dimensions(cx + 70, 70 + i * 26, 70, 20).build());
        }

        addDrawableChild(ButtonWidget.builder(Text.literal(L.t("Refresh")), b -> {
            status = "";
            reload();
        }).dimensions(cx - 140, height - 28, 80, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("<"), b -> {
            page = (page + pages - 1) % pages;
            clearAndInit();
        }).dimensions(cx - 56, height - 28, 30, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal(">"), b -> {
            page = (page + 1) % pages;
            clearAndInit();
        }).dimensions(cx - 22, height - 28, 30, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal(L.t("Back")), b -> client.setScreen(parent))
                .dimensions(cx + 20, height - 28, 120, 20).build());
    }

    private void act(Community.Entry e) {
        if (tab == 0) {
            status = L.t("Loading...");
            CompletableFuture.runAsync(() -> {
                try {
                    status = Community.installConfig(e)
                            ? L.t("Config installed: ") + e.name
                            : L.t("The config file is invalid.");
                } catch (IOException | RuntimeException ex) {
                    status = L.t("Error: ") + ex.getMessage();
                }
            });
        } else {
            status = L.t("Downloading: ") + e.name + "...";
            CompletableFuture.runAsync(() -> {
                try {
                    String file = Community.downloadPack(e);
                    status = L.t("Saved to resourcepacks: ") + file + ". " + L.t("Enable it in the game settings (Resource Packs).");
                } catch (IOException | RuntimeException ex) {
                    status = L.t("Download error: ") + ex.getMessage();
                }
            });
        }
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
        int cx = width / 2;
        ctx.fill(cx - 140 + tab * 94, 52, cx - 140 + tab * 94 + 92, 55, HexTheme.RED);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        int cx = width / 2;
        ctx.drawTextWithShadow(textRenderer, L.t("COMMUNITY"), cx - 140, 20, HexTheme.RED);
        if (tab == 2) {
            String[] steps = {
                    L.t("How to add your own textures to Community:"),
                    L.t("1. Edit textures in Tools > Texture Editor. The edited pack is saved in your resourcepacks folder (a folder, e.g. ending with _hexedit)."),
                    L.t("2. Right-click that folder > Send to > Compressed (zipped) folder. The .zip must contain pack.mcmeta."),
                    L.t("3. Send the .zip (up to 25 MB) to the admin, for example on Discord."),
                    L.t("4. The admin uploads it to hexmod-data/packs and adds it to community.json. After a few minutes everyone sees it in Tools > Community."),
                    L.t("Configs: Tools > Configs > Copy to clipboard, then send the text to the admin.")};
            int gy = 62;
            for (int i = 0; i < steps.length; i++) {
                for (var line : textRenderer.wrapLines(Text.literal(steps[i]), 290)) {
                    ctx.drawTextWithShadow(textRenderer, line, cx - 140, gy, i == 0 ? HexTheme.RED : HexTheme.TEXT);
                    gy += 10;
                }
                gy += 4;
            }
            return;
        }
        List<Community.Entry> list = entries();
        for (int i = 0; i < PER_PAGE; i++) {
            int idx = page * PER_PAGE + i;
            if (idx >= list.size()) break;
            Community.Entry e = list.get(idx);
            int y = 70 + i * 26;
            ctx.drawTextWithShadow(textRenderer, textRenderer.trimToWidth(e.name, 200), cx - 140, y + 1, HexTheme.TEXT);
            var m = ctx.getMatrices();
            m.pushMatrix();
            m.translate((float) (cx - 140), (float) (y + 12));
            m.scale(0.75f, 0.75f);
            ctx.drawTextWithShadow(textRenderer, L.t("by ") + e.author, 0, 0, HexTheme.TEXT_DIM);
            m.popMatrix();
        }
        if (list.isEmpty()) {
            ctx.drawTextWithShadow(textRenderer, loading ? L.t("Loading...") : L.t("No entries yet."), cx - 140, 76, HexTheme.TEXT_DIM);
        }
        var m = ctx.getMatrices();
        m.pushMatrix();
        m.translate((float) (cx - 140), (float) (height - 64));
        m.scale(0.75f, 0.75f);
        ctx.drawTextWithShadow(textRenderer, L.t("Updates automatically. New entries appear without updating the mod."), 0, 0, HexTheme.TEXT_DIM);
        m.popMatrix();
        if (!status.isEmpty()) {
            for (var line : textRenderer.wrapLines(Text.literal(status), 290)) {
                ctx.drawCenteredTextWithShadow(textRenderer, line, cx, height - 52, HexTheme.TEXT);
            }
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
