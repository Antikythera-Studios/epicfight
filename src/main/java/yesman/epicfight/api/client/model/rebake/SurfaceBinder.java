package yesman.epicfight.api.client.model.rebake;

import yesman.epicfight.api.client.model.Meshes;
import yesman.epicfight.api.client.model.SkinnedMesh;
import yesman.epicfight.main.EpicFightMod;

public final class SurfaceBinder {
	private SurfaceBinder() {
	}

	public static void bind(RebakedMesh mesh, RawMesh source) {
		if (!RebakeConfig.SURFACE_BINDING || mesh == null) {
			return;
		}

		try {
			SkinnedMesh body = Meshes.BIPED.get();

			if (body == null) {
				return;
			}

			BodyDeformation.prepare(body);

			if (BodyDeformation.triangleCount() == 0) {
				return;
			}

			SurfaceBinding binding = SurfaceBinding.bind(source, BodyDeformation.restCorners(), BodyDeformation.triangleCount());
			mesh.setBinding(binding);
		} catch (Exception e) {
			EpicFightMod.LOGGER.warn("Surface binding failed, falling back to skinning: {}", e.toString());
		}
	}
}
