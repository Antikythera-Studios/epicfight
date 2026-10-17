package yesman.epicfight.client.renderer.patched.entity;
import yesman.epicfight.EpicFight;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Vector4f;
import yesman.epicfight.api.client.event.EpicFightClientEventHooks;
import yesman.epicfight.api.client.event.types.registry.RegisterPatchedRenderersEvent;
import yesman.epicfight.api.client.event.types.render.PrepareModelEvent;
import yesman.epicfight.api.extension.EntityExtension;
import yesman.epicfight.api.client.model.SkinnedMesh;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.api.utils.math.MathUtils;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.api.utils.math.Vec2i;
import yesman.epicfight.api.utils.math.Vec3f;
import yesman.epicfight.client.renderer.LayerRenderer;
import yesman.epicfight.client.renderer.patched.layer.LayerUtil;
import yesman.epicfight.client.renderer.patched.layer.PatchedLayer;
import yesman.epicfight.client.renderer.patched.layer.RenderOriginalModelLayer;
import yesman.epicfight.main.EpicFightMod;
import yesman.epicfight.mixin.client.MixinLivingEntityRenderer;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

import java.io.IOException;
import java.io.Reader;
import java.util.List;
import java.util.Map;

public abstract class PatchedLivingEntityRenderer<E extends LivingEntity, T extends LivingEntityPatch<E>, M extends EntityModel<E>, R extends LivingEntityRenderer<E, M>, AM extends SkinnedMesh> extends PatchedEntityRenderer<E, T, R, AM> implements LayerRenderer<E, T, M> {
	protected final Map<Class<?>, PatchedLayer<E, T, M, ? extends RenderLayer<E, M>>> patchedLayers = Maps.newHashMap();
	protected final List<PatchedLayer<E, T, M, ? extends RenderLayer<E, M>>> customLayers = Lists.newArrayList();
	
	public PatchedLivingEntityRenderer(EntityRendererProvider.Context context, EntityType<?> entityType) {
		ResourceLocation type = EntityType.getKey(entityType);
		FileToIdConverter filetoidconverter = FileToIdConverter.json("animated_layers/" + type.getPath());
		List<Pair<ResourceLocation, JsonElement>> layers = Lists.newArrayList();
		
		for (Map.Entry<ResourceLocation, Resource> entry : filetoidconverter.listMatchingResources(context.getResourceManager()).entrySet()) {
			Reader reader = null;
			
			try {
				reader = entry.getValue().openAsReader();
				JsonElement jsonelement = GsonHelper.fromJson(new GsonBuilder().create(), reader, JsonElement.class);
				layers.add(Pair.of(entry.getKey(), jsonelement));
			} catch (IllegalArgumentException | IOException | JsonParseException jsonparseexception) {
				EpicFight.LOGGER.error("Failed to parse layer file {} for {}", entry.getKey(), type);
				jsonparseexception.printStackTrace();
			} finally {
				try {
					if (reader != null) {
						reader.close();
					}
				} catch (IOException e) {
				}
			}
		}
		
		LayerUtil.addLayer(this, entityType, layers);
	}
	
	@SuppressWarnings("unchecked")
	public PatchedLivingEntityRenderer<E, T, M, R, AM> initLayerLast(EntityRendererProvider.Context context, EntityType<?> entityType) {
		List<RenderLayer<E, M>> vanillaLayers = null;
		
		if (entityType == EntityType.PLAYER) {
			if (context.getEntityRenderDispatcher().playerRenderers.get(PlayerSkin.Model.WIDE) instanceof LivingEntityRenderer livingentityrenderer) {
				vanillaLayers = livingentityrenderer.layers;
			}
		} else {
			if (context.getEntityRenderDispatcher().renderers.get(entityType) instanceof LivingEntityRenderer livingentityrenderer) {
				vanillaLayers = livingentityrenderer.layers;
			}
		}
		
		if (vanillaLayers != null) {
			for (RenderLayer<E, M> layer : vanillaLayers) {
				Class<?> layerClass = layer.getClass();
				
				if (layerClass.isAnonymousClass()) {
					layerClass = layer.getClass().getSuperclass();
				}
				
				if (this.patchedLayers.containsKey(layerClass)) {
					continue;
				}
				
				this.addPatchedLayer(layerClass, new RenderOriginalModelLayer<> ("Root", new Vec3f(0.0F, this.getDefaultLayerHeightCorrection(), 0.0F), new Vec3f(0.0F, 0.0F, 0.0F)));
			}
		}
		
		return this;
	}
	
	@Override
	public void render(E entity, T entitypatch, R renderer, MultiBufferSource buffer, PoseStack poseStack, int packedLight, float partialTicks) {
		super.render(entity, entitypatch, renderer, buffer, poseStack, packedLight, partialTicks);

		MixinLivingEntityRenderer livingEntityRendererAccessor = (MixinLivingEntityRenderer)renderer;

		boolean isVisible = livingEntityRendererAccessor.invokeIsBodyVisible(entity);
		boolean isVisibleToPlayer = !isVisible && !entity.isInvisibleTo(Minecraft.getInstance().player);
		boolean isGlowing = Minecraft.getInstance().shouldEntityAppearGlowing(entity);
		RenderType renderType = livingEntityRendererAccessor.invokeGetRenderType(entity, isVisible, isVisibleToPlayer, isGlowing);
		Armature armature = entitypatch.getArmature();
		if (armature == null) {
			yesman.epicfight.EpicFight.LOGGER.warn("[EpicFight] {} armature is null — cannot render Epic Fight model", entity.getType());
			return;
		}
		poseStack.pushPose();
		this.mulPoseStack(poseStack, armature, entity, entitypatch, partialTicks);
		this.prepareVanillaModel(entity, renderer.getModel(), renderer, partialTicks);
		this.setArmaturePose(entitypatch, armature, partialTicks);
		
		if (renderType != null) {
			AM mesh = this.getMeshProvider(entitypatch).get();
			this.prepareModel(mesh, entity, entitypatch, renderer);
			PrepareModelEvent prepareModelEvent = new PrepareModelEvent(this, mesh, entitypatch, buffer, poseStack, packedLight, partialTicks);

			if (!EpicFightClientEventHooks.Render.PREPARE_MODEL_TO_RENDER.post(prepareModelEvent).isCanceled()) {
				Vector4f color = new Vector4f(1.0F, 1.0F, 1.0F, isVisibleToPlayer ? 0.15F : 1.0F);
				entitypatch.getEntityDecorations().modifyColor(color, partialTicks);
				
				int blockLight = (packedLight & 0xF0) >> 4;
				int skyLight = (packedLight & 0xF00000) >> 20;
				Vec2i lightUv = new Vec2i(blockLight, skyLight);
				entitypatch.getEntityDecorations().modifyLight(lightUv, partialTicks);
				int modifiedLight = LightTexture.pack(lightUv.x, lightUv.y);
				mesh.draw(poseStack, buffer, renderType, modifiedLight, color.x(), color.y(), color.z(), color.w(), this.getOverlayCoord(entity, entitypatch, partialTicks), armature, armature.getPoseMatrices());
				
				entitypatch.getEntityDecorations().listDecorationOverlays().forEach(decorationOverlay -> {
					if (decorationOverlay.shouldRender()) {
						Vector4f overlayColor = decorationOverlay.color(partialTicks);
						mesh.draw(poseStack, buffer, decorationOverlay.getRenderType(), modifiedLight, overlayColor.x(), overlayColor.y(), overlayColor.z(), overlayColor.w(), OverlayTexture.NO_OVERLAY, armature, armature.getPoseMatrices());
					}
				});
			}
		}
		
		if (!entity.isSpectator()) {
			this.renderLayer(renderer, entitypatch, entity, armature.getPoseMatrices(), buffer, poseStack, packedLight, partialTicks);
		}
		
		if (renderType != null) {
			if (Minecraft.getInstance().getEntityRenderDispatcher().shouldRenderHitBoxes()) {
				entitypatch.getClientAnimator().renderDebuggingInfoForAllLayers(poseStack, buffer, partialTicks);
			}
		}
		
		poseStack.popPose();
	}
	
	protected void prepareVanillaModel(E entity, M model, LivingEntityRenderer<E, M> renderer, float partialTicks) {
		boolean shouldSit = entity.isPassenger() && entity.getVehicle() != null && EntityExtension.of(entity.getVehicle()).epicfight$shouldRiderSit();
		model.riding = shouldSit;
		model.young = entity.isBaby();
		float f = Mth.rotLerp(partialTicks, entity.yBodyRotO, entity.yBodyRot);
		float f1 = Mth.rotLerp(partialTicks, entity.yHeadRotO, entity.yHeadRot);
		float f2 = f1 - f;
		
		if (shouldSit && entity.getVehicle() instanceof LivingEntity livingentity) {
			f = Mth.rotLerp(partialTicks, livingentity.yBodyRotO, livingentity.yBodyRot);
			f2 = f1 - f;
			float f3 = Mth.wrapDegrees(f2);
			if (f3 < -85.0F) {
				f3 = -85.0F;
			}

			if (f3 >= 85.0F) {
				f3 = 85.0F;
			}

			f = f1 - f3;
			if (f3 * f3 > 2500.0F) {
				f += f3 * 0.2F;
			}

			f2 = f1 - f;
		}

		float f6 = Mth.lerp(partialTicks, entity.xRotO, entity.getXRot());
		
		if (LivingEntityRenderer.isEntityUpsideDown(entity)) {
			f6 *= -1.0F;
			f2 *= -1.0F;
		}
		
		float f7 = ((MixinLivingEntityRenderer)renderer).invokeGetBob(entity, partialTicks);
		float f8 = 0.0F;
		float f5 = 0.0F;
		
		if (!shouldSit && entity.isAlive()) {
			f8 = entity.walkAnimation.speed(partialTicks);
			f5 = entity.walkAnimation.position() - entity.walkAnimation.speed() * (1.0F - partialTicks);
			if (entity.isBaby()) {
				f5 *= 3.0F;
			}

			if (f8 > 1.0F) {
				f8 = 1.0F;
			}
		}
		
		model.prepareMobModel(entity, f5, f8, partialTicks);
		model.setupAnim(entity, f5, f8, f7, f2, f6);
	}
	
	protected void prepareModel(AM mesh, E entity, T entitypatch, R renderer) {
		mesh.initialize();
	}
	
	protected void renderLayer(LivingEntityRenderer<E, M> renderer, T entitypatch, E entity, OpenMatrix4f[] poses, MultiBufferSource buffer, PoseStack poseStack, int packedLight, float partialTicks) {
		float f = MathUtils.lerpBetween(entity.yBodyRotO, entity.yBodyRot, partialTicks);
        float f1 = MathUtils.lerpBetween(entity.yHeadRotO, entity.yHeadRot, partialTicks);
        float f2 = f1 - f;
		float f7 = entity.getViewXRot(partialTicks);
		float bob = ((MixinLivingEntityRenderer)renderer).invokeGetBob(entity, partialTicks);
		
		for (RenderLayer<E, M> layer : renderer.layers) {
			Class<?> layerClass = layer.getClass();
			
			if (layerClass.isAnonymousClass()) {
				layerClass = layerClass.getSuperclass();
			}
			
			if (this.patchedLayers.containsKey(layerClass)) {
				this.patchedLayers.get(layerClass).renderLayer(entity, entitypatch, layer, poseStack, buffer, packedLight, poses, bob, f2, f7, partialTicks);
			}
		}
		
		for (PatchedLayer<E, T, M, ? extends RenderLayer<E, M>> patchedLayer : this.customLayers) {
			patchedLayer.renderLayer(entity, entitypatch, null, poseStack, buffer, packedLight, poses, bob, f2, f7, partialTicks);
		}
	}
	
	protected int getOverlayCoord(E entity, T entitypatch, float partialTicks) {
		int initU = 0;
		int initV = OverlayTexture.v(entity.hurtTime > 0 || entity.deathTime > 0);
		
		Vec2i coord = new Vec2i(initU, initV);
		entitypatch.getEntityDecorations().modifyOverlay(coord, partialTicks);
		
		return OverlayTexture.pack(coord.x, coord.y);
	}
	
	@Override
	public void mulPoseStack(PoseStack poseStack, Armature armature, E entity, T entitypatch, float partialTicks) {
		super.mulPoseStack(poseStack, armature, entity, entitypatch, partialTicks);
        
        if (entity.isCrouching()) {
			poseStack.translate(0.0D, 0.15D, 0.0D);
		}
	}
	
	@Override
	public void addPatchedLayer(Class<?> originalLayerClass, PatchedLayer<E, T, M, ? extends RenderLayer<E, M>> patchedLayer) {
		this.patchedLayers.putIfAbsent(originalLayerClass, patchedLayer);
	}
	
	/**
	 * Use this method in {@link RegisterPatchedRenderersEvent.ModifyEntity}}
	 */
	public void addPatchedLayerAlways(Class<?> originalLayerClass, PatchedLayer<E, T, M, ? extends RenderLayer<E, M>> patchedLayer) {
		this.patchedLayers.put(originalLayerClass, patchedLayer);
	}
	
	@Override
	public void addCustomLayer(PatchedLayer<E, T, M, ? extends RenderLayer<E, M>> patchedLayer) {
		this.customLayers.add(patchedLayer);
	}
	
	protected float getDefaultLayerHeightCorrection() {
		return 1.15F;
	}
}
