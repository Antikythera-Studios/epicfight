package com.yesman.epicfight.asset.armature;

import com.yesman.akythera.core.asset.armature.Armature;
import com.yesman.akythera.core.asset.armature.Bone;
import org.jspecify.annotations.NullMarked;

import java.util.Map;

@NullMarked
public class HumanoidArmature extends Armature {
    private final Bone pelvis;
    private final Bone body;
    private final Bone bodyBend;
    private final Bone head;
    private final Bone rightShoulder;
    private final Bone rightArm;
    private final Bone rightArmBend;
    private final Bone rightHandItemHolder;
    private final Bone rightElbow;
    private final Bone leftShoulder;
    private final Bone leftArm;
    private final Bone leftArmBend;
    private final Bone leftHandItemHolder;
    private final Bone leftElbow;
    private final Bone rightLeg;
    private final Bone rightLegBend;
    private final Bone rightKnee;
    private final Bone leftLeg;
    private final Bone leftLegBend;
    private final Bone leftKnee;

    public HumanoidArmature(Bone rootBone, int boneCount, Map<String, Bone> bonesByName) {
        super(rootBone, boneCount, bonesByName);

        pelvis = getBoneByNameOrThrow("pelvis");
        body = getBoneByNameOrThrow("body");
        bodyBend = getBoneByNameOrThrow("body_bend");
        head = getBoneByNameOrThrow("head");
        rightShoulder = getBoneByNameOrThrow("right_shoulder");
        rightArm = getBoneByNameOrThrow("right_arm");
        rightArmBend = getBoneByNameOrThrow("right_arm_bend");
        rightHandItemHolder = getBoneByNameOrThrow("right_hand_itemholder");
        rightElbow = getBoneByNameOrThrow("right_elbow");
        leftShoulder = getBoneByNameOrThrow("left_shoulder");
        leftArm = getBoneByNameOrThrow("left_arm");
        leftArmBend = getBoneByNameOrThrow("left_arm_bend");
        leftHandItemHolder = getBoneByNameOrThrow("left_hand_itemholder");
        leftElbow = getBoneByNameOrThrow("left_elbow");
        rightLeg = getBoneByNameOrThrow("right_leg");
        rightLegBend = getBoneByNameOrThrow("right_leg_bend");
        rightKnee = getBoneByNameOrThrow("right_knee");
        leftLeg = getBoneByNameOrThrow("left_leg");
        leftLegBend = getBoneByNameOrThrow("left_leg_bend");
        leftKnee = getBoneByNameOrThrow("left_knee");
    }

    public final Bone pelvis() {
        return pelvis;
    }

    public final Bone body() {
        return body;
    }

    public final Bone bodyBend() {
        return bodyBend;
    }

    public final Bone head() {
        return head;
    }

    public final Bone rightShoulder() {
        return rightShoulder;
    }

    public final Bone rightArm() {
        return rightArm;
    }

    public final Bone rightArmBend() {
        return rightArmBend;
    }

    public final Bone rightHandItemHolder() {
        return rightHandItemHolder;
    }

    public final Bone rightElbow() {
        return rightElbow;
    }

    public final Bone leftShoulder() {
        return leftShoulder;
    }

    public final Bone leftArm() {
        return leftArm;
    }

    public final Bone leftArmBend() {
        return leftArmBend;
    }

    public final Bone leftHandItemHolder() {
        return leftHandItemHolder;
    }

    public final Bone leftElbow() {
        return leftElbow;
    }

    public final Bone rightLeg() {
        return rightLeg;
    }

    public final Bone rightLegBend() {
        return rightLegBend;
    }

    public final Bone rightKnee() {
        return rightKnee;
    }

    public final Bone leftLeg() {
        return leftLeg;
    }

    public final Bone leftLegBend() {
        return leftLegBend;
    }

    public final Bone leftKnee() {
        return leftKnee;
    }
}
