package yesman.epicfight.compat.mcreator;


import yesman.epicfight.api.client.event.EpicFightClientEventHooks;
import yesman.epicfight.api.client.event.types.render.ValidatePlayerModelEvent;
import yesman.epicfight.compat.ICompatModule;
import yesman.epicfight.world.capabilities.IPersistentEntityData;

public class MCreatorPlayerAnimationsCompat implements ICompatModule {
    @Override
	public void onInitialize() {}

    @Override
	public void onInitializeServer() {}

    @Override
	public void onInitializeClient() {}

    @Override
	public void onInitializeClientServer() {
        EpicFightClientEventHooks.Render.VALIDATE_PLAYER_MODEL_TO_RENDER.registerEvent(this::renderEvent);
    }

    private void renderEvent(ValidatePlayerModelEvent event) {
        String animation = ((IPersistentEntityData) event.getPlayerPatch().getOriginal()).epicfight$getPersistentData().getString("PlayerCurrentAnimation");
        if (!animation.isEmpty()) event.setShouldRender(false);
    }
}
