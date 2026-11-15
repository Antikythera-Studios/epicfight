package yesman.epicfight.client.renderer.patched.entity;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.ArrowLayer;
import net.minecraft.client.renderer.entity.layers.BeeStingerLayer;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import net.minecraft.client.renderer.entity.layers.PlayerItemInHandLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.world.entity.EntityType;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.client.model.Meshes;
import yesman.epicfight.client.events.engine.RenderEngine;
import yesman.epicfight.client.mesh.HumanoidMesh;
import yesman.epicfight.client.renderer.patched.layer.PatchedArrowLayer;
import yesman.epicfight.client.renderer.patched.layer.PatchedBeeStingerLayer;
import yesman.epicfight.client.renderer.patched.layer.PatchedCapeLayer;
import yesman.epicfight.client.renderer.patched.layer.PatchedItemInHandLayer;
import yesman.epicfight.client.world.capabilites.entitypatch.player.AbstractClientPlayerPatch;

public class PPlayerRenderer extends PHumanoidRenderer<AbstractClientPlayer, AbstractClientPlayerPatch<AbstractClientPlayer>, PlayerModel<AbstractClientPlayer>, PlayerRenderer, HumanoidMesh> {
	public PPlayerRenderer(EntityRendererProvider.Context context, EntityType<?> entityType) {
		super(Meshes.BIPED, context, entityType);
		
		this.addPatchedLayer(ArrowLayer.class, new PatchedArrowLayer<> (context));
		this.addPatchedLayer(BeeStingerLayer.class, new PatchedBeeStingerLayer<> ());
		this.addPatchedLayer(CapeLayer.class, new PatchedCapeLayer());
		this.addPatchedLayer(PlayerItemInHandLayer.class, new PatchedItemInHandLayer<> ());
	}
	
	@Override
	protected void prepareModel(HumanoidMesh mesh, AbstractClientPlayer entity, AbstractClientPlayerPatch<AbstractClientPlayer> entitypatch, PlayerRenderer renderer) {
		super.prepareModel(mesh, entity, entitypatch, renderer);
		
		renderer.setModelProperties(entity);
		PlayerModel<AbstractClientPlayer> model = renderer.getModel();
		
		mesh.head.setHidden(!model.head.visible);
		mesh.hat.setHidden(!model.hat.visible);
		mesh.jacket.setHidden(!model.jacket.visible);
		mesh.torso.setHidden(!model.body.visible);
		mesh.leftArm.setHidden(!model.leftArm.visible);
		mesh.leftLeg.setHidden(!model.leftLeg.visible);
		mesh.leftPants.setHidden(!model.leftPants.visible);
		mesh.leftSleeve.setHidden(!model.leftSleeve.visible);
		mesh.rightArm.setHidden(!model.rightArm.visible);
		mesh.rightLeg.setHidden(!model.rightLeg.visible);
		mesh.rightPants.setHidden(!model.rightPants.visible);
		mesh.rightSleeve.setHidden(!model.rightSleeve.visible);

		if (RenderEngine.isLocalPlayerInFirstPerson(entity)) {
			mesh.head.setHidden(true);
			mesh.hat.setHidden(true);
		}
	}
	
	@Override
	public AssetAccessor<HumanoidMesh> getMeshProvider(AbstractClientPlayerPatch<AbstractClientPlayer> entitypatch) {
		return PlayerSkin.Model.SLIM.equals(entitypatch.getOriginal().getSkin().model()) ? Meshes.ALEX : Meshes.BIPED;
	}
	
	@Override
	public AssetAccessor<HumanoidMesh> getDefaultMesh() {
		return Meshes.BIPED;
	}
}