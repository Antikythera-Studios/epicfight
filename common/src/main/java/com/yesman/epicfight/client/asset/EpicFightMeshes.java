package com.yesman.epicfight.client.asset;

import com.yesman.akythera.api.asset.AssetAccessor;
import com.yesman.akythera.client.api.mesh.MeshManager;
import com.yesman.epicfight.EpicFight;
import com.yesman.epicfight.client.mesh.HumanoidMesh;

public final class EpicFightMeshes {
    private EpicFightMeshes() {}

    public static final AssetAccessor<HumanoidMesh> HUMANOID = MeshManager.skeletalMesh(EpicFight.MODID, "humanoid", HumanoidMesh::new);
}
