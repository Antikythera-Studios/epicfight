package yesman.epicfight.client.renderer.patched.entity;

import net.minecraft.client.model.ZombieVillagerModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ZombieVillagerRenderer;
import net.minecraft.client.renderer.entity.layers.VillagerProfessionLayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.ZombieVillager;
import yesman.epicfight.api.client.model.Meshes;
import yesman.epicfight.client.mesh.VillagerMesh;
import yesman.epicfight.client.renderer.patched.layer.PatchedVillagerProfessionLayer;
import yesman.epicfight.world.capabilities.entitypatch.MobPatch;

public class PZombieVillagerRenderer extends PHumanoidRenderer<ZombieVillager, MobPatch<ZombieVillager>, ZombieVillagerModel<ZombieVillager>, ZombieVillagerRenderer, VillagerMesh> {
	public PZombieVillagerRenderer(EntityRendererProvider.Context context, EntityType<?> entityType) {
		super(Meshes.VILLAGER_ZOMBIE, context, entityType);
		
		this.addPatchedLayer(VillagerProfessionLayer.class, new PatchedVillagerProfessionLayer());
	}
}