package yesman.epicfight.mixin.common;

import net.minecraft.world.entity.monster.EnderMan;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yesman.epicfight.api.event.impl.VanillaEntityEventHooks;

/// Mixin for [EnderMan] that intercepts [teleport] to fire [VanillaEntityEventHooks.onEndermanTeleports].
///
/// This replaces NeoForge's [EntityTeleportEvent.EnderEntity].
@Mixin(value = EnderMan.class)
public abstract class MixinEnderManTeleport {
    @Inject(
        at = @At(value = "HEAD"),
        method = "teleport()Z",
        cancellable = true
    )
    private void epicfight$teleport(CallbackInfoReturnable<Boolean> info) {
        EnderMan self = (EnderMan)(Object)this;
        if (VanillaEntityEventHooks.onEndermanTeleports(self)) {
            info.setReturnValue(false);
            info.cancel();
        }
    }
}
