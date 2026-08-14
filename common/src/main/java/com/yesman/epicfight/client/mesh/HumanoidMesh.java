package com.yesman.epicfight.client.mesh;

import com.yesman.akythera.client.core.asset.mesh.SkeletalMesh;
import com.yesman.akythera.core.Akythera;

public class HumanoidMesh extends SkeletalMesh {
    public final SkeletalMeshPart head;
    public final SkeletalMeshPart torso;
    public final SkeletalMeshPart leftArm;
    public final SkeletalMeshPart rightArm;
    public final SkeletalMeshPart leftLeg;
    public final SkeletalMeshPart rightLeg;
    public final SkeletalMeshPart hat;
    public final SkeletalMeshPart jacket;
    public final SkeletalMeshPart leftSleeve;
    public final SkeletalMeshPart rightSleeve;
    public final SkeletalMeshPart leftPants;
    public final SkeletalMeshPart rightPants;
    
    public HumanoidMesh(SkeletalMeshBuilder meshBuilder) {
        super(meshBuilder);

        head = getPart("head", Akythera.LOGGER::warn);
        torso = getPart("body", Akythera.LOGGER::warn);
        leftArm = getPart("left_arm", Akythera.LOGGER::warn);
        rightArm = getPart("right_arm", Akythera.LOGGER::warn);
        leftLeg = getPart("left_leg", Akythera.LOGGER::warn);
        rightLeg = getPart("right_leg", Akythera.LOGGER::warn);

        hat = getPart("hat", Akythera.LOGGER::warn);
        jacket = getPart("jacket", Akythera.LOGGER::warn);
        leftSleeve = getPart("left_sleeve", Akythera.LOGGER::warn);
        rightSleeve = getPart("right_sleeve", Akythera.LOGGER::warn);
        leftPants = getPart("left_pants", Akythera.LOGGER::warn);
        rightPants = getPart("right_pants", Akythera.LOGGER::warn);
    }
}
