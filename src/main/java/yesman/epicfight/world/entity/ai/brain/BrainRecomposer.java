package yesman.epicfight.world.entity.ai.brain;
import yesman.epicfight.EpicFight;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.MoveToTargetSink;
import net.minecraft.world.entity.ai.behavior.OneShot;
import net.minecraft.world.entity.ai.behavior.RunOne;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.CrossbowItem;
import yesman.epicfight.main.EpicFightMod;
import yesman.epicfight.world.entity.ai.behavior.AnimatedCombatBehavior;
import yesman.epicfight.world.entity.ai.behavior.BackUpIfTooCloseStopInaction;
import yesman.epicfight.world.entity.ai.behavior.MoveToTargetSinkStopInaction;

public final class BrainRecomposer {
	private static final Map<EntityType<?>, BrainRecomposeFunction> BRAIN_REPLACE_DEST_MAPPER = ImmutableMap.of(
		EntityType.PIGLIN, BrainRecomposer::recomposePiglinBrain,
		EntityType.PIGLIN_BRUTE, BrainRecomposer::recomposePiglinBruteBrain,
		EntityType.HOGLIN, BrainRecomposer::recomposeHoglinBrain,
		EntityType.ZOGLIN, BrainRecomposer::recomposeZoglinBrain
	);
	
	public static void recomposeBrainByType(EntityType<?> entityType, Brain<?> brain, AnimatedCombatBehavior<?> animatedCombatBehavior, MoveToTargetSinkStopInaction chaseBehavior) {
		BrainRecomposeFunction brainRecomposeFunction = BRAIN_REPLACE_DEST_MAPPER.get(entityType);
		if (brainRecomposeFunction == null) {
			EpicFight.LOGGER.error("Failed to find brain recompose function for entity type: {}", entityType);
			return;
		}
		brainRecomposeFunction.recomposeBrain(brain, animatedCombatBehavior, chaseBehavior);
	}
	
	public static void recomposePiglinBrain(Brain<?> brain, AnimatedCombatBehavior<?> animatedCombatBehavior, MoveToTargetSinkStopInaction chaseBehavior) {
		if (animatedCombatBehavior != null) {
			replaceBehavior(brain, Activity.FIGHT, 13, animatedCombatBehavior, OneShot.class, AnimatedCombatBehavior.class);
		}
		
		replaceBehavior(brain, Activity.FIGHT, 11, BehaviorBuilder.triggerIf((entity) -> entity.isHolding(is -> is.getItem() instanceof CrossbowItem), BackUpIfTooCloseStopInaction.create(5, 0.75F)), OneShot.class);
		replaceBehavior(brain, Activity.CORE, 1, chaseBehavior, MoveToTargetSink.class, MoveToTargetSinkStopInaction.class);
		removeBehavior(brain, Activity.CELEBRATE, 15, RunOne.class);
	}
	
	public static void recomposePiglinBruteBrain(Brain<?> brain, AnimatedCombatBehavior<?> animatedCombatBehavior, MoveToTargetSinkStopInaction chaseBehavior) {
		if (animatedCombatBehavior != null) {
			replaceBehavior(brain, Activity.FIGHT, 12, animatedCombatBehavior, OneShot.class, AnimatedCombatBehavior.class);
		}
		
		BrainRecomposer.replaceBehavior(brain, Activity.CORE, 1, chaseBehavior, MoveToTargetSink.class, MoveToTargetSinkStopInaction.class);
	}
	
	public static void recomposeHoglinBrain(Brain<?> brain, AnimatedCombatBehavior<?> animatedCombatBehavior, MoveToTargetSinkStopInaction chaseBehavior) {
		replaceBehavior(brain, Activity.CORE, 1, chaseBehavior, MoveToTargetSink.class, MoveToTargetSinkStopInaction.class);
		replaceBehavior(brain, Activity.FIGHT, 13, animatedCombatBehavior, OneShot.class);
		removeBehavior(brain, Activity.FIGHT, 14, OneShot.class);
	}
	
	public static void recomposeZoglinBrain(Brain<?> brain, AnimatedCombatBehavior<?> animatedCombatBehavior, MoveToTargetSinkStopInaction chaseBehavior) {
		replaceBehavior(brain, Activity.CORE, 1, chaseBehavior, MoveToTargetSink.class, MoveToTargetSinkStopInaction.class);
		replaceBehavior(brain, Activity.FIGHT, 11, animatedCombatBehavior, OneShot.class);
		removeBehavior(brain, Activity.FIGHT, 12, OneShot.class);
	}
	
	public static <E extends LivingEntity> void removeBehavior(Brain<E> brain, Activity activity, int priority, Class<?> targetBehaviorClass) {
		Set<BehaviorControl<? super E>> set = brain.availableBehaviorsByPriority.get(priority).get(activity);
		set.removeIf((behavior) -> targetBehaviorClass.isInstance(behavior));
	}
	
	@SuppressWarnings("unchecked")
	@SafeVarargs
	public static <E extends LivingEntity> void replaceBehavior(Brain<E> brain, Activity activity, int priority, BehaviorControl<?> newBehavior, Class<?>... targetClasses) {
		Set<BehaviorControl<? super E>> set = brain.availableBehaviorsByPriority.get(priority).get(activity);
		boolean removed = set.removeIf((behavior) -> contains(behavior, targetClasses));
		
		if (removed) {
			set.add((BehaviorControl<? super E>) newBehavior);
		}
	}
	
	@SafeVarargs
	private static <E extends LivingEntity> boolean contains(Object behavior, Class<?>... targetClasses) {
		for (Class<?> targetClass : targetClasses) {
			if (targetClass.isAssignableFrom(behavior.getClass())) {
				return true;
			}
		}
		
		return false;
	}
	
	private BrainRecomposer() {}
	
	@FunctionalInterface
	public interface BrainRecomposeFunction {
		public void recomposeBrain(Brain<?> piglinBrain, AnimatedCombatBehavior<?> animatedCombatBehavior, MoveToTargetSinkStopInaction chaseBehavior);
	}
}