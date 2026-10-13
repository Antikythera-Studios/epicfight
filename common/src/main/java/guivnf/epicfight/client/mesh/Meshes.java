package guivnf.epicfight.client.mesh;

import com.yesman.akythera.api.asset.AssetAccessor;
import com.yesman.akythera.client.api.mesh.MeshManager;
import guivnf.epicfight.EpicFight;

public final class Meshes {
    private Meshes() {}

    public static final AssetAccessor<HumanoidMesh> HUMANOID = MeshManager.skeletalMesh(EpicFight.MODID, "entity/humanoid", HumanoidMesh::new);
}
