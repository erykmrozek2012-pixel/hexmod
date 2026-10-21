package com.hexmod.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.Locale;

/** Poziom saturacji i szacowany czas do konca. Widoczny tylko w walce. */
public class SaturationModule extends Module {
    private int ticks = 0;
    private float last = -1f;
    private float rate = 0f;

    public SaturationModule() {
        super("saturation", "Saturation", "Poziom i szacowany czas saturacji w walce");
        at(10, 46);
        option("bar", "Pasek", true);
        option("pct", "Procenty", true);
    }

    @Override public int w() { return 140; }
    @Override public int h() { return option("bar") ? 18 : 12; }

    @Override
    public void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        if (++ticks % 20 != 0) return;
        float sat = mc.player.getHungerManager().getSaturationLevel();
        if (last >= 0f) {
            float drop = last - sat;
            if (drop >= 0f) rate = rate == 0f ? drop : rate * 0.7f + drop * 0.3f;
        }
        last = sat;
    }

    @Override
    protected void draw(DrawContext ctx, boolean preview) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        if (!preview && !CombatTracker.inCombat()) return;
        float sat = preview ? 5f : mc.player.getHungerManager().getSaturationLevel();
        float frac = Math.min(1f, sat / 20f);
        boolean empty = !preview && sat <= 0.01f;
        String text = empty ? "Saturacja: BRAK" : String.format(Locale.ROOT, "Saturacja: %.1f", sat);
        if (!empty && option("pct")) text += " (" + Math.round(frac * 100f) + "%)";
        if (!empty) {
            float secs = preview ? 12f : (rate > 0.01f ? sat / rate : -1f);
            if (secs >= 0f) text += String.format(Locale.ROOT, " ~%.0fs", secs);
        }
        ctx.drawTextWithShadow(mc.textRenderer, text, 0, 0, empty ? 0xFFD10A0A : rgb());
        if (option("bar")) drawBar(ctx, 0, 11, w(), 4, frac, empty ? 0xFFD10A0A : (0xFF000000 | rgb()));
    }
}
