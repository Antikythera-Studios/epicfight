package yesman.epicfight.api.animation;

import net.minecraft.util.Mth;
import org.apache.commons.lang3.mutable.MutableInt;

import java.util.*;

public class AnimationClip {
	public static final AnimationClip EMPTY_CLIP = new AnimationClip();
	
	protected Map<String, TransformSheet> jointTransforms = new HashMap<> ();
	protected float clipTime;
	protected float[] bakedTimes;

    /// To modify existing keyframes in runtime and keep the baked state, call [#setBaked] again
    /// after finishing clip modification. (Frequent calls of this method will cause a performance issue)
	public void addJointTransform(String jointName, TransformSheet sheet) {
		this.jointTransforms.put(jointName, sheet);
		this.bakedTimes = null;
	}
	
	public boolean hasJointTransform(String jointName) {
		return this.jointTransforms.containsKey(jointName);
	}

    /// Bakes all keyframes to optimize calculating current pose,
	public void bakeKeyframes() {
		Set<Float> timestamps = new HashSet<> ();
		
		this.jointTransforms.values().forEach(transformSheet -> {
			transformSheet.forEach((i, keyframe) -> {
				timestamps.add(keyframe.time());
			});
		});
		
		float[] bakedTimestamps = new float[timestamps.size()];
		MutableInt mi = new MutableInt(0);
		
		timestamps.stream().sorted().toList().forEach(f -> {
			bakedTimestamps[mi.getAndAdd(1)] = f;
		});
		
		Map<String, TransformSheet> bakedJointTransforms = new HashMap<> ();
		
		this.jointTransforms.forEach((jointName, transformSheet) -> {
			bakedJointTransforms.put(jointName, transformSheet.createInterpolated(bakedTimestamps));
		});
		
		this.jointTransforms = bakedJointTransforms;
		this.bakedTimes = bakedTimestamps;
	}
	
	/// Bake keyframes supposing all keyframes are aligned (mainly used when creating link animations)
	public void setBaked() {
		TransformSheet transformSheet = this.jointTransforms.get("Root");
		
		if (transformSheet != null) {
			this.bakedTimes = new float[transformSheet.getKeyframes().length];
			
			for (int i = 0; i < transformSheet.getKeyframes().length; i++) {
				this.bakedTimes[i] = transformSheet.getKeyframes()[i].time();
			}
		}
	}
	
	public TransformSheet getJointTransform(String jointName) {
		return this.jointTransforms.get(jointName);
	}
	
	public final Pose getPoseInTime(float time) {
		Pose pose = new Pose();

        if (time < 0.0F) {
            time = this.clipTime + time;
        }

		if (Float.isNaN(time)) {
			time = 0.0F;
		}

		if (this.bakedTimes != null && this.bakedTimes.length > 0) {
			// Binary search; each iteration must shrink the interval or the loop cannot end.
			int begin = 0, end = this.bakedTimes.length - 1;
			
			while (end - begin > 1) {
				int mid = begin + (end - begin) / 2;
				
				if (this.bakedTimes[mid] > time) {
					end = mid;
				} else {
					begin = mid;
				}
			}
			
			float span = this.bakedTimes[end] - this.bakedTimes[begin];
			float delta = span <= 0.0F || !Float.isFinite(span) ? 0.0F
				: Mth.clamp((time - this.bakedTimes[begin]) / span, 0.0F, 1.0F);
			TransformSheet.InterpolationInfo iInfo = new TransformSheet.InterpolationInfo(begin, end, delta);
			
			for (String jointName : this.jointTransforms.keySet()) {
				pose.putJointData(jointName, this.jointTransforms.get(jointName).getInterpolatedTransform(iInfo));
			}
		} else {
			for (String jointName : this.jointTransforms.keySet()) {
				pose.putJointData(jointName, this.jointTransforms.get(jointName).getInterpolatedTransform(time));
			}
		}
		
		return pose;
	}
	
    /// @return returns protected keyframes of each joint to keep the baked state of keyframes.
	public Map<String, TransformSheet> getJointTransforms() {
		return Collections.unmodifiableMap(this.jointTransforms);
	}
	
	public void reset() {
		this.jointTransforms.clear();
		this.bakedTimes = null;
	}
	
	public void setClipTime(float clipTime) {
		this.clipTime = clipTime;
	}
	
	public float getClipTime() {
		return this.clipTime;
	}
}