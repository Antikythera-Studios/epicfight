package yesman.epicfight.world.capabilities.item;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import yesman.epicfight.EpicFight;
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.LivingMotion;
import yesman.epicfight.api.animation.LivingMotions;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.api.animation.types.MainFrameAnimation;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.event.types.player.ModifyComboCounter;
import yesman.epicfight.api.ex_cap.data.Moveset;
import yesman.epicfight.api.ex_cap.managers.ConditionalManager;
import yesman.epicfight.api.ex_cap.managers.ItemPresetManager;
import yesman.epicfight.api.ex_cap.managers.MovesetManager;
import yesman.epicfight.api.ex_cap.provider.CoreWeaponCapabilityProvider;
import yesman.epicfight.api.ex_cap.provider.ProviderConditional;
import yesman.epicfight.gameasset.ColliderPreset;
import yesman.epicfight.particle.HitParticleType;
import yesman.epicfight.registry.deferred.holders.DeferredConditional;
import yesman.epicfight.registry.deferred.holders.DeferredMoveset;
import yesman.epicfight.registry.entries.EpicFightAttributes;
import yesman.epicfight.registry.entries.EpicFightParticles;
import yesman.epicfight.registry.entries.EpicFightSkillDataKeys;
import yesman.epicfight.registry.entries.EpicFightSounds;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.SkillSlots;
import yesman.epicfight.skill.guard.GuardSkill;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public class WeaponCapability extends CapabilityItem {
    protected final CoreWeaponCapabilityProvider coreProvider;
    @Deprecated(since = "26.1", forRemoval = true)
	protected final Function<LivingEntityPatch<?>, Style> stylegetter;
    @Deprecated(since = "26.1", forRemoval = true)
	protected final Function<LivingEntityPatch<?>, Boolean> weaponCombinationPredicator;
    @Deprecated(since = "26.1", forRemoval = true)
	protected final Skill passiveSkill;
    protected final boolean offHandAlone;
	protected final SoundEvent smashingSound;
	protected final SoundEvent hitSound;
	protected final HitParticleType hitParticle;
    protected final Map<Style, Moveset> moveSets;
    @Deprecated(since = "26.1", forRemoval = true)
	protected final Map<Style, List<AnimationAccessor<? extends AttackAnimation>>> autoAttackMotions;
    @Deprecated(since = "26.1", forRemoval = true)
	protected final Map<Style, Function<ItemStack, Skill>> innateSkill;
    @Deprecated(since = "26.1", forRemoval = true)
	protected final Map<Style, Map<LivingMotion, AnimationAccessor<? extends StaticAnimation>>> livingMotionModifiers;
	protected final boolean canBePlacedOffhand;
    @Deprecated(since = "26.1", forRemoval = true)
    protected final Function<Style, Boolean> comboCancel;
    protected final ModifyComboCounter.ComboCounterHandler comboCounterHandler;
	protected final ZoomInType zoomInType;
	protected final float reach;

    /// A custom capability tag that eases identifying categories
    /// Weapon capabilities have a registry name of their weapon type builder
    protected Set<ResourceLocation> customTags;

	protected WeaponCapability(WeaponCapability.Builder builder) {
		super(builder);
        this.coreProvider = new CoreWeaponCapabilityProvider();
        builder.provider.forEach(rl  -> coreProvider.addConditional(ConditionalManager.get(rl).build()));
        this.moveSets = Maps.newHashMap();
        builder.moveSets.forEach( (style, set) -> this.moveSets.put(style, MovesetManager.getBuilder(set).build(set)));
        this.offHandAlone = builder.offHandAlone;
        this.autoAttackMotions = builder.autoAttackMotionMap;
		this.innateSkill = builder.innateSkillByStyle;
		this.livingMotionModifiers = builder.livingMotionModifiers;
		this.stylegetter = builder.styleProvider;
		this.weaponCombinationPredicator = builder.weaponCombinationPredicator;
		this.passiveSkill = builder.passiveSkill;
		this.smashingSound = builder.swingSound.value();
		this.hitParticle = builder.hitParticle.value() instanceof HitParticleType trueParticle ? trueParticle : (HitParticleType) EpicFightParticles.HIT_BLUNT.value();
		this.hitSound = builder.hitSound.value();
		this.canBePlacedOffhand = builder.canBePlacedOffhand;
		this.comboCancel = builder.comboCancel;
        this.comboCounterHandler = builder.comboCounterHandler;
		this.zoomInType = builder.zoomInType;
		this.reach = builder.reach;
        this.customTags = Collections.unmodifiableSet(builder.customTags);
        this.id = builder.identifier;
	}



    public Moveset getCurrentSet(LivingEntityPatch<?> patch)
    {
        // Offhand-only mirror mode: getStyle() goes through the cap's conditional list and the
        // dual-pair conditionals (DUAL_SWORDS / DUAL_DAGGERS) misfire because they only check
        // "offhand category", so a single weapon in the offhand still resolves to the dual
        // moveset. Re-evaluate while skipping those offhand-targeted WEAPON_CATEGORY conditionals
        // to get the natural single-wield style — TWO_HAND for longsword/katana (the moveset
        // that carries LONGSWORD_GUARD and LIECHTENAUER), ONE_HAND for a regular sword (the
        // moveset that carries SWORD_GUARD and SWEEPING_EDGE) — i.e. the same moveset the player
        // would see with the weapon in mainhand alone. The reference-equality check keeps the
        // bypass off the mainhand cap and off non-mirror sessions.
        if (patch.isMirrorMode() && patch.getHoldingItemCapability(InteractionHand.OFF_HAND) == this && this.moveSets != null) {
            Style natural = coreProvider.getNaturalSingleWieldStyle(patch);
            if (natural != null) {
                Moveset set = this.moveSets.get(natural);
                if (set != null) return set;
            }
            Moveset fallback = this.moveSets.get(Styles.COMMON);
            if (fallback != null) return fallback;
        }
        Style style = getStyle(patch);
        return moveSets.getOrDefault(style, moveSets.get(Styles.COMMON));
    }

    /**
     * Returns the moveset registered for an explicit style, bypassing {@link #getStyle}. Used by
     * the offhand-only universal-mirror path in {@code ComboAttacks}: when a weapon sits alone
     * in the offhand, the dual-pair conditionals (DUAL_SWORDS / DUAL_DAGGERS / ...) misfire
     * because they only check "is offhand category X?" without verifying the mainhand companion,
     * so {@code getStyle()} returns {@code TWO_HAND}. We bypass that and explicitly request the
     * one-handed moveset so the single-weapon combo plays instead of the dual combo. Falls back
     * to the {@code COMMON} moveset, then null, when nothing is registered for the style.
     */
    public Moveset getMovesetForStyle(Style style) {
        if (this.moveSets == null) return null;
        return this.moveSets.getOrDefault(style, this.moveSets.get(Styles.COMMON));
    }

    private AnimationAccessor<? extends StaticAnimation> processGuard(List<AnimationAccessor<? extends StaticAnimation>> motions, GuardSkill.BlockType blockType, PlayerPatch<?> playerpatch, SkillContainer container, int counter)
    {
        if (!motions.isEmpty()) {
            AnimationAccessor<? extends StaticAnimation> result = motions.get(counter % motions.size());
            if (blockType == GuardSkill.BlockType.ADVANCED_GUARD && !playerpatch.isLogicalClient()) {
                result = motions.get(container.getDataManager().getDataValue(EpicFightSkillDataKeys.PARRY_MOTION_COUNTER) % motions.size());
                container.getDataManager().setDataSyncF(EpicFightSkillDataKeys.PARRY_MOTION_COUNTER, count -> count + 1);
            }
            return result;
        }
        return null;
    }

    @Override
    public AnimationAccessor<? extends StaticAnimation> getGuardMotion(GuardSkill skill, GuardSkill.BlockType blockType, PlayerPatch<?> playerpatch)
    {
        Moveset currentSet = getCurrentSet(playerpatch);
        SkillContainer container = playerpatch.getSkill(SkillSlots.GUARD);
        int counter = blockType == GuardSkill.BlockType.ADVANCED_GUARD && container.getDataManager().hasData(EpicFightSkillDataKeys.PARRY_MOTION_COUNTER) ? container.getDataManager().getDataValue(EpicFightSkillDataKeys.PARRY_MOTION_COUNTER) : 0;
        if (currentSet != null) {
            Map<Skill, Map<GuardSkill.BlockType, List<AnimationAccessor<? extends StaticAnimation>>>> skillSpecificGuardMotions = currentSet.getSkillSpecificGuardAnimations();
            Map<GuardSkill.BlockType, List<AnimationAccessor<? extends StaticAnimation>>> defaultGuardMotions = currentSet.getDefaultGuardAnimations();
            if (skillSpecificGuardMotions != null && skillSpecificGuardMotions.containsKey(skill) && skillSpecificGuardMotions.get(skill).containsKey(blockType)) {
                List<AnimationAccessor<? extends StaticAnimation>> motions = skillSpecificGuardMotions.get(skill).get(blockType);
                return processGuard(motions, blockType, playerpatch, container, counter);
            } else if (defaultGuardMotions != null && defaultGuardMotions.containsKey(blockType)) {
                List<AnimationAccessor<? extends StaticAnimation>> motions = defaultGuardMotions.get(blockType);
                return processGuard(motions, blockType, playerpatch, container, counter);
            }
        }
        return super.getGuardMotion(skill, blockType, playerpatch);
    }

    @Override
	public final List<AnimationAccessor<? extends AttackAnimation>> getAutoAttackMotion(PlayerPatch<?> playerpatch) {
        Moveset set = getCurrentSet(playerpatch);
        if (set == null) {
            //Fallback
            List<AnimationAccessor<? extends AttackAnimation>> attacks = autoAttackMotions.getOrDefault(getStyle(playerpatch), autoAttackMotions.get(Styles.COMMON));
            if (attacks == null || attacks.isEmpty()) {
                return super.getAutoAttackMotion(playerpatch);
            }
            return attacks;
        }
		return set.getComboAttackAnimations();
	}
	
	@Override
	public final Skill getInnateSkill(PlayerPatch<?> playerpatch, ItemStack itemstack) {
        Moveset set = getCurrentSet(playerpatch);
        if (set == null) {
            //Fallback Logic
            if (innateSkill.get(getStyle(playerpatch)) == null)
                return null;
            return innateSkill.get(getStyle(playerpatch)).apply(itemstack);
        }
        return set.getWeaponInnateSkill() == null ? null : set.getWeaponInnateSkill().apply(itemstack, playerpatch);
	}
	
	@Override
	public Skill getPassiveSkill(PlayerPatch<?> playerPatch) {
		Moveset set = getCurrentSet(playerPatch);
        if (set == null) {
            //Fallback logic
            return getPassiveSkill();
        }
        return set.getWeaponPassiveSkill() != null ? set.getWeaponPassiveSkill().value() : null;
	}

    /// Legacy method
    @Deprecated(forRemoval = true)
    public Skill getPassiveSkill()
    {
        return passiveSkill;
    }

	@Override
	public final List<AnimationAccessor<? extends AttackAnimation>> getMountAttackMotion(PlayerPatch<?> playerpatch) {
        Moveset set = getCurrentSet(playerpatch);
        if (set == null) {
            //Fallback logic
            return this.autoAttackMotions.get(Styles.MOUNT);
        }
        return set.getMountAttackAnimations();
    }

    /// Legacy method used by addons
    @Deprecated(forRemoval = true, since = "1.1.0")
    public final List<AnimationAccessor<? extends AttackAnimation>> getMountAttackMotion()
    {
        return this.autoAttackMotions.get(Styles.MOUNT);
    }
	
	@Override @NotNull
	public Style getStyle(LivingEntityPatch<?> entityPatch) {
        Style style = coreProvider.getStyle(entityPatch);
        if (style == null)
        {
            return this.stylegetter.apply(entityPatch);
        }
        return style;
	}

    /// Public accessor for {@link CoreWeaponCapabilityProvider#getNaturalSingleWieldStyle}. Returns
    /// the style this cap would use if held alone on either hand (TWO_HAND for longsword/katana,
    /// ONE_HAND for sword/dagger), bypassing the dual-pair offhand misfire described in the
    /// provider. Used by {@link LivingEntityPatch#isMainhandItemValid} to suppress the mainhand
    /// item render when a two-handed-by-nature weapon sits alone in the offhand.
    public Style getNaturalSingleWieldStyle(LivingEntityPatch<?> entityPatch) {
        return coreProvider.getNaturalSingleWieldStyle(entityPatch);
    }

	@Override
	public SoundEvent getSmashingSound() {
		return this.smashingSound;
	}
	
	@Override
	public SoundEvent getHitSound() {
		return this.hitSound;
	}
	
	@Override
	public HitParticleType getHitParticle() {
		return this.hitParticle;
	}
	
	@Override
	public boolean canBePlacedOffhand() {
		return this.canBePlacedOffhand;
	}
	
	@Override @Deprecated(forRemoval = true)
	public boolean shouldCancelCombo(LivingEntityPatch<?> entitypatch) {
		return this.comboCancel.apply(this.getStyle(entitypatch));
	}

    @Override
    public int handleComboCounter(ModifyComboCounter.Causal causal, PlayerPatch<?> entitypatch, @Nullable AnimationAccessor<? extends MainFrameAnimation> nextAnimation, int original) {
        return this.comboCounterHandler.handleComboCounter(this, causal, entitypatch, nextAnimation, original);
    }

	@Override
	public ZoomInType getZoomInType() {
		return this.zoomInType;
	}

    @Override
	public Map<LivingMotion, AnimationAccessor<? extends StaticAnimation>> getLivingMotionModifier(LivingEntityPatch<?> player, InteractionHand hand) {
		Moveset set = getCurrentSet(player);
		// getCurrentSet handles the offhand-only mirror-mode bypass centrally: it re-evaluates
		// the cap's style providers while filtering out the dual-pair conditionals that misfire
		// when this cap sits alone in the offhand. So the natural single-wield moveset comes
		// back here -- TWO_HAND for longsword (LONGSWORD_GUARD + LIECHTENAUER hold/walk),
		// ONE_HAND for a regular sword -- and the player's hold/walk/block modifiers match what
		// they'd see with the weapon in mainhand. The visual flip is applied once at the
		// renderer in ClientAnimator.getPose.
		boolean offhandMirror = hand == InteractionHand.OFF_HAND && player.isMirrorMode();
        if (set == null || set.getLivingMotionModifiers() == null)
        {
            if (this.livingMotionModifiers == null || hand == InteractionHand.OFF_HAND && !offhandMirror) {
                return super.getLivingMotionModifier(player, hand);
            }
            Style legacyStyle = offhandMirror ? coreProvider.getNaturalSingleWieldStyle(player) : this.getStyle(player);
            if (legacyStyle == null) legacyStyle = Styles.ONE_HAND;
            Map<LivingMotion, AnimationAccessor<? extends StaticAnimation>> motions = this.livingMotionModifiers.getOrDefault(legacyStyle, Maps.newHashMap());
            this.livingMotionModifiers.getOrDefault(Styles.COMMON, Maps.newHashMap()).forEach(motions::putIfAbsent);

            return motions;
        }
        Map<LivingMotion, AnimationAccessor<? extends StaticAnimation>> result = Maps.newHashMap();
        result.putAll(set.getLivingMotionModifiers());
        return result;
	}
	
	@Override
	public UseAnim getUseAnimation(LivingEntityPatch<?> entityPatch) {
        Moveset set = getCurrentSet(entityPatch);
        if (set == null || set.getLivingMotionModifiers() == null)
        {
            //Fallback
            if (this.livingMotionModifiers != null) {
                Style style = this.getStyle(entityPatch);
                if (this.livingMotionModifiers.containsKey(style)) {
                    if (this.livingMotionModifiers.get(style).containsKey(LivingMotions.BLOCK)) {
                        return UseAnim.BLOCK;
                    }
                }
            }
        }
		else if (set.getLivingMotionModifiers().containsKey(LivingMotions.BLOCK)) {
            return UseAnim.BLOCK;
        }
		return UseAnim.NONE;
	}
	
	@Override
	public boolean canHoldInOffhandAlone() {
		return offHandAlone;
	}
	
	@Override
	public boolean checkOffhandValid(LivingEntityPatch<?> entitypatch) {
        Boolean valid = coreProvider.checkVisibleOffHand(entitypatch);
        if (valid == null) {
            valid = super.checkOffhandValid(entitypatch) || weaponCombinationPredicator.apply(entitypatch);
        } else {
            valid = valid || super.checkOffhandValid(entitypatch);
        }
        return valid;
	}
	
	@Override
	public boolean availableOnHorse(LivingEntityPatch<?> entityPatch) {
        Moveset set = getCurrentSet(entityPatch);
        if (set == null || set.getMountAttackAnimations() == null || set.getMountAttackAnimations().isEmpty())
		    return availableOnHorse();
        return true;
	}

    @Override
    public boolean availableOnHorse() {
        return this.autoAttackMotions.containsKey(Styles.MOUNT);
    }

    @Override
	public float getReach() {
		return this.reach;
	}
	
	public static WeaponCapability.Builder builder() {
        return new Builder();
	}

    @Override
    public LivingMotion getLivingMotion(LivingEntityPatch<?> entitypatch, InteractionHand hand) {
        Moveset set = getCurrentSet(entitypatch);
        // getCurrentSet has the offhand-only mirror-mode bypass; it returns the natural
        // single-wield moveset for this cap so the custom motion (e.g. LIECHTENAUER hold) lines
        // up with what the player would see in mainhand.
        if (set == null || set.getCustomMotion().apply(entitypatch, hand) == null)
            return super.getLivingMotion(entitypatch, hand);
        return set.getCustomMotion().apply(entitypatch, hand);
    }

    public boolean hasMatchingTag(ResourceLocation rl) {
        return customTags.contains(rl);
    }

    public Set<ResourceLocation> getTags() {
        return customTags;
    }

    /// All fields marked with {@link Deprecated} have been moved to {@link Moveset} and exist as legacy fallback options to prevent addons from breaking.
    public static class Builder extends CapabilityItem.Builder<WeaponCapability.Builder> {
        /** List of resource locations for conditional logic providers. */
        List<ResourceLocation> provider;
        /** @deprecated Moved to {@link Moveset}. Fallback for determining the current combat style. */
        @Deprecated(forRemoval = true) Function<LivingEntityPatch<?>, Style> styleProvider;
        /** @deprecated Moved to {@link Moveset}. Determines if specific weapon combinations are valid. */
        @Deprecated(forRemoval = true) Function<LivingEntityPatch<?>, Boolean> weaponCombinationPredicator;
        /** @deprecated Moved to {@link Moveset}. The passive skill granted by this weapon. */
        @Deprecated(forRemoval = true) Skill passiveSkill;
		Holder<SoundEvent> swingSound;
		Holder<SoundEvent> hitSound;
		Holder<ParticleType<?>> hitParticle;
        protected BiConsumer<Item, Builder> explicitItemOverride = null;
        Map<Style, ResourceLocation> moveSets;
        double baseAP;
        double aPScaling;
        final Map<Style, Moveset.Builder> pendingBuilders;
        final List<ProviderConditional.Builder> pendingConditionals;
        double impactBase;
        double impactScaling;
        /** @deprecated Use {@link Moveset}. Maps styles to auto-attack animation sequences. */
        @Deprecated(forRemoval = true) Map<Style, List<AnimationAccessor<? extends AttackAnimation>>> autoAttackMotionMap;
        /** @deprecated Use {@link Moveset}. Maps styles to the innate skill they provide. */
        @Deprecated(forRemoval = true) Map<Style, Function<ItemStack, Skill>> innateSkillByStyle;
        /** @deprecated Use {@link Moveset}. Modifies living animations (walking, idling) based on style. */
        @Deprecated(forRemoval = true) Map<Style, Map<LivingMotion, AnimationAccessor<? extends StaticAnimation>>> livingMotionModifiers;
        /** @deprecated Use {@link #comboCounterHandler}. Logic for resetting/canceling combos. */
        @Deprecated Function<Style, Boolean> comboCancel;
        ModifyComboCounter.ComboCounterHandler comboCounterHandler;
		boolean canBePlacedOffhand;
		ZoomInType zoomInType;
		float reach;
        boolean offHandAlone;

        Set<ResourceLocation> customTags = new HashSet<> ();

        public Builder copy() {
            Builder copy = new Builder();
            super.paste(copy);
            copy.constructor = this.constructor;
            copy.provider.addAll(this.provider);
            copy.category = this.category;
            copy.styleProvider = this.styleProvider;
            copy.weaponCombinationPredicator = this.weaponCombinationPredicator;
            copy.passiveSkill = this.passiveSkill;
            copy.offHandAlone = this.offHandAlone;
            copy.collider = this.collider;
            copy.attributeMap.putAll(this.attributeMap);

            copy.swingSound = this.swingSound;
            copy.hitSound = this.hitSound;
            copy.hitParticle = this.hitParticle;

            copy.comboCancel = this.comboCancel;
            copy.comboCounterHandler = this.comboCounterHandler;

            copy.canBePlacedOffhand = this.canBePlacedOffhand;
            copy.zoomInType = this.zoomInType;
            copy.reach = this.reach;

            if (this.moveSets != null) {
                copy.moveSets = Maps.newHashMap();
                copy.moveSets.putAll(this.moveSets);
            }
            if (this.autoAttackMotionMap != null) {
                copy.autoAttackMotionMap = Maps.newHashMap();
                for (Map.Entry<Style, List<AnimationAccessor<? extends AttackAnimation>>> entry
                        : this.autoAttackMotionMap.entrySet()) {

                    copy.autoAttackMotionMap.put(
                            entry.getKey(),
                            Lists.newArrayList(entry.getValue())
                    );
                }
            }

            if (this.innateSkillByStyle != null) {
                copy.innateSkillByStyle = Maps.newHashMap(this.innateSkillByStyle);
            }

            if (this.livingMotionModifiers != null) {
                copy.livingMotionModifiers = Maps.newHashMap();

                for (Map.Entry<Style, Map<LivingMotion, AnimationAccessor<? extends StaticAnimation>>> entry
                        : this.livingMotionModifiers.entrySet()) {

                    copy.livingMotionModifiers.put(
                            entry.getKey(),
                            Maps.newHashMap(entry.getValue())
                    );
                }
            }

            copy.customTags.addAll(this.customTags);
            return copy;
        }


		protected Builder() {
            super();
            this.category = null;
            this.provider = Lists.newArrayList();
            this.offHandAlone = false;
            this.pendingBuilders = Maps.newHashMap();
            this.pendingConditionals = Lists.newArrayList();
			this.constructor = WeaponCapability::new;
			this.styleProvider = (entitypatch) -> Styles.ONE_HAND;
			this.weaponCombinationPredicator = (entitypatch) -> false;
			this.passiveSkill = null;
			this.swingSound = null;
			this.hitSound = null;
            this.moveSets = Maps.newHashMap();
			this.hitParticle = null;
			this.autoAttackMotionMap = Maps.newHashMap();
			this.innateSkillByStyle = Maps.newHashMap();
			this.livingMotionModifiers = null;
			this.canBePlacedOffhand = true;
			this.comboCancel = (style) -> true;
            this.comboCounterHandler = null;
			this.zoomInType = ZoomInType.NONE;
			this.reach = 0.2F;
            this.baseAP = 0;
            this.aPScaling = 1;
            this.impactBase = 1;
            this.impactScaling = 1;
		}

        public Builder identifier(ResourceLocation id)
        {
            return super.identifier(id);
        }

        /**
         * Configures whether the weapon functions independently in the off-hand.
         * @param offHandAlone True for independent off-hand logic.
         * @return This builder for chaining.
         */
        public Builder offHandAlone(final boolean offHandAlone) {
            this.offHandAlone = offHandAlone;
            return this;
        }

        public void exportBuiltMovesets()
        {
            pendingBuilders.forEach(
                    ((style, builder) -> {
                        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
                                identifier.getNamespace(),
                                identifier.getPath() + "/generated/" + style.toString().toLowerCase(Locale.ROOT)
                        );

                        MovesetManager.addMoveset(id, builder);
                        this.addMoveset(style, id);
                    })
            );
        }

        public void exportBuiltConditionals()
        {
            pendingConditionals.forEach(
                    builder -> {
                        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
                                identifier.getNamespace(),
                                identifier.getPath() + "/generated/" + builder.getWieldStyle().toString().toLowerCase(Locale.ROOT)
                        );
                        ConditionalManager.addConditional(id, builder);
                        this.addConditionals(id);
                    }
            );
        }

        @ApiStatus.Internal
        public void removeConditional(ResourceLocation rl)
        {
            this.provider.remove(rl);
        }

        @ApiStatus.Internal
        public void removeMoveset(Style style)
        {
            this.moveSets.remove(style);
        }


        @Deprecated(forRemoval = true)
		public Builder styleProvider(Function<LivingEntityPatch<?>, Style> styleProvider) {
			this.styleProvider = styleProvider;
			return this;
		}

        /**
         * Sets the scaling values used to calculate attributes based on weapon tier.
         * @param baseAP         Base Armor Penetration.
         * @param aPScaling      Armor Penetration gained per tier.
         * @param impactBase     Base Impact/Knockback.
         * @param impactScaling  Impact gained per tier.
         * @return This builder for chaining.
         */
        public Builder setTierValues(double baseAP, double aPScaling, double impactBase, double impactScaling)
        {
            this.baseAP = baseAP;
            this.aPScaling = aPScaling;
            this.impactBase = impactBase;
            this.impactScaling = impactScaling;
            return this;
        }

        /**
         * Calculates and applies attributes to the weapon based on its tier.
         * <p>Internal use only during registry events.</p>
         * @param tier The numerical tier of the item.
         */
        @ApiStatus.Internal
        public void modifyTierAttributes(int tier)
        {
            if (tier != 0) this.addStyleAttibutes(Styles.COMMON, EpicFightAttributes.ARMOR_NEGATION, EpicFightAttributes.getArmorNegationModifier(baseAP + aPScaling * tier));
            this.addStyleAttibutes(Styles.COMMON, EpicFightAttributes.IMPACT, EpicFightAttributes.getImpactModifier(impactBase + impactScaling * tier));
        }

        @Deprecated(forRemoval = true)
		public Builder passiveSkill(Skill passiveSkill) {
			this.passiveSkill = passiveSkill;
			return this;
		}

		public Builder swingSound(Holder<SoundEvent> swingSound) {
			this.swingSound = swingSound;
			return this;
		}

        /**
         * @deprecated Use {@link #swingSound(Holder)} instead for safely handling sound events.
         * @param swingSound the raw object
         * @return the builder
         */
        @Deprecated
        public Builder swingSound(SoundEvent swingSound) {
            return swingSound(Holder.direct(swingSound));
        }

        /**
         * @deprecated Use {@link #swingSound(Holder)} instead for safely handling sound events.
         * @param hitParticle the raw object
         * @return the builder
         */
        @Deprecated
        public Builder hitParticle(HitParticleType hitParticle) {
            return hitParticle(Holder.direct(hitParticle));
        }

        /**
         * Links an external conditional provider to this weapon.
         * @param conditionals ResourceLocations of the conditionals.
         * @return This builder for chaining.
         */
        public Builder addConditionals(ResourceLocation... conditionals)
        {
            provider.addAll(Arrays.asList(conditionals));
            return this;
        }

        /**
         * Registers a conditional logic block and attaches it to this weapon.
         * @param builders The builders for the conditional logic.
         * @return This builder for chaining.
         */
        public Builder addConditionals(ProviderConditional.Builder... builders)
        {
            this.pendingConditionals.addAll(Arrays.asList(builders));
            return this;
        }

        /**
         * Registers a moveset anonymously by generating a ResourceLocation based on the style.
         * <p>
         * This method constructs a unique identifier using the pattern {@code [namespace]/[style_name]},
         * registers the moveset via the {@link MovesetManager}, and appends it to this builder.
         *
         * @param style   The visual or functional {@link Style} to associate with this moveset.
         * @param builder A builder containing the moveset data to be registered.
         * @return This builder instance for method chaining (Fluent API).
         * @throws NullPointerException if style or builder is null.
         */
        public Builder addMoveset(@NotNull Style style, @NotNull Moveset.Builder builder)
        {
            this.pendingBuilders.put(style, builder);
            return this;
        }

        @ApiStatus.Internal
        public void addMovesets(Map<Style, ResourceLocation> moveSets) {
            this.moveSets.putAll(moveSets);
        }

        @ApiStatus.Internal
        public Builder addConditionals(List<ResourceLocation> conditionals)
        {
            provider.addAll(conditionals);
            return this;
        }

        public Builder addConditionals(DeferredConditional... conditionals)
        {
            List<DeferredConditional> rls = Arrays.asList(conditionals);
            rls.forEach(conditionalEntry -> provider.add(conditionalEntry.getId()));
            return this;
        }


		public Builder hitSound(Holder<SoundEvent> hitSound) {
			this.hitSound = hitSound;
			return this;
		}

        /**
         * @deprecated Use {@link #hitSound(Holder)} instead for safely handling sound events.
         */
        @Deprecated(forRemoval = true)
        public Builder hitSound(SoundEvent hitSound) {
            this.hitSound = Holder.direct(hitSound);
            return this;
        }

		public Builder hitParticle(Holder<ParticleType<?>> hitParticle) {
			this.hitParticle = hitParticle;
			return this;
		}


        public Builder addMoveset(Style style, ResourceLocation moveSet) {
            moveSets.put(style, moveSet);
            return this;
        }

        public Builder addMoveset(Style style, DeferredMoveset moveSet) {
            this.addMoveset(style, moveSet.getId());
            return this;
        }

		public Builder canBePlacedOffhand(boolean canBePlacedOffhand) {
			this.canBePlacedOffhand = canBePlacedOffhand;
			return this;
		}
		
		public Builder reach(float reach) {
			this.reach = reach;
			return this;
		}

        /**
         * Adds a custom tag to this weapon for compatibility or filtering.
         * @param customTag ResourceLocation of the tag.
         * @return This builder for chaining.
         */
        public Builder addTag(ResourceLocation customTag) {
            this.customTags.add(customTag);
            return this;
        }


        @Deprecated(forRemoval = true)
        public Builder livingMotionModifier(Style wieldStyle, LivingMotion livingMotion, AnimationAccessor<? extends StaticAnimation> animation) {
			if (AnimationManager.checkNull(animation)) {
                EpicFight.LOGGER.warn("Unable to put an empty animation to weapon capability builder: {}, {}", livingMotion, animation);
				return this;
			}

			if (this.livingMotionModifiers == null) {
				this.livingMotionModifiers = Maps.newHashMap();
			}

			if (!this.livingMotionModifiers.containsKey(wieldStyle)) {
				this.livingMotionModifiers.put(wieldStyle, Maps.newHashMap());
			}

			this.livingMotionModifiers.get(wieldStyle).put(livingMotion, animation);

			return this;
		}

		@SafeVarargs @Deprecated(forRemoval = true, since = "1.21.1")
		public final Builder newStyleCombo(Style style, AnimationAccessor<? extends AttackAnimation>... animation) {
			this.autoAttackMotionMap.put(style, Lists.newArrayList(animation));
			return this;
		}

        @Deprecated(forRemoval = true, since = "1.21.1")
		public Builder weaponCombinationPredicator(Function<LivingEntityPatch<?>, Boolean> predicator) {
			this.weaponCombinationPredicator = predicator;
			return this;
		}

        @Deprecated(forRemoval = true, since = "1.21.1")
		public Builder innateSkill(Style style, Function<ItemStack, Skill> innateSkill) {
			this.innateSkillByStyle.put(style, innateSkill);
			return this;
		}

        /// @deprecated - Use a more sensitive version [#comboCounterHandler]
        @Deprecated
		public Builder comboCancel(Function<Style, Boolean> comboCancel) {
			this.comboCancel = comboCancel;
			return this;
		}

        /**
         * Sets the handler responsible for managing combo counters.
         * @param comboHandler The handler implementation.
         * @return This builder for chaining.
         */
        public Builder comboCounterHandler(ModifyComboCounter.ComboCounterHandler comboHandler) {
            this.comboCounterHandler = comboHandler;
            return this;
        }

		public Builder zoomInType(ZoomInType zoomInType) {
			this.zoomInType = zoomInType;
			return this;
		}

		public Map<Style, List<AnimationAccessor<? extends AttackAnimation>>> getComboAnimations() {
			return ImmutableMap.copyOf(this.autoAttackMotionMap);
		}

        public Builder explicitItemOverride(BiConsumer<Item, Builder> explicitItemOverride) {
            this.explicitItemOverride = explicitItemOverride;
            return this;
        }

        @ApiStatus.Internal
        public void handleOverrides(Item item) {
            if (explicitItemOverride != null) {
                explicitItemOverride.accept(item, this);
            }
        }

        @Override
        protected Builder merge() {
            if (this.parent == null) {
                if (this.category == null) {
                    this.category(WeaponCategories.FIST);
                }
                if (this.collider == null) {
                    this.collider(ColliderPreset.FIST);
                }
                if (this.swingSound == null) {
                    this.swingSound(EpicFightSounds.WHOOSH);
                }
                if (this.hitSound == null) {
                    this.hitSound(EpicFightSounds.BLUNT_HIT);
                }
                if (this.hitParticle == null) {
                    this.hitParticle(EpicFightParticles.HIT_BLADE);
                }
                if (this.comboCounterHandler == null) {
                    this.comboCounterHandler(ModifyComboCounter.ComboCounterHandler.DEFAULT_COMBO_HANDLER);
                }
                if (this.zoomInType == null) {
                    this.zoomInType(ZoomInType.NONE);
                }
                return this;
            }
            Builder result = WeaponCapability.builder();
            Deque<CapabilityItem.Builder<?>> stack = new ArrayDeque<>();
            CapabilityItem.Builder<?> current = this;

            while (current != null) {
                stack.push(current);
                current = ItemPresetManager.get(current.parent);
            }
            while (!stack.isEmpty()) {
                CapabilityItem.Builder<?> builder = stack.pop();
                applyWeapon(result, builder);
            }
            if (result.category == null) {
                result.category(WeaponCategories.FIST);
            }
            if (result.collider == null) {
                result.collider(ColliderPreset.FIST);
            }
            if (result.swingSound == null) {
                result.swingSound(EpicFightSounds.WHOOSH);
            }
            if (result.hitSound == null) {
                result.hitSound(EpicFightSounds.BLUNT_HIT);
            }
            if (result.hitParticle == null)
            {
                result.hitParticle(EpicFightParticles.HIT_BLADE);
            }
            if (result.comboCounterHandler == null) {
                result.comboCounterHandler(ModifyComboCounter.ComboCounterHandler.DEFAULT_COMBO_HANDLER);
            }
            if (result.zoomInType == null) {
                result.zoomInType(ZoomInType.NONE);
            }
            return result;
        }
    }

    public static void applyWeapon(Builder result, CapabilityItem.Builder<?> builder) {
        if (builder.attributeMap != null) {
            result.attributeMap.putAll(builder.attributeMap);
        }
        if (builder.category != null) {
            result.category = builder.category;
        }
        if (builder.collider != null) {
            result.collider = builder.collider;
        }
        result.identifier = builder.identifier;
        if (builder instanceof WeaponCapability.Builder weaponBuilder) {

            if (weaponBuilder.swingSound != null) {
                result.swingSound = weaponBuilder.swingSound;
            }
            if (weaponBuilder.hitSound != null) {
                result.hitSound = weaponBuilder.hitSound;
            }
            if (weaponBuilder.hitParticle != null) {
                result.hitParticle = weaponBuilder.hitParticle;
            }

            result.baseAP = weaponBuilder.baseAP;
            result.aPScaling = weaponBuilder.aPScaling;
            result.impactBase = weaponBuilder.impactBase;
            result.impactScaling = weaponBuilder.impactScaling;
            result.reach = weaponBuilder.reach;
            result.zoomInType = weaponBuilder.zoomInType;
            result.canBePlacedOffhand = weaponBuilder.canBePlacedOffhand;
            result.offHandAlone = weaponBuilder.offHandAlone;

            if (weaponBuilder.provider != null) result.provider.addAll(weaponBuilder.provider);
            if (weaponBuilder.moveSets != null) result.moveSets.putAll(weaponBuilder.moveSets);
            if (weaponBuilder.pendingBuilders != null) result.pendingBuilders.putAll(weaponBuilder.pendingBuilders);
            if (weaponBuilder.pendingConditionals != null) result.pendingConditionals.addAll(weaponBuilder.pendingConditionals);
            if (weaponBuilder.customData != null) result.customData.putAll(weaponBuilder.customData);

            result.comboCounterHandler = weaponBuilder.comboCounterHandler;

            if (weaponBuilder.styleProvider != null) result.styleProvider = weaponBuilder.styleProvider;
            if (weaponBuilder.weaponCombinationPredicator != null) result.weaponCombinationPredicator = weaponBuilder.weaponCombinationPredicator;
            if (weaponBuilder.passiveSkill != null) result.passiveSkill = weaponBuilder.passiveSkill;

            if (weaponBuilder.autoAttackMotionMap != null) {
                result.autoAttackMotionMap.putAll(weaponBuilder.autoAttackMotionMap);
            }
            if (weaponBuilder.innateSkillByStyle != null) {
                result.innateSkillByStyle.putAll(weaponBuilder.innateSkillByStyle);
            }
            if (weaponBuilder.livingMotionModifiers != null) {
                if (result.livingMotionModifiers == null) result.livingMotionModifiers = Maps.newHashMap();
                result.livingMotionModifiers.putAll(weaponBuilder.livingMotionModifiers);
            }
            if (weaponBuilder.comboCancel != null) result.comboCancel = weaponBuilder.comboCancel;
        }

    }
}