package yesman.epicfight.client.input;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;

import org.jetbrains.annotations.NotNull;
import yesman.epicfight.client.ClientEngine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/// A specialized [KeyMapping] used by Epic Fight to represent combat-related key bindings.
///
/// This enforces that all [KeyMapping#isDown()] or related checks to return `false`
/// whenever the player is **not** in Epic Fight mode.
///
/// **Important:** Other mods or consumers should *not* rely on this behavior.
/// They should explicitly check whether the player is in Epic Fight mode through
/// [yesman.epicfight.client.ClientEngine#isEpicFightMode()] instead of depending on
/// this key mapping's conditional logic.
///
/// This also force setting [KeyConflictContext#IN_GAME],
/// since a [CombatKeyMapping] is usually used for player moves
/// (e.g, dodge, guard, mover skill, epic fight attack).
///
/// Note: The author of this code is not entirely certain about the exact purpose of
/// [KeyConflictContext#IN_GAME].
/// It appears mainly relevant to key modifiers
/// (e.g., distinguishing Shift + E from E) and does not affect the red conflict highlighting
/// in the key bindings menu, since vanilla key mappings do not assign any [KeyConflictContext].
///
/// This class is primarily used as a fallback or metadata reference for compatibility with
/// other mods (hopefully!).
/// Otherwise, it has no meaningful function beyond normal [KeyMapping] behavior.
///
/// Future maintainers should consider refactoring or removing this class
/// if it becomes problematic or a maintenance burden.
public class CombatKeyMapping extends KeyMapping {

    private static final List<CombatKeyMapping> COMBAT_KEY_MAPPINGS = Collections.synchronizedList(new ArrayList<>());

    public static List<CombatKeyMapping> getCombatKeyMappings() {
        return COMBAT_KEY_MAPPINGS;
    }

    public CombatKeyMapping(String description, int code, String category) {
        this(description, InputConstants.Type.KEYSYM, code, category);
    }

    /// This key mapping only applies [KeyConflictContext#IN_GAME] since it represents player moves.
    public CombatKeyMapping(String description, InputConstants.Type type, int code, String category) {
        super(description, type, code, category);
        COMBAT_KEY_MAPPINGS.add(this);
    }

        public boolean isActiveAndMatches(@NotNull InputConstants.Key keyCode) {
        return super.matches(keyCode.getValue(), 0) && ClientEngine.getInstance().isEpicFightMode();
    }
}