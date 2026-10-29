package com.hexmod.gui;

/** Wspolna tabela 64 kolorow (8x8) uzywana w oknie wyboru koloru. */
final class ColorTable {
    private ColorTable() {}

    static final int[] ALL = build();

    private static int[] build() {
        int[] base = {
                0xD10A0A, 0xFF3B30, 0xFF6A00, 0xFF9500, 0xFFD60A, 0xFFE680, 0xBFFF00, 0x30D158,
                0x00A86B, 0x00C7BE, 0x64D2FF, 0x32ADE6, 0x0A84FF, 0x5E5CE6, 0xBF5AF2, 0xFF6EB4,
                0xFFFFFF, 0xE5E5EA, 0xC7C7CC, 0x8E8E93, 0x636366, 0x3A3A3C, 0x1C1C1E, 0x000000,
                0x8B5A2B, 0xC8A165, 0xA2845E, 0x7A0505, 0x1E6B3A, 0x2C3E6B, 0x6B2C5E, 0x40E0D0};
        int[] t = new int[64];
        System.arraycopy(base, 0, t, 0, 32);
        float[] hues = {0f, 30f, 55f, 120f, 170f, 210f, 270f, 320f};
        float[][] sv = {{0.35f, 1f}, {0.6f, 0.55f}, {1f, 0.7f}, {0.5f, 0.8f}};
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 8; c++) {
                t[32 + r * 8 + c] = ColorUtil.hsv(hues[c] / 360f, sv[r][0], sv[r][1]);
            }
        }
        return t;
    }
}
