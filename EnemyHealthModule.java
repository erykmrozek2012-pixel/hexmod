package com.hexmod.module;

import net.minecraft.entity.player.PlayerEntity;

import java.util.Locale;

/** Zdrowie wroga nad glowa (z absorpcja). */
public class EnemyHealthModule extends OverheadModule {
    public EnemyHealthModule() {
        super("enemy_health", "Zdrowie wroga", "Ile zdrowia ma wrog (nad glowa)", 0);
    }

    @Override
    protected String text(PlayerEntity p) {
        return String.format(Locale.ROOT, "HP %.1f", p.getHealth() + p.getAbsorptionAmount());
    }

    @Override
    protected int colorOf(PlayerEntity p) {
        float ratio = p.getHealth() / Math.max(1f, p.getMaxHealth());
        if (ratio > 0.66f) return 0x30D158;
        if (ratio > 0.33f) return 0xFFD60A;
        return 0xD10A0A;
    }
}
