package yesman.epicfight.platform.fabric.client;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import org.jetbrains.annotations.NotNull;
import yesman.epicfight.platform.client.KeyMappingRegistrar;

public final class FabricKeyMappingRegistrar implements KeyMappingRegistrar {
    @Override
    public void registerKeyMapping(@NotNull final KeyMapping keyMapping) {
        KeyBindingHelper.registerKeyBinding(keyMapping);
    }
}
