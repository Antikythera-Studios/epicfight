package yesman.epicfight.compat;

import yesman.epicfight.EpicFight;
import yesman.epicfight.main.EpicFightSharedConstants;

import java.lang.reflect.Constructor;

/// Compat module interface for Fabric.
/// On NeoForge, modules received Object instances. On Fabric, modules
/// register their own callbacks via Fabric API in the no-arg methods.
public interface ICompatModule {
	static void loadCompatModule(Class<? extends ICompatModule> compatModule) {
		try {
			Constructor<? extends ICompatModule> constructor = compatModule.getConstructor();
			ICompatModule compatModuleInstance = constructor.newInstance();
			compatModuleInstance.onInitialize();
			compatModuleInstance.onInitializeServer();

			if (EpicFightSharedConstants.isPhysicalClient()) {
				compatModuleInstance.onInitializeClient();
				compatModuleInstance.onInitializeClientServer();
			}

            EpicFight.LOGGER.info("Loaded mod compatibility module: {}", compatModule.getSimpleName());
		} catch (Exception e) {
            EpicFight.LOGGER.error("Failed to load mod compatibility module: {}", e.getMessage());
			e.printStackTrace();
		}
	}

	/// Common initialization — register Fabric callbacks here.
	void onInitialize();

	/// Server-side initialization.
	void onInitializeServer();

	/// Client-side initialization — register Fabric client callbacks here.
	void onInitializeClient();

	/// Client-server initialization (client-side server events).
	void onInitializeClientServer();
}
