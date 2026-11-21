package yesman.epicfight.skill;

import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.util.Mth;
import yesman.epicfight.EpicFight;
import yesman.epicfight.api.utils.side.ClientOnly;
import yesman.epicfight.api.event.EpicFightEventHooks;
import yesman.epicfight.api.event.types.player.SkillCastEvent;
import yesman.epicfight.api.event.types.player.SkillConsumeEvent;
import yesman.epicfight.client.events.engine.ControlEngine;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;
import yesman.epicfight.network.client.CPSkillRequest;
import yesman.epicfight.network.server.SPChangeSkill;
import yesman.epicfight.network.server.SPSetRemotePlayerSkill;
import yesman.epicfight.registry.callbacks.SkillDataKeyCallbacks;
import yesman.epicfight.skill.Skill.ActivateType;
import yesman.epicfight.skill.modules.ChargeableSkill;
import yesman.epicfight.skill.modules.HoldableSkill;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.world.gamerule.EpicFightGameRules;

import org.jetbrains.annotations.Nullable;
import java.util.Set;
import java.util.function.Consumer;

public class SkillContainer {
    protected Skill skill;
    protected int prevDuration;
    protected int duration;
    protected int maxDuration;
    protected int stack;
    protected int replaceCooldown;
    protected float resource;
    protected float prevResource;
    protected float maxResource;
    protected boolean isActivated;
    protected boolean disabled;

    protected final SkillSlot slot;
    protected final PlayerPatch<?> executor;
    protected final SkillDataManager skillDataManager;

    public SkillContainer(PlayerPatch<?> executor, SkillSlot skillSlot) {
        this.executor = executor;
        this.slot = skillSlot;
        this.skillDataManager = new SkillDataManager(this);
    }

    public PlayerPatch<?> getExecutor() {
        return this.executor;
    }

    public LocalPlayerPatch getClientExecutor() {
        return (LocalPlayerPatch)this.executor;
    }

    public ServerPlayerPatch getServerExecutor() {
        return (ServerPlayerPatch)this.executor;
    }

    public boolean setSkill(@Nullable Skill skill) {
        return this.setSkill(skill, false);
    }

    public boolean setSkill(@Nullable Skill skill, boolean initialize) {
        // For remote players, call setSkillRemote instead
        if (this.executor.isLogicalClient() && !this.executor.getOriginal().isLocalPlayer()) {
            return false;
        }

        if (this.skill == skill && !initialize) {
            return false;
        }

        if (skill != null && skill.category != this.slot.category()) {
            return false;
        }

        if (this.skill != null) {
            this.skill.onRemoved(this);

            if (this.executor.isLogicalClient()) {
                this.skill.onRemoveClient(this);
            }

            this.executor.getPlayerSkills().removeSkillFromContainer(this.skill);
        }

        this.skill = skill;
        this.resetValues();

        // Remove all data keys
        this.skillDataManager.clearData();

        if (skill != null) {
            Set<Holder<SkillDataKey<?>>> datakeys = SkillDataKeyCallbacks.getSkillDataKeyMap().get(skill.getClass());

            if (datakeys != null) {
                datakeys.forEach(this.skillDataManager::registerData);
            }

            skill.onInitiate(this, this.executor.getEventListener());

            if (this.executor.isLogicalClient()) {
                skill.onInitiateClient(this);
            }

            this.setMaxResource(skill.consumption);
            this.setMaxDuration(skill.maxDuration);
            this.executor.getPlayerSkills().setSkillToContainer(skill, this);
        }

        this.executor.clampMaxAttributes();
        this.stack = 0;

        if (initialize) {
            this.setDisabled(false);
        }

        return true;
    }

    @ClientOnly
    public void setSkillRemote(@Nullable Skill skill) {
        // For server players or a local player, call setSkill instead
        if (!this.executor.isLogicalClient() || this.executor.getOriginal().isLocalPlayer()) {
            return;
        }

        if (this.skill == skill) {
            return;
        }

        if (skill != null && skill.category != this.slot.category()) {
            return;
        }

        if (this.skill != null) {
            this.skill.onRemoveClient(this);
            this.executor.getPlayerSkills().removeSkillFromContainer(this.skill);
        }

        this.skill = skill;
        this.resetValues();

        // Remove all data keys
        this.skillDataManager.clearData();

        if (skill != null) {
            Set<Holder<SkillDataKey<?>>> datakeys = SkillDataKeyCallbacks.getSkillDataKeyMap().get(skill.getClass());

            if (datakeys != null && !datakeys.isEmpty()) {
                datakeys.stream().filter(holder -> holder.value().syncronizeToRemotePlayers()).forEach(this.skillDataManager::registerData);
            }

            skill.onInitiateClient(this);
            this.executor.getPlayerSkills().setSkillToContainer(skill, this);

            this.setMaxResource(skill.consumption);
            this.setMaxDuration(skill.maxDuration);
        }

        this.stack = 0;
    }

    public boolean isDisabled() {
        return this.disabled;
    }

    public void setDisabled(boolean disable) {
        this.disabled = disable;
    }

    public void resetValues() {
        this.isActivated = false;
        this.prevDuration = 0;
        this.duration = 0;
        this.prevResource = 0.0F;
        this.resource = 0.0F;
    }

    public boolean isEmpty() {
        return this.skill == null;
    }

    public boolean hasSkill() {
        return this.skill != null;
    }

    public void setResource(float value) {
        if (this.skill != null) {
            this.skill.setConsumption(this, value);
        } else {
            this.prevResource = 0;
            this.resource = 0;
        }
    }

    public void setMaxDuration(int value) {
        this.maxDuration = Math.max(value, 0);
    }

    public void setDuration(int value) {
        if (this.skill != null) {
            if (!this.isActivated() && value > 0) {
                this.isActivated = true;
            }

            this.duration = Mth.clamp(value, 0, this.maxDuration);
        } else {
            this.duration = 0;
        }
    }

    public void setStack(int stack) {
        if (this.skill != null) {
            this.stack = Mth.clamp(stack, 0, this.skill.maxStackSize);

            if (this.stack <= 0 && this.skill.shouldDeactivateAutomatically(this.executor)) {
                this.deactivate();
                this.skill.onReset(this);
            }
        } else {
            this.stack = 0;
        }
    }

    public void setMaxResource(float maxResource) {
        this.maxResource = maxResource;
    }

    public void setReplaceCooldown(int replaceCooldown) {
        this.replaceCooldown = Mth.clamp(replaceCooldown, 0, EpicFightGameRules.SKILL_REPLACE_COOLDOWN.getRuleValue(this.executor.getOriginal().level()));
    }

    @ClientOnly
    public SkillCastEvent sendCastRequest(LocalPlayerPatch executor, ControlEngine controlEngine) {
        CompoundTag arguments = new CompoundTag();

        if (this.skill != null) {
            this.skill.gatherArguments(this, controlEngine, arguments);
        }

        SkillCastEvent event = new SkillCastEvent(executor, this, arguments);

        if (this.skill == null) {
            return event;
        }

        CustomPacketPayload packet = null;

        if (this.skill instanceof HoldableSkill holdableSkill && this.skill.getActivateType() == Skill.ActivateType.HELD) {
            if (executor.isHoldingSkill(this.skill)) {
                packet = this.skill.getExecutionPacket(this, event.getArguments());
                executor.resetHolding();
            } else {
                if (!this.canUse(executor, event)) {
                    this.skill.validationFeedback(this);
                    return event;
                }

                CPSkillRequest castpacket = new CPSkillRequest(this.getSlot(), CPSkillRequest.WorkType.HOLD_START);
                holdableSkill.gatherHoldArguments(this, controlEngine, castpacket.arguments());
                packet = castpacket;
            }
        } else {
            if (!this.canUse(executor, event)) {
                this.skill.validationFeedback(this);
                return event;
            }

            packet = this.skill.getExecutionPacket(this, event.getArguments());
        }

        if (packet != null) {
            controlEngine.addPacketToSend(packet);
        }

        return event;
    }

    @ClientOnly
    public void sendCancelRequest(LocalPlayerPatch executor, ControlEngine controlEngine) {
        CPSkillRequest packet = new CPSkillRequest(this.getSlot(), CPSkillRequest.WorkType.CANCEL);
        controlEngine.addPacketToSend(packet);
    }

    public boolean requestCasting(ServerPlayerPatch executor, CompoundTag args) {
        SkillCastEvent event = new SkillCastEvent(executor, this, args);

        if (this.canUse(executor, event)) {
            this.skill.executeOnServer(this, event.getArguments());
            return true;
        }

        return false;
    }

    public boolean requestCancel(ServerPlayerPatch executor, CompoundTag args) {
        if (this.skill != null) {
            this.skill.cancelOnServer(this, args);
            return true;
        }

        return false;
    }

    public boolean requestHold(ServerPlayerPatch executor, CompoundTag args) {
        if (this.skill instanceof HoldableSkill holdableSkill) {
            SkillCastEvent event = new SkillCastEvent(executor, this, args);

            if (this.canUse(executor, event)) {
                SkillConsumeEvent consumeEvent = new SkillConsumeEvent(executor, this.skill, this.skill.resource, args);
                EpicFightEventHooks.Player.CONSUME_SKILL.postWithListener(consumeEvent, executor.getEventListener());

                if (!consumeEvent.isCanceled()) {
                    consumeEvent.getResourceType().consumer.consume(this, executor, consumeEvent.getAmount());
                }

                executor.startSkillHolding(holdableSkill);

                return true;
            }
        }

        return false;
    }

    public SkillDataManager getDataManager() {
        return this.skillDataManager;
    }

    public void transferDataTo(SkillContainer skillContainer) {
        this.skillDataManager.transferDataTo(skillContainer.skillDataManager);
        skillContainer.prevDuration = this.prevDuration;
        skillContainer.duration = this.duration;
        skillContainer.maxDuration = this.maxDuration;
        skillContainer.resource = this.resource;
        skillContainer.prevResource = this.prevResource;
        skillContainer.maxResource = this.maxResource;
        skillContainer.isActivated = this.isActivated;
        skillContainer.disabled = this.disabled;
        skillContainer.stack = this.stack;
    }

    public float getResource() {
        return this.resource;
    }

    public int getRemainDuration() {
        return this.duration;
    }

    public boolean canUse(PlayerPatch<?> executor, SkillCastEvent event) {
        if (this.skill == null) {
            return false;
        } else {
            if (executor.isHoldingSkill(this.skill) && this.skill instanceof ChargeableSkill chargingSkill) {
                if (executor.isLogicalClient()) {
                    return true;
                } else {
                    return executor.getSkillChargingTicks() >= chargingSkill.getMinChargingTicks();
                }
            }

            event.setSkillExecutable(this.skill.canExecute(this));
            event.setStateExecutable(this.skill.isExecutableState(executor));

            EpicFightEventHooks.Player.CAST_SKILL.postWithListener(event, executor.getEventListener());

            if (!event.isCanceled() && event.isExecutable()) {
                return (executor.getOriginal().isCreative() || this.skill.resourcePredicate(executor, event)) || (this.isActivated() && this.skill.activateType == ActivateType.DURATION);
            } else {
                return false;
            }
        }
    }

    public void update() {
        if (this.replaceCooldown > 0) this.replaceCooldown = Mth.clamp(this.replaceCooldown - 1, 0, EpicFightGameRules.SKILL_REPLACE_COOLDOWN.getRuleValue(this.executor.getOriginal().level()));
        if (this.skill != null) this.skill.updateContainer(this);
    }

    public int getStack() {
        return this.stack;
    }

    public SkillSlot getSlot() {
        return this.slot;
    }

    public int getSlotId() {
        return this.slot.universalOrdinal();
    }

    public Skill getSkill() {
        return this.skill;
    }

    public float getMaxResource() {
        return this.maxResource;
    }

    public void activate() {
        EpicFight.LOGGER.debug("SkillContainer activate: {} for player {}", this.getSlotId(), this.executor.getOriginal().getId());
        if (!this.isActivated) {
            this.prevDuration = this.maxDuration;
            this.duration = this.maxDuration;
            this.isActivated = true;
        }
    }

    public void deactivate() {
        if (this.isActivated) {
            this.prevDuration = 0;
            this.duration = 0;
            this.isActivated = false;
        }
    }

    public boolean isActivated() {
        return this.isActivated;
    }

    public boolean hasSkill(Skill skill) {
        return this.skill != null && this.skill.equals(skill);
    }

    public boolean isFull() {
        return this.skill == null || this.stack >= this.skill.maxStackSize;
    }

    public float getResource(float partialTicks) {
        return this.skill != null && this.maxResource > 0 ? (this.prevResource + ((this.resource - this.prevResource) * partialTicks)) / this.maxResource : 0;
    }

    public float getNeededResource() {
        return this.skill != null ? this.maxResource - this.resource : 0;
    }

    public float getDurationRatio(float partialTicks) {
        return this.skill != null && this.maxDuration > 0 ? (this.prevDuration + ((this.duration - this.prevDuration) * partialTicks)) / this.maxDuration : 0;
    }

    /// Returns whether the player is currently on cooldown for replacing a skill.
    /// The player must not be in Creative mode for the cooldown to apply.
    public boolean onReplaceCooldown() {
        return this.replaceCooldown > 0 && !this.executor.getOriginal().isCreative();
    }

    public int getReplaceCooldown() {
        return this.replaceCooldown;
    }

    public SPChangeSkill createSyncPacketToLocalPlayer() {
        return new SPChangeSkill(this.getSlot(), this.executor.getOriginal().getId(), Skill.holderOrNull(this.getSkill()));
    }

    public SPSetRemotePlayerSkill createSyncPacketToRemotePlayer() {
        return new SPSetRemotePlayerSkill(this.getSlot(), this.executor.getOriginal().getId(), Skill.holderOrNull(this.getSkill()));
    }

    /// Use this method instead of calling getClientExecutor multiple times to avoid repetitive type-casting
    @ClientOnly
    public void runOnLocalClient(Consumer<LocalPlayerPatch> run) {
        if (this.executor.isLogicalClient()) {
            run.accept(this.getClientExecutor());
        }
    }

    /// Use this method instead of calling getServerExecutor multiple times to avoid repetitive type-casting
    public void runOnServer(Consumer<ServerPlayerPatch> run) {
        if (!this.executor.isLogicalClient()) {
            run.accept(this.getServerExecutor());
        }
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof SkillContainer skillContainer) {
            return this.slot.equals(skillContainer.slot);
        }

        return false;
    }
}