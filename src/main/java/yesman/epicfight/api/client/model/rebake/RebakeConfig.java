package yesman.epicfight.api.client.model.rebake;

public final class RebakeConfig {
	private RebakeConfig() {
	}

	public static final float MATCH_DISTANCE = 0.1F;

	public static final float COINCIDENT_DISTANCE = 0.035F;

	public static final float MATCH_NORMAL_DEGREES = 35.0F;

	public static final float MATCH_NORMAL_COS = (float) Math.cos(Math.toRadians(MATCH_NORMAL_DEGREES));

	public static final int VOXEL_RESOLUTION = 40;

	public static final int VOXEL_DILATION = 2;

	public static final int DIFFUSION_ITERATIONS = 64;

	public static final float WELD_EPSILON = 1.0F / 8192.0F;

	public static final float TESSELLATION_STEP = 0.125F;

	public static final int MAX_SEGMENTS = 12;

	public static final float WEIGHT_FILTER_RADIUS = 0.0F;

	public static final boolean BLEND_BAND_BOUNDARIES = false;

	public static final boolean FOLD_BANDS = true;

	public static final float FOLD_BAND_HALF_WIDTH = 0.03125F;

	public static final float STEP_PROBE_DISTANCE = 0.03125F;

	public static final float PURE_BAND_WEIGHT = 0.9F;

	public static final float COLLAPSE_AREA_RATIO = 0.15F;

	public static final float MIN_EDGE_LENGTH = 0.03125F;

	public static final float SURFACE_BIND_DISTANCE = 0.125F;

	public static final boolean SURFACE_BINDING = false;

	public static final boolean REPORT_FIT = true;

	public static final float BENDABLE_LENGTH = 4.0F / 16.0F;

	public static final boolean HANDOVER_TWO_ROWS = false;

	public static final boolean BAND_CUTS = true;

	public static final boolean WORLD_GRID_CUTS = false;

	public static final float SMOOTH_FIELD_FRACTION = 0.25F;

	public static final float RIGID_WEIGHT_TOLERANCE = 0.02F;
}
