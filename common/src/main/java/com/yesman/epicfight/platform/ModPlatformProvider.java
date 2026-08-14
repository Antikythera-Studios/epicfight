package com.yesman.epicfight.platform;

import org.jetbrains.annotations.NotNull;

import java.util.ServiceLoader;

/// Provides access to the universal [ModPlatform] instance.
///
/// The implementation is discovered lazily through [ServiceLoader]: each platform jar
/// declares its implementation class in a
/// `META-INF/services/com.yesman.epicfight.platform.ModPlatform` file, and the declaration
/// found on the classpath is instantiated on the first [#get] call.
///
/// Because the lookup is pulled by the first caller instead of pushed from the mod entry
/// point, it does not matter which mod constructs first — the platform is available even
/// before Epic Fight's own entry point has run.
public final class ModPlatformProvider {
    private ModPlatformProvider() {
    }

    /// Initialization-on-demand holder: the JVM class-loads this nested class (and therefore
    /// runs the service lookup) only when [#get] is first called, and guarantees it happens
    /// exactly once even under concurrent access.
    private static final class Holder {
        private static final ModPlatform INSTANCE =
            ServiceLoader.load(ModPlatform.class, ModPlatformProvider.class.getClassLoader())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                    "No ModPlatform implementation found. The platform jar must declare one in META-INF/services/"
                        + ModPlatform.class.getName() + "."
                ));
    }

    public static @NotNull ModPlatform get() {
        return Holder.INSTANCE;
    }
}
