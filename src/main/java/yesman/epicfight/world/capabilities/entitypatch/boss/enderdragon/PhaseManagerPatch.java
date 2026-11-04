package yesman.epicfight.world.capabilities.entitypatch.boss.enderdragon;

import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.DragonPhaseInstance;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhaseManager;

public class PhaseManagerPatch extends EnderDragonPhaseManager {
	private final DragonPhaseInstance[] patchedPhases = new DragonPhaseInstance[EnderDragonPhase.getCount()];
	
	public PhaseManagerPatch(EnderDragon dragon, EnderDragonPatch dragonpatch) {
		super(dragon);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <T extends DragonPhaseInstance> T getPhase(EnderDragonPhase<T> phase) {
		if (this.patchedPhases != null) {
			int i = phase.getId();
			
			if (this.patchedPhases[i] == null) {
				this.patchedPhases[i] = phase.createInstance(this.dragon);
			}
			
			return (T)this.patchedPhases[i];
		} else {
			return (T)phase.createInstance(this.dragon);
		}
	}
	
	@Override
	public void setPhase(EnderDragonPhase<?> phase) {
		if (phase.createInstance(this.dragon) instanceof PatchedDragonPhase || phase == EnderDragonPhase.DYING) {
			super.setPhase(phase);
		}
	}
}