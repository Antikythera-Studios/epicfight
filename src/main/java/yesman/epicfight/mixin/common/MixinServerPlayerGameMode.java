package yesman.epicfight.mixin.common;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yesman.epicfight.api.event.impl.VanillaPlayerEventHooks;

/// Mixin for [ServerPlayerGameMode] that intercepts [useItem] to fire [VanillaPlayerEventHooks.onUseItemInServerSide].
///
/// This replaces NeoForge's [PlayerInteractEvent.RightClickItem].
@Mixin(value = ServerPlayerGameMode.class)
public abstract class MixinServerPlayerGameMode {
    @Inject(
        at = @At(value = "HEAD"),
        method = "useItem(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;",
        cancellable = true
    )
    private void epicfight$useItem(ServerPlayer player, Level level, ItemStack itemStack, InteractionHand hand, CallbackInfoReturnable<InteractionResult> info) {
        if (VanillaPlayerEventHooks.onUseItemInServerSide(player)) {
            info.setReturnValue(InteractionResult.PASS);
            info.cancel();
        }
    }
}
