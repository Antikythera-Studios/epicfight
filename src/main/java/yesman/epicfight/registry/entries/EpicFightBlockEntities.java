package yesman.epicfight.registry.entries;
import yesman.epicfight.EpicFight;

import com.google.common.collect.ImmutableSet;

import net.minecraft.Util;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.world.level.block.entity.BlockEntityType;
import yesman.epicfight.registry.deferred_shim.DeferredHolderShim;
import yesman.epicfight.registry.deferred_shim.DeferredRegisterShim;
import yesman.epicfight.main.EpicFightMod;
import yesman.epicfight.world.level.block.entity.FractureBlockEntity;
import yesman.epicfight.world.level.block.entity.UniversalBlockEntityType;

public final class EpicFightBlockEntities {
	private EpicFightBlockEntities() {}
	
	public static final DeferredRegisterShim<BlockEntityType<?>> REGISTRY = new DeferredRegisterShim<>(Registries.BLOCK_ENTITY_TYPE, EpicFight.MODID);
	
	public static final DeferredHolderShim<BlockEntityType<?>, BlockEntityType<FractureBlockEntity>> FRACTURE =
		REGISTRY.register(
			  "fracture_block"
			, () ->
				new UniversalBlockEntityType<>(
					  FractureBlockEntity::new
					, ImmutableSet.of(EpicFightBlocks.FRACTURE.get())
					, Util.fetchChoiceType(References.BLOCK_ENTITY, "fracture_block")
				)
		);
}