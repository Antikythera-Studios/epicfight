package yesman.epicfight.compat.simplytooltips;


import net.sweenus.simplytooltips.api.TooltipProviderRegistry;
import yesman.epicfight.compat.ICompatModule;

public class SimplyTooltipsModule implements ICompatModule {
    @Override
	public void onInitialize() {

    }

    @Override
	public void onInitializeServer() {

    }

    @Override
	public void onInitializeClient() {
		TooltipProviderRegistry.register(new EpicFightTooltipProvider(), 2);
    }

    @Override
	public void onInitializeClientServer() {

    }
}
