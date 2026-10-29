package dev.isxander.controlify.api.bind;

import java.util.Optional;

/// Stub for Controlify's InputBinding.
public class InputBinding {
    public boolean digitalNow() { return false; }
    public float analogueNow() { return 0; }
    public boolean justPressed() { return false; }
    public boolean digitalPrev() { return false; }
    public void fakePress() {}
    public java.util.List<Object> getRelevantInputs() { return java.util.List.of(); }
    public Object boundInput() { return null; }
    public boolean justReleased() { return false; }
    public boolean pressed() { return false; }
    public Optional<Boolean> guiPressed() { return Optional.empty(); }
    public net.minecraft.resources.ResourceLocation id() { return null; }
}
