package yesman.epicfight.compat.spatialgui.mixin;

import net.minecraft.client.CameraType;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;
import yesman.epicfight.compat.spatialgui.SpatialGUICompat;

/// Skips the auto third-person switch when entering Epic Fight mode while a
/// Spatial GUI screen is being rendered in world space.
@Mixin(value = LocalPlayerPatch.class, remap = false)
public abstract class MixinLocalPlayerPatch {
    @Redirect(
            method = "toEpicFightMode(Z)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/Options;setCameraType(Lnet/minecraft/client/CameraType;)V",
                    remap = true
            )
    )
    private void epicfight$skipTpsSwitchWhileSpatialGui(Options options, CameraType cameraType) {
        if (SpatialGUICompat.spatialActive()) {
            return;
        }
        options.setCameraType(cameraType);
    }
}
