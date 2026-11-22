package guivnf.epicfight.world.ability;

import com.yesman.akythera.api.event.BuiltInEventHooks;
import com.yesman.akythera.api.world.ability.AbilityResourceRef;
import com.yesman.akythera.api.world.ability.AbilitySystemComponent;
import com.yesman.akythera.api.world.ability.AbilityTag;
import com.yesman.akythera.api.world.ability.StoredAbilityResource;
import com.yesman.akythera.api.world.patch.entity.LivingEntityPatch;
import guivnf.epicfight.EpicFight;
import guivnf.epicfight.akythera.providers.EpicfightAbilityResources;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public final class Guard {
    private static final float GUARDED_DAMAGE = 0.4F;
    private static final float BASE_COST = 1.0F;
    private static final float STAMINA_PER_DAMAGE = 1.0F;

    private Guard() {}

    public static void registerEvents() {
        BuiltInEventHooks.Entity.HURT.registerEvent(event -> {
            if (!(event.getEntityPatch() instanceof LivingEntityPatch<?> patch)) return;

            AbilitySystemComponent abilitySystem = patch.getComponent(AbilitySystemComponent.class);
            Optional<AbilityTag> guarding = AbilityTag.ENUM_MANAGER.get(EpicFight.identifier("guarding"));

            if (abilitySystem == null || guarding.isEmpty() || !abilitySystem.hasAbilityTag(guarding.get())) return;

            LivingEntity entity = patch.getOriginal();
            DamageSource source = event.getDamageSource();
            Vec3 from = source.getSourcePosition();

            // vanilla handles hits during invulnerability ticks
            if (from == null || source.is(DamageTypeTags.BYPASSES_SHIELD) || entity.invulnerableTime > 10) return;
            if (from.subtract(entity.position()).horizontal().dot(Vec3.directionFromRotation(0.0F, entity.getYHeadRot())) <= 0.0D) return;

            StoredAbilityResource stamina = StoredAbilityResource.get(new AbilityResourceRef.ByData(EpicfightAbilityResources.STAMINA));

            if (stamina == null) return;

            float amount = event.getAmount();
            boolean paid = stamina.consume(patch, BASE_COST + amount * STAMINA_PER_DAMAGE);

            if (paid) {
                event.setAmount(amount * GUARDED_DAMAGE);
            } else {
                stamina.write(patch, 0.0F);
            }

            AbilityTag.ENUM_MANAGER.get(EpicFight.identifier(paid ? "event/guard_hit" : "event/guard_break")).ifPresent(abilitySystem::triggerEvent);
        });
    }
}
