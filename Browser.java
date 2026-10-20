package com.hexmod.module;

import java.io.IOException;

/** Otwieranie linku w domyslnej przegladarce (bez zaleznosci od API Minecrafta). */
public final class Browser {
    private Browser() {}

    public static boolean open(String url) {
        try {
            String os = System.getProperty("os.name", "").toLowerCase();
            ProcessBuilder pb;
            if (os.contains("win")) {
                pb = new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", url);
            } else if (os.contains("mac")) {
                pb = new ProcessBuilder("open", url);
            } else {
                pb = new ProcessBuilder("xdg-open", url);
            }
            pb.start();
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
