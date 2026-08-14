package com.yesman.epicfight.fabric;

import com.yesman.epicfight.EpicFight;
import com.yesman.epicfight.fabric.networking.ServerBoundNetworkReceiverRegistrar;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

public final class EpicFightFabric implements ModInitializer {
    public EpicFightFabric() {
        EpicFight.onModConstructed();
    }

    @Override
    public void onInitialize() {
        EpicFight.onModInitialized();

        // Initialize networking payload types & receivers
        ServerBoundNetworkReceiverRegistrar.registerTypes();
        ServerBoundNetworkReceiverRegistrar.registerReceivers();

        // Register commands
        CommandRegistrationCallback.EVENT.register((dispatcher, _, _) -> {
            EpicFight.onRegisterCommands(dispatcher);
        });
    }
}
