package yesman.epicfight.mixin.client;

import net.minecraft.client.model.ElytraModel;
import net.minecraft.client.renderer.entity.layers.ElytraLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ElytraLayer.class)
public interface ElytraLayerAccessor {
    @Accessor("elytraModel")
    ElytraModel<?> epicfight$getElytraModel();
}
