package yesman.epicfight.platform.fabric.mixin;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnPlacementType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.levelgen.Heightmap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/// Exposes the package-private {@code SpawnPlacements.register} method so spawn placements
/// can be registered on Fabric (replaces NeoForge's RegisterSpawnPlacementsEvent).
@Mixin(SpawnPlacements.class)
public interface SpawnPlacementsAccessor {
    @Invoker("register")
    static <T extends Mob> void epicfight$register(
            EntityType<T> type,
            SpawnPlacementType placement,
            Heightmap.Types heightmap,
            SpawnPlacements.SpawnPredicate<T> predicate) {
        throw new UnsupportedOperationException();
    }
}
