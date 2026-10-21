package com.hexmod.gui;

import com.hexmod.module.L;
import com.hexmod.module.Browser;
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
import java.util.stream.Stream;

/** Obraz na tarczy: wybierz PNG z folderu hexmod/shield, a mod wstawi go na przod tarczy w pakiecie. */
public class ShieldImageScreen extends Screen {
    private static int quality = 8;
    private static boolean flip = false;

    private final Screen parent;
    private final Path packFolder;
    private List<Path> images = List.of();
    private String status = "";

    public ShieldImageScreen(Screen parent, Path packFolder) {
        super(Text.literal(L.t("Shield Image")));
        this.parent = parent;
        this.packFolder = packFolder;
    }

    private List<Path> listImages() {
        try {
            Files.createDirectories(PackTools.shieldDir());
            try (Stream<Path> s = Files.list(PackTools.shieldDir())) {
                return s.filter(p -> p.getFileName().toString().toLowerCase().endsWith(".png")).sorted().toList();
            }
        } catch (IOException e) {
            return List.of();
        }
    }

    @Override
    protected void init() {
        int cx = width / 2;
        images = listImages();
        int y = 76;
        for (int i = 0; i < Math.min(4, images.size()); i++) {
            final Path p = images.get(i);
            String label = textRenderer.trimToWidth(L.t("Use: ") + p.getFileName(), 270);
            addDrawableChild(ButtonWidget.builder(Text.literal(label), b -> {
                status = PackTools.applyShield(packFolder, p, quality, flip);
            }).dimensions(cx - 140, y, 280, 18).build());
            y += 20;
        }
        int by = height - 74;
        addDrawableChild(ButtonWidget.builder(Text.literal(L.t("Quality: ") + quality + "x (" + (64 * quality) + "px)"), b -> {
            quality = quality >= 16 ? 2 : quality * 2;
            clearAndInit();
        }).dimensions(cx - 140, by - 24, 138, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal(L.t("Mirror horizontally: ") + (flip ? L.t("ON") : L.t("OFF"))), b -> {
            flip = !flip;
            clearAndInit();
        }).dimensions(cx + 2, by - 24, 138, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal(L.t("Open folder")), b -> {
            try {
                Files.createDirectories(PackTools.shieldDir());
            } catch (IOException ignored) {}
            if (!Browser.open(PackTools.shieldDir().toAbsolutePath().toString())) {
                status = L.t("Cannot open the folder: ") + PackTools.shieldDir().toAbsolutePath();
            }
        }).dimensions(cx - 140, by, 138, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal(L.t("Refresh list")), b -> clearAndInit())
                .dimensions(cx + 2, by, 138, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal(L.t("Reload textures in game")), b -> {
            client.reloadResources();
            status = L.t("Reloading textures...");
        }).dimensions(cx - 140, by + 24, 280, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal(L.t("Back")), b -> client.setScreen(parent))
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
        int x = width / 2 - 140;
        ctx.drawTextWithShadow(textRenderer, L.t("SHIELD IMAGE"), x, 14, HexTheme.RED);
        wrapped(ctx, L.t("1. Click 'Open folder' and put your PNG file there.  2. Click 'Refresh list'.  ")
                + L.t("3. Click 'Use' next to the image."), x, 28, 280, HexTheme.TEXT_DIM);
        if (images.isEmpty()) {
            ctx.drawTextWithShadow(textRenderer, L.t("No PNG images in the folder."), x, 80, HexTheme.TEXT_DIM);
        }
        if (!status.isEmpty()) {
            wrapped(ctx, status, x, height - 124, 280, HexTheme.TEXT);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
