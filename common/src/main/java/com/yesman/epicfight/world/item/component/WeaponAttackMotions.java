package com.yesman.epicfight.world.item.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.yesman.akythera.core.animation.playable.AnimationMontage;
import com.yesman.akythera.core.registry.AkytheraRegistries;
import net.minecraft.resources.ResourceKey;

import java.util.List;

public record WeaponAttackMotions(
    List<ResourceKey<AnimationMontage>> comboAttacks,  // Sequential combo animationsToReplace played in order
    ResourceKey<AnimationMontage> dash,                // Attack animation played while sprinting
    ResourceKey<AnimationMontage> airSlash             // Attack animation played while jumping
) {
    public static final Codec<WeaponAttackMotions> CODEC = RecordCodecBuilder.create(
        instance ->
            instance.group(
                ResourceKey.codec(AkytheraRegistries.DataPack.ANIM_MONTAGE_KEY).listOf().fieldOf("combo_attacks").forGetter(WeaponAttackMotions::comboAttacks),
                ResourceKey.codec(AkytheraRegistries.DataPack.ANIM_MONTAGE_KEY).fieldOf("dash").forGetter(WeaponAttackMotions::dash),
                ResourceKey.codec(AkytheraRegistries.DataPack.ANIM_MONTAGE_KEY).fieldOf("air_slash").forGetter(WeaponAttackMotions::airSlash)
            )
            .apply(instance, WeaponAttackMotions::new)
    );
}
