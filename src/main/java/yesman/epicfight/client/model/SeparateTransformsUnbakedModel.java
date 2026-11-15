package yesman.epicfight.client.model;

import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemModelGenerator;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/// Unbaked model that wraps a [base] model and per-perspective [perspectives] models for the
/// NeoForge [neoforge:separate_transforms] format.
///
/// Bakes all sub-models and returns a [SeparateTransformsBakedModel] that delegates to the
/// appropriate perspective model based on the [ItemDisplayContext].
public class SeparateTransformsUnbakedModel implements UnbakedModel {
    private final BlockModel base;
    private final Map<ItemDisplayContext, BlockModel> perspectives;

    public SeparateTransformsUnbakedModel(BlockModel base, Map<ItemDisplayContext, BlockModel> perspectives) {
        this.base = base;
        this.perspectives = perspectives;
    }

    @Override
    public Collection<ResourceLocation> getDependencies() {
        Set<ResourceLocation> deps = new HashSet<>();

        if (base != null) {
            deps.addAll(base.getDependencies());
        }

        for (BlockModel perspective : perspectives.values()) {
            deps.addAll(perspective.getDependencies());
        }

        return deps;
    }

    @Override
    public void resolveParents(Function<ResourceLocation, UnbakedModel> resolver) {
        if (base != null) {
            base.resolveParents(resolver);
        }

        for (BlockModel perspective : perspectives.values()) {
            perspective.resolveParents(resolver);
        }
    }

    /// Checks if the root parent of the given model is [ModelBakery.GENERATION_MARKER].
    /// If so, runs [ItemModelGenerator] to generate quads from the layer textures before baking.
    /// This replicates the special handling in [ModelBakery.ModelBakerImpl.bakeUncached].
    private net.minecraft.client.resources.model.BakedModel bakeModel(BlockModel model, ModelBaker baker, Function<Material, net.minecraft.client.renderer.texture.TextureAtlasSprite> spriteGetter, ModelState modelState) {
        BlockModel rootModel = model.getRootModel();

        if (rootModel == net.minecraft.client.resources.model.ModelBakery.GENERATION_MARKER) {
            // Generate quads from the layer textures, just like ModelBakery does for item models
            ItemModelGenerator generator = new ItemModelGenerator();
            BlockModel generatedModel = generator.generateBlockModel(spriteGetter, model);
            return generatedModel.bake(baker, model, spriteGetter, modelState, false);
        }

        return model.bake(baker, spriteGetter, modelState);
    }

    @Override
    public SeparateTransformsBakedModel bake(ModelBaker baker, Function<Material, net.minecraft.client.renderer.texture.TextureAtlasSprite> spriteGetter, ModelState modelState) {
        net.minecraft.client.resources.model.BakedModel baseBaked = null;

        if (base != null) {
            try {
                baseBaked = bakeModel(base, baker, spriteGetter, modelState);
            } catch (Exception e) {
                yesman.epicfight.EpicFight.LOGGER.error("[EpicFight] SeparateTransforms: failed to bake base model: {}", e.getMessage(), e);
            }
        }

        Map<ItemDisplayContext, net.minecraft.client.resources.model.BakedModel> perspectiveBaked = new HashMap<>();

        for (Map.Entry<ItemDisplayContext, BlockModel> entry : perspectives.entrySet()) {
            try {
                net.minecraft.client.resources.model.BakedModel baked = bakeModel(entry.getValue(), baker, spriteGetter, modelState);

                if (baked != null) {
                    perspectiveBaked.put(entry.getKey(), baked);
                }
            } catch (Exception e) {
                yesman.epicfight.EpicFight.LOGGER.error("[EpicFight] SeparateTransforms: failed to bake perspective {} : {}", entry.getKey(), e.getMessage(), e);
            }
        }

        return new SeparateTransformsBakedModel(baseBaked, perspectiveBaked);
    }
}
