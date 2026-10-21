package com.hexmod.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;

import java.util.Locale;

/** Najblizszy wrog (gracz niebedacy sojusznikiem): nick, odleglosc i HP. */
public class NearestEnemyModule extends Module {
    public NearestEnemyModule() {
        super("nearest_enemy", "Target Info", "Nick, odleglosc i HP najblizszego gracza");
        at(10, 372);
        option("hp", "HP", true);
    }

    @Override public int w() { return 130; }

    @Override
    protected void draw(DrawContext ctx, boolean preview) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (preview) {
            ctx.drawTextWithShadow(mc.textRenderer, "Wrog: 4.2m" + (option("hp") ? "  HP 18.0" : ""), 0, 0, rgb());
            return;
        }
        if (mc.player == null || mc.world == null) return;
        PlayerEntity best = null;
        float bd = 64f;
        for (PlayerEntity pl : mc.world.getPlayers()) {
            if (pl == mc.player || pl.isSpectator()) continue;
            if (AlliesManager.isAlly(pl.getName().getString())) continue;
            float d = mc.player.distanceTo(pl);
            if (d < bd) {
                bd = d;
                best = pl;
            }
        }
        if (best == null) {
            ctx.drawTextWithShadow(mc.textRenderer, "Wrog: brak", 0, 0, rgb());
            return;
        }
        String t = best.getName().getString() + ": " + String.format(Locale.ROOT, "%.1fm", bd);
        if (option("hp")) t += String.format(Locale.ROOT, "  HP %.1f", best.getHealth() + best.getAbsorptionAmount());
        ctx.drawTextWithShadow(mc.textRenderer, t, 0, 0, rgb());
    }
}
