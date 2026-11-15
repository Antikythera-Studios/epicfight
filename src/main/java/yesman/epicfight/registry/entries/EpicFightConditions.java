package yesman.epicfight.registry.entries;
import yesman.epicfight.EpicFight;

import java.util.NoSuchElementException;
import java.util.function.Supplier;

import net.minecraft.resources.ResourceLocation;
import yesman.epicfight.registry.deferred_shim.DeferredHolderShim;
import yesman.epicfight.registry.deferred_shim.DeferredRegisterShim;
import yesman.epicfight.data.conditions.Condition;
import yesman.epicfight.data.conditions.entity.HealthPoint;
import yesman.epicfight.data.conditions.entity.OffhandItemCategory;
import yesman.epicfight.data.conditions.entity.PlayerName;
import yesman.epicfight.data.conditions.entity.PlayerSkillActivated;
import yesman.epicfight.data.conditions.entity.RandomChance;
import yesman.epicfight.data.conditions.entity.TargetInDistance;
import yesman.epicfight.data.conditions.entity.TargetInEyeHeight;
import yesman.epicfight.data.conditions.entity.TargetInPov;
import yesman.epicfight.data.conditions.itemstack.TagValueCondition;
import yesman.epicfight.main.EpicFightMod;
import yesman.epicfight.registry.EpicFightRegistries;

public final class EpicFightConditions {
	private EpicFightConditions() {}
	
	public static final DeferredRegisterShim<Supplier<Condition<?>>> REGISTRY = new DeferredRegisterShim<>(EpicFightRegistries.Keys.CONDITION, EpicFight.MODID);
	
	public static <T extends Condition<?>> Supplier<T> getConditionOrThrow(ResourceLocation key) throws NoSuchElementException, ClassCastException {
		if (!EpicFightRegistries.CONDITION.containsKey(key)) {
			throw new NoSuchElementException("No condition named " + key);
		}
		
		return getConditionOrNull(key);
	}
	
	@SuppressWarnings("unchecked")
	public static <T extends Condition<?>> Supplier<T> getConditionOrNull(ResourceLocation key) throws ClassCastException {
		return (Supplier<T>) EpicFightRegistries.CONDITION.get(key);
	}
	
	//EntityPatch conditions
	public static final DeferredHolderShim<Supplier<Condition<?>>, Supplier<Condition<?>>> OFFHAND_ITEM_CATEGORY = REGISTRY.register("offhand_item_category", () -> OffhandItemCategory::new);
	
	public static final DeferredHolderShim<Supplier<Condition<?>>, Supplier<Condition<?>>> PLAYER_SKILL_ACTIVATED = REGISTRY.register("skill_active", () -> PlayerSkillActivated::new);
	public static final DeferredHolderShim<Supplier<Condition<?>>, Supplier<Condition<?>>> PLAYER_NAME = REGISTRY.register("player_name", () -> PlayerName::new);
	public static final DeferredHolderShim<Supplier<Condition<?>>, Supplier<Condition<?>>> HEALTH_POINT = REGISTRY.register("health", () -> HealthPoint::new);
	public static final DeferredHolderShim<Supplier<Condition<?>>, Supplier<Condition<?>>> RANDOM = REGISTRY.register("random_chance", () -> RandomChance::new);
	public static final DeferredHolderShim<Supplier<Condition<?>>, Supplier<Condition<?>>> TARGET_IN_DISTANCE = REGISTRY.register("within_distance", () -> TargetInDistance::new);
	public static final DeferredHolderShim<Supplier<Condition<?>>, Supplier<Condition<?>>> TARGET_IN_EYE_HEIGHT = REGISTRY.register("within_eye_height", () -> TargetInEyeHeight::new);
	public static final DeferredHolderShim<Supplier<Condition<?>>, Supplier<Condition<?>>> TARGET_IN_POV = REGISTRY.register("within_angle", () -> TargetInPov::new);
	public static final DeferredHolderShim<Supplier<Condition<?>>, Supplier<Condition<?>>> TARGET_IN_POV_HORIZONTAL = REGISTRY.register("within_angle_horizontal", () -> TargetInPov.TargetInPovHorizontal::new);

	//Itemstack conditions — upstream 1.20.1 API restored for datapack compat
	public static final DeferredHolderShim<Supplier<Condition<?>>, Supplier<Condition<?>>> TAG_VALUE = REGISTRY.register("tag_value", () -> TagValueCondition::new);
}
