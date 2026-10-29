package com.hexmod.gui;

import com.hexmod.module.L;
import com.hexmod.module.PackTools;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/** Prosty edytor pikseli PNG: olowek, gumka, wypelnienie, pipeta, cofanie i zapis do pakietu. */
public class PixelEditorScreen extends Screen {
    private enum Tool { PENCIL, ERASER, FILL, PICK }

    private static final int MAX = 64;
    private static final int[] PALETTE = {
            0xFFD10A0A, 0xFFFF3B30, 0xFFFF6A00, 0xFFFF9500, 0xFFFFD60A, 0xFFFFE680, 0xFFBFFF00, 0xFF30D158,
            0xFF00A86B, 0xFF00C7BE, 0xFF64D2FF, 0xFF32ADE6, 0xFF0A84FF, 0xFF5E5CE6, 0xFFBF5AF2, 0xFFFF6EB4,
            0xFFFFFFFF, 0xFFE5E5EA, 0xFFC7C7CC, 0xFF8E8E93, 0xFF636366, 0xFF3A3A3C, 0xFF1C1C1E, 0xFF000000,
            0xFF8B5A2B, 0xFFC8A165, 0xFFA2845E, 0xFF7A0505, 0xFF1E6B3A, 0xFF2C3E6B, 0xFF6B2C5E, 0xFF40E0D0};

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

    // --- wybor koloru jak w Paincie: kwadrat nasycenie/jasnosc + pasek odcienia + wlasne kolory ---
    private static final int SB_W = 128;
    private static final int SV_W = 100, SV_H = 80, HUE_W = 14;
    private static final int FAV_PAGE = 16;
    private static final int FAV_LIMIT = 1000;
    private int favPage = 0;
    private static final List<Integer> FAV = new ArrayList<>();
    private static boolean favLoaded = false;
    private float hue = 0f, sat = 1f, val = 1f;
    private int drag = 0; // 0 nic, 1 kwadrat, 2 pasek odcienia
    private final int[] svCache = new int[50 * 40];
    private float cacheHue = -1f;

    public PixelEditorScreen(Screen parent, Path file) {
        super(Text.literal(L.t("Pixel Editor")));
        this.parent = parent;
        this.file = file;
        loadFavs();
        setColor(color);
        try {
            PackTools.Img img = PackTools.readImage(file);
            w = img.w();
            h = img.h();
            px = img.px();
            loaded = w <= MAX && h <= MAX;
            if (!loaded) status = L.t("Image ") + w + "x" + h + L.t(" is too large (maximum ") + MAX + "x" + MAX + ").";
        } catch (IOException | RuntimeException e) {
            status = L.t("Could not load: ") + e.getMessage();
        }
    }

    private int palY() { return height - 84; }

    @Override
    protected void init() {
        addDrawableChild(ButtonWidget.builder(Text.literal(L.t("Back")), b -> client.setScreen(parent))
                .dimensions(6, height - 26, 84, 20).build());
        if (!loaded) return;

        String[] names = {L.t("Pencil"), L.t("Eraser"), L.t("Fill"), L.t("Picker")};
        Tool[] tools = Tool.values();
        for (int i = 0; i < 4; i++) {
            final Tool t = tools[i];
            addDrawableChild(ButtonWidget.builder(Text.literal(names[i]), b -> tool = t)
                    .dimensions(10, 20 + i * 22, 80, 20).build());
        }
        addDrawableChild(ButtonWidget.builder(Text.literal(L.t("Undo")), b -> {
            if (!undo.isEmpty()) px = undo.pop();
        }).dimensions(10, 114, 80, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal(L.t("Save")), b -> save(false))
                .dimensions(10, 140, 80, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal(L.t("Save+reload")), b -> save(true))
                .dimensions(10, 162, 80, 20).build());

        hexField = new TextFieldWidget(textRenderer, 96, height - 28, 70, 18, Text.literal(L.t("Color")));
        hexField.setPlaceholder(Text.literal("#RRGGBB"));
        hexField.setMaxLength(9);
        addDrawableChild(hexField);
        addDrawableChild(ButtonWidget.builder(Text.literal(L.t("Set")), b -> setHex())
                .dimensions(168, height - 28, 44, 18).build());

        addDrawableChild(ButtonWidget.builder(Text.literal(L.t("Save color")), b -> addFav())
                .dimensions(sbX(), 144, 76, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal(L.t("Remove")), b -> removeFav())
                .dimensions(sbX() + 80, 144, 40, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("<"), b -> favPage = Math.max(0, favPage - 1))
                .dimensions(sbX(), 166, 20, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal(">"), b -> favPage = Math.min(favPages() - 1, favPage + 1))
                .dimensions(sbX() + 24, 166, 20, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal(L.t("More colors")), b ->
                client.setScreen(new ColorPickerScreen(this, color & 0xFFFFFF, c -> setColor(0xFF000000 | c), null)))
                .dimensions(sbX() + 48, 166, 76, 18).build());

        int areaX = 96, areaW = width - 96 - 8 - SB_W, areaH = height - 70;
        zoom = Math.max(1, Math.min(areaW / w, areaH / h));
        ox = areaX + (areaW - w * zoom) / 2;
        oy = 14 + (areaH - h * zoom) / 2;
    }

    private void setHex() {
        String s = hexField.getText().trim().replace("#", "");
        try {
            if (s.length() == 6) {
                setColor(0xFF000000 | Integer.parseInt(s, 16));
            } else if (s.length() == 8) {
                long v = Long.parseLong(s, 16);
                setColor((int) (((v & 0xFF) << 24) | (v >> 8)));
            } else {
                status = L.t("Enter a color as #RRGGBB.");
            }
        } catch (NumberFormatException e) {
            status = L.t("Invalid color. Example: #FF0000");
        }
    }

    private void save(boolean reload) {
        try {
            PackTools.writeImage(file, new PackTools.Img(w, h, px));
            status = L.t("Saved: ") + file.getFileName();
            if (reload) {
                client.reloadResources();
                status += L.t(" (reloading textures)");
            }
        } catch (IOException | RuntimeException e) {
            status = L.t("Save error: ") + e.getMessage();
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

        int sx0 = sbX(), sy0 = 16;
        if (down && !wasDown && !inside) {
            if (mx >= sx0 && mx < sx0 + SV_W && my >= sy0 && my < sy0 + SV_H) {
                drag = 1;
            } else if (mx >= sx0 + SV_W + 6 && mx < sx0 + SV_W + 6 + HUE_W && my >= sy0 && my < sy0 + SV_H) {
                drag = 2;
            } else {
                for (int i = 0; i < FAV_PAGE; i++) {
                    int idx = favPage * FAV_PAGE + i;
                    if (idx >= FAV.size()) break;
                    int fx = sx0 + (i % 8) * 15, fy = 112 + (i / 8) * 14;
                    if (mx >= fx && mx < fx + 13 && my >= fy && my < fy + 12) setColor(FAV.get(idx));
                }
            }
        }
        if (!down) drag = 0;
        if (down && drag != 0) {
            if (drag == 1) {
                sat = clamp01((mx - sx0) / (float) (SV_W - 1));
                val = 1f - clamp01((my - sy0) / (float) (SV_H - 1));
            } else {
                hue = clamp01((my - sy0) / (float) (SV_H - 1)) * 0.9999f;
            }
            color = hsv(hue, sat, val);
            lastX = -1;
            lastY = -1;
            wasDown = true;
            return;
        }

        if (down && !wasDown) {
            if (inside) {
                pushUndo();
                switch (tool) {
                    case PENCIL -> px[cy * w + cx] = color;
                    case ERASER -> px[cy * w + cx] = 0;
                    case FILL -> fill(cx, cy, color);
                    case PICK -> {
                        int c = px[cy * w + cx];
                        if ((c >>> 24) != 0) setColor(c);
                        tool = Tool.PENCIL;
                    }
                }
                lastX = cx;
                lastY = cy;
            } else {
                int gx = Math.floorDiv(mx - 10, 10), gy = Math.floorDiv(my - palY(), 10);
                if (gx >= 0 && gx < 8 && gy >= 0 && gy < 4) setColor(PALETTE[gy * 8 + gx]);
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
            int sx = 10 + (i % 8) * 10, sy = palY() + (i / 8) * 10;
            ctx.fill(sx, sy, sx + 9, sy + 9, PALETTE[i]);
            if (PALETTE[i] == color) {
                ctx.fill(sx - 1, sy - 1, sx + 10, sy, HexTheme.RED);
                ctx.fill(sx - 1, sy + 9, sx + 10, sy + 10, HexTheme.RED);
                ctx.fill(sx - 1, sy, sx, sy + 9, HexTheme.RED);
                ctx.fill(sx + 9, sy, sx + 10, sy + 9, HexTheme.RED);
            }
        }
        ctx.fill(96, 6 + 0, 108, 18, 0xFF000000);
        ctx.fill(97, 7, 107, 17, color);
        ctx.drawTextWithShadow(textRenderer, String.format("#%06X", color & 0xFFFFFF), 112, 8, HexTheme.TEXT_DIM);
        drawPicker(ctx);
    }

    private int sbX() { return width - SB_W; }

    private static float clamp01(float v) { return v < 0f ? 0f : Math.min(v, 1f); }

    private static int hsv(float h, float sa, float v) {
        h = h - (float) Math.floor(h);
        float c = v * sa;
        float hp = h * 6f;
        float x = c * (1f - Math.abs(hp % 2f - 1f));
        float r = 0, g = 0, b = 0;
        switch ((int) hp) {
            case 0 -> { r = c; g = x; }
            case 1 -> { r = x; g = c; }
            case 2 -> { g = c; b = x; }
            case 3 -> { g = x; b = c; }
            case 4 -> { r = x; b = c; }
            default -> { r = c; b = x; }
        }
        float m = v - c;
        int ri = Math.round((r + m) * 255f), gi = Math.round((g + m) * 255f), bi = Math.round((b + m) * 255f);
        return 0xFF000000 | (ri << 16) | (gi << 8) | bi;
    }

    /** Ustawia kolor i dopasowuje do niego kwadrat i pasek odcienia. */
    private void setColor(int argb) {
        color = argb;
        float r = ((argb >> 16) & 255) / 255f, g = ((argb >> 8) & 255) / 255f, b = (argb & 255) / 255f;
        float max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b));
        float d = max - min;
        val = max;
        sat = max == 0f ? 0f : d / max;
        if (d > 0f) {
            float hh;
            if (max == r) hh = ((g - b) / d) % 6f;
            else if (max == g) hh = (b - r) / d + 2f;
            else hh = (r - g) / d + 4f;
            hue = hh / 6f;
            if (hue < 0f) hue += 1f;
        }
    }

    private void drawPicker(DrawContext ctx) {
        int sx0 = sbX(), sy0 = 16;
        if (cacheHue != hue) {
            cacheHue = hue;
            for (int yy = 0; yy < 40; yy++) {
                for (int xx = 0; xx < 50; xx++) svCache[yy * 50 + xx] = hsv(hue, xx / 49f, 1f - yy / 39f);
            }
        }
        ctx.fill(sx0 - 1, sy0 - 1, sx0 + SV_W + 1, sy0 + SV_H + 1, 0xFF000000);
        for (int yy = 0; yy < 40; yy++) {
            for (int xx = 0; xx < 50; xx++) {
                int px0 = sx0 + xx * 2, py0 = sy0 + yy * 2;
                ctx.fill(px0, py0, px0 + 2, py0 + 2, svCache[yy * 50 + xx]);
            }
        }
        int mx = sx0 + Math.round(sat * (SV_W - 1)), my = sy0 + Math.round((1f - val) * (SV_H - 1));
        ctx.fill(mx - 3, my - 3, mx + 4, my + 4, 0xFF000000);
        ctx.fill(mx - 2, my - 2, mx + 3, my + 3, 0xFFFFFFFF);
        ctx.fill(mx - 1, my - 1, mx + 2, my + 2, color);

        int hx = sx0 + SV_W + 6;
        ctx.fill(hx - 1, sy0 - 1, hx + HUE_W + 1, sy0 + SV_H + 1, 0xFF000000);
        for (int i = 0; i < 40; i++) ctx.fill(hx, sy0 + i * 2, hx + HUE_W, sy0 + i * 2 + 2, hsv(i / 40f, 1f, 1f));
        int hy = sy0 + Math.round(hue * (SV_H - 1));
        ctx.fill(hx - 2, hy - 1, hx + HUE_W + 2, hy + 2, 0xFF000000);
        ctx.fill(hx - 1, hy, hx + HUE_W + 1, hy + 1, 0xFFFFFFFF);

        ctx.drawTextWithShadow(textRenderer, L.t("Custom colors"), sx0, 100, HexTheme.TEXT_DIM);
        favPage = Math.min(favPage, favPages() - 1);
        for (int i = 0; i < FAV_PAGE; i++) {
            int idx = favPage * FAV_PAGE + i;
            int fx = sx0 + (i % 8) * 15, fy = 112 + (i / 8) * 14;
            ctx.fill(fx, fy, fx + 13, fy + 12, 0xFF000000);
            if (idx < FAV.size()) {
                ctx.fill(fx + 1, fy + 1, fx + 12, fy + 11, FAV.get(idx));
                if (FAV.get(idx) == color) ctx.fill(fx, fy + 12, fx + 13, fy + 13, HexTheme.RED);
            } else {
                ctx.fill(fx + 1, fy + 1, fx + 12, fy + 11, 0xFF2A2A2E);
            }
        }
    }

    private static int favPages() {
        return Math.max(1, (FAV.size() + FAV_PAGE - 1) / FAV_PAGE);
    }

    private static Path favFile() {
        return FabricLoader.getInstance().getConfigDir().resolve("hexmod").resolve("colors.txt");
    }

    private static void loadFavs() {
        if (favLoaded) return;
        favLoaded = true;
        try {
            if (!Files.exists(favFile())) return;
            for (String line : Files.readAllLines(favFile())) {
                String t = line.trim().replace("#", "");
                if (t.length() == 6 && FAV.size() < FAV_LIMIT) FAV.add(0xFF000000 | Integer.parseInt(t, 16));
            }
        } catch (IOException | RuntimeException ignored) {}
    }

    private static void saveFavs() {
        try {
            Files.createDirectories(favFile().getParent());
            List<String> lines = new ArrayList<>();
            for (int c : FAV) lines.add(String.format("%06X", c & 0xFFFFFF));
            Files.write(favFile(), lines);
        } catch (IOException ignored) {}
    }

    private void addFav() {
        int c = color | 0xFF000000;
        if (FAV.contains(c)) {
            status = L.t("This color is already saved.");
        } else {
            FAV.add(c);
            saveFavs();
            status = L.t("Saved color ") + String.format("#%06X", c & 0xFFFFFF);
        }
    }

    private void removeFav() {
        if (FAV.remove(Integer.valueOf(color | 0xFF000000))) {
            saveFavs();
            status = L.t("Color removed from the list.");
        } else {
            status = L.t("This color is not in the list.");
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
