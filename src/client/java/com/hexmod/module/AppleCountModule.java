package com.hexmod.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Arm;
import net.minecraft.util.math.Vec3d;

/**
 * Licznik refow (zlote jablka) i koxow (zaczarowane zlote jablka), jak Golden Apple Count.
 * Nad reka wroga pokazuje tekst z liczba jablek, ktore trzyma. Na HUD pokazuje, ile masz ich w ekwipunku.
 */
public class AppleCountModule extends Module {
    private static final int REF_COLOR = 0xFFD60A;
    private static final int KOX_COLOR = 0xBF5AF2;

    public AppleCountModule() {
        super("apples", "Gapple Count", "Liczba refow (zlote jablka) i koxow (zaczarowane) w rece wroga i w ekwipunku");
        at(10, 294);
        option("ref", "Ref", true);
        option("kox", "Kox", true);
        option("enemy", "Nad wrogiem", true);
        option("own", "Moje", true);
        option("walls", "Przez sciany", false);
    }

    @Override public int w() { return 90; }
    @Override public int h() { return 24; }

    private static boolean isRef(ItemStack s) {
        return s != null && !s.isEmpty() && s.isOf(Items.GOLDEN_APPLE);
    }

    private static boolean isKox(ItemStack s) {
        return s != null && !s.isEmpty() && s.isOf(Items.ENCHANTED_GOLDEN_APPLE);
    }

    /** Widget na HUD: ile refow i koxow masz w ekwipunku. */
    @Override
    protected void draw(DrawContext ctx, boolean preview) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!option("own")) return;
        int refs = 12, koxes = 3;
        if (!preview && mc.player != null) {
            refs = 0;
            koxes = 0;
            var inv = mc.player.getInventory();
            for (int i = 0; i < inv.size(); i++) {
                ItemStack s = inv.getStack(i);
                if (isRef(s)) refs += s.getCount();
                if (isKox(s)) koxes += s.getCount();
            }
        }
        int y = 0;
        if (option("ref")) {
            ctx.drawTextWithShadow(mc.textRenderer, "Ref: " + refs, 0, y, 0xFF000000 | REF_COLOR);
            y += 12;
        }
        if (option("kox")) {
            ctx.drawTextWithShadow(mc.textRenderer, "Kox: " + koxes, 0, y, 0xFF000000 | KOX_COLOR);
        }
    }

    /** Pozycja dloni gracza w swiecie (przyblizona): z boku i lekko z przodu ciala. */
    private static Vec3d handPos(PlayerEntity pl, boolean mainHand) {
        double yaw = Math.toRadians(pl.getBodyYaw());
        boolean right = (pl.getMainArm() == Arm.RIGHT) == mainHand;
        double side = right ? 1.0 : -1.0;
        double rx = -Math.cos(yaw), rz = -Math.sin(yaw);
        double fx = -Math.sin(yaw), fz = Math.cos(yaw);
        return new Vec3d(pl.getX() + rx * 0.4 * side + fx * 0.3,
                pl.getY() + pl.getHeight() * 0.62,
                pl.getZ() + rz * 0.4 * side + fz * 0.3);
    }

    private static void label(DrawContext ctx, MinecraftClient mc, PlayerEntity pl, ItemStack s, boolean mainHand,
                              boolean walls, boolean showRef, boolean showKox, float sc) {
        boolean ref = showRef && isRef(s), kox = showKox && isKox(s);
        if (!ref && !kox) return;
        Vec3d pos = handPos(pl, mainHand);
        if (!walls && !Projection.visible(pos)) return;
        float[] p = Projection.project(ctx, pos);
        if (p == null) return;
        int sw = ctx.getScaledWindowWidth(), sh = ctx.getScaledWindowHeight();
        int px = Math.round(p[0]), py = Math.round(p[1]);
        if (px < 4 || px > sw - 40 || py < 4 || py > sh - 12) return;
        String t = (ref ? "Ref x" : "Kox x") + s.getCount();
        int tw = mc.textRenderer.getWidth(t);
        // Stala wielkosc na ekranie: nie zalezy od odleglosci, tylko od skali widgetu (edytor HUD).
        var m = ctx.getMatrices();
        m.pushMatrix();
        m.translate((float) px, (float) py);
        m.scale(sc, sc);
        ctx.fill(-tw / 2 - 2, -2, tw / 2 + 2, 10, 0x88000000);
        ctx.drawTextWithShadow(mc.textRenderer, t, -tw / 2, 0, 0xFF000000 | (ref ? REF_COLOR : KOX_COLOR));
        m.popMatrix();
    }

    /** Teksty nad rekami wrogow, rysowane osobnym elementem HUD (wolane z HexmodClient). */
    public static void renderMarkers(DrawContext ctx) {
        if (Remote.isLocked()) return;
        AppleCountModule am = null;
        for (Module x : ModuleManager.all()) if (x instanceof AppleCountModule a) am = a;
        if (am == null || !am.isEnabled() || am.hidden || Module.extrasHidden || !am.option("enemy")) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;
        boolean walls = am.option("walls"), showRef = am.option("ref"), showKox = am.option("kox");
        for (PlayerEntity pl : mc.world.getPlayers()) {
            if (pl == mc.player || pl.isSpectator()) continue;
            if (mc.player.distanceTo(pl) > 48f) continue;
            label(ctx, mc, pl, pl.getMainHandStack(), true, walls, showRef, showKox, am.scale);
            label(ctx, mc, pl, pl.getOffHandStack(), false, walls, showRef, showKox, am.scale);
        }
    }
}
