package yesman.epicfight.api.client.model.rebake;

import java.util.Map;
import java.util.Optional;

import javax.annotation.Nullable;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

public final class CapturingVertexConsumer implements VertexConsumer {
	private final RawMesh out;
	private final int primitiveSize;

	private final int[] ring = new int[4];
	private int ringCount;

	private double px, py, pz;
	private float nx = 0.0F, ny = 1.0F, nz = 0.0F;
	private float u, v;
	private float red = 1.0F, green = 1.0F, blue = 1.0F, alpha = 1.0F;

	private int light;
	private boolean hasVertex;

	CapturingVertexConsumer(RawMesh out, int primitiveSize) {
		this.out = out;
		this.primitiveSize = primitiveSize;
	}

	@Override
	public VertexConsumer vertex(double x, double y, double z) {
		this.px = x;
		this.py = y;
		this.pz = z;
		this.hasVertex = true;
		this.nx = 0.0F;
		this.ny = 0.0F;
		this.nz = 0.0F;
		this.u = 0.0F;
		this.v = 0.0F;
		this.red = 1.0F;
		this.green = 1.0F;
		this.blue = 1.0F;

		return this;
	}

	@Override
	public VertexConsumer color(int red, int green, int blue, int alpha) {
		this.red = red / 255.0F;
		this.green = green / 255.0F;
		this.blue = blue / 255.0F;
		this.alpha = alpha / 255.0F;

		return this;
	}

	@Override
	public VertexConsumer uv(float u, float v) {
		this.u = u;
		this.v = v;

		return this;
	}

	@Override
	public VertexConsumer overlayCoords(int u, int v) {
		return this;
	}

	@Override
	public VertexConsumer uv2(int u, int v) {
		this.light = (v & 0xFFFF) << 16 | (u & 0xFFFF);

		return this;
	}

	@Override
	public VertexConsumer normal(float x, float y, float z) {
		this.nx = x;
		this.ny = y;
		this.nz = z;

		return this;
	}

	@Override
	public void endVertex() {
		if (!this.hasVertex) {
			return;
		}

		this.hasVertex = false;
		this.ring[this.ringCount++] = this.out.addVertex((float) this.px, (float) this.py, (float) this.pz,
			this.nx, this.ny, this.nz, this.u, this.v, this.red, this.green, this.blue, this.alpha,
			this.light != 0 ? 1.0F : 0.0F);

		if (this.ringCount < this.primitiveSize) {
			return;
		}

		if (this.primitiveSize == 4) {
			this.out.addQuad(this.ring[0], this.ring[1], this.ring[2], this.ring[3]);
		} else {
			this.out.addTriangle(this.ring[0], this.ring[1], this.ring[2]);
		}

		this.ringCount = 0;
	}

	@Override
	public void defaultColor(int defaultR, int defaultG, int defaultB, int defaultA) {
	}

	@Override
	public void unsetDefaultColor() {
	}

	public static final class Buffers implements MultiBufferSource {
		private final RawMesh out;
		private final Map<RenderType, CapturingVertexConsumer> consumers = Maps.newIdentityHashMap();

		@Nullable
		private RenderType active;
		private int groupCounter;

		public Buffers(RawMesh out) {
			this.out = out;
		}

		@Override
		public VertexConsumer getBuffer(RenderType renderType) {
			if (renderType != this.active) {
				this.active = renderType;
				ResourceLocation texture = textureOf(renderType);
				RawMesh.Group group = this.out.beginGroup("capture:" + (texture != null ? texture : "untextured") + "#" + (this.groupCounter++));
				group.texture = texture;
				group.renderType = renderType;
			}

			return this.consumers.computeIfAbsent(renderType, type -> new CapturingVertexConsumer(this.out, primitiveSizeOf(type)));
		}

		public void finish() {
			this.out.finish();
		}
	}

	static int primitiveSizeOf(RenderType renderType) {
		return switch (renderType.mode()) {
			case TRIANGLES, TRIANGLE_STRIP, TRIANGLE_FAN -> 3;
			default -> 4;
		};
	}

	@Nullable
	public static ResourceLocation textureOf(RenderType renderType) {
		if (renderType instanceof RenderType.CompositeRenderType composite
				&& composite.state.textureState instanceof RenderStateShard.TextureStateShard shard) {
			Optional<ResourceLocation> texture = shard.texture;

			return texture.orElse(null);
		}

		return null;
	}
}
