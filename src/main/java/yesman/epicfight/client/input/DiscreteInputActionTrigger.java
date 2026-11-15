package yesman.epicfight.client.input;

import net.minecraft.client.KeyMapping;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import yesman.epicfight.api.client.input.action.EpicFightInputAction;
import yesman.epicfight.api.client.input.action.InputAction;
import yesman.epicfight.api.client.input.controller.ControllerBinding;
import yesman.epicfight.api.client.input.controller.EpicFightControllerModProvider;
import yesman.epicfight.api.client.input.controller.IEpicFightControllerMod;
import yesman.epicfight.api.client.input.DiscreteActionHandler;
import yesman.epicfight.api.client.input.InputManager;

/// Handles triggering of a discrete (one-time) [InputAction]
/// based on the current input state.
///
/// Consumers of this API provide only the "what to do" for each action;
/// this class determines the "when" to trigger it.
///
/// Internally, it supports both vanilla keyboard/mouse input and third-party controllers.
///
/// **Note:** This is an internal API.
/// Consumers should prefer using higher-level components
/// such as [InputManager] unless direct access is truly required.
@ApiStatus.Internal
public final class DiscreteInputActionTrigger {
    private DiscreteInputActionTrigger() {
    }

    @Nullable
    private static IEpicFightControllerMod getControllerModApi() {
        return EpicFightControllerModProvider.get();
    }

    /// Called on every client tick to potentially trigger the provided callback for a given input action.
    ///
    /// Determines **when** to trigger the action; consumers define **how** it executes.
    /// For example, for [EpicFightInputAction#OPEN_SKILL_SCREEN], this method decides when to call
    /// the callback that opens the screen, but not how the screen is opened.
    ///
    /// Consumers do not need to know any keyboard/mouse or controller input internals.
    ///
    /// @param action  The input action to monitor.
    /// @param handler The callback to run when the action triggers.
    public static void triggerOnPress(@NotNull InputAction action, @NotNull DiscreteActionHandler handler) {
        final IEpicFightControllerMod controllerMod = getControllerModApi();
        final KeyMapping keyMapping = action.keyMapping();
        if (controllerMod == null) {
            handleKeyboardAndMouse(keyMapping, handler);
            return;
        }

        switch (controllerMod.getInputMode()) {
            case MIXED -> action.controllerBinding()
                    .ifPresentOrElse(
                            controllerBinding -> {
                                final boolean handled = handleController(controllerBinding, handler);
                                if (!handled) {
                                    handleKeyboardAndMouse(keyMapping, handler);
                                }
                            },
                            () -> handleKeyboardAndMouse(keyMapping, handler)
                    );
            case CONTROLLER -> action.controllerBinding()
                    .ifPresentOrElse(
                            controllerBinding -> handleController(controllerBinding, handler),
                            () -> handleKeyboardAndMouse(keyMapping, handler)
                    );
            case KEYBOARD_MOUSE -> handleKeyboardAndMouse(keyMapping, handler);
        }
    }

    private static void handleKeyboardAndMouse(@NotNull KeyMapping keyMapping, @NotNull DiscreteActionHandler handler) {
        while (keyMapping.consumeClick()) {
            handler.onAction(createContext(false));
        }
    }

    private static boolean handleController(@NotNull ControllerBinding controllerBinding, @NotNull DiscreteActionHandler handler) {
        if (controllerBinding.isDigitalJustPressed()) {
            handler.onAction(createContext(true));
            return true;
        }
        return false;
    }

    @NotNull
    private static DiscreteActionHandler.Context createContext(boolean triggeredByController) {
        return new DiscreteActionHandler.Context(triggeredByController);
    }
}
