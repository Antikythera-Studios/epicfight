package com.yesman.epicfight.world.item.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.yesman.akythera.core.util.EnumerableDataBlock;
import com.yesman.epicfight.world.item.datablock.WeaponCategory;
import com.yesman.epicfight.world.item.datablock.WeaponStance;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;

import java.util.List;
import java.util.Set;

/// Properties of a weapon that is determined by the current [WeaponStance]
///
/// @param weaponCategories    Weapon categorizing tag run by [EnumerableDataBlock].
/// @param swingSound          A sound being played when any attack animation played equipping this item.
/// @param hitSound            A sound being played when hurt any entities.
/// @param hitParticle         A particle being played when hurt any entities.
/// @param equippableInOffhand Whether the item can be put on offhand
public record EpicFightWeaponProperties(
    Set<WeaponCategory> weaponCategories,
    WeaponAttackMotions comboAttacks,
    Holder<SoundEvent> swingSound,
    Holder<SoundEvent> hitSound,
    ParticleType<?> hitParticle,
    boolean equippableInOffhand
) {
    public static final Codec<EpicFightWeaponProperties> CODEC = RecordCodecBuilder.create(
        instance ->
            instance.group(
                WeaponCategory.ENUM_MANAGER.getNameCodec().listOf().fieldOf("categories").forGetter(EpicFightWeaponProperties::weaponCategoriesList),
                WeaponAttackMotions.CODEC.fieldOf("combo_attacks").forGetter(EpicFightWeaponProperties::comboAttacks),
                SoundEvent.CODEC.fieldOf("swing_sound").forGetter(EpicFightWeaponProperties::swingSound),
                SoundEvent.CODEC.fieldOf("hit_sound").forGetter(EpicFightWeaponProperties::hitSound),
                BuiltInRegistries.PARTICLE_TYPE.byNameCodec().fieldOf("hit_particle").forGetter(EpicFightWeaponProperties::hitParticle),
                Codec.BOOL.fieldOf("offhand_equippable").forGetter(EpicFightWeaponProperties::equippableInOffhand)
            )
            .apply(instance, EpicFightWeaponProperties::new)
    );

    // For serialization(List -> Set)
    private EpicFightWeaponProperties(
        List<WeaponCategory> weaponCategory,
        WeaponAttackMotions comboAttacks,
        Holder<SoundEvent> swingSound,
        Holder<SoundEvent> hitSound,
        ParticleType<?> hitParticle,
        boolean equippableInOffhand
    ) {
        this(Set.copyOf(weaponCategory), comboAttacks, swingSound, hitSound, hitParticle, equippableInOffhand);
    }

    // For serialization(Set -> List)
    private List<WeaponCategory> weaponCategoriesList() {
        return List.copyOf(weaponCategories);
    }
}
