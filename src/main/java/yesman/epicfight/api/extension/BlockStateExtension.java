package yesman.epicfight.api.extension;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/// Fabric-compatible extension interface for BlockState methods that exist in NeoForge but not vanilla.
/// Implemented via MixinBlockStateBase. Defaults match NeoForge's vanilla behavior.
public interface BlockStateExtension {
	/// NeoForge's BlockState#canEntityDestroy(Level, BlockPos, Entity). Default: true.
	default boolean epicfight$canEntityDestroy(BlockGetter level, BlockPos pos, Entity entity) {
		return true;
	}

	/// NeoForge's BlockState#getSoundType(Level, BlockPos, Entity).
	/// Delegates to the no-arg vanilla method.
	default SoundType epicfight$getSoundType(BlockGetter level, BlockPos pos, Entity entity) {
		return ((BlockBehaviour.BlockStateBase) this).getSoundType();
	}

	/// NeoForge's BlockState#getLightEmission(BlockGetter, BlockPos).
	/// Delegates to the no-arg vanilla method.
	default int epicfight$getLightEmission(BlockGetter level, BlockPos pos) {
		return ((BlockBehaviour.BlockStateBase) this).getLightEmission();
	}

	/// Helper to cast a BlockState to BlockStateExtension
	static BlockStateExtension of(BlockState state) {
		return (BlockStateExtension) state;
	}
}
