package yesman.epicfight.api.client.input.action;
import net.minecraft.client.Minecraft;

import net.minecraft.client.KeyMapping;
import org.jetbrains.annotations.NotNull;
import yesman.epicfight.api.client.input.controller.ControllerBinding;
import yesman.epicfight.api.client.input.controller.EpicFightControllerModProvider;
import yesman.epicfight.client.input.EpicFightKeyMappings;
import yesman.epicfight.compat.controlify.EpicFightControlifyControllerMod;

import java.util.*;

/// Represents a default set of input actions used in the Epic Fight mod.
///
/// Each action is linked to a corresponding Minecraft vanilla [KeyMapping] or
/// an Epic Fight custom key mapping.
///
/// These mappings only support keyboard and mouse input.
/// Controller input is not directly supported to avoid dependencies on third-party controller mods.
///
/// For implementation details, refer to [#keyMapping()].
public enum EpicFightInputAction implements InputAction {
    ATTACK,
    MOBILITY,
    GUARD,
    DODGE,
    LOCK_ON,
    LOCK_ON_SHIFT_LEFT,
    LOCK_ON_SHIFT_RIGHT,
    LOCK_ON_SHIFT_FREELY,
    SWITCH_MODE,
    WEAPON_INNATE_SKILL,
    WEAPON_INNATE_SKILL_TOOLTIP,
    OPEN_SKILL_SCREEN,
    OPEN_CONFIG_SCREEN,
    OPEN_EMOTE_WHEEL_SCREEN,
    SWITCH_VANILLA_MODEL_DEBUGGING;

    final private int id;

    EpicFightInputAction() {
        this.id = InputAction.ENUM_MANAGER.assign(this);
    }

    @Override
    public int universalOrdinal() {
        return this.id;
    }

    @Override
    @NotNull
    public KeyMapping keyMapping() {
        return switch (this) {
            case ATTACK -> EpicFightKeyMappings.ATTACK;
            case MOBILITY -> EpicFightKeyMappings.MOVER_SKILL;
            case GUARD -> EpicFightKeyMappings.GUARD;
            case DODGE -> EpicFightKeyMappings.DODGE;
            case LOCK_ON -> EpicFightKeyMappings.LOCK_ON;
            case LOCK_ON_SHIFT_LEFT -> EpicFightKeyMappings.LOCK_ON_SHIFT_LEFT;
            case LOCK_ON_SHIFT_RIGHT -> EpicFightKeyMappings.LOCK_ON_SHIFT_RIGHT;
            case LOCK_ON_SHIFT_FREELY -> EpicFightKeyMappings.LOCK_ON_SHIFT_FREELY;
            case SWITCH_MODE -> EpicFightKeyMappings.SWITCH_MODE;
            case WEAPON_INNATE_SKILL -> EpicFightKeyMappings.WEAPON_INNATE_SKILL;
            case WEAPON_INNATE_SKILL_TOOLTIP -> EpicFightKeyMappings.WEAPON_INNATE_SKILL_TOOLTIP;
            case OPEN_SKILL_SCREEN -> EpicFightKeyMappings.SKILL_EDIT;
            case OPEN_CONFIG_SCREEN -> EpicFightKeyMappings.OPEN_CONFIG_SCREEN;
            case OPEN_EMOTE_WHEEL_SCREEN -> EpicFightKeyMappings.OPEN_EMOTE_WHEEL;
            case SWITCH_VANILLA_MODEL_DEBUGGING -> EpicFightKeyMappings.SWITCH_VANILLA_MODEL_DEBUGGING;
        };
    }

    @Override
    public @NotNull Optional<@NotNull ControllerBinding> controllerBinding() {
        if (EpicFightControllerModProvider.get() == null) {
            throw new IllegalStateException("controllerBinding() must not be called when the controller mod is not installed");
        }
        return Optional.of(EpicFightControlifyControllerMod.getBinding(this));
    }
}
