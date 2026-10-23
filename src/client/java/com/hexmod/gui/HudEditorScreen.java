package com.hexmod.gui;

import com.hexmod.module.L;
import com.hexmod.module.Module;
import com.hexmod.module.ModuleManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/** Edytor HUD: przeciagnij widget myszka, a na dole ustaw skale, kolor, ukrywanie i opcje. */
public class HudEditorScreen extends Screen {
    private int panelH = 82;

    private final Screen parent;
    private Module selected;
    private boolean dragging;
    private boolean wasDown;
    private int dragDx, dragDy;
    private boolean needsInit;
    private Module listening;
    private boolean paletteOpen;
    private final boolean[] heldAtStart = new boolean[349];

    public HudEditorScreen(Screen parent) {
        super(Text.literal(L.t("HUD Editor")));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int cx = width / 2;
        java.util.List<String> keys = new java.util.ArrayList<>();
        if (selected != null) {
            keys.add("__style");
            keys.add("__reset");
            keys.addAll(selected.options.keySet());
        }
        int n = keys.size();
        String[] labels = new String[n];
        int[] bw = new int[n];
        int[] rowOf = new int[n];
        int rows = 1, cur = 0, maxW = width - 16;
        for (int i = 0; i < n; i++) {
            String k = keys.get(i);
            labels[i] = k.equals("__reset") ? L.t("Reset") : k.equals("__style")
                    ? L.t("Style: ") + L.t(Module.STYLE_NAMES[selected.style])
                    : L.t(selected.optionLabels.getOrDefault(k, k)) + ": " + (selected.option(k) ? L.t("ON") : L.t("OFF"));
            bw[i] = Math.max(60, textRenderer.getWidth(labels[i]) + 12);
            if (cur > 0 && cur + bw[i] + 4 > maxW) {
                rows++;
                cur = 0;
            }
            rowOf[i] = rows - 1;
            cur += bw[i] + 4;
        }
        panelH = 82 + (rows - 1) * 22;
        int y0 = height - panelH;

        if (selected == null) {
            addDrawableChild(ButtonWidget.builder(Text.literal(L.t("Done")), b -> client.setScreen(parent))
                    .dimensions(cx - 30, y0 + 20, 60, 20).build());
            return;
        }

        int x = cx - 125;
        addDrawableChild(ButtonWidget.builder(Text.literal("-"), b -> changeScale(-0.1f))
                .dimensions(x, y0 + 6, 24, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("+"), b -> changeScale(0.1f))
                .dimensions(x + 28, y0 + 6, 24, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal(L.t("Color")), b -> paletteOpen = !paletteOpen)
                .dimensions(x + 56, y0 + 6, 56, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal(selected.hidden ? L.t("Show") : L.t("Hide")), b -> {
            selected.hidden = !selected.hidden;
            ModuleManager.save();
            needsInit = true;
        }).dimensions(x + 116, y0 + 6, 70, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal(L.t("Done")), b -> client.setScreen(parent))
                .dimensions(x + 190, y0 + 6, 60, 20).build());

        String keyLabel = listening == selected
                ? L.t("Press a key...") : L.t("Key: ") + keyName(selected.bindKey);
        addDrawableChild(ButtonWidget.builder(Text.literal(keyLabel), b -> {
            listening = listening == selected ? null : selected;
            resetHeld();
            needsInit = true;
        }).dimensions(cx - 125, y0 + 30, 130, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal(L.t("Mode: ") + (selected.bindHold ? L.t("hold") : L.t("toggle"))), b -> {
            selected.bindHold = !selected.bindHold;
            ModuleManager.save();
            needsInit = true;
        }).dimensions(cx + 9, y0 + 30, 96, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal(L.t("Remove")), b -> {
            selected.bindKey = -1;
            listening = null;
            ModuleManager.save();
            needsInit = true;
        }).dimensions(cx + 109, y0 + 30, 36, 20).build());

        int[] rowX = new int[rows];
        int[] rowW = new int[rows];
        for (int i = 0; i < n; i++) rowW[rowOf[i]] += bw[i] + 4;
        for (int r = 0; r < rows; r++) rowX[r] = cx - (rowW[r] - 4) / 2;
        for (int i = 0; i < n; i++) {
            final String k = keys.get(i);
            int ox = rowX[rowOf[i]];
            rowX[rowOf[i]] += bw[i] + 4;
            addDrawableChild(ButtonWidget.builder(Text.literal(labels[i]), b -> {
                if (k.equals("__reset")) {
                    selected.resetLook();
                } else if (k.equals("__style")) {
                    selected.style = (selected.style + 1) % Module.STYLE_COUNT;
                } else {
                    selected.options.put(k, !selected.option(k));
                }
                ModuleManager.save();
                needsInit = true;
            }).dimensions(ox, y0 + 54 + rowOf[i] * 22, bw[i], 20).build());
        }
    }

    private static final int[] PALETTE_HEX = {
            0xD10A0A, 0xFF3B30, 0xFF6A00, 0xFF9500, 0xFFD60A, 0xFFE680, 0xBFFF00, 0x30D158,
            0x00A86B, 0x00C7BE, 0x64D2FF, 0x32ADE6, 0x0A84FF, 0x5E5CE6, 0xBF5AF2, 0xFF6EB4,
            0xFFFFFF, 0xE5E5EA, 0xC7C7CC, 0x8E8E93, 0x636366, 0x3A3A3C, 0x1C1C1E, 0x000000,
            0x8B5A2B, 0xC8A165, 0xA2845E, 0x7A0505, 0x1E6B3A, 0x2C3E6B, 0x6B2C5E, 0x40E0D0};

    private int paletteX() { return width / 2 - 105; }

    private int paletteY() { return height - panelH - 70; }

    private void resetHeld() {
        long win = client.getWindow().getHandle();
        for (int k = 32; k < heldAtStart.length; k++) {
            heldAtStart[k] = GLFW.glfwGetKey(win, k) == GLFW.GLFW_PRESS;
        }
    }

    /** Czeka na nacisniecie klawisza przypisywanego do widgetu (Backspace = usun, Prawy Shift pominiety). */
    private void pollListening() {
        if (listening == null) return;
        if (listening != selected) { listening = null; return; }
        long win = client.getWindow().getHandle();
        for (int k = 32; k < heldAtStart.length; k++) {
            boolean down = GLFW.glfwGetKey(win, k) == GLFW.GLFW_PRESS;
            if (!down) { heldAtStart[k] = false; continue; }
            if (heldAtStart[k] || k == GLFW.GLFW_KEY_ESCAPE || k == GLFW.GLFW_KEY_RIGHT_SHIFT) continue;
            selected.bindKey = k == GLFW.GLFW_KEY_BACKSPACE ? -1 : k;
            listening = null;
            ModuleManager.save();
            needsInit = true;
            return;
        }
    }

    static String keyName(int k) {
        if (k <= 0) return L.t("none");
        switch (k) {
            case 32: return L.t("Space");
            case 257: return "Enter";
            case 258: return "Tab";
            case 260: return "Insert";
            case 261: return "Delete";
            case 262: return L.t("Right arrow");
            case 263: return L.t("Left arrow");
            case 264: return L.t("Down arrow");
            case 265: return L.t("Up arrow");
            case 266: return "Page Up";
            case 267: return "Page Down";
            case 268: return "Home";
            case 269: return "End";
            case 280: return "Caps Lock";
            case 340: return L.t("Left Shift");
            case 341: return L.t("Left Ctrl");
            case 342: return L.t("Left Alt");
            case 345: return L.t("Right Ctrl");
            case 346: return L.t("Right Alt");
            default: break;
        }
        if (k >= 290 && k <= 301) return "F" + (k - 289);
        if (k >= 320 && k <= 329) return "Num " + (k - 320);
        String n = GLFW.glfwGetKeyName(k, 0);
        return n == null ? L.t("Key ") + k : n.toUpperCase();
    }

    private void changeScale(float d) {
        selected.scale = Math.max(0.5f, Math.min(3f, Math.round((selected.scale + d) * 10f) / 10f));
        ModuleManager.save();
    }

    private void cycleColor() {
        int idx = 0;
        for (int i = 0; i < Module.PALETTE.length; i++) {
            if (Module.PALETTE[i] == selected.color) idx = i;
        }
        selected.color = Module.PALETTE[(idx + 1) % Module.PALETTE.length];
        ModuleManager.save();
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
        ctx.fill(0, 0, width, height, 0x70000000);
        ctx.fill(0, height - panelH, width, height, HexTheme.PANEL);
        ctx.fill(0, height - panelH, width, height - panelH + 2, HexTheme.RED);
    }

    private boolean previewHit(Module m, int mx, int my) {
        int[] b = m.overlayPreviewBox(width / 2, height / 3);
        return b != null && mx >= b[0] - 2 && mx <= b[2] + 2 && my >= b[1] - 2 && my <= b[3] + 2;
    }

    /** Czy kursor jest nad przyciskiem panelu (wtedy klikniecie nie chwyta widgetu). */
    private boolean overPanelWidget(int mx, int my) {
        for (var c : children()) {
            if (c.isMouseOver(mx, my)) return true;
        }
        return false;
    }

    private void handleMouse(int mx, int my) {
        long win = client.getWindow().getHandle();
        boolean down = GLFW.glfwGetMouseButton(win, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;

        if (paletteOpen && down && !wasDown && selected != null) {
            int px0 = paletteX(), py0 = paletteY();
            if (mx >= px0 && mx < px0 + 8 * 16 + 4 && my >= py0 && my < py0 + 4 * 16 + 4) {
                int col = Math.floorDiv(mx - px0 - 2, 16), row = Math.floorDiv(my - py0 - 2, 16);
                if (col >= 0 && col < 8 && row >= 0 && row < 4) {
                    selected.color = PALETTE_HEX[row * 8 + col];
                    ModuleManager.save();
                }
                wasDown = true;
                return;
            }
            if (!overPanelWidget(mx, my)) paletteOpen = false;
        }

        if (down && !wasDown && !overPanelWidget(mx, my)) {
            List<Module> all = ModuleManager.all();
            Module hit = null;
            for (int i = all.size() - 1; i >= 0; i--) {
                Module m = all.get(i);
                if (!m.isEnabled() || (Module.extrasHidden && !m.isBasic())) continue;
                boolean inside = m.hasOverlayPreview() ? previewHit(m, mx, my) : (!m.isOverlay() && m.contains(mx, my));
                if (inside) { hit = m; break; }
            }
            if (hit != null) {
                if (hit != selected) { selected = hit; needsInit = true; }
                dragging = true;
                dragDx = mx - hit.x;
                dragDy = my - hit.y;
            } else if (selected != null && my < height - panelH) {
                selected = null;
                needsInit = true;
            }
        }
        if (down && dragging && selected != null) {
            if (selected.isOverlay()) {
                selected.x = Math.max(-200, Math.min(200, mx - dragDx));
                selected.y = Math.max(-200, Math.min(200, my - dragDy));
            } else {
                selected.x = Math.max(0, Math.min(width - 12, mx - dragDx));
                selected.y = Math.max(0, Math.min(height - 12, my - dragDy));
            }
        }
        if (!down && wasDown && dragging) {
            dragging = false;
            ModuleManager.save();
        }
        wasDown = down;
    }

    private void outline(DrawContext ctx, int x1, int y1, int x2, int y2, int c) {
        ctx.fill(x1, y1, x2, y1 + 1, c);
        ctx.fill(x1, y2 - 1, x2, y2, c);
        ctx.fill(x1, y1, x1 + 1, y2, c);
        ctx.fill(x2 - 1, y1, x2, y2, c);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        handleMouse(mouseX, mouseY);
        pollListening();

        boolean any = false;
        for (Module m : ModuleManager.all()) {
            if (!m.isEnabled() || (Module.extrasHidden && !m.isBasic())) continue;
            any = true;
            if (m.hasOverlayPreview()) {
                int ax = width / 2, ay = height / 3;
                if (!m.hidden) m.drawOverlayPreview(ctx, ax, ay);
                int[] b = m.overlayPreviewBox(ax, ay);
                if (b != null) {
                    outline(ctx, b[0] - 2, b[1] - 2, b[2] + 2, b[3] + 2, m == selected ? HexTheme.RED : HexTheme.TEXT_DIM);
                }
                ctx.drawCenteredTextWithShadow(textRenderer,
                        m.hidden ? L.t("Enemy Tags (hidden)") : "\u25BC " + L.t("enemy head"), ax, ay + 3, HexTheme.TEXT_DIM);
                continue;
            }
            if (m.isOverlay()) continue;
            m.place(ctx);
            boolean sel = m == selected;
            if (!m.hidden) m.paint(ctx, true);
            int x1 = m.x - 2, y1 = m.y - 2;
            int x2 = m.x + Math.round(m.w() * m.scale) + 2, y2 = m.y + Math.round(m.h() * m.scale) + 2;
            outline(ctx, x1, y1, x2, y2, sel ? HexTheme.RED : HexTheme.TEXT_DIM);
            if (m.hidden) {
                ctx.drawTextWithShadow(textRenderer, L.t(m.name) + L.t(" (hidden)"), m.x, m.y, HexTheme.TEXT_DIM);
            }
        }

        if (paletteOpen && selected != null) {
            int px0 = paletteX(), py0 = paletteY();
            ctx.fill(px0 - 2, py0 - 2, px0 + 8 * 16 + 4, py0 + 4 * 16 + 4, 0xFF000000);
            ctx.fill(px0 - 1, py0 - 1, px0 + 8 * 16 + 3, py0 + 4 * 16 + 3, HexTheme.PANEL);
            for (int i = 0; i < PALETTE_HEX.length; i++) {
                int sx = px0 + 2 + (i % 8) * 16, sy = py0 + 2 + (i / 8) * 16;
                ctx.fill(sx, sy, sx + 14, sy + 14, 0xFF000000 | PALETTE_HEX[i]);
                if (PALETTE_HEX[i] == selected.color) outline(ctx, sx - 1, sy - 1, sx + 15, sy + 15, HexTheme.RED);
            }
        }

        String hint = any
                ? L.t("Drag a widget with the mouse. Click a widget to set scale, color and options.")
                : L.t("No modules enabled. Enable them in the Hex Client menu (Right Shift).");
        ctx.drawCenteredTextWithShadow(textRenderer, hint, width / 2, 8, HexTheme.TEXT);

        if (selected != null) {
            String info = L.t(selected.name) + L.t("  |  scale ") + String.format(java.util.Locale.ROOT, "%.1f", selected.scale);
            ctx.drawCenteredTextWithShadow(textRenderer, info, width / 2, height - panelH - 12, HexTheme.RED);
        }
    }

    @Override
    public void removed() {
        ModuleManager.save();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
