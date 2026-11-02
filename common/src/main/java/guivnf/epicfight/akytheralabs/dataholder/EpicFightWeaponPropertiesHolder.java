package guivnf.epicfight.akytheralabs.dataholder;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.yesman.akythera.api.component.PatchComponent;
import com.yesman.akythera.core.animation.playable.AnimationMontage;
import com.yesman.akythera.core.registry.AkytheraRegistries;
import com.yesman.akythera.core.util.StaticRegistryAccess;
import com.yesman.akythera.core.util.helper.ParseUtils;
import com.yesman.akytheralabs.AkytheraProject;
import com.yesman.akytheralabs.akythera.entitypatch.component.ComponentDataHolder;
import com.yesman.akytheralabs.akythera.itempatch.component.WeaponStanceVariationAdapterHolder;
import com.yesman.akytheralabs.akythera.entitypatch.component.RootAttachableComponent;
import com.yesman.akytheralabs.dataholder.DataBlockHolder;
import com.yesman.akytheralabs.dataholder.DataHolder;
import com.yesman.akytheralabs.dataholder.EditorReference;
import com.yesman.akytheralabs.editor.EditorEditable;
import guivnf.epicfight.world.item.component.EpicFightWeaponProperties;
import guivnf.epicfight.world.item.datablock.WeaponCategory;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/// Editor holder for one [EpicFightWeaponProperties] block: scalar rows through the producer map
/// (montage project-holder popups, sound & particle ResourceKey popups, offhand checkbox), then its
/// own combo-attack / category list rows.
///
/// Attached under the [WeaponStanceVariationAdapterHolder] whose stance it describes, or — as
/// [RootAttachableComponent] — straight onto the patch, where it is the item's default.
public final class EpicFightWeaponPropertiesHolder extends ComponentDataHolder<EpicFightWeaponProperties>
    implements RootAttachableComponent
{
    public static final MapCodec<EpicFightWeaponPropertiesHolder> EDITOR_CODEC = RecordCodecBuilder.mapCodec(
        instance -> instance.group(
            DataBlockHolder.blockCodec(WeaponCategory.ENUM_MANAGER).listOf().optionalFieldOf("categories", List.of())
                .forGetter(d -> d.categories),
            ResourceKey.codec(AkytheraRegistries.DataPack.ANIM_MONTAGE_KEY).listOf().optionalFieldOf("combo_attacks", List.of())
                .forGetter(d -> d.comboAttacks.stream().map(m -> m.unwrapKey().orElseThrow()).toList()),
            ResourceKey.codec(AkytheraRegistries.DataPack.ANIM_MONTAGE_KEY).optionalFieldOf("dash")
                .forGetter(d -> Optional.ofNullable(d.dash).map(montage -> montage.unwrapKey().orElseThrow())),
            ResourceKey.codec(AkytheraRegistries.DataPack.ANIM_MONTAGE_KEY).optionalFieldOf("air_slash")
                .forGetter(d -> Optional.ofNullable(d.airSlash).map(montage -> montage.unwrapKey().orElseThrow())),
            ResourceKey.codec(Registries.SOUND_EVENT).optionalFieldOf("swing_sound")
                .forGetter(ParseUtils.provideOptional(d -> d.swingSound)),
            ResourceKey.codec(Registries.SOUND_EVENT).optionalFieldOf("hit_sound")
                .forGetter(ParseUtils.provideOptional(d -> d.hitSound)),
            ResourceKey.codec(Registries.PARTICLE_TYPE).optionalFieldOf("hit_particle")
                .forGetter(ParseUtils.provideOptional(d -> d.hitParticle)),
            Codec.BOOL.optionalFieldOf("offhand_equippable")
                .forGetter(ParseUtils.provideOptional(d -> d.equippableInOffhand))
        ).apply(instance, (categories, combos, dash, airSlash, swing, hit, particle, offhand) -> {
            EpicFightWeaponPropertiesHolder d = new EpicFightWeaponPropertiesHolder(
                dash.map(EpicFightWeaponPropertiesHolder::resolveMontage).orElse(null),
                airSlash.map(EpicFightWeaponPropertiesHolder::resolveMontage).orElse(null),
                swing.orElse(null), hit.orElse(null), particle.orElse(null), offhand.orElse(null));
            d.categories.addAll(categories);
            combos.forEach(key -> d.comboAttacks.add(resolveMontage(key)));
            return d;
        })
    );

    @EditorEditable(label = "gui.epicfight.item_patch_screen.row.dash", order = 0, inheritable = true)
    private @Nullable Holder<AnimationMontage> dash;

    @EditorEditable(label = "gui.epicfight.item_patch_screen.row.air_slash", order = 1, inheritable = true)
    private @Nullable Holder<AnimationMontage> airSlash;

    @EditorEditable(label = "gui.epicfight.item_patch_screen.row.swing_sound", order = 2, inheritable = true)
    private @Nullable ResourceKey<SoundEvent> swingSound;

    @EditorEditable(label = "gui.epicfight.item_patch_screen.row.hit_sound", order = 3, inheritable = true)
    private @Nullable ResourceKey<SoundEvent> hitSound;

    @EditorEditable(label = "gui.epicfight.item_patch_screen.row.hit_particle", order = 4, inheritable = true)
    private @Nullable ResourceKey<ParticleType<?>> hitParticle;

    @EditorEditable(label = "gui.epicfight.item_patch_screen.row.offhand_equippable", order = 5, inheritable = true)
    private @Nullable Boolean equippableInOffhand;

    /// Live lists the Details panel's list rows mutate. Ordered after the scalars so the block reads
    /// the way the sections did.
    @EditorEditable(
        label = "gui.epicfight.item_patch_screen.section.combo_attacks",
        defaultElement = "defaultMontage",
        order = 6,
        inheritable = true
    )
    final List<Holder<AnimationMontage>> comboAttacks = new ArrayList<>();

    @EditorEditable(
        label = "gui.epicfight.item_patch_screen.section.categories",
        defaultElement = "defaultCategory",
        emptyCandidates = "gui.epicfight.item_patch_screen.empty.categories",
        order = 7,
        inheritable = true
    )
    final List<WeaponCategory> categories = new ArrayList<>();

    EpicFightWeaponPropertiesHolder(
        @Nullable Holder<AnimationMontage> dash,
        @Nullable Holder<AnimationMontage> airSlash,
        @Nullable ResourceKey<SoundEvent> swingSound,
        @Nullable ResourceKey<SoundEvent> hitSound,
        @Nullable ResourceKey<ParticleType<?>> hitParticle,
        @Nullable Boolean equippableInOffhand
    ) {
        this.dash = dash;
        this.airSlash = airSlash;
        this.swingSound = swingSound;
        this.hitSound = hitSound;
        this.hitParticle = hitParticle;
        this.equippableInOffhand = equippableInOffhand;
    }

    /// Fixed: one may be attached per host.
    @Override
    public String fixedName() {
        return "properties";
    }

    public static EpicFightWeaponPropertiesHolder createDefault() {
        // Everything unset: a block attached over a parent's inherits until the author claims a row.
        return new EpicFightWeaponPropertiesHolder(null, null, null, null, null, null);
    }

    static EpicFightWeaponPropertiesHolder from(EpicFightWeaponProperties properties) {
        EpicFightWeaponPropertiesHolder d = new EpicFightWeaponPropertiesHolder(
            properties.dash().map(EpicFightWeaponPropertiesHolder::resolveMontage).orElse(null),
            properties.airSlash().map(EpicFightWeaponPropertiesHolder::resolveMontage).orElse(null),
            properties.swingSound().map(sound -> sound.unwrapKey().orElseThrow()).orElse(null),
            properties.hitSound().map(sound -> sound.unwrapKey().orElseThrow()).orElse(null),
            properties.hitParticle().map(EpicFightWeaponPropertiesHolder::particleKeyOf).orElse(null),
            properties.equippableInOffhand().orElse(null)
        );

        properties.weaponCategories().ifPresent(d.categories::addAll);
        properties.comboAttacks().ifPresent(list -> list.forEach(key -> d.comboAttacks.add(resolveMontage(key))));

        return d;
    }

    /// The `+` button of the combo-attack list: a dangling montage the author then picks, see
    /// [EditorEditable#defaultElement].
    static Holder<AnimationMontage> defaultMontage() {
        return resolveMontage(emptyMontageKey());
    }

    /// The `+` button of the category list: the first category the engine loaded or this project
    /// defines, null while there is neither.
    static @Nullable WeaponCategory defaultCategory() {
        AkytheraProject project = AkytheraProject.getCurrentProject();

        return project.getMergedDataBlockIdentifiers(WeaponCategory.ENUM_MANAGER).stream()
            .map(id -> project.getDataBlockHolder(WeaponCategory.ENUM_MANAGER, id))
            .filter(Objects::nonNull)
            .map(Holder::value)
            .findFirst()
            .orElse(null);
    }

    @Override
    public void readFrom(EpicFightWeaponProperties object) {
        EpicFightWeaponPropertiesHolder imported = from(object);

        dash = imported.dash;
        airSlash = imported.airSlash;
        swingSound = imported.swingSound;
        hitSound = imported.hitSound;
        hitParticle = imported.hitParticle;
        equippableInOffhand = imported.equippableInOffhand;

        comboAttacks.clear();
        comboAttacks.addAll(imported.comboAttacks);
        categories.clear();
        categories.addAll(imported.categories);
    }

    @Override
    public Class<? extends PatchComponent> generateEventually() {
        return EpicFightWeaponProperties.class;
    }

    @Override
    public EpicFightWeaponProperties makeObject() {
        Registry<SoundEvent> soundEvents = StaticRegistryAccess.getRegistryAccess().lookupOrThrow(Registries.SOUND_EVENT);
        Registry<ParticleType<?>> particleTypes = StaticRegistryAccess.getRegistryAccess().lookupOrThrow(Registries.PARTICLE_TYPE);

        // The editor list can hold the same category twice (nothing stops two rows picking it); the
        // runtime component is a set, so the duplicate collapses here rather than being rejected.
        //
        // An empty list is the editor's "unset": the block defers to whatever it overrides.
        return new EpicFightWeaponProperties(
            categories.isEmpty() ? Optional.empty() : Optional.of(new LinkedHashSet<>(categories)),
            comboAttacks.isEmpty()
                ? Optional.empty()
                : Optional.of(comboAttacks.stream().map(montage -> montage.unwrapKey().orElseThrow()).toList()),
            Optional.ofNullable(dash).map(montage -> montage.unwrapKey().orElseThrow()),
            Optional.ofNullable(airSlash).map(montage -> montage.unwrapKey().orElseThrow()),
            Optional.ofNullable(swingSound).map(soundEvents::getOrThrow),
            Optional.ofNullable(hitSound).map(soundEvents::getOrThrow),
            Optional.ofNullable(hitParticle).map(key -> particleTypes.getOrThrow(key).value()),
            Optional.ofNullable(equippableInOffhand),
            isEnabled()
        );
    }

    /// The shared montage holders (project entries follow renames through them).
    @Override
    public Stream<EditorReference> referencedReferences() {
        return Stream.concat(Stream.of(dash, airSlash), comboAttacks.stream())
            .filter(h -> h instanceof EditorReference)
            .map(h -> (EditorReference) h);
    }

    // ***************************************************************
    // Source export (File > Export > As Source Code)
    // ***************************************************************

    @Override
    public String sourceTemplate() {
        return "components/weapon_properties.ftl";
    }

    /// Tokens for `components/weapon_properties.ftl`. A dangling `<none>` exports the raw
    /// `akythera:empty` id — a compile-time TODO in the generated source, not an export failure.
    @Override
    public Map<String, Object> getDTO() {
        Map<String, Object> dto = new HashMap<>();

        if (!categories.isEmpty()) {
            dto.put("categories", categories.stream()
                .map(category -> Objects.toString(WeaponCategory.ENUM_MANAGER.derefIdentifier(category), ""))
                .toList());
        }

        if (!comboAttacks.isEmpty()) {
            dto.put("combo", comboAttacks.stream().map(EpicFightWeaponPropertiesHolder::montageId).toList());
        }

        if (dash != null) dto.put("dash", montageId(dash));
        if (airSlash != null) dto.put("air_slash", montageId(airSlash));
        if (swingSound != null) dto.put("swing_sound", swingSound.identifier().toString());
        if (hitSound != null) dto.put("hit_sound", hitSound.identifier().toString());
        if (hitParticle != null) dto.put("hit_particle", hitParticle.identifier().toString());
        if (equippableInOffhand != null) dto.put("offhand", equippableInOffhand);

        return dto;
    }

    private static String montageId(Holder<AnimationMontage> montage) {
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
