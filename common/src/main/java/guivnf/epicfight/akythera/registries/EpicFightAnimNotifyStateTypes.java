package guivnf.epicfight.akythera.registries;

import com.mojang.serialization.MapCodec;
import com.yesman.akythera.api.animation.notify.AnimationNotifyState;
import com.yesman.akythera.core.registry.AkytheraRegistries;
import com.yesman.akythera.platform.ModPlatform;
import com.yesman.akythera.platform.ModPlatformProvider;
import guivnf.epicfight.EpicFight;
import guivnf.epicfight.world.animation.KeepMomentumNotifyState;
import net.minecraft.core.Holder;

public final class EpicFightAnimNotifyStateTypes {
    private EpicFightAnimNotifyStateTypes() {}

    public static final ModPlatform.Registrar<MapCodec<? extends AnimationNotifyState<?>>> REGISTRAR =
        ModPlatformProvider.get().getRegistrar(AkytheraRegistries.Frozen.ANIM_NOTIFY_STATE_TYPE_KEY, EpicFight.MODID);

    public static final Holder<MapCodec<? extends AnimationNotifyState<?>>> KEEP_MOMENTUM = REGISTRAR.register("keep_momentum", () -> KeepMomentumNotifyState.CODEC);
}
