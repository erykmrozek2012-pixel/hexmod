package com.hexmod.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;

/** Okresla, czy gracz jest w walce (zadal albo otrzymal obrazenia w ciagu ostatnich sekund). */
public final class CombatTracker {
    private static final long WINDOW_MS = 10_000;
    private static long lastCombat = 0;

    private CombatTracker() {}

    public static void markCombat() {
        lastCombat = System.currentTimeMillis();
    }

    public static void tick() {
        PlayerEntity p = MinecraftClient.getInstance().player;
        if (p != null && p.hurtTime > 0) markCombat();
    }

    /** Ile milisekund zostalo do konca walki (0, gdy nie ma walki). */
    public static long remainingMs() {
        return Math.max(0L, WINDOW_MS - (System.currentTimeMillis() - lastCombat));
    }

    public static boolean inCombat() {
        return System.currentTimeMillis() - lastCombat < WINDOW_MS;
    }
}
