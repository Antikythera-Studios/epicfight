package yesman.epicfight.mixin.common;

import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

/// Yarn mappings: victims is Map<Entity, Integer> (not List<LivingEntity>)
@Mixin(AreaEffectCloud.class)
public interface AreaEffectCloudAccessor {
    @Accessor("victims")
    Map<Entity, Integer> epicfight$getVictims();
}
