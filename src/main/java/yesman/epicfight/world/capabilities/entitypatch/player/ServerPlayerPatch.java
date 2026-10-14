package yesman.epicfight.world.capabilities.entitypatch.player;

import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import yesman.epicfight.api.animation.LivingMotion;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.EpicFight;
import yesman.epicfight.api.event.EpicFightEventHooks;
import yesman.epicfight.api.event.IdentifierProvider;
import yesman.epicfight.api.event.types.player.ChangeInnateSkillEvent;
import yesman.epicfight.api.event.types.player.SetTargetEvent;
import yesman.epicfight.api.utils.AttackResult;
import yesman.epicfight.network.EpicFightNetworkManager;
import yesman.epicfight.network.EpicFightNetworkManager.PayloadBundleBuilder;
import yesman.epicfight.network.common.BiDirectionalSyncEmoteSlots;
import yesman.epicfight.network.server.SPChangeLivingMotion;
import yesman.epicfight.network.server.SPInitSkills;
import yesman.epicfight.network.server.SPModifyPlayerData;
import yesman.epicfight.network.server.SPSkillFeedback;
import yesman.epicfight.registry.entries.EpicFightAttributes;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.SkillSlots;
import yesman.epicfight.skill.modules.HoldableSkill;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.item.CapabilityItem;

import java.util.HashMap;
import java.util.Map;

public class ServerPlayerPatch extends PlayerPatch<ServerPlayer> {
	private LivingEntity attackTarget;
	private boolean updatedMotionCurrentTick;
	private ItemStack prevHeldItem = ItemStack.EMPTY;
	private ItemStack prevHeldItemOffHand = ItemStack.EMPTY;

	public ServerPlayerPatch(ServerPlayer entity) {
		super(entity);

        this.getEventListener().registerEvent(
            EpicFightEventHooks.Entity.DELIVER_DAMAGE_POST,
            event -> {
                if (!event.getDamageSource().shouldChargeWeapon()) return;

                SkillContainer container = this.getSkill(SkillSlots.WEAPON_INNATE);
                // Charge the weapon-innate slot from whichever hand is the active combat hand so
                // an offhand-only carrier accumulates resource on hits with their offhand weapon.
                ItemStack activeItem = this.getOriginal().getItemInHand(this.getPrimaryHand());

                if (!container.isFull() && !container.isActivated() && container.hasSkill(EpicFightCapabilities.getItemStackCapability(activeItem).getInnateSkill(this, activeItem))) {
                    float value = container.getResource() + event.getModifiedDamage();

                    if (value > 0.0F) {
                        container.getSkill().setConsumptionSynchronize(container, value);
                    }
                }
            },
            IdentifierProvider.permanent(),
            10
        );
	}
	
	@Override
	public void onJoinWorld(ServerPlayer player, Level level, boolean worldgenSpawn) {
		super.onJoinWorld(player, level, worldgenSpawn);

        PayloadBundleBuilder payloadBundleBuilder = PayloadBundleBuilder.beginWith(new SPInitSkills(this.getPlayerSkills()));
        payloadBundleBuilder.and(new BiDirectionalSyncEmoteSlots(this));
        payloadBundleBuilder.send((first, others) -> EpicFightNetworkManager.sendToPlayer(first, player, others));

        // Initialize weapon innate skill for the player's current weapon.
        // On Fabric, the LivingEntity#onEquipItem mixin may not fire on the first tick
        // because lastEquipmentItemStacks is initialized with the player's loaded equipment
        // before detectEquipmentUpdates runs. This ensures the WEAPON_INNATE skill slot
        // is populated immediately on join, matching NeoForge's behavior where the
        // LivingEquipmentChangeEvent fires during the first baseTick after join.
        CapabilityItem mainhandCap = this.getHoldingItemCapability(InteractionHand.MAIN_HAND);
        CapabilityItem offhandCap = this.getHoldingItemCapability(InteractionHand.OFF_HAND);
        boolean mainBare = mainhandCap.isEmpty() || mainhandCap.getWeaponCategory() == CapabilityItem.WeaponCategories.FIST;
        boolean offhandReal = !offhandCap.isEmpty()
                && offhandCap.getWeaponCategory() != CapabilityItem.WeaponCategories.FIST
                && offhandCap.getWeaponCategory() != CapabilityItem.WeaponCategories.SHIELD;
        CapabilityItem innateCap = (mainBare && offhandReal) ? offhandCap : mainhandCap;
        ItemStack innateItem = (mainBare && offhandReal) ? this.original.getOffhandItem() : this.original.getMainHandItem();
        if (!innateCap.isEmpty()) {
            innateCap.changeWeaponInnateSkill(this, innateItem);
        }
	}
	
	@Override
	public void onStartTracking(ServerPlayer trackingPlayer) {
		PayloadBundleBuilder payloadBundleBuilder = PayloadBundleBuilder.create();
		SPChangeLivingMotion msg = new SPChangeLivingMotion(this.getOriginal().getId());
		msg.putEntries(this.getAnimator().getLivingAnimations().entrySet());
		
		payloadBundleBuilder.and(msg);
		
		this.getPlayerSkills().listSkillContainers().filter(skillContainer -> !skillContainer.isEmpty() && skillContainer.getSkill().getCategory().shouldSynchronize()).forEach(skillContainer -> {
			payloadBundleBuilder.and(skillContainer.createSyncPacketToRemotePlayer());
			skillContainer.getDataManager().onTracked(payloadBundleBuilder);
			skillContainer.getSkill().onTracked(skillContainer, payloadBundleBuilder);
		});
		
		payloadBundleBuilder.and(SPModifyPlayerData.setPlayerMode(this.getOriginal().getId(), this.playerMode));
		payloadBundleBuilder.send((first, others) -> EpicFightNetworkManager.sendToPlayer(first, trackingPlayer, others));
	}
	
	@Override
	public void preTick() {
		super.preTick();
		this.updatedMotionCurrentTick = false;

		// Detect held item changes for the player. On Fabric, the LivingEntity#onEquipItem mixin
		// does not fire when a player switches hotbar slots (only when setItemSlot is called),
		// so we need to detect changes here and call updateHeldItem manually, matching NeoForge's
		// LivingEquipmentChangeEvent behavior which fires from collectEquipmentChanges every tick.
		ItemStack currentMain = this.original.getMainHandItem();
		ItemStack currentOff = this.original.getOffhandItem();

		if (!ItemStack.isSameItemSameComponents(this.prevHeldItem, currentMain)) {
			CapabilityItem fromCap = EpicFightCapabilities.getItemStackCapability(this.prevHeldItem);
			CapabilityItem toCap = this.getHoldingItemCapability(InteractionHand.MAIN_HAND);
			ItemStack previous = this.prevHeldItem;
			this.prevHeldItem = currentMain.copy();
			this.updateHeldItem(fromCap, toCap, previous, currentMain, InteractionHand.MAIN_HAND);
		}

		if (!ItemStack.isSameItemSameComponents(this.prevHeldItemOffHand, currentOff)) {
			CapabilityItem fromCap = EpicFightCapabilities.getItemStackCapability(this.prevHeldItemOffHand);
			CapabilityItem toCap = this.getHoldingItemCapability(InteractionHand.OFF_HAND);
			ItemStack previous = this.prevHeldItemOffHand;
			this.prevHeldItemOffHand = currentOff.copy();
			this.updateHeldItem(fromCap, toCap, previous, currentOff, InteractionHand.OFF_HAND);
		}
	}

	@Override
	public void updateMotion(boolean considerInaction) {
	}
	
	@Override
	public void updateHeldItem(CapabilityItem fromCap, CapabilityItem toCap, ItemStack from, ItemStack to, InteractionHand hand) {
		if (this.isHoldingAny()) {
			this.getSkillContainerFor(this.holdingSkill.asSkill()).ifPresent((container) -> {
				container.getSkill().cancelOnServer(container, null);
				EpicFightNetworkManager.sendToPlayer(SPSkillFeedback.expired(container.getSlot()), this.original);
			});
			
			this.resetHolding();
		}
		
		// Drive innate-skill registration from the cap that's actually the active combat weapon.
		// In offhand-only mirror mode the mainhand is bare/fist and has no innate, so without
		// this the WEAPON_INNATE skill slot stays empty -- the long-press would never fire
		// Liechtenauer/Battojutsu/etc. for an offhand-only weapon. Use the post-update value of
		// each slot when computing primary so the cap reflects what the player will be holding
		// after this updateHeldItem call returns.
		ItemStack postMainItem = (hand == InteractionHand.MAIN_HAND) ? to : this.original.getMainHandItem();
		ItemStack postOffItem = (hand == InteractionHand.OFF_HAND) ? to : this.original.getOffhandItem();
		CapabilityItem postMainCap = (hand == InteractionHand.MAIN_HAND) ? toCap : this.getHoldingItemCapability(InteractionHand.MAIN_HAND);
		CapabilityItem postOffCap = (hand == InteractionHand.OFF_HAND) ? toCap : this.getHoldingItemCapability(InteractionHand.OFF_HAND);
		boolean mainBareAfter = postMainCap.isEmpty()
				|| postMainCap.getWeaponCategory() == CapabilityItem.WeaponCategories.FIST;
		// Keep parity with {@link LivingEntityPatch#isMirrorMode}: shields don't trigger mirror mode.
		boolean offhandRealAfter = !postOffCap.isEmpty()
				&& postOffCap.getWeaponCategory() != CapabilityItem.WeaponCategories.FIST
				&& postOffCap.getWeaponCategory() != CapabilityItem.WeaponCategories.SHIELD;
		boolean willMirror = mainBareAfter && offhandRealAfter;
		CapabilityItem innateCap = willMirror ? postOffCap : postMainCap;
		ItemStack innateItem = willMirror ? postOffItem : postMainItem;
		innateCap.changeWeaponInnateSkill(this, innateItem);
        EpicFightEventHooks.Player.CHANGE_INNATE_SKILL.postWithListener(new ChangeInnateSkillEvent(this, from, fromCap, to, toCap, hand), this.getEventListener());

		if (hand == InteractionHand.OFF_HAND) {
			if (!from.isEmpty()) {
				from.getOrDefault(net.minecraft.core.component.DataComponents.ATTRIBUTE_MODIFIERS, net.minecraft.world.item.component.ItemAttributeModifiers.EMPTY).forEach(EquipmentSlot.MAINHAND, (attribute, modifier) -> {
					if (Attributes.ATTACK_SPEED.equals(attribute)) {
						this.original.getAttribute(EpicFightAttributes.OFFHAND_ATTACK_SPEED).removeModifier(modifier);
					}
				});
			}
			
			if (!fromCap.isEmpty()) {
				Multimap<Holder<Attribute>, AttributeModifier> modifiers = fromCap.getAllAttributeModifiers();
				modifiers.get(EpicFightAttributes.ARMOR_NEGATION).forEach(this.original.getAttribute(EpicFightAttributes.OFFHAND_ARMOR_NEGATION)::removeModifier);
				modifiers.get(EpicFightAttributes.IMPACT).forEach(this.original.getAttribute(EpicFightAttributes.OFFHAND_IMPACT)::removeModifier);
				modifiers.get(EpicFightAttributes.MAX_STRIKES).forEach(this.original.getAttribute(EpicFightAttributes.OFFHAND_MAX_STRIKES)::removeModifier);
				modifiers.get(Attributes.ATTACK_SPEED).forEach(this.original.getAttribute(EpicFightAttributes.OFFHAND_ATTACK_SPEED)::removeModifier);
			}
			
			if (!to.isEmpty()) {
				to.getOrDefault(net.minecraft.core.component.DataComponents.ATTRIBUTE_MODIFIERS, net.minecraft.world.item.component.ItemAttributeModifiers.EMPTY).forEach(EquipmentSlot.MAINHAND, (attribute, modifier) -> {
					if (Attributes.ATTACK_SPEED.equals(attribute)) {
						this.applyOffhandModifier(EpicFightAttributes.OFFHAND_ATTACK_SPEED, modifier);
					}
				});
			}
			
			if (!toCap.isEmpty()) {
				Multimap<Holder<Attribute>, AttributeModifier> modifiers = toCap.getAttributeModifiers(this);
				modifiers.get(EpicFightAttributes.ARMOR_NEGATION).forEach(modifier -> this.applyOffhandModifier(EpicFightAttributes.OFFHAND_ARMOR_NEGATION, modifier));
				modifiers.get(EpicFightAttributes.IMPACT).forEach(modifier -> this.applyOffhandModifier(EpicFightAttributes.OFFHAND_IMPACT, modifier));
				modifiers.get(EpicFightAttributes.MAX_STRIKES).forEach(modifier -> this.applyOffhandModifier(EpicFightAttributes.OFFHAND_MAX_STRIKES, modifier));
				modifiers.get(Attributes.ATTACK_SPEED).forEach(modifier -> this.applyOffhandModifier(EpicFightAttributes.OFFHAND_ATTACK_SPEED, modifier));
			}
		}
		
		this.modifyLivingMotionByCurrentItem(true);
		
		super.updateHeldItem(fromCap, toCap, from, to, hand);
	}
	
	/// Applies an offhand attribute modifier idempotently: updateHeldItem can run
	/// more than once per equipment change (collectEquipmentChanges hook plus the
	/// preTick hotbar-switch detector), and AttributeInstance.addTransientModifier
	/// throws when the same modifier id is applied twice.
	private void applyOffhandModifier(Holder<Attribute> attribute, AttributeModifier modifier) {
		AttributeInstance instance = this.original.getAttribute(attribute);
		if (instance != null) {
			instance.removeModifier(modifier.id());
			instance.addTransientModifier(modifier);
		}
	}
	
	public void modifyLivingMotionByCurrentItem() {
		this.modifyLivingMotionByCurrentItem(false);
	}
	
	/**
	 * @param checkOldAnimations: when true, it compares the animations and send the packet if it has any changes
	 */
	public void modifyLivingMotionByCurrentItem(boolean checkOldAnimations) {
		if (this.updatedMotionCurrentTick && checkOldAnimations) {
			return;
		}
		
		Map<LivingMotion, AssetAccessor<? extends StaticAnimation>> oldLivingAnimations = this.getAnimator().getLivingAnimations();
		Map<LivingMotion, AssetAccessor<? extends StaticAnimation>> newLivingAnimations = Maps.newHashMap();
		
		CapabilityItem mainhandCap = this.getHoldingItemCapability(InteractionHand.MAIN_HAND);
		CapabilityItem offhandCap = this.getAdvancedHoldingItemCapability(InteractionHand.OFF_HAND);

		Map<LivingMotion, AssetAccessor<? extends StaticAnimation>> livingMotionModifiers = new HashMap<>(mainhandCap.getLivingMotionModifier(this, InteractionHand.MAIN_HAND));
		Map<LivingMotion, ? extends AssetAccessor<? extends StaticAnimation>> offhandModifiers = offhandCap.getLivingMotionModifier(this, InteractionHand.OFF_HAND);

		// Match the universal-mirror approach used for combo animations: when the weapon is alone
		// in the offhand and the cap didn't return any living-motion modifiers from the OFF_HAND
		// query, re-query the cap as if it were the mainhand. Built-in weapons already plug their
		// offhand-mirror handling into the OFF_HAND path (WeaponCapability.getLivingMotionModifier
		// + the bypass in getCurrentSet), so they take the first branch and we never re-query --
		// no behavior change for them. Addon caps that only ever register modifiers under
		// MAIN_HAND (the typical "I don't know about offhand-only mode" custom CapabilityItem)
		// return empty here, fall through to the second query, and hand back their hold/walk/run
		// set. The visual flip is applied once at the renderer in ClientAnimator.getPose, so we
		// don't need a mirrored animation -- the mainhand animation plays through the pose mirror
		// and lands on the correct side.
		if (offhandModifiers.isEmpty() && this.isMirrorMode()) {
			offhandModifiers = offhandCap.getLivingMotionModifier(this, InteractionHand.MAIN_HAND);
		}
		livingMotionModifiers.putAll(offhandModifiers);
		
		for (Map.Entry<LivingMotion, AssetAccessor<? extends StaticAnimation>> entry : livingMotionModifiers.entrySet()) {
			AssetAccessor<? extends StaticAnimation> aniamtion = entry.getValue();
			
			if (!oldLivingAnimations.containsKey(entry.getKey())) {
				this.updatedMotionCurrentTick = true;
			} else if (oldLivingAnimations.get(entry.getKey()) != aniamtion) {
				this.updatedMotionCurrentTick = true;
			}
			
			newLivingAnimations.put(entry.getKey(), aniamtion);
		}
		
		for (LivingMotion oldLivingMotion : oldLivingAnimations.keySet()) {
			if (!newLivingAnimations.containsKey(oldLivingMotion)) {
				this.updatedMotionCurrentTick = true;
				break;
			}
		}
		
		if (this.updatedMotionCurrentTick || !checkOldAnimations) {
			this.getAnimator().resetLivingAnimations();
			newLivingAnimations.forEach(this.getAnimator()::addLivingAnimation);

			SPChangeLivingMotion msg = new SPChangeLivingMotion(this.original.getId());
			msg.putEntries(newLivingAnimations.entrySet());

			EpicFightNetworkManager.sendToAllPlayerTrackingThisEntityWithSelf(msg, this.original);
		}
	}
	
	@Override
	public void sendToAllPlayersTrackingMe(CustomPacketPayload packet, CustomPacketPayload... otherPackets) {
		if (!this.isLogicalClient()) {
			EpicFightNetworkManager.sendToAllPlayerTrackingThisEntityWithSelf(packet, this.original, otherPackets);
		}
	}
	
	@Override
	public void setModelYRot(float amount, boolean sendPacket) {
		super.setModelYRot(amount, sendPacket);
		
		if (sendPacket) {
			EpicFightNetworkManager.sendToAllPlayerTrackingThisEntityWithSelf(SPModifyPlayerData.setPlayerYRot(this.original.getId(), this.modelYRot), this.original);
		}
	}
	
	@Override
	public void disableModelYRot(boolean sendPacket) {
		super.disableModelYRot(sendPacket);
		
		if (sendPacket) {
			EpicFightNetworkManager.sendToAllPlayerTrackingThisEntityWithSelf(SPModifyPlayerData.disablePlayerYRot(this.original.getId()), this.original);
		}
	}
	
	@Override
	public AttackResult tryHurt(DamageSource damageSource, float amount) {
		if (this.getOriginal().getAbilities().invulnerable && !damageSource.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
			return AttackResult.missed(amount); 
		}

        return super.tryHurt(damageSource, amount);
	}

	@Override
	public void toVanillaMode(boolean synchronize) {
		super.toVanillaMode(synchronize);
		
		if (synchronize) {
			EpicFightNetworkManager.sendToAllPlayerTrackingThisEntityWithSelf(SPModifyPlayerData.setPlayerMode(this.original.getId(), PlayerMode.VANILLA), this.original);
		}
	}
	
	@Override
	public void toEpicFightMode(boolean synchronize) {
		super.toEpicFightMode(synchronize);
		
		if (synchronize) {
			EpicFightNetworkManager.sendToAllPlayerTrackingThisEntityWithSelf(SPModifyPlayerData.setPlayerMode(this.original.getId(), PlayerMode.EPICFIGHT), this.original);
		}
	}
	
	@Override
	public boolean isTargetInvulnerable(Entity target) {
		if (target instanceof Player && !this.getOriginal().server.isPvpAllowed()) {
			return true;
		}
		
		return super.isTargetInvulnerable(target);
	}
	
	@Override
	public void setLastAttackSuccess(boolean setter) {
		if (setter) {
			EpicFightNetworkManager.sendToPlayer(SPModifyPlayerData.setLastAttackResult(this.original.getId(), true), this.original);
		}
		
		this.isLastAttackSuccess = setter;
	}
	
	public void setAttackTarget(LivingEntity entity) {
		SetTargetEvent event = new SetTargetEvent(this, entity);
        EpicFightEventHooks.Player.SET_TARGET.postWithListener(event, this.getEventListener());
        if (event.isCanceled()) return;

		this.attackTarget = event.getTarget();
	}
	
	@Override
	public boolean startSkillHolding(HoldableSkill chargingSkill) {
		if (super.startSkillHolding(chargingSkill)) {
			EpicFightNetworkManager.sendToPlayer(SPSkillFeedback.held(this.getSkillContainerFor(chargingSkill.asSkill()).get().getSlot()), this.getOriginal());
			return true;
		}
		
		return false;
	}
	
	@Override
	public LivingEntity getTarget() {
		return this.attackTarget;
	}
	
	@Override
	public void setGrapplingTarget(LivingEntity grapplingTarget) {
		super.setGrapplingTarget(grapplingTarget);
		EpicFightNetworkManager.sendToPlayer(SPModifyPlayerData.setGrapplingTarget(this.original.getId(), grapplingTarget), this.original);
	}
}