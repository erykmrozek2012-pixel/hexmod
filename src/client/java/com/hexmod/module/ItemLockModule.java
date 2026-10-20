package com.hexmod.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;

/**
 * Item lock: blokuje wyrzucenie (Q / Ctrl+Q) i zamiane z druga reka (F) dla zablokowanych slotow paska
 * oraz dla wybranych przedmiotow (totem, zlote jablko). Dziala w grze, nie w oknie ekwipunku.
 */
public class ItemLockModule extends Module {
    private static long lastNotice = 0;

    public ItemLockModule() {
        super("item_lock", "Item lock", "Blokada wyrzucenia i zamiany zablokowanych slotow/przedmiotow");
        at(10, 190);
        for (int i = 1; i <= 9; i++) option("slot" + i, "Slot " + i, false);
        option("totem", "Totem", true);
        option("gapple", "Gapple", true);
        option("offhand", "Druga reka", true);
    }

    @Override public int w() { return 90; }

    private static boolean stackLocked(ItemLockModule m, ItemStack s) {
        if (s == null || s.isEmpty()) return false;
        if (m.option("totem") && s.isOf(Items.TOTEM_OF_UNDYING)) return true;
        return m.option("gapple") && (s.isOf(Items.GOLDEN_APPLE) || s.isOf(Items.ENCHANTED_GOLDEN_APPLE));
    }

    private static ItemLockModule instance() {
        for (Module x : ModuleManager.all()) if (x instanceof ItemLockModule m) return m;
        return null;
    }

    @Override
    protected void draw(DrawContext ctx, boolean preview) {
        MinecraftClient mc = MinecraftClient.getInstance();
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 9; i++) if (option("slot" + i)) sb.append(i).append(' ');
        String t = sb.length() == 0 ? "Lock: tylko itemy" : "Lock: " + sb.toString().trim();
        ctx.drawTextWithShadow(mc.textRenderer, t, 0, 0, rgb());
    }

    /** Wolane na poczatku kazdego ticka, zanim gra obsluzy klawisze: zjada zablokowane nacisniecia. */
    public static void beforeInput(MinecraftClient mc) {
        if (mc.player == null || Remote.isLocked()) return;
        ItemLockModule m = instance();
        if (m == null || !m.isEnabled()) return;
        if (mc.currentScreen != null) return;

        int slot = mc.player.getInventory().getSelectedSlot();
        ItemStack held = mc.player.getMainHandStack();
        boolean lockedHeld = m.option("slot" + (slot + 1)) || stackLocked(m, held);
        if (lockedHeld && !held.isEmpty()) {
            boolean blocked = false;
            while (mc.options.dropKey.wasPressed()) blocked = true;
            if (blocked) notice(mc, "Item lock: nie mozna wyrzucic.");
        }
        boolean lockedSwap = (m.option("offhand") && !mc.player.getOffHandStack().isEmpty() && stackLocked(m, mc.player.getOffHandStack()))
                || lockedHeld;
        if (lockedSwap) {
            boolean blocked = false;
            while (mc.options.swapHandsKey.wasPressed()) blocked = true;
            if (blocked) notice(mc, "Item lock: zamiana zablokowana.");
        }
    }

    private static void notice(MinecraftClient mc, String msg) {
        long now = System.currentTimeMillis();
        if (now - lastNotice < 800) return;
        lastNotice = now;
        mc.player.sendMessage(Text.literal(msg), true);
    }
}
