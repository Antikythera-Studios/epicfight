package yesman.epicfight.registry.entries;
import yesman.epicfight.EpicFight;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.phys.Vec3;
import yesman.epicfight.registry.deferred_shim.DeferredHolderShim;
import yesman.epicfight.registry.deferred_shim.DeferredRegisterShim;
import yesman.epicfight.api.animation.SynchedAnimationVariableKey;
import yesman.epicfight.api.animation.SynchedAnimationVariableKey.SynchedIndependentAnimationVariableKey;
import yesman.epicfight.api.utils.ByteBufCodecsExtends;
import yesman.epicfight.main.EpicFightMod;
import yesman.epicfight.registry.EpicFightRegistries;

public final class EpicFightSynchedAnimationVariableKeys {
	private EpicFightSynchedAnimationVariableKeys() {}
	
	public static final DeferredRegisterShim<SynchedAnimationVariableKey<?>> REGISTRY = new DeferredRegisterShim<>(EpicFightRegistries.Keys.SYNCHED_ANIMATION_VARIABLE_KEY, EpicFight.MODID);
	
	public static final DeferredHolderShim<SynchedAnimationVariableKey<?>, SynchedIndependentAnimationVariableKey<Vec3>> DESTINATION =
		REGISTRY.register("destination", () -> 
			SynchedAnimationVariableKey.independent(animator -> animator.getEntityPatch().getOriginal().position(), true, ByteBufCodecsExtends.VEC3)
		);
	
	public static final DeferredHolderShim<SynchedAnimationVariableKey<?>, SynchedIndependentAnimationVariableKey<Integer>> TARGET_ENTITY =
		REGISTRY.register("target_entity", () ->
			SynchedAnimationVariableKey.independent(animator -> -1, true, ByteBufCodecs.INT)
		);
	
	public static final DeferredHolderShim<SynchedAnimationVariableKey<?>, SynchedIndependentAnimationVariableKey<Float>> Y_ROT =
		REGISTRY.register("y_rot", () ->
			SynchedAnimationVariableKey.independent(animator -> animator.getEntityPatch().getOriginal().getYRot(), true, ByteBufCodecs.FLOAT)
		);
	
	public static final DeferredHolderShim<SynchedAnimationVariableKey<?>, SynchedIndependentAnimationVariableKey<Integer>> CHARGING_TICKS =
		REGISTRY.register("charging_ticks", () ->
			SynchedAnimationVariableKey.independent(animator ->  0, true, ByteBufCodecs.INT)
		);
}
