package yesman.epicfight.api.client.model.rebake;

import java.util.List;

import javax.annotation.Nullable;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

public final class TypeProbe implements MultiBufferSource {
	private final List<RenderType> asked = Lists.newArrayList();
	private final int wanted;

	private TypeProbe(int wanted) {
		this.wanted = wanted;
	}

	@Nullable
	public static List<RenderType> askedFor(int wanted, LayerAttachment.AtRest render) {
		TypeProbe probe = new TypeProbe(Math.max(1, wanted));

		try (OffscreenCapture.Scope scope = OffscreenCapture.begin(probe)) {
			render.render(new PoseStack(), probe);
		} catch (Enough enough) {
		} catch (Throwable t) {
			return null;
		}

		return probe.asked;
	}

	@Override
	public VertexConsumer getBuffer(RenderType renderType) {
		if (!this.asked.contains(renderType)) {
			this.asked.add(renderType);
		}

		if (this.asked.size() >= this.wanted) {
			throw ENOUGH;
		}

		return Discard.INSTANCE;
	}

	private static final class Enough extends RuntimeException {
		private static final long serialVersionUID = 1L;

		Enough() {
			super(null, null, false, false);
		}
	}

	private static final Enough ENOUGH = new Enough();

	private static final class Discard implements VertexConsumer {
		static final Discard INSTANCE = new Discard();

		@Override
		public VertexConsumer vertex(double x, double y, double z) {
			return this;
		}

		@Override
		public VertexConsumer color(int red, int green, int blue, int alpha) {
			return this;
		}

		@Override
		public VertexConsumer uv(float u, float v) {
			return this;
		}

		@Override
		public VertexConsumer overlayCoords(int u, int v) {
			return this;
		}

		@Override
		public VertexConsumer uv2(int u, int v) {
			return this;
		}

		@Override
		public VertexConsumer normal(float x, float y, float z) {
			return this;
		}

		@Override
		public void endVertex() {
		}

		@Override
		public void defaultColor(int red, int green, int blue, int alpha) {
		}

		@Override
		public void unsetDefaultColor() {
		}
	}
}
