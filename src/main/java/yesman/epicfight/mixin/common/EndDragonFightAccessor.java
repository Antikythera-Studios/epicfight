package yesman.epicfight.mixin.common;

import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.world.level.dimension.end.EndDragonFight;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/// Yarn mappings: dragonEvent is ServerBossEvent (not BossEvent)
@Mixin(EndDragonFight.class)
public interface EndDragonFightAccessor {
    @Accessor("dragonEvent")
    ServerBossEvent epicfight$getDragonEvent();
}
