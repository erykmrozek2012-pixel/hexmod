package com.hexmod.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;

import java.util.Locale;

public class EatTimerModule extends Module {
    public EatTimerModule() {
        super("eat_timer", "Czas jedzenia", "Odliczanie jedzenia z ulamkami sekundy");
        at(10, 58);
    }

    @Override public int w() { return 90; }

    @Override
    protected void draw(DrawContext ctx, boolean preview) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (preview) {
            ctx.drawTextWithShadow(mc.textRenderer, "Jedzenie: 1.32s", 0, 0, rgb());
            return;
        }
        PlayerEntity p = mc.player;
        if (p == null || !p.isUsingItem()) return;
        if (!p.getActiveItem().contains(DataComponentTypes.FOOD)) return;
        double left = p.getItemUseTimeLeft() / 20.0;
        ctx.drawTextWithShadow(mc.textRenderer, String.format(Locale.ROOT, "Jedzenie: %.2fs", left), 0, 0, rgb());
    }
}
