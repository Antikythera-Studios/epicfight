package yesman.epicfight.client.renderer.patched.entity;

import net.minecraft.client.model.SpiderModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.SpiderRenderer;
import net.minecraft.client.renderer.entity.layers.SpiderEyesLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Spider;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.client.model.Meshes;
import yesman.epicfight.client.mesh.SpiderMesh;
import yesman.epicfight.client.renderer.patched.layer.PatchedEyesLayer;
import yesman.epicfight.world.capabilities.entitypatch.mob.SpiderPatch;

public class PSpiderRenderer extends PatchedLivingEntityRenderer<Spider, SpiderPatch<Spider>, SpiderModel<Spider>, SpiderRenderer<Spider>, SpiderMesh> {
	private static final ResourceLocation SPIDER_EYE_TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/spider_eyes.png");
	
	public PSpiderRenderer(EntityRendererProvider.Context context, EntityType<?> entityType) {
		super(context, entityType);
		this.addPatchedLayer(SpiderEyesLayer.class, new PatchedEyesLayer<>(SPIDER_EYE_TEXTURE, Meshes.SPIDER));
	}
	
	@Override
	public AssetAccessor<SpiderMesh> getDefaultMesh() {
		return Meshes.SPIDER;
	}
}