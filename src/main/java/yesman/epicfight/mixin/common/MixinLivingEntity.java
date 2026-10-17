package yesman.epicfight.mixin.common;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yesman.epicfight.api.client.camera.EpicFightCameraAPI;
import yesman.epicfight.api.event.EpicFightEventHooks;
import yesman.epicfight.api.event.impl.VanillaEntityEventHooks;
import yesman.epicfight.api.event.types.entity.EntityRemovedEvent;
import yesman.epicfight.api.utils.AttackResult;
import yesman.epicfight.network.EpicFightNetworkManager;
import yesman.epicfight.network.server.SPAbsorption;
import yesman.epicfight.registry.entries.EpicFightAttributes;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.HurtableEntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.damagesource.EpicFightDamageSource;

@Mixin(value = LivingEntity.class)
public abstract class MixinLivingEntity {
	@Shadow
	protected abstract void hurtArmor(DamageSource damageSource, float amount);

	@Inject(at = @At(value = "TAIL"), method = "<init>(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/level/Level;)V")
	private void epicfight$constructor(EntityType<?> entityType, Level level, CallbackInfo info) {
		LivingEntity self = (LivingEntity)((Object)this);
		
		EpicFightCapabilities.getUnparameterizedEntityPatch(self, HurtableEntityPatch.class).ifPresent(entitypatch -> {
			AttributeSupplier.Builder builder = AttributeSupplier.builder();
			java.util.Set<net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute>> added = new java.util.HashSet<>();

			self.getAttributes().supplier.instances.forEach((k, v) -> {
				builder.add(k, v.getBaseValue());
				added.add(k);
			});

			if (!added.contains(Attributes.ATTACK_DAMAGE)) builder.add(Attributes.ATTACK_DAMAGE);
            if (!added.contains(EpicFightAttributes.WEIGHT)) builder.add(EpicFightAttributes.WEIGHT);
            if (!added.contains(EpicFightAttributes.IMPACT)) builder.add(EpicFightAttributes.IMPACT);
            if (!added.contains(EpicFightAttributes.ARMOR_NEGATION)) builder.add(EpicFightAttributes.ARMOR_NEGATION);
            if (!added.contains(EpicFightAttributes.MAX_STRIKES)) builder.add(EpicFightAttributes.MAX_STRIKES);
            if (!added.contains(EpicFightAttributes.STUN_ARMOR)) builder.add(EpicFightAttributes.STUN_ARMOR);
            if (!added.contains(EpicFightAttributes.ASSASSINATION_RESISTANCE)) builder.add(EpicFightAttributes.ASSASSINATION_RESISTANCE);
            if (!added.contains(EpicFightAttributes.OFFHAND_ARMOR_NEGATION)) builder.add(EpicFightAttributes.OFFHAND_ARMOR_NEGATION);
            if (!added.contains(EpicFightAttributes.OFFHAND_IMPACT)) builder.add(EpicFightAttributes.OFFHAND_IMPACT);
            if (!added.contains(EpicFightAttributes.OFFHAND_MAX_STRIKES)) builder.add(EpicFightAttributes.OFFHAND_MAX_STRIKES);
            if (!added.contains(EpicFightAttributes.OFFHAND_ATTACK_SPEED)) builder.add(EpicFightAttributes.OFFHAND_ATTACK_SPEED);

			self.getAttributes().supplier = builder.build();
		});
	}
	
	@Inject(at = @At(value = "TAIL"), method = "blockUsingShield(Lnet/minecraft/world/entity/LivingEntity;)V", cancellable = true)
	private void epicfight$blockUsingShield(LivingEntity p_21200_, CallbackInfo info) {
		LivingEntity self = (LivingEntity)((Object)this);
		LivingEntityPatch<?> opponentEntitypatch = EpicFightCapabilities.getEntityPatch(p_21200_, LivingEntityPatch.class);
		LivingEntityPatch<?> selfEntitypatch = EpicFightCapabilities.getEntityPatch(self, LivingEntityPatch.class);

		if (opponentEntitypatch != null) {
			opponentEntitypatch.setLastAttackResult(AttackResult.blocked(0.0F));

			if (selfEntitypatch != null && opponentEntitypatch.getEpicFightDamageSource() != null) {
				opponentEntitypatch.onAttackBlocked(opponentEntitypatch.getEpicFightDamageSource(), selfEntitypatch);
			}
		}

		// Fire LivingShieldBlockEvent equivalent — plays the shield hit animation
		VanillaEntityEventHooks.onBlockAttacksWithShield(self);
	}
	
	@Inject(at = @At(value = "RETURN"), method = "hurt", cancellable = true)
	private void epicfight$hurt(DamageSource damagesource, float amount, CallbackInfoReturnable<Boolean> info) {
		LivingEntity self = (LivingEntity)((Object)this);
		LivingEntityPatch<?> entitypatch = EpicFightCapabilities.getEntityPatch(damagesource.getEntity(), LivingEntityPatch.class);
		
		if (entitypatch != null) {
			if (info.getReturnValue()) {
				entitypatch.setLastAttackEntity(self);
			}
		}
	}
	
	@Inject(at = @At(value = "HEAD"), method = "push(Lnet/minecraft/world/entity/Entity;)V", cancellable = true)
	private void epicfight$push(Entity p_20293_, CallbackInfo info) {
		LivingEntity self = (LivingEntity)((Object)this);
		LivingEntityPatch<?> entitypatch = EpicFightCapabilities.getEntityPatch(self, LivingEntityPatch.class);
		
		if (entitypatch != null && !entitypatch.canPush(p_20293_)) {
			info.cancel();
		}
	}
	
	@Inject(at = @At(value = "HEAD"), method = "getDamageAfterArmorAbsorb(Lnet/minecraft/world/damagesource/DamageSource;F)F", cancellable = true)
	private void epicfight$getDamageAfterArmorAbsorb(DamageSource source, float amount, CallbackInfoReturnable<Float> info) {
		if (source instanceof EpicFightDamageSource epicFightDamageSource && !source.is(DamageTypeTags.BYPASSES_ARMOR)) {
			this.hurtArmor(source, amount);
			float armorNegationAmount = amount * Math.min(epicFightDamageSource.calculateArmorNegation() * 0.01F , 1.0F);
			float amountElse = amount - armorNegationAmount;
			LivingEntity self = (LivingEntity)((Object)this);
			amountElse = CombatRules.getDamageAfterAbsorb(self, amountElse, source, (float)self.getArmorValue(), (float)self.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
			info.setReturnValue(armorNegationAmount + amountElse);
			info.cancel();
		}
	}
	
	@Inject(at = @At(value = "TAIL"), method = "setAbsorptionAmount(F)V")
	private void epicfight$setAbsorptionAmount(float absorptionAmount, CallbackInfo info) {
		LivingEntity self = (LivingEntity)((Object)this);
		
		if (!self.level().isClientSide()) {
			EpicFightNetworkManager.sendToAllPlayerTrackingThisEntity(new SPAbsorption(self.getId(), absorptionAmount), self);
		}
	}
	
	@Inject(at = @At(value = "TAIL"), method = "makePoofParticles()V")
	private void epicfight$makePoofParticles(CallbackInfo info) {
		LivingEntity self = (LivingEntity)((Object)this);
		
		EpicFightCapabilities.getUnparameterizedEntityPatch(self, LivingEntityPatch.class).ifPresent(entitypatch -> {
            // We needed a hook when an entity is removed by death, makePoofParticles provided a perfect place to achieve it
            EpicFightEventHooks.Entity.ON_REMOVED.postWithListener(new EntityRemovedEvent(Entity.RemovalReason.KILLED, entitypatch), entitypatch.getEventListener());
		});
	}

    @WrapOperation(
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;getYRot()F"
        ),
        method = "jumpFromGround()V"
    )
    private float epicfight$jumpFromGround(LivingEntity livingEntity, Operation<Void> originalOperation) {
        if (livingEntity instanceof Player player && player.isLocalPlayer()) {
            EpicFightCameraAPI cameraApi = EpicFightCameraAPI.getInstance();
            return cameraApi.isTPSMode() ? cameraApi.getCameraYRot() : livingEntity.getYRot();
        }

        return livingEntity.getYRot();
    }

    @WrapOperation(
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;getYRot()F",
            ordinal = 0
        ),
        method = "tick()V"
    )
    private float epicfight$tick(LivingEntity livingEntity, Operation<Void> originalOperation) {
        // returns the basis y rotation as camera in TPS mode
        if (livingEntity instanceof Player player && player.isLocalPlayer()) {
            return EpicFightCameraAPI.getInstance().getYRotForHead(player);
        }

        return livingEntity.getYRot();
    }

    @WrapOperation(
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;getYRot()F"
        ),
        method = "tickHeadTurn(FF)F"
    )
    protected float epicfight$tickHeadTurn(LivingEntity livingEntity, Operation<Float> original) {
        // returns the basis y rotation as camera in TPS mode
        if (livingEntity instanceof Player player && player.isLocalPlayer()) {
            return EpicFightCameraAPI.getInstance().getYRotForHead(player);
        }

        return livingEntity.getYRot();
    }

    // ===== Missing NeoForge entity event hooks =====

    /// LivingDeathEvent — fires when a living entity dies
    @Inject(at = @At(value = "HEAD"), method = "die(Lnet/minecraft/world/damagesource/DamageSource;)V")
    private void epicfight$die(DamageSource damageSource, CallbackInfo info) {
        LivingEntity self = (LivingEntity)(Object)this;
        VanillaEntityEventHooks.onLivingDeath(self, damageSource);
    }

    /// LivingDamageEvent.Pre — fires before damage is applied, allows modifying damage amount
    @Inject(at = @At(value = "HEAD"), method = "actuallyHurt(Lnet/minecraft/world/damagesource/DamageSource;F)V", cancellable = true)
    private void epicfight$actuallyHurtPre(DamageSource damageSource, float amount, CallbackInfo info) {
        LivingEntity self = (LivingEntity)(Object)this;
        float[] modifiedAmount = {amount};
        VanillaEntityEventHooks.onCalculateDamagePre(self, damageSource, amount, newAmount -> modifiedAmount[0] = newAmount);
        // If the hook modified the damage to 0, cancel
        if (modifiedAmount[0] <= 0 && amount > 0) {
            info.cancel();
        }
    }

    /// LivingDamageEvent.Post — fires after damage is applied
    @Inject(at = @At(value = "TAIL"), method = "actuallyHurt(Lnet/minecraft/world/damagesource/DamageSource;F)V")
    private void epicfight$actuallyHurtPost(DamageSource damageSource, float amount, CallbackInfo info) {
        LivingEntity self = (LivingEntity)(Object)this;
        VanillaEntityEventHooks.onCalculateDamagePost(self, damageSource, amount);
    }

    /// LivingEquipmentChangeEvent — fires when equipment changes
    /// In Yarn mappings, the equipment change method is onEquipItem(EquipmentSlot, ItemStack old, ItemStack new)
    /// rather than NeoForge's setItemSlot. We inject at HEAD to capture the change before it applies.
    @Inject(at = @At(value = "HEAD"), method = "onEquipItem(Lnet/minecraft/world/entity/EquipmentSlot;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)V")
    private void epicfight$setItemSlot(EquipmentSlot slot, ItemStack from, ItemStack to, CallbackInfo info) {
        LivingEntity self = (LivingEntity)(Object)this;
        VanillaEntityEventHooks.onEquipmentChanged(self, from, to, slot);
    }

    /// MobEffectEvent.Added — fires when a mob effect is added
    @Inject(at = @At(value = "TAIL"), method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;)Z")
    private void epicfight$addEffect(MobEffectInstance mobEffectInstance, CallbackInfoReturnable<Boolean> info) {
        LivingEntity self = (LivingEntity)(Object)this;
        if (info.getReturnValue()) {
            VanillaEntityEventHooks.onMobEffectAdded(mobEffectInstance, self);
        }
    }

    /// MobEffectEvent.Remove — fires when a mob effect is removed
    /// In Yarn mappings, removeEffect takes Holder<MobEffect> rather than MobEffectInstance.
    /// We look up the active MobEffectInstance from the entity before removal.
    @Inject(at = @At(value = "HEAD"), method = "removeEffect(Lnet/minecraft/core/Holder;)Z")
    private void epicfight$removeEffect(Holder<MobEffect> mobEffect, CallbackInfoReturnable<Boolean> info) {
        LivingEntity self = (LivingEntity)(Object)this;
        MobEffectInstance instance = self.getEffect(mobEffect);
        if (instance != null) {
            VanillaEntityEventHooks.onMobEffectRemoved(instance, self);
        }
    }

    /// LivingKnockBackEvent — fires when an entity is knocked back, allows canceling
    @Inject(at = @At(value = "HEAD"), method = "knockback(DDD)V", cancellable = true)
    private void epicfight$knockback(double strength, double ratioX, double ratioZ, CallbackInfo info) {
        LivingEntity self = (LivingEntity)(Object)this;
        if (VanillaEntityEventHooks.onKnockedBack(self)) {
            info.cancel();
        }
    }

    /// LivingIncomingDamageEvent — fires before damage is processed, allows canceling
    @Inject(at = @At(value = "HEAD"), method = "hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z", cancellable = true)
    private void epicfight$hurtIncoming(DamageSource damageSource, float amount, CallbackInfoReturnable<Boolean> info) {
        LivingEntity self = (LivingEntity)(Object)this;
        if (VanillaEntityEventHooks.onDamageIncomes(self, damageSource, amount)) {
            info.setReturnValue(false);
            info.cancel();
        }
    }

    /// MobEffectEvent.Expired — fires when a mob effect expires due to duration reaching 0.
    /// We wrap the onEffectRemoved call within tickEffects to distinguish expiration from explicit removal.
    @WrapOperation(
        method = "tickEffects()V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;onEffectRemoved(Lnet/minecraft/world/effect/MobEffectInstance;)V")
    )
    private void epicfight$onEffectExpired(LivingEntity instance, MobEffectInstance effect, Operation<Void> original) {
        original.call(instance, effect);
        VanillaEntityEventHooks.onMobEffectExpired(effect, instance);
    }

    /// LivingDropsEvent — fires when a living entity drops items on death, allows canceling
    @Inject(at = @At(value = "HEAD"), method = "dropAllDeathLoot(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;)V", cancellable = true)
    private void epicfight$dropAllDeathLoot(ServerLevel serverLevel, DamageSource damageSource, CallbackInfo info) {
        LivingEntity self = (LivingEntity)(Object)this;
        java.util.Collection<net.minecraft.world.entity.item.ItemEntity> drops = new java.util.ArrayList<>();
        if (VanillaEntityEventHooks.onDropItems(self, damageSource, drops)) {
            info.cancel();
        }
    }
}