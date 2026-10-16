package yesman.epicfight.mixin.common;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.core.MappedRegistry;

/// Lets the deferred-register shim reopen a frozen registry for late binds —
/// vanilla/RegistryDataLoader freezes every registry at the end of the first
/// datapack load, but Fabric-side mod constructors can keep registering after
/// that point (NeoForge drains all registrations before freezing).
@Mixin(MappedRegistry.class)
public interface MappedRegistryAccessor {
    @Accessor("frozen")
    boolean epicfight$isFrozen();

    @Accessor("frozen")
    void epicfight$setFrozen(boolean frozen);
}
