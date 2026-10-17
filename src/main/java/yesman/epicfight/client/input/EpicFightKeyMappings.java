package yesman.epicfight.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;

import org.jetbrains.annotations.NotNull;
import yesman.epicfight.generated.LangKeys;
import yesman.epicfight.platform.client.ClientModPlatformProvider;

import java.util.ArrayList;
import java.util.List;

public class EpicFightKeyMappings {

    // GUI key-mappings
    public static final KeyMapping WEAPON_INNATE_SKILL_TOOLTIP =
            registerKey(new KeyMapping(
                    LangKeys.KEY_SHOW_TOOLTIP,
                    InputConstants.Type.KEYSYM,
                    InputConstants.KEY_LSHIFT,
                    EpicFightInputCategories.GUI
            ));

    public static final KeyMapping SKILL_EDIT =
            registerKey(new KeyMapping(
                    LangKeys.KEY_SKILL_GUI,
                    InputConstants.Type.KEYSYM,
                    InputConstants.KEY_K,
                    EpicFightInputCategories.GUI
            ));

    public static final KeyMapping OPEN_CONFIG_SCREEN =
            registerKey(new KeyMapping(
                    LangKeys.KEY_CONFIG,
                    InputConstants.Type.KEYSYM,
                    -1,
                    EpicFightInputCategories.GUI
            ));

    public static final KeyMapping OPEN_EMOTE_WHEEL =
            registerKey(new KeyMapping(
                    LangKeys.KEY_EMOTE,
                    InputConstants.Type.KEYSYM,
                    InputConstants.KEY_Y,
                    EpicFightInputCategories.GUI
            ));

    // In-game keymappings
    public static final KeyMapping DODGE =
            registerKey(new CombatKeyMapping(
                    LangKeys.KEY_DODGE,
                    InputConstants.KEY_LALT,
                    EpicFightInputCategories.COMBAT
            ));

    public static final KeyMapping GUARD =
            registerKey(new CombatKeyMapping(
                    LangKeys.KEY_GUARD,
                    InputConstants.Type.MOUSE,
                    InputConstants.MOUSE_BUTTON_RIGHT,
                    EpicFightInputCategories.COMBAT
            ));

    public static final KeyMapping ATTACK =
            registerKey(new CombatKeyMapping(
                    LangKeys.KEY_ATTACK,
                    InputConstants.Type.MOUSE,
                    InputConstants.MOUSE_BUTTON_LEFT,
                    EpicFightInputCategories.COMBAT
            ));

    public static final KeyMapping WEAPON_INNATE_SKILL =
            registerKey(new CombatKeyMapping(
                    LangKeys.KEY_WEAPON_INNATE_SKILL,
                    InputConstants.Type.MOUSE,
                    InputConstants.MOUSE_BUTTON_LEFT,
                    EpicFightInputCategories.COMBAT
            ));

    public static final KeyMapping MOVER_SKILL =
            registerKey(new CombatKeyMapping(
                    LangKeys.KEY_MOVER_SKILL,
                    InputConstants.KEY_SPACE,
                    EpicFightInputCategories.COMBAT
            ));

    public static final KeyMapping SWITCH_MODE =
            registerKey(new CombatKeyMapping(
                    LangKeys.KEY_SWITCH_MODE,
                    InputConstants.Type.KEYSYM,
                    InputConstants.KEY_R,
                    EpicFightInputCategories.COMBAT
            ));

    public static final KeyMapping LOCK_ON =
            registerKey(new CombatKeyMapping(
                    LangKeys.KEY_LOCK_ON,
                    InputConstants.Type.KEYSYM,
                    InputConstants.KEY_G,
                    EpicFightInputCategories.CAMERA
            ));

    public static final KeyMapping LOCK_ON_SHIFT_LEFT =
            registerKey(new CombatKeyMapping(
                    LangKeys.KEY_LOCK_ON_SHIFT_LEFT,
                    InputConstants.Type.KEYSYM,
                    InputConstants.KEY_LEFT,
                    EpicFightInputCategories.CAMERA
            ));

    public static final KeyMapping LOCK_ON_SHIFT_RIGHT =
            registerKey(new CombatKeyMapping(
                    LangKeys.KEY_LOCK_ON_SHIFT_RIGHT,
                    InputConstants.Type.KEYSYM,
                    InputConstants.KEY_RIGHT,
                    EpicFightInputCategories.CAMERA
            ));

    public static final KeyMapping LOCK_ON_SHIFT_FREELY =
            registerKey(new CombatKeyMapping(
                    LangKeys.KEY_LOCK_ON_SHIFT_FREELY,
                    InputConstants.Type.MOUSE,
                    InputConstants.MOUSE_BUTTON_MIDDLE,
                    EpicFightInputCategories.CAMERA
            ));

    // Systemical key mappings especially for debugging
    public static final KeyMapping SWITCH_VANILLA_MODEL_DEBUGGING =
            registerKey(new KeyMapping(
                    LangKeys.KEY_SWITCH_VANILLA_MODEL_DEBUG,
                    InputConstants.Type.KEYSYM,
                    -1,
                    EpicFightInputCategories.SYSTEM
            ));


    private static List<@NotNull KeyMapping> keyMappings;

    private static @NotNull KeyMapping registerKey(@NotNull KeyMapping keyMapping) {
        if (keyMappings == null) {
            keyMappings = new ArrayList<>();
        }
        keyMappings.add(keyMapping);
        return keyMapping;
    }

    public static void registerKeys() {
        for (KeyMapping keyMapping : keyMappings) {
            ClientModPlatformProvider.get().keyMappingRegistrar().registerKeyMapping(keyMapping);
        }
    }
}
