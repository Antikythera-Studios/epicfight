package yesman.epicfight.registry.entries;
import yesman.epicfight.EpicFight;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import yesman.epicfight.registry.deferred_shim.DeferredHolderShim;
import yesman.epicfight.registry.deferred_shim.DeferredRegisterShim;
import yesman.epicfight.main.EpicFightMod;
import yesman.epicfight.world.level.block.FractureBlock;

public final class EpicFightBlocks {
	private EpicFightBlocks() {}
	
	public static final DeferredRegisterShim<Block> REGISTRY = new DeferredRegisterShim<>(Registries.BLOCK, EpicFight.MODID);
	
	public static final DeferredHolderShim<Block, FractureBlock> FRACTURE = REGISTRY.register("fracture_block", () -> new FractureBlock(BlockBehaviour.Properties.of()));
}