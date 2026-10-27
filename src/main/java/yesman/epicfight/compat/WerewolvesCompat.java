package yesman.epicfight.compat;

import com.mojang.blaze3d.vertex.PoseStack;

import de.teamlapen.werewolves.api.entities.werewolf.WerewolfForm;
import de.teamlapen.werewolves.client.model.WerewolfEarsModel;
import de.teamlapen.werewolves.client.render.layer.HumanWerewolfLayer;
import de.teamlapen.werewolves.entities.player.werewolf.WerewolfPlayer;
import de.teamlapen.werewolves.util.Helper;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.IEventBus;
import yesman.epicfight.api.client.forgeevent.PatchedRenderersEvent;
import yesman.epicfight.api.client.forgeevent.RenderEpicFightPlayerEvent;
import yesman.epicfight.api.client.model.SkinnedMesh;
import yesman.epicfight.api.client.model.transformer.HumanoidModelTransformer;
import yesman.epicfight.api.client.model.transformer.VanillaModelTransformer;
import yesman.epicfight.api.forgeevent.BattleModeSustainableEvent;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.client.renderer.EpicFightRenderTypes;
import yesman.epicfight.client.renderer.patched.entity.PPlayerRenderer;
import yesman.epicfight.client.renderer.patched.layer.PatchedLayer;
import yesman.epicfight.client.world.capabilites.entitypatch.player.AbstractClientPlayerPatch;
import yesman.epicfight.mixin.teamlapen.MixinHumanWerewolfLayer;

public class WerewolvesCompat implements ICompatModule {
	private static final HumanoidModelTransformer VANILLA_TRANSFORMER = new VanillaModelTransformer();

	@Override
	public void onModEventBus(IEventBus eventBus) {
		
	}

	@Override
	public void onForgeEventBus(IEventBus eventBus) {
		eventBus.<BattleModeSustainableEvent>addListener((event) -> {
			WerewolfForm form = WerewolfPlayer.get(event.getPlayerPatch().getOriginal()).getForm();
			
			if (form == WerewolfForm.SURVIVALIST || form == WerewolfForm.BEAST) {
				event.setCanceled(true);
			}
		});
	}
	
	@OnlyIn(Dist.CLIENT)
	@Override
	public void onModEventBusClient(IEventBus eventBus) {
		eventBus.<PatchedRenderersEvent.Modify>addListener((event) -> {
			if (event.get(EntityType.PLAYER) instanceof PPlayerRenderer playerrenderer) {
				playerrenderer.addPatchedLayerAlways(HumanWerewolfLayer.class, new EpicFightHumanWerewolfLayer<> ());
			}
		});
	}
	
	@OnlyIn(Dist.CLIENT)
	@Override
	public void onForgeEventBusClient(IEventBus eventBus) {
		eventBus.<RenderEpicFightPlayerEvent>addListener((event) -> {
			WerewolfForm form = WerewolfPlayer.get(event.getPlayerPatch().getOriginal()).getForm();
			
			if (form == WerewolfForm.SURVIVALIST || form == WerewolfForm.BEAST) {
				event.setShouldRender(false);
			}
		});
	}
	
	@OnlyIn(Dist.CLIENT)
	public static class EpicFightHumanWerewolfLayer<A extends HumanoidModel<AbstractClientPlayer>> extends PatchedLayer<AbstractClientPlayer, AbstractClientPlayerPatch<AbstractClientPlayer>, PlayerModel<AbstractClientPlayer>, HumanWerewolfLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>, A>> {
		private SkinnedMesh mesh;
		private SkinnedMesh slimMesh;
		
		@Override
		protected void renderLayer( AbstractClientPlayerPatch<AbstractClientPlayer> entitypatch
				                  , AbstractClientPlayer entityliving
				                  , HumanWerewolfLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>, A> vanillaLayer
				                  , PoseStack poseStack
				                  , MultiBufferSource buffer
				                  , int packedLight
				                  , OpenMatrix4f[] poses
				                  , float bob
				                  , float yRot
				                  , float xRot
				                  , float partialTicks
				                  )
		{
			@SuppressWarnings("unchecked")
			MixinHumanWerewolfLayer<AbstractClientPlayer, A> accessor = (MixinHumanWerewolfLayer<AbstractClientPlayer, A>)vanillaLayer;
			String modelType = entityliving.getModelName();
			A vanillaModel = accessor.getModel();
			
			if (vanillaModel instanceof WerewolfEarsModel werewolfEars) {
				werewolfEars.head.loadPose(werewolfEars.head.getInitialPose());
				werewolfEars.hat.loadPose(werewolfEars.hat.getInitialPose());
				werewolfEars.body.loadPose(werewolfEars.body.getInitialPose());
				werewolfEars.leftArm.loadPose(werewolfEars.leftArm.getInitialPose());
				werewolfEars.rightArm.loadPose(werewolfEars.rightArm.getInitialPose());
				werewolfEars.leftLeg.loadPose(werewolfEars.leftLeg.getInitialPose());
				werewolfEars.rightLeg.loadPose(werewolfEars.rightLeg.getInitialPose());
			}
			
			SkinnedMesh mesh;
			
			if ("default".equals(modelType)) {
				if (this.mesh == null) {
					this.mesh = VANILLA_TRANSFORMER.transformArmorModel(vanillaModel);
				}
				
				mesh = this.mesh;
			} else {
				if (this.slimMesh == null) {
					this.slimMesh = VANILLA_TRANSFORMER.transformArmorModel(vanillaModel);
				}
				
				mesh = this.slimMesh;
			}
			
			Helper.asIWerewolf(entityliving).filter(werewolf -> werewolf.getForm() == WerewolfForm.HUMAN).ifPresent(werewolf -> {
	            ResourceLocation texture = accessor.getTextures().get(werewolf.getSkinType() % accessor.getTextures().size());
	            RenderType rendertype = EpicFightRenderTypes.getTriangulated(RenderType.entityCutoutNoCull(texture));
	            mesh.draw(poseStack, buffer, rendertype, packedLight, 1.0F, 1.0F, 1.0F, 1.0F, LivingEntityRenderer.getOverlayCoords(entityliving, 0.0F), entitypatch.getArmature(), poses);
	        });
		}
	}
}
