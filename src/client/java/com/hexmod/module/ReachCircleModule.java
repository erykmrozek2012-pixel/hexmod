package com.hexmod.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/** Kolo wokol Ciebie pokazujace zasieg ataku; zmienia kolor, gdy jakis gracz jest w zasiegu. */
public class ReachCircleModule extends Module {
    private static final int SEGMENTS = 96;

    public ReachCircleModule() {
        super("reach_circle", "Kolo zasiegu", "Kolo zasiegu ataku; zmienia kolor, gdy gracz jest w srodku");
        color = 0x64D2FF;
    }

    @Override
    protected boolean overlay() {
        return true;
    }

    private static boolean anyInRange(MinecraftClient mc, Vec3d eye, double range) {
        for (PlayerEntity pl : mc.world.getPlayers()) {
            if (pl == mc.player || pl.isSpectator()) continue;
            Box b = pl.getBoundingBox();
            Vec3d closest = new Vec3d(
                    Math.max(b.minX, Math.min(eye.x, b.maxX)),
                    Math.max(b.minY, Math.min(eye.y, b.maxY)),
                    Math.max(b.minZ, Math.min(eye.z, b.maxZ)));
            if (eye.distanceTo(closest) <= range) return true;
        }
        return false;
    }

    private static void segment(DrawContext ctx, float[] a, float[] b, int c) {
        float dx = b[0] - a[0], dy = b[1] - a[1];
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        if (dist > 250f) return;
        int steps = Math.max(1, (int) (dist / 2f));
        for (int k = 0; k <= steps; k++) {
            int x = Math.round(a[0] + dx * k / steps);
            int y = Math.round(a[1] + dy * k / steps);
            ctx.fill(x, y, x + 2, y + 2, c);
        }
    }

    @Override
    protected void draw(DrawContext ctx, boolean preview) {
        if (preview) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;
        if (!mc.options.getPerspective().isFirstPerson()) return;

        float tp = mc.getRenderTickCounter().getTickProgress(false);
        Vec3d center = mc.player.getLerpedPos(tp);
        double r = mc.player.getEntityInteractionRange();
        boolean inside = anyInRange(mc, mc.player.getCameraPosVec(tp), r);
        int c = inside ? 0xFFD10A0A : rgb();

        float[] prev = null;
        for (int i = 0; i <= SEGMENTS; i++) {
            double ang = 2.0 * Math.PI * i / SEGMENTS;
            Vec3d pt = new Vec3d(center.x + r * Math.cos(ang), center.y + 0.05, center.z + r * Math.sin(ang));
            float[] s = Projection.project(ctx, pt);
            if (s != null && prev != null) segment(ctx, prev, s, c);
            prev = s;
        }
    }
}
