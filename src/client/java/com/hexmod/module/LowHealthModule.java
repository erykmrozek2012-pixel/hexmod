package com.hexmod.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;

/** Czerwona pulsujaca ramka na brzegach ekranu przy niskim zdrowiu (3 serca lub mniej). */
public class LowHealthModule extends Module {
    public LowHealthModule() {
        super("low_hp", "Low HP Warning", "Red screen edges when your health is low");
        option("pulse", "Pulse", true);
    }

    @Override
    protected boolean overlay() {
        return true;
    }

    @Override
    protected void draw(DrawContext ctx, boolean preview) {
        if (preview) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        PlayerEntity p = mc.player;
        if (p == null || mc.currentScreen != null || p.isSpectator() || p.isCreative()) return;
        if (p.getHealth() + p.getAbsorptionAmount() > 6f) return;
        int a = 0x38;
        if (option("pulse")) a = 0x20 + (int) (0x28 * (0.5 + 0.5 * Math.sin(System.currentTimeMillis() / 220.0)));
        int w = ctx.getScaledWindowWidth(), h = ctx.getScaledWindowHeight();
        for (int i = 0; i < 6; i++) {
            int t = 4 + i * 4;
            int col = ((a * (6 - i) / 6) << 24) | 0xD10A0A;
            ctx.fill(0, 0, w, t, col);
            ctx.fill(0, h - t, w, h, col);
            ctx.fill(0, t, t, h - t, col);
            ctx.fill(w - t, t, w, h - t, col);
        }
    }
}
