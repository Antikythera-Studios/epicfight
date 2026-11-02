package guivnf.epicfight.akythera.registries;

import com.yesman.akythera.api.data.modularize.ScriptType;
import guivnf.epicfight.akythera.scripts.animator.IdleAnimatorScript;
import guivnf.epicfight.akythera.scripts.animator.PlayerAnimatorScript;
import guivnf.epicfight.akythera.scripts.entitypatches.PlayerPatchScript;

import java.util.function.BiConsumer;

public final class EpicFightScriptTypes {
    private EpicFightScriptTypes() {}

    public static void register(final BiConsumer<String, ScriptType> registry) {
        registry.accept("player_animator_script", new ScriptType(PlayerAnimatorScript.class));
        registry.accept("idle_animator_script", new ScriptType(IdleAnimatorScript.class));
        registry.accept("player_patch_script", new ScriptType(PlayerPatchScript.class));
    }
}
