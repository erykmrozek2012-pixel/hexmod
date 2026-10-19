package com.hexmod.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.Locale;

/** Ile jeszcze poleci Twoja perla enderu (szacunek z fizyki lotu, tylko liczba w widgecie). */
public class PearlTimerModule extends Module {
    public PearlTimerModule() {
        super("pearl_timer", "Czas perly", "Ile sekund jeszcze poleci Twoja perla");
        at(120, 130);
    }

    @Override public int w() { return 90; }

    private static double flightSeconds(MinecraftClient mc, EnderPearlEntity pearl) {
        ClientWorld world = mc.world;
        if (world == null) return -1;
        Vec3d p = new Vec3d(pearl.getX(), pearl.getY(), pearl.getZ());
        Vec3d v = pearl.getVelocity();
        for (int t = 1; t <= 240; t++) {
            Vec3d np = p.add(v);
            var hit = world.raycast(new RaycastContext(p, np,
                    RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, pearl));
            if (hit.getType() != HitResult.Type.MISS) return t / 20.0;
            p = np;
            v = v.multiply(0.99).subtract(0, 0.03, 0);
        }
        return -1;
    }

    @Override
    protected void draw(DrawContext ctx, boolean preview) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (preview) {
            ctx.drawTextWithShadow(mc.textRenderer, "Perla: 1.4s", 0, 0, rgb());
            return;
        }
        if (mc.world == null || mc.player == null) return;
        double best = -1;
        for (Entity e : mc.world.getEntities()) {
            if (!(e instanceof EnderPearlEntity pearl) || pearl.getOwner() != mc.player) continue;
            double s = flightSeconds(mc, pearl);
            if (s >= 0 && (best < 0 || s < best)) best = s;
        }
        if (best >= 0) {
            ctx.drawTextWithShadow(mc.textRenderer, String.format(Locale.ROOT, "Perla: %.1fs", best), 0, 0, rgb());
        }
    }
}
