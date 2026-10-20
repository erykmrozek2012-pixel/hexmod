package com.hexmod.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;

/**
 * Item lock: blokuje wyrzucenie (Q / Ctrl+Q) i zamiane z druga reka (F) w grze ORAZ przenoszenie,
 * wyrzucanie i zamiane zablokowanych przedmiotow w oknie ekwipunku (przez mixin na klikniecia slotow).
 * Zablokowac mozna: sloty paska 1-9, totem, gapple oraz dowolny przedmiot komenda .lock.
 */
public class ItemLockModule extends Module {
    private static long lastNotice = 0;

    public ItemLockModule() {
        super("item_lock", "Item lock", "Blokada wyrzucenia i przenoszenia zablokowanych slotow/przedmiotow");
        at(10, 190);
        for (int i = 1; i <= 9; i++) option("slot" + i, "Slot " + i, false);
        option("totem", "Totem", true);
        option("gapple", "Gapple", true);
        option("offhand", "Druga reka", true);
    }

    @Override public int w() { return 100; }

    private static boolean stackLocked(ItemLockModule m, ItemStack s) {
        if (s == null || s.isEmpty()) return false;
        if (m.option("totem") && s.isOf(Items.TOTEM_OF_UNDYING)) return true;
        if (m.option("gapple") && (s.isOf(Items.GOLDEN_APPLE) || s.isOf(Items.ENCHANTED_GOLDEN_APPLE))) return true;
        return ItemLockManager.has(s);
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
        int extra = ItemLockManager.list().size();
        String t = sb.length() == 0 ? "Lock: itemy" : "Lock: " + sb.toString().trim();
        if (extra > 0) t += " +" + extra;
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

    /** Czy slot nalezy do Twojego ekwipunku i jest zamrozonym slotem paska (opcje Slot 1-9). */
    private static boolean slotFrozen(ItemLockModule m, PlayerEntity player, Slot slot) {
        if (slot.inventory != player.getInventory()) return false;
        int idx = slot.getIndex();
        return idx >= 0 && idx < 9 && m.option("slot" + (idx + 1));
    }

    private static boolean ownSlot(PlayerEntity player, Slot slot) {
        return slot.inventory == player.getInventory();
    }

    /**
     * Wolane z mixina przy kazdym kliknieciu slotu w oknie. Zwraca true, gdy klikniecie trzeba anulowac.
     * Chroni tylko Twoj ekwipunek: z skrzyni mozesz normalnie wyjac zablokowany przedmiot.
     */
    public static boolean blocksClick(int syncId, int slotId, int button, SlotActionType type, PlayerEntity player) {
        if (player == null || Remote.isLocked()) return false;
        ItemLockModule m = instance();
        if (m == null || !m.isEnabled()) return false;
        ScreenHandler h = player.currentScreenHandler;
        if (h == null || h.syncId != syncId) return false;
        if (type != SlotActionType.PICKUP && type != SlotActionType.QUICK_MOVE && type != SlotActionType.SWAP
                && type != SlotActionType.THROW && type != SlotActionType.PICKUP_ALL) {
            return false;
        }

        // Klikniecie poza oknem z zablokowanym przedmiotem na kursorze = wyrzucenie.
        if (slotId < 0 || slotId >= h.slots.size()) {
            boolean drop = slotId == -999 && (type == SlotActionType.PICKUP || type == SlotActionType.THROW);
            if (drop && stackLocked(m, h.getCursorStack())) return deny("Item lock: nie mozna wyrzucic.");
            return false;
        }

        Slot slot = h.slots.get(slotId);
        if (type == SlotActionType.PICKUP_ALL) {
            return stackLocked(m, h.getCursorStack()) && deny("Item lock: zablokowany przedmiot.");
        }

        if (ownSlot(player, slot)) {
            if (slotFrozen(m, player, slot)) return deny("Item lock: ten slot jest zablokowany.");
            if (stackLocked(m, slot.getStack())) return deny("Item lock: ten przedmiot jest zablokowany.");
        }

        // Klawisze 1-9 / F nad slotem: zamiana z paskiem. Sprawdzamy tez slot docelowy.
        if (type == SlotActionType.SWAP) {
            ItemStack target = ItemStack.EMPTY;
            boolean frozen = false;
            if (button >= 0 && button < 9) {
                target = player.getInventory().getStack(button);
                frozen = m.option("slot" + (button + 1));
            } else if (button == 40) {
                target = player.getOffHandStack();
            }
            if (frozen || stackLocked(m, target)) return deny("Item lock: zamiana zablokowana.");
        }
        return false;
    }

    private static boolean deny(String msg) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) notice(mc, msg);
        return true;
    }

    private static void notice(MinecraftClient mc, String msg) {
        long now = System.currentTimeMillis();
        if (now - lastNotice < 800) return;
        lastNotice = now;
        mc.player.sendMessage(Text.literal(msg), true);
    }
}
