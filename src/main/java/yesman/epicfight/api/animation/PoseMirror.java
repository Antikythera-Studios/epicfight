package yesman.epicfight.api.animation;

import org.joml.Quaternionf;
import yesman.epicfight.api.utils.math.Vec3f;

import java.util.Map;

/**
 * Reflects a {@link Pose} across the model's sagittal plane (X = 0).
 *
 * The biped armature is laid out so positive X is the right side and negative X is the left:
 * {@code Thigh_R} parent-relative translation is +0.125, {@code Thigh_L} is -0.125, and similarly
 * for shoulders/arms/hands. Mirroring an animation therefore means (a) negate translation X,
 * (b) flip the rotation handedness across the YZ plane, (c) swap data between {@code *_R} and
 * {@code *_L} joint pairs.
 *
 * Used at pose-evaluation time so a single authored animation can serve both hands without
 * authoring a duplicate file.
 */
public final class PoseMirror {
    private PoseMirror() {}

    private static final String SUFFIX_R = "_R";
    private static final String SUFFIX_L = "_L";

    /** Returns the joint name on the opposite side of the body, or the input unchanged for
     *  joints that have no side (exemple: Root, Torso, Chest, Head). */
    public static String mirrorJointName(String name) {
        if (name.endsWith(SUFFIX_R)) {
            return name.substring(0, name.length() - SUFFIX_R.length()) + SUFFIX_L;
        }
        if (name.endsWith(SUFFIX_L)) {
            return name.substring(0, name.length() - SUFFIX_L.length()) + SUFFIX_R;
        }
        return name;
    }

    /** Writes the X-mirror of {@code source} into {@code dest} and returns {@code dest}. */
    public static JointTransform mirrorJointTransform(JointTransform source, JointTransform dest) {
        return mirrorJointTransform(source, dest, false);
    }

    /** Same as {@link #mirrorJointTransform(JointTransform, JointTransform)} but optionally keeps
     *  the {@link JointTransform#RESULT1} (frontResult) entry untouched. The Head joint funnels the
     *  player's live mouse-look yaw through frontResult, which is set per-frame to track the
     *  camera. The pose mirror would normally flip Yaw(theta) to Yaw(-theta) along with everything
     *  else, and because {@link yesman.epicfight.api.client.animation.ClientAnimator#getPose}
     *  interpolates between the unmirrored and mirrored poses during the F-swap blend, the
     *  resulting head rotation passes through Yaw(0) at blend = 0.5 -- the head snaps to face
     *  forward mid-transition before settling. Holding RESULT1 unchanged means the rendered head
     *  stays locked to the actual look direction throughout the blend, so only the animation's
     *  baseline pose mirrors (chest, body, etc.) while the camera-driven rotation is identity in
     *  both endpoints and never blends through forward. */
    public static JointTransform mirrorJointTransform(JointTransform source, JointTransform dest, boolean preserveFrontResult) {
        Vec3f t = dest.translation();
        Vec3f sourceTranslation = source.translation();
        t.set(-sourceTranslation.x, sourceTranslation.y, sourceTranslation.z);

        // A reflection across the X = 0 plane conjugates a rotation (qx, qy, qz, qw) into
        // (qx, -qy, -qz, qw). The rotation axis reflects (X stays, Y/Z flip) and the rotation
        // handedness flips; the two together reduce to negating qy and qz.
        Quaternionf q = dest.rotation();
        Quaternionf sourceRotation = source.rotation();
        q.set(sourceRotation.x, -sourceRotation.y, -sourceRotation.z, sourceRotation.w);

        // Scale is invariant: each joint's local axes mirror together with its geometry, so
        // axis-aligned scale values describe the same shape on either side.
        dest.scale().set(source.scale());

        // The auxiliary transform stack (PARENT/JOINT_LOCAL_TRANSFORM / ANIMATION_TRANSFORM/
        // also has to be reflected. JointMask.KEEP_CHILD_LOCROT writes X-asymmetric data
        // here -- a half-X chest delta into PARENT and the chest's currentToLowest delta into
        // JOINT_LOCAL_TRANSFORM -- so the masked legs follow the upper body during guard. If we
        // leave these entries un-mirrored, the leg compensation pulls toward the un-mirrored
        // chest pose even though the rendered chest is on the other side. That's the "legs
        // misplaced when walking + guarding on offhand" bug -- amplified by sneak because the
        // chest delta the modifier corrects for grows. Each entry is mirrored into dest's own
        // TransformEntry objects (reused when already present) so the source pose stays
        // untouched and repeated mirroring stays allocation-free.
        Map<String, JointTransform.TransformEntry> entries = dest.entries();
        Map<String, JointTransform.TransformEntry> sourceEntries = source.entries();

        entries.keySet().removeIf(key -> !sourceEntries.containsKey(key));

        for (Map.Entry<String, JointTransform.TransformEntry> e : sourceEntries.entrySet()) {
            JointTransform.TransformEntry destEntry = entries.get(e.getKey());

            if (preserveFrontResult && JointTransform.RESULT1.equals(e.getKey())) {
                if (destEntry == null) {
                    entries.put(e.getKey(), new JointTransform.TransformEntry(e.getValue().multiplyFunction, e.getValue().transform.copy()));
                } else {
                    destEntry.multiplyFunction = e.getValue().multiplyFunction;
                    destEntry.transform.copyFrom(e.getValue().transform);
                }
                continue;
            }

            if (destEntry == null) {
                entries.put(e.getKey(), new JointTransform.TransformEntry(e.getValue().multiplyFunction, mirrorJointTransform(e.getValue().transform, JointTransform.empty())));
            } else {
                destEntry.multiplyFunction = e.getValue().multiplyFunction;
                mirrorJointTransform(e.getValue().transform, destEntry.transform);
            }
        }

        return dest;
    }

    /** Builds a new pose containing the X-mirrored joint data of {@code source} with R/L joint
     *  names swapped. The result is independent of the input.
     *
     *  Every joint's {@link JointTransform#RESULT1 frontResult} is preserved verbatim. The
     *  frontResult slot is reserved for runtime-injected data already expressed in
     *  world-aligned terms -- the {@code Head}'s mouse-look yaw, the {@code Chest}'s spyglass /
     *  aim yaw correction (driven by {@code getYHeadRot() - yBodyRot}), shoulder pitch from
     *  riding-pose modifiers, IK targets, etc. Passing those through the {@code (qx, -qy, -qz)}
     *  mirror inverts the live yaw direction, which is exactly the "camera left, body right"
     *  inversion the spyglass exhibits when its meta-animation's POSE_MODIFIER (propagated to
     *  the {@link yesman.epicfight.api.animation.types.MirroredAnimation} via
     *  {@link yesman.epicfight.api.animation.types.StaticAnimation#addProperty StaticAnimation.addProperty})
     *  writes the yaw correction onto the source pose only to have the X-mirror flip it back.
     *  The base transform still mirrors so the body shape itself reflects normally. */
    public static Pose mirrorPose(Pose source) {
        return mirrorPose(source, false);
    }

    /** {@link #mirrorPose(Pose)} with an option to leave the Root joint un-reflected.
     *
     *  This split exists because two callers ask for different things:
     *
     *  - {@link yesman.epicfight.api.animation.types.MirroredAnimation#getPoseByTime} (called
     *    when an {@code OFFHAND} sub-animation of a {@link yesman.epicfight.api.animation.types.MirrorAnimation}
     *    or any standalone {@code MirroredAnimation} accessor produces its layer pose) needs
     *    {@code preserveRoot = true}. The {@link yesman.epicfight.api.client.animation.property.JointMask#KEEP_CHILD_LOCROT}
     *    bind modifier compares the layer's Root to the (unmirrored) base layer's Root to
     *    derive leg compensation, and a clip like spyglass authors a non-trivial Root keyframe.
     *    Reflecting it makes the delta cross two mirror states -- legs lock into a wrong
     *    rotation in steady-state, and the link transition into walk teleports because the
     *    link's interpolated Root no longer matches the mainhand path. Leaving Root verbatim
     *    means legs compensate identically to mainhand and link blending stays continuous.
     *
     *  - {@link yesman.epicfight.api.client.animation.ClientAnimator#getPose} (called when
     *    {@code entitypatch.isMirrorMode()} is true to flip the entire composed pose for an
     *    offhand-only weapon) needs {@code preserveRoot = false}. There is no follow-up bind
     *    modifier pass after the global mirror, and animations like the trident throw
     *    ({@link AimAnimation} / {@link ReboundAnimation}) author a Root yaw that has to flip
     *    so the body's overall facing reflects -- otherwise the throw lean ends up backward
     *    on offhand. */
    public static Pose mirrorPose(Pose source, boolean preserveRoot) {
        return mirrorPose(source, new Pose(), preserveRoot);
    }

    /** {@link #mirrorPose(Pose, boolean)} writing into {@code dest}, reusing its stored
     *  {@link JointTransform} objects so repeated mirroring stays allocation-free. Joints in
     *  {@code dest} whose mirrored name has no source counterpart are dropped. */
    public static Pose mirrorPose(Pose source, Pose dest, boolean preserveRoot) {
        dest.getJointTransformData().keySet().removeIf(name -> !source.hasTransform(mirrorJointName(name)));

        for (Map.Entry<String, JointTransform> entry : source.getJointTransformData().entrySet()) {
            String sourceName = entry.getKey();
            String mirroredName = mirrorJointName(sourceName);

            if (preserveRoot && "Root".equals(sourceName)) {
                dest.getOrCreateJointTransform(sourceName).copyFrom(entry.getValue());
                continue;
            }

            // The two callers want different frontResult policies:
            //
            //   - MirroredAnimation per-layer mirror ({@code preserveRoot = true}): the source
            //     animation has already had its POSE_MODIFIER pass run before we mirror. Things
            //     like the spyglass's {@code chest.frontResult} carry a world-aligned yaw
            //     (driven by {@code yHeadRot - yBodyRot}); flipping qy turns "lean toward camera"
            //     into "lean away from camera." Preserving every joint's frontResult keeps that
            //     correction pointing the right way on offhand. The spyglass clip doesn't pair
            //     it with a head.rotation write, so there's no cancellation to break.
            //
            //   - Global pose mirror in mirror mode ({@code preserveRoot = false}): a follow-up
            //     pass over the already-composed pose for offhand-only-weapon mode. Animations
            //     like {@link AimAnimation} (trident, bow, javelin) write {@code head.rotation}
            //     and {@code chest.frontResult} with opposite signs that cancel into a balanced
            //     world-space orientation. Mirroring them together keeps the cancellation; we
            //     only protect Head's frontResult, which holds the live mouse-look yaw written
            //     by {@code poseTick} that one is already world-aligned and reflecting it
            //     would invert camera tracking.
            boolean preserveFrontResult = preserveRoot || "Head".equals(sourceName);
            mirrorJointTransform(entry.getValue(), dest.getOrCreateJointTransform(mirroredName), preserveFrontResult);
        }
        return dest;
    }
}
