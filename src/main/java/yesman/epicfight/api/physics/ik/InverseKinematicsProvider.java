package yesman.epicfight.api.physics.ik;

import org.joml.Quaternionf;

import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import yesman.epicfight.api.animation.Joint;
import yesman.epicfight.api.animation.JointTransform;
import yesman.epicfight.api.animation.Pose;
import yesman.epicfight.api.animation.TransformSheet;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.api.physics.SimulationProvider;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.api.utils.math.QuaternionUtils;
import yesman.epicfight.api.utils.math.Vec3f;
import yesman.epicfight.world.capabilities.entitypatch.boss.enderdragon.EnderDragonPatch;

public interface InverseKinematicsProvider extends SimulationProvider<InverseKinematicsSimulatable, InverseKinematicsSimulator.InverseKinematicsObject, InverseKinematicsSimulator.InverseKinematicsBuilder, InverseKinematicsProvider> {
	default TransformSheet clipAnimation(TransformSheet transformSheet, InverseKinematicsSimulator.BakedInverseKinematicsDefinition ikDefinition) {
		if (ikDefinition.clipAnimation()) {
			return transformSheet.copy(ikDefinition.startFrame(), ikDefinition.endFrame());
		} else {
			return transformSheet.getFirstFrame();
		}
	}
	
	default void startPartAnimation(InverseKinematicsSimulator.BakedInverseKinematicsDefinition bakedIKDefinition, InverseKinematicsSimulator.InverseKinematicsObject ikObject, TransformSheet partAnimation, Vec3f targetpos) {
		Vec3f footpos = ikObject.getTipPosition(1.0F);
		Vec3f worldStartToEnd = targetpos.copy().sub(footpos);
		partAnimation.correctAnimationByNewPosition(bakedIKDefinition.startPosition(), bakedIKDefinition.startToEnd(), footpos, worldStartToEnd);
		ikObject.start(targetpos, partAnimation, 1.0F);
	}
	
	default void startSimple(InverseKinematicsSimulator.InverseKinematicsObject ikObject) {
		ikObject.start(new Vec3f(0.0F, 0.0F, 0.0F), ikObject.getAnimation(), 1.0F);
	}
	
	default Vec3f getRayCastedTipPosition(InverseKinematicsSimulatable ikSimulatable, Vec3f clipStart, OpenMatrix4f toWorldCoord, float maxYDown, float leastHeight) {
		Vec3f clipStartWorld = OpenMatrix4f.transform3v(toWorldCoord, clipStart, null);
		BlockHitResult clipResult = ikSimulatable.toEntity().level().clip(
			new ClipContext(
				  new Vec3(clipStartWorld.x, clipStartWorld.y, clipStartWorld.z)
				, new Vec3(clipStartWorld.x, clipStartWorld.y - maxYDown, clipStartWorld.z)
				, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, ikSimulatable.toEntity()
			)
		);

		float dy = (clipResult.getType() != HitResult.Type.MISS) ? clipStartWorld.y - clipResult.getBlockPos().getY() - 1 : maxYDown;

		return new Vec3f(clipStartWorld.x, clipStartWorld.y - dy + leastHeight, clipStartWorld.z);
	}
	
	default void correctRootRotation(JointTransform rootTransform, EnderDragonPatch enderdragonpatch, float partialTicks) {
		float xRoot = enderdragonpatch.getRootXRotO() + (enderdragonpatch.getRootXRot() - enderdragonpatch.getRootXRotO()) * partialTicks;
		float zRoot = enderdragonpatch.getRootZRotO() + (enderdragonpatch.getRootZRot() - enderdragonpatch.getRootZRotO()) * partialTicks;
		Quaternionf quat = QuaternionUtils.ZP.rotationDegrees(zRoot);
		quat.mul(QuaternionUtils.XP.rotationDegrees(-xRoot));

		rootTransform.frontResult(JointTransform.rotation(quat), OpenMatrix4f::mulAsOriginInverse);
	}
	
	default void applyFabrikToJoint(Vec3f recalculatedPosition, Pose pose, Armature armature, Joint startJoint, Joint endJoint, Quaternionf tipRotation) {
		FABRIK fabrik = new FABRIK(pose, armature, startJoint, endJoint);
    	fabrik.run(recalculatedPosition, 10);
    	OpenMatrix4f tipRotationMatrix = OpenMatrix4f.fromQuaternion(tipRotation);
    	OpenMatrix4f animRotation = armature.getBoundTransformFor(pose, endJoint).removeTranslation();
    	OpenMatrix4f animToTipRotation = OpenMatrix4f.mul(OpenMatrix4f.invert(animRotation, null), tipRotationMatrix, null);
    	pose.orElseEmpty(endJoint.getName()).overwriteRotation(JointTransform.fromMatrixWithoutScale(animToTipRotation));
	}
}
