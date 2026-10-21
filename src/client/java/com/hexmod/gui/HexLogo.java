package com.hexmod.gui;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

/** Logo Hex Client: szesciokat z napisem. */
final class HexLogo {
    private HexLogo() {}

    static void hex(DrawContext ctx, int cx, int cy, int r, int color) {
        for (int dy = -r; dy <= r; dy++) {
            double a = Math.abs(dy);
            if (a > r * 0.866) continue;
            int hw = (int) Math.round(r - a * 0.5774);
            ctx.fill(cx - hw, cy + dy, cx + hw + 1, cy + dy + 1, color);
        }
    }

    static void draw(DrawContext ctx, TextRenderer tr, int x, int y) {
        hex(ctx, x + 7, y + 7, 7, HexTheme.RED);
        hex(ctx, x + 7, y + 7, 4, HexTheme.PANEL);
        ctx.drawTextWithShadow(tr, "HEX CLIENT", x + 18, y + 3, HexTheme.RED);
    }
}
