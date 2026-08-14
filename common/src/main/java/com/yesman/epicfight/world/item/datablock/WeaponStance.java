package com.yesman.epicfight.world.item.datablock;

import com.yesman.akythera.api.world.patch.item.ItemPatch;
import com.yesman.akythera.core.util.EnumerableDataBlock;
import com.yesman.akythera.core.util.EnumerableDataBlockManager;

/// Separation identifier of [ItemPatch]. Notifies how the player handle weapons by various
/// conditions like item in offhand, skill activation.
public interface WeaponStance extends EnumerableDataBlock {
	EnumerableDataBlockManager<WeaponStance> ENUM_MANAGER = new EnumerableDataBlockManager<>("weapon_stance");

    @Override
    default EnumerableDataBlockManager<WeaponStance> getEnumManager() {
        return ENUM_MANAGER;
    }

    /// Returns whether players can use items in offhand while in the stance
	boolean isOffhandFree();
}