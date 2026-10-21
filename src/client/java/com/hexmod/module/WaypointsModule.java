package com.hexmod.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Waypointy: znaczniki w swiecie (nazwa + odleglosc) oraz lista najblizszych na HUD.
 * Zarzadzanie punktami: Narzedzia -> Waypointy albo komendy .wp add/del/list.
 */
public class WaypointsModule extends Module {
    public WaypointsModule() {
        super("waypoints", "Waypointy", "Znaczniki zapisanych miejsc w swiecie i lista najblizszych");
        at(10, 130);
        option("world", "Znaczniki", true);
        option("list", "Lista", true);
        option("dist", "Odleglosc", true);
        option("death", "Punkt smierci", true);
    }

    private boolean wasDead = false;

    /** Po smierci zapisuje waypoint "Smierc" w miejscu, gdzie zginales (poprzedni taki punkt jest zastepowany). */
    @Override
    public void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) {
            wasDead = false;
            return;
        }
        boolean dead = mc.player.isDead();
        if (dead && !wasDead && option("death")) {
            WaypointManager.removeByName("Smierc");
            WaypointManager.addHere("Smierc");
        }
        wasDead = dead;
    }

    @Override public int w() { return 120; }
    @Override public int h() { return 36; }

    private static String fmt(double d) {
        if (d >= 1000) return String.format(Locale.ROOT, "%.1fkm", d / 1000.0);
        return Math.round(d) + "m";
    }

    private List<WaypointManager.Waypoint> here() {
        String dim = WaypointManager.currentDim();
        List<WaypointManager.Waypoint> out = new ArrayList<>();
        for (WaypointManager.Waypoint w : WaypointManager.list()) {
            if (w.dim == null || w.dim.isEmpty() || w.dim.equals(dim)) out.add(w);
        }
        return out;
    }

    private static double dist(MinecraftClient mc, WaypointManager.Waypoint w) {
        double dx = mc.player.getX() - (w.x + 0.5), dy = mc.player.getY() - (w.y + 0.5), dz = mc.player.getZ() - (w.z + 0.5);
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /** Lista najblizszych punktow na HUD (widget). */
    @Override
    protected void draw(net.minecraft.client.gui.DrawContext ctx, boolean preview) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!option("list")) return;
        if (preview) {
            ctx.drawTextWithShadow(mc.textRenderer, "Dom: 120m", 0, 0, rgb());
            ctx.drawTextWithShadow(mc.textRenderer, "Baza: 340m", 0, 12, rgb());
            return;
        }
        if (mc.player == null) return;
        List<WaypointManager.Waypoint> pts = here();
        pts.sort(Comparator.comparingDouble(w -> dist(mc, w)));
        int line = 0;
        for (int i = 0; i < Math.min(3, pts.size()); i++) {
            WaypointManager.Waypoint w = pts.get(i);
            String t = option("dist") ? w.name + ": " + fmt(dist(mc, w)) : w.name;
            ctx.drawTextWithShadow(mc.textRenderer, t, 0, line, 0xFF000000 | w.color);
            line += 12;
        }
    }

    /** Znaczniki w swiecie, rysowane osobnym elementem HUD (wolane z HexmodClient). */
    public static void renderMarkers(DrawContext ctx) {
        if (Remote.isLocked()) return;
        Module m = null;
        for (Module x : ModuleManager.all()) if (x instanceof WaypointsModule) m = x;
        if (!(m instanceof WaypointsModule wm) || !wm.isEnabled() || wm.hidden) return;
        if (Module.extrasHidden || !wm.option("world")) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;
        int sw = ctx.getScaledWindowWidth(), sh = ctx.getScaledWindowHeight();
        for (WaypointManager.Waypoint w : wm.here()) {
            float[] p = Projection.project(ctx, new Vec3d(w.x + 0.5, w.y + 1.0, w.z + 0.5));
            if (p == null) continue;
            if (p[0] < 4 || p[0] > sw - 4 || p[1] < 4 || p[1] > sh - 14) continue;
            int px = Math.round(p[0]), py = Math.round(p[1]);
            int col = 0xFF000000 | w.color;
            ctx.fill(px - 3, py - 3, px + 3, py + 3, 0xFF000000);
            ctx.fill(px - 2, py - 2, px + 2, py + 2, col);
            String label = wm.option("dist") ? w.name + " " + fmt(dist(mc, w)) : w.name;
            int tw = mc.textRenderer.getWidth(label);
            ctx.drawTextWithShadow(mc.textRenderer, label, px - tw / 2, py - 14, col);
        }
    }
}
