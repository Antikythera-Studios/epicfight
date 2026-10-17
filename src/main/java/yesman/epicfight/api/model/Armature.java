package yesman.epicfight.api.model;

import yesman.epicfight.EpicFight;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import com.google.common.collect.Maps;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import yesman.epicfight.api.animation.Joint;
import yesman.epicfight.api.animation.JointTransform;
import yesman.epicfight.api.animation.Pose;
import yesman.epicfight.api.asset.JsonAssetLoader;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.main.EpicFightMod;
import yesman.epicfight.main.EpicFightSharedConstants;

public class Armature {
	private final String name;
	private final Int2ObjectMap<Joint> jointById;
	private final Map<String, Joint> jointByName;
	private final Map<String, Joint.HierarchicalJointAccessor> pathIndexMap;
	private final int jointCount;
	private final OpenMatrix4f[] poseMatrices;
	public final Joint rootJoint;
	
	public Armature(String name, int jointNumber, Joint rootJoint, Map<String, Joint> jointMap) {
		this.name = name;
		this.jointCount = jointNumber;
		this.rootJoint = rootJoint;
		this.jointByName = jointMap;
		this.jointById = new Int2ObjectOpenHashMap<>();
		this.pathIndexMap = new HashMap<> ();
		
		this.jointByName.values().forEach((joint) -> {
			this.jointById.put(joint.getId(), joint);
		});
		
		this.poseMatrices = OpenMatrix4f.allocateMatrixArray(this.jointCount);
	}
	
	protected Joint getOrLogException(Map<String, Joint> jointMap, String name) {
		if (!jointMap.containsKey(name)) {
			if (EpicFightSharedConstants.IS_DEV_ENV) {
				EpicFight.LOGGER.debug("Cannot find the joint named " + name + " in " + this.getClass().getCanonicalName());
			}
			
			return Joint.EMPTY;
		}
		
		return jointMap.get(name);
	}
	
	public void setPose(Pose pose) {
		this.getPoseTransform(this.rootJoint, new OpenMatrix4f(), pose, this.poseMatrices, false);
	}
	
	public void bakeOriginMatrices() {
		this.rootJoint.initOriginTransform(new OpenMatrix4f());
	}
	
	public OpenMatrix4f[] getPoseMatrices() {
		return this.poseMatrices;
	}
	
	/**
	 * @param applyOriginTransform if you need a final pose of the animations, give it false.
	 */
	public OpenMatrix4f[] getPoseAsTransformMatrix(Pose pose, boolean applyOriginTransform) {
		OpenMatrix4f[] jointMatrices = new OpenMatrix4f[this.jointCount];
		this.getPoseTransform(this.rootJoint, new OpenMatrix4f(), pose, jointMatrices, applyOriginTransform);
		return jointMatrices;
	}
	
	private void getPoseTransform(Joint joint, OpenMatrix4f parentTransform, Pose pose, OpenMatrix4f[] jointMatrices, boolean applyOriginTransform) {
		OpenMatrix4f result = pose.orElseEmpty(joint.getName()).getAnimationBoundMatrix(joint, parentTransform);
		jointMatrices[joint.getId()] = result;
		
		for (Joint joints : joint.getSubJoints()) {
			this.getPoseTransform(joints, result, pose, jointMatrices, applyOriginTransform);
		}
		
		if (applyOriginTransform) {
			result.mulBack(joint.getToOrigin());
		}
	}
	
	/**
	 * Inapposite past perfect
	 */
	@Deprecated(forRemoval = true, since = "1.21.1")
	public OpenMatrix4f getBindedTransformFor(Pose pose, Joint joint) {
		return this.getBoundTransformByJointIndex(pose, this.searchPathIndex(joint.getName()).createAccessTicket(this.rootJoint));
	}
	
	public OpenMatrix4f getBoundTransformFor(Pose pose, Joint joint) {
		return this.getBoundTransformByJointIndex(pose, this.searchPathIndex(joint.getName()).createAccessTicket(this.rootJoint));
	}
	
	public OpenMatrix4f getBoundTransformByJointIndex(Pose pose, Joint.AccessTicket pathIndices) {
		return this.getBoundJointTransformRecursively(pose, this.rootJoint, new OpenMatrix4f(), pathIndices);
	}
	
	private OpenMatrix4f getBoundJointTransformRecursively(Pose pose, Joint joint, OpenMatrix4f parentTransform, Joint.AccessTicket pathIndices) {
		JointTransform jt = pose.orElseEmpty(joint.getName());
		OpenMatrix4f result = jt.getAnimationBoundMatrix(joint, parentTransform);
		
		return pathIndices.hasNext() ? this.getBoundJointTransformRecursively(pose, pathIndices.next(), result, pathIndices) : result;
	}
	
	public boolean hasJoint(String name) {
		return this.jointByName.containsKey(name);
	}
	
	public Joint searchJointById(int id) {
		return this.jointById.get(id);
	}
	
	public Joint searchJointByName(String name) {
		return this.jointByName.get(name);
	}
	
	/**
	 * Search and record joint path from root to terminal
	 * 
	 * @param terminalJointName
	 * @return
	 */
	public Joint.HierarchicalJointAccessor searchPathIndex(String terminalJointName) {
		return this.searchPathIndex(this.rootJoint, terminalJointName);
	}
	
	/**
	 * Search and record joint path to terminal
	 * 
	 * @param start
	 * @param terminalJointName
	 * @return
	 */
	public Joint.HierarchicalJointAccessor searchPathIndex(Joint start, String terminalJointName) {
		String signature = start.getName() + "-" + terminalJointName;
		
		if (this.pathIndexMap.containsKey(signature)) {
			return this.pathIndexMap.get(signature);
		} else {
			Joint.HierarchicalJointAccessor.Builder pathBuilder = start.searchPath(Joint.HierarchicalJointAccessor.builder(), terminalJointName);
			Joint.HierarchicalJointAccessor accessor;
			
			if (pathBuilder == null) {
				throw new IllegalArgumentException("Failed to get joint path index for " + terminalJointName);
			} else {
				accessor = pathBuilder.build();
				this.pathIndexMap.put(signature, accessor);
			}
			
			return accessor;
		}
	}
	
	public void gatherAllJointsInPathToTerminal(String terminalJointName, Collection<String> jointsInPath) {
		if (!this.jointByName.containsKey(terminalJointName)) {
			throw new NoSuchElementException("No " + terminalJointName + " joint in this armature!");
		}
		
		Joint.HierarchicalJointAccessor pathIndices = this.searchPathIndex(terminalJointName);
		Joint.AccessTicket accessTicket = pathIndices.createAccessTicket(this.rootJoint);
		
		Joint joint = this.rootJoint;
		jointsInPath.add(joint.getName());
		
		while (accessTicket.hasNext()) {
			jointsInPath.add(accessTicket.next().getName());
		}
	}
	
	public int getJointNumber() {
		return this.jointCount;
	}
	
	@Override
	public String toString() {
		return this.name;
	}
	
	public Armature deepCopy() {
		Map<String, Joint> oldToNewJoint = Maps.newHashMap();
		oldToNewJoint.put("empty", Joint.EMPTY);
		
		Joint newRoot = this.copyHierarchy(this.rootJoint, oldToNewJoint);
		newRoot.initOriginTransform(new OpenMatrix4f());
		Armature newArmature = null;
		
		// Uses reflection to keep the type of copied armature
		try {
			Constructor<? extends Armature> constructor = this.getClass().getConstructor(String.class, int.class, Joint.class, Map.class);
			newArmature = constructor.newInstance(this.name, this.jointCount, newRoot, oldToNewJoint);
		} catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new IllegalStateException("Armature copy failed! " + e);
		}
		
		return newArmature;
	}
	
	private Joint copyHierarchy(Joint joint, Map<String, Joint> oldToNewJoint) {
		if (joint == Joint.EMPTY) {
			return Joint.EMPTY;
		}
		
		Joint newJoint = new Joint(joint.getName(), joint.getId(), joint.getLocalTransform());
		oldToNewJoint.put(joint.getName(), newJoint);
		
		for (Joint subJoint : joint.getSubJoints()) {
			newJoint.addSubJoints(this.copyHierarchy(subJoint, oldToNewJoint));
		}
		
		return newJoint;
	}
	
	public JsonObject toJsonObject() {
		JsonObject root = new JsonObject();
		JsonObject armature = new JsonObject();
		
		JsonArray jointNamesArray = new JsonArray();
		JsonArray jointHierarchy = new JsonArray();
		
		this.jointById.int2ObjectEntrySet().stream().sorted((entry1, entry2) -> Integer.compare(entry1.getIntKey(), entry2.getIntKey())).forEach((entry) -> jointNamesArray.add(entry.getValue().getName()));
		armature.add("joints", jointNamesArray);
		armature.add("hierarchy", jointHierarchy);
		
		exportJoint(jointHierarchy, this.rootJoint, true);
		
		root.add("armature", armature);
		
		return root;
	}
	
	private static void exportJoint(JsonArray parent, Joint joint, boolean root) {
		JsonObject jointJson = new JsonObject();
		jointJson.addProperty("identifier", joint.getName());
		
		JsonArray transformMatrix = new JsonArray();
		OpenMatrix4f localMatrixInBlender = new OpenMatrix4f(joint.getLocalTransform());
		
		if (root) {
			localMatrixInBlender.mulFront(OpenMatrix4f.invert(JsonAssetLoader.BLENDER_TO_MINECRAFT_COORD, null));
		}
		
		localMatrixInBlender.transpose();
		localMatrixInBlender.toList().forEach(transformMatrix::add);
		jointJson.add("transform", transformMatrix);
		parent.add(jointJson);
		
		if (!joint.getSubJoints().isEmpty()) {
			JsonArray children = new JsonArray();
			jointJson.add("children", children);
			joint.getSubJoints().forEach((joint$2) -> exportJoint(children, joint$2, false));
		}
	}
}
