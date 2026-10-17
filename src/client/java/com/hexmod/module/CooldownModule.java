package com.hexmod.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;

import java.util.List;
import java.util.Locale;

/** Cooldowny wybranych przedmiotow w sekundach. Ktore przedmioty - ustawiasz w edytorze HUD. */
public class CooldownModule extends Module {
    private record Tracked(String key, Item item, String label, int totalTicks) {}

    private static final List<Tracked> TRACKED = List.of(
            new Tracked("pearl", Items.ENDER_PEARL, "Perla", 20),
            new Tracked("shield", Items.SHIELD, "Tarcza", 100),
            new Tracked("chorus", Items.CHORUS_FRUIT, "Chorus", 20),
            new Tracked("wind", Items.WIND_CHARGE, "Wind charge", 10)
    );

    public CooldownModule() {
        super("cooldowns", "Cooldowny", "Czasy odnowienia wybranych przedmiotow");
        at(10, 70);
        for (Tracked t : TRACKED) option(t.key(), t.label(), true);
    }

    @Override public int w() { return 90; }
    @Override public int h() { return 48; }

    @Override
    protected void draw(DrawContext ctx, boolean preview) {
        MinecraftClient mc = MinecraftClient.getInstance();
        PlayerEntity p = mc.player;
        if (p == null) return;
        int line = 0;
        for (Tracked t : TRACKED) {
            if (!option(t.key())) continue;
            String text;
            if (preview) {
                text = t.label() + ": 0.8s";
            } else {
                float progress = p.getItemCooldownManager().getCooldownProgress(t.item().getDefaultStack(), 0f);
                if (progress <= 0f) continue;
                text = String.format(Locale.ROOT, "%s: %.1fs", t.label(), progress * t.totalTicks() / 20f);
            }
            ctx.drawTextWithShadow(mc.textRenderer, text, 0, line, rgb());
            line += 12;
        }
    }
}
