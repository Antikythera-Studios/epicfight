package yesman.epicfight.platform.neoforged.client.event;

import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.server.packs.resources.ResourceProvider;
import java.util.function.Consumer;

/// Stub for NeoForge's RegisterShadersEvent.
public class RegisterShadersEvent {
    private final ResourceProvider resourceProvider;

    public RegisterShadersEvent(ResourceProvider resourceProvider) {
        this.resourceProvider = resourceProvider;
    }

    public ResourceProvider getResourceProvider() {
        return resourceProvider;
    }

    public void registerShader(ShaderInstance shader, Consumer<ShaderInstance> callback) {}
    public void registerShader(ShaderInstance shader) {}
}
