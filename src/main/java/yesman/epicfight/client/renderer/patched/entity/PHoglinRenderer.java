package yesman.epicfight.client.renderer.patched.entity;

import net.minecraft.client.model.HoglinModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.hoglin.HoglinBase;
import yesman.epicfight.api.animation.JointTransform;
import yesman.epicfight.api.animation.Pose;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.client.model.Meshes;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.api.utils.math.Vec3f;
import yesman.epicfight.client.mesh.HoglinMesh;
import yesman.epicfight.world.capabilities.entitypatch.MobPatch;

public class PHoglinRenderer<E extends Mob & HoglinBase, T extends MobPatch<E>> extends PatchedLivingEntityRenderer<E, T, HoglinModel<E>, MobRenderer<E, HoglinModel<E>>, HoglinMesh> {
	public PHoglinRenderer(EntityRendererProvider.Context context, EntityType<?> entityType) {
		super(context, entityType);
	}
	
	@Override
	public void setJointTransforms(T entitypatch, Armature armature, Pose pose, float partialTicks) {
		if (entitypatch.getOriginal().isBaby()) {
			pose.orElseEmpty("Head").frontResult(JointTransform.scale(new Vec3f(1.25F, 1.25F, 1.25F)), OpenMatrix4f::mul);
		}
	}
	
	@Override
	public AssetAccessor<HoglinMesh> getDefaultMesh() {
		return Meshes.HOGLIN;
	}
}