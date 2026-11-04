package yesman.epicfight.mixin.common;

import net.minecraft.core.MappedRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/// Accessor for [MappedRegistry] that allows toggling the frozen flag.
/// This is needed for datapack-driven registries (emote, weapon_data) that are
/// populated during resource reload, after the registry has been frozen during bootstrap.
/// On NeoForge, DataPackRegistryEvent handles this automatically.
@Mixin(MappedRegistry.class)
public interface WritableRegistryAccessor {
    @Accessor("frozen")
    @Mutable
    void setFrozen(boolean frozen);
}
