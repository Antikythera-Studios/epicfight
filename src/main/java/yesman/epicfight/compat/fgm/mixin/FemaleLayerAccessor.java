package yesman.epicfight.compat.fgm.mixin;

import com.wildfire.render.GenderLayer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;

/// On NeoForge, GenderLayer had a private getBreastTexture method accessed via @Invoker.
/// The Fabric version of Wildfire Gender does not have this method, so this mixin
/// is kept only as a type tag for instanceof checks in WildfireFGMCompat.
/// Texture retrieval is handled directly via AbstractClientPlayer.getSkin().texture().
@Mixin(GenderLayer.class)
public interface FemaleLayerAccessor<E extends LivingEntity, M extends HumanoidModel<E>> {
}
