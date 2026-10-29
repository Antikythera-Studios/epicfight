package dev.isxander.controlify.bindings;

import net.minecraft.resources.ResourceLocation;
import java.util.function.Predicate;

/// Stub for Controlify's BindContext.
public class BindContext {
    public static BindContext ANY_SCREEN = new BindContext();
    public static BindContext IN_GAME = new BindContext();

    public BindContext() {}

    public BindContext(ResourceLocation id, Predicate<net.minecraft.client.Minecraft> predicate) {}

    public static BindContext of(String category) { return new BindContext(); }
}
