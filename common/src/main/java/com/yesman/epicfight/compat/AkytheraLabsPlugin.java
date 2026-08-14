package com.yesman.epicfight.compat;

import com.yesman.akythera.client.compat.IClientModPlugin;
import com.yesman.akytheralabs.SourceTemplateEngine;
import com.yesman.akytheralabs.akythera.itempatch.component.ItemPatchComponentHolders;
import com.yesman.epicfight.EpicFight;
import com.yesman.epicfight.akytheralabs.dataholder.WeaponStanceSystemHolder;

public final class AkytheraLabsPlugin implements IClientModPlugin {
    @Override
    public void onInitializeClient() {
        // Must match EpicFightComponentBlueprints.WEAPON_STANCE_SYSTEM's registry id.
        ItemPatchComponentHolders.REGISTRY.register(
            EpicFight.identifier("weapon_stance_system"),
            WeaponStanceSystemHolder.class,
            WeaponStanceSystemHolder::createDefault,
            WeaponStanceSystemHolder.EDITOR_CODEC
        );

        // Resolve this jar's components/weapon_stance_system.ftl for source export.
        SourceTemplateEngine.addTemplateSource(WeaponStanceSystemHolder.class);
    }
}
