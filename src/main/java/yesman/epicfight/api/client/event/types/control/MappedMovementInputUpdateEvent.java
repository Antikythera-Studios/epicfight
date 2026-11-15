package yesman.epicfight.api.client.event.types.control;
import net.minecraft.client.Minecraft;

import net.minecraft.client.player.Input;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import yesman.epicfight.api.client.input.InputManager;
import yesman.epicfight.api.client.input.PlayerInputState;
import yesman.epicfight.api.event.LivingEntityPatchEvent;
import yesman.epicfight.client.input.InputUtils;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;

/// Client-side only.
///
/// MUST NOT be called on a dedicated server — doing so will crash the server.
public class MappedMovementInputUpdateEvent extends LivingEntityPatchEvent {
    /// **DEPRECATED:** This field is kept for backward compatibility with existing Epic Fight addons.
    /// Consumers should migrate to using [#getInputState()] and
    /// [InputManager#setInputState] instead, which fully support controller input.
    ///
    /// @see #getMovementInput()
    @SuppressWarnings("DeprecatedIsStillUsed")
    @NotNull
    @Deprecated
    private final Input movementInput;

    // This was set as @Nullable to introduce PlayerInputState in a backward compatible way.
    // As soon as we introduce a breaking change by removing the existing constructor that sets movementInput, we
    // should set this to @NotNull.
    @Nullable
    private final PlayerInputState inputState;

    /// **DEPRECATED:** Please use the new constructor that accepts [PlayerInputState] to support controllers.
    /// This constructor is only kept for backward compatibility with addons that rely on vanilla [Input].
    ///
    /// @see InputManager
    @Deprecated
    @ApiStatus.Internal
    public MappedMovementInputUpdateEvent(LocalPlayerPatch playerPatch, @NotNull Input input) {
        super(playerPatch);
        this.movementInput = input;
        this.inputState = null;
    }

    /// Creates a new [MappedObject] with an immutable [PlayerInputState].
    /// Use this constructor to fully support controllers.
    ///
    /// @param playerPatch the patched local player
    /// @param inputState  the current input state
    @ApiStatus.Internal
    public MappedMovementInputUpdateEvent(LocalPlayerPatch playerPatch, @NotNull PlayerInputState inputState) {
        super(playerPatch);
        this.inputState = inputState;
        // DEPRECATED: Still set the vanilla Input for backward compatibility to avoid Epic Fight addons breakage.
        // Not setting this, may break any consumers that depend on the deprecated MovementObject#getMovementInput() method.
        this.movementInput = playerPatch.getOriginal().input;
    }

    /// **DEPRECATED:** Use [#getInputState()] instead to support controllers.
    /// Note that [PlayerInputState] is immutable. You cannot directly modify the fields
    /// like in vanilla [Input]. To apply changes, use [InputManager#setInputState].
    ///
    /// @return the vanilla input instance (deprecated)
    /// @see InputManager#setInputState 
    @Deprecated
    public @NotNull Input getMovementInput() {
        return this.movementInput;
    }

    /// Returns the current input state for the player.
    /// This method abstracts over vanilla [Input] and controller input, providing a unified,
    /// immutable [PlayerInputState].
    ///
    /// @return the current player input state
    @NotNull
    public PlayerInputState getInputState() {
        if (inputState == null) {
            return InputManager.getInputState(movementInput);
        }
        return inputState;
    }

    /// Currently, this calls [Input#tick] without performing any additional logic.
    /// This abstraction was introduced to allow calling it without depending on the vanilla Minecraft [Input],
    /// enabling Epic Fight to introduce changes in future updates if necessary to support controllers.
    @ApiStatus.Experimental
    public void sneakingTick(boolean isSneaking, float sneakingSpeedMultiplier) {
        InputUtils.sneakingTick(isSneaking, sneakingSpeedMultiplier);
    }
}
