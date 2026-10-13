package guivnf.epicfight.fabric;

import com.yesman.akythera.core.registry.AkytheraRegistries;
import guivnf.epicfight.EpicFight;
import guivnf.epicfight.akythera.registries.EpicFightScriptTypes;
import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;

public final class EpicFightFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        EpicFightScriptTypes.register((name, type) ->
            Registry.register(AkytheraRegistries.Frozen.SCRIPT_TYPE, Identifier.fromNamespaceAndPath(EpicFight.MODID, name), type)
        );
    }
}
