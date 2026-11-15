package yesman.epicfight.compat.trinkets;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.emi.trinkets.TrinketFeatureRenderer;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.EntityType;

import yesman.epicfight.api.client.event.EpicFightClientEventHooks;
import yesman.epicfight.api.utils.math.MathUtils;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.client.renderer.patched.entity.PPlayerRenderer;
import yesman.epicfight.client.renderer.patched.layer.PatchedLayer;
import yesman.epicfight.client.world.capabilites.entitypatch.player.AbstractClientPlayerPatch;
import yesman.epicfight.compat.ICompatModule;

/// Fabric replacement for NeoForge's `CuriosCompat`.
///
/// Trinkets adds its accessories through a [TrinketFeatureRenderer] attached to the vanilla
/// player renderer, so once Epic Fight takes over rendering that layer is never reached and
/// accessories disappear. This re-attaches it as a patched layer anchored to the animated
/// armature's Root joint, which is the same live path `CuriosCompat` uses on NeoForge.
public class TrinketsCompat implements ICompatModule {
	@Override
	public void onInitialize() {
	}

	@Override
	public void onInitializeServer() {
	}

	@Override
	public void onInitializeClient() {
		EpicFightClientEventHooks.Registry.MODIFY_PATCHED_ENTITY.registerEvent(event -> {
			if (event.get(EntityType.PLAYER) instanceof PPlayerRenderer playerrenderer) {
				playerrenderer.addPatchedLayerAlways(TrinketFeatureRenderer.class, new PatchedTrinketsLayer());
			}
		});
	}

	@Override
	public void onInitializeClientServer() {
	}

	public static class PatchedTrinketsLayer extends PatchedLayer<AbstractClientPlayer, AbstractClientPlayerPatch<AbstractClientPlayer>, PlayerModel<AbstractClientPlayer>, TrinketFeatureRenderer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>>> {
		@Override
		protected void renderLayer(
			  AbstractClientPlayerPatch<AbstractClientPlayer> entitypatch
			, AbstractClientPlayer livingEntity
			, TrinketFeatureRenderer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> vanillaLayer
			, PoseStack poseStack
			, MultiBufferSource buffers
			, int packedLight
			, OpenMatrix4f[] poses
			, float bob
			, float yRot
			, float xRot
			, float partialTicks
		) {
			// castLayer returns null when Trinkets' feature renderer is absent from this renderer.
			if (vanillaLayer == null) {
				return;
			}

			poseStack.pushPose();
			OpenMatrix4f modelMatrix = poses[entitypatch.getArmature().searchJointByName("Root").getId()];
			MathUtils.mulStack(poseStack, modelMatrix);
			poseStack.translate(0.0F, 0.75F, 0.0F);
			poseStack.scale(-1.0F, -1.0F, 1.0F);
			vanillaLayer.render(poseStack, buffers, packedLight, livingEntity, livingEntity.walkAnimation.position(partialTicks), livingEntity.walkAnimation.speed(partialTicks), partialTicks, bob, yRot, xRot);
			poseStack.popPose();
		}
	}
}
