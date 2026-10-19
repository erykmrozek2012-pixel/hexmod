package com.hexmod.gui;

import com.hexmod.module.PackTools;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.nio.file.Path;
import java.util.List;

/** Przegladanie folderow tekstur pakietu; klikniecie PNG otwiera edytor pikseli. */
public class TextureBrowserScreen extends Screen {
    private static final int ROWS = 8;

    private final Screen parent;
    private final Path root;
    private Path dir;
    private int offset = 0;
    private List<Path> entries = List.of();

    public TextureBrowserScreen(Screen parent, Path root) {
        super(Text.literal("Tekstury"));
        this.parent = parent;
        this.root = root;
        this.dir = root;
    }

    @Override
    protected void init() {
        int cx = width / 2;
        entries = PackTools.listEntries(dir);
        int y = 40;
        if (!dir.equals(root)) {
            addDrawableChild(ButtonWidget.builder(Text.literal("[..] wstecz"), b -> {
                dir = dir.getParent();
                offset = 0;
                clearAndInit();
            }).dimensions(cx - 140, y, 250, 18).build());
            y += 20;
        }
        int shown = 0;
        for (int i = offset; i < entries.size() && shown < ROWS; i++, shown++) {
            final Path p = entries.get(i);
            boolean isDir = java.nio.file.Files.isDirectory(p);
            String name = p.getFileName().toString();
            String label = textRenderer.trimToWidth(isDir ? "[" + name + "]" : name, 240);
            addDrawableChild(ButtonWidget.builder(Text.literal(label), b -> {
                if (isDir) {
                    dir = p;
                    offset = 0;
                    clearAndInit();
                } else {
                    client.setScreen(new PixelEditorScreen(this, p));
                }
            }).dimensions(cx - 140, y, 250, 18).build());
            y += 20;
        }
        addDrawableChild(ButtonWidget.builder(Text.literal("^"), b -> {
            offset = Math.max(0, offset - ROWS);
            clearAndInit();
        }).dimensions(cx + 116, 40, 24, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("v"), b -> {
            if (offset + ROWS < entries.size()) offset += ROWS;
            clearAndInit();
        }).dimensions(cx + 116, 62, 24, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Wstecz"), b -> client.setScreen(parent))
                .dimensions(cx - 50, height - 28, 100, 20).build());
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, width, height, HexTheme.OVERLAY);
        ctx.fill(width / 2 - 150, 4, width / 2 + 150, height - 2, HexTheme.PANEL);
        ctx.fill(width / 2 - 150, 4, width / 2 + 150, 7, HexTheme.RED);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        int x = width / 2 - 140;
        ctx.drawTextWithShadow(textRenderer, "TEKSTURY", x, 14, HexTheme.RED);
        String rel = root.relativize(dir).toString().replace('\\', '/');
        ctx.drawTextWithShadow(textRenderer, textRenderer.trimToWidth("/textures/" + rel, 280), x, 27, HexTheme.TEXT_DIM);
        if (entries.isEmpty()) {
            ctx.drawTextWithShadow(textRenderer, "Pusty folder.", x, 62, HexTheme.TEXT_DIM);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
