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
        option("kills", "Kills", true);
        option("deaths", "Deaths", true);
        option("ratio", "K/D", true);
        option("streak", "Streak", true);
        defaultStyle(7);
    }

    @Override public int w() { return 150; }
    private boolean line1() { return option("kills") || option("deaths"); }

    private boolean line2() { return option("ratio") || option("streak"); }

    @Override
    public int h() {
        return Math.max(12, (line1() ? 12 : 0) + (line2() ? 12 : 0));
    }

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
        int k = preview ? 3 : kills, d = preview ? 1 : deaths, st = preview ? 2 : streak;
        int y = 0;
        if (line1()) {
            String t1 = (option("kills") ? L.t("Kills: ") + k : "") + (option("kills") && option("deaths") ? "  " : "")
                    + (option("deaths") ? L.t("Deaths: ") + d : "");
            ctx.drawTextWithShadow(mc.textRenderer, t1, 0, y, rgb());
            y += 12;
        }
        if (line2()) {
            String kd = d == 0 ? String.format(Locale.ROOT, "%.1f", (float) k) : String.format(Locale.ROOT, "%.2f", (float) k / d);
            String t2 = (option("ratio") ? L.t("K/D: ") + kd : "") + (option("ratio") && option("streak") ? "  " : "")
                    + (option("streak") ? L.t("Streak: ") + st : "");
            ctx.drawTextWithShadow(mc.textRenderer, t2, 0, y, rgb());
        }
    }
}
