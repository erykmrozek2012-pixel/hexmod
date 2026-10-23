package com.hexmod.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/** Keystrokes: pokazuje, ktore klawisze ruchu i przyciski myszy sa wcisniete. */
public class KeystrokesModule extends Module {
    public KeystrokesModule() {
        super("keystrokes", "Keystrokes", "Pressed movement keys and mouse buttons");
        at(10, 350);
        option("mouse", "Mouse buttons", true);
        option("space", "Space bar", true);
    }

    @Override public int w() { return 58; }

    @Override
    public int h() {
        return 38 + (option("mouse") ? 20 : 0) + (option("space") ? 20 : 0);
    }

    private void key(DrawContext ctx, MinecraftClient mc, int x, int y, int w, String label, boolean down) {
        int accent = 0xCC000000 | (rgb() & 0xFFFFFF);
        ctx.fill(x, y, x + w, y + 18, down ? accent : 0x88000000);
        int tw = mc.textRenderer.getWidth(label);
        ctx.drawTextWithShadow(mc.textRenderer, label, x + (w - tw) / 2, y + 5, down ? 0xFF101014 : 0xFFFFFFFF);
    }

    @Override
    protected void draw(DrawContext ctx, boolean preview) {
        MinecraftClient mc = MinecraftClient.getInstance();
        boolean fw = preview || mc.options.forwardKey.isPressed();
        boolean lf = !preview && mc.options.leftKey.isPressed();
        boolean bk = !preview && mc.options.backKey.isPressed();
        boolean rt = !preview && mc.options.rightKey.isPressed();
        key(ctx, mc, 20, 0, 18, "W", fw);
        key(ctx, mc, 0, 20, 18, "A", lf);
        key(ctx, mc, 20, 20, 18, "S", bk);
        key(ctx, mc, 40, 20, 18, "D", rt);
        int y = 40;
        if (option("mouse")) {
            key(ctx, mc, 0, y, 28, "LMB", !preview && mc.options.attackKey.isPressed());
            key(ctx, mc, 30, y, 28, "RMB", !preview && mc.options.useKey.isPressed());
            y += 20;
        }
        if (option("space")) {
            key(ctx, mc, 0, y, 58, L.t("SPACE"), !preview && mc.options.jumpKey.isPressed());
        }
    }
}
