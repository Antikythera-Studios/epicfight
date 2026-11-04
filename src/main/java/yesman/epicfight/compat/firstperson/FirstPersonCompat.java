package yesman.epicfight.compat.firstperson;

import dev.tr7zw.firstperson.api.FirstPersonAPI;

import yesman.epicfight.client.events.engine.RenderEngine;
import yesman.epicfight.compat.ICompatModule;

public class FirstPersonCompat implements ICompatModule {
	@Override
	public void onInitializeClient() {
		RenderEngine.setFirstPersonBodyOwner(FirstPersonAPI::isEnabled);
	}

	@Override
	public void onInitializeClientServer() {
	}

	@Override
	public void onInitialize() {
	}

	@Override
	public void onInitializeServer() {
	}
}
