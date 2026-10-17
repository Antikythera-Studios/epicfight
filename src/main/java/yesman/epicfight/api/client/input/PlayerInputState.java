package yesman.epicfight.api.client.input;
import net.minecraft.client.Minecraft;

import net.minecraft.client.player.Input;
import net.minecraft.world.phys.Vec2;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/// Represents the abstract state of the player input (client-side).
/// Mirrors the vanilla Minecraft [Input] class.
public record PlayerInputState(
        float leftImpulse,
        float forwardImpulse,
        boolean up,
        boolean down,
        boolean left,
        boolean right,
        boolean jumping,
        boolean sneaking
) {
    public static PlayerInputState fromVanillaInput(Input input) {
        return new PlayerInputState(
                input.leftImpulse, input.forwardImpulse,
                input.up, input.down,
                input.left, input.right,
                input.jumping, input.shiftKeyDown
        );
    }

    /// Applies the values from a [PlayerInputState] to a vanilla [Input] instance.
    ///
    /// **Note:** Updating the vanilla [Input] fields has side effects, so this change is **not immutable**.
    ///
    /// @param updated the new input state to apply
    /// @param input   the existing vanilla Input instance to update
    public static Input applyToVanillaInput(@NotNull PlayerInputState updated, @NotNull Input input) {
        if (input.leftImpulse != updated.leftImpulse()) input.leftImpulse = updated.leftImpulse();
        if (input.forwardImpulse != updated.forwardImpulse()) input.forwardImpulse = updated.forwardImpulse();
        if (input.up != updated.up()) input.up = updated.up();
        if (input.down != updated.down()) input.down = updated.down();
        if (input.left != updated.left()) input.left = updated.left();
        if (input.right != updated.right()) input.right = updated.right();
        if (input.jumping != updated.jumping()) input.jumping = updated.jumping();
        if (input.shiftKeyDown != updated.sneaking()) input.shiftKeyDown = updated.sneaking();
        return input;
    }

    public Vec2 getMoveVector() {
        return new Vec2(this.leftImpulse, this.forwardImpulse);
    }

    public boolean hasForwardImpulse() {
        return this.forwardImpulse > 1.0E-5F;
    }

    public @NotNull PlayerInputState copyWith(
            @Nullable Float leftImpulse,
            @Nullable Float forwardImpulse,
            @Nullable Boolean up,
            @Nullable Boolean down,
            @Nullable Boolean left,
            @Nullable Boolean right,
            @Nullable Boolean jumping,
            @Nullable Boolean sneaking
    ) {
        return new PlayerInputState(
                leftImpulse != null ? leftImpulse : this.leftImpulse,
                forwardImpulse != null ? forwardImpulse : this.forwardImpulse,
                up != null ? up : this.up,
                down != null ? down : this.down,
                left != null ? left : this.left,
                right != null ? right : this.right,
                jumping != null ? jumping : this.jumping,
                sneaking != null ? sneaking : this.sneaking
        );
    }

    public @NotNull PlayerInputState withLeftImpulse(float leftImpulse) {
        return copyWith(leftImpulse, null, null, null, null, null, null, null);
    }

    public @NotNull PlayerInputState withForwardImpulse(float forwardImpulse) {
        return copyWith(null, forwardImpulse, null, null, null, null, null, null);
    }

    public @NotNull PlayerInputState withUp(boolean up) {
        return copyWith(null, null, up, null, null, null, null, null);
    }

    public @NotNull PlayerInputState withDown(boolean down) {
        return copyWith(null, null, null, down, null, null, null, null);
    }

    public @NotNull PlayerInputState withLeft(boolean left) {
        return copyWith(null, null, null, null, left, null, null, null);
    }

    public @NotNull PlayerInputState withRight(boolean right) {
        return copyWith(null, null, null, null, null, right, null, null);
    }

    public @NotNull PlayerInputState withJumping(boolean jumping) {
        return copyWith(null, null, null, null, null, null, jumping, null);
    }

    public @NotNull PlayerInputState withSneaking(boolean sneaking) {
        return copyWith(null, null, null, null, null, null, null, sneaking);
    }
}
