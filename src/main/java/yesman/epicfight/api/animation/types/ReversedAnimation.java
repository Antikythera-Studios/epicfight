package yesman.epicfight.api.animation.types;

import yesman.epicfight.api.animation.AnimationClip;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.Pose;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.client.animation.Layer;
import yesman.epicfight.api.client.animation.property.ClientAnimationProperties;
import yesman.epicfight.api.client.animation.property.JointMaskEntry;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.api.utils.side.ClientOnly;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

import java.util.List;
import java.util.Optional;

/**
 * StaticAnimation that plays a wrapped source animation backwards.
 *
 * Implements reverse playback as a pose-time decorator: the wrapper's elapsed-time runs
 * forward like a normal animation, but each pose query maps {@code t -> totalTime - t} on
 * the source. This avoids touching {@link yesman.epicfight.api.animation.AnimationPlayer}'s
 * reversed flag, which is geared toward looping movement clips and would also flip the link
 * animation that runs at the start of playback.
 *
 * Used for unequip flourishes -- the equip clip runs end-to-start so the item shrinks back
 * to scale 0 and the arm retracts. No second JSON file is required.
 */
public class ReversedAnimation extends StaticAnimation {
    private final AssetAccessor<? extends StaticAnimation> source;

    public ReversedAnimation(float transitionTime, boolean repeatPlay,
                             AnimationAccessor<? extends ReversedAnimation> accessor,
                             AssetAccessor<? extends StaticAnimation> source,
                             AssetAccessor<? extends Armature> armature) {
        super(transitionTime, repeatPlay, accessor, armature);
        this.source = source;
    }

    @Override
    public List<AssetAccessor<? extends StaticAnimation>> getSubAnimations() {
        return List.of();
    }

    @Override
    public AnimationClip getAnimationClip() {
        return this.source.get().getAnimationClip();
    }

    @Override
    public Pose getPoseByTime(LivingEntityPatch<?> entitypatch, float time, float partialTicks) {
        float reversedTime = Math.max(0.0F, this.getTotalTime() - time);
        return this.source.get().getPoseByTime(entitypatch, reversedTime, partialTicks);
    }

    @Override
    public Pose getPoseByTime(LivingEntityPatch<?> entitypatch, float time, float partialTicks, Pose pose) {
        float reversedTime = Math.max(0.0F, this.getTotalTime() - time);
        return pose.loadDeep(this.source.get().getPoseByTime(entitypatch, reversedTime, partialTicks));
    }

    @Override
    public Optional<JointMaskEntry> getJointMaskEntry(LivingEntityPatch<?> entitypatch, boolean useCurrentMotion) {
        // Prefer this wrapper's own data file (so an unequip variant can opt into a different
        // mask / priority than its forward counterpart). Fall back to the source's mask when
        // the wrapper has no data file of its own.
        Optional<JointMaskEntry> own = this.getProperty(ClientAnimationProperties.JOINT_MASK);
        return own.isPresent() ? own : this.source.get().getJointMaskEntry(entitypatch, useCurrentMotion);
    }

    @Override @ClientOnly
    public Layer.Priority getPriority() {
        return this.getProperty(ClientAnimationProperties.PRIORITY).orElseGet(() -> this.source.get().getPriority());
    }

    @Override @ClientOnly
    public Layer.LayerType getLayerType() {
        return this.getProperty(ClientAnimationProperties.LAYER_TYPE).orElseGet(() -> this.source.get().getLayerType());
    }

    @Override
    public boolean isClientAnimation() {
        return true;
    }

    public AssetAccessor<? extends StaticAnimation> getSource() {
        return this.source;
    }
}
