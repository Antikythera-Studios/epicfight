package com.yesman.epicfight.akythera.animation;

import com.yesman.akythera.api.animation.animator.AnimationInterfaceKey;

public enum EpicFightAnimationInterfaceKey implements AnimationInterfaceKey {
    IDLE, GROUND_EIGHT_DIRECTION_MOVE, JUMP, FALL, SWIM;

    final int id;

    EpicFightAnimationInterfaceKey() {
        this.id = AnimationInterfaceKey.ENUM_MANAGER.assign(this);
    }

    @Override
    public int universalOrdinal() {
        return this.id;
    }
}
