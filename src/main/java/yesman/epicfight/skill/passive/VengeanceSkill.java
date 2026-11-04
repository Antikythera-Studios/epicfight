package yesman.epicfight.skill.passive;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import yesman.epicfight.api.event.EntityEventListener;
import yesman.epicfight.api.event.EpicFightEventHooks;
import yesman.epicfight.api.event.IdentifierProvider;
import yesman.epicfight.api.utils.math.ValueModifier;
import yesman.epicfight.api.utils.side.ClientOnly;
import yesman.epicfight.client.gui.BattleModeGui;
import yesman.epicfight.EpicFight;
import yesman.epicfight.network.EntityPairingPacketTypes;
import yesman.epicfight.network.EpicFightNetworkManager;
import yesman.epicfight.network.server.SPEntityPairingPacket;
import yesman.epicfight.network.server.SPPlayUISound;
import yesman.epicfight.registry.entries.EpicFightSkillDataKeys;
import yesman.epicfight.registry.entries.EpicFightSounds;
import yesman.epicfight.skill.SkillBuilder;
import yesman.epicfight.skill.SkillContainer;

import java.util.List;

public class VengeanceSkill extends PassiveSkill {
    public static final IdentifierProvider TARGET = IdentifierProvider.constant(EpicFight.identifier("vengeance_target"));

    private float damageBonus;

    public VengeanceSkill(SkillBuilder<?> builder) {
        super(builder);
    }

    @Override
    public void loadDatapackParameters(CompoundTag parameters) {
        super.loadDatapackParameters(parameters);
        this.damageBonus = parameters.getFloat("damage_bonus");
    }

    @Override
    public void onInitiate(SkillContainer container, EntityEventListener eventListener) {
        super.onInitiate(container, eventListener);

        eventListener.registerEvent(
            EpicFightEventHooks.Entity.TAKE_DAMAGE_PRE,
            event -> {
                if (event.getDamageSource().getEntity() == null) {
                    return;
                }

                int currentTargetId = container.getDataManager().getDataValue(EpicFightSkillDataKeys.ENTITY_ID);

                if (currentTargetId == -1 && event.getDamageSource().getEntity() instanceof LivingEntity livingentity) {
                    setNewTarget(container, livingentity);
                } else if (currentTargetId == event.getDamageSource().getEntity().getId()) {
                    container.getDataManager().setDataSync(EpicFightSkillDataKeys.TICK_RECORD, container.getExecutor().getOriginal().tickCount);
                } else if (currentTargetId != event.getDamageSource().getEntity().getId() && event.getDamageSource().getEntity() instanceof LivingEntity livingentity) {
                    if (canResetTarget(container)) {
                        setNewTarget(container, livingentity);
                    }
                }
            },
            this
        );

        eventListener.registerEvent(
            EpicFightEventHooks.Entity.DELIVER_DAMAGE_PRE,
            event -> {
                int currentTargetId = container.getDataManager().getDataValue(EpicFightSkillDataKeys.ENTITY_ID);

                if (currentTargetId == event.getTarget().getId()) {
                    event.getDamageSource().attachDamageModifier(ValueModifier.multiplier(1.0F + this.damageBonus));
                    container.getDataManager().setDataSync(EpicFightSkillDataKeys.TICK_RECORD, container.getExecutor().getOriginal().tickCount);
                } else if (container.isActivated()) {
                    float f = this.damageBonus * container.getDurationRatio(1.0F);
                    event.getDamageSource().attachDamageModifier(ValueModifier.multiplier(1.0F + f));
                }
            },
            this
        );

        eventListener.registerEvent(
            EpicFightEventHooks.Entity.KILL_ENTITY,
            event -> {
                if (container.isActivated()) {
                    return;
                }

                int currentTargetId = container.getDataManager().getDataValue(EpicFightSkillDataKeys.ENTITY_ID);

                if (currentTargetId == event.getKilledEntity().getId()) {
                    this.executeOnServer(container, null);
                    container.getDataManager().setDataSync(EpicFightSkillDataKeys.ENTITY_ID, -1);
                }
            },
            this
        );
    }

    @Override
    public void onRemoved(SkillContainer container) {
        container.runOnServer(serverplayerpatch -> {
            int currentTargetId = container.getDataManager().getDataValue(EpicFightSkillDataKeys.ENTITY_ID);
            Entity entity = container.getExecutor().getLevel().getEntity(currentTargetId);

            if (entity != null) {
                EpicFightNetworkManager.sendToPlayer(new SPEntityPairingPacket(currentTargetId, EntityPairingPacketTypes.VENGEANCE_TARGET_CANCEL), serverplayerpatch.getOriginal());
            }
        });

        super.onRemoved(container);
    }

    @Override
    public void updateContainer(SkillContainer container) {
        super.updateContainer(container);

        container.runOnServer(serverplayerpatch -> {
            int currentTargetId = container.getDataManager().getDataValue(EpicFightSkillDataKeys.ENTITY_ID);

            if (currentTargetId > -1) {
                Entity entity = container.getExecutor().getLevel().getEntity(currentTargetId);

                if (container.getExecutor().getOriginal().tickCount - container.getDataManager().getDataValue(EpicFightSkillDataKeys.TICK_RECORD) >= 160) {
                    cancelTarget(container);
                } else if (entity == null || !entity.isAlive()) {
                    cancelTarget(container);
                }
            }
        });
    }

    @Override
    public void executeOnServer(SkillContainer container, CompoundTag args) {
        super.executeOnServer(container, args);
        container.getExecutor().getOriginal().playSound(EpicFightSounds.VENGEANCE.get(), 1.0F, 1.0F);
        EpicFightNetworkManager.sendToAllPlayerTrackingThisEntityWithSelf(new SPEntityPairingPacket(container.getExecutor().getOriginal().getId(), EntityPairingPacketTypes.VENGEANCE_OVERLAY), container.getServerExecutor().getOriginal());
    }

    @Override
    public void cancelOnServer(SkillContainer container, CompoundTag args) {
        super.cancelOnServer(container, args);
        EpicFightNetworkManager.sendToAllPlayerTrackingThisEntityWithSelf(new SPEntityPairingPacket(container.getExecutor().getOriginal().getId(), EntityPairingPacketTypes.VENGEANCE_TARGET_CANCEL), container.getServerExecutor().getOriginal());
    }

    @Override
    public void onTracked(SkillContainer container, EpicFightNetworkManager.PayloadBundleBuilder payloadBuilder) {
        if (container.isActivated()) {
            payloadBuilder.and(new SPEntityPairingPacket(container.getExecutor().getOriginal().getId(), EntityPairingPacketTypes.VENGEANCE_OVERLAY));
        }
    }

    @Override @ClientOnly
    public void executeOnClient(SkillContainer container, CompoundTag args) {
        container.activate();
        container.getExecutor().playLocalSound(EpicFightSounds.VENGEANCE);
    }

    public static boolean tickExceeded(SkillContainer container) {
        return container.getExecutor().getOriginal().tickCount - container.getDataManager().getDataValue(EpicFightSkillDataKeys.TICK_RECORD) >= 160;
    }

    public static boolean canResetTarget(SkillContainer container) {
        return container.getExecutor().getOriginal().tickCount - container.getDataManager().getDataValue(EpicFightSkillDataKeys.TICK_RECORD) >= 80;
    }

    public static void setNewTarget(SkillContainer container, LivingEntity target) {
        cancelTarget(container);

        container.getDataManager().setDataSync(EpicFightSkillDataKeys.ENTITY_ID, target.getId());
        container.getDataManager().setDataSync(EpicFightSkillDataKeys.TICK_RECORD, container.getExecutor().getOriginal().tickCount);

        container.runOnServer(serverplayerpatch -> {
            EpicFightNetworkManager.sendToPlayer(new SPPlayUISound(EpicFightSounds.VENGEANCE), serverplayerpatch.getOriginal());
            EpicFightNetworkManager.sendToPlayer(new SPEntityPairingPacket(target.getId(), EntityPairingPacketTypes.VENGEANCE_OVERLAY), serverplayerpatch.getOriginal());
        });
    }

    public static void cancelTarget(SkillContainer container) {
        int currentTargetId = container.getDataManager().getDataValue(EpicFightSkillDataKeys.ENTITY_ID);
        Entity entity = container.getExecutor().getLevel().getEntity(currentTargetId);
        container.getDataManager().setDataSync(EpicFightSkillDataKeys.ENTITY_ID, -1);

        container.runOnServer(serverplayerpatch -> {
            if (entity != null) {
                EpicFightNetworkManager.sendToPlayer(new SPEntityPairingPacket(currentTargetId, EntityPairingPacketTypes.VENGEANCE_TARGET_CANCEL), serverplayerpatch.getOriginal());
            }
        });
    }

    @Override @ClientOnly
    public boolean shouldDraw(SkillContainer container) {
        return container.isActivated() || (container.getDataManager().getDataValue(EpicFightSkillDataKeys.ENTITY_ID) > -1 && !tickExceeded(container));
    }

    @Override @ClientOnly
    public void drawOnGui(BattleModeGui gui, SkillContainer container, GuiGraphics guiGraphics, float x, float y, float partialTick) {
        guiGraphics.blit(this.getSkillTexture(), (int)x, (int)y, 24, 24, 0, 0, 1, 1, 1, 1);

        if (container.isActivated()) {
            float f = Math.round(this.damageBonus * 100.0F * container.getDurationRatio(1.0F));
            guiGraphics.drawString(gui.getFont(), ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(f) + "%", (int)(x + 6), (int)(y + 8), 16777215, true);
        } else if (canResetTarget(container)) {
            int seconds = 4 - ((container.getExecutor().getOriginal().tickCount - container.getDataManager().getDataValue(EpicFightSkillDataKeys.TICK_RECORD)) - 80) / 20;
            guiGraphics.drawString(gui.getFont(), String.valueOf(seconds), (int)(x + 6), (int)(y + 8), 16777215, true);
        }
    }

    @Override @ClientOnly
    public List<Object> getTooltipArgsOfScreen(List<Object> list) {
        list.add(ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(this.damageBonus * 100.0F));
        list.add(ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(this.damageBonus * 100.0F));
        list.add(String.valueOf(this.maxDuration / 20));

        return list;
    }
}
