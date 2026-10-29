package yesman.epicfight.mixin.client;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yesman.epicfight.api.client.event.impl.VanillaGeneralClientEventHooks;

@Mixin(value = MultiPlayerGameMode.class)
public abstract class MixinMultiPlayerGameMode {
    @Inject(
        at = @At(value = "HEAD"),
        method = "useItem(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;",
        cancellable = true
    )
    private void epicfight$useItem(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> info) {
        if (VanillaGeneralClientEventHooks.onUseItemInClientSide(player, player.getItemInHand(hand), hand)) {
            info.setReturnValue(InteractionResult.PASS);
            info.cancel();
        }
    }
}
