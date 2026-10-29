package yesman.epicfight.world.damagesource;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;
import yesman.epicfight.EpicFight;

public final class EpicFightDamageTypes {
	private EpicFightDamageTypes() {}
	
	public static final ResourceKey<DamageType> SHOCKWAVE = ResourceKey.create(Registries.DAMAGE_TYPE, EpicFight.identifier("shockwave"));
	public static final ResourceKey<DamageType> WITHER_BEAM = ResourceKey.create(Registries.DAMAGE_TYPE, EpicFight.identifier("wither_beam"));
}