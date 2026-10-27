package yesman.epicfight.api.client.model.rebake;

import java.io.InputStream;

import javax.annotation.Nullable;

import com.mojang.blaze3d.platform.NativeImage;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import yesman.epicfight.main.EpicFightMod;

public final class TextureCoverage {
	private static final int SAMPLES = 8;

	private final java.util.List<NativeImage> images;

	private TextureCoverage(java.util.List<NativeImage> images) {
		this.images = images;
	}

	@Nullable
	public static TextureCoverage of(@Nullable ResourceLocation texture) {
		return of(texture == null ? java.util.List.of() : java.util.List.of(texture));
	}

	@Nullable
	public static TextureCoverage of(java.util.Collection<ResourceLocation> textures) {
		if (textures.isEmpty() || Minecraft.getInstance().getResourceManager() == null) {
			return null;
		}

		java.util.List<NativeImage> images = new java.util.ArrayList<> ();

		for (ResourceLocation texture : textures) {
			if (texture == null) {
				continue;
			}

			try (InputStream stream = Minecraft.getInstance().getResourceManager().open(texture)) {
				images.add(NativeImage.read(stream));
			} catch (Exception e) {
				EpicFightMod.LOGGER.debug("Could not read {}: {}", texture, e.toString());
			}
		}

		return images.isEmpty() ? null : new TextureCoverage(images);
	}

	public float[] paintedSplit(RawMesh mesh, int firstTriangle, int triangleCount, float boundary) {
		float below = 0.0F;
		float above = 0.0F;

		for (int t = firstTriangle; t < firstTriangle + triangleCount; t++) {
			int a = mesh.triangleVertex(t, 0), b = mesh.triangleVertex(t, 1), c = mesh.triangleVertex(t, 2);

			if (this.isCap(mesh, a, b, c)) {
				continue;
			}

			float area = this.areaOf(mesh, a, b, c);
			int samples = 0;
			int hitsBelow = 0;
			int hitsAbove = 0;

			for (int i = 1; i < SAMPLES; i++) {
				for (int j = 1; i + j < SAMPLES; j++) {
					float wa = (float) i / SAMPLES;
					float wb = (float) j / SAMPLES;
					float wc = 1.0F - wa - wb;
					samples++;

					if (!this.painted(mesh.u(a) * wa + mesh.u(b) * wb + mesh.u(c) * wc,
							mesh.v(a) * wa + mesh.v(b) * wb + mesh.v(c) * wc)) {
						continue;
					}

					if (mesh.py(a) * wa + mesh.py(b) * wb + mesh.py(c) * wc < boundary) {
						hitsBelow++;
					} else {
						hitsAbove++;
					}
				}
			}

			if (samples > 0) {
				below += area * hitsBelow / samples;
				above += area * hitsAbove / samples;
			}
		}

		return new float[] { below, above };
	}

	private float areaOf(RawMesh mesh, int a, int b, int c) {
		float ux = mesh.px(b) - mesh.px(a), uy = mesh.py(b) - mesh.py(a), uz = mesh.pz(b) - mesh.pz(a);
		float vx = mesh.px(c) - mesh.px(a), vy = mesh.py(c) - mesh.py(a), vz = mesh.pz(c) - mesh.pz(a);
		float nx = uy * vz - uz * vy, ny = uz * vx - ux * vz, nz = ux * vy - uy * vx;

		return 0.5F * (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
	}

	private boolean isCap(RawMesh mesh, int a, int b, int c) {
		float ux = mesh.px(b) - mesh.px(a), uy = mesh.py(b) - mesh.py(a), uz = mesh.pz(b) - mesh.pz(a);
		float vx = mesh.px(c) - mesh.px(a), vy = mesh.py(c) - mesh.py(a), vz = mesh.pz(c) - mesh.pz(a);
		float nx = uy * vz - uz * vy, ny = uz * vx - ux * vz, nz = ux * vy - uy * vx;

		return Math.abs(ny) > Math.abs(nx) && Math.abs(ny) > Math.abs(nz);
	}

	public interface PaintedSample {
		void at(float x, float y, float z);
	}

	public void forEachPaintedSample(RawMesh mesh, int firstTriangle, int triangleCount, PaintedSample consumer) {
		for (int t = firstTriangle; t < firstTriangle + triangleCount; t++) {
			int a = mesh.triangleVertex(t, 0), b = mesh.triangleVertex(t, 1), c = mesh.triangleVertex(t, 2);

			for (int i = 1; i < SAMPLES; i++) {
				for (int j = 1; i + j < SAMPLES; j++) {
					float wa = (float) i / SAMPLES;
					float wb = (float) j / SAMPLES;
					float wc = 1.0F - wa - wb;

					if (this.painted(mesh.u(a) * wa + mesh.u(b) * wb + mesh.u(c) * wc,
							mesh.v(a) * wa + mesh.v(b) * wb + mesh.v(c) * wc)) {
						consumer.at(mesh.px(a) * wa + mesh.px(b) * wb + mesh.px(c) * wc,
							mesh.py(a) * wa + mesh.py(b) * wb + mesh.py(c) * wc,
							mesh.pz(a) * wa + mesh.pz(b) * wb + mesh.pz(c) * wc);
					}
				}
			}
		}
	}

	private boolean painted(float u, float v) {
		for (NativeImage image : this.images) {
			int x = (int) Math.floor(u * image.getWidth());
			int y = (int) Math.floor(v * image.getHeight());

			if (x < 0 || y < 0 || x >= image.getWidth() || y >= image.getHeight()) {
				continue;
			}

			if ((image.getPixelRGBA(x, y) >>> 24) != 0) {
				return true;
			}
		}

		return false;
	}

	public void close() {
		this.images.forEach(NativeImage::close);
	}
}
