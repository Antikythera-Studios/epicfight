package yesman.epicfight.client.renderer.patched.layer;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.ZombieVillagerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.VillagerProfessionLayer;
import net.minecraft.client.resources.metadata.animation.VillagerMetaDataSection;
import net.minecraft.core.DefaultedRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerDataHolder;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerType;
import yesman.epicfight.api.client.model.Meshes;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.client.mesh.VillagerMesh;
import yesman.epicfight.mixin.client.VillagerProfessionLayerAccessor;
import yesman.epicfight.world.capabilities.entitypatch.MobPatch;

public class PatchedVillagerProfessionLayer extends ModelRenderLayer<ZombieVillager, MobPatch<ZombieVillager>, ZombieVillagerModel<ZombieVillager>, VillagerProfessionLayer<ZombieVillager, ZombieVillagerModel<ZombieVillager>>, VillagerMesh> {
	public PatchedVillagerProfessionLayer() {
		super(Meshes.VILLAGER_ZOMBIE);
	}
	
	@Override
	protected void renderLayer(MobPatch<ZombieVillager> entitypatch, ZombieVillager entityliving, VillagerProfessionLayer<ZombieVillager, ZombieVillagerModel<ZombieVillager>> vanillaLayer, PoseStack postStack, MultiBufferSource buffer, int packedLight,
			OpenMatrix4f[] poses, float bob, float yRot, float xRot, float partialTicks) {
		
		if (!entityliving.isInvisible()) {
			VillagerData villagerdata = ((VillagerDataHolder)entitypatch.getOriginal()).getVillagerData();
			
			VillagerProfessionLayerAccessor accessor = (VillagerProfessionLayerAccessor) vanillaLayer;
			VillagerMetaDataSection.Hat typeHat = vanillaLayer.getHatData(accessor.epicfight$getTypeHatCache(), "type", BuiltInRegistries.VILLAGER_TYPE, villagerdata.getType());
	        VillagerMetaDataSection.Hat professionHat = vanillaLayer.getHatData(accessor.epicfight$getProfessionHatCache(), "profession", BuiltInRegistries.VILLAGER_PROFESSION, villagerdata.getProfession());
	        
	        if (!(typeHat == VillagerMetaDataSection.Hat.NONE || typeHat == VillagerMetaDataSection.Hat.PARTIAL && professionHat != VillagerMetaDataSection.Hat.FULL)
	        		|| !entityliving.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
	        	this.mesh.get().head.setHidden(true);
	        	this.mesh.get().hat.setHidden(true);
	        }
	        
	        if (!entitypatch.getOriginal().getItemBySlot(EquipmentSlot.LEGS).isEmpty()) {
				this.mesh.get().jacket.setHidden(true);
			}
	        
			this.mesh.get().draw(postStack, buffer, RenderType.entityCutoutNoCull(vanillaLayer.getResourceLocation("type", BuiltInRegistries.VILLAGER_TYPE.getKey(villagerdata.getType()))),
					packedLight, 1.0F, 1.0F, 1.0F, 1.0F, LivingEntityRenderer.getOverlayCoords(entityliving, 0.0F), entitypatch.getArmature(), poses);
			
			if (villagerdata.getProfession() != VillagerProfession.NONE) {
				this.mesh.get().draw(postStack, buffer, RenderType.entityCutoutNoCull(vanillaLayer.getResourceLocation("profession", BuiltInRegistries.VILLAGER_PROFESSION.getKey(villagerdata.getProfession()))),
						packedLight, 1.0F, 1.0F, 1.0F, 1.0F, LivingEntityRenderer.getOverlayCoords(entityliving, 0.0F), entitypatch.getArmature(), poses);
			}
		}
	}
}