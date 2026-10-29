package yesman.epicfight.skill.passive;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
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
import yesman.epicfight.registry.entries.EpicFightSkillDataKeys;
import yesman.epicfight.skill.SkillBuilder;
import yesman.epicfight.skill.SkillContainer;

import java.util.List;

public class BonebreakerSkill extends PassiveSkill {
    public static final IdentifierProvider CRACKINESS = IdentifierProvider.constant(EpicFight.identifier("bonebreaker_target_crackiness"));

	private float damageBonus;
	private int maxDamageBonusStacks;
	
	public BonebreakerSkill(SkillBuilder<?> builder) {
		super(builder);
	}
	
	@Override
	public void loadDatapackParameters(CompoundTag parameters) {
		super.loadDatapackParameters(parameters);
		this.damageBonus = parameters.getFloat("damage_bonus");
		this.maxDamageBonusStacks = parameters.getInt("max_damage_bonus_stacks");
	}

    @Override
    public void onInitiate(SkillContainer container, EntityEventListener eventListener) {
        super.onInitiate(container, eventListener);

        eventListener.registerEvent(
            EpicFightEventHooks.Entity.DELIVER_DAMAGE_PRE,
            event -> {
                int currentTargetId = container.getDataManager().getDataValue(EpicFightSkillDataKeys.ENTITY_ID);

                if (currentTargetId == -1) {
                    container.getDataManager().setDataSync(EpicFightSkillDataKeys.ENTITY_ID, event.getTarget().getId());
                    container.getDataManager().setDataSync(EpicFightSkillDataKeys.STACKS, 1);
                    EpicFightNetworkManager.sendToPlayer(new SPEntityPairingPacket(event.getTarget().getId(), EntityPairingPacketTypes.BONEBREAKER_BEGIN), container.getServerExecutor().getOriginal());
                } else if (currentTargetId == event.getTarget().getId()) {
                    int stacks = container.getDataManager().getDataValue(EpicFightSkillDataKeys.STACKS);
                    event.getDamageSource().attachDamageModifier(ValueModifier.multiplier(1.0F + this.damageBonus * stacks));

                    if (stacks + 1 == this.maxDamageBonusStacks) {
                        EpicFightNetworkManager.sendToPlayer(new SPEntityPairingPacket(event.getTarget().getId(), EntityPairingPacketTypes.BONEBREAKER_MAX_STACK), container.getServerExecutor().getOriginal());
                    }

                    container.getDataManager().setDataSync(EpicFightSkillDataKeys.STACKS, Math.min(stacks + 1, this.maxDamageBonusStacks));
                }
            },
            this
        );

        eventListener.registerEvent(
            EpicFightEventHooks.Entity.MODIFY_ATTACK_DAMAGE,
            event -> {
                container.runOnServer(serverExecutor -> {
                    int currentTargetId = container.getDataManager().getDataValue(EpicFightSkillDataKeys.ENTITY_ID);

                    if (currentTargetId != -1) {
                        Entity entity = serverExecutor.getLevel().getEntity(currentTargetId);

                        if (!serverExecutor.getCurrentlyActuallyHitEntities().contains(entity) && !serverExecutor.getCurrentlyActuallyHitEntities().isEmpty()) {
                            Entity newTarget = serverExecutor.getCurrentlyActuallyHitEntities().get(0);

                            if (entity != null) {
                                EpicFightNetworkManager.sendToPlayer(new SPEntityPairingPacket(entity.getId(), EntityPairingPacketTypes.BONEBREAKER_CLEAR), serverExecutor.getOriginal());
                            }

                            container.getDataManager().setDataSync(EpicFightSkillDataKeys.ENTITY_ID, newTarget.getId());
                            container.getDataManager().setDataSync(EpicFightSkillDataKeys.STACKS, 1);
                            EpicFightNetworkManager.sendToPlayer(new SPEntityPairingPacket(newTarget.getId(), EntityPairingPacketTypes.BONEBREAKER_BEGIN), serverExecutor.getOriginal());
                        }
                    }
                });
            },
            this
        );
    }
	
	@Override
	public void onRemoved(SkillContainer container) {
		super.onRemoved(container);
		
		container.runOnServer(serverExecutor -> {
			int currentTargetId = container.getDataManager().getDataValue(EpicFightSkillDataKeys.ENTITY_ID);
			
			if (currentTargetId != -1) {
				Entity entity = container.getExecutor().getLevel().getEntity(currentTargetId);
				
				if (entity != null) {
					EpicFightNetworkManager.sendToPlayer(new SPEntityPairingPacket(entity.getId(), EntityPairingPacketTypes.BONEBREAKER_CLEAR), serverExecutor.getOriginal());
				}
			}
		});
	}
	
	@Override
	public void updateContainer(SkillContainer container) {
		super.updateContainer(container);
		
		container.runOnServer(serverExecutor -> {
			int currentTargetId = container.getDataManager().getDataValue(EpicFightSkillDataKeys.ENTITY_ID);
			
			if (currentTargetId > -1) {
				Entity entity = container.getExecutor().getLevel().getEntity(currentTargetId);
				
				if (entity == null || !entity.isAlive()) {
					container.getDataManager().setDataSync(EpicFightSkillDataKeys.ENTITY_ID, -1);
					container.getDataManager().setDataSync(EpicFightSkillDataKeys.STACKS, 0);
				}
			}
		});
	}
	
	@Override @ClientOnly
	public boolean shouldDraw(SkillContainer container) {
		Entity target = container.getExecutor().getLevel().getEntity(container.getDataManager().getDataValue(EpicFightSkillDataKeys.ENTITY_ID));
		return target != null && target.isAlive();
	}
	
	@Override @ClientOnly
	public void drawOnGui(BattleModeGui gui, SkillContainer container, GuiGraphics guiGraphics, float x, float y, float partialTick) {
		guiGraphics.blit(this.getSkillTexture(), (int)x, (int)y, 24, 24, 0, 0, 1, 1, 1, 1);
		guiGraphics.drawString(gui.getFont(), String.valueOf(container.getDataManager().getDataValue(EpicFightSkillDataKeys.STACKS)), (int)(x + 10), (int)(y + 10), 16777215, true);
	}
	
	@Override @ClientOnly
	public List<Object> getTooltipArgsOfScreen(List<Object> list) {
		list.add(ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(this.damageBonus * 100.0F));
		list.add(this.maxDamageBonusStacks);
		
		return list;
	}
}
