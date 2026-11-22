package guivnf.epicfight;

import com.yesman.akythera.api.animation.animator.AnimationInterfaceKey;
import com.yesman.akythera.api.world.ability.AbilitySlot;
import com.yesman.akythera.api.world.ability.AbilitySlotGroup;
import com.yesman.akythera.api.world.patch.item.WeaponStance;
import com.yesman.akythera.core.animation.FSMAnimationState;
import com.yesman.akythera.core.util.EnumerableDataBlockSync;
import guivnf.epicfight.akythera.EpicFightComponentBlueprints;
import guivnf.epicfight.akythera.datablocks.EpicfightAbilitySlot;
import guivnf.epicfight.akythera.datablocks.EpicfightAbilitySlotGroup;
import guivnf.epicfight.akythera.datablocks.EpicfightAnimationInterfaceKey;
import guivnf.epicfight.akythera.datablocks.EpicfightFSMAnimationState;
import guivnf.epicfight.akythera.registries.EpicFightAnimNotifyStateTypes;
import guivnf.epicfight.akythera.registries.EpicFightScriptFunctions;
import guivnf.epicfight.akythera.registries.EpicFightScriptTypes;
import guivnf.epicfight.world.ability.Guard;
import guivnf.epicfight.world.item.datablock.EpicFightWeaponCategory;
import guivnf.epicfight.world.item.datablock.EpicFightWeaponStance;
import guivnf.epicfight.world.item.datablock.WeaponCategory;
import net.minecraft.resources.Identifier;

public final class EpicFight {
    public static final String MODID = "epicfight";

    private EpicFight() {}

    public static Identifier identifier(final String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }

    public static void onModConstructed() {
        EnumerableDataBlockSync.addSyncedDataBlock(WeaponCategory.ENUM_MANAGER);
        Guard.registerEvents();
    }

    public static void registerObjects() {
        EpicFightScriptTypes.REGISTRAR.notifyPresence();
        EpicFightScriptFunctions.REGISTRAR.notifyPresence();
        EpicFightComponentBlueprints.ITEM_PATCH_REGISTRAR.notifyPresence();
        EpicFightAnimNotifyStateTypes.REGISTRAR.notifyPresence();
    }

    public static void onModInitialized() {
        WeaponCategory.ENUM_MANAGER.loadEnum();
    }

    public static void registerDataBlockEnumClasses() {
        WeaponStance.ENUM_MANAGER.registerEnumCls(MODID, EpicFightWeaponStance.class);
        WeaponCategory.ENUM_MANAGER.registerEnumCls(MODID, EpicFightWeaponCategory.class);
        FSMAnimationState.ENUM_MANAGER.registerEnumCls(MODID, EpicfightFSMAnimationState.class);
        AnimationInterfaceKey.ENUM_MANAGER.registerEnumCls(MODID, EpicfightAnimationInterfaceKey.class);
        AbilitySlotGroup.ENUM_MANAGER.registerEnumCls(MODID, EpicfightAbilitySlotGroup.class);
        AbilitySlot.ENUM_MANAGER.registerEnumCls(MODID, EpicfightAbilitySlot.class);
    }
}
