package yesman.epicfight.api.animation.types;

import yesman.epicfight.api.animation.AnimationClip;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.Pose;
import yesman.epicfight.api.animation.PoseMirror;
import yesman.epicfight.api.animation.property.AnimationProperty;
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
 * StaticAnimation that plays a wrapped source animation reflected across X = 0.
 *
 * Pose-time decorator: each pose query delegates to the source then runs the result through
 * {@link PoseMirror#mirrorPose(Pose)}, which swaps R/L joint names and negates the X axis.
 * Used for offhand equip/unequip flourishes -- the source clip animates Tool_R / Arm_R / etc.
 * but the offhand variant needs to land on Tool_L / Arm_L. Mirroring at pose-time means we
 * don't need to author a second animation file; reading its own data file gives the offhand
 * variant a {@code left_arms} mask so the mirrored joints survive masking.
 *
 * Unlike a per-layer mirror flag, the reflection is intrinsic to this animation type: a layer
 * running this clip is mirrored, a layer running anything else is not. No state to leak, no
 * gates on living-motion playback.
 */
public class MirroredAnimation extends StaticAnimation {
    private final AssetAccessor<? extends StaticAnimation> source;

    public MirroredAnimation(float transitionTime, boolean repeatPlay,
                             AnimationAccessor<? extends MirroredAnimation> accessor,
                             AssetAccessor<? extends StaticAnimation> source,
                             AssetAccessor<? extends Armature> armature) {
        super(transitionTime, repeatPlay, accessor, armature);
        this.source = source;
    }

    /** Constructor used by {@link DirectMirroredAnimation} -- carries no registered accessor and
     *  derives all data from {@code source}. The {@code registryPath} is informational only;
     *  there is no animmodels JSON file at that path. */
    protected MirroredAnimation(float transitionTime, boolean repeatPlay,
                                String registryPath,
                                AssetAccessor<? extends StaticAnimation> source,
                                AssetAccessor<? extends Armature> armature) {
        super(transitionTime, repeatPlay, registryPath, armature);
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

    private final ThreadLocal<Pose> sourceScratch = ThreadLocal.withInitial(Pose::new);

    @Override
    public Pose getPoseByTime(LivingEntityPatch<?> entitypatch, float time, float partialTicks) {
        return PoseMirror.mirrorPose(this.source.get().getPoseByTime(entitypatch, time, partialTicks), true);
    }

    @Override
    public Pose getPoseByTime(LivingEntityPatch<?> entitypatch, float time, float partialTicks, Pose pose) {
        this.sourceScratch.get().loadDeep(this.source.get().getPoseByTime(entitypatch, time, partialTicks));
        return PoseMirror.mirrorPose(this.sourceScratch.get(), pose, true);
    }

    /** Joints in the mirrored pose are keyed under their reflected name ({@code Arm_R} ↔
     *  {@code Arm_L}). The source clip stores transforms under the unreflected names, so any
     *  caller asking whether this animation drives a particular joint has to flip the name
     *  before checking the source. Without this override
     *  {@link yesman.epicfight.api.client.animation.ClientAnimator#applyBindModifier} skips
     *  the {@code Root} {@code KEEP_CHILD_LOCROT} hook for any mask whose registered key
     *  contains an {@code _L}/{@code _R} suffix that the source doesn't author -- and the
     *  legs stop following the mirrored chest. */
    @Override
    public boolean hasTransformFor(String joint) {
        return this.source.get().hasTransformFor(PoseMirror.mirrorJointName(joint));
    }

    @Override
    public Optional<JointMaskEntry> getJointMaskEntry(LivingEntityPatch<?> entitypatch, boolean useCurrentMotion) {
        // Prefer an explicit mask on the wrapper itself (used by the equip/unequip offhand variants
        // which carry their own data file). Otherwise inherit the source's mask but auto-flip the
        // joint names so a {@code right_arms} mask becomes {@code left_arms}, matching the mirrored
        // pose. Without this flip the offhand variant would mask off the wrong arm.
        Optional<JointMaskEntry> own = this.getProperty(ClientAnimationProperties.JOINT_MASK);
        if (own.isPresent()) {
            return own;
        }
        return this.source.get().getJointMaskEntry(entitypatch, useCurrentMotion).map(JointMaskEntry::mirror);
    }

    @Override
    public <V> Optional<V> getProperty(AnimationProperty<V> propertyType) {
        Optional<V> own = super.getProperty(propertyType);
        return own.isPresent() ? own : this.source.get().getProperty(propertyType);
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
