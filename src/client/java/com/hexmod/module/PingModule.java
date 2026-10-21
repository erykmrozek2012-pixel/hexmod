package com.hexmod.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;

public class PingModule extends Module {
    public PingModule() {
        super("ping", "Ping", "Latency to the server");
        at(10, 22);
    }

    @Override public int w() { return 60; }

    @Override
    protected void draw(DrawContext ctx, boolean preview) {
        MinecraftClient mc = MinecraftClient.getInstance();
        int ping = 0;
        if (mc.player != null && mc.getNetworkHandler() != null) {
            PlayerListEntry e = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
            if (e != null) ping = e.getLatency();
        }
        ctx.drawTextWithShadow(mc.textRenderer, "Ping: " + ping + "ms", 0, 0, rgb());
    }
}
