package com.hexmod.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.Vec3d;

/**
 * Licznik refow (miotanych mikstur): nad wrogiem pokazuje, ile miotanych mikstur trzyma w rece
 * (z ikona mikstury), a na HUD liczy mikstury w Twoim ekwipunku.
 */
public class RefCounterModule extends Module {
    public RefCounterModule() {
        super("refs", "Licznik refow", "Ile miotanych mikstur (refow) ma wrog w rece i ile masz Ty");
        at(10, 160);
        option("enemy", "Nad wrogiem", true);
        option("own", "Moje refy", true);
    }

    @Override public int w() { return 80; }

    private static boolean isRef(ItemStack s) {
        return s != null && !s.isEmpty() && s.isOf(Items.SPLASH_POTION);
    }

    private static int countOwn(MinecraftClient mc) {
        int total = 0;
        var inv = mc.player.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            ItemStack s = inv.getStack(i);
            if (isRef(s)) total += s.getCount();
        }
        return total;
    }

    /** Widget na HUD: ile refow masz w ekwipunku. */
    @Override
    protected void draw(DrawContext ctx, boolean preview) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!option("own")) return;
        int n = preview || mc.player == null ? 12 : countOwn(mc);
        ctx.drawItem(Items.SPLASH_POTION.getDefaultStack(), 0, -4);
        ctx.drawTextWithShadow(mc.textRenderer, "x" + n, 18, 0, rgb());
    }

    /** Liczba refow nad wrogiem, rysowana osobnym elementem HUD (wolane z HexmodClient). */
    public static void renderMarkers(DrawContext ctx) {
        if (Remote.isLocked()) return;
        RefCounterModule rm = null;
        for (Module x : ModuleManager.all()) if (x instanceof RefCounterModule r) rm = r;
        if (rm == null || !rm.isEnabled() || rm.hidden || Module.extrasHidden || !rm.option("enemy")) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;
        int sw = ctx.getScaledWindowWidth(), sh = ctx.getScaledWindowHeight();
        for (PlayerEntity pl : mc.world.getPlayers()) {
            if (pl == mc.player) continue;
            if (mc.player.distanceTo(pl) > 40f) continue;
            int count = 0;
            if (isRef(pl.getMainHandStack())) count += pl.getMainHandStack().getCount();
            if (isRef(pl.getOffHandStack())) count += pl.getOffHandStack().getCount();
            if (count <= 0) continue;
            float[] p = Projection.project(ctx, new Vec3d(pl.getX(), pl.getY() + pl.getHeight() * 0.55, pl.getZ()));
            if (p == null) continue;
            int px = Math.round(p[0]), py = Math.round(p[1]);
            if (px < 4 || px > sw - 30 || py < 4 || py > sh - 20) continue;
            ctx.drawItem(Items.SPLASH_POTION.getDefaultStack(), px + 6, py - 8);
            ctx.drawTextWithShadow(mc.textRenderer, "x" + count, px + 24, py - 4, 0xFF000000 | rm.rgb());
        }
    }
}
