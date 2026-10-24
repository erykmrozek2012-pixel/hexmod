package com.hexmod;

import com.hexmod.gui.HexmodScreen;
import com.hexmod.module.AlliesManager;
import com.hexmod.module.ChatCommands;
import com.hexmod.module.MacroManager;
import com.hexmod.module.CombatTracker;
import com.hexmod.module.ItemLockManager;
import com.hexmod.module.KillStatsModule;
import com.hexmod.module.L;
import com.hexmod.module.ItemLockModule;
import com.hexmod.module.WaypointManager;
import com.hexmod.gui.SpotifyControls;
import com.hexmod.module.AppleCountModule;
import com.hexmod.module.ComboModule;
import com.hexmod.module.ReachModule;
import com.hexmod.module.WaypointsModule;
import com.hexmod.module.DamageDealtModule;
import com.hexmod.module.Module;
import com.hexmod.module.ModuleManager;
import com.hexmod.module.Remote;
import com.hexmod.module.ShieldBreakModule;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class HexmodClient implements ClientModInitializer {
    public static final String MOD_ID = "hexmod";
    private static final KeyBinding.Category CATEGORY = KeyBinding.Category.create(Identifier.of(MOD_ID, "main"));

    private static KeyBinding openMenuKey;
    private static int refreshTicks = 0;

    @Override
    public void onInitializeClient() {
        L.load();
        SpotifyControls.register();
        ModuleManager.init();
        ModuleManager.load();
        AlliesManager.load();
        MacroManager.load();
        WaypointManager.load();
        ItemLockManager.load();
        Remote.refresh();

        openMenuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.hexmod.open_menu",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                CATEGORY
        ));

        ClientTickEvents.START_CLIENT_TICK.register(ItemLockModule::beforeInput);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openMenuKey.wasPressed()) {
                client.setScreen(new HexmodScreen());
            }
            if (++refreshTicks % 6000 == 0) Remote.refresh();
            if (Remote.isLocked()) return;
            MacroManager.tick();
            CombatTracker.tick();
            for (Module m : ModuleManager.all()) {
                if (m.isEnabled()) m.tick();
            }
        });

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient() && !Remote.isLocked()) {
                CombatTracker.markCombat();
                if (entity instanceof PlayerEntity victim) {
                    DamageDealtModule.onAttack(victim);
                    ComboModule.onAttack();
                    KillStatsModule.onAttack(victim);
                    ReachModule.onAttack(player, entity);
                }
                if (entity instanceof PlayerEntity target
                        && target.isBlocking()
                        && player.getStackInHand(hand).isIn(ItemTags.AXES)) {
                    ShieldBreakModule.onEnemyShieldHit(target);
                }
            }
            return ActionResult.PASS;
        });

        ClientSendMessageEvents.ALLOW_CHAT.register(message -> !ChatCommands.handle(message));

        HudElementRegistry.attachElementAfter(
                VanillaHudElements.MISC_OVERLAYS,
                Identifier.of(MOD_ID, "widgets"),
                (ctx, tickCounter) -> {
                    for (Module m : ModuleManager.all()) {
                        if (m.isEnabled()) m.render(ctx);
                    }
                    WaypointsModule.renderMarkers(ctx);
                    AppleCountModule.renderMarkers(ctx);
                });
    }
}
