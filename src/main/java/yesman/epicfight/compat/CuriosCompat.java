package yesman.epicfight.compat;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;
import top.theillusivec4.curios.api.client.ICurioRenderer.HumanoidRender;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;
import top.theillusivec4.curios.client.render.CuriosLayer;
import yesman.epicfight.api.client.forgeevent.PatchedRenderersEvent;
import javax.annotation.Nullable;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import top.theillusivec4.curios.api.client.ICurioRenderer;
import yesman.epicfight.api.client.model.SkinnedMesh;
import yesman.epicfight.api.client.model.rebake.ModelRebaker;
import yesman.epicfight.api.client.model.rebake.RebakedMesh;
import yesman.epicfight.api.client.model.rebake.LayerAttachment;
import yesman.epicfight.api.client.model.rebake.WornRebaker;
import yesman.epicfight.api.client.model.rebake.ModelSnapshot;
import yesman.epicfight.api.client.model.rebake.VanillaPoseSync;
import yesman.epicfight.main.EpicFightMod;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.client.ClientEngine;
import yesman.epicfight.client.renderer.patched.entity.PatchedLivingEntityRenderer;
import yesman.epicfight.client.renderer.patched.layer.ModelRenderLayer;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

public class CuriosCompat implements ICompatModule {
	@SuppressWarnings("unchecked")
	@OnlyIn(Dist.CLIENT)
	@Override
	public void onModEventBusClient(IEventBus eventBus) {
		PatchedLivingEntityRenderer.claimLayer(CuriosLayer.class, PatchedCuriosLayerRenderer::new);

		eventBus.<PatchedRenderersEvent.Modify>addListener((event) -> {
			if (event.get(EntityType.PLAYER) instanceof PatchedLivingEntityRenderer patchedlivingrenderer) {
				patchedlivingrenderer.addPatchedLayerAlways(CuriosLayer.class, new PatchedCuriosLayerRenderer());
			} else {
			}
		});
		
		eventBus.<EntityRenderersEvent.AddLayers>addListener((event) -> PatchedCuriosLayerRenderer.clearCache());
	}
	
	@OnlyIn(Dist.CLIENT)
	@Override
	public void onForgeEventBusClient(IEventBus eventBus) {
	}
	
	@Override
	public void onModEventBus(IEventBus eventBus) {
	}
	
	@Override
	public void onForgeEventBus(IEventBus eventBus) {
	}
	
	@OnlyIn(Dist.CLIENT)
	public static class PatchedCuriosLayerRenderer extends ModelRenderLayer<LivingEntity, LivingEntityPatch<LivingEntity>, EntityModel<LivingEntity>, CuriosLayer<LivingEntity, EntityModel<LivingEntity>>, SkinnedMesh> {
		public PatchedCuriosLayerRenderer() {
			super(null);
		}
		

		static void clearCache() {
			ModelRebaker.clear();
			LayerAttachment.clear();
			WornRebaker.clear();
		}
		
		@Override
		protected void renderLayer(
			  LivingEntityPatch<LivingEntity> entitypatch
			, LivingEntity livingEntity
			, CuriosLayer<LivingEntity, EntityModel<LivingEntity>> vanillaLayer
			, PoseStack poseStack
			, MultiBufferSource buffers
			, int packedLight
			, OpenMatrix4f[] poses
			, float bob
			, float yRot
			, float xRot
			, float partialTicks
		) {

			CuriosApi.getCuriosInventory(livingEntity).ifPresent((handler) -> {
				handler.getCurios().forEach((id, stacksHandler) -> {
					IDynamicStackHandler stackHandler = stacksHandler.getStacks();
					IDynamicStackHandler cosmeticStacksHandler = stacksHandler.getCosmeticStacks();
					
					for (int i = 0; i < stackHandler.getSlots(); i++) {
						ItemStack stack = cosmeticStacksHandler.getStackInSlot(i);
						boolean cosmetic = true;
						NonNullList<Boolean> renderStates = stacksHandler.getRenders();
						boolean renderable = renderStates.size() > i && renderStates.get(i);

						
						if (stack.isEmpty() && renderable) {
							stack = stackHandler.getStackInSlot(i);
							cosmetic = false;
						}
						
						if (stack.isEmpty()) {
							continue;
						}
						
						final ItemStack finalStack = stack;
						SlotContext slotContext = new SlotContext(id, livingEntity, i, cosmetic, renderable);
						
						CuriosRendererRegistry.getRenderer(stack.getItem()).ifPresent(curioRenderer -> {
							try {
								this.renderOne(curioRenderer, finalStack, slotContext, entitypatch, livingEntity, vanillaLayer, poseStack, buffers, packedLight, poses, bob, yRot, xRot, partialTicks);
							} catch (Exception e) {
								EpicFightMod.LOGGER.debug("{} failed to render: {}", finalStack.getItem(), e.toString());
							}
						});
					}
				});
			});
		}
		
		private void renderOne(ICurioRenderer curioRenderer, ItemStack stack, SlotContext slotContext,
				LivingEntityPatch<LivingEntity> entitypatch, LivingEntity livingEntity,
				CuriosLayer<LivingEntity, EntityModel<LivingEntity>> vanillaLayer, PoseStack poseStack,
				MultiBufferSource buffers, int packedLight, OpenMatrix4f[] poses, float bob, float yRot, float xRot, float partialTicks) {
			if (curioRenderer instanceof EpicFightCurioRenderer epicfightRenderer) {
				epicfightRenderer.draw(stack, slotContext, entitypatch, livingEntity, vanillaLayer, poseStack, buffers, packedLight, poses, bob, yRot, xRot, partialTicks);
				
				return;
			}
			
			if (curioRenderer instanceof HumanoidRender humanoidRenderer && !ClientEngine.getInstance().isVanillaModelDebuggingMode()) {
				HumanoidModel<LivingEntity> curioModel = humanoidRenderer.getModel(stack, slotContext);
				ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
				
				if (curioModel != null && itemId != null) {
					RebakedMesh mesh = ModelRebaker.bakeHumanoid("curio/" + itemId, curioModel);
					
					if (mesh != null) {
						mesh.initialize();
						mesh.bindAnimationSource(curioModel);
						mesh.draw(
							  poseStack
							, buffers
							, RenderType.entityCutoutNoCull(humanoidRenderer.getModelTexture(stack, slotContext))
							, packedLight
							, 1.0F, 1.0F, 1.0F, 1.0F
							, OverlayTexture.NO_OVERLAY
							, entitypatch.getArmature()
							, poses
						);
						
						return;
					}
				}
			}
			
			LivingEntityRenderer<?, ?> parentRenderer = parentRendererFor(livingEntity);
			
			if (parentRenderer == null) {
				return;
			}
			
			ResourceLocation curioId = ForgeRegistries.ITEMS.getKey(stack.getItem());

			if (curioId != null) {
				String key = WornRebaker.keyed("curio/" + curioId + "#" + slotContext.identifier() + "/" + slotContext.index()
					+ (slotContext.cosmetic() ? "/cosmetic" : ""),
					yesman.epicfight.api.client.model.rebake.ArmorRebaker.tintOf(stack));
				java.util.List<WornRebaker.Baked> baked = WornRebaker.bake(key, livingEntity, entitypatch.getArmature(),
					(atRest, captureBuffers) -> curioRenderer.render(stack, slotContext, atRest, parentRenderer,
						captureBuffers, 0, 0.0F, 0.0F, partialTicks, bob, yRot, xRot));

				if (baked != null) {
					java.util.List<net.minecraft.client.renderer.RenderType> now = WornRebaker.typesNow(key, baked,
						(atRest, probeBuffers) -> curioRenderer.render(stack, slotContext, atRest, parentRenderer,
							probeBuffers, packedLight, 0.0F, 0.0F, partialTicks, bob, yRot, xRot));

					if (now == null) {
						return;
					}


					for (int i = 0; i < baked.size(); i++) {
						WornRebaker.Baked pass = baked.get(i);
						pass.mesh().initialize();
						pass.mesh().draw(
							  poseStack
							, buffers
							, now.get(i)
							, packedLight
							, 1.0F, 1.0F, 1.0F, 1.0F
							, OverlayTexture.NO_OVERLAY
							, entitypatch.getArmature()
							, poses
						);
					}

					return;
				}
			}


			HumanoidModel<?> body = parentRenderer.getModel() instanceof HumanoidModel<?> humanoid ? humanoid : null;
			ModelSnapshot borrowed = ModelSnapshot.of(body);

			if (body != null) {
				VanillaPoseSync.apply(body, entitypatch.getArmature(), poses);
			}

			poseStack.pushPose();
			VanillaPoseSync.pushPartSpace(poseStack);

			try {
				curioRenderer.render(stack, slotContext, poseStack, parentRenderer, buffers, packedLight, 0.0F, 0.0F, partialTicks, bob, yRot, xRot);
			} finally {
				poseStack.popPose();

				if (borrowed != null) {
					borrowed.restore();
				}
			}
		}
		
		@Nullable
		private static LivingEntityRenderer<?, ?> parentRendererFor(LivingEntity livingEntity) {
			if (livingEntity instanceof AbstractClientPlayer abstractClientPlayer) {
				return (LivingEntityRenderer<?, ?>) Minecraft.getInstance().getEntityRenderDispatcher().getSkinMap().get(abstractClientPlayer.getModelName());
			}
			
			if (Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(livingEntity) instanceof LivingEntityRenderer<?, ?> renderer) {
				return renderer;
			}
			
			return (LivingEntityRenderer<?, ?>) Minecraft.getInstance().getEntityRenderDispatcher().getSkinMap().get("default");
		}
	}
	
	@OnlyIn(Dist.CLIENT)
	public interface EpicFightCurioRenderer {
		void draw(
			  ItemStack itemstack
			, SlotContext slotContext
			, LivingEntityPatch<LivingEntity> entitypatch
			, LivingEntity livingEntity
			, CuriosLayer<LivingEntity, EntityModel<LivingEntity>> vanillaLayer
			, PoseStack poseStack
			, MultiBufferSource buffer
			, int packedLight
			, OpenMatrix4f[] poses
			, float bob
			, float yRot
			, float xRot
			, float partialTicks
		);
	}
}
