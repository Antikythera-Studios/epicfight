package yesman.epicfight.world.capabilities.item;

import com.google.common.collect.*;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import yesman.epicfight.EpicFight;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.LivingMotion;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.api.animation.types.MainFrameAnimation;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.collider.Collider;
import yesman.epicfight.api.event.types.player.ModifyComboCounter;
import yesman.epicfight.api.ex_cap.managers.ItemPresetManager;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.gameasset.ColliderPreset;
import yesman.epicfight.network.EpicFightNetworkManager;
import yesman.epicfight.network.EpicFightNetworkManager.PayloadBundleBuilder;
import yesman.epicfight.network.server.SPChangeSkill;
import yesman.epicfight.network.server.SPSetRemotePlayerSkill;
import yesman.epicfight.network.server.SPSetSkillContainerValue;
import yesman.epicfight.particle.HitParticleType;
import yesman.epicfight.registry.deferred.holders.DeferredCustomData;
import yesman.epicfight.registry.deferred.holders.DeferredPreset;
import yesman.epicfight.registry.entries.EpicFightAttributes;
import yesman.epicfight.registry.entries.EpicFightEnchantments;
import yesman.epicfight.registry.entries.EpicFightParticles;
import yesman.epicfight.registry.entries.EpicFightSounds;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.SkillSlots;
import yesman.epicfight.skill.guard.GuardSkill;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.world.capabilities.item.custom.CustomData;

import javax.annotation.Nullable;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;

import static yesman.epicfight.generated.LangKeys.*;

public class CapabilityItem {
	public static CapabilityItem EMPTY = CapabilityItem.builder().build();
	protected static List<AnimationAccessor<? extends AttackAnimation>> commonAutoAttackMotion;
	protected final WeaponCategory weaponCategory;
	
	static {
		commonAutoAttackMotion = Lists.newArrayList();
		commonAutoAttackMotion.add(Animations.FIST_AUTO1);
		commonAutoAttackMotion.add(Animations.FIST_AUTO2);
		commonAutoAttackMotion.add(Animations.FIST_AUTO3);
		commonAutoAttackMotion.add(Animations.FIST_DASH);
		commonAutoAttackMotion.add(Animations.FIST_AIR_SLASH);
	}
	
	public static List<AnimationAccessor<? extends AttackAnimation>> getBasicAutoAttackMotions() {
		return commonAutoAttackMotion;
	}
	
	public static List<AttributeModifier> getAttributeModifiersAsWeapon(Holder<Attribute> attribute, EquipmentSlot slot, ItemStack itemstack, @Nullable LivingEntityPatch<?> entitypatch) {
		List<AttributeModifier> attributeModifiers = Lists.newArrayList();
		
		itemstack.getAttributeModifiers().forEach(slot, (attribute$1, modifier) -> {
			if (attribute$1 == attribute) {
				attributeModifiers.add(modifier);
			}
		});
		
		CapabilityItem itemCapability = EpicFightCapabilities.getItemStackCapability(itemstack);
		
		if (!itemCapability.isEmpty()) {
			itemCapability.getAttributeModifiers(entitypatch).forEach((attribute$1, modifier) -> {
				if (attribute$1 == attribute) {
					attributeModifiers.add(modifier);
				}
			});
		}
		
		return attributeModifiers;
	}

    protected static boolean validateAttribute(LivingEntityPatch<?> patch, Holder<Attribute> attributeHolder) {
        return patch.getOriginal().getAttributes().hasAttribute(attributeHolder);
    }

	protected Map<Style, Map<Holder<Attribute>, AttributeModifier>> attributeMap;
	protected Map<Style, ItemAttributeModifiers> modifiers;
	protected final Map<Holder<CustomData<?>>, Object> customData;
	///For debugging only
	protected ResourceLocation id;
	protected Collider collider;
	
	protected CapabilityItem(CapabilityItem.Builder<?> builder) {
        this.weaponCategory = Objects.requireNonNullElse(builder.category, WeaponCategories.FIST);
		this.collider = builder.collider;
		this.id = builder.identifier;
		this.customData = ImmutableMap.copyOf(builder.customData);
		ImmutableMap.Builder<Style, Map<Holder<Attribute>, AttributeModifier>> attributeMapbuilder = ImmutableMap.builder();
		
		for (Map.Entry<Style, Map<Holder<Attribute>, AttributeModifier>> entry : builder.attributeMap.entrySet()) {
			attributeMapbuilder.put(entry.getKey(), (entry.getValue()));
		}
		
		this.attributeMap = attributeMapbuilder.build();
	}

	public void modifySimplyTooltip(ItemStack itemStack, List<Component> itemTooltip, LivingEntityPatch<?> entitypatch) {
		int index = 0;
		boolean modifyIn = false;

		for (int i = 0; i < itemTooltip.size(); i++) {
			Component textComp = itemTooltip.get(i);
			index = i;

			if (this.findComponentArgument(textComp, Attributes.ATTACK_SPEED.value().getDescriptionId()) != null) {
				modifyIn = true;
				break;
			}
		}

		index++;

		Map<Holder<Attribute>, AttributeModifier> attribute = this.getDamageAttributesInCondition(getStyle(entitypatch));

		if (attribute != null) {
			if (!modifyIn) {
				itemTooltip.add(index, Component.literal(""));
				index++;
				itemTooltip.add(index, Component.translatable("epicfight.gui.attribute").withStyle(ChatFormatting.GRAY));
				index++;
			}

			Holder<Attribute> armorNegation = EpicFightAttributes.ARMOR_NEGATION;
			Holder<Attribute> impact = EpicFightAttributes.IMPACT;
			Holder<Attribute> maxStrikes = EpicFightAttributes.MAX_STRIKES;
			Holder<Enchantment> impactful = entitypatch.getOriginal().level().holderOrThrow(EpicFightEnchantments.IMPACTFUL);
			Holder<Enchantment> armorPiercing = entitypatch.getOriginal().level().holderOrThrow(EpicFightEnchantments.ARMOR_PIERCING);


			if (attribute.containsKey(armorNegation) && validateAttribute(entitypatch, armorNegation)) {
				double value = attribute.get(armorNegation).amount() + entitypatch.getOriginal().getAttribute(armorNegation).getBaseValue();
				if (itemStack.getEnchantmentLevel(armorPiercing) > 0) {
					value *= 1.0D + itemStack.getEnchantmentLevel(armorPiercing) * 0.05D;
				}
				if (value > 0.0D) {
					itemTooltip.add(index, Component.literal(" ").append(Component.translatable(armorNegation.value().getDescriptionId() + ".value", ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(value))));
				}
			}

			if (attribute.containsKey(impact) && validateAttribute(entitypatch, impact)) {
				double value = attribute.get(impact).amount() + entitypatch.getOriginal().getAttribute(impact).getBaseValue();
				if (itemStack.getEnchantmentLevel(impactful) > 0) {
					value *= (1.0F + itemStack.getEnchantmentLevel(impactful) * 0.07F);
				}
				if (value > 0.0D) {
					int i = itemStack.getEnchantmentLevel(entitypatch.getOriginal().level().holderOrThrow(Enchantments.KNOCKBACK));
					value *= (1.0F + i * 0.12F);
					itemTooltip.add(index++, Component.literal(" ").append(Component.translatable(impact.value().getDescriptionId() + ".value", ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(value))));
				}
			}

			if (attribute.containsKey(maxStrikes) && validateAttribute(entitypatch, maxStrikes)) {
				double value = attribute.get(maxStrikes).amount() + entitypatch.getOriginal().getAttribute(maxStrikes).getBaseValue();

				if (value > 0.0D) {
					itemTooltip.add(index++, Component.literal(" ").append(Component.translatable(maxStrikes.value().getDescriptionId() + ".value", ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(value))));
				}
			} else {
				itemTooltip.add(index++, Component.literal(" ").append(Component.translatable(maxStrikes.value().getDescriptionId() + ".value", ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(maxStrikes.value().getDefaultValue()))));
			}
		}
	}

	public void modifyItemTooltip(ItemStack itemstack, List<Component> itemTooltip, LivingEntityPatch<?> entitypatch) {

		Style style = this instanceof RangedWeaponCapability ? Styles.RANGED : this.getStyle(entitypatch);
		itemTooltip.add(1, Component.translatable(EpicFight.MODID + ".style." + style.toString().toLowerCase(Locale.ROOT)).withStyle(ChatFormatting.DARK_GRAY));
		
		int index = 0;
		boolean modifyIn = false;

		for (int i = 0; i < itemTooltip.size(); i++) {
			Component textComp = itemTooltip.get(i);
			index = i;
			
			if (this.findComponentArgument(textComp, Attributes.ATTACK_SPEED.value().getDescriptionId()) != null) {
				modifyIn = true;
				break;
			}
		}
		
		index++;
		
		Map<Holder<Attribute>, AttributeModifier> attribute = this.getDamageAttributesInCondition(style);


		if (attribute != null) {
			if (!modifyIn) {
				itemTooltip.add(index, Component.literal(""));
				index++;
				itemTooltip.add(index, Component.translatable("epicfight.gui.attribute").withStyle(ChatFormatting.GRAY));
				index++;
			}

			Holder<Attribute> armorNegation = EpicFightAttributes.ARMOR_NEGATION;
			Holder<Attribute> impact = EpicFightAttributes.IMPACT;
			Holder<Attribute> maxStrikes = EpicFightAttributes.MAX_STRIKES;
			Holder<Enchantment> impactful = entitypatch.getOriginal().level().holderOrThrow(EpicFightEnchantments.IMPACTFUL);
			Holder<Enchantment> armorPiercing = entitypatch.getOriginal().level().holderOrThrow(EpicFightEnchantments.ARMOR_PIERCING);


			if (attribute.containsKey(armorNegation) && validateAttribute(entitypatch, armorNegation)) {
				double value = attribute.get(armorNegation).amount() + entitypatch.getOriginal().getAttribute(armorNegation).getBaseValue();
				if (itemstack.getEnchantmentLevel(armorPiercing) > 0) {
					value *= 1.0D + itemstack.getEnchantmentLevel(armorPiercing) * 0.05D;
				}
				if (value > 0.0D) {
					itemTooltip.add(index, Component.literal(" ").append(Component.translatable(armorNegation.value().getDescriptionId() + ".value", ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(value))));
				}
			}
			
			if (attribute.containsKey(impact) && validateAttribute(entitypatch, impact)) {
				double value = attribute.get(impact).amount() + entitypatch.getOriginal().getAttribute(impact).getBaseValue();
				if (itemstack.getEnchantmentLevel(impactful) > 0) {
					value *= (1.0F + itemstack.getEnchantmentLevel(impactful) * 0.07F);
				}
				if (value > 0.0D) {
					int i = itemstack.getEnchantmentLevel(entitypatch.getOriginal().level().holderOrThrow(Enchantments.KNOCKBACK));
					value *= (1.0F + i * 0.12F);
					itemTooltip.add(index++, Component.literal(" ").append(Component.translatable(impact.value().getDescriptionId() + ".value", ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(value))));
				}
			}
			
			if (attribute.containsKey(maxStrikes) && validateAttribute(entitypatch, maxStrikes)) {
				double value = attribute.get(maxStrikes).amount() + entitypatch.getOriginal().getAttribute(maxStrikes).getBaseValue();

				if (value > 0.0D) {
					itemTooltip.add(index++, Component.literal(" ").append(Component.translatable(maxStrikes.value().getDescriptionId() + ".value", ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(value))));
				}
			} else {
				itemTooltip.add(index++, Component.literal(" ").append(Component.translatable(maxStrikes.value().getDescriptionId() + ".value", ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(maxStrikes.value().getDefaultValue()))));
			}
		}
	}

	protected Object findComponentArgument(Component component, String key) {
		// check the content.
		if (component.getContents() instanceof TranslatableContents contents) {
			// is self?
			if (contents.getKey().equals(key)) {
				return component;
			}
			
			if (contents.getArgs() != null) {
				// check all arguments.
				for (Object arg : contents.getArgs()) {
					if (arg instanceof Component argComponent) {
						Object ret = this.findComponentArgument(argComponent, key);
						if (ret != null) {
							return ret;
						}
					}
				}
			}
		}
		// check all sibling.
		for (Component siblingComponent : component.getSiblings()) {
			Object ret = this.findComponentArgument(siblingComponent, key);
			if (ret != null) {
				return ret;
			}
		}
		
		return null;
	}
	
	public List<AnimationAccessor<? extends AttackAnimation>> getAutoAttackMotion(PlayerPatch<?> playerpatch) {
		return getBasicAutoAttackMotions();
	}

	public List<AnimationAccessor<? extends AttackAnimation>> getMountAttackMotion(PlayerPatch<?> playerPatch) {
		return getMountAttackMotion();
	}

	@SuppressWarnings("unchecked")
	public <T> Optional<T> getCustomData(DeferredCustomData<? extends CustomData<T>> data) {
		Object result = customData.get(data);
		return Optional.of((T) result);
	}


	/// Use {@link #getMountAttackMotion(PlayerPatch)} for dynamic assigning, this is used as legacy fallback.
    @Deprecated(forRemoval = true)
    public List<AnimationAccessor<? extends AttackAnimation>> getMountAttackMotion()
    {
        return null;
    }
	
	@Nullable
	public Skill getInnateSkill(PlayerPatch<?> playerpatch, ItemStack itemstack) {
		return null;
	}
	
	@Nullable
	public Skill getPassiveSkill(PlayerPatch<?> playerPatch) {
		return getPassiveSkill();
	}

    /// Use {@link #getPassiveSkill(PlayerPatch)} for dynamic allocation, this is primarily a fallback.
    @Deprecated(forRemoval = true) @Nullable
    public Skill getPassiveSkill()
    {
        return null;
    }

	public WeaponCategory getWeaponCategory() {
		return this.weaponCategory;
	}

	public boolean isWeaponCategory(WeaponCategory target) {
		if (this.weaponCategory == target) return true;

		// Population Phase: Use the ImmutableList directly
		List<WeaponCategory> immediateParents = weaponCategory.getParents();
		if (immediateParents.isEmpty()) return false;

		Deque<WeaponCategory> stack = new ArrayDeque<>(immediateParents);
		Set<WeaponCategory> visited = new HashSet<>();

		while (!stack.isEmpty()) {
			WeaponCategory current = stack.pop();
			if (current == target) return true;

			if (visited.add(current)) {
				// Because of ImmutableList, we know getParents() is safe to loop
				for (WeaponCategory p : current.getParents()) {
					stack.push(p);
				}
			}
		}
		return false;
	}
	
	public void changeWeaponInnateSkill(ServerPlayerPatch playerpatch, ItemStack itemstack) {
		Skill weaponInnateSkill = this.getInnateSkill(playerpatch, itemstack);
		SkillContainer weaponInnateSkillContainer = playerpatch.getSkill(SkillSlots.WEAPON_INNATE);
		PayloadBundleBuilder toLocal = PayloadBundleBuilder.create();
		PayloadBundleBuilder toRemote = PayloadBundleBuilder.create();
		EpicFight.LOGGER.debug("Capability Item Preset: {}", id);
		if (weaponInnateSkill != null) {
			if (weaponInnateSkillContainer.getSkill() != weaponInnateSkill) {
				weaponInnateSkillContainer.setSkill(weaponInnateSkill);
			}
			
			toLocal.and(new SPChangeSkill(SkillSlots.WEAPON_INNATE, playerpatch.getOriginal().getId(), weaponInnateSkill.holder()));
		} else {
			toLocal.and(SPSetSkillContainerValue.enable(SkillSlots.WEAPON_INNATE, true, playerpatch.getOriginal().getId()));
		}
		
		weaponInnateSkillContainer.setDisabled(weaponInnateSkill == null);
		
		toRemote.and(new SPSetRemotePlayerSkill(SkillSlots.WEAPON_INNATE, playerpatch.getOriginal().getId(), Skill.holderOrNull(weaponInnateSkill)));
		
		Skill passiveSkill = this.getPassiveSkill(playerpatch);
		SkillContainer passiveSkillContainer = playerpatch.getSkill(SkillSlots.WEAPON_PASSIVE);
		
		if (passiveSkill != null) {
			if (passiveSkillContainer.getSkill() != passiveSkill) {
				passiveSkillContainer.setSkill(passiveSkill);
				toLocal.and(new SPChangeSkill(SkillSlots.WEAPON_PASSIVE, playerpatch.getOriginal().getId(), passiveSkill.holder()));
				toRemote.and(new SPSetRemotePlayerSkill(SkillSlots.WEAPON_PASSIVE, playerpatch.getOriginal().getId(), passiveSkill.holder()));
			}
		} else {
			passiveSkillContainer.setSkill(null);
			toLocal.and(new SPChangeSkill(SkillSlots.WEAPON_PASSIVE, playerpatch.getOriginal().getId(), null));
			toRemote.and(new SPSetRemotePlayerSkill(SkillSlots.WEAPON_PASSIVE, playerpatch.getOriginal().getId(), null));
		}
		
		toLocal.send((first, others) -> {
			EpicFightNetworkManager.sendToPlayer(first, (ServerPlayer)playerpatch.getOriginal(), others);
		});
		
		toRemote.send((first, others) -> {
			EpicFightNetworkManager.sendToAllPlayerTrackingThisEntity(first, (ServerPlayer)playerpatch.getOriginal(), others);
		});
	}
	
	public SoundEvent getSmashingSound() {
		return EpicFightSounds.WHOOSH.get();
	}

	public SoundEvent getHitSound() {
		return EpicFightSounds.BLUNT_HIT.get();
	}

	public Collider getWeaponCollider() {
		return this.collider != null ? this.collider : ColliderPreset.FIST;
	}

	public HitParticleType getHitParticle() {
		return EpicFightParticles.HIT_BLUNT.get();
	}
	
	public final Map<Holder<Attribute>, AttributeModifier> getDamageAttributesInCondition(Style style) {
		Map<Holder<Attribute>, AttributeModifier> attributes = this.attributeMap.getOrDefault(style, Maps.newHashMap());
		this.attributeMap.getOrDefault(Styles.COMMON, Maps.newHashMap()).forEach(attributes::putIfAbsent);

		return attributes;
	}
	
	public Multimap<Holder<Attribute>, AttributeModifier> getAttributeModifiers(@Nullable LivingEntityPatch<?> entitypatch) {
		Multimap<Holder<Attribute>, AttributeModifier> map = HashMultimap.create();
		
		if (entitypatch != null) {
			Map<Holder<Attribute>, AttributeModifier> modifierMap = this.getDamageAttributesInCondition(this.getStyle(entitypatch));
			
			if (modifierMap != null) {
				for (Entry<Holder<Attribute>, AttributeModifier> entry : modifierMap.entrySet()) {
					map.put(entry.getKey(), entry.getValue());
				}
			}
		}
		
		return map;
    }
	
	public Multimap<Holder<Attribute>, AttributeModifier> getAllAttributeModifiers() {
		Multimap<Holder<Attribute>, AttributeModifier> map = HashMultimap.create();
		
		for (Map<Holder<Attribute>, AttributeModifier> attrMap : this.attributeMap.values()) {
			for (Entry<Holder<Attribute>, AttributeModifier> entry : attrMap.entrySet()) {
				map.put(entry.getKey(), entry.getValue());
			}
		}
		
		return map;
    }
	
	public Map<LivingMotion, AnimationAccessor<? extends StaticAnimation>> getLivingMotionModifier(LivingEntityPatch<?> playerpatch, InteractionHand hand) {
		return Maps.newHashMap();
	}

    @NotNull
	public Style getStyle(LivingEntityPatch<?> entitypatch) {
		return this.canBePlacedOffhand() ? Styles.ONE_HAND : Styles.TWO_HAND;
	}
	
	public AnimationAccessor<? extends StaticAnimation> getGuardMotion(GuardSkill skill, GuardSkill.BlockType blockType, PlayerPatch<?> playerpatch) {
		return null;
	}
	
	public boolean canBePlacedOffhand() {
		return true;
	}
	
    /// Use [#handleComboCounter(PlayerPatch, AnimationAccessor)] with animation sensitive version
    @Deprecated(forRemoval = true)
    public boolean shouldCancelCombo(LivingEntityPatch<?> entitypatch) {
        return true;
    }

    /// @param nextAnimation null when causal == [ModifyComboCounter.Causal#TIME_EXPIRED]
    public int handleComboCounter(ModifyComboCounter.Causal causal, PlayerPatch<?> entitypatch, @Nullable AnimationAccessor<? extends MainFrameAnimation> nextAnimation, int original) {
        return ModifyComboCounter.ComboCounterHandler.DEFAULT_COMBO_HANDLER.handleComboCounter(this, causal, entitypatch, nextAnimation, original);
    }

	public boolean isEmpty() {
		return this == CapabilityItem.EMPTY;
	}
	
	public CapabilityItem findRecursive(ItemStack item) {
		return this;
	}
	
	public boolean availableOnHorse(LivingEntityPatch<?> entityPatch) {
		return availableOnHorse();
	}

    /// Use {@link #availableOnHorse(LivingEntityPatch)} instead for allowing living entity patch parameterization.
    @Deprecated(forRemoval = true)
    public boolean availableOnHorse()
    {
        return true;
    }

	public boolean checkOffhandValid(LivingEntityPatch<?> entitypatch) {
		return this.getStyle(entitypatch).canUseOffhand() && EpicFightCapabilities.getItemStackCapability(entitypatch.getOriginal().getOffhandItem()).canHoldInOffhandAlone();
	}
	
	public boolean canHoldInOffhandAlone() {
		return true;
	}
	
	public float getReach() {
		return 0.0F;
	}
	
	/**
	 * Get a custom composite living motion when holding item
	 * @param entitypatch
	 * @return
	 */
	public LivingMotion getLivingMotion(LivingEntityPatch<?> entitypatch, InteractionHand hand) {
		return null;
	}
	
	/**
	 * Called when player attacks with holding this item {@link AttackAnimation#attackTick}
	 * 
	 * @param entitypatch
	 * @param animation
	 */
	public void onStrike(LivingEntityPatch<?> entitypatch, AttackAnimation animation) {
	}
	
	public UseAnim getUseAnimation(LivingEntityPatch<?> entitypatch) {
		return UseAnim.NONE;
	}
	
	public ZoomInType getZoomInType() {
		return ZoomInType.NONE;
	}

	public enum WeaponCategories implements WeaponCategory {
		NOT_WEAPON(WEAPON_CATEGORY_NOT_WEAPON),
        AXE(WEAPON_CATEGORY_AXE),
        FIST(WEAPON_CATEGORY_FIST),
        GREATSWORD(WEAPON_CATEGORY_GREATSWORD),
        HOE(WEAPON_CATEGORY_HOE),
        PICKAXE(WEAPON_CATEGORY_PICKAXE),
        SHOVEL(WEAPON_CATEGORY_SHOVEL),
        SWORD(WEAPON_CATEGORY_SWORD),
        UCHIGATANA(WEAPON_CATEGORY_UCHIGATANA, SWORD),
        SPEAR(WEAPON_CATEGORY_SPEAR),
        TACHI(WEAPON_CATEGORY_TACHI, SWORD),
        TRIDENT(WEAPON_CATEGORY_TRIDENT),
        LONGSWORD(WEAPON_CATEGORY_LONGSWORD, SWORD),
        DAGGER(WEAPON_CATEGORY_DAGGER),
        SHIELD(WEAPON_CATEGORY_SHIELD),
		RANGED(WEAPON_CATEGORY_RANGED);

        final Component translationKey;
		final List<WeaponCategory> parent;
		final int id;
		
		WeaponCategories(String translationKey, WeaponCategory... parents) {
            this.translationKey = Component.translatable(translationKey);
			this.id = WeaponCategory.ENUM_MANAGER.assign(this);
			this.parent = ImmutableList.copyOf(parents);
		}

		WeaponCategories(String translationKey) {
			this.translationKey = Component.translatable(translationKey);
			this.id = WeaponCategory.ENUM_MANAGER.assign(this);
			this.parent = ImmutableList.of();
		}

		@Override
		public List<WeaponCategory> getParents() {
			return parent;
		}

		@Override
        public Component getTranslatable() {
            return this.translationKey;
        }

		@Override
		public int universalOrdinal() {
			return this.id;
		}
	}
	
	public enum Styles implements Style {
		COMMON(true), ONE_HAND(true), TWO_HAND(false), MOUNT(true), RANGED(false), SHEATH(false), OCHS(false);
		
		final boolean canUseOffhand;
		final int id;
		
		Styles(boolean canUseOffhand) {
			this.id = Style.ENUM_MANAGER.assign(this);
			this.canUseOffhand = canUseOffhand;
		}
		
		@Override
		public int universalOrdinal() {
			return this.id;
		}
		
		public boolean canUseOffhand() {
			return this.canUseOffhand;
		}
	}
	
	public enum ZoomInType {
		NONE, ALWAYS, USE_TICK, AIMING, CUSTOM
	}
	
	public static CapabilityItem.Builder<?> builder() {
		return new CapabilityItem.Builder<> ();
	}
	
	@SuppressWarnings("unchecked")
	public static class Builder<T extends Builder<T>> {
		protected ResourceLocation parent;
		protected Function<T, CapabilityItem> constructor;
		protected Map<Style, Map<Holder<Attribute>, AttributeModifier>> attributeMap;
		protected WeaponCategory category;
		protected Collider collider;
		protected ResourceLocation identifier;
		protected Map<Holder<CustomData<?>>, Object> customData;

		protected Builder() {
			this.constructor = CapabilityItem::new;
			this.attributeMap = Maps.newHashMap();
			this.customData = Maps.newHashMap();
			this.category = null;
			this.collider = null;
		}

		public T copy()
		{
			Builder<T> result = new Builder<>();
			result.parent = this.parent;
			result.identifier = this.identifier;
			result.attributeMap = this.attributeMap;
			result.category = this.category;
			result.collider = this.collider;
			return (T) result;
		}

		protected void paste(T builder)
		{
			builder.parent = this.parent;
			builder.identifier = this.identifier;
			builder.attributeMap = this.attributeMap;
			builder.category = this.category;
			builder.collider = this.collider;
			builder.customData = this.customData;
		}

		public T parent(ResourceLocation parent)
		{
			this.parent = parent;
			return (T) this;
		}

		public T parent(DeferredPreset<T> preset)
		{
			return this.parent(preset.getId());
		}

		public T identifier(ResourceLocation id)
		{
			this.identifier = id;
			return (T) this;
		}
		
		public T constructor(Function<T, CapabilityItem> constructor) {
			this.constructor = constructor;
			return (T)this;
		}
		
		public T category(WeaponCategory category) {
			this.category = category;
			return (T)this;
		}
		
		public T collider(Collider collider) {
			this.collider = collider;
			return (T)this;
		}

		public void setCustomDataInternal(DeferredCustomData<? extends CustomData<?>> data, Object obj) {
			this.customData.put(data, obj);
		}

		public void setCustomDataInternal(Holder<CustomData<?>> data, Object obj) {
			this.customData.put(data, obj);
		}
		
		public T addStyleAttibutes(Style style, Holder<Attribute> attribute, AttributeModifier attributePair) {
			Map<Holder<Attribute>, AttributeModifier> map = this.attributeMap.computeIfAbsent(style, (key) -> new HashMap<> ());
			map.put(attribute, attributePair);
			return (T)this;
		}

		//Only handles generics, if you want to make a full weapon capability you need a full override
		protected T merge()
		{
			if (this.parent == null) {
				if (this.category == null) {
					this.category(WeaponCategories.FIST);
				}
				if (this.collider == null) {
					this.collider(ColliderPreset.FIST);
				}
				return (T) this;
			}
			T result = (T) CapabilityItem.builder();
			Deque<T> stack = new ArrayDeque<>();
			Builder<T> current = this;

			while (current != null) {
				stack.push((T) current);
				current = (T) ItemPresetManager.get(current.parent);
			}
			while (!stack.isEmpty()) {
				T builder = stack.pop();
				handleLayers(result, builder);
			}
            if (result.category == null) {
                result.category(WeaponCategories.FIST);
            }
            if (result.collider == null) {
                result.collider(ColliderPreset.FIST);
            }
			return (T)this;
		}

		protected void handleLayers(T result, T builder) {
			apply(result, builder);
		}

		@ApiStatus.Internal
		public void registerCustomData(Holder<CustomData<?>> data) {
			customData.putIfAbsent(data, data.value().defaultValue());
		}

		public <R> T setCustomData(DeferredCustomData<? extends CustomData<R>> customData, R data) {
			String modId = customData.getId().getNamespace();
			if (!ModList.get().isLoaded(modId)) return (T) this;
            this.setCustomDataInternal(customData, data);
			return (T)this;
		}

		public final CapabilityItem build() {
			return this.constructor.apply(this.merge());
		}
		
		public Collider getCollider() {
			return this.collider;
		}

		protected void apply(T result, T builder)
		{
			if (builder.constructor != null) {
				result.constructor = builder.constructor;
			}
			if (builder.attributeMap != null) {
				result.attributeMap.putAll(builder.attributeMap);
			}
			if (builder.category != null) {
				result.category = builder.category;
			}
			if (builder.collider != null) {
				result.collider = builder.collider;
			}
		}
	}


}
