package yesman.epicfight.registry.entries;
import yesman.epicfight.EpicFight;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import yesman.epicfight.registry.deferred_shim.DeferredHolderShim;
import yesman.epicfight.registry.deferred_shim.DeferredRegisterShim;
import yesman.epicfight.main.EpicFightMod;
import yesman.epicfight.world.item.DaggerItem;
import yesman.epicfight.world.item.GloveItem;
import yesman.epicfight.world.item.GreatswordItem;
import yesman.epicfight.world.item.LongswordItem;
import yesman.epicfight.world.item.SkillBookItem;
import yesman.epicfight.world.item.SpearItem;
import yesman.epicfight.world.item.TachiItem;
import yesman.epicfight.world.item.TieredWeaponItem;
import yesman.epicfight.world.item.UchigatanaItem;

public final class EpicFightItems {
	private EpicFightItems() {}
	
	public static final DeferredRegisterShim<Item> REGISTRY = new DeferredRegisterShim<>(Registries.ITEM, EpicFight.MODID);
	
	// Uchigatana & sheath
	public static final DeferredHolderShim<Item, UchigatanaItem> UCHIGATANA = REGISTRY.register("uchigatana",
		() -> new UchigatanaItem(new Item.Properties().rarity(Rarity.RARE).durability(1625).attributes(UchigatanaItem.createUchigatanaAttributes()))
	);
	public static final DeferredHolderShim<Item, Item> UCHIGATANA_SHEATH = REGISTRY.register("uchigatana_sheath",
		() -> new Item(new Item.Properties().rarity(Rarity.EPIC))
	);
	
	// Greatsword variants
	public static final DeferredHolderShim<Item, GreatswordItem> WOODEN_GREATSWORD = REGISTRY.register("wooden_greatsword", () ->
		new GreatswordItem(Tiers.WOOD, new Item.Properties().attributes(GreatswordItem.createGreatswordAttributes(Tiers.WOOD)))
	);
	public static final DeferredHolderShim<Item, GreatswordItem> STONE_GREATSWORD = REGISTRY.register("stone_greatsword", () ->
		new GreatswordItem(Tiers.STONE, new Item.Properties().attributes(GreatswordItem.createGreatswordAttributes(Tiers.STONE)))
	);
	public static final DeferredHolderShim<Item, GreatswordItem> IRON_GREATSWORD = REGISTRY.register("iron_greatsword", () ->
		new GreatswordItem(Tiers.IRON, new Item.Properties().attributes(GreatswordItem.createGreatswordAttributes(Tiers.IRON)))
	);
	public static final DeferredHolderShim<Item, GreatswordItem> GOLDEN_GREATSWORD = REGISTRY.register("golden_greatsword", () ->
		new GreatswordItem(Tiers.GOLD, new Item.Properties().attributes(GreatswordItem.createGreatswordAttributes(Tiers.GOLD)))
	);
	public static final DeferredHolderShim<Item, GreatswordItem> DIAMOND_GREATSWORD = REGISTRY.register("diamond_greatsword", () ->
		new GreatswordItem(Tiers.DIAMOND, new Item.Properties().attributes(GreatswordItem.createGreatswordAttributes(Tiers.DIAMOND)))
	);
	public static final DeferredHolderShim<Item, GreatswordItem> NETHERITE_GREATSWORD = REGISTRY.register("netherite_greatsword", () ->
		new GreatswordItem(Tiers.NETHERITE, new Item.Properties().fireResistant().attributes(GreatswordItem.createGreatswordAttributes(Tiers.NETHERITE)))
	);
	
	// Spear variants
	public static final DeferredHolderShim<Item, SpearItem> WOODEN_SPEAR = REGISTRY.register("wooden_spear", () ->
		new SpearItem(Tiers.WOOD, new Item.Properties().attributes(SpearItem.createSpearAttributes(Tiers.WOOD)))
	);
	public static final DeferredHolderShim<Item, SpearItem> STONE_SPEAR = REGISTRY.register("stone_spear", () ->
		new SpearItem(Tiers.STONE, new Item.Properties().attributes(SpearItem.createSpearAttributes(Tiers.STONE)))
	);
	public static final DeferredHolderShim<Item, SpearItem> IRON_SPEAR = REGISTRY.register("iron_spear", () ->
		new SpearItem(Tiers.IRON, new Item.Properties().attributes(SpearItem.createSpearAttributes(Tiers.IRON)))
	);
	public static final DeferredHolderShim<Item, SpearItem> GOLDEN_SPEAR = REGISTRY.register("golden_spear", () ->
		new SpearItem(Tiers.GOLD, new Item.Properties().attributes(SpearItem.createSpearAttributes(Tiers.GOLD)))
	);
	public static final DeferredHolderShim<Item, SpearItem> DIAMOND_SPEAR = REGISTRY.register("diamond_spear", () ->
		new SpearItem(Tiers.DIAMOND, new Item.Properties().attributes(SpearItem.createSpearAttributes(Tiers.DIAMOND)))
	);
	public static final DeferredHolderShim<Item, SpearItem> NETHERITE_SPEAR = REGISTRY.register("netherite_spear", () ->
		new SpearItem(Tiers.NETHERITE, new Item.Properties().fireResistant().attributes(SpearItem.createSpearAttributes(Tiers.NETHERITE)))
	);
	
	// Tachi variants
	public static final DeferredHolderShim<Item, TachiItem> WOODEN_TACHI = REGISTRY.register("wooden_tachi", () ->
		new TachiItem(Tiers.WOOD, new Item.Properties().attributes(TachiItem.createTachiAttributes(Tiers.WOOD)))
	);
	public static final DeferredHolderShim<Item, TachiItem> STONE_TACHI = REGISTRY.register("stone_tachi", () ->
		new TachiItem(Tiers.STONE, new Item.Properties().attributes(TachiItem.createTachiAttributes(Tiers.STONE)))
	);
	public static final DeferredHolderShim<Item, TachiItem> IRON_TACHI = REGISTRY.register("iron_tachi", () ->
		new TachiItem(Tiers.IRON, new Item.Properties().attributes(TachiItem.createTachiAttributes(Tiers.IRON)))
	);
	public static final DeferredHolderShim<Item, TachiItem> GOLDEN_TACHI = REGISTRY.register("golden_tachi", () ->
		new TachiItem(Tiers.GOLD, new Item.Properties().attributes(TachiItem.createTachiAttributes(Tiers.GOLD)))
	);
	public static final DeferredHolderShim<Item, TachiItem> DIAMOND_TACHI = REGISTRY.register("diamond_tachi", () ->
		new TachiItem(Tiers.DIAMOND, new Item.Properties().attributes(TachiItem.createTachiAttributes(Tiers.DIAMOND)))
	);
	public static final DeferredHolderShim<Item, TachiItem> NETHERITE_TACHI = REGISTRY.register("netherite_tachi", () ->
		new TachiItem(Tiers.NETHERITE, new Item.Properties().fireResistant().attributes(TachiItem.createTachiAttributes(Tiers.NETHERITE)))
	);
	
	// Longsword variants
	public static final DeferredHolderShim<Item, LongswordItem> WOODEN_LONGSWORD = REGISTRY.register("wooden_longsword", () ->
		new LongswordItem(Tiers.WOOD, new Item.Properties().attributes(LongswordItem.createLongswordAttributes(Tiers.WOOD)))
	);
	public static final DeferredHolderShim<Item, LongswordItem> STONE_LONGSWORD = REGISTRY.register("stone_longsword", () ->
		new LongswordItem(Tiers.STONE, new Item.Properties().attributes(LongswordItem.createLongswordAttributes(Tiers.STONE)))
	);
	public static final DeferredHolderShim<Item, LongswordItem> IRON_LONGSWORD = REGISTRY.register("iron_longsword", () ->
		new LongswordItem(Tiers.IRON, new Item.Properties().attributes(LongswordItem.createLongswordAttributes(Tiers.IRON)))
	);
	public static final DeferredHolderShim<Item, LongswordItem> GOLDEN_LONGSWORD = REGISTRY.register("golden_longsword", () ->
		new LongswordItem(Tiers.GOLD, new Item.Properties().attributes(LongswordItem.createLongswordAttributes(Tiers.GOLD)))
	);
	public static final DeferredHolderShim<Item, LongswordItem> DIAMOND_LONGSWORD = REGISTRY.register("diamond_longsword", () ->
		new LongswordItem(Tiers.DIAMOND, new Item.Properties().attributes(LongswordItem.createLongswordAttributes(Tiers.DIAMOND)))
	);
	public static final DeferredHolderShim<Item, LongswordItem> NETHERITE_LONGSWORD = REGISTRY.register("netherite_longsword", () ->
		new LongswordItem(Tiers.NETHERITE, new Item.Properties().fireResistant().attributes(LongswordItem.createLongswordAttributes(Tiers.NETHERITE)))
	);
	
	// Dagger variants
	public static final DeferredHolderShim<Item, DaggerItem> WOODEN_DAGGER = REGISTRY.register("wooden_dagger", () ->
		new DaggerItem(Tiers.WOOD, new Item.Properties().attributes(DaggerItem.createDaggerAttributes(Tiers.WOOD)))
	);
	public static final DeferredHolderShim<Item, DaggerItem> STONE_DAGGER = REGISTRY.register("stone_dagger", () ->
		new DaggerItem(Tiers.STONE, new Item.Properties().attributes(DaggerItem.createDaggerAttributes(Tiers.STONE)))
	);
	public static final DeferredHolderShim<Item, DaggerItem> IRON_DAGGER = REGISTRY.register("iron_dagger", () ->
		new DaggerItem(Tiers.IRON, new Item.Properties().attributes(DaggerItem.createDaggerAttributes(Tiers.IRON)))
	);
	public static final DeferredHolderShim<Item, DaggerItem> GOLDEN_DAGGER = REGISTRY.register("golden_dagger", () ->
		new DaggerItem(Tiers.GOLD, new Item.Properties().attributes(DaggerItem.createDaggerAttributes(Tiers.GOLD)))
	);
	public static final DeferredHolderShim<Item, DaggerItem> DIAMOND_DAGGER = REGISTRY.register("diamond_dagger", () ->
		new DaggerItem(Tiers.DIAMOND, new Item.Properties().attributes(DaggerItem.createDaggerAttributes(Tiers.DIAMOND)))
	);
	public static final DeferredHolderShim<Item, DaggerItem> NETHERITE_DAGGER = REGISTRY.register("netherite_dagger", () ->
		new DaggerItem(Tiers.NETHERITE, new Item.Properties().fireResistant().attributes(DaggerItem.createDaggerAttributes(Tiers.NETHERITE)))
	);
	
	// Weapons etc
	public static final DeferredHolderShim<Item, GloveItem> GLOVE = REGISTRY.register("glove", () ->
		new GloveItem(new Item.Properties().durability(1625).attributes(TieredWeaponItem.createAttributes(2.0F, 0.0F)))
	);
	public static final DeferredHolderShim<Item, SwordItem> BOKKEN = REGISTRY.register("bokken", () ->
		new SwordItem(Tiers.WOOD, new Item.Properties().attributes(SwordItem.createAttributes(Tiers.WOOD, 3, -2.4F)))
	);
	
	// Stray armor
	public static final DeferredHolderShim<Item, ArmorItem> STRAY_HAT = REGISTRY.register("stray_hat", () -> new ArmorItem(EpicFightArmorMaterials.STRAY_CLOTH, ArmorItem.Type.HELMET, new Item.Properties()));
	public static final DeferredHolderShim<Item, ArmorItem> STRAY_ROBE = REGISTRY.register("stray_robe", () -> new ArmorItem(EpicFightArmorMaterials.STRAY_CLOTH, ArmorItem.Type.CHESTPLATE, new Item.Properties()));
	public static final DeferredHolderShim<Item, ArmorItem> STRAY_PANTS = REGISTRY.register("stray_pants", () -> new ArmorItem(EpicFightArmorMaterials.STRAY_CLOTH, ArmorItem.Type.LEGGINGS, new Item.Properties()));
	
	public static final DeferredHolderShim<Item, SkillBookItem> SKILLBOOK = REGISTRY.register("skillbook", () -> new SkillBookItem(new Item.Properties().rarity(Rarity.RARE).stacksTo(1)));
}