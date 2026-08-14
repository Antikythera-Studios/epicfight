package com.yesman.epicfight.asset.armature;

import com.yesman.akythera.api.asset.AssetAccessor;
import com.yesman.akythera.api.asset.armature.ArmatureManager;
import com.yesman.epicfight.EpicFight;

public final class EpicFightArmatures {
    private EpicFightArmatures() {}

    public static final AssetAccessor<HumanoidArmature> HUMANOID = ArmatureManager.accessor(EpicFight.MODID, "humanoid", HumanoidArmature::new);
}
