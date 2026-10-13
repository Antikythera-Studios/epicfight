package guivnf.epicfight.world.item.datablock;

import com.yesman.akythera.api.world.patch.item.WeaponStance;

public enum EpicFightWeaponStance implements WeaponStance {
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
