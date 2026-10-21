package com.hexmod.gui;

import com.hexmod.module.L;
import com.hexmod.module.SpotifyClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

/** Spotify: dziala bez logowania (czyta aplikacje Spotify na komputerze). Logowanie jest opcjonalne. */
public class SpotifyScreen extends Screen {
    private static final int MAIN = 0, GUIDE = 1, ADVANCED = 2;

    private final Screen parent;
    private int mode = MAIN;
    private TextFieldWidget idField;

    public SpotifyScreen(Screen parent) {
        super(Text.literal("Spotify"));
        this.parent = parent;
    }

    private void tab(String label, int x, int w, int target) {
        addDrawableChild(ButtonWidget.builder(Text.literal(label), b -> { mode = target; clearAndInit(); })
                .dimensions(x, 26, w, 18).build());
    }

    @Override
    protected void init() {
        int cx = width / 2;
        tab("Status", cx - 140, 90, MAIN);
        tab(L.t("Guide"), cx - 46, 90, GUIDE);
        tab(L.t("Advanced"), cx + 48, 92, ADVANCED);

        if (mode == ADVANCED) {
            idField = new TextFieldWidget(textRenderer, cx - 140, 76, 200, 20, Text.literal("Client ID"));
            idField.setPlaceholder(Text.literal(L.t("Custom Client ID (optional)")));
            idField.setMaxLength(64);
            idField.setText(SpotifyClient.clientId());
            addDrawableChild(idField);
            addDrawableChild(ButtonWidget.builder(Text.literal(L.t("Save")), b -> {
                SpotifyClient.setClientId(idField.getText());
                SpotifyClient.status = L.t("Client ID saved.");
            }).dimensions(cx + 66, 76, 74, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal(L.t("Log in (exact sync)")),
                    b -> SpotifyClient.startAutoLogin()).dimensions(cx - 140, 102, 280, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal(L.t("Log out")), b -> SpotifyClient.logout())
                    .dimensions(cx - 140, 126, 280, 20).build());
        }

        addDrawableChild(ButtonWidget.builder(Text.literal(L.t("Back")), b -> client.setScreen(parent))
                .dimensions(cx - 50, height - 28, 100, 20).build());
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, width, height, HexTheme.OVERLAY);
        ctx.fill(width / 2 - 150, 4, width / 2 + 150, height - 2, HexTheme.PANEL);
        ctx.fill(width / 2 - 150, 4, width / 2 + 150, 7, HexTheme.RED);
    }

    private int wrapped(DrawContext ctx, String text, int x, int y, int maxW, int color) {
        for (OrderedText line : textRenderer.wrapLines(Text.literal(text), maxW)) {
            ctx.drawTextWithShadow(textRenderer, line, x, y, color);
            y += 10;
        }
        return y;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        int cx = width / 2;
        int x = cx - 140;
        ctx.drawTextWithShadow(textRenderer, "SPOTIFY", x, 12, HexTheme.RED);

        if (mode == MAIN) {
            int y = 54;
            y = wrapped(ctx, L.t("You do not need to do anything. Start the Spotify app on your computer and play music. ")
                    + L.t("The widget will show the track and lyrics by itself."), x, y, 280, HexTheme.TEXT_DIM) + 6;
            String info = SpotifyClient.localInfo.isEmpty() ? L.t("Waiting for data from the Spotify app...") : SpotifyClient.localInfo;
            ctx.drawTextWithShadow(textRenderer, L.t("Detected:"), x, y, HexTheme.TEXT);
            wrapped(ctx, info, x, y + 12, 280, HexTheme.RED);
            wrapped(ctx, L.t("Remember to enable the 'Spotify' module in the Modules tab."), x, y + 40, 280, HexTheme.TEXT_DIM);
        } else if (mode == GUIDE) {
            int y = 54;
            String[] steps = {
                    L.t("1. Start the Spotify app on your computer and play music."),
                    L.t("2. In the Hex Client menu (Right Shift), Modules tab, enable 'Spotify'."),
                    L.t("3. The track and lyrics appear on screen. No login needed."),
                    L.t("4. Set the widget position, size and color in the 'HUD Editor'."),
                    L.t("The lyrics sync from the start of the track, so after seeking they may drift. ")
                            + L.t("For exact sync, log in on the Advanced tab.")
            };
            for (String step : steps) {
                y = wrapped(ctx, step, x, y, 280, HexTheme.TEXT) + 3;
            }
        } else {
            int y = 152;
            y = wrapped(ctx, L.t("Optional: logging in to Spotify gives the exact track time and synced lyrics ")
                    + L.t("even after seeking. The Client ID is set by the admin in config.json (hexmod-data repository)."),
                    x, y, 280, HexTheme.TEXT_DIM);
        }

        if (!SpotifyClient.status.isEmpty() && mode == ADVANCED) {
            ctx.drawCenteredTextWithShadow(textRenderer, SpotifyClient.status, cx, height - 42, HexTheme.TEXT);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
