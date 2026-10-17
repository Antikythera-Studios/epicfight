package yesman.epicfight.api.utils;
import yesman.epicfight.platform.neoforged.client.ClientHooks;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import org.apache.logging.log4j.Logger;
import org.joml.Matrix4f;
import yesman.epicfight.api.animation.Pose;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.client.model.Mesh;
import yesman.epicfight.api.client.model.SkinnedMesh;
import yesman.epicfight.api.client.physics.cloth.ClothSimulator;
import yesman.epicfight.api.client.physics.cloth.ClothSimulator.ClothObject;
import yesman.epicfight.api.physics.SimulationTypes;
import yesman.epicfight.api.utils.math.MathUtils;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.api.utils.math.Vec3f;
import yesman.epicfight.client.events.engine.RenderEngine;
import yesman.epicfight.client.renderer.patched.entity.PatchedEntityRenderer;
import yesman.epicfight.client.renderer.patched.entity.PatchedLivingEntityRenderer;
import yesman.epicfight.client.renderer.patched.layer.PatchedCapeLayer;
import yesman.epicfight.client.renderer.patched.layer.WearableItemLayer;
import yesman.epicfight.client.world.capabilites.entitypatch.player.AbstractClientPlayerPatch;
import yesman.epicfight.main.EpicFightMod;
import yesman.epicfight.mixin.client.MinecraftAccessor;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

import java.util.List;
import java.util.function.Function;

public class EntitySnapshot<T extends LivingEntityPatch<?>> {
	private static final InteractionHand[] HANDS = InteractionHand.values();
	
	public static EntitySnapshot<LivingEntityPatch<?>> captureLivingEntity(LivingEntityPatch<?> entitypatch) {
		if (RenderEngine.getInstance().hasRendererFor(entitypatch.getOriginal())) {
			return new EntitySnapshot<> (entitypatch);
		}
		
		return null;
	}
	
	public static PlayerSnapshot capturePlayer(AbstractClientPlayerPatch<?> playerpatch) {
		if (RenderEngine.getInstance().hasRendererFor(playerpatch.getOriginal())) {
			return new PlayerSnapshot(playerpatch);
		}
		
		return null;
	}
	
	protected final T entitypatch;
	protected final RenderableFigure entityFigure;
	protected final OpenMatrix4f[] poseMatrices;
	protected final OpenMatrix4f modelMatrix;
	protected final Vec3 position;
	protected final List<RenderableFigure> armorMeshes;
	protected final List<Pair<InteractionHand, ItemStack>> handItems;
	protected final float yRot;
	protected final float heightHalf;
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public EntitySnapshot(T entitypatch) {
		LivingEntityRenderer<LivingEntity, EntityModel<LivingEntity>> vanillarenderer = (LivingEntityRenderer<LivingEntity, EntityModel<LivingEntity>>)Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(entitypatch.getOriginal());
		PatchedEntityRenderer patchedrenderer = (PatchedEntityRenderer)RenderEngine.getInstance().getEntityRenderer(entitypatch.getOriginal());
		AssetAccessor<SkinnedMesh> meshAccessor = patchedrenderer.getMeshProvider(entitypatch);
		
		ResourceLocation textureLocation = vanillarenderer.getTextureLocation(entitypatch.getOriginal());
		
		if (textureLocation == null) {
			EpicFightMod.logAndStacktraceIfDevSide(Logger::warn, "No texture for " + entitypatch.getOriginal(), NullPointerException::new, "No texture is provided by vanilla renderer " + vanillarenderer.getClass().getSimpleName());
		}
		
		if (meshAccessor == null || meshAccessor.isEmpty()) {
			EpicFightMod.logAndStacktraceIfDevSide(Logger::warn, "No mesh for " + entitypatch.getOriginal(), NullPointerException::new, "No mesh is provided by patched renderer " + patchedrenderer.getClass().getSimpleName());
		}
		
		this.entityFigure = new RenderableFigure(meshAccessor.get(), textureLocation);
		
		Pose pose = entitypatch.getAnimator().getPose(1.0F);
		patchedrenderer.setJointTransforms(entitypatch, entitypatch.getArmature(), pose, 1.0F);
		this.poseMatrices = entitypatch.getArmature().getPoseAsTransformMatrix(pose, false);
		this.modelMatrix = entitypatch.getModelMatrix(1.0F);
		
		ImmutableList.Builder<RenderableFigure> builder = ImmutableList.builder();
		
		entitypatch.getOriginal().getArmorSlots().forEach(itemstack -> {
			if (!(itemstack.getItem() instanceof ArmorItem)) {
				return;
			}
			
			EquipmentSlot armorSlot = ((ArmorItem)itemstack.getItem()).getEquipmentSlot();
			SkinnedMesh armor = WearableItemLayer.getCachedModel(itemstack.getItem());
			ResourceLocation texture = WearableItemLayer.getArmorResource(entitypatch.getOriginal(), itemstack, armorSlot, null);
			
			if (armor != null) {
				builder.add(new RenderableFigure(armor, texture));
			}
		});
		
		this.armorMeshes = builder.build();
		
		ImmutableList.Builder<Pair<InteractionHand, ItemStack>> builder$2 = ImmutableList.builder();
		
		for (InteractionHand hand : HANDS) {
			ItemStack itemstack = entitypatch.getAdvancedHoldingItemStack(hand);
			
			if (!itemstack.isEmpty()) {
				builder$2.add(Pair.of(hand, itemstack));
			}
		}
		
		this.handItems = builder$2.build();
		this.position = entitypatch.getOriginal().position();
		this.yRot = Mth.wrapDegrees(entitypatch.getYRot());
		this.heightHalf = entitypatch.getOriginal().getBbHeight() * 0.5F;
		this.entitypatch = entitypatch;
	}
	
	public void render(PoseStack poseStack, MultiBufferSource buffers, RenderType rendertype, Mesh.DrawingFunction drawingFunction, int packedLight, float r, float g, float b, float a) {
		if (this.entityFigure.mesh == null || this.entityFigure.texture == null) {
			return;
		}
		
		this.entityFigure.mesh.initialize();
		this.entityFigure.mesh.draw(poseStack, buffers, rendertype, drawingFunction, packedLight, r, g, b, a, OverlayTexture.NO_OVERLAY, this.entitypatch.getArmature(), this.poseMatrices);
		
		for (RenderableFigure armorFigures : this.armorMeshes) {
			armorFigures.mesh.initialize();
			armorFigures.mesh.draw(poseStack, buffers, rendertype, drawingFunction, packedLight, r, g, b, a, OverlayTexture.NO_OVERLAY, this.entitypatch.getArmature(), this.poseMatrices);
		}
	}
	
	public void renderTextured(PoseStack poseStack, MultiBufferSource buffers, Function<ResourceLocation, RenderType> rendertypeFunction, Mesh.DrawingFunction drawingFunction, int packedLight, float r, float g, float b, float a) {
		if (this.entityFigure.mesh == null || this.entityFigure.texture == null) {
			return;
		}
		
		this.entityFigure.mesh.initialize();
		this.entityFigure.mesh.draw(poseStack, buffers, rendertypeFunction.apply(this.entityFigure.texture), drawingFunction, packedLight, r, g, b, a, OverlayTexture.NO_OVERLAY, this.entitypatch.getArmature(), this.poseMatrices);
		
		for (RenderableFigure armorFigures : this.armorMeshes) {
			armorFigures.mesh.initialize();
			armorFigures.mesh.draw(poseStack, buffers, rendertypeFunction.apply(armorFigures.texture), drawingFunction, packedLight, r, g, b, a, OverlayTexture.NO_OVERLAY, this.entitypatch.getArmature(), this.poseMatrices);
		}
	}
	
	public void renderItems(PoseStack poseStack, MultiBufferSource buffers, RenderType rendertype, Mesh.DrawingFunction drawingFunction, int packedLight, float alpha) {
		for (Pair<InteractionHand, ItemStack> items : this.handItems) {
			ItemStack itemstack = items.getSecond();
			
			if (RenderEngine.getInstance().getItemRenderer(itemstack).appearedInAfterimage()) {
				poseStack.pushPose();
				BakedModel bakedmodel = Minecraft.getInstance().getItemRenderer().getModel(itemstack, this.entitypatch.getOriginal().level(), this.entitypatch.getOriginal(), this.entitypatch.getOriginal().getId() + ItemDisplayContext.THIRD_PERSON_RIGHT_HAND.ordinal());
				
				if (!bakedmodel.isCustomRenderer()) {
					MathUtils.mulStack(poseStack, RenderEngine.getInstance().getItemRenderer(itemstack).getCorrectionMatrix(this.entitypatch, items.getFirst(), this.poseMatrices));
					bakedmodel = ClientHooks.handleCameraTransforms(poseStack, bakedmodel, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, false);
					poseStack.translate(-0.5F, -0.5F, -0.5F);

					renderModelLists(bakedmodel, itemstack, packedLight, OverlayTexture.NO_OVERLAY, alpha, poseStack, buffers.getBuffer(rendertype), drawingFunction);
				}
				poseStack.popPose();
			}
		}
	}
	
	public OpenMatrix4f[] poseMatrices() {
		return this.poseMatrices;
	}
	
	public OpenMatrix4f getModelMatrix() {
		return this.modelMatrix;
	}
	
	public float getYRot() {
		return this.yRot;
	}
	
	public float getHeightHalf() {
		return this.heightHalf;
	}
	
	public Vec3 getPosition() {
		return this.position;
	}
	
	/**
	 * Code copy from {@link ItemRenderer#renderModelLists} but replaces putting bulk data to follow DrawingFunction
	 */
	@SuppressWarnings("deprecation")
	public static void renderModelLists(BakedModel pModel, ItemStack pStack, int pCombinedLight, int pCombinedOverlay, float alpha, PoseStack pPoseStack, VertexConsumer pBuffer, Mesh.DrawingFunction drawingFunction) {
		RandomSource randomsource = RandomSource.create();
		
		for (Direction direction : Direction.values()) {
			randomsource.setSeed(42L);
			renderQuadList(pPoseStack, pBuffer, pModel.getQuads((BlockState)null, direction, randomsource), pStack, pCombinedLight, pCombinedOverlay, alpha, drawingFunction);
		}

		randomsource.setSeed(42L);
		renderQuadList(pPoseStack, pBuffer, pModel.getQuads((BlockState)null, (Direction)null, randomsource), pStack, pCombinedLight, pCombinedOverlay, alpha, drawingFunction);
	}
	
	public static void renderQuadList(PoseStack pPoseStack, VertexConsumer pBuffer, List<BakedQuad> pQuads, ItemStack pItemStack, int pCombinedLight, int pCombinedOverlay, float alpha, Mesh.DrawingFunction drawingFunction) {
		boolean flag = !pItemStack.isEmpty();
		PoseStack.Pose posestack$pose = pPoseStack.last();
		
		for (BakedQuad bakedquad : pQuads) {
			int i = -1;
			
			if (flag && bakedquad.isTinted()) {
				i = ((MinecraftAccessor) Minecraft.getInstance()).epicfight$getItemColors().getColor(pItemStack, bakedquad.getTintIndex());
			}
			
			float f = (float) (i >> 16 & 255) / 255.0F;
			float f1 = (float) (i >> 8 & 255) / 255.0F;
			float f2 = (float) (i & 255) / 255.0F;
			
			drawingFunction.putBulkData(posestack$pose, bakedquad, pBuffer, f, f1, f2, alpha, pCombinedLight, pCombinedOverlay, true);
		}
	}
	
	public record RenderableFigure(Mesh mesh, ResourceLocation texture) {
	}
	
	public static class PlayerSnapshot extends EntitySnapshot<AbstractClientPlayerPatch<?>> {
		protected final Matrix4f localMatrix;
		protected final OpenMatrix4f[] unboundPoseMatrices;
		protected RenderableFigure capeFigure;
		
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public PlayerSnapshot(AbstractClientPlayerPatch<?> entitypatch) {
			super(entitypatch);
			
			PatchedLivingEntityRenderer patchedrenderer = (PatchedLivingEntityRenderer)RenderEngine.getInstance().getEntityRenderer(entitypatch.getOriginal());
			PoseStack poseStack = new PoseStack();
			patchedrenderer.mulPoseStack(poseStack, entitypatch.getArmature(), entitypatch.getOriginal(), entitypatch, 1.0F);
			this.localMatrix = poseStack.last().pose();
			this.unboundPoseMatrices = new OpenMatrix4f[entitypatch.getArmature().getPoseMatrices().length];
			
			for (int i = 0; i < entitypatch.getArmature().getPoseMatrices().length; i++) {
				this.unboundPoseMatrices[i] = new OpenMatrix4f(entitypatch.getArmature().getPoseMatrices()[i]);
			}
			
			if (entitypatch.getOriginal().isModelPartShown(PlayerModelPart.CAPE) && entitypatch.getOriginal().getSkin().capeTexture() != null) {
				entitypatch.getSimulator(SimulationTypes.CLOTH).ifPresent(clohtSimulator -> {
					clohtSimulator.getRunningObject(ClothSimulator.PLAYER_CLOAK).ifPresent(clothObj -> {
						ClothObject capturedClothObj = clothObj.captureMyself();
						Function<Float, OpenMatrix4f> partialColliderTransformProvider = (partialFrame) -> {
							Vec3 pos = entitypatch.getOriginal().getPosition(partialFrame);
							float yRotLerp = Mth.rotLerp(partialFrame, entitypatch.getYRotO(), entitypatch.getYRot());
							
							return OpenMatrix4f.createTranslation((float)pos.x, (float)pos.y, (float)pos.z).rotateDeg(180.0F - yRotLerp, Vec3f.Y_AXIS);
				        };
						
						capturedClothObj.tick(entitypatch, partialColliderTransformProvider, 1.0F, entitypatch.getArmature(), this.unboundPoseMatrices);
						this.capeFigure = new RenderableFigure(capturedClothObj, entitypatch.getOriginal().getSkin().capeTexture());
					});
				});
			}
		}
		
		@Override
		public void render(PoseStack poseStack, MultiBufferSource buffers, RenderType rendertype, Mesh.DrawingFunction drawingFunction, int packedLight, float r, float g, float b, float a) {
			if (this.capeFigure != null) {
				PatchedCapeLayer.renderSimulatingCape(poseStack, buffers, rendertype, drawingFunction, (ClothObject)this.capeFigure.mesh, this.position.x, this.position.y, this.position.z, r, g, b, a, this.entitypatch, this.unboundPoseMatrices, packedLight, this.localMatrix, this.yRot);
			}
			
			super.render(poseStack, buffers, rendertype, drawingFunction, packedLight, r, g, b, a);
		}
		
		@Override
		public void renderTextured(PoseStack poseStack, MultiBufferSource buffers, Function<ResourceLocation, RenderType> rendertypeFunction, Mesh.DrawingFunction drawingFunction, int packedLight, float r, float g, float b, float a) {
			super.renderTextured(poseStack, buffers, rendertypeFunction, drawingFunction, packedLight, r, g, b, a);
			
			if (this.capeFigure != null) {
				PatchedCapeLayer.renderSimulatingCape(poseStack, buffers, rendertypeFunction.apply(this.capeFigure.texture), drawingFunction, (ClothObject)this.capeFigure.mesh, this.position.x, this.position.y, this.position.z, r, g, b, a, this.entitypatch, this.unboundPoseMatrices, packedLight, this.localMatrix, this.yRot);
			}
		}
	}
}
