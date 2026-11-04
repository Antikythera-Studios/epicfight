package yesman.epicfight.api.client.input.action;
import net.minecraft.client.Minecraft;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.KeyMapping;
import org.jetbrains.annotations.Nullable;
import yesman.epicfight.api.client.input.controller.ControllerBinding;
import yesman.epicfight.api.utils.ExtensibleEnum;
import yesman.epicfight.api.utils.ExtensibleEnumManager;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/// Represents a client-side input action in Epic Fight mod.
///
/// Each action is associated with:
///
/// - a Minecraft vanilla [KeyMapping] (supports keyboard and mouse only).
/// - a [ControllerBinding], which is an abstraction around the input binding from
///  third-party controller mods (supports controller only).
///
/// **Important:** This class must be called **only on the client**.
@ApiStatus.Experimental
public interface InputAction extends ExtensibleEnum {
    ExtensibleEnumManager<InputAction> ENUM_MANAGER = new ExtensibleEnumManager<>("input_action");

    /// Returns the Minecraft vanilla [KeyMapping] associated with this action.
    ///
    /// **Note:** This only supports keyboard and mouse input and does not support controllers.
    ///
    /// @return the vanilla [KeyMapping] for this action
    /// @see #controllerBinding
    @NotNull
    KeyMapping keyMapping();

    /// Returns the universal controller binding associated with this action, if available.
    ///
    /// This method may return [Optional#empty()] if the action does not support controller input.
    ///
    /// **Important:** Consumers must **not** call this method if the controller mod is not installed,
    /// since the creation of a [ControllerBinding] requires depending on APIs from the controller mod.
    ///
    /// If this was called and the controller mod was not installed, the behavior is undefined and depends
    /// on the implementation details.
    /// Usually a [ClassNotFoundException] or [IllegalStateException] is thrown.
    ///
    /// @return the [ControllerBinding] for this action, or [Optional#empty()] if not supported
    /// @see ControllerBinding
    @NotNull Optional<@NotNull ControllerBinding> controllerBinding();

    /// Returns whether this input action corresponds to a vanilla standard Minecraft input bind.
    ///
    /// Vanilla actions are those defined by the base game (e.g., attack, jump, move),
    /// while non-vanilla actions are custom Epic Fight actions introduced by the mod.
    ///
    /// @return `true` if this action is linked to a vanilla key mapping.
    default boolean isVanilla() {
        return false;
    }

    /// Returns a set of all input actions that are not part of the vanilla Minecraft input bindings.
    ///
    /// @return a set containing all non-vanilla [InputAction]
    /// @see InputAction#isVanilla
    static @NotNull Set<InputAction> nonVanillaActions() {
        Set<InputAction> result = new HashSet<>();
        for (InputAction action : InputAction.ENUM_MANAGER.universalValues()) {
            if (!action.isVanilla()) result.add(action);
        }
        return result;
    }

    /// Gets the input action corresponding to a [KeyMapping].
    ///
    /// @param keyMapping the key mapping; must not be `null`
    /// @return the corresponding action, or null if none matches
    static @Nullable InputAction fromKeyMapping(@NotNull KeyMapping keyMapping) {
        return InputAction.ENUM_MANAGER.universalValues().stream()
                .filter(action -> action.keyMapping() == keyMapping)
                .findFirst()
                .orElse(null);
    }
}
