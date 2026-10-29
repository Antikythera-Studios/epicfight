package yesman.epicfight.mixin.common;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;

@Mixin(value = Projectile.class)
public interface MixinProjectile {
	@Invoker("onHitEntity")
	public void invoke_onHitEntity(EntityHitResult pResult);
}
