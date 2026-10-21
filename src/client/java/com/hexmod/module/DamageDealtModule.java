package com.hexmod.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** Ile HP zadales przeciwnikowi: ostatnie trafienie i suma w biezacej walce. */
public class DamageDealtModule extends Module {
    private static final Map<UUID, Float> LAST_HEALTH = new HashMap<>();
    private static UUID target = null;
    private static long attackAt = 0;
    private static String targetName = "";
    private static float lastHit = 0f;
    private static float total = 0f;
    private static long lastHitAt = 0;

    public DamageDealtModule() {
        super("damage_dealt", "Damage Dealt", "Ile HP zadales przeciwnikowi");
        at(120, 146);
    }

    /** Wywolywane, gdy atakujesz gracza. */
    public static void onAttack(PlayerEntity t) {
        target = t.getUuid();
        targetName = t.getName().getString();
        attackAt = System.currentTimeMillis();
    }

    @Override public int w() { return 130; }
    @Override public int h() { return 24; }

    @Override
    public void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;
        long now = System.currentTimeMillis();
        for (PlayerEntity pl : mc.world.getPlayers()) {
            if (pl == mc.player) continue;
            UUID id = pl.getUuid();
            float hp = pl.getHealth() + pl.getAbsorptionAmount();
            Float prev = LAST_HEALTH.put(id, hp);
            if (prev != null && hp < prev && id.equals(target) && now - attackAt < 1000) {
                float dmg = prev - hp;
                if (now - lastHitAt > 10_000) total = 0f;
                lastHit = dmg;
                total += dmg;
                lastHitAt = now;
                attackAt = 0;
            }
        }
    }

    @Override
    protected void draw(DrawContext ctx, boolean preview) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (preview) {
            ctx.drawTextWithShadow(mc.textRenderer, "Zadano: 6.0 HP", 0, 0, rgb());
            ctx.drawTextWithShadow(mc.textRenderer, "Razem: 12.5 (Wrog)", 0, 12, rgb());
            return;
        }
        if (System.currentTimeMillis() - lastHitAt > 10_000) return;
        ctx.drawTextWithShadow(mc.textRenderer, String.format(Locale.ROOT, "Zadano: %.1f HP", lastHit), 0, 0, rgb());
        ctx.drawTextWithShadow(mc.textRenderer,
                String.format(Locale.ROOT, "Razem: %.1f (%s)", total, targetName), 0, 12, rgb());
    }
}
