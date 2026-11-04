package yesman.epicfight.world.capabilities.item;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Pair;
import io.netty.util.internal.StringUtil;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;
import yesman.epicfight.EpicFight;
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.LivingMotion;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.api.data.reloader.ItemCapabilityReloadListener;
import yesman.epicfight.api.event.EpicFightEventHooks;
import yesman.epicfight.api.event.types.registry.WeaponCapabilityPresetRegistryEvent;
import yesman.epicfight.api.ex_cap.managers.ItemPresetManager;
import yesman.epicfight.api.ex_cap.modules.core.events.ExCapBuilderCreationEvent;
import yesman.epicfight.data.conditions.Condition.EntityPatchCondition;
import yesman.epicfight.gameasset.ColliderPreset;
import yesman.epicfight.network.server.SPDatapackSync;
import yesman.epicfight.particle.HitParticleType;
import yesman.epicfight.registry.EpicFightRegistries;
import yesman.epicfight.registry.entries.EpicFightConditions;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.provider.ExtraEntryProvider;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class WeaponTypeReloadListener extends SimpleJsonResourceReloadListener {
    public static void registerDefaultWeaponTypes() {
        // Ensure ItemPresetManager.BUILDERS is populated from the registry before exporting.
        // This is safe to call multiple times — acceptEvent() clears and rebuilds.
        if (ItemPresetManager.getEntries().isEmpty()) {
            ExCapBuilderCreationEvent builderEvent = EpicFightEventHooks.Registry.EX_CAP_BUILDER_CREATION.post(new ExCapBuilderCreationEvent());
            ItemPresetManager.acceptEvent(builderEvent);
        }

        Map<ResourceLocation, Function<Item, ? extends CapabilityItem.Builder<?>>> typeEntry = Maps.newHashMap();

        WeaponCapabilityPresetRegistryEvent weaponCapabilityPresetRegistryEvent = new WeaponCapabilityPresetRegistryEvent(typeEntry);
        EpicFightEventHooks.Registry.WEAPON_CAPABILITY_PRESET.post(weaponCapabilityPresetRegistryEvent);
        PRESETS.putAll(weaponCapabilityPresetRegistryEvent.getTypeEntry());
        ItemPresetManager.export(PRESETS);
    }

    public static final String DIRECTORY = "capabilities/weapons/types";

    private static final Gson GSON = (new GsonBuilder()).create();
    private static final Map<ResourceLocation, Function<Item, ? extends CapabilityItem.Builder<?>>> PRESETS = Maps.newHashMap();
    private static final Map<ResourceLocation, CompoundTag> CAPABILITY_COMPOUNDS = Maps.newHashMap();

    public WeaponTypeReloadListener() {
        super(GSON, DIRECTORY);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> packEntry, ResourceManager resourceManager, ProfilerFiller profilerFiller) {
        clear();
        packEntry.forEach((key, value) -> {
            CompoundTag compTag = null;

            try {
                compTag = TagParser.parseTag(value.toString());
            } catch (CommandSyntaxException e) {
                e.printStackTrace();
            }

            try {
                final CompoundTag comptagFinal = compTag;
                PRESETS.put(key, (itemstack) -> deserializeWeaponCapabilityBuilder(key, comptagFinal));
                CAPABILITY_COMPOUNDS.put(key, compTag);
            } catch (Exception e) {
                EpicFight.LOGGER.warn("Error while deserializing weapon type datapack: {}", key);
                e.printStackTrace();
            }
        });
    }

    public static Function<Item, ? extends CapabilityItem.Builder<?>> getOrThrow(String typeName) {
        ResourceLocation rl = ResourceLocation.parse(typeName);

        if (!PRESETS.containsKey(rl)) {
            throw new IllegalArgumentException("Can't find weapon type: " + rl);
        }

        return PRESETS.get(rl);
    }

    public static Function<Item, ? extends CapabilityItem.Builder<?>> get(String typeName) {
        return get(ResourceLocation.parse(typeName));
    }

    public static Function<Item, ? extends CapabilityItem.Builder<?>> get(ResourceLocation typeName) {
        return PRESETS.get(typeName);
    }

    public static <T extends CapabilityItem.Builder<?>> void register(ResourceLocation rl, T builder) {
        PRESETS.put(rl, (item) -> builder);
    }

    public static WeaponCapability.Builder deserializeWeaponCapabilityBuilder(ResourceLocation rl, CompoundTag tag) {
        return deserializeWeaponCapabilityBuilder(rl, tag, null);
    }

    /// @deprecated Use Non-datapack sensitive version. [#deserializeWeaponCapabilityBuilder(ResourceLocation, CompoundTag)]
    /// @param extraEntryProvider Returns extra-entry created in runtime. (Datapack editor) Exists to access animations
    @Deprecated
    @SuppressWarnings("unchecked")
    public static WeaponCapability.Builder deserializeWeaponCapabilityBuilder(ResourceLocation rl, CompoundTag tag, @Nullable ExtraEntryProvider extraEntryProvider) {
        WeaponCapability.Builder builder = WeaponCapability.builder();

        if (!tag.contains("category") || StringUtil.isNullOrEmpty(tag.getString("category"))) {
            throw new IllegalArgumentException("Define weapon category.");
        }

        builder.category(WeaponCategory.ENUM_MANAGER.getOrThrow(tag.getString("category")));
        builder.collider(ColliderPreset.deserializeSimpleCollider(tag.getCompound("collider")));
        builder.canBePlacedOffhand(!tag.contains("usable_in_offhand") || tag.getBoolean("usable_in_offhand"));

        if (tag.contains("hit_particle")) {
            Optional<Holder.Reference<ParticleType<?>>> particleType = BuiltInRegistries.PARTICLE_TYPE.getHolder(ResourceLocation.parse(tag.getString("hit_particle")));
            particleType.ifPresent( particleHolder -> {
                if (!(particleHolder.value() instanceof HitParticleType)) {
                    EpicFight.LOGGER.warn("{} is not a hit particle type in {}", tag.getString("hit_particle"), rl);
                } else {
                    builder.hitParticle(particleHolder);
                }
            });

        }

        if (tag.contains("swing_sound")) {
            Holder<SoundEvent> sound = BuiltInRegistries.SOUND_EVENT.getHolder(ResourceLocation.parse(tag.getString("swing_sound"))).get();

            builder.swingSound(sound);
        }

        if (tag.contains("hit_sound")) {
            Holder<SoundEvent> sound = BuiltInRegistries.SOUND_EVENT.getHolder(ResourceLocation.parse(tag.getString("hit_sound"))).get();

            builder.hitSound(sound);
        }

        CompoundTag combosTag = tag.getCompound("combos");

        for (String key : combosTag.getAllKeys()) {
            Style style = Style.ENUM_MANAGER.getOrThrow(key);
            ListTag comboAnimations = combosTag.getList(key, Tag.TAG_STRING);
            List<AnimationAccessor<? extends AttackAnimation>> anims = new ArrayList<>();

            for (int i = 0; i < comboAnimations.size(); i++) {
                String animId = comboAnimations.getString(i);
                AnimationAccessor<? extends AttackAnimation> animation = extraEntryProvider == null ? AnimationManager.byKey(animId) : extraEntryProvider.getExtraOrBuiltInAnimation(animId);

                if (animation == null) {
                    EpicFight.LOGGER.warn("Can't find an animation named {} in {}", comboAnimations.getString(i), rl);
                } else {
                    anims.add(animation);
                }
            }

            builder.newStyleCombo(style, anims.toArray(new AnimationAccessor[0]));
        }

        CompoundTag innateSkillsTag = tag.getCompound("innate_skills");

        for (String key : innateSkillsTag.getAllKeys()) {
            Style style = Style.ENUM_MANAGER.getOrThrow(key);
            Skill skill = EpicFightRegistries.SKILL.get(ResourceLocation.parse(innateSkillsTag.getString(key)));

            builder.innateSkill(style, itemstack -> skill);
        }

        CompoundTag livingmotionModifierTag = tag.getCompound("livingmotion_modifier");

        for (String sStyle : livingmotionModifierTag.getAllKeys()) {
            Style style = Style.ENUM_MANAGER.getOrThrow(sStyle);
            CompoundTag styleAnimationTag = livingmotionModifierTag.getCompound(sStyle);

            for (String sLivingmotion : styleAnimationTag.getAllKeys()) {
                LivingMotion livingmotion = LivingMotion.ENUM_MANAGER.getOrThrow(sLivingmotion);

                String animId = styleAnimationTag.getString(sLivingmotion);
                AnimationAccessor<? extends AttackAnimation> animation = extraEntryProvider == null ? AnimationManager.byKey(animId) : extraEntryProvider.getExtraOrBuiltInAnimation(animId);

                if (animation == null) {
                    EpicFight.LOGGER.warn("No animation named {}", styleAnimationTag.getString(sLivingmotion));
                } else {
                    builder.livingMotionModifier(style, livingmotion, animation);
                }
            }
        }

        CompoundTag stylesTag = tag.getCompound("styles");
        final List<Pair<Predicate<LivingEntityPatch<?>>, Style>> conditions = Lists.newArrayList();
        final Style defaultStyle = Style.ENUM_MANAGER.getOrThrow(stylesTag.getString("default"));

        for (Tag caseTag : stylesTag.getList("cases", Tag.TAG_COMPOUND)) {
            CompoundTag caseCompTag = (CompoundTag)caseTag;
            List<EntityPatchCondition> conditionList = Lists.newArrayList();

            for (Tag offhandTag : caseCompTag.getList("conditions", Tag.TAG_COMPOUND)) {
                CompoundTag offhandCompound = (CompoundTag)offhandTag;
                Supplier<EntityPatchCondition> conditionProvider = EpicFightConditions.getConditionOrThrow(ResourceLocation.parse(offhandCompound.getString("predicate")));
                EntityPatchCondition condition = conditionProvider.get();
                condition.read(offhandCompound);
                conditionList.add(condition);
            }

            conditions.add(Pair.of((entitypatch) -> {
                for (EntityPatchCondition condition : conditionList) {
                    if (!condition.predicate(entitypatch)) {
                        return false;
                    }
                }

                return true;
            }, Style.ENUM_MANAGER.getOrThrow(caseCompTag.getString("style"))));
        }

        builder.styleProvider((entitypatch) -> {
            for (Pair<Predicate<LivingEntityPatch<?>>, Style> entry : conditions) {
                if (entry.getFirst().test(entitypatch)) {
                    return entry.getSecond();
                }
            }

            return defaultStyle;
        });

        if (tag.contains("offhand_item_compatible_predicate")) {
            ListTag offhandValidatorList = tag.getList("offhand_item_compatible_predicate", Tag.TAG_COMPOUND);
            List<EntityPatchCondition> conditionList = Lists.newArrayList();

            for (Tag offhandTag : offhandValidatorList) {
                CompoundTag offhandCompound = (CompoundTag)offhandTag;
                Supplier<EntityPatchCondition> conditionProvider = EpicFightConditions.getConditionOrThrow(ResourceLocation.parse(offhandCompound.getString("predicate")));
                EntityPatchCondition condition = conditionProvider.get();
                condition.read(offhandCompound);
                conditionList.add(condition);
            }

            builder.weaponCombinationPredicator((entitypatch) -> {
                for (EntityPatchCondition condition : conditionList) {
                    if (!condition.predicate(entitypatch)) {
                        return false;
                    }
                }

                return true;
            });
        }

        if (tag.contains("custom_tags")) {
            for (Tag customTag : tag.getList("custom_tags", Tag.TAG_STRING)) {
                builder.addTag(ResourceLocation.parse(customTag.getAsString()));
            }
        }

        // Add data pack path as default tag
        builder.addTag(rl);

        return builder;
    }

    public static Stream<CompoundTag> getWeaponTypeDataStream() {
        Stream<CompoundTag> tagStream = CAPABILITY_COMPOUNDS.entrySet().stream().map((entry) -> {
            entry.getValue().putString("registry_name", entry.getKey().toString());
            return entry.getValue();
        });
        return tagStream;
    }

    public static Set<Map.Entry<ResourceLocation, Function<Item, ? extends CapabilityItem.Builder<?>>>> entries() {
        return PRESETS.entrySet();
    }

    public static void clear() {
        PRESETS.clear();
        WeaponTypeReloadListener.registerDefaultWeaponTypes();
    }

    public static void processServerPacket(SPDatapackSync packet) {
        if (packet.packetType() == SPDatapackSync.PacketType.WEAPON_TYPE) {
            PRESETS.clear();
            //
            registerDefaultWeaponTypes();

            for (CompoundTag tag : packet.tags()) {
                ResourceLocation rl = ResourceLocation.parse(tag.getString("registry_name"));

                try
                {
                    PRESETS.put(rl, (itemstack) -> deserializeWeaponCapabilityBuilder(rl, tag));
                }
                catch (Exception e)
                {
                    throw new RuntimeException("Weapon type " + rl + " encountered an error",e);
                }
            }

            ItemCapabilityReloadListener.weaponTypeProcessedCheck();
        }
    }
}