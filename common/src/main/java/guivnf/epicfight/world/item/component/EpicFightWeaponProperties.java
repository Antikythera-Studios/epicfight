package guivnf.epicfight.world.item.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.yesman.akythera.api.component.PatchComponent;
import com.yesman.akythera.api.component.UniqueComponent;
import com.yesman.akythera.api.world.patch.item.ItemPatch;
import com.yesman.akythera.api.world.patch.item.WeaponStance;
import com.yesman.akythera.api.world.patch.item.component.WeaponStanceVariationAdapter;
import com.yesman.akythera.core.animation.playable.AnimationMontage;
import com.yesman.akythera.core.blueprint.ComponentBlueprint;
import com.yesman.akythera.core.registry.AkytheraRegistries;
import com.yesman.akythera.core.util.EnumerableDataBlock;
import guivnf.epicfight.world.item.datablock.WeaponCategory;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/// Properties of a weapon that is determined by the current [WeaponStance]
///
/// Attached under the [WeaponStanceVariationAdapter] whose stance it describes, or straight onto the
/// [ItemPatch] — where it is the item's **default**, in force whenever no adapter's condition holds.
/// Unique per host, so either answers `getComponent(EpicFightWeaponProperties.class)`.
///
/// Every property is optional and overrides on its own: a block declaring only [#swingSound] takes
/// the rest from whatever block it overrides, see [#overwrite]. An empty [#weaponCategories] is a
/// declaration — the item has no categories — where an absent one defers.
///
/// Simultaneously a component and a blueprint: [#instantiate] copies the authored block per host, so
/// each host owns its own switch.
public final class EpicFightWeaponProperties implements UniqueComponent, ComponentBlueprint<EpicFightWeaponProperties, ItemPatch> {
    public static final MapCodec<EpicFightWeaponProperties> MAP_CODEC = RecordCodecBuilder.mapCodec(
        instance ->
            instance.group(
                WeaponCategory.ENUM_MANAGER.getNameCodec().listOf().<Set<WeaponCategory>>xmap(Set::copyOf, List::copyOf)
                    .optionalFieldOf("categories").forGetter(EpicFightWeaponProperties::weaponCategories),
                ResourceKey.codec(AkytheraRegistries.DataPack.ANIM_MONTAGE_KEY).listOf().optionalFieldOf("combo_attacks").forGetter(EpicFightWeaponProperties::comboAttacks),
                ResourceKey.codec(AkytheraRegistries.DataPack.ANIM_MONTAGE_KEY).optionalFieldOf("dash").forGetter(EpicFightWeaponProperties::dash),
                ResourceKey.codec(AkytheraRegistries.DataPack.ANIM_MONTAGE_KEY).optionalFieldOf("air_slash").forGetter(EpicFightWeaponProperties::airSlash),
                SoundEvent.CODEC.optionalFieldOf("swing_sound").forGetter(EpicFightWeaponProperties::swingSound),
                SoundEvent.CODEC.optionalFieldOf("hit_sound").forGetter(EpicFightWeaponProperties::hitSound),
                BuiltInRegistries.PARTICLE_TYPE.byNameCodec().optionalFieldOf("hit_particle").forGetter(EpicFightWeaponProperties::hitParticle),
                Codec.BOOL.optionalFieldOf("offhand_equippable").forGetter(EpicFightWeaponProperties::equippableInOffhand),
                ENABLED_CODEC.forGetter(EpicFightWeaponProperties::enabled)
            )
            .apply(instance, EpicFightWeaponProperties::new)
    );

    public static final Codec<EpicFightWeaponProperties> CODEC = MAP_CODEC.codec();

    /// Weapon categorizing tag run by [EnumerableDataBlock].
    private final Optional<Set<WeaponCategory>> weaponCategories;

    /// Sequential combo animations played in order.
    private final Optional<List<ResourceKey<AnimationMontage>>> comboAttacks;

    /// Attack animation played while sprinting.
    private final Optional<ResourceKey<AnimationMontage>> dash;

    /// Attack animation played while jumping.
    private final Optional<ResourceKey<AnimationMontage>> airSlash;

    /// A sound being played when any attack animation played equipping this item.
    private final Optional<Holder<SoundEvent>> swingSound;

    /// A sound being played when hurt any entities.
    private final Optional<Holder<SoundEvent>> hitSound;

    /// A particle being played when hurt any entities.
    private final Optional<ParticleType<?>> hitParticle;

    /// Whether the item can be put on offhand
    private final Optional<Boolean> equippableInOffhand;

    /// Whether this component takes part, see [PatchComponent#isEnabled]. On the authored block, the
    /// state every copy starts in.
    private boolean enabled;

    public EpicFightWeaponProperties(
        final Optional<Set<WeaponCategory>> weaponCategories,
        final Optional<List<ResourceKey<AnimationMontage>>> comboAttacks,
        final Optional<ResourceKey<AnimationMontage>> dash,
        final Optional<ResourceKey<AnimationMontage>> airSlash,
        final Optional<Holder<SoundEvent>> swingSound,
        final Optional<Holder<SoundEvent>> hitSound,
        final Optional<ParticleType<?>> hitParticle,
        final Optional<Boolean> equippableInOffhand,
        final boolean enabled
    ) {
        this.weaponCategories = weaponCategories;
        this.comboAttacks = comboAttacks;
        this.dash = dash;
        this.airSlash = airSlash;
        this.swingSound = swingSound;
        this.hitSound = hitSound;
        this.hitParticle = hitParticle;
        this.equippableInOffhand = equippableInOffhand;
        this.enabled = enabled;
    }

    public Optional<Set<WeaponCategory>> weaponCategories() {
        return weaponCategories;
    }

    public Optional<List<ResourceKey<AnimationMontage>>> comboAttacks() {
        return comboAttacks;
    }

    public Optional<ResourceKey<AnimationMontage>> dash() {
        return dash;
    }

    public Optional<ResourceKey<AnimationMontage>> airSlash() {
        return airSlash;
    }

    public Optional<Holder<SoundEvent>> swingSound() {
        return swingSound;
    }

    public Optional<Holder<SoundEvent>> hitSound() {
        return hitSound;
    }

    public Optional<ParticleType<?>> hitParticle() {
        return hitParticle;
    }

    public Optional<Boolean> equippableInOffhand() {
        return equippableInOffhand;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    /// Takes each property from `overwrite` where it declares one, from this block where it does not.
    /// The switch is `overwrite`'s.
    @Override
    public PatchComponent overwrite(final PatchComponent overwrite) {
        if (!(overwrite instanceof EpicFightWeaponProperties other)) {
            return overwrite;
        }

        return new EpicFightWeaponProperties(
            other.weaponCategories.or(() -> weaponCategories),
            other.comboAttacks.or(() -> comboAttacks),
            other.dash.or(() -> dash),
            other.airSlash.or(() -> airSlash),
            other.swingSound.or(() -> swingSound),
            other.hitSound.or(() -> hitSound),
            other.hitParticle.or(() -> hitParticle),
            other.equippableInOffhand.or(() -> equippableInOffhand),
            other.enabled
        );
    }

    @Override
    public EpicFightWeaponProperties instantiate(final ItemPatch blueprintOwner) {
        return new EpicFightWeaponProperties(
            weaponCategories, comboAttacks, dash, airSlash, swingSound, hitSound, hitParticle, equippableInOffhand, enabled
        );
    }

    @Override
    public boolean enabled() {
        return enabled;
    }

    @Override
    public MapCodec<? extends ComponentBlueprint<EpicFightWeaponProperties, ItemPatch>> mapCodec() {
        return MAP_CODEC;
    }
}
