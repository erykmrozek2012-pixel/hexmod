package com.hexmod.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.effect.StatusEffectInstance;

import java.util.Locale;

/** Aktywne efekty (potiony) z czasem. */
public class EffectsModule extends Module {
    public EffectsModule() {
        super("effects", "Efekty", "Aktywne efekty z pozostalym czasem");
        at(120, 74);
    }

    @Override public int w() { return 130; }
    @Override public int h() { return 48; }

    @Override
    protected void draw(DrawContext ctx, boolean preview) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        if (preview) {
            ctx.drawTextWithShadow(mc.textRenderer, "Speed 2 1:30", 0, 0, rgb());
            ctx.drawTextWithShadow(mc.textRenderer, "Strength 1 0:45", 0, 12, rgb());
            return;
        }
        int line = 0;
        for (StatusEffectInstance e : mc.player.getStatusEffects()) {
            String name = e.getEffectType().value().getName().getString();
            int sec = e.getDuration() / 20;
            String time = e.getDuration() < 0 ? "inf" : String.format(Locale.ROOT, "%d:%02d", sec / 60, sec % 60);
            ctx.drawTextWithShadow(mc.textRenderer, name + " " + (e.getAmplifier() + 1) + " " + time, 0, line, rgb());
            line += 12;
            if (line >= 48) break;
        }
    }
}
