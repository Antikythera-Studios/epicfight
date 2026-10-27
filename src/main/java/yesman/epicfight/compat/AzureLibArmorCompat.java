package yesman.epicfight.compat;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.LivingEntity;
import mod.azure.azurelibarmor.animatable.client.RenderProvider;
import mod.azure.azurelibarmor.core.animatable.GeoAnimatable;
import mod.azure.azurelibarmor.renderer.GeoArmorRenderer;
import yesman.epicfight.api.client.forgeevent.AnimatedArmorTextureEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.IEventBus;

public class AzureLibArmorCompat implements ICompatModule {
	@OnlyIn(Dist.CLIENT)
	@Override
	public void onModEventBusClient(IEventBus eventBus) {
	}
	
	@OnlyIn(Dist.CLIENT)
	@Override
	public void onForgeEventBusClient(IEventBus eventBus) {
		eventBus.addListener(this::getGeoArmorTexturePath);
	}
	
	@Override
	public void onModEventBus(IEventBus eventBus) {
	}
	
	@Override
	public void onForgeEventBus(IEventBus eventBus) {
	}
	
	@OnlyIn(Dist.CLIENT)
	@SuppressWarnings("unchecked")
	public void getGeoArmorTexturePath(AnimatedArmorTextureEvent event) {
		RenderProvider customRenderProperties = RenderProvider.of(event.getItemstack());

		if (customRenderProperties != null) {
			HumanoidModel<?> extensionRenderer = customRenderProperties.getHumanoidArmorModel(event.getLivingEntity(),
				event.getItemstack(), event.getEquipmentSlot(), (HumanoidModel<LivingEntity>) event.getOriginalModel());

			if (extensionRenderer instanceof GeoArmorRenderer geoArmorRenderer
					&& event.getItemstack().getItem() instanceof GeoAnimatable geoAnimatable) {
				event.setResultLocation(geoArmorRenderer.getTextureLocation(geoAnimatable));
			}
		}
	}
}
