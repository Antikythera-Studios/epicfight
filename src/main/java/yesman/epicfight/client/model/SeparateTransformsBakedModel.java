package yesman.epicfight.client.model;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Map;

/// Baked model that delegates to a [base] model or a per-perspective model based on the [ItemDisplayContext].
///
/// The actual perspective swapping happens in [MixinItemRenderer], which wraps the [renderModelLists] call
/// to swap the baked model based on the current [ItemDisplayContext] parameter.
public class SeparateTransformsBakedModel implements BakedModel {
    private final BakedModel baseBaked;
    private final Map<ItemDisplayContext, BakedModel> perspectiveBaked;
    private ItemTransforms mergedTransforms;

    public SeparateTransformsBakedModel(BakedModel baseBaked, Map<ItemDisplayContext, BakedModel> perspectiveBaked) {
        this.baseBaked = baseBaked;
        this.perspectiveBaked = perspectiveBaked;
    }

    /// Returns the perspective model for the given [ItemDisplayContext], or the base model if no
    /// perspective model is defined for that context.
    public BakedModel getPerspectiveModel(ItemDisplayContext context) {
        if (perspectiveBaked != null && perspectiveBaked.containsKey(context)) {
            return perspectiveBaked.get(context);
        }

        return baseBaked;
    }

    public BakedModel getBaseBaked() {
        return baseBaked;
    }

    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction direction, RandomSource random) {
        return baseBaked != null ? baseBaked.getQuads(state, direction, random) : List.of();
    }

    @Override
    public boolean useAmbientOcclusion() {
        return baseBaked != null && baseBaked.useAmbientOcclusion();
    }

    @Override
    public boolean isGui3d() {
        return baseBaked != null && baseBaked.isGui3d();
    }

    @Override
    public boolean usesBlockLight() {
        return baseBaked != null && baseBaked.usesBlockLight();
    }

    @Override
    public boolean isCustomRenderer() {
        return baseBaked != null && baseBaked.isCustomRenderer();
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return baseBaked != null ? baseBaked.getParticleIcon() : null;
    }

    @Override
    public ItemTransforms getTransforms() {
        if (baseBaked == null) {
            return ItemTransforms.NO_TRANSFORMS;
        }

        if (mergedTransforms == null) {
            ItemTransforms baseTransforms = baseBaked.getTransforms();

            mergedTransforms = new ItemTransforms(baseTransforms) {
                @Override
                public ItemTransform getTransform(ItemDisplayContext context) {
                    BakedModel perspective = perspectiveBaked.get(context);

                    if (perspective != null) {
                        return perspective.getTransforms().getTransform(context);
                    }

                    return baseTransforms.getTransform(context);
                }
            };
        }

        return mergedTransforms;
    }

    @Override
    public ItemOverrides getOverrides() {
        return baseBaked != null ? baseBaked.getOverrides() : ItemOverrides.EMPTY;
    }
}
