package yesman.epicfight.compat.fgm;
import yesman.epicfight.EpicFight;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.wildfire.api.IGenderArmor;
import com.wildfire.main.WildfireGender;
import com.wildfire.main.WildfireHelper;
import com.wildfire.main.entitydata.Breasts;
import com.wildfire.main.entitydata.EntityConfig;
import com.wildfire.physics.BreastPhysics;
import com.wildfire.render.GenderLayer;
import com.wildfire.render.WildfireModelRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.armortrim.ArmorTrim;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.block.Blocks;


import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import yesman.epicfight.api.animation.Joint;
import yesman.epicfight.api.client.event.EpicFightClientEventHooks;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.client.renderer.patched.entity.PPlayerRenderer;
import yesman.epicfight.client.renderer.patched.layer.PatchedLayer;
import yesman.epicfight.client.world.capabilites.entitypatch.player.AbstractClientPlayerPatch;
import yesman.epicfight.compat.ICompatModule;
import yesman.epicfight.model.armature.HumanoidArmature;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

public class WildfireFGMCompat implements ICompatModule {
    @Override
	public void onInitialize() {

    }

    @Override
	public void onInitializeServer() {

    }

    @Override
	public void onInitializeClient() {
        EpicFightClientEventHooks.Registry.MODIFY_PATCHED_ENTITY.registerEvent(event -> {
            if (event.get(EntityType.PLAYER) instanceof PPlayerRenderer playerrenderer) {
                playerrenderer.addPatchedLayerAlways(GenderLayer.class, new EpicFightWildfireRenderLayer());
            }
        });
    }

    @Override
	public void onInitializeClientServer() {

    }

    public static class EpicFightWildfireRenderLayer extends PatchedLayer<AbstractClientPlayer, AbstractClientPlayerPatch<AbstractClientPlayer>, PlayerModel<AbstractClientPlayer>, GenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>>> {
        private static final WildfireModelRenderer.OverlayModelBox lBreastWear = new WildfireModelRenderer.OverlayModelBox(true, 64, 64, 17, 34, -4.0F, 0.0F, 0.0F, 4, 5, 3, 0.0F, false);
        private static final WildfireModelRenderer.OverlayModelBox rBreastWear = new WildfireModelRenderer.OverlayModelBox(false, 64, 64, 21, 34, 0.0F, 0.0F, 0.0F, 4, 5, 3, 0.0F, false);
        private static final WildfireModelRenderer.BreastModelBox lBoobArmor = new WildfireModelRenderer.BreastModelBox(64, 32, 16, 17, -4.0F, 0.0F, 0.0F, 4, 5, 3, 0.0F, false);
        private static final WildfireModelRenderer.BreastModelBox rBoobArmor = new WildfireModelRenderer.BreastModelBox(64, 32, 20, 17, 0.0F, 0.0F, 0.0F, 4, 5, 3, 0.0F, false);

        WildfireModelRenderer.BreastModelBox lB;
        WildfireModelRenderer.BreastModelBox rB;
        float preSize;
        float preOffsetZ;

        @Override
        protected void renderLayer(AbstractClientPlayerPatch<AbstractClientPlayer> entityPatch, AbstractClientPlayer entity, @Nullable GenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> vanillaLayer, PoseStack poseStack, MultiBufferSource buffer, int packedLight, OpenMatrix4f[] poses, float bob, float yRot, float xRot, float partialTicks) {
            if (vanillaLayer instanceof yesman.epicfight.compat.fgm.mixin.FemaleLayerAccessor<?, ?> accessor) {
                if (!entity.isSpectator()) {
                    try {
                        EntityConfig entityConfig = EntityConfig.getEntity(entity);
                        if (entityConfig == null) {
                            return;
                        }

                        ItemStack armorStack = entity.getItemBySlot(EquipmentSlot.CHEST);
                        IGenderArmor genderArmor = WildfireHelper.getArmorConfig(armorStack);
                        boolean isChestplateOccupied = genderArmor.coversBreasts();
                        if (genderArmor.alwaysHidesBreasts() || !entityConfig.showBreastsInArmor() && isChestplateOccupied) {
                            return;
                        }

                        RenderType breastRenderType = null;
                        // NeoForge had GenderLayer.getBreastTexture(); Fabric does not.
                        // Get the player's skin texture directly instead.
                        ResourceLocation entityTexture = entity.getSkin().texture();
                        if (entityTexture != null) {
                            boolean bodyVisible = !entity.isInvisible();
                            Minecraft minecraft = Minecraft.getInstance();
                            boolean translucent = !bodyVisible && minecraft.player != null && !entity.isInvisibleTo(minecraft.player);
                            if (translucent) {
                                breastRenderType = RenderType.itemEntityTranslucentCull(entityTexture);
                            } else if (bodyVisible) {
                                breastRenderType = RenderType.entityTranslucent(entityTexture);
                            } else if (minecraft.shouldEntityAppearGlowing(entity)) {
                                breastRenderType = RenderType.outline(entityTexture);
                            } else if (!isChestplateOccupied) {
                                return;
                            }
                        } else if (!isChestplateOccupied) {
                            return;
                        }

                        Breasts breasts = entityConfig.getBreasts();
                        float breastOffsetX = (float)Math.round((float)Math.round(breasts.getXOffset() * 100.0F) / 100.0F * 10.0F) / 10.0F;
                        float breastOffsetY = (float)(-Math.round((float)Math.round(breasts.getYOffset() * 100.0F) / 100.0F * 10.0F)) / 10.0F;
                        float breastOffsetZ = (float)(-Math.round((float)Math.round(breasts.getZOffset() * 100.0F) / 100.0F * 10.0F)) / 10.0F;
                        BreastPhysics leftBreastPhysics = entityConfig.getLeftBreastPhysics();
                        float bSize = leftBreastPhysics.getBreastSize(partialTicks);
                        float outwardAngle = (float)Math.round(breasts.getCleavage() * 100.0F) / 100.0F * 100.0F;
                        outwardAngle = Math.min(outwardAngle, 10.0F);
                        this.resizeBox(bSize, breastOffsetZ);
                        float overlayAlpha = entity.isInvisible() ? 0.15F : 1.0F;
                        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                        float lPhysPositionY = Mth.lerp(partialTicks, leftBreastPhysics.getPrePositionY(), leftBreastPhysics.getPositionY());
                        float lPhysPositionX = Mth.lerp(partialTicks, leftBreastPhysics.getPrePositionX(), leftBreastPhysics.getPositionX());
                        float leftBounceRotation = Mth.lerp(partialTicks, leftBreastPhysics.getPreBounceRotation(), leftBreastPhysics.getBounceRotation());
                        float rPhysPositionY;
                        float rPhysPositionX;
                        float rightBounceRotation;
                        if (breasts.isUniboob()) {
                            rPhysPositionY = lPhysPositionY;
                            rPhysPositionX = lPhysPositionX;
                            rightBounceRotation = leftBounceRotation;
                        } else {
                            BreastPhysics rightBreastPhysics = entityConfig.getRightBreastPhysics();
                            rPhysPositionY = Mth.lerp(partialTicks, rightBreastPhysics.getPrePositionY(), rightBreastPhysics.getPositionY());
                            rPhysPositionX = Mth.lerp(partialTicks, rightBreastPhysics.getPrePositionX(), rightBreastPhysics.getPositionX());
                            rightBounceRotation = Mth.lerp(partialTicks, rightBreastPhysics.getPreBounceRotation(), rightBreastPhysics.getBounceRotation());
                        }

                        float breastSize = bSize * 1.5F;
                        if (breastSize > 0.7F) {
                            breastSize = 0.7F;
                        }

                        if (bSize > 0.7F) {
                            breastSize = bSize;
                        }

                        if (breastSize < 0.02F) {
                            return;
                        }

                        float zOff = 0.0625F - bSize * 0.0625F;
                        breastSize = bSize + 0.5F * Math.abs(bSize - 0.7F) * 2.0F;
                        float resistance = entityConfig.getArmorPhysicsOverride() ? 0.0F : Mth.clamp(genderArmor.physicsResistance(), 0.0F, 1.0F);
                        boolean breathingAnimation = true && resistance <= 0.5F && (!entity.isUnderWater() || MobEffectUtil.hasWaterBreathing(entity) || entity.level().getBlockState(BlockPos.containing(entity.getX(), entity.getEyeY(), entity.getZ())).is(Blocks.BUBBLE_COLUMN));
                        boolean bounceEnabled = entityConfig.hasBreastPhysics() && (!isChestplateOccupied || resistance < 1.0F);
                        int overlay = LivingEntityRenderer.getOverlayCoords(entity, 0.0F);
                        HumanoidModel<?> model = vanillaLayer.getParentModel();
                        boolean hasJacketLayer = entity.isModelPartShown(PlayerModelPart.JACKET);

                        this.renderBreastWithTransforms(entityPatch, armorStack, poseStack, buffer, breastRenderType, packedLight, overlay, overlayAlpha, bounceEnabled, lPhysPositionX, lPhysPositionY, leftBounceRotation, breastSize, breastOffsetX, breastOffsetY, breastOffsetZ, zOff, outwardAngle, breasts.isUniboob(), isChestplateOccupied, breathingAnimation, true, hasJacketLayer, partialTicks);
                        this.renderBreastWithTransforms(entityPatch, armorStack, poseStack, buffer, breastRenderType, packedLight, overlay, overlayAlpha, bounceEnabled, rPhysPositionX, rPhysPositionY, rightBounceRotation, breastSize, -breastOffsetX, breastOffsetY, breastOffsetZ, zOff, -outwardAngle, breasts.isUniboob(), isChestplateOccupied, breathingAnimation, false, hasJacketLayer, partialTicks);
                        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                    } catch (Exception e) {
                        WildfireGender.LOGGER.error("Failed to render gender layer", e);
                    }

                }
            }
        }
        protected void resizeBox(float breastSize, float breastOffsetZ) {
            float reducer = -1.0F;
            if (breastSize < 0.84F) {
                ++reducer;
            }

            if (breastSize < 0.72F) {
                ++reducer;
            }

            if (this.preSize != breastSize || this.preOffsetZ != breastOffsetZ) {
                this.lB = new WildfireModelRenderer.BreastModelBox(64, 64, 16, 17, -4.0F, 0.0F, 0.0F, 4, 5, (int)(4.0F - breastOffsetZ - reducer), 0.0F, false);
                this.rB = new WildfireModelRenderer.BreastModelBox(64, 64, 20, 17, 0.0F, 0.0F, 0.0F, 4, 5, (int)(4.0F - breastOffsetZ - reducer), 0.0F, false);
                this.preSize = breastSize;
                this.preOffsetZ = breastOffsetZ;
            }

        }

        private void renderBreastWithTransforms(LivingEntityPatch<?> entity, ItemStack armorStack, PoseStack matrixStack, MultiBufferSource bufferSource, @Nullable RenderType breastRenderType, int light, int overlay, float alpha, boolean bounceEnabled, float physPositionX, float physPositionY, float bounceRotation, float breastSize, float breastOffsetX, float breastOffsetY, float breastOffsetZ, float zOff, float outwardAngle, boolean uniboob, boolean isChestplateOccupied, boolean breathingAnimation, boolean left, boolean hasJacketLayer, float partialTicks) {
            matrixStack.pushPose();
            if (entity.getArmature() instanceof HumanoidArmature armature)
            {
                try {
                    Joint chest = armature.chest;
                    Matrix4f transform = OpenMatrix4f.exportToMojangMatrix(armature.getBoundTransformFor(entity.getAnimator().getPose(partialTicks), chest));
                    Vector3f translationVector = transform.getTranslation(new Vector3f());
                    matrixStack.translate(translationVector.x, translationVector.y, translationVector.z);
                    matrixStack.mulPose(transform.getNormalizedRotation(new Quaternionf()).rotateXYZ(Mth.PI, Mth.PI, 0));
                    matrixStack.translate(0, -0.35, 0.00125);

                    Vector3f scaleVector = transform.getScale(new Vector3f());
                    matrixStack.scale(scaleVector.x, scaleVector.y, scaleVector.z - 0.0125f);

                    if (bounceEnabled) {
                        matrixStack.translate(physPositionX / 32.0F, 0.0F, 0.0F);
                        matrixStack.translate(0.0F, physPositionY / 32.0F, 0.0F);
                    }

                    matrixStack.translate(breastOffsetX * 0.0625F, 0.05625F + breastOffsetY * 0.0625F, zOff - 0.125F + breastOffsetZ * 0.0625F);
                    if (!uniboob) {
                        matrixStack.translate(-0.125F * (float)(left ? 1 : -1), 0.0F, 0.0F);
                    }

                    if (bounceEnabled) {
                        matrixStack.mulPose((new Quaternionf()).rotationXYZ(0.0F, (float)((double)bounceRotation * (Math.PI / 180D)), 0.0F));
                    }

                    if (!uniboob) {
                        matrixStack.translate(0.125F * (float)(left ? 1 : -1), 0.0F, 0.0F);
                    }

                    float rotationMultiplier = 0.0F;
                    if (bounceEnabled) {
                        matrixStack.translate(0.0F, -0.035F * breastSize, 0.0F);
                        rotationMultiplier = -physPositionY / 12.0F;
                    }

                    float totalRotation = breastSize + rotationMultiplier;
                    if (!bounceEnabled) {
                        totalRotation = breastSize;
                    }

                    if (totalRotation > breastSize + 0.2F) {
                        totalRotation = breastSize + 0.2F;
                    }

                    totalRotation = Math.min(totalRotation, 1.0F);
                    if (isChestplateOccupied) {
                        matrixStack.translate(0.0F, 0.0F, 0.01F);
                    }

                    matrixStack.mulPose((new Quaternionf()).rotationXYZ(0.0F, (float)((double)outwardAngle * (Math.PI / 180D)), 0.0F));
                    matrixStack.mulPose((new Quaternionf()).rotationXYZ((float)((double)(-35.0F * totalRotation) * (Math.PI / 180D)), 0.0F, 0.0F));
                    if (breathingAnimation) {
                        float f5 = -Mth.cos((float)entity.getOriginal().tickCount * 0.09F) * 0.45F + 0.45F;
                        matrixStack.mulPose((new Quaternionf()).rotationXYZ((float)((double)f5 * (Math.PI / 180D)), 0.0F, 0.0F));
                    }

                    matrixStack.scale(0.9995F, 1.0F, 1.0F);
                    this.renderBreast(entity, armorStack, matrixStack, bufferSource, breastRenderType, light, overlay, alpha, left, hasJacketLayer);
                } catch (Exception e) {
                    WildfireGender.LOGGER.error("Failed to render breast", e);
                }
            }
            matrixStack.popPose();
        }

        private void shiftForJacket(PoseStack matrixStack) {
            matrixStack.translate(0.0F, 0.0F, -0.015F);
            matrixStack.scale(1.05F, 1.05F, 1.05F);
        }

        private void renderBreast(LivingEntityPatch<?> entity, ItemStack armorStack, PoseStack matrixStack, MultiBufferSource bufferSource, @Nullable RenderType breastRenderType, int light, int overlay, float alpha, boolean left, boolean hasJacketLayer) {
            if (breastRenderType != null) {
                VertexConsumer vertexConsumer = bufferSource.getBuffer(breastRenderType);
                int color = FastColor.ARGB32.color(FastColor.as8BitChannel(alpha), -1);
                renderBox(left ? this.lB : this.rB, matrixStack, vertexConsumer, light, overlay, color);
                if (hasJacketLayer) {
                    this.shiftForJacket(matrixStack);
                    renderBox(left ? lBreastWear : rBreastWear, matrixStack, vertexConsumer, light, overlay, color);
                }
            } else if (hasJacketLayer) {
                this.shiftForJacket(matrixStack);
            }

            if (!armorStack.isEmpty()) {
                Item armor = armorStack.getItem();
                if (armor instanceof ArmorItem armorItem) {
                    matrixStack.pushPose();
                    matrixStack.translate(left ? 0.001F : -0.001F, 0.015F, -0.015F);
                    matrixStack.scale(1.05F, 1.0F, 1.0F);
                    WildfireModelRenderer.BreastModelBox armorBox = left ? lBoobArmor : rBoobArmor;
                    Holder<ArmorMaterial> material = armorItem.getMaterial();
                    int color = armorStack.is(ItemTags.DYEABLE) ? DyedItemColor.getOrDefault(armorStack, -6265536) : -1;

                    for(ArmorMaterial.Layer layer : material.value().layers()) {
                        ResourceLocation armorTexture = yesman.epicfight.platform.neoforged.client.ClientHooks.getArmorTexture(entity.getOriginal(), armorStack, layer, false, EquipmentSlot.CHEST);
                        RenderType armorType = RenderType.armorCutoutNoCull(armorTexture);
                        VertexConsumer armorVertexConsumer = bufferSource.getBuffer(armorType);
                        renderBox(armorBox, matrixStack, armorVertexConsumer, light, OverlayTexture.NO_OVERLAY, layer.dyeable() ? color : -1);
                    }

                    //ArmorTrim trim = armorStack.get(DataComponents.TRIM);
                    //TODO: Fix this later
//                    if (trim != null) {
//                        TextureAtlasSprite sprite = this.armorTrimAtlas.getSprite(trim.outerTexture(material));
//                        VertexConsumer trimVertexConsumer = sprite.wrap(bufferSource.getBuffer(Sheets.armorTrimsSheet(((TrimPattern)trim.pattern().value()).decal())));
//                        renderBox(armorBox, matrixStack, trimVertexConsumer, light, OverlayTexture.NO_OVERLAY, -1);
//                    }

                    if (armorStack.hasFoil()) {
                        renderBox(armorBox, matrixStack, bufferSource.getBuffer(RenderType.armorEntityGlint()), light, OverlayTexture.NO_OVERLAY, -1);
                    }

                    matrixStack.popPose();
                }
            }
        }

        private static void renderBox(WildfireModelRenderer.ModelBox model, PoseStack matrixStack, VertexConsumer bufferIn, int light, int overlay, int color) {
            Matrix4f matrix4f = matrixStack.last().pose();
            Matrix3f matrix3f = matrixStack.last().normal();

            for(WildfireModelRenderer.TexturedQuad quad : model.quads) {
                Vector3f vector3f = new Vector3f((float)quad.normal.x(), (float)quad.normal.y(), (float)quad.normal.z());
                vector3f.mul(matrix3f);

                for(WildfireModelRenderer.PositionTextureVertex vertex : quad.vertexPositions) {
                    bufferIn.addVertex(matrix4f, vertex.x() / 16.0F, vertex.y() / 16.0F, vertex.z() / 16.0F).setColor(color).setUv(vertex.u(), vertex.v()).setOverlay(overlay).setLight(light).setNormal(vector3f.x(), vector3f.y(), vector3f.z());
                }
            }
        }
    }


}
