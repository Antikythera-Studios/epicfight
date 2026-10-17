package yesman.epicfight.registry.entries;
import yesman.epicfight.platform.fabric.event.EntityAttributeModificationEvent;
import yesman.epicfight.platform.fabric.event.EntityAttributeCreationEvent;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.monster.AbstractSkeleton;




import yesman.epicfight.registry.deferred_shim.DeferredHolderShim;
import yesman.epicfight.registry.deferred_shim.DeferredRegisterShim;
import yesman.epicfight.EpicFight;
import yesman.epicfight.world.capabilities.entitypatch.boss.WitherGhostPatch;
import yesman.epicfight.world.capabilities.entitypatch.boss.WitherPatch;
import yesman.epicfight.world.capabilities.entitypatch.boss.enderdragon.EnderDragonPatch;
import yesman.epicfight.world.capabilities.entitypatch.mob.CreeperPatch;
import yesman.epicfight.world.capabilities.entitypatch.mob.EndermanPatch;
import yesman.epicfight.world.capabilities.entitypatch.mob.HoglinPatch;
import yesman.epicfight.world.capabilities.entitypatch.mob.IronGolemPatch;
import yesman.epicfight.world.capabilities.entitypatch.mob.PiglinBrutePatch;
import yesman.epicfight.world.capabilities.entitypatch.mob.PiglinPatch;
import yesman.epicfight.world.capabilities.entitypatch.mob.RavagerPatch;
import yesman.epicfight.world.capabilities.entitypatch.mob.VindicatorPatch;
import yesman.epicfight.world.capabilities.entitypatch.mob.WitherSkeletonPatch;
import yesman.epicfight.world.capabilities.entitypatch.mob.ZoglinPatch;
import yesman.epicfight.world.capabilities.entitypatch.mob.ZombiePatch;
import yesman.epicfight.world.entity.WitherGhostClone;

public final class EpicFightAttributes {
	private EpicFightAttributes() {}
	
	static {
		Attributes.ATTACK_DAMAGE.value().setSyncable(true);
	}
	
	public static final DeferredRegisterShim<Attribute> REGISTRY = new DeferredRegisterShim<>(Registries.ATTRIBUTE, EpicFight.MODID);
	
    public static final DeferredHolderShim<Attribute, Attribute> MAX_STAMINA = REGISTRY.register("stamina", () -> new RangedAttribute("attribute.name." + EpicFight.MODID + ".stamina", 15.0D, 0.0D, 1024.0D).setSyncable(true));
    public static final DeferredHolderShim<Attribute, Attribute> STAMINA_REGEN = REGISTRY.register("stamina_regen", () -> new RangedAttribute("attribute.name." + EpicFight.MODID + ".stamina_regen", 1.0D, 0.0D, 30.0D).setSyncable(true));
    public static final DeferredHolderShim<Attribute, Attribute> STUN_ARMOR = REGISTRY.register("stun_armor", () -> new RangedAttribute("attribute.name." + EpicFight.MODID + ".stun_armor", 0.0D, 0.0D, 1024.0D).setSyncable(true));
    public static final DeferredHolderShim<Attribute, Attribute> WEIGHT = REGISTRY.register("weight", () -> new RangedAttribute("attribute.name." + EpicFight.MODID + ".weight", 0.0D, 0.0D, 1024.0).setSyncable(true));
    public static final DeferredHolderShim<Attribute, Attribute> MAX_STRIKES = REGISTRY.register("max_strikes", () -> new RangedAttribute("attribute.name." + EpicFight.MODID + ".max_strikes", 1.0D, 1.0D, 1024.0).setSyncable(true));
	public static final DeferredHolderShim<Attribute, Attribute> ARMOR_NEGATION = REGISTRY.register("armor_negation", () -> new RangedAttribute("attribute.name." + EpicFight.MODID + ".armor_negation", 0.0D, 0.0D, 100.0D).setSyncable(true));
	public static final DeferredHolderShim<Attribute, Attribute> IMPACT = REGISTRY.register("impact", () -> new RangedAttribute("attribute.name." + EpicFight.MODID + ".impact", 0.5D, 0.0D, 1024.0).setSyncable(true));
	public static final DeferredHolderShim<Attribute, Attribute> ASSASSINATION_RESISTANCE = REGISTRY.register("execution_resistance", () -> new RangedAttribute("attribute.name." + EpicFight.MODID + ".execution_resistance", 0.0D, 0.0D, 10.0D).setSyncable(true));
	
	public static final DeferredHolderShim<Attribute, Attribute> OFFHAND_ATTACK_SPEED = REGISTRY.register("offhand_attack_speed", () -> new RangedAttribute("attribute.name." + EpicFight.MODID + ".offhand_attack_speed", 4.0D, 0.0D, 1024.0D).setSyncable(true));
	public static final DeferredHolderShim<Attribute, Attribute> OFFHAND_MAX_STRIKES = REGISTRY.register("offhand_max_strikes", () -> new RangedAttribute("attribute.name." + EpicFight.MODID + ".offhand_max_strikes", 1.0D, 1.0D, 1024.0).setSyncable(true));
	public static final DeferredHolderShim<Attribute, Attribute> OFFHAND_ARMOR_NEGATION = REGISTRY.register("offhand_armor_negation", () -> new RangedAttribute("attribute.name." + EpicFight.MODID + ".offhand_armor_negation", 0.0D, 0.0D, 100.0D).setSyncable(true));
	public static final DeferredHolderShim<Attribute, Attribute> OFFHAND_IMPACT = REGISTRY.register("offhand_impact", () -> new RangedAttribute("attribute.name." + EpicFight.MODID + ".offhand_impact", 0.5D, 0.0D, 1024.0).setSyncable(true));
	
	// Modifier ids for epicfight attribute
	public static final ResourceLocation ARMOR_NEGATION_MODIFIER = EpicFight.identifier("armor_negation");
	public static final ResourceLocation MAX_STRIKE_MODIFIER = EpicFight.identifier("max_strikes");
	public static final ResourceLocation IMPACT_MODIFIER = EpicFight.identifier("impact");
	public static final ResourceLocation ATTACK_DAMAGE_MODIFIER = EpicFight.identifier("attack_damage");
	public static final ResourceLocation ATTACK_SPEED_MODIFIER = EpicFight.identifier("attack_speed");
    
	public static AttributeModifier getArmorNegationModifier(double value) {
		return new AttributeModifier(ARMOR_NEGATION_MODIFIER, value, AttributeModifier.Operation.ADD_VALUE);
	}

	public static AttributeModifier getMaxStrikesModifier(int value) {
		return new AttributeModifier(MAX_STRIKE_MODIFIER, value, AttributeModifier.Operation.ADD_VALUE);
	}

	public static AttributeModifier getImpactModifier(double value) {
		return new AttributeModifier(IMPACT_MODIFIER, value, AttributeModifier.Operation.ADD_VALUE);
	}

	public static AttributeModifier getDamageBonusModifier(double value) {
		return new AttributeModifier(ATTACK_DAMAGE_MODIFIER, value, AttributeModifier.Operation.ADD_VALUE);
	}

	public static AttributeModifier getSpeedBonusModifier(double value) {
		return new AttributeModifier(ATTACK_SPEED_MODIFIER, value, AttributeModifier.Operation.ADD_VALUE);
	}
	
	
	public static final class EventBus {
		private EventBus() {}
		
		
		public static void entityAttributeCreationEvent(EntityAttributeCreationEvent event) {
			event.put(EpicFightEntityTypes.WITHER_SKELETON_MINION.get(), AbstractSkeleton.createAttributes().build());
			event.put(EpicFightEntityTypes.WITHER_GHOST_CLONE.get(), WitherGhostClone.createAttributes().build());
			event.put(EpicFightEntityTypes.DODGE_LOCATION_INDICATOR.get(), LivingEntity.createLivingAttributes().build());
		}
		
		
		public static void entityAttributeModificationEvent(EntityAttributeModificationEvent event) {
			common(EntityType.CAVE_SPIDER, event);
			common(EntityType.EVOKER, event);
			common(EntityType.IRON_GOLEM, event);
			humanoid(EntityType.PILLAGER, event);
			common(EntityType.RAVAGER, event);
			common(EntityType.SPIDER, event);
			common(EntityType.VEX, event);
			humanoid(EntityType.VINDICATOR, event);
			humanoid(EntityType.WITCH, event);
			common(EntityType.HOGLIN, event);
			common(EntityType.ZOGLIN, event);
			common(EntityType.ENDER_DRAGON, event);
			common(EntityType.CREEPER, event);
			humanoid(EntityType.DROWNED, event);
			common(EntityType.ENDERMAN, event);
			humanoid(EntityType.HUSK, event);
			humanoid(EntityType.PIGLIN, event);
			humanoid(EntityType.PIGLIN_BRUTE, event);
			humanoid(EntityType.SKELETON, event);
			humanoid(EntityType.STRAY, event);
			humanoid(EntityType.WITHER_SKELETON, event);
			humanoid(EntityType.ZOMBIE, event);
			humanoid(EntityType.ZOMBIE_VILLAGER, event);
			humanoid(EntityType.ZOMBIFIED_PIGLIN, event);
			common(EpicFightEntityTypes.WITHER_SKELETON_MINION.get(), event);
			player(EntityType.PLAYER, event);
			dragon(EntityType.ENDER_DRAGON, event);
			common(EntityType.WITHER, event);
			
			CreeperPatch.initAttributes(event);
			EnderDragonPatch.initAttributes(event);
			EndermanPatch.initAttributes(event);
			HoglinPatch.initAttributes(event);
			IronGolemPatch.initAttributes(event);
			PiglinBrutePatch.initAttributes(event);
			PiglinPatch.initAttributes(event);
			RavagerPatch.initAttributes(event);
			VindicatorPatch.initAttributes(event);
			WitherPatch.initAttributes(event);
			WitherGhostPatch.initAttributes(event);
			WitherSkeletonPatch.initAttributes(event);
			ZoglinPatch.initAttributes(event);
			ZombiePatch.initAttributes(event);
		}
	    
	    private static void common(EntityType<? extends LivingEntity> entityType, EntityAttributeModificationEvent event) {
			event.add(entityType, EpicFightAttributes.WEIGHT);
			event.add(entityType, EpicFightAttributes.ARMOR_NEGATION);
			event.add(entityType, EpicFightAttributes.IMPACT);
			event.add(entityType, EpicFightAttributes.MAX_STRIKES);
			event.add(entityType, EpicFightAttributes.STUN_ARMOR);
		}
	    
	    private static void humanoid(EntityType<? extends LivingEntity> entityType, EntityAttributeModificationEvent event) {
	    	common(entityType, event);
			event.add(entityType, EpicFightAttributes.OFFHAND_ATTACK_SPEED);
			event.add(entityType, EpicFightAttributes.OFFHAND_MAX_STRIKES);
			event.add(entityType, EpicFightAttributes.OFFHAND_ARMOR_NEGATION);
			event.add(entityType, EpicFightAttributes.OFFHAND_IMPACT);
		}
	    
	    private static void player(EntityType<? extends LivingEntity> entityType, EntityAttributeModificationEvent event) {
	    	humanoid(entityType, event);
			event.add(entityType, EpicFightAttributes.MAX_STAMINA);
			event.add(entityType, EpicFightAttributes.STAMINA_REGEN);
		}
	    
	    private static void dragon(EntityType<? extends EnderDragon> entityType, EntityAttributeModificationEvent event) {
	    	common(entityType, event);
			event.add(entityType, Attributes.ATTACK_DAMAGE);
		}
	}
}