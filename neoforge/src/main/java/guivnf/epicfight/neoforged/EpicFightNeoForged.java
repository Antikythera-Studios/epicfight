package guivnf.epicfight.neoforged;

import com.yesman.akythera.api.data.modularize.ScriptType;
import com.yesman.akythera.core.registry.AkytheraRegistries;
import guivnf.epicfight.EpicFight;
import guivnf.epicfight.akythera.registries.EpicFightScriptTypes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(EpicFight.MODID)
public final class EpicFightNeoForged {
    public EpicFightNeoForged(final IEventBus modEventBus) {
        EpicFight.registerDataBlockEnumClasses();

        DeferredRegister<ScriptType> scriptTypes = DeferredRegister.create(AkytheraRegistries.Frozen.SCRIPT_TYPE_KEY, EpicFight.MODID);
        EpicFightScriptTypes.register((name, type) -> scriptTypes.register(name, () -> type));
        scriptTypes.register(modEventBus);
    }
}
