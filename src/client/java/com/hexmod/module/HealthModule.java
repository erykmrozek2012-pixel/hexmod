package com.hexmod.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;

import java.util.Locale;

/** Twoje zdrowie i dodatkowe serca (absorpcja) jako liczba. */
public class HealthModule extends Module {
    public HealthModule() {
        super("health", "Health", "Your health and absorption hearts");
        at(130, 34);
    }

    @Override public int w() { return 90; }

    @Override
    protected void draw(DrawContext ctx, boolean preview) {
        MinecraftClient mc = MinecraftClient.getInstance();
        PlayerEntity p = mc.player;
        float hp = preview ? 14.5f : (p == null ? 20f : p.getHealth());
        float abs = preview ? 4f : (p == null ? 0f : p.getAbsorptionAmount());
        String t = "HP: " + String.format(Locale.ROOT, "%.1f", hp);
        if (abs > 0.05f) t += " +" + String.format(Locale.ROOT, "%.1f", abs);
        int col = hp <= 6f ? 0xFFD10A0A : (hp <= 12f ? 0xFFFFD60A : rgb());
        ctx.drawTextWithShadow(mc.textRenderer, t, 0, 0, col);
    }
}
