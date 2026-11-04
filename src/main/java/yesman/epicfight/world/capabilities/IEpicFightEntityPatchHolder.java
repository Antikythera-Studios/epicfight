package yesman.epicfight.world.capabilities;

import yesman.epicfight.world.capabilities.provider.AttachmentEntityPatchProvider;

/// Interface injected into [net.minecraft.world.entity.Entity] via mixin.
/// Provides access to the entity patch provider without NeoForge's AttachmentType system.
public interface IEpicFightEntityPatchHolder {
    AttachmentEntityPatchProvider epicfight$getEntityPatchProvider();
    void epicfight$setEntityPatchProvider(AttachmentEntityPatchProvider provider);
}
