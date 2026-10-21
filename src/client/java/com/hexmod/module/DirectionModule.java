package com.hexmod.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/** Kierunek patrzenia (strona swiata i kat). */
public class DirectionModule extends Module {
    private static final String[] DIRS = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};

    public DirectionModule() {
        super("direction", "Kierunek", "Strona swiata, w ktora patrzysz");
        at(10, 252);
    }

    @Override public int w() { return 80; }

    @Override
    protected void draw(DrawContext ctx, boolean preview) {
        MinecraftClient mc = MinecraftClient.getInstance();
        float yaw = preview || mc.player == null ? 45f : mc.player.getYaw();
        float y = ((yaw % 360f) + 360f) % 360f;
        String d = DIRS[Math.round(y / 45f) % 8];
        ctx.drawTextWithShadow(mc.textRenderer, d + " (" + Math.round(y) + ")", 0, 0, rgb());
    }
}
