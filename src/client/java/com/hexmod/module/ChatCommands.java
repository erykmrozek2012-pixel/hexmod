package com.hexmod.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

/**
 * Komendy wpisywane na czacie (nie sa wysylane na serwer):
 * .hexoff - chowa wszystko, co moze byc zakazane przy sprawdzaniu (zostaja tylko bezpieczne widgety),
 * .hexon - przywraca. Dzialaja zawsze, niezaleznie od tego, ktore moduly sa wlaczone.
 */
public final class ChatCommands {
    private ChatCommands() {}

    private static void say(String s) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) mc.player.sendMessage(Text.literal("[Hexmod] " + s), false);
    }

    /** Zwraca true, gdy wiadomosc zostala obsluzona i nie ma isc na serwer. */
    public static boolean handle(String msg) {
        if (msg == null) return false;
        String trimmed = msg.trim();
        String low = trimmed.toLowerCase();
        String first = low.split("\\s+")[0];
        // .hexoff / .hexon maja pierwszenstwo nad wszystkim (takze gdy dostep jest zablokowany) i nigdy nie ida na serwer.
        if (first.equals(".hexoff") || first.equals(".hexon")) {
            boolean off = first.equals(".hexoff");
            Module.extrasHidden = off;
            say(off ? L.t("Extra widgets hidden.") : L.t("Extra widgets visible."));
            return true;
        }
        if (first.equals(".hex")) return true;
        if (Remote.isLocked()) return false;
        // Po .hexoff dodatkowe komendy sa ukryte: nic nie robia i nie wysylaja sie na serwer.
        if (Module.extrasHidden && (low.startsWith(".wp") || low.startsWith(".lock") || low.startsWith(".kd"))) return true;
        if (first.equals(".lyrics")) {
            String[] p = trimmed.split("\\s+");
            if (p.length > 1) {
                try {
                    SpotifyClient.setLyricOffsetMs(Math.round(Double.parseDouble(p[1].replace(',', '.')) * 1000.0));
                } catch (NumberFormatException e) {
                    say(L.t("Usage: .lyrics <seconds>, e.g. .lyrics 0.5"));
                    return true;
                }
            }
            say(L.t("Lyrics offset: ") + String.format(java.util.Locale.ROOT, "%+.1fs", SpotifyClient.lyricOffsetMs() / 1000.0));
            return true;
        }
        if (low.startsWith(".kd")) {
            KillStatsModule.reset();
            say(L.t("K/D counter reset."));
            return true;
        }
        if (low.startsWith(".wp")) return waypoint(trimmed);
        if (low.startsWith(".lock")) return lock(trimmed);
        return false;
    }

    /** .wp add [nazwa], .wp del nazwa, .wp list */
    private static boolean waypoint(String msg) {
        String[] p = msg.trim().split("\\s+", 3);
        if (!p[0].equalsIgnoreCase(".wp")) return false;
        String sub = p.length > 1 ? p[1].toLowerCase() : "";
        String arg = p.length > 2 ? p[2] : "";
        switch (sub) {
            case "add" -> say(WaypointManager.addHere(arg) ? L.t("Waypoint added.") : L.t("Cannot add waypoint."));
            case "del" -> say(WaypointManager.removeByName(arg.trim()) ? L.t("Waypoint removed.") : L.t("No such waypoint."));
            case "list" -> {
                if (WaypointManager.forServer().isEmpty()) say(L.t("No waypoints."));
                for (WaypointManager.Waypoint w : WaypointManager.forServer()) say(w.name + ": " + w.x + " " + w.y + " " + w.z);
            }
            case "death" -> say(WaypointsModule.toggleDeath()
                    ? L.t("Death point: ON (a Death waypoint after every death).") : L.t("Death point: off."));
            default -> say(L.t(".wp add [name] / .wp del name / .wp list / .wp death"));
        }
        return true;
    }

    /** .lock (przedmiot w rece), .lock nazwa (np. ender_pearl), .lock list, .lock clear */
    private static boolean lock(String msg) {
        String[] p = msg.trim().split("\\s+", 2);
        if (!p[0].equalsIgnoreCase(".lock")) return false;
        MinecraftClient mc = MinecraftClient.getInstance();
        String arg = p.length > 1 ? p[1].trim() : "";
        if (arg.equalsIgnoreCase("list")) {
            var ids = ItemLockManager.list();
            say(ids.isEmpty() ? L.t("No locked items (besides the totem and gapple from the options).") : L.t("Locked: ") + String.join(", ", ids));
            return true;
        }
        if (arg.equalsIgnoreCase("offhand")) {
            say(ItemLockModule.toggleOffhandLock()
                    ? L.t("Offhand: LOCKED (F and inventory swaps are blocked).")
                    : L.t("Offhand: unlocked, you can change the item."));
            return true;
        }
        if (arg.equalsIgnoreCase("clear")) {
            ItemLockManager.clear();
            say(L.t("Locked items list cleared."));
            return true;
        }
        String id;
        if (arg.isEmpty()) {
            if (mc.player == null || mc.player.getMainHandStack().isEmpty()) {
                say(L.t("Hold an item or type: .lock name (e.g. .lock ender_pearl)."));
                return true;
            }
            id = ItemLockManager.idOf(mc.player.getMainHandStack());
        } else {
            id = ItemLockManager.resolve(arg);
            if (id == null) {
                say(L.t("No such item: ") + arg);
                return true;
            }
        }
        say((ItemLockManager.toggleId(id) ? L.t("Locked: ") : L.t("Unlocked: ")) + id);
        return true;
    }
}
