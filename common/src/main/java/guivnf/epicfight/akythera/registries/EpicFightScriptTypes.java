package guivnf.epicfight.akythera.registries;

import com.yesman.akythera.api.data.modularize.ScriptType;
import guivnf.epicfight.akythera.scripts.animator.PlayerAnimatorScript;

import java.util.function.BiConsumer;

public final class EpicFightScriptTypes {
    private EpicFightScriptTypes() {}

    public static void register(final BiConsumer<String, ScriptType> registry) {
        registry.accept("player_animator_script", new ScriptType(PlayerAnimatorScript.class));
    }
}
