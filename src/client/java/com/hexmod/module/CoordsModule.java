package com.hexmod.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public class CoordsModule extends Module {
    public CoordsModule() {
        super("coords", "Koordynaty", "Pozycja X Y Z");
        at(10, 34);
    }

    @Override public int w() { return 100; }

    @Override
    protected void draw(DrawContext ctx, boolean preview) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        String t = "XYZ: " + mc.player.getBlockX() + " " + mc.player.getBlockY() + " " + mc.player.getBlockZ();
        ctx.drawTextWithShadow(mc.textRenderer, t, 0, 0, rgb());
    }
}
