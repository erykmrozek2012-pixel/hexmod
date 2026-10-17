package com.hexmod.gui;

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
    private static final int PANEL_H = 64;

    private final Screen parent;
    private Module selected;
    private boolean dragging;
    private boolean wasDown;
    private int dragDx, dragDy;
    private boolean needsInit;

    public HudEditorScreen(Screen parent) {
        super(Text.literal("Edytor HUD"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int cx = width / 2;
        int y0 = height - PANEL_H;

        if (selected == null) {
            addDrawableChild(ButtonWidget.builder(Text.literal("Gotowe"), b -> client.setScreen(parent))
                    .dimensions(cx - 30, y0 + 20, 60, 20).build());
            return;
        }

        int x = cx - 125;
        addDrawableChild(ButtonWidget.builder(Text.literal("-"), b -> changeScale(-0.1f))
                .dimensions(x, y0 + 8, 24, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("+"), b -> changeScale(0.1f))
                .dimensions(x + 28, y0 + 8, 24, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Kolor"), b -> cycleColor())
                .dimensions(x + 56, y0 + 8, 56, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal(selected.hidden ? "Pokaz" : "Ukryj"), b -> {
            selected.hidden = !selected.hidden;
            ModuleManager.save();
            needsInit = true;
        }).dimensions(x + 116, y0 + 8, 70, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Gotowe"), b -> client.setScreen(parent))
                .dimensions(x + 190, y0 + 8, 60, 20).build());

        int ox = cx - (selected.options.size() * 74) / 2;
        for (String key : selected.options.keySet()) {
            String label = selected.optionLabels.getOrDefault(key, key)
                    + ": " + (selected.option(key) ? "ON" : "OFF");
            final String k = key;
            addDrawableChild(ButtonWidget.builder(Text.literal(label), b -> {
                selected.options.put(k, !selected.option(k));
                ModuleManager.save();
                needsInit = true;
            }).dimensions(ox, y0 + 34, 70, 20).build());
            ox += 74;
        }
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
        ctx.fill(0, height - PANEL_H, width, height, HexTheme.PANEL);
        ctx.fill(0, height - PANEL_H, width, height - PANEL_H + 2, HexTheme.RED);
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

        if (down && !wasDown && !overPanelWidget(mx, my)) {
            List<Module> all = ModuleManager.all();
            Module hit = null;
            for (int i = all.size() - 1; i >= 0; i--) {
                Module m = all.get(i);
                if (!m.isEnabled()) continue;
                boolean inside = m.hasOverlayPreview() ? previewHit(m, mx, my) : (!m.isOverlay() && m.contains(mx, my));
                if (inside) { hit = m; break; }
            }
            if (hit != null) {
                if (hit != selected) { selected = hit; needsInit = true; }
                dragging = true;
                dragDx = mx - hit.x;
                dragDy = my - hit.y;
            } else if (selected != null && my < height - PANEL_H) {
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

        boolean any = false;
        for (Module m : ModuleManager.all()) {
            if (!m.isEnabled()) continue;
            any = true;
            if (m.hasOverlayPreview()) {
                int ax = width / 2, ay = height / 3;
                if (!m.hidden) m.drawOverlayPreview(ctx, ax, ay);
                int[] b = m.overlayPreviewBox(ax, ay);
                if (b != null) {
                    outline(ctx, b[0] - 2, b[1] - 2, b[2] + 2, b[3] + 2, m == selected ? HexTheme.RED : HexTheme.TEXT_DIM);
                }
                ctx.drawCenteredTextWithShadow(textRenderer,
                        m.hidden ? "Wskazniki wroga (ukryte)" : "\u25BC glowa wroga", ax, ay + 3, HexTheme.TEXT_DIM);
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
                ctx.drawTextWithShadow(textRenderer, m.name + " (ukryty)", m.x, m.y, HexTheme.TEXT_DIM);
            }
        }

        String hint = any
                ? "Przeciagnij widget myszka. Kliknij widget, aby ustawic skale, kolor i opcje."
                : "Brak wlaczonych modulow. Wlacz je w menu Hexmoda (Prawy Shift).";
        ctx.drawCenteredTextWithShadow(textRenderer, hint, width / 2, 8, HexTheme.TEXT);

        if (selected != null) {
            String info = selected.name + "  |  skala " + String.format(java.util.Locale.ROOT, "%.1f", selected.scale);
            ctx.drawCenteredTextWithShadow(textRenderer, info, width / 2, height - PANEL_H - 12, HexTheme.RED);
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
