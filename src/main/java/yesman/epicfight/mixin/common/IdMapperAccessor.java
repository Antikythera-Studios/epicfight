package yesman.epicfight.mixin.common;

import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import java.util.List;
import net.minecraft.core.IdMapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/// Accessor mixin for IdMapper private fields.
/// Yarn mappings: tToId is Reference2IntMap (not Object2IntMap), idToT is List (not Int2ObjectMap)
@Mixin(IdMapper.class)
public interface IdMapperAccessor<T> {
    @Accessor("tToId")
    Reference2IntMap<T> epicfight$getTToId();

    @Accessor("idToT")
    List<T> epicfight$getIdToT();

    @Accessor("nextId")
    int epicfight$getNextId();

    @Accessor("nextId")
    void epicfight$setNextId(int value);
}
