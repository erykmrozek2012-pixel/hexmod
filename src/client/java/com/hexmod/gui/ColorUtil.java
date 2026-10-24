package com.hexmod.gui;

/** Przeliczanie kolorow HSV <-> RGB dla wyboru koloru w edytorze HUD. */
final class ColorUtil {
    private ColorUtil() {}

    /** h, s, v w zakresie 0..1; zwraca 0xRRGGBB. */
    static int hsv(float h, float s, float v) {
        h = h - (float) Math.floor(h);
        float c = v * s;
        float hp = h * 6f;
        float x = c * (1f - Math.abs(hp % 2f - 1f));
        float r = 0, g = 0, b = 0;
        switch ((int) hp) {
            case 0 -> { r = c; g = x; }
            case 1 -> { r = x; g = c; }
            case 2 -> { g = c; b = x; }
            case 3 -> { g = x; b = c; }
            case 4 -> { r = x; b = c; }
            default -> { r = c; b = x; }
        }
        float m = v - c;
        int ri = Math.round((r + m) * 255f), gi = Math.round((g + m) * 255f), bi = Math.round((b + m) * 255f);
        return (ri << 16) | (gi << 8) | bi;
    }

    /** Zwraca {h, s, v} (0..1) dla koloru 0xRRGGBB. */
    static float[] toHsv(int rgb) {
        float r = ((rgb >> 16) & 255) / 255f, g = ((rgb >> 8) & 255) / 255f, b = (rgb & 255) / 255f;
        float max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b));
        float d = max - min;
        float h = 0f;
        if (d > 0f) {
            float hh;
            if (max == r) hh = ((g - b) / d) % 6f;
            else if (max == g) hh = (b - r) / d + 2f;
            else hh = (r - g) / d + 4f;
            h = hh / 6f;
            if (h < 0f) h += 1f;
        }
        return new float[]{h, max == 0f ? 0f : d / max, max};
    }
}
