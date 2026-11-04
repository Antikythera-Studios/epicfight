package yesman.epicfight.api.utils.datastructure;

import net.minecraft.core.IdMapper;
import yesman.epicfight.mixin.common.IdMapperAccessor;

public class ClearableIdMapper<I> extends IdMapper<I> {
	public ClearableIdMapper() {
		super(512);
	}

	public ClearableIdMapper(int size) {
		super(size);
	}

	public void clear() {
		((IdMapperAccessor<I>) (Object) this).epicfight$getTToId().clear();
		((IdMapperAccessor<I>) (Object) this).epicfight$getIdToT().clear();
		((IdMapperAccessor<I>) (Object) this).epicfight$setNextId(0);
	}
}
