package guivnf.epicfight;

import com.mojang.serialization.MapCodec;
import com.yesman.akythera.api.animation.animator.AnimationInterfaceKey;
import com.yesman.akythera.api.data.modularize.ScriptType;
import com.yesman.akythera.api.world.ability.AbilitySlot;
import com.yesman.akythera.api.world.ability.AbilitySlotGroup;
import com.yesman.akythera.api.world.patch.item.WeaponStance;
import com.yesman.akythera.core.animation.FSMAnimationState;
import com.yesman.akythera.core.blueprint.ComponentBlueprint;
import com.yesman.akythera.core.registry.AkytheraRegistries;
import com.yesman.akythera.core.util.EnumerableDataBlockSync;
import com.yesman.akythera.platform.ModPlatform;
import com.yesman.akythera.platform.ModPlatformProvider;
import guivnf.epicfight.akythera.EpicFightComponentBlueprints;
import guivnf.epicfight.akythera.datablocks.EpicfightAbilitySlot;
import guivnf.epicfight.akythera.datablocks.EpicfightAbilitySlotGroup;
import guivnf.epicfight.akythera.datablocks.EpicfightAnimationInterfaceKey;
import guivnf.epicfight.akythera.datablocks.EpicfightFSMAnimationState;
import guivnf.epicfight.akythera.registries.EpicFightScriptTypes;
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
    }

    public static void registerObjects() {
        ModPlatform.Registrar<ScriptType> scriptTypes =
            ModPlatformProvider.get().getRegistrar(AkytheraRegistries.Frozen.SCRIPT_TYPE_KEY, MODID);
        EpicFightScriptTypes.register((name, type) -> scriptTypes.register(name, () -> type));

        ModPlatform.Registrar<MapCodec<? extends ComponentBlueprint<?, ?>>> itemPatchComponents =
            ModPlatformProvider.get().getRegistrar(AkytheraRegistries.Frozen.ITEM_PATCH_COMPONENT_TYPES_KEY, MODID);
        EpicFightComponentBlueprints.registerItemPatchComponents((name, codec) -> itemPatchComponents.register(name, () -> codec));
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
