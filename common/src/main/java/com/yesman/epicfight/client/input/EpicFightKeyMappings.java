package com.yesman.epicfight.client.input;

import net.minecraft.client.KeyMapping;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class EpicFightKeyMappings {
    private EpicFightKeyMappings() {}

    private static final List<@NonNull KeyMapping> MODDED_KEYS = new ArrayList<>();

    private static @NonNull KeyMapping registerKey(@NonNull KeyMapping keyMapping) {
        MODDED_KEYS.add(keyMapping);
        return keyMapping;
    }

    /// Register modded keys via mod loader lifecycle hooks since vanilla minecraft code
    /// doesn't provide canonical way to register modded key mappings
    public static List<@NonNull KeyMapping> getModdedKeys() {
        return Collections.unmodifiableList(MODDED_KEYS);
    }
}
