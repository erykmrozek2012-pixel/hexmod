package com.hexmod.gui;

import com.hexmod.module.PackTools;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Edytor tekstur: wybierz pakiet z folderu resourcepacks, potem Itemy PvP, wszystkie tekstury albo obraz na tarcze. */
public class TexturePackScreen extends Screen {
    private static final int ROWS = 7;

    private static Path lastFolder = null;
    private static String lastName = "";

    private final Screen parent;
    private List<Path> packs = List.of();
    private int offset = 0;
    private Path folder = lastFolder;
    private String packName = lastName;
    private String status = "";

    public TexturePackScreen(Screen parent) {
        super(Text.literal("Edytor tekstur"));
        this.parent = parent;
    }

    private void select(Path p) {
        try {
            folder = PackTools.extractIfZip(p);
            packName = p.getFileName().toString();
            lastFolder = folder;
            lastName = packName;
            status = Files.isDirectory(p) ? "" : "Pakiet .zip rozpakowano do kopii: " + folder.getFileName();
        } catch (IOException e) {
            status = "Blad: " + e.getMessage();
        }
        clearAndInit();
    }

    @Override
    protected void init() {
        int cx = width / 2;
        if (folder == null) {
            packs = PackTools.listPacks();
            int y = 50;
            for (int i = offset; i < Math.min(packs.size(), offset + ROWS); i++) {
                final Path p = packs.get(i);
                String label = textRenderer.trimToWidth(p.getFileName().toString(), 250);
                addDrawableChild(ButtonWidget.builder(Text.literal(label), b -> select(p))
                        .dimensions(cx - 140, y, 250, 20).build());
                y += 22;
            }
            addDrawableChild(ButtonWidget.builder(Text.literal("^"), b -> {
                offset = Math.max(0, offset - ROWS);
                clearAndInit();
            }).dimensions(cx + 116, 50, 24, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("v"), b -> {
                if (offset + ROWS < packs.size()) offset += ROWS;
                clearAndInit();
            }).dimensions(cx + 116, 72, 24, 20).build());
        } else {
            addDrawableChild(ButtonWidget.builder(Text.literal("Itemy PvP (szybki wybor)"),
                    b -> client.setScreen(new PvpItemsScreen(this, folder)))
                    .dimensions(cx - 140, 50, 280, 24).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("Wszystkie tekstury (foldery i szukanie)"), b -> {
                Path tex = PackTools.texturesDir(folder);
                if (Files.isDirectory(tex)) client.setScreen(new TextureBrowserScreen(this, tex));
                else status = "Ten pakiet nie ma jeszcze folderu tekstur. Najpierw edytuj item w 'Itemy PvP'.";
            }).dimensions(cx - 140, 78, 280, 22).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("Obraz na tarcze"),
                    b -> client.setScreen(new ShieldImageScreen(this, folder)))
                    .dimensions(cx - 140, 104, 280, 22).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("Przeladuj tekstury w grze"), b -> {
                client.reloadResources();
                status = "Przeladowuje tekstury...";
            }).dimensions(cx - 140, 130, 280, 22).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("Zmien pakiet"), b -> {
                folder = null;
                lastFolder = null;
                status = "";
                clearAndInit();
            }).dimensions(cx - 140, 156, 280, 22).build());
        }
        addDrawableChild(ButtonWidget.builder(Text.literal("Wstecz"), b -> client.setScreen(parent))
                .dimensions(cx - 50, height - 28, 100, 20).build());
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, width, height, HexTheme.OVERLAY);
        ctx.fill(width / 2 - 150, 4, width / 2 + 150, height - 2, HexTheme.PANEL);
        ctx.fill(width / 2 - 150, 4, width / 2 + 150, 7, HexTheme.RED);
    }

    private int wrapped(DrawContext ctx, String text, int x, int y, int maxW, int color) {
        for (OrderedText line : textRenderer.wrapLines(Text.literal(text), maxW)) {
            ctx.drawTextWithShadow(textRenderer, line, x, y, color);
            y += 10;
        }
        return y;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        int cx = width / 2;
        int x = cx - 140;
        ctx.drawTextWithShadow(textRenderer, "EDYTOR TEKSTUR", x, 14, HexTheme.RED);
        if (folder == null) {
            ctx.drawTextWithShadow(textRenderer, "Wybierz pakiet tekstur do edycji:", x, 32, HexTheme.TEXT);
            if (packs.isEmpty()) {
                wrapped(ctx, "Nie znaleziono pakietow w folderze resourcepacks.", x, 54, 280, HexTheme.TEXT_DIM);
            }
        } else {
            ctx.drawTextWithShadow(textRenderer, "Pakiet: " + textRenderer.trimToWidth(packName, 230), x, 32, HexTheme.TEXT);
            wrapped(ctx, "Aby zobaczyc zmiany, pakiet musi byc wlaczony w ustawieniach gry (Pakiety zasobow).",
                    x, 184, 280, HexTheme.TEXT_DIM);
        }
        if (!status.isEmpty()) {
            wrapped(ctx, status, x, height - 62, 280, HexTheme.TEXT);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
