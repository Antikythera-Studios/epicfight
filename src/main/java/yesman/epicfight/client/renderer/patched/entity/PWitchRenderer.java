package yesman.epicfight.client.renderer.patched.entity;

import net.minecraft.client.model.WitchModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.WitchRenderer;
import net.minecraft.client.renderer.entity.layers.WitchItemLayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Witch;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.client.model.Meshes;
import yesman.epicfight.client.mesh.VillagerMesh;
import yesman.epicfight.client.renderer.patched.layer.PatchedItemInHandLayer;
import yesman.epicfight.world.capabilities.entitypatch.mob.WitchPatch;

public class PWitchRenderer extends PatchedLivingEntityRenderer<Witch, WitchPatch, WitchModel<Witch>, WitchRenderer, VillagerMesh> {
	public PWitchRenderer(EntityRendererProvider.Context context, EntityType<?> entityType) {
		super(context, entityType);
		this.addPatchedLayer(WitchItemLayer.class, new PatchedItemInHandLayer<>());
	}
	
	@Override
	public AssetAccessor<VillagerMesh> getDefaultMesh() {
		return Meshes.WITCH;
	}
}