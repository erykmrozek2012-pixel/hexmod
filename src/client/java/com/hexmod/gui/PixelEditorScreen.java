package com.hexmod.gui;

import com.hexmod.module.PackTools;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;

/** Prosty edytor pikseli PNG: olowek, gumka, wypelnienie, pipeta, cofanie i zapis do pakietu. */
public class PixelEditorScreen extends Screen {
    private enum Tool { PENCIL, ERASER, FILL, PICK }

    private static final int MAX = 64;
    private static final int[] PALETTE = {
            0xFF000000, 0xFFFFFFFF, 0xFF7F7F7F, 0xFFD10A0A, 0xFFFF7F00, 0xFFFFD60A, 0xFF30D158, 0xFF00A86B,
            0xFF64D2FF, 0xFF0A84FF, 0xFFBF5AF2, 0xFFFF6EB4, 0xFF8B5A2B, 0xFFC8A165, 0xFF3C3C3C, 0xFFDADADA};

    private final Screen parent;
    private final Path file;
    private int w, h, zoom, ox, oy;
    private int[] px;
    private boolean loaded;
    private Tool tool = Tool.PENCIL;
    private int color = 0xFFD10A0A;
    private final Deque<int[]> undo = new ArrayDeque<>();
    private boolean wasDown;
    private int lastX = -1, lastY = -1;
    private String status = "";
    private TextFieldWidget hexField;

    public PixelEditorScreen(Screen parent, Path file) {
        super(Text.literal("Edytor pikseli"));
        this.parent = parent;
        this.file = file;
        try {
            PackTools.Img img = PackTools.readImage(file);
            w = img.w();
            h = img.h();
            px = img.px();
            loaded = w <= MAX && h <= MAX;
            if (!loaded) status = "Obraz " + w + "x" + h + " jest za duzy (maksymalnie " + MAX + "x" + MAX + ").";
        } catch (IOException | RuntimeException e) {
            status = "Nie udalo sie wczytac: " + e.getMessage();
        }
    }

    private int palX() { return 216; }
    private int palY() { return height - 28; }

    @Override
    protected void init() {
        addDrawableChild(ButtonWidget.builder(Text.literal("Wstecz"), b -> client.setScreen(parent))
                .dimensions(6, height - 26, 84, 20).build());
        if (!loaded) return;

        String[] names = {"Olowek", "Gumka", "Wypelnij", "Pipeta"};
        Tool[] tools = Tool.values();
        for (int i = 0; i < 4; i++) {
            final Tool t = tools[i];
            addDrawableChild(ButtonWidget.builder(Text.literal(names[i]), b -> tool = t)
                    .dimensions(10, 20 + i * 22, 80, 20).build());
        }
        addDrawableChild(ButtonWidget.builder(Text.literal("Cofnij"), b -> {
            if (!undo.isEmpty()) px = undo.pop();
        }).dimensions(10, 114, 80, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Zapisz"), b -> save(false))
                .dimensions(10, 140, 80, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Zapisz+reload"), b -> save(true))
                .dimensions(10, 162, 80, 20).build());

        hexField = new TextFieldWidget(textRenderer, 96, height - 28, 70, 18, Text.literal("Kolor"));
        hexField.setPlaceholder(Text.literal("#RRGGBB"));
        hexField.setMaxLength(9);
        addDrawableChild(hexField);
        addDrawableChild(ButtonWidget.builder(Text.literal("Ustaw"), b -> setHex())
                .dimensions(168, height - 28, 44, 18).build());

        int areaX = 96, areaW = width - 96 - 8, areaH = height - 70;
        zoom = Math.max(1, Math.min(areaW / w, areaH / h));
        ox = areaX + (areaW - w * zoom) / 2;
        oy = 14 + (areaH - h * zoom) / 2;
    }

    private void setHex() {
        String s = hexField.getText().trim().replace("#", "");
        try {
            if (s.length() == 6) {
                color = 0xFF000000 | Integer.parseInt(s, 16);
            } else if (s.length() == 8) {
                long v = Long.parseLong(s, 16);
                color = (int) (((v & 0xFF) << 24) | (v >> 8));
            } else {
                status = "Wpisz kolor jako #RRGGBB.";
            }
        } catch (NumberFormatException e) {
            status = "Zly kolor. Przyklad: #FF0000";
        }
    }

    private void save(boolean reload) {
        try {
            PackTools.writeImage(file, new PackTools.Img(w, h, px));
            status = "Zapisano: " + file.getFileName();
            if (reload) {
                client.reloadResources();
                status += " (przeladowuje tekstury)";
            }
        } catch (IOException | RuntimeException e) {
            status = "Blad zapisu: " + e.getMessage();
        }
    }

    private void pushUndo() {
        undo.push(px.clone());
        while (undo.size() > 30) undo.removeLast();
    }

    private void line(int x0, int y0, int x1, int y1, int value) {
        int dx = Math.abs(x1 - x0), dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1, sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;
        while (true) {
            if (x0 >= 0 && y0 >= 0 && x0 < w && y0 < h) px[y0 * w + x0] = value;
            if (x0 == x1 && y0 == y1) break;
            int e2 = 2 * err;
            if (e2 > -dy) { err -= dy; x0 += sx; }
            if (e2 < dx) { err += dx; y0 += sy; }
        }
    }

    private void fill(int x, int y, int value) {
        int target = px[y * w + x];
        if (target == value) return;
        Deque<Integer> stack = new ArrayDeque<>();
        stack.push(y * w + x);
        while (!stack.isEmpty()) {
            int i = stack.pop();
            if (px[i] != target) continue;
            px[i] = value;
            int cx = i % w, cy = i / w;
            if (cx > 0) stack.push(i - 1);
            if (cx < w - 1) stack.push(i + 1);
            if (cy > 0) stack.push(i - w);
            if (cy < h - 1) stack.push(i + w);
        }
    }

    private void handleMouse(int mx, int my) {
        long win = client.getWindow().getHandle();
        boolean down = GLFW.glfwGetMouseButton(win, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        int cx = Math.floorDiv(mx - ox, zoom), cy = Math.floorDiv(my - oy, zoom);
        boolean inside = cx >= 0 && cy >= 0 && cx < w && cy < h;

        if (down && !wasDown) {
            if (inside) {
                pushUndo();
                switch (tool) {
                    case PENCIL -> px[cy * w + cx] = color;
                    case ERASER -> px[cy * w + cx] = 0;
                    case FILL -> fill(cx, cy, color);
                    case PICK -> {
                        int c = px[cy * w + cx];
                        if ((c >>> 24) != 0) color = c;
                        tool = Tool.PENCIL;
                    }
                }
                lastX = cx;
                lastY = cy;
            } else {
                for (int i = 0; i < PALETTE.length; i++) {
                    int sx = palX() + i * 14;
                    if (mx >= sx && mx < sx + 12 && my >= palY() && my < palY() + 12) color = PALETTE[i];
                }
            }
        } else if (down && inside && lastX >= 0) {
            if (tool == Tool.PENCIL) line(lastX, lastY, cx, cy, color);
            else if (tool == Tool.ERASER) line(lastX, lastY, cx, cy, 0);
            lastX = cx;
            lastY = cy;
        }
        if (!down) { lastX = -1; lastY = -1; }
        wasDown = down;
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, width, height, HexTheme.OVERLAY);
        ctx.fill(4, 4, width - 4, height - 2, HexTheme.PANEL);
        ctx.fill(4, 4, width - 4, 7, HexTheme.RED);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawTextWithShadow(textRenderer, file.getFileName().toString(), 10, 10, HexTheme.RED);
        if (!status.isEmpty()) {
            ctx.drawTextWithShadow(textRenderer, textRenderer.trimToWidth(status, width - 110), 100, height - 42, HexTheme.TEXT);
        }
        if (!loaded) return;

        handleMouse(mouseX, mouseY);

        ctx.fill(ox - 1, oy - 1, ox + w * zoom + 1, oy + h * zoom + 1, 0xFF000000);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int x0 = ox + x * zoom, y0 = oy + y * zoom;
                ctx.fill(x0, y0, x0 + zoom, y0 + zoom, ((x + y) & 1) == 0 ? 0xFF808080 : 0xFFA0A0A0);
                int c = px[y * w + x];
                if ((c >>> 24) != 0) ctx.fill(x0, y0, x0 + zoom, y0 + zoom, c);
            }
        }

        int row = tool.ordinal();
        ctx.fill(4, 22 + row * 22, 8, 38 + row * 22, HexTheme.RED);

        for (int i = 0; i < PALETTE.length; i++) {
            int sx = palX() + i * 14;
            ctx.fill(sx - 1, palY() - 1, sx + 13, palY() + 13, 0xFF000000);
            ctx.fill(sx, palY(), sx + 12, palY() + 12, PALETTE[i]);
        }
        ctx.fill(96, 6 + 0, 108, 18, 0xFF000000);
        ctx.fill(97, 7, 107, 17, color);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
