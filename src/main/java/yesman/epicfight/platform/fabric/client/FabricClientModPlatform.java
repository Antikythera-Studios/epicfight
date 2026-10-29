package yesman.epicfight.platform.fabric.client;

import org.jetbrains.annotations.NotNull;
import yesman.epicfight.platform.client.ClientModPlatform;
import yesman.epicfight.platform.client.KeyMappingRegistrar;

public final class FabricClientModPlatform implements ClientModPlatform {

    private final FabricKeyMappingRegistrar keyMappingRegistrar = new FabricKeyMappingRegistrar();

    @Override
    public @NotNull KeyMappingRegistrar keyMappingRegistrar() {
        return keyMappingRegistrar;
    }
}
