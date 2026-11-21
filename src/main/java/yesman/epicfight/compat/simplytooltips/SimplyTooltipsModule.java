package yesman.epicfight.compat.simplytooltips;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.sweenus.simplytooltips.api.TooltipProviderRegistry;
import yesman.epicfight.compat.ICompatModule;

public class SimplyTooltipsModule implements ICompatModule {
    @Override
    public void onModEventBus(IEventBus eventBus) {
        eventBus.addListener(FMLCommonSetupEvent.class, setup -> TooltipProviderRegistry.register(new EpicFightTooltipProvider(), 2));
    }

    @Override
    public void onGameEventBus(IEventBus eventBus) {

    }

    @Override
    public void onModEventBusClient(IEventBus eventBus) {

    }

    @Override
    public void onGameEventBusClient(IEventBus eventBus) {

    }
}
