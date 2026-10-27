package yesman.epicfight.client.events;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import yesman.epicfight.api.client.model.SoftBodyTranslatable;
import yesman.epicfight.api.client.physics.cloth.ClothSimulatable;
import yesman.epicfight.client.ClientEngine;
import yesman.epicfight.client.particle.AirBurstParticle;
import yesman.epicfight.client.particle.AnimationTrailParticle;
import yesman.epicfight.client.particle.AshDirectionalParticle;
import yesman.epicfight.client.particle.BladeRushParticle;
import yesman.epicfight.client.particle.BloodParticle;
import yesman.epicfight.client.particle.CatharsisParticle;
import yesman.epicfight.client.particle.CutParticle;
import yesman.epicfight.client.particle.DustParticle;
import yesman.epicfight.client.particle.EnderParticle;
import yesman.epicfight.client.particle.EntityAfterimageParticle;
import yesman.epicfight.client.particle.EviscerateParticle;
import yesman.epicfight.client.particle.FeatherParticle;
import yesman.epicfight.client.particle.ForceFieldEndParticle;
import yesman.epicfight.client.particle.ForceFieldParticle;
import yesman.epicfight.client.particle.GroundSlamParticle;
import yesman.epicfight.client.particle.HitBluntParticle;
import yesman.epicfight.client.particle.HitCutParticle;
import yesman.epicfight.client.particle.LaserParticle;
import yesman.epicfight.client.particle.ProjectileTrailParticle;
import yesman.epicfight.client.particle.TsunamiSplashParticle;
import yesman.epicfight.client.renderer.blockentity.FractureBlockRenderer;
import yesman.epicfight.client.renderer.entity.DroppedNetherStarRenderer;
import yesman.epicfight.client.renderer.entity.WitherGhostRenderer;
import yesman.epicfight.client.renderer.entity.WitherSkeletonMinionRenderer;
import yesman.epicfight.client.renderer.patched.item.RenderItemBase;
import yesman.epicfight.api.client.model.rebake.BodyDeformation;
import yesman.epicfight.api.client.model.rebake.ModelRebaker;
import yesman.epicfight.client.renderer.patched.layer.WearableItemLayer;
import yesman.epicfight.main.EpicFightMod;
import yesman.epicfight.particle.EpicFightParticles;
import yesman.epicfight.skill.SkillCategory;
import yesman.epicfight.world.entity.EpicFightEntities;
import yesman.epicfight.world.level.block.entity.EpicFightBlockEntities;

@Mod.EventBusSubscriber(modid=EpicFightMod.MODID, value=Dist.CLIENT, bus=EventBusSubscriber.Bus.MOD)
public class ClientModBusEvent {
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onParticleRegistry(final RegisterParticleProvidersEvent event) {
    	event.registerSpriteSet(EpicFightParticles.ENDERMAN_DEATH_EMIT.get(), EnderParticle.EndermanDeathEmitProvider::new);
    	event.registerSpriteSet(EpicFightParticles.HIT_BLUNT.get(), HitBluntParticle.Provider::new);
    	event.registerSpecial(EpicFightParticles.HIT_BLADE.get(), new HitCutParticle.Provider());
    	event.registerSpriteSet(EpicFightParticles.CUT.get(), CutParticle.Provider::new);
    	event.registerSpriteSet(EpicFightParticles.NORMAL_DUST.get(), DustParticle.NormalDustProvider::new);
    	event.registerSpriteSet(EpicFightParticles.DUST_EXPANSIVE.get(), DustParticle.ExpansiveDustProvider::new);
    	event.registerSpriteSet(EpicFightParticles.DUST_CONTRACTIVE.get(), DustParticle.ContractiveDustProvider::new);
    	event.registerSpecial(EpicFightParticles.EVISCERATE.get(), new EviscerateParticle.Provider());
    	event.registerSpriteSet(EpicFightParticles.BLOOD.get(), BloodParticle.Provider::new);
    	event.registerSpriteSet(EpicFightParticles.BLADE_RUSH_SKILL.get(), BladeRushParticle.Provider::new);
    	event.registerSpecial(EpicFightParticles.GROUND_SLAM.get(), new GroundSlamParticle.Provider());
    	event.registerSpriteSet(EpicFightParticles.BREATH_FLAME.get(), EnderParticle.BreathFlameProvider::new);
    	event.registerSpecial(EpicFightParticles.FORCE_FIELD.get(), new ForceFieldParticle.Provider());
    	event.registerSpecial(EpicFightParticles.FORCE_FIELD_END.get(), new ForceFieldEndParticle.Provider());
    	event.registerSpecial(EpicFightParticles.ADRENALINE_PLAYER_BEATING.get(), new EntityAfterimageParticle.AdrenalineParticleProvider());
    	event.registerSpecial(EpicFightParticles.WHITE_AFTERIMAGE.get(), new EntityAfterimageParticle.WhiteAfterimageProvider());
    	event.registerSpecial(EpicFightParticles.LASER.get(), new LaserParticle.Provider());
    	event.registerSpecial(EpicFightParticles.NEUTRALIZE.get(), new DustParticle.ExpansiveMetaParticle.Provider());
    	event.registerSpecial(EpicFightParticles.BOSS_CASTING.get(), new DustParticle.ContractiveMetaParticle.Provider());
    	event.registerSpriteSet(EpicFightParticles.TSUNAMI_SPLASH.get(), TsunamiSplashParticle.Provider::new);
    	event.registerSpecial(EpicFightParticles.SWING_TRAIL.get(), new AnimationTrailParticle.Provider());
    	event.registerSpecial(EpicFightParticles.PROJECTILE_TRAIL.get(), new ProjectileTrailParticle.Provider());
    	event.registerSpriteSet(EpicFightParticles.FEATHER.get(), FeatherParticle.Provider::new);
    	event.registerSpecial(EpicFightParticles.AIR_BURST.get(), new AirBurstParticle.Provider());
    	event.registerSpriteSet(EpicFightParticles.ASH_DIRECTIONAL.get(), AshDirectionalParticle.Provider::new);
    	event.registerSpriteSet(EpicFightParticles.CATHARSIS.get(), CatharsisParticle.Provider::new);
    }
	
	@SubscribeEvent
	public static void registerRenderersEvent(RegisterRenderers event) {
		event.registerEntityRenderer(EpicFightEntities.AREA_EFFECT_BREATH.get(), NoopRenderer::new);
		event.registerEntityRenderer(EpicFightEntities.DROPPED_NETHER_STAR.get(), DroppedNetherStarRenderer::new);
		event.registerEntityRenderer(EpicFightEntities.DEATH_HARVEST_ORB.get(), NoopRenderer::new);
		event.registerEntityRenderer(EpicFightEntities.DODGE_LOCATION_INDICATOR.get(), NoopRenderer::new);
		event.registerEntityRenderer(EpicFightEntities.WITHER_GHOST_CLONE.get(), WitherGhostRenderer::new);
		event.registerEntityRenderer(EpicFightEntities.WITHER_SKELETON_MINION.get(), WitherSkeletonMinionRenderer::new);
		
		event.registerBlockEntityRenderer(EpicFightBlockEntities.FRACTURE.get(), FractureBlockRenderer::new);
	}
	
	/**
	 * Not directly related, but used this method to initialize {@link RenderItemBase#itemRenderer} and {@link RenderItemBase#itemInHandRenderer} because the event called right after gameRenerer created
	 * @param event
	 */
	@SubscribeEvent
	public static void registerStageEvent(RenderLevelStageEvent.RegisterStageEvent event) {
		RenderItemBase.initItemRenderers(Minecraft.getInstance());
	}
	
	@SubscribeEvent
	public static void addLayersEvent(EntityRenderersEvent.AddLayers event) {
		WearableItemLayer.clearModels();
		ModelRebaker.clear();
		BodyDeformation.invalidateRest();
		ClientEngine.getInstance().renderEngine.reloadEntityRenderers(event.getContext());
		SoftBodyTranslatable.TRACKING_SIMULATION_SUBJECTS.removeIf(ClothSimulatable::invalid);
		
		for (ClothSimulatable simOwner : SoftBodyTranslatable.TRACKING_SIMULATION_SUBJECTS) {
			simOwner.getClothSimulator().getAllRunningObjects().forEach((entry) -> {
				simOwner.getClothSimulator().restart(entry.getKey());
			});
		}
	}
	
	@SubscribeEvent
	public static void registerGuiOverlaysEvent(RegisterGuiOverlaysEvent event) {
		event.registerAboveAll("stamina_bar", ClientEngine.getInstance().renderEngine.battleModeUI::renderStaminaBar);
		event.registerAboveAll("skills", ClientEngine.getInstance().renderEngine.battleModeUI::renderNormalSkills);
		event.registerAboveAll("weapon_innate", ClientEngine.getInstance().renderEngine.battleModeUI::renderWeaponInnateSkill);
		event.registerAboveAll("charging_bar", ClientEngine.getInstance().renderEngine.battleModeUI::renderCharingBar);
	}
	
	@SubscribeEvent
	public static void registerAdditionalEvent(ModelEvent.RegisterAdditional event) {
		SkillCategory.ENUM_MANAGER.universalValues().stream().filter(skillCategory -> !skillCategory.bookIcon().equals(SkillCategory.DEFAULT_BOOK_ICON)).forEach(skillCategory -> {
			event.register(new ModelResourceLocation(skillCategory.bookIcon(), "inventory"));
		});
	}
	
	@SubscribeEvent
	public static void modifyBakingResultEvent(ModelEvent.ModifyBakingResult event) {
		ModelResourceLocation skillbookLocation = new ModelResourceLocation(SkillCategory.DEFAULT_BOOK_ICON, "inventory");
		
		if (event.getModels().containsKey(skillbookLocation)) {
			List<ItemOverrides.BakedOverride> skillCategoryOverrides = new ArrayList<> ();
			
			SkillCategory.ENUM_MANAGER.universalValues().stream().filter(skillCategory -> !skillCategory.bookIcon().equals(SkillCategory.DEFAULT_BOOK_ICON)).sorted((c1, c2) -> {
				return Integer.compare(c2.universalOrdinal(), c1.universalOrdinal());
			}).forEach(skillCategory -> {
				ModelResourceLocation model = new ModelResourceLocation(skillCategory.bookIcon(), "inventory");
				ItemOverrides.PropertyMatcher[] propertyMatchers = new ItemOverrides.PropertyMatcher[1];
				propertyMatchers[0] = new ItemOverrides.PropertyMatcher(0, skillCategory.universalOrdinal());
				BakedModel bakedModel = event.getModelBakery().getBakedTopLevelModels().get(model);
				skillCategoryOverrides.add(new ItemOverrides.BakedOverride(propertyMatchers, bakedModel));
			});
			
			ItemOverrides overrides = event.getModels().get(skillbookLocation).getOverrides();
			overrides.overrides = skillCategoryOverrides.toArray(i -> new ItemOverrides.BakedOverride[i]);
			overrides.properties = new ResourceLocation[]{EpicFightMod.identifier("skill")};
		}
	}
}
