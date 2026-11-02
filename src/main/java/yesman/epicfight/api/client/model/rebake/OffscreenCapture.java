package yesman.epicfight.api.client.model.rebake;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.RenderType;
import yesman.epicfight.mixin.client.AccessorRenderBuffersHolder;

public final class OffscreenCapture {
	private OffscreenCapture() {
	}

	@Nullable
	private static Redirect active;

	public static boolean isActive() {
		return active != null;
	}

	@Nullable
	public static MultiBufferSource.BufferSource redirect() {
		return active;
	}

	@Nullable
	private static RenderBuffers captureBuffers;

	public static Scope begin(MultiBufferSource buffers) {
		Redirect previous = active;
		active = new Redirect(buffers);

		Minecraft minecraft = Minecraft.getInstance();
		LevelRenderer levelRenderer = minecraft.levelRenderer;

		if (captureBuffers == null) {
			captureBuffers = new RenderBuffers();
		}

		AccessorRenderBuffersHolder.OfMinecraft client = (AccessorRenderBuffersHolder.OfMinecraft) minecraft;
		AccessorRenderBuffersHolder.OfLevelRenderer level =
			(AccessorRenderBuffersHolder.OfLevelRenderer) levelRenderer;
		RenderBuffers savedClient = client.epicfight$getRenderBuffers();
		RenderBuffers savedLevel = level.epicfight$getRenderBuffers();

		client.epicfight$setRenderBuffers(captureBuffers);
		level.epicfight$setRenderBuffers(captureBuffers);

		return () -> {
			client.epicfight$setRenderBuffers(savedClient);
			level.epicfight$setRenderBuffers(savedLevel);
			active = previous;
		};
	}

	public interface Scope extends AutoCloseable {
		@Override
		void close();
	}

	private static final class Redirect extends MultiBufferSource.BufferSource {
		private final MultiBufferSource buffers;

		Redirect(MultiBufferSource buffers) {
			super(new BufferBuilder(256), ImmutableMap.of());

			this.buffers = buffers;
		}

		@Override
		public VertexConsumer getBuffer(RenderType renderType) {
			return this.buffers.getBuffer(renderType);
		}

		@Override
		public void endBatch() {
		}

		@Override
		public void endBatch(RenderType renderType) {
		}
	}
}
