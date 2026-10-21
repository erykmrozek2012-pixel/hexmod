package com.hexmod.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/** Combo: ile razy z rzedu trafiles gracza, zanim ktos Cie uderzyl albo minely 3 sekundy bez trafienia. */
public class ComboModule extends Module {
    private static int combo = 0;
    private static long lastHit = 0;

    public ComboModule() {
        super("combo", "Combo", "Liczba trafien z rzedu bez otrzymania obrazen");
        at(10, 210);
    }

    /** Wolane, gdy atakujesz gracza. */
    public static void onAttack() {
        long now = System.currentTimeMillis();
        if (now - lastHit > 3000) combo = 0;
        combo++;
        lastHit = now;
    }

    @Override public int w() { return 70; }

    @Override
    public void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        if (mc.player.hurtTime > 0) combo = 0;
        if (System.currentTimeMillis() - lastHit > 3000) combo = 0;
    }

    @Override
    protected void draw(DrawContext ctx, boolean preview) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ctx.drawTextWithShadow(mc.textRenderer, "Combo: " + (preview ? 3 : combo), 0, 0, rgb());
    }
}
