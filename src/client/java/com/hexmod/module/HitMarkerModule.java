package com.hexmod.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/** Krzyzyk przy celowniku przez chwile po trafieniu gracza. */
public class HitMarkerModule extends Module {
    private static long lastHit = 0;

    public HitMarkerModule() {
        super("hit_marker", "Hit Marker", "Cross at the crosshair when you hit a player");
    }

    @Override
    protected boolean overlay() {
        return true;
    }

    /** Wolane, gdy atakujesz gracza. */
    public static void onAttack() {
        lastHit = System.currentTimeMillis();
    }

    @Override
    protected void draw(DrawContext ctx, boolean preview) {
        if (preview) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.currentScreen != null) return;
        long age = System.currentTimeMillis() - lastHit;
        if (age < 0 || age > 250) return;
        int a = 0xFF - (int) (age * 0xC0 / 250);
        int col = (a << 24) | (rgb() & 0xFFFFFF);
        int cx = ctx.getScaledWindowWidth() / 2, cy = ctx.getScaledWindowHeight() / 2;
        for (int k = 3; k <= 7; k++) {
            ctx.fill(cx + k, cy + k, cx + k + 2, cy + k + 2, col);
            ctx.fill(cx - k - 1, cy + k, cx - k + 1, cy + k + 2, col);
            ctx.fill(cx + k, cy - k - 1, cx + k + 2, cy - k + 1, col);
            ctx.fill(cx - k - 1, cy - k - 1, cx - k + 1, cy - k + 1, col);
        }
    }
}
