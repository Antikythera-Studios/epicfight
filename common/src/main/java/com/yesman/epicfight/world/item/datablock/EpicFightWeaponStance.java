package com.yesman.epicfight.world.item.datablock;

public enum EpicFightWeaponStance implements WeaponStance {
    /// A *wildcard* weapon stance to get a root itempatch properties
    COMMON(true),
    ONE_HAND(true),
    TWO_HAND(false),
    MOUNT(true),
    RANGED(false),
    SHEATH(false),
    OCHS(false);

    final boolean isOffhandFree;
    final int id;

    EpicFightWeaponStance(boolean isOffhandFree) {
        this.isOffhandFree = isOffhandFree;
        this.id = ENUM_MANAGER.assign(this);
    }

    @Override
    public int universalOrdinal() {
        return this.id;
    }

    @Override
    public boolean isOffhandFree() {
        return this.isOffhandFree;
    }
}
