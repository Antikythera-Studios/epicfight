package yesman.epicfight.client.renderer.patched.entity;

import net.minecraft.client.model.IronGolemModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.IronGolemRenderer;
import net.minecraft.client.renderer.entity.layers.IronGolemCrackinessLayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.IronGolem;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.client.model.Meshes;
import yesman.epicfight.client.mesh.IronGolemMesh;
import yesman.epicfight.client.renderer.patched.layer.PatchedGolemCrackLayer;
import yesman.epicfight.world.capabilities.entitypatch.mob.IronGolemPatch;

public class PIronGolemRenderer extends PatchedLivingEntityRenderer<IronGolem, IronGolemPatch, IronGolemModel<IronGolem>, IronGolemRenderer, IronGolemMesh> {
	public PIronGolemRenderer(EntityRendererProvider.Context context, EntityType<?> entityType) {
		super(context, entityType);
		this.addPatchedLayer(IronGolemCrackinessLayer.class, new PatchedGolemCrackLayer(Meshes.IRON_GOLEM));
	}
	
	@Override
	public AssetAccessor<IronGolemMesh> getDefaultMesh() {
		return Meshes.IRON_GOLEM;
	}
}