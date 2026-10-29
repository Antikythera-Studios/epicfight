package yesman.epicfight.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yesman.epicfight.api.client.event.EpicFightClientEventHooks;
import yesman.epicfight.api.client.event.types.render.RenderHandEvent;

@Mixin(value = ItemInHandRenderer.class)
public abstract class MixinItemInHandRenderer {
    @Inject(
        method = "renderHandsWithItems(FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/player/LocalPlayer;I)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderArmWithItem(Lnet/minecraft/client/player/AbstractClientPlayer;FFLnet/minecraft/world/InteractionHand;FLnet/minecraft/world/item/ItemStack;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            ordinal = 0
        ),
        cancellable = true
    )
    private void epicfight$renderHandsWithItems(float partialTicks, PoseStack poseStack, MultiBufferSource.BufferSource bufferSource, LocalPlayer player, int packedLight, CallbackInfo callbackInfo) {
        RenderHandEvent event = new RenderHandEvent(player, partialTicks, poseStack, bufferSource, packedLight);

        boolean canceled = EpicFightClientEventHooks.Render.RENDER_HAND.post(event).isCanceled();
        if (canceled) {
            // Vanilla renderHandsWithItems calls bufferSource.endBatch() at the end.
            // Since we cancel the entire method, we must flush the buffer here,
            // otherwise the EpicFight first-person model is drawn but never appears on screen.
            bufferSource.endBatch();
            callbackInfo.cancel();
        }
    }
}