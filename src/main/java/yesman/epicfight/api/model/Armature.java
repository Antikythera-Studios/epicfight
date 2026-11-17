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
	/**
	 * Per-thread scratch matrices shared by the pose/matrix evaluation entry points.
	 * Slot layout: [0..2] temporary matrices for {@link JointTransform#getAnimationBoundMatrix},
	 * [3] identity/parent seed, [4,5] alternating buffers for bound-transform walks,
	 * [6,7] spare caller-space temps (never touched by the internal eval paths — safe
	 * to use as {@code dest} for {@link #getBoundTransformFor(Pose, Joint, OpenMatrix4f)}).
	 * Armatures may be shared between render and tick threads, so the arena is thread-local.
	 */
	private static final ThreadLocal<OpenMatrix4f[]> EVAL_SCRATCH = ThreadLocal.withInitial(() -> OpenMatrix4f.allocateMatrixArray(8));

	public static OpenMatrix4f[] evalScratch() {
		return EVAL_SCRATCH.get();
	}

	private final String name;
	private final Int2ObjectMap<Joint> jointById;
	private final Map<String, Joint> jointByName;
	private final Map<String, Joint.HierarchicalJointAccessor> pathIndexMap;
	private final int jointCount;
	private final OpenMatrix4f[] poseMatrices;
	private java.util.List<Joint> jointsInDFSOrder;
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
		OpenMatrix4f[] scratch = EVAL_SCRATCH.get();
		this.getPoseTransform(this.rootJoint, scratch[3].setIdentity(), pose, this.poseMatrices, false);
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
		return this.getPoseAsTransformMatrix(pose, applyOriginTransform, OpenMatrix4f.allocateMatrixArray(this.jointCount));
	}

	/**
	 * {@link #getPoseAsTransformMatrix(Pose, boolean)} writing into the elements of
	 * {@code jointMatrices} (must be a fully-allocated array, e.g. from
	 * {@link OpenMatrix4f#allocateMatrixArray}).
	 */
	public OpenMatrix4f[] getPoseAsTransformMatrix(Pose pose, boolean applyOriginTransform, OpenMatrix4f[] jointMatrices) {
		OpenMatrix4f[] scratch = EVAL_SCRATCH.get();
		this.getPoseTransform(this.rootJoint, scratch[3].setIdentity(), pose, jointMatrices, applyOriginTransform);
		return jointMatrices;
	}

	private void getPoseTransform(Joint joint, OpenMatrix4f parentTransform, Pose pose, OpenMatrix4f[] jointMatrices, boolean applyOriginTransform) {
		OpenMatrix4f[] scratch = EVAL_SCRATCH.get();
		OpenMatrix4f result = jointMatrices[joint.getId()];
		pose.orElseEmpty(joint.getName()).getAnimationBoundMatrix(joint, parentTransform, result, scratch[0], scratch[1], scratch[2]);

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

	/** {@link #getBoundTransformFor(Pose, Joint)} writing into {@code dest}. */
	public OpenMatrix4f getBoundTransformFor(Pose pose, Joint joint, OpenMatrix4f dest) {
		return this.getBoundTransformByJointIndex(pose, this.searchPathIndex(joint.getName()).createAccessTicket(this.rootJoint), dest);
	}

	public OpenMatrix4f getBoundTransformByJointIndex(Pose pose, Joint.AccessTicket pathIndices) {
		return this.getBoundTransformByJointIndex(pose, pathIndices, new OpenMatrix4f());
	}

	public OpenMatrix4f getBoundTransformByJointIndex(Pose pose, Joint.AccessTicket pathIndices, OpenMatrix4f dest) {
		OpenMatrix4f[] scratch = EVAL_SCRATCH.get();
		OpenMatrix4f current = scratch[4];
		OpenMatrix4f next = scratch[5];
		Joint joint = this.rootJoint;

		pose.orElseEmpty(joint.getName()).getAnimationBoundMatrix(joint, scratch[3].setIdentity(), current, scratch[0], scratch[1], scratch[2]);

		while (pathIndices.hasNext()) {
			joint = pathIndices.next();
			pose.orElseEmpty(joint.getName()).getAnimationBoundMatrix(joint, current, next, scratch[0], scratch[1], scratch[2]);
			OpenMatrix4f swap = current;
			current = next;
			next = swap;
		}

		return dest.load(current);
	}

	/**
	 * Joints in pre-order depth-first order; fixed at armature construction, so it is
	 * computed once and reused (e.g. by {@code ClientAnimator#applyBindModifier}).
	 */
	public java.util.List<Joint> getJointsInDFSOrder() {
		if (this.jointsInDFSOrder == null) {
			java.util.List<Joint> joints = new java.util.ArrayList<>(this.jointCount);
			this.gatherDFS(this.rootJoint, joints);
			this.jointsInDFSOrder = java.util.Collections.unmodifiableList(joints);
		}

		return this.jointsInDFSOrder;
	}

	private void gatherDFS(Joint joint, java.util.List<Joint> out) {
		out.add(joint);

		for (Joint sub : joint.getSubJoints()) {
			this.gatherDFS(sub, out);
		}
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
