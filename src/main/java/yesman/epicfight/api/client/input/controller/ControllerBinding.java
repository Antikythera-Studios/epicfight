package yesman.epicfight.api.client.input.controller;
import net.minecraft.client.Minecraft;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/// Represents a controller button or analogue stick axis.
/// Provides current and previous state, analogue/digital values, and metadata.
///
/// Serves a similar role to Minecraft's vanilla [net.minecraft.client.KeyMapping],
/// but for controller input instead of keyboard or mouse.
///
/// ### Input signals
///
/// Controller inputs can be either analogue or digital:
///
/// - **Analogue:** Inputs with a continuous range of values (e.g., 0.0 to 1.0), such as the movement axes
/// of a stick or triggers.
/// - **Digital:** Inputs that are binary,
/// such as buttons or stick presses (e.g., L3/R3), which are either pressed or released.
///
/// **Note:** A single physical control can produce multiple input types.
/// For example:
///
/// - moving a left stick generates analogue signals (X/Y axes)
/// - pressing the stick down generates a separate digital input
public interface ControllerBinding {
    /// The ID of the binding (e.g., `epicfight:attack`).
    ///
    /// @return the ID
    @NotNull
    ResourceLocation id();

    /// Returns whether the digital state is currently active in this tick.
    ///
    /// @return the current digital state, this tick.
    boolean isDigitalActiveNow();

    /// Returns whether the digital state was active in the previous tick.
    ///
    /// @return the previous digital state, 1 tick ago.
    boolean wasDigitalActivePreviously();

    /// Returns whether the digital state was just pressed.
    ///
    /// @return true if the binding is pressed this tick and not pressed the previous tick.
    boolean isDigitalJustPressed();

    /// Returns whether the digital state was just released.
    ///
    /// @return true if the binding is not pressed this tick and pressed the previous tick.
    boolean isDigitalJustReleased();

    /// Returns the current analogue input value.
    ///
    /// @return the current analogue value in the range `0.0`–`1.0`, representing this tick's state.
    float getAnalogueNow();

    /// Simulates a press of this binding.
    ///
    /// Can be used for GUI interactions or synthetic input from other systems.
    void emulatePress();

    /// Returns a unique ID for the physical control.
    /// The returned object must have a stable [Object#equals]/[Object#hashCode] implementation.
    ///
    /// @return a unique and comparable identifier for the physical control
    @NotNull
    Object physicalInputId();

    /// Returns whether both controller bindings refer to the same physical control.
    default boolean isBoundToSamePhysicalInput(@NotNull ControllerBinding other) {
        return physicalInputId().equals(other.physicalInputId());
    }
}
