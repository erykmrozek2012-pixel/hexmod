package com.hexmod.mixin;

import com.hexmod.module.ItemLockModule;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.slot.SlotActionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Pozwala Item lockowi anulowac klikniecia slotow (przenoszenie, wyrzucanie, zamiana) w oknie ekwipunku. */
@Mixin(ClientPlayerInteractionManager.class)
public class ClientPlayerInteractionManagerMixin {
    @Inject(method = "clickSlot", at = @At("HEAD"), cancellable = true)
    private void hexmod$itemLock(int syncId, int slotId, int button, SlotActionType actionType,
                                 PlayerEntity player, CallbackInfo ci) {
        if (ItemLockModule.blocksClick(syncId, slotId, button, actionType, player)) {
            ci.cancel();
        }
    }
}
