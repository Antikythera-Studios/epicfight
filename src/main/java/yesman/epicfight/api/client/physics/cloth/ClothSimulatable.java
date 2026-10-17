package yesman.epicfight.api.client.physics.cloth;

import javax.annotation.Nullable;

import net.minecraft.world.phys.Vec3;
import yesman.epicfight.api.animation.Animator;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.api.physics.SimulatableObject;

public interface ClothSimulatable extends SimulatableObject {
	@Nullable
	Armature getArmature();
	
	@Nullable
	Animator getSimulatableAnimator();
	
	boolean invalid();
	public Vec3 getObjectVelocity();
	public float getYRot();
	public float getYRotO();
	
	// Cloth object requires providing location info for 2 steps before for accurate continuous collide detection.
	public Vec3 getAccurateCloakLocation(float partialFrame);
	public Vec3 getAccuratePartialLocation(float partialFrame);
	public float getAccurateYRot(float partialFrame);
	public float getYRotDelta(float partialFrame);
	public float getScale();
	public float getGravity();
	
	ClothSimulator getClothSimulator();
}