package guivnf.epicfight.world.animation;

import com.mojang.serialization.MapCodec;
import com.yesman.akythera.api.animation.notify.AnimationNotifyState;
import com.yesman.akythera.api.animation.notify.NotifyTriggerThread;
import com.yesman.akythera.api.world.patch.entity.LivingEntityPatch;
import com.yesman.akythera.api.world.patch.entity.component.EntityMovementComponent;
import com.yesman.akythera.core.animation.playable.PlayableAnimation;
import com.yesman.akythera.core.math.TimeWindow;
import com.yesman.akythera.core.util.LogicalSide;

/// A notify state that keeps the owner's horizontal speed and facing through the air in its time window and ignores its
/// movement input there, so a leap can't be steered, turned or stopped once it has left the ground. On any animation
/// the animator plays (a montage or a state machine's).
///
/// Asks the owner's [EntityMovementComponent] every tick, so it does nothing on an owner without one.
public final class KeepMomentumNotifyState extends AnimationNotifyState<LivingEntityPatch<?>> {
    public static final MapCodec<KeepMomentumNotifyState> CODEC = MapCodec.unit(KeepMomentumNotifyState::new);

    public KeepMomentumNotifyState() {
        super(LogicalSide.BOTH, NotifyTriggerThread.WORLD);
    }

    @Override
    public void onBegin(final LivingEntityPatch<?> owner, final PlayableAnimation animation, final TimeWindow timeWindow) {
        EntityMovementComponent movement = owner.getEntityMovementComponent();

        if (movement != null) movement.keepMomentumInNextUpdate();
    }

    @Override
    public void onTick(final LivingEntityPatch<?> owner, final PlayableAnimation animation, final float prevElapsedTime, final float currentElapsedTime) {
        EntityMovementComponent movement = owner.getEntityMovementComponent();

        if (movement != null) movement.keepMomentumInNextUpdate();
    }

    @Override
    public void onEnd(final LivingEntityPatch<?> owner, final PlayableAnimation animation, final boolean interrupted) {
    }

    @Override
    public MapCodec<? extends AnimationNotifyState<?>> mapCodec() {
        return CODEC;
    }
}
