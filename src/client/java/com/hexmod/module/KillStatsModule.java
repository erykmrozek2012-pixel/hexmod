package com.hexmod.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;

import java.util.Locale;
import java.util.UUID;

/** K/D Counter: zabojstwa, smierci, seria i stosunek K/D w tej sesji gry (komenda .kd zeruje). */
public class KillStatsModule extends Module {
    private static int kills = 0, deaths = 0, streak = 0;
    private static UUID victim = null;
    private static long victimAt = 0;
    private boolean wasDead = false;

    public KillStatsModule() {
        super("kd", "K/D Counter", "Kills, deaths and kill streak in this session");
        at(10, 380);
    }

    @Override public int w() { return 150; }
    @Override public int h() { return 24; }

    /** Wolane, gdy atakujesz gracza: zapamietuje ofiare, zeby policzyc zabojstwo. */
    public static void onAttack(PlayerEntity target) {
        victim = target.getUuid();
        victimAt = System.currentTimeMillis();
    }

    public static void reset() {
        kills = 0;
        deaths = 0;
        streak = 0;
        victim = null;
    }

    @Override
    public void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) {
            wasDead = false;
            return;
        }
        boolean dead = mc.player.isDead();
        if (dead && !wasDead) {
            deaths++;
            streak = 0;
        }
        wasDead = dead;
        if (victim == null) return;
        if (System.currentTimeMillis() - victimAt > 8000L) {
            victim = null;
            return;
        }
        for (PlayerEntity pl : mc.world.getPlayers()) {
            if (pl.getUuid().equals(victim) && (pl.isDead() || pl.getHealth() <= 0f)) {
                kills++;
                streak++;
                victim = null;
                break;
            }
        }
    }

    @Override
    protected void draw(DrawContext ctx, boolean preview) {
        MinecraftClient mc = MinecraftClient.getInstance();
        int k = preview ? 3 : kills, d = preview ? 1 : deaths, s = preview ? 2 : streak;
        ctx.drawTextWithShadow(mc.textRenderer, L.t("Kills: ") + k + "  " + L.t("Deaths: ") + d, 0, 0, rgb());
        String kd = d == 0 ? String.format(Locale.ROOT, "%.1f", (float) k) : String.format(Locale.ROOT, "%.2f", (float) k / d);
        ctx.drawTextWithShadow(mc.textRenderer, L.t("K/D: ") + kd + "  " + L.t("Streak: ") + s, 0, 12, rgb());
    }
}
