package com.hexmod.gui;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

import java.util.function.Consumer;

/** Przelacznik suwakowy ON/OFF z plynna animacja galki. */
public class ToggleSwitch extends ClickableWidget {
    private static final int KNOB = 0xFFFFFFFF;
    private static final long ANIM_MS = 150;

    private boolean state;
    private long lastChange = 0;
    private final Consumer<Boolean> onChange;

    public ToggleSwitch(int x, int y, boolean initial, Consumer<Boolean> onChange) {
        super(x, y, 36, 18, Text.empty());
        this.state = initial;
        this.onChange = onChange;
    }

    public boolean isOn() {
        return state;
    }

    @Override
    public void onClick(Click click, boolean doubled) {
        state = !state;
        lastChange = Util.getMeasuringTimeMs();
        onChange.accept(state);
    }

    @Override
    protected void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
        float t = Math.min(1f, (Util.getMeasuringTimeMs() - lastChange) / (float) ANIM_MS);
        float pos = state ? t : 1f - t;
        if (lastChange == 0) pos = state ? 1f : 0f;

        int x = getX(), y = getY();
        ctx.fill(x, y, x + width, y + height, state ? HexTheme.RED : HexTheme.GRAY_OFF);

        int knob = height - 4;
        int knobX = x + 2 + Math.round(pos * (width - knob - 4));
        ctx.fill(knobX, y + 2, knobX + knob, y + 2 + knob, KNOB);
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {}
}
