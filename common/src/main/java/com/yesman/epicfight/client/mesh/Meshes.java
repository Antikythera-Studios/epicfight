package com.yesman.epicfight.client.mesh;

import com.yesman.akythera.api.asset.AssetAccessor;
import com.yesman.akythera.client.api.mesh.MeshManager;
import com.yesman.epicfight.EpicFight;

public final class Meshes {
    private Meshes() {}

    public static final AssetAccessor<HumanoidMesh> HUMANOID = MeshManager.skeletalMesh(EpicFight.MODID, "entity/humanoid", HumanoidMesh::new);
}
