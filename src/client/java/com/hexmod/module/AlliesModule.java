package com.hexmod.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;

/** Widget: sojusznicy w poblizu (z ikona gwiazdki i odlegloscia). */
public class AlliesModule extends Module {
    public static final String ICON = "\u2605";

    public AlliesModule() {
        super("allies", "Allies", "Sojusznicy w poblizu z ikona przy nicku");
        at(230, 10);
    }

    @Override public int w() { return 110; }
    @Override public int h() { return 60; }

    @Override
    protected void draw(DrawContext ctx, boolean preview) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;
        if (preview) {
            ctx.drawTextWithShadow(mc.textRenderer, ICON + " Przyjaciel 12m", 0, 0, rgb());
            ctx.drawTextWithShadow(mc.textRenderer, ICON + " Kumpel 30m", 0, 12, rgb());
            return;
        }
        int line = 0;
        for (PlayerEntity p : mc.world.getPlayers()) {
            if (p == mc.player || !AlliesManager.isAlly(p.getName().getString())) continue;
            String t = ICON + " " + p.getName().getString() + " " + Math.round(mc.player.distanceTo(p)) + "m";
            ctx.drawTextWithShadow(mc.textRenderer, t, 0, line, rgb());
            line += 12;
            if (line >= 60) break;
        }
    }
}
