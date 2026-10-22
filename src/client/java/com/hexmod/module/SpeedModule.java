package com.hexmod.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.Locale;

/** Predkosciomierz: predkosc pozioma w blokach na sekunde. */
public class SpeedModule extends Module {
    private double prevX, prevZ;
    private boolean has = false;
    private double speed = 0;

    public SpeedModule() {
        super("speed", "Speed", "Horizontal speed in blocks per second");
        at(10, 334);
    }

    @Override public int w() { return 90; }

    @Override
    public void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) {
            has = false;
            return;
        }
        double x = mc.player.getX(), z = mc.player.getZ();
        if (has) {
            double now = Math.sqrt((x - prevX) * (x - prevX) + (z - prevZ) * (z - prevZ)) * 20.0;
            speed = speed * 0.6 + now * 0.4;
        }
        prevX = x;
        prevZ = z;
        has = true;
    }

    @Override
    protected void draw(DrawContext ctx, boolean preview) {
        MinecraftClient mc = MinecraftClient.getInstance();
        double v = preview ? 5.6 : speed;
        ctx.drawTextWithShadow(mc.textRenderer, String.format(Locale.ROOT, "%.1f", v) + " " + L.t("blocks/s"), 0, 0, rgb());
    }
}
