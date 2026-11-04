package yesman.epicfight.mixin.common;

import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import yesman.epicfight.api.extension.BlockStateExtension;

@Mixin(BlockBehaviour.BlockStateBase.class)
public class MixinBlockStateBase implements BlockStateExtension {
}
