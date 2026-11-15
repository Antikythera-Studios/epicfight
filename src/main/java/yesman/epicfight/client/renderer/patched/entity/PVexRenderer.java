package yesman.epicfight.client.renderer.patched.entity;

import net.minecraft.client.model.VexModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.VexRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Vex;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.client.model.Meshes;
import yesman.epicfight.client.mesh.VexMesh;
import yesman.epicfight.client.renderer.patched.layer.PatchedItemInHandLayer;
import yesman.epicfight.world.capabilities.entitypatch.mob.VexPatch;

public class PVexRenderer extends PatchedLivingEntityRenderer<Vex, VexPatch, VexModel, VexRenderer, VexMesh> {
	public PVexRenderer(EntityRendererProvider.Context context, EntityType<?> entityType) {
		super(context, entityType);
		this.addPatchedLayer(ItemInHandLayer.class, new PatchedItemInHandLayer<>());
	}
	
	@Override
	public AssetAccessor<VexMesh> getDefaultMesh() {
		return Meshes.VEX;
	}
}