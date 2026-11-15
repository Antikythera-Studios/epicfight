package yesman.epicfight.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import yesman.epicfight.client.model.SeparateTransformsBakedModel;

/// Swaps the baked model to the per-perspective model when rendering items with [SeparateTransformsBakedModel].
///
/// Vanilla 1.21.1 [BakedModel] has no [applyTransform] method — transforms come from [getTransforms] and
/// quads from [getQuads] (which doesn't receive the display context). So per-perspective quads require
/// swapping the entire baked model based on the current [ItemDisplayContext].
@Mixin(value = ItemRenderer.class)
public abstract class MixinItemRenderer {
    @WrapOperation(
        method = "render(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IILnet/minecraft/client/resources/model/BakedModel;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/entity/ItemRenderer;renderModelLists(Lnet/minecraft/client/resources/model/BakedModel;Lnet/minecraft/world/item/ItemStack;IILcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;)V"
        )
    )
    private void epicfight$renderModelLists(
        ItemRenderer instance,
        BakedModel originalModel,
        ItemStack itemStack,
        int packedLight,
        int packedOverlay,
        com.mojang.blaze3d.vertex.PoseStack poseStack,
        com.mojang.blaze3d.vertex.VertexConsumer vertexConsumer,
        Operation<Void> original,
        @Local(argsOnly = true) ItemDisplayContext displayContext
    ) {
        BakedModel modelToUse = originalModel;

        if (originalModel instanceof SeparateTransformsBakedModel separateTransforms) {
            BakedModel perspectiveModel = separateTransforms.getPerspectiveModel(displayContext);

            if (perspectiveModel != null) {
                modelToUse = perspectiveModel;
            }
        }

        original.call(instance, modelToUse, itemStack, packedLight, packedOverlay, poseStack, vertexConsumer);
    }
}
