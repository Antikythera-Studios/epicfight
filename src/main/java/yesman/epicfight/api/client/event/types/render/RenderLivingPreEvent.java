package yesman.epicfight.api.client.event.types.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import yesman.epicfight.api.event.CancelableEvent;
import yesman.epicfight.api.event.Event;

/// Fired before [net.minecraft.client.renderer.entity.LivingEntityRenderer#render].
///
/// Cancels the vanilla render when a patched renderer exists for the entity.
/// Replaces NeoForge's [RenderLivingEvent.Pre].
public class RenderLivingPreEvent extends Event implements CancelableEvent {
	private final LivingEntity entity;
	private final EntityRenderer<?> renderer;
	private final float partialTick;
	private final PoseStack poseStack;
	private final MultiBufferSource bufferSource;
	private final int packedLight;

	public RenderLivingPreEvent(LivingEntity entity, EntityRenderer<?> renderer, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
		this.entity = entity;
		this.renderer = renderer;
		this.partialTick = partialTick;
		this.poseStack = poseStack;
		this.bufferSource = bufferSource;
		this.packedLight = packedLight;
	}

	public LivingEntity getEntity() {
		return entity;
	}

	public EntityRenderer<?> getRenderer() {
		return renderer;
	}

	public float getPartialTick() {
		return partialTick;
	}

	public PoseStack getPoseStack() {
		return poseStack;
	}

	public MultiBufferSource getBufferSource() {
		return bufferSource;
	}

	public int getPackedLight() {
		return packedLight;
	}
}
