package com.yesman.epicfight.world.item.datablock;

import com.yesman.akythera.core.util.EnumerableDataBlock;
import com.yesman.akythera.core.util.EnumerableDataBlockManager;
import net.minecraft.world.item.Item;

public interface WeaponCategory extends EnumerableDataBlock {
    EnumerableDataBlockManager<WeaponCategory> ENUM_MANAGER = new EnumerableDataBlockManager<>("weapon_category");

    /// An item to shown on skill description screen
    Item iconItem();

    @Override
    default EnumerableDataBlockManager<WeaponCategory> getEnumManager() {
        return ENUM_MANAGER;
    }
}