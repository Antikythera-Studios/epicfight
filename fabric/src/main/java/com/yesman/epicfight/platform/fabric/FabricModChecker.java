package com.yesman.epicfight.platform.fabric;

import net.fabricmc.loader.api.FabricLoader;

public final class FabricModChecker {
    private FabricModChecker() {}

    /// A workaround for mixin plugin that injects only when specific mod is loaded,
    /// since their injection is done before initializing any mod loaders.
    ///
    /// For normal cases, use [#isModLoaded]
    public static boolean isModLoaded(final String id) {
        return FabricLoader.getInstance().isModLoaded(id);
    }
}
