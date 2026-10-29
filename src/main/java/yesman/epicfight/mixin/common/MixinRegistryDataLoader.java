package yesman.epicfight.mixin.common;

import net.minecraft.resources.RegistryDataLoader;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yesman.epicfight.client.online.cosmetics.Emote;
import yesman.epicfight.registry.EpicFightRegistries;

import java.util.ArrayList;
import java.util.List;

/// Adds the EMOTE registry to the data pack registries.
/// On NeoForge, this is done via [DataPackRegistryEvent.dataPackRegistry] which registers
/// both the element codec (for JSON loading) and the network codec (for client synchronization).
/// On Fabric, we inject into the static initializer of [RegistryDataLoader] to add our registry
/// to both [WORLDGEN_REGISTRIES] (for data pack loading) and [SYNCHRONIZED_REGISTRIES]
/// (for client-server synchronization in multiplayer).
/// This makes EMOTE a dynamic registry accessible via [RegistryAccess].
@Mixin(RegistryDataLoader.class)
public class MixinRegistryDataLoader {
    @Shadow @Final @Mutable
    private static List<RegistryDataLoader.RegistryData<?>> WORLDGEN_REGISTRIES;

    @Shadow @Final @Mutable
    private static List<RegistryDataLoader.RegistryData<?>> SYNCHRONIZED_REGISTRIES;

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void epicfight$addEmoteRegistry(CallbackInfo ci) {
        // Add EMOTE to WORLDGEN_REGISTRIES for data pack loading.
        // Vanilla will automatically load JSON files from data/<namespace>/emote/ using Emote.CODEC.
        List<RegistryDataLoader.RegistryData<?>> newWorldgenList = new ArrayList<>(WORLDGEN_REGISTRIES);
        newWorldgenList.add(new RegistryDataLoader.RegistryData<>(EpicFightRegistries.Keys.EMOTE, Emote.CODEC, false));
        WORLDGEN_REGISTRIES = newWorldgenList;

        // Add EMOTE to SYNCHRONIZED_REGISTRIES for multiplayer client-server synchronization.
        // NeoForge passes both element codec and network codec to dataPackRegistry(),
        // which means the registry is synchronized to clients. Without this, multiplayer
        // clients won't have the EMOTE registry and the Emote wheel will be empty.
        List<RegistryDataLoader.RegistryData<?>> newSyncList = new ArrayList<>(SYNCHRONIZED_REGISTRIES);
        newSyncList.add(new RegistryDataLoader.RegistryData<>(EpicFightRegistries.Keys.EMOTE, Emote.CODEC, false));
        SYNCHRONIZED_REGISTRIES = newSyncList;
    }
}
