package guivnf.epicfight;

import com.yesman.akythera.api.world.patch.item.WeaponStance;
import com.yesman.akythera.core.animation.FSMAnimationState;
import guivnf.epicfight.akythera.datablocks.EpicfightFSMAnimationState;
import guivnf.epicfight.world.item.datablock.EpicFightWeaponStance;

public final class EpicFight {
    public static final String MODID = "epicfight";

    private EpicFight() {}

    public static void registerDataBlockEnumClasses() {
        WeaponStance.ENUM_MANAGER.registerEnumCls(MODID, EpicFightWeaponStance.class);
        FSMAnimationState.ENUM_MANAGER.registerEnumCls(MODID, EpicfightFSMAnimationState.class);
    }
}
