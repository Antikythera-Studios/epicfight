package yesman.epicfight.api.utils.math;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.joml.*;
import org.joml.Math;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MathUtils {
	public static final Vec3 XP = new Vec3(1.0D, 0.0D, 0.0D);
	public static final Vec3 XN = new Vec3(-1.0D, 0.0D, 0.0D);
	public static final Vec3 YP = new Vec3(0.0D, 1.0D, 0.0D);
	public static final Vec3 YN = new Vec3(0.0D, -1.0D, 0.0D);
	public static final Vec3 ZP = new Vec3(0.0D, 0.0D, 1.0D);
	public static final Vec3 ZN = new Vec3(0.0D, 0.0D, -1.0D);
	
	public static OpenMatrix4f getModelMatrixIntegral(float xPosO, float xPos, float yPosO, float yPos, float zPosO, float zPos, float xRotO, float xRot, float yRotO, float yRot, float partialTick, float scaleX, float scaleY, float scaleZ) {
		OpenMatrix4f modelMatrix = new OpenMatrix4f();
		Vec3f translation = new Vec3f(-(xPosO + (xPos - xPosO) * partialTick), ((yPosO + (yPos - yPosO) * partialTick)), -(zPosO + (zPos - zPosO) * partialTick));
		float partialXRot = Mth.rotLerp(partialTick, xRotO, xRot);
		float partialYRot = Mth.rotLerp(partialTick, yRotO, yRot);
		modelMatrix.translate(translation).rotateDeg(-partialYRot, Vec3f.Y_AXIS).rotateDeg(-partialXRot, Vec3f.X_AXIS).scale(scaleX, scaleY, scaleZ);
		
		return modelMatrix;
	}
	
	/**
	 * Blender 2.79 bezier curve
	 * @param t: 0 ~ 1
	 * @retur
	 */
	public static double bezierCurve(double t) {
		double p1 = 0.0D;
		double p2 = 0.0D;
		double p3 = 1.0D;
		double p4 = 1.0D;
		double v1, v2, v3, v4;
		
		v1 = p1;
		v2 = 3.0D * (p2 - p1);
		v3 = 3.0D * (p1 - 2.0D * p2 + p3);
		v4 = p4 - p1 + 3.0D * (p2 - p3);
		
		return v1 + t * v2 + t * t * v3 + t * t * t * v4;
	}
	
	public static float bezierCurve(float t) {
		return (float)bezierCurve((double)t);
	}
	
	public static int getSign(double value) {
		return value > 0.0D ? 1 : -1;
	}
	
	public static Vec3 getVectorForRotation(float pitch, float yaw) {
		float f = pitch * (float) Math.PI / 180F;
		float f1 = -yaw * (float) Math.PI / 180F;
		float f2 = Mth.cos(f1);
		float f3 = Mth.sin(f1);
		float f4 = Mth.cos(f);
		float f5 = Mth.sin(f);
		
		return new Vec3(f3 * f4, -f5, f2 * f4);
	}
	
	public static float lerpBetween(float f1, float f2, float zero2one) {
		float f = 0;

		for (f = f2 - f1; f < -180.0F; f += 360.0F) {
		}

		while (f >= 180.0F) {
			f -= 360.0F;
		}

		return f1 + zero2one * f;
	}
	
	public static float rotlerp(float from, float to, float limit) {
		float f = Mth.wrapDegrees(to - from);
		
		if (f > limit) {
			f = limit;
		}
		
		if (f < -limit) {
			f = -limit;
		}
		
		float f1 = from + f;
		
		while (f1 >= 180.0F) {
			f1 -= 360.0F;
		}
		
		while (f1 <= -180.0F) {
			f1 += 360.0F;
		}
		
		return f1;
	}
	
	public static float rotWrap(double d) {
		while (d >= 180.0) {
			d -= 360.0;
		}
		while (d < -180.0) {
			d += 360.0;
		}
		return (float)d;
	}
	
	public static float wrapRadian(float pValue) {
		float maxRot = (float)Math.PI * 2.0F;
		float f = pValue % maxRot;
		
		if (f >= Math.PI) {
			f -= maxRot;
		}
		
		if (f < -Math.PI) {
			f += maxRot;
		}
		
		return f;
	}
	
	public static float lerpDegree(float from, float to, float progression) {
		from = Mth.wrapDegrees(from);
		to = Mth.wrapDegrees(to);
		
		if (Math.abs(from - to) > 180.0F) {
			if (to < 0.0F) {
				from -= 360.0F;
			} else if (to > 0.0F) {
				from += 360.0F;
			}
		}
		
		return Mth.lerp(progression, from, to);
	}
	
	public static float findNearestRotation(float src, float rotation) {
		float diff = Math.abs(src - rotation);
		float idealRotation = rotation;
		int sign = Mth.sign(src - rotation);
		
		if (sign == 0) {
			return rotation;
		}
		
		while (true) {
			float next = idealRotation + sign * 360.0F;
			
			if (Math.abs(src - next) > diff) {
				return idealRotation;
			}
			
			idealRotation = next;
			diff = Math.abs(src - next);
		}
	}
	
	public static Vec3 getNearestVector(Vec3 from, Vec3... vectors) {
		double minLength = 1000000.0D;
		int index = 0;
		
		for (int i = 0; i < vectors.length; i++) {
			if (vectors[i] == null) {
				continue;
			}
			
			double distSqr = from.distanceToSqr(vectors[i]);
			
			if (distSqr < minLength) {
				minLength = distSqr;
				index = i;
			}
		}
		
		return vectors[index];
	}
	
	public static Vec3 getNearestVector(Vec3 from, List<Vec3> vectors) {
		return getNearestVector(from, vectors.toArray(new Vec3[0]));
	}
	
	public static int greatest(int... iList) {
		int max = Integer.MIN_VALUE;
		
		for (int i : iList) {
			if (max < i) {
				max = i;
			}
		}
		
		return max;
	}
	
	public static int least(int... iList) {
		int min = Integer.MAX_VALUE;
		
		for (int i : iList) {
			if (min > i) {
				min = i;
			}
		}
		
		return min;
	}
	
	public static float greatest(float... fList) {
		float max = -1000000.0F;
		
		for (float f : fList) {
			if (max < f) {
				max = f;
			}
		}
		
		return max;
	}
	
	public static float least(float... fList) {
		float min = 1000000.0F;
		
		for (float f : fList) {
			if (min > f) {
				min = f;
			}
		}
		
		return min;
	}
	
	public static double greatest(double... dList) {
		double max = -1000000.0D;
		
		for (double d : dList) {
			if (max < d) {
				max = d;
			}
		}
		
		return max;
	}
	
	public static double least(double... dList) {
		double min = 1000000.0D;
		
		for (double d : dList) {
			if (min > d) {
				min = d;
			}
		}
		
		return min;
	}
	
	@Deprecated(forRemoval = true, since = "1.21.1")
	public static void translateStack(PoseStack poseStack, OpenMatrix4f mat) {
		poseStack.translate(mat.m30, mat.m31, mat.m32);
	}
	
	private static final OpenMatrix4f OPEN_MATRIX_BUFFER = new OpenMatrix4f();
	
	@Deprecated(forRemoval = true, since = "1.21.1")
	public static void rotateStack(PoseStack poseStack, OpenMatrix4f mat) {
		OpenMatrix4f.transpose(mat, OPEN_MATRIX_BUFFER);
		poseStack.mulPose(getQuaternionFromMatrix(OPEN_MATRIX_BUFFER));
	}
	
	@Deprecated(forRemoval = true, since = "1.21.1")
	public static void scaleStack(PoseStack poseStack, OpenMatrix4f mat) {
		OpenMatrix4f.transpose(mat, OPEN_MATRIX_BUFFER);
		Vector3f vector = getScaleVectorFromMatrix(OPEN_MATRIX_BUFFER);
		poseStack.scale(vector.x(), vector.y(), vector.z());
	}
	
	private static final Matrix4f BUFFER4F = new Matrix4f();
	private static final Matrix3f BUFFER3F = new Matrix3f();
	private static final OpenMatrix4f NORMAL_MAT_BUFFER = new OpenMatrix4f();

	/// Multiplies the pose matrix by [mat] directly instead of delegating to
	/// [PoseStack#mulPose(Matrix4f)]: mulPose recomputes the normal matrix as the
	/// inverse-transpose of the *entire* pose when the input isn't orthonormal, which
	/// inherits the GUI entity renderer's scale(30,30,-30) and shrinks normals to ~0
	/// (the "dark player in inventory" bug upstream fixed in PR #2313 by not touching
	/// normals at all). We instead rotate normals by the local matrix's rotation —
	/// scale stripped via orthonormalization — so held items keep correct directional
	/// shading in world renders too.
	public static void mulStack(PoseStack poseStack, OpenMatrix4f mat) {
		OpenMatrix4f.exportToMojangMatrix(mat, BUFFER4F);
		poseStack.last().pose().mul(BUFFER4F);
		OpenMatrix4f.exportToMojangMatrix(OpenMatrix4f.removeScale(mat, NORMAL_MAT_BUFFER), BUFFER4F);
		poseStack.last().normal().mul(BUFFER3F.set(BUFFER4F));
	}

    public static double getAngleBetween(Vec3f a, Vec3f b) {
        Vec3f normA = Vec3f.normalize(a, null);
        Vec3f normB = Vec3f.normalize(b, null);

        double cos = (normA.x * normB.x + normA.y * normB.y + normA.z * normB.z);
        return Math.toDegrees(Math.acos(cos));
    }

    public static double getAngleBetween(Vec3 a, Vec3 b) {
        Vec3 normA = a.normalize();
        Vec3 normB = b.normalize();

        double cos = (normA.x * normB.x + normA.y * normB.y + normA.z * normB.z);
        return Math.toDegrees(Math.safeAcos(cos));
    }
	
	public static float getAngleBetween(Quaternionf a, Quaternionf b) {
		float dot = a.w * b.w + a.x * b.x + a.y * b.y + a.z * b.z;
		return 2.0F * (Math.safeAcos(MathUtils.getSign(dot) * b.w) - Math.safeAcos(a.w));
	}
	
	public static double getXRotOfVector(Vec3 vec) {
		Vec3 normalized = vec.normalize();
		return -(Math.atan2(normalized.y, (float)Math.sqrt(normalized.x * normalized.x + normalized.z * normalized.z)) * (180D / Math.PI));
	}
	
	public static double getYRotOfVector(Vec3 vec) {
		Vec3 normalized = vec.normalize();
		return Math.atan2(normalized.z, normalized.x) * (180D / Math.PI) - 90.0F;
	}
	
	private static Quaternionf getQuaternionFromMatrix(OpenMatrix4f mat) {
		Quaternionf quat = new Quaternionf(0, 0, 0, 1);
		quat.setFromUnnormalized(OpenMatrix4f.exportToMojangMatrix(mat.transpose(null)));
		return quat;
	}
	
	public static Vec3f lerpVector(Vec3f start, Vec3f end, float delta) {
		return lerpVector(start, end, delta, new Vec3f());
	}
	
	public static Vec3f lerpVector(Vec3f start, Vec3f end, float delta, Vec3f dest) {
		if (dest == null) {
			dest = new Vec3f();
		}
		
		dest.x = start.x + (end.x - start.x) * delta;
		dest.y = start.y + (end.y - start.y) * delta;
		dest.z = start.z + (end.z - start.z) * delta;
		
		return dest;
	}
	
	public static Vec3 lerpVector(Vec3 start, Vec3 end, float delta) {
		return new Vec3(start.x + (end.x - start.x) * delta, start.y + (end.y - start.y) * delta, start.z + (end.z - start.z) * delta);
	}
	
	public static Vector3f lerpMojangVector(Vector3f start, Vector3f end, float delta) {
		float x = start.x() + (end.x() - start.x()) * delta;
		float y = start.y() + (end.y() - start.y()) * delta;
		float z = start.z() + (end.z() - start.z()) * delta;
		return new Vector3f(x, y, z);
	}
	
	public static Vec3 projectVector(Vec3 from, Vec3 to) {
		double dot = to.dot(from);
		double normalScale = 1.0D / ((to.x * to.x) + (to.y * to.y) + (to.z * to.z));
		
		return new Vec3(dot * to.x * normalScale, dot * to.y * normalScale, dot * to.z * normalScale);
	}
	
	public static Vec3f projectVector(Vec3f from, Vec3f to, Vec3f dest) {
		if (dest == null) {
			dest = new Vec3f();
		}
		
		float dot = Vec3f.dot(to, from);
		float normalScale = 1.0F / ((to.x * to.x) + (to.y * to.y) + (to.z * to.z));
		
		dest.x = dot * to.x * normalScale;
		dest.y = dot * to.y * normalScale;
		dest.z = dot * to.z * normalScale;
		
		return dest;
	}
	
	public static void setQuaternion(Quaternionf quat, float x, float y, float z, float w) {
		quat.set(x, y, z, w);
	}
	
	public static Quaternionf mulQuaternion(Quaternionf left, Quaternionf right, Quaternionf dest) {
		if (dest == null) {
			dest = new Quaternionf(0.0F, 0.0F, 0.0F, 1.0F);
		}
		
		float f = left.x();
	    float f1 = left.y();
	    float f2 = left.z();
	    float f3 = left.w();
	    float f4 = right.x();
	    float f5 = right.y();
	    float f6 = right.z();
	    float f7 = right.w();
	    float i = f3 * f4 + f * f7 + f1 * f6 - f2 * f5;
	    float j = f3 * f5 - f * f6 + f1 * f7 + f2 * f4;
	    float k = f3 * f6 + f * f5 - f1 * f4 + f2 * f7;
	    float r = f3 * f7 - f * f4 - f1 * f5 - f2 * f6;
	    
	    dest.set(i, j, k, r);
	    
	    return dest;
	}
	
	public static Quaternionf lerpQuaternion(Quaternionf from, Quaternionf to, float delta) {
		return lerpQuaternion(from, to, delta, null);
	}
	
	public static Quaternionf lerpQuaternion(Quaternionf from, Quaternionf to, float delta, Quaternionf dest) {
		if (dest == null) {
			dest = new Quaternionf();
		}
		
		float fromX = from.x();
		float fromY = from.y();
		float fromZ = from.z();
		float fromW = from.w();
		float toX = to.x();
		float toY = to.y();
		float toZ = to.z();
		float toW = to.w();
		float resultX;
		float resultY;
		float resultZ;
		float resultW;
		float dot = fromW * toW + fromX * toX + fromY * toY + fromZ * toZ;
		float blendI = 1.0F - delta;
		
		if (dot < 0.0F) {
			resultW = blendI * fromW + delta * -toW;
			resultX = blendI * fromX + delta * -toX;
			resultY = blendI * fromY + delta * -toY;
			resultZ = blendI * fromZ + delta * -toZ;
		} else {
			resultW = blendI * fromW + delta * toW;
			resultX = blendI * fromX + delta * toX;
			resultY = blendI * fromY + delta * toY;
			resultZ = blendI * fromZ + delta * toZ;
		}
		
		dest.set(resultX, resultY, resultZ, resultW);
		dest.normalize();
		
		return dest;
	}
	
	private static Vector3f getScaleVectorFromMatrix(OpenMatrix4f mat) {
		Vec3f a = new Vec3f(mat.m00, mat.m10, mat.m20);
		Vec3f b = new Vec3f(mat.m01, mat.m11, mat.m21);
		Vec3f c = new Vec3f(mat.m02, mat.m12, mat.m22);
		return new Vector3f(a.length(), b.length(), c.length());
	}
	
	public static <T> Set<Set<T>> getSubset(Collection<T> collection) {
		Set<Set<T>> subsets = new HashSet<> ();
		List<T> asList = new ArrayList<> (collection);
		createSubset(0, asList, new HashSet<> (), subsets);
		
		return subsets;
	}
	
	private static <T> void createSubset(int idx, List<T> elements, Set<T> parent, Set<Set<T>> subsets) {
		for (int i = idx; i < elements.size(); i++) {
			Set<T> subset = new HashSet<> (parent);
			subset.add(elements.get(i));
			subsets.add(subset);
			
			createSubset(i + 1, elements, subset, subsets);
		}
	}
	
	public static int getLeastAngleVectorIdx(Vec3f src, Vec3f... candidates) {
		int leastVectorIdx = -1;
		int current = 0;
		float maxDot = -10000.0F;
		
		for (Vec3f normzlizedVec : Stream.of(candidates).map((vec) -> vec.normalize()).collect(Collectors.toList())) {
			float dot = Vec3f.dot(src, normzlizedVec);
			
			if (maxDot < dot) {
				maxDot = dot;
				leastVectorIdx = current;
			}
			
			current++;
		}
		
		return leastVectorIdx;
	}
	
	public static Vec3f getLeastAngleVector(Vec3f src, Vec3f... candidates) {
		return candidates[getLeastAngleVectorIdx(src, candidates)];
	}
	
	public static boolean canBeSeen(Entity target, Entity watcher, double maxDistance) {
		if (target.level() != watcher.level()) {
			return false;
		}
		
		double sqr = maxDistance * maxDistance;
		Level level = target.level();
		Vec3 vec1 = watcher.getEyePosition();
		
		double height = target.getBoundingBox().maxY - target.getBoundingBox().minY;
		Vec3 vec2 = target.position().add(0.0D, height * 0.15D, 0.0D);
		Vec3 vec3 = target.position().add(0.0D, height * 0.5D, 0.0D);
		Vec3 vec4 = target.position().add(0.0D, height * 0.95D, 0.0D);
		
		return vec1.distanceToSqr(vec2) < sqr && level.clip(new ClipContext(vec1, vec2, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, watcher)).getType() == HitResult.Type.MISS ||
				vec1.distanceToSqr(vec3) < sqr && level.clip(new ClipContext(vec1, vec3, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, watcher)).getType() == HitResult.Type.MISS ||
				vec1.distanceToSqr(vec4) < sqr && level.clip(new ClipContext(vec1, vec4, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, watcher)).getType() == HitResult.Type.MISS;
	}
	
	public static int packColor(int r, int g, int b, int a) {
		int ir = r << 16;
		int ig = g << 8;
		int ib = b;
		int ia = a << 24;
		
		return ir | ig | ib | ia;
	}
	
	public static void unpackColor(int packedColor, Vector4i result) {
		int b = (packedColor & 0x000000FF);
		int g = (packedColor & 0x0000FF00) >>> 8;
		int r = (packedColor & 0x00FF0000) >>> 16;
		int a = (packedColor & 0xFF000000) >>> 24;
		
		result.x = r;
		result.y = g;
		result.z = b;
		result.w = a;
	}
	
	public static byte normalIntValue(float pNum) {
		return (byte)((int)(Mth.clamp(pNum, -1.0F, 1.0F) * 127.0F) & 255);
	}

    /**
     * Wrap a value within bounds
     */
    public static int wrapClamp(int value, int min, int max) {
        int stride = max - min + 1;
        while (value < min) value += stride;
        while (value > max) value -= stride;
        return value;
    }

    /**
     * Transform the world coordinate system to -1~1 screen coord system
     * @param projectionMatrix	current projection matrix
     * @param camera		    a camera object
     * @param position		    a source vector to transform
     */
    public static Vec2 worldToScreenCoord(Matrix4f projectionMatrix, Camera camera, Vec3 position) {
        Vector4f relativeCamera = new Vector4f((float)camera.getPosition().x() - (float)position.x(), (float)camera.getPosition().y() - (float)position.y(), (float)camera.getPosition().z() - (float)position.z(), 1.0F);
        relativeCamera.rotate(Axis.YP.rotationDegrees(camera.getYRot() + 180.0F));
        relativeCamera.rotate(Axis.XP.rotationDegrees(camera.getXRot()));
        relativeCamera.mul(projectionMatrix);

        float depth = relativeCamera.w;
        relativeCamera.mul(1.0F / relativeCamera.w());

        if (depth < 0.0F) {
            relativeCamera.x = -relativeCamera.x;
            relativeCamera.y = -relativeCamera.y;
        }

        return new Vec2(relativeCamera.x(), relativeCamera.y());
    }

	private MathUtils() {}
}