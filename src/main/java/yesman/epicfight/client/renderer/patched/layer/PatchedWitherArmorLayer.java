package yesman.epicfight.client.renderer.patched.layer;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.WitherBossModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.layers.WitherArmorLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import yesman.epicfight.api.animation.AnimationPlayer;
import yesman.epicfight.api.client.model.Mesh;
import yesman.epicfight.api.client.model.Meshes;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.client.mesh.WitherMesh;
import yesman.epicfight.client.renderer.EpicFightRenderTypes;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.world.capabilities.entitypatch.boss.WitherPatch;

public class PatchedWitherArmorLayer extends ModelRenderLayer<WitherBoss, WitherPatch, WitherBossModel<WitherBoss>, WitherArmorLayer, WitherMesh> {
	private static final ResourceLocation WITHER_ARMOR_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/wither/wither_armor.png");
	
	public PatchedWitherArmorLayer() {
		super(Meshes.WITHER);
	}
	
	@Override
	protected void renderLayer(WitherPatch entitypatch, WitherBoss entityliving, WitherArmorLayer vanillaLayer, PoseStack poseStack, MultiBufferSource buffers, int packedLight, OpenMatrix4f[] poses, float bob, float yRot, float xRot, float partialTick) {
		if (entitypatch.isArmorActivated()) {
			float progress = (float)entityliving.tickCount + partialTick;
			poseStack.pushPose();
			poseStack.translate(0.0D, -0.1D, 0.0D);
			poseStack.scale(1.05F, 1.05F, 1.05F);
			int transparencyCount = entitypatch.getTransparency();
			float transparency = 1.0F;
			
			if (transparencyCount == 0) {
				transparency = entitypatch.isGhost() ? 0.0F : 1.0F;
				AnimationPlayer animationPlayer = entitypatch.getAnimator().getPlayerFor(null);
				
				if (animationPlayer.getAnimation().get() == Animations.WITHER_SPELL_ARMOR) {
					transparency = (animationPlayer.getPrevElapsedTime() + (animationPlayer.getElapsedTime() - animationPlayer.getPrevElapsedTime()) * partialTick) / (Animations.WITHER_SPELL_ARMOR.get().getTotalTime() - 0.5F);
				}
			} else {
				if (transparencyCount < 0) {
					transparency = 1.0F - (Math.abs(transparencyCount) + partialTick) / 41.0F;
				} else if (transparencyCount > 0) {
					transparency = (Math.abs(transparencyCount) + partialTick) / 41.0F;
				}
			}
			
			RenderType renderType = EpicFightRenderTypes.makeTriangulated(RenderType.energySwirl(WITHER_ARMOR_LOCATION, Mth.cos(progress * 0.02F) * 3.0F % 1.0F, progress * 0.01F % 1.0F));
			this.mesh.get().drawPosed(poseStack, buffers.getBuffer(renderType), Mesh.DrawingFunction.NEW_ENTITY, packedLight, 0.5F * transparency, 0.5F * transparency, 0.5F * transparency, 1.0F, OverlayTexture.NO_OVERLAY, entitypatch.getArmature(), poses);
			
			poseStack.popPose();
		}
	}
}