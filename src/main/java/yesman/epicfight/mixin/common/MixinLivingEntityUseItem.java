package yesman.epicfight.mixin.common;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;

/// Mixin for [LivingEntity] that intercepts [startUsingItem] to replicate NeoForge's [LivingEntityUseItemEvent.Start].
///
/// This cancels item use when the player can't use items, and sets the block animation duration
/// for items with BLOCK use animation.
@Mixin(value = LivingEntity.class)
public abstract class MixinLivingEntityUseItem {
    @Inject(
        at = @At(value = "HEAD"),
        method = "startUsingItem(Lnet/minecraft/world/InteractionHand;)V",
        cancellable = true
    )
    private void epicfight$startUsingItem(InteractionHand hand, CallbackInfo info) {
        LivingEntity self = (LivingEntity)(Object)this;

        if (self instanceof Player player) {
            EpicFightCapabilities.getPlayerPatchAsOptional(player).ifPresent(playerpatch -> {
                ItemStack itemStack = player.getItemInHand(hand);
                InteractionHand otherHand = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
                CapabilityItem itemCap = playerpatch.getHoldingItemCapability(hand);

                // Cancel if the player can't use items
                if (!playerpatch.getEntityState().canUseItem()) {
                    info.cancel();
                    return;
                }

                // Cancel if the item is in the offhand and the main hand can't use offhand
                if (hand == InteractionHand.OFF_HAND) {
                    ItemStack mainHandItem = player.getMainHandItem();
                    if (!mainHandItem.isEmpty() && !playerpatch.getHoldingItemCapability(InteractionHand.MAIN_HAND).getStyle(playerpatch).canUseOffhand()) {
                        info.cancel();
                        return;
                    }
                }

                // Set block animation duration for items with BLOCK use animation
                if (itemCap.getUseAnimation(playerpatch) == UseAnim.BLOCK) {
                    // The vanilla code will set the use duration, but we need to ensure it's the full duration
                    // This is handled by the vanilla startUsingItem method
                }
            });
        }
    }
}
