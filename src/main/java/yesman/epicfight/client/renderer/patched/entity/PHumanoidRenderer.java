package yesman.epicfight.client.renderer.patched.entity;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.ElytraLayer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import yesman.epicfight.api.animation.JointTransform;
import yesman.epicfight.api.animation.Pose;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.api.utils.math.Vec3f;
import yesman.epicfight.client.mesh.HumanoidMesh;
import yesman.epicfight.client.renderer.patched.layer.PatchedElytraLayer;
import yesman.epicfight.client.renderer.patched.layer.PatchedHeadLayer;
import yesman.epicfight.client.renderer.patched.layer.PatchedItemInHandLayer;
import yesman.epicfight.client.renderer.patched.layer.WearableItemLayer;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

public class PHumanoidRenderer<E extends LivingEntity, T extends LivingEntityPatch<E>, M extends HumanoidModel<E>, R extends LivingEntityRenderer<E, M>, AM extends HumanoidMesh> extends PatchedLivingEntityRenderer<E, T, M, R, AM> {
	private final AssetAccessor<AM> mesh;
	
	public PHumanoidRenderer(AssetAccessor<AM> mesh, EntityRendererProvider.Context context, EntityType<?> entityType) {
		super(context, entityType);
		
		this.mesh = mesh;
		this.addPatchedLayer(ElytraLayer.class, new PatchedElytraLayer<>());
		this.addPatchedLayer(ItemInHandLayer.class, new PatchedItemInHandLayer<>());
		this.addPatchedLayer(HumanoidArmorLayer.class, new WearableItemLayer<>(mesh, false, context.getModelManager()));
		this.addPatchedLayer(CustomHeadLayer.class, new PatchedHeadLayer<>());
	}
	
	@Override
	public void setJointTransforms(T entitypatch, Armature armature, Pose pose, float partialTicks) {
		if (entitypatch.getOriginal().isBaby()) {
			pose.orElseEmpty("Head").frontResult(JointTransform.scale(new Vec3f(1.25F, 1.25F, 1.25F)), OpenMatrix4f::mul);
		}
	}
	
	@Override
	protected float getDefaultLayerHeightCorrection() {
		return 0.75F;
	}
	
	@Override
	public AssetAccessor<AM> getDefaultMesh() {
		return this.mesh;
	}
}