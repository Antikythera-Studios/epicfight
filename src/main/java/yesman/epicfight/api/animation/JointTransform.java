package yesman.epicfight.api.animation;

import java.util.Map;

import org.joml.Quaternionf;

import com.google.common.collect.Maps;

import net.minecraft.util.Mth;
import yesman.epicfight.api.utils.math.AnimationTransformEntry;
import yesman.epicfight.api.utils.math.MathUtils;
import yesman.epicfight.api.utils.math.MatrixOperation;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.api.utils.math.Vec3f;

public class JointTransform {
	public static final String ANIMATION_TRANSFORM = "animation_transform";
	public static final String JOINT_LOCAL_TRANSFORM = "joint_local_transform";
	public static final String PARENT = "parent";
	public static final String RESULT1 = "front_result";
	public static final String RESULT2 = "overwrite_rotation";
	
	public static class TransformEntry {
		public MatrixOperation multiplyFunction;
		public final JointTransform transform;

		public TransformEntry(MatrixOperation multiplyFunction, JointTransform transform) {
			this.multiplyFunction = multiplyFunction;
			this.transform = transform;
		}
	}

	private static final ThreadLocal<OpenMatrix4f[]> ENTRY_TMP = ThreadLocal.withInitial(() -> OpenMatrix4f.allocateMatrixArray(3));
	
	private final Map<String, TransformEntry> entries = Maps.newHashMap();
	private final Vec3f translation;
	private final Vec3f scale;
	private final Quaternionf rotation;

	public JointTransform(Vec3f translation, Quaternionf rotation, Vec3f scale) {
		this.translation = translation;
		this.rotation = rotation;
		this.scale = scale;
	}

	public void clearAll() {
		this.clearTransform();
		this.entries.clear();
	}
	
	public Vec3f translation() {
		return this.translation;
	}

	public Quaternionf rotation() {
		return this.rotation;
	}
	
	public Vec3f scale() {
		return this.scale;
	}

	/** Direct access to the auxiliary transform stack
	 *  Package-private so {@link PoseMirror} can rebuild entries with mirrored transforms; the
	 *  rest of the codebase should keep using the {@link #parent(JointTransform, MatrixOperation)}
	 *  / {@link #jointLocal(JointTransform, MatrixOperation)} setters which run merge logic. */
	Map<String, TransformEntry> entries() {
		return this.entries;
	}
	
	public void clearTransform() {
		this.translation.set(0.0F, 0.0F, 0.0F);
		this.rotation.set(0.0F, 0.0F, 0.0F, 1.0F);
		this.scale.set(1.0F, 1.0F, 1.0F);
	}
	
	public JointTransform copy() {
		return JointTransform.empty().copyFrom(this);
	}
	
	public JointTransform copyFrom(JointTransform jt) {
		Vec3f newV = jt.translation();
		Quaternionf newQ = jt.rotation();
		Vec3f newS = jt.scale;
		this.translation.set(newV);
		this.rotation.set(newQ);
		this.scale.set(newS);
		this.entries.putAll(jt.entries);

		return this;
	}

	/**
	 * Deep variant of {@link #copyFrom}: entries are copied into this transform's own
	 * {@link TransformEntry}/{@link JointTransform} objects (reused when present) so the
	 * result shares no entry transforms with {@code jt}.
	 */
	public JointTransform copyFromDeep(JointTransform jt) {
		this.translation.set(jt.translation());
		this.rotation.set(jt.rotation());
		this.scale.set(jt.scale);
		this.entries.keySet().removeIf(key -> !jt.entries.containsKey(key));

		for (Map.Entry<String, TransformEntry> entry : jt.entries.entrySet()) {
			TransformEntry dest = this.entries.get(entry.getKey());
			TransformEntry src = entry.getValue();

			if (dest == null) {
				this.entries.put(entry.getKey(), new TransformEntry(src.multiplyFunction, src.transform.copy()));
			} else {
				dest.multiplyFunction = src.multiplyFunction;
				dest.transform.copyFrom(src.transform);
			}
		}

		return this;
	}
	
	public void jointLocal(JointTransform transform, MatrixOperation multiplyFunction) {
		this.mergeOrPut(JOINT_LOCAL_TRANSFORM, transform, multiplyFunction);
	}

	public void parent(JointTransform transform, MatrixOperation multiplyFunction) {
		this.mergeOrPut(PARENT, transform, multiplyFunction);
	}

	public void animationTransform(JointTransform transform, MatrixOperation multiplyFunction) {
		this.mergeOrPut(ANIMATION_TRANSFORM, transform, multiplyFunction);
	}

	public void frontResult(JointTransform transform, MatrixOperation multiplyFunction) {
		this.mergeOrPut(RESULT1, transform, multiplyFunction);
	}

	public void overwriteRotation(JointTransform transform) {
		this.mergeOrPut(RESULT2, transform, OpenMatrix4f::mul);
	}

	/**
	 * Merges {@code transform} into the named entry (result = existing op applied as
	 * {@code op(new, existing)}) and stores {@code multiplyFunction} as the entry's op,
	 * replicating {@link AnimationTransformEntry#put}. The stored transform is owned by the
	 * entry and rewritten in place so repeated modifier writes stay allocation-free.
	 */
	private void mergeOrPut(String entryName, JointTransform transform, MatrixOperation multiplyFunction) {
		TransformEntry entry = this.entries.get(entryName);

		if (entry == null) {
			this.entries.put(entryName, new TransformEntry(multiplyFunction, transform.copy()));
			return;
		}

		OpenMatrix4f[] tmp = ENTRY_TMP.get();
		transform.toMatrix(tmp[0], tmp[2]);
		entry.transform.toMatrix(tmp[1], tmp[2]);
		JointTransform.fromMatrix(entry.multiplyFunction.mul(tmp[0], tmp[1], tmp[2]), entry.transform);
		entry.multiplyFunction = multiplyFunction;
	}

	public JointTransform mergeIfExist(String entryName, JointTransform transform) {
		if (this.entries.containsKey(entryName)) {
			TransformEntry transformEntry = this.entries.get(entryName);
			return JointTransform.mul(transform, transformEntry.transform, transformEntry.multiplyFunction);
		}

		return transform;
	}

	public OpenMatrix4f getAnimationBoundMatrix(Joint joint, OpenMatrix4f parentTransform) {
		return this.getAnimationBoundMatrix(joint, parentTransform, new OpenMatrix4f(), new OpenMatrix4f(), new OpenMatrix4f(), new OpenMatrix4f());
	}

	/**
	 * Computes the animation-bound matrix for {@code joint} directly into {@code dest}
	 * (scratch {@code tmp1}..{@code tmp3} must not alias {@code dest} or {@code parentTransform}).
	 * Equivalent to running an {@link AnimationTransformEntry}: the result is
	 * PARENT * JOINT_LOCAL_TRANSFORM * ANIMATION_TRANSFORM * RESULT1 * RESULT2, where the
	 * PARENT/JOINT_LOCAL_TRANSFORM/ANIMATION_TRANSFORM slots merge any same-named entry
	 * transform ahead of the runtime matrix via the entry's stored operation.
	 */
	public OpenMatrix4f getAnimationBoundMatrix(Joint joint, OpenMatrix4f parentTransform, OpenMatrix4f dest, OpenMatrix4f tmp1, OpenMatrix4f tmp2, OpenMatrix4f tmp3) {
		TransformEntry entry = this.entries.get(PARENT);

		if (entry == null) {
			dest.load(parentTransform);
		} else {
			entry.transform.toMatrix(tmp1, tmp3);
			entry.multiplyFunction.mul(tmp1, parentTransform, dest);
		}

		entry = this.entries.get(JOINT_LOCAL_TRANSFORM);

		if (entry == null) {
			OpenMatrix4f.mul(dest, joint.getLocalTransform(), dest);
		} else {
			entry.transform.toMatrix(tmp1, tmp3);
			entry.multiplyFunction.mul(tmp1, joint.getLocalTransform(), tmp2);
			OpenMatrix4f.mul(dest, tmp2, dest);
		}

		entry = this.entries.get(ANIMATION_TRANSFORM);
		this.toMatrix(tmp1, tmp3);

		if (entry == null) {
			OpenMatrix4f.mul(dest, tmp1, dest);
		} else {
			entry.transform.toMatrix(tmp2, tmp3);
			entry.multiplyFunction.mul(tmp2, tmp1, tmp2);
			OpenMatrix4f.mul(dest, tmp2, dest);
		}

		entry = this.entries.get(RESULT1);

		if (entry != null) {
			entry.transform.toMatrix(tmp1, tmp3);
			entry.multiplyFunction.mul(dest, tmp1, dest);
		}

		entry = this.entries.get(RESULT2);

		if (entry != null) {
			entry.transform.toMatrix(tmp1, tmp3);
			entry.multiplyFunction.mul(dest, tmp1, dest);
		}

		return dest;
	}

	public OpenMatrix4f toMatrix() {
		return new OpenMatrix4f().translate(this.translation).mulBack(OpenMatrix4f.fromQuaternion(this.rotation)).scale(this.scale);
	}

	/** Writes {@link #toMatrix()} into {@code dest}; {@code rotScratch} is used as the rotation
	 *  basis matrix and must not alias {@code dest}. */
	public OpenMatrix4f toMatrix(OpenMatrix4f dest, OpenMatrix4f rotScratch) {
		return dest.setIdentity().translate(this.translation).mulBack(OpenMatrix4f.fromQuaternion(this.rotation, rotScratch)).scale(this.scale);
	}
	
	@Override
	public String toString() {
		return String.format("translation:%s, rotation:%s, scale:%s %d entries ", this.translation, this.rotation, this.scale, this.entries.size());
	}
	
	public static JointTransform interpolateTransform(JointTransform prev, JointTransform next, float progression, JointTransform dest) {
		if (dest == null) {
			dest = JointTransform.empty();
		}
		
		MathUtils.lerpVector(prev.translation, next.translation, progression, dest.translation);
		MathUtils.lerpQuaternion(prev.rotation, next.rotation, progression, dest.rotation);
		MathUtils.lerpVector(prev.scale, next.scale, progression, dest.scale);
		
		return dest;
	}
	
	public static JointTransform interpolate(JointTransform prev, JointTransform next, float progression) {
		return interpolate(prev, next, progression, null);
	}
	
	public static JointTransform interpolate(JointTransform prev, JointTransform next, float progression, JointTransform dest) {
		if (dest == null) {
			dest = JointTransform.empty();
		}
		
		if (prev == null || next == null) {
			dest.clearTransform();
			return dest;
		}
		
		progression = Mth.clamp(progression, 0.0F, 1.0F);
		interpolateTransform(prev, next, progression, dest);

		dest.entries.keySet().removeIf(key -> !prev.entries.containsKey(key) && !next.entries.containsKey(key));

		for (Map.Entry<String, TransformEntry> entry : prev.entries.entrySet()) {
			JointTransform transform = next.entries.containsKey(entry.getKey()) ? next.entries.get(entry.getKey()).transform : JointTransform.empty();
			TransformEntry destEntry = dest.entries.get(entry.getKey());

			if (destEntry == null) {
				dest.entries.put(entry.getKey(), new TransformEntry(entry.getValue().multiplyFunction, interpolateTransform(entry.getValue().transform, transform, progression, JointTransform.empty())));
			} else {
				destEntry.multiplyFunction = entry.getValue().multiplyFunction;
				interpolateTransform(entry.getValue().transform, transform, progression, destEntry.transform);
			}
		}

		for (Map.Entry<String, TransformEntry> entry : next.entries.entrySet()) {
			if (!prev.entries.containsKey(entry.getKey())) {
				TransformEntry destEntry = dest.entries.get(entry.getKey());

				if (destEntry == null) {
					dest.entries.put(entry.getKey(), new TransformEntry(entry.getValue().multiplyFunction, interpolateTransform(JointTransform.empty(), entry.getValue().transform, progression, JointTransform.empty())));
				} else {
					destEntry.multiplyFunction = entry.getValue().multiplyFunction;
					interpolateTransform(JointTransform.empty(), entry.getValue().transform, progression, destEntry.transform);
				}
			}
		}

		return dest;
	}
	
	public static JointTransform fromMatrixWithoutScale(OpenMatrix4f matrix) {
		return new JointTransform(matrix.toTranslationVector(), matrix.toQuaternion(), new Vec3f(1.0F, 1.0F, 1.0F));
	}
	
	public static JointTransform translation(Vec3f vec) {
		return JointTransform.translationRotation(vec, new Quaternionf(0.0F, 0.0F, 0.0F, 1.0F));
	}
	
	public static JointTransform rotation(Quaternionf quat) {
		return JointTransform.translationRotation(new Vec3f(0.0F, 0.0F, 0.0F), quat);
	}
	
	public static JointTransform scale(Vec3f vec) {
		return new JointTransform(new Vec3f(0.0F, 0.0F, 0.0F), new Quaternionf(0.0F, 0.0F, 0.0F, 1.0F), vec);
	}
	
	public static JointTransform fromMatrix(OpenMatrix4f matrix) {
		return new JointTransform(matrix.toTranslationVector(), matrix.toQuaternion(), matrix.toScaleVector());
	}

	/** Decomposes {@code matrix} into {@code dest} (TRS in-place, entries cleared). */
	public static JointTransform fromMatrix(OpenMatrix4f matrix, JointTransform dest) {
		matrix.toTranslationVector(dest.translation);
		matrix.toQuaternion(dest.rotation);
		matrix.toScaleVector(dest.scale);
		dest.entries.clear();
		return dest;
	}
	
	public static JointTransform translationRotation(Vec3f vec, Quaternionf quat) {
		return new JointTransform(vec, quat, new Vec3f(1.0F, 1.0F, 1.0F));
	}
	
	public static JointTransform mul(JointTransform left, JointTransform right, MatrixOperation operation) {
		return JointTransform.fromMatrix(operation.mul(left.toMatrix(), right.toMatrix(), null));
	}
	
	public static JointTransform fromPrimitives(float locX, float locY, float locZ, float quatX, float quatY, float quatZ, float quatW, float scaX, float scaY, float scaZ) {
		return new JointTransform(new Vec3f(locX, locY, locZ), new Quaternionf(quatX, quatY, quatZ, quatW), new Vec3f(scaX, scaY, scaZ));
	}
	
	public static JointTransform empty() {
		return new JointTransform(new Vec3f(0.0F, 0.0F, 0.0F), new Quaternionf(0.0F, 0.0F, 0.0F, 1.0F), new Vec3f(1.0F, 1.0F, 1.0F));
	}
}