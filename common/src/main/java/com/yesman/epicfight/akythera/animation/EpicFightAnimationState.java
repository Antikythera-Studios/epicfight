package com.yesman.epicfight.akythera.animation;

import com.yesman.akythera.core.animation.driver.FSMAnimationState;

public enum EpicFightAnimationState implements FSMAnimationState {
    IN_WATER,
    ON_GROUND,
    ON_AIR,
    IDLE,
    MOVE,
    SWIM,
    ;

    final int id;

    EpicFightAnimationState() {
        this.id = FSMAnimationState.ENUM_MANAGER.assign(this);
    }

    @Override
    public int universalOrdinal() {
        return this.id;
    }
}
