package yesman.epicfight.client.renderer.patched.layer;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.LivingEntity;
import yesman.epicfight.api.client.model.rebake.LayerAttachment;
import yesman.epicfight.api.client.model.rebake.VanillaPoseSync;
import yesman.epicfight.api.client.model.rebake.WornRebaker;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.main.EpicFightMod;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

public class SyncedModelLayer<E extends LivingEntity, T extends LivingEntityPatch<E>, M extends EntityModel<E>> extends PatchedLayer<E, T, M, RenderLayer<E, M>> {
	private static final java.util.Set<String> QUIET = com.google.common.collect.Sets.newHashSet();

	private static String keyFor(RenderLayer<?, ?> layer, LivingEntity entity) {
		String base = "layer/" + layer.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(layer));

		if (!(layer instanceof net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer<?, ?, ?>)) {
			return base;
		}

		StringBuilder worn = new StringBuilder(base);

		for (net.minecraft.world.entity.EquipmentSlot slot : new net.minecraft.world.entity.EquipmentSlot[] {
				net.minecraft.world.entity.EquipmentSlot.HEAD, net.minecraft.world.entity.EquipmentSlot.CHEST,
				net.minecraft.world.entity.EquipmentSlot.LEGS, net.minecraft.world.entity.EquipmentSlot.FEET }) {
			net.minecraft.resources.ResourceLocation id = net.minecraftforge.registries.ForgeRegistries.ITEMS
				.getKey(entity.getItemBySlot(slot).getItem());
			worn.append('/').append(id == null ? "-" : id);
		}

		return worn.toString();
	}

	@Override
	protected void renderLayer(T entitypatch, E entityliving, RenderLayer<E, M> vanillaLayer, PoseStack poseStack, MultiBufferSource buffer, int packedLight,
			OpenMatrix4f[] poses, float bob, float yRot, float xRot, float partialTicks) {
		if (vanillaLayer == null) {
			return;
		}

		String key = keyFor(vanillaLayer, entityliving);
		java.util.List<WornRebaker.Baked> worn = WornRebaker.bake(key,
			entityliving, entitypatch.getArmature(),
			(atRest, buffers) -> vanillaLayer.render(atRest, buffers, 0, entityliving, 0.0F, 0.0F, partialTicks, bob, yRot, xRot));

		if (worn != null) {
			java.util.List<net.minecraft.client.renderer.RenderType> now = WornRebaker.typesNow(key, worn,
				(atRest, buffers) -> vanillaLayer.render(atRest, buffers, packedLight, entityliving,
					0.0F, 0.0F, partialTicks, bob, yRot, xRot));

			if (now == null) {
				if (QUIET.add(key)) {
					StringBuilder equipped = new StringBuilder();

					for (net.minecraft.world.entity.EquipmentSlot slot : net.minecraft.world.entity.EquipmentSlot.values()) {
						equipped.append(equipped.length() == 0 ? "" : ", ").append(slot.getName()).append('=')
							.append(net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(
								entityliving.getItemBySlot(slot).getItem()));
					}

				}

				return;
			}

			for (int i = 0; i < worn.size(); i++) {
				WornRebaker.Baked pass = worn.get(i);
				pass.mesh().initialize();
				pass.mesh().draw(poseStack, buffer, now.get(i), packedLight,
					1.0F, 1.0F, 1.0F, 1.0F, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,
					entitypatch.getArmature(), poses);
			}

			return;
		}

		poseStack.pushPose();
		VanillaPoseSync.pushPartSpace(poseStack);

		try {
			vanillaLayer.render(poseStack, buffer, packedLight, entityliving, 0.0F, 0.0F, partialTicks, bob, yRot, xRot);
		} catch (Exception e) {
			EpicFightMod.LOGGER.debug("A render layer failed to draw: {}", e.toString());
		}

		poseStack.popPose();
	}
}
