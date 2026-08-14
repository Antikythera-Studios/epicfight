package com.yesman.epicfight.world.item.datablock;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public enum EpicFightWeaponCategory implements WeaponCategory {
    NOT_WEAPON(Items.AIR),
    AXE(Items.IRON_AXE),
    //FIST(EpicFightItems.GLOVE.value()),
    //GREATSWORD(EpicFightItems.IRON_GREATSWORD.value()),
    HOE(Items.IRON_HOE),
    PICKAXE(Items.IRON_PICKAXE),
    SHOVEL(Items.IRON_SHOVEL),
    SWORD(Items.IRON_SWORD),
    //UCHIGATANA(EpicFightItems.UCHIGATANA.value()),
    SPEAR(Items.IRON_SPEAR),
    //TACHI(EpicFightItems.IRON_TACHI.value()),
    TRIDENT(Items.TRIDENT),
    //LONGSWORD(EpicFightItems.IRON_LONGSWORD.value()),
    //DAGGER(EpicFightItems.IRON_DAGGER.value()),
    SHIELD(Items.SHIELD),
    RANGED(Items.BOW)
    ;

    final Item iconItem;
    final int id;

    EpicFightWeaponCategory(Item iconItem) {
        this.iconItem = iconItem;
        this.id = ENUM_MANAGER.assign(this);
    }

    @Override
    public Item iconItem() {
        return iconItem;
    }

    @Override
    public int universalOrdinal() {
        return id;
    }
}
