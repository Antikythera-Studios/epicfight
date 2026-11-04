package yesman.epicfight.platform.neoforged.client;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.Model;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
public class ClientHooks {
    public static Object onClickInput(int button, int key, InteractionHand hand) { return null; }
    public static float getGuiFarPlane() { return 11000.0F; }
    public static BakedModel handleCameraTransforms(PoseStack poseStack, BakedModel model, ItemDisplayContext displayContext, boolean applyLeftHandTransform) {
        model.getTransforms().getTransform(displayContext).apply(applyLeftHandTransform, poseStack);
        return model;
    }
    public static Model getArmorModel(LivingEntity entity, ItemStack stack, EquipmentSlot slot, Model model) { return model; }
    public static int getArmorLayerTintColor(ItemStack stack, LivingEntity entity, ArmorMaterial.Layer layer, int layerIdx, int fallbackColor) {
        if (layer.dyeable()) {
            net.minecraft.world.item.component.DyedItemColor dyedColor = stack.get(net.minecraft.core.component.DataComponents.DYED_COLOR);
            if (dyedColor != null) {
                return dyedColor.rgb();
            }
        }
        return fallbackColor;
    }
    public static ResourceLocation getArmorTexture(Entity entity, ItemStack stack, ArmorMaterial.Layer layer, boolean innerModel, EquipmentSlot slot) { return layer.texture(innerModel); }
}
