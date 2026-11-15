package yesman.epicfight.api.event.impl;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import org.jetbrains.annotations.Nullable;
import yesman.epicfight.api.animation.LivingMotions;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.event.EpicFightEventHooks;
import yesman.epicfight.api.event.types.entity.*;
import yesman.epicfight.api.utils.AttackResult;
import yesman.epicfight.api.utils.math.ValueModifier;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.main.EpicFightSharedConstants;
import yesman.epicfight.mixin.common.MixinProjectile;
import yesman.epicfight.network.EpicFightNetworkManager;
import yesman.epicfight.network.server.SPMobEffectControl;
import yesman.epicfight.registry.entries.EpicFightEntityTypes;
import yesman.epicfight.registry.entries.EpicFightMobEffects;
import yesman.epicfight.registry.entries.EpicFightParticles;
import yesman.epicfight.registry.entries.EpicFightSounds;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.EntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.HumanoidMobPatch;
import yesman.epicfight.world.capabilities.entitypatch.HurtableEntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.mob.EndermanPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.world.capabilities.item.ArmorCapability;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.capabilities.projectile.ProjectilePatch;
import yesman.epicfight.world.damagesource.EpicFightDamageSource;
import yesman.epicfight.world.damagesource.EpicFightDamageTypeTags;
import yesman.epicfight.world.damagesource.StunType;
import yesman.epicfight.world.gamerule.EpicFightGameRules;

import java.util.Collection;
import java.util.function.Consumer;

/// EventHook hooks that must be triggered by either mod loader event or Mixins.
///
/// Some hooks provide information about canceling or modifying the remaining
/// behavior. Each needs to be implmeneted through the caller.
public final class VanillaEntityEventHooks {

    /// Called when instantiating the entity
    ///
    /// @see Entity#Entity(EntityType, Level)
    @SuppressWarnings({"unchecked"})
    public static void onConstruct(Entity entity) {
        EpicFightCapabilities.getUnparameterizedEntityPatch(entity, EntityPatch.class).ifPresent(entitypatch -> {
            if (entitypatch.uninitialized()) {
                entitypatch.onConstructed(entity);
            }
        });
    }

    /// Called when an entity joins a world
    ///
    /// @see ClientLevel#addEntity
    /// @see PersistentEntitySectionManager#addEntity
    @SuppressWarnings({"unchecked"})
    public static void onJoinLevel(Entity entity, Level level, boolean worldgen) {
        EpicFightCapabilities.getUnparameterizedEntityPatch(entity, EntityPatch.class).ifPresent(entitypatch -> {
            if (entitypatch.uninitialized()) {
                entitypatch.onJoinWorld(entity, level, worldgen);
            }
        });
    }

    /// Called before ticking an entity
    ///
    /// @see ClientLevel#tickNonPassenger
    /// @see ServerLevel#tickNonPassenger
    public static void preTick(Entity entity) {
        EpicFightCapabilities.getUnparameterizedEntityPatch(entity, EntityPatch.class).ifPresent(entitypatch -> {
            entitypatch.preTick();

            if (entitypatch.isLogicalClient()) {
                entitypatch.preTickClient();
            } else {
                entitypatch.preTickServer();
            }
        });
    }

    /// Called after ticking an entity
    ///
    /// @see ClientLevel#tickNonPassenger
    /// @see ServerLevel#tickNonPassenger
    public static void postTick(Entity entity) {
        EpicFightCapabilities.getUnparameterizedEntityPatch(entity, EntityPatch.class).ifPresent(entitypatch -> {
            entitypatch.postTick();

            if (entitypatch.isLogicalClient()) {
                entitypatch.postTickClient();
            } else {
                entitypatch.postTickServer();
            }
        });
    }

    /// Called when an entity is dead
    ///
    /// @see LivingEntity#die
    public static void onLivingDeath(LivingEntity entity, DamageSource damageSource) {
        EpicFightCapabilities.getUnparameterizedEntityPatch(entity, LivingEntityPatch.class).ifPresent(entitypatch -> {
            entitypatch.onDeath(damageSource);
        });

        EpicFightCapabilities.getUnparameterizedEntityPatch(damageSource.getEntity(), LivingEntityPatch.class).ifPresent(entitypatch -> {
            EpicFightEventHooks.Entity.KILL_ENTITY.postWithListener(new KillEntityEvent(entitypatch, entity, damageSource), entitypatch.getEventListener());
        });

        /* Chicken explode code (leave for entity udpate)
        if (entity instanceof Chicken) {
            Vec3 pos = entity.position();

            for (int i = -1; i <= 1; i+=2) {
                for (int j = -1; j <= 1; j+=2) {
                    for (int k = 0; k < 8; k++) {
                        float power = 0.4F;
                        float powerX = entity.getRandom().nextFloat() * power;
                        float powerY = (entity.getRandom().nextFloat() + 0.5F) * power;
                        float powerZ = entity.getRandom().nextFloat() * power;
                        entity.level().addParticle( EpicFightParticles.FEATHER.get(), pos.x, pos.y, pos.z, i * powerX, powerY, j * powerZ);
                    }
                }
            }
        }
        */
    }

    /// Called when an entity is knocked by incoming attacks
    ///
    /// @see LivingEntity#knockback
    ///
    /// @return whether cancel the event
    public static boolean onKnockedBack(LivingEntity entity) {
        HurtableEntityPatch<?> entitypatch = EpicFightCapabilities.getEntityPatch(entity, HurtableEntityPatch.class);

        if (entitypatch != null) {
            return entitypatch.shouldCancelKnockback();
        }

        return false;
    }

    /// Called when an entity takes
    ///
    /// @param entity           the entity being hurt
    /// @param damageSource     the damage type
    /// @param amount           the amount of damage
    ///
    /// @see LivingEntity#hurt
    ///
    /// @return whether cancel the taken damage
    public static boolean onDamageIncomes(LivingEntity entity, DamageSource damageSource, float amount) {
        // Let the event passes in the cases we don't want to handle without canceling it to avoid potential issue with other mods
        if (entity.level().isClientSide() || entity.isInvulnerableTo(damageSource) || entity.invulnerableTime > 10 && amount <= entity.lastHurt || entity.getHealth() <= 0.0F) return false;

        if (damageSource instanceof EpicFightDamageSource epicfightDamagesource && damageSource.getEntity() instanceof ServerPlayer serverplayer) {
            ServerPlayerPatch playerpatch = EpicFightCapabilities.getServerPlayerPatch(serverplayer);

            if (playerpatch != null) {
                DealDamageEvent.Income dealDamageAttack = new DealDamageEvent.Income(playerpatch, entity, epicfightDamagesource, amount);
                EpicFightEventHooks.Entity.DELIVER_DAMAGE_INCOME.postWithListener(dealDamageAttack, playerpatch.getEventListener());

                if (dealDamageAttack.isCanceled()) {
                    return true;
                }
            }
        }

        LivingEntityPatch<?> entitypatch = EpicFightCapabilities.getEntityPatch(entity, LivingEntityPatch.class);
        AttackResult result = entitypatch != null ? entitypatch.tryHurt(damageSource, amount) : AttackResult.success(amount);

        EpicFightCapabilities.getUnparameterizedEntityPatch(damageSource.getEntity(), LivingEntityPatch.class).ifPresent(attackerentitypatch -> {
            attackerentitypatch.setLastAttackResult(result);
        });

        return !result.resultType.dealtDamage();
    }

    /// Called before calculating damage with Epic Fight damage system [ValueModifier]
    ///
    /// @param hitEntity        the entity being hurt
    /// @param damageSource     the damage type
    /// @param amount           the amount of damage
    ///
    /// @see LivingEntity#actuallyHurt
    public static void onCalculateDamagePre(LivingEntity hitEntity, DamageSource damageSource, float amount, Consumer<Float> modifiedDamageApplier) {
        @Nullable EpicFightDamageSource epicfightDamageSource = damageSource instanceof EpicFightDamageSource ? (EpicFightDamageSource)damageSource : null;
        @Nullable Entity causingEntity = damageSource.getEntity();
        @Nullable LivingEntityPatch<?> hitEntityPatch = EpicFightCapabilities.getEntityPatch(hitEntity, LivingEntityPatch.class);
        @Nullable LivingEntityPatch<?> causingEntityPatch = EpicFightCapabilities.getEntityPatch(causingEntity, LivingEntityPatch.class);

        ValueModifier.ResultCalculator damageCalculator = ValueModifier.calculator();
        float finalDamage = amount;

        if (hitEntityPatch != null) {
            TakeDamageEvent.Pre takeDamagePre = new TakeDamageEvent.Pre(hitEntityPatch, damageSource, damageCalculator, finalDamage);
            EpicFightEventHooks.Entity.TAKE_DAMAGE_PRE.postWithListener(takeDamagePre, hitEntityPatch.getEventListener());
        }

        if (causingEntity != null) {
            if (causingEntityPatch != null) {
                finalDamage = causingEntityPatch.getModifiedBaseDamage(finalDamage);

                if (epicfightDamageSource != null) {
                    DealDamageEvent.Pre dealDamagePre = new DealDamageEvent.Pre(causingEntityPatch, hitEntity, epicfightDamageSource, finalDamage);
                    EpicFightEventHooks.Entity.DELIVER_DAMAGE_PRE.postWithListener(dealDamagePre, causingEntityPatch.getEventListener());
                }
            }
        }

        if (epicfightDamageSource != null && epicfightDamageSource.is(EpicFightDamageTypeTags.EXECUTION)) {
            if (hitEntityPatch != null) {
                int executionResistance = hitEntityPatch.getAssassinationResistance();

                if (executionResistance > 0) {
                    hitEntityPatch.setExecutionResistance(executionResistance - 1);
                } else {
                    finalDamage = EpicFightSharedConstants.EXECUTION_DAMAGE;
                }
            } else {
                finalDamage = EpicFightSharedConstants.EXECUTION_DAMAGE;
            }
        }

        if (Float.compare(EpicFightSharedConstants.EXECUTION_DAMAGE, finalDamage) != 0) {
            if (epicfightDamageSource != null) {
                epicfightDamageSource.attachDamageModifier(damageCalculator);
                finalDamage = epicfightDamageSource.calculateDamageAgainst(causingEntity, hitEntity, finalDamage);
            } else {
                finalDamage = damageCalculator.getResult(finalDamage);
            }

            modifiedDamageApplier.accept(finalDamage);
        }

        // Apply stun & knockback
        if (finalDamage > 0.0F && epicfightDamageSource != null && !epicfightDamageSource.is(EpicFightDamageTypeTags.NO_STUN)) {
            @Nullable HurtableEntityPatch<?> hitEntityPatchAsHurtable = hitEntityPatch != null ? hitEntityPatch : EpicFightCapabilities.getEntityPatch(hitEntity, HurtableEntityPatch.class);

            if (hitEntityPatchAsHurtable != null) {
                StunType stunType = epicfightDamageSource.getStunType();
                float stunTime = 0.0F;
                float knockBackAmount = 0.0F;
                float stunShield = hitEntityPatchAsHurtable.getStunShield();
                float impact = epicfightDamageSource.calculateImpact();

                if (stunShield > impact) {
                    if (stunType == StunType.SHORT || stunType == StunType.LONG) {
                        stunType = StunType.NONE;
                    }
                }

                if (hitEntityPatch != null) {
                    StunnedEvent entityStunnedEvent = new StunnedEvent(epicfightDamageSource, hitEntityPatch, stunType);
                    EpicFightEventHooks.Entity.ON_STUNNED.postWithListener(entityStunnedEvent, hitEntityPatch.getEventListener());

                    if (entityStunnedEvent.isCanceled()) {
                        // Skip all stunnig jobs behind when canceled
                        return;
                    }
                }

                hitEntityPatchAsHurtable.damageStunShield(finalDamage, impact);

                switch (stunType) {
                    case SHORT -> {
                        // Solution by Cyber2049(github): Fix stun immunity
                        stunType = StunType.NONE;

                        if (!hitEntity.hasEffect(EpicFightMobEffects.STUN_IMMUNITY) && Float.compare(hitEntityPatchAsHurtable.getStunShield(), 0.0F) == 0) {
                            float totalStunTime = (0.25F + impact * 0.1F) * (1.0F - hitEntityPatchAsHurtable.getStunReduction());

                            if (totalStunTime >= 0.075F) {
                                stunTime = totalStunTime - 0.1F;
                                boolean isLongStun = totalStunTime >= 0.83F;
                                stunTime = isLongStun ? 0.83F : stunTime;
                                stunType = isLongStun ? StunType.LONG : StunType.SHORT;
                                knockBackAmount = Math.min(isLongStun ? impact * 0.05F : totalStunTime, 2.0F);
                            }

                            stunTime *= (float) (1.0F - hitEntity.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
                        }
                    }
                    case LONG -> {
                        stunType = hitEntity.hasEffect(EpicFightMobEffects.STUN_IMMUNITY) ? StunType.NONE : StunType.LONG;
                        knockBackAmount = Math.min(impact * 0.05F, 5.0F);
                        stunTime = 0.83F;
                    }
                    case HOLD -> {
                        stunType = StunType.SHORT;
                        stunTime = impact * 0.25F;
                    }
                    case KNOCKDOWN -> {
                        stunType = hitEntity.hasEffect(EpicFightMobEffects.STUN_IMMUNITY) ? StunType.NONE : StunType.KNOCKDOWN;
                        knockBackAmount = Math.min(impact * 0.05F, 5.0F);
                        stunTime = 2.0F;
                    }
                    case NEUTRALIZE -> {
                        hitEntityPatchAsHurtable.playSound(EpicFightSounds.NEUTRALIZE_MOBS.get(), 3.0F, 0.0F, 0.1F);
                        EpicFightParticles.AIR_BURST.get().spawnParticleWithArgument(((ServerLevel)hitEntity.level()), hitEntity, damageSource.getDirectEntity());
                        knockBackAmount = 0.0F;
                        stunTime = 2.0F;
                    }
                    default -> {
                    }
                }

                Vec3 sourcePosition = epicfightDamageSource.getInitialPosition();
                hitEntityPatchAsHurtable.setStunReductionOnHit(stunType);
                boolean stunApplied = hitEntityPatchAsHurtable.applyStun(stunType, stunTime);

                if (sourcePosition != null) {
                    if (!(hitEntity instanceof Player) && stunApplied) {
                        hitEntity.lookAt(EntityAnchorArgument.Anchor.FEET, sourcePosition);
                    }

                    if (knockBackAmount > 0.0F) {
                        float weight = hitEntityPatchAsHurtable.getWeight();
                        if (weight > 0.0F) {
                            knockBackAmount *= 40.0F / weight;
                        }

                        hitEntityPatchAsHurtable.knockBackEntity(sourcePosition, knockBackAmount);
                    }
                }
            }
        }

        if (damageSource.is(DamageTypes.FALL) && finalDamage > 1.0F && EpicFightGameRules.HAS_FALL_ANIMATION.getRuleValue(hitEntity.level())) {
            LivingEntityPatch<?> entitypatch = EpicFightCapabilities.getEntityPatch(hitEntity, LivingEntityPatch.class);

            if (entitypatch != null && !entitypatch.getEntityState().inaction()) {
                AssetAccessor<? extends StaticAnimation> fallAnimation = entitypatch.getAnimator().getLivingAnimation(LivingMotions.LANDING_RECOVERY, entitypatch.getHitAnimation(StunType.FALL));

                if (fallAnimation != null) {
                    entitypatch.playAnimationSynchronized(fallAnimation, 0);
                }
            }
        }
    }

    /// Called after damage calculation is finished.
    ///
    /// @param hitEntity        the entity being hurt
    /// @param damageSource     the damage type
    /// @param amount           the amount of damage
    ///
    /// @see LivingEntity#actuallyHurt
    public static void onCalculateDamagePost(LivingEntity hitEntity, DamageSource damageSource, float amount) {
        if (damageSource instanceof EpicFightDamageSource epicFightDamageSource) {
            EpicFightCapabilities.getUnparameterizedEntityPatch(damageSource.getEntity(), LivingEntityPatch.class).ifPresent(entitypatch -> {
                EpicFightEventHooks.Entity.DELIVER_DAMAGE_POST.postWithListener(new DealDamageEvent.Post(entitypatch, hitEntity, epicFightDamageSource, amount), entitypatch.getEventListener());
            });
        }

        if (damageSource.getEntity() != null) {
            EpicFightCapabilities.getUnparameterizedEntityPatch(hitEntity, LivingEntityPatch.class).ifPresent(entitypatch -> {
                EpicFightEventHooks.Entity.TAKE_DAMAGE_POST.postWithListener(new TakeDamageEvent.Post(entitypatch, damageSource, amount), entitypatch.getEventListener());
            });
        }
    }

    /// Called when an entity blocks
    ///
    /// @see LivingEntity#hurt
    public static void onBlockAttacksWithShield(LivingEntity entity) {
        EpicFightCapabilities.<LivingEntity, LivingEntityPatch<LivingEntity>>getParameterizedEntityPatch(entity, LivingEntity.class, LivingEntityPatch.class).ifPresent(entitypatch -> {
            entitypatch.playAnimationSynchronized(Animations.BIPED_HIT_SHIELD, 0.0F);
        });
    }

    /// Called when an entity drops items by death
    ///
    /// @see LivingEntity#dropAllDeathLoot
    ///
    /// @return whether cancel the event
    public static boolean onDropItems(LivingEntity entity, DamageSource source, Collection<ItemEntity> drops) {
        LivingEntityPatch<?> entitypatch = EpicFightCapabilities.getEntityPatch(entity, LivingEntityPatch.class);
        return entitypatch != null && entitypatch.onDrop(source, drops);
    }

    /// Called when a projectile hits a block or an entity
    ///
    /// @see Projectile#hitTargetOrDeflectSelf
    ///
    /// @return whether cancel the event
    public static boolean onProjectileImpacts(HitResult hitResult, Projectile projectile) {
        ProjectilePatch<?> projectilepatch = EpicFightCapabilities.getEntityPatch(projectile, ProjectilePatch.class);
        boolean shouldCancel = false;

        if (projectilepatch != null) {
            if (projectilepatch.onProjectileImpact(hitResult)) {
                shouldCancel = true;
            }
        }

        if (hitResult instanceof EntityHitResult rayResult) {
            LivingEntityPatch<?> entitypatch = EpicFightCapabilities.getEntityPatch(rayResult.getEntity(), LivingEntityPatch.class);

            if (entitypatch != null) {
                if (!entitypatch.getEntityState().setProjectileImpactResult(projectile, hitResult)) {
                    shouldCancel = true;
                }

                HitByProjectileEvent hitByProjectileEvent = new HitByProjectileEvent(entitypatch, hitResult, projectile);
                EpicFightEventHooks.Entity.HIT_BY_PROJECTILE.postWithListener(hitByProjectileEvent, entitypatch.getEventListener());

                if (hitByProjectileEvent.isCanceled()) {
                    shouldCancel = true;
                }
            }

            if (projectile.getOwner() != null) {
                // Epic Fight Modification: disable shooting riding entities
                if (rayResult.getEntity().equals(projectile.getOwner().getVehicle())) {
                    shouldCancel = true;
                }

                // NeoForge uses PartEntity.getParent(); in vanilla, EnderDragonPart exposes the parent via the parentMob field
                if (rayResult.getEntity() instanceof net.minecraft.world.entity.boss.EnderDragonPart enderDragonPart) {
                    Entity parent = enderDragonPart.parentMob;

                    if (projectile.getOwner().is(parent)) {
                        shouldCancel = true;
                    }
                }
            }

            if (EpicFightEntityTypes.DODGE_LOCATION_INDICATOR.get().equals(rayResult.getEntity().getType())) {
                ((MixinProjectile)projectile).invoke_onHitEntity(rayResult);
                shouldCancel = true;
            }
        }

        if (projectilepatch != null && !shouldCancel) {
            projectilepatch.setHit(true);
        }

        return shouldCancel;
    }

    /// Called when an item stack in a slot from an entity is changed
    ///
    /// @see LivingEntity#collectEquipmentChanges
    public static void onEquipmentChanged(LivingEntity entity, ItemStack from, ItemStack to, EquipmentSlot slot) {
        EpicFightCapabilities.getUnparameterizedEntityPatch(entity, HurtableEntityPatch.class).ifPresent(hurtableEntitypatch -> {
            hurtableEntitypatch.setDefaultStunReduction(slot, from, to);
        });

        LivingEntityPatch<?> entitypatch = EpicFightCapabilities.getEntityPatch(entity, LivingEntityPatch.class);
        CapabilityItem fromCap = EpicFightCapabilities.getItemStackCapability(from);
        CapabilityItem toCap = EpicFightCapabilities.getItemStackCapability(to);

        if (slot != EquipmentSlot.OFFHAND) {
            if (!fromCap.isEmpty()) {
                entity.getAttributes().removeAttributeModifiers(fromCap.getAttributeModifiers(entitypatch));
            }

            if (!toCap.isEmpty()) {
                entity.getAttributes().addTransientAttributeModifiers(toCap.getAttributeModifiers(entitypatch));
            }
        }

        if (entitypatch != null) {
            if (slot.getType() == EquipmentSlot.Type.HAND) {
                InteractionHand hand = slot == EquipmentSlot.MAINHAND ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
                entitypatch.updateHeldItem(fromCap, toCap, from, to, hand);
            } else if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
                boolean isFromItemArmor = fromCap instanceof ArmorCapability;
                boolean isToItemArmor = toCap instanceof ArmorCapability;

                if (isFromItemArmor || isToItemArmor) {
                    entitypatch.updateArmor(isFromItemArmor ? (ArmorCapability)fromCap : null, isToItemArmor ? (ArmorCapability)toCap : null, slot);
                }
            }
        }
    }

    /// Called when sizing a hitbox of an entity
    ///
    /// @see Entity#refreshDimensions
    public static void onSizingEntity(Entity entity, EntityScaler scaler) {
        if (entity instanceof EnderDragon) {
            scaler.accept(EntityDimensions.scalable(5.0F, 3.0F));
        }
    }

    /// Called when a mob effect is applied to an entity
    ///
    /// @see LivingEntity#addEffect
    public static void onMobEffectAdded(MobEffectInstance mobEffectInstance, LivingEntity entity) {
        if (!entity.level().isClientSide()) {
            EpicFightNetworkManager.sendToAllPlayerTrackingThisEntity(new SPMobEffectControl(SPMobEffectControl.Action.ACTIVATE, mobEffectInstance.getEffect(), entity.getId()), entity);
        }
    }

    /// Called when a mob effect is removed from an entity
    ///
    /// @see LivingEntity#removeEffect
    /// @see LivingEntity#onEffectRemoved
    public static void onMobEffectRemoved(MobEffectInstance mobEffectInstance, LivingEntity entity) {
        if (!entity.level().isClientSide() && mobEffectInstance != null) {
            EpicFightNetworkManager.sendToAllPlayerTrackingThisEntity(new SPMobEffectControl(SPMobEffectControl.Action.REMOVE, mobEffectInstance.getEffect(), entity.getId()), entity);
        }
    }

    /// Called when a mob effect is removed from an entity, due to the duration expiration
    ///
    /// @see LivingEntity#tickEffects
    public static void onMobEffectExpired(MobEffectInstance mobEffectInstance, LivingEntity entity) {
        if (!entity.level().isClientSide()) {
            EpicFightNetworkManager.sendToAllPlayerTrackingThisEntity(new SPMobEffectControl(SPMobEffectControl.Action.REMOVE, mobEffectInstance.getEffect(), entity.getId()), entity);
        }
    }

    /// Called when an entity mount or dismount another entity
    ///
    /// @see Entity#startRiding
    /// @see Entity#removeVehicle
    public static void onEntityMount(Entity mounting, Entity beingMounted, boolean isMountingOrDismounting) {
        EpicFightCapabilities.getUnparameterizedEntityPatch(mounting, HumanoidMobPatch.class).ifPresent(humanoidMobPatch -> {
            if (!mounting.level().isClientSide()) {
                if (beingMounted instanceof Mob) {
                    humanoidMobPatch.onMount(isMountingOrDismounting, beingMounted);
                }
            }
        });
    }

    /// Called when an entity jumps
    ///
    /// @see LivingEntity#jumpFromGround
    public static void onJump(LivingEntity entity) {
        EpicFightCapabilities.<LivingEntity, LivingEntityPatch<LivingEntity>>getParameterizedEntityPatch(entity, LivingEntity.class, LivingEntityPatch.class).ifPresent(entitypatch -> {
            if (entitypatch.isLogicalClient()) {
                if (!entitypatch.getEntityState().inaction() && !entity.isInWater()) {
                    AssetAccessor<? extends StaticAnimation> jumpAnimation = entitypatch.getClientAnimator().getJumpAnimation();
                    entitypatch.playAnimationInClientSide(jumpAnimation, 0.0F);
                }
            }
        });
    }

    /// Called when EnderMan spawns, on the main island of Ender world
    ///
    /// @return whether cancel the event
    public static boolean onEnderManSapwns(EnderMan enderMan) {
        if (enderMan.level().dimension() == Level.END) {
            return EpicFightGameRules.NO_MOBS_IN_BOSSFIGHT.getRuleValue(enderMan.level()) && enderMan.position().horizontalDistanceSqr() < 40000;
        }

        return false;
    }

    /// Called when Enderman tries to teleport
    ///
    /// @see EnderMan#teleport
    ///
    /// @return whether cancel the event
    public static boolean onEndermanTeleports(EnderMan enderMan) {
        EndermanPatch endermanPatch = EpicFightCapabilities.getEntityPatch(enderMan, EndermanPatch.class);

        if (endermanPatch != null) {
            if (endermanPatch.getEntityState().inaction()) {
                for (Entity collideEntity : endermanPatch.getOriginal().level().getEntitiesOfClass(Entity.class, endermanPatch.getOriginal().getBoundingBox().inflate(0.2D, 0.2D, 0.2D))) {
                    if (collideEntity instanceof Projectile) {
                        return false;
                    }
                }

                return true;
            } else {
                return endermanPatch.isRaging();
            }
        }

        return false;
    }

    @FunctionalInterface
    public interface EntityScaler {
        void accept(EntityDimensions entityDimensions);
    }

    private VanillaEntityEventHooks() {}
}
