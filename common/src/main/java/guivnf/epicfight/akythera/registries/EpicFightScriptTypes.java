package guivnf.epicfight.akythera.registries;

import com.yesman.akythera.api.data.modularize.ScriptType;
import com.yesman.akythera.core.registry.AkytheraRegistries;
import com.yesman.akythera.platform.ModPlatform;
import com.yesman.akythera.platform.ModPlatformProvider;
import guivnf.epicfight.EpicFight;
import guivnf.epicfight.akythera.scripts.animator.IdleAnimatorScript;
import guivnf.epicfight.akythera.scripts.animator.PlayerAnimatorScript;
import guivnf.epicfight.akythera.scripts.entitypatches.PlayerPatchScript;
import net.minecraft.core.Holder;

public final class EpicFightScriptTypes {
    private EpicFightScriptTypes() {}

    public static final ModPlatform.Registrar<ScriptType> REGISTRAR =
        ModPlatformProvider.get().getRegistrar(AkytheraRegistries.Frozen.SCRIPT_TYPE_KEY, EpicFight.MODID);

    public static final Holder<ScriptType> PLAYER_ANIMATOR_SCRIPT = REGISTRAR.register("player_animator_script", () -> new ScriptType(PlayerAnimatorScript.class));
    public static final Holder<ScriptType> IDLE_ANIMATOR_SCRIPT = REGISTRAR.register("idle_animator_script", () -> new ScriptType(IdleAnimatorScript.class));
    public static final Holder<ScriptType> PLAYER_PATCH_SCRIPT = REGISTRAR.register("player_patch_script", () -> new ScriptType(PlayerPatchScript.class));
}
