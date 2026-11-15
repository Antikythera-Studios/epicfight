package dev.isxander.controlify.api.bind;

import dev.isxander.controlify.bindings.BindContext;
import net.minecraft.network.chat.Component;

/// Stub for Controlify's InputBindingBuilder.
public class InputBindingBuilder {
    public InputBindingBuilder justPressed() { return this; }
    public InputBindingBuilder justReleased() { return this; }
    public InputBindingBuilder pressed() { return this; }
    public InputBindingBuilder analogue() { return this; }
    public InputBindingBuilder category(Component category) { return this; }
    public InputBindingBuilder name(Component name) { return this; }
    public InputBindingBuilder id(net.minecraft.resources.ResourceLocation id) { return this; }
    public InputBindingBuilder id(String id) { return this; }
    public InputBindingBuilder allowedContexts(BindContext... contexts) { return this; }
    public InputBindingBuilder radialCandidate(net.minecraft.resources.ResourceLocation id) { return this; }
    public InputBindingBuilder always() { return this; }
    public InputBindingBuilder addKeyCorrelation(net.minecraft.client.KeyMapping key) { return this; }
    public InputBindingBuilder description(net.minecraft.network.chat.Component desc) { return this; }
    public InputBinding build() { return new InputBinding(); }
}
