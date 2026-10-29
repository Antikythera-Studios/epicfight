package yesman.epicfight.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;


import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import yesman.epicfight.api.client.input.InputManager;
import yesman.epicfight.api.client.input.action.InputAction;
import yesman.epicfight.mixin.client.KeyMappingAccessor;

/// Internal utility for simplified input checks.
/// Consumers should avoid calling these methods, as they may break in future versions.
///
/// @see InputManager
@ApiStatus.Internal
public final class InputUtils {
    private InputUtils() {
    }

    /// Gets the [InputConstants.Key] bound to a [KeyMapping].
    /// On Fabric, KeyMapping.getKey() is not available, so we use the accessor mixin.
    public static InputConstants.Key getKey(@NotNull KeyMapping keyMapping) {
        return ((KeyMappingAccessor) keyMapping).epicfight$getKey();
    }

    /// Handles firing the [Object.InteractionKeyMappingTriggered] input event for keyboard/mouse actions
    /// and runs the callback only if the event is not canceled.
    public static void runKeyboardMouseEvent(@NotNull InputAction action, @NotNull Runnable handler) {
        final KeyMapping keyMapping = action.keyMapping();

        final InputConstants.Key key = getKey(keyMapping);
        final boolean isMouse = InputConstants.Type.MOUSE == key.getType();

        final int mouseButton = isMouse ? key.getValue() : -1;

        if (checkInteractionKeyUsable(mouseButton, keyMapping)) {
            handler.run();
        }
    }

    public static void sneakingTick(boolean isSneaking, float sneakingSpeedMultiplier) {
        final LocalPlayer localPlayer = Minecraft.getInstance().player;
        if (localPlayer != null) {
            sneakingTick(localPlayer, isSneaking, sneakingSpeedMultiplier);
        }
    }

    /// Checks if the given key mapping is interaction key (block or entity interaction) and triggers
    /// [Object.InteractionKeyMappingTriggered] event
    public static boolean checkInteractionKeyUsable(int mouseButton, KeyMapping keyMapping) {
        Options option = Minecraft.getInstance().options;

        // On Fabric, Epic Fight's CombatKeyMapping instances are separate objects from
        // the vanilla option key mappings, so these identity checks will be false for
        // Epic Fight's combat keys. This is intentional — Epic Fight handles its own
        // input cancellation via mixins on Minecraft.startAttack / startUseItem.
        // The NeoForge InteractionKeyMappingTriggered event is not needed on Fabric
        // because the cancellation is handled at the Minecraft class level.
        if (
            keyMapping == option.keyAttack ||
                keyMapping == option.keyUse ||
                keyMapping == option.keyPickItem
        ) {
            return false;
        }

        return true;
    }

    /// Currently, this calls [Input#tick] without performing any additional logic.
    /// This abstraction was introduced to allow calling it without depending on the vanilla Minecraft [Input],
    /// enabling Epic Fight to introduce changes in future updates if necessary to support controllers.
    public static void sneakingTick(@NotNull LocalPlayer player, boolean isSneaking, float sneakingSpeedMultiplier) {
        player.input.tick(isSneaking, sneakingSpeedMultiplier);
    }
}
