package com.yesman.epicfight.platform.neoforged;

import net.neoforged.fml.loading.FMLLoader;

public final class NeoForgeModChecker {
    private NeoForgeModChecker() {}

    /// A workaround for mixin plugin that injects only when specific mod is loaded,
    /// since their injection is done before initializing any mod loaders.
    ///
    /// For normal cases, use [#isModLoaded]
    public static boolean isModLoaded(final String id) {
        return FMLLoader.getCurrent().getLoadingModList().getModFileById(id) != null;
    }
}
