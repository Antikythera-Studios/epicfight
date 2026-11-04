package yesman.epicfight.client.renderer.patched.entity;

import net.minecraft.client.model.RavagerModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RavagerRenderer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Ravager;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.client.model.Meshes;
import yesman.epicfight.client.mesh.RavagerMesh;
import yesman.epicfight.world.capabilities.entitypatch.mob.RavagerPatch;

public class PRavagerRenderer extends PatchedLivingEntityRenderer<Ravager, RavagerPatch, RavagerModel, RavagerRenderer, RavagerMesh> {
	public PRavagerRenderer(EntityRendererProvider.Context context, EntityType<?> entityType) {
		super(context, entityType);
	}
	
	@Override
	public AssetAccessor<RavagerMesh> getDefaultMesh() {
		return Meshes.RAVAGER;
	}
}