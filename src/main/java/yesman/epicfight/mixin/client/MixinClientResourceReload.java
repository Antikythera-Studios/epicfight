package yesman.epicfight.mixin.client;

import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ReloadInstance;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yesman.epicfight.registry.DeferredInitPhase;

/**
 * Runs client deferred initialization at the FIRST client-resource reload:
 * {@code createReload} swaps in the new {@code MultiPackResourceManager}
 * (packs already mounted) before any parallel listener preparation begins,
 * so deferred registrations land before the animation manager's first
 * {@code prepare} instead of racing it from CLIENT_STARTED.
 */
@Mixin(ReloadableResourceManager.class)
public abstract class MixinClientResourceReload {

    @Shadow
    @Final
    private PackType type;

    @Unique
    private boolean epicfight$initialized;

    @Inject(method = "createReload", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/packs/resources/SimpleReloadInstance;"
                    + "create(Lnet/minecraft/server/packs/resources/ResourceManager;"
                    + "Ljava/util/List;Ljava/util/concurrent/Executor;"
                    + "Ljava/util/concurrent/Executor;"
                    + "Ljava/util/concurrent/CompletableFuture;Z)"
                    + "Lnet/minecraft/server/packs/resources/ReloadInstance;"))
    private void epicfight$deferredClientInit(CallbackInfoReturnable<ReloadInstance> cir) {
        if (this.type == PackType.CLIENT_RESOURCES && !this.epicfight$initialized) {
            this.epicfight$initialized = true;
            // No catch-and-ignore: a genuine init failure must surface, not
            // pretend the phase completed.
            DeferredInitPhase.run();
        }
    }
}
