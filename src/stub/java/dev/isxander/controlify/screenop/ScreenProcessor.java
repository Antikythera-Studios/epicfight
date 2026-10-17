package dev.isxander.controlify.screenop;

import dev.isxander.controlify.controller.ControllerEntity;
import net.minecraft.client.gui.screens.Screen;

/// Stub for Controlify's ScreenProcessor.
public class ScreenProcessor<T extends Screen> {
    protected final T screen;

    public ScreenProcessor(T screen) {
        this.screen = screen;
    }

    public void onWidgetRebuild() {}
    public void handleButtons(ControllerEntity controller) {}
    public void playClackSound() {}
}
