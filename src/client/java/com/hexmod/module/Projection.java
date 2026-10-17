package com.hexmod.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Vec3d;

/** Zamiana punktu w swiecie na wspolrzedne ekranu (widok z pierwszej osoby). */
public final class Projection {
    private Projection() {}

    /** Zwraca {x, y} na ekranie (skala GUI) albo null, gdy punkt jest za kamera. */
    public static float[] project(DrawContext ctx, Vec3d target) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity p = mc.player;
        if (p == null) return null;

        float tp = mc.getRenderTickCounter().getTickProgress(false);
        Vec3d cam = p.getCameraPosVec(tp);
        double yaw = Math.toRadians(p.getYaw(tp));
        double pitch = Math.toRadians(p.getPitch(tp));

        Vec3d f = new Vec3d(-Math.sin(yaw) * Math.cos(pitch), -Math.sin(pitch), Math.cos(yaw) * Math.cos(pitch));
        Vec3d r = new Vec3d(-Math.cos(yaw), 0, -Math.sin(yaw));
        Vec3d u = r.crossProduct(f);
        Vec3d d = target.subtract(cam);

        double zf = d.dotProduct(f);
        if (zf < 0.05) return null;
        double xr = d.dotProduct(r);
        double yu = d.dotProduct(u);

        double t = Math.tan(Math.toRadians(mc.options.getFov().getValue()) / 2.0);
        double w = ctx.getScaledWindowWidth();
        double h = ctx.getScaledWindowHeight();
        float sx = (float) (w / 2.0 + (xr / (zf * t)) * (h / 2.0));
        float sy = (float) (h / 2.0 - (yu / (zf * t)) * (h / 2.0));
        return new float[]{sx, sy};
    }
}
