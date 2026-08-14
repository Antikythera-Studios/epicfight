package com.yesman.epicfight.world.item.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.yesman.akythera.api.component.PatchComponent;
import com.yesman.akythera.api.data.modularize.node.PureNode;
import com.yesman.akythera.api.data.modularize.node.ScriptNode;
import com.yesman.akythera.api.world.patch.entity.EntityPatch;
import com.yesman.akythera.core.blueprint.ComponentBlueprint;
import com.yesman.akythera.core.blueprint.script.ScriptVariables;
import com.yesman.epicfight.world.item.datablock.WeaponStance;
import net.minecraft.world.entity.LivingEntity;

import java.util.Map;

/// A series of combo attack animation provider based on the owner's state.
///
/// e.g. Equipper is in one-handed stance with a sword in default. While
///      switching to a two=handed stance when equipping another weapon
///      tagged as "sword" in offhand.
///
/// @param conditionalStances (stance, bool function) pair that defines when the stance establishes
/// @param defaultStance      A stance when no conditional stance meets the current state of equipper
/// @param weaponProperties   A set of data under specific stance
public record WeaponStanceSystem(
    Map<WeaponStance, PureNode> conditionalStances,
    WeaponStance defaultStance,
    Map<WeaponStance, EpicFightWeaponProperties> weaponProperties
) implements PatchComponent, ComponentBlueprint<WeaponStanceSystem, Void> {
    public static final MapCodec<WeaponStanceSystem> CODEC = RecordCodecBuilder.mapCodec(
        instance ->
            instance.group(
                Codec.unboundedMap(WeaponStance.ENUM_MANAGER.getNameCodec(), PureNode.CODEC)
                    .fieldOf("conditional_stances").forGetter(WeaponStanceSystem::conditionalStances),
                WeaponStance.ENUM_MANAGER.getNameCodec()
                    .fieldOf("default_stance").forGetter(WeaponStanceSystem::defaultStance),
                Codec.unboundedMap(WeaponStance.ENUM_MANAGER.getNameCodec(), EpicFightWeaponProperties.CODEC)
                    .fieldOf("properties").forGetter(WeaponStanceSystem::weaponProperties)
            )
            .apply(instance, WeaponStanceSystem::new)
    );

    /// Returns the current stance of item equipper
    public WeaponStance getCurrentStance(final EntityPatch<LivingEntity> equipper) {
        ScriptVariables variables = new ScriptVariables();
        variables.setValue("equipper", equipper);

        for (Map.Entry<WeaponStance, PureNode> entry : conditionalStances.entrySet()) {
            if (entry.getValue().execute(null, new ScriptNode.ExecutionContext(variables))) {
                return entry.getKey();
            }
        }

        return defaultStance;
    }

    // Self-sustaining blueprint
    @Override
    public WeaponStanceSystem instantiate(final Void blueprintOwner) {
        return this;
    }

    @Override
    public MapCodec<? extends ComponentBlueprint<WeaponStanceSystem, Void>> mapCodec() {
        return CODEC;
    }
}
