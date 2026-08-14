package com.yesman.epicfight.akytheralabs.dataholder;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.yesman.akythera.core.animation.playable.AnimationMontage;
import com.yesman.akythera.core.registry.AkytheraRegistries;
import com.yesman.akythera.core.util.StaticRegistryAccess;
import com.yesman.akytheralabs.AkytheraProject;
import com.yesman.akytheralabs.dataholder.DataHolder;
import com.yesman.akytheralabs.dataholder.EditorReference;
import com.yesman.akytheralabs.editor.EditorEditable;
import com.yesman.epicfight.world.item.component.EpicFightWeaponProperties;
import com.yesman.epicfight.world.item.component.WeaponAttackMotions;
import com.yesman.epicfight.world.item.datablock.WeaponCategory;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/// Editor holder for one stance's [EpicFightWeaponProperties]. The owning stance lives on the
/// enclosing block ([WeaponStanceSystemHolder]'s default stance row or a conditional stance), not
/// here — this object renders as a nested editor block: scalar rows through the producer map
/// (montage project-holder popups, sound & particle ResourceKey popups, offhand checkbox), then its
/// own combo-attack / category list rows.
public final class EpicFightWeaponPropertiesHolder {
    static final Codec<EpicFightWeaponPropertiesHolder> CODEC = RecordCodecBuilder.create(
        instance -> instance.group(
            WeaponCategory.ENUM_MANAGER.getNameCodec().listOf().optionalFieldOf("categories", List.of())
                .forGetter(d -> d.categories),
            ResourceKey.codec(AkytheraRegistries.DataPack.ANIM_MONTAGE_KEY).listOf().optionalFieldOf("combo_attacks", List.of())
                .forGetter(d -> d.comboAttacks.stream().map(m -> m.unwrapKey().orElseThrow()).toList()),
            ResourceKey.codec(AkytheraRegistries.DataPack.ANIM_MONTAGE_KEY).fieldOf("dash")
                .forGetter(d -> d.dash.unwrapKey().orElseThrow()),
            ResourceKey.codec(AkytheraRegistries.DataPack.ANIM_MONTAGE_KEY).fieldOf("air_slash")
                .forGetter(d -> d.airSlash.unwrapKey().orElseThrow()),
            ResourceKey.codec(Registries.SOUND_EVENT).fieldOf("swing_sound").forGetter(d -> d.swingSound),
            ResourceKey.codec(Registries.SOUND_EVENT).fieldOf("hit_sound").forGetter(d -> d.hitSound),
            ResourceKey.codec(Registries.PARTICLE_TYPE).fieldOf("hit_particle").forGetter(d -> d.hitParticle),
            Codec.BOOL.fieldOf("offhand_equippable").forGetter(d -> d.equippableInOffhand)
        ).apply(instance, (categories, combos, dash, airSlash, swing, hit, particle, offhand) -> {
            EpicFightWeaponPropertiesHolder d = new EpicFightWeaponPropertiesHolder(
                resolveMontage(dash), resolveMontage(airSlash), swing, hit, particle, offhand);
            d.categories.addAll(categories);
            combos.forEach(key -> d.comboAttacks.add(resolveMontage(key)));
            return d;
        })
    );

    @EditorEditable(label = "gui.epicfight.item_patch_screen.row.dash", order = 0)
    private Holder<AnimationMontage> dash;

    @EditorEditable(label = "gui.epicfight.item_patch_screen.row.air_slash", order = 1)
    private Holder<AnimationMontage> airSlash;

    @EditorEditable(label = "gui.epicfight.item_patch_screen.row.swing_sound", order = 2)
    private ResourceKey<SoundEvent> swingSound;

    @EditorEditable(label = "gui.epicfight.item_patch_screen.row.hit_sound", order = 3)
    private ResourceKey<SoundEvent> hitSound;

    @EditorEditable(label = "gui.epicfight.item_patch_screen.row.hit_particle", order = 4)
    private ResourceKey<ParticleType<?>> hitParticle;

    @EditorEditable(label = "gui.epicfight.item_patch_screen.row.offhand_equippable", order = 5)
    private boolean equippableInOffhand;

    /// Live lists the Details panel's list rows mutate. Ordered after the scalars so the block reads
    /// the way the sections did.
    @EditorEditable(
        label = "gui.epicfight.item_patch_screen.section.combo_attacks",
        defaultElement = "defaultMontage",
        order = 6
    )
    final List<Holder<AnimationMontage>> comboAttacks = new ArrayList<>();

    @EditorEditable(
        label = "gui.epicfight.item_patch_screen.section.categories",
        defaultElement = "defaultCategory",
        order = 7
    )
    final List<WeaponCategory> categories = new ArrayList<>();

    EpicFightWeaponPropertiesHolder(
        Holder<AnimationMontage> dash,
        Holder<AnimationMontage> airSlash,
        ResourceKey<SoundEvent> swingSound,
        ResourceKey<SoundEvent> hitSound,
        ResourceKey<ParticleType<?>> hitParticle,
        boolean equippableInOffhand
    ) {
        this.dash = dash;
        this.airSlash = airSlash;
        this.swingSound = swingSound;
        this.hitSound = hitSound;
        this.hitParticle = hitParticle;
        this.equippableInOffhand = equippableInOffhand;
    }

    static EpicFightWeaponPropertiesHolder createDefault() {
        return new EpicFightWeaponPropertiesHolder(
            resolveMontage(emptyMontageKey()),
            resolveMontage(emptyMontageKey()),
            ResourceKey.create(Registries.SOUND_EVENT, DataHolder.EMPTY_REFERENCE),
            ResourceKey.create(Registries.SOUND_EVENT, DataHolder.EMPTY_REFERENCE),
            ResourceKey.create(Registries.PARTICLE_TYPE, DataHolder.EMPTY_REFERENCE),
            false
        );
    }

    static EpicFightWeaponPropertiesHolder from(EpicFightWeaponProperties properties) {
        EpicFightWeaponPropertiesHolder d = new EpicFightWeaponPropertiesHolder(
            resolveMontage(properties.comboAttacks().dash()),
            resolveMontage(properties.comboAttacks().airSlash()),
            properties.swingSound().unwrapKey().orElseThrow(),
            properties.hitSound().unwrapKey().orElseThrow(),
            particleKeyOf(properties.hitParticle()),
            properties.equippableInOffhand()
        );

        d.categories.addAll(properties.weaponCategories());
        properties.comboAttacks().comboAttacks().forEach(key -> d.comboAttacks.add(resolveMontage(key)));

        return d;
    }

    /// The `+` button of the combo-attack list: a dangling montage the author then picks, see
    /// [EditorEditable#defaultElement].
    static Holder<AnimationMontage> defaultMontage() {
        return resolveMontage(emptyMontageKey());
    }

    /// The `+` button of the category list.
    /// ⚠ Requires at least one registered weapon category.
    static WeaponCategory defaultCategory() {
        return WeaponCategory.ENUM_MANAGER.universalValues().iterator().next();
    }

    EpicFightWeaponProperties makeProperties() {
        Registry<SoundEvent> soundEvents = StaticRegistryAccess.getRegistryAccess().lookupOrThrow(Registries.SOUND_EVENT);
        Registry<ParticleType<?>> particleTypes = StaticRegistryAccess.getRegistryAccess().lookupOrThrow(Registries.PARTICLE_TYPE);

        // The editor list can hold the same category twice (nothing stops two rows picking it); the
        // runtime component is a set, so the duplicate collapses here rather than being rejected.
        Set<WeaponCategory> categorySet = new LinkedHashSet<>(categories);

        return new EpicFightWeaponProperties(
            categorySet,
            new WeaponAttackMotions(
                comboAttacks.stream().map(m -> m.unwrapKey().orElseThrow()).toList(),
                dash.unwrapKey().orElseThrow(),
                airSlash.unwrapKey().orElseThrow()
            ),
            soundEvents.getOrThrow(swingSound),
            soundEvents.getOrThrow(hitSound),
            particleTypes.getOrThrow(hitParticle).value(),
            equippableInOffhand
        );
    }

    /// The shared montage holders (project entries follow renames through them).
    Stream<EditorReference> referencedReferences() {
        return Stream.concat(Stream.of(dash, airSlash), comboAttacks.stream())
            .filter(h -> h instanceof EditorReference)
            .map(h -> (EditorReference) h);
    }

    /// ***************************************************************
    /// Source export accessors (a dangling `<none>` exports the raw
    /// `akythera:empty` id — a compile-time TODO, not an export failure)
    /// ***************************************************************

    String dashIdForExport() {
        return montageId(dash);
    }

    String airSlashIdForExport() {
        return montageId(airSlash);
    }

    String swingSoundIdForExport() {
        return swingSound.identifier().toString();
    }

    String hitSoundIdForExport() {
        return hitSound.identifier().toString();
    }

    String hitParticleIdForExport() {
        return hitParticle.identifier().toString();
    }

    boolean equippableInOffhandForExport() {
        return equippableInOffhand;
    }

    static String montageId(Holder<AnimationMontage> montage) {
        return montage.unwrapKey().map(k -> k.identifier().toString()).orElse("");
    }

    private static ResourceKey<AnimationMontage> emptyMontageKey() {
        return ResourceKey.create(AkytheraRegistries.DataPack.ANIM_MONTAGE_KEY, DataHolder.EMPTY_REFERENCE);
    }

    private static Holder<AnimationMontage> resolveMontage(ResourceKey<AnimationMontage> key) {
        return AkytheraProject.getCurrentProject().getEntryHolderOrDangling(key);
    }

    private static ResourceKey<ParticleType<?>> particleKeyOf(ParticleType<?> particle) {
        return StaticRegistryAccess.getRegistryAccess().lookupOrThrow(Registries.PARTICLE_TYPE)
            .getResourceKey(particle).orElseThrow();
    }
}
