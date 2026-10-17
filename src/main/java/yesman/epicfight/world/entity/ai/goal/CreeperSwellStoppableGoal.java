package yesman.epicfight.world.entity.ai.goal;

import net.minecraft.world.entity.ai.goal.SwellGoal;
import net.minecraft.world.entity.monster.Creeper;
import yesman.epicfight.world.capabilities.entitypatch.MobPatch;

public class CreeperSwellStoppableGoal extends SwellGoal {
	protected Creeper creeperEntity;
	protected MobPatch<?> creeperpatch;
	
	public CreeperSwellStoppableGoal(MobPatch<?> creeperpatch, Creeper creeper) {
		super(creeper);
		this.creeperEntity = creeper;
		this.creeperpatch = creeperpatch;
	}
	
	@Override
	public boolean canUse() {
		return super.canUse() && !this.creeperpatch.getEntityState().inaction();
    }
	
	@Override
	public boolean requiresUpdateEveryTick() {
		return false;
	}
	
	@Override
	public void stop() {
		super.stop();
		this.creeperEntity.setSwellDir(-1);
    }
}