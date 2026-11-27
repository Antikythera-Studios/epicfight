package yesman.epicfight.api.utils.math;

import java.util.Collection;
import java.util.List;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import net.minecraft.world.phys.Vec3;
import yesman.epicfight.EpicFight;

public class Vec3f extends Vec2f {
	public static final Vec3f X_AXIS = new Vec3f(1.0F, 0.0F, 0.0F);
	public static final Vec3f Y_AXIS = new Vec3f(0.0F, 1.0F, 0.0F);
	public static final Vec3f Z_AXIS = new Vec3f(0.0F, 0.0F, 1.0F);
	public static final Vec3f M_X_AXIS = new Vec3f(-1.0F, 0.0F, 0.0F);
	public static final Vec3f M_Y_AXIS = new Vec3f(0.0F, -1.0F, 0.0F);
	public static final Vec3f M_Z_AXIS = new Vec3f(0.0F, 0.0F, -1.0F);
	public static final Vec3f ZERO = new Vec3f(0.0F, 0.0F, 0.0F);
	
	public float z;
	
	public Vec3f() {
		super();
		this.z = 0;
	}
	
	public Vec3f(float x, float y, float z) {
		super(x, y);
		this.z = z;
	}
	
	public Vec3f(double x, double y, double z) {
		this((float)x, (float)y, (float)z);
	}
	
	public Vec3f(Vec3 mojangVec) {
		this((float)mojangVec.x, (float)mojangVec.y, (float)mojangVec.z);
	}
	
	public Vec3f set(float x, float y, float z) {
		this.x = x;
		this.y = y;
		this.z = z;
		return this;
	}
	
	public Vec3f set(Vec3 vec3f) {
		this.x = (float)vec3f.x;
		this.y = (float)vec3f.y;
		this.z = (float)vec3f.z;
		return this;
	}
	
	public Vec3f set(Vec3f vec3f) {
		this.x = vec3f.x;
		this.y = vec3f.y;
		this.z = vec3f.z;
		return this;
	}
	
	public Vec3f add(float x, float y, float z) {
		this.x += x;
		this.y += y;
		this.z += z;
		return this;
	}
	
	public Vec3f add(Vec3f vec) {
		return this.add(vec.x, vec.y, vec.z);
	}
	
	public Vec3f add(Vec3 vec) {
		return this.add((float)vec.x, (float)vec.y, (float)vec.z);
	}
	
	public Vec3f sub(float x, float y, float z) {
		this.x -= x;
		this.y -= y;
		this.z -= z;
		return this;
	}
	
	public Vec3f sub(Vec3f vec) {
		return this.sub(vec.x, vec.y, vec.z);
	}
	
	public static Vec3f add(Vec3f left, Vec3f right, Vec3f dest) {
		if (dest == null) {
			return new Vec3f(left.x + right.x, left.y + right.y, left.z + right.z);
		} else {
			dest.set(left.x + right.x, left.y + right.y, left.z + right.z);
			return dest;
		}
	}
	
	public static Vec3f sub(Vec3f left, Vec3f right, Vec3f dest) {
		if (dest == null) {
			return new Vec3f(left.x - right.x, left.y - right.y, left.z - right.z);
		} else {
			dest.set(left.x - right.x, left.y - right.y, left.z - right.z);
			return dest;
		}
	}
	
	public Vec3f multiply(Vec3f vec) {
		return multiply(this, this, vec.x, vec.y, vec.z);
	}
	
	public Vec3f multiply(float x, float y, float z) {
		return multiply(this, this, x, y, z);
	}
	
	public static Vec3f multiply(Vec3f src, Vec3f dest, float x, float y, float z) {
		if (dest == null) {
			dest = new Vec3f();
		}
		
		dest.x = src.x * x;
		dest.y = src.y * y;
		dest.z = src.z * z;
		
		return dest;
	}
	
	@Override
	public Vec3f scale(float f) {
		return scale(this, this, f);
	}
	
	public static Vec3f scale(Vec3f src, Vec3f dest, float f) {
		if (dest == null) {
			dest = new Vec3f();
		}
		
		dest.x = src.x * f;
		dest.y = src.y * f;
		dest.z = src.z * f;
		
		return dest;
	}
	
	public Vec3f copy() {
		return new Vec3f(this.x, this.y, this.z);
	}
	
	public float length() {
		return (float) Math.sqrt(this.lengthSqr());
	}
	
	public float lengthSqr() {
		return this.x * this.x + this.y * this.y + this.z * this.z;
	}
	
	public float distance(Vec3f opponent) {
		return (float)Math.sqrt(this.distanceSqr(opponent));
	}
	
	public float distanceSqr(Vec3f opponent) {
		return (float)(Math.pow(this.x - opponent.x, 2) + Math.pow(this.y - opponent.y, 2) + Math.pow(this.z - opponent.z, 2));
	}
	
	public float horizontalDistance() {
		return (float)Math.sqrt(this.x * this.x + this.z * this.z);
	}

	public float horizontalDistanceSqr() {
		return this.x * this.x + this.z * this.z;
	}

	public void rotate(float degree, Vec3f axis) {
		rotate(degree, axis, this, this);
	}
	
	public void invalidate() {
		this.x = Float.NaN;
		this.y = Float.NaN;
		this.z = Float.NaN;
	}
	
	public boolean validateValues() {
		return Float.isFinite(this.x) && Float.isFinite(this.y) && Float.isFinite(this.z);
	}
	
	public static Vec3f rotate(float degree, Vec3f axis, Vec3f src, Vec3f dest) {
		if (dest == null) {
			dest = new Vec3f();
		}
		
		return OpenMatrix4f.transform3v(OpenMatrix4f.createRotatorDeg(degree, axis), src, dest);
	}
	
	private static final ThreadLocal<Vector3f> SRC = ThreadLocal.withInitial(Vector3f::new);
	private static final ThreadLocal<Vector3f> TRANSFORM_RESULT = ThreadLocal.withInitial(Vector3f::new);
	
	public static Vec3f rotate(Quaternionf rot, Vec3f src, Vec3f dest) {
		if (dest == null) {
			dest = new Vec3f();
		}
		
		Vector3f srcVec = SRC.get();
		Vector3f result = TRANSFORM_RESULT.get();
		srcVec.set(src.x, src.y, src.z);
		rot.transform(srcVec, result);
		dest.set(result.x, result.y, result.z);
		
		return dest;
	}
	
	public static float dot(Vec3f left, Vec3f right) {
		return left.x * right.x + left.y * right.y + left.z * right.z;
	}
	
	public static Vec3f cross(Vec3f left, Vec3f right, Vec3f dest) {
		if (dest == null) {
			dest = new Vec3f();
		}
		
		dest.set(left.y * right.z - left.z * right.y, right.x * left.z - right.z * left.x, left.x * right.y - left.y * right.x);
		
		return dest;
	}
	
	public static float getAngleBetween(Vec3f a, Vec3f b) {
		return (float) Math.acos(Math.min(1.0F, Vec3f.dot(a, b) / (a.length() * b.length())));
	}
	
	public static Quaternionf getRotatorBetween(Vec3f a, Vec3f b, Quaternionf dest) {
		if (dest == null) {
			dest = new Quaternionf();
		}
		
		Vec3f axis = Vec3f.cross(a, b, null).normalize();
		float dotDivLength = Vec3f.dot(a, b) / (a.length() * b.length());
		
		if (!Float.isFinite(dotDivLength)) {
			EpicFight.LOGGER.info("Warning : given vector's length is zero");
			(new IllegalArgumentException()).printStackTrace();
			dotDivLength = 1.0F;
		}
		
		float radian = (float)Math.acos(Math.min(1.0F, dotDivLength));
		dest.setAngleAxis(radian, axis.x, axis.y, axis.z);
		
		return dest;
	}
	
	public static Vec3f interpolate(Vec3f from, Vec3f to, float interpolation, Vec3f dest) {
		if (dest == null) {
			dest = new Vec3f();
		}
		
		dest.x = from.x + (to.x - from.x) * interpolation;
		dest.y = from.y + (to.y - from.y) * interpolation;
		dest.z = from.z + (to.z - from.z) * interpolation;
		
		return dest;
	}
	
	public Vec3f normalize() {
		return normalize(this, this);
	}
	
	public static Vec3f normalize(Vec3f src, Vec3f dest) {
		if (dest == null) {
			dest = new Vec3f();
		}
		
		float norm = (float) Math.sqrt(src.x * src.x + src.y * src.y + src.z * src.z);
		
		if (norm > 1E-5F) {
			dest.x = src.x / norm;
			dest.y = src.y / norm;
			dest.z = src.z / norm;
		} else {
			dest.x = 0;
			dest.y = 0;
			dest.z = 0;
		}
		
		return dest;
	}
	
	@Override
	public String toString() {
		return "[" + this.x + ", " + this.y + ", " + this.z + "]";
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		} else if (o instanceof Vec3f vec3f) {
			return Float.compare(this.x, vec3f.x) == 0 && Float.compare(this.y, vec3f.y) == 0 && Float.compare(this.z, vec3f.z) == 0;
		}
		
		return false;
	}
	
	@Override
	public int hashCode() {
		int j = Float.floatToIntBits(this.x);
		int i = (int) (j ^ j >>> 32);
		j = Float.floatToIntBits(this.y);
		i = 31 * i + (int) (j ^ j >>> 32);
		j = Float.floatToIntBits(this.z);
		
		return 31 * i + (int) (j ^ j >>> 32);
	}
	
	public static Vec3f average(Collection<Vec3f> vectors, Vec3f dest) {
		if (dest == null) {
			dest = new Vec3f();
		}
		
		dest.set(0.0F, 0.0F, 0.0F);
		
		for (Vec3f v : vectors) {
			dest.add(v);
		}
		
		dest.scale(1.0F / vectors.size());
		
		return dest;
	}
	
	public static Vec3f average(Vec3f dest, Vec3f... vectors) {
		if (dest == null) {
			dest = new Vec3f();
		}
		
		dest.set(0.0F, 0.0F, 0.0F);
		
		for (Vec3f v : vectors) {
			dest.add(v);
		}
		
		dest.scale(vectors.length);
		
		return dest;
	}
	
	public static int getNearest(Vec3f from, List<Vec3f> vectors) {
		float minLength = Float.MAX_VALUE;
		int index = -1;
		
		for (int i = 0; i < vectors.size(); i++) {
			if (vectors.get(i) == null) {
				continue;
			}
			
			if (!vectors.get(i).validateValues()) {
				continue;
			}
			
			float distSqr = from.distanceSqr(vectors.get(i));
			
			if (distSqr < minLength) {
				minLength = distSqr;
				index = i;
			}
		}
		
		return index;
	}
	
	public static int getNearest(Vec3f from, Vec3f... vectors) {
		float minLength = Float.MAX_VALUE;
		int index = -1;
		
		for (int i = 0; i < vectors.length; i++) {
			if (vectors[i] == null) {
				continue;
			}
			
			if (!vectors[i].validateValues()) {
				continue;
			}
			
			float distSqr = from.distanceSqr(vectors[i]);
			
			if (distSqr < minLength) {
				minLength = distSqr;
				index = i;
			}
		}
		
		return index;
	}
	
	public static int getMostSimilar(Vec3f start, Vec3f end, Vec3f... vectors) {
		Vec3f basis = BASIS_DIRECTION.get();
		Vec3f comparision = COMPARISION.get();
		Vec3f.sub(end, start, basis);
		float maxDot = Float.MIN_VALUE;
		int index = -1;
		
		for (int i = 0; i < vectors.length; i++) {
			if (vectors[i] == null) {
				continue;
			}
			
			if (!vectors[i].validateValues()) {
				continue;
			}
			
			Vec3f.sub(vectors[i], start, comparision);
			float dot = Vec3f.dot(basis, comparision) / basis.length() * comparision.length();
			
			if (dot > maxDot) {
				maxDot = dot;
				index = i;
			}
		}
		
		return index;
	}
	
	private static final ThreadLocal<Vec3f> BASIS_DIRECTION = ThreadLocal.withInitial(Vec3f::new);
	private static final ThreadLocal<Vec3f> COMPARISION = ThreadLocal.withInitial(Vec3f::new);
	
	public static int getMostSimilar(Vec3f start, Vec3f end, List<Vec3f> vectors) {
		Vec3f basis = BASIS_DIRECTION.get();
		Vec3f comparision = COMPARISION.get();
		Vec3f.sub(end, start, basis);
		float maxDot = Float.MIN_VALUE;
		int index = -1;
		
		for (int i = 0; i < vectors.size(); i++) {
			if (vectors.get(i) == null) {
				continue;
			}
			
			if (!vectors.get(i).validateValues()) {
				continue;
			}
			
			Vec3f.sub(vectors.get(i), start, comparision);
			float dot = Vec3f.dot(basis, comparision) / basis.length() * comparision.length();
			
			if (dot > maxDot) {
				maxDot = dot;
				index = i;
			}
		}
		
		return index;
	}
	
	public Vector3f toMojangVector() {
		return new Vector3f(this.x, this.y, this.z);
	}
	
	public Vec3 toDoubleVector() {
		return new Vec3(this.x, this.y, this.z);
	}
	
	public static Vec3f fromMojangVector(Vector3f vec3) {
		return new Vec3f(vec3.x(), vec3.y(), vec3.z());
	}
	
	public static Vec3f fromDoubleVector(Vec3 vec3) {
		return new Vec3f((float)vec3.x(), (float)vec3.y(), (float)vec3.z());
	}
	
	private static final ThreadLocal<OpenMatrix4f> DEST = ThreadLocal.withInitial(OpenMatrix4f::new);
	
	public Vec3f rotateDegree(Vec3f axis, float degree) {
		OpenMatrix4f dest = DEST.get();
		OpenMatrix4f.ofRotationDegree(degree, axis, dest);
		OpenMatrix4f.transform3v(dest, this, this);
		return this;
	}
}