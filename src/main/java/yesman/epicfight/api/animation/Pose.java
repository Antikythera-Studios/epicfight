package yesman.epicfight.api.animation;

import java.util.Map;
import java.util.function.BiConsumer;

import com.google.common.base.Predicate;
import com.google.common.collect.Maps;

public class Pose {
	public static final Pose EMPTY_POSE = new Pose();
	
	public static Pose interpolatePose(Pose pose1, Pose pose2, float pregression) {
		return interpolatePose(pose1, pose2, pregression, new Pose());
	}

	/**
	 * Interpolates {@code pose1} and {@code pose2} into {@code dest}, reusing the joint
	 * transforms already stored in {@code dest} so repeated calls stay allocation-free.
	 * Joints present in {@code dest} but absent from both inputs are dropped.
	 */
	public static Pose interpolatePose(Pose pose1, Pose pose2, float pregression, Pose dest) {
		dest.jointTransformData.keySet().removeIf(name -> !pose1.hasTransform(name) && !pose2.hasTransform(name));

		JointTransform empty = JointTransform.empty();

		for (Map.Entry<String, JointTransform> entry : pose1.jointTransformData.entrySet()) {
			JointTransform.interpolate(entry.getValue(), pose2.orElse(entry.getKey(), empty), pregression, dest.getOrCreateJointTransform(entry.getKey()));
		}

		for (Map.Entry<String, JointTransform> entry : pose2.jointTransformData.entrySet()) {
			if (!pose1.hasTransform(entry.getKey())) {
				JointTransform.interpolate(empty, entry.getValue(), pregression, dest.getOrCreateJointTransform(entry.getKey()));
			}
		}

		return dest;
	}

	protected final Map<String, JointTransform> jointTransformData;
	
	public Pose() {
		this(Maps.newHashMap());
	}
	
	public Pose(Map<String, JointTransform> jointTransforms) {
		this.jointTransformData = jointTransforms;
	}
	
	public void putJointData(String name, JointTransform transform) {
		this.jointTransformData.put(name, transform);
	}

	/** Returns the stored transform for {@code name}, creating and storing an empty one if absent.
	 *  Lets dest-posed evaluations reuse {@link JointTransform} instances across frames. */
	public JointTransform getOrCreateJointTransform(String name) {
		return this.jointTransformData.computeIfAbsent(name, key -> JointTransform.empty());
	}

	/**
	 * OVERWRITE-deep load: every joint from {@code src} is copied into this pose's own
	 * {@link JointTransform} objects (created when missing), so the result shares no
	 * transform instances with {@code src}. Joints absent from {@code src} keep their data.
	 */
	public Pose loadDeep(Pose src) {
		src.forEachEnabledTransforms((name, transform) -> this.getOrCreateJointTransform(name).copyFromDeep(transform));
		return this;
	}

	/** Deep copy: a new pose whose joint transforms share no instances with this pose. */
	public Pose copy() {
		return new Pose().loadDeep(this);
	}
	
	public Map<String, JointTransform> getJointTransformData() {
		return this.jointTransformData;
	}
	
	public void disableJoint(Predicate<? super Map.Entry<String, JointTransform>> predicate) {
		this.jointTransformData.entrySet().removeIf(predicate);
	}

	/** Drops every joint not listed in {@code names}; pooled poses are trimmed to the
	 *  active joint set before being refilled so stale transforms never leak through. */
	public void retainJoints(java.util.Set<String> names) {
		this.jointTransformData.keySet().retainAll(names);
	}
	
	public void disableAllJoints() {
		this.jointTransformData.clear();
	}
	
	public boolean hasTransform(String jointName) {
		return this.jointTransformData.containsKey(jointName);
	}
	
	public JointTransform get(String jointName) {
		return this.jointTransformData.get(jointName);
	}
	
	public JointTransform orElseEmpty(String jointName) {
		return this.jointTransformData.getOrDefault(jointName, JointTransform.empty());
	}
	
	public JointTransform orElse(String jointName, JointTransform orElse) {
		return this.jointTransformData.getOrDefault(jointName, orElse);
	}
	
	public void forEachEnabledTransforms(BiConsumer<String, JointTransform> task) {
		this.jointTransformData.forEach(task);
	}
	
	public void load(Pose pose, LoadOperation operation) {
		switch (operation) {
		case SET -> {
			this.disableAllJoints();
			pose.forEachEnabledTransforms(this::putJointData);
		}
		case OVERWRITE -> {
			pose.forEachEnabledTransforms(this::putJointData);
		}
		case APPEND_ABSENT -> {
			pose.forEachEnabledTransforms((name, transform) -> {
				if (!this.hasTransform(name)) {
					this.putJointData(name, transform);
				}
			});
		}
		}
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Pose: ");
		
		for (Map.Entry<String, JointTransform> entry : this.jointTransformData.entrySet()) {
			sb.append(String.format("%s{%s, %s}, ", entry.getKey(), entry.getValue().translation().toString(), entry.getValue().rotation().toString()) + "\n");
		}
		
		return sb.toString();
	}
	
	public enum LoadOperation {
		SET, OVERWRITE, APPEND_ABSENT
	}
}