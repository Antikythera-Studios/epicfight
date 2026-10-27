package yesman.epicfight.client.renderer.patched.entity;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.ArrowLayer;
import net.minecraft.client.renderer.entity.layers.BeeStingerLayer;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import net.minecraft.client.renderer.entity.layers.PlayerItemInHandLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.EntityType;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.client.model.Meshes;
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
		
		PlayerModel<AbstractClientPlayer> model = renderer.getModel();
		
		boolean head = !model.head.visible;
		boolean hat = !model.hat.visible;
		boolean jacket = !model.jacket.visible;
		boolean torso = !model.body.visible;
		boolean leftArm = !model.leftArm.visible;
		boolean leftLeg = !model.leftLeg.visible;
		boolean leftPants = !model.leftPants.visible;
		boolean leftSleeve = !model.leftSleeve.visible;
		boolean rightArm = !model.rightArm.visible;
		boolean rightLeg = !model.rightLeg.visible;
		boolean rightPants = !model.rightPants.visible;
		boolean rightSleeve = !model.rightSleeve.visible;
		
		renderer.setModelProperties(entity);
		
		mesh.head.setHidden(head || !model.head.visible);
		mesh.hat.setHidden(hat || !model.hat.visible);
		mesh.jacket.setHidden(jacket || !model.jacket.visible);
		mesh.torso.setHidden(torso || !model.body.visible);
		mesh.leftArm.setHidden(leftArm || !model.leftArm.visible);
		mesh.leftLeg.setHidden(leftLeg || !model.leftLeg.visible);
		mesh.leftPants.setHidden(leftPants || !model.leftPants.visible);
		mesh.leftSleeve.setHidden(leftSleeve || !model.leftSleeve.visible);
		mesh.rightArm.setHidden(rightArm || !model.rightArm.visible);
		mesh.rightLeg.setHidden(rightLeg || !model.rightLeg.visible);
		mesh.rightPants.setHidden(rightPants || !model.rightPants.visible);
		mesh.rightSleeve.setHidden(rightSleeve || !model.rightSleeve.visible);
	}
	
	@Override
	public AssetAccessor<HumanoidMesh> getMeshProvider(AbstractClientPlayerPatch<AbstractClientPlayer> entitypatch) {
		return entitypatch.getOriginal().getModelName().equals("slim") ? Meshes.ALEX : Meshes.BIPED;
	}
	
	@Override
	public AssetAccessor<HumanoidMesh> getDefaultMesh() {
		return Meshes.BIPED;
	}
}
