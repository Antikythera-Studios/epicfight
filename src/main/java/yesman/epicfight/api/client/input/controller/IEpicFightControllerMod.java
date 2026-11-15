package yesman.epicfight.api.client.input.controller;
import net.minecraft.client.Minecraft;

import org.jetbrains.annotations.NotNull;
import yesman.epicfight.api.client.input.InputMode;
import yesman.epicfight.api.client.input.PlayerInputState;
import yesman.epicfight.api.client.input.action.EpicFightInputAction;

/// Represents an integration layer for third-party controller mods used by Epic Fight.
///
/// This interface must be implemented by any external controller mod to provide
/// controller input support for Epic Fight.
///
/// It acts as a bridge between the controller mod's input system
/// (e.g., Controlify, Controllable, MidnightControls) and Epic Fight's input handling logic.
///
/// Epic Fight relies on this interface to determine and manage the current [InputMode].
/// Since input mode management is not part of the vanilla
/// Minecraft input system, controller mods must supply their own implementation.
///
/// **Note:** This interface exposes low-level controller integration.
/// Most consumers should use a higher-level abstraction unless direct access is necessary for functionality
/// that cannot be achieved otherwise.
public interface IEpicFightControllerMod {
    /// Returns the controller mod's display name (e.g., `Controlify`).
    /// Intended for logging or debugging only; should not influence gameplay logic or be used as a workaround.
    String getModName();

    /// Returns the current input mode (keyboard/mouse or controller).
    @NotNull
    InputMode getInputMode();

    /// Retrieves the current input state.
    /// This is used internally by Epic Fight to perform actions such as [EpicFightInputAction#DODGE].
    ///
    /// @return a [PlayerInputState] representing the current input state.
    @NotNull
    PlayerInputState getInputState();
}
