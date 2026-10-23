package com.hexmod.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.Locale;

/** Combat Timer: ile sekund zostalo do konca walki (po trafieniu albo otrzymaniu obrazen). */
public class CombatTimerModule extends Module {
    public CombatTimerModule() {
        super("combat_timer", "Combat Timer", "Seconds left in combat after hitting or being hit");
        at(10, 364);
        option("bar", "Bar", true);
        option("always", "Always show", false);
    }

    @Override public int w() { return 100; }

    @Override
    public int h() {
        return option("bar") ? 18 : 12;
    }

    @Override
    protected void draw(DrawContext ctx, boolean preview) {
        MinecraftClient mc = MinecraftClient.getInstance();
        boolean in = preview || CombatTracker.inCombat();
        if (!in) {
            if (option("always")) ctx.drawTextWithShadow(mc.textRenderer, L.t("Out of combat"), 0, 0, 0xFF9A9A9A);
            return;
        }
        long rem = preview ? 6400L : CombatTracker.remainingMs();
        ctx.drawTextWithShadow(mc.textRenderer, L.t("Combat: ") + String.format(Locale.ROOT, "%.1fs", rem / 1000f), 0, 0, 0xFFD10A0A);
        if (option("bar")) drawBar(ctx, 0, 11, w(), 4, rem / 10000f, 0xFFD10A0A);
    }
}
