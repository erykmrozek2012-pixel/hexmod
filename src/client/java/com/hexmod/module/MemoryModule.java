package com.hexmod.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/** Zuzycie pamieci RAM przez gre. */
public class MemoryModule extends Module {
    public MemoryModule() {
        super("memory", "Pamiec RAM", "Zuzycie pamieci przez gre");
        at(10, 280);
    }

    @Override public int w() { return 110; }

    @Override
    protected void draw(DrawContext ctx, boolean preview) {
        MinecraftClient mc = MinecraftClient.getInstance();
        Runtime rt = Runtime.getRuntime();
        long used = (rt.totalMemory() - rt.freeMemory()) / 1048576L;
        long max = rt.maxMemory() / 1048576L;
        ctx.drawTextWithShadow(mc.textRenderer, "RAM: " + used + "/" + max + " MB", 0, 0, rgb());
    }
}
