package guivnf.epicfight.world.ability;

import com.yesman.akythera.api.world.ability.AbilityResource;
import com.yesman.akythera.api.world.ability.AbilityResources;
import com.yesman.akythera.api.world.ability.StoredAbilityResource;
import guivnf.epicfight.EpicFight;

public final class EpicFightAbilityResources {
    public static final StoredAbilityResource STAMINA = new StoredAbilityResource(15.0F, 0.15F, 30, AbilityResource.Kind.CONSUMPTIVE);

    private EpicFightAbilityResources() {}

    public static void register() {
        AbilityResources.register(EpicFight.identifier("stamina"), STAMINA);
    }
}
