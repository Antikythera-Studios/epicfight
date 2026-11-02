package guivnf.epicfight.akytheralabs.compat;

import com.yesman.akythera.client.compat.IClientModPlugin;
import com.yesman.akytheralabs.SourceTemplateEngine;
import com.yesman.akytheralabs.akythera.itempatch.component.ItemPatchComponentHolders;
import com.yesman.akytheralabs.dataholder.DataBlockRegistries;
import guivnf.epicfight.EpicFight;
import guivnf.epicfight.akytheralabs.dataholder.EpicFightWeaponPropertiesHolder;
import guivnf.epicfight.akytheralabs.dataholder.WeaponCategoryHolder;
import guivnf.epicfight.world.item.datablock.WeaponCategory;

public final class AkytheraLabsPlugin implements IClientModPlugin {
    @Override
    public void onInitializeClient() {
        // must match the id in EpicFightComponentBlueprints
        ItemPatchComponentHolders.REGISTRY.register(
            EpicFight.identifier("weapon_properties"),
            EpicFightWeaponPropertiesHolder.class,
            _ -> EpicFightWeaponPropertiesHolder.createDefault(),
            EpicFightWeaponPropertiesHolder.EDITOR_CODEC
        );

        DataBlockRegistries.register(WeaponCategory.ENUM_MANAGER, WeaponCategoryHolder::new, WeaponCategoryHolder.CODEC);

        // Resolve this jar's components/weapon_properties.ftl for source export.
        SourceTemplateEngine.addTemplateSource(EpicFightWeaponPropertiesHolder.class);
    }
}
