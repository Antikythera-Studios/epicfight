package yesman.epicfight.world.capabilities.entitypatch.mob;
import yesman.epicfight.platform.fabric.event.EntityAttributeModificationEvent;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;

import yesman.epicfight.api.animation.Animator;
import yesman.epicfight.api.animation.LivingMotions;
import yesman.epicfight.api.client.animation.ClientAnimator;
import yesman.epicfight.api.utils.side.ClientOnly;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.network.EntityPairingPacketTypes;
import yesman.epicfight.network.EpicFightNetworkManager;
import yesman.epicfight.network.server.SPEntityPairingPacket;
import yesman.epicfight.registry.entries.EpicFightAttributes;
import yesman.epicfight.world.capabilities.entitypatch.Factions;
import yesman.epicfight.world.capabilities.entitypatch.HumanoidMobPatch;

public class ZombiePatch<T extends PathfinderMob> extends HumanoidMobPatch<T> {
	public ZombiePatch(T original) {
		super(original, Factions.UNDEAD);
	}
	
	@Override
	public void onStartTracking(ServerPlayer trackingPlayer) {
		if (!this.getHoldingItemCapability(InteractionHand.MAIN_HAND).isEmpty()) {
			SPEntityPairingPacket packet = new SPEntityPairingPacket(this.original.getId(), EntityPairingPacketTypes.ZOMBIE_SPAWN);
			EpicFightNetworkManager.sendToPlayer(packet, trackingPlayer);
		}
		
		super.onStartTracking(trackingPlayer);
	}
	
	@Override @ClientOnly
	public void entityPairing(SPEntityPairingPacket packet) {
		super.entityPairing(packet);
		
		if (packet.pairingPacketType() == EntityPairingPacketTypes.ZOMBIE_SPAWN) {
			ClientAnimator animator = this.getClientAnimator();
			animator.addLivingAnimation(LivingMotions.IDLE, Animations.BIPED_IDLE);
			animator.addLivingAnimation(LivingMotions.WALK, Animations.BIPED_WALK);
			animator.addLivingAnimation(LivingMotions.CHASE, Animations.BIPED_WALK);
			animator.setCurrentMotionsAsDefault();
		}
	}
	
	public static void initAttributes(EntityAttributeModificationEvent event) {
		event.add(EntityType.ZOMBIE, EpicFightAttributes.IMPACT, 1.0D);
	}
	
	@Override
	public void initAnimator(Animator animator) {
		super.initAnimator(animator);
		animator.addLivingAnimation(LivingMotions.IDLE, Animations.ZOMBIE_IDLE);
		animator.addLivingAnimation(LivingMotions.WALK, Animations.ZOMBIE_WALK);
		animator.addLivingAnimation(LivingMotions.CHASE, Animations.ZOMBIE_CHASE);
		animator.addLivingAnimation(LivingMotions.FALL, Animations.BIPED_FALL);
		animator.addLivingAnimation(LivingMotions.MOUNT, Animations.BIPED_MOUNT);
		animator.addLivingAnimation(LivingMotions.DEATH, Animations.BIPED_DEATH);
	}
	
	@Override
	public void updateMotion(boolean considerInaction) {
		super.commonAggressiveMobUpdateMotion(considerInaction);
	}
}