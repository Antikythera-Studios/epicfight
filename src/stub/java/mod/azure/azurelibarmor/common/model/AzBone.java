package mod.azure.azurelibarmor.common.model;

import java.util.List;

/// Stub for AzureLibArmor's AzBone.
public class AzBone {
    public void setRotX(int x) {}
    public void setRotY(int y) {}
    public void setRotZ(int z) {}
    public int getRotX() { return 0; }
    public int getRotY() { return 0; }
    public int getRotZ() { return 0; }
    public boolean isHidden() { return false; }
    public void setHidden(boolean hidden) {}
    public boolean isHidingChildren() { return false; }
    public List<AzBone> getChildBones() { return List.of(); }
    public float getScaleX() { return 1.0F; }
    public float getScaleY() { return 1.0F; }
    public float getScaleZ() { return 1.0F; }
    public float getPosX() { return 0; }
    public float getPosY() { return 0; }
    public float getPosZ() { return 0; }
    public AzBoneSnapshot getInitialAzSnapshot() { return new AzBoneSnapshot(); }
    public String getName() { return ""; }
    public java.util.List<mod.azure.azurelibarmor.common.cache.object.GeoCube> getCubes() { return java.util.List.of(); }
}
