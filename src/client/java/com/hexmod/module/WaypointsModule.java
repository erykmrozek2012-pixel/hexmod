package com.hexmod.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Waypointy (osobne dla kazdego serwera): znaczniki w swiecie oraz HUD z lista najblizszych,
 * z odlegloscia i strzalka kierunku. Zarzadzanie: Narzedzia -> Waypoints albo komendy .wp.
 */
public class WaypointsModule extends Module {
    private static final String[] ARROWS = {"\u2191", "\u2197", "\u2192", "\u2198", "\u2193", "\u2199", "\u2190", "\u2196"};
    private boolean wasDead = false;

    public WaypointsModule() {
        super("waypoints", "Waypoints", "Saved places as markers in the world and a list of the nearest");
        at(10, 130);
        option("world", "Markers", true);
        option("list", "List", true);
        option("dist", "Distance", true);
        option("arrow", "Direction arrows", true);
        option("five", "Show 5 waypoints", false);
        option("death", "Death point", true);
        defaultStyle(7);
    }

    /** Przelacza zapisywanie punktu smierci (komenda .wp death). Zwraca nowy stan. */
    public static boolean toggleDeath() {
        for (Module x : ModuleManager.all()) {
            if (x instanceof WaypointsModule m) {
                boolean v = !m.option("death");
                m.options.put("death", v);
                ModuleManager.save();
                return v;
            }
        }
        return false;
    }

    /** Po smierci zapisuje waypoint "Death" w miejscu, gdzie zginales (poprzedni taki punkt jest zastepowany). */
    @Override
    public void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) {
            wasDead = false;
            return;
        }
        boolean dead = mc.player.isDead();
        if (dead && !wasDead && option("death")) {
            WaypointManager.removeByName("Death");
            WaypointManager.addHere("Death");
        }
        wasDead = dead;
    }

    private int limit() {
        return option("five") ? 5 : 3;
    }

    @Override public int w() { return 150; }

    @Override
    public int h() {
        if (!option("list")) return 12;
        MinecraftClient mc = MinecraftClient.getInstance();
        int n = mc.player == null ? 3 : Math.min(limit(), here().size());
        return Math.max(12, n * 12);
    }

    private static String fmt(double d) {
        if (d >= 1000) return String.format(Locale.ROOT, "%.1fkm", d / 1000.0);
        return Math.round(d) + "m";
    }

    /** Waypointy z biezacego serwera i wymiaru. */
    private List<WaypointManager.Waypoint> here() {
        String dim = WaypointManager.currentDim();
        List<WaypointManager.Waypoint> out = new ArrayList<>();
        for (WaypointManager.Waypoint w : WaypointManager.list()) {
            if (!WaypointManager.onThisServer(w)) continue;
            if (w.dim == null || w.dim.isEmpty() || w.dim.equals(dim)) out.add(w);
        }
        return out;
    }

    private static double dist(MinecraftClient mc, WaypointManager.Waypoint w) {
        double dx = mc.player.getX() - (w.x + 0.5), dy = mc.player.getY() - (w.y + 0.5), dz = mc.player.getZ() - (w.z + 0.5);
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /** Strzalka kierunku do waypointu wzgledem tego, w ktora strone patrzysz. */
    private static String arrowTo(MinecraftClient mc, WaypointManager.Waypoint w) {
        double dx = (w.x + 0.5) - mc.player.getX(), dz = (w.z + 0.5) - mc.player.getZ();
        double target = Math.toDegrees(Math.atan2(-dx, dz));
        double diff = (((target - mc.player.getYaw()) % 360.0) + 540.0) % 360.0 - 180.0;
        int idx = (int) Math.round(diff / 45.0);
        return ARROWS[((idx % 8) + 8) % 8];
    }

    private void line(DrawContext ctx, MinecraftClient mc, int y, int color, String text, String arrow) {
        ctx.fill(0, y + 2, 5, y + 7, 0xFF000000);
        ctx.fill(1, y + 3, 4, y + 6, 0xFF000000 | color);
        ctx.drawTextWithShadow(mc.textRenderer, mc.textRenderer.trimToWidth(text, w() - 24), 9, y, 0xFF000000 | color);
        if (arrow != null) {
            ctx.drawTextWithShadow(mc.textRenderer, arrow, w() - 10, y, 0xFFFFFFFF);
        }
    }

    /** HUD: lista najblizszych waypointow (nazwa, odleglosc, strzalka kierunku). */
    @Override
    protected void draw(DrawContext ctx, boolean preview) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!option("list")) return;
        boolean arrows = option("arrow");
        if (preview) {
            line(ctx, mc, 0, 0xFFD60A, L.t("Home: 120m"), arrows ? ARROWS[0] : null);
            line(ctx, mc, 12, 0x30D158, L.t("Base: 340m"), arrows ? ARROWS[2] : null);
            line(ctx, mc, 24, 0x64D2FF, "Farm: 512m", arrows ? ARROWS[5] : null);
            return;
        }
        if (mc.player == null) return;
        List<WaypointManager.Waypoint> pts = here();
        pts.sort(Comparator.comparingDouble(w -> dist(mc, w)));
        int y = 0;
        for (int i = 0; i < Math.min(limit(), pts.size()); i++) {
            WaypointManager.Waypoint wp = pts.get(i);
            String t = option("dist") ? wp.name + ": " + fmt(dist(mc, wp)) : wp.name;
            line(ctx, mc, y, wp.color, t, arrows ? arrowTo(mc, wp) : null);
            y += 12;
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
            String label = wm.option("dist") ? w.name + " " + fmt(dist(mc, w)) : w.name;
            int tw = mc.textRenderer.getWidth(label);
            // Stala wielkosc na ekranie niezaleznie od odleglosci; rozmiar = skala widgetu w edytorze HUD.
            var mx = ctx.getMatrices();
            mx.pushMatrix();
            mx.translate((float) px, (float) py);
            mx.scale(wm.scale, wm.scale);
            ctx.fill(-3, -3, 3, 3, 0xFF000000);
            ctx.fill(-2, -2, 2, 2, col);
            ctx.drawTextWithShadow(mc.textRenderer, label, -tw / 2, -14, col);
            mx.popMatrix();
        }
    }
}
