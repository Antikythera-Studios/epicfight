package guivnf.epicfight.akythera;

import com.mojang.serialization.MapCodec;
import com.yesman.akythera.core.blueprint.ComponentBlueprint;
import com.yesman.akythera.core.registry.AkytheraRegistries;
import com.yesman.akythera.platform.ModPlatform;
import com.yesman.akythera.platform.ModPlatformProvider;
import guivnf.epicfight.EpicFight;
import guivnf.epicfight.world.item.component.EpicFightWeaponProperties;
import net.minecraft.core.Holder;

public final class EpicFightComponentBlueprints {
    private EpicFightComponentBlueprints() {}

    public static final ModPlatform.Registrar<MapCodec<? extends ComponentBlueprint<?, ?>>> ITEM_PATCH_REGISTRAR =
        ModPlatformProvider.get().getRegistrar(AkytheraRegistries.Frozen.ITEM_PATCH_COMPONENT_TYPES_KEY, EpicFight.MODID);

    public static final Holder<MapCodec<? extends ComponentBlueprint<?, ?>>> WEAPON_PROPERTIES =
        ITEM_PATCH_REGISTRAR.register("weapon_properties", () -> EpicFightWeaponProperties.MAP_CODEC);
}
