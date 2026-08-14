package com.yesman.epicfight.client.core;

import com.yesman.akythera.api.lifecycle.ComponentsFactory;
import com.yesman.akythera.client.compat.IClientModPlugin;
import com.yesman.akythera.client.gui.widget.common.WidgetTheme;
import com.yesman.akythera.core.Akythera;
import com.yesman.akythera.platform.ModPlatformProvider;
import com.yesman.epicfight.compat.CompatibleMod;
import net.minecraft.client.Minecraft;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Objects;

/// Common functionalities shared between the platform client entry points.
public final class EpicFightClient {
    private EpicFightClient() {
    }

    /// Called on each mod loader's mod entry point class's constructor
    public static void onModConstructed() {
        // TODO: Register client-side EnumerableDataBlock used by Epic Fight here
    }

    /// Called on mod loader lifecycle when the mod initializes
    public static void onModInitialized() {
        Arrays.stream(CompatibleMod.values())
            .filter(mod -> ModPlatformProvider.get().isModLoaded(mod.getModId()) && mod.getClientCompatibilityModule() != null)
            .map(mod -> {
                try {
                    return mod.getClientCompatibilityModule().getDeclaredConstructor().newInstance();
                } catch (
                    InstantiationException |
                    IllegalAccessException |
                    InvocationTargetException |
                    NoSuchMethodException e
                ) {
                    Akythera.LOGGER.error("Can't create an instance of client compatibility module {}. Skipped.", mod.getModId(), e);
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .forEach(IClientModPlugin::onInitializeClient);

        //ComputeShaderProvider.checkIfSupports();
        //InputAction.ENUM_MANAGER.loadEnum();
        WidgetTheme.ENUM_MANAGER.loadEnum();
    }
}
