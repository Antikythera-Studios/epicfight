package guivnf.epicfight.akythera;

import com.mojang.serialization.MapCodec;
import com.yesman.akythera.core.blueprint.ComponentBlueprint;
import guivnf.epicfight.world.item.component.EpicFightWeaponProperties;

import java.util.function.BiConsumer;

public final class EpicFightComponentBlueprints {
    private EpicFightComponentBlueprints() {}

    public static void registerItemPatchComponents(final BiConsumer<String, MapCodec<? extends ComponentBlueprint<?, ?>>> registry) {
        registry.accept("weapon_properties", EpicFightWeaponProperties.MAP_CODEC);
    }
}
