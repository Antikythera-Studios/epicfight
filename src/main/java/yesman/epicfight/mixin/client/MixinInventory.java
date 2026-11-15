package yesman.epicfight.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.entity.player.Inventory;
import yesman.epicfight.client.events.engine.ControlEngine;

@Mixin(Inventory.class)
public class MixinInventory {
    @Inject(
            method = "swapPaint",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onCycleHotbarSlot(double direction, CallbackInfo ci) {
        // Called whenever the player changes their selected hotbar item via the mouse wheel or other input systems.
        if (ControlEngine.isHotbarCyclingDisabled()) {
            // Object.MouseScrollingEvent is already cancelled in ControlEngine.Events#mouseScrollEvent to block hotbar cycling for mouse input.
            // Controller inputs are unaffected, so we also cancel it here to enforce the restriction
            // for all input methods.
            ci.cancel();
        }
    }
}
