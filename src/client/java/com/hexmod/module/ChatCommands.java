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
        if (!trimmed.toLowerCase().startsWith(".hex") || Remote.isLocked()) return false;
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
}
