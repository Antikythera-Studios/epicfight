package com.yesman.epicfight.akythera.registries;

import com.yesman.akythera.api.data.modularize.ScriptType;
import com.yesman.akythera.core.registry.AkytheraRegistries;
import com.yesman.epicfight.EpicFight;
import com.yesman.epicfight.akythera.scripts.animator.PlayerAnimatorScript;
import com.yesman.epicfight.platform.ModPlatform;
import com.yesman.epicfight.platform.ModPlatformProvider;
import net.minecraft.core.Holder;

public final class EpicFightScriptTypes {
    private EpicFightScriptTypes() {}

    public static final ModPlatform.Registrar<ScriptType> REGISTRAR =
        ModPlatformProvider.get().getRegistrar(AkytheraRegistries.Frozen.SCRIPT_TYPE_KEY, EpicFight.MODID);

    public static final Holder<ScriptType> PLAYER_ANIMATOR_SCRIPT =
        REGISTRAR.register("player_animator_script", () -> new ScriptType(PlayerAnimatorScript.class));
}
