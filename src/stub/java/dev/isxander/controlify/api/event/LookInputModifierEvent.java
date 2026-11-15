package dev.isxander.controlify.api.event;

import org.joml.Vector2f;

/// Stub event for Controlify's LOOK_INPUT_MODIFIER.
/// The real Controlify API provides this event to modify player look input.
/// This stub exists so the code compiles without the real Controlify dependency.
/// When Controlify is re-enabled as a dependency, this stub will be replaced
/// by the real API class.
public interface LookInputModifierEvent {
    /// Returns the current look input vector.
    /// x = horizontal (yaw), y = vertical (pitch).
    Vector2f lookInput();
}
