package yesman.epicfight.skill.passive;

import net.minecraft.core.particles.ParticleTypes;
import yesman.epicfight.api.animation.types.DodgeAnimation;
import yesman.epicfight.api.event.EntityEventListener;
import yesman.epicfight.api.event.EpicFightEventHooks;
import yesman.epicfight.network.EntityPairingPacketTypes;
import yesman.epicfight.network.EpicFightNetworkManager;
import yesman.epicfight.network.server.SPEntityPairingPacket;
import yesman.epicfight.registry.entries.EpicFightSounds;
import yesman.epicfight.skill.SkillBuilder;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.SkillSlots;

public class TechnicianSkill extends PassiveSkill {
    private static final java.util.UUID EVENT_UUID = java.util.UUID.fromString("99e5c782-fdaf-11eb-9a03-0242ac130003");

    public TechnicianSkill(SkillBuilder<?> builder) {
        super(builder);
    }

    @Override
    public void onInitiate(SkillContainer skillContainer, EntityEventListener eventListener) {
        super.onInitiate(skillContainer, eventListener);

        eventListener.registerEvent(
            EpicFightEventHooks.Entity.ON_DODGE,
            event -> {
                skillContainer.getExecutor().playSound(EpicFightSounds.TECHNICIAN.get(), 1.0F, 1.0F, 1.0F);
                skillContainer.getServerExecutor().getOriginal().serverLevel().sendParticles(ParticleTypes.POOF, event.getLocation().x(), event.getLocation().y(), event.getLocation().z(), 4, 0.0D, 0.0D, 0.0D, 0.075D);
                float consumption = skillContainer.getExecutor().getModifiedStaminaConsume(skillContainer.getExecutor().getSkill(SkillSlots.DODGE).getSkill().getConsumption());
                skillContainer.getExecutor().setStamina(skillContainer.getExecutor().getStamina() + consumption);
            },
            this
        );

        eventListener.registerEvent(
            EpicFightEventHooks.Animation.BEGIN,
            event -> {
                if (!skillContainer.getExecutor().isLogicalClient() && event.getAnimation().checkType(DodgeAnimation.class)) {
                    EpicFightNetworkManager.sendToAllPlayerTrackingThisEntityWithSelf(new SPEntityPairingPacket(skillContainer.getServerExecutor().getOriginal().getId(), EntityPairingPacketTypes.TECHNICIAN_ACTIVATED), skillContainer.getServerExecutor().getOriginal());
                }
            },
            this
        );
    }
}