package yesman.epicfight.compat.geckolib;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;


import software.bernie.geckolib.event.GeoRenderEvent;
import yesman.epicfight.api.client.event.EpicFightClientEventHooks;
import yesman.epicfight.api.client.model.transformer.HumanoidModelBaker;
import yesman.epicfight.client.events.engine.RenderEngine;
import yesman.epicfight.client.gui.EntityUI;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;
import yesman.epicfight.compat.ICompatModule;
import yesman.epicfight.compat.geckolib.client.GeoModelTransformer;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.gamerule.EpicFightGameRules;

public class GeckolibCompat implements ICompatModule {
	@Override
	public void onInitializeClient() {
		HumanoidModelBaker.registerNewTransformer(new GeoModelTransformer());
		GeoRenderEvent.Entity.Pre.EVENT.register(this::geoEntityRenderPreEvent);
		GeoRenderEvent.Entity.Post.EVENT.register(this::geoEntityRenderPostEvent);
	}

	@Override
	public void onInitializeClientServer() {
        EpicFightClientEventHooks.Render.ANIMATED_ARMOR_TEXTURE.registerEvent(GeoModelTransformer::getGeoArmorTexturePath);
	}
	
	@Override
	public void onInitialize() {
	}
	
	@Override
	public void onInitializeServer() {
	}
	
	public boolean geoEntityRenderPreEvent(GeoRenderEvent.Entity.Pre event) {
		Entity entity = event.getEntity();

		if (entity.level() == null) {
			return false;
		}

		boolean canceled = false;
		
		if (entity instanceof LivingEntity livingentity) {
			RenderEngine renderEngine = RenderEngine.getInstance();
			
			if (renderEngine.hasRendererFor(livingentity)) {
				LivingEntityPatch<?> entitypatch = EpicFightCapabilities.getEntityPatch(livingentity, LivingEntityPatch.class);
				LocalPlayerPatch playerpatch = null;
				float originalYRot = 0.0F;
				
				if ((event.getPartialTick() == 0.0F || event.getPartialTick() == 1.0F) && entitypatch instanceof LocalPlayerPatch localPlayerPatch) {
					playerpatch = localPlayerPatch;
					originalYRot = playerpatch.getModelYRot();
					playerpatch.setModelYRotInGui(livingentity.getYRot());
					event.getPoseStack().translate(0, 0.1D, 0);
				}
				
				if (entitypatch != null && entitypatch.overrideRender()) {
					canceled = true;
					renderEngine.renderEntityArmatureModel(livingentity, entitypatch, event.getRenderer(), event.getBufferSource(), event.getPoseStack(), event.getPackedLight(), event.getPartialTick());
					
					if (EpicFightCapabilities.ClientModule.getCachedLocalPlayerPatch() != null && !renderEngine.minecraft.options.hideGui && !EpicFightGameRules.DISABLE_ENTITY_UI.getRuleValue(livingentity.level())) {
						for (EntityUI entityIndicator : EntityUI.ENTITY_UI_LIST) {
							if (entityIndicator.shouldDraw(livingentity, entitypatch, EpicFightCapabilities.ClientModule.getCachedLocalPlayerPatch(), event.getPartialTick())) {
								entityIndicator.draw(livingentity, entitypatch, EpicFightCapabilities.ClientModule.getCachedLocalPlayerPatch(), event.getPoseStack(), event.getBufferSource(), event.getPartialTick());
							}
						}
					}
				}
				
				if (playerpatch != null) {
					playerpatch.disableModelYRotInGui(originalYRot);
				}
			}
		}

		return !canceled;
	}
	
	public void geoEntityRenderPostEvent(GeoRenderEvent.Entity.Post event) {
		Entity entity = event.getEntity();
		
		if (entity.level() == null) {
			return;
		}
		
		if (entity instanceof LivingEntity livingentity) {
			RenderEngine renderEngine = RenderEngine.getInstance();
			
			if (EpicFightCapabilities.ClientModule.getCachedLocalPlayerPatch() != null && !renderEngine.minecraft.options.hideGui && !EpicFightGameRules.DISABLE_ENTITY_UI.getRuleValue(livingentity.level())) {
				LivingEntityPatch<?> entitypatch = EpicFightCapabilities.getEntityPatch(livingentity, LivingEntityPatch.class);
				
				for (EntityUI entityIndicator : EntityUI.ENTITY_UI_LIST) {
					if (entityIndicator.shouldDraw(livingentity, entitypatch, EpicFightCapabilities.ClientModule.getCachedLocalPlayerPatch(), event.getPartialTick())) {
						entityIndicator.draw(livingentity, entitypatch, EpicFightCapabilities.ClientModule.getCachedLocalPlayerPatch(), event.getPoseStack(), event.getBufferSource(), event.getPartialTick());
					}
				}
			}
		}
	}
}