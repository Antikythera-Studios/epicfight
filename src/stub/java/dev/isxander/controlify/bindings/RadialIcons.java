package dev.isxander.controlify.bindings;
import dev.isxander.controlify.utils.render.CGuiPose;
public class RadialIcons {
    public static final Object LOOK_INPUT_MODIFIER = new Object();
    public static RadialIcons inGame() { return new RadialIcons(); }

    @FunctionalInterface
    public interface IconRenderer {
        void render(Object graphics, float x, float y, float tickDelta);
    }

    public static void registerIcon(net.minecraft.resources.ResourceLocation id, IconRenderer renderer) {}
}
