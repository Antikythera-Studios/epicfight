package dev.isxander.controlify.utils.render;
public class CGuiPose {
    public static CGuiPose ofPush(Object... args) { return new CGuiPose(); }
    public CGuiPose translate(float x, float y) { return this; }
    public CGuiPose scale(float x, float y) { return this; }
    public CGuiPose pop() { return this; }
}
