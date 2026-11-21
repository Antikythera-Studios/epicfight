package yesman.epicfight.world.capabilities.entitypatch;

import com.mojang.datafixers.util.Pair;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.*;
import yesman.epicfight.api.animation.Pose;
import yesman.epicfight.api.animation.types.*;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.client.animation.ClientAnimator;
import yesman.epicfight.api.collider.Collider;
import yesman.epicfight.api.event.EntityEventListener;
import yesman.epicfight.api.event.EpicFightEventHooks;
import yesman.epicfight.api.event.IdentifierProvider;
import yesman.epicfight.api.event.types.entity.*;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.api.utils.AttackResult;
import yesman.epicfight.api.utils.AttackResult.ResultType;
import yesman.epicfight.api.utils.EntitySnapshot;
import yesman.epicfight.api.utils.math.*;
import yesman.epicfight.api.utils.side.ClientOnly;
import yesman.epicfight.client.renderer.EpicFightRenderTypes;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.gameasset.Armatures;
import yesman.epicfight.main.EpicFightMod;
import yesman.epicfight.main.EpicFightSharedConstants;
import yesman.epicfight.mixin.common.MixinMob;
import yesman.epicfight.mixin.common.MixinPlayer;
import yesman.epicfight.model.armature.types.ToolHolderArmature;
import yesman.epicfight.network.EntityPairingPacketTypes;
import yesman.epicfight.network.EpicFightNetworkManager;
import yesman.epicfight.network.common.AbstractAnimatorControl;
import yesman.epicfight.network.server.SPAnimatorControl;
import yesman.epicfight.network.server.SPEntityPairingPacket;
import yesman.epicfight.particle.HitParticleType;
import yesman.epicfight.registry.entries.EpicFightAttributes;
import yesman.epicfight.registry.entries.EpicFightExpandedEntityDataAccessors;
import yesman.epicfight.registry.entries.EpicFightParticles;
import yesman.epicfight.registry.entries.EpicFightSounds;
import yesman.epicfight.skill.passive.BonebreakerSkill;
import yesman.epicfight.skill.passive.StaminaPillagerSkill;
import yesman.epicfight.skill.passive.VengeanceSkill;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.EntityDecorations.DecorationOverlay;
import yesman.epicfight.world.capabilities.entitypatch.EntityDecorations.RenderAttributeModifier;
import yesman.epicfight.world.capabilities.item.ArmorCapability;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.damagesource.EpicFightDamageSource;
import yesman.epicfight.world.damagesource.EpicFightDamageSources;
import yesman.epicfight.world.damagesource.StunType;
import yesman.epicfight.world.entity.data.ExpandedSyncedData;
import yesman.epicfight.world.gamerule.EpicFightGameRules;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class LivingEntityPatch<T extends LivingEntity> extends HurtableEntityPatch<T> {
    public static final double WEIGHT_CORRECTION = 37.037D;

    protected Armature armature;
    protected Animator animator;
    protected EntityState state = EntityState.DEFAULT_STATE;

    protected Vec3 lastAttackPosition;
    protected EpicFightDamageSource epicFightDamageSource;

    protected boolean isLastAttackSuccess;
    protected float lastDealDamage;
    protected ResultType lastAttackResultType;

    protected Entity lastTryHurtEntity;
    protected LivingEntity grapplingTarget;

    protected ExpandedSyncedData expandedSynchedData;

    public LivingMotion currentLivingMotion = LivingMotions.IDLE;
    public LivingMotion currentCompositeMotion = LivingMotions.IDLE;

    protected final Map<InteractionHand, Joint> parentJointOfHands = new HashMap<> ();
    protected final EntityDecorations entityDecorations = new EntityDecorations();
    protected final EntityEventListener eventListener = new EntityEventListener(this);

    public LivingEntityPatch(T entity) {
        super(entity);

        if (!this.isFakeEntity()) {
            this.expandedSynchedData = new ExpandedSyncedData(this.original::getId, !this.isLogicalClient());
            this.registerExpandedEntityDataAccessors(this.expandedSynchedData);
        }
    }

    @Override
    public void onConstructed(T original) {
        this.armature = Armatures.getArmatureFor(this);

        Animator animator = EpicFightSharedConstants.getAnimator(this);
        this.animator = animator;

        this.initAnimator(animator);
        animator.postInit();
    }

    protected void registerExpandedEntityDataAccessors(final ExpandedSyncedData expandedSynchedData) {
        expandedSynchedData.register(EpicFightExpandedEntityDataAccessors.AIRBORNE);
        expandedSynchedData.register(EpicFightExpandedEntityDataAccessors.ASSASSINATION_RESISTANCE);
        expandedSynchedData.register(EpicFightExpandedEntityDataAccessors.STUN_SHIELD);
        expandedSynchedData.register(EpicFightExpandedEntityDataAccessors.MAX_STUN_SHIELD);
    }

    protected void initAnimator(Animator animator) {
        animator.getVariables().putSharedVariableWithDefault(AttackAnimation.ATTACK_TRIED_ENTITIES);
        animator.getVariables().putSharedVariableWithDefault(AttackAnimation.ACTUALLY_HIT_ENTITIES);
        animator.getVariables().putSharedVariableWithDefault(ActionAnimation.ACTION_ANIMATION_COORD);

        if (this.armature instanceof ToolHolderArmature toolArmature) {
            this.setParentJointOfHand(InteractionHand.MAIN_HAND, toolArmature.rightToolJoint());
            this.setParentJointOfHand(InteractionHand.OFF_HAND, toolArmature.leftToolJoint());
        }
    }

    @Override
    public void onJoinWorld(T entity, Level level, boolean worldgenSpawn) {
        super.onJoinWorld(entity, level, worldgenSpawn);

        if (entity.getAttributeBaseValue(EpicFightAttributes.WEIGHT) == 0.0D) {
            EntityDimensions entityDimensions = entity.getDimensions(net.minecraft.world.entity.Pose.STANDING);
            double weight = entityDimensions.width() * entityDimensions.height() * WEIGHT_CORRECTION;
            entity.getAttribute(EpicFightAttributes.WEIGHT).setBaseValue(weight);
        }
    }

    public abstract void updateMotion(boolean considerInaction);

    public Armature getArmature() {
        return this.armature;
    }

    @Override
    public void preTick() {
        super.preTick();

        if (this.original.getHealth() <= 0.0F) {
            this.original.setXRot(0);

            AnimationPlayer animPlayer = this.getAnimator().getPlayerFor(null);

            if (this.original.deathTime >= 19 && !animPlayer.isEmpty() && !animPlayer.isEnd()) {
                this.original.deathTime--;
            }
        }

        this.animator.tick();
        this.entityDecorations.tick();

        if (!this.getEntityState().inaction() && this.original.onGround() && this.isAirborneState()) {
            this.setAirborneState(false);
        }
    }

    public void poseTick(DynamicAnimation animation, Pose pose, float elapsedTime, float partialTick) {
        if (pose.hasTransform("Head") && this.armature.hasJoint("Head")) {
            if (animation.doesHeadRotFollowEntityHead()) {
                float headRelativeRot = Mth.rotLerp(partialTick, Mth.wrapDegrees(this.original.yBodyRotO - this.original.yHeadRotO), Mth.wrapDegrees(this.original.yBodyRot - this.original.yHeadRot));
                // See AbstractClientPlayerPatch.poseTick: PoseMirror now preserves the Head's
                // frontResult, so the yaw injected below survives the mirror untouched and we no
                // longer pre-negate under isMirrorMode (which used to snap the head at the
                // moment the F-swap flipped mirror state).
                OpenMatrix4f toOriginalRotation = new OpenMatrix4f(this.armature.getBoundTransformFor(pose, this.armature.searchJointByName("Head"))).removeScale().removeTranslation().invert();
                Vec3f xAxis = OpenMatrix4f.transform3v(toOriginalRotation, Vec3f.X_AXIS, null);
                Vec3f yAxis = OpenMatrix4f.transform3v(toOriginalRotation, Vec3f.Y_AXIS, null);
                OpenMatrix4f headRotation = OpenMatrix4f.createRotatorDeg(headRelativeRot, yAxis).rotateDeg(-Mth.rotLerp(partialTick, this.original.xRotO, this.original.getXRot()), xAxis);
                pose.orElseEmpty("Head").frontResult(JointTransform.fromMatrix(headRotation), OpenMatrix4f::mul);
            }
        }
    }

    @Override
    public void preTickClient() {
        super.preTickClient();
        this.tickMirrorBlend();
    }

    @Override @ClientOnly
    public void entityPairing(SPEntityPairingPacket packet) {
        super.entityPairing(packet);

        if (packet.pairingPacketType().is(EntityPairingPacketTypes.class)) {
            switch (packet.pairingPacketType().toEnum(EntityPairingPacketTypes.class)) {
            case BONEBREAKER_BEGIN -> {
                this.entityDecorations.addDecorationOverlay(BonebreakerSkill.CRACKINESS, new DecorationOverlay() {
                    static final ResourceLocation TEXTURE = EpicFightMod.identifier("textures/entity/overlay/crack_level1.png");

                    @Override
                    public RenderType getRenderType() {
                        return EpicFightRenderTypes.overlayModel(TEXTURE);
                    }
                });
            }
            case BONEBREAKER_MAX_STACK -> {
                getLevel().playLocalSound(getOriginal(), EpicFightSounds.OLD_FALL.get(), SoundSource.MASTER, 50.0F, 1.0F);

                this.entityDecorations.addDecorationOverlay(BonebreakerSkill.CRACKINESS, new DecorationOverlay() {
                    static final ResourceLocation TEXTURE = EpicFightMod.identifier("textures/entity/overlay/crack_level2.png");

                    @Override
                    public RenderType getRenderType() {
                        return EpicFightRenderTypes.overlayModel(TEXTURE);
                    }
                });
            }
            case BONEBREAKER_CLEAR -> {
                this.entityDecorations.removeDecorationOverlay(BonebreakerSkill.CRACKINESS);
            }
            case STAMINA_PILLAGER_BODY_ASHES -> {
                this.entityDecorations.addColorModifier(StaminaPillagerSkill.ASHEN_DECORATIONS, (resultColor, partialTick) -> {
                    float rotProgression = Mth.clamp(1.0F - ((LivingEntityPatch.this.original.deathTime + partialTick) / 16), 0.0F, 1.0F);
                    float color = Mth.clampedLerp(0.28F, 1.0F, rotProgression * rotProgression);
                    resultColor.x = color;
                    resultColor.y = color;
                    resultColor.z = color;
                });

                this.entityDecorations.addOverlayCoordModifier(StaminaPillagerSkill.ASHEN_DECORATIONS, (resultOverlay, partialTick) -> {
                    resultOverlay.x = OverlayTexture.NO_WHITE_U;
                    resultOverlay.y = OverlayTexture.WHITE_OVERLAY_V;
                });

                this.entityDecorations.addParticleGenerator(StaminaPillagerSkill.ASHEN_DECORATIONS, () -> {
                    OpenMatrix4f boundRootTransform = LivingEntityPatch.this.armature.getBoundTransformFor(LivingEntityPatch.this.animator.getPose(1.0F), LivingEntityPatch.this.armature.rootJoint);
                    Vec3f boundRootPos = boundRootTransform.toTranslationVector().add((float)LivingEntityPatch.this.getOriginal().getX(), (float)LivingEntityPatch.this.getOriginal().getY(), (float)LivingEntityPatch.this.getOriginal().getZ());
                    RandomSource random = LivingEntityPatch.this.original.getRandom();
                    Vec3 lookVec = LivingEntityPatch.this.original.getLookAngle().scale(0.1D);

                    for (int i = 0; i < 3; i++) {
                        LivingEntityPatch.this.original.level().addParticle(
                            EpicFightParticles.ASH_DIRECTIONAL.get(),
                            boundRootPos.x + random.nextGaussian() * 0.4F,
                            boundRootPos.y + random.nextGaussian() * 0.6F,
                            boundRootPos.z + random.nextGaussian() * 0.4F,
                            lookVec.x,
                            0.1F,
                            lookVec.z
                        );
                    }

                    return this.original.isRemoved();
                });
            }
            case FLASH_WHITE -> {
                int durationTick = packet.buffer().readInt();
                int maxOverlay = packet.buffer().readInt();
                int maxBrightness = packet.buffer().readInt();
                boolean disableRed = packet.buffer().readBoolean();

                this.entityDecorations.addOverlayCoordModifier(IdentifierProvider.constant("flashing_white"), new RenderAttributeModifier<> () {
                    private int tickCount;

                    @Override
                    public void modifyValue(Vec2i value, float partialTick) {
                        float f = Mth.sin((this.tickCount + partialTick) / (durationTick + 1.0F) * (float)Math.PI) * maxOverlay;
                        value.x = (int)f;

                        if (disableRed) {
                            value.y = OverlayTexture.WHITE_OVERLAY_V;
                        }
                    }

                    @Override
                    public boolean tick() {
                        return this.tickCount++ > durationTick;
                    }
                });

                this.entityDecorations.addLightModifier(IdentifierProvider.constant("flashing_white"), new RenderAttributeModifier<> () {
                    private int tickCount;

                    @Override
                    public void modifyValue(Vec2i value, float partialTick) {
                        float f = Mth.sin((this.tickCount + partialTick) / (durationTick + 1.0F) * (float)Math.PI) * maxBrightness;
                        value.x += (int)f;
                    }

                    @Override
                    public boolean tick() {
                        return this.tickCount++ > durationTick;
                    }
                });
            }
            case VENGEANCE_OVERLAY -> {
                this.entityDecorations.addColorModifier(VengeanceSkill.TARGET, (color, partialTick) -> {
                    color.x = 1.0F;
                    color.y = 0.5F;
                    color.z = 0.5F;
                });
            }
            case VENGEANCE_TARGET_CANCEL -> {
                this.entityDecorations.removeColorModifier(VengeanceSkill.TARGET);
            }
            default -> {}
            }
        }
    }

    public void onFall(float distance, float multiplier) {
        FallEvent fallEvent = new FallEvent(this, distance, multiplier);
        EpicFightEventHooks.Entity.ON_FALL.postWithListener(fallEvent, this.eventListener);

        if (!this.getOriginal().level().isClientSide() && this.isAirborneState() && fallEvent.doesPlayFallAnimation() && EpicFightGameRules.HAS_FALL_ANIMATION.getRuleValue(this.getLevel())) {
            AssetAccessor<? extends StaticAnimation> fallAnimation = this.getAnimator().getLivingAnimation(LivingMotions.LANDING_RECOVERY, this.getHitAnimation(StunType.FALL));

            if (fallAnimation != null) {
                this.playAnimationSynchronized(fallAnimation, 0);
            }
        }

        this.setAirborneState(false);
    }

    public void onDeath(DamageSource damageSource) {
        this.getAnimator().playDeathAnimation();
        this.currentLivingMotion = LivingMotions.DEATH;
    }

    public void updateEntityState() {
        this.state = this.animator.getEntityState();
    }

    public void updateEntityState(EntityState entityState) {
        this.state = entityState;
    }

    public void cancelItemUse() {
        if (this.original.isUsingItem()) {
            this.original.stopUsingItem();
            EventHooks.onUseItemStop(this.original, this.original.getUseItem(), this.original.getUseItemRemainingTicks());
        }
    }

    public CapabilityItem getHoldingItemCapability(InteractionHand hand) {
        return EpicFightCapabilities.getItemStackCapability(this.original.getItemInHand(hand));
    }

    /**
     * Returns an empty capability if the item in mainhand is incompatible with the item in offhand
     */
    public CapabilityItem getAdvancedHoldingItemCapability(InteractionHand hand) {
        if (hand == InteractionHand.MAIN_HAND) {
            return this.getHoldingItemCapability(hand);
        } else {
            return this.isOffhandItemValid() ? this.getHoldingItemCapability(hand) : CapabilityItem.EMPTY;
        }
    }

    public ItemStack getAdvancedHoldingItemStack(InteractionHand hand) {
        if (hand == InteractionHand.MAIN_HAND) {
            return this.original.getItemInHand(hand);
        } else {
            return this.isOffhandItemValid() ? this.original.getItemInHand(hand) : ItemStack.EMPTY;
        }
    }

    public EpicFightDamageSource getDamageSource(AnimationAccessor<? extends StaticAnimation> animation, InteractionHand hand) {
        return EpicFightDamageSources
                .mobAttack(this.original)
                .setAnimation(animation)
                .setBaseArmorNegation(this.getArmorNegation(hand))
                .setBaseImpact(this.getImpact(hand))
                .setUsedItem(this.original.getItemInHand(hand));
    }

    public AttackResult tryHurt(DamageSource damageSource, float amount) {
        TakeDamageEvent.Income takeDamageEvent$income = new TakeDamageEvent.Income(this, damageSource, amount);
        EpicFightEventHooks.Entity.TAKE_DAMAGE_INCOME.postWithListener(takeDamageEvent$income, this.getEventListener());

        if (takeDamageEvent$income.isCanceled()) {
            return new AttackResult(ResultType.MISSED, takeDamageEvent$income.getDamage());
        } else {
            return AttackResult.of(this.getEntityState().attackResult(damageSource), amount);
        }
    }

    public AttackResult tryHarm(Entity target, EpicFightDamageSource damagesource, float amount) {
        LivingEntityPatch<?> entitypatch = EpicFightCapabilities.getEntityPatch(target, LivingEntityPatch.class);
        return (entitypatch != null) ? entitypatch.tryHurt(damagesource, amount) : AttackResult.success(amount);
    }

    /**
     * Since 20.12.1, There's no need to call epicfight damage source manually since vanilla damage sources are replaced by mixin {@link MixinPlayer}, {@link MixinMob}
     */
    @Nullable
    @ApiStatus.Internal
    public EpicFightDamageSource getEpicFightDamageSource() {
        return this.epicFightDamageSource;
    }

    /**
     * Swap item and attributes of mainhand for offhand item and attributes
     * You must call {@link LivingEntityPatch#recoverMainhandDamage} method again after finishing the damaging process.
     */
    protected void setOffhandDamage(InteractionHand hand, ItemStack mainhandItemStack, ItemStack offhandItemStack, boolean offhandValid, Collection<AttributeModifier> mainhandAttributes, Collection<AttributeModifier> offhandAttributes) {
        if (hand == InteractionHand.MAIN_HAND) {
            return;
        }

        // Swap hand items to decrease the durability of offhand item
        this.getOriginal().setItemInHand(InteractionHand.MAIN_HAND, offhandValid ? offhandItemStack : ItemStack.EMPTY);
        this.getOriginal().setItemInHand(InteractionHand.OFF_HAND, mainhandItemStack);

        // Swap item's attributes before {@link LivingEntity#setItemInHand} called
        AttributeInstance damageAttributeInstance = this.original.getAttribute(Attributes.ATTACK_DAMAGE);
        mainhandAttributes.forEach(damageAttributeInstance::removeModifier);
        offhandAttributes.forEach(damageAttributeInstance::addTransientModifier);
    }

    /**
     * Set mainhand item's attribute modifiers
     */
    protected void recoverMainhandDamage(InteractionHand hand, ItemStack mainhandItemStack, ItemStack offhandItemStack, Collection<AttributeModifier> mainhandAttributes, Collection<AttributeModifier> offhandAttributes) {
        if (hand == InteractionHand.MAIN_HAND) {
            return;
        }

        this.getOriginal().setItemInHand(InteractionHand.MAIN_HAND, mainhandItemStack);
        this.getOriginal().setItemInHand(InteractionHand.OFF_HAND, offhandItemStack);

        AttributeInstance damageAttributeInstance = this.original.getAttribute(Attributes.ATTACK_DAMAGE);
        offhandAttributes.forEach(damageAttributeInstance::removeModifier);
        mainhandAttributes.forEach(damageAttributeInstance::addTransientModifier);
    }

    public void setLastAttackResult(AttackResult attackResult) {
        this.lastAttackResultType = attackResult.resultType;
        this.lastDealDamage = attackResult.damage;
    }

    public void setLastAttackEntity(Entity tryHurtEntity) {
        this.lastTryHurtEntity = tryHurtEntity;
    }

    protected boolean checkLastAttackSuccess(Entity target) {
        boolean success = target.is(this.lastTryHurtEntity);
        this.lastTryHurtEntity = null;

        if (success && !this.isLastAttackSuccess) {
            this.setLastAttackSuccess(true);
        }

        return success;
    }

    public AttackResult attack(EpicFightDamageSource damageSource, Entity target, InteractionHand hand) {
        return this.checkLastAttackSuccess(target) ? new AttackResult(this.lastAttackResultType, this.lastDealDamage) : AttackResult.missed(0.0F);
    }

    public float getModifiedBaseDamage(float baseDamage) {
        ModifyBaseDamageEvent event = new ModifyBaseDamageEvent(this, baseDamage, ValueModifier.calculator());
        EpicFightEventHooks.Entity.MODIFY_ATTACK_DAMAGE.postWithListener(event, this.getEventListener());
        return event.calculateModifiedDamage();
    }

    public float getAttackSpeed(InteractionHand hand) {
        // Return default attack speed when attack speed is not registered
        if (!this.original.getAttributes().hasAttribute(Attributes.ATTACK_SPEED)) {
            return 1.0F;
        }

        float baseItemSpeed;

        if (hand == InteractionHand.MAIN_HAND) {
            baseItemSpeed = (float)this.original.getAttributeValue(Attributes.ATTACK_SPEED);
        } else {
            baseItemSpeed = (float)(this.isOffhandItemValid() ? this.original.getAttributeValue(EpicFightAttributes.OFFHAND_ATTACK_SPEED) : this.original.getAttributeBaseValue(Attributes.ATTACK_SPEED));
        }

        return this.getModifiedAttackSpeedOfItem(this.getAdvancedHoldingItemCapability(hand), baseItemSpeed);
    }

    public float getModifiedAttackSpeedOfItem(CapabilityItem itemCapability, float baseSpeed) {
        ModifyAttackSpeedEvent event = new ModifyAttackSpeedEvent(this, itemCapability, baseSpeed);
        EpicFightEventHooks.Entity.MODIFY_ATTACK_SPEED.postWithListener(event, this.getEventListener());

        float weight = this.getWeight();

        if (weight > 40.0F) {
            float attenuation = Mth.clamp(EpicFightGameRules.WEIGHT_PENALTY.getRuleValue(this.getOriginal().level()), 0, 100) / 100.0F;
            return event.getAttackSpeed() + (-0.1F * (weight / 40.0F) * Math.max(event.getAttackSpeed() - 0.8F, 0.0F) * attenuation);
        } else {
            return event.getAttackSpeed();
        }
    }

    public boolean onDrop(DamageSource source, Collection<ItemEntity> drops) {
        return false;
    }

    @Override
    public float getStunShield() {
        return this.expandedSynchedData.get(EpicFightExpandedEntityDataAccessors.STUN_SHIELD);
    }

    @Override
    public void setStunShield(float value) {
        float clamped = Mth.clamp(value, 0.0F, this.getMaxStunShield());
        this.expandedSynchedData.set(EpicFightExpandedEntityDataAccessors.STUN_SHIELD, clamped);
    }

    public float getMaxStunShield() {
        return this.expandedSynchedData.get(EpicFightExpandedEntityDataAccessors.MAX_STUN_SHIELD);
    }

    public void setMaxStunShield(float value) {
        float maximized = Math.max(value, 0.0F);
        this.expandedSynchedData.set(EpicFightExpandedEntityDataAccessors.MAX_STUN_SHIELD, maximized);
    }

    public int getAssassinationResistance() {
        return this.expandedSynchedData.get(EpicFightExpandedEntityDataAccessors.ASSASSINATION_RESISTANCE);
    }

    public void setExecutionResistance(int value) {
        int maxExecutionResistance = (int)this.original.getAttributeValue(EpicFightAttributes.ASSASSINATION_RESISTANCE);
        int minimized = Math.min(maxExecutionResistance, value);
        this.expandedSynchedData.set(EpicFightExpandedEntityDataAccessors.ASSASSINATION_RESISTANCE, minimized);
    }

    @Override
    public float getWeight() {
        return (float)this.original.getAttributeValue(EpicFightAttributes.WEIGHT);
    }

    public void rotateTo(float degree, float limit, boolean syncPrevRot) {
        LivingEntity entity = this.getOriginal();
        float yRot = Mth.wrapDegrees(entity.getYRot());
        float amount = Mth.clamp(Mth.wrapDegrees(degree - yRot), -limit, limit);
        float f1 = yRot + amount;

        if (syncPrevRot) {
            entity.yRotO = f1;
            entity.yHeadRotO = f1;
            entity.yBodyRotO = f1;
        }

        entity.setYRot(f1);
        entity.yHeadRot = f1;
        entity.yBodyRot = f1;
    }

    public void rotateTo(@NotNull Entity target, float limit, boolean syncPrevRot) {
        Vec3 playerPosition = this.original.position();
        Vec3 targetPosition = target.position();
        float yaw = (float)MathUtils.getYRotOfVector(targetPosition.subtract(playerPosition));
        this.rotateTo(yaw, limit, syncPrevRot);
    }

    public float getYRotDeltaTo(@NotNull Entity target) {
        Vec3 playerPosition = this.getOriginal().position();
        Vec3 targetPosition = target.position();
        float yRotToTarget = (float)MathUtils.getYRotOfVector(targetPosition.subtract(playerPosition));
        float yRotCurrent = Mth.wrapDegrees(this.getOriginal().getYRot());

        return Mth.clamp(Mth.wrapDegrees(yRotToTarget - yRotCurrent), -this.getYRotLimit(), this.getYRotLimit());
    }

    public LivingEntity getTarget() {
        return this.original.getLastHurtMob();
    }

    public float getAttackDirectionPitch(float partialTick) {
        float partialTicks = EpicFightSharedConstants.isPhysicalClient() ? partialTick : 1.0F;
        float pitch = -this.getOriginal().getViewXRot(partialTicks);
        float correct = (pitch > 0) ? 0.03333F * (float)Math.pow(pitch, 2) : -0.03333F * (float)Math.pow(pitch, 2);

        return Mth.clamp(correct, -30.0F, 30.0F);
    }

    @Override
    public OpenMatrix4f getModelMatrix(float partialTicks) {
        float yRotO;
        float yRot;
        float scale = this.original.isBaby() ? 0.5F : 1.0F;

        if (this.original.getVehicle() instanceof LivingEntity ridingEntity) {
            yRotO = ridingEntity.yBodyRotO;
            yRot = ridingEntity.yBodyRot;
        } else {
            yRotO = this.isLogicalClient() ? this.original.yBodyRotO : this.original.getYRot();
            yRot = this.isLogicalClient() ? this.original.yBodyRot : this.original.getYRot();
        }

        return MathUtils.getModelMatrixIntegral(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, yRotO, yRot, partialTicks, scale, scale, scale);
    }

    /**
     * Play an animation
     * This method doesn't synchronize animations between client and server
     * Use {@link #playAnimationSynchronized} instead if you want to synchronize animations to every remote player
     */
    public void playAnimation(AssetAccessor<? extends StaticAnimation> animation, float transitionTimeModifier) {
        this.animator.playAnimation(animation, transitionTimeModifier);
    }

    /**
     * Play an animation without convert time
     */
    public void playAnimationInstantly(AssetAccessor<? extends StaticAnimation> animation) {
        this.animator.playAnimationInstantly(animation);

        if (!this.isLogicalClient()) {
            this.handleAnimationPayload(new SPAnimatorControl(AbstractAnimatorControl.Action.PLAY_INSTANTLY, animation, this, 0.0F));
        }
    }

    /**
     * Reserve an animation to be played after the current animation
     */
    public void reserveAnimation(AssetAccessor<? extends StaticAnimation> animation) {
        this.animator.reserveAnimation(animation);

        if (!this.isLogicalClient()) {
            this.handleAnimationPayload(new SPAnimatorControl(AbstractAnimatorControl.Action.RESERVE, animation, this, 0.0F));
        }
    }

    /**
     * Stop playing an animation
     */
    public void stopPlaying(AssetAccessor<? extends StaticAnimation> animation) {
        this.animator.stopPlaying(animation);

        if (!this.isLogicalClient()) {
            this.handleAnimationPayload(new SPAnimatorControl(AbstractAnimatorControl.Action.STOP, animation, this, -1.0F));
        }
    }

    /**
     * Play an animation ensuring synchronization between client-server
     * Plays animation when getting response from server if it called in client side.
     * Do not call this in client side for non-player entities.
     */
    public void playAnimationSynchronized(AssetAccessor<? extends StaticAnimation> animation, float transitionTimeModifier) {
        this.animator.playAnimation(animation, transitionTimeModifier);

        if (!this.isLogicalClient()) {
            this.handleAnimationPayload(new SPAnimatorControl(AbstractAnimatorControl.Action.PLAY, animation, this, transitionTimeModifier));
        }
    }

    /**
     * Play an animation only in client side, including all clients tracking this entity
     */
    public void playAnimationInClientSide(AssetAccessor<? extends StaticAnimation> animation, float transitionTimeModifier) {
        if (this.isLogicalClient()) {
            this.animator.playAnimation(animation, transitionTimeModifier);
        } else {
            this.sendToAllPlayersTrackingMe(new SPAnimatorControl(AbstractAnimatorControl.Action.PLAY, animation, this.original.getId(), transitionTimeModifier, false));
        }
    }

    /**
     * Play a shooting animation to end aim pose
     * Synchronized if the method is called in server side
     */
    public void playShootingAnimation() {
        this.animator.playShootingAnimation();

        if (!this.isLogicalClient()) {
            this.sendToAllPlayersTrackingMe(new SPAnimatorControl(AbstractAnimatorControl.Action.SHOT, Animations.EMPTY_ANIMATION, this.original.getId(), 0.0F, false));
        }
    }

    /**
     * Play an animation with custom packet
     */
    private void handleAnimationPayload(SPAnimatorControl payload) {
        if (this.isLogicalClient()) {
            throw new IllegalStateException("Cannot send animation sync payload in client side.");
        }

        switch (payload.action()) {
        case SOFT_PAUSE, HARD_PAUSE -> {
            throw new UnsupportedOperationException("Only PLAY, PLAY_INSTANTLY, STOP and RESERVE are allowed");
        }
        default -> {
        }
        }

        if (payload.action().syncVariables()) {
            payload.animationVariables().addAll(this.getAnimator().getVariables().createPendingVariablesPayloads(payload.animation()));
        }

        this.sendToAllPlayersTrackingMe(payload);
    }

    /**
     * Pause an animator until it receives a proper order
     * @param action SOFT_PAUSE: resume when next animation plays
     * 				 HARD_PAUSE: resume when hard pause is set false
     * @param pause
     **/
    public void pauseAnimator(AbstractAnimatorControl.Action action, boolean pause) {
        switch (action) {
        case SOFT_PAUSE -> {
            this.animator.setSoftPause(pause);
        }
        case HARD_PAUSE -> {
            this.animator.setHardPause(pause);
        }
        default -> {
            throw new UnsupportedOperationException("Only SOFT_PAUSE and HARD_PAUSE are allowed");
        }
        }

        if (!this.isLogicalClient()) {
            this.sendToAllPlayersTrackingMe(new SPAnimatorControl(action, Animations.EMPTY_ANIMATION, this.original.getId(), 0.0F, pause));
        }
    }

    public void sendToAllPlayersTrackingMe(CustomPacketPayload packet, CustomPacketPayload... others) {
        if (!this.isLogicalClient()) {
            EpicFightNetworkManager.sendToAllPlayerTrackingThisEntity(packet, this.original, others);
        }
    }

    public void resetSize(EntityDimensions size) {
        EntityDimensions entitysize = this.original.dimensions;
        EntityDimensions entitysize1 = size;
        this.original.dimensions = entitysize1;

        if (entitysize1.width() < entitysize.width()) {
            double d0 = (double)entitysize1.width() / 2.0D;
            this.original.setBoundingBox(
                new AABB(
                      this.original.getX() - d0
                    , this.original.getY()
                    , this.original.getZ() - d0
                    , this.original.getX() + d0
                    , this.original.getY() + (double)entitysize1.height()
                    , this.original.getZ() + d0
                )
            );
        } else {
            AABB axisalignedbb = this.original.getBoundingBox();
            this.original.setBoundingBox(
                new AABB(
                      axisalignedbb.minX
                    , axisalignedbb.minY
                    , axisalignedbb.minZ
                    , axisalignedbb.minX + (double)entitysize1.width()
                    , axisalignedbb.minY + (double)entitysize1.height()
                    , axisalignedbb.minZ + (double)entitysize1.width()
                )
            );

            if (entitysize1.width() > entitysize.width() && !this.original.level().isClientSide()) {
                float f = entitysize.width() - entitysize1.width();
                this.original.move(MoverType.SELF, new Vec3(f, 0.0D, f));
            }
        }
    }

    @Override
    public boolean applyStun(StunType stunType, float stunTime) {
        this.original.xxa = 0.0F;
        this.original.yya = 0.0F;
        this.original.zza = 0.0F;
        this.original.setDeltaMovement(0.0D, 0.0D, 0.0D);
        this.cancelKnockback = true;

        AssetAccessor<? extends StaticAnimation> hitAnimation = this.getHitAnimation(stunType);

        if (hitAnimation != null) {
            ApplyStunEvent applyStunEvent = new ApplyStunEvent(this, stunType, hitAnimation, stunType.hasFixedStunTime() ? 0.0F : stunTime);
            EpicFightEventHooks.Entity.APPLY_STUN.postWithListener(applyStunEvent, this.getEventListener());

            this.playAnimationSynchronized(applyStunEvent.getStunAnimation(), applyStunEvent.getStunTime());
            return true;
        }

        return false;
    }

    public void beginAction(ActionAnimation animation) {
    }

    public void updateHeldItem(CapabilityItem fromCap, CapabilityItem toCap, ItemStack from, ItemStack to, InteractionHand hand) {
    }

    public void updateArmor(@Nullable ArmorCapability fromCap, @Nullable ArmorCapability toCap, EquipmentSlot slotType) {
        if (this.original.getAttributes().hasAttribute(EpicFightAttributes.STUN_ARMOR)) {
            if (fromCap != null) {
                this.original.getAttributes().removeAttributeModifiers(fromCap.getAttributeModifiersForArmor(this, slotType));
            }

            if (toCap != null) {
                this.original.getAttributes().addTransientAttributeModifiers(toCap.getAttributeModifiersForArmor(this, slotType));
            }
        }
    }

    /**
     * Fired when my attack is blocked
     * @param damageSource
     * @param blocker
     */
    public void onAttackBlocked(DamageSource damageSource, LivingEntityPatch<?> blocker) {
    }

    public void onStrike(AttackAnimation animation, InteractionHand hand) {
        this.getAdvancedHoldingItemCapability(hand).onStrike(this, animation);
    }

    public void onMount(boolean isMountOrDismount, Entity ridingEntity) {
    }

    public void notifyGrapplingWarning() {
    }

    public void onDodgeSuccess(DamageSource damageSource, Vec3 location) {
        DodgeEvent dodgeSuccessEvent = new DodgeEvent(this, damageSource, location);
        EpicFightEventHooks.Entity.ON_DODGE.postWithListener(dodgeSuccessEvent, this.getEventListener());
    }

    public void countHurtTime(float damageTaken) {
        this.original.lastHurt = damageTaken;
        this.original.invulnerableTime = 20;
        this.original.hurtDuration = 10;
        this.original.hurtTime = this.original.hurtDuration;
    }

    @Override
    public boolean isStunned() {
        return this.getEntityState().hurt();
    }

    @SuppressWarnings("unchecked")
    public <A extends Animator> A getAnimator() {
        return (A) this.animator;
    }

    @ClientOnly
    public ClientAnimator getClientAnimator() {
        return this.getAnimator();
    }

    public ServerAnimator getServerAnimator() {
        return this.getAnimator();
    }

    public abstract AssetAccessor<? extends StaticAnimation> getHitAnimation(StunType stunType);
    public void aboutToDeath() {}

    public SoundEvent getWeaponHitSound(InteractionHand hand) {
        return this.getAdvancedHoldingItemCapability(hand).getHitSound();
    }

    public SoundEvent getSwingSound(InteractionHand hand) {
        CapabilityItem itemCap = this.getAdvancedHoldingItemCapability(hand);
        return this.entityDecorations.getModifiedSwingSound(itemCap.getSmashingSound(), itemCap);
    }

    public HitParticleType getWeaponHitParticle(InteractionHand hand) {
        return this.getAdvancedHoldingItemCapability(hand).getHitParticle();
    }

    public Collider getColliderMatching(InteractionHand hand) {
        return this.getAdvancedHoldingItemCapability(hand).getWeaponCollider();
    }

    public int getMaxStrikes(InteractionHand hand) {
        return (int) (hand == InteractionHand.MAIN_HAND ?
            this.original.getAttributeValue(EpicFightAttributes.MAX_STRIKES) :
                this.isOffhandItemValid() ?
                    this.original.getAttributeValue(EpicFightAttributes.OFFHAND_MAX_STRIKES) :
                        this.original.getAttribute(EpicFightAttributes.MAX_STRIKES).getBaseValue());
    }

    public float getArmorNegation(InteractionHand hand) {
        return (float) (hand == InteractionHand.MAIN_HAND ?
            this.original.getAttributeValue(EpicFightAttributes.ARMOR_NEGATION) :
                this.isOffhandItemValid() ?
                    this.original.getAttributeValue(EpicFightAttributes.OFFHAND_ARMOR_NEGATION) :
                        this.original.getAttribute(EpicFightAttributes.ARMOR_NEGATION).getBaseValue());
    }

    public float getImpact(InteractionHand hand) {
        float impact;
        int i = 0;

        if (hand == InteractionHand.MAIN_HAND) {
            impact = (float)this.original.getAttributeValue(EpicFightAttributes.IMPACT);
            i = this.getOriginal().getMainHandItem().getEnchantmentLevel(this.getLevel().registryAccess().holderOrThrow(Enchantments.KNOCKBACK));
        } else {
            if (this.isOffhandItemValid()) {
                impact = (float)this.original.getAttributeValue(EpicFightAttributes.OFFHAND_IMPACT);
                i = this.getOriginal().getOffhandItem().getEnchantmentLevel(this.getLevel().registryAccess().holderOrThrow(Enchantments.KNOCKBACK));
            } else {
                impact = (float)this.original.getAttribute(EpicFightAttributes.IMPACT).getBaseValue();
            }
        }

        return impact * (1.0F + i * 0.12F);
    }

    public float getReach(InteractionHand hand) {
        return this.getAdvancedHoldingItemCapability(hand).getReach();
    }

    public ItemStack getValidItemInHand(InteractionHand hand) {
        if (hand == InteractionHand.MAIN_HAND) {
            return this.original.getItemInHand(hand);
        } else {
            return this.isOffhandItemValid() ? this.original.getItemInHand(hand) : ItemStack.EMPTY;
        }
    }

    /**
     * Persistent "offhand-only mirror" mode: true when the mainhand is bare (empty or fist
     * gloves) and the offhand carries a real one-handed weapon. While true:
     *
     * <ul>
     *   <li>{@link yesman.epicfight.api.client.animation.Layer#getEnabledPose} reflects the
     *       composed pose across X = 0 so the entity appears as a left-handed wielder.</li>
     *   <li>{@link yesman.epicfight.api.collider.Collider#updateAndSelectCollideEntity}
     *       remaps the queried joint to the opposite side so hits land where the visual
     *       weapon is.</li>
     *   <li>{@link #getPrimaryItemCapability()} / {@link #getPrimaryHand()} return the
     *       offhand cap / OFF_HAND, letting skill systems migrate to a single accessor that
     *       transparently picks up the active weapon.</li>
     * </ul>
     *
     * Derived purely from inventory (which vanilla syncs both ways), so server and every
     * tracking client compute the same value without extra network plumbing. Greatswords are
     * naturally excluded -- vanilla blocks them from the offhand slot via
     * {@code canBePlacedOffhand=false}.
     */
    public boolean isMirrorMode() {
        CapabilityItem mainhand = this.getHoldingItemCapability(InteractionHand.MAIN_HAND);
        boolean mainhandIsBare = mainhand.isEmpty()
                || mainhand.getWeaponCategory() == CapabilityItem.WeaponCategories.FIST;
        if (!mainhandIsBare) {
            return false;
        }
        CapabilityItem offhand = this.getHoldingItemCapability(InteractionHand.OFF_HAND);
        // Shields are not weapons -- they have authored offhand animations of their own (BIPED_BLOCK
        // routes to shield_offhand via MirrorAnimation). Triggering the global pose mirror on top of
        // that would double-flip the shield-side body pose into the mainhand-looking variant.
        return !offhand.isEmpty()
                && offhand.getWeaponCategory() != CapabilityItem.WeaponCategories.FIST
                && offhand.getWeaponCategory() != CapabilityItem.WeaponCategories.SHIELD;
    }

    /** Smoothed approximation of {@link #isMirrorMode()} for rendering: 0 = not mirrored,
     *  1 = fully mirrored, intermediate values during transitions. Without this, pressing F to
     *  swap mainhand/offhand snaps the entire pose across X = 0 in a single frame; with the
     *  blend, ClientAnimator.getPose interpolates between the unmirrored and mirrored poses
     *  over a handful of ticks so the transition is visible as a fold instead of a teleport. */
    private float mirrorBlend = 0.0F;
    private float mirrorBlendO = 0.0F;
    private static final float MIRROR_BLEND_STEP = 0.2F;

    /** Tick the blend toward whichever endpoint matches the live mirror state. Call from the
     *  client tick path -- the field is per-instance and the server patch doesn't render, so
     *  ticking it server-side would just be wasted work. */
    public void tickMirrorBlend() {
        this.mirrorBlendO = this.mirrorBlend;
        float target = this.isMirrorMode() ? 1.0F : 0.0F;

        if (this.mirrorBlend < target) {
            this.mirrorBlend = Math.min(target, this.mirrorBlend + MIRROR_BLEND_STEP);
        } else if (this.mirrorBlend > target) {
            this.mirrorBlend = Math.max(target, this.mirrorBlend - MIRROR_BLEND_STEP);
        }
    }

    public float getMirrorBlend(float partialTick) {
        return Mth.lerp(partialTick, this.mirrorBlendO, this.mirrorBlend);
    }

    /** Hand currently driving combat. {@link InteractionHand#OFF_HAND} when {@link #isMirrorMode()}
     *  reports true, {@link InteractionHand#MAIN_HAND} otherwise. */
    public InteractionHand getPrimaryHand() {
        return this.isMirrorMode() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
    }

    /** Capability of the active combat weapon. Equivalent to
     *  {@code getHoldingItemCapability(getPrimaryHand())} -- existing call sites that read
     *  {@code getHoldingItemCapability(MAIN_HAND)} can migrate here for transparent offhand-mode
     *  support without otherwise changing semantics. */
    public CapabilityItem getPrimaryItemCapability() {
        return this.getHoldingItemCapability(this.getPrimaryHand());
    }

    /** The mainhand wins when both hands carry items: the mainhand render is never suppressed by
     *  whatever is in the offhand. Suppression of the offhand item (when it conflicts with the
     *  mainhand or with the mainhand's natural two-handed style) is handled in
     *  {@link #isOffhandItemValid()}. */
    public boolean isMainhandItemValid() {
        return true;
    }

    public boolean isOffhandItemValid() {
        // When the mainhand is bare/fist-only, an offhand one-handed weapon is treated as valid
        // on its own so the renderer shows it and the universal mirrored-combo path in
        // ComboAttacks can fire. The original mainhand-driven check still runs for every other
        // case, so dual-wield pairs and opt-in companions (shield + sword, ...) keep their
        // existing semantics. Greatswords are unaffected: vanilla blocks them from the offhand
        // slot via canBePlacedOffhand=false.
        //
        // We deliberately do not call offhand.getStyle(this).canUseOffhand() here -- the
        // dual-pair style conditionals (DUAL_SWORDS, DUAL_DAGGERS, ...) only check "does offhand
        // hold category X?" without verifying a matching mainhand, so when queried from the
        // offhand cap with mainhand empty they misfire and report TWO_HAND. The category +
        // empty-mainhand check below is enough to determine validity for our purposes.
        CapabilityItem mainhand = this.getHoldingItemCapability(InteractionHand.MAIN_HAND);
        boolean mainhandIsBare = mainhand.isEmpty()
                || mainhand.getWeaponCategory() == CapabilityItem.WeaponCategories.FIST;

        if (mainhandIsBare) {
            CapabilityItem offhand = this.getHoldingItemCapability(InteractionHand.OFF_HAND);
            boolean offhandIsRealWeapon = !offhand.isEmpty()
                    && offhand.getWeaponCategory() != CapabilityItem.WeaponCategories.FIST;
            if (offhandIsRealWeapon) {
                return true;
            }
        } else {
            // Mainhand has a real item. If the offhand also carries a weapon whose natural single-wield
            // style is two-handed (katana, longsword, ...), the mainhand wins -- suppress the offhand
            // render so the two items don't fight side-by-side.
            CapabilityItem offhand = this.getHoldingItemCapability(InteractionHand.OFF_HAND);
            if (offhand instanceof yesman.epicfight.world.capabilities.item.WeaponCapability weaponCap) {
                yesman.epicfight.world.capabilities.item.Style natural = weaponCap.getNaturalSingleWieldStyle(this);
                if (natural != null && !natural.canUseOffhand()) {
                    return false;
                }
            }
        }

        return mainhand.checkOffhandValid(this);
    }

    public Joint getParentJointOfHand(InteractionHand hand) {
        return this.parentJointOfHands.getOrDefault(hand, this.armature.rootJoint);
    }

    public void setParentJointOfHand(InteractionHand hand, Joint joint) {
        this.parentJointOfHands.put(hand, joint);
    }

    public boolean isTargetInvulnerable(Entity target) {
        if (!target.isPickable() || target.isSpectator()) {
            return true;
        }

        if (this.original.getRootVehicle() == target.getRootVehicle() && !target.canRiderInteract()) {
            return true;
        }

        return this.original.isAlliedTo(target) && this.original.getTeam() != null && !this.original.getTeam().isAllowFriendlyFire();
    }

    public boolean canPush(Entity entity) {
        LivingEntityPatch<?> entitypatch = EpicFightCapabilities.getEntityPatch(entity, LivingEntityPatch.class);

        if (entitypatch != null) {
            EntityState state = entitypatch.getEntityState();

            if (state.inaction()) {
                return false;
            }
        }

        EntityState thisState = this.getEntityState();

        return !thisState.inaction() && !entity.is(this.grapplingTarget);
    }

    public LivingEntity getGrapplingTarget() {
        return this.grapplingTarget;
    }

    public void setGrapplingTarget(LivingEntity grapplingTarget) {
        this.grapplingTarget = grapplingTarget;
    }

    public Vec3 getLastAttackPosition() {
        return this.lastAttackPosition;
    }

    public void setLastAttackPosition() {
        this.lastAttackPosition = this.original.position();
    }

    public void setAirborneState(boolean flag) {
        this.expandedSynchedData.set(EpicFightExpandedEntityDataAccessors.AIRBORNE, flag);
    }

    public boolean isAirborneState() {
        return this.expandedSynchedData.get(EpicFightExpandedEntityDataAccessors.AIRBORNE);
    }

    public void setLastAttackSuccess(boolean setter) {
        this.isLastAttackSuccess = setter;
    }

    public boolean isLastAttackSuccess() {
        return this.isLastAttackSuccess;
    }

    public boolean shouldMoveOnCurrentSide(ActionAnimation actionAnimation) {
        return !this.isLogicalClient();
    }

    public boolean isFirstPerson() {
        return false;
    }

    @Override
    public boolean overrideRender() {
        return true;
    }

    public boolean shouldBlockMoving() {
        return false;
    }

    /**
     * Returns a value that the entity can trace a target in rotation by a tick
     * @return
     */
    public float getYRotLimit() {
        return 20.0F;
    }

    public double getXOld() {
        return this.original.xOld;
    }

    public double getYOld() {
        return this.original.yOld;
    }

    public double getZOld() {
        return this.original.zOld;
    }

    /**
     * Use this instead of {@link Entity#getYRot()} to get the y rotation especiall player's turning is locked
     * @return
     */
    public float getYRot() {
        return this.original.getYRot();
    }

    public float getYRotO() {
        return this.original.yRotO;
    }

    public void setYRot(float yRot) {
        this.original.setYRot(yRot);

        if (this.isLogicalClient()) {
            this.original.yBodyRot = yRot;
            this.original.yHeadRot = yRot;
        }
    }

    public void setYRotO(float yRot) {
        this.original.yRotO = yRot;

        if (this.isLogicalClient()) {
            this.original.yBodyRotO = yRot;
            this.original.yHeadRotO = yRot;
        }
    }

    @Override
    public EntityState getEntityState() {
        return this.state;
    }

    public InteractionHand getAttackingHand() {
        Pair<AnimationPlayer, AttackAnimation> layerInfo = this.getAnimator().findFor(AttackAnimation.class);

        if (layerInfo != null) {
            return layerInfo.getSecond().getPhaseByTime(layerInfo.getFirst().getElapsedTime()).hand;
        }
        return null;
    }

    public LivingMotion getCurrentLivingMotion() {
        return this.currentLivingMotion;
    }

    public List<Entity> getCurrentlyAttackTriedEntities() {
        return this.getAnimator().getVariables().getOrDefaultSharedVariable(AttackAnimation.ATTACK_TRIED_ENTITIES);
    }

    public List<LivingEntity> getCurrentlyActuallyHitEntities() {
        return this.getAnimator().getVariables().getOrDefaultSharedVariable(AttackAnimation.ACTUALLY_HIT_ENTITIES);
    }

    public void removeHurtEntities() {
        this.getAnimator().getVariables().getOrDefaultSharedVariable(AttackAnimation.ATTACK_TRIED_ENTITIES).clear();
        this.getAnimator().getVariables().getOrDefaultSharedVariable(AttackAnimation.ACTUALLY_HIT_ENTITIES).clear();
    }

    public abstract Faction getFaction();

    public final ExpandedSyncedData getExpandedSynchedData() {
        return this.expandedSynchedData;
    }

    public final EntityDecorations getEntityDecorations() {
        return this.entityDecorations;
    }

    public final EntityEventListener getEventListener() {
        return this.eventListener;
    }

    @ClientOnly
    public EntitySnapshot<?> captureEntitySnapshot() {
        return EntitySnapshot.captureLivingEntity(this);
    }
}