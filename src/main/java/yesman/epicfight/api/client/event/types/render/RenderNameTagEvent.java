package yesman.epicfight.api.client.event.types.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import yesman.epicfight.api.event.Event;

/// Replaces NeoForge's [RenderNameTagEvent].
/// Allows mods to cancel or modify name tag rendering.
public class RenderNameTagEvent extends Event {
	public enum Result {
		DEFAULT,
		ALWAYS_RENDER,
		DENY
	}

	private final Entity entity;
	private Component content;
	private final EntityRenderer<?> renderer;
	private final PoseStack poseStack;
	private final MultiBufferSource bufferSource;
	private final int packedLight;
	private final float partialTick;
	private Result result = Result.DEFAULT;

	public RenderNameTagEvent(Entity entity, Component content, EntityRenderer<?> renderer, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, float partialTick) {
		this.entity = entity;
		this.content = content;
		this.renderer = renderer;
		this.poseStack = poseStack;
		this.bufferSource = bufferSource;
		this.packedLight = packedLight;
		this.partialTick = partialTick;
	}

	public Entity getEntity() {
		return entity;
	}

	public Component getContent() {
		return content;
	}

	public void setContent(Component content) {
		this.content = content;
	}

	public EntityRenderer<?> getRenderer() {
		return renderer;
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

	public float getPartialTick() {
		return partialTick;
	}

	public Result getResult() {
		return result;
	}

	public void setResult(Result result) {
		this.result = result;
	}
}
