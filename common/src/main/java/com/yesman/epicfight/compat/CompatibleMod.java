package com.yesman.epicfight.compat;

import com.yesman.akythera.client.compat.IClientModPlugin;
import com.yesman.akythera.compat.IModPlugin;
import com.yesman.akythera.core.Akythera;
import com.yesman.epicfight.platform.ModPlatform;
import com.yesman.epicfight.platform.ModPlatformProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.NoSuchElementException;
import java.util.Objects;

/// List of mods with custom compatibility modules.
/// Only includes mods requiring manual registration via IModCompatibility.
/// Mods with official API entry-points (e.g., Shoulder Surfing, Controlify, JEI, KubeJS) are excluded.
public enum CompatibleMod {
    AKYTHERA_LABS("akytheralabs", null, AkytheraLabsPlugin.class),
    ;

    private final @NotNull String modId;
    private final @Nullable Class<? extends IModPlugin> compatibilityModule;
    private final @Nullable Class<? extends IClientModPlugin> clientCompatibilityModule;

    CompatibleMod(
        @NotNull String modId,
        @Nullable Class<? extends IModPlugin> compatibilityModule,
        @Nullable Class<? extends IClientModPlugin> clientCompatibilityModule
    ) {
        this.modId = modId;
        this.compatibilityModule = compatibilityModule;
        this.clientCompatibilityModule = clientCompatibilityModule;

        if (Objects.isNull(compatibilityModule) && Objects.isNull(clientCompatibilityModule)) {
            throw new IllegalStateException("Both common and client modules are null");
        }
    }

    public @NotNull String getModId() {
        return modId;
    }

    public String versionString() throws NoSuchElementException {
        ModPlatform.ModInfo modInfo = ModPlatformProvider.get().getLoadedModInfo(modId);

        if (modInfo == null) {
            throw new NoSuchElementException("Mod " + modId + " doesn't exist.");
        }

        return modInfo.versionString();
    }

    // https://semver.org
    public enum VersionComponent {
        MAJOR(0), MINOR(1), PATCH(2);

        final int index;

        VersionComponent(int index) {
            this.index = index;
        }
    }

    /// Returns the parsed component version value or `null` if parsing fails.
    ///
    /// Example values for version `"2.4.9"`:
    ///
    /// - [VersionComponent#MAJOR] -> `2`
    /// - [VersionComponent#MINOR] -> `4`
    /// - [VersionComponent#PATCH] -> `9`
    ///
    /// Usage example:
    ///
    /// ```java
    /// MinecraftMod mod = MinecraftMod.AZURE_LIB; // Version "3.4.11"
    ///
    /// Integer major = mod.getVersionComponent(VersionComponent.MAJOR); // 3
    /// Integer minor = mod.getVersionComponent(VersionComponent.MINOR); // 4
    /// Integer patch = mod.getVersionComponent(VersionComponent.PATCH); // 11
    ///```
    public @Nullable Integer getVersionComponent(@NotNull VersionComponent component) {
        final String version = versionString();

        try {
            final String[] parts = version.split("\\.");
            return Integer.parseInt(parts[component.index]);
        } catch (Exception e) {
            Akythera.LOGGER.error("Failed to parse the '{}' mod version '{}': {}", name(), version, e.toString());
            return null;
        }
    }

    public @Nullable Class<? extends IModPlugin> getCompatibilityModule() {
        return compatibilityModule;
    }

    public @Nullable Class<? extends IClientModPlugin> getClientCompatibilityModule() {
        return clientCompatibilityModule;
    }
}
