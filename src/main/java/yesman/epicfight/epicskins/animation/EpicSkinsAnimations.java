package yesman.epicfight.epicskins.animation;
import yesman.epicfight.EpicFight;



import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.AnimationManager.AnimationBuilder;
import yesman.epicfight.api.animation.AnimationManager.AnimationRegistryEvent;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.gameasset.Armatures;
import yesman.epicfight.main.EpicFightMod;


public class EpicSkinsAnimations {
	public static AnimationAccessor<StaticAnimation> BIPED_IDLE1;
	public static AnimationAccessor<StaticAnimation> BIPED_IDLE2;
	public static AnimationAccessor<StaticAnimation> BIPED_STANDING;
	
	
	public static void registerAnimations(AnimationRegistryEvent event) {
		event.newBuilder(EpicFight.EPICSKINS_MODID, EpicSkinsAnimations::build);
	}
	
	public static void build(AnimationBuilder builder) {	
		BIPED_IDLE1 = builder.nextAccessor("biped/skinscreen_idle1", (accessor) -> new StaticAnimation(false, accessor, Armatures.BIPED));
		BIPED_IDLE2 = builder.nextAccessor("biped/skinscreen_idle2", (accessor) -> new StaticAnimation(false, accessor, Armatures.BIPED));
		BIPED_STANDING = builder.nextAccessor("biped/skinscreen_stand", (accessor) -> new StaticAnimation(false, accessor, Armatures.BIPED));
	}
}
