package com.hexmod.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public class FpsModule extends Module {
    public FpsModule() {
        super("fps", "FPS", "Licznik klatek na sekunde");
        at(10, 10);
    }

    @Override public int w() { return 50; }

    @Override
    protected void draw(DrawContext ctx, boolean preview) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ctx.drawTextWithShadow(mc.textRenderer, "FPS: " + mc.getCurrentFps(), 0, 0, rgb());
    }
}
