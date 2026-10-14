package yesman.epicfight.registry;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.fabricmc.fabric.api.event.registry.RegistryAttribute;
import net.fabricmc.fabric.api.event.registry.RegistryEntryAddedCallback;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import yesman.epicfight.registry.callbacks.SkillCallbacks;
import yesman.epicfight.registry.callbacks.SkillDataKeyCallbacks;
import yesman.epicfight.registry.callbacks.SynchedAnimationVariableKeyCallbacks;
import yesman.epicfight.registry.deferred_shim.DeferredRegisterShim;
import yesman.epicfight.EpicFight;
import yesman.epicfight.api.animation.SynchedAnimationVariableKey;
import yesman.epicfight.api.ex_cap.data.Moveset;
import yesman.epicfight.api.ex_cap.data.modifier.WeaponModifier;
import yesman.epicfight.api.ex_cap.provider.ProviderConditional;
import yesman.epicfight.client.online.cosmetics.Emote;
import yesman.epicfight.data.conditions.Condition;
import yesman.epicfight.registry.entries.*;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillDataKey;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.capabilities.item.custom.CustomData;
import yesman.epicfight.world.entity.data.ExpandedEntityDataAccessor;
import java.util.List;
import java.util.function.Supplier;

public abstract class EpicFightRegistries {
    public static final Registry<Supplier<Condition<?>>> CONDITION = createRegistry(Keys.CONDITION, false);
    public static final Registry<ExpandedEntityDataAccessor<?>> EXPANDED_ENTITY_DATA_ACCESSOR = createRegistry(Keys.EXPANDED_ENTITY_DATA_ACCESSOR, true);
    public static final Registry<Skill> SKILL = createRegistry(Keys.SKILL, true);
    public static final Registry<SkillDataKey<?>> SKILL_DATA_KEY = createRegistry(Keys.SKILL_DATA_KEY, true);
    public static final Registry<SynchedAnimationVariableKey<?>> SYNCHED_ANIMATION_VARIABLE = createRegistry(Keys.SYNCHED_ANIMATION_VARIABLE_KEY, true);
    public static final Registry<CapabilityItem.Builder<?>> BUILDERS = createRegistry(Keys.BUILDERS, true);
    public static final Registry<Moveset.Builder> MOVESETS = createRegistry(Keys.MOVESETS, true);
    public static final Registry<ProviderConditional.Builder> PROVIDER_CONDITIONALS = createRegistry(Keys.PROVIDER_CONDITIONALS, true);
    public static final Registry<WeaponModifier.Builder> MODIFIERS = createRegistry(Keys.MODIFIERS, true);
    public static final Registry<CustomData<?>> WEAPON_DATA = createRegistry(Keys.WEAPON_DATA, true);
    public static final Registry<CustomData<?>> MOVESET_DATA = createRegistry(Keys.MOVESET_DATA, true);
    // EMOTE is a data pack registry (dynamic), not a static registry.
    // It is registered via MixinRegistryDataLoader into RegistryDataLoader.SYNCHRONIZED_REGISTRIES,
    // matching the NeoForge DataPackRegistryEvent approach. Access it via RegistryAccess.

    private static <T> Registry<T> createRegistry(ResourceKey<Registry<T>> key, boolean synced) {
        var builder = FabricRegistryBuilder.createSimple(key);
        if (synced) {
            builder.attribute(RegistryAttribute.SYNCED);
        }
        return builder.buildAndRegister();
    }

    public static final List<DeferredRegisterShim<?>> DEFERRED_REGISTRIES = List.of(
        EpicFightArmorMaterials.REGISTRY, EpicFightAttributes.REGISTRY, EpicFightBlockEntities.REGISTRY,
        EpicFightBlocks.REGISTRY, EpicFightCommandArgumentTypes.REGISTRY, EpicFightConditions.REGISTRY,
        EpicFightCreativeTabs.REGISTRY, EpicFightDataComponentTypes.REGISTRY, EpicFightEntityTypes.REGISTRY,
        EpicFightExpandedEntityDataAccessors.REGISTRY, EpicFightItems.REGISTRY, EpicFightLootItemFunctions.REGISTRY,
        EpicFightMobEffects.REGISTRY, EpicFightParticles.REGISTRY, EpicFightPotions.REGISTRY,
        EpicFightSkillDataKeys.REGISTRY, EpicFightSounds.REGISTRY, EpicFightSkills.REGISTRY,
        EpicFightSynchedAnimationVariableKeys.REGISTRY, EpicFightMovesets.REGISTRY,
        EpicFightProviderConditionals.REGISTRY, EpicFightItemCapabilityPresets.REGISTRY,
        EpicFightModifiers.REGISTRY, EpicFightMovesetData.REGISTER
    );

    /**
     * Fires registry callbacks in the correct order after all deferred registries have been accepted.
     *
     * Ordering matters:
     *   1. SKILL bake (sets holder on each skill)
     *   2. SKILL_DATA_KEY add (per-entry, populates dataKeysBySkillClasses) then bake (builds CLASS_TO_DATA_KEYS map, depends on SKILL being baked)
     *   3. SYNCHED_ANIMATION_VARIABLE bake (populates ID_MAPPER)
     */
    public static void bakeRegistries() {
        // 1. Bake SKILL registry — sets holder reference on each Skill
        try {
            SkillCallbacks.getSkillCallback().onBake(SKILL);
        } catch (Throwable e) {
            EpicFight.LOGGER.warn("Failed to bake SKILL registry callbacks", e);
        }

        // 2. Fire onAdd for existing SKILL_DATA_KEY entries (callback wasn't registered when they were added),
        //    then bake (depends on SKILL registry being baked first)
        try {
            SkillDataKeyCallbacks skillDataKeyCallbacks = SkillDataKeyCallbacks.getRegistryCallback();
            SKILL_DATA_KEY.holders().forEach(holder -> {
                try {
                    int id = SKILL_DATA_KEY.getId(holder.value());
                    skillDataKeyCallbacks.onAdd(SKILL_DATA_KEY, id, holder.key(), holder.value());
                } catch (Throwable e) {
                    EpicFight.LOGGER.warn("Failed to fire onAdd for SKILL_DATA_KEY entry", e);
                }
            });
            skillDataKeyCallbacks.onBake(SKILL_DATA_KEY);
        } catch (Throwable e) {
            EpicFight.LOGGER.warn("Failed to bake SKILL_DATA_KEY registry callbacks", e);
        }

        // 3. Bake SYNCHED_ANIMATION_VARIABLE registry — populates ID_MAPPER
        try {
            SynchedAnimationVariableKeyCallbacks.getRegistryCallback().onBake(SYNCHED_ANIMATION_VARIABLE);
        } catch (Throwable e) {
            EpicFight.LOGGER.warn("Failed to bake SYNCHED_ANIMATION_VARIABLE registry callbacks", e);
        }
    }

    /**
     * Registers the RegistryEntryAddedCallback for SKILL_DATA_KEY so that dynamically added entries
     * (e.g., from datapack reloads) also trigger the onAdd callback.
     * Call this during mod initialization, before any reloads happen.
     */
    public static void registerDynamicCallbacks() {
        RegistryEntryAddedCallback.event(SKILL_DATA_KEY).register((id, resourceLocation, value) -> {
            try {
                var resourceKey = ResourceKey.create(Keys.SKILL_DATA_KEY, resourceLocation);
                SkillDataKeyCallbacks.getRegistryCallback().onAdd(SKILL_DATA_KEY, id, resourceKey, value);
            } catch (Throwable e) {
                EpicFight.LOGGER.warn("Failed to fire dynamic onAdd for SKILL_DATA_KEY", e);
            }
        });
    }

    public interface Keys {
        ResourceKey<Registry<CapabilityItem.Builder<?>>> BUILDERS = key("item_capability_builder");
        ResourceKey<Registry<Moveset.Builder>> MOVESETS = key("moveset");
        ResourceKey<Registry<ProviderConditional.Builder>> PROVIDER_CONDITIONALS = key("provider_conditional");
        ResourceKey<Registry<WeaponModifier.Builder>> MODIFIERS = key("modifiers");
        ResourceKey<Registry<CustomData<?>>> WEAPON_DATA = key("weapon_data");
        ResourceKey<Registry<CustomData<?>>> MOVESET_DATA = key("moveset_data");
        ResourceKey<Registry<Supplier<Condition<?>>>> CONDITION = key("condition");
        ResourceKey<Registry<ExpandedEntityDataAccessor<?>>> EXPANDED_ENTITY_DATA_ACCESSOR = key("expanded_entity_data_accessor");
        ResourceKey<Registry<SynchedAnimationVariableKey<?>>> SYNCHED_ANIMATION_VARIABLE_KEY = key("synched_animation_variable_key");
        ResourceKey<Registry<Skill>> SKILL = key("skill");
        ResourceKey<Registry<SkillDataKey<?>>> SKILL_DATA_KEY = key("skill_data_key");
        ResourceKey<Registry<Emote>> EMOTE = key("emote");
        private static <T> ResourceKey<Registry<T>> key(String name) { return ResourceKey.createRegistryKey(EpicFight.identifier(name)); }
    }
}
