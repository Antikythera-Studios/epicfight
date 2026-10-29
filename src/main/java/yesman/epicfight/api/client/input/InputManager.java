package yesman.epicfight.api.client.input;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import yesman.epicfight.api.client.input.action.InputAction;
import yesman.epicfight.api.client.input.controller.ControllerBinding;
import yesman.epicfight.api.client.input.controller.EpicFightControllerModProvider;
import yesman.epicfight.api.client.input.controller.IEpicFightControllerMod;
import yesman.epicfight.client.input.DiscreteInputActionTrigger;
import yesman.epicfight.client.input.InputUtils;

import java.util.Optional;
import java.util.function.Function;

/// High-level input API that abstracts direct interactions with [KeyMapping]
/// and supports controllers if an Epic Fight controller mod implementation is present
/// (see [EpicFightControllerModProvider]).
///
/// Use this class whenever possible to ensure input works consistently across
/// keyboard/mouse and supported controllers.
///
/// **Warning:** This API is currently marked as experimental.
/// This designation does not imply that the implementation is of an **'experimental'** quality,
/// but rather indicates that classes, methods, and fields may be subject to renaming, relocation, or removal.
/// The Epic Fight team reserves the right to modify or completely remove any components of the API at any time,
/// without prior notice.
@ApiStatus.Experimental
public final class InputManager {
    private InputManager() {
    }

    @Nullable
    private static IEpicFightControllerMod getControllerModApi() {
        return EpicFightControllerModProvider.get();
    }

    /// Returns the current input mode (keyboard/mouse or controller).
    /// Equivalent to [IEpicFightControllerMod#getInputMode()], but guaranteed
    /// to return a non-null value even if no controller mod is present.
    @NotNull
    public static InputMode getInputMode() {
        final IEpicFightControllerMod controllerMod = getControllerModApi();
        return controllerMod == null ? InputMode.KEYBOARD_MOUSE : controllerMod.getInputMode();
    }

    /// Checks if controller or gamepad input is currently supported.
    ///
    /// **Note:** The [InputMode#MIXED] mode supports both controller and keyboard/mouse input at the same time.
    /// Returning `true` here does not necessarily mean the input mode is exclusively [InputMode#CONTROLLER].
    ///
    /// @return `true` if a controller mod is present and the current input mode allows controller input.
    /// @see InputMode
    public static boolean supportsControllerInput() {
        return getInputMode().supportsController();
    }

    /// Returns whether the given input action is active during this tick.
    ///
    /// The behavior differs depending on the input source:
    ///
    /// - **Keyboard/Mouse:** Follows Minecraft's internal behavior.
    ///   May return `false` while a screen is open, even if the physical key is held down.
    /// - **Controller:** The behavior is handled externally and is irrelevant to this method.
    ///   It is usually determined by an input context during the
    ///   controller binding registration (third-party API),
    ///   which decides whether to return `false` or `true` when the physical input is down.
    ///
    /// If no controller mod is present, only the [KeyMapping] (Keyboard/Mouse) is checked.
    /// This is usually useful for in-game continuous actions.
    /// It should not be used while a screen is open.
    ///
    /// @param action the input action to check
    /// @see ControllerBinding
    public static boolean isActionActive(@NotNull InputAction action) {
        return checkAction(action, InputManager::isKeyDown);
    }

    /// Returns whether the given input action is currently physically active this tick.
    ///
    /// The behavior differs depending on the input source:
    ///  - **Keyboard/Mouse:** Always checks the physical key state, ignoring vanilla GUI filtering
    ///    and bypassing the [mouse multiple-keybind sharing bug](https://github.com/Epic-Fight/epicfight/issues/2174)
    ///    (present in versions before 1.21.10).
    /// - **Controller:** Similarly to [#isActionActive], the behavior is handled externally
    ///   and is irrelevant to this method.
    ///
    /// If no controller mod is present, only the [KeyMapping] (Keyboard/Mouse) is checked.
    /// This is usually useful for GUI continuous actions.
    /// It should not be used in-game with no screens.
    ///
    /// @param action the input action to check
    /// @see #isActionActive
    public static boolean isActionPhysicallyActive(@NotNull InputAction action) {
        return checkAction(action, InputManager::isPhysicalKeyDown);
    }

    /// Shared internal utility between [#isActionActive] and [#isActionPhysicallyActive] to handle the differences.
    private static boolean checkAction(@NotNull InputAction action, @NotNull Function<KeyMapping, Boolean> keyboardCheck) {
        final IEpicFightControllerMod controllerMod = getControllerModApi();
        if (controllerMod == null) {
            return keyboardCheck.apply(action.keyMapping());
        }

        return switch (controllerMod.getInputMode()) {
            case KEYBOARD_MOUSE -> keyboardCheck.apply(action.keyMapping());
            case CONTROLLER -> action.controllerBinding()
                    .map(ControllerBinding::isDigitalActiveNow)
                    .orElse(keyboardCheck.apply(action.keyMapping()));
            case MIXED -> keyboardCheck.apply(action.keyMapping())
                    || action.controllerBinding()
                    .map(ControllerBinding::isDigitalActiveNow)
                    .orElse(false);
        };
    }

    /// Called on every client tick to potentially trigger the provided callback for a given input action.
    ///
    /// @param action  The input action to monitor and trigger.
    /// @param handler The callback to invoke when the action triggers.
    /// @see DiscreteInputActionTrigger#triggerOnPress Internal implementation details.
    public static void triggerOnPress(@NotNull InputAction action, @NotNull DiscreteActionHandler handler) {
        DiscreteInputActionTrigger.triggerOnPress(action, handler);
    }

    /// Convenience overload of [#triggerOnPress(InputAction, DiscreteActionHandler)]
    /// for callbacks that do not require the [DiscreteActionHandler.Context].
    public static void triggerOnPress(@NotNull InputAction action, @NotNull Runnable runnable) {
        triggerOnPress(action, (context) -> runnable.run());
    }

    /// Checks whether the given input action is assigned to the same key / button as another action.
    ///
    /// For keyboard/mouse, this compares the key codes; for controllers, it compares the digital button.
    /// **Note:** [InputMode#MIXED] is currently unsupported and its behavior is undefined.
    ///
    /// @param action  the first input action
    /// @param action2 the second input action
    /// @return `true` if both actions are triggered by the same key or controller button; `false` otherwise
    public static boolean isBoundToSamePhysicalInput(@NotNull InputAction action, @NotNull InputAction action2) {
        final IEpicFightControllerMod controllerMod = getControllerModApi();
        if (controllerMod != null && controllerMod.getInputMode() == InputMode.CONTROLLER) {
            final Optional<ControllerBinding> optionalControllerBinding = action.controllerBinding();
            final Optional<ControllerBinding> optionalControllerBinding2 = action2.controllerBinding();
            if (optionalControllerBinding.isPresent() && optionalControllerBinding2.isPresent()) {
                return optionalControllerBinding.get().isBoundToSamePhysicalInput(optionalControllerBinding2.get());
            }
        }

        final KeyMapping keyMapping1 = action.keyMapping();
        final KeyMapping keyMapping2 = action2.keyMapping();
        return InputUtils.getKey(keyMapping1) == InputUtils.getKey(keyMapping2);
    }

    /// Retrieves the current input state for the current player (client-side).
    ///
    ///  You should use this method instead of depending on the vanilla [Input] directly support controllers.
    ///
    /// The [PlayerInputState] is immutable, so properties cannot be updated directly, for that,
    /// use [InputManager#setInputState].
    ///
    /// **Note:** [InputMode#MIXED] is currently unsupported and its behavior is undefined.
    ///
    /// @param vanillaInput the Minecraft vanilla [Input] which will be mapped to a [PlayerInputState];
    ///                                                                                                                                                                                                                                                                                                                                                                         ignored if using a controller.
    /// @return an immutable [PlayerInputState] representing the current input state.
    /// @see InputManager#setInputState
    @NotNull
    public static PlayerInputState getInputState(@NotNull Input vanillaInput) {
        final IEpicFightControllerMod controllerMod = getControllerModApi();
        if (controllerMod != null && controllerMod.getInputMode() == InputMode.CONTROLLER) {
            return controllerMod.getInputState();
        }

        return PlayerInputState.fromVanillaInput(vanillaInput);
    }

    /// Convenience overload of [#getInputState(Input)] that requires the full [LocalPlayer],
    /// which is needed to read the vanilla [Input] used for non-controller inputs.
    ///
    /// @param localPlayer the player whose vanilla [Input] will be read; ignored when using a controller.
    /// @return an immutable [PlayerInputState] representing the current input state.
    @NotNull
    public static PlayerInputState getInputState(@NotNull LocalPlayer localPlayer) {
        return getInputState(localPlayer.input);
    }

    /// Updates the current input state for the current player (client-side).
    ///
    /// Consider using this instead of modifying fields in the vanilla [Input] directly
    /// to avoid direct dependency on Minecraft.
    ///
    /// @param inputState the updated input state.
    /// @see InputManager#getInputState
    public static void setInputState(@NotNull PlayerInputState inputState) {
        final LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            final Input input = player.input;
            PlayerInputState.applyToVanillaInput(inputState, input);
        }
    }

    /// Checks whether the vanilla [KeyMapping] is down.
    ///
    /// **Note:** This may report `false` if a Minecraft screen is open, so it respects
    /// Minecraft internals.
    /// The exact behavior varied from one Minecraft version to another.
    private static boolean isKeyDown(@NotNull KeyMapping keyMapping) {
        final boolean isDown = keyMapping.isDown();
        if (!isDown && InputUtils.getKey(keyMapping).getType() == InputConstants.Type.MOUSE) {
            // TODO: (WORKAROUND) Remove this entire "if" statement when
            //  porting to Minecraft 1.21.10 or a newer version.
            //  This exists only due to inconsistent behavior in older Minecraft versions,
            //  such as 1.21.1 and 1.20.1.
            //  It fixes an issue where the weapon's innate skill fails to trigger
            //  even though the left mouse button is actually pressed.
            //  In vanilla Minecraft, "KeyMapping#isDown" incorrectly reports "false"
            //  when multiple keybindings share the same physical mouse button.
            //  (This is not an issue with keyboard inputs.)
            //  When porting to 1.21.10 or 1.22, test the weapon's innate skill
            //  without this condition.
            //  If it works correctly, remove this "if" block.
            //  For more details, see: https://github.com/Epic-Fight/epicfight/issues/2174
            return isPhysicalKeyDown(keyMapping);
        }
        return isDown;
    }

    /// Checks whether the physical key is actually pressed, regardless of Minecraft's internal state.
    ///
    /// This method does not respect any Minecraft behavior and may return `true` even
    /// if a screen is open, for example.
    ///
    /// Consumers or addons should **never** rely on this internal method unless absolutely necessary.
    /// For instance, Epic Fight still uses it internally as a workaround for a specific issue.
    ///
    /// This method serves as a workaround for an issue where the weapon's innate skill fails to trigger
    /// when bound to the left mouse button.
    ///
    /// Since other keybindings may share the same physical input,
    /// Minecraft incorrectly reports the key as `false`, even though it should be `true`.
    ///
    /// This issue occurs in versions `1.21.1` and `1.20.1` but is fixed in 1.21.10 and newer.
    ///
    /// Once migration to a newer version is complete, this workaround should be removed entirely
    /// while ensuring the weapon's innate skill continues to function correctly.
    ///
    /// For more details, see [Issue #2174](https://github.com/Epic-Fight/epicfight/issues/2174).
    ///
    /// Note: At the time of writing, this workaround is confirmed to be unnecessary in 1.21.10,
    /// but may still (though unlikely) be required in 1.22 or later versions.
    ///
    /// This is also useful when a screen is open,
    /// since [#isKeyDown(KeyMapping)] will return `false`.
    ///
    /// See [issue #2170](https://github.com/Epic-Fight/epicfight/issues/2170) for details.
    @ApiStatus.Internal
    private static boolean isPhysicalKeyDown(@NotNull KeyMapping keyMapping) {
        if (!RenderSystem.isOnRenderThread()) {
            return keyMapping.isDown();
        }

        final InputConstants.Key key = InputUtils.getKey(keyMapping);
        final int keyValue = key.getValue();
        final long windowPointer = Minecraft.getInstance().getWindow().getWindow();

        if (key.getType() == InputConstants.Type.KEYSYM) {
            return keyValue != GLFW.GLFW_KEY_UNKNOWN && GLFW.glfwGetKey(windowPointer, keyValue) > 0;
        } else if (key.getType() == InputConstants.Type.MOUSE) {
            return GLFW.glfwGetMouseButton(windowPointer, keyValue) > 0;
        }
        return false;
    }
}
