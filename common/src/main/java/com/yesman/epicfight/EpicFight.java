package com.yesman.epicfight;

import com.mojang.brigadier.CommandDispatcher;
import com.yesman.akythera.api.animation.animator.AnimationInterfaceKey;
import com.yesman.akythera.compat.IModPlugin;
import com.yesman.akythera.core.Akythera;
import com.yesman.akythera.core.animation.driver.FSMAnimationState;
import com.yesman.akythera.api.animation.impl.AkytheraAnimationState;
import com.yesman.epicfight.akythera.EpicFightComponentBlueprints;
import com.yesman.epicfight.akythera.animation.EpicFightAnimationInterfaceKey;
import com.yesman.epicfight.akythera.animation.EpicFightAnimationState;
import com.yesman.epicfight.akythera.registries.EpicFightScriptTypes;
import com.yesman.epicfight.compat.CompatibleMod;
import com.yesman.epicfight.platform.ModPlatformProvider;
import com.yesman.epicfight.world.item.datablock.EpicFightWeaponCategory;
import com.yesman.epicfight.world.item.datablock.EpicFightWeaponStance;
import com.yesman.epicfight.world.item.datablock.WeaponCategory;
import com.yesman.epicfight.world.item.datablock.WeaponStance;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Objects;

public class EpicFight {
    public static final String MODID = "epicfight";

    /// Creates an identifier that points to an Epic Fight resource.
    public static @NonNull Identifier identifier(@NonNull final String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }

    /// Called on each mod loader's mod entry point class's constructor
    public static void onModConstructed() {
        EpicFightScriptTypes.REGISTRAR.notifyPresence();
        EpicFightComponentBlueprints.REGISTRAR.notifyPresence();
    }

    /// Called on mod loader lifecycle when the mod initializes (both physical sides)
    public static void onModInitialized() {
        Arrays.stream(CompatibleMod.values())
            .filter(mod -> ModPlatformProvider.get().isModLoaded(mod.getModId()) && mod.getCompatibilityModule() != null)
            .map(mod -> {
                try {
                    return mod.getCompatibilityModule().getDeclaredConstructor().newInstance();
                } catch (
                    InstantiationException |
                    IllegalAccessException |
                    InvocationTargetException |
                    NoSuchMethodException e
                ) {
                    Akythera.LOGGER.error("Can't create an instance of compatibility module {}. Skipped.", mod.getModId(), e);
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .forEach(IModPlugin::onInitialize);

        WeaponStance.ENUM_MANAGER.loadEnum();
        WeaponCategory.ENUM_MANAGER.loadEnum();
    }

    public static void registerDataBlockEnumClasses() {
        AnimationInterfaceKey.ENUM_MANAGER.registerEnumCls(MODID, EpicFightAnimationInterfaceKey.class);
        FSMAnimationState.ENUM_MANAGER.registerEnumCls(MODID, EpicFightAnimationState.class);
        WeaponStance.ENUM_MANAGER.registerEnumCls(MODID, EpicFightWeaponStance.class);
        WeaponCategory.ENUM_MANAGER.registerEnumCls(MODID, EpicFightWeaponCategory.class);
    }

    /// Called on mod loader lifecycle when the mod initializes in a dedicated server
    public static void onModInitializedInDedicatedServer() {
    }

    /// Register in-game commands
    public static void onRegisterCommands(final CommandDispatcher<CommandSourceStack> dispatcher) {
    }
}
