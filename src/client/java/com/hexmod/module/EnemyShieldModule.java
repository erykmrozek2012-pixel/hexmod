package com.hexmod.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.Vec3d;

import java.util.Optional;

/** Status tarczy gracza, na ktorego patrzysz (do 20 kratek): uniesiona / opuszczona / brak tarczy. */
public class EnemyShieldModule extends Module {
    private static final double RANGE = 20.0;

    public EnemyShieldModule() {
        super("enemy_shield", "Enemy Shield", "Czy wrog ma uniesiona tarcze");
        centered(34);
    }

    @Override public int w() { return 140; }

    /** Najblizszy widoczny gracz na linii wzroku (nie tylko w zasiegu reki). */
    private static PlayerEntity findTarget(MinecraftClient mc) {
        if (mc.player == null || mc.world == null) return null;
        float tp = mc.getRenderTickCounter().getTickProgress(false);
        Vec3d eye = mc.player.getCameraPosVec(tp);
        Vec3d end = eye.add(mc.player.getRotationVec(tp).multiply(RANGE));
        PlayerEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (PlayerEntity pl : mc.world.getPlayers()) {
            if (pl == mc.player || pl.isSpectator()) continue;
            Optional<Vec3d> hit = pl.getBoundingBox().expand(0.2).raycast(eye, end);
            if (hit.isEmpty()) continue;
            double d = eye.squaredDistanceTo(hit.get());
            if (d < bestDist && mc.player.canSee(pl)) {
                best = pl;
                bestDist = d;
            }
        }
        return best;
    }

    @Override
    protected void draw(DrawContext ctx, boolean preview) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (preview) {
            ctx.drawTextWithShadow(mc.textRenderer, "Tarcza wroga: UNIESIONA", 0, 0, rgb());
            return;
        }
        PlayerEntity target = findTarget(mc);
        if (target == null) return;

        boolean holds = target.getMainHandStack().isOf(Items.SHIELD) || target.getOffHandStack().isOf(Items.SHIELD);
        String text;
        int color;
        if (target.isBlocking()) {
            text = "Tarcza wroga: UNIESIONA";
            color = 0xFFD10A0A;
        } else if (holds) {
            text = "Tarcza wroga: opuszczona";
            color = rgb();
        } else {
            text = "Tarcza wroga: brak";
            color = 0xFF9A9A9A;
        }
        ctx.drawTextWithShadow(mc.textRenderer, text, 0, 0, color);
    }
}
