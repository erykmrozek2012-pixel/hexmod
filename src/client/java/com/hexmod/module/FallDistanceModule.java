package com.hexmod.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;

import java.util.Locale;

/** Dystans spadania w blokach; na czerwono, gdy upadek zabilby przy obecnym zdrowiu. */
public class FallDistanceModule extends Module {
    public FallDistanceModule() {
        super("fall_distance", "Fall Distance", "Blocks fallen, red when the fall would kill you");
        at(130, 82);
    }

    @Override public int w() { return 90; }

    @Override
    protected void draw(DrawContext ctx, boolean preview) {
        MinecraftClient mc = MinecraftClient.getInstance();
        PlayerEntity p = mc.player;
        double fd = preview ? 7.4 : (p == null ? 0.0 : p.fallDistance);
        double life = preview ? 14.5 : (p == null ? 20.0 : p.getHealth() + p.getAbsorptionAmount());
        int col = fd - 3.0 >= life ? 0xFFD10A0A : (fd > 3.0 ? 0xFFFFD60A : rgb());
        ctx.drawTextWithShadow(mc.textRenderer, L.t("Fall: ") + String.format(Locale.ROOT, "%.1f", Math.max(0.0, fd)), 0, 0, col);
    }
}
