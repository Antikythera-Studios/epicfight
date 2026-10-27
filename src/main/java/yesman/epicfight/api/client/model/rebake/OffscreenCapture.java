package yesman.epicfight.api.client.model.rebake;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

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

	public static Scope begin(MultiBufferSource buffers) {
		Redirect previous = active;
		active = new Redirect(buffers);

		return () -> active = previous;
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
