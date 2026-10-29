package dev.isxander.controlify.screenop;

import java.util.function.Function;

/// Stub for Controlify's ScreenProcessorProvider.
public class ScreenProcessorProvider {
    public static <T extends net.minecraft.client.gui.screens.Screen> void registerProvider(Class<T> screenClass, Function<T, ScreenProcessor<T>> factory) {}
}
