package yesman.epicfight.skill.passive;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import yesman.epicfight.api.event.EntityEventListener;
import yesman.epicfight.api.event.EpicFightEventHooks;
import yesman.epicfight.api.utils.side.ClientOnly;
import yesman.epicfight.client.gui.BattleModeGui;
import yesman.epicfight.client.gui.screen.SkillBookScreen;
import yesman.epicfight.network.EntityPairingPacketTypes;
import yesman.epicfight.network.EpicFightNetworkManager;
import yesman.epicfight.network.server.SPEntityPairingPacket;
import yesman.epicfight.network.server.SPSkillFeedback;
import yesman.epicfight.registry.entries.EpicFightSounds;
import yesman.epicfight.skill.*;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

public class HyperVitalitySkill extends PassiveSkill {
    public HyperVitalitySkill(SkillBuilder<?> builder) {
        super(builder);
    }

    @Override
    public void onInitiate(SkillContainer container, EntityEventListener eventListener) {
        super.onInitiate(container, eventListener);

        eventListener.registerEvent(
            EpicFightEventHooks.Player.CONSUME_SKILL,
            event -> {
                if (!this.isDisabled(container) && event.getSkill().getCategory() == SkillCategories.WEAPON_INNATE) {
                    PlayerPatch<?> playerpatch = container.getExecutor();
                    Player player = playerpatch.getOriginal();

                    if (playerpatch.getSkill(SkillSlots.WEAPON_INNATE).getStack() < 1) {
                        if (container.getStack() > 0 && !player.isCreative()) {
                            float consumption = event.getSkill().getConsumption();

                            if (playerpatch.consumeForSkill(this, Skill.Resource.STAMINA, consumption * 0.1F)) {
                                event.setResourceType(Skill.Resource.NONE);
                                container.setMaxResource(consumption * 0.2F);

                                container.runOnServer(serverplayerpatch -> {
                                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(), EpicFightSounds.HYPERVITALITY.get(), player.getSoundSource(), 1.0F, 1.0F);
                                    container.setMaxDuration(event.getSkill().getMaxDuration());
                                    container.activate();
                                    EpicFightNetworkManager.sendToPlayer(SPSkillFeedback.executed(container.getSlot()), serverplayerpatch.getOriginal());

                                    SPEntityPairingPacket pairingPacket = new SPEntityPairingPacket(player.getId(), EntityPairingPacketTypes.FLASH_WHITE);

                                    // durationTick, maxOverlay, maxBrightness
                                    pairingPacket.buffer().writeInt(4);
                                    pairingPacket.buffer().writeInt(15);
                                    pairingPacket.buffer().writeInt(8);
                                    pairingPacket.buffer().writeBoolean(false);

                                    EpicFightNetworkManager.sendToAllPlayerTrackingThisEntityWithSelf(pairingPacket, serverplayerpatch.getOriginal());
                                });
                            }
                        }
                    }
                }
            },
            this,
            1
        );

        eventListener.registerEvent(
            EpicFightEventHooks.Player.CANCEL_SKILL,
            event -> {
                if (!container.getExecutor().getOriginal().isCreative() && event.getSkillContainer().getSkill().getCategory() == SkillCategories.WEAPON_INNATE && this.isActivated(container)) {
                    container.setResource(0.0F);
                    container.deactivate();
                    this.setStackSynchronize(container, container.getStack() - 1);
                    EpicFightNetworkManager.sendToPlayer(SPSkillFeedback.executed(container.getSlot()), container.getServerExecutor().getOriginal());
                }
            },
            this
        );
    }

    @Override @ClientOnly
    public void executeOnClient(SkillContainer container, CompoundTag arguments) {
        super.executeOnClient(container, arguments);
        container.activate();
    }

    @Override
    public void cancelOnClient(SkillContainer container, CompoundTag arguments) {
        super.cancelOnClient(container, arguments);
        container.deactivate();
    }

    @Override
    public boolean shouldDraw(SkillContainer container) {
        return this.isActivated(container) || container.getStack() == 0;
    }

    @Override @ClientOnly
    public void drawOnGui(BattleModeGui gui, SkillContainer container, GuiGraphics guiGraphics, float x, float y, float partialTick) {
        guiGraphics.blit(this.getSkillTexture(), (int)x, (int)y, 24, 24, 0, 0, 1, 1, 1, 1);

        if (!this.isActivated(container)) {
            String remainTime = String.format("%.0f", container.getMaxResource() - container.getResource());
            guiGraphics.drawString(gui.getFont(), remainTime, (int)(x + 12 - 4 * (remainTime.length())), (int)(y + 6), 16777215, true);
        }
    }

    @Override @ClientOnly
    public boolean getCustomConsumptionTooltips(SkillBookScreen.AttributeIconList consumptionList) {
        consumptionList.add(Component.translatable("attribute.name.epicfight.stamina.consume.tooltip"), Component.translatable("skill.epicfight.hypervitality.consume.tooltip"), SkillBookScreen.STAMINA_TEXTURE_INFO);
        return true;
    }
}