package com.yesman.epicfight.client.input;

import com.yesman.epicfight.EpicFight;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public final class EpicFightInputCategories {
    private EpicFightInputCategories() {
    }

    public static final KeyMapping.Category COMBAT = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(EpicFight.MODID, "combat"));
    public static final KeyMapping.Category GUI = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(EpicFight.MODID, "gui"));
    public static final KeyMapping.Category SYSTEM = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(EpicFight.MODID, "system"));
    public static final KeyMapping.Category CAMERA = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(EpicFight.MODID, "camera"));
}
