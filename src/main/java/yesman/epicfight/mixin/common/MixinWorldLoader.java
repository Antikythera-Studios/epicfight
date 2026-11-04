package yesman.epicfight.mixin.common;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.server.WorldLoader;
import yesman.epicfight.registry.DeferredInitPhase;

/// Fires the NeoForge-equivalent "all constructors done" phase at the head of
/// the first datapack load — every Fabric entrypoint has run by then, so all
/// animation/skill providers are registered before addons resolve them.
@Mixin(WorldLoader.class)
public abstract class MixinWorldLoader {
    @Inject(method = "load", at = @At("HEAD"))
    private static void epicfight$deferredInit(CallbackInfoReturnable<?> ci) {
        DeferredInitPhase.run();
    }
}
