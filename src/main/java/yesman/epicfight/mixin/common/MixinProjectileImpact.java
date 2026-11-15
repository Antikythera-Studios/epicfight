package yesman.epicfight.mixin.common;

import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yesman.epicfight.api.event.impl.VanillaEntityEventHooks;

/// Mixin for [Projectile] that intercepts [onHit] to fire [VanillaEntityEventHooks.onProjectileImpacts].
///
/// This replaces NeoForge's [ProjectileImpactEvent].
@Mixin(value = Projectile.class)
public abstract class MixinProjectileImpact {
    @Inject(
        at = @At(value = "HEAD"),
        method = "onHit(Lnet/minecraft/world/phys/HitResult;)V",
        cancellable = true
    )
    private void epicfight$onHit(HitResult hitResult, CallbackInfo info) {
        Projectile self = (Projectile)(Object)this;
        if (VanillaEntityEventHooks.onProjectileImpacts(hitResult, self)) {
            info.cancel();
        }
    }
}
