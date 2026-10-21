package com.hexmod.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;

import java.util.Locale;

/** "Tarcza break": zlamana tarcza wroga (topor) i Twoja wylaczona tarcza. */
public class ShieldBreakModule extends Module {
    private static final long ENEMY_BREAK_MS = 5000;
    private static long enemyUntil = 0;
    private static String enemyName = "";

    public ShieldBreakModule() {
        super("shield_break", "Shield Break", "Komunikat po zlamaniu tarczy toporem");
        centered(52);
    }

    public static void onEnemyShieldHit(PlayerEntity target) {
        enemyUntil = System.currentTimeMillis() + ENEMY_BREAK_MS;
        enemyName = target.getName().getString();
    }

    @Override public int w() { return 150; }
    @Override public int h() { return 24; }

    @Override
    protected void draw(DrawContext ctx, boolean preview) {
        MinecraftClient mc = MinecraftClient.getInstance();
        PlayerEntity p = mc.player;
        if (p == null) return;
        if (preview) {
            ctx.drawTextWithShadow(mc.textRenderer, "Tarcza break: Wrog 4.2s", 0, 0, rgb());
            return;
        }
        int line = 0;
        long left = enemyUntil - System.currentTimeMillis();
        if (left > 0) {
            ctx.drawTextWithShadow(mc.textRenderer,
                    String.format(Locale.ROOT, "Tarcza break: %s %.1fs", enemyName, left / 1000.0), 0, line, rgb());
            line += 12;
        }
        float progress = p.getItemCooldownManager().getCooldownProgress(Items.SHIELD.getDefaultStack(), 0f);
        if (progress > 0f) {
            ctx.drawTextWithShadow(mc.textRenderer,
                    String.format(Locale.ROOT, "Twoja tarcza: break %.1fs", progress * 5f), 0, line, 0xFFD10A0A);
        }
    }
}
