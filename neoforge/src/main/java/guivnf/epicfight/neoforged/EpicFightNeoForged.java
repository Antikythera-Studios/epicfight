package guivnf.epicfight.neoforged;

import guivnf.epicfight.EpicFight;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod(EpicFight.MODID)
public final class EpicFightNeoForged {
    public EpicFightNeoForged(final IEventBus modEventBus) {
        EpicFight.onModConstructed();
        EpicFight.registerDataBlockEnumClasses();
        EpicFight.registerObjects();

        modEventBus.addListener((final FMLCommonSetupEvent event) -> EpicFight.onModInitialized());
    }
}
