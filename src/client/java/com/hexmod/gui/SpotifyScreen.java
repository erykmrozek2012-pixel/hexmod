package com.hexmod.gui;

import com.hexmod.module.SpotifyClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

/** Logowanie do Spotify: Client ID, link logowania i wklejenie adresu po zalogowaniu. */
public class SpotifyScreen extends Screen {
    private final Screen parent;
    private TextFieldWidget idField;

    public SpotifyScreen(Screen parent) {
        super(Text.literal("Spotify"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int cx = width / 2;

        idField = new TextFieldWidget(textRenderer, cx - 140, 50, 200, 20, Text.literal("Client ID"));
        idField.setPlaceholder(Text.literal("Spotify Client ID"));
        idField.setMaxLength(64);
        idField.setText(SpotifyClient.clientId());
        addDrawableChild(idField);
        addDrawableChild(ButtonWidget.builder(Text.literal("Zapisz"), b -> {
            SpotifyClient.setClientId(idField.getText());
            SpotifyClient.status = "Zapisano Client ID.";
        }).dimensions(cx + 66, 50, 74, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("1. Kopiuj link logowania"), b -> {
            SpotifyClient.setClientId(idField.getText());
            String url = SpotifyClient.loginUrl();
            if (url == null) {
                SpotifyClient.status = "Najpierw wpisz Client ID.";
            } else {
                client.keyboard.setClipboard(url);
                SpotifyClient.status = "Skopiowano. Wklej link w przegladarce i zaloguj sie.";
            }
        }).dimensions(cx - 140, 82, 280, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("2. Wklej adres po zalogowaniu"),
                b -> SpotifyClient.completeLogin(client.keyboard.getClipboard()))
                .dimensions(cx - 140, 108, 280, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Wyloguj"), b -> SpotifyClient.logout())
                .dimensions(cx - 140, 134, 280, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Wstecz"), b -> client.setScreen(parent))
                .dimensions(cx - 50, height - 30, 100, 20).build());
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, width, height, HexTheme.OVERLAY);
        ctx.fill(width / 2 - 150, 8, width / 2 + 150, height - 4, HexTheme.PANEL);
        ctx.fill(width / 2 - 150, 8, width / 2 + 150, 11, HexTheme.RED);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        int cx = width / 2;
        ctx.drawTextWithShadow(textRenderer, "SPOTIFY", cx - 140, 22, HexTheme.RED);
        ctx.drawTextWithShadow(textRenderer,
                SpotifyClient.isLoggedIn() ? "Zalogowano" : "Niezalogowano", cx - 140, 36,
                SpotifyClient.isLoggedIn() ? HexTheme.TEXT : HexTheme.TEXT_DIM);

        int y = 164;
        String[] help = {
                "Po zalogowaniu przegladarka pokaze blad strony.",
                "To normalne. Skopiuj caly adres z paska",
                "przegladarki i kliknij przycisk 2."
        };
        for (String line : help) {
            ctx.drawTextWithShadow(textRenderer, line, cx - 140, y, HexTheme.TEXT_DIM);
            y += 11;
        }
        if (!SpotifyClient.status.isEmpty()) {
            ctx.drawCenteredTextWithShadow(textRenderer, SpotifyClient.status, cx, height - 46, HexTheme.TEXT);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
