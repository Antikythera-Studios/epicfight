package yesman.epicfight.world.capabilities.entitypatch.boss.enderdragon;

import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;

public class PatchedPhases {
	public static final EnderDragonPhase<DragonFlyingPhase> FLYING = EnderDragonPhase.create(DragonFlyingPhase.class, "Flying");
	public static final EnderDragonPhase<DragonGroundBattlePhase> GROUND_BATTLE = EnderDragonPhase.create(DragonGroundBattlePhase.class, "Ground Battle");
	public static final EnderDragonPhase<DragonLandingPhase> LANDING = EnderDragonPhase.create(DragonLandingPhase.class, "Landing");
	public static final EnderDragonPhase<DragonAirstrikePhase> AIRSTRIKE = EnderDragonPhase.create(DragonAirstrikePhase.class, "Airstrike");
	public static final EnderDragonPhase<DragonCrystalLinkPhase> CRYSTAL_LINK = EnderDragonPhase.create(DragonCrystalLinkPhase.class, "Crystal Link");
	public static final EnderDragonPhase<DragonNeutralizedPhase> NEUTRALIZED = EnderDragonPhase.create(DragonNeutralizedPhase.class, "Neutralized");
}