package com.hexmod.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/** Zegar z godzina komputera. */
public class ClockModule extends Module {
    private static final DateTimeFormatter SHORT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter LONG = DateTimeFormatter.ofPattern("HH:mm:ss");

    public ClockModule() {
        super("clock", "Zegar", "Aktualna godzina");
        at(10, 266);
        option("seconds", "Sekundy", false);
    }

    @Override public int w() { return 60; }

    @Override
    protected void draw(DrawContext ctx, boolean preview) {
        MinecraftClient mc = MinecraftClient.getInstance();
        String t = LocalTime.now().format(option("seconds") ? LONG : SHORT);
        ctx.drawTextWithShadow(mc.textRenderer, t, 0, 0, rgb());
    }
}
