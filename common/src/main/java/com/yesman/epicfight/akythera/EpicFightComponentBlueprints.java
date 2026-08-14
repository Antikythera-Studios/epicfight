package com.yesman.epicfight.akythera;

import com.mojang.serialization.MapCodec;
import com.yesman.akythera.core.blueprint.ComponentBlueprint;
import com.yesman.akythera.core.registry.AkytheraRegistries;
import com.yesman.akythera.platform.ModPlatform;
import com.yesman.akythera.platform.ModPlatformProvider;
import com.yesman.epicfight.EpicFight;
import com.yesman.epicfight.world.item.component.WeaponStanceSystem;
import net.minecraft.core.Holder;

public final class EpicFightComponentBlueprints {
    private EpicFightComponentBlueprints() {}

    public static final ModPlatform.Registrar<MapCodec<? extends ComponentBlueprint<?, ?>>> REGISTRAR =
        ModPlatformProvider.get().getRegistrar(AkytheraRegistries.Frozen.COMPONENT_BLUEPRINT_TYPES_KEY, EpicFight.MODID);

    // ********************* Entity patch components *********************


    // ********************* Item patch components *********************
    public static final Holder<MapCodec<? extends ComponentBlueprint<?, ?>>> WEAPON_STANCE_SYSTEM =
        REGISTRAR.register("weapon_stance_system", () -> WeaponStanceSystem.CODEC);
}
