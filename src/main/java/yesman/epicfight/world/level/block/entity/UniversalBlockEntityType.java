package yesman.epicfight.world.level.block.entity;

import java.util.Set;

import com.mojang.datafixers.types.Type;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class UniversalBlockEntityType<T extends BlockEntity> extends BlockEntityType<T> {
	public UniversalBlockEntityType(BlockEntitySupplier<T> p_155259_, Set<Block> p_155260_, Type<?> p_155261_) {
		super(p_155259_, p_155260_, p_155261_);
	}
	
	@Override
	public boolean isValid(BlockState blockState) {
		return true;
	}
}