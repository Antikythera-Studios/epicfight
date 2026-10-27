package yesman.epicfight.api.client.model.rebake;

import java.util.Locale;

public final class JointNames {
	private JointNames() {
	}

	public static final int ROOT = 0;
	public static final int THIGH_R = 1;
	public static final int LEG_R = 2;
	public static final int KNEE_R = 3;
	public static final int THIGH_L = 4;
	public static final int LEG_L = 5;
	public static final int KNEE_L = 6;
	public static final int TORSO = 7;
	public static final int CHEST = 8;
	public static final int HEAD = 9;
	public static final int SHOULDER_R = 10;
	public static final int ARM_R = 11;
	public static final int HAND_R = 12;
	public static final int ELBOW_R = 14;
	public static final int SHOULDER_L = 15;
	public static final int ARM_L = 16;
	public static final int HAND_L = 17;
	public static final int ELBOW_L = 19;

	public static int creaseJoint(int below, int above) {
		int low = Math.min(below, above);
		int high = Math.max(below, above);

		if (low == THIGH_R && high == LEG_R) {
			return KNEE_R;
		}

		if (low == THIGH_L && high == LEG_L) {
			return KNEE_L;
		}

		if (low == ARM_R && high == HAND_R) {
			return ELBOW_R;
		}

		if (low == ARM_L && high == HAND_L) {
			return ELBOW_L;
		}

		return -1;
	}

	public enum Region {
		UNKNOWN(ROOT),
		HEAD_REGION(JointNames.HEAD, JointNames.HEAD),
		TORSO_REGION(CHEST, TORSO, CHEST),
		ARM_RIGHT(ARM_R, SHOULDER_R, ARM_R, HAND_R, ELBOW_R),
		ARM_LEFT(ARM_L, SHOULDER_L, ARM_L, HAND_L, ELBOW_L),
		LEG_RIGHT(THIGH_R, THIGH_R, LEG_R, KNEE_R),
		LEG_LEFT(THIGH_L, THIGH_L, LEG_L, KNEE_L);

		public final int fallbackJoint;
		private final int[] allowed;

		Region(int fallbackJoint, int... allowed) {
			this.fallbackJoint = fallbackJoint;
			this.allowed = allowed;
		}

		public boolean permits(int joint) {
			if (this == UNKNOWN) {
				return true;
			}

			for (int candidate : this.allowed) {
				if (candidate == joint) {
					return true;
				}
			}

			return false;
		}
	}

	public static Region regionOf(String path) {
		if (path == null || path.isEmpty()) {
			return Region.UNKNOWN;
		}

		String[] segments = path.split("/");

		for (String segment : segments) {
			Region region = regionOfSegment(segment);

			if (region != Region.UNKNOWN) {
				return region;
			}
		}

		return Region.UNKNOWN;
	}

	public static Region regionByPosition(float x, float y, float z) {
		if (y > 1.55F) {
			return Region.HEAD_REGION;
		}

		if (y < 0.70F) {
			return x >= 0.0F ? Region.LEG_RIGHT : Region.LEG_LEFT;
		}

		if (y < 0.80F || y > 1.45F) {
			return Region.UNKNOWN;
		}

		float lateral = Math.abs(x);

		if (lateral > 0.32F) {
			return x >= 0.0F ? Region.ARM_RIGHT : Region.ARM_LEFT;
		}

		if (lateral < 0.22F) {
			return Region.TORSO_REGION;
		}

		return Region.UNKNOWN;
	}

	private static Region regionOfSegment(String rawSegment) {
		if (rawSegment != null && rawSegment.startsWith("capture:")) {
			return Region.UNKNOWN;
		}

		String name = normalise(rawSegment);

		if (name.isEmpty()) {
			return Region.UNKNOWN;
		}

		if (name.startsWith("armor")) {
			name = name.substring("armor".length());
		}

		boolean left = mentionsSide(name, "left", 'l');
		boolean right = mentionsSide(name, "right", 'r');

		if (contains(name, "leg", "boot", "shoe", "pants", "trouser", "thigh", "knee", "shin", "calf", "foot", "feet", "greave", "sabaton")) {
			if (left) {
				return Region.LEG_LEFT;
			}

			if (right) {
				return Region.LEG_RIGHT;
			}

			return Region.TORSO_REGION;
		}

		if (contains(name, "arm", "sleeve", "hand", "glove", "shoulder", "pauldron", "elbow", "bracer", "gauntlet", "wrist")) {
			if (left) {
				return Region.ARM_LEFT;
			}

			if (right) {
				return Region.ARM_RIGHT;
			}

			return Region.TORSO_REGION;
		}

		if (contains(name, "head", "helmet", "helm", "hat", "hair", "face", "mask", "horn", "ear", "crown", "visor", "skull")) {
			return Region.HEAD_REGION;
		}

		if (contains(name, "body", "torso", "chest", "jacket", "waist", "belt", "hip", "pelvis", "spine", "cape", "cloak", "back", "chestplate", "cuirass", "tabard", "skirt")) {
			return Region.TORSO_REGION;
		}

		return Region.UNKNOWN;
	}

	private static boolean contains(String haystack, String... needles) {
		for (String needle : needles) {
			if (haystack.contains(needle)) {
				return true;
			}
		}

		return false;
	}

	private static boolean mentionsSide(String name, String word, char initial) {
		if (name.contains(word)) {
			return true;
		}

		return (name.length() > 1 && name.charAt(0) == initial && !Character.isLetter(name.charAt(1)))
			|| (name.length() > 1 && name.charAt(name.length() - 1) == initial && !Character.isLetter(name.charAt(name.length() - 2)));
	}

	private static String normalise(String raw) {
		StringBuilder builder = new StringBuilder(raw.length());

		for (int i = 0; i < raw.length(); i++) {
			char c = raw.charAt(i);

			if (Character.isLetterOrDigit(c)) {
				builder.append(Character.toLowerCase(c));
			} else if (builder.length() > 0 && builder.charAt(builder.length() - 1) != '_') {
				builder.append('_');
			}
		}

		return builder.toString().toLowerCase(Locale.ROOT);
	}
}
