package yesman.epicfight.skill;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.ApiStatus;
import yesman.epicfight.api.animation.types.EntityState;
import yesman.epicfight.api.event.EntityEventListener;
import yesman.epicfight.api.event.EpicFightEventHooks;
import yesman.epicfight.api.event.IdentifierProvider;
import yesman.epicfight.api.event.types.player.SkillCancelEvent;
import yesman.epicfight.api.event.types.player.SkillCastEvent;
import yesman.epicfight.api.utils.side.ClientOnly;
import yesman.epicfight.client.events.engine.ControlEngine;
import yesman.epicfight.client.gui.BattleModeGui;
import yesman.epicfight.client.gui.screen.SkillBookScreen;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;
import yesman.epicfight.EpicFight;
import yesman.epicfight.main.EpicFightSharedConstants;
import yesman.epicfight.network.EpicFightNetworkManager;
import yesman.epicfight.network.client.CPSkillRequest;
import yesman.epicfight.network.server.SPSetSkillContainerValue;
import yesman.epicfight.network.server.SPSkillFeedback;
import yesman.epicfight.registry.EpicFightRegistries;
import yesman.epicfight.skill.modules.ChargeableSkill;
import yesman.epicfight.skill.modules.HoldableSkill;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.capabilities.item.WeaponCategory;

import javax.annotation.Nullable;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;

public abstract class Skill implements IdentifierProvider {
	public static final Codec<Holder<Skill>> CODEC = EpicFightRegistries.SKILL.holderByNameCodec();
	public static final StreamCodec<RegistryFriendlyByteBuf, Holder<Skill>> STREAM_CODEC = ByteBufCodecs.holderRegistry(EpicFightRegistries.Keys.SKILL);
	
	public record ModifierEntry(Holder<Attribute> attribute, AttributeModifier modifier) {
	}
	
	public static final Codec<List<ModifierEntry>> ATTRIBUTE_ENTRY_CODEC = Codec.list(
		RecordCodecBuilder.create(instance -> 
			instance.group(
				Attribute.CODEC.fieldOf("attribute").forGetter(ModifierEntry::attribute),
				AttributeModifier.MAP_CODEC.forGetter(ModifierEntry::modifier)
			)
			.apply(instance, ModifierEntry::new)
		)
	);
	
	public static Holder<Skill> holderOrNull(Skill skill) {
		return skill == null ? null : skill.holder();
	}
	
	public static Skill skillOrNull(Holder<Skill> skill) {
		return skill == null ? null : skill.value();
	}
	
	@SuppressWarnings("unchecked")
	public static <B extends SkillBuilder<B>> B createBuilder(Function<B, ? extends Skill> constructor) {
		return (B)new SkillBuilder<> (constructor);
	}
	
	public static <B extends SkillBuilder<B>> B createIdentityBuilder(Function<B, ? extends Skill> constructor) {
		return new SkillBuilder<> (constructor).setCategory(SkillCategories.IDENTITY).setResource(Resource.NONE);
	}
	
	public static <B extends SkillBuilder<B>> B createMoverBuilder(Function<B, ? extends Skill> constructor) {
		return new SkillBuilder<> (constructor).setCategory(SkillCategories.MOVER).setResource(Resource.STAMINA);
	}
	
	public static final Skill EMPTY = new Skill() {};
	
	private final Map<Holder<Attribute>, AttributeModifier> attributes = new HashMap<> ();
	protected final ResourceLocation registryName;
	protected final SkillCategory category;
	protected final CreativeModeTab creativeTab;
	protected final ActivateType activateType;
	protected final Resource resource;
	protected float consumption;
	protected int maxDuration;
	protected int maxStackSize;

	protected Holder<Skill> holder;

	public Skill(SkillBuilder<?> builder) {
		if (builder.registryName == null) {
			throw new IllegalArgumentException("No registry identifier is given for the skill " + this.getClass().getCanonicalName());
		}

		this.registryName = builder.registryName;
		this.category = builder.category;
		this.creativeTab = builder.tab;
		this.activateType = builder.activateType;
		this.resource = builder.resource;
	}

	@ApiStatus.Internal
	private Skill() {
        this.registryName = EpicFight.identifier("empty");
		this.category = SkillCategories.EMPTY;
		this.creativeTab = null;
		this.activateType = ActivateType.ONE_SHOT;
		this.resource = Resource.NONE;
	}

	/// Load parameters from datapack consists as [CompoundTag]
	/// Old data must be cleared
	public void loadDatapackParameters(CompoundTag parameters) {
		this.consumption = parameters.getFloat("consumption");
		this.maxDuration = parameters.getInt("max_duration");
		this.maxStackSize = parameters.contains("max_stacks") ? parameters.getInt("max_stacks") : 1;
		this.attributes.clear();

		if (parameters.contains("attribute_modifiers")) {
			ListTag modifierListTag = parameters.getList("attribute_modifiers", Tag.TAG_COMPOUND);
			ATTRIBUTE_ENTRY_CODEC.parse(NbtOps.INSTANCE, modifierListTag).result().ifPresent(modifiers -> modifiers.forEach(modifierEntry -> this.attributes.put(modifierEntry.attribute, modifierEntry.modifier)));
		}
	}

	/// Checks if the player can execute the skill or not
    ///
    /// Mainly examines state variables like [Entity#isInWater], [Entity#isOnFire], [Entity#onGround()]
    ///
    /// Epic Fight states based on animations also validated here. See with [EntityState]
	public boolean isExecutableState(PlayerPatch<?> executor) {
		return !executor.getOriginal().isSpectator() && !executor.isInAir() && executor.getEntityState().canUseSkill();
	}

	/**
	 * Check the resource & other restrictions to execute the skill
	 */
	public boolean canExecute(SkillContainer container) {
		return this.checkExecuteCondition(container);
	}

	/**
	 * This makes the skill icon white in Gui if it returns false
	 */
	public boolean checkExecuteCondition(SkillContainer container) {
		return true;
	}

	/// Fired when the player failed at casting the skill since of validation failed.
    ///
    /// Notify which state caused validation fail via screen messages, custom UI, chats, etc
	@ClientOnly
	public void validationFeedback(SkillContainer container) {
	}

	/// Returns a skill cast request packet to send to the server
	@ClientOnly
	public CustomPacketPayload getExecutionPacket(SkillContainer container, @Nullable CompoundTag args) {
		return new CPSkillRequest(container.getSlot(), CPSkillRequest.WorkType.CAST, args);
	}

	@ClientOnly
	public void gatherArguments(SkillContainer container, ControlEngine controlEngine, CompoundTag arguments) {
	}

	public void executeOnServer(SkillContainer container, CompoundTag args) {
		SPSkillFeedback feedbackPacket = SPSkillFeedback.executed(container.getSlot());
		ServerPlayerPatch executor = container.getServerExecutor();

		if (executor.isHoldingAny()) {
			if (executor.getHoldingSkill() instanceof ChargeableSkill) {
				feedbackPacket.arguments().putInt("chargingTicks", executor.getAccumulatedChargeTicks());
			}

			if (executor.getHoldingSkill() == this) {
				executor.getHoldingSkill().onStopHolding(container, feedbackPacket);
			}

			executor.resetHolding();
		} else {
			container.activate();
		}

		EpicFightNetworkManager.sendToPlayer(feedbackPacket, executor.getOriginal());
	}

	public void cancelOnServer(SkillContainer container, CompoundTag args) {
		ServerPlayerPatch executor = container.getServerExecutor();
		SkillCancelEvent skillCancelEvent = new SkillCancelEvent(executor, container);
        EpicFightEventHooks.Player.CANCEL_SKILL.postWithListener(skillCancelEvent, executor.getEventListener());
		EpicFightNetworkManager.sendToPlayer(SPSkillFeedback.expired(container.getSlot()), executor.getOriginal());
	}

	public final float getDefaultConsumptionAmount(PlayerPatch<?> executor) {
        return switch (this.resource) {
            case STAMINA -> executor.getModifiedStaminaConsume(this.consumption);
            case WEAPON_CHARGE, COOLDOWN -> 1;
            default -> 0.0F;
        };
	}

	/// Instant feedback when the skill is casted successfully
    @ClientOnly
	public void executeOnClient(SkillContainer container, CompoundTag args) {
	}

	/// Called when the skill is canceled.
    ///
    /// Skill are normally canceled by duration expiration, toggling by user inputs, but it's also possible to cancel
    /// skills by user defined behaviors. Epic Fight API provides some automated skill cancel, but it's not forced.
    ///
    /// Use this method to clear any activation states
    @ClientOnly
	public void cancelOnClient(SkillContainer container, CompoundTag args) {
		LocalPlayerPatch executor = container.getClientExecutor();
		SkillCancelEvent skillCancelEvent = new SkillCancelEvent(executor, container);
        EpicFightEventHooks.Player.CANCEL_SKILL.postWithListener(skillCancelEvent, executor.getEventListener());
	}

	public void onTracked(SkillContainer container, EpicFightNetworkManager.PayloadBundleBuilder payloadBuilder) {
	}

    /// Called when the skill is equipped to the player's skill slots
	public void onInitiate(SkillContainer container, EntityEventListener eventListener) {
		container.maxDuration = this.maxDuration;

		for (Map.Entry<Holder<Attribute>, AttributeModifier> stat : this.attributes.entrySet()) {
			AttributeInstance attr = container.getExecutor().getOriginal().getAttribute(stat.getKey());

			if (attr != null && !attr.hasModifier(stat.getValue().id())) {
				attr.addTransientModifier(stat.getValue());
			}
		}
	}

    /// Called when the skill is unequipped from the player's skill slots
    public void onRemoved(SkillContainer container) {
        for (Map.Entry<Holder<Attribute>, AttributeModifier> stat : this.attributes.entrySet()) {
            AttributeInstance attr = container.getExecutor().getOriginal().getAttribute(stat.getKey());

            if (attr != null && attr.hasModifier(stat.getValue().id())) {
                attr.removeModifier(stat.getValue());
            }
        }

        // Removes all components created by this skill
        container.getExecutor().getEventListener().removeListenersBelongTo(this);
        container.getExecutor().getEntityDecorations().removeAll(this);
    }

	/// Initiates the skill for local and remote players.
	/// [#onInitiate] only called for server and local players while [#onInitiateClient] is
    /// also called for remote players.
    ///
    /// Use [#onInitiateClient] if the skill has some synchronize state based on [SkillDataKey] or
    /// some client initialization code that is inaccessible in a dedicated server.
    @ClientOnly
	public void onInitiateClient(SkillContainer container) {
	}

	/// Clear the states defined by [#onInitiateClient]
    @ClientOnly
	public void onRemoveClient(SkillContainer container) {
        // Removes all components created by this skill
        container.getExecutor().getEntityDecorations().removeAll(this);
	}

	/// When stacks reach to zero
	public void onReset(SkillContainer container) {
	}

	public void setConsumption(SkillContainer container, float value) {
		container.resource = Mth.clamp(value, 0, container.getMaxResource());

		if (value >= container.getMaxResource()) {
			if (container.stack < this.maxStackSize) {
				container.stack++;
				container.resource = 0;
				container.prevResource = 0;
			} else {
				container.resource = container.getMaxResource();
				container.prevResource = container.getMaxResource();
			}
		} else if (value == 0 && container.stack > 0) {
			--container.stack;
		}
	}

	public void updateContainer(SkillContainer container) {
		container.prevResource = container.resource;
		container.prevDuration = container.duration;

		if (this.resource == Resource.COOLDOWN) {
			if (container.stack < this.maxStackSize) {
				container.setResource(container.resource + this.getCooldownRegenPerSecond(container.getExecutor()) * EpicFightSharedConstants.A_TICK);
			}
		}

		if (container.isActivated()) {
			if (this.activateType == ActivateType.DURATION) {
				container.duration--;
			}

			boolean isEnd = false;

			if (this.activateType == ActivateType.TOGGLE) {
				if (container.stack <= 0 && !container.getExecutor().getOriginal().isCreative()) {
					isEnd = true;
				}
			} else if (this.activateType != ActivateType.HELD) {
				if (container.duration <= 0) {
					isEnd = true;
				}
			}

			if (isEnd) {
				container.runOnServer(serverplayerpatch -> {
                    this.cancelOnServer(container, null);
				});

				container.deactivate();
			}
		}

		if (this.activateType == ActivateType.HELD && container.getExecutor().getHoldingSkill() == this) {
			HoldableSkill holdableSkill = (HoldableSkill)this;
			holdableSkill.holdTick(container);

			container.runOnServer(serverExecutor -> {
				container.getExecutor().resetActionTick();

				if (this instanceof ChargeableSkill chargingSkill && container.getExecutor().getSkillChargingTicks(1.0F) > chargingSkill.getAllowedMaxChargingTicks()) {
					SPSkillFeedback feedbackPacket = SPSkillFeedback.executed(container.getSlot());
					feedbackPacket.arguments().putInt("chargingTicks", serverExecutor.getAccumulatedChargeTicks());
					chargingSkill.onStopHolding(container, feedbackPacket);
					container.getExecutor().resetHolding();
					EpicFightNetworkManager.sendToPlayer(feedbackPacket, serverExecutor.getOriginal());
				}
			});
		}
	}

	public boolean isActivated(SkillContainer container) {
		return this.equals(container.getSkill()) && container.isActivated();
	}

	public boolean isDisabled(SkillContainer container) {
		return !this.equals(container.getSkill()) || container.isDisabled();
	}

	/// Make sure this method is called in a server side.
	public void setConsumptionSynchronize(SkillContainer container, float amount) {
		if (this.equals(container.skill)) {
			setSkillConsumptionSynchronize(container, amount);
		}
	}

	public void setMaxDurationSynchronize(SkillContainer container, int amount) {
		if (this.equals(container.skill)) {
			setSkillMaxDurationSynchronize(container, amount);
		}
	}

	public void setDurationSynchronize(SkillContainer container, int amount) {
		if (this.equals(container.skill)) {
			setSkillDurationSynchronize(container, amount);
		}
	}

	public void setStackSynchronize(SkillContainer container, int amount) {
		if (this.equals(container.skill)) {
			setSkillStackSynchronize(container, amount);
		}
	}

	public void setMaxResourceSynchronize(SkillContainer container, float amount) {
		if (this.equals(container.skill)) {
			setSkillMaxResourceSynchronize(container, amount);
		}
	}

	public static void setSkillConsumptionSynchronize(SkillContainer skillContainer, float fVal) {
		skillContainer.setResource(fVal);
		EpicFightNetworkManager.sendToPlayer(SPSetSkillContainerValue.resource(skillContainer.getSlot(), fVal, skillContainer.getExecutor().getOriginal().getId()), skillContainer.getServerExecutor().getOriginal());
	}

	public static void setSkillDurationSynchronize(SkillContainer skillContainer, int iVal) {
		skillContainer.setDuration(iVal);
		EpicFightNetworkManager.sendToPlayer(SPSetSkillContainerValue.duration(skillContainer.getSlot(), iVal, skillContainer.getExecutor().getOriginal().getId()), skillContainer.getServerExecutor().getOriginal());
	}

	public static void setSkillMaxDurationSynchronize(SkillContainer skillContainer, int iVal) {
		skillContainer.setMaxDuration(iVal);
		EpicFightNetworkManager.sendToPlayer(SPSetSkillContainerValue.maxDuration(skillContainer.getSlot(), iVal, skillContainer.getExecutor().getOriginal().getId()), skillContainer.getServerExecutor().getOriginal());
	}

	public static void setSkillStackSynchronize(SkillContainer skillContainer, int iVal) {
		skillContainer.setStack(iVal);
		EpicFightNetworkManager.sendToPlayer(SPSetSkillContainerValue.stacks(skillContainer.getSlot(), iVal, skillContainer.getExecutor().getOriginal().getId()), skillContainer.getServerExecutor().getOriginal());
	}

	public static void setSkillMaxResourceSynchronize(SkillContainer skillContainer, float fVal) {
		skillContainer.setMaxResource(fVal);
		EpicFightNetworkManager.sendToPlayer(SPSetSkillContainerValue.maxResource(skillContainer.getSlot(), fVal, skillContainer.getExecutor().getOriginal().getId()), skillContainer.getServerExecutor().getOriginal());
	}

	public ResourceLocation getRegistryName() {
		return this.registryName;
	}

	public String getTranslationKey() {
		return String.format("skill.%s.%s", this.getRegistryName().getNamespace(), this.getRegistryName().getPath());
	}

	public float getCooldownRegenPerSecond(PlayerPatch<?> playerpatch) {
		return 1.0F;
	}

	public SkillCategory getCategory() {
		return this.category;
	}

	public CreativeModeTab getCreativeTab() {
		return this.creativeTab;
	}

	public int getMaxStack() {
		return this.maxStackSize;
	}

	public int getMaxDuration() {
		return this.maxDuration;
	}

	public float getConsumption() {
		return this.consumption;
	}

	public Set<Entry<Holder<Attribute>, AttributeModifier>> getModfierEntry() {
		return this.attributes.entrySet();
	}

	public boolean resourcePredicate(PlayerPatch<?> playerpatch, SkillCastEvent event) {
		return playerpatch.consumeForSkill(this, this.resource, event.getArguments());
	}

	public boolean shouldDeactivateAutomatically(PlayerPatch<?> executor) {
		return !executor.getOriginal().isCreative();
	}

	public ActivateType getActivateType() {
		return this.activateType;
	}

	public Resource getResourceType() {
		return this.resource;
	}

	public Skill getPriorSkill() {
		return null;
	}

	public Skill registerPropertiesToAnimation() {
		return this;
	}

	@ClientOnly
	public void onScreen(LocalPlayerPatch playerpatch, float resolutionX, float resolutionY) {
	}

    @ClientOnly
	public List<Component> getTooltipOnItem(ItemStack itemStack, CapabilityItem cap, PlayerPatch<?> playerpatch) {
		return new ArrayList<> ();
	}

    @ClientOnly
	public List<Object> getTooltipArgsOfScreen(List<Object> list) {
		return list;
	}

    @ClientOnly
	public void drawOnGui(BattleModeGui gui, SkillContainer container, GuiGraphics guiGraphics, float x, float y, float partialTick) {
	}

    @ClientOnly
	public ResourceLocation getSkillTexture() {
		return ResourceLocation.fromNamespaceAndPath(this.getRegistryName().getNamespace(), String.format("textures/gui/skills/%s/%s.png", this.category.toString().toLowerCase(Locale.ROOT), this.getRegistryName().getPath()));
	}

    @ClientOnly
	public boolean shouldDraw(SkillContainer container) {
		return false;
	}
	
	@Override
	public String toString() {
		return this.getRegistryName().toString();
	}

    @Override
    public ResourceLocation getId() {
        return this.getRegistryName();
    }

	/**
	 * For Simply Tooltips Compatibility
	 * @param itemStack
	 * @param itemCap
	 * @param playerPatch
	 * @return
	 */
	public Component getTranslatedTooltip(ItemStack itemStack, CapabilityItem itemCap, PlayerPatch<?> playerPatch)
	{
		return Component.translatable(this.getTranslationKey() + ".tooltip");
	}

	public Component getDisplayName() {
		return Component.translatable(String.format("%s.%s.%s", "skill", this.getRegistryName().getNamespace(), this.getRegistryName().getPath()));
	}
	
	public Set<WeaponCategory> getAvailableWeaponCategories() {
		return null;
	}

    @ClientOnly
	public boolean getCustomConsumptionTooltips(SkillBookScreen.AttributeIconList consumeIconList) {
		return false;
	}
	
	public Holder<Skill> holder() {
		if (this.holder == null && this.registryName != null) {
			this.holder = EpicFightRegistries.SKILL.getHolder(
				net.minecraft.resources.ResourceKey.create(EpicFightRegistries.Keys.SKILL, this.registryName)
			).orElse(null);
		}
		return this.holder;
	}
	
	@ApiStatus.Internal
	public void setHolder(Holder<Skill> holder) {
		this.holder = holder;
	}
	
	public enum ActivateType {
		ONE_SHOT, DURATION, DURATION_INFINITE, TOGGLE, HELD
	}
	
	public enum Resource {
		NONE(
			(skillContainer, playerpatch, amount) -> true,
			(skillContainer, playerpatch, amount) -> {}
		),
		
		WEAPON_CHARGE(
			(skillContainer, playerpatch, amount) -> skillContainer.getStack() >= amount,
			(skillContainer, playerpatch, amount) -> {
				Skill.setSkillStackSynchronize(skillContainer, skillContainer.getStack() - 1);
			}
		),
		
		COOLDOWN(
			(skillContainer, playerpatch, amount) -> skillContainer.getStack() >= amount,
			(skillContainer, playerpatch, amount) -> {
				Skill.setSkillStackSynchronize(skillContainer, skillContainer.getStack() - 1);
			}
		),
		
		STAMINA(
			(skillContainer, playerpatch, amount) -> playerpatch.hasStamina(amount),
			(skillContainer, playerpatch, amount) -> {
				playerpatch.resetActionTick();
				playerpatch.setStamina(playerpatch.getStamina() - amount);
			}
		),
		
		HEALTH(
			(skillContainer, playerpatch, amount) -> playerpatch.getOriginal().getHealth() > amount,
			(skillContainer, playerpatch, amount) -> {
				playerpatch.getOriginal().setHealth(playerpatch.getOriginal().getHealth() - amount);
			}
		);
		
		public final ResourcePredicate predicate;
		public final ResourceConsumer consumer;
		
		Resource(ResourcePredicate predicate, ResourceConsumer consumer) {
			this.predicate = predicate;
			this.consumer = consumer;
		}
		
		@FunctionalInterface
		public interface ResourcePredicate {
			boolean canExecute(SkillContainer skillContainer, PlayerPatch<?> playerpatch, float amount);
		}
		
		@FunctionalInterface
		public interface ResourceConsumer {
			void consume(SkillContainer skillContainer, ServerPlayerPatch playerpatch, float amount);
		}
	}
}
