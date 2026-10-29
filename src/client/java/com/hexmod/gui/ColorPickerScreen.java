package com.hexmod.gui;

import com.hexmod.module.L;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.function.IntConsumer;

/**
 * Wspolne okno wyboru koloru: tabela 64 kolorow, kwadrat nasycenie/jasnosc + pasek odcienia,
 * pole #RRGGBB oraz wlasne kolory bez limitu (strony). Kazda zmiana trafia od razu do onChange.
 */
public class ColorPickerScreen extends Screen {
    private static final int W = 280, H = 236;
    private static final int PER_PAGE = 32;

    private final Screen parent;
    private final IntConsumer onChange;
    private final Runnable onDone;
    private int rgb;
    private float hue = 0f, sat = 1f, val = 1f;
    private int page = 0;
    private int dragMode = 0; // 0 nic, 1 kwadrat, 2 pasek odcienia
    private boolean wasDown;
    private TextFieldWidget hexField;
    private String status = "";

    public ColorPickerScreen(Screen parent, int rgb, IntConsumer onChange, Runnable onDone) {
        super(Text.literal(L.t("Pick a color")));
        this.parent = parent;
        this.onChange = onChange;
        this.onDone = onDone;
        this.rgb = rgb & 0xFFFFFF;
        syncHsv();
    }

    private int px() { return (width - W) / 2; }

    private int py() { return Math.max(4, (height - H) / 2); }

    private void syncHsv() {
        float[] hsv = ColorUtil.toHsv(rgb);
        if (hsv[1] > 0f) hue = hsv[0];
        sat = hsv[1];
        val = hsv[2];
    }

    private int maxPage() {
        return Math.max(0, (ColorStore.list().size() - 1) / PER_PAGE);
    }

    @Override
    protected void init() {
        int px = px(), py = py();
        hexField = new TextFieldWidget(textRenderer, px + 146, py + 108, 74, 18, Text.literal(L.t("Color")));
        hexField.setPlaceholder(Text.literal("#RRGGBB"));
        hexField.setMaxLength(7);
        hexField.setText(String.format("#%06X", rgb));
        addDrawableChild(hexField);
        addDrawableChild(ButtonWidget.builder(Text.literal(L.t("Set")), b -> applyHex())
                .dimensions(px + 224, py + 108, 40, 18).build());

        int by = py + H - 26;
        addDrawableChild(ButtonWidget.builder(Text.literal(L.t("Save color")), b -> {
            ColorStore.add(rgb);
            page = 0;
        }).dimensions(px + 8, by, 80, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal(L.t("Remove")), b -> {
            ColorStore.remove(rgb);
            page = Math.min(page, maxPage());
        }).dimensions(px + 92, by, 56, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("<"), b -> page = Math.max(0, page - 1))
                .dimensions(px + 152, by, 18, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal(">"), b -> page = Math.min(maxPage(), page + 1))
                .dimensions(px + 172, by, 18, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal(L.t("Done")), b -> close())
                .dimensions(px + 224, by, 48, 18).build());
    }

    private void applyHex() {
        String s = hexField.getText().trim().replace("#", "");
        try {
            if (s.length() == 6) {
                pick(Integer.parseInt(s, 16));
                status = "";
            } else {
                status = L.t("Enter a color as #RRGGBB.");
            }
        } catch (NumberFormatException e) {
            status = L.t("Invalid color. Example: #FF0000");
        }
    }

    /** Ustawia kolor (z tabeli, listy lub pola hex). */
    private void pick(int c) {
        rgb = c & 0xFFFFFF;
        syncHsv();
        push();
    }

    private void push() {
        if (hexField != null) hexField.setText(String.format("#%06X", rgb));
        if (onChange != null) onChange.accept(rgb);
    }

    private static boolean in(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private static float clamp01(float v) {
        return v < 0f ? 0f : Math.min(v, 1f);
    }

    private void handleMouse(int mx, int my) {
        long win = client.getWindow().getHandle();
        boolean down = GLFW.glfwGetMouseButton(win, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        int px = px(), py = py();
        int tx = px + 8, ty = py + 22;
        int svx = px + 146, svy = py + 22, hux = px + 252;
        int sgx = px + 8, sgy = py + 168;
        if (down && !wasDown) {
            if (in(mx, my, tx, ty, 128, 128)) {
                int col = (mx - tx) / 16, row = (my - ty) / 16;
                if (col < 8 && row < 8) pick(ColorTable.ALL[row * 8 + col]);
            } else if (in(mx, my, svx, svy, 100, 80)) {
                dragMode = 1;
            } else if (in(mx, my, hux, svy, 12, 80)) {
                dragMode = 2;
            } else if (in(mx, my, sgx, sgy, 256, 32)) {
                int col = (mx - sgx) / 16, row = (my - sgy) / 16;
                int idx = page * PER_PAGE + row * 16 + col;
                List<Integer> list = ColorStore.list();
                if (idx >= 0 && idx < list.size()) pick(list.get(idx));
            }
        }
        if (down && dragMode != 0) {
            if (dragMode == 1) {
                sat = clamp01((mx - svx) / 99f);
                val = 1f - clamp01((my - svy) / 79f);
            } else {
                hue = clamp01((my - svy) / 79f) * 0.9999f;
            }
            rgb = ColorUtil.hsv(hue, sat, val);
            push();
        }
        if (!down) dragMode = 0;
        wasDown = down;
    }

    private void outline(DrawContext ctx, int x1, int y1, int x2, int y2, int c) {
        ctx.fill(x1, y1, x2, y1 + 1, c);
        ctx.fill(x1, y2 - 1, x2, y2, c);
        ctx.fill(x1, y1, x1 + 1, y2, c);
        ctx.fill(x2 - 1, y1, x2, y2, c);
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        int px = px(), py = py();
        ctx.fill(0, 0, width, height, HexTheme.OVERLAY);
        ctx.fill(px, py, px + W, py + H, HexTheme.PANEL);
        ctx.fill(px, py, px + W, py + 3, HexTheme.RED);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        handleMouse(mouseX, mouseY);
        int px = px(), py = py();

        ctx.drawTextWithShadow(textRenderer, L.t("Pick a color"), px + 8, py + 8, HexTheme.RED);

        // tabela kolorow 8x8
        int tx = px + 8, ty = py + 22;
        for (int i = 0; i < ColorTable.ALL.length; i++) {
            int sx = tx + (i % 8) * 16, sy = ty + (i / 8) * 16;
            ctx.fill(sx, sy, sx + 14, sy + 14, 0xFF000000 | ColorTable.ALL[i]);
            if (ColorTable.ALL[i] == rgb) outline(ctx, sx - 1, sy - 1, sx + 15, sy + 15, HexTheme.RED);
        }

        // kwadrat nasycenie/jasnosc + pasek odcienia
        int svx = px + 146, svy = py + 22, hux = px + 252;
        for (int yy = 0; yy < 40; yy++) {
            for (int xx = 0; xx < 50; xx++) {
                ctx.fill(svx + xx * 2, svy + yy * 2, svx + xx * 2 + 2, svy + yy * 2 + 2,
                        0xFF000000 | ColorUtil.hsv(hue, xx / 49f, 1f - yy / 39f));
            }
        }
        int mxp = svx + Math.round(sat * 99f), myp = svy + Math.round((1f - val) * 79f);
        ctx.fill(mxp - 3, myp - 3, mxp + 4, myp + 4, 0xFF000000);
        ctx.fill(mxp - 2, myp - 2, mxp + 3, myp + 3, 0xFFFFFFFF);
        ctx.fill(mxp - 1, myp - 1, mxp + 2, myp + 2, 0xFF000000 | rgb);
        for (int i = 0; i < 40; i++) {
            ctx.fill(hux, svy + i * 2, hux + 12, svy + i * 2 + 2, 0xFF000000 | ColorUtil.hsv(i / 40f, 1f, 1f));
        }
        int hy = svy + Math.round(hue * 79f);
        ctx.fill(hux - 2, hy - 1, hux + 14, hy + 2, 0xFF000000);
        ctx.fill(hux - 1, hy, hux + 13, hy + 1, 0xFFFFFFFF);

        // podglad aktualnego koloru
        ctx.fill(px + 145, py + 131, px + 265, py + 149, 0xFF000000);
        ctx.fill(px + 146, py + 132, px + 264, py + 148, 0xFF000000 | rgb);
        if (!status.isEmpty()) {
            ctx.drawTextWithShadow(textRenderer, textRenderer.trimToWidth(status, 120), px + 146, py + 152, HexTheme.TEXT_DIM);
        }

        // wlasne kolory (strony po 32)
        ctx.drawTextWithShadow(textRenderer, L.t("Saved colors") + " (" + ColorStore.list().size() + ")", px + 8, py + 156, HexTheme.TEXT_DIM);
        int sgx = px + 8, sgy = py + 168;
        List<Integer> list = ColorStore.list();
        for (int i = 0; i < PER_PAGE; i++) {
            int sx = sgx + (i % 16) * 16, sy = sgy + (i / 16) * 16;
            int idx = page * PER_PAGE + i;
            ctx.fill(sx, sy, sx + 14, sy + 14, idx < list.size() ? 0xFF000000 | list.get(idx) : 0xFF2A2A30);
            if (idx < list.size() && list.get(idx) == rgb) outline(ctx, sx - 1, sy - 1, sx + 15, sy + 15, HexTheme.RED);
        }
        ctx.drawTextWithShadow(textRenderer, (page + 1) + "/" + (maxPage() + 1), px + 194, py + H - 21, HexTheme.TEXT);
    }

    @Override
    public void close() {
        if (onDone != null) onDone.run();
        client.setScreen(parent);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
