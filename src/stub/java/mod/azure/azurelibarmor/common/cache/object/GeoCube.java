package mod.azure.azurelibarmor.common.cache.object;

import com.mojang.blaze3d.vertex.PoseStack;

/// Stub for AzureLibArmor's GeoCube.
public class GeoCube {
    public GeoQuad[] quads() { return new GeoQuad[0]; }
    public static void translateToPivotPoint(PoseStack poseStack, GeoCube cube) {}
    public static void translateAwayFromPivotPoint(PoseStack poseStack, GeoCube cube) {}
    public static void rotateMatrixAroundCube(PoseStack poseStack, GeoCube cube) {}
}
