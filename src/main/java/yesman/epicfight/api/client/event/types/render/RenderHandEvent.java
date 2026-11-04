package yesman.epicfight.api.client.event.types.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.player.LocalPlayer;
import yesman.epicfight.api.event.CancelableEvent;
import yesman.epicfight.api.event.Event;

/// Fired before [net.minecraft.client.renderer.ItemInHandRenderer#renderHandsWithItems].
///
/// Cancels the vanilla first-person hand render so Epic Fight's animated first-person model can take over.
/// Replaces NeoForge's [RenderHandEvent].
public class RenderHandEvent extends Event implements CancelableEvent {
	private final LocalPlayer player;
	private final float partialTick;
	private final PoseStack poseStack;
	private final MultiBufferSource bufferSource;
	private final int packedLight;

	public RenderHandEvent(LocalPlayer player, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
		this.player = player;
		this.partialTick = partialTick;
		this.poseStack = poseStack;
		this.bufferSource = bufferSource;
		this.packedLight = packedLight;
	}

	public LocalPlayer getPlayer() {
		return player;
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
