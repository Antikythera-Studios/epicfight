package yesman.epicfight.client.model;

import net.fabricmc.fabric.api.client.model.loading.v1.FabricBakedModelManager;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelModifier;
import net.fabricmc.fabric.api.renderer.v1.model.ForwardingBakedModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import yesman.epicfight.skill.SkillCategory;
import yesman.epicfight.world.item.SkillBookItem;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/// Fabric port of the NeoForge [ModelEvent.RegisterAdditional]/[ModelEvent.ModifyBakingResult]
/// logic that gives the skill book item its per-category textures.
///
/// Upstream injects [ItemOverrides] into the baked skillbook model, keyed on the
/// [epicfight:skill] item property returning the category's universal ordinal. On vanilla
/// semantics that fails: [ItemProperties.register] stores the function as a
/// ClampedItemPropertyFunction, so the ordinal is clamped to [0,1] at resolve time and
/// every category collapses onto the same override. Instead we wrap the baked model and
/// override [ItemOverrides#resolve] to map the stack's skill to the category model
/// directly — equivalent to upstream, but without the clamped property lookup.
///
/// Category models are registered via [Context#addModels] so they bake through the normal
/// top-level pipeline (which runs ItemModelGenerator for item/generated parents). They are
/// not exposed to bake events — models added this way get the "fabric_resource" variant
/// which skips AfterBake — so resolve() fetches them from the BakedModelManager at render
/// time. Addons' [SkillCategory]s therefore work without extra registration.
public class SkillBookModelLoadingPlugin implements ModelLoadingPlugin {
    private static final ModelResourceLocation SKILLBOOK_TOP_LEVEL = new ModelResourceLocation(SkillCategory.DEFAULT_BOOK_ICON, ModelResourceLocation.INVENTORY_VARIANT);

    @Override
    public void onInitializeModelLoader(Context context) {
        Set<ResourceLocation> categoryModels = new HashSet<>();

        SkillCategory.ENUM_MANAGER.universalValues().stream()
            .map(SkillCategory::bookIcon)
            .filter(icon -> !icon.equals(SkillCategory.DEFAULT_BOOK_ICON))
            .forEach(icon -> categoryModels.add(ResourceLocation.fromNamespaceAndPath(icon.getNamespace(), "item/" + icon.getPath())));

        context.addModels(categoryModels);
        context.modifyModelAfterBake().register(SkillBookModelLoadingPlugin::wrapSkillBookModel);
    }

    private static BakedModel wrapSkillBookModel(@Nullable BakedModel model, ModelModifier.AfterBake.Context context) {
        if (model == null || !SKILLBOOK_TOP_LEVEL.equals(context.topLevelId()) || !(context.sourceModel() instanceof BlockModel sourceModel)) {
            return model;
        }

        return new SkillBookBakedModel(model, new SkillBookItemOverrides(context.baker(), sourceModel));
    }

    private static ResourceLocation itemModelId(ResourceLocation bookIcon) {
        return ResourceLocation.fromNamespaceAndPath(bookIcon.getNamespace(), "item/" + bookIcon.getPath());
    }

    private static class SkillBookBakedModel extends ForwardingBakedModel {
        private final ItemOverrides overrides;

        SkillBookBakedModel(BakedModel base, ItemOverrides overrides) {
            this.wrapped = base;
            this.overrides = overrides;
        }

        @Override
        public ItemOverrides getOverrides() {
            return this.overrides;
        }
    }

    private static class SkillBookItemOverrides extends ItemOverrides {
        SkillBookItemOverrides(net.minecraft.client.resources.model.ModelBaker baker, BlockModel source) {
            super(baker, source, List.of());
        }

        @Override
        public BakedModel resolve(BakedModel model, ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
            return SkillBookItem.getContainSkill(stack)
                .map(holder -> {
                    ResourceLocation icon = holder.value().getCategory().bookIcon();
                    if (icon.equals(SkillCategory.DEFAULT_BOOK_ICON)) {
                        return model;
                    }
                    BakedModel categoryModel = ((FabricBakedModelManager) Minecraft.getInstance().getModelManager()).getModel(itemModelId(icon));
                    return categoryModel != null ? categoryModel : model;
                })
                .orElse(model);
        }
    }
}
