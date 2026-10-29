package yesman.epicfight.api.animation.property;

import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import yesman.epicfight.api.animation.AnimationPlayer;
import yesman.epicfight.api.animation.JointTransform;
import yesman.epicfight.api.animation.Keyframe;
import yesman.epicfight.api.animation.TransformSheet;
import yesman.epicfight.api.animation.property.AnimationProperty.ActionAnimationProperty;
import yesman.epicfight.api.animation.property.AnimationProperty.AttackAnimationProperty;
import yesman.epicfight.api.animation.property.AnimationProperty.DestLocationProvider;
import yesman.epicfight.api.animation.property.AnimationProperty.YRotProvider;
import yesman.epicfight.api.animation.types.ActionAnimation;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.api.animation.types.AttackAnimation.Phase;
import yesman.epicfight.api.animation.types.DynamicAnimation;
import yesman.epicfight.api.animation.types.EntityState;
import yesman.epicfight.api.animation.types.grappling.GrapplingAttackAnimation;
import yesman.epicfight.api.utils.math.MathUtils;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.api.utils.math.Vec3f;
import yesman.epicfight.api.utils.math.Vec4f;
import yesman.epicfight.registry.entries.EpicFightSynchedAnimationVariableKeys;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.MobPatch;

public class MoveCoordFunctions {
	/**
	 * Defines a function that how to interpret given coordinate and return the movement vector from entity's current position
	 */
	@FunctionalInterface
	public interface MoveCoordGetter {
		Vec3f get(DynamicAnimation animation, LivingEntityPatch<?> entitypatch, TransformSheet transformSheet, float prevElapsedTime, float elapsedTime);
	}
	
	/**
	 * Defines a function that how to build the coordinate of {@link ActionAnimation}
	 */
	@FunctionalInterface
	public interface MoveCoordSetter {
		void set(DynamicAnimation animation, LivingEntityPatch<?> entitypatch, TransformSheet transformSheet);
	}
	
	/**
	 * MODEL_COORD
	 *  - Calculates the coordinate gap between previous and current elapsed time
	 *  - the coordinate doesn't reflect the entity's rotation
	 */
	public static final MoveCoordGetter MODEL_COORD = (animation, entitypatch, coord, prevElapsedTime, elapsedTime) -> {
		LivingEntity livingentity = entitypatch.getOriginal();
		JointTransform oJt = coord.getInterpolatedTransform(prevElapsedTime);
		JointTransform jt = coord.getInterpolatedTransform(elapsedTime);
		Vec4f prevpos = new Vec4f(oJt.translation());
		Vec4f currentpos = new Vec4f(jt.translation());
		
		OpenMatrix4f rotationTransform = entitypatch.getModelMatrix(1.0F).removeTranslation().removeScale();
		OpenMatrix4f localTransform = entitypatch.getArmature().searchJointByName("Root").getLocalTransform().removeTranslation();
		rotationTransform.mulBack(localTransform);
		currentpos.transform(rotationTransform);
		prevpos.transform(rotationTransform);
		
		boolean hasNoGravity = entitypatch.getOriginal().isNoGravity();
		boolean moveVertical = animation.getProperty(ActionAnimationProperty.MOVE_VERTICAL).orElse(false) || animation.getProperty(ActionAnimationProperty.COORD).isPresent();
		float dx = prevpos.x - currentpos.x;
		float dy = (moveVertical || hasNoGravity) ? currentpos.y - prevpos.y : 0.0F;
		float dz = prevpos.z - currentpos.z;
		dx = Math.abs(dx) > 0.0001F ? dx : 0.0F;
		dz = Math.abs(dz) > 0.0001F ? dz : 0.0F;
		
		BlockPos blockpos = new BlockPos.MutableBlockPos(livingentity.getX(), livingentity.getBoundingBox().minY - 1.0D, livingentity.getZ());
		BlockState blockState = livingentity.level().getBlockState(blockpos);
		AttributeInstance movementSpeed = livingentity.getAttribute(Attributes.MOVEMENT_SPEED);
		
		boolean soulboost = blockState.is(BlockTags.SOUL_SPEED_BLOCKS) && EnchantmentHelper.getEnchantmentLevel(livingentity.level().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getHolder(Enchantments.SOUL_SPEED).orElseThrow(), livingentity) > 0;
		float speedFactor = (float)(soulboost ? 1.0D : livingentity.level().getBlockState(blockpos).getBlock().getSpeedFactor());
		float moveMultiplier = (float)(animation.getProperty(ActionAnimationProperty.AFFECT_SPEED).orElse(false) ? (movementSpeed.getValue() / movementSpeed.getBaseValue()) : 1.0F);
		
		return new Vec3f(dx * moveMultiplier * speedFactor, dy, dz * moveMultiplier * speedFactor);
	};
	
	/**
	 * WORLD_COORD
	 * - Calculates the coordinate of current elapsed time
	 * - the coordinate is the world position
	 */
	public static final MoveCoordGetter WORLD_COORD = (animation, entitypatch, coord, prevElapsedTime, elapsedTime) -> {
		JointTransform jt = coord.getInterpolatedTransform(elapsedTime);
		Vec3 entityPos = entitypatch.getOriginal().position();
		
		return jt.translation().copy().sub(Vec3f.fromDoubleVector(entityPos));
	};
	
	/**
	 * ATTACHED
	 * Calculates the relative position of a grappling target entity.
	 *  - especially used by {@link GrapplingAttackAnimation}
	 *  - read by {@link MoveCoordFunctions#RAW_COORD}
	 */
	public static final MoveCoordGetter ATTACHED = (animation, entitypatch, coord, prevElapsedTime, elapsedTime) -> {
		LivingEntity target = entitypatch.getGrapplingTarget();
		
		if (target == null) {
			return MODEL_COORD.get(animation, entitypatch, coord, prevElapsedTime, elapsedTime);
		}
		
		TransformSheet rootCoord = animation.getCoord();
		LivingEntity livingentity = entitypatch.getOriginal();
		Vec3f model = rootCoord.getInterpolatedTransform(elapsedTime).translation();
		Vec3f world = OpenMatrix4f.transform3v(OpenMatrix4f.createRotatorDeg(-target.getYRot(), Vec3f.Y_AXIS), model, null);
		Vec3f dst = Vec3f.fromDoubleVector(target.position()).add(world);
		entitypatch.setYRot(Mth.wrapDegrees(target.getYRot() + 180.0F));
		
		return dst.sub(Vec3f.fromDoubleVector(livingentity.position()));
	};
	
	/******************************************************
	 * Action animation properties 
	 ******************************************************/
	
	/**
	 * No destination
	 */
	public static final DestLocationProvider NO_DEST = (DynamicAnimation self, LivingEntityPatch<?> entitypatch) -> {
		return null;
	};
	
	/**
	 * Location of the current attack target
	 */
	public static final DestLocationProvider ATTACK_TARGET_LOCATION = (DynamicAnimation self, LivingEntityPatch<?> entitypatch) -> {
		return entitypatch.getTarget() == null ? null : entitypatch.getTarget().position();
	};
	
	/**
	 * Location set by Animation Variable
	 */
	public static final DestLocationProvider SYNCHED_DEST_VARIABLE = (DynamicAnimation self, LivingEntityPatch<?> entitypatch) -> {
		return entitypatch.getAnimator().getVariables().getOrDefault(EpicFightSynchedAnimationVariableKeys.DESTINATION.get(), self.getRealAnimation());
	};
	
	/**
	 * Location of current attack target that is provided by animation variable
	 */
	public static final DestLocationProvider SYNCHED_TARGET_ENTITY_LOCATION_VARIABLE = (DynamicAnimation self, LivingEntityPatch<?> entitypatch) -> {
		Optional<Integer> targetEntityId = entitypatch.getAnimator().getVariables().get(EpicFightSynchedAnimationVariableKeys.TARGET_ENTITY.get(), self.getRealAnimation());
		
		if (targetEntityId.isPresent()) {
			Entity entity = entitypatch.getOriginal().level().getEntity(targetEntityId.get());
			
			if (entity != null) {
				return entity.position();
			}
		}
		
		return entitypatch.getOriginal().position();
	};
	
	/**
	 * Looking direction from an action beginning location to a destination location
	 */
	public static final YRotProvider LOOK_DEST = (DynamicAnimation self, LivingEntityPatch<?> entitypatch) -> {
		Vec3 destLocation = self.getRealAnimation().get().getProperty(ActionAnimationProperty.DEST_LOCATION_PROVIDER).orElse(NO_DEST).get(self, entitypatch);
		
		if (destLocation != null) {
			Vec3 startInWorld = entitypatch.getAnimator().getVariables().getOrDefault(ActionAnimation.BEGINNING_LOCATION, self.getRealAnimation());
			
			if (startInWorld == null) {
				startInWorld = entitypatch.getOriginal().position();
			}
			
			Vec3 toDestWorld = destLocation.subtract(startInWorld);
			float yRot = (float)Mth.wrapDegrees(MathUtils.getYRotOfVector(toDestWorld));
			float entityYRot = MathUtils.rotlerp(entitypatch.getYRot(), yRot, entitypatch.getYRotLimit());
			
			return entityYRot;
		} else {
			return entitypatch.getYRot();
		}
	};
	
	/**
	 * Rotate an entity toward target for attack animations
	 */
	public static final YRotProvider MOB_ATTACK_TARGET_LOOK = (DynamicAnimation self, LivingEntityPatch<?> entitypatch) -> {
		if (!entitypatch.isLogicalClient() && entitypatch instanceof MobPatch<?> mobpatch) {
			AnimationPlayer player = entitypatch.getAnimator().getPlayerFor(self.getAccessor());
			float elapsedTime = player.getElapsedTime();
			EntityState state = self.getState(entitypatch, elapsedTime);
			
			if (state.getLevel() == 1 && !state.turningLocked()) {
				mobpatch.getOriginal().getNavigation().stop();
				entitypatch.getOriginal().attackAnim = 2;
				LivingEntity target = entitypatch.getTarget();
				
				if (target != null) {
					float currentYRot = Mth.wrapDegrees(entitypatch.getOriginal().getYRot());
					float clampedYRot = entitypatch.getYRotDeltaTo(target);
					
			        return currentYRot + clampedYRot;
				}
			}
		}
		
		return entitypatch.getYRot();
	};
	
	/******************************************************
	 * MoveCoordSetters
	 * Consider that getAnimationPlayer(self) returns null at the beginning.
	 ******************************************************/
	/**
	 * Sets a raw animation coordinate as action animation's coord
	 *  - read by {@link MoveCoordFunctions#MODEL_COORD}
	 */
	public static final MoveCoordSetter RAW_COORD = (self, entitypatch, transformSheet) -> {
		transformSheet.readFrom(self.getCoord().copyAll());
	};
	
	/**
	 * Sets a raw animation coordinate multiplied by entity's pitch as action animation's coord
	 *  - read by {@link MoveCoordFunctions#MODEL_COORD}
	 */
	public static final MoveCoordSetter RAW_COORD_WITH_X_ROT = (self, entitypatch, transformSheet) -> {
		TransformSheet sheet = self.getCoord().copyAll();
		float xRot = entitypatch.getOriginal().getXRot();
		
		for (Keyframe kf : sheet.getKeyframes()) {
			kf.transform().translation().rotate(-xRot, Vec3f.X_AXIS);
		}
		
		transformSheet.readFrom(sheet);
	};
	
	/**
	 * Trace the origin point(0, 0, 0) in blender coord system as the destination
	 *  - specify the {@link ActionAnimationProperty#DEST_LOCATION_PROVIDER} or it will act as {@link MoveCoordFunctions#RAW_COORD}.
	 *  - the first keyframe's location is where the entity is in world
	 *  - you can specify target frame distance by {@link ActionAnimationProperty#COORD_START_KEYFRAME_INDEX}, {@link ActionAnimationProperty#COORD_DEST_KEYFRAME_INDEX}
	 *  - the coord after destination frame will not be scaled or rotated by distance gap between start location and end location in world coord
	 *  - entity's x rotation is not affected by this coord function
	 *  - entity's y rotation is the direction toward a destination, or you can give specific rotation value by {@link ActionAnimation#ENTITY_Y_ROT AnimationProperty}
	 *  - no movements in link animation
	 *  - read by {@link MoveCoordFunctions#WORLD_COORD}
	 */
	public static final MoveCoordSetter TRACE_ORIGIN_AS_DESTINATION = (self, entitypatch, transformSheet) -> {
		if (self.isLinkAnimation()) {
			transformSheet.readFrom(TransformSheet.EMPTY_SHEET_PROVIDER.apply(entitypatch.getOriginal().position()));
			return;
		}
		
		Keyframe[] coordKeyframes = self.getCoord().getKeyframes();
		int startFrame = self.getRealAnimation().get().getProperty(ActionAnimationProperty.COORD_START_KEYFRAME_INDEX).orElse(0);
		int destFrame = self.getRealAnimation().get().getProperty(ActionAnimationProperty.COORD_DEST_KEYFRAME_INDEX).orElse(coordKeyframes.length - 1);
		Vec3 destInWorld = self.getRealAnimation().get().getProperty(ActionAnimationProperty.DEST_LOCATION_PROVIDER).orElse(NO_DEST).get(self, entitypatch);
		
		if (destInWorld == null) {
			Vec3f beginningPosition = coordKeyframes[0].transform().translation().copy().multiply(1.0F, 1.0F, -1.0F);
			beginningPosition.rotate(-entitypatch.getYRot(), Vec3f.Y_AXIS);
			destInWorld = entitypatch.getOriginal().position().add(-beginningPosition.x, -beginningPosition.y, -beginningPosition.z);
		}
		
		Vec3 startInWorld = entitypatch.getAnimator().getVariables().getOrDefault(ActionAnimation.BEGINNING_LOCATION, self.getRealAnimation());
		
		if (startInWorld == null) {
			startInWorld = entitypatch.getOriginal().position();
		}
		
		Vec3 toTargetInWorld = destInWorld.subtract(startInWorld);
		float yRot = (float)Mth.wrapDegrees(MathUtils.getYRotOfVector(toTargetInWorld));
		Optional<YRotProvider> destYRotProvider = self.getRealAnimation().get().getProperty(ActionAnimationProperty.DEST_COORD_YROT_PROVIDER);
		float destYRot = destYRotProvider.isEmpty() ? yRot : destYRotProvider.get().get(self, entitypatch);
		
		TransformSheet result = self.getCoord().transformToWorldCoordOriginAsDest(entitypatch, startInWorld, destInWorld, yRot, destYRot, startFrame, destFrame);
		transformSheet.readFrom(result);
	};
	
	/**
	 * Trace the target entity's position (use it with MODEL_COORD)
	 *  - the location of the last keyfram is basis to limit maximum distance
	 *  - rotation is where the entity is looking
	 */
	public static final MoveCoordSetter TRACE_TARGET_DISTANCE = (self, entitypatch, transformSheet) -> {
		Vec3 destLocation = self.getRealAnimation().get().getProperty(ActionAnimationProperty.DEST_LOCATION_PROVIDER).orElse(NO_DEST).get(self, entitypatch);
		
		if (destLocation != null) {
			TransformSheet transform = self.getCoord().copyAll();
			Keyframe[] coord = transform.getKeyframes();
			Keyframe[] realAnimationCoord = self.getRealAnimation().get().getCoord().getKeyframes();
			Vec3 startInWorld = entitypatch.getAnimator().getVariables().getOrDefault(ActionAnimation.BEGINNING_LOCATION, self.getRealAnimation());
			
			if (startInWorld == null) {
				startInWorld = entitypatch.getOriginal().position();
			}
			
			int startFrame = self.getRealAnimation().get().getProperty(ActionAnimationProperty.COORD_START_KEYFRAME_INDEX).orElse(0);
			int realAnimationEndFrame = self.getRealAnimation().get().getProperty(ActionAnimationProperty.COORD_DEST_KEYFRAME_INDEX).orElse(self.getRealAnimation().get().getCoord().getKeyframes().length - 1);
			Vec3 toDestWorld = destLocation.subtract(startInWorld);
			Vec3f toDestAnim = realAnimationCoord[realAnimationEndFrame].transform().translation();
			LivingEntity attackTarget = entitypatch.getTarget();
			
			// Calculate Entity-Entity collide radius
			float entityRadius = 0.0F;
			
			if (attackTarget != null) {
				float reach = 0.0F;
				
				if (self.getRealAnimation().get() instanceof AttackAnimation attackAnimation) {
					Optional<Float> reachOpt = attackAnimation.getProperty(AttackAnimationProperty.REACH);
					
					if (reachOpt.isPresent()) {
						reach = reachOpt.get();
					} else {
						AnimationPlayer player = entitypatch.getAnimator().getPlayerFor(self.getAccessor());
						
						if (player != null) {
							Phase phase = attackAnimation.getPhaseByTime(player.getElapsedTime());
							reach = entitypatch.getReach(phase.hand);
						}
					}
				}
				
				entityRadius = (attackTarget.getBbWidth() + entitypatch.getOriginal().getBbWidth()) * 0.7F + reach;
			}
			
			float worldLength = Math.max((float)toDestWorld.length() - entityRadius, 0.0F);
			float animLength = toDestAnim.length();
			
			float dot = entitypatch.getAnimator().getVariables().getOrDefault(ActionAnimation.INITIAL_LOOK_VEC_DOT, self.getRealAnimation());
			float lookLength = Mth.lerp(dot, animLength, worldLength);
			float scale = Math.min(lookLength / animLength, 1.0F);
			
			if (self.isLinkAnimation()) {
				scale *= coord[coord.length - 1].transform().translation().length() / animLength;
			}
			
			int endFrame = self.getRealAnimation().get().getProperty(ActionAnimationProperty.COORD_DEST_KEYFRAME_INDEX).orElse(coord.length - 1);
			
			for (int i = startFrame; i <= endFrame; i++) {
				Vec3f translation = coord[i].transform().translation();
				translation.x *= scale;
				
				if (translation.z < 0.0F) {
					translation.z *= scale;
				}
			}
			
			transformSheet.readFrom(transform);
		} else {
			transformSheet.readFrom(self.getCoord().copyAll());
		}
	};
	
	/**
	 * Trace the target entity's position (use it MODEL_COORD)
	 *  - the location of the last keyframe is a basis to limit maximum distance
	 *  - rotation is the direction toward a target entity
	 */
	public static final MoveCoordSetter TRACE_TARGET_LOCATION_ROTATION = (self, entitypatch, transformSheet) -> {
		Vec3 destLocation = self.getRealAnimation().get().getProperty(ActionAnimationProperty.DEST_LOCATION_PROVIDER).orElse(NO_DEST).get(self, entitypatch);
		
		if (destLocation != null) {
			TransformSheet transform = self.getCoord().copyAll();
			Keyframe[] coord = transform.getKeyframes();
			Keyframe[] realAnimationCoord = self.getRealAnimation().get().getCoord().getKeyframes();
			Vec3 startInWorld = entitypatch.getAnimator().getVariables().getOrDefault(ActionAnimation.BEGINNING_LOCATION, self.getRealAnimation());
			
			if (startInWorld == null) {
				startInWorld = entitypatch.getOriginal().position();
			}
			
			int startFrame = self.getRealAnimation().get().getProperty(ActionAnimationProperty.COORD_START_KEYFRAME_INDEX).orElse(0);
			int endFrame = self.isLinkAnimation() ? coord.length - 1 : self.getRealAnimation().get().getProperty(ActionAnimationProperty.COORD_DEST_KEYFRAME_INDEX).orElse(coord.length - 1);
			Vec3 toDestWorld = destLocation.subtract(startInWorld);
			Vec3f toDestAnim = realAnimationCoord[endFrame].transform().translation();
			LivingEntity attackTarget = entitypatch.getTarget();
			
			// Calculate Entity-Entity collide radius
			float entityRadius = 0.0F;
			
			if (attackTarget != null) {
				float reach = 0.0F;
				
				if (self.getRealAnimation().get() instanceof AttackAnimation attackAnimation) {
					Optional<Float> reachOpt = attackAnimation.getProperty(AttackAnimationProperty.REACH);
					
					if (reachOpt.isPresent()) {
						reach = reachOpt.get();
					} else {
						AnimationPlayer player = entitypatch.getAnimator().getPlayerFor(self.getAccessor());
						
						if (player != null) {
							Phase phase = attackAnimation.getPhaseByTime(player.getElapsedTime());
							reach = entitypatch.getReach(phase.hand);
						}
					}
				}
				
				entityRadius = (attackTarget.getBbWidth() + entitypatch.getOriginal().getBbWidth()) * 0.7F + reach;
			}
			
			float worldLength = Math.max((float)toDestWorld.length() - entityRadius, 0.0F);
			float animLength = toDestAnim.length();
			float scale = Math.min(worldLength / animLength, 1.0F);
			
			if (self.isLinkAnimation()) {
				scale *= coord[endFrame].transform().translation().length() / animLength;
			}
			
			for (int i = startFrame; i <= endFrame; i++) {
				Vec3f translation = coord[i].transform().translation();
				translation.x *= scale;
				
				if (translation.z < 0.0F) {
					translation.z *= scale;
				}
			}
			
			transformSheet.readFrom(transform);
		} else {
			transformSheet.readFrom(self.getCoord().copyAll());
		}
	};
	
	public static final MoveCoordSetter VEX_TRACE = (self, entitypatch, transformSheet) -> {
		if (!self.isLinkAnimation()) {
			TransformSheet transform = self.getCoord().copyAll();
			
			if (entitypatch.getTarget() != null) {
				Keyframe[] keyframes = transform.getKeyframes();
				Vec3 pos = entitypatch.getOriginal().position();
				Vec3 targetpos = entitypatch.getTarget().getEyePosition();
				double flyDistance = Math.max(5.0D, targetpos.subtract(pos).length() * 2);
				
				transform.forEach((index, keyframe) -> {
					keyframe.transform().translation().scale((float)(flyDistance / Math.abs(keyframes[keyframes.length - 1].transform().translation().z)));
				});
				
				Vec3 toTarget = targetpos.subtract(pos);
				float xRot = (float)-MathUtils.getXRotOfVector(toTarget);
				float yRot = (float)MathUtils.getYRotOfVector(toTarget);
				
				entitypatch.setYRot(yRot);
				
				transform.forEach((index, keyframe) -> {
					keyframe.transform().translation().rotateDegree(Vec3f.X_AXIS, xRot);
					keyframe.transform().translation().rotateDegree(Vec3f.Y_AXIS, 180.0F - yRot);
					keyframe.transform().translation().add(entitypatch.getOriginal().position());
				});
				
				transformSheet.readFrom(transform);
			} else {
				transform.forEach((index, keyframe) -> {
					keyframe.transform().translation().rotateDegree(Vec3f.Y_AXIS, 180.0F - entitypatch.getYRot());
					keyframe.transform().translation().add(entitypatch.getOriginal().position());
				});
			}
		}
	};
}