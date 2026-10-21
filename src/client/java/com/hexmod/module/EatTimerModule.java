package com.hexmod.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;

import java.util.Locale;

public class EatTimerModule extends Module {
    public EatTimerModule() {
        super("eat_timer", "Eat Timer", "Odliczanie jedzenia z ulamkami sekundy");
        at(10, 58);
        option("bar", "Pasek", true);
        option("pct", "Procenty", false);
    }

    private int total = 0;

    @Override public int w() { return 110; }
    @Override public int h() { return option("bar") ? 18 : 12; }

    @Override
    protected void draw(DrawContext ctx, boolean preview) {
        MinecraftClient mc = MinecraftClient.getInstance();
        float frac;
        String text;
        if (preview) {
            frac = 0.66f;
            text = "Jedzenie: 1.32s";
        } else {
            PlayerEntity p = mc.player;
            if (p == null || !p.isUsingItem() || !p.getActiveItem().contains(DataComponentTypes.FOOD)) {
                total = 0;
                return;
            }
            int left = p.getItemUseTimeLeft();
            total = Math.max(total, left);
            frac = total > 0 ? (float) left / total : 0f;
            text = String.format(Locale.ROOT, "Jedzenie: %.2fs", left / 20.0);
        }
        if (option("pct")) text += " (" + Math.round(frac * 100f) + "%)";
        ctx.drawTextWithShadow(mc.textRenderer, text, 0, 0, rgb());
        if (option("bar")) drawBar(ctx, 0, 11, w(), 4, frac, 0xFF000000 | rgb());
    }
}
