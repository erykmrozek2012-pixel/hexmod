package com.hexmod.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.Perspective;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.HashMap;

/** Zamiana punktu w swiecie na wspolrzedne ekranu (takze w F5) i sprawdzanie, czy punkt jest zaslloniety. */
public final class Projection {
    public record Cam(Vec3d pos, double yaw, double pitch) {}

    private static Cam cached;
    private static float cachedTp = Float.NaN;
    private static long cachedTime = -1;

    private static final HashMap<Integer, Boolean> SEE = new HashMap<>();
    private static long seeTime = Long.MIN_VALUE;

    private Projection() {}

    /** Czy widzisz gracza. Wynik (promienie w swiecie) liczony raz na tick zamiast w kazdej klatce i w kazdym module. */
    public static boolean canSee(PlayerEntity pl) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return false;
        long t = mc.world.getTime();
        if (t != seeTime) {
            SEE.clear();
            seeTime = t;
        }
        Boolean b = SEE.get(pl.getId());
        if (b == null) {
            b = mc.player.canSee(pl);
            SEE.put(pl.getId(), b);
        }
        return b;
    }

    private static Vec3d forward(double yaw, double pitch) {
        return new Vec3d(-Math.sin(yaw) * Math.cos(pitch), -Math.sin(pitch), Math.cos(yaw) * Math.cos(pitch));
    }

    /** Pozycja i obrot kamery w tej klatce (z perspektywa F5 wlacznie). */
    public static Cam camera() {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.world == null) return null;
        float tp = mc.getRenderTickCounter().getTickProgress(false);
        long time = mc.world.getTime();
        if (cached != null && tp == cachedTp && time == cachedTime) return cached;

        Vec3d eye = p.getCameraPosVec(tp);
        double yaw = p.getYaw(tp);
        double pitch = p.getPitch(tp);
        Perspective persp = mc.options.getPerspective();
        Cam cam;
        if (persp.isFirstPerson()) {
            cam = new Cam(eye, Math.toRadians(yaw), Math.toRadians(pitch));
        } else {
            if (persp.isFrontView()) {
                yaw += 180.0;
                pitch = -pitch;
            }
            double ry = Math.toRadians(yaw), rp = Math.toRadians(pitch);
            Vec3d f = forward(ry, rp);
            double dist = 4.0;
            Vec3d end = eye.subtract(f.multiply(dist));
            var hit = mc.world.raycast(new RaycastContext(eye, end,
                    RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, p));
            if (hit.getType() != HitResult.Type.MISS) {
                dist = Math.max(0.1, eye.distanceTo(hit.getPos()) - 0.1);
            }
            cam = new Cam(eye.subtract(f.multiply(dist)), ry, rp);
        }
        cached = cam;
        cachedTp = tp;
        cachedTime = time;
        return cam;
    }

    /** Zwraca {x, y} na ekranie (skala GUI) albo null, gdy punkt jest za kamera. Bez tworzenia obiektow Vec3d. */
    public static float[] project(DrawContext ctx, Vec3d target) {
        MinecraftClient mc = MinecraftClient.getInstance();
        Cam c = camera();
        if (c == null) return null;

        double cy = Math.cos(c.yaw()), sy = Math.sin(c.yaw());
        double cp = Math.cos(c.pitch()), sp = Math.sin(c.pitch());
        double fx = -sy * cp, fy = -sp, fz = cy * cp;
        double rx = -cy, rz = -sy;
        double ux = -rz * fy, uy = rz * fx - rx * fz, uz = rx * fy;
        Vec3d pos = c.pos();
        double dx = target.x - pos.x, dy = target.y - pos.y, dz = target.z - pos.z;

        double zf = dx * fx + dy * fy + dz * fz;
        if (zf < 0.05) return null;
        double xr = dx * rx + dz * rz;
        double yu = dx * ux + dy * uy + dz * uz;

        double t = Math.tan(Math.toRadians(mc.options.getFov().getValue()) / 2.0);
        double w = ctx.getScaledWindowWidth();
        double h = ctx.getScaledWindowHeight();
        float sx = (float) (w / 2.0 + (xr / (zf * t)) * (h / 2.0));
        float sY = (float) (h / 2.0 - (yu / (zf * t)) * (h / 2.0));
        return new float[]{sx, sY};
    }

    /** Czy miedzy kamera a punktem nie ma bloku (punkt widoczny, nie zaslloniety). */
    public static boolean visible(Vec3d target) {
        MinecraftClient mc = MinecraftClient.getInstance();
        Cam c = camera();
        if (c == null || mc.world == null || mc.player == null) return false;
        var hit = mc.world.raycast(new RaycastContext(c.pos(), target,
                RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player));
        return hit.getType() == HitResult.Type.MISS;
    }
}
