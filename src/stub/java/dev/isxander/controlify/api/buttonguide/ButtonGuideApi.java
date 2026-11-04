package dev.isxander.controlify.api.buttonguide;
import dev.isxander.controlify.api.bind.InputBindingSupplier;
public class ButtonGuideApi {
    public static ButtonGuideApi getInstance() { return new ButtonGuideApi(); }
    public void guideRegistries() {}
    public void registerIcon(net.minecraft.resources.ResourceLocation id, Object consumer) {}
    public static void addGuideToButton(Object button, InputBindingSupplier binding, ButtonGuidePredicate predicate) {}
}
