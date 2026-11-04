package yesman.epicfight.client.renderer.patched.entity;

import net.minecraft.client.model.CreeperModel;
import net.minecraft.client.renderer.entity.CreeperRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Creeper;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.client.model.Meshes;
import yesman.epicfight.api.utils.math.Vec2i;
import yesman.epicfight.client.mesh.CreeperMesh;
import yesman.epicfight.world.capabilities.entitypatch.mob.CreeperPatch;

public class PCreeperRenderer extends PatchedLivingEntityRenderer<Creeper, CreeperPatch, CreeperModel<Creeper>, CreeperRenderer, CreeperMesh> {
	public PCreeperRenderer(EntityRendererProvider.Context context, EntityType<?> entityType) {
		super(context, entityType);
	}
	
	@Override
	protected int getOverlayCoord(Creeper entity, CreeperPatch entitypatch, float partialTick) {
		float swelling = entity.getSwelling(partialTick);
		float u = (int) (swelling * 10.0F) % 2 == 0 ? 0.0F : Mth.clamp(swelling, 0.5F, 1.0F);
		int initU = OverlayTexture.u(u);
		int initV = OverlayTexture.v(entity.hurtTime > 0 || entity.deathTime > 0);
		Vec2i coord = new Vec2i(initU, initV);
		entitypatch.getEntityDecorations().modifyOverlay(coord, partialTick);
		
		return OverlayTexture.pack(coord.x, coord.y);
	}
	
	@Override
	public AssetAccessor<CreeperMesh> getDefaultMesh() {
		return Meshes.CREEPER;
	}
}