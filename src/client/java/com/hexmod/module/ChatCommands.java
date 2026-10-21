package com.hexmod.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

/**
 * Komendy wpisywane na czacie (nie sa wysylane na serwer):
 * .hexoff - ukryj dodatkowe widgety,  .hexon - pokaz je,
 * .hexoff nazwa - wylacz modul,  .hexon nazwa - wlacz modul.
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
        if (Remote.isLocked()) return false;
        if (trimmed.toLowerCase().startsWith(".wp")) return waypoint(trimmed);
        if (trimmed.toLowerCase().startsWith(".lock")) return lock(trimmed);
        if (!trimmed.toLowerCase().startsWith(".hex")) return false;
        String[] parts = trimmed.split("\\s+");
        String cmd = parts[0].toLowerCase();
        boolean on = cmd.equals(".hexon");
        boolean off = cmd.equals(".hexoff");
        if (!on && !off) {
            if (cmd.equals(".hex")) {
                say(".hexoff / .hexon - ukryj / pokaz dodatkowe widgety. "
                        + ".hexoff <modul> / .hexon <modul> - wylacz / wlacz modul.");
                return true;
            }
            return false;
        }
        if (parts.length == 1) {
            Module.extrasHidden = off;
            say(off ? "Dodatkowe widgety ukryte." : "Dodatkowe widgety widoczne.");
            return true;
        }
        String q = parts[1].toLowerCase();
        for (Module m : ModuleManager.all()) {
            if (m.id.equalsIgnoreCase(q) || m.name.toLowerCase().replace(' ', '_').equals(q)) {
                m.setEnabled(on);
                say(m.name + (on ? ": wlaczony." : ": wylaczony."));
                return true;
            }
        }
        say("Nie znaleziono modulu: " + parts[1]);
        return true;
    }

    /** .wp add [nazwa], .wp del nazwa, .wp list */
    private static boolean waypoint(String msg) {
        String[] p = msg.trim().split("\\s+", 3);
        if (!p[0].equalsIgnoreCase(".wp")) return false;
        String sub = p.length > 1 ? p[1].toLowerCase() : "";
        String arg = p.length > 2 ? p[2] : "";
        switch (sub) {
            case "add" -> say(WaypointManager.addHere(arg) ? "Dodano waypoint." : "Nie mozna dodac waypointu.");
            case "del" -> say(WaypointManager.removeByName(arg.trim()) ? "Usunieto waypoint." : "Nie ma takiego waypointu.");
            case "list" -> {
                if (WaypointManager.list().isEmpty()) say("Brak waypointow.");
                for (WaypointManager.Waypoint w : WaypointManager.list()) say(w.name + ": " + w.x + " " + w.y + " " + w.z);
            }
            default -> say(".wp add [nazwa] / .wp del nazwa / .wp list");
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
            say(ids.isEmpty() ? "Brak zablokowanych przedmiotow (poza totemem i gapple z opcji)." : "Zablokowane: " + String.join(", ", ids));
            return true;
        }
        if (arg.equalsIgnoreCase("clear")) {
            ItemLockManager.clear();
            say("Wyczyszczono liste zablokowanych przedmiotow.");
            return true;
        }
        String id;
        if (arg.isEmpty()) {
            if (mc.player == null || mc.player.getMainHandStack().isEmpty()) {
                say("Trzymaj przedmiot w rece albo wpisz: .lock nazwa (np. .lock ender_pearl).");
                return true;
            }
            id = ItemLockManager.idOf(mc.player.getMainHandStack());
        } else {
            id = ItemLockManager.resolve(arg);
            if (id == null) {
                say("Nie ma takiego przedmiotu: " + arg);
                return true;
            }
        }
        say((ItemLockManager.toggleId(id) ? "Zablokowano: " : "Odblokowano: ") + id);
        return true;
    }
}
