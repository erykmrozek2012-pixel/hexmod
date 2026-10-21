package com.hexmod.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.Locale;

/** Zasieg ostatniego trafienia: odleglosc od Twoich oczu do najblizszego punktu hitboxa przeciwnika. */
public class ReachModule extends Module {
    private static double lastReach = -1;
    private static long lastAt = 0;

    public ReachModule() {
        super("reach", "Reach", "Distance of your last hit");
        at(10, 224);
    }

    /** Wolane, gdy atakujesz gracza. */
    public static void onAttack(PlayerEntity player, Entity target) {
        Vec3d eye = player.getCameraPosVec(1f);
        Box b = target.getBoundingBox();
        double cx = Math.max(b.minX, Math.min(eye.x, b.maxX));
        double cy = Math.max(b.minY, Math.min(eye.y, b.maxY));
        double cz = Math.max(b.minZ, Math.min(eye.z, b.maxZ));
        lastReach = eye.distanceTo(new Vec3d(cx, cy, cz));
        lastAt = System.currentTimeMillis();
    }

    @Override public int w() { return 90; }

    @Override
    protected void draw(DrawContext ctx, boolean preview) {
        MinecraftClient mc = MinecraftClient.getInstance();
        String t;
        if (preview) t = L.t("Reach: 2.84");
        else if (lastReach < 0 || System.currentTimeMillis() - lastAt > 5000) t = L.t("Reach: -");
        else t = String.format(Locale.ROOT, L.t("Reach: %.2f"), lastReach);
        ctx.drawTextWithShadow(mc.textRenderer, t, 0, 0, rgb());
    }
}
