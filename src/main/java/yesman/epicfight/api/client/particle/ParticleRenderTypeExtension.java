package yesman.epicfight.api.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.particle.ParticleRenderType;

/**
 * Optional hook for custom {@link ParticleRenderType}s drawn by Epic Fight's
 * particle loop: {@link #end()} is invoked exactly once per rendered layer, in a
 * finally block after begin/render/upload, so a type can restore framebuffer or
 * pipeline state even when particle rendering or the mesh upload throws.
 */
@Environment(EnvType.CLIENT)
public interface ParticleRenderTypeExtension extends ParticleRenderType {
	void end();
}
