package yesman.epicfight.api.utils.math;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.List;

import javax.annotation.Nullable;

import org.joml.Math;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import com.google.common.collect.Lists;

import net.minecraft.world.phys.Vec3;
import org.lwjgl.system.MemoryUtil;
import yesman.epicfight.api.animation.JointTransform;

public class OpenMatrix4f {
	private static final ThreadLocal<FloatBuffer> MATRIX_TRANSFORMER = ThreadLocal.withInitial(() -> ByteBuffer.allocateDirect(16 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer());
	private static final ThreadLocal<Vec3f> VECTOR_STORAGE = ThreadLocal.withInitial(Vec3f::new);
	private static final ThreadLocal<Vec4f> VEC4_STORAGE = ThreadLocal.withInitial(Vec4f::new);
	private static final ThreadLocal<OpenMatrix4f> MATRIX_STORAGE = ThreadLocal.withInitial(OpenMatrix4f::new);
	
	public static final OpenMatrix4f IDENTITY = new OpenMatrix4f();
	
	/*
	 * m00 m01 m02 m03
	 * m10 m11 m12 m13
	 * m20 m21 m22 m23
	 * m30 m31 m32 m33
	 */
	public float m00, m01, m02, m03, m10, m11, m12, m13, m20, m21, m22, m23, m30, m31, m32, m33;
	
	private final boolean immutable;
	
	public OpenMatrix4f() {
		this.setIdentity();
		this.immutable = false;
	}
	
	public OpenMatrix4f(final OpenMatrix4f src) {
		this(src, false);
	}
	
	public OpenMatrix4f(final OpenMatrix4f src, boolean immutable) {
		load(src);
		this.immutable = immutable;
	}
	
	public OpenMatrix4f(final JointTransform jointTransform) {
		load(OpenMatrix4f.fromQuaternion(jointTransform.rotation()).translate(jointTransform.translation()).scale(jointTransform.scale()));
		this.immutable = false;
	}
	
	public OpenMatrix4f(
		  float m00, float m01, float m02, float m03
		, float m10, float m11, float m12, float m13
		, float m20, float m21, float m22, float m23
		, float m30, float m31, float m32, float m33
	) {
		this.m00 = m00;
		this.m01 = m01;
		this.m02 = m02;
		this.m03 = m03;
		this.m10 = m10;
		this.m11 = m11;
		this.m12 = m12;
		this.m13 = m13;
		this.m20 = m20;
		this.m21 = m21;
		this.m22 = m22;
		this.m23 = m23;
		this.m30 = m30;
		this.m31 = m31;
		this.m32 = m32;
		this.m33 = m33;
		this.immutable = false;
	}
	
	public OpenMatrix4f setIdentity() {
		return setIdentity(this);
	}
	
	/**
	 * Set the given matrix to be the identity matrix.
	 * @param m The matrix to set to the identity
	 * @return m
	 */
	public static OpenMatrix4f setIdentity(OpenMatrix4f m) {
		if (m.immutable) {
			throw new UnsupportedOperationException("Can't modify immutable matrix");
		}
		
		m.m00 = 1.0f;
		m.m01 = 0.0f;
		m.m02 = 0.0f;
		m.m03 = 0.0f;
		m.m10 = 0.0f;
		m.m11 = 1.0f;
		m.m12 = 0.0f;
		m.m13 = 0.0f;
		m.m20 = 0.0f;
		m.m21 = 0.0f;
		m.m22 = 1.0f;
		m.m23 = 0.0f;
		m.m30 = 0.0f;
		m.m31 = 0.0f;
		m.m32 = 0.0f;
		m.m33 = 1.0f;
		
		return m;
	}
	
	public OpenMatrix4f load(OpenMatrix4f src) {
		return load(src, this);
	}

	public static OpenMatrix4f load(OpenMatrix4f src, @Nullable OpenMatrix4f dest) {
		if (dest == null) {
			dest = new OpenMatrix4f();
		} else if (dest.immutable) {
			throw new UnsupportedOperationException("Can't modify immutable matrix");
		}
		
		dest.m00 = src.m00;
		dest.m01 = src.m01;
		dest.m02 = src.m02;
		dest.m03 = src.m03;
		dest.m10 = src.m10;
		dest.m11 = src.m11;
		dest.m12 = src.m12;
		dest.m13 = src.m13;
		dest.m20 = src.m20;
		dest.m21 = src.m21;
		dest.m22 = src.m22;
		dest.m23 = src.m23;
		dest.m30 = src.m30;
		dest.m31 = src.m31;
		dest.m32 = src.m32;
		dest.m33 = src.m33;
		
		return dest;
	}
	
	public static OpenMatrix4f load(@Nullable OpenMatrix4f dest, float[] elements) {
		if (dest == null) {
			dest = new OpenMatrix4f();
		} else if (dest.immutable) {
			throw new UnsupportedOperationException("Can't modify immutable matrix");
		}
		
		dest.m00 = elements[0];
		dest.m01 = elements[1];
		dest.m02 = elements[2];
		dest.m03 = elements[3];
		dest.m10 = elements[4];
		dest.m11 = elements[5];
		dest.m12 = elements[6];
		dest.m13 = elements[7];
		dest.m20 = elements[8];
		dest.m21 = elements[9];
		dest.m22 = elements[10];
		dest.m23 = elements[11];
		dest.m30 = elements[12];
		dest.m31 = elements[13];
		dest.m32 = elements[14];
		dest.m33 = elements[15];
		
		return dest;
	}
	
	public static OpenMatrix4f load(@Nullable OpenMatrix4f dest, FloatBuffer buf) {
		if (dest == null) {
			dest = new OpenMatrix4f();
		} else if (dest.immutable) {
			throw new UnsupportedOperationException("Can't modify immutable matrix");
		}
		
		buf.position(0);
		
		dest.m00 = buf.get();
		dest.m01 = buf.get();
		dest.m02 = buf.get();
		dest.m03 = buf.get();
		dest.m10 = buf.get();
		dest.m11 = buf.get();
		dest.m12 = buf.get();
		dest.m13 = buf.get();
		dest.m20 = buf.get();
		dest.m21 = buf.get();
		dest.m22 = buf.get();
		dest.m23 = buf.get();
		dest.m30 = buf.get();
		dest.m31 = buf.get();
		dest.m32 = buf.get();
		dest.m33 = buf.get();
		
		return dest;
	}

	public OpenMatrix4f load(FloatBuffer buf) {
		return OpenMatrix4f.load(this, buf);
	}
	
	public OpenMatrix4f store(FloatBuffer buf) {
		buf.put(m00);
		buf.put(m01);
		buf.put(m02);
		buf.put(m03);
		buf.put(m10);
		buf.put(m11);
		buf.put(m12);
		buf.put(m13);
		buf.put(m20);
		buf.put(m21);
		buf.put(m22);
		buf.put(m23);
		buf.put(m30);
		buf.put(m31);
		buf.put(m32);
		buf.put(m33);
		
		return this;
	}

	public OpenMatrix4f store(long address){
		MemoryUtil.memPutFloat(address, m00);
		MemoryUtil.memPutFloat(address + 4L, m01);
		MemoryUtil.memPutFloat(address + 4L * 2, m02);
		MemoryUtil.memPutFloat(address + 4L * 3, m03);

		MemoryUtil.memPutFloat(address + 4L * 4, m10);
		MemoryUtil.memPutFloat(address + 4L * 5, m11);
		MemoryUtil.memPutFloat(address + 4L * 6, m12);
		MemoryUtil.memPutFloat(address + 4L * 7, m13);

		MemoryUtil.memPutFloat(address + 4L * 8, m20);
		MemoryUtil.memPutFloat(address + 4L * 9, m21);
		MemoryUtil.memPutFloat(address + 4L * 10, m22);
		MemoryUtil.memPutFloat(address + 4L * 11, m23);

		MemoryUtil.memPutFloat(address + 4L * 12, m30);
		MemoryUtil.memPutFloat(address + 4L * 13, m31);
		MemoryUtil.memPutFloat(address + 4L * 14, m32);
		MemoryUtil.memPutFloat(address + 4L * 15, m33);

		return this;
	}
	
	public List<Float> toList() {
		List<Float> elements = Lists.newArrayList();
		
		elements.add(0, m00);
		elements.add(1, m01);
		elements.add(2, m02);
		elements.add(3, m03);
		elements.add(4, m10);
		elements.add(5, m11);
		elements.add(6, m12);
		elements.add(7, m13);
		elements.add(8, m20);
		elements.add(9, m21);
		elements.add(10, m22);
		elements.add(11, m23);
		elements.add(12, m30);
		elements.add(13, m31);
		elements.add(14, m32);
		elements.add(15, m33);
		
		return elements;
	}
	
	public OpenMatrix4f unmodifiable() {
		return new OpenMatrix4f(this, true);
	}
	
	public static OpenMatrix4f add(OpenMatrix4f left, OpenMatrix4f right, @Nullable OpenMatrix4f dest) {
		if (dest == null) {
			dest = new OpenMatrix4f();
		} else if (dest.immutable) {
			throw new UnsupportedOperationException("Can't modify immutable matrix");
		}

		dest.m00 = left.m00 + right.m00;
		dest.m01 = left.m01 + right.m01;
		dest.m02 = left.m02 + right.m02;
		dest.m03 = left.m03 + right.m03;
		dest.m10 = left.m10 + right.m10;
		dest.m11 = left.m11 + right.m11;
		dest.m12 = left.m12 + right.m12;
		dest.m13 = left.m13 + right.m13;
		dest.m20 = left.m20 + right.m20;
		dest.m21 = left.m21 + right.m21;
		dest.m22 = left.m22 + right.m22;
		dest.m23 = left.m23 + right.m23;
		dest.m30 = left.m30 + right.m30;
		dest.m31 = left.m31 + right.m31;
		dest.m32 = left.m32 + right.m32;
		dest.m33 = left.m33 + right.m33;

		return dest;
	}
	
	public OpenMatrix4f mulFront(OpenMatrix4f mulTransform) {
		return OpenMatrix4f.mul(mulTransform, this, this);
	}
	
	public OpenMatrix4f mulBack(OpenMatrix4f mulTransform) {
		return OpenMatrix4f.mul(this, mulTransform, this);
	}
	
	public static OpenMatrix4f mul(OpenMatrix4f left, OpenMatrix4f right, @Nullable OpenMatrix4f dest) {
		if (dest == null) {
			dest = new OpenMatrix4f();
		} else if (dest.immutable) {
			throw new UnsupportedOperationException("Can't modify immutable matrix");
		}
		
		float m00 = left.m00 * right.m00 + left.m10 * right.m01 + left.m20 * right.m02 + left.m30 * right.m03;
		float m01 = left.m01 * right.m00 + left.m11 * right.m01 + left.m21 * right.m02 + left.m31 * right.m03;
		float m02 = left.m02 * right.m00 + left.m12 * right.m01 + left.m22 * right.m02 + left.m32 * right.m03;
		float m03 = left.m03 * right.m00 + left.m13 * right.m01 + left.m23 * right.m02 + left.m33 * right.m03;
		float m10 = left.m00 * right.m10 + left.m10 * right.m11 + left.m20 * right.m12 + left.m30 * right.m13;
		float m11 = left.m01 * right.m10 + left.m11 * right.m11 + left.m21 * right.m12 + left.m31 * right.m13;
		float m12 = left.m02 * right.m10 + left.m12 * right.m11 + left.m22 * right.m12 + left.m32 * right.m13;
		float m13 = left.m03 * right.m10 + left.m13 * right.m11 + left.m23 * right.m12 + left.m33 * right.m13;
		float m20 = left.m00 * right.m20 + left.m10 * right.m21 + left.m20 * right.m22 + left.m30 * right.m23;
		float m21 = left.m01 * right.m20 + left.m11 * right.m21 + left.m21 * right.m22 + left.m31 * right.m23;
		float m22 = left.m02 * right.m20 + left.m12 * right.m21 + left.m22 * right.m22 + left.m32 * right.m23;
		float m23 = left.m03 * right.m20 + left.m13 * right.m21 + left.m23 * right.m22 + left.m33 * right.m23;
		float m30 = left.m00 * right.m30 + left.m10 * right.m31 + left.m20 * right.m32 + left.m30 * right.m33;
		float m31 = left.m01 * right.m30 + left.m11 * right.m31 + left.m21 * right.m32 + left.m31 * right.m33;
		float m32 = left.m02 * right.m30 + left.m12 * right.m31 + left.m22 * right.m32 + left.m32 * right.m33;
		float m33 = left.m03 * right.m30 + left.m13 * right.m31 + left.m23 * right.m32 + left.m33 * right.m33;
		
		dest.m00 = m00;
		dest.m01 = m01;
		dest.m02 = m02;
		dest.m03 = m03;
		dest.m10 = m10;
		dest.m11 = m11;
		dest.m12 = m12;
		dest.m13 = m13;
		dest.m20 = m20;
		dest.m21 = m21;
		dest.m22 = m22;
		dest.m23 = m23;
		dest.m30 = m30;
		dest.m31 = m31;
		dest.m32 = m32;
		dest.m33 = m33;
		
		return dest;
	}
	
	public static OpenMatrix4f mulMatrices(OpenMatrix4f... srcs) {
		OpenMatrix4f result = new OpenMatrix4f();
		
		for (OpenMatrix4f src : srcs) {
			result.mulBack(src);
		}
		
		return result;
	}
	
	public static OpenMatrix4f mulAsOrigin(OpenMatrix4f left, OpenMatrix4f right, OpenMatrix4f dest) {
		float x = right.m30;
		float y = right.m31;
		float z = right.m32;
		
		OpenMatrix4f result = mul(left, right, dest);
		result.m30 = x;
		result.m31 = y;
		result.m32 = z;
		
		return result;
	}
	
	public static OpenMatrix4f mulAsOriginInverse(OpenMatrix4f left, OpenMatrix4f right, OpenMatrix4f dest) {
		return mulAsOrigin(right, left, dest);
	}
	
	public static Vec4f transform(OpenMatrix4f matrix, Vec4f src, @Nullable Vec4f dest) {
		if (dest == null) {
			dest = new Vec4f();
		}
		
		float x = matrix.m00 * src.x + matrix.m10 * src.y + matrix.m20 * src.z + matrix.m30 * src.w;
		float y = matrix.m01 * src.x + matrix.m11 * src.y + matrix.m21 * src.z + matrix.m31 * src.w;
		float z = matrix.m02 * src.x + matrix.m12 * src.y + matrix.m22 * src.z + matrix.m32 * src.w;
		float w = matrix.m03 * src.x + matrix.m13 * src.y + matrix.m23 * src.z + matrix.m33 * src.w;
		
		dest.x = x;
		dest.y = y;
		dest.z = z;
		dest.w = w;
		
		return dest;
	}
	
	public static Vec3 transform(OpenMatrix4f matrix, Vec3 src) {
		double x = matrix.m00 * src.x + matrix.m10 * src.y + matrix.m20 * src.z + matrix.m30;
		double y = matrix.m01 * src.x + matrix.m11 * src.y + matrix.m21 * src.z + matrix.m31;
		double z = matrix.m02 * src.x + matrix.m12 * src.y + matrix.m22 * src.z + matrix.m32;
		
		return new Vec3(x, y ,z);
	}
	
	public static Vec3f transform3v(OpenMatrix4f matrix, Vec3f src, @Nullable Vec3f dest) {
		if (dest == null) {
			dest = new Vec3f();
		}
		
		Vec4f vec4 = VEC4_STORAGE.get();
		vec4.set(src.x, src.y, src.z, 1.0F);
		transform(matrix, vec4, vec4);
		dest.x = vec4.x;
		dest.y = vec4.y;
		dest.z = vec4.z;

		return dest;
	}
	
	public OpenMatrix4f transpose() {
		return transpose(this);
	}
	
	public OpenMatrix4f transpose(OpenMatrix4f dest) {
		return transpose(this, dest);
	}
	
	public static OpenMatrix4f transpose(OpenMatrix4f src, @Nullable OpenMatrix4f dest) {
		if (dest == null) {
		   dest = new OpenMatrix4f();
		} else if (dest.immutable) {
			throw new UnsupportedOperationException("Can't modify immutable matrix");
		}
		
		float m00 = src.m00;
		float m01 = src.m10;
		float m02 = src.m20;
		float m03 = src.m30;
		float m10 = src.m01;
		float m11 = src.m11;
		float m12 = src.m21;
		float m13 = src.m31;
		float m20 = src.m02;
		float m21 = src.m12;
		float m22 = src.m22;
		float m23 = src.m32;
		float m30 = src.m03;
		float m31 = src.m13;
		float m32 = src.m23;
		float m33 = src.m33;
		
		dest.m00 = m00;
		dest.m01 = m01;
		dest.m02 = m02;
		dest.m03 = m03;
		dest.m10 = m10;
		dest.m11 = m11;
		dest.m12 = m12;
		dest.m13 = m13;
		dest.m20 = m20;
		dest.m21 = m21;
		dest.m22 = m22;
		dest.m23 = m23;
		dest.m30 = m30;
		dest.m31 = m31;
		dest.m32 = m32;
		dest.m33 = m33;
		
		return dest;
	}

	public float determinant() {
		float f = m00 * ((m11 * m22 * m33 + m12 * m23 * m31 + m13 * m21 * m32) - m13 * m22 * m31 - m11 * m23 * m32 - m12 * m21 * m33);
		f -= m01 * ((m10 * m22 * m33 + m12 * m23 * m30 + m13 * m20 * m32) - m13 * m22 * m30 - m10 * m23 * m32 - m12 * m20 * m33);
		f += m02 * ((m10 * m21 * m33 + m11 * m23 * m30 + m13 * m20 * m31) - m13 * m21 * m30	- m10 * m23 * m31 - m11 * m20 * m33);
		f -= m03 * ((m10 * m21 * m32 + m11 * m22 * m30 + m12 * m20 * m31) - m12 * m21 * m30 - m10 * m22 * m31 - m11 * m20 * m32);
		
		return f;
	}
	
	private static float determinant3x3(float t00, float t01, float t02, float t10, float t11, float t12, float t20, float t21, float t22) {
		return t00 * (t11 * t22 - t12 * t21) + t01 * (t12 * t20 - t10 * t22) + t02 * (t10 * t21 - t11 * t20);
	}
	
	public OpenMatrix4f invert() {
		return OpenMatrix4f.invert(this, this);
	}
	
	public static OpenMatrix4f invert(OpenMatrix4f src, @Nullable OpenMatrix4f dest) {
		float determinant = src.determinant();
		
		if (determinant != 0.0F) {
			if (dest == null) {
				dest = new OpenMatrix4f();
			} else if (dest.immutable) {
				throw new UnsupportedOperationException("Can't modify immutable matrix");
			}
			
			float determinant_inv = 1.0F / determinant;

			float t00 =  determinant3x3(src.m11, src.m12, src.m13, src.m21, src.m22, src.m23, src.m31, src.m32, src.m33);
			float t01 = -determinant3x3(src.m10, src.m12, src.m13, src.m20, src.m22, src.m23, src.m30, src.m32, src.m33);
			float t02 =  determinant3x3(src.m10, src.m11, src.m13, src.m20, src.m21, src.m23, src.m30, src.m31, src.m33);
			float t03 = -determinant3x3(src.m10, src.m11, src.m12, src.m20, src.m21, src.m22, src.m30, src.m31, src.m32);
			float t10 = -determinant3x3(src.m01, src.m02, src.m03, src.m21, src.m22, src.m23, src.m31, src.m32, src.m33);
			float t11 =  determinant3x3(src.m00, src.m02, src.m03, src.m20, src.m22, src.m23, src.m30, src.m32, src.m33);
			float t12 = -determinant3x3(src.m00, src.m01, src.m03, src.m20, src.m21, src.m23, src.m30, src.m31, src.m33);
			float t13 =  determinant3x3(src.m00, src.m01, src.m02, src.m20, src.m21, src.m22, src.m30, src.m31, src.m32);
			float t20 =  determinant3x3(src.m01, src.m02, src.m03, src.m11, src.m12, src.m13, src.m31, src.m32, src.m33);
			float t21 = -determinant3x3(src.m00, src.m02, src.m03, src.m10, src.m12, src.m13, src.m30, src.m32, src.m33);
			float t22 =  determinant3x3(src.m00, src.m01, src.m03, src.m10, src.m11, src.m13, src.m30, src.m31, src.m33);
			float t23 = -determinant3x3(src.m00, src.m01, src.m02, src.m10, src.m11, src.m12, src.m30, src.m31, src.m32);
			float t30 = -determinant3x3(src.m01, src.m02, src.m03, src.m11, src.m12, src.m13, src.m21, src.m22, src.m23);
			float t31 =  determinant3x3(src.m00, src.m02, src.m03, src.m10, src.m12, src.m13, src.m20, src.m22, src.m23);
			float t32 = -determinant3x3(src.m00, src.m01, src.m03, src.m10, src.m11, src.m13, src.m20, src.m21, src.m23);
			float t33 =  determinant3x3(src.m00, src.m01, src.m02, src.m10, src.m11, src.m12, src.m20, src.m21, src.m22);

			dest.m00 = t00 * determinant_inv;
			dest.m11 = t11 * determinant_inv;
			dest.m22 = t22 * determinant_inv;
			dest.m33 = t33 * determinant_inv;
			dest.m01 = t10 * determinant_inv;
			dest.m10 = t01 * determinant_inv;
			dest.m20 = t02 * determinant_inv;
			dest.m02 = t20 * determinant_inv;
			dest.m12 = t21 * determinant_inv;
			dest.m21 = t12 * determinant_inv;
			dest.m03 = t30 * determinant_inv;
			dest.m30 = t03 * determinant_inv;
			dest.m13 = t31 * determinant_inv;
			dest.m31 = t13 * determinant_inv;
			dest.m32 = t23 * determinant_inv;
			dest.m23 = t32 * determinant_inv;
			
			return dest; 
		} else {
			if (dest == null) {
				dest = new OpenMatrix4f();
			} else {
				dest.setIdentity();
			}
			
			return dest;
		}
	}
	
	public OpenMatrix4f translate(float x, float y, float z) {
		VECTOR_STORAGE.get().set(x, y, z);
		return translate(VECTOR_STORAGE.get(), this);
	}
	
	public OpenMatrix4f translate(Vec3f vec) {
		return translate(vec, this);
	}
	
	public OpenMatrix4f translate(Vec3f vec, OpenMatrix4f dest) {
		return translate(vec, this, dest);
	}

	public static OpenMatrix4f translate(Vec3f vec, OpenMatrix4f src, @Nullable OpenMatrix4f dest) {
		if (dest == null) {
			dest = new OpenMatrix4f();
		} else if (dest.immutable) {
			throw new UnsupportedOperationException("Can't modify immutable matrix");
		}
		
		dest.m30 += src.m00 * vec.x + src.m10 * vec.y + src.m20 * vec.z;
		dest.m31 += src.m01 * vec.x + src.m11 * vec.y + src.m21 * vec.z;
		dest.m32 += src.m02 * vec.x + src.m12 * vec.y + src.m22 * vec.z;
		dest.m33 += src.m03 * vec.x + src.m13 * vec.y + src.m23 * vec.z;
		
		return dest;
	}
	
	public static OpenMatrix4f createTranslation(float x, float y, float z) {
		return ofTranslation(x, y, z, null);
	}
	
	public static OpenMatrix4f ofTranslation(float x, float y, float z, OpenMatrix4f dest) {
		if (dest == null) {
			dest = new OpenMatrix4f();
		} else if (dest.immutable) {
			throw new UnsupportedOperationException("Can't modify immutable matrix");
		}
		
		dest.setIdentity();
		dest.m30 = x;
		dest.m31 = y;
		dest.m32 = z;
		
		return dest;
	}
	
	public static OpenMatrix4f createScale(float x, float y, float z) {
		return ofScale(x, y, z, null);
	}
	
	public static OpenMatrix4f ofScale(float x, float y, float z, OpenMatrix4f dest) {
		if (dest == null) {
			dest = new OpenMatrix4f();
		} else if (dest.immutable) {
			throw new UnsupportedOperationException("Can't modify immutable matrix");
		}
		
		dest.setIdentity();
		dest.m00 = x;
		dest.m11 = y;
		dest.m22 = z;
		
		return dest;
	}
	
	public OpenMatrix4f rotateDeg(float angle, Vec3f axis) {
		return rotate((float)Math.toRadians(angle), axis);
	}
	
	public OpenMatrix4f rotate(float angle, Vec3f axis) {
		return rotate(angle, axis, this);
	}
	
	public OpenMatrix4f rotate(float angle, Vec3f axis, OpenMatrix4f dest) {
		return rotate(angle, axis, this, dest);
	}
	
	public static OpenMatrix4f createRotatorDeg(float degree, Vec3f axis) {
		return rotate((float)Math.toRadians(degree), axis, new OpenMatrix4f(), null);
	}
	
	public static OpenMatrix4f ofRotationDegree(float degree, Vec3f axis, @Nullable OpenMatrix4f dest) {
		dest.setIdentity();
		return rotate((float)Math.toRadians(degree), axis, dest, dest);
	}
	
	public static OpenMatrix4f rotate(float angle, Vec3f axis, OpenMatrix4f src, @Nullable OpenMatrix4f dest) {
		if (dest == null) {
			dest = new OpenMatrix4f();
		} else if (dest.immutable) {
			throw new UnsupportedOperationException("Can't modify immutable matrix");
		}
		
		float c = (float) Math.cos(angle);
		float s = (float) Math.sin(angle);
		float oneminusc = 1.0f - c;
		float xy = axis.x * axis.y;
		float yz = axis.y * axis.z;
		float xz = axis.x * axis.z;
		float xs = axis.x * s;
		float ys = axis.y * s;
		float zs = axis.z * s;

		float f00 = axis.x * axis.x * oneminusc+c;
		float f01 = xy * oneminusc + zs;
		float f02 = xz * oneminusc - ys;
		// n[3] not used
		float f10 = xy * oneminusc - zs;
		float f11 = axis.y * axis.y * oneminusc+c;
		float f12 = yz * oneminusc + xs;
		// n[7] not used
		float f20 = xz * oneminusc + ys;
		float f21 = yz * oneminusc - xs;
		float f22 = axis.z * axis.z * oneminusc+c;

		float t00 = src.m00 * f00 + src.m10 * f01 + src.m20 * f02;
		float t01 = src.m01 * f00 + src.m11 * f01 + src.m21 * f02;
		float t02 = src.m02 * f00 + src.m12 * f01 + src.m22 * f02;
		float t03 = src.m03 * f00 + src.m13 * f01 + src.m23 * f02;
		float t10 = src.m00 * f10 + src.m10 * f11 + src.m20 * f12;
		float t11 = src.m01 * f10 + src.m11 * f11 + src.m21 * f12;
		float t12 = src.m02 * f10 + src.m12 * f11 + src.m22 * f12;
		float t13 = src.m03 * f10 + src.m13 * f11 + src.m23 * f12;
		
		dest.m20 = src.m00 * f20 + src.m10 * f21 + src.m20 * f22;
		dest.m21 = src.m01 * f20 + src.m11 * f21 + src.m21 * f22;
		dest.m22 = src.m02 * f20 + src.m12 * f21 + src.m22 * f22;
		dest.m23 = src.m03 * f20 + src.m13 * f21 + src.m23 * f22;
		dest.m00 = t00;
		dest.m01 = t01;
		dest.m02 = t02;
		dest.m03 = t03;
		dest.m10 = t10;
		dest.m11 = t11;
		dest.m12 = t12;
		dest.m13 = t13;
		
		return dest;
	}
	
	public Vec3f toTranslationVector() {
		return toTranslationVector(this);
	}
	
	public Vec3f toTranslationVector(Vec3f dest) {
		return toTranslationVector(this, dest);
	}
	
	public static Vec3f toTranslationVector(OpenMatrix4f matrix) {
		return toTranslationVector(matrix, null);
	}
	
	public static Vec3f toTranslationVector(OpenMatrix4f matrix, Vec3f dest) {
		if (dest == null) {
			dest = new Vec3f();
		}
		
		dest.x = matrix.m30;
		dest.y = matrix.m31;
		dest.z = matrix.m32;
		
		return dest;
	}
	
	public Quaternionf toQuaternion() {
		return OpenMatrix4f.toQuaternion(this);
	}
	
	public Quaternionf toQuaternion(Quaternionf dest) {
		return OpenMatrix4f.toQuaternion(this, dest);
	}
	
	public static Quaternionf toQuaternion(OpenMatrix4f matrix) {
		return toQuaternion(matrix, new Quaternionf());
	}
	
	public static Quaternionf toQuaternion(OpenMatrix4f matrix, Quaternionf dest) {
		if (dest == null) {
			dest = new Quaternionf();
		}

		OpenMatrix4f storage = MATRIX_STORAGE.get();
		OpenMatrix4f.load(matrix, storage);

		float w, x, y, z;
		storage.transpose();

        float lenX = storage.m00 * storage.m00 + storage.m01 * storage.m01 + storage.m02 * storage.m02;
        float lenY = storage.m10 * storage.m10 + storage.m11 * storage.m11 + storage.m12 * storage.m12;
        float lenZ = storage.m20 * storage.m20 + storage.m21 * storage.m21 + storage.m22 * storage.m22;

        if (lenX == 0.0F || lenY == 0.0F || lenZ == 0.0F) {
        	return dest.set(0.0F, 0.0F, 0.0F, 1.0F);
        }

        lenX = Math.invsqrt(lenX);
        lenY = Math.invsqrt(lenY);
        lenZ = Math.invsqrt(lenZ);

        storage.m00 *= lenX; storage.m01 *= lenX; storage.m02 *= lenX;
        storage.m10 *= lenY; storage.m11 *= lenY; storage.m12 *= lenY;
        storage.m20 *= lenZ; storage.m21 *= lenZ; storage.m22 *= lenZ;

		float t;
        float tr = storage.m00 + storage.m11 + storage.m22;

        if (tr >= 0.0F) {
            t = (float)Math.sqrt(tr + 1.0F);
            w = t * 0.5F;
            t = 0.5F / t;
            x = (storage.m12 - storage.m21) * t;
            y = (storage.m20 - storage.m02) * t;
            z = (storage.m01 - storage.m10) * t;
        } else {
            if (storage.m00 >= storage.m11 && storage.m00 >= storage.m22) {
                t = (float)Math.sqrt(storage.m00 - (storage.m11 + storage.m22) + 1.0);
                x = t * 0.5F;
                t = 0.5F / t;
                y = (storage.m10 + storage.m01) * t;
                z = (storage.m02 + storage.m20) * t;
                w = (storage.m12 - storage.m21) * t;
            } else if (storage.m11 > storage.m22) {
                t = (float)Math.sqrt(storage.m11 - (storage.m22 + storage.m00) + 1.0F);
                y = t * 0.5F;
                t = 0.5F / t;
                z = (storage.m21 + storage.m12) * t;
                x = (storage.m10 + storage.m01) * t;
                w = (storage.m20 - storage.m02) * t;
            } else {
                t = (float)Math.sqrt(storage.m22 - (storage.m00 + storage.m11) + 1.0F);
                z = t * 0.5F;
                t = 0.5F / t;
                x = (storage.m02 + storage.m20) * t;
                y = (storage.m21 + storage.m12) * t;
                w = (storage.m01 - storage.m10) * t;
            }
        }

        dest.x = x;
		dest.y = y;
		dest.z = z;
		dest.w = w;

        return dest;
	}

	public static OpenMatrix4f fromQuaternion(Quaternionf quaternion) {
		return fromQuaternion(quaternion, null);
	}
	
	public static OpenMatrix4f fromQuaternion(Quaternionf quaternion, OpenMatrix4f dest) {
		if (dest == null) {
			dest = new OpenMatrix4f();
		} else if (dest.immutable) {
			throw new UnsupportedOperationException("Can't modify immutable matrix");
		}
		
		float x = quaternion.x();
		float y = quaternion.y();
		float z = quaternion.z();
		float w = quaternion.w();
		float xy = x * y;
		float xz = x * z;
		float xw = x * w;
		float yz = y * z;
		float yw = y * w;
		float zw = z * w;
		float xSquared = 2F * x * x;
		float ySquared = 2F * y * y;
		float zSquared = 2F * z * z;
		dest.m00 = 1.0F - ySquared - zSquared;
		dest.m01 = 2.0F * (xy - zw);
		dest.m02 = 2.0F * (xz + yw);
		dest.m10 = 2.0F * (xy + zw);
		dest.m11 = 1.0F - xSquared - zSquared;
		dest.m12 = 2.0F * (yz - xw);
		dest.m20 = 2.0F * (xz - yw);
		dest.m21 = 2.0F * (yz + xw);
		dest.m22 = 1.0F - xSquared - ySquared;
		
		return dest;
	}
	
	public OpenMatrix4f scale(float x, float y, float z) {
		VECTOR_STORAGE.get().set(x, y, z);
		return this.scale(VECTOR_STORAGE.get());
	}
	
	public OpenMatrix4f scale(Vec3f vec) {
		return scale(vec, this, this);
	}
	
	public static OpenMatrix4f scale(Vec3f vec, OpenMatrix4f src, @Nullable OpenMatrix4f dest) {
		if (dest == null) {
			dest = new OpenMatrix4f();
		} else if (dest.immutable) {
			throw new UnsupportedOperationException("Can't modify immutable matrix");
		}
		
		dest.m00 = src.m00 * vec.x;
		dest.m01 = src.m01 * vec.x;
		dest.m02 = src.m02 * vec.x;
		dest.m03 = src.m03 * vec.x;
		dest.m10 = src.m10 * vec.y;
		dest.m11 = src.m11 * vec.y;
		dest.m12 = src.m12 * vec.y;
		dest.m13 = src.m13 * vec.y;
		dest.m20 = src.m20 * vec.z;
		dest.m21 = src.m21 * vec.z;
		dest.m22 = src.m22 * vec.z;
		dest.m23 = src.m23 * vec.z;
		
		return dest;
	}
	
	public Vec3f toScaleVector() {
		return toScaleVector(null);
	}
	
	public Vec3f toScaleVector(Vec3f dest) {
		if (dest == null) {
			dest = new Vec3f();
		}
		
		Vec3f storage = VECTOR_STORAGE.get();
		storage.set(this.m00, this.m01, this.m02);
		dest.x = storage.length();

		storage.set(this.m10, this.m11, this.m12);
		dest.y = storage.length();

		storage.set(this.m20, this.m21, this.m22);
		dest.z = storage.length();
		
		return dest;
	}
	
	public OpenMatrix4f removeTranslation() {
		return removeTranslation(this, null);
	}
	
	public static OpenMatrix4f removeTranslation(OpenMatrix4f src, @Nullable OpenMatrix4f dest) {
		if (dest == null) {
			dest = new OpenMatrix4f();
		} else if (dest.immutable) {
			throw new UnsupportedOperationException("Can't modify immutable matrix");
		}
		
		dest.load(src);
		dest.m30 = 0.0F;
		dest.m31 = 0.0F;
		dest.m32 = 0.0F;
		
		return dest;
	}
	
	public OpenMatrix4f removeScale() {
		return removeScale(this, null);
	}
	
	public static OpenMatrix4f removeScale(OpenMatrix4f src, @Nullable OpenMatrix4f dest) {
		if (dest == null) {
			dest = new OpenMatrix4f();
		} else if (dest.immutable) {
			throw new UnsupportedOperationException("Can't modify immutable matrix");
		}
		
		Vec3f storage = VECTOR_STORAGE.get();
		storage.set(src.m00, src.m01, src.m02);
		float xScale = storage.length();

		storage.set(src.m10, src.m11, src.m12);
		float yScale = storage.length();

		storage.set(src.m20, src.m21, src.m22);
		float zScale = storage.length();
		
		dest.load(src);
		dest.scale(1.0F / xScale, 1.0F / yScale, 1.0F / zScale);
		
		return dest;
	}
	
	@Override
	public String toString() {
		return "\n" +
				String.format("%.4f", m00) + " " + String.format("%.4f", m01) + " " + String.format("%.4f", m02) + " " + String.format("%.4f", m03) + "\n" +
				String.format("%.4f", m10) + " " + String.format("%.4f", m11) + " " + String.format("%.4f", m12) + " " + String.format("%.4f", m13) + "\n" +
				String.format("%.4f", m20) + " " + String.format("%.4f", m21) + " " + String.format("%.4f", m22) + " " + String.format("%.4f", m23) + "\n" +
				String.format("%.4f", m30) + " " + String.format("%.4f", m31) + " " + String.format("%.4f", m32) + " " + String.format("%.4f", m33) + "\n"
				;
	}
	
	public static Matrix4f exportToMojangMatrix(OpenMatrix4f src) {
		return exportToMojangMatrix(src, null);
	}
	
	public static Matrix4f exportToMojangMatrix(OpenMatrix4f src, Matrix4f dest) {
		if (dest == null) {
			dest = new Matrix4f();
		}
		
		FloatBuffer transformer = MATRIX_TRANSFORMER.get();
		transformer.position(0);
		src.store(transformer);
		transformer.position(0);

		return dest.set(transformer);
	}
	
	public static OpenMatrix4f importFromMojangMatrix(Matrix4f src) {
		FloatBuffer transformer = MATRIX_TRANSFORMER.get();
		transformer.position(0);
		src.get(transformer);

		return OpenMatrix4f.load(null, transformer);
	}
	
	public static OpenMatrix4f[] allocateMatrixArray(int size) {
		OpenMatrix4f[] matrixArray = new OpenMatrix4f[size];
		
		for (int i = 0; i < size; i++) {
			matrixArray[i] = new OpenMatrix4f();
		}
		
		return matrixArray;
	}
}