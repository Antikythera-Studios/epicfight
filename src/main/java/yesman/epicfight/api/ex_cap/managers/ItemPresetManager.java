package yesman.epicfight.api.ex_cap.managers;

import com.google.common.collect.Maps;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.ApiStatus;
import net.minecraft.resources.ResourceLocation;
import yesman.epicfight.EpicFight;
import yesman.epicfight.api.ex_cap.data.modifier.WeaponModifier;
import yesman.epicfight.api.ex_cap.modules.core.events.ExCapBuilderCreationEvent;
import yesman.epicfight.registry.EpicFightRegistries;
import yesman.epicfight.registry.deferred.holders.DeferredPreset;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.capabilities.item.WeaponCapability;
import yesman.epicfight.world.capabilities.item.WeaponCapabilityPresets;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;

@ApiStatus.Experimental
public class ItemPresetManager {
    private static final Map<ResourceLocation, CapabilityItem.Builder<?>> BUILDERS = Maps.newHashMap();

    public static Set<Map.Entry<ResourceLocation, CapabilityItem.Builder<?>>> getEntries() {
        return BUILDERS.entrySet();
    }

    public static CapabilityItem.Builder<?> get(ResourceLocation id) {
        return BUILDERS.get(id);
    }

    public static CapabilityItem.Builder<?> get(DeferredPreset<? extends CapabilityItem.Builder<?>> entry)
    {
        return get(entry.getId());
    }


    @ApiStatus.Internal
    public static void acceptEvent(@Deprecated ExCapBuilderCreationEvent event) {
        BUILDERS.clear();
        for (var entry : EpicFightRegistries.BUILDERS.entrySet()) {
            entry.getValue().identifier(entry.getKey().location());
            EpicFight.LOGGER.info("Pulling {} from register", entry.getKey().location());
            if (entry.getValue() instanceof WeaponCapability.Builder builder)
            {
                builder.exportBuiltMovesets();
                builder.exportBuiltConditionals();
            }
            ExCapManager.addAcceptor(entry.getKey().location());
            BUILDERS.put(entry.getKey().location(), entry.getValue());
        }
        BUILDERS.putAll(event.getBuilders());
        BUILDERS.values().forEach( builder -> {
            if (builder instanceof WeaponCapability.Builder weaponBuilder)
            {
                EpicFightRegistries.WEAPON_DATA.holders().forEach(
                        weaponBuilder::registerCustomData
                );
            }
        });
    }

    @ApiStatus.Internal
    public static void export(Map<ResourceLocation, Function<Item, ? extends CapabilityItem.Builder<?>>> event)
    {
        BUILDERS.forEach((entry, builder) -> event.put(entry, item -> WeaponCapabilityPresets.registerPreset(builder, item)));
    }

    public static void add(ResourceLocation id, CompoundTag cTag)
    {
        //TODO: Implement
    }

    public static void modify(WeaponModifier modifier)
    {
        for (var target : modifier.targets())
        {
            CapabilityItem.Builder<?> builder = BUILDERS.get(target);
            modifier.weaponCustomData().forEach(builder::setCustomDataInternal);
            if (builder instanceof WeaponCapability.Builder weaponBuilder)
            {
                modifier.conditionalModifier().forEach((resourceLocation, operation) ->
                {
                    if (operation == WeaponModifier.Operation.APPEND)
                    {
                        weaponBuilder.addConditionals(resourceLocation);
                    }
                    if (operation == WeaponModifier.Operation.REMOVE)
                    {
                        weaponBuilder.removeConditional(resourceLocation);
                    }
                });
                modifier.overrideModifiers().accept(weaponBuilder);
                weaponBuilder.addMovesets(modifier.movesetModifier());
            }
        }
    }
}
