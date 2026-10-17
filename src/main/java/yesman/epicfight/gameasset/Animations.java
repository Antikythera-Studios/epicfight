package yesman.epicfight.gameasset;
import yesman.epicfight.EpicFight;
import yesman.epicfight.config.ServerConfig;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.ints.IntIntPair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.DragonFireball;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.CollisionContext;




import org.joml.Quaternionf;
import org.joml.Vector3f;
import yesman.epicfight.api.animation.*;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.AnimationManager.AnimationBuilder;
import yesman.epicfight.api.animation.AnimationManager.AnimationRegistryEvent;
import yesman.epicfight.api.animation.AnimationVariables.IndependentVariableKey;
import yesman.epicfight.api.animation.Pose;
import yesman.epicfight.api.animation.property.AnimationEvent;
import yesman.epicfight.api.animation.property.AnimationEvent.InPeriodEvent;
import yesman.epicfight.api.animation.property.AnimationEvent.InTimeEvent;
import yesman.epicfight.api.animation.property.AnimationEvent.Side;
import yesman.epicfight.api.animation.property.AnimationEvent.SimpleEvent;
import yesman.epicfight.api.animation.property.AnimationParameters;
import yesman.epicfight.api.animation.property.AnimationProperty;
import yesman.epicfight.api.animation.property.AnimationProperty.ActionAnimationProperty;
import yesman.epicfight.api.animation.property.AnimationProperty.AttackAnimationProperty;
import yesman.epicfight.api.animation.property.AnimationProperty.AttackPhaseProperty;
import yesman.epicfight.api.animation.property.AnimationProperty.StaticAnimationProperty;
import yesman.epicfight.api.animation.property.MoveCoordFunctions;
import yesman.epicfight.api.animation.types.*;
import yesman.epicfight.api.animation.types.AttackAnimation.Phase;
import yesman.epicfight.api.animation.types.grappling.GrapplingAttackAnimation;
import yesman.epicfight.api.animation.types.grappling.GrapplingTryAnimation;
import yesman.epicfight.api.animation.types.procedural.*;
import yesman.epicfight.api.extension.BlockStateExtension;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.collider.OBBCollider;
import yesman.epicfight.api.physics.ik.InverseKinematicsSimulator.InverseKinematicsDefinition;
import yesman.epicfight.api.utils.HitEntityList;
import yesman.epicfight.api.utils.HitEntityList.Priority;
import yesman.epicfight.api.utils.LevelUtil;
import yesman.epicfight.api.utils.TimePairList;
import yesman.epicfight.api.utils.math.*;
import yesman.epicfight.main.EpicFightSharedConstants;
import yesman.epicfight.mixin.common.MixinWitherBossAccessor;
import yesman.epicfight.model.armature.types.ToolHolderArmature;
import yesman.epicfight.registry.entries.*;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.identity.MeteorSlamSkill;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.boss.WitherPatch;
import yesman.epicfight.world.capabilities.entitypatch.boss.enderdragon.EnderDragonPatch;
import yesman.epicfight.world.capabilities.entitypatch.boss.enderdragon.PatchedPhases;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.damagesource.*;

import java.util.List;
import java.util.Optional;
import java.util.Set;


public class Animations {
    public static DirectStaticAnimation EMPTY_ANIMATION = new DirectStaticAnimation() {
        @Override
        public void loadAnimation() {
        }

        @Override
        public AnimationClip getAnimationClip() {
            return AnimationClip.EMPTY_CLIP;
        }
    };

    public static AnimationAccessor<StaticAnimation> BIPED_IDLE;
    public static AnimationAccessor<MovementAnimation> BIPED_WALK;
    public static AnimationAccessor<MovementAnimation> BIPED_RUN;
    public static AnimationAccessor<MovementAnimation> BIPED_SNEAK;
    public static AnimationAccessor<MovementAnimation> BIPED_SWIM;
    public static AnimationAccessor<StaticAnimation> BIPED_FLOAT;
    public static AnimationAccessor<StaticAnimation> BIPED_KNEEL;
    public static AnimationAccessor<StaticAnimation> BIPED_FALL;
    public static AnimationAccessor<StaticAnimation> BIPED_FLYING;
    public static AnimationAccessor<StaticAnimation> BIPED_CREATIVE_IDLE;
    public static AnimationAccessor<SelectiveAnimation> BIPED_CREATIVE_FLYING;
    public static AnimationAccessor<StaticAnimation> BIPED_MOUNT;
    public static AnimationAccessor<StaticAnimation> BIPED_SIT;
    public static AnimationAccessor<StaticAnimation> BIPED_JUMP;
    public static AnimationAccessor<LongHitAnimation> BIPED_DEATH;
    public static AnimationAccessor<StaticAnimation> BIPED_EQUIP_ITEM;
    public static AnimationAccessor<ReversedAnimation> BIPED_UNEQUIP_ITEM;
    public static AnimationAccessor<MirroredAnimation> BIPED_EQUIP_ITEM_OFFHAND;
    public static AnimationAccessor<MirroredAnimation> BIPED_UNEQUIP_ITEM_OFFHAND;
    public static AnimationAccessor<SelectiveAnimation> BIPED_DIG;
    public static AnimationAccessor<MovementAnimation> BIPED_RUN_SPEAR;
    public static AnimationAccessor<StaticAnimation> BIPED_HOLD_GREATSWORD;
    public static AnimationAccessor<StaticAnimation> BIPED_HOLD_UCHIGATANA_SHEATHING;
    public static AnimationAccessor<StaticAnimation> BIPED_HOLD_UCHIGATANA;
    public static AnimationAccessor<StaticAnimation> BIPED_HOLD_TACHI;
    public static AnimationAccessor<StaticAnimation> BIPED_HOLD_LONGSWORD;
    public static AnimationAccessor<StaticAnimation> BIPED_HOLD_LIECHTENAUER;
    public static AnimationAccessor<StaticAnimation> BIPED_HOLD_SPEAR;
    public static AnimationAccessor<StaticAnimation> BIPED_HOLD_DUAL_WEAPON;
    public static AnimationAccessor<StaticAnimation> BIPED_HOLD_CROSSBOW;
    public static AnimationAccessor<StaticAnimation> BIPED_HOLD_MAP_TWOHAND;
    public static AnimationAccessor<StaticAnimation> BIPED_HOLD_MAP_OFFHAND;
    public static AnimationAccessor<StaticAnimation> BIPED_HOLD_MAP_MAINHAND;
    public static AnimationAccessor<StaticAnimation> BIPED_HOLD_MAP_TWOHAND_MOVE;
    public static AnimationAccessor<StaticAnimation> BIPED_HOLD_MAP_OFFHAND_MOVE;
    public static AnimationAccessor<StaticAnimation> BIPED_HOLD_MAP_MAINHAND_MOVE;
    public static AnimationAccessor<MovementAnimation> BIPED_WALK_GREATSWORD;
    public static AnimationAccessor<MovementAnimation> BIPED_WALK_SPEAR;
    public static AnimationAccessor<MovementAnimation> BIPED_WALK_UCHIGATANA_SHEATHING;
    public static AnimationAccessor<MovementAnimation> BIPED_WALK_UCHIGATANA;
    public static AnimationAccessor<MovementAnimation> BIPED_WALK_TWOHAND;
    public static AnimationAccessor<MovementAnimation> BIPED_WALK_LONGSWORD;
    public static AnimationAccessor<MovementAnimation> BIPED_WALK_LIECHTENAUER;
    public static AnimationAccessor<MovementAnimation> BIPED_RUN_GREATSWORD;
    public static AnimationAccessor<MovementAnimation> BIPED_RUN_UCHIGATANA;
    public static AnimationAccessor<MovementAnimation> BIPED_RUN_UCHIGATANA_SHEATHING;
    public static AnimationAccessor<MovementAnimation> BIPED_RUN_DUAL;
    public static AnimationAccessor<MovementAnimation> BIPED_RUN_LONGSWORD;
    public static AnimationAccessor<StaticAnimation> BIPED_UCHIGATANA_SCRAP;
    public static AnimationAccessor<StaticAnimation> BIPED_LIECHTENAUER_READY;
    public static AnimationAccessor<MirrorAnimation> BIPED_HIT_SHIELD;
    public static AnimationAccessor<MovementAnimation> BIPED_CLIMBING;
    public static AnimationAccessor<StaticAnimation> BIPED_SLEEPING;
    public static AnimationAccessor<AimAnimation> BIPED_BOW_AIM;
    public static AnimationAccessor<ReboundAnimation> BIPED_BOW_SHOT;
    public static AnimationAccessor<MirrorAnimation> BIPED_DRINK;
    public static AnimationAccessor<MirrorAnimation> BIPED_EAT;
    public static AnimationAccessor<MirrorAnimation> BIPED_SPYGLASS_USE;
    public static AnimationAccessor<AimAnimation> BIPED_CROSSBOW_AIM;
    public static AnimationAccessor<ReboundAnimation> BIPED_CROSSBOW_SHOT;
    public static AnimationAccessor<StaticAnimation> BIPED_CROSSBOW_RELOAD;
    public static AnimationAccessor<AimAnimation> BIPED_JAVELIN_AIM;
    public static AnimationAccessor<ReboundAnimation> BIPED_JAVELIN_THROW;
    public static AnimationAccessor<HitAnimation> BIPED_HIT_SHORT;
    public static AnimationAccessor<LongHitAnimation> BIPED_HIT_LONG;
    public static AnimationAccessor<LongHitAnimation> BIPED_HIT_ON_MOUNT;
    public static AnimationAccessor<LongHitAnimation> BIPED_LANDING;
    public static AnimationAccessor<KnockdownAnimation> BIPED_KNOCKDOWN;
    public static AnimationAccessor<MirrorAnimation> BIPED_BLOCK;
    public static AnimationAccessor<DodgeAnimation> BIPED_ROLL_FORWARD;
    public static AnimationAccessor<DodgeAnimation> BIPED_ROLL_BACKWARD;
    public static AnimationAccessor<DodgeAnimation> BIPED_STEP_FORWARD;
    public static AnimationAccessor<DodgeAnimation> BIPED_STEP_BACKWARD;
    public static AnimationAccessor<DodgeAnimation> BIPED_STEP_LEFT;
    public static AnimationAccessor<DodgeAnimation> BIPED_STEP_RIGHT;
    public static AnimationAccessor<DodgeAnimation> BIPED_KNOCKDOWN_WAKEUP_LEFT;
    public static AnimationAccessor<DodgeAnimation> BIPED_KNOCKDOWN_WAKEUP_RIGHT;
    public static AnimationAccessor<ActionAnimation> BIPED_DEMOLITION_LEAP_CHARGING;
    public static AnimationAccessor<ActionAnimation> BIPED_DEMOLITION_LEAP;
    public static AnimationAccessor<ActionAnimation> BIPED_PHANTOM_ASCENT_FORWARD;
    public static AnimationAccessor<ActionAnimation> BIPED_PHANTOM_ASCENT_BACKWARD;
    public static AnimationAccessor<AttackAnimation> BIPED_MOB_ONEHAND1;
    public static AnimationAccessor<AttackAnimation> BIPED_MOB_ONEHAND2;
    public static AnimationAccessor<AttackAnimation> BIPED_MOB_GREATSWORD;
    public static AnimationAccessor<AttackAnimation> BIPED_MOB_TACHI;
    public static AnimationAccessor<AttackAnimation> BIPED_MOB_SPEAR_ONEHAND;
    public static AnimationAccessor<AttackAnimation> BIPED_MOB_SPEAR_TWOHAND1;
    public static AnimationAccessor<AttackAnimation> BIPED_MOB_SPEAR_TWOHAND2;
    public static AnimationAccessor<AttackAnimation> BIPED_MOB_SPEAR_TWOHAND3;
    public static AnimationAccessor<AttackAnimation> BIPED_MOB_SWORD_DUAL1;
    public static AnimationAccessor<AttackAnimation> BIPED_MOB_SWORD_DUAL2;
    public static AnimationAccessor<AttackAnimation> BIPED_MOB_SWORD_DUAL3;
    public static AnimationAccessor<AttackAnimation> BIPED_MOB_LONGSWORD1;
    public static AnimationAccessor<AttackAnimation> BIPED_MOB_LONGSWORD2;
    public static AnimationAccessor<AttackAnimation> BIPED_MOB_UCHIGATANA1;
    public static AnimationAccessor<AttackAnimation> BIPED_MOB_UCHIGATANA2;
    public static AnimationAccessor<AttackAnimation> BIPED_MOB_UCHIGATANA3;
    public static AnimationAccessor<AttackAnimation> BIPED_MOB_DAGGER_ONEHAND1;
    public static AnimationAccessor<AttackAnimation> BIPED_MOB_DAGGER_ONEHAND2;
    public static AnimationAccessor<AttackAnimation> BIPED_MOB_DAGGER_ONEHAND3;
    public static AnimationAccessor<AttackAnimation> BIPED_MOB_DAGGER_TWOHAND1;
    public static AnimationAccessor<AttackAnimation> BIPED_MOB_DAGGER_TWOHAND2;
    public static AnimationAccessor<EmoteAnimation> BIPED_FRUSTRATED;
    public static AnimationAccessor<EmoteAnimation> BIPED_HOPAK;
    public static AnimationAccessor<EmoteAnimation> BIPED_LAUGH;
    public static AnimationAccessor<EmoteAnimation> BIPED_SALUTE;
    public static AnimationAccessor<EmoteAnimation> BIPED_SLIT_THROAT;
    public static AnimationAccessor<EmoteAnimation> BIPED_WAVE_HAND;
    public static AnimationAccessor<RangedAttackAnimation> BIPED_MOB_THROW;
    public static AnimationAccessor<StaticAnimation> CREEPER_IDLE;
    public static AnimationAccessor<MovementAnimation> CREEPER_WALK;
    public static AnimationAccessor<LongHitAnimation> CREEPER_HIT_LONG;
    public static AnimationAccessor<HitAnimation> CREEPER_HIT_SHORT;
    public static AnimationAccessor<LongHitAnimation> CREEPER_DEATH;
    public static AnimationAccessor<StaticAnimation> DRAGON_IDLE;
    public static AnimationAccessor<EnderDragonWalkAnimation> DRAGON_WALK;
    public static AnimationAccessor<StaticAnimation> DRAGON_FLY;
    public static AnimationAccessor<EnderDragonDeathAnimation> DRAGON_DEATH;
    public static AnimationAccessor<EnderDragonActionAnimation> DRAGON_GROUND_TO_FLY;
    public static AnimationAccessor<EnderDragonDynamicActionAnimation> DRAGON_FLY_TO_GROUND;
    public static AnimationAccessor<EnderDragonAttackAnimation> DRAGON_ATTACK1;
    public static AnimationAccessor<EnderDragonAttackAnimation> DRAGON_ATTACK2;
    public static AnimationAccessor<EnderDragonAttackAnimation> DRAGON_ATTACK3;
    public static AnimationAccessor<EnderDragonAttackAnimation> DRAGON_ATTACK4;
    public static AnimationAccessor<EnderDragonActionAnimation> DRAGON_ATTACK4_RECOVERY;
    public static AnimationAccessor<EnderDragonActionAnimation> DRAGON_FIREBALL;
    public static AnimationAccessor<StaticAnimation> DRAGON_AIRSTRIKE;
    public static AnimationAccessor<EnderDragonActionAnimation> DRAGON_BACKJUMP_PREPARE;
    public static AnimationAccessor<AttackAnimation> DRAGON_BACKJUMP_MOVE;
    public static AnimationAccessor<EnderDragonActionAnimation> DRAGON_BACKJUMP_RECOVERY;
    public static AnimationAccessor<EnderDragonActionAnimation> DRAGON_CRYSTAL_LINK;
    public static AnimationAccessor<EnderDragonActionAnimation> DRAGON_NEUTRALIZED;
    public static AnimationAccessor<EnderDragonActionAnimation> DRAGON_NEUTRALIZED_RECOVERY;
    public static AnimationAccessor<StaticAnimation> ENDERMAN_IDLE;
    public static AnimationAccessor<MovementAnimation> ENDERMAN_WALK;
    public static AnimationAccessor<LongHitAnimation> ENDERMAN_DEATH;
    public static AnimationAccessor<HitAnimation> ENDERMAN_HIT_SHORT;
    public static AnimationAccessor<LongHitAnimation> ENDERMAN_HIT_LONG;
    public static AnimationAccessor<LongHitAnimation> ENDERMAN_NEUTRALIZED;
    public static AnimationAccessor<InvincibleAnimation> ENDERMAN_CONVERT_RAGE;
    public static AnimationAccessor<StaticAnimation> ENDERMAN_RAGE_IDLE;
    public static AnimationAccessor<MovementAnimation> ENDERMAN_RAGE_WALK;
    public static AnimationAccessor<AttackAnimation> ENDERMAN_GRASP;
    public static AnimationAccessor<AttackAnimation> ENDERMAN_TP_KICK1;
    public static AnimationAccessor<AttackAnimation> ENDERMAN_TP_KICK2;
    public static AnimationAccessor<AttackAnimation> ENDERMAN_KNEE;
    public static AnimationAccessor<AttackAnimation> ENDERMAN_KICK1;
    public static AnimationAccessor<AttackAnimation> ENDERMAN_KICK2;
    public static AnimationAccessor<AttackAnimation> ENDERMAN_KICK_COMBO;
    public static AnimationAccessor<ActionAnimation> ENDERMAN_TP_EMERGENCE;
    public static AnimationAccessor<StaticAnimation> SPIDER_IDLE;
    public static AnimationAccessor<MovementAnimation> SPIDER_CRAWL;
    public static AnimationAccessor<LongHitAnimation> SPIDER_DEATH;
    public static AnimationAccessor<HitAnimation> SPIDER_HIT;
    public static AnimationAccessor<LongHitAnimation> SPIDER_NEUTRALIZED;
    public static AnimationAccessor<AttackAnimation> SPIDER_ATTACK;
    public static AnimationAccessor<AttackAnimation> SPIDER_JUMP_ATTACK;
    public static AnimationAccessor<StaticAnimation> GOLEM_IDLE;
    public static AnimationAccessor<MovementAnimation> GOLEM_WALK;
    public static AnimationAccessor<LongHitAnimation> GOLEM_DEATH;
    public static AnimationAccessor<AttackAnimation> GOLEM_ATTACK1;
    public static AnimationAccessor<AttackAnimation> GOLEM_ATTACK2;
    public static AnimationAccessor<AttackAnimation> GOLEM_ATTACK3;
    public static AnimationAccessor<AttackAnimation> GOLEM_ATTACK4;
    public static AnimationAccessor<StaticAnimation> HOGLIN_IDLE;
    public static AnimationAccessor<MovementAnimation> HOGLIN_WALK;
    public static AnimationAccessor<LongHitAnimation> HOGLIN_DEATH;
    public static AnimationAccessor<AttackAnimation> HOGLIN_ATTACK;
    public static AnimationAccessor<StaticAnimation> ILLAGER_IDLE;
    public static AnimationAccessor<MovementAnimation> ILLAGER_WALK;
    public static AnimationAccessor<StaticAnimation> VINDICATOR_IDLE_AGGRESSIVE;
    public static AnimationAccessor<MovementAnimation> VINDICATOR_CHASE;
    public static AnimationAccessor<AttackAnimation> VINDICATOR_SWING_AXE1;
    public static AnimationAccessor<AttackAnimation> VINDICATOR_SWING_AXE2;
    public static AnimationAccessor<AttackAnimation> VINDICATOR_SWING_AXE3;
    public static AnimationAccessor<StaticAnimation> EVOKER_CAST_SPELL;
    public static AnimationAccessor<StaticAnimation> PIGLIN_IDLE;
    public static AnimationAccessor<MovementAnimation> PIGLIN_WALK;
    public static AnimationAccessor<StaticAnimation> PIGLIN_ZOMBIFIED_IDLE;
    public static AnimationAccessor<MovementAnimation> PIGLIN_ZOMBIFIED_WALK;
    public static AnimationAccessor<MovementAnimation> PIGLIN_ZOMBIFIED_CHASE;
    public static AnimationAccessor<StaticAnimation> PIGLIN_CELEBRATE1;
    public static AnimationAccessor<StaticAnimation> PIGLIN_CELEBRATE2;
    public static AnimationAccessor<StaticAnimation> PIGLIN_CELEBRATE3;
    public static AnimationAccessor<StaticAnimation> PIGLIN_ADMIRE;
    public static AnimationAccessor<LongHitAnimation> PIGLIN_DEATH;
    public static AnimationAccessor<StaticAnimation> RAVAGER_IDLE;
    public static AnimationAccessor<MovementAnimation> RAVAGER_WALK;
    public static AnimationAccessor<LongHitAnimation> RAVAGER_DEATH;
    public static AnimationAccessor<ActionAnimation> RAVAGER_STUN;
    public static AnimationAccessor<AttackAnimation> RAVAGER_ATTACK1;
    public static AnimationAccessor<AttackAnimation> RAVAGER_ATTACK2;
    public static AnimationAccessor<AttackAnimation> RAVAGER_ATTACK3;
    public static AnimationAccessor<StaticAnimation> VEX_IDLE;
    public static AnimationAccessor<StaticAnimation> VEX_FLIPPING;
    public static AnimationAccessor<LongHitAnimation> VEX_DEATH;
    public static AnimationAccessor<HitAnimation> VEX_HIT;
    public static AnimationAccessor<AttackAnimation> VEX_CHARGE;
    public static AnimationAccessor<LongHitAnimation> VEX_NEUTRALIZED;
    public static AnimationAccessor<StaticAnimation> WITCH_DRINKING;
    public static AnimationAccessor<StaticAnimation> WITHER_SKELETON_IDLE;
    public static AnimationAccessor<InvincibleAnimation> WITHER_SKELETON_SPECIAL_SPAWN;
    public static AnimationAccessor<MovementAnimation> WITHER_SKELETON_WALK;
    public static AnimationAccessor<MovementAnimation> WITHER_SKELETON_CHASE;
    public static AnimationAccessor<AttackAnimation> WITHER_SKELETON_ATTACK1;
    public static AnimationAccessor<AttackAnimation> WITHER_SKELETON_ATTACK2;
    public static AnimationAccessor<AttackAnimation> WITHER_SKELETON_ATTACK3;
    public static AnimationAccessor<StaticAnimation> WITHER_IDLE;
    public static AnimationAccessor<AttackAnimation> WITHER_CHARGE;
    public static AnimationAccessor<LongHitAnimation> WITHER_DEATH;
    public static AnimationAccessor<LongHitAnimation> WITHER_NEUTRALIZED;
    public static AnimationAccessor<InvincibleAnimation> WITHER_SPELL_ARMOR;
    public static AnimationAccessor<ActionAnimation> WITHER_BLOCKED;
    public static AnimationAccessor<InvincibleAnimation> WITHER_GHOST_STANDBY;
    public static AnimationAccessor<AttackAnimation> WITHER_SWIRL;
    public static AnimationAccessor<ActionAnimation> WITHER_BEAM;
    public static AnimationAccessor<AttackAnimation> WITHER_BACKFLIP;
    public static AnimationAccessor<StaticAnimation> ZOMBIE_IDLE;
    public static AnimationAccessor<MovementAnimation> ZOMBIE_WALK;
    public static AnimationAccessor<MovementAnimation> ZOMBIE_CHASE;
    public static AnimationAccessor<AttackAnimation> ZOMBIE_ATTACK1;
    public static AnimationAccessor<AttackAnimation> ZOMBIE_ATTACK2;
    public static AnimationAccessor<AttackAnimation> ZOMBIE_ATTACK3;
    public static AnimationAccessor<ComboAttackAnimation> AXE_AUTO1;
    public static AnimationAccessor<ComboAttackAnimation> AXE_AUTO2;
    public static AnimationAccessor<DashAttackAnimation> AXE_DASH;
    public static AnimationAccessor<AirSlashAnimation> AXE_AIRSLASH;
    public static AnimationAccessor<ComboAttackAnimation> FIST_AUTO1;
    public static AnimationAccessor<ComboAttackAnimation> FIST_AUTO2;
    public static AnimationAccessor<ComboAttackAnimation> FIST_AUTO3;
    public static AnimationAccessor<DashAttackAnimation> FIST_DASH;
    public static AnimationAccessor<AirSlashAnimation> FIST_AIR_SLASH;
    public static AnimationAccessor<ComboAttackAnimation> SPEAR_ONEHAND_AUTO;
    public static AnimationAccessor<AirSlashAnimation> SPEAR_ONEHAND_AIR_SLASH;
    public static AnimationAccessor<ComboAttackAnimation> SPEAR_TWOHAND_AUTO1;
    public static AnimationAccessor<ComboAttackAnimation> SPEAR_TWOHAND_AUTO2;
    public static AnimationAccessor<AirSlashAnimation> SPEAR_TWOHAND_AIR_SLASH;
    public static AnimationAccessor<DashAttackAnimation> SPEAR_DASH;
    public static AnimationAccessor<MountAttackAnimation> SPEAR_MOUNT_ATTACK;
    public static AnimationAccessor<StaticAnimation> SPEAR_GUARD;
    public static AnimationAccessor<GuardAnimation> SPEAR_GUARD_HIT;
    public static AnimationAccessor<ComboAttackAnimation> SWORD_AUTO1;
    public static AnimationAccessor<ComboAttackAnimation> SWORD_AUTO2;
    public static AnimationAccessor<ComboAttackAnimation> SWORD_AUTO3;
    public static AnimationAccessor<DashAttackAnimation> SWORD_DASH;
    public static AnimationAccessor<AirSlashAnimation> SWORD_AIR_SLASH;
    public static AnimationAccessor<StaticAnimation> SWORD_GUARD;
    public static AnimationAccessor<GuardAnimation> SWORD_GUARD_HIT;
    public static AnimationAccessor<GuardAnimation> SWORD_GUARD_ACTIVE_HIT1;
    public static AnimationAccessor<GuardAnimation> SWORD_GUARD_ACTIVE_HIT2;
    public static AnimationAccessor<GuardAnimation> SWORD_GUARD_ACTIVE_HIT3;
    public static AnimationAccessor<GuardAnimation> LONGSWORD_GUARD_ACTIVE_HIT1;
    public static AnimationAccessor<GuardAnimation> LONGSWORD_GUARD_ACTIVE_HIT2;
    public static AnimationAccessor<ComboAttackAnimation> SWORD_DUAL_AUTO1;
    public static AnimationAccessor<ComboAttackAnimation> SWORD_DUAL_AUTO2;
    public static AnimationAccessor<ComboAttackAnimation> SWORD_DUAL_AUTO3;
    public static AnimationAccessor<DashAttackAnimation> SWORD_DUAL_DASH;
    public static AnimationAccessor<AirSlashAnimation> SWORD_DUAL_AIR_SLASH;
    public static AnimationAccessor<StaticAnimation> SWORD_DUAL_GUARD;
    public static AnimationAccessor<GuardAnimation> SWORD_DUAL_GUARD_HIT;
    public static AnimationAccessor<LongHitAnimation> BIPED_COMMON_NEUTRALIZED;
    public static AnimationAccessor<LongHitAnimation> GREATSWORD_GUARD_BREAK;
    public static AnimationAccessor<AttackAnimation> METEOR_SLAM;
    public static AnimationAccessor<AttackAnimation> REVELATION_ONEHAND;
    public static AnimationAccessor<AttackAnimation> REVELATION_TWOHAND;
    public static AnimationAccessor<ComboAttackAnimation> LONGSWORD_AUTO1;
    public static AnimationAccessor<ComboAttackAnimation> LONGSWORD_AUTO2;
    public static AnimationAccessor<ComboAttackAnimation> LONGSWORD_AUTO3;
    public static AnimationAccessor<DashAttackAnimation> LONGSWORD_DASH;
    public static AnimationAccessor<ComboAttackAnimation> LONGSWORD_LIECHTENAUER_AUTO1;
    public static AnimationAccessor<ComboAttackAnimation> LONGSWORD_LIECHTENAUER_AUTO2;
    public static AnimationAccessor<ComboAttackAnimation> LONGSWORD_LIECHTENAUER_AUTO3;
    public static AnimationAccessor<AirSlashAnimation> LONGSWORD_AIR_SLASH;
    public static AnimationAccessor<StaticAnimation> LONGSWORD_GUARD;
    public static AnimationAccessor<GuardAnimation> LONGSWORD_GUARD_HIT;
    public static AnimationAccessor<ComboAttackAnimation> TACHI_AUTO1;
    public static AnimationAccessor<ComboAttackAnimation> TACHI_AUTO2;
    public static AnimationAccessor<ComboAttackAnimation> TACHI_AUTO3;
    public static AnimationAccessor<DashAttackAnimation> TACHI_DASH;
    public static AnimationAccessor<ComboAttackAnimation> TOOL_AUTO1;
    public static AnimationAccessor<ComboAttackAnimation> TOOL_AUTO2;
    public static AnimationAccessor<DashAttackAnimation> TOOL_DASH;
    public static AnimationAccessor<ComboAttackAnimation> UCHIGATANA_AUTO1;
    public static AnimationAccessor<ComboAttackAnimation> UCHIGATANA_AUTO2;
    public static AnimationAccessor<ComboAttackAnimation> UCHIGATANA_AUTO3;
    public static AnimationAccessor<DashAttackAnimation> UCHIGATANA_DASH;
    public static AnimationAccessor<AirSlashAnimation> UCHIGATANA_AIR_SLASH;
    public static AnimationAccessor<ComboAttackAnimation> UCHIGATANA_SHEATHING_AUTO;
    public static AnimationAccessor<DashAttackAnimation> UCHIGATANA_SHEATHING_DASH;
    public static AnimationAccessor<AirSlashAnimation> UCHIGATANA_SHEATH_AIR_SLASH;
    public static AnimationAccessor<StaticAnimation> UCHIGATANA_GUARD;
    public static AnimationAccessor<GuardAnimation> UCHIGATANA_GUARD_HIT;
    public static AnimationAccessor<MountAttackAnimation> SWORD_MOUNT_ATTACK;
    public static AnimationAccessor<ComboAttackAnimation> GREATSWORD_AUTO1;
    public static AnimationAccessor<ComboAttackAnimation> GREATSWORD_AUTO2;
    public static AnimationAccessor<DashAttackAnimation> GREATSWORD_DASH;
    public static AnimationAccessor<AirSlashAnimation> GREATSWORD_AIR_SLASH;
    public static AnimationAccessor<StaticAnimation> GREATSWORD_GUARD;
    public static AnimationAccessor<GuardAnimation> GREATSWORD_GUARD_HIT;
    public static AnimationAccessor<ComboAttackAnimation> DAGGER_AUTO1;
    public static AnimationAccessor<ComboAttackAnimation> DAGGER_AUTO2;
    public static AnimationAccessor<ComboAttackAnimation> DAGGER_AUTO3;
    public static AnimationAccessor<DashAttackAnimation> DAGGER_DASH;
    public static AnimationAccessor<AirSlashAnimation> DAGGER_AIR_SLASH;
    public static AnimationAccessor<ComboAttackAnimation> DAGGER_DUAL_AUTO1;
    public static AnimationAccessor<ComboAttackAnimation> DAGGER_DUAL_AUTO2;
    public static AnimationAccessor<ComboAttackAnimation> DAGGER_DUAL_AUTO3;
    public static AnimationAccessor<ComboAttackAnimation> DAGGER_DUAL_AUTO4;
    public static AnimationAccessor<DashAttackAnimation> DAGGER_DUAL_DASH;
    public static AnimationAccessor<AirSlashAnimation> DAGGER_DUAL_AIR_SLASH;
    public static AnimationAccessor<ComboAttackAnimation> TRIDENT_AUTO1;
    public static AnimationAccessor<ComboAttackAnimation> TRIDENT_AUTO2;
    public static AnimationAccessor<ComboAttackAnimation> TRIDENT_AUTO3;
    public static AnimationAccessor<AttackAnimation> THE_GUILLOTINE;
    public static AnimationAccessor<AttackAnimation> SWEEPING_EDGE;
    public static AnimationAccessor<AttackAnimation> DANCING_EDGE;
    public static AnimationAccessor<AttackAnimation> HEARTPIERCER;
    public static AnimationAccessor<AttackAnimation> GRASPING_SPIRAL_FIRST;
    public static AnimationAccessor<AttackAnimation> GRASPING_SPIRAL_SECOND;
    public static AnimationAccessor<StaticAnimation> STEEL_WHIRLWIND_CHARGING;
    public static AnimationAccessor<AttackAnimation> STEEL_WHIRLWIND;
    public static AnimationAccessor<AttackAnimation> BATTOJUTSU;
    public static AnimationAccessor<AttackAnimation> BATTOJUTSU_DASH;
    public static AnimationAccessor<AttackAnimation> RUSHING_TEMPO1;
    public static AnimationAccessor<AttackAnimation> RUSHING_TEMPO2;
    public static AnimationAccessor<AttackAnimation> RUSHING_TEMPO3;
    public static AnimationAccessor<AttackAnimation> RELENTLESS_COMBO;
    public static AnimationAccessor<AttackAnimation> EVISCERATE_FIRST;
    public static AnimationAccessor<AttackAnimation> EVISCERATE_SECOND;
    public static AnimationAccessor<AttackAnimation> BLADE_RUSH_COMBO1;
    public static AnimationAccessor<AttackAnimation> BLADE_RUSH_COMBO2;
    public static AnimationAccessor<AttackAnimation> BLADE_RUSH_COMBO3;
    public static AnimationAccessor<LongHitAnimation> BLADE_RUSH_HIT;
    public static AnimationAccessor<GrapplingAttackAnimation> BLADE_RUSH_EXECUTE_BIPED;
    public static AnimationAccessor<GrapplingTryAnimation> BLADE_RUSH_TRY;
    public static AnimationAccessor<ActionAnimation> BLADE_RUSH_FAILED;
    public static AnimationAccessor<AttackAnimation> WRATHFUL_LIGHTING;
    public static AnimationAccessor<AttackAnimation> TSUNAMI;
    public static AnimationAccessor<AttackAnimation> TSUNAMI_REINFORCED;
    public static AnimationAccessor<ActionAnimation> EVERLASTING_ALLEGIANCE_CALL;
    public static AnimationAccessor<ActionAnimation> EVERLASTING_ALLEGIANCE_CATCH;
    public static AnimationAccessor<AttackAnimation> SHARP_STAB;

    public static AnimationAccessor<OffAnimation> OFF_ANIMATION_HIGHEST;
    public static AnimationAccessor<OffAnimation> OFF_ANIMATION_MIDDLE;
    public static AnimationAccessor<OffAnimation> OFF_ANIMATION_LOWEST;

    
    public static void registerAnimations(AnimationRegistryEvent event) {
        event.newBuilder(EpicFight.MODID, Animations::build);
    }

    public static void build(AnimationBuilder builder) {
        BIPED_IDLE = builder.nextAccessor("biped/living/idle", (accessor) -> new StaticAnimation(true, accessor, Armatures.BIPED));
        BIPED_WALK = builder.nextAccessor("biped/living/walk", (accessor) -> new MovementAnimation(true, accessor, Armatures.BIPED));
        BIPED_FLYING = builder.nextAccessor("biped/living/fly", (accessor) -> new StaticAnimation(true, accessor, Armatures.BIPED));
        BIPED_CREATIVE_IDLE = builder.nextAccessor("biped/living/creative_idle", (accessor) -> new StaticAnimation(true, accessor, Armatures.BIPED));
        BIPED_CREATIVE_FLYING = builder.nextAccessor("biped/living/creative_fly", (accessor) ->
            new SelectiveAnimation((entitypatch) -> {
                    Vec3 view = entitypatch.getOriginal().getViewVector(1.0F);
                    Vec3 move = entitypatch.getOriginal().getDeltaMovement();

                    double dot = view.dot(move);

                    return dot < 0.0D ? 1 : 0;
                },
                accessor,
                new DirectStaticAnimation(EpicFightSharedConstants.GENERAL_ANIMATION_TRANSITION_TIME, true, EpicFight.identifier("biped/living/creative_fly_forward"), Armatures.BIPED)
                    .addProperty(StaticAnimationProperty.POSE_MODIFIER, Animations.ReusableSources.FLYING_CORRECTION),
                new DirectStaticAnimation(EpicFightSharedConstants.GENERAL_ANIMATION_TRANSITION_TIME, true, EpicFight.identifier("biped/living/creative_fly_backward"), Armatures.BIPED)
                    .addProperty(StaticAnimationProperty.POSE_MODIFIER, Animations.ReusableSources.FLYING_CORRECTION2)
            )
        );

        BIPED_HOLD_CROSSBOW = builder.nextAccessor("biped/living/hold_crossbow", (accessor) -> new StaticAnimation(true, accessor, Armatures.BIPED));
        BIPED_HOLD_MAP_TWOHAND = builder.nextAccessor("biped/living/hold_map_twohand", (accessor) ->
            new StaticAnimation(true, accessor, Armatures.BIPED)
                .addProperty(StaticAnimationProperty.POSE_MODIFIER, Animations.ReusableSources.MAP_ARMS_CORRECTION)
        );
        BIPED_HOLD_MAP_OFFHAND = builder.nextAccessor("biped/living/hold_map_offhand", (accessor) ->
            new StaticAnimation(true, accessor, Armatures.BIPED)
                .addProperty(StaticAnimationProperty.POSE_MODIFIER, Animations.ReusableSources.MAP_ARMS_CORRECTION)
        );
        BIPED_HOLD_MAP_MAINHAND = builder.nextAccessor("biped/living/hold_map_mainhand", (accessor) ->
            new StaticAnimation(true, accessor, Armatures.BIPED)
                .addProperty(StaticAnimationProperty.POSE_MODIFIER, Animations.ReusableSources.MAP_ARMS_CORRECTION)
        );
        BIPED_HOLD_MAP_TWOHAND_MOVE = builder.nextAccessor("biped/living/hold_map_twohand_move", (accessor) ->
            new StaticAnimation(true, accessor, Armatures.BIPED)
                .addProperty(StaticAnimationProperty.POSE_MODIFIER, Animations.ReusableSources.MAP_ARMS_CORRECTION)
        );
        BIPED_HOLD_MAP_OFFHAND_MOVE = builder.nextAccessor("biped/living/hold_map_offhand_move", (accessor) ->
            new StaticAnimation(true, accessor, Armatures.BIPED)
                .addProperty(StaticAnimationProperty.POSE_MODIFIER, Animations.ReusableSources.MAP_ARMS_CORRECTION)
        );
        BIPED_HOLD_MAP_MAINHAND_MOVE = builder.nextAccessor("biped/living/hold_map_mainhand_move", (accessor) ->
            new StaticAnimation(true, accessor, Armatures.BIPED)
                .addProperty(StaticAnimationProperty.POSE_MODIFIER, Animations.ReusableSources.MAP_ARMS_CORRECTION)
        );

        BIPED_RUN = builder.nextAccessor("biped/living/run", (accessor) -> new MovementAnimation(true, accessor, Armatures.BIPED));
        BIPED_SNEAK = builder.nextAccessor("biped/living/sneak", (accessor) -> new MovementAnimation(true, accessor, Armatures.BIPED));
        BIPED_SWIM = builder.nextAccessor("biped/living/swim", (accessor) -> new MovementAnimation(true, accessor, Armatures.BIPED));
        BIPED_FLOAT = builder.nextAccessor("biped/living/float", (accessor) -> new StaticAnimation(true, accessor, Armatures.BIPED));
        BIPED_KNEEL = builder.nextAccessor("biped/living/kneel", (accessor) -> new StaticAnimation(true, accessor, Armatures.BIPED));
        BIPED_FALL = builder.nextAccessor("biped/living/fall", (accessor) -> new StaticAnimation(true, accessor, Armatures.BIPED));
        BIPED_MOUNT = builder.nextAccessor("biped/living/mount", (accessor) ->
            new StaticAnimation(true, accessor, Armatures.BIPED)
                .addProperty(StaticAnimationProperty.FIXED_HEAD_ROTATION, true)
                .addProperty(StaticAnimationProperty.ON_ITEM_CHANGE_EVENT, SimpleEvent.create(Animations.ReusableSources.SET_TOOLS_BACK_WHEN_MOUNT_AND_ITEM_CHANGED, Side.CLIENT))
                .addEvents(StaticAnimationProperty.ON_BEGIN_EVENTS, SimpleEvent.create(Animations.ReusableSources.SET_TOOLS_BACK_WHEN_MOUNT, Side.CLIENT))
                .addEvents(StaticAnimationProperty.ON_END_EVENTS, SimpleEvent.create(Animations.ReusableSources.REVERT_TO_HANDS, Side.CLIENT))
        );

        BIPED_SIT = builder.nextAccessor("biped/living/sit", (accessor) ->
            new StaticAnimation(true, accessor, Armatures.BIPED)
                .addProperty(StaticAnimationProperty.FIXED_HEAD_ROTATION, true)
        );

        BIPED_DIG = builder.nextAccessor("biped/living/dig", (accessor) ->
            new SelectiveAnimation(
                (entitypatch) -> entitypatch.getOriginal().swingingArm == InteractionHand.OFF_HAND ? 1 : 0,
                accessor,
                new DirectStaticAnimation(0.1F, true, EpicFight.identifier("biped/living/dig_mainhand"), Armatures.BIPED),
                new DirectStaticAnimation(0.1F, true, EpicFight.identifier("biped/living/dig_offhand"), Armatures.BIPED)
            )
        );

        // Equip flourish: short upper-body clip played when an item arrives in a held slot.
        // The mainhand variants sit on COMPOSITE_LAYER/HIGH with a "right_arms" mask; the offhand
        // variants sit on COMPOSITE_LAYER/HIGHEST with a "left_arms" mask and apply pose mirroring
        // at runtime via MirroredAnimation. Putting them above MIDDLE means hold_uchigatana etc.
        // (which live on MIDDLE/LOWEST) keep ticking on the layers underneath, so when the
        // flourish finishes those clips just resume rather than needing a re-trigger.
        BIPED_EQUIP_ITEM = builder.nextAccessor("biped/living/equip_item", (accessor) ->
            new StaticAnimation(0.1F, false, accessor, Armatures.BIPED)
        );

        BIPED_UNEQUIP_ITEM = builder.nextAccessor("biped/living/unequip_item", (accessor) ->
            new ReversedAnimation(0.1F, false, accessor, BIPED_EQUIP_ITEM, Armatures.BIPED)
        );

        BIPED_EQUIP_ITEM_OFFHAND = builder.nextAccessor("biped/living/equip_item_offhand", (accessor) ->
            new MirroredAnimation(0.1F, false, accessor, BIPED_EQUIP_ITEM, Armatures.BIPED)
        );

        BIPED_UNEQUIP_ITEM_OFFHAND = builder.nextAccessor("biped/living/unequip_item_offhand", (accessor) ->
            new MirroredAnimation(0.1F, false, accessor, BIPED_UNEQUIP_ITEM, Armatures.BIPED)
        );

        BIPED_BOW_AIM = builder.nextAccessor("biped/combat/bow_aim", (accessor) -> new AimAnimation(true, accessor, "biped/combat/bow_aim_mid", "biped/combat/bow_aim_up", "biped/combat/bow_aim_down", "biped/combat/bow_aim_lying", Armatures.BIPED));
        BIPED_BOW_SHOT = builder.nextAccessor("biped/combat/bow_shot", (accessor) -> new ReboundAnimation(0.05F, accessor, "biped/combat/bow_shot_mid", "biped/combat/bow_shot_up", "biped/combat/bow_shot_down", "biped/combat/bow_shot_lying", Armatures.BIPED));
        BIPED_DRINK = builder.nextAccessor("biped/living/drink", (accessor) ->
            new MirrorAnimation(0.35F, true, accessor, "biped/living/drink_mainhand", "biped/living/drink_offhand", Armatures.BIPED)
                .addProperty(StaticAnimationProperty.FIXED_HEAD_ROTATION, true)
        );
        BIPED_EAT = builder.nextAccessor("biped/living/eat", (accessor) ->
            new MirrorAnimation(0.35F, true, accessor, "biped/living/eat_mainhand", "biped/living/eat_offhand", Armatures.BIPED)
                .addProperty(StaticAnimationProperty.FIXED_HEAD_ROTATION, true)
        );

        BIPED_SPYGLASS_USE = builder.nextAccessor("biped/living/spyglass", (accessor) ->
            new MirrorAnimation(0.15F, true, accessor, "biped/living/spyglass_mainhand", "biped/living/spyglass_offhand", Armatures.BIPED)
                .addProperty(StaticAnimationProperty.PLAY_SPEED_MODIFIER, (self, entitypatch, speed, prevElapsedTime, elapsedTime) -> {
                    if (self.isLinkAnimation()) {
                        return speed;
                    }

                    return 0.0F;
                })
                .addProperty(StaticAnimationProperty.POSE_MODIFIER, (self, pose, entitypatch, elapsedTime, partialTicks) -> {
                    if (entitypatch.isFirstPerson()) {
                        pose.disableAllJoints();
                    } else if (!(self.isLinkAnimation())) {
                        LivingMotion livingMotion = entitypatch.getCurrentLivingMotion();
                        Pose rawPose;

                        if (livingMotion == LivingMotions.SWIM || livingMotion == LivingMotions.FLY || livingMotion == LivingMotions.CREATIVE_FLY) {
                            rawPose = self.getRawPose(3.3333F);
                        } else {
                            float xRot = Mth.clamp((entitypatch.getOriginal().getXRot() + 90.0F) * 0.0166666666666667F, 0.0F, 3.0F);
                            rawPose = self.getRawPose(xRot);
                            float f = 90.0F;
                            float ratio = (f - Math.abs(entitypatch.getOriginal().getXRot())) / f;
                            float yawOffset = entitypatch.getOriginal().getVehicle() != null ? entitypatch.getOriginal().getYHeadRot() : entitypatch.getOriginal().yBodyRot;
                            rawPose.get("Chest").frontResult(
                                  JointTransform.rotation(QuaternionUtils.YP.rotationDegrees(Mth.wrapDegrees(entitypatch.getOriginal().getYHeadRot() - yawOffset) * ratio))
                                , OpenMatrix4f::mulAsOriginInverse
                            );
                        }

                        pose.load(rawPose, Pose.LoadOperation.OVERWRITE);
                    }
                })
                .addProperty(StaticAnimationProperty.FIXED_HEAD_ROTATION, true)
        );

        BIPED_CROSSBOW_AIM = builder.nextAccessor("biped/combat/crossbow_aim", (accessor) -> new AimAnimation(true, accessor, "biped/combat/crossbow_aim_mid", "biped/combat/crossbow_aim_up", "biped/combat/crossbow_aim_down", "biped/combat/crossbow_aim_lying", Armatures.BIPED));
        BIPED_CROSSBOW_SHOT = builder.nextAccessor("biped/combat/crossbow_shot", (accessor) -> new ReboundAnimation(accessor, "biped/combat/crossbow_shot_mid", "biped/combat/crossbow_shot_up", "biped/combat/crossbow_shot_down", "biped/combat/crossbow_shot_lying", Armatures.BIPED));
        BIPED_CROSSBOW_RELOAD = builder.nextAccessor("biped/combat/crossbow_reload", (accessor) -> new StaticAnimation(false, accessor, Armatures.BIPED));
        BIPED_JUMP = builder.nextAccessor("biped/living/jump", (accessor) -> new StaticAnimation(0.083F, false, accessor, Armatures.BIPED));
        BIPED_RUN_SPEAR = builder.nextAccessor("biped/living/run_spear", (accessor) -> new MovementAnimation(true, accessor, Armatures.BIPED));
        BIPED_BLOCK = builder.nextAccessor("biped/living/shield", (accessor) -> new MirrorAnimation(0.25F, true, accessor, "biped/living/shield_mainhand", "biped/living/shield_offhand", Armatures.BIPED));
        BIPED_HOLD_GREATSWORD = builder.nextAccessor("biped/living/hold_greatsword", (accessor) -> new StaticAnimation(true, accessor, Armatures.BIPED));
        BIPED_HOLD_UCHIGATANA_SHEATHING = builder.nextAccessor("biped/living/hold_uchigatana_sheath", (accessor) -> new StaticAnimation(true, accessor, Armatures.BIPED));
        BIPED_HOLD_UCHIGATANA = builder.nextAccessor("biped/living/hold_uchigatana", (accessor) -> new StaticAnimation(true, accessor, Armatures.BIPED));
        BIPED_HOLD_TACHI = builder.nextAccessor("biped/living/hold_tachi", (accessor) -> new StaticAnimation(true, accessor, Armatures.BIPED));
        BIPED_HOLD_LONGSWORD = builder.nextAccessor("biped/living/hold_longsword", (accessor) -> new StaticAnimation(true, accessor, Armatures.BIPED));
        BIPED_HOLD_SPEAR = builder.nextAccessor("biped/living/hold_spear", (accessor) -> new StaticAnimation(true, accessor, Armatures.BIPED));
        BIPED_HOLD_DUAL_WEAPON = builder.nextAccessor("biped/living/hold_dual", (accessor) -> new StaticAnimation(true, accessor, Armatures.BIPED));
        BIPED_HOLD_LIECHTENAUER = builder.nextAccessor("biped/living/hold_liechtenauer", (accessor) -> new StaticAnimation(true, accessor, Armatures.BIPED));

        BIPED_WALK_GREATSWORD = builder.nextAccessor("biped/living/walk_greatsword", (accessor) -> new MovementAnimation(true, accessor, Armatures.BIPED));
        BIPED_WALK_SPEAR = builder.nextAccessor("biped/living/walk_spear", (accessor) -> new MovementAnimation(true, accessor, Armatures.BIPED));
        BIPED_WALK_UCHIGATANA_SHEATHING = builder.nextAccessor("biped/living/walk_uchigatana_sheath", (accessor) -> new MovementAnimation(true, accessor, Armatures.BIPED));
        BIPED_WALK_UCHIGATANA = builder.nextAccessor("biped/living/walk_uchigatana", (accessor) -> new MovementAnimation(true, accessor, Armatures.BIPED));
        BIPED_WALK_TWOHAND = builder.nextAccessor("biped/living/walk_twohand", (accessor) -> new MovementAnimation(true, accessor, Armatures.BIPED));
        BIPED_WALK_LONGSWORD = builder.nextAccessor("biped/living/walk_longsword", (accessor) -> new MovementAnimation(true, accessor, Armatures.BIPED));
        BIPED_WALK_LIECHTENAUER = builder.nextAccessor("biped/living/walk_liechtenauer", (accessor) -> new MovementAnimation(true, accessor, Armatures.BIPED));

        BIPED_RUN_GREATSWORD = builder.nextAccessor("biped/living/run_greatsword", (accessor) -> new MovementAnimation(true, accessor, Armatures.BIPED));
        BIPED_RUN_UCHIGATANA = builder.nextAccessor("biped/living/run_uchigatana", (accessor) -> new MovementAnimation(true, accessor, Armatures.BIPED));
        BIPED_RUN_UCHIGATANA_SHEATHING = builder.nextAccessor("biped/living/run_uchigatana_sheath", (accessor) -> new MovementAnimation(true, accessor, Armatures.BIPED));
        BIPED_RUN_DUAL = builder.nextAccessor("biped/living/run_dual", (accessor) -> new MovementAnimation(true, accessor, Armatures.BIPED));
        BIPED_RUN_LONGSWORD = builder.nextAccessor("biped/living/run_longsword", (accessor) -> new MovementAnimation(true, accessor, Armatures.BIPED));

        BIPED_UCHIGATANA_SCRAP = builder.nextAccessor("biped/living/uchigatana_scrap", (accessor) ->
            new StaticAnimation(0.05F, false, accessor, Armatures.BIPED)
                .addEvents(InTimeEvent.create(0.15F, ReusableSources.PLAY_SOUND, AnimationEvent.Side.CLIENT).params(EpicFightSounds.SWORD_IN.get()))
        );
        BIPED_LIECHTENAUER_READY = builder.nextAccessor("biped/living/liechtenauer_ready", (accessor) -> new StaticAnimation(0.1F, false, accessor, Armatures.BIPED));

        BIPED_HIT_SHIELD = builder.nextAccessor("biped/combat/hit_shield", (accessor) -> new MirrorAnimation(0.05F, false, accessor, "biped/combat/hit_shield_mainhand", "biped/combat/hit_shield_offhand", Armatures.BIPED));

        BIPED_CLIMBING = builder.nextAccessor("biped/living/climb", (accessor) ->
            new MovementAnimation(0.16F, true, accessor, Armatures.BIPED)
                .addProperty(StaticAnimationProperty.PLAY_SPEED_MODIFIER, (self, entitypatch, speed, prevElapsedTime, elapsedTime) -> {
                    if (self.isLinkAnimation()) {
                        return 1.0F;
                    }

                    double deltaY = entitypatch.getOriginal().getY() - entitypatch.getYOld();

                    if (Math.abs(deltaY) < 0.04D) {
                        return 0.0F;
                    } else {
                        return deltaY < 0.0D ? -1.0F : 1.0F;
                    }
                })
                .addProperty(StaticAnimationProperty.FIXED_HEAD_ROTATION, true)
                .addProperty(StaticAnimationProperty.ON_ITEM_CHANGE_EVENT, SimpleEvent.create(Animations.ReusableSources.SET_TOOLS_BACK_WHEN_ITEM_CHANGED, Side.CLIENT))
                .addEvents(StaticAnimationProperty.ON_BEGIN_EVENTS,
                    SimpleEvent.create(Animations.ReusableSources.SET_TOOLS_BACK, Side.CLIENT),
                    SimpleEvent.create(Animations.ReusableSources.UPDATE_Y_TO_NEARBY_LADDER, Side.CLIENT)
                )
                .addEvents(StaticAnimationProperty.TICK_EVENTS, SimpleEvent.create(Animations.ReusableSources.UPDATE_Y_TO_NEARBY_LADDER, Side.CLIENT))
                .addEvents(StaticAnimationProperty.ON_END_EVENTS, SimpleEvent.create(Animations.ReusableSources.REVERT_TO_HANDS, Side.CLIENT))
                .newTimePair(0.0F, 10000.0F)
                    .addStateRemoveOld(EntityState.COMBO_ATTACKS_DOABLE, false)
                    .addStateRemoveOld(EntityState.SKILL_EXECUTABLE, false)
                    .addStateRemoveOld(EntityState.TURNING_LOCKED, true)
                    .addStateRemoveOld(EntityState.INACTION, true)
        );

        BIPED_SLEEPING = builder.nextAccessor("biped/living/sleep", (accessor) -> new StaticAnimation(0.16F, true, accessor, Armatures.BIPED));

        BIPED_JAVELIN_AIM = builder.nextAccessor("biped/combat/javelin_aim", (accessor) ->
            new AimAnimation(false, accessor, "biped/combat/javelin_aim_mid", "biped/combat/javelin_aim_up", "biped/combat/javelin_aim_down", "biped/combat/javelin_aim_lying", Armatures.BIPED)
                .addProperty(StaticAnimationProperty.PLAY_SPEED_MODIFIER,
                    (animation, entitypatch, speed, prevElapsedTime, elapsedTime) -> {
                        if (animation.isLinkAnimation()) {
                            return 1.0F;
                        }

                        if (entitypatch.getOriginal().isUsingItem() && elapsedTime + EpicFightSharedConstants.A_TICK * speed > animation.getTotalTime()) {
                            return 0.0F;
                        }

                        return 1.0F;
                    }
                )
        );

        BIPED_JAVELIN_THROW = builder.nextAccessor("biped/combat/javelin_throw", (accessor) -> new ReboundAnimation(0.08F, accessor, "biped/combat/javelin_throw_mid", "biped/combat/javelin_throw_up", "biped/combat/javelin_throw_down", "biped/combat/javelin_throw_lying", Armatures.BIPED));

        OFF_ANIMATION_HIGHEST = builder.nextAccessor("common/off_highest", (accessor) -> new OffAnimation(accessor));
        OFF_ANIMATION_MIDDLE = builder.nextAccessor("common/off_middle", (accessor) -> new OffAnimation(accessor));
        OFF_ANIMATION_LOWEST = builder.nextAccessor("common/off_lowest", (accessor) -> new OffAnimation(accessor));

        ZOMBIE_IDLE = builder.nextAccessor("zombie/idle", (accessor) -> new StaticAnimation(true, accessor, Armatures.BIPED));
        ZOMBIE_WALK = builder.nextAccessor("zombie/walk", (accessor) -> new MovementAnimation(true, accessor, Armatures.BIPED));
        ZOMBIE_CHASE = builder.nextAccessor("zombie/chase", (accessor) -> new MovementAnimation(true, accessor, Armatures.BIPED));

        CREEPER_IDLE = builder.nextAccessor("creeper/idle", (accessor) -> new StaticAnimation(true, accessor, Armatures.CREEPER));
        CREEPER_WALK = builder.nextAccessor("creeper/walk", (accessor) -> new MovementAnimation(true, accessor, Armatures.CREEPER));

        ENDERMAN_IDLE = builder.nextAccessor("enderman/idle", (accessor) -> new StaticAnimation(true, accessor, Armatures.ENDERMAN));
        ENDERMAN_WALK = builder.nextAccessor("enderman/walk", (accessor) -> new MovementAnimation(true, accessor, Armatures.ENDERMAN));
        ENDERMAN_RAGE_IDLE = builder.nextAccessor("enderman/rage_idle", (accessor) -> new StaticAnimation(true, accessor, Armatures.ENDERMAN));
        ENDERMAN_RAGE_WALK = builder.nextAccessor("enderman/rage_walk", (accessor) -> new MovementAnimation(true, accessor, Armatures.ENDERMAN));

        WITHER_SKELETON_WALK = builder.nextAccessor("wither_skeleton/walk", (accessor) -> new MovementAnimation(true, accessor, Armatures.BIPED));
        WITHER_SKELETON_CHASE = builder.nextAccessor("wither_skeleton/chase", (accessor) -> new MovementAnimation(0.36F, true, accessor, Armatures.BIPED));
        WITHER_SKELETON_IDLE = builder.nextAccessor("wither_skeleton/idle", (accessor) -> new StaticAnimation(true, accessor, Armatures.BIPED));
        WITHER_SKELETON_SPECIAL_SPAWN = builder.nextAccessor("wither_skeleton/special_spawn", (accessor) -> new InvincibleAnimation(0.0F, accessor, Armatures.BIPED));

        SPIDER_IDLE = builder.nextAccessor("spider/idle", (accessor) -> new StaticAnimation(true, accessor, Armatures.SPIDER));
        SPIDER_CRAWL = builder.nextAccessor("spider/crawl", (accessor) -> new MovementAnimation(true, accessor, Armatures.SPIDER));

        GOLEM_IDLE = builder.nextAccessor("iron_golem/idle", (accessor) -> new StaticAnimation(true, accessor, Armatures.IRON_GOLEM));
        GOLEM_WALK = builder.nextAccessor("iron_golem/walk", (accessor) -> new MovementAnimation(true, accessor, Armatures.IRON_GOLEM));

        HOGLIN_IDLE = builder.nextAccessor("hoglin/idle", (accessor) -> new StaticAnimation(true, accessor, Armatures.HOGLIN));
        HOGLIN_WALK = builder.nextAccessor("hoglin/walk", (accessor) -> new MovementAnimation(true, accessor, Armatures.HOGLIN));

        ILLAGER_IDLE = builder.nextAccessor("illager/idle", (accessor) -> new StaticAnimation(true, accessor, Armatures.BIPED));
        ILLAGER_WALK = builder.nextAccessor("illager/walk", (accessor) -> new MovementAnimation(true, accessor, Armatures.BIPED));
        VINDICATOR_IDLE_AGGRESSIVE = builder.nextAccessor("illager/idle_aggressive", (accessor) -> new StaticAnimation(true, accessor, Armatures.BIPED));
        VINDICATOR_CHASE = builder.nextAccessor("illager/chase", (accessor) -> new MovementAnimation(true, accessor, Armatures.BIPED));
        EVOKER_CAST_SPELL = builder.nextAccessor("illager/spellcast", (accessor) -> new StaticAnimation(true, accessor, Armatures.BIPED));

        RAVAGER_IDLE = builder.nextAccessor("ravager/idle", (accessor) -> new StaticAnimation(true, accessor, Armatures.RAVAGER));
        RAVAGER_WALK = builder.nextAccessor("ravager/walk", (accessor) -> new MovementAnimation(true, accessor, Armatures.RAVAGER));

        VEX_IDLE = builder.nextAccessor("vex/idle", (accessor) -> new StaticAnimation(true, accessor, Armatures.VEX));
        VEX_FLIPPING = builder.nextAccessor("vex/flip", (accessor) -> new StaticAnimation(0.05F, true, accessor, Armatures.VEX));

        PIGLIN_IDLE = builder.nextAccessor("piglin/idle", (accessor) -> new StaticAnimation(true, accessor, Armatures.PIGLIN));
        PIGLIN_WALK = builder.nextAccessor("piglin/walk", (accessor) -> new MovementAnimation(true, accessor, Armatures.PIGLIN));
        PIGLIN_ZOMBIFIED_IDLE = builder.nextAccessor("piglin/zombified_idle", (accessor) -> new StaticAnimation(true, accessor, Armatures.PIGLIN));
        PIGLIN_ZOMBIFIED_WALK = builder.nextAccessor("piglin/zombified_walk", (accessor) -> new MovementAnimation(true, accessor, Armatures.PIGLIN));
        PIGLIN_ZOMBIFIED_CHASE = builder.nextAccessor("piglin/zombified_chase", (accessor) -> new MovementAnimation(true, accessor, Armatures.PIGLIN));
        PIGLIN_CELEBRATE1 = builder.nextAccessor("piglin/celebrate1", (accessor) -> new StaticAnimation(true, accessor, Armatures.PIGLIN));
        PIGLIN_CELEBRATE2 = builder.nextAccessor("piglin/celebrate2", (accessor) -> new StaticAnimation(true, accessor, Armatures.PIGLIN));
        PIGLIN_CELEBRATE3 = builder.nextAccessor("piglin/celebrate3", (accessor) -> new StaticAnimation(true, accessor, Armatures.PIGLIN));
        PIGLIN_ADMIRE = builder.nextAccessor("piglin/admire", (accessor) -> new StaticAnimation(true, accessor, Armatures.PIGLIN));

        WITHER_IDLE = builder.nextAccessor("wither/idle", (accessor) -> new StaticAnimation(true, accessor, Armatures.WITHER));

        SPEAR_GUARD = builder.nextAccessor("biped/skill/guard_spear", (accessor) -> new StaticAnimation(true, accessor, Armatures.BIPED));
        SWORD_GUARD = builder.nextAccessor("biped/skill/guard_sword", (accessor) -> new StaticAnimation(true, accessor, Armatures.BIPED));
        SWORD_DUAL_GUARD = builder.nextAccessor("biped/skill/guard_dualsword", (accessor) -> new StaticAnimation(true, accessor, Armatures.BIPED));
        GREATSWORD_GUARD = builder.nextAccessor("biped/skill/guard_greatsword", (accessor) -> new StaticAnimation(0.25F, true, accessor, Armatures.BIPED));
        UCHIGATANA_GUARD = builder.nextAccessor("biped/skill/guard_uchigatana", (accessor) -> new StaticAnimation(0.25F, true, accessor, Armatures.BIPED));
        LONGSWORD_GUARD = builder.nextAccessor("biped/skill/guard_longsword", (accessor) -> new StaticAnimation(0.25F, true, accessor, Armatures.BIPED));

        STEEL_WHIRLWIND_CHARGING = builder.nextAccessor("biped/skill/steel_whirlwind_charging", (accessor) ->
            new StaticAnimation(0.15F, false, accessor, Armatures.BIPED)
                .addProperty(StaticAnimationProperty.PLAY_SPEED_MODIFIER, Animations.ReusableSources.CHARGING));

        /**
         * Main Frame Animations
         **/
        BIPED_ROLL_FORWARD = builder.nextAccessor("biped/skill/roll_forward", (accessor) ->
            new DodgeAnimation(0.1F, accessor, 0.6F, 0.8F, Armatures.BIPED)
                .addEvents(InTimeEvent.create(0.0F, ReusableSources.PLAY_SOUND, AnimationEvent.Side.CLIENT).params(EpicFightSounds.ROLL.get())));

        BIPED_ROLL_BACKWARD = builder.nextAccessor("biped/skill/roll_backward", (accessor) ->
            new DodgeAnimation(0.1F, accessor, 0.6F, 0.8F, Armatures.BIPED)
                .addEvents(InTimeEvent.create(0.0F, ReusableSources.PLAY_SOUND, AnimationEvent.Side.CLIENT).params(EpicFightSounds.ROLL.get())));

        BIPED_STEP_FORWARD = builder.nextAccessor("biped/skill/step_forward", (accessor) ->
            new DodgeAnimation(0.1F, 0.35F, accessor, 0.6F, 1.65F, Armatures.BIPED)
                .addState(EntityState.LOOK_TARGET, true)
                .newTimePair(0.0F, 0.2F)
                    .addStateRemoveOld(EntityState.COMBO_ATTACKS_DOABLE, false)
                    .addStateRemoveOld(EntityState.SKILL_EXECUTABLE, false)
                .addEvents(InTimeEvent.create(0.0F, ReusableSources.PLAY_STEPPING_SOUND, AnimationEvent.Side.CLIENT)));

        BIPED_STEP_BACKWARD = builder.nextAccessor("biped/skill/step_backward", (accessor) ->
            new DodgeAnimation(0.1F, 0.35F, accessor, 0.6F, 1.65F, Armatures.BIPED)
                .addState(EntityState.LOOK_TARGET, true)
                .newTimePair(0.0F, 0.2F)
                    .addStateRemoveOld(EntityState.COMBO_ATTACKS_DOABLE, false)
                    .addStateRemoveOld(EntityState.SKILL_EXECUTABLE, false)
                .addEvents(InTimeEvent.create(0.0F, ReusableSources.PLAY_STEPPING_SOUND, AnimationEvent.Side.CLIENT)));

        BIPED_STEP_LEFT = builder.nextAccessor("biped/skill/step_left", (accessor) ->
            new DodgeAnimation(0.1F, 0.35F, accessor, 0.6F, 1.65F, Armatures.BIPED)
                .addState(EntityState.LOOK_TARGET, true)
                .newTimePair(0.0F, 0.2F)
                    .addStateRemoveOld(EntityState.COMBO_ATTACKS_DOABLE, false)
                    .addStateRemoveOld(EntityState.SKILL_EXECUTABLE, false)
                .addEvents(InTimeEvent.create(0.0F, ReusableSources.PLAY_STEPPING_SOUND, AnimationEvent.Side.CLIENT)));

        BIPED_STEP_RIGHT = builder.nextAccessor("biped/skill/step_right", (accessor) ->
            new DodgeAnimation(0.1F, 0.35F, accessor, 0.6F, 1.65F, Armatures.BIPED)
                .addState(EntityState.LOOK_TARGET, true)
                .newTimePair(0.0F, 0.2F)
                    .addStateRemoveOld(EntityState.COMBO_ATTACKS_DOABLE, false)
                    .addStateRemoveOld(EntityState.SKILL_EXECUTABLE, false)
                .addEvents(InTimeEvent.create(0.0F, ReusableSources.PLAY_STEPPING_SOUND, AnimationEvent.Side.CLIENT)));

        BIPED_KNOCKDOWN_WAKEUP_LEFT = builder.nextAccessor("biped/skill/knockdown_wakeup_left", (accessor) -> new DodgeAnimation(0.1F, accessor, 0.8F, 0.6F, Armatures.BIPED));
        BIPED_KNOCKDOWN_WAKEUP_RIGHT = builder.nextAccessor("biped/skill/knockdown_wakeup_right", (accessor) -> new DodgeAnimation(0.1F, accessor, 0.8F, 0.6F, Armatures.BIPED));

        BIPED_DEMOLITION_LEAP_CHARGING = builder.nextAccessor("biped/skill/demolition_leap_charge", (accessor) ->
            new ActionAnimation(0.15F, accessor, Armatures.BIPED)
                .addProperty(ActionAnimationProperty.STOP_MOVEMENT, true)
                .addProperty(StaticAnimationProperty.PLAY_SPEED_MODIFIER, Animations.ReusableSources.CHARGING)
                .addProperty(StaticAnimationProperty.POSE_MODIFIER, (self, pose, entitypatch, time, partialTicks) -> {
                    if (!self.isStaticAnimation()) {
                        return;
                    }

                    float xRot = Mth.clamp(entitypatch.getOriginal().getXRot(), -60.0F, 50.0F);
                    JointTransform head = pose.orElseEmpty("Head");
                    MathUtils.mulQuaternion(QuaternionUtils.XP.rotationDegrees(xRot), head.rotation(), head.rotation());
                })
                .addProperty(StaticAnimationProperty.RESET_LIVING_MOTION, LivingMotions.IDLE)
                .newTimePair(0.0F, Float.MAX_VALUE)
                    .addStateRemoveOld(EntityState.COMBO_ATTACKS_DOABLE, true)
                    .addStateRemoveOld(EntityState.SKILL_EXECUTABLE, true));

        BIPED_DEMOLITION_LEAP = builder.nextAccessor("biped/skill/demolition_leap", (accessor) ->
            new ActionAnimation(0.05F, 0.4F, accessor, Armatures.BIPED)
                .addProperty(ActionAnimationProperty.SYNC_CAMERA, true)
                .newTimePair(0.0F, 1.0F)
                    .addStateRemoveOld(EntityState.UPDATE_LIVING_MOTION, false));

        BIPED_PHANTOM_ASCENT_FORWARD = builder.nextAccessor("biped/skill/phantom_ascent_forward", (accessor) ->
            new ActionAnimation(0.05F, 0.7F, accessor, Armatures.BIPED)
                    .addStateRemoveOld(EntityState.MOVEMENT_LOCKED, false)
                .newTimePair(0.0F, 0.5F)
                    .addStateRemoveOld(EntityState.INACTION, true)
                .addEvents(StaticAnimationProperty.ON_BEGIN_EVENTS, SimpleEvent.create((entitypatch, animation, params) -> {
                    Vec3 pos = entitypatch.getOriginal().position();

                    entitypatch.playSound(EpicFightSounds.TUMBLE.get(), 0, 0);
                    entitypatch.getLevel().addAlwaysVisibleParticle(EpicFightParticles.AIR_BURST.get(), pos.x, pos.y + entitypatch.getOriginal().getBbHeight() * 0.5D, pos.z, 0, -1, 2);
                }, Side.CLIENT)));
        BIPED_PHANTOM_ASCENT_BACKWARD = builder.nextAccessor("biped/skill/phantom_ascent_backward", (accessor) ->
            new ActionAnimation(0.05F, 0.7F, accessor, Armatures.BIPED)
                    .addStateRemoveOld(EntityState.MOVEMENT_LOCKED, false)
                .newTimePair(0.0F, 0.5F)
                    .addStateRemoveOld(EntityState.INACTION, true)
                .addEvents(StaticAnimationProperty.ON_BEGIN_EVENTS, SimpleEvent.create((entitypatch, animation, params) -> {
                    Vec3 pos = entitypatch.getOriginal().position();

                    entitypatch.playSound(EpicFightSounds.TUMBLE.get(), 0, 0);
                    entitypatch.getLevel().addAlwaysVisibleParticle(EpicFightParticles.AIR_BURST.get(), pos.x, pos.y + entitypatch.getOriginal().getBbHeight() * 0.5D, pos.z, 0, -1, 2);
                }, Side.CLIENT)));

        FIST_AUTO1 = builder.nextAccessor("biped/combat/fist_auto1", (accessor) ->
            new ComboAttackAnimation(0.08F, 0.05F, 0.15F, 0.15F, InteractionHand.OFF_HAND, null, Armatures.BIPED.get().toolL, accessor, Armatures.BIPED)
                .addProperty(AttackPhaseProperty.PARTICLE, EpicFightParticles.HIT_BLUNT)
                .addProperty(AttackAnimationProperty.BASIS_ATTACK_SPEED, 3.2F));
        FIST_AUTO2 = builder.nextAccessor("biped/combat/fist_auto2", (accessor) ->
            new ComboAttackAnimation(0.08F, 0.05F, 0.15F, 0.15F, null, Armatures.BIPED.get().toolR, accessor, Armatures.BIPED)
                .addProperty(AttackPhaseProperty.PARTICLE, EpicFightParticles.HIT_BLUNT)
                .addProperty(AttackAnimationProperty.BASIS_ATTACK_SPEED, 3.2F));
        FIST_AUTO3 = builder.nextAccessor("biped/combat/fist_auto3", (accessor) ->
            new ComboAttackAnimation(0.08F, 0.05F, 0.15F, 0.5F, InteractionHand.OFF_HAND, null, Armatures.BIPED.get().toolL, accessor, Armatures.BIPED)
                .addProperty(AttackPhaseProperty.PARTICLE, EpicFightParticles.HIT_BLUNT)
                .addProperty(AttackAnimationProperty.BASIS_ATTACK_SPEED, 3.2F));
        FIST_DASH = builder.nextAccessor("biped/combat/fist_dash", (accessor) ->
            new DashAttackAnimation(0.06F, 0.05F, 0.15F, 0.3F, 0.7F, null, Armatures.BIPED.get().shoulderR, accessor, Armatures.BIPED)
                .addProperty(AttackPhaseProperty.PARTICLE, EpicFightParticles.HIT_BLUNT)
                .addProperty(StaticAnimationProperty.PLAY_SPEED_MODIFIER, Animations.ReusableSources.CONSTANT_ONE));

        SWORD_AUTO1 = builder.nextAccessor("biped/combat/sword_auto1", (accessor) ->
            new ComboAttackAnimation(0.1F, 0.0F, 0.1F, 0.4F, null, Armatures.BIPED.get().toolR, accessor, Armatures.BIPED)
                .addProperty(AttackAnimationProperty.BASIS_ATTACK_SPEED, 1.6F));
        SWORD_AUTO2 = builder.nextAccessor("biped/combat/sword_auto2", (accessor) ->
            new ComboAttackAnimation(0.1F, 0.05F, 0.15F, 0.4F, null, Armatures.BIPED.get().toolR, accessor, Armatures.BIPED)
                .addProperty(AttackAnimationProperty.BASIS_ATTACK_SPEED, 1.6F));
        SWORD_AUTO3 = builder.nextAccessor("biped/combat/sword_auto3", (accessor) ->
            new ComboAttackAnimation(0.1F, 0.05F, 0.15F, 0.6F, null, Armatures.BIPED.get().toolR, accessor, Armatures.BIPED)
                .addProperty(AttackAnimationProperty.BASIS_ATTACK_SPEED, 1.6F));
        SWORD_DASH = builder.nextAccessor("biped/combat/sword_dash", (accessor) ->
            new DashAttackAnimation(0.1F, 0.1F, 0.15F, 0.25F, 0.65F, null, Armatures.BIPED.get().toolR, accessor, Armatures.BIPED, true)
                .addProperty(AttackAnimationProperty.BASIS_ATTACK_SPEED, 1.6F));

        GREATSWORD_AUTO1 = builder.nextAccessor("biped/combat/greatsword_auto1", (accessor) ->
            new ComboAttackAnimation(0.25F, 0.15F, 0.25F, 0.65F, null, Armatures.BIPED.get().toolR, accessor, Armatures.BIPED)
                .addProperty(AttackAnimationProperty.BASIS_ATTACK_SPEED, 1.0F)
                .newTimePair(0.0F, 0.5F)
                    .addStateRemoveOld(EntityState.SKILL_EXECUTABLE, false)
        );
        GREATSWORD_AUTO2 = builder.nextAccessor("biped/combat/greatsword_auto2", (accessor) ->
            new ComboAttackAnimation(0.1F, 0.5F, 0.65F, 1.5F, null, Armatures.BIPED.get().toolR, accessor, Armatures.BIPED)
                .addProperty(AttackAnimationProperty.BASIS_ATTACK_SPEED, 1.0F)
                .newTimePair(0.0F, 0.5F)
                    .addStateRemoveOld(EntityState.SKILL_EXECUTABLE, false)
        );
        GREATSWORD_DASH = builder.nextAccessor("biped/combat/greatsword_dash", (accessor) ->
            new DashAttackAnimation(0.2F, 0.2F, 0.35F, 0.6F, 1.2F, null, Armatures.BIPED.get().toolR, accessor, Armatures.BIPED, false)
                .addProperty(AttackPhaseProperty.SOURCE_TAG, Set.of(EpicFightDamageTypeTags.FINISHER))
                .addProperty(AttackAnimationProperty.BASIS_ATTACK_SPEED, 1.0F)
                .addProperty(ActionAnimationProperty.MOVE_VERTICAL, false)
                .addEvents(InTimeEvent.create(0.4F, Animations.ReusableSources.FRACTURE_GROUND_SIMPLE, Side.CLIENT).params(new Vec3f(0.0F, -0.24F, -2.0F), Armatures.BIPED.get().toolR, 1.1D, 0.55F))
        );

        SPEAR_ONEHAND_AUTO = builder.nextAccessor("biped/combat/spear_onehand_auto", (accessor) ->
            new ComboAttackAnimation(0.1F, 0.35F, 0.45F, 0.75F, null, Armatures.BIPED.get().toolR, accessor, Armatures.BIPED)
                .addProperty(AttackAnimationProperty.BASIS_ATTACK_SPEED, 1.2F)
        );
        SPEAR_TWOHAND_AUTO1 = builder.nextAccessor("biped/combat/spear_twohand_auto1", (accessor) ->
            new ComboAttackAnimation(0.1F, 0.2F, 0.3F, 0.45F, null, Armatures.BIPED.get().toolR, accessor, Armatures.BIPED)
                .addProperty(AttackAnimationProperty.BASIS_ATTACK_SPEED, 1.2F)
                .newTimePair(0.0F, 0.55F)
                    .addStateRemoveOld(EntityState.SKILL_EXECUTABLE, false)
        );
        SPEAR_TWOHAND_AUTO2 = builder.nextAccessor("biped/combat/spear_twohand_auto2", (accessor) ->
            new ComboAttackAnimation(0.1F, 0.2F, 0.3F, 0.7F, null, Armatures.BIPED.get().toolR, accessor, Armatures.BIPED)
                .addProperty(AttackAnimationProperty.BASIS_ATTACK_SPEED, 1.2F)
        );
        SPEAR_DASH = builder.nextAccessor("biped/combat/spear_dash", (accessor) ->
            new DashAttackAnimation(0.1F, 0.25F, 0.3F, 0.4F, 0.8F, null, Armatures.BIPED.get().toolR, accessor, Armatures.BIPED, true)
                .addProperty(AttackAnimationProperty.BASIS_ATTACK_SPEED, 1.2F)
        );

        TOOL_AUTO1 = builder.nextAccessor("biped/combat/tool_auto1", (accessor) ->
            new ComboAttackAnimation(0.13F, 0.05F, 0.15F, 0.3F, null, Armatures.BIPED.get().toolR, accessor, Armatures.BIPED)
                .setResourceLocation(EpicFight.MODID, "biped/combat/sword_auto1")
        );
        TOOL_AUTO2 = builder.nextAccessor("biped/combat/sword_auto4", (accessor) ->
            new ComboAttackAnimation(0.13F, 0.05F, 0.15F, 0.4F, null, Armatures.BIPED.get().toolR, accessor, Armatures.BIPED)
        );
        TOOL_DASH = builder.nextAccessor("biped/combat/tool_dash", (accessor) ->
            new DashAttackAnimation(0.16F, 0.08F, 0.15F, 0.25F, 0.58F, null, Armatures.BIPED.get().toolR, accessor, Armatures.BIPED, true)
                .addProperty(AttackPhaseProperty.MAX_STRIKES_MODIFIER, ValueModifier.adder(1))
        );

        AXE_DASH = builder.nextAccessor("biped/combat/axe_dash", (accessor) ->
            new DashAttackAnimation(0.25F, 0.08F, 0.4F, 0.46F, 0.9F, null, Armatures.BIPED.get().toolR, accessor, Armatures.BIPED, true));

        SWORD_DUAL_AUTO1 = builder.nextAccessor("biped/combat/sword_dual_auto1", (accessor) ->
            new ComboAttackAnimation(0.08F, 0.1F, 0.2F, 0.3F, null, Armatures.BIPED.get().toolR, accessor, Armatures.BIPED)
                .addProperty(AttackAnimationProperty.BASIS_ATTACK_SPEED, 1.6F)
                .newTimePair(0.0F, 0.2F)
                    .addStateRemoveOld(EntityState.COMBO_ATTACKS_DOABLE, false));
        SWORD_DUAL_AUTO2 = builder.nextAccessor("biped/combat/sword_dual_auto2", (accessor) ->
            new ComboAttackAnimation(0.1F, 0.1F, 0.2F, 0.3F, InteractionHand.OFF_HAND, null, Armatures.BIPED.get().toolL, accessor, Armatures.BIPED)
                .addProperty(AttackAnimationProperty.BASIS_ATTACK_SPEED, 1.6F)
                .newTimePair(0.0F, 0.2F)
                    .addStateRemoveOld(EntityState.COMBO_ATTACKS_DOABLE, false));
        SWORD_DUAL_AUTO3 = builder.nextAccessor("biped/combat/sword_dual_auto3", (accessor) ->
            new ComboAttackAnimation(0.1F, accessor, Armatures.BIPED,
                    new Phase(0.0F, 0.25F, 0.25F, 0.35F, 0.6F, Float.MAX_VALUE, InteractionHand.MAIN_HAND, AttackAnimation.JointColliderPair.of(Armatures.BIPED.get().toolR, null), AttackAnimation.JointColliderPair.of(Armatures.BIPED.get().toolL, null)))
                .addProperty(AttackAnimationProperty.BASIS_ATTACK_SPEED, 1.6F));
        SWORD_DUAL_DASH = builder.nextAccessor("biped/combat/sword_dual_dash", (accessor) ->
            new DashAttackAnimation(0.16F, accessor, Armatures.BIPED,
                    new Phase(0.0F, 0.05F, 0.05F, 0.3F, 0.75F, Float.MAX_VALUE, InteractionHand.MAIN_HAND, AttackAnimation.JointColliderPair.of(Armatures.BIPED.get().toolR, null), AttackAnimation.JointColliderPair.of(Armatures.BIPED.get().toolL, null)))
                .addProperty(AttackAnimationProperty.BASIS_ATTACK_SPEED, 1.6F)
                .addProperty(ActionAnimationProperty.COORD_SET_BEGIN, MoveCoordFunctions.RAW_COORD)
                .addProperty(ActionAnimationProperty.COORD_SET_TICK, null));

        UCHIGATANA_AUTO1 = builder.nextAccessor("biped/combat/uchigatana_auto1", (accessor) ->
            new ComboAttackAnimation(0.05F, 0.15F, 0.25F, 0.3F, null, Armatures.BIPED.get().toolR, accessor, Armatures.BIPED)
                .addProperty(AttackAnimationProperty.BASIS_ATTACK_SPEED, 2.0F));
        UCHIGATANA_AUTO2 = builder.nextAccessor("biped/combat/uchigatana_auto2", (accessor) ->
            new ComboAttackAnimation(0.05F, 0.2F, 0.3F, 0.3F, null, Armatures.BIPED.get().toolR, accessor, Armatures.BIPED)
                .addProperty(AttackAnimationProperty.BASIS_ATTACK_SPEED, 2.0F));
        UCHIGATANA_AUTO3 = builder.nextAccessor("biped/combat/uchigatana_auto3", (accessor) ->
            new ComboAttackAnimation(0.1F, 0.15F, 0.25F, 0.5F, null, Armatures.BIPED.get().toolR, accessor, Armatures.BIPED)
                .addProperty(AttackAnimationProperty.BASIS_ATTACK_SPEED, 2.0F));
        UCHIGATANA_DASH = builder.nextAccessor("biped/combat/uchigatana_dash", (accessor) ->
            new DashAttackAnimation(0.1F, 0.05F, 0.05F, 0.15F, 0.6F, null, Armatures.BIPED.get().toolR, accessor, Armatures.BIPED, true)
                .addProperty(AttackAnimationProperty.BASIS_ATTACK_SPEED, 2.0F));
        UCHIGATANA_SHEATHING_AUTO = builder.nextAccessor("biped/combat/uchigatana_sheath_auto", (accessor) ->
            new ComboAttackAnimation(0.05F, 0.0F, 0.1F, 0.65F, ColliderPreset.BATTOJUTSU, Armatures.BIPED.get().rootJoint, accessor, Armatures.BIPED)
                .addProperty(AttackPhaseProperty.ARMOR_NEGATION_MODIFIER, ValueModifier.adder(30.0F))
                .addProperty(AttackPhaseProperty.DAMAGE_MODIFIER, ValueModifier.multiplier(2.0F))
                .addProperty(AttackPhaseProperty.MAX_STRIKES_MODIFIER, ValueModifier.adder(3))
                .addProperty(AttackPhaseProperty.SWING_SOUND, EpicFightSounds.WHOOSH_SHARP.get()));
        UCHIGATANA_SHEATHING_DASH = builder.nextAccessor("biped/combat/uchigatana_sheath_dash", (accessor) ->
            new DashAttackAnimation(0.05F, 0.05F, 0.2F, 0.35F, 0.65F, ColliderPreset.BATTOJUTSU_DASH, Armatures.BIPED.get().rootJoint, accessor, Armatures.BIPED)
                .addProperty(AttackPhaseProperty.ARMOR_NEGATION_MODIFIER, ValueModifier.adder(30.0F))
                .addProperty(AttackPhaseProperty.DAMAGE_MODIFIER, ValueModifier.multiplier(2.0F))
                .addProperty(AttackPhaseProperty.MAX_STRIKES_MODIFIER, ValueModifier.adder(3))
                .addProperty(AttackPhaseProperty.SWING_SOUND, EpicFightSounds.WHOOSH_SHARP.get()));

        AXE_AUTO1 = builder.nextAccessor("biped/combat/axe_auto1", (accessor) -> new ComboAttackAnimation(0.15F, 0.05F, 0.15F, 0.7F, null, Armatures.BIPED.get().toolR, accessor, Armatures.BIPED));
        AXE_AUTO2 = builder.nextAccessor("biped/combat/axe_auto2", (accessor) -> new ComboAttackAnimation(0.15F, 0.05F, 0.15F, 0.85F, null, Armatures.BIPED.get().toolR, accessor, Armatures.BIPED));

        LONGSWORD_AUTO1 = builder.nextAccessor("biped/combat/longsword_auto1", (accessor) ->
            new ComboAttackAnimation(0.1F, 0.25F, 0.35F, 0.5F, null, Armatures.BIPED.get().toolR, accessor, Armatures.BIPED)
                .addProperty(AttackAnimationProperty.BASIS_ATTACK_SPEED, 1.2F));
        LONGSWORD_AUTO2 = builder.nextAccessor("biped/combat/longsword_auto2", (accessor) ->
            new ComboAttackAnimation(0.15F, 0.2F, 0.3F, 0.45F, null, Armatures.BIPED.get().toolR, accessor, Armatures.BIPED)
                .addProperty(AttackAnimationProperty.BASIS_ATTACK_SPEED, 1.2F));
        LONGSWORD_AUTO3 = builder.nextAccessor("biped/combat/longsword_auto3", (accessor) ->
            new ComboAttackAnimation(0.05F, 0.2F, 0.3F, 1.0F, null, Armatures.BIPED.get().toolR, accessor, Armatures.BIPED)
                .addProperty(AttackAnimationProperty.BASIS_ATTACK_SPEED, 1.2F));
        LONGSWORD_DASH = builder.nextAccessor("biped/combat/longsword_dash", (accessor) ->
            new DashAttackAnimation(0.1F, 0.1F, 0.25F, 0.4F, 0.75F, null, Armatures.BIPED.get().toolR, accessor, Armatures.BIPED, true)
                .addProperty(AttackAnimationProperty.BASIS_ATTACK_SPEED, 1.2F));

        LONGSWORD_LIECHTENAUER_AUTO1 = builder.nextAccessor("biped/combat/longsword_liechtenauer_auto1", (accessor) ->
            new ComboAttackAnimation(0.1F, 0.15F, 0.25F, 0.5F, null, Armatures.BIPED.get().toolR, accessor, Armatures.BIPED)
                .addProperty(AttackAnimationProperty.BASIS_ATTACK_SPEED, 1.2F));
        LONGSWORD_LIECHTENAUER_AUTO2 = builder.nextAccessor("biped/combat/longsword_liechtenauer_auto2", (accessor) ->
            new ComboAttackAnimation(0.1F, 0.2F, 0.3F, 0.5F, null, Armatures.BIPED.get().toolR, accessor, Armatures.BIPED)
                .addProperty(AttackAnimationProperty.BASIS_ATTACK_SPEED, 1.2F));
        LONGSWORD_LIECHTENAUER_AUTO3 = builder.nextAccessor("biped/combat/longsword_liechtenauer_auto3", (accessor) ->
            new ComboAttackAnimation(0.25F, 0.1F, 0.2F, 0.7F, null, Armatures.BIPED.get().toolR, accessor, Armatures.BIPED)
                .addProperty(AttackAnimationProperty.BASIS_ATTACK_SPEED, 1.2F));

        TACHI_AUTO1 = builder.nextAccessor("biped/combat/tachi_auto1", (accessor) ->
            new ComboAttackAnimation(0.1F, 0.35F, 0.4F, 0.55F, null, Armatures.BIPED.get().toolR, accessor, Armatures.BIPED)
                .addProperty(AttackAnimationProperty.BASIS_ATTACK_SPEED, 1.2F)
                .addProperty(AttackAnimationProperty.EXTRA_COLLIDERS, 3));
        TACHI_AUTO2 = builder.nextAccessor("biped/combat/tachi_auto2", (accessor) ->
            new ComboAttackAnimation(0.15F, 0.2F, 0.3F, 0.5F, null, Armatures.BIPED.get().toolR, accessor, Armatures.BIPED)
                .addProperty(AttackAnimationProperty.BASIS_ATTACK_SPEED, 1.2F));
        TACHI_AUTO3 = builder.nextAccessor("biped/combat/tachi_auto3", (accessor) ->
            new ComboAttackAnimation(0.15F, 0.2F, 0.3F, 0.85F, null, Armatures.BIPED.get().toolR, accessor, Armatures.BIPED)
                .addProperty(AttackAnimationProperty.BASIS_ATTACK_SPEED, 1.2F));
        TACHI_DASH = builder.nextAccessor("biped/combat/tachi_dash", (accessor) ->
            new DashAttackAnimation(0.1F, 0.3F, 0.3F, 0.4F, 1.0F, null, Armatures.BIPED.get().toolR, accessor, Armatures.BIPED, true)
                .addProperty(AttackAnimationProperty.BASIS_ATTACK_SPEED, 1.2F));

        DAGGER_AUTO1 = builder.nextAccessor("biped/combat/dagger_auto1", (accessor) ->
            new ComboAttackAnimation(0.05F, 0.05F, 0.15F, 0.25F, null, Armatures.BIPED.get().toolR, accessor, Armatures.BIPED)
                .addProperty(AttackAnimationProperty.BASIS_ATTACK_SPEED, 2.4F));
        DAGGER_AUTO2 = builder.nextAccessor("biped/combat/dagger_auto2", (accessor) ->
            new ComboAttackAnimation(0.05F, 0.0F, 0.1F, 0.25F, null, Armatures.BIPED.get().toolR, accessor, Armatures.BIPED)
                .addProperty(AttackAnimationProperty.BASIS_ATTACK_SPEED, 2.4F));
        DAGGER_AUTO3 = builder.nextAccessor("biped/combat/dagger_auto3", (accessor) ->
            new ComboAttackAnimation(0.05F, 0.2F, 0.25F, 0.4F, null, Armatures.BIPED.get().toolR, accessor, Armatures.BIPED)
                .addProperty(AttackAnimationProperty.BASIS_ATTACK_SPEED, 2.4F));
        DAGGER_DASH = builder.nextAccessor("biped/combat/dagger_dash", (accessor) ->
            new DashAttackAnimation(0.05F, 0.1F, 0.2F, 0.25F, 0.6F, null, Armatures.BIPED.get().toolR, accessor, Armatures.BIPED, true)
                .addProperty(AttackAnimationProperty.BASIS_ATTACK_SPEED, 2.4F)
                .newTimePair(0.0F, 0.4F)
                    .addStateRemoveOld(EntityState.COMBO_ATTACKS_DOABLE, false)
                .newConditionalTimePair((entitypatch) -> (entitypatch.isLastAttackSuccess() ? 1 : 0), 0.4F, 0.6F)
                    .addConditionalState(0, EntityState.COMBO_ATTACKS_DOABLE, false)
                    .addConditionalState(1, EntityState.COMBO_ATTACKS_DOABLE, true));

        DAGGER_DUAL_AUTO1 = builder.nextAccessor("biped/combat/dagger_dual_auto1", (accessor) ->
            new ComboAttackAnimation(0.05F, 0.1F, 0.2F, 0.25F, null, Armatures.BIPED.get().toolR, accessor, Armatures.BIPED)
                .addProperty(AttackAnimationProperty.BASIS_ATTACK_SPEED, 2.4F));
        DAGGER_DUAL_AUTO2 = builder.nextAccessor("biped/combat/dagger_dual_auto2", (accessor) ->
            new ComboAttackAnimation(0.05F, 0.0F, 0.1F, 0.16F, InteractionHand.OFF_HAND, null, Armatures.BIPED.get().toolL, accessor, Armatures.BIPED)
                .addProperty(AttackAnimationProperty.BASIS_ATTACK_SPEED, 2.4F));
        DAGGER_DUAL_AUTO3 = builder.nextAccessor("biped/combat/dagger_dual_auto3", (accessor) ->
            new ComboAttackAnimation(0.05F, 0.0F, 0.1F, 0.2F, null, Armatures.BIPED.get().toolR, accessor, Armatures.BIPED)
                .addProperty(AttackAnimationProperty.BASIS_ATTACK_SPEED, 2.4F));
        DAGGER_DUAL_AUTO4 = builder.nextAccessor("biped/combat/dagger_dual_auto4", (accessor) ->
            new ComboAttackAnimation(0.15F, accessor, Armatures.BIPED,
                      new Phase(0.0F, 0.1F, 0.1F, 0.2F, 0.2F, 0.2F, InteractionHand.OFF_HAND, Armatures.BIPED.get().toolL, null)
                    , new Phase(0.2F, 0.2F, 0.3F, 0.6F, 0.6F, Armatures.BIPED.get().toolR, null))
                .addProperty(AttackAnimationProperty.BASIS_ATTACK_SPEED, 2.4F));
        DAGGER_DUAL_DASH = builder.nextAccessor("biped/combat/dagger_dual_dash", (accessor) ->
            new DashAttackAnimation(0.1F, accessor, Armatures.BIPED,
                      new Phase(0.0F, 0.1F, 0.2F, 0.3F, 0.65F, Float.MAX_VALUE, InteractionHand.MAIN_HAND, AttackAnimation.JointColliderPair.of(Armatures.BIPED.get().toolR, null), AttackAnimation.JointColliderPair.of(Armatures.BIPED.get().toolL, null)))
                .addProperty(AttackAnimationProperty.BASIS_ATTACK_SPEED, 2.4F)
                .addProperty(ActionAnimationProperty.MOVE_VERTICAL, true));

        TRIDENT_AUTO1 = builder.nextAccessor("biped/combat/trident_auto1", (accessor) -> new ComboAttackAnimation(0.3F, 0.05F, 0.16F, 0.45F, null, Armatures.BIPED.get().toolR, accessor, Armatures.BIPED));
        TRIDENT_AUTO2 = builder.nextAccessor("biped/combat/trident_auto2", (accessor) -> new ComboAttackAnimation(0.05F, 0.25F, 0.36F, 0.55F, null, Armatures.BIPED.get().toolR, accessor, Armatures.BIPED));
        TRIDENT_AUTO3 = builder.nextAccessor("biped/combat/trident_auto3", (accessor) -> new ComboAttackAnimation(0.2F, 0.3F, 0.46F, 0.9F, null, Armatures.BIPED.get().toolR, accessor, Armatures.BIPED));

        SWORD_AIR_SLASH = builder.nextAccessor("biped/combat/sword_airslash", (accessor) -> new AirSlashAnimation(0.1F, 0.15F, 0.26F, 0.5F, null, Armatures.BIPED.get().toolR, accessor, Armatures.BIPED));
        SWORD_DUAL_AIR_SLASH = builder.nextAccessor("biped/combat/sword_dual_airslash", (accessor) -> new AirSlashAnimation(0.1F, 0.15F, 0.26F, 0.5F, ColliderPreset.DUAL_SWORD_AIR_SLASH, Armatures.BIPED.get().torso, accessor, Armatures.BIPED));
        UCHIGATANA_AIR_SLASH = builder.nextAccessor("biped/combat/uchigatana_airslash", (accessor) -> new AirSlashAnimation(0.1F, 0.05F, 0.16F, 0.3F, null, Armatures.BIPED.get().toolR, accessor, Armatures.BIPED));
        UCHIGATANA_SHEATH_AIR_SLASH = builder.nextAccessor("biped/combat/uchigatana_sheath_airslash", (accessor) ->
            new AirSlashAnimation(0.1F, 0.1F, 0.16F, 0.3F, null, Armatures.BIPED.get().toolR, accessor, Armatures.BIPED)
                .addProperty(AttackPhaseProperty.ARMOR_NEGATION_MODIFIER, ValueModifier.adder(30.0F))
                .addProperty(AttackPhaseProperty.MAX_STRIKES_MODIFIER, ValueModifier.adder(2))
                .addProperty(AttackPhaseProperty.SWING_SOUND, EpicFightSounds.WHOOSH_SHARP.get())
                .addProperty(AttackAnimationProperty.BASIS_ATTACK_SPEED, 2.0F));
        SPEAR_ONEHAND_AIR_SLASH = builder.nextAccessor("biped/combat/spear_onehand_airslash", (accessor) -> new AirSlashAnimation(0.1F, 0.15F, 0.26F, 0.4F, null, Armatures.BIPED.get().toolR, accessor, Armatures.BIPED));
        SPEAR_TWOHAND_AIR_SLASH = builder.nextAccessor("biped/combat/spear_twohand_airslash", (accessor) -> new AirSlashAnimation(0.1F, 0.25F, 0.36F, 0.6F, null, Armatures.BIPED.get().toolR, accessor, Armatures.BIPED)
                .addProperty(AttackPhaseProperty.SOURCE_TAG, Set.of(EpicFightDamageTypeTags.FINISHER)));
        LONGSWORD_AIR_SLASH = builder.nextAccessor("biped/combat/longsword_airslash", (accessor) -> new AirSlashAnimation(0.1F, 0.3F, 0.41F, 0.5F, null, Armatures.BIPED.get().toolR, accessor, Armatures.BIPED));
        GREATSWORD_AIR_SLASH = builder.nextAccessor("biped/combat/greatsword_airslash", (accessor) -> new AirSlashAnimation(0.1F, 0.5F, 0.55F, 0.71F, 0.75F, false, null, Armatures.BIPED.get().toolR, accessor, Armatures.BIPED)
                .addProperty(AttackPhaseProperty.SOURCE_TAG, Set.of(EpicFightDamageTypeTags.FINISHER)));
        FIST_AIR_SLASH = builder.nextAccessor("biped/combat/fist_airslash", (accessor) -> new AirSlashAnimation(0.1F, 0.15F, 0.26F, 0.4F, null, Armatures.BIPED.get().toolR, accessor, Armatures.BIPED)
                .addProperty(AttackAnimationProperty.BASIS_ATTACK_SPEED, 4.0F));
        DAGGER_AIR_SLASH = builder.nextAccessor("biped/combat/dagger_airslash", (accessor) -> new AirSlashAnimation(0.1F, 0.15F, 0.26F, 0.45F, null, Armatures.BIPED.get().toolR, accessor, Armatures.BIPED)
                .addProperty(AttackAnimationProperty.BASIS_ATTACK_SPEED, 2.4F));
        DAGGER_DUAL_AIR_SLASH = builder.nextAccessor("biped/combat/dagger_dual_airslash", (accessor) -> new AirSlashAnimation(0.1F, 0.15F, 0.26F, 0.4F, ColliderPreset.DUAL_DAGGER_AIR_SLASH, Armatures.BIPED.get().torso, accessor, Armatures.BIPED)
                .addProperty(AttackAnimationProperty.BASIS_ATTACK_SPEED, 2.0F)
                .setResourceLocation(EpicFight.MODID, "biped/combat/sword_dual_airslash"));
        AXE_AIRSLASH = builder.nextAccessor("biped/combat/axe_airslash", (accessor) -> new AirSlashAnimation(0.1F, 0.3F, 0.4F, 0.65F, null, Armatures.BIPED.get().toolR, accessor, Armatures.BIPED));

        SWORD_MOUNT_ATTACK = builder.nextAccessor("biped/combat/sword_mount_attack", (accessor) -> new MountAttackAnimation(0.16F, 0.1F, 0.2F, 0.25F, 0.7F, null, Armatures.BIPED.get().toolR, accessor, Armatures.BIPED));
        SPEAR_MOUNT_ATTACK = builder.nextAccessor("biped/combat/spear_mount_attack", (accessor) ->
            new MountAttackAnimation(0.16F, 0.38F, 0.38F, 0.45F, 0.8F, null, Armatures.BIPED.get().toolR, accessor, Armatures.BIPED)
                .addProperty(StaticAnimationProperty.POSE_MODIFIER, Animations.ReusableSources.COMBO_ATTACK_DIRECTION_MODIFIER));

        BIPED_MOB_ONEHAND1 = builder.nextAccessor("biped/combat/mob_onehand1", (accessor) ->
            new AttackAnimation(0.08F, 0.45F, 0.55F, 0.66F, 0.95F, null, Armatures.BIPED.get().toolR, accessor, Armatures.BIPED)
                .addProperty(StaticAnimationProperty.POSE_MODIFIER, Animations.ReusableSources.COMBO_ATTACK_DIRECTION_MODIFIER));
        BIPED_MOB_ONEHAND2 = builder.nextAccessor("biped/combat/mob_onehand2", (accessor) ->
            new AttackAnimation(0.08F, 0.45F, 0.5F, 0.61F, 0.95F, null, Armatures.BIPED.get().toolR, accessor, Armatures.BIPED)
                .addProperty(StaticAnimationProperty.POSE_MODIFIER, Animations.ReusableSources.COMBO_ATTACK_DIRECTION_MODIFIER));

        BIPED_MOB_GREATSWORD = builder.nextAccessor("biped/combat/mob_greatsword1", (accessor) ->
            new AttackAnimation(0.15F, 0.45F, 0.85F, 0.95F, 2.2F, null, Armatures.BIPED.get().toolR, accessor, Armatures.BIPED)
                .addProperty(AttackPhaseProperty.STUN_TYPE, StunType.KNOCKDOWN)
                .addProperty(StaticAnimationProperty.POSE_MODIFIER, Animations.ReusableSources.COMBO_ATTACK_DIRECTION_MODIFIER));

        BIPED_MOB_TACHI = builder.nextAccessor("biped/combat/mob_tachi_special", (accessor) ->
            new AttackAnimation(0.15F, 0.15F, 0.25F, 0.35F, 1.0F, null, Armatures.BIPED.get().toolR, accessor, Armatures.BIPED)
                .addProperty(StaticAnimationProperty.POSE_MODIFIER, Animations.ReusableSources.COMBO_ATTACK_DIRECTION_MODIFIER));

        BIPED_MOB_SPEAR_ONEHAND = builder.nextAccessor("biped/combat/mob_spear_onehand", (accessor) ->
            new AttackAnimation(0.15F, 0.15F, 0.4F, 0.5F, 1.1F, null, Armatures.BIPED.get().toolR, accessor, Armatures.BIPED)
                .addProperty(StaticAnimationProperty.POSE_MODIFIER, Animations.ReusableSources.COMBO_ATTACK_DIRECTION_MODIFIER));
        BIPED_MOB_SPEAR_TWOHAND1 = builder.nextAccessor("biped/combat/mob_spear_twohand1", (accessor) ->
            new AttackAnimation(0.15F, 0.15F, 0.4F, 0.5F, 1.0F, null, Armatures.BIPED.get().toolR, accessor, Armatures.BIPED)
                .addProperty(StaticAnimationProperty.POSE_MODIFIER, Animations.ReusableSources.COMBO_ATTACK_DIRECTION_MODIFIER));
        BIPED_MOB_SPEAR_TWOHAND2 = builder.nextAccessor("biped/combat/mob_spear_twohand2", (accessor) ->
            new AttackAnimation(0.15F, 0.15F, 0.4F, 0.5F, 1.0F, null, Armatures.BIPED.get().toolR, accessor, Armatures.BIPED)
                .addProperty(StaticAnimationProperty.POSE_MODIFIER, Animations.ReusableSources.COMBO_ATTACK_DIRECTION_MODIFIER));
        BIPED_MOB_SPEAR_TWOHAND3 = builder.nextAccessor("biped/combat/mob_spear_twohand3", (accessor) ->
            new AttackAnimation(0.15F, 0.15F, 0.4F, 0.5F, 1.0F, null, Armatures.BIPED.get().toolR, accessor, Armatures.BIPED)
                .addProperty(StaticAnimationProperty.POSE_MODIFIER, Animations.ReusableSources.COMBO_ATTACK_DIRECTION_MODIFIER));

        BIPED_MOB_SWORD_DUAL1 = builder.nextAccessor("biped/combat/mob_sword_dual1", (accessor) ->
            new AttackAnimation(0.1F, accessor, Armatures.BIPED,
                    new Phase(0.0F, 0.35F, 0.4F, 0.5F, 0.55F, 0.55F, InteractionHand.OFF_HAND, Armatures.BIPED.get().toolL, null), new Phase(0.55F, 0.55F, 0.65F, 0.75F, 1.15F, Float.MAX_VALUE, Armatures.BIPED.get().toolR, null))
                .addProperty(StaticAnimationProperty.POSE_MODIFIER, Animations.ReusableSources.COMBO_ATTACK_DIRECTION_MODIFIER));
        BIPED_MOB_SWORD_DUAL2 = builder.nextAccessor("biped/combat/mob_sword_dual2", (accessor) ->
            new AttackAnimation(0.1F, accessor, Armatures.BIPED,
                    new Phase(0.0F, 0.3F, 0.3F, 0.45F, 0.55F, 0.55F, InteractionHand.OFF_HAND, Armatures.BIPED.get().toolL, null), new Phase(0.55F, 0.55F, 0.65F, 0.75F, 1.15F, Float.MAX_VALUE, Armatures.BIPED.get().toolR, null))
                .addProperty(StaticAnimationProperty.POSE_MODIFIER, Animations.ReusableSources.COMBO_ATTACK_DIRECTION_MODIFIER));
        BIPED_MOB_SWORD_DUAL3 = builder.nextAccessor("biped/combat/mob_sword_dual3", (accessor) ->
            new AttackAnimation(0.1F, 0.25F, 0.85F, 0.95F, 1.4F, null, Armatures.BIPED.get().toolR, accessor, Armatures.BIPED)
                .addProperty(ActionAnimationProperty.MOVE_VERTICAL, true));

        BIPED_MOB_LONGSWORD1 = builder.nextAccessor("biped/combat/mob_longsword1", (accessor) ->
            new AttackAnimation(0.15F, accessor, Armatures.BIPED,
                    new Phase(0.0F, 0.15F, 0.25F, 0.35F, 0.45F, 0.65F, Armatures.BIPED.get().toolR, null), new Phase(0.65F, 0.85F, 1.0F, 1.1F, 1.55F, Float.MAX_VALUE, Armatures.BIPED.get().toolR, null))
                .addProperty(StaticAnimationProperty.POSE_MODIFIER, Animations.ReusableSources.COMBO_ATTACK_DIRECTION_MODIFIER));
        BIPED_MOB_LONGSWORD2 = builder.nextAccessor("biped/combat/mob_longsword2", (accessor) ->
            new AttackAnimation(0.25F, 0.3F, 0.45F, 0.55F, 1.0F, null, Armatures.BIPED.get().toolR, accessor, Armatures.BIPED)
                .addProperty(StaticAnimationProperty.POSE_MODIFIER, Animations.ReusableSources.COMBO_ATTACK_DIRECTION_MODIFIER));

        BIPED_MOB_UCHIGATANA1 = builder.nextAccessor("biped/combat/mob_uchigatana1", (accessor) ->
            new AttackAnimation(0.05F, 0.3F, 0.2F, 0.3F, 0.7F, null, Armatures.BIPED.get().toolR, accessor, Armatures.BIPED)
                .addProperty(StaticAnimationProperty.POSE_MODIFIER, Animations.ReusableSources.COMBO_ATTACK_DIRECTION_MODIFIER));
        BIPED_MOB_UCHIGATANA2 = builder.nextAccessor("biped/combat/mob_uchigatana2", (accessor) ->
            new AttackAnimation(0.15F, 0.01F, 0.01F, 0.1F, 0.55F, null, Armatures.BIPED.get().toolR, accessor, Armatures.BIPED)
                .addProperty(StaticAnimationProperty.POSE_MODIFIER, Animations.ReusableSources.COMBO_ATTACK_DIRECTION_MODIFIER));
        BIPED_MOB_UCHIGATANA3 = builder.nextAccessor("biped/combat/mob_uchigatana3", (accessor) ->
            new AttackAnimation(0.15F, 0.01F, 0.1F, 0.2F, 0.7F, null, Armatures.BIPED.get().toolR, accessor, Armatures.BIPED)
                .addProperty(StaticAnimationProperty.POSE_MODIFIER, Animations.ReusableSources.COMBO_ATTACK_DIRECTION_MODIFIER));

        BIPED_MOB_DAGGER_ONEHAND1 = builder.nextAccessor("biped/combat/mob_dagger_onehand1", (accessor) ->
            new AttackAnimation(0.1F, 0.05F, 0.15F, 0.25F, 0.4F, null, Armatures.BIPED.get().toolR, accessor, Armatures.BIPED)
                .addProperty(StaticAnimationProperty.POSE_MODIFIER, Animations.ReusableSources.COMBO_ATTACK_DIRECTION_MODIFIER));
        BIPED_MOB_DAGGER_ONEHAND2 = builder.nextAccessor("biped/combat/mob_dagger_onehand2", (accessor) ->
            new AttackAnimation(0.1F, 0.05F, 0.01F, 0.1F, 0.45F, null, Armatures.BIPED.get().toolR, accessor, Armatures.BIPED)
                .addProperty(StaticAnimationProperty.POSE_MODIFIER, Animations.ReusableSources.COMBO_ATTACK_DIRECTION_MODIFIER));
        BIPED_MOB_DAGGER_ONEHAND3 = builder.nextAccessor("biped/combat/mob_dagger_onehand3", (accessor) ->
            new AttackAnimation(0.1F, 0.3F, 0.5F, 0.6F, 0.9F, null, Armatures.BIPED.get().toolR, accessor, Armatures.BIPED)
                .addProperty(StaticAnimationProperty.POSE_MODIFIER, Animations.ReusableSources.COMBO_ATTACK_DIRECTION_MODIFIER));
        BIPED_MOB_DAGGER_TWOHAND1 = builder.nextAccessor("biped/combat/mob_dagger_twohand1", (accessor) ->
            new AttackAnimation(0.15F, accessor, Armatures.BIPED,
                      new Phase(0.0F, 0.0F, 0.05F, 0.15F, 0.3F, 0.3F, Armatures.BIPED.get().toolR, null)
                    , new Phase(0.3F, 0.3F, 0.3F, 0.4F, 0.5F, 0.5F, InteractionHand.OFF_HAND, Armatures.BIPED.get().toolL, null)
                    , new Phase(0.5F, 0.5F, 0.55F, 0.65F, 1.0F, Float.MAX_VALUE, Armatures.BIPED.get().toolR, null))
                .addProperty(StaticAnimationProperty.POSE_MODIFIER, Animations.ReusableSources.COMBO_ATTACK_DIRECTION_MODIFIER));
        BIPED_MOB_DAGGER_TWOHAND2 = builder.nextAccessor("biped/combat/mob_dagger_twohand2", (accessor) ->
            new AttackAnimation(0.1F, 0.25F, 0.75F, 0.85F, 1.0F, null, Armatures.BIPED.get().toolR, accessor, Armatures.BIPED)
                .addProperty(StaticAnimationProperty.POSE_MODIFIER, Animations.ReusableSources.COMBO_ATTACK_DIRECTION_MODIFIER));

        BIPED_FRUSTRATED = builder.nextAccessor("biped/emote/frustrated", (accessor) ->
            new EmoteAnimation(0.1F, accessor, Armatures.BIPED)
                .addEvents(StaticAnimationProperty.ON_BEGIN_EVENTS, SimpleEvent.create(Animations.ReusableSources.SET_TOOLS_BACK, Side.CLIENT))
                .addEvents(StaticAnimationProperty.ON_END_EVENTS, SimpleEvent.create(Animations.ReusableSources.REVERT_TO_HANDS, Side.CLIENT))
                .addProperty(StaticAnimationProperty.FIXED_HEAD_ROTATION, true));

        BIPED_HOPAK = builder.nextAccessor("biped/emote/hopak", (accessor) ->
            new EmoteAnimation(0.2F, true, accessor, Armatures.BIPED)
                .addEvents(StaticAnimationProperty.ON_BEGIN_EVENTS, SimpleEvent.create(Animations.ReusableSources.SET_TOOLS_BACK, Side.CLIENT))
                .addEvents(StaticAnimationProperty.ON_END_EVENTS, SimpleEvent.create(Animations.ReusableSources.REVERT_TO_HANDS, Side.CLIENT)));

        BIPED_LAUGH = builder.nextAccessor("biped/emote/laugh", (accessor) ->
            new EmoteAnimation(0.1F, accessor, Armatures.BIPED)
                .addEvents(StaticAnimationProperty.ON_BEGIN_EVENTS, SimpleEvent.create(Animations.ReusableSources.SET_TOOLS_BACK, Side.CLIENT))
                .addEvents(StaticAnimationProperty.ON_END_EVENTS, SimpleEvent.create(Animations.ReusableSources.REVERT_TO_HANDS, Side.CLIENT)));

        BIPED_SALUTE = builder.nextAccessor("biped/emote/salute", (accessor) ->
            new EmoteAnimation(accessor, Armatures.BIPED)
                .addEvents(StaticAnimationProperty.ON_BEGIN_EVENTS, SimpleEvent.create(Animations.ReusableSources.SET_TOOLS_BACK, Side.CLIENT))
                .addEvents(StaticAnimationProperty.ON_END_EVENTS, SimpleEvent.create(Animations.ReusableSources.REVERT_TO_HANDS, Side.CLIENT)));

        BIPED_SLIT_THROAT = builder.nextAccessor("biped/emote/slit_throat", (accessor) ->
            new EmoteAnimation(0.15F, accessor, Armatures.BIPED)
                .addEvents(StaticAnimationProperty.ON_BEGIN_EVENTS, SimpleEvent.create(Animations.ReusableSources.SET_TOOLS_BACK, Side.CLIENT))
                .addEvents(StaticAnimationProperty.ON_END_EVENTS, SimpleEvent.create(Animations.ReusableSources.REVERT_TO_HANDS, Side.CLIENT))
                .addProperty(StaticAnimationProperty.FIXED_HEAD_ROTATION, true));

        BIPED_WAVE_HAND = builder.nextAccessor("biped/emote/wave_hand", (accessor) ->
            new EmoteAnimation(accessor, Armatures.BIPED)
                .addEvents(StaticAnimationProperty.ON_BEGIN_EVENTS, SimpleEvent.create(Animations.ReusableSources.SET_TOOLS_BACK, Side.CLIENT))
                .addEvents(StaticAnimationProperty.ON_END_EVENTS, SimpleEvent.create(Animations.ReusableSources.REVERT_TO_HANDS, Side.CLIENT)));

        BIPED_MOB_THROW = builder.nextAccessor("biped/combat/mob_throw", (accessor) -> new RangedAttackAnimation(0.11F, 0.1F, 0.45F, 0.49F, 0.95F, null, Armatures.BIPED.get().rootJoint, accessor, Armatures.BIPED));

        SWORD_GUARD_HIT = builder.nextAccessor("biped/skill/guard_sword_hit", (accessor) -> new GuardAnimation(0.05F, accessor, Armatures.BIPED));
        SWORD_GUARD_ACTIVE_HIT1 = builder.nextAccessor("biped/skill/guard_sword_hit_active1", (accessor) -> new GuardAnimation(0.05F, 0.2F, accessor, Armatures.BIPED));
        SWORD_GUARD_ACTIVE_HIT2 = builder.nextAccessor("biped/skill/guard_sword_hit_active2", (accessor) -> new GuardAnimation(0.05F, 0.2F, accessor, Armatures.BIPED));
        SWORD_GUARD_ACTIVE_HIT3 = builder.nextAccessor("biped/skill/guard_sword_hit_active3", (accessor) -> new GuardAnimation(0.05F, 0.2F, accessor, Armatures.BIPED));

        LONGSWORD_GUARD_ACTIVE_HIT1 = builder.nextAccessor("biped/skill/guard_longsword_hit_active1", (accessor) -> new GuardAnimation(0.05F, 0.2F, accessor, Armatures.BIPED));
        LONGSWORD_GUARD_ACTIVE_HIT2 = builder.nextAccessor("biped/skill/guard_longsword_hit_active2", (accessor) -> new GuardAnimation(0.05F, 0.2F, accessor, Armatures.BIPED));

        SWORD_DUAL_GUARD_HIT = builder.nextAccessor("biped/skill/guard_dualsword_hit", (accessor) -> new GuardAnimation(0.05F, accessor, Armatures.BIPED));
        BIPED_COMMON_NEUTRALIZED = builder.nextAccessor("biped/skill/guard_break1", (accessor) -> new LongHitAnimation(0.05F, accessor, Armatures.BIPED));
        GREATSWORD_GUARD_BREAK = builder.nextAccessor("biped/skill/guard_break2", (accessor) -> new LongHitAnimation(0.05F, accessor, Armatures.BIPED));

        LONGSWORD_GUARD_HIT = builder.nextAccessor("biped/skill/guard_longsword_hit", (accessor) -> new GuardAnimation(0.05F, accessor, Armatures.BIPED));
        SPEAR_GUARD_HIT = builder.nextAccessor("biped/skill/guard_spear_hit", (accessor) -> new GuardAnimation(0.05F, accessor, Armatures.BIPED));
        GREATSWORD_GUARD_HIT = builder.nextAccessor("biped/skill/guard_greatsword_hit", (accessor) -> new GuardAnimation(0.05F, accessor, Armatures.BIPED));
        UCHIGATANA_GUARD_HIT = builder.nextAccessor("biped/skill/guard_uchigatana_hit", (accessor) -> new GuardAnimation(0.05F, accessor, Armatures.BIPED));

        METEOR_SLAM = builder.nextAccessor("biped/skill/greatsword_slam", (accessor) ->
            new AttackAnimation(0.05F, 0.0F, 0.2F, 0.3F, 1.0F, ColliderPreset.GREATSWORD, Armatures.BIPED.get().toolR, accessor, Armatures.BIPED)
                .removeProperty(AttackPhaseProperty.SWING_SOUND)
                .addProperty(ActionAnimationProperty.MOVE_ON_LINK, false)
                .addProperty(ActionAnimationProperty.STOP_MOVEMENT, true)
                .addProperty(ActionAnimationProperty.MOVE_VERTICAL, true)
                .addProperty(ActionAnimationProperty.COORD_SET_BEGIN, (self, entitypatch, transformSheet) -> {
                    if (self.isLinkAnimation()) {
                        return;
                    }

                    HitResult hitResult = entitypatch.getOriginal().pick(50.0D, 1.0F, false);
                    Vec3 to = hitResult.getLocation();
                    Vec3 from = entitypatch.getOriginal().position();
                    Vec3 correction = to.subtract(from).normalize().scale(5.0D);

                    TransformSheet correctedCoord = self.getCoord().getCorrectedModelCoord(entitypatch, from, to.add(correction), 0, 2);
                    transformSheet.readFrom(correctedCoord);
                })
                .addProperty(ActionAnimationProperty.COORD_SET_TICK, null)
                .addProperty(StaticAnimationProperty.PLAY_SPEED_MODIFIER, (self, entitypatch, speed, prevElapsedTime, elapsedTime) -> {
                    if (0.2F > elapsedTime) {
                        if (entitypatch instanceof PlayerPatch<?> playerpatch) {
                            Optional<SkillContainer> skill = playerpatch.getSkillContainerFor(EpicFightSkills.METEOR_SLAM.get());

                            if (skill.isPresent()) {
                                return (float)Math.sqrt(7.0F / MeteorSlamSkill.getFallDistance(skill.get()));
                            }
                        }
                    }

                    return 1.0F;
                })
                .addEvents(StaticAnimationProperty.ON_BEGIN_EVENTS, SimpleEvent.create((entitypatch, animation, params) -> {
                    entitypatch.playSound(EpicFightSounds.ENTITY_MOVE.get(), 1.0F, 0.0F, 0.0F);
                }, Side.CLIENT))
                .addEvents(InTimeEvent.create(0.25F, Animations.ReusableSources.FRACTURE_METEOR_STRIKE, Side.SERVER)
                                        .params(new Vec3f(0.0F, -0.2F, -1.8F), Armatures.BIPED.get().toolR, 0.3F)));

        REVELATION_ONEHAND = builder.nextAccessor("biped/skill/revelation_normal", (accessor) ->
            new AttackAnimation(0.05F, 0.0F, 0.05F, 0.1F, 0.35F, ColliderPreset.FIST, Armatures.BIPED.get().legR, accessor, Armatures.BIPED)
                .addProperty(AttackPhaseProperty.SWING_SOUND, EpicFightSounds.WHOOSH.get())
                .addProperty(AttackPhaseProperty.PARTICLE, EpicFightParticles.HIT_BLUNT)
                .addProperty(AttackPhaseProperty.HIT_SOUND, EpicFightSounds.BLUNT_HIT.get())
                .addProperty(AttackPhaseProperty.SOURCE_TAG, Set.of(EpicFightDamageTypeTags.COUNTER))
                .addProperty(AttackPhaseProperty.STUN_TYPE, StunType.NEUTRALIZE)
                .addProperty(AttackPhaseProperty.MAX_STRIKES_MODIFIER, ValueModifier.setter(1))
                .addProperty(AttackPhaseProperty.IMPACT_MODIFIER, ValueModifier.setter(0.5F))
                .addProperty(AttackPhaseProperty.ARMOR_NEGATION_MODIFIER, ValueModifier.setter(0.0F))
                .addProperty(AttackPhaseProperty.DAMAGE_MODIFIER, ValueModifier.setter(2.0F))
                .addProperty(ActionAnimationProperty.COORD_SET_BEGIN, null)
                .addProperty(ActionAnimationProperty.COORD_SET_TICK, MoveCoordFunctions.TRACE_TARGET_LOCATION_ROTATION)
                .addProperty(ActionAnimationProperty.ENTITY_YROT_PROVIDER, MoveCoordFunctions.LOOK_DEST)
                .addProperty(StaticAnimationProperty.PLAY_SPEED_MODIFIER, Animations.ReusableSources.CONSTANT_ONE));

        REVELATION_TWOHAND = builder.nextAccessor("biped/skill/revelation_twohand", (accessor) ->
            new AttackAnimation(0.1F, 0.0F, 0.05F, 0.1F, 0.35F, ColliderPreset.FIST_FIXED, Armatures.BIPED.get().rootJoint, accessor, Armatures.BIPED)
                .addProperty(AttackPhaseProperty.SWING_SOUND, EpicFightSounds.WHOOSH.get())
                .addProperty(AttackPhaseProperty.PARTICLE, EpicFightParticles.HIT_BLUNT)
                .addProperty(AttackPhaseProperty.HIT_SOUND, EpicFightSounds.BLUNT_HIT.get())
                .addProperty(AttackPhaseProperty.SOURCE_TAG, Set.of(EpicFightDamageTypeTags.COUNTER))
                .addProperty(AttackPhaseProperty.STUN_TYPE, StunType.NEUTRALIZE)
                .addProperty(AttackPhaseProperty.MAX_STRIKES_MODIFIER, ValueModifier.setter(1))
                .addProperty(AttackPhaseProperty.IMPACT_MODIFIER, ValueModifier.setter(0.5F))
                .addProperty(AttackPhaseProperty.ARMOR_NEGATION_MODIFIER, ValueModifier.setter(0.0F))
                .addProperty(AttackPhaseProperty.DAMAGE_MODIFIER, ValueModifier.setter(2.0F))
                .addProperty(ActionAnimationProperty.COORD_SET_BEGIN, null)
                .addProperty(ActionAnimationProperty.COORD_SET_TICK, MoveCoordFunctions.TRACE_TARGET_LOCATION_ROTATION)
                .addProperty(ActionAnimationProperty.ENTITY_YROT_PROVIDER, MoveCoordFunctions.LOOK_DEST)
                .addProperty(StaticAnimationProperty.PLAY_SPEED_MODIFIER, Animations.ReusableSources.CONSTANT_ONE));

        BIPED_HIT_SHORT = builder.nextAccessor("biped/combat/hit_short", (accessor) -> new HitAnimation(0.05F, accessor, Armatures.BIPED));
        BIPED_HIT_LONG = builder.nextAccessor("biped/combat/hit_long", (accessor) -> new LongHitAnimation(0.08F, accessor, Armatures.BIPED));
        BIPED_HIT_ON_MOUNT = builder.nextAccessor("biped/combat/hit_on_mount", (accessor) -> new LongHitAnimation(0.08F, accessor, Armatures.BIPED)
                .addProperty(StaticAnimationProperty.ON_ITEM_CHANGE_EVENT, SimpleEvent.create(Animations.ReusableSources.SET_TOOLS_BACK_WHEN_MOUNT_AND_ITEM_CHANGED, Side.CLIENT))
                .addEvents(StaticAnimationProperty.ON_BEGIN_EVENTS, SimpleEvent.create(Animations.ReusableSources.SET_TOOLS_BACK_WHEN_MOUNT, Side.CLIENT))
                .addEvents(StaticAnimationProperty.ON_END_EVENTS, SimpleEvent.create(Animations.ReusableSources.REVERT_TO_HANDS, Side.CLIENT)));

        BIPED_LANDING = builder.nextAccessor("biped/living/landing", (accessor) -> new LongHitAnimation(0.03F, accessor, Armatures.BIPED));

        BIPED_KNOCKDOWN = builder.nextAccessor("biped/combat/knockdown", (accessor) -> new KnockdownAnimation(0.08F, accessor, Armatures.BIPED));
        BIPED_DEATH = builder.nextAccessor("biped/living/death", (accessor) -> new LongHitAnimation(0.16F, accessor, Armatures.BIPED));

        CREEPER_HIT_SHORT = builder.nextAccessor("creeper/hit_short", (accessor) -> new HitAnimation(0.05F, accessor, Armatures.CREEPER));
        CREEPER_HIT_LONG = builder.nextAccessor("creeper/hit_long", (accessor) -> new LongHitAnimation(0.08F, accessor, Armatures.CREEPER));
        CREEPER_DEATH = builder.nextAccessor("creeper/death", (accessor) -> new LongHitAnimation(0.16F, accessor, Armatures.CREEPER));

        ENDERMAN_HIT_SHORT = builder.nextAccessor("enderman/hit_short", (accessor) -> new HitAnimation(0.05F, accessor, Armatures.ENDERMAN));
        ENDERMAN_HIT_LONG = builder.nextAccessor("enderman/hit_long", (accessor) -> new LongHitAnimation(0.08F, accessor, Armatures.ENDERMAN));
        ENDERMAN_NEUTRALIZED = builder.nextAccessor("enderman/neutralized", (accessor) -> new LongHitAnimation(0.18F, accessor, Armatures.ENDERMAN));
        ENDERMAN_CONVERT_RAGE = builder.nextAccessor("enderman/convert_rage", (accessor) -> new InvincibleAnimation(0.16F, accessor, Armatures.ENDERMAN));
        ENDERMAN_TP_KICK1 = builder.nextAccessor("enderman/tp_kick1", (accessor) ->
            new AttackAnimation(0.06F, 0.15F, 0.3F, 0.4F, 1.0F, ColliderPreset.ENDERMAN_LIMB, Armatures.ENDERMAN.get().legR, accessor, Armatures.ENDERMAN)
                .addEvents(StaticAnimationProperty.ON_BEGIN_EVENTS,
                    SimpleEvent.create(Animations.ReusableSources.TELEPORT_ATTACK_SERVER, Side.SERVER),
                    SimpleEvent.create(Animations.ReusableSources.TELEPORT_ATTACK_CLIENT, Side.CLIENT)
                )
        );
        ENDERMAN_TP_KICK2 = builder.nextAccessor("enderman/tp_kick2", (accessor) ->
            new AttackAnimation(0.16F, 0.15F, 0.25F, 0.45F, 1.0F, ColliderPreset.ENDERMAN_LIMB, Armatures.ENDERMAN.get().legR, accessor, Armatures.ENDERMAN)
                .addEvents(StaticAnimationProperty.ON_BEGIN_EVENTS,
                    SimpleEvent.create(Animations.ReusableSources.TELEPORT_ATTACK_SERVER, Side.SERVER),
                    SimpleEvent.create(Animations.ReusableSources.TELEPORT_ATTACK_CLIENT, Side.CLIENT)
                )
        );
        ENDERMAN_KICK1 = builder.nextAccessor("enderman/rush_kick", (accessor) ->
            new AttackAnimation(0.16F, 0.66F, 0.7F, 0.81F, 1.6F, ColliderPreset.ENDERMAN_LIMB, Armatures.ENDERMAN.get().legL, accessor, Armatures.ENDERMAN)
                .addProperty(AttackPhaseProperty.IMPACT_MODIFIER, ValueModifier.setter(4.0F)));
        ENDERMAN_KICK2 = builder.nextAccessor("enderman/jump_kick", (accessor) -> new AttackAnimation(0.16F, 0.8F, 0.8F, 0.9F, 1.3F, ColliderPreset.ENDERMAN_LIMB, Armatures.ENDERMAN.get().legR, accessor, Armatures.ENDERMAN));
        ENDERMAN_KNEE = builder.nextAccessor("enderman/knee", (accessor) ->
            new AttackAnimation(0.16F, 0.25F, 0.25F, 0.31F, 1.0F, ColliderPreset.FIST, Armatures.ENDERMAN.get().legR, accessor, Armatures.ENDERMAN)
                .addProperty(AttackPhaseProperty.STUN_TYPE, StunType.LONG));
        ENDERMAN_KICK_COMBO = builder.nextAccessor("enderman/kick_twice", (accessor) ->
            new AttackAnimation(0.1F, accessor, Armatures.ENDERMAN,
                    new Phase(0.0F, 0.15F, 0.15F, 0.21F, 0.46F, 0.6F, Armatures.ENDERMAN.get().legR, ColliderPreset.ENDERMAN_LIMB),
                    new Phase(0.6F, 0.75F, 0.75F, 0.81F, 1.6F, Float.MAX_VALUE, Armatures.ENDERMAN.get().legL, ColliderPreset.ENDERMAN_LIMB))
                .addProperty(ActionAnimationProperty.STOP_MOVEMENT, true));
        ENDERMAN_GRASP = builder.nextAccessor("enderman/grasp", (accessor) ->
            new AttackAnimation(0.06F, 0.5F, 0.45F, 1.0F, 1.0F, ColliderPreset.ENDERMAN_LIMB, Armatures.BIPED.get().toolR, accessor, Armatures.ENDERMAN)
                .addProperty(StaticAnimationProperty.POSE_MODIFIER, Animations.ReusableSources.COMBO_ATTACK_DIRECTION_MODIFIER));
        ENDERMAN_DEATH = builder.nextAccessor("enderman/death", (accessor) -> new LongHitAnimation(0.16F, accessor, Armatures.ENDERMAN));
        ENDERMAN_TP_EMERGENCE = builder.nextAccessor("enderman/teleport", (accessor) ->
            new ActionAnimation(0.05F, accessor, Armatures.ENDERMAN)
                .addProperty(ActionAnimationProperty.STOP_MOVEMENT, true));

        DRAGON_IDLE = builder.nextAccessor("dragon/idle", (accessor) -> new StaticAnimation(0.6F, true, accessor, Armatures.DRAGON));

        DRAGON_WALK = builder.nextAccessor("dragon/walk", (accessor) ->
            new EnderDragonWalkAnimation(0.35F, accessor, Armatures.DRAGON)
                .addProperty(StaticAnimationProperty.IK_DEFINITION, List.of(
                      InverseKinematicsDefinition.create(Armatures.DRAGON.get().legFrontL1, Armatures.DRAGON.get().legFrontL3, Armatures.DRAGON.get().legFrontR3, IntIntPair.of(0, 3), 0.12F, 0, new boolean[] {true, true, true})
                    , InverseKinematicsDefinition.create(Armatures.DRAGON.get().legFrontR1, Armatures.DRAGON.get().legFrontR3, Armatures.DRAGON.get().legFrontL3, IntIntPair.of(2, 5), 0.12F, 2, new boolean[] {true, true, true})
                    , InverseKinematicsDefinition.create(Armatures.DRAGON.get().legBackL1, Armatures.DRAGON.get().legBackL3, Armatures.DRAGON.get().legBackR3, IntIntPair.of(2, 5), 0.1344F, 4, new boolean[] {true, true, true})
                    , InverseKinematicsDefinition.create(Armatures.DRAGON.get().legBackR1, Armatures.DRAGON.get().legBackR3, Armatures.DRAGON.get().legBackL3, IntIntPair.of(0, 3), 0.1344F, 2, new boolean[] {true, true, true})
                )));

        DRAGON_FLY = builder.nextAccessor("dragon/fly", (accessor) ->
            new StaticAnimation(0.35F, true, accessor, Armatures.DRAGON)
                .addEvents(InTimeEvent.create(0.4F, Animations.ReusableSources.WING_FLAP, AnimationEvent.Side.CLIENT)));

        DRAGON_DEATH = builder.nextAccessor("dragon/death", (accessor) -> new EnderDragonDeathAnimation(1.0F, accessor, Armatures.DRAGON));

        DRAGON_GROUND_TO_FLY = builder.nextAccessor("dragon/ground_to_fly", (accessor) ->
            new EnderDragonActionAnimation(0.25F, accessor, Armatures.DRAGON)
                .addProperty(ActionAnimationProperty.STOP_MOVEMENT, true)
                .addProperty(ActionAnimationProperty.MOVE_VERTICAL, true)
                .addProperty(StaticAnimationProperty.IK_DEFINITION, List.of(
                      InverseKinematicsDefinition.create(Armatures.DRAGON.get().legFrontL1, Armatures.DRAGON.get().legFrontL3, null, IntIntPair.of(3, 7), 0.12F, 0, new boolean[] {true, false, false, false})
                    , InverseKinematicsDefinition.create(Armatures.DRAGON.get().legFrontR1, Armatures.DRAGON.get().legFrontR3, null, IntIntPair.of(3, 7), 0.12F, 0, new boolean[] {true, false, false, false})
                    , InverseKinematicsDefinition.create(Armatures.DRAGON.get().legBackL1, Armatures.DRAGON.get().legBackL3, null, IntIntPair.of(4, 7), 0.1344F, 0, new boolean[] {true, false, false, false})
                    , InverseKinematicsDefinition.create(Armatures.DRAGON.get().legBackR1, Armatures.DRAGON.get().legBackR3, null, IntIntPair.of(4, 7), 0.1344F, 0, new boolean[] {true, false, false, false})
                ))
                .addEvents(
                    InTimeEvent.create(0.25F, ReusableSources.WING_FLAP, AnimationEvent.Side.CLIENT),
                    InTimeEvent.create(1.05F, ReusableSources.WING_FLAP, AnimationEvent.Side.CLIENT),
                    InTimeEvent.create(1.45F, (entitypatch, animation, params) -> {
                        if (entitypatch instanceof EnderDragonPatch enderDragonPatch) {
                            enderDragonPatch.setFlyingPhase();
                        }
                    }, AnimationEvent.Side.BOTH)
                ));

        DRAGON_FLY_TO_GROUND = builder.nextAccessor("dragon/fly_to_ground", (accessor) ->
            new EnderDragonDynamicActionAnimation(0.35F, accessor, Armatures.DRAGON)
                .addProperty(ActionAnimationProperty.STOP_MOVEMENT, true)
                .addProperty(ActionAnimationProperty.MOVE_VERTICAL, true)
                .addProperty(ActionAnimationProperty.MOVE_ON_LINK, false)
                .addProperty(ActionAnimationProperty.MOVE_TIME, TimePairList.create(0.0F, 1.35F))
                .addProperty(ActionAnimationProperty.COORD_SET_BEGIN, (self, entitypatch, transformSheet) -> {
                    if (!self.isLinkAnimation() && entitypatch instanceof EnderDragonPatch dragonpatch) {
                        TransformSheet transform = self.getCoord().copyAll();
                        Vec3 dragonpos = dragonpatch.getOriginal().position();
                        Vec3 targetpos = dragonpatch.getOriginal().getPhaseManager().getPhase(PatchedPhases.LANDING).getLandingPosition();
                        float horizontalDistance = (float) dragonpos.subtract(0, dragonpos.y, 0).distanceTo(targetpos.subtract(0, targetpos.y, 0));
                        float verticalDistance = (float) Math.abs(dragonpos.y - targetpos.y);
                        JointTransform jt0 = transform.getKeyframes()[0].transform();
                        JointTransform jt1 = transform.getKeyframes()[1].transform();
                        JointTransform jt2 = transform.getKeyframes()[2].transform();
                        OpenMatrix4f coordReverse = OpenMatrix4f.createRotatorDeg(90F, Vec3f.X_AXIS);
                        Vec3f jointCoord = OpenMatrix4f.transform3v(coordReverse, new Vec3f(jt0.translation().x, verticalDistance, horizontalDistance), null);
                        jt0.translation().set(jointCoord);
                        jt1.translation().set(MathUtils.lerpVector(jt0.translation(), jt2.translation(), transform.getKeyframes()[1].time() / transform.getKeyframes()[2].time()));

                        transformSheet.readFrom(transform);
                    } else {
                        transformSheet.readFrom(TransformSheet.EMPTY_SHEET);
                    }
                })
                .addProperty(StaticAnimationProperty.IK_DEFINITION, List.of(
                      InverseKinematicsDefinition.create(Armatures.DRAGON.get().legFrontL1, Armatures.DRAGON.get().legFrontL3, null, IntIntPair.of(0, 4), 0.12F, 9, new boolean[] {false, false, false, true})
                    , InverseKinematicsDefinition.create(Armatures.DRAGON.get().legFrontR1, Armatures.DRAGON.get().legFrontR3, null, IntIntPair.of(0, 4), 0.12F, 9, new boolean[] {false, false, false, true})
                    , InverseKinematicsDefinition.create(Armatures.DRAGON.get().legBackL1, Armatures.DRAGON.get().legBackL3, null, IntIntPair.of(0, 4), 0.1344F, 7, new boolean[] {false, false, false, true})
                    , InverseKinematicsDefinition.create(Armatures.DRAGON.get().legBackR1, Armatures.DRAGON.get().legBackR3, null, IntIntPair.of(0, 4), 0.1344F, 7, new boolean[] {false, false, false, true})
                ))
                .addEvents(
                    InTimeEvent.create(0.3F, ReusableSources.WING_FLAP, AnimationEvent.Side.CLIENT), InTimeEvent.create(1.1F, (entitypatch, animation, params) -> {
                        entitypatch.playSound(EpicFightSounds.SLAM_HEAVY.get(), 0, 0);
                        LivingEntity original = entitypatch.getOriginal();
                        BlockPos blockpos = original.level().getHeightmapPos(Heightmap.Types.WORLD_SURFACE, original.blockPosition());
                        original.level().addParticle(EpicFightParticles.GROUND_SLAM.get(), blockpos.getX(), blockpos.getY(), blockpos.getZ(), 3.0D, 100.0D, 1.0D);
                    }, AnimationEvent.Side.CLIENT),
                    InTimeEvent.create(1.1F, (entitypatch, animation, params) -> {
                        LivingEntity original = entitypatch.getOriginal();
                        DamageSource extDamageSource = EpicFightDamageSources.mobAttack(original).setAnimation(DRAGON_FLY_TO_GROUND).setStunType(StunType.KNOCKDOWN);

                        for (Entity entity : original.level().getEntities(original, original.getBoundingBox().deflate(3.0D, 0.0D, 3.0D))) {
                            entity.hurt(extDamageSource, 6.0F);
                        }
                    }, AnimationEvent.Side.SERVER)
                ));

        DRAGON_ATTACK1 = builder.nextAccessor("dragon/attack1", (accessor) ->
            new EnderDragonAttackAnimation(0.35F, 0.4F, 0.65F, 0.76F, 1.9F, ColliderPreset.DRAGON_LEG, Armatures.DRAGON.get().legFrontR3, accessor, Armatures.DRAGON)
                .addProperty(AttackPhaseProperty.STUN_TYPE, StunType.KNOCKDOWN)
                .addProperty(StaticAnimationProperty.IK_DEFINITION, List.of(
                      InverseKinematicsDefinition.create(Armatures.DRAGON.get().legFrontL1, Armatures.DRAGON.get().legFrontL3, null, IntIntPair.of(2, 4), 0.12F, 0, new boolean[] {true, true})
                    , InverseKinematicsDefinition.create(Armatures.DRAGON.get().legFrontR1, Armatures.DRAGON.get().legFrontR3, null, IntIntPair.of(0, 5), 0.12F, 0, new boolean[] {false, false, false, false, true})
                    , InverseKinematicsDefinition.create(Armatures.DRAGON.get().legBackL1, Armatures.DRAGON.get().legBackL3, null, null, 0.1344F, 0, new boolean[] {})
                    , InverseKinematicsDefinition.create(Armatures.DRAGON.get().legBackR1, Armatures.DRAGON.get().legBackR3, null, IntIntPair.of(1, 4), 0.1344F, 0, new boolean[] {true, false, true})
                ))
                .addEvents(InTimeEvent.create(0.65F, (entitypatch, animation, params) -> {
                    entitypatch.playSound(EpicFightSounds.SLAM_HEAVY.get(), 0, 0);

                    if (entitypatch instanceof EnderDragonPatch dragonpatch) {
                        dragonpatch.getIKSimulator().getRunningObject(Armatures.DRAGON.get().legFrontR3).ifPresent((ikObject) -> {
                            Vec3f tipPosition = ikObject.getDestination();
                            entitypatch.getLevel().addParticle(EpicFightParticles.GROUND_SLAM.get(), tipPosition.x, tipPosition.y, tipPosition.z, 0.5D, 100.0D, 0.5D);
                        });
                    }
                }, AnimationEvent.Side.CLIENT)));

        DRAGON_ATTACK2 = builder.nextAccessor("dragon/attack2", (accessor) ->
            new EnderDragonAttackAnimation(0.35F, 0.25F, 0.45F, 0.66F, 0.75F, ColliderPreset.DRAGON_LEG, Armatures.DRAGON.get().legFrontR3, accessor, Armatures.DRAGON)
                .addProperty(StaticAnimationProperty.IK_DEFINITION, List.of(
                      InverseKinematicsDefinition.create(Armatures.DRAGON.get().legFrontL1, Armatures.DRAGON.get().legFrontL3, null, IntIntPair.of(1, 4), 0.12F, 0, new boolean[] {true, true, true})
                    , InverseKinematicsDefinition.create(Armatures.DRAGON.get().legBackL1, Armatures.DRAGON.get().legBackL3, null, null, 0.1344F, 0, new boolean[] {})
                    , InverseKinematicsDefinition.create(Armatures.DRAGON.get().legBackR1, Armatures.DRAGON.get().legBackR3, null, null, 0.1344F, 0, new boolean[] {})
                )));

        DRAGON_ATTACK3 = builder.nextAccessor("dragon/attack3", (accessor) ->
            new EnderDragonAttackAnimation(0.35F, 0.25F, 0.45F, 0.66F, 0.75F, ColliderPreset.DRAGON_LEG, Armatures.DRAGON.get().legFrontL3, accessor, Armatures.DRAGON)
                .addProperty(StaticAnimationProperty.IK_DEFINITION, List.of(
                      InverseKinematicsDefinition.create(Armatures.DRAGON.get().legFrontR1, Armatures.DRAGON.get().legFrontR3, null, IntIntPair.of(1, 4), 0.12F, 0, new boolean[] {true, true, true})
                    , InverseKinematicsDefinition.create(Armatures.DRAGON.get().legBackL1, Armatures.DRAGON.get().legBackL3, null, null, 0.1344F, 0, new boolean[] {})
                    , InverseKinematicsDefinition.create(Armatures.DRAGON.get().legBackR1, Armatures.DRAGON.get().legBackR3, null, null, 0.1344F, 0, new boolean[] {})
                )));

        DRAGON_ATTACK4 = builder.nextAccessor("dragon/attack4", (accessor) ->
            new EnderDragonAttackAnimation(0.35F, 0.5F, 1.15F, 1.26F, 1.9F, ColliderPreset.DRAGON_BODY, Armatures.DRAGON.get().rootJoint, accessor, Armatures.DRAGON)
                .addProperty(AttackPhaseProperty.STUN_TYPE, StunType.KNOCKDOWN)
                .addProperty(ActionAnimationProperty.MOVE_VERTICAL, true)
                .addProperty(StaticAnimationProperty.IK_DEFINITION, List.of(
                      InverseKinematicsDefinition.create(Armatures.DRAGON.get().legFrontL1, Armatures.DRAGON.get().legFrontL3, null, IntIntPair.of(0, 7), 0.12F, 0, new boolean[] {false, false, false, false, true, true, true})
                    , InverseKinematicsDefinition.create(Armatures.DRAGON.get().legFrontR1, Armatures.DRAGON.get().legFrontR3, null, IntIntPair.of(0, 7), 0.12F, 0, new boolean[] {false, false, false, false, true, true, true})
                    , InverseKinematicsDefinition.create(Armatures.DRAGON.get().legBackL1, Armatures.DRAGON.get().legBackL3, null, IntIntPair.of(3, 8), 0.1344F, 0, new boolean[] {false, false, false, false, true})
                    , InverseKinematicsDefinition.create(Armatures.DRAGON.get().legBackR1, Armatures.DRAGON.get().legBackR3, null, IntIntPair.of(3, 8), 0.1344F, 0, new boolean[] {false, false, false, false, true})
                ))
                .addEvents(
                    InTimeEvent.create(1.2F, (entitypatch, animation, params) -> {
                        entitypatch.playSound(EpicFightSounds.SLAM_HEAVY.get(), 0, 0);

                        if (entitypatch instanceof EnderDragonPatch dragonpatch) {
                            dragonpatch.getIKSimulator().getRunningObject(Armatures.DRAGON.get().legFrontR3).ifPresent((ikObject) -> {
                                Vec3f tipPosition = ikObject.getDestination();
                                entitypatch.getLevel().addParticle(EpicFightParticles.GROUND_SLAM.get(), tipPosition.x, tipPosition.y, tipPosition.z, 3.0D, 100.0D, 1.0D);
                            });
                        }
                    }, AnimationEvent.Side.CLIENT),
                    InTimeEvent.create(1.85F, (entitypatch, animation, params) -> {
                        entitypatch.getAnimator().reserveAnimation(DRAGON_ATTACK4_RECOVERY);
                    }, AnimationEvent.Side.BOTH))
                );

        DRAGON_ATTACK4_RECOVERY = builder.nextAccessor("dragon/attack4_recovery", (accessor) ->
            new EnderDragonActionAnimation(0.35F, accessor, Armatures.DRAGON)
                .addProperty(StaticAnimationProperty.IK_DEFINITION, List.of(
                      InverseKinematicsDefinition.create(Armatures.DRAGON.get().legFrontL1, Armatures.DRAGON.get().legFrontL3, null, IntIntPair.of(0, 4), 0.12F, 0, new boolean[] {true, false, true, true})
                    , InverseKinematicsDefinition.create(Armatures.DRAGON.get().legFrontR1, Armatures.DRAGON.get().legFrontR3, null, IntIntPair.of(0, 3), 0.12F, 0, new boolean[] {true, true, true})
                    , InverseKinematicsDefinition.create(Armatures.DRAGON.get().legBackL1, Armatures.DRAGON.get().legBackL3, null, IntIntPair.of(0, 5), 0.1344F, 0, new boolean[] {true, true, false, false, true})
                    , InverseKinematicsDefinition.create(Armatures.DRAGON.get().legBackR1, Armatures.DRAGON.get().legBackR3, null, IntIntPair.of(0, 4), 0.1344F, 0, new boolean[] {true, true, false, false})
                )));

        DRAGON_FIREBALL = builder.nextAccessor("dragon/fireball", (accessor) ->
            new EnderDragonActionAnimation(0.16F, accessor, Armatures.DRAGON)
                .addProperty(StaticAnimationProperty.IK_DEFINITION, List.of(
                      InverseKinematicsDefinition.create(Armatures.DRAGON.get().legFrontL1, Armatures.DRAGON.get().legFrontL3, null, IntIntPair.of(0, 5), 0.12F, 0, new boolean[] {true, true, true, true, true})
                    , InverseKinematicsDefinition.create(Armatures.DRAGON.get().legFrontR1, Armatures.DRAGON.get().legFrontR3, null, IntIntPair.of(0, 5), 0.12F, 0, new boolean[] {true, true, true, true, true})
                    , InverseKinematicsDefinition.create(Armatures.DRAGON.get().legBackL1, Armatures.DRAGON.get().legBackL3, null, IntIntPair.of(0, 5), 0.1344F, 0, new boolean[] {true, true, true, true, true})
                    , InverseKinematicsDefinition.create(Armatures.DRAGON.get().legBackR1, Armatures.DRAGON.get().legBackR3, null, IntIntPair.of(0, 5), 0.1344F, 0, new boolean[] {true, true, true, true, true})
                ))
                .addEvents(
                    InTimeEvent.create(0.65F, (entitypatch, animation, params) -> {
                        LivingEntity original = entitypatch.getOriginal();
                        Entity target = entitypatch.getTarget();
                        Vec3 pos = original.position();
                        Vec3 toTarget = target.position().subtract(original.position()).normalize().scale(original.getBbWidth() * 0.5D);

                        double d6 = (float)(pos.x + toTarget.x);
                        double d7 = (float)(pos.y + 2.0F);
                        double d8 = (float)(pos.z + toTarget.z);
                        double dx = target.getX() - d6;
                        double dy = target.getY(0.5D) - d7;
                        double dz = target.getZ() - d8;

                        if (!original.isSilent()) {
                           original.level().levelEvent(null, 1017, original.blockPosition(), 0);
                        }

                        DragonFireball dragonfireball = new DragonFireball(original.level(), original, new Vec3(dx, dy, dz));
                        dragonfireball.moveTo(d6, d7, d8, 0.0F, 0.0F);
                        original.level().addFreshEntity(dragonfireball);
                    }, Side.SERVER))
                );

        DRAGON_AIRSTRIKE = builder.nextAccessor("dragon/airstrike", (accessor) ->
            new StaticAnimation(0.35F, true, accessor, Armatures.DRAGON)
                .addEvents(InTimeEvent.create(0.3F, ReusableSources.WING_FLAP, AnimationEvent.Side.CLIENT)));

        DRAGON_BACKJUMP_PREPARE = builder.nextAccessor("dragon/backjump_prepare", (accessor) ->
            new EnderDragonActionAnimation(0.35F, accessor, Armatures.DRAGON)
                .addProperty(StaticAnimationProperty.IK_DEFINITION, List.of(
                      InverseKinematicsDefinition.create(Armatures.DRAGON.get().legFrontL1, Armatures.DRAGON.get().legFrontL3, null, IntIntPair.of(0, 4), 0.12F, 0, new boolean[] {true, true, true, true})
                    , InverseKinematicsDefinition.create(Armatures.DRAGON.get().legFrontR1, Armatures.DRAGON.get().legFrontR3, null, IntIntPair.of(0, 4), 0.12F, 0, new boolean[] {true, true, true, true})
                    , InverseKinematicsDefinition.create(Armatures.DRAGON.get().legBackL1, Armatures.DRAGON.get().legBackL3, null, IntIntPair.of(0, 4), 0.1344F, 0, new boolean[] {true, true, true, true})
                    , InverseKinematicsDefinition.create(Armatures.DRAGON.get().legBackR1, Armatures.DRAGON.get().legBackR3, null, IntIntPair.of(0, 4), 0.1344F, 0, new boolean[] {true, true, true, true})
                ))
                .addEvents(InTimeEvent.create(0.3F, (entitypatch, animation, params) -> {
                    entitypatch.getAnimator().reserveAnimation(DRAGON_BACKJUMP_MOVE);
                }, Side.BOTH)));

        DRAGON_BACKJUMP_MOVE = builder.nextAccessor("dragon/backjump_move", (accessor) ->
            new AttackAnimation(0.0F, 10.0F, 10.0F, 10.0F, 10.0F, ColliderPreset.FIST, Armatures.DRAGON.get().rootJoint, accessor, Armatures.DRAGON)
                .addProperty(ActionAnimationProperty.COORD_SET_BEGIN, MoveCoordFunctions.RAW_COORD)
                .addProperty(ActionAnimationProperty.COORD_SET_TICK, null)
                .addEvents(InTimeEvent.create(1.0F, (entitypatch, animation, params) -> {
                    entitypatch.getAnimator().reserveAnimation(DRAGON_BACKJUMP_RECOVERY);
                }, Side.BOTH)));

        DRAGON_BACKJUMP_RECOVERY = builder.nextAccessor("dragon/backjump_recovery", (accessor) ->
            new EnderDragonActionAnimation(0.0F, accessor, Armatures.DRAGON)
                .addProperty(StaticAnimationProperty.IK_DEFINITION, List.of(
                      InverseKinematicsDefinition.create(Armatures.DRAGON.get().legFrontL1, Armatures.DRAGON.get().legFrontL3, null, IntIntPair.of(0, 4), 0.12F, 0, new boolean[] {false, true, true, true})
                    , InverseKinematicsDefinition.create(Armatures.DRAGON.get().legFrontR1, Armatures.DRAGON.get().legFrontR3, null, IntIntPair.of(0, 4), 0.12F, 0, new boolean[] {false, true, true, true})
                    , InverseKinematicsDefinition.create(Armatures.DRAGON.get().legBackL1, Armatures.DRAGON.get().legBackL3, null, IntIntPair.of(0, 4), 0.1344F, 0, new boolean[] {true, true, true, true})
                    , InverseKinematicsDefinition.create(Armatures.DRAGON.get().legBackR1, Armatures.DRAGON.get().legBackR3, null, IntIntPair.of(0, 4), 0.1344F, 0, new boolean[] {true, true, true, true})
                ))
            .addProperty(ActionAnimationProperty.MOVE_VERTICAL, true)
            .addEvents(
                InTimeEvent.create(0.15F, (entitypatch, animation, params) -> {
                    entitypatch.playSound(EpicFightSounds.SLAM_HEAVY.get(), 0, 0);

                    if (entitypatch instanceof EnderDragonPatch dragonpatch) {
                        dragonpatch.getIKSimulator().getRunningObject(Armatures.DRAGON.get().legFrontR3).ifPresent((ikObject) -> {
                            Vec3f tipPosition = ikObject.getDestination();
                            entitypatch.getLevel().addParticle(EpicFightParticles.GROUND_SLAM.get(), tipPosition.x, tipPosition.y, tipPosition.z, 3.0D, 100.0D, 1.0D);
                        });
                    }
                }, AnimationEvent.Side.CLIENT))
            );

        DRAGON_CRYSTAL_LINK = builder.nextAccessor("dragon/crystal_link", (accessor) ->
            new EnderDragonActionAnimation(0.5F, accessor, Armatures.DRAGON)
                .addProperty(StaticAnimationProperty.IK_DEFINITION, List.of(
                      InverseKinematicsDefinition.create(Armatures.DRAGON.get().legFrontL1, Armatures.DRAGON.get().legFrontL3, null, IntIntPair.of(0, 2), 0.12F, 0, new boolean[] {true, true})
                    , InverseKinematicsDefinition.create(Armatures.DRAGON.get().legFrontR1, Armatures.DRAGON.get().legFrontR3, null, IntIntPair.of(0, 2), 0.12F, 0, new boolean[] {true, true})
                    , InverseKinematicsDefinition.create(Armatures.DRAGON.get().legBackL1, Armatures.DRAGON.get().legBackL3, null, IntIntPair.of(0, 2), 0.1344F, 0, new boolean[] {true, true})
                    , InverseKinematicsDefinition.create(Armatures.DRAGON.get().legBackR1, Armatures.DRAGON.get().legBackR3, null, IntIntPair.of(0, 2), 0.1344F, 0, new boolean[] {true, true})
                ))
            .addEvents(
                InTimeEvent.create(7.0F, (entitypatch, animation, params) -> {
                    entitypatch.getOriginal().playSound(SoundEvents.ENDER_DRAGON_GROWL, 7.0F, 0.8F + entitypatch.getOriginal().getRandom().nextFloat() * 0.3F);
                    entitypatch.getOriginal().setHealth(entitypatch.getOriginal().getMaxHealth());

                    if (entitypatch instanceof EnderDragonPatch dragonpatch) {
                        dragonpatch.getOriginal().getPhaseManager().setPhase(PatchedPhases.GROUND_BATTLE);
                        dragonpatch.setStunShield(0.0F);
                    }
                }, AnimationEvent.Side.SERVER),
                InTimeEvent.create(7.0F, (entitypatch, animation, params) -> {
                    Entity original = entitypatch.getOriginal();
                    original.level().addParticle(EpicFightParticles.FORCE_FIELD_END.get(), original.getX(), original.getY() + 2.0D, original.getZ(), 0, 0, 0);
                }, AnimationEvent.Side.CLIENT))
            );

        DRAGON_NEUTRALIZED = builder.nextAccessor("dragon/neutralized", (accessor) ->
            new EnderDragonActionAnimation(0.1F, accessor, Armatures.DRAGON)
                .addProperty(StaticAnimationProperty.IK_DEFINITION, List.of(
                      InverseKinematicsDefinition.create(Armatures.DRAGON.get().legFrontL1, Armatures.DRAGON.get().legFrontL3, null, IntIntPair.of(0, 4), 0.12F, 0, new boolean[] {true, true, true, true})
                    , InverseKinematicsDefinition.create(Armatures.DRAGON.get().legFrontR1, Armatures.DRAGON.get().legFrontR3, null, IntIntPair.of(0, 4), 0.12F, 0, new boolean[] {true, true, true, true})
                    , InverseKinematicsDefinition.create(Armatures.DRAGON.get().legBackL1, Armatures.DRAGON.get().legBackL3, null, IntIntPair.of(0, 4), 0.1344F, 0, new boolean[] {true, true, true, true})
                    , InverseKinematicsDefinition.create(Armatures.DRAGON.get().legBackR1, Armatures.DRAGON.get().legBackR3, null, IntIntPair.of(0, 4), 0.1344F, 0, new boolean[] {true, true, true, true})
                ))
            .addEvents(InTimeEvent.create(3.95F, (entitypatch, animation, params) -> {
                entitypatch.getAnimator().playAnimation(DRAGON_NEUTRALIZED_RECOVERY, 0);
            }, AnimationEvent.Side.BOTH)));

        DRAGON_NEUTRALIZED_RECOVERY = builder.nextAccessor("dragon/neutralized_recovery", (accessor) ->
            new EnderDragonActionAnimation(0.05F, accessor, Armatures.DRAGON)
                .addProperty(StaticAnimationProperty.IK_DEFINITION, List.of(
                      InverseKinematicsDefinition.create(Armatures.DRAGON.get().legFrontL1, Armatures.DRAGON.get().legFrontL3, null, IntIntPair.of(0, 5), 0.12F, 0, new boolean[] {true, true, true, false, true})
                    , InverseKinematicsDefinition.create(Armatures.DRAGON.get().legFrontR1, Armatures.DRAGON.get().legFrontR3, null, IntIntPair.of(0, 5), 0.12F, 0, new boolean[] {true, false, true, true, true})
                    , InverseKinematicsDefinition.create(Armatures.DRAGON.get().legBackL1, Armatures.DRAGON.get().legBackL3, null, IntIntPair.of(0, 5), 0.1344F, 0, new boolean[] {true, true, true, true, true})
                    , InverseKinematicsDefinition.create(Armatures.DRAGON.get().legBackR1, Armatures.DRAGON.get().legBackR3, null, IntIntPair.of(0, 4), 0.1344F, 0, new boolean[] {true, true, true, true})
                ))
                .addEvents(InTimeEvent.create(1.6F, (entitypatch, animation, params) -> {
                    if (entitypatch instanceof EnderDragonPatch enderdragonpatch) {
                        enderdragonpatch.getOriginal().getPhaseManager().getPhase(PatchedPhases.GROUND_BATTLE).fly();
                    }
                }, AnimationEvent.Side.SERVER)));

        SPIDER_ATTACK = builder.nextAccessor("spider/attack", (accessor) -> new AttackAnimation(0.15F, 0.31F, 0.31F, 0.36F, 0.44F, ColliderPreset.SPIDER, Armatures.SPIDER.get().head, accessor, Armatures.SPIDER));
        SPIDER_JUMP_ATTACK = builder.nextAccessor("spider/jump_attack", (accessor) ->
            new AttackAnimation(0.15F, 0.25F, 0.5F, 0.6F, 1.0F,  ColliderPreset.SPIDER, Armatures.SPIDER.get().head, accessor, Armatures.SPIDER)
                .addProperty(ActionAnimationProperty.MOVE_VERTICAL, true));
        SPIDER_HIT = builder.nextAccessor("spider/hit", (accessor) -> new HitAnimation(0.08F, accessor, Armatures.SPIDER));
        SPIDER_NEUTRALIZED = builder.nextAccessor("spider/neutralized", (accessor) -> new LongHitAnimation(0.08F, accessor, Armatures.SPIDER));
        SPIDER_DEATH = builder.nextAccessor("spider/death", (accessor) -> new LongHitAnimation(0.16F, accessor, Armatures.SPIDER));

        GOLEM_ATTACK1 = builder.nextAccessor("iron_golem/attack1", (accessor) ->
            new AttackAnimation(0.2F, 0.1F, 0.2F, 0.35F, 0.9F, ColliderPreset.HEAD, Armatures.IRON_GOLEM.get().head, accessor, Armatures.IRON_GOLEM)
                .addProperty(AttackPhaseProperty.STUN_TYPE, StunType.KNOCKDOWN));
        GOLEM_ATTACK2 = builder.nextAccessor("iron_golem/attack2", (accessor) ->
            new AttackAnimation(0.34F, 0.1F, 0.4F, 0.6F, 1.3F, ColliderPreset.GOLEM_SMASHDOWN, Armatures.IRON_GOLEM.get().LA4, accessor, Armatures.IRON_GOLEM)
                .addProperty(AttackPhaseProperty.SOURCE_TAG, Set.of(EpicFightDamageTypeTags.FINISHER)));
        GOLEM_ATTACK3 = builder.nextAccessor("iron_golem/attack3", (accessor) ->
            new AttackAnimation(0.16F, 0.4F, 0.4F, 0.5F, 0.9F, ColliderPreset.GOLEM_SWING_ARM, Armatures.IRON_GOLEM.get().RA4, accessor, Armatures.IRON_GOLEM)
                .addProperty(StaticAnimationProperty.POSE_MODIFIER, Animations.ReusableSources.COMBO_ATTACK_DIRECTION_MODIFIER));
        GOLEM_ATTACK4 = builder.nextAccessor("iron_golem/attack4", (accessor) ->
            new AttackAnimation(0.16F, 0.4F, 0.4F, 0.5F, 0.9F, ColliderPreset.GOLEM_SWING_ARM, Armatures.IRON_GOLEM.get().LA4, accessor, Armatures.IRON_GOLEM)
                .addProperty(StaticAnimationProperty.POSE_MODIFIER, Animations.ReusableSources.COMBO_ATTACK_DIRECTION_MODIFIER));
        GOLEM_DEATH = builder.nextAccessor("iron_golem/death", (accessor) -> new LongHitAnimation(0.11F, accessor, Armatures.IRON_GOLEM));

        VINDICATOR_SWING_AXE1 = builder.nextAccessor("illager/swing_axe1", (accessor) -> new AttackAnimation(0.2F, 0.2F, 0.3F, 0.4F, 0.9F, ColliderPreset.TOOLS, Armatures.BIPED.get().toolR, accessor, Armatures.BIPED));
        VINDICATOR_SWING_AXE2 = builder.nextAccessor("illager/swing_axe2", (accessor) -> new AttackAnimation(0.1F, 0.2F, 0.3F, 0.4F, 0.9F, ColliderPreset.TOOLS, Armatures.BIPED.get().toolR, accessor, Armatures.BIPED));
        VINDICATOR_SWING_AXE3 = builder.nextAccessor("illager/swing_axe3", (accessor) -> new AttackAnimation(0.1F, 0.15F, 0.45F, 0.55F, 1.05F, ColliderPreset.TOOLS, Armatures.BIPED.get().toolR, accessor, Armatures.BIPED));

        PIGLIN_DEATH = builder.nextAccessor("piglin/death", (accessor) -> new LongHitAnimation(0.16F, accessor, Armatures.PIGLIN));

        HOGLIN_DEATH = builder.nextAccessor("hoglin/death", (accessor) -> new LongHitAnimation(0.16F, accessor, Armatures.HOGLIN));
        HOGLIN_ATTACK = builder.nextAccessor("hoglin/attack", (accessor) -> new AttackAnimation(0.16F, 0.25F, 0.25F, 0.45F, 1.0F, ColliderPreset.GOLEM_SWING_ARM, Armatures.HOGLIN.get().head, accessor, Armatures.HOGLIN));

        RAVAGER_DEATH = builder.nextAccessor("ravager/death", (accessor) -> new LongHitAnimation(0.11F, accessor, Armatures.RAVAGER));
        RAVAGER_STUN = builder.nextAccessor("ravager/groggy", (accessor) ->
            new ActionAnimation(0.16F, accessor, Armatures.RAVAGER)
                .addProperty(ActionAnimationProperty.STOP_MOVEMENT, true));
        RAVAGER_ATTACK1 = builder.nextAccessor("ravager/attack1", (accessor) -> new AttackAnimation(0.16F, 0.2F, 0.4F, 0.5F, 0.55F, ColliderPreset.HEADBUTT_RAVAGER, Armatures.RAVAGER.get().head, accessor, Armatures.RAVAGER));
        RAVAGER_ATTACK2 = builder.nextAccessor("ravager/attack2", (accessor) -> new AttackAnimation(0.16F, 0.2F, 0.4F, 0.5F, 1.3F, ColliderPreset.HEADBUTT_RAVAGER, Armatures.RAVAGER.get().head, accessor, Armatures.RAVAGER));
        RAVAGER_ATTACK3 = builder.nextAccessor("ravager/attack3", (accessor) -> new AttackAnimation(0.16F, 0.0F, 1.1F, 1.16F, 1.6F, ColliderPreset.HEADBUTT_RAVAGER, Armatures.RAVAGER.get().head, accessor, Armatures.RAVAGER));

        VEX_HIT = builder.nextAccessor("vex/hit", (accessor) -> new HitAnimation(0.048F, accessor, Armatures.VEX));
        VEX_DEATH = builder.nextAccessor("vex/death", (accessor) -> new LongHitAnimation(0.16F, accessor, Armatures.VEX));
        VEX_CHARGE = builder.nextAccessor("vex/charge", (accessor) ->
            new AttackAnimation(0.11F, 0.3F, 0.3F, 0.5F, 1.5F, ColliderPreset.VEX_CHARGE, Armatures.VEX.get().rootJoint, accessor, Armatures.VEX)
                .addProperty(AttackPhaseProperty.SOURCE_LOCATION_PROVIDER, LivingEntityPatch::getLastAttackPosition)
                .addProperty(ActionAnimationProperty.COORD_SET_BEGIN, MoveCoordFunctions.VEX_TRACE)
                .addProperty(ActionAnimationProperty.COORD_SET_TICK, null)
                .addProperty(ActionAnimationProperty.COORD_GET, MoveCoordFunctions.WORLD_COORD)
                .addProperty(ActionAnimationProperty.MOVE_ON_LINK, false)
                .addProperty(ActionAnimationProperty.MOVE_VERTICAL, true)
                .addProperty(ActionAnimationProperty.REMOVE_DELTA_MOVEMENT, true)
                .addEvents(StaticAnimationProperty.ON_BEGIN_EVENTS, SimpleEvent.create((entitypatch, animation, params) -> entitypatch.setLastAttackPosition(), Side.SERVER))
                .newTimePair(0.0F, 1.5F)
                .addStateRemoveOld(EntityState.MOVEMENT_LOCKED, true)
                .addStateRemoveOld(EntityState.TURNING_LOCKED, true));

        VEX_NEUTRALIZED = builder.nextAccessor("vex/neutralized", (accessor) -> new LongHitAnimation(0.1F, accessor, Armatures.VEX));

        WITCH_DRINKING = builder.nextAccessor("witch/drink", (accessor) -> new StaticAnimation(0.16F, false, accessor, Armatures.BIPED).addProperty(StaticAnimationProperty.FIXED_HEAD_ROTATION, true));

        WITHER_SKELETON_ATTACK1 = builder.nextAccessor("wither_skeleton/sword_attack1", (accessor) ->
            new AttackAnimation(0.16F, 0.2F, 0.3F, 0.41F, 0.7F, ColliderPreset.SWORD, Armatures.BIPED.get().toolR, accessor, Armatures.BIPED)
                .addProperty(StaticAnimationProperty.POSE_MODIFIER, Animations.ReusableSources.COMBO_ATTACK_DIRECTION_MODIFIER));
        WITHER_SKELETON_ATTACK2 = builder.nextAccessor("wither_skeleton/sword_attack2", (accessor) ->
            new AttackAnimation(0.16F, 0.25F, 0.25F, 0.36F, 0.7F, ColliderPreset.SWORD, Armatures.BIPED.get().toolR, accessor, Armatures.BIPED)
                .addProperty(StaticAnimationProperty.POSE_MODIFIER, Animations.ReusableSources.COMBO_ATTACK_DIRECTION_MODIFIER));
        WITHER_SKELETON_ATTACK3 = builder.nextAccessor("wither_skeleton/sword_attack3", (accessor) ->
            new AttackAnimation(0.16F, 0.25F, 0.25F, 0.36F, 0.7F, ColliderPreset.SWORD, Armatures.BIPED.get().toolR, accessor, Armatures.BIPED)
                .addProperty(StaticAnimationProperty.POSE_MODIFIER, Animations.ReusableSources.COMBO_ATTACK_DIRECTION_MODIFIER));

        WITHER_CHARGE = builder.nextAccessor("wither/rush", (accessor) ->
            new AttackAnimation(0.35F, 0.35F, 0.35F, 0.66F, 2.05F, ColliderPreset.WITHER_CHARGE, Armatures.WITHER.get().rootJoint, accessor, Armatures.WITHER)
                .addProperty(AttackPhaseProperty.SWING_SOUND, EpicFightSounds.BIG_ENTITY_MOVE.get())
                .addProperty(AttackPhaseProperty.HIT_SOUND, EpicFightSounds.BLUNT_HIT_HARD.get())
                .addProperty(AttackPhaseProperty.SOURCE_LOCATION_PROVIDER, LivingEntityPatch::getLastAttackPosition)
                .addProperty(AttackPhaseProperty.STUN_TYPE, StunType.KNOCKDOWN)
                .addProperty(AttackPhaseProperty.MAX_STRIKES_MODIFIER, ValueModifier.adder(100))
                .addProperty(AttackPhaseProperty.DAMAGE_MODIFIER, ValueModifier.setter(15.0F))
                .addProperty(ActionAnimationProperty.MOVE_VERTICAL, true)
                .addProperty(ActionAnimationProperty.COORD_SET_BEGIN, (self, entitypatch, transformSheet) -> {
                    Entity target;

                    if (
                        !self.isLinkAnimation() && entitypatch instanceof WitherPatch witherpatch &&
                            (target = entitypatch.getOriginal().level().getEntity(witherpatch.getOriginal().getAlternativeTarget(0))) != null
                    ) {
                        TransformSheet transform = self.getTransfroms().get("Root").copyAll();
                        Keyframe[] keyframes = transform.getKeyframes();
                        int startFrame = 1;
                        int endFrame = 5;
                        Vec3f keyOrigin = keyframes[startFrame].transform().translation().multiply(1.0F, 1.0F, 0.0F);
                        Vec3f keyLast = keyframes[3].transform().translation();
                        Vec3 pos = entitypatch.getOriginal().getEyePosition();
                        Vec3 targetpos = target.position();
                        float horizontalDistance = (float)targetpos.subtract(pos).length();
                        float verticalDistance = (float)(targetpos.y - pos.y);
                        Vec3f prevPosition = Vec3f.sub(keyLast, keyOrigin, null);
                        Vec3f newPosition = new Vec3f(keyLast.x, verticalDistance, -horizontalDistance);
                        float scale = Math.min(newPosition.length() / prevPosition.length(), 5.0F);
                        Quaternionf rotator = Vec3f.getRotatorBetween(newPosition, keyLast, null);

                        for (int i = startFrame; i <= endFrame; i++) {
                            Vec3f translation = keyframes[i].transform().translation();
                            translation.z *= scale;
                            OpenMatrix4f.transform3v(OpenMatrix4f.fromQuaternion(rotator), translation, translation);
                        }

                        transformSheet.readFrom(transform);
                    } else {
                        transformSheet.readFrom(self.getTransfroms().get("Root").copyAll());
                    }
                }).addProperty(ActionAnimationProperty.COORD_SET_TICK, null)
                .addEvents(InTimeEvent.create(0.4F, (entitypatch, animation, params) -> {
                        if (entitypatch instanceof WitherPatch witherpatch) {
                            witherpatch.startCharging();
                        } else {
                            entitypatch.setLastAttackPosition();
                        }
                    }, Side.SERVER), InTimeEvent.create(0.4F, (entitypatch, animation, params) -> {
                        Entity entity = entitypatch.getOriginal();
                        entitypatch.getLevel().addParticle(EpicFightParticles.WHITE_AFTERIMAGE.get(), entity.getX(), entity.getY(), entity.getZ(), Double.longBitsToDouble(entity.getId()), 0, 0);
                    }, Side.CLIENT))
                .addEvents(StaticAnimationProperty.ON_BEGIN_EVENTS, SimpleEvent.create((entitypatch, animation, params) -> {
                        if (entitypatch instanceof WitherPatch witherpatch) {
                            if (!witherpatch.getOriginal().isPowered()) {
                                ((WitherPatch)entitypatch).setArmorActivated(true);
                            }
                        }
                    }, Side.CLIENT))
                .addEvents(StaticAnimationProperty.ON_END_EVENTS, SimpleEvent.create((entitypatch, animation, params) -> {
                        if (entitypatch instanceof WitherPatch witherpatch) {
                            if (!witherpatch.getOriginal().isPowered()) {
                                ((WitherPatch)entitypatch).setArmorActivated(false);
                            }
                        }
                    }, Side.CLIENT)
                ));

        WITHER_DEATH = builder.nextAccessor("wither/death", (accessor) -> new LongHitAnimation(0.16F, accessor, Armatures.WITHER));
        WITHER_NEUTRALIZED = builder.nextAccessor("wither/neutralized", (accessor) ->
            new LongHitAnimation(0.05F, accessor, Armatures.WITHER)
                .addProperty(ActionAnimationProperty.MOVE_VERTICAL, true)
                .addEvents(StaticAnimationProperty.ON_BEGIN_EVENTS,
                    SimpleEvent.create((entitypatch, animation, params) -> {
                        Entity entity = entitypatch.getOriginal();
                        entity.level().addParticle(EpicFightParticles.NEUTRALIZE.get(), entity.getX(), entity.getEyeY(), entity.getZ(), 3.0D, Double.longBitsToDouble(15), Double.NaN);
                    }, Side.CLIENT)
                ));

        WITHER_SPELL_ARMOR = builder.nextAccessor("wither/spell_wither_armor", (accessor) ->
            new InvincibleAnimation(0.35F, accessor, Armatures.WITHER)
                .addProperty(ActionAnimationProperty.MOVE_VERTICAL, false)
                .addEvents(StaticAnimationProperty.ON_BEGIN_EVENTS,
                    SimpleEvent.create((entitypatch, animation, params) -> {
                        entitypatch.playSound(EpicFightSounds.WITHER_SPELL_ARMOR.get(), 5.0F, 0.0F, 0.0F);
                        Entity entity = entitypatch.getOriginal();
                        entity.level().addParticle(EpicFightParticles.BOSS_CASTING.get(), entity.getX(), entity.getEyeY(), entity.getZ(), 5.0D, Double.longBitsToDouble(20), Double.longBitsToDouble(4));
                    }, Side.CLIENT))
                .addEvents(
                    InTimeEvent.create(0.5F, (entitypatch, animation, params) -> {
                        ((WitherPatch)entitypatch).setArmorActivated(true);
                    }, Side.SERVER))
                );

        WITHER_BLOCKED = builder.nextAccessor("wither/charging_blocked", (accessor) ->
            new ActionAnimation(0.05F, accessor, Armatures.WITHER)
                .addEvents(StaticAnimationProperty.ON_BEGIN_EVENTS,
                        SimpleEvent.create((entitypatch, animation, params) -> {
                        if (entitypatch instanceof WitherPatch witherpatch) {

                            if (!witherpatch.getOriginal().isPowered()) {
                                ((WitherPatch)entitypatch).setArmorActivated(true);
                            }
                        }
                    }, Side.SERVER)
                )
                .addEvents(StaticAnimationProperty.ON_END_EVENTS,
                        SimpleEvent.create((entitypatch, animation, params) -> {
                        if (entitypatch instanceof WitherPatch witherpatch) {
                            if (!witherpatch.getOriginal().isPowered()) {
                                ((WitherPatch)entitypatch).setArmorActivated(false);
                            }
                        }
                    }, Side.SERVER))
                );

        WITHER_GHOST_STANDBY = builder.nextAccessor("wither/ghost_stand", (accessor) -> new InvincibleAnimation(0.16F, accessor, Armatures.WITHER));

        WITHER_SWIRL = builder.nextAccessor("wither/swirl", (accessor) ->
            new AttackAnimation(0.2F, 0.05F, 0.4F, 0.51F, 1.6F, ColliderPreset.WITHER_CHARGE, Armatures.WITHER.get().torso, accessor, Armatures.WITHER)
                .addProperty(AttackPhaseProperty.SWING_SOUND, EpicFightSounds.WHOOSH_BIG.get())
                .addProperty(AttackPhaseProperty.MAX_STRIKES_MODIFIER, ValueModifier.setter(3))
                .addProperty(AttackPhaseProperty.DAMAGE_MODIFIER, ValueModifier.setter(6.0F)));

        WITHER_BEAM = builder.nextAccessor("wither/laser", (accessor) ->
            new ActionAnimation(0.05F, accessor, Armatures.WITHER)
                .addProperty(ActionAnimationProperty.MOVE_VERTICAL, false)
                .addEvents(
                    InTimeEvent.create(0.0F, (entitypatch, animation, params) -> {
                        entitypatch.playSound(EpicFightSounds.BUZZ.get(), 0.0F, 0.0F);

                        if (entitypatch instanceof WitherPatch witherpatch) {
                            for (int i = 0; i < 3; i++) {
                                Entity headTarget = witherpatch.getAlternativeTargetEntity(i);

                                if (headTarget == null) {
                                    headTarget = witherpatch.getAlternativeTargetEntity(0);
                                }

                                if (headTarget != null) {
                                    witherpatch.setLaserTarget(i, headTarget);
                                }
                            }
                        }
                    }, Side.SERVER),
                    InTimeEvent.create(0.7F, (entitypatch, animation, params) -> {
                        if (entitypatch instanceof WitherPatch witherpatch) {
                            for (int i = 0; i < 3; i++) {
                                Entity headTarget = witherpatch.getLaserTargetEntity(i);

                                if (headTarget != null) {
                                    Vec3 pos = headTarget.position().add(0.0D, headTarget.getBbHeight() * 0.5D, 0.0D);
                                    witherpatch.setLaserTargetPosition(i, pos);
                                    witherpatch.setLaserTarget(i, null);
                                }
                            }
                        }
                    }, Side.SERVER),
                    InTimeEvent.create(0.9F, (entitypatch, animation, params) -> {
                        if (entitypatch instanceof WitherPatch witherpatch) {
                            WitherBoss witherboss = witherpatch.getOriginal();
                            MixinWitherBossAccessor witherBossAccessor = witherpatch.getOriginalAsMixinAccessor();
                            witherboss.level().playLocalSound(witherboss.getX(), witherboss.getY(), witherboss.getZ(), EpicFightSounds.LASER_BLAST.get(), SoundSource.HOSTILE, 1.0F, 1.0F, false);

                            for (int i = 0; i < 3; i++) {
                                Vec3 laserDestination = witherpatch.getLaserTargetPosition(i);
                                Entity headTarget = witherpatch.getAlternativeTargetEntity(i);

                                if (headTarget != null) {
                                    witherpatch.getLevel().addAlwaysVisibleParticle(EpicFightParticles.LASER.get(), witherBossAccessor.invoke_getHeadX(i), witherBossAccessor.invoke_getHeadY(i), witherBossAccessor.invoke_getHeadZ(i), laserDestination.x, laserDestination.y, laserDestination.z);
                                }
                            }
                        }
                    }, Side.CLIENT),
                    InTimeEvent.create(0.9F, (entitypatch, animation, params) -> {
                        if (entitypatch instanceof WitherPatch witherpatch) {
                            WitherBoss witherboss = witherpatch.getOriginal();
                            MixinWitherBossAccessor witherBossAccessor = witherpatch.getOriginalAsMixinAccessor();
                            List<Entity> hurted = Lists.newArrayList();

                            for (int i = 0; i < 3; i++) {
                                Vec3 laserDestination = witherpatch.getLaserTargetPosition(i);
                                Entity headTarget = witherpatch.getAlternativeTargetEntity(i);

                                if (headTarget != null) {
                                    double x = witherBossAccessor.invoke_getHeadX(i);
                                    double y = witherBossAccessor.invoke_getHeadY(i);
                                    double z = witherBossAccessor.invoke_getHeadZ(i);
                                    Vec3 direction = laserDestination.subtract(x, y, z);
                                    Vec3 start = new Vec3(x, y, z);
                                    Vec3 destination = start.add(direction.normalize().scale(200.0D));
                                    BlockHitResult hitResult = witherboss.level().clip(new ClipContext(start, destination, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()));
                                    Vec3 hitLocation = hitResult.getLocation();
                                    double xLength = hitLocation.x - x;
                                    double yLength = hitLocation.y - y;
                                    double zLength = hitLocation.z - z;
                                    double horizontalDistance = Math.sqrt(xLength * xLength + zLength * zLength);
                                    double length = Math.sqrt(xLength * xLength + yLength * yLength + zLength * zLength);
                                    float yRot = (float)(-Math.atan2(zLength, xLength) * (180D / Math.PI)) - 90.0F;
                                    float xRot = (float)(Math.atan2(yLength, horizontalDistance) * (180D / Math.PI));
                                    OBBCollider collider = new OBBCollider(0.25D, 0.25D, length * 0.5D, 0.0D, 0.0D, length * 0.5D);
                                    collider.transform(OpenMatrix4f.createTranslation((float)-x, (float)y, (float)-z).rotateDeg(yRot, Vec3f.Y_AXIS).rotateDeg(-xRot, Vec3f.X_AXIS));
                                    List<Entity> hitEntities = collider.getCollideEntities(witherboss);

                                    EpicFightDamageSource damagesource = EpicFightDamageSources.witherBeam(witherboss).setAnimation(WITHER_BEAM);

                                    hitEntities.forEach((entity) -> {
                                        if (!hurted.contains(entity)) {
                                            hurted.add(entity);
                                            entity.hurt(damagesource, 12.0F);
                                        }
                                    });

                                    Level.ExplosionInteraction explosion$blockinteraction = witherboss.level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING) ? Level.ExplosionInteraction.BLOCK : Level.ExplosionInteraction.NONE;
                                    witherboss.level().explode(witherboss, hitLocation.x, hitLocation.y, hitLocation.z, 0.0F, false, explosion$blockinteraction);
                                }
                            }
                        }
                    }, Side.SERVER),
                    InTimeEvent.create(2.3F, (entitypatch, animation, params) -> {
                        if (entitypatch instanceof WitherPatch witherpatch) {
                            for (int i = 0; i < 3; i++) {
                                witherpatch.setLaserTargetPosition(i, new Vec3(Double.NaN, Double.NaN, Double.NaN));
                            }
                        }
                    }, Side.SERVER)
                )
            );

        WITHER_BACKFLIP = builder.nextAccessor("wither/backflip", (accessor) ->
            new AttackAnimation(0.2F, 0.3F, 0.5F, 0.66F, 2.1F, ColliderPreset.WITHER_CHARGE, Armatures.WITHER.get().torso, accessor, Armatures.WITHER)
                .addProperty(AttackPhaseProperty.SWING_SOUND, EpicFightSounds.BIG_ENTITY_MOVE.get())
                .addProperty(AttackPhaseProperty.HIT_SOUND, EpicFightSounds.BLUNT_HIT_HARD.get())
                .addProperty(AttackPhaseProperty.MAX_STRIKES_MODIFIER, ValueModifier.adder(100))
                .addProperty(AttackPhaseProperty.DAMAGE_MODIFIER, ValueModifier.setter(10.0F))
                .addProperty(AttackPhaseProperty.STUN_TYPE, StunType.KNOCKDOWN)
                .addProperty(ActionAnimationProperty.COORD_SET_BEGIN, MoveCoordFunctions.RAW_COORD)
                .addProperty(ActionAnimationProperty.COORD_SET_TICK, null));

        ZOMBIE_ATTACK1 = builder.nextAccessor("zombie/attack1", (accessor) ->
            new AttackAnimation(0.1F, 0.3F, 0.4F, 0.6F, 0.85F, ColliderPreset.FIST, Armatures.BIPED.get().toolR, accessor, Armatures.BIPED)
                .addProperty(StaticAnimationProperty.POSE_MODIFIER, Animations.ReusableSources.COMBO_ATTACK_DIRECTION_MODIFIER));
        ZOMBIE_ATTACK2 = builder.nextAccessor("zombie/attack2", (accessor) ->
            new AttackAnimation(0.1F, 0.3F, 0.4F, 0.6F, 0.85F, ColliderPreset.FIST, Armatures.BIPED.get().toolL, accessor, Armatures.BIPED)
                .addProperty(StaticAnimationProperty.POSE_MODIFIER, Animations.ReusableSources.COMBO_ATTACK_DIRECTION_MODIFIER));
        ZOMBIE_ATTACK3 = builder.nextAccessor("zombie/attack3", (accessor) ->
            new AttackAnimation(0.1F, 0.5F, 0.5F, 0.6F, 1.15F, ColliderPreset.HEAD, Armatures.BIPED.get().head, accessor, Armatures.BIPED));

        SWEEPING_EDGE = builder.nextAccessor("biped/skill/sweeping_edge", (accessor) ->
            new AttackAnimation(0.1F, 0.0F, 0.15F, 0.3F, 0.8F, null, Armatures.BIPED.get().toolR, accessor, Armatures.BIPED)
                .addProperty(AttackAnimationProperty.BASIS_ATTACK_SPEED, 1.6F)
                .addProperty(AttackAnimationProperty.EXTRA_COLLIDERS, 1)
                .addProperty(StaticAnimationProperty.POSE_MODIFIER, Animations.ReusableSources.COMBO_ATTACK_DIRECTION_MODIFIER));

        DANCING_EDGE = builder.nextAccessor("biped/skill/dancing_edge", (accessor) ->
            new AttackAnimation(0.1F, accessor, Armatures.BIPED,
                    new Phase(0.0F, 0.25F, 0.4F, 0.4F, 0.4F, Armatures.BIPED.get().toolR, null),
                    new Phase(0.4F, 0.4F, 0.5F, 0.55F, 0.6F, InteractionHand.OFF_HAND, Armatures.BIPED.get().toolL, null),
                    new Phase(0.6F, 0.6F, 0.7F, 1.15F, Float.MAX_VALUE, Armatures.BIPED.get().toolR, null))
                .addProperty(AttackAnimationProperty.BASIS_ATTACK_SPEED, 1.6F)
                .addProperty(ActionAnimationProperty.MOVE_VERTICAL, true));

        THE_GUILLOTINE = builder.nextAccessor("biped/skill/the_guillotine", (accessor) ->
            new AttackAnimation(0.15F, 0.2F, 0.7F, 0.75F, 1.1F, null, Armatures.BIPED.get().toolR, accessor, Armatures.BIPED)
                .addProperty(ActionAnimationProperty.MOVE_VERTICAL, true)
                .addProperty(StaticAnimationProperty.PLAY_SPEED_MODIFIER, Animations.ReusableSources.CONSTANT_ONE));

        HEARTPIERCER = builder.nextAccessor("biped/skill/heartpiercer", (accessor) ->
            new AttackAnimation(0.11F, accessor, Armatures.BIPED,
                    new Phase(0.0F, 0.3F, 0.36F, 0.5F, 0.5F, Armatures.BIPED.get().toolR, null),
                    new Phase(0.5F, 0.5F, 0.56F, 0.75F, 0.75F, Armatures.BIPED.get().toolR, null),
                    new Phase(0.75F, 0.75F, 0.81F, 1.05F, Float.MAX_VALUE, Armatures.BIPED.get().toolR, null))
                .addProperty(AttackAnimationProperty.BASIS_ATTACK_SPEED, 1.2F)
                .addProperty(StaticAnimationProperty.POSE_MODIFIER, Animations.ReusableSources.COMBO_ATTACK_DIRECTION_MODIFIER));

        GRASPING_SPIRAL_FIRST = builder.nextAccessor("biped/skill/grasping_spire_first", (accessor) ->
            new AttackAnimation(0.1F, 0.25F, 0.3F, 0.4F, 0.8F, null, Armatures.BIPED.get().toolR, accessor, Armatures.BIPED)
                .addProperty(AttackAnimationProperty.BASIS_ATTACK_SPEED, 1.2F)
                .addProperty(StaticAnimationProperty.POSE_MODIFIER, Animations.ReusableSources.COMBO_ATTACK_DIRECTION_MODIFIER)
                .setResourceLocation(EpicFight.MODID, "biped/combat/spear_dash")
                .addEvents(StaticAnimationProperty.ON_END_EVENTS,
                    SimpleEvent.create((entitypatch, animation, params) -> {
                        List<LivingEntity> hitEnemies = entitypatch.getCurrentlyActuallyHitEntities();
                        Vec3 vec = entitypatch.getOriginal().position().add(Vec3.directionFromRotation(new Vec2(0.0F, entitypatch.getOriginal().getYRot())));

                        if (animation.get() instanceof AttackAnimation attackAnimation) {
                            for (LivingEntity e : hitEnemies) {
                                if (e.isAlive()) {
                                    LivingEntityPatch<?> targetpatch = EpicFightCapabilities.getEntityPatch(e, LivingEntityPatch.class);

                                    if (targetpatch != null) {
                                        DamageSource dmgSource = attackAnimation.getEpicFightDamageSource(entitypatch, e, attackAnimation.phases[0]);

                                        if (!targetpatch.tryHurt(dmgSource, 0).resultType.dealtDamage()) {
                                            continue;
                                        }
                                    }

                                    Vec3 toAttacker = e.position().subtract(vec).multiply(0.3F, 0.3F, 0.3F);
                                    e.setPos(vec.add(toAttacker));
                                }
                            }
                        }
                    }, AnimationEvent.Side.SERVER))
                .addEvents(
                    InTimeEvent.create(0.75F, (entitypatch, animation, params) -> {
                        if (entitypatch.isLastAttackSuccess()) {
                            entitypatch.playAnimationSynchronized(GRASPING_SPIRAL_SECOND, 0.0F);
                        }
                    }, AnimationEvent.Side.SERVER)
                ));

        GRASPING_SPIRAL_SECOND = builder.nextAccessor("biped/skill/grasping_spire_second", (accessor) ->
            new AttackAnimation(0.1F, 0.0F, 0.5F, 0.6F, 0.95F, null, Armatures.BIPED.get().toolR, accessor, Armatures.BIPED)
                .addProperty(AttackAnimationProperty.BASIS_ATTACK_SPEED, 1.2F)
                .addProperty(StaticAnimationProperty.POSE_MODIFIER, Animations.ReusableSources.COMBO_ATTACK_DIRECTION_MODIFIER));

        STEEL_WHIRLWIND = builder.nextAccessor("biped/skill/steel_whirlwind", (accessor) ->
            new AttackAnimation(0.15F, accessor, Armatures.BIPED,
                new Phase(0.0F, 0.0F, 0.0F, 0.2F, 0.45F, 0.45F, Armatures.BIPED.get().rootJoint, ColliderPreset.STEEL_WHIRLWIND), new Phase(0.45F, 0.45F, 0.45F, 0.65F, 1.0F, 1.0F, Armatures.BIPED.get().rootJoint, ColliderPreset.STEEL_WHIRLWIND),
                new Phase(1.0F, 1.0F, 1.0F, 1.2F, 2.55F, Float.MAX_VALUE, Armatures.BIPED.get().rootJoint, ColliderPreset.STEEL_WHIRLWIND))
                .addProperty(AttackAnimationProperty.BASIS_ATTACK_SPEED, 1.0F)
                .addProperty(AttackAnimationProperty.EXTRA_COLLIDERS, 4)
                .addProperty(ActionAnimationProperty.COORD_SET_BEGIN, (animation, entitypatch, transformSheet) -> {
                    if (!animation.isLinkAnimation()) {
                        int chargingPower = entitypatch.getAnimator().getVariables().get(EpicFightSynchedAnimationVariableKeys.CHARGING_TICKS.get(), animation.getRealAnimation()).orElse(0);
                        transformSheet.readFrom(animation.getCoord().copyAll().extendsZCoord(0.6666F + chargingPower / 5.0F, 0, 2));
                    } else {
                        MoveCoordFunctions.RAW_COORD.set(animation, entitypatch, transformSheet);
                    }
                })
                .addProperty(ActionAnimationProperty.COORD_SET_TICK, null)
                .addProperty(ActionAnimationProperty.MOVE_VERTICAL, false)
                .addProperty(StaticAnimationProperty.POSE_MODIFIER, Animations.ReusableSources.COMBO_ATTACK_DIRECTION_MODIFIER)
                .addProperty(StaticAnimationProperty.PLAY_SPEED_MODIFIER, (self, entitypatch, speed, prevElapsedTime, elapsedTime) -> {
                    if (elapsedTime < 1.05F) {
                        int chargingPower = entitypatch.getAnimator().getVariables().get(EpicFightSynchedAnimationVariableKeys.CHARGING_TICKS.get(), self.getRealAnimation()).orElse(0);
                        return 0.6666F + chargingPower / 20.0F;
                    }

                    return 1.0F;
                })
                .newTimePair(0.0F, 2.55F)
                    .addStateRemoveOld(EntityState.SKILL_EXECUTABLE, false));

        BATTOJUTSU = builder.nextAccessor("biped/skill/battojutsu", (accessor) ->
            new AttackAnimation(0.15F, 0.0F, 0.75F, 0.8F, 1.2F, ColliderPreset.BATTOJUTSU, Armatures.BIPED.get().rootJoint, accessor, Armatures.BIPED)
                .addProperty(AttackPhaseProperty.SWING_SOUND, EpicFightSounds.WHOOSH_SHARP.get())
                .addProperty(StaticAnimationProperty.PLAY_SPEED_MODIFIER, Animations.ReusableSources.CONSTANT_ONE)
                .addEvents(InTimeEvent.create(0.05F, ReusableSources.PLAY_SOUND, AnimationEvent.Side.SERVER).params(EpicFightSounds.SWORD_IN.get())));

        BATTOJUTSU_DASH = builder.nextAccessor("biped/skill/battojutsu_dash", (accessor) ->
            new AttackAnimation(0.15F, 0.43F, 0.7F, 0.8F, 1.4F, ColliderPreset.BATTOJUTSU_DASH, Armatures.BIPED.get().rootJoint, accessor, Armatures.BIPED)
                .addProperty(AttackPhaseProperty.SWING_SOUND, EpicFightSounds.WHOOSH_SHARP.get())
                .addProperty(ActionAnimationProperty.COORD_SET_BEGIN, MoveCoordFunctions.RAW_COORD)
                .addProperty(ActionAnimationProperty.COORD_SET_TICK, null)
                .addProperty(StaticAnimationProperty.PLAY_SPEED_MODIFIER, Animations.ReusableSources.CONSTANT_ONE)
                .addEvents(
                    InTimeEvent.create(0.05F, ReusableSources.PLAY_SOUND, AnimationEvent.Side.SERVER).params(EpicFightSounds.SWORD_IN.get()),
                    InTimeEvent.create(0.65F, (entitypatch, animation, params) -> {
                        LivingEntity entity = entitypatch.getOriginal();
                        entity.level().addParticle(EpicFightParticles.WHITE_AFTERIMAGE.get(), entity.getX(), entity.getY(), entity.getZ(), Double.longBitsToDouble(entity.getId()), 0, 0);
                        RandomSource random = entity.getRandom();
                        double x = entity.getX() + (random.nextDouble() - random.nextDouble()) * 2.0D;
                        double y = entity.getY();
                        double z = entity.getZ() + (random.nextDouble() - random.nextDouble()) * 2.0D;
                        entity.level().addParticle(ParticleTypes.EXPLOSION, x, y, z, random.nextDouble() * 0.005D, 0.0D, 0.0D);
                    }, Side.CLIENT)
                ));

        RUSHING_TEMPO1 = builder.nextAccessor("biped/skill/rushing_tempo1", (accessor) ->
            new AttackAnimation(0.05F, 0.0F, 0.15F, 0.25F, 0.6F, null, Armatures.BIPED.get().toolR, accessor, Armatures.BIPED)
                .addProperty(AttackAnimationProperty.BASIS_ATTACK_SPEED, 1.6F)
                .addProperty(AttackAnimationProperty.EXTRA_COLLIDERS, 2)
                .addProperty(ActionAnimationProperty.RESET_PLAYER_COMBO_COUNTER, false)
                .newTimePair(0.0F, 0.25F)
                    .addStateRemoveOld(EntityState.COMBO_ATTACKS_DOABLE, false));

        RUSHING_TEMPO2 = builder.nextAccessor("biped/skill/rushing_tempo2", (accessor) ->
            new AttackAnimation(0.05F, 0.0F, 0.15F, 0.25F, 0.6F, null, Armatures.BIPED.get().toolR, accessor, Armatures.BIPED)
                .addProperty(AttackAnimationProperty.BASIS_ATTACK_SPEED, 1.6F)
                .addProperty(AttackAnimationProperty.EXTRA_COLLIDERS, 2)
                .addProperty(ActionAnimationProperty.RESET_PLAYER_COMBO_COUNTER, false)
                .newTimePair(0.0F, 0.25F)
                    .addStateRemoveOld(EntityState.COMBO_ATTACKS_DOABLE, false));

        RUSHING_TEMPO3 = builder.nextAccessor("biped/skill/rushing_tempo3", (accessor) ->
            new AttackAnimation(0.05F, 0.0F, 0.2F, 0.25F, 0.6F, null, Armatures.BIPED.get().toolR, accessor, Armatures.BIPED)
                .addProperty(AttackAnimationProperty.BASIS_ATTACK_SPEED, 1.6F)
                .addProperty(AttackAnimationProperty.EXTRA_COLLIDERS, 2)
                .addProperty(ActionAnimationProperty.RESET_PLAYER_COMBO_COUNTER, false)
                .newTimePair(0.0F, 0.25F)
                    .addStateRemoveOld(EntityState.COMBO_ATTACKS_DOABLE, false));

        RELENTLESS_COMBO = builder.nextAccessor("biped/skill/relentless_combo", (accessor) ->
            new AttackAnimation(0.05F, accessor, Armatures.BIPED,
                    new Phase(0.0F, 0.016F, 0.066F, 0.133F, 0.133F, InteractionHand.OFF_HAND, Armatures.BIPED.get().rootJoint, ColliderPreset.FIST_FIXED),
                    new Phase(0.133F, 0.133F, 0.183F, 0.25F, 0.25F, Armatures.BIPED.get().rootJoint, ColliderPreset.FIST_FIXED),
                    new Phase(0.25F, 0.25F, 0.3F, 0.366F, 0.366F, InteractionHand.OFF_HAND, Armatures.BIPED.get().rootJoint, ColliderPreset.FIST_FIXED),
                    new Phase(0.366F, 0.366F, 0.416F, 0.483F, 0.483F, Armatures.BIPED.get().rootJoint, ColliderPreset.FIST_FIXED),
                    new Phase(0.483F, 0.483F, 0.533F, 0.6F, 0.6F, InteractionHand.OFF_HAND, Armatures.BIPED.get().rootJoint, ColliderPreset.FIST_FIXED),
                    new Phase(0.6F, 0.6F, 0.65F, 0.716F, 0.716F, Armatures.BIPED.get().rootJoint, ColliderPreset.FIST_FIXED),
                    new Phase(0.716F, 0.716F, 0.766F, 0.833F, 0.833F, InteractionHand.OFF_HAND, Armatures.BIPED.get().rootJoint, ColliderPreset.FIST_FIXED),
                    new Phase(0.833F, 0.833F, 0.883F, 1.1F, 1.1F, Armatures.BIPED.get().rootJoint, ColliderPreset.FIST_FIXED))
                .addProperty(AttackAnimationProperty.BASIS_ATTACK_SPEED, 4.0F));

        EVISCERATE_FIRST = builder.nextAccessor("biped/skill/eviscerate_first", (accessor) ->
            new AttackAnimation(0.08F, 0.0F, 0.05F, 0.15F, 0.45F, null, Armatures.BIPED.get().toolR, accessor, Armatures.BIPED)
                .addProperty(AttackAnimationProperty.BASIS_ATTACK_SPEED, 2.4F)
                .addProperty(ActionAnimationProperty.COORD_SET_BEGIN, null)
                .addProperty(ActionAnimationProperty.COORD_SET_TICK, MoveCoordFunctions.TRACE_TARGET_LOCATION_ROTATION)
                .addProperty(ActionAnimationProperty.ENTITY_YROT_PROVIDER, MoveCoordFunctions.LOOK_DEST));

        EVISCERATE_SECOND = builder.nextAccessor("biped/skill/eviscerate_second", (accessor) ->
            new AttackAnimation(0.15F, 0.0F, 0.04F, 0.05F, 0.4F, null, Armatures.BIPED.get().toolR, accessor, Armatures.BIPED)
                .addProperty(AttackPhaseProperty.HIT_SOUND, EpicFightSounds.EVISCERATE.get())
                .addProperty(AttackPhaseProperty.PARTICLE, EpicFightParticles.EVISCERATE)
                .addProperty(AttackAnimationProperty.BASIS_ATTACK_SPEED, 2.4F));

        BLADE_RUSH_COMBO1 = builder.nextAccessor("biped/skill/blade_rush_combo1", (accessor) ->
            new AttackAnimation(0.1F, 0.0F, 0.15F, 0.35F, 0.85F, ColliderPreset.BIPED_BODY_COLLIDER, Armatures.BIPED.get().rootJoint, accessor, Armatures.BIPED)
                .addProperty(AttackPhaseProperty.HIT_PRIORITY, Priority.TARGET)
                .addProperty(AttackAnimationProperty.ATTACK_SPEED_FACTOR, 0.0F)
                .addProperty(ActionAnimationProperty.MOVE_ON_LINK, false)
                .addProperty(ActionAnimationProperty.NO_GRAVITY_TIME, TimePairList.create(0.0F, 0.35F))
                .addProperty(ActionAnimationProperty.DEST_LOCATION_PROVIDER, MoveCoordFunctions.SYNCHED_TARGET_ENTITY_LOCATION_VARIABLE)
                .addProperty(ActionAnimationProperty.COORD_UPDATE_TIME, TimePairList.create(0.0F, 0.25F))
                .addProperty(ActionAnimationProperty.COORD_SET_BEGIN, null)
                .addProperty(ActionAnimationProperty.COORD_SET_TICK, MoveCoordFunctions.TRACE_ORIGIN_AS_DESTINATION)
                .addProperty(ActionAnimationProperty.COORD_GET, MoveCoordFunctions.WORLD_COORD)
                .addProperty(ActionAnimationProperty.COORD_START_KEYFRAME_INDEX, 1)
                .addProperty(ActionAnimationProperty.COORD_DEST_KEYFRAME_INDEX, 4)
                .addProperty(ActionAnimationProperty.ENTITY_YROT_PROVIDER, MoveCoordFunctions.LOOK_DEST)
                .addProperty(StaticAnimationProperty.PLAY_SPEED_MODIFIER, Animations.ReusableSources.CONSTANT_ONE)
                .newTimePair(0.0F, 0.65F)
                    .addStateRemoveOld(EntityState.SKILL_EXECUTABLE, false));

        BLADE_RUSH_COMBO2 = builder.nextAccessor("biped/skill/blade_rush_combo2", (accessor) ->
            new AttackAnimation(0.1F, 0.0F, 0.15F, 0.35F, 0.85F, ColliderPreset.BIPED_BODY_COLLIDER, Armatures.BIPED.get().rootJoint, accessor, Armatures.BIPED)
                .addProperty(AttackPhaseProperty.HIT_PRIORITY, Priority.TARGET)
                .addProperty(AttackAnimationProperty.ATTACK_SPEED_FACTOR, 0.0F)
                .addProperty(ActionAnimationProperty.MOVE_ON_LINK, false)
                .addProperty(ActionAnimationProperty.NO_GRAVITY_TIME, TimePairList.create(0.0F, 0.35F))
                .addProperty(ActionAnimationProperty.DEST_LOCATION_PROVIDER, MoveCoordFunctions.SYNCHED_TARGET_ENTITY_LOCATION_VARIABLE)
                .addProperty(ActionAnimationProperty.COORD_UPDATE_TIME, TimePairList.create(0.0F, 0.3F))
                .addProperty(ActionAnimationProperty.COORD_SET_BEGIN, null)
                .addProperty(ActionAnimationProperty.COORD_SET_TICK, MoveCoordFunctions.TRACE_ORIGIN_AS_DESTINATION)
                .addProperty(ActionAnimationProperty.COORD_GET, MoveCoordFunctions.WORLD_COORD)
                .addProperty(ActionAnimationProperty.COORD_START_KEYFRAME_INDEX, 1)
                .addProperty(ActionAnimationProperty.COORD_DEST_KEYFRAME_INDEX, 2)
                .addProperty(ActionAnimationProperty.ENTITY_YROT_PROVIDER, MoveCoordFunctions.LOOK_DEST)
                .addProperty(StaticAnimationProperty.PLAY_SPEED_MODIFIER, Animations.ReusableSources.CONSTANT_ONE)
                .newTimePair(0.0F, 0.65F)
                    .addStateRemoveOld(EntityState.SKILL_EXECUTABLE, false));

        BLADE_RUSH_COMBO3 = builder.nextAccessor("biped/skill/blade_rush_combo3", (accessor) ->
            new AttackAnimation(0.1F, 0.0F, 0.2F, 0.35F, 0.85F, ColliderPreset.BIPED_BODY_COLLIDER, Armatures.BIPED.get().rootJoint, accessor, Armatures.BIPED)
                .addProperty(AttackPhaseProperty.HIT_PRIORITY, Priority.TARGET)
                .addProperty(AttackAnimationProperty.ATTACK_SPEED_FACTOR, 0.0F)
                .addProperty(ActionAnimationProperty.MOVE_ON_LINK, false)
                .addProperty(ActionAnimationProperty.NO_GRAVITY_TIME, TimePairList.create(0.0F, 0.35F))
                .addProperty(ActionAnimationProperty.DEST_LOCATION_PROVIDER, MoveCoordFunctions.SYNCHED_TARGET_ENTITY_LOCATION_VARIABLE)
                .addProperty(ActionAnimationProperty.COORD_UPDATE_TIME, TimePairList.create(0.0F, 0.25F))
                .addProperty(ActionAnimationProperty.COORD_SET_BEGIN, null)
                .addProperty(ActionAnimationProperty.COORD_SET_TICK, MoveCoordFunctions.TRACE_ORIGIN_AS_DESTINATION)
                .addProperty(ActionAnimationProperty.COORD_GET, MoveCoordFunctions.WORLD_COORD)
                .addProperty(ActionAnimationProperty.COORD_START_KEYFRAME_INDEX, 1)
                .addProperty(ActionAnimationProperty.COORD_DEST_KEYFRAME_INDEX, 4)
                .addProperty(ActionAnimationProperty.ENTITY_YROT_PROVIDER, MoveCoordFunctions.LOOK_DEST)
                .addProperty(StaticAnimationProperty.PLAY_SPEED_MODIFIER, Animations.ReusableSources.CONSTANT_ONE)
                .newTimePair(0.0F, 0.6F)
                    .addStateRemoveOld(EntityState.SKILL_EXECUTABLE, false));

        BLADE_RUSH_HIT = builder.nextAccessor("biped/interact/blade_rush_hit", (accessor) ->
            new LongHitAnimation(0.1F, accessor, Armatures.BIPED)
                .addProperty(ActionAnimationProperty.IS_DEATH_ANIMATION, true));

        BLADE_RUSH_EXECUTE_BIPED = builder.nextAccessor("biped/skill/blade_rush_execute", (accessor) ->
            new GrapplingAttackAnimation(0.5F, 1.5F, accessor, Armatures.BIPED)
                .addProperty(AttackPhaseProperty.SOURCE_TAG, Set.of(EpicFightDamageTypeTags.EXECUTION, DamageTypeTags.BYPASSES_ARMOR))
                .addProperty(ActionAnimationProperty.COORD_UPDATE_TIME, TimePairList.create(0.0F, 0.5F))
                .addProperty(ActionAnimationProperty.NO_GRAVITY_TIME, TimePairList.create(0.0F, 0.95F))
                .addEvents(
                    InTimeEvent.create(0.1F, (entitypatch, animation, params) -> {
                        LivingEntity grapplingTarget = entitypatch.getGrapplingTarget();

                        if (grapplingTarget != null) {
                            entitypatch.playSound(EpicFightSounds.BLADE_HIT.get(), 0.0F, 0.0F);
                        }
                    }, Side.CLIENT),
                    InTimeEvent.create(0.3F, (entitypatch, animation, params) -> {
                        LivingEntity grapplingTarget = entitypatch.getGrapplingTarget();

                        if (grapplingTarget != null) {
                            entitypatch.playSound(EpicFightSounds.BLADE_HIT.get(), 0.0F, 0.0F);
                        }
                    }, Side.CLIENT)
                ));

        BLADE_RUSH_FAILED = builder.nextAccessor("biped/skill/blade_rush_failed", (accessor) ->
            new ActionAnimation(0.0F, 0.85F, accessor, Armatures.BIPED)
                .addProperty(ActionAnimationProperty.MOVE_VERTICAL, true)
                .addProperty(ActionAnimationProperty.NO_GRAVITY_TIME, TimePairList.create(0.0F, 0.0F)));

        BLADE_RUSH_TRY = builder.nextAccessor("biped/skill/blade_rush_try", (accessor) ->
            new GrapplingTryAnimation(0.1F, 0.0F, 0.4F, 0.4F, 0.45F, ColliderPreset.BIPED_BODY_COLLIDER, Armatures.BIPED.get().rootJoint, accessor, BLADE_RUSH_HIT, BLADE_RUSH_EXECUTE_BIPED, BLADE_RUSH_FAILED, Armatures.BIPED)
                .addProperty(ActionAnimationProperty.COORD_START_KEYFRAME_INDEX, 1)
                .addProperty(ActionAnimationProperty.NO_GRAVITY_TIME, TimePairList.create(0.15F, 0.35F))
                .addProperty(ActionAnimationProperty.DEST_LOCATION_PROVIDER, MoveCoordFunctions.SYNCHED_TARGET_ENTITY_LOCATION_VARIABLE));

        WRATHFUL_LIGHTING = builder.nextAccessor("biped/skill/wrathful_lighting", (accessor) ->
            new AttackAnimation(0.15F, accessor, Armatures.BIPED,
                    new Phase(0.0F, 0.0F, 0.3F, 0.36F, 1.0F, Float.MAX_VALUE, Armatures.BIPED.get().toolR, null),
                    new Phase(InteractionHand.MAIN_HAND, Armatures.BIPED.get().rootJoint, null))
                .addProperty(StaticAnimationProperty.PLAY_SPEED_MODIFIER, Animations.ReusableSources.CONSTANT_ONE)
                .addEvents(InTimeEvent.create(0.35F, ReusableSources.SUMMON_THUNDER, AnimationEvent.Side.SERVER)));

        TSUNAMI = builder.nextAccessor("biped/skill/tsunami", (accessor) ->
            new AttackAnimation(0.2F, 0.2F, 0.35F, 1.0F, 1.8F, ColliderPreset.BIPED_BODY_COLLIDER, Armatures.BIPED.get().rootJoint, accessor, Armatures.BIPED)
                .addProperty(AttackPhaseProperty.MAX_STRIKES_MODIFIER, ValueModifier.adder(10))
                .addProperty(ActionAnimationProperty.COORD_SET_BEGIN, MoveCoordFunctions.RAW_COORD)
                .addProperty(ActionAnimationProperty.COORD_SET_TICK, null)
                .addProperty(ActionAnimationProperty.MOVE_VERTICAL, true)
                .addProperty(ActionAnimationProperty.NO_GRAVITY_TIME, TimePairList.create(0.2F, 1.1F))
                .addProperty(StaticAnimationProperty.PLAY_SPEED_MODIFIER, Animations.ReusableSources.CONSTANT_ONE)
                .addEvents(StaticAnimationProperty.ON_END_EVENTS, SimpleEvent.create(Animations.ReusableSources.RESTORE_BOUNDING_BOX, AnimationEvent.Side.BOTH))
                .addEvents(StaticAnimationProperty.TICK_EVENTS, SimpleEvent.create(Animations.ReusableSources.RESIZE_BOUNDING_BOX, AnimationEvent.Side.BOTH).params(EntityDimensions.scalable(0.6F, 1.0F)))
                .addEvents(InPeriodEvent.create(0.35F, 1.0F, (entitypatch, animation, params) -> {
                    Vec3 pos = entitypatch.getOriginal().position();

                    for (int x = -1; x <= 1; x += 2) {
                        for (int z = -1; z <= 1; z += 2) {
                            Vec3 rand = new Vec3(Math.random() * x, Math.random(), Math.random() * z).normalize().scale(2.0D);
                            entitypatch.getLevel().addParticle(EpicFightParticles.TSUNAMI_SPLASH.get(), pos.x + rand.x, pos.y + rand.y - 1.0D, pos.z + rand.z, rand.x * 0.1D, rand.y * 0.1D, rand.z * 0.1D);
                        }
                    }
                }, AnimationEvent.Side.CLIENT))
                .addEvents(InTimeEvent.create(0.35F, (entitypatch, animation, params) -> {
                    entitypatch.playSound(SoundEvents.TRIDENT_RIPTIDE_3, 0, 0);
                }, Side.CLIENT), InTimeEvent.create(0.35F, (entitypatch, animation, params) -> {
                    entitypatch.setAirborneState(true);
                }, AnimationEvent.Side.SERVER)));

        TSUNAMI_REINFORCED = builder.nextAccessor("biped/skill/tsunami_reinforced", (accessor) ->
            new AttackAnimation(0.2F, 0.2F, 0.35F, 0.65F, 1.3F, ColliderPreset.BIPED_BODY_COLLIDER, Armatures.BIPED.get().rootJoint, accessor, Armatures.BIPED)
                .addProperty(AttackPhaseProperty.MAX_STRIKES_MODIFIER, ValueModifier.adder(10))
                .addProperty(ActionAnimationProperty.COORD_SET_BEGIN, MoveCoordFunctions.RAW_COORD_WITH_X_ROT)
                .addProperty(ActionAnimationProperty.COORD_SET_TICK, null)
                .addProperty(ActionAnimationProperty.MOVE_VERTICAL, true)
                .addProperty(ActionAnimationProperty.NO_GRAVITY_TIME, TimePairList.create(0.15F, 0.85F))
                .addProperty(StaticAnimationProperty.PLAY_SPEED_MODIFIER, Animations.ReusableSources.CONSTANT_ONE)
                .addProperty(StaticAnimationProperty.POSE_MODIFIER, Animations.ReusableSources.ROOT_X_MODIFIER)
                .addEvents(StaticAnimationProperty.ON_END_EVENTS, SimpleEvent.create(Animations.ReusableSources.RESTORE_BOUNDING_BOX, AnimationEvent.Side.BOTH))
                .addEvents(StaticAnimationProperty.TICK_EVENTS, SimpleEvent.create(Animations.ReusableSources.RESIZE_BOUNDING_BOX, AnimationEvent.Side.BOTH).params(EntityDimensions.scalable(0.6F, 1.0F)))
                .addEvents(InPeriodEvent.create(0.35F, 1.0F, (entitypatch, animation, params) -> {
                    Vec3 pos = entitypatch.getOriginal().position();

                    for (int x = -1; x <= 1; x += 2) {
                        for (int z = -1; z <= 1; z += 2) {
                            Vec3 rand = new Vec3(Math.random() * x, Math.random(), Math.random() * z).normalize().scale(2.0D);
                            entitypatch.getLevel().addParticle(EpicFightParticles.TSUNAMI_SPLASH.get(), pos.x + rand.x, pos.y + rand.y - 1.0D, pos.z + rand.z, rand.x * 0.1D, rand.y * 0.1D, rand.z * 0.1D);
                        }
                    }
                }, AnimationEvent.Side.CLIENT))
                .addEvents(InTimeEvent.create(0.35F, (entitypatch, animation, params) -> {
                    entitypatch.playSound(SoundEvents.TRIDENT_RIPTIDE_3, 0, 0);
                }, Side.CLIENT), InTimeEvent.create(0.35F, (entitypatch, animation, params) -> {
                    entitypatch.setAirborneState(true);
                }, AnimationEvent.Side.SERVER)));

        EVERLASTING_ALLEGIANCE_CALL = builder.nextAccessor("biped/skill/everlasting_allegiance_call", (accessor) ->
            new ActionAnimation(0.1F, 0.55F, accessor, Armatures.BIPED)
                .addProperty(ActionAnimationProperty.STOP_MOVEMENT, true));
        EVERLASTING_ALLEGIANCE_CATCH = builder.nextAccessor("biped/skill/everlasting_allegiance_catch", (accessor) ->
            new ActionAnimation(0.05F, 0.8F, accessor, Armatures.BIPED)
                .addProperty(ActionAnimationProperty.STOP_MOVEMENT, true));

        SHARP_STAB = builder.nextAccessor("biped/skill/sharp_stab", (accessor) -> new AttackAnimation(0.15F, 0.05F, 0.1F, 0.15F, 0.7F, ColliderPreset.LONGSWORD, Armatures.BIPED.get().toolR, accessor, Armatures.BIPED));
    }

    public static abstract class ReusableSources {
        public static final AnimationEvent.E1<EntityDimensions> RESIZE_BOUNDING_BOX = (entitypatch, animation, params) -> {
            if (params != null) {
                entitypatch.resetSize(params.first());
            }
        };

        public static final AnimationEvent.E1<Boolean> RESTORE_BOUNDING_BOX = (entitypatch, animation, params) -> {
            entitypatch.getOriginal().refreshDimensions();
        };

        public static final AnimationEvent.E0 WING_FLAP = (entitypatch, animation, params) -> {
            if (entitypatch instanceof EnderDragonPatch enderDragonPatch) {
                enderDragonPatch.getOriginal().onFlap();
            }
        };

        public static final AnimationEvent.E4<Vec3f, Joint, Double, Float> FRACTURE_GROUND_SIMPLE = (entitypatch, animation, params) -> {
            Vec3 position = entitypatch.getOriginal().position();
            OpenMatrix4f modelTransform = entitypatch.getArmature().getBoundTransformFor(animation.get().getPoseByTime(entitypatch, params.fourth(), 1.0F), params.second())
                                                     .mulFront(
                                                         OpenMatrix4f.createTranslation((float)position.x, (float)position.y, (float)position.z)
                                                                     .mulBack(OpenMatrix4f.createRotatorDeg(180.0F, Vec3f.Y_AXIS)
                                                                     .mulBack(entitypatch.getModelMatrix(1.0F))));

            Level level = entitypatch.getLevel();
            Vec3 weaponEdge = OpenMatrix4f.transform(modelTransform, (params.first()).toDoubleVector());
            Vec3 slamStartPos;
            BlockHitResult hitResult = level.clip(new ClipContext(position.add(0.0D, 0.1D, 0.0D), weaponEdge, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entitypatch.getOriginal()));

            if (hitResult.getType() == HitResult.Type.BLOCK) {
                Direction direction = hitResult.getDirection();
                BlockPos collidePos = hitResult.getBlockPos().offset(direction.getStepX(), direction.getStepY(), direction.getStepZ());

                if (!LevelUtil.canTransferShockWave(level, collidePos, level.getBlockState(collidePos))) {
                    collidePos = collidePos.below();
                }

                slamStartPos = new Vec3(collidePos.getX(), collidePos.getY(), collidePos.getZ());
            } else {
                slamStartPos = weaponEdge.subtract(0.0D, 1.0D, 0.0D);
            }

            LevelUtil.circleSlamFracture(entitypatch.getOriginal(), level, slamStartPos, params.third(), false, false);
        };

        public static final AnimationEvent.E3<Vec3f, Joint, Float> FRACTURE_METEOR_STRIKE = (entitypatch, animation, params) -> {
            if (entitypatch instanceof PlayerPatch<?> playerpatch) {
                playerpatch.getSkillContainerFor(EpicFightSkills.METEOR_SLAM.get()).ifPresent(skill -> {
                    double slamRadius = Math.log(MeteorSlamSkill.getFallDistance(skill) * entitypatch.getOriginal().getAttributeValue(EpicFightAttributes.IMPACT));
                    FRACTURE_GROUND_SIMPLE.fire(entitypatch, animation, AnimationParameters.of(params.first(), params.second(), Double.valueOf(slamRadius), params.third()));
                });
            }
        };

        public static final AnimationEvent.E0 SUMMON_THUNDER = (entitypatch, animation, params) -> {
            if (entitypatch.isLogicalClient()) {
                return;
            }

            if (animation.get() instanceof AttackAnimation attackAnimation) {
                Phase phase = attackAnimation.phases[1];

                int i = (int)ValueModifier.calculator().attach(phase.getProperty(AttackPhaseProperty.MAX_STRIKES_MODIFIER).orElse(ValueModifier.setter(3))).getResult(0);
                float damage = ValueModifier.calculator().attach(phase.getProperty(AttackPhaseProperty.DAMAGE_MODIFIER).orElse(ValueModifier.setter(8.0F))).getResult(0);

                LivingEntity original = entitypatch.getOriginal();
                ServerLevel level = (ServerLevel)original.level();
                float total = damage + ExtraDamageInstance.SWEEPING_EDGE_ENCHANTMENT.create().get(original, original.getItemInHand(InteractionHand.MAIN_HAND), null, damage);

                List<Entity> list = level.getEntities(original, original.getBoundingBox().inflate(10.0D, 4.0D, 10.0D), (e) -> {
                    return !(e.distanceToSqr(original) > 100.0D) && !e.isAlliedTo(original) && entitypatch.getOriginal().hasLineOfSight(e);
                });

                list = HitEntityList.Priority.HOSTILITY.sort(entitypatch, list);
                int count = 0;

                while (count < i && count < list.size()) {
                    Entity e = list.get(count++);
                    BlockPos blockpos = e.blockPosition();
                    LightningBolt lightningbolt = EntityType.LIGHTNING_BOLT.create(level);
                    // Suppresses vanilla thunderHit and spawnFire outright, so NeoForge's
                    // extra setDamage(0.0F) here has no vanilla equivalent and is not needed.
                    lightningbolt.setVisualOnly(true);
                    lightningbolt.moveTo(Vec3.atBottomCenterOf(blockpos));
                    lightningbolt.setCause(entitypatch instanceof ServerPlayerPatch serverPlayerPatch ? serverPlayerPatch.getOriginal() : null);

                    DamageSource dmgSource = new DamageSource(e.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.LIGHTNING_BOLT), entitypatch.getOriginal());
                    EpicFightDamageSource damageSource = attackAnimation.getEpicFightDamageSource(dmgSource, entitypatch, e, phase).setUsedItem(entitypatch.getOriginal().getItemInHand(InteractionHand.MAIN_HAND));
                    e.hurt(damageSource, total);
                    e.thunderHit(level, lightningbolt);

                    level.addFreshEntity(lightningbolt);
                }

                if (count > 0) {
                    if (level.getGameRules().getBoolean(GameRules.RULE_WEATHER_CYCLE) && level.random.nextFloat() < 0.08F && level.getThunderLevel(1.0F) < 1.0F) {
                        level.setWeatherParameters(0, Mth.randomBetweenInclusive(level.random, 12000, 180000), true, true);
                    }

                    original.playSound(SoundEvents.TRIDENT_THUNDER.value(), 5.0F, 1.0F);
                }
            }
        };

        public static final AnimationEvent.E1<SoundEvent> PLAY_SOUND = (entitypatch, animation, params) -> entitypatch.playSound(params.first(), 0, 0);
        public static final IndependentVariableKey<Boolean> TOOLS_IN_BACK = AnimationVariables.unsyncIndependent((animator) -> false, true);

        private static void moveToolBonesToBack(LivingEntityPatch<?> entitypatch, AssetAccessor<? extends StaticAnimation> animation, ToolHolderArmature toolArmature) {
            entitypatch.setParentJointOfHand(InteractionHand.MAIN_HAND, toolArmature.backToolJoint());
            entitypatch.setParentJointOfHand(InteractionHand.OFF_HAND, toolArmature.backToolJoint());
            entitypatch.getAnimator().getVariables().put(TOOLS_IN_BACK, animation, true);
        }

        private static void moveToolBonesToHands(LivingEntityPatch<?> entitypatch, AssetAccessor<? extends StaticAnimation> animation, ToolHolderArmature toolArmature) {
            entitypatch.setParentJointOfHand(InteractionHand.MAIN_HAND, toolArmature.rightToolJoint());
            entitypatch.setParentJointOfHand(InteractionHand.OFF_HAND, toolArmature.leftToolJoint());
            entitypatch.getAnimator().getVariables().remove(TOOLS_IN_BACK, animation);
        }

        public static final AnimationEvent.E0 SET_TOOLS_BACK = (entitypatch, animation, params) -> {
            if (entitypatch.getArmature() instanceof ToolHolderArmature toolArmature) {
                moveToolBonesToBack(entitypatch, animation, toolArmature);
            }
        };

        public static final AnimationEvent.E0 SET_TOOLS_BACK_WHEN_MOUNT = (entitypatch, animation, params) -> {
            // Keep the weapon in hand on every mount. The original behavior sheathed it to the back
            // whenever the weapon wasn't `availableOnHorse`, which broke skeletons on skeleton horses
            // (skeleton trap), spider jockeys, and chicken jockeys -- the bow visually clipped to the
            // skeleton's back even though the rider was actively shooting. Bows/crossbows still aim
            // and shoot via the existing AIM/SHOT composite layer regardless of mount type.
        };

        /// Fabric-compatible ladder detection.
        /// Replaces NeoForge's BlockState.isLadder(Level, BlockPos, LivingEntity) extension.
        /// Checks BlockTags.CLIMBABLE (ladders, vines, scaffolding, etc.) and open trapdoors
        /// with a climbable block below (matching vanilla onClimbable behavior).
        private static boolean isLadderBlock(BlockState state, Level level, BlockPos pos, LivingEntity entity) {
            if (state.is(BlockTags.CLIMBABLE)) {
                return true;
            }
            if (state.hasProperty(BlockStateProperties.OPEN) && state.getValue(BlockStateProperties.OPEN)) {
                BlockPos below = pos.below();
                BlockState belowState = level.getBlockState(below);
                return belowState.is(BlockTags.CLIMBABLE);
            }
            return false;
        }

        @SuppressWarnings("incomplete-switch")
        public static final AnimationEvent.E0 UPDATE_Y_TO_NEARBY_LADDER = (entitypatch, animation, params) -> {
            LivingEntity original = entitypatch.getOriginal();
            BlockState bs = original.getInBlockState();
            Level level = original.level();
            BlockPos bp = original.blockPosition();

            boolean isSpectator = (entitypatch.getOriginal() instanceof Player && entitypatch.getOriginal().isSpectator());
            Direction direction = null;

            if (isSpectator || original.onGround() || !original.isAlive()) {
                direction = Direction.UP;
            }

            if (ServerConfig.fullBoundingBoxLadders) {
                if (isLadderBlock(bs, level, bp, original)) {
                    if (bs.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
                        direction = bs.getValue(BlockStateProperties.HORIZONTAL_FACING);
                    } else {
                        if (bs.hasProperty(BlockStateProperties.UP) && bs.getValue(BlockStateProperties.UP)) {
                            direction = Direction.UP;
                        } else if (bs.hasProperty(BlockStateProperties.NORTH) && bs.getValue(BlockStateProperties.NORTH)) {
                            direction = Direction.SOUTH;
                        } else if (bs.hasProperty(BlockStateProperties.WEST) && bs.getValue(BlockStateProperties.WEST)) {
                            direction = Direction.EAST;
                        } else if (bs.hasProperty(BlockStateProperties.SOUTH) && bs.getValue(BlockStateProperties.SOUTH)) {
                            direction = Direction.NORTH;
                        } else if (bs.hasProperty(BlockStateProperties.EAST) && bs.getValue(BlockStateProperties.EAST)) {
                            direction = Direction.WEST;
                        }
                    }
                }
            } else {
                AABB bb = original.getBoundingBox();
                int mX = Mth.floor(bb.minX);
                int mY = Mth.floor(bb.minY);
                int mZ = Mth.floor(bb.minZ);

                for (int y2 = mY; y2 < bb.maxY; y2++) {
                    for (int x2 = mX; x2 < bb.maxX; x2++) {
                        for (int z2 = mZ; z2 < bb.maxZ; z2++) {
                            BlockPos tmp = new BlockPos(x2, y2, z2);
                            bs = level.getBlockState(tmp);
                            if (isLadderBlock(bs, level, tmp, original)) {
                                if (bs.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
                                    direction = bs.getValue(BlockStateProperties.HORIZONTAL_FACING);
                                } else {
                                    if (bs.hasProperty(BlockStateProperties.UP) && bs.getValue(BlockStateProperties.UP)) {
                                        direction = Direction.UP;
                                    } else if (bs.hasProperty(BlockStateProperties.NORTH) && bs.getValue(BlockStateProperties.NORTH)) {
                                        direction = Direction.SOUTH;
                                    } else if (bs.hasProperty(BlockStateProperties.WEST) && bs.getValue(BlockStateProperties.WEST)) {
                                        direction = Direction.EAST;
                                    } else if (bs.hasProperty(BlockStateProperties.SOUTH) && bs.getValue(BlockStateProperties.SOUTH)) {
                                        direction = Direction.NORTH;
                                    } else if (bs.hasProperty(BlockStateProperties.EAST) && bs.getValue(BlockStateProperties.EAST)) {
                                        direction = Direction.WEST;
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (direction != null) {
                switch(direction) {
                case NORTH -> {
                    entitypatch.setYRot(0.0F);
                    entitypatch.setYRotO(0.0F);
                }
                case EAST -> {
                    entitypatch.setYRot(90.0F);
                    entitypatch.setYRotO(90.0F);
                }
                case WEST -> {
                    entitypatch.setYRot(-90.0F);
                    entitypatch.setYRotO(-90.0F);
                }
                case SOUTH -> {
                    entitypatch.setYRot(180.0F);
                    entitypatch.setYRotO(180.0F);
                }
                }
            }
        };

        public static final AnimationEvent.E2<CapabilityItem, CapabilityItem> SET_TOOLS_BACK_WHEN_ITEM_CHANGED = (entitypatch, animation, params) -> {
            if (entitypatch.getArmature() instanceof ToolHolderArmature humanoidArmature) {
                if (!params.first().isEmpty()) {
                    moveToolBonesToBack(entitypatch, animation, humanoidArmature);
                } else {
                    moveToolBonesToHands(entitypatch, animation, humanoidArmature);
                }
            }
        };

        public static final AnimationEvent.E2<CapabilityItem, CapabilityItem> SET_TOOLS_BACK_WHEN_MOUNT_AND_ITEM_CHANGED = (entitypatch, animation, params) -> {
            // Mirror SET_TOOLS_BACK_WHEN_MOUNT: keep weapons in hand on every mount type. Always
            // revert the parent joint of hand back to the hand joints so swapped-in weapons render
            // in the rider's hands rather than clipping to the back.
            if (entitypatch.getArmature() instanceof ToolHolderArmature humanoidArmature) {
                moveToolBonesToHands(entitypatch, animation, humanoidArmature);
            }
        };

        public static final AnimationEvent.E0 REVERT_TO_HANDS = (entitypatch, animation, params) -> {
            if (entitypatch.getAnimator().getVariables().getOrDefault(TOOLS_IN_BACK, animation) && entitypatch.getArmature() instanceof ToolHolderArmature toolArmature) {
                moveToolBonesToHands(entitypatch, animation, toolArmature);
            }
        };

        public static final AnimationEvent.E0 SYNC_COORD_ROTATION = (entitypatch, animation, params) -> {
            animation.get().getProperty(ActionAnimationProperty.COORD).ifPresent(coordTransform -> {
                Quaternionf rotation = coordTransform.getInterpolatedRotation(animation.get().getTotalTime());
                Vector3f eulerAngles = rotation.getEulerAnglesYXZ(new Vector3f());
                entitypatch.setYRotO(Mth.wrapDegrees(entitypatch.getYRot() + (float)Math.toDegrees(eulerAngles.y)));
                entitypatch.setYRot(Mth.wrapDegrees(entitypatch.getYRot() + (float)Math.toDegrees(eulerAngles.y)));
            });
        };

        public static final AnimationEvent.E0 PLAY_STEPPING_SOUND = (entitypatch, animation, params) -> {
            BlockState state = entitypatch.getLevel().getBlockState(entitypatch.getOriginal().getOnPos());
            entitypatch.playSound(BlockStateExtension.of(state).epicfight$getSoundType(entitypatch.getLevel(), entitypatch.getOriginal().blockPosition(), entitypatch.getOriginal()).getHitSound(), 0, 0);
        };

        public static final AnimationEvent.E0 TELEPORT_ATTACK_SERVER = (entitypatch, self, param) -> {
            entitypatch.getAnimator().getVariables().put(EpicFightSynchedAnimationVariableKeys.DESTINATION.get(), self, entitypatch.getOriginal().position());
            entitypatch.getAnimator().getVariables().put(EpicFightSynchedAnimationVariableKeys.TARGET_ENTITY.get(), self, entitypatch.getTarget().getId());
            entitypatch.getAnimator().getVariables().putDefaultValue(EpicFightSynchedAnimationVariableKeys.Y_ROT.get(), self);
        };

        public static final AnimationEvent.E0 TELEPORT_ATTACK_CLIENT = (entitypatch, self, param) -> {
            Entity entity = entitypatch.getOriginal();

            entitypatch.getAnimator().getVariables().get(EpicFightSynchedAnimationVariableKeys.DESTINATION.get(), self).ifPresent(vec -> {
                entity.setPos(vec.x(), vec.y(), vec.z());
                entity.xo = entity.getX();
                entity.yo = entity.getY();
                entity.zo = entity.getZ();
                entity.xOld = entity.getX();
                entity.yOld = entity.getY();
                entity.zOld = entity.getZ();
            });

            entitypatch.getAnimator().getVariables().get(EpicFightSynchedAnimationVariableKeys.Y_ROT.get(), self).ifPresent(yRot -> {
                entity.setYRot(yRot);
                entity.yRotO = yRot;
            });

            entitypatch.getAnimator().getVariables().get(EpicFightSynchedAnimationVariableKeys.TARGET_ENTITY.get(), self).ifPresent(entityId -> {
                Entity target = entitypatch.getLevel().getEntity(entityId);

                if (entity instanceof Mob mob && target instanceof LivingEntity livingentity) {
                    mob.setTarget(livingentity);
                }
            });
        };

        public static final AnimationProperty.PoseModifier COMBO_ATTACK_DIRECTION_MODIFIER = (self, pose, entitypatch, time, partialTicks) -> {
            if (!self.isStaticAnimation() || entitypatch instanceof PlayerPatch<?> playerpatch && playerpatch.isFirstPerson()) {
                return;
            }

            float pitch = entitypatch.getAttackDirectionPitch(partialTicks);
            JointTransform chest = pose.orElseEmpty("Chest");
            chest.frontResult(JointTransform.rotation(QuaternionUtils.XP.rotationDegrees(-pitch)), OpenMatrix4f::mulAsOriginInverse);

            if (entitypatch instanceof PlayerPatch) {
                float xRot = MathUtils.lerpBetween(entitypatch.getOriginal().xRotO, entitypatch.getOriginal().getXRot(), partialTicks);
                OpenMatrix4f toOriginalRotation = entitypatch.getArmature().getBoundTransformFor(pose, entitypatch.getArmature().searchJointByName("Head")).removeScale().removeTranslation().invert();
                Vec3f xAxis = OpenMatrix4f.transform3v(toOriginalRotation, Vec3f.X_AXIS, null);
                OpenMatrix4f headRotation = OpenMatrix4f.createRotatorDeg(-(pitch + xRot), xAxis);

                pose.orElseEmpty("Head").frontResult(JointTransform.fromMatrix(headRotation), OpenMatrix4f::mul);
            }
        };

        public static final AnimationProperty.PoseModifier ROOT_X_MODIFIER = (self, pose, entitypatch, time, partialTicks) -> {
            float pitch = -entitypatch.getOriginal().getXRot();
            JointTransform chest = pose.orElseEmpty("Root");
            chest.frontResult(JointTransform.rotation(QuaternionUtils.XP.rotationDegrees(-pitch)), OpenMatrix4f::mulAsOriginInverse);
        };

        public static final AnimationProperty.PoseModifier FLYING_CORRECTION = (self, pose, entitypatch, elapsedTime, partialTicks) -> {
            Vec3 vec3d = entitypatch.getOriginal().getViewVector(partialTicks);
            Vec3 vec3d1 = entitypatch.getOriginal().getDeltaMovement();
            double d0 = vec3d1.horizontalDistanceSqr();
            double d1 = vec3d.horizontalDistanceSqr();

            if (d0 > 0.0D && d1 > 0.0D) {
                JointTransform root = pose.orElseEmpty("Root");
                JointTransform head = pose.orElseEmpty("Head");
                double d2 = (vec3d1.x * vec3d.x + vec3d1.z * vec3d.z) / (Math.sqrt(d0) * Math.sqrt(d1));
                double d3 = vec3d1.x * vec3d.z - vec3d1.z * vec3d.x;
                float zRot = Mth.clamp((float)(Math.signum(d3) * Math.acos(d2)), -1.0F, 1.0F);

                root.frontResult(JointTransform.rotation(QuaternionUtils.ZP.rotation(zRot)), OpenMatrix4f::mulAsOriginInverse);

                float xRot = (float) MathUtils.getXRotOfVector(vec3d1) * 2.0F;

                MathUtils.mulQuaternion(QuaternionUtils.XP.rotationDegrees(xRot), root.rotation(), root.rotation());
                MathUtils.mulQuaternion(QuaternionUtils.XP.rotationDegrees(-xRot), head.rotation(), head.rotation());
            }
        };

        public static final AnimationProperty.PoseModifier FLYING_CORRECTION2 = (self, pose, entitypatch, elapsedTime, partialTicks) -> {
            Vec3 vec3d = entitypatch.getOriginal().getViewVector(partialTicks);
            Vec3 vec3d1 = entitypatch.getOriginal().getDeltaMovement();
            double d0 = vec3d1.horizontalDistanceSqr();
            double d1 = vec3d.horizontalDistanceSqr();

            if (d0 > 0.0D && d1 > 0.0D) {
                JointTransform root = pose.orElseEmpty("Root");
                JointTransform head = pose.orElseEmpty("Head");
                float xRot = (float) MathUtils.getXRotOfVector(vec3d1) * 2.0F;
                MathUtils.mulQuaternion(QuaternionUtils.XP.rotationDegrees(-xRot), root.rotation(), root.rotation());
                MathUtils.mulQuaternion(QuaternionUtils.XP.rotationDegrees(xRot), head.rotation(), head.rotation());
            }
        };

        public static final AnimationProperty.PoseModifier MAP_ARMS_CORRECTION = (self, pose, entitypatch, elapsedTime, partialTicks) -> {
            float xRot = 50.0F - (entitypatch.getOriginal().xRotO + (entitypatch.getOriginal().getXRot() - entitypatch.getOriginal().xRotO) * partialTicks);
            xRot = Mth.clamp(xRot, 0.0F, 50.0F);

            JointTransform shoulderL = pose.orElseEmpty("Shoulder_L");
            JointTransform shoulderR = pose.orElseEmpty("Shoulder_R");

            float trans = xRot / 500.0F;

            shoulderL.jointLocal(JointTransform.translation(new Vec3f(0.0F, trans, -trans)), OpenMatrix4f::mul);
            shoulderR.jointLocal(JointTransform.translation(new Vec3f(0.0F, trans, -trans)), OpenMatrix4f::mul);
            shoulderL.frontResult(JointTransform.rotation(QuaternionUtils.XP.rotationDegrees(xRot)), OpenMatrix4f::mulAsOriginInverse);
            shoulderR.frontResult(JointTransform.rotation(QuaternionUtils.XP.rotationDegrees(xRot)), OpenMatrix4f::mulAsOriginInverse);
        };

        public static final AnimationProperty.PoseModifier APPLY_COORD_ROTATION = (self, pose, entitypatch, elapsedTime, partialTicks) -> {
            if (!entitypatch.getAnimator().getPlayerFor(self.getAccessor()).isEnd()) {
                self.getProperty(ActionAnimationProperty.COORD).ifPresent(coordTransform -> {
                    Quaternionf rotation = coordTransform.getInterpolatedRotation(elapsedTime);
                    pose.get("Root").parent(JointTransform.rotation(rotation), OpenMatrix4f::mul);
                });
            }
        };

        public static final AnimationProperty.PlaybackSpeedModifier CONSTANT_ONE = (self, entitypatch, speed, prevElapsedTime, elapsedTime) -> 1.0F;

        public static final AnimationProperty.PlaybackSpeedModifier CHARGING = (self, entitypatch, speed, prevElapsedTime, elapsedTime) -> {
            if (self.isLinkAnimation()) {
                return 1.0F;
            } else {
                return (float)-Math.pow((self.getTotalTime() - elapsedTime) / self.getTotalTime() - 1.0F, 2) + 1.0F;
            }
        };
    }
}
