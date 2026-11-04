package yesman.epicfight.world.entity.ai.goal;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import yesman.epicfight.api.animation.types.EntityState;
import yesman.epicfight.world.capabilities.entitypatch.MobPatch;

public class AnimatedAttackGoal<T extends MobPatch<?>> extends Goal {
	protected final T mobpatch;
	protected final CombatBehaviors<T> combatBehaviors;
	
	public AnimatedAttackGoal(T mobpatch, CombatBehaviors<T> combatBehaviors) {
		this.mobpatch = mobpatch;
		this.combatBehaviors = combatBehaviors;
	}
	
	@Override
	public boolean canUse() {
		return this.checkTargetValid();
	}
	
	@Override
	public void tick() {
		if (this.mobpatch.getTarget() == null) {
			return;	
		}
		
		EntityState state = this.mobpatch.getEntityState();
		this.combatBehaviors.tick();
		
		if (this.combatBehaviors.hasActivatedMove()) {
			if (state.canBasicAttack()) {
				CombatBehaviors.Behavior<T> result = this.combatBehaviors.tryProceed();
				
				if (result != null) {
					result.execute(this.mobpatch);
				}
			}
		} else {
			if (!state.inaction()) {
				CombatBehaviors.Behavior<T> result = this.combatBehaviors.selectRandomBehaviorSeries();
				
				if (result != null) {
					result.execute(this.mobpatch);
				}
			}
		}
	}
	
	protected boolean checkTargetValid() {
		LivingEntity livingentity = this.mobpatch.getTarget();
		
		if (livingentity == null) {
			return false;
		} else if (!livingentity.isAlive()) {
			return false;
		} else {
			return !(livingentity instanceof Player player) || !livingentity.isSpectator() && !player.isCreative();
		}
	}
}