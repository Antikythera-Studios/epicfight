package guivnf.epicfight.akythera.providers;

import com.google.gson.JsonPrimitive;
import com.yesman.akythera.api.data.modularize.AkytheraScriptFunctions;
import com.yesman.akythera.api.data.provider.AbilityResourceBlueprintProvider;
import com.yesman.akythera.core.blueprint.AbilityResourceBlueprint;
import com.yesman.akythera.core.blueprint.script.BlueprintBody;
import com.yesman.akythera.core.registry.AkytheraRegistries;
import guivnf.epicfight.EpicFight;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

public final class EpicfightAbilityResources extends AbilityResourceBlueprintProvider {
    public static final ResourceKey<AbilityResourceBlueprint> STAMINA =
        ResourceKey.create(AkytheraRegistries.DataPack.ABILITY_RESOURCE_KEY, EpicFight.identifier("stamina"));

    public EpicfightAbilityResources(final PackOutput packOutput) {
        super(packOutput);
    }

    @Override
    protected void buildEntries(final BiConsumer<ResourceKey<AbilityResourceBlueprint>, AbilityResourceBlueprint> writer) {
        writer.accept(STAMINA, new AbilityResourceBlueprint(
            AkytheraScriptFunctions.STORED_ABILITY_RESOURCE_CONSTRUCTOR.value(),
            new BlueprintBody(
                Map.of(),
                Map.of(),
                Map.of(
                    "kind", new JsonPrimitive("consumptive"),
                    "max", new JsonPrimitive(15.0F),
                    "regen_delay", new JsonPrimitive(30),
                    "regen_per_tick", new JsonPrimitive(0.15F)
                ),
                List.of()
            )
        ));
    }
}
