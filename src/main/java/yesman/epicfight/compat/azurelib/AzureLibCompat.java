package yesman.epicfight.compat.azurelib;

import yesman.epicfight.api.client.event.EpicFightClientEventHooks;
import yesman.epicfight.api.client.model.transformer.HumanoidModelBaker;
import yesman.epicfight.compat.ICompatModule;
import yesman.epicfight.compat.azurelib.client.AzureModelTransformer;

public class AzureLibCompat implements ICompatModule {
	@Override
	public void onInitialize() {
	}

	@Override
	public void onInitializeServer() {
	}

	@Override
	public void onInitializeClient() {
		HumanoidModelBaker.registerNewTransformer(new AzureModelTransformer());
	}

	@Override
	public void onInitializeClientServer() {
		EpicFightClientEventHooks.Render.ANIMATED_ARMOR_TEXTURE.registerEvent(AzureModelTransformer::getGeoArmorTexturePath);
	}
}
