package guivnf.epicfight.akytheralabs.dataholder;

import com.yesman.akythera.client.api.gui.viewer.Model3DViewer;
import com.yesman.akythera.core.math.TimeWindow;
import com.yesman.akythera.core.util.LogicalSide;
import com.yesman.akytheralabs.akythera.animation.notify.AnimationNotifyStateHolder;
import guivnf.epicfight.world.animation.KeepMomentumNotifyState;

import java.util.Map;

public final class KeepMomentumNotifyStateHolder extends AnimationNotifyStateHolder<KeepMomentumNotifyState> {
    @Override
    public void readFrom(KeepMomentumNotifyState object) {
    }

    @Override
    public KeepMomentumNotifyState makeObject() {
        return new KeepMomentumNotifyState();
    }

    @Override
    public LogicalSide logicalSide() {
        return LogicalSide.BOTH;
    }

    @Override
    public String notifyName() {
        return KeepMomentumNotifyState.class.getSimpleName();
    }

    @Override
    public void simulateBegin(Model3DViewer previewer, TimeWindow window) {
    }

    @Override
    public void simulateTick(Model3DViewer previewer, float prevElapsedTime, float currentElapsedTime) {
    }

    @Override
    public void simulateEnd(Model3DViewer previewer) {
    }

    @Override
    public String sourceTemplate() {
        return "notify_state/keep_momentum.ftl";
    }

    @Override
    public Map<String, Object> getDTO() {
        return Map.of();
    }
}
