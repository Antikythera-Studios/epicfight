package yesman.epicfight.data.conditions.entity;
import net.fabricmc.api.Environment;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.fabricmc.api.EnvType;

import yesman.epicfight.api.utils.side.ClientOnly;
import yesman.epicfight.data.conditions.Condition.EntityPatchCondition;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

import java.util.List;

public class TargetInEyeHeight extends EntityPatchCondition {
	@Override
	public TargetInEyeHeight read(CompoundTag tag) {
		return this;
	}
	
	@Override
	public CompoundTag serializePredicate() {
		return new CompoundTag();
	}
	
	@Override
	public boolean predicate(LivingEntityPatch<?> target) {
		// When the mob is a passenger (chicken jockey, skeleton trap, etc.) its Y is offset upward
		// by the vehicle. Compare against the vehicle's Y so the rider can still hit a target at
		// ground level despite being lifted; otherwise the eye-height check fails and the rider
		// never triggers its attack behaviors.
		double mobY = target.getOriginal().isPassenger() && target.getOriginal().getVehicle() != null
			? target.getOriginal().getVehicle().getY()
			: target.getOriginal().getY();
		double veticalDistance = Math.abs(mobY - target.getTarget().getY());
		return veticalDistance < target.getOriginal().getEyeHeight();
	}
	
	@Override @ClientOnly
    @Environment(EnvType.CLIENT) // TODO: Remove @Environment annotation and completely decouple the widget provider code
	public List<ParameterEditor> getAcceptingParameters(Screen screen) {
		return List.of();
	}
}
