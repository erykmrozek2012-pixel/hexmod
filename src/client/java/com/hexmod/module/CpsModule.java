package com.hexmod.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.Deque;

/** Kliki na sekunde (lewy | prawy przycisk myszy). */
public class CpsModule extends Module {
    private final Deque<Long> left = new ArrayDeque<>();
    private final Deque<Long> right = new ArrayDeque<>();
    private boolean prevLeft, prevRight;

    public CpsModule() {
        super("cps", "CPS", "Kliki na sekunde");
        at(120, 10);
    }

    @Override public int w() { return 70; }

    @Override
    protected void draw(DrawContext ctx, boolean preview) {
        MinecraftClient mc = MinecraftClient.getInstance();
        long win = mc.getWindow().getHandle();
        long now = System.currentTimeMillis();
        boolean l = GLFW.glfwGetMouseButton(win, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean r = GLFW.glfwGetMouseButton(win, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        if (l && !prevLeft) left.addLast(now);
        if (r && !prevRight) right.addLast(now);
        prevLeft = l;
        prevRight = r;
        while (!left.isEmpty() && now - left.peekFirst() > 1000) left.pollFirst();
        while (!right.isEmpty() && now - right.peekFirst() > 1000) right.pollFirst();
        ctx.drawTextWithShadow(mc.textRenderer, "CPS: " + left.size() + " | " + right.size(), 0, 0, rgb());
    }
}
