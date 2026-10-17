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
        super("saturation", "Saturacja", "Poziom i szacowany czas saturacji w walce");
        at(10, 46);
    }

    @Override public int w() { return 130; }

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
        if (preview) {
            ctx.drawTextWithShadow(mc.textRenderer, "Saturacja: 5.0 (~12s)", 0, 0, rgb());
            return;
        }
        if (!CombatTracker.inCombat()) return;
        float sat = mc.player.getHungerManager().getSaturationLevel();
        if (sat <= 0.01f) {
            ctx.drawTextWithShadow(mc.textRenderer, "Saturacja: BRAK", 0, 0, 0xFFD10A0A);
            return;
        }
        String text = String.format(Locale.ROOT, "Saturacja: %.1f", sat);
        if (rate > 0.01f) text += String.format(Locale.ROOT, " (~%.0fs)", sat / rate);
        ctx.drawTextWithShadow(mc.textRenderer, text, 0, 0, rgb());
    }
}
