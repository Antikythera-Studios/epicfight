package yesman.epicfight.mixin.client;

import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LightTexture.class)
public interface LightTextureAccessor {
    @Accessor("lightTexture")
    DynamicTexture epicfight$getLightTexture();
}
