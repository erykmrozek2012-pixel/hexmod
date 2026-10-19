package com.hexmod.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/**
 * Kolo zasiegu ataku na ziemi wokol Ciebie i innych graczy (takze w F5). Zmienia kolor na czerwony,
 * gdy ktos jest w zasiegu. Linia chowa sie za blokami.
 */
public class ReachCircleModule extends Module {
    public ReachCircleModule() {
        super("reach_circle", "Kolo zasiegu", "Kolo zasiegu ataku wokol Ciebie i innych graczy");
        color = 0x64D2FF;
        option("self", "Moje", true);
        option("others", "Inni", true);
        option("occ", "Zaslon", true);
    }

    @Override
    protected boolean overlay() {
        return true;
    }

    @Override
    public boolean hasOverlayPreview() {
        return true;
    }

    private static boolean anyInRange(MinecraftClient mc, PlayerEntity owner, double range) {
        Vec3d eye = owner.getEyePos();
        for (PlayerEntity pl : mc.world.getPlayers()) {
            if (pl == owner || pl.isSpectator()) continue;
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

    private void ring(DrawContext ctx, Vec3d center, double r, int segs, boolean occ, int c) {
        float[] prev = null;
        boolean prevVisible = false;
        for (int i = 0; i <= segs; i++) {
            double ang = 2.0 * Math.PI * i / segs;
            Vec3d pt = new Vec3d(center.x + r * Math.cos(ang), center.y + 0.05, center.z + r * Math.sin(ang));
            float[] s = Projection.project(ctx, pt);
            boolean vis = s != null && (!occ || Projection.visible(pt));
            if (vis && prev != null && prevVisible) segment(ctx, prev, s, c);
            prev = s;
            prevVisible = vis;
        }
    }

    @Override
    protected void draw(DrawContext ctx, boolean preview) {
        if (preview) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;
        float tp = mc.getRenderTickCounter().getTickProgress(false);
        boolean occ = option("occ");
        int hit = 0xFFD10A0A;

        if (option("self")) {
            double r = mc.player.getEntityInteractionRange();
            ring(ctx, mc.player.getLerpedPos(tp), r, 96, occ, anyInRange(mc, mc.player, r) ? hit : rgb());
        }
        if (option("others")) {
            for (PlayerEntity pl : mc.world.getPlayers()) {
                if (pl == mc.player || pl.isSpectator() || pl.isInvisible()) continue;
                if (mc.player.squaredDistanceTo(pl) > 32.0 * 32.0) continue;
                ring(ctx, pl.getLerpedPos(tp), 3.0, 64, occ, anyInRange(mc, pl, 3.0) ? hit : (0xB0000000 | color));
            }
        }
    }

    // ---------- podglad w edytorze HUD (elipsa) ----------

    @Override
    public void drawOverlayPreview(DrawContext ctx, int ax, int ay) {
        float[] prev = null;
        for (int i = 0; i <= 48; i++) {
            double ang = 2.0 * Math.PI * i / 48;
            float[] s = {(float) (ax + 44 * Math.cos(ang)), (float) (ay + 30 + 12 * Math.sin(ang))};
            if (prev != null) segment(ctx, prev, s, rgb());
            prev = s;
        }
    }

    @Override
    public int[] overlayPreviewBox(int ax, int ay) {
        return new int[]{ax - 46, ay + 16, ax + 46, ay + 44};
    }
}
