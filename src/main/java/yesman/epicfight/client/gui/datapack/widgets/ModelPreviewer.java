package yesman.epicfight.client.gui.datapack.widgets;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import yesman.epicfight.api.animation.*;
import yesman.epicfight.api.animation.types.*;
import yesman.epicfight.api.animation.types.AttackAnimation.Phase;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.client.animation.ClientAnimator;
import yesman.epicfight.api.client.animation.Layer;
import yesman.epicfight.api.client.animation.property.ClientAnimationProperties;
import yesman.epicfight.api.client.animation.property.TrailInfo;
import yesman.epicfight.api.client.model.Mesh;
import yesman.epicfight.api.client.model.SkinnedMesh;
import yesman.epicfight.api.client.model.SoftBodyTranslatable;
import yesman.epicfight.api.client.physics.cloth.ClothSimulatable;
import yesman.epicfight.api.client.physics.cloth.ClothSimulator;
import yesman.epicfight.api.collider.Collider;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.api.physics.PhysicsSimulator;
import yesman.epicfight.api.physics.SimulatableObject;
import yesman.epicfight.api.physics.SimulationTypes;
import yesman.epicfight.api.physics.bezier.CubicBezierCurve;
import yesman.epicfight.api.utils.math.*;
import yesman.epicfight.client.particle.AnimationTrailParticle;
import yesman.epicfight.client.particle.EpicFightParticleRenderTypes;
import yesman.epicfight.client.renderer.EpicFightShaders;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.main.EpicFightSharedConstants;
import yesman.epicfight.world.capabilities.entitypatch.Faction;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.damagesource.StunType;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/// We're refactoring UI codes, use [yesman.epicfight.client.gui.widgets.ModelPreviewer] instead
@Deprecated
public class ModelPreviewer extends AbstractWidget implements ResizableComponent, TickableComponent {
	private final ModelRenderTarget modelRenderTarget;
	private final List<AssetAccessor<? extends StaticAnimation>> animationsToPlay = Lists.newArrayList();
	private final List<CustomTrailParticle> trailParticles = Lists.newArrayList();
	private final CheckBox showColliderCheckbox = new CheckBox(Minecraft.getInstance().font, 0, 60, 0, 10, null, null, true, Component.translatable("datapack_edit.model_player.collider"), null);
	private final CheckBox showItemCheckbox = new CheckBox(Minecraft.getInstance().font, 0, 40, 0, 10, null, null, true, Component.translatable("datapack_edit.model_player.item"), null);
	private final CheckBox showTrailCheckbox = new CheckBox(Minecraft.getInstance().font, 0, 40, 0, 10, null, null, true, Component.translatable("datapack_edit.model_player.trail"), null);
	
	private double zoom = -3.0D;
	private float xRot = 0.0F;
	private float yRotO = 180.0F;
	private float yRot = 180.0F;
	private float xMove = 0.0F;
	private float yMove = 0.0F;
	private int index;
	private float attackTimeBegin;
	private float attackTimeEnd;
	
	private FakeEntityPatch entitypatch;
	private NoEntityAnimator animator;
	private AssetAccessor<? extends SkinnedMesh> mesh;
	private AssetAccessor<? extends Armature> armature;
	private ResourceLocation figureTexture;
	
	private Joint colliderJoint;
	private Collider collider;
	private List<TrailInfo> trailInfoList = Lists.newArrayList();
	private Item item;
	
	private Vec4f backgroundClearColor;
	private boolean cameraMoveEnabled = true;
	private boolean cameraRotationEnabled = true;
	private boolean zoomingCameraEnabled = true;
	
	private ClothSimulator clothSimulator;
	private SoftBodyTranslatable cloakMesh;
	private ResourceLocation cloakTexture;
	private Vec3f cloakColor = new Vec3f(1.0F, 1.0F, 1.0F);
	
	public ModelPreviewer(int x1, int x2, int y1, int y2, HorizontalSizing horizontal, VerticalSizing vertical, AssetAccessor<? extends Armature> armature, AssetAccessor<? extends SkinnedMesh> mesh) {
		super(x1, y1, x2, y2, Component.literal(""));
		
		if (armature != null) {
			this.entitypatch = new FakeEntityPatch(armature.get());
			this.animator = new NoEntityAnimator(this.entitypatch);
			this.entitypatch.initAnimator(this.animator);
		}
		
		this.x1 = x1;
		this.x2 = x2;
		this.y1 = y1;
		this.y2 = y2;
		this.horizontalSizingOption = horizontal;
		this.verticalSizingOption = vertical;
		this.mesh = mesh;
		this.armature = armature;
		
		this.modelRenderTarget = new ModelRenderTarget();
		this.resize(Minecraft.getInstance().screen.getRectangle());
		
		// default clear color
		this.modelRenderTarget.setClearColor(0.1552F, 0.1552F, 0.1552F, 1.0F);
		this.modelRenderTarget.clear(Minecraft.ON_OSX);
	}
	
	public void setMesh(AssetAccessor<? extends SkinnedMesh> mesh) {
		this.mesh = mesh;
	}
	
	public void setFigureTexture(ResourceLocation texture) {
		this.figureTexture = texture;
	}
	
	public void initCloakInfo(SoftBodyTranslatable cloakMesh, ResourceLocation cloakTexture, ClothSimulator.ClothObjectBuilder builder) {
		this.cloakMesh = cloakMesh;
		this.cloakTexture = cloakTexture;
		
		if (builder != null) {
			this.clothSimulator = new ClothSimulator();
			this.clothSimulator.runWhen(ClothSimulator.MODELPREVIEWER_CLOAK, cloakMesh, builder, () -> true);
		}
	}
	
	public void removeCloak() {
		if (this.clothSimulator != null) {
			this.clothSimulator.stop(ClothSimulator.MODELPREVIEWER_CLOAK);
			this.clothSimulator = null;
		}
		
		this.cloakMesh = null;
		this.cloakTexture = null;
	}
	
	public void setCloakColor(int colorCode) {
		float b = (colorCode & 255) / 255.0F;
		float g = ((colorCode & 65280) >> 8) / 255.0F;
		float r = ((colorCode & 16711680) >> 16) / 255.0F;
		
		this.cloakColor = new Vec3f(r, g, b);
	}
	
	public void setCloakColor(float r, float g, float b) {
		this.cloakColor = new Vec3f(r, g, b);
	}
	
	public Vec3f getCloakColor() {
		return this.cloakColor;
	}
	
	public SoftBodyTranslatable getCloakMesh() {
		return this.cloakMesh;
	}
	
	public void setCloakTexture(ResourceLocation cloakTexture) {
		this.cloakTexture = cloakTexture;
	}
	
	public ResourceLocation getCloakTexture() {
		return this.cloakTexture;
	}
	
	public AssetAccessor<? extends Armature> getArmature() {
		return this.armature;
	}
	
	public AssetAccessor<? extends SkinnedMesh> getMesh() {
		return this.mesh;
	}
	
	public Animator getAnimator() {
		return this.animator;
	}
	
	public void setCameraTransform(double zoom, float xRot, float yRot, float xMove, float yMove) {
		this.zoom = zoom;
		this.xRot = xRot;
		this.yRotO = yRot;
		this.yRot = yRot;
		this.xMove = xMove;
		this.yMove = yMove;
	}
	
	public void setCollider(Collider collider) {
		this.collider = collider;
	}
	
	public void setCollider(Collider collider, Joint joint) {
		this.collider = collider;
		this.colliderJoint = joint;
	}
	
	public void setColliderJoint(Joint joint) {
		this.colliderJoint = joint;
	}
	
	public void setTrailInfo(TrailInfo... trailInfos) {
		this.trailInfoList.clear();
		
		for (TrailInfo trailInfo : trailInfos) {
			this.trailInfoList.add(trailInfo);
		}
	}
	
	public void setItemToRender(Item item) {
		this.item = item;
	}
	
	public void setAttackTimeBegin(float attackTimeBegin) {
		this.attackTimeBegin = attackTimeBegin;
	}
	
	public void setAttackTimeEnd(float attackTimeEnd) {
		this.attackTimeEnd = attackTimeEnd;
	}
	
	public void setBackgroundClearColor(Vec4f clearColor) {
		this.backgroundClearColor = clearColor;
	}
	
	public void enableCameraControll(boolean enable) {
		this.cameraMoveEnabled = enable;
		this.cameraRotationEnabled = enable;
		this.zoomingCameraEnabled = enable;
	}
	
	public void enableCameraMove(boolean enable) {
		this.cameraMoveEnabled = enable;
	}
	
	public void enableCameraRotation(boolean enable) {
		this.cameraRotationEnabled = enable;
	}
	
	public void enableZoomingCamera(boolean enable) {
		this.zoomingCameraEnabled = enable;
	}
	
	public void addAnimationToPlay(AssetAccessor<? extends StaticAnimation> animation) {
        // Blocks crashes for exceptional cases
        if (animation == null) {
            return;
        }

		if (this.index == -1) {
			this.index = 0;
		}
		
		this.animationsToPlay.add(animation);
		this.animator.playAnimation(animation, 0.0F);
	}
	
	public void removeAnimationPlayingAnimation(AssetAccessor<? extends StaticAnimation> animation) {
		this.animationsToPlay.remove(animation);
	}
	
	public void clearAnimations() {
		this.index = -1;
		this.animationsToPlay.clear();
		this.animator.playAnimation(Animations.EMPTY_ANIMATION, 0.0F);
		
		((ClientAnimator)this.animator).iterAllLayers((layer) -> {
			layer.off(this.animator.getEntityPatch());
		});
		
		this.animator.playAnimation(Animations.OFF_ANIMATION_HIGHEST, 0.0F);
		this.animator.playAnimation(Animations.OFF_ANIMATION_MIDDLE, 0.0F);
		this.animator.playAnimation(Animations.OFF_ANIMATION_LOWEST, 0.0F);
	}
	
	public void restartAnimations() {
		this.index = 0;
		
		if (this.animationsToPlay.size() == 0) {
			this.index = -1;
			return;
		}
		
		this.animator.playAnimation(this.animationsToPlay.get(0), 0.0F);
	}
	
	@Override
	public void _tick() {
		if (this.animator != null) {
			this.animator.tick();
		}
		
		this.yRotO = this.yRot;
		this.trailParticles.forEach((trail) -> trail.tick());
		this.trailParticles.removeIf((trail) -> !trail.isAlive());
		
		if (this.entitypatch != null && this.clothSimulator != null) {
			this.clothSimulator.tick(this.entitypatch);
		}
	}
	
	@Override
	public boolean mouseClicked(double x, double y, int button) {
		if (this.active && this.visible) {
			if (this.isValidClickButton(button)) {
				boolean flag = this.clicked(x, y);
				
				if (flag) {
					this.playDownSound(Minecraft.getInstance().getSoundManager());
					
					if (this.item != null) {
						if (this.showItemCheckbox.mouseClicked(x, y, button)) {
							return true;
						}
					}
					
					if (!this.trailInfoList.isEmpty()) {
						if (this.showTrailCheckbox.mouseClicked(x, y, button)) {
							return true;
						}
					}
					
					if (this.collider != null) {
						if (this.showColliderCheckbox.mouseClicked(x, y, button)) {
							return true;
						}
					}
					
					return true;
				}
			}
		}
		
		return false;
	}
	
	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
		if (this.isMouseOver(mouseX, mouseY)) {
			if (button == 0) {
				if (this.cameraRotationEnabled) {
					this.xRot = (float)Mth.clamp(this.xRot + dy * 2.5D, -30.0D, 45.0D);
					this.yRot += dx * 2.5D;
				}
			} else if (button == 2) {
				if (this.cameraMoveEnabled) {
					this.xMove += (float)dx * 0.015F * -this.zoom;
					this.yMove += -(float)dy * 0.015F * -this.zoom;
				}
			}
			
			return true;
		}
		
		return false;
	}
	
	@Override
	public boolean mouseScrolled(double x, double y, double xScroll, double yScroll) {
		if (this.zoomingCameraEnabled) {
			this.zoom = Mth.clamp(this.zoom + yScroll * 0.5D, -20.0D, -0.5D);
			return true;
		}
		
		return false;
	}
	
	protected void renderFigure(GuiGraphics guiGraphics, Tesselator tesselator) {
		RenderSystem.enableDepthTest();

        final float partialTicks =((DeltaTracker.Timer)Minecraft.getInstance().getTimer()).deltaTickResidual;

		if (this.mesh != null && this.mesh.get() != null) {
			if (this.animator != null) {
				Pose pose = this.animator.getPose(partialTicks);
				this.mesh.get().initialize();
				OpenMatrix4f[] poseMatrices = this.entitypatch.getArmature().getPoseAsTransformMatrix(pose, false);

				if (this.figureTexture != null) {
					RenderSystem.setShader(GameRenderer::getRendertypeCloudsShader);
					RenderSystem.setShaderTexture(0, this.figureTexture);
                    BufferBuilder bufferbuilder = tesselator.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL);
					this.mesh.get().drawPosed(guiGraphics.pose(), bufferbuilder, Mesh.DrawingFunction.POSITION_TEX_COLOR_NORMAL, -1, 0.9411F, 0.9411F, 0.9411F, 1.0F, -1, this.entitypatch.getArmature(), poseMatrices);

                    MeshData meshData = bufferbuilder.build();

                    if (meshData != null) {
                        BufferUploader.drawWithShader(meshData);
                    }
				} else {
					RenderSystem.setShader(EpicFightShaders::getPositionColorNormalShader);
                    BufferBuilder bufferbuilder = tesselator.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
					this.mesh.get().drawPosed(guiGraphics.pose(), bufferbuilder, Mesh.DrawingFunction.POSITION_COLOR_NORMAL, -1, 0.9411F, 0.9411F, 0.9411F, 1.0F, -1, this.entitypatch.getArmature(), poseMatrices);

                    MeshData meshData = bufferbuilder.build();

                    if (meshData != null) {
                        BufferUploader.drawWithShader(meshData);
                    }
				}

				if (this.item != null && this.showItemCheckbox._getValue()) {
					BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
					ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
					ItemStack itemstack = new ItemStack(this.item);
					OpenMatrix4f correction = new OpenMatrix4f().translate(0F, 0F, -0.13F).rotateDeg(-90.0F, Vec3f.X_AXIS);
					OpenMatrix4f handTransform = correction.mulFront(this.entitypatch.getArmature().getBoundTransformFor(pose, this.getArmature().get().searchJointByName("Tool_R")));
					
					guiGraphics.pose().pushPose();
					MathUtils.mulStack(guiGraphics.pose(), handTransform);
					
					BakedModel model = itemRenderer.getItemModelShaper().getItemModel(this.item);
					BakedModel overridedModel = model.getOverrides().resolve(model, itemstack, null, null, 0);
					DynamicTexture light = Minecraft.getInstance().gameRenderer.lightTexture() != null ? ((yesman.epicfight.mixin.client.LightTextureAccessor) Minecraft.getInstance().gameRenderer.lightTexture()).epicfight$getLightTexture() : null;
					
					// Update light color
					light.getPixels().setPixelRGBA(0, 0, 0xFFFFFFFF);
					light.upload();
					
					itemRenderer.render(itemstack, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, false, guiGraphics.pose(), bufferSource, 0, OverlayTexture.NO_OVERLAY, overridedModel);
					bufferSource.endBatch();
					
					guiGraphics.pose().popPose();
				}
				
				if (!this.trailParticles.isEmpty() && this.showTrailCheckbox._getValue()) {
					RenderSystem.setShader(GameRenderer::getParticleShader);
                    RenderSystem.enableBlend();
                    Minecraft.getInstance().gameRenderer.lightTexture().turnOnLightLayer();

					for (CustomTrailParticle trail : this.trailParticles) {
						ParticleRenderType particleRendertype = trail.getRenderType();
                        BufferBuilder bufferbuilder = particleRendertype.begin(tesselator, Minecraft.getInstance().getTextureManager());
						trail.render(bufferbuilder, null, partialTicks);
                        MeshData meshdata = bufferbuilder.build();

                        if (meshdata != null) {
                            BufferUploader.drawWithShader(meshdata);
                        }
					}

                    RenderSystem.depthMask(true);
                    RenderSystem.disableBlend();
                    Minecraft.getInstance().gameRenderer.lightTexture().turnOffLightLayer();
				}
				
				if (this.collider != null && this.showColliderCheckbox._getValue()) {
					RenderType renderType = this.collider.getRenderType();
                    BufferBuilder bufferbuilder = tesselator.begin(renderType.mode(), renderType.format);
					RenderSystem.setShader(GameRenderer::getRendertypeLinesShader);
					
					AnimationPlayer player = this.animator.getPlayerFor(null);
					float elapsedTime = player.getPrevElapsedTime() + (player.getElapsedTime() - player.getPrevElapsedTime()) * partialTicks;
					boolean red = elapsedTime >= this.attackTimeBegin && elapsedTime <= this.attackTimeEnd;
					
					if (this.colliderJoint != null) {
						Pose prevPose = this.animator.getPose(0.0F);
						Pose currentPose = this.animator.getPose(1.0F);
						this.collider.drawInternal(guiGraphics.pose(), bufferbuilder, this.entitypatch.getArmature(), this.colliderJoint, prevPose, currentPose, partialTicks, red ? 0xFFFF0000 : -1);
					} else {
						DynamicAnimation animation = player.getAnimation().get();
						
						if (animation instanceof AttackAnimation attackanimation) {
							Phase phase = attackanimation.getPhaseByTime(elapsedTime);
							
							for (AttackAnimation.JointColliderPair pair : phase.getColliders()) {
								Pose prevPose = animation.getRawPose(player.getPrevElapsedTime());
								Pose currentPose = animation.getRawPose(player.getElapsedTime());
								this.collider.drawInternal(guiGraphics.pose(), bufferbuilder, this.entitypatch.getArmature(), pair.getFirst(), prevPose, currentPose, partialTicks, -1);
							}
						}
					}

                    MeshData meshData = bufferbuilder.build();

                    if (meshData != null) {
                        RenderSystem.lineWidth(3.0F);
                        RenderSystem.disableCull();
                        BufferUploader.drawWithShader(meshData);
                        RenderSystem.lineWidth(1.0F);
                        RenderSystem.enableCull();
                    }
				}
			} else {
				RenderSystem.setShader(EpicFightShaders::getPositionColorNormalShader);
				this.mesh.get().initialize();
				
				if (this.figureTexture != null) {
					RenderSystem.setShader(GameRenderer::getRendertypeCloudsShader);
					RenderSystem.setShaderTexture(0, this.figureTexture);
                    BufferBuilder bufferbuilder = tesselator.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL);
					this.mesh.get().draw(guiGraphics.pose(), bufferbuilder, Mesh.DrawingFunction.POSITION_TEX_COLOR_NORMAL, -1, 0.9411F, 0.9411F, 0.9411F, 1.0F, OverlayTexture.NO_OVERLAY);
                    MeshData meshData = bufferbuilder.build();

                    if (meshData != null) {
                        RenderSystem.lineWidth(3.0F);
                        RenderSystem.disableCull();
                        BufferUploader.drawWithShader(meshData);
                    }
				} else {
					RenderSystem.setShader(EpicFightShaders::getPositionColorNormalShader);
                    BufferBuilder  bufferbuilder = tesselator.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
					this.mesh.get().draw(guiGraphics.pose(), bufferbuilder, Mesh.DrawingFunction.POSITION_COLOR_NORMAL, -1, 0.9411F, 0.9411F, 0.9411F, 1.0F, OverlayTexture.NO_OVERLAY);
                    MeshData meshData = bufferbuilder.build();

                    if (meshData != null) {
                        RenderSystem.lineWidth(3.0F);
                        RenderSystem.disableCull();
                        BufferUploader.drawWithShader(meshData);
                    }
				}
			}
		}
		
		if (this.cloakMesh != null && this.cloakTexture != null) {
			TextureManager textureManager = Minecraft.getInstance().getTextureManager();
			AbstractTexture texture = textureManager.getTexture(this.cloakTexture);
			
			if (texture != MissingTextureAtlasSprite.getTexture()) {
				this.cloakMesh.getOriginalMesh().initialize();
				
				if (this.animator != null && this.entitypatch != null && this.clothSimulator != null) {
					this.clothSimulator.getRunningObject(ClothSimulator.MODELPREVIEWER_CLOAK).ifPresent((clothObj) -> {
			            Function<Float, OpenMatrix4f> partialColliderTransformProvider = (partialFrame) -> {
							Vec3 pos = this.entitypatch.getAccuratePartialLocation(partialFrame);
							float yRotLerp = this.entitypatch.getAccurateYRot(partialFrame);
							return OpenMatrix4f.createTranslation((float)pos.x, (float)pos.y, (float)pos.z).rotateDeg(180.0F - yRotLerp, Vec3f.Y_AXIS);
			            };
			            
			            Pose pose = this.animator.getPose(partialTicks);
			            this.entitypatch.getArmature().setPose(pose);
			            
						clothObj.tick(this.entitypatch, partialColliderTransformProvider, partialTicks, this.entitypatch.getArmature(), this.entitypatch.getArmature().getPoseMatrices());
						RenderSystem.setShader(GameRenderer::getRendertypeCloudsShader);
						RenderSystem.setShaderTexture(0, this.cloakTexture);
						BufferBuilder bufferbuilder$2 = tesselator.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL);
						
						guiGraphics.pose().pushPose();
						guiGraphics.pose().mulPose(Axis.YP.rotationDegrees(this.entitypatch.getYRot() - 180.0F));
						clothObj.drawPosed(guiGraphics.pose(), bufferbuilder$2, Mesh.DrawingFunction.POSITION_TEX_COLOR_NORMAL, -1, this.cloakColor.x, this.cloakColor.y, this.cloakColor.z, 1.0F, OverlayTexture.NO_OVERLAY, this.entitypatch.getArmature(), this.entitypatch.getArmature().getPoseMatrices());
						guiGraphics.pose().popPose();
						
						BufferUploader.drawWithShader(bufferbuilder$2.buildOrThrow());
					});
				} else {
					RenderSystem.setShader(GameRenderer::getRendertypeCloudsShader);
					BufferBuilder bufferbuilder = tesselator.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL);
					RenderSystem.setShaderTexture(0, this.cloakTexture);
					((Mesh)this.cloakMesh).draw(guiGraphics.pose(), bufferbuilder, Mesh.DrawingFunction.POSITION_TEX_COLOR_NORMAL, -1, this.cloakColor.x, this.cloakColor.y, this.cloakColor.z, 1.0F, OverlayTexture.NO_OVERLAY);

                    MeshData meshData = bufferbuilder.build();

                    if (meshData != null) {
					    BufferUploader.drawWithShader(meshData);
                    }
				}
			}
		}
		
		RenderSystem.disableDepthTest();
	}
	
	@Override
	public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		Minecraft minecraft = Minecraft.getInstance();
		minecraft.getMainRenderTarget().unbindWrite();
		
		RenderSystem.enableDepthTest();
		
		ScreenRectangle screenrectangle = null;
		boolean scissorApplied = guiGraphics.scissorStack.stack.size() > 0;
		
		// If scissor test is enabled, remove it.
		if (scissorApplied) {
			screenrectangle = guiGraphics.scissorStack.stack.peekLast();
			guiGraphics.disableScissor();
		}
		
		this.modelRenderTarget.clear(true);
		this.modelRenderTarget.bindWrite(true);
		
		if (this.backgroundClearColor != null) {
			this.modelRenderTarget.setClearColor(this.backgroundClearColor.x, this.backgroundClearColor.y, this.backgroundClearColor.z, this.backgroundClearColor.w);
		}
		
		Tesselator tesselator = RenderSystem.renderThreadTesselator();
		Matrix4f oldProjection = RenderSystem.getProjectionMatrix();
		Matrix4f perspective = new Matrix4f().setPerspective((float)Math.toRadians(50.0F), (float)this.width / (float)this.height, 0.05F, 100.0F);
		
		ShaderInstance prevShader = RenderSystem.getShader();
		RenderSystem.setProjectionMatrix(perspective, VertexSorting.DISTANCE_TO_ORIGIN);
		RenderSystem.getModelViewStack().pushMatrix();
		RenderSystem.getModelViewStack().identity();
		RenderSystem.applyModelViewMatrix();
		
		guiGraphics.pose().pushPose();
		guiGraphics.pose().translate(this.xMove, this.yMove - 1.0D, this.zoom);
		guiGraphics.pose().mulPose(Axis.XP.rotationDegrees(this.xRot));
		guiGraphics.pose().mulPose(Axis.YP.rotationDegrees(this.yRot));
		
		this.renderFigure(guiGraphics, tesselator);
		
		guiGraphics.pose().popPose();
		
		RenderSystem.setProjectionMatrix(oldProjection, VertexSorting.ORTHOGRAPHIC_Z);
		RenderSystem.getModelViewStack().popMatrix();
		RenderSystem.applyModelViewMatrix();
		RenderSystem.setShader(() -> prevShader);
		
		this.modelRenderTarget.unbindWrite();
		
		minecraft.getMainRenderTarget().bindWrite(true);
		
		if (scissorApplied) {
			guiGraphics.enableScissor(screenrectangle.left(), screenrectangle.top(), screenrectangle.right(), screenrectangle.bottom());
		}

		this.modelRenderTarget.blitToScreen(guiGraphics);

		if (this.animator != null) {
			// Visibility control widgets
			int top = this._getY() + 6;
			int right = this._getX() + this._getWidth() - 2;
			
			if (!this.trailInfoList.isEmpty()) {
				right -= this.showTrailCheckbox._getWidth();
				
				this.showTrailCheckbox._setX(right);
				this.showTrailCheckbox._setY(top);
				this.showTrailCheckbox._renderWidget(guiGraphics, mouseX, mouseY, partialTicks);
			}
			
			if (this.item != null) {
				right -= this.showItemCheckbox._getWidth();
				
				this.showItemCheckbox._setX(right);
				this.showItemCheckbox._setY(top);
				this.showItemCheckbox._renderWidget(guiGraphics, mouseX, mouseY, partialTicks);
			}
			
			if (this.collider != null) {
				right -= this.showColliderCheckbox._getWidth();
				
				this.showColliderCheckbox._setX(right);
				this.showColliderCheckbox._setY(top);
				this.showColliderCheckbox._renderWidget(guiGraphics, mouseX, mouseY, partialTicks);
			}
		}
		
		RenderSystem.disableDepthTest();
	}
	
	@Override
	protected void updateWidgetNarration(NarrationElementOutput narrationElementInput) {
		narrationElementInput.add(NarratedElementType.TITLE, this.createNarrationMessage());
	}
	
	@Override
	public void resize(ScreenRectangle screenRectangle) {
		if (this.getHorizontalSizingOption() != null) {
			this.getHorizontalSizingOption().resizeFunction.resize(this, screenRectangle, this.getX1(), this.getX2());
		}
		
		if (this.getVerticalSizingOption() != null) {
			this.getVerticalSizingOption().resizeFunction.resize(this, screenRectangle, this.getY1(), this.getY2());
		}
		
		double guiScale = Minecraft.getInstance().getWindow().getGuiScale();
		this.modelRenderTarget.resize(Math.max(8, this._getWidth() * (int)guiScale), Math.max(8, this._getHeight() * (int)guiScale), true);
	}
	
	public void onDestroy() {
		this.modelRenderTarget.destroyBuffers();
	}
	
	public class FakeEntityPatch extends LivingEntityPatch<LivingEntity> implements SimulatableObject, ClothSimulatable {
		public FakeEntityPatch(Armature armature) {
			super(null);
			this.armature = armature.deepCopy();
		}
		
		public void setAnimator() {
			this.animator = ModelPreviewer.this.animator;
		}
		
		@Override
		public void initAnimator(Animator animator) {
			this.animator = animator;
		}
		
		@Override
		public void updateMotion(boolean considerInaction) {
			
		}
		
		@Override
		public AssetAccessor<? extends StaticAnimation> getHitAnimation(StunType stunType) {
			return null;
		}
		
		@Override
		public boolean isLogicalClient() {
			return true;
		}
		
		@Override
		public void cancelItemUse() {
		}
		
		@Override
		public float getAttackDirectionPitch(float partialTick) {
			return 0.0F;
		}
		
		@Override
		public OpenMatrix4f getModelMatrix(float partialTicks) {
			return MathUtils.getModelMatrixIntegral(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, partialTicks, 1.0F, 1.0F, 1.0F);
		}
		
		@Override
		public void poseTick(DynamicAnimation animation, Pose pose, float time, float partialTicks) {
		}
		
		@Override
		public void updateEntityState() {
		}
		
		@Override
		public boolean invalid() {
			return false;
		}
		
		@Override
		public Vec3 getObjectVelocity() {
			return Vec3.ZERO;
		}
		
		@Override
		public Vec3 getAccurateCloakLocation(float partialFrame) {
			return Vec3.ZERO;
		}
		
		@Override
		public Vec3 getAccuratePartialLocation(float partialFrame) {
			return Vec3.ZERO;
		}
		
		@Override
		public float getAccurateYRot(float partialFrame) {
			return ModelPreviewer.this.yRot;
		}
		
		@Override
		public float getYRotDelta(float partialFrame) {
			return ModelPreviewer.this.yRot - ModelPreviewer.this.yRotO;
		}
		
		@Override
		public float getYRot() {
			return ModelPreviewer.this.yRot;
		}
		
		@Override
		public float getYRotO() {
			return ModelPreviewer.this.yRotO;
		}
		
		@Override
		public <SIM extends PhysicsSimulator<?, ?, ?, ?, ?>> Optional<SIM> getSimulator(SimulationTypes<?, ?, ?, ?, ?, SIM> simulationType) {
			return Optional.empty();
		}

		@Override
		public float getScale() {
			return 1.0F;
		}

		@Override
		public Animator getSimulatableAnimator() {
			return this.animator;
		}

		@Override
		public float getGravity() {
			return 9.8F;
		}

		@Override
		public ClothSimulator getClothSimulator() {
			return null;
		}
		
		@Override
		public Faction getFaction() {
			return null;
		}
		
		@Override
		public boolean isFakeEntity() {
			return true;
		}
	}
	
	public class NoEntityAnimator extends ClientAnimator {
		public NoEntityAnimator(LivingEntityPatch<?> entitypatch) {
			super(entitypatch, NoEntityBaseLayer::new);
		}
		
		@Override
		public void tick() {
			this.baseLayer.update(this.entitypatch);
			
			if (this.baseLayer.animationPlayer.isEnd() && this.baseLayer.getNextAnimation() == null) {
				AssetAccessor<? extends StaticAnimation> toPlay = ModelPreviewer.this.index > -1 && ModelPreviewer.this.index < ModelPreviewer.this.animationsToPlay.size() ? ModelPreviewer.this.animationsToPlay.get(ModelPreviewer.this.index) : Animations.EMPTY_ANIMATION;
				this.baseLayer.playAnimation(toPlay, this.entitypatch, 0.0F);
				
				if (!ModelPreviewer.this.trailInfoList.isEmpty()) {
					for (TrailInfo trailInfo : ModelPreviewer.this.trailInfoList) {
						if (trailInfo.playable()) {
							CustomTrailParticle trail = new CustomTrailParticle(ModelPreviewer.this.entitypatch.getArmature().searchJointByName(trailInfo.joint()), toPlay, trailInfo);
							ModelPreviewer.this.trailParticles.add(trail);
						} else {
							toPlay.get().getProperty(ClientAnimationProperties.TRAIL_EFFECT).ifPresent(trailInfos -> {
								for (TrailInfo info : trailInfos) {
									if (info.hand() != InteractionHand.MAIN_HAND) {
										continue;
									}
									
									TrailInfo combinedTrailInfo = trailInfo.overwrite(info);
									
									if (combinedTrailInfo.playable()) {
										CustomTrailParticle trail = new CustomTrailParticle(ModelPreviewer.this.entitypatch.getArmature().searchJointByName(combinedTrailInfo.joint()), toPlay, combinedTrailInfo);
										ModelPreviewer.this.trailParticles.add(trail);
									}
								}
							});
						}
					}
				}
				
				ModelPreviewer.this.index = (ModelPreviewer.this.index + 1) % ModelPreviewer.this.animationsToPlay.size();
			}
		}
		
		public LivingEntityPatch<?> getEntityPatch() {
			return this.entitypatch;
		}
		
		static class NoEntityAnimationPlayer extends AnimationPlayer {
			@Override
			public void tick(LivingEntityPatch<?> entitypatch) {
				DynamicAnimation nowPlay = this.getAnimation().get();
				
				this.prevElapsedTime = this.elapsedTime;
				this.elapsedTime += EpicFightSharedConstants.A_TICK * (this.isReversed() && nowPlay.canBePlayedReverse() ? -1.0F : 1.0F);
				
				if (this.elapsedTime >= nowPlay.getTotalTime()) {
					if (nowPlay.isRepeat()) {
						this.prevElapsedTime = 0;
						this.elapsedTime %= nowPlay.getTotalTime();
					} else {
						this.elapsedTime = nowPlay.getTotalTime();
						this.isEnd = true;
					}
				} else if (this.elapsedTime < 0) {
					if (nowPlay.isRepeat()) {
						this.prevElapsedTime = nowPlay.getTotalTime();
						this.elapsedTime = nowPlay.getTotalTime() + this.elapsedTime;
					} else {
						this.elapsedTime = 0.0F;
						this.isEnd = true;
					}
				}
			}
			
			@Override
			public void begin(AssetAccessor<? extends DynamicAnimation> animation, LivingEntityPatch<?> entitypatch) {
			}
			
			@Override
			public Pose getCurrentPose(LivingEntityPatch<?> entitypatch, float partialTicks) {
				return this.play.get().getRawPose(this.prevElapsedTime + (this.elapsedTime - this.prevElapsedTime) * partialTicks);
			}
		}
		
		static class NoEntityLayer extends Layer {
			public NoEntityLayer(Priority priority) {
				super(priority, NoEntityAnimationPlayer::new);
			}
			
			public void playAnimation(AssetAccessor<? extends StaticAnimation> nextAnimation, LivingEntityPatch<?> entitypatch, float convertTimeModifier) {
				Pose lastPose = entitypatch.getAnimator().getPose(1.0F);
				this.resume();
				
				if (!nextAnimation.get().isMetaAnimation()) {
					this.setLinkAnimation(nextAnimation, entitypatch, lastPose, convertTimeModifier);
					this.linkAnimation.putOnPlayer(this.animationPlayer, entitypatch);
					this.nextAnimation = nextAnimation;
				}
			}
			
			@Override
			public void playAnimationInstantly(AssetAccessor<? extends DynamicAnimation> nextAnimation, LivingEntityPatch<?> entitypatch) {
				this.resume();
				nextAnimation.get().putOnPlayer(this.animationPlayer, entitypatch);
				this.nextAnimation = null;
			}
			
			@Override
			protected void setLinkAnimation(AssetAccessor<? extends StaticAnimation> nextAnimation, LivingEntityPatch<?> entitypatch, Pose lastPose, float convertTimeModifier) {
				Pose currentPose = this.animationPlayer.getAnimation().get().getRawPose(this.animationPlayer.getElapsedTime());
				Pose nextAnimationPose = nextAnimation.get().getRawPose(0.0F);
				float totalTime = nextAnimation.get().getTransitionTime();
				
				AssetAccessor<? extends DynamicAnimation> fromAnimation = this.animationPlayer.isEmpty() ? entitypatch.getClientAnimator().baseLayer.animationPlayer.getAnimation() : this.animationPlayer.getAnimation();
				
				if (fromAnimation instanceof LinkAnimation linkAnimation) {
					fromAnimation = linkAnimation.getFromAnimation();
				}
				
				this.linkAnimation.getAnimationClip().reset();
				this.linkAnimation.setTotalTime(totalTime);
				this.linkAnimation.setConnectedAnimations(fromAnimation, nextAnimation);
				
				Map<String, JointTransform> data1 = currentPose.getJointTransformData();
				Map<String, JointTransform> data2 = nextAnimationPose.getJointTransformData();
				
				for (String jointName : data1.keySet()) {
					if (data1.containsKey(jointName) && data2.containsKey(jointName)) {
						Keyframe[] keyframes = new Keyframe[2];
						keyframes[0] = new Keyframe(0.0F, data1.get(jointName));
						keyframes[1] = new Keyframe(totalTime, data2.get(jointName));
						TransformSheet sheet = new TransformSheet(keyframes);
						this.linkAnimation.getAnimationClip().addJointTransform(jointName, sheet);
					}
				}
				
				this.animationPlayer.setPlayAnimation(this.linkAnimation);
			}
			
			public void update(LivingEntityPatch<?> entitypatch) {
				if (this.paused) {
					this.animationPlayer.setElapsedTime(this.animationPlayer.getElapsedTime());
				} else {
					this.animationPlayer.tick(entitypatch);
				}
				
				if (!this.paused && this.animationPlayer.isEnd()) {
					if (this.nextAnimation != null) {
						this.nextAnimation.get().putOnPlayer(this.animationPlayer, entitypatch);
						this.nextAnimation = null;
					} else {
						if (this.animationPlayer.getAnimation() instanceof LayerOffAnimation) {
							this.animationPlayer.getAnimation().get().end(entitypatch, Animations.EMPTY_ANIMATION, true);
						} else {
							this.off(entitypatch);
						}
					}
				}
			}
			
			public Pose getEnabledPose(LivingEntityPatch<?> entitypatch, float partialTick) {
				DynamicAnimation animation = this.animationPlayer.getAnimation().get();
				Pose pose = animation.getRawPose(this.animationPlayer.getPrevElapsedTime() + (this.animationPlayer.getElapsedTime() - this.animationPlayer.getPrevElapsedTime()) * partialTick);
				pose.disableJoint((entry) -> !animation.hasTransformFor(entry.getKey()));
				
				return pose;
			}
			
			public void off(LivingEntityPatch<?> entitypatch) {
				if (!this.isDisabled() && !(this.animationPlayer.getAnimation() instanceof LayerOffAnimation)) {
					float convertTime = entitypatch.getClientAnimator().baseLayer.animationPlayer.getAnimation().get().getTransitionTime();
					setLayerOffAnimation(this.animationPlayer.getAnimation(), this.getEnabledPose(entitypatch, 1.0F), this.layerOffAnimation, convertTime);
					this.playAnimationInstantly(this.layerOffAnimation, entitypatch);
				}
			}
		}
		
		static class NoEntityBaseLayer extends Layer.BaseLayer {
			public NoEntityBaseLayer() {
				super(NoEntityAnimationPlayer::new);
				
				this.compositeLayers.clear();
				
				for (Priority priority : Priority.values()) {
					this.compositeLayers.computeIfAbsent(priority, NoEntityLayer::new);
				}
				
				this.baseLayerPriority = Priority.LOWEST;
			}
			
			@Override
			public void playAnimation(AssetAccessor<? extends StaticAnimation> nextAnimation, LivingEntityPatch<?> entitypatch, float convertTimeModifier) {
				Priority priority = nextAnimation.get().getPriority();
				this.baseLayerPriority = priority;
				this.offCompositeLayersLowerThan(entitypatch, nextAnimation);
				
				Pose lastPose = entitypatch.getAnimator().getPose(1.0F);
				this.resume();
				
				if (!nextAnimation.get().isMetaAnimation()) {
					this.setLinkAnimation(nextAnimation, entitypatch, lastPose, convertTimeModifier);
					this.linkAnimation.putOnPlayer(this.animationPlayer, entitypatch);
					entitypatch.updateEntityState();
					this.nextAnimation = nextAnimation;
				}
			}
			
			@Override
			public void playAnimationInstantly(AssetAccessor<? extends DynamicAnimation> nextAnimation, LivingEntityPatch<?> entitypatch) {
				this.resume();
				nextAnimation.get().putOnPlayer(this.animationPlayer, entitypatch);
				this.nextAnimation = null;
			}
			
			@Override
			protected void playLivingAnimation(AssetAccessor<? extends StaticAnimation> nextAnimation, LivingEntityPatch<?> entitypatch) {
				this.resume();
				
				if (!nextAnimation.get().isMetaAnimation()) {
					this.concurrentLinkAnimation.acceptFrom(this.animationPlayer.getAnimation().get().getRealAnimation(), nextAnimation, this.animationPlayer.getElapsedTime());
					this.concurrentLinkAnimation.putOnPlayer(this.animationPlayer, entitypatch);
					this.nextAnimation = nextAnimation;
				}
			}
			
			@Override
			public void update(LivingEntityPatch<?> entitypatch) {
				if (this.paused) {
					this.animationPlayer.setElapsedTime(this.animationPlayer.getElapsedTime());
				} else {
					this.animationPlayer.tick(entitypatch);
				}
				
				if (!this.paused && this.animationPlayer.isEnd()) {
					if (this.nextAnimation != null) {
						this.nextAnimation.get().putOnPlayer(this.animationPlayer, entitypatch);
						this.nextAnimation = null;
					} else {
						if (this.animationPlayer.getAnimation() instanceof LayerOffAnimation) {
							this.animationPlayer.getAnimation().get().end(entitypatch, Animations.EMPTY_ANIMATION, true);
						} else {
							this.off(entitypatch);
						}
					}
				}
				
				for (Layer layer : this.compositeLayers.values()) {
					layer.update(entitypatch);
				}
			}
			
			@Override
			protected void setLinkAnimation(AssetAccessor<? extends StaticAnimation> nextAnimation, LivingEntityPatch<?> entitypatch, Pose lastPose, float convertTimeModifier) {
				Pose currentPose = this.animationPlayer.getAnimation().get().getRawPose(this.animationPlayer.getElapsedTime());
				Pose nextAnimationPose = nextAnimation.get().getRawPose(0.0F);
				float totalTime = nextAnimation.get().getTransitionTime();
				
				AssetAccessor<? extends DynamicAnimation> fromAnimation = this.animationPlayer.isEmpty() ? entitypatch.getClientAnimator().baseLayer.animationPlayer.getAnimation() : this.animationPlayer.getAnimation();
				
				if (fromAnimation instanceof LinkAnimation linkAnimation) {
					fromAnimation = linkAnimation.getFromAnimation();
				}
				
				this.linkAnimation.getAnimationClip().reset();
				this.linkAnimation.setTotalTime(totalTime);
				this.linkAnimation.setConnectedAnimations(fromAnimation, nextAnimation);
				
				Map<String, JointTransform> data1 = currentPose.getJointTransformData();
				Map<String, JointTransform> data2 = nextAnimationPose.getJointTransformData();
				
				for (String jointName : data1.keySet()) {
					if (data1.containsKey(jointName) && data2.containsKey(jointName)) {
						Keyframe[] keyframes = new Keyframe[2];
						keyframes[0] = new Keyframe(0.0F, data1.get(jointName));
						keyframes[1] = new Keyframe(totalTime, data2.get(jointName));
						TransformSheet sheet = new TransformSheet(keyframes);
						this.linkAnimation.getAnimationClip().addJointTransform(jointName, sheet);
					}
				}
				
				this.animationPlayer.setPlayAnimation(this.linkAnimation);
			}
			
			public void offCompositeLayerLowerThan(LivingEntityPatch<?> entitypatch, StaticAnimation nextAnimation) {
				for (Priority p : nextAnimation.getPriority().lowersAndEqual()) {
					if (p == Priority.LOWEST && !nextAnimation.isMainFrameAnimation()) {
						continue;
					}
					
					this.compositeLayers.get(p).off(entitypatch);
				}
			}
			
			public Layer getLayer(Priority priority) {
				return this.compositeLayers.get(priority);
			}
			
			@Override
			public void off(LivingEntityPatch<?> entitypatch) {
			}
			
			@Override
			protected boolean isDisabled() {
				return false;
			}
			
			@Override
			protected boolean isBaseLayer() {
				return true;
			}
		}
	}
	
	class ModelRenderTarget extends RenderTarget {
		public ModelRenderTarget() {
			super(true);
			
			RenderSystem.assertOnRenderThreadOrInit();
			Window window = Minecraft.getInstance().getWindow();
			
			this.resize(window.getWidth(), window.getHeight(), false);
		}
		
		private void blitToScreen(GuiGraphics guiGraphics) {
			RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
			RenderSystem.setShaderTexture(0, this.colorTextureId);
			
			float left = ModelPreviewer.this._getX();
			float top = ModelPreviewer.this._getY();
			float right = left + ModelPreviewer.this._getWidth();
			float bottom = top + ModelPreviewer.this._getHeight();
			
			float u = (float) this.viewWidth / (float) this.width;
			float v = (float) this.viewHeight / (float) this.height;
			
			guiGraphics.pose().pushPose();
			
			PoseStack.Pose lastPose = guiGraphics.pose().last();
			Tesselator tesselator = Tesselator.getInstance();
			BufferBuilder bufferbuilder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
			bufferbuilder.addVertex(lastPose, left, bottom, 0.0F).setUv(0.0F, 0.0F).setColor(255, 255, 255, 255);
			bufferbuilder.addVertex(lastPose, right, bottom, 0.0F).setUv(u, 0.0F).setColor(255, 255, 255, 255);
			bufferbuilder.addVertex(lastPose, right, top, 0.0F).setUv(u, v).setColor(255, 255, 255, 255);
			bufferbuilder.addVertex(lastPose, left, top, 0.0F).setUv(0.0F, v).setColor(255, 255, 255, 255);
			BufferUploader.drawWithShader(bufferbuilder.buildOrThrow());
			
			guiGraphics.pose().popPose();
		}
	}
	
	class CustomTrailParticle extends AnimationTrailParticle {
		@SuppressWarnings("deprecation")
		protected CustomTrailParticle(Joint joint, AssetAccessor<? extends StaticAnimation> animation, TrailInfo trailInfo) {
			super(ModelPreviewer.this.entitypatch.getArmature(), ModelPreviewer.this.entitypatch, joint, animation, trailInfo);
		}

        @Override
        public ParticleRenderType getRenderType() {
            return EpicFightParticleRenderTypes.TRAIL_EFFECT.apply(this.trailInfo.texturePath());
        }

		@Override
		public void tick() {
			AnimationPlayer animPlayer = ModelPreviewer.this.animator.getPlayerFor(null);
			this.trailEdges.removeIf(v -> !v.isAlive());
			
			if (this.shouldRemove) {
				if (this.lifetime-- == 0) {
					this.remove();
				}
			} else {
				if (this.animation != animPlayer.getRealAnimation() || animPlayer.getElapsedTime() > this.trailInfo.endTime()) {
					this.shouldRemove = true;
					this.lifetime = this.trailInfo.trailLifetime();
				}
			}
			
			if (this.trailInfo.fadeTime() > 0.0F && this.trailInfo.endTime() < animPlayer.getElapsedTime()) {
				return;
			}
			
			boolean isTrailInvisible = animPlayer.getAnimation().get().isLinkAnimation() || animPlayer.getElapsedTime() <= this.trailInfo.startTime();
			boolean isFirstTrail = this.trailEdges.isEmpty();
			boolean needCorrection = (!isTrailInvisible && isFirstTrail);
			
			if (needCorrection) {
				float startCorrection = Math.max((this.trailInfo.startTime() - animPlayer.getPrevElapsedTime()) / (animPlayer.getElapsedTime() - animPlayer.getPrevElapsedTime()), 0.0F);
				this.startEdgeCorrection = this.trailInfo.interpolateCount() * 2 * startCorrection;
			}
			
			TrailInfo trailInfo = this.trailInfo;
			Pose prevPose = this.owner.getAnimator().getPose(0.0F);
			Pose middlePose = this.owner.getAnimator().getPose(0.5F);
			Pose currentPose = this.owner.getAnimator().getPose(1.0F);
			OpenMatrix4f prevJointTf = ModelPreviewer.this.entitypatch.getArmature().getBoundTransformFor(prevPose, this.joint);
			OpenMatrix4f middleJointTf = ModelPreviewer.this.entitypatch.getArmature().getBoundTransformFor(middlePose, this.joint);
			OpenMatrix4f currentJointTf = ModelPreviewer.this.entitypatch.getArmature().getBoundTransformFor(currentPose, this.joint);
			Vec3 prevStartPos = OpenMatrix4f.transform(prevJointTf, trailInfo.start());
			Vec3 prevEndPos = OpenMatrix4f.transform(prevJointTf, trailInfo.end());
			Vec3 middleStartPos = OpenMatrix4f.transform(middleJointTf, trailInfo.start());
			Vec3 middleEndPos = OpenMatrix4f.transform(middleJointTf, trailInfo.end());
			Vec3 currentStartPos = OpenMatrix4f.transform(currentJointTf, trailInfo.start());
			Vec3 currentEndPos = OpenMatrix4f.transform(currentJointTf, trailInfo.end());
			
			List<Vec3> finalStartPositions;
			List<Vec3> finalEndPositions;
			boolean visibleTrail;
			
			if (isTrailInvisible) {
				finalStartPositions = Lists.newArrayList();
				finalEndPositions = Lists.newArrayList();
				finalStartPositions.add(prevStartPos);
				finalStartPositions.add(middleStartPos);
				finalEndPositions.add(prevEndPos);
				finalEndPositions.add(middleEndPos);
				
				this.invisibleTrailEdges.clear();
				visibleTrail = false;
			} else {
				List<Vec3> startPosList = Lists.newArrayList();
				List<Vec3> endPosList = Lists.newArrayList();
				TrailEdge edge1;
				TrailEdge edge2;
				
				if (isFirstTrail) {
					int lastIdx = this.invisibleTrailEdges.size() - 1;
					edge1 = this.invisibleTrailEdges.get(lastIdx);
					edge2 = new TrailEdge(prevStartPos, prevEndPos, -1);
				} else {
					edge1 = this.trailEdges.get(this.trailEdges.size() - (this.trailInfo.interpolateCount() / 2 + 1));
					edge2 = this.trailEdges.get(this.trailEdges.size() - 1);
					edge2.lifetime++;
				}
				
				startPosList.add(edge1.start);
				endPosList.add(edge1.end);
				startPosList.add(edge2.start);
				endPosList.add(edge2.end);
				startPosList.add(middleStartPos);
				endPosList.add(middleEndPos);
				startPosList.add(currentStartPos);
				endPosList.add(currentEndPos);
				
				finalStartPositions = CubicBezierCurve.getBezierInterpolatedPoints(startPosList, 1, 3, this.trailInfo.interpolateCount());
				finalEndPositions = CubicBezierCurve.getBezierInterpolatedPoints(endPosList, 1, 3, this.trailInfo.interpolateCount());
				
				if (!isFirstTrail) {
					finalStartPositions.remove(0);
					finalEndPositions.remove(0);
				}
				
				visibleTrail = true;
			}
			
			this.makeTrailEdges(finalStartPositions, finalEndPositions, visibleTrail ? this.trailEdges : this.invisibleTrailEdges);
		}
		
		@Override
		public void render(VertexConsumer vertexConsumer, Camera camera, float partialTick) {
			if (this.trailEdges.isEmpty()) {
				return;
			}

			PoseStack poseStack = new PoseStack();
			this.setupPoseStack(poseStack, camera, partialTick);
			Matrix4f matrix4f = poseStack.last().pose();
			int edges = this.trailEdges.size() - 1;
			boolean startFade = this.trailEdges.get(0).lifetime == 1;
			boolean endFade = this.trailEdges.get(edges).lifetime == this.trailInfo.trailLifetime();
			float startEdge = (startFade ? this.trailInfo.interpolateCount() * 2 * partialTick : 0.0F) + this.startEdgeCorrection;
			float endEdge = endFade ? Math.min(edges - (this.trailInfo.interpolateCount() * 2) * (1.0F - partialTick), edges - 1) : edges - 1;
			float interval = 1.0F / (endEdge - startEdge);
			float fading = 1.0F;
			
			if (this.shouldRemove) {
				if (TrailInfo.isValidTime(this.trailInfo.fadeTime())) {
					fading = ((float)this.lifetime / (float)this.trailInfo.trailLifetime());
				} else {
					fading = Mth.clamp((this.lifetime + (1.0F - partialTick)) / this.trailInfo.trailLifetime(), 0.0F, 1.0F);
				}
			}
			
			float partialStartEdge = interval * (startEdge % 1.0F);
			float from = -partialStartEdge;
			float to = -partialStartEdge + interval;
			
			for (int i = (int)(startEdge); i < (int)endEdge + 1; i++) {
				TrailEdge e1 = this.trailEdges.get(i);
				TrailEdge e2 = this.trailEdges.get(i + 1);
				Vector4f pos1 = new Vector4f((float)e1.start.x, (float)e1.start.y, (float)e1.start.z, 1.0F);
				Vector4f pos2 = new Vector4f((float)e1.end.x, (float)e1.end.y, (float)e1.end.z, 1.0F);
				Vector4f pos3 = new Vector4f((float)e2.end.x, (float)e2.end.y, (float)e2.end.z, 1.0F);
				Vector4f pos4 = new Vector4f((float)e2.start.x, (float)e2.start.y, (float)e2.start.z, 1.0F);
				
				pos1.mul(matrix4f);
				pos2.mul(matrix4f);
				pos3.mul(matrix4f);
				pos4.mul(matrix4f);
				
				float alphaFrom = Mth.clamp(from, 0.0F, 1.0F);
				float alphaTo = Mth.clamp(to, 0.0F, 1.0F);
				
				vertexConsumer.addVertex(pos1.x(), pos1.y(), pos1.z()).setUv(from, 1.0F).setColor(this.rCol, this.gCol, this.bCol, this.alpha * alphaFrom * fading).setLight(0);
				vertexConsumer.addVertex(pos2.x(), pos2.y(), pos2.z()).setUv(from, 0.0F).setColor(this.rCol, this.gCol, this.bCol, this.alpha * alphaFrom * fading).setLight(0);
				vertexConsumer.addVertex(pos3.x(), pos3.y(), pos3.z()).setUv(to, 0.0F).setColor(this.rCol, this.gCol, this.bCol, this.alpha * alphaTo * fading).setLight(0);
				vertexConsumer.addVertex(pos4.x(), pos4.y(), pos4.z()).setUv(to, 1.0F).setColor(this.rCol, this.gCol, this.bCol, this.alpha * alphaTo * fading).setLight(0);

				from += interval;
				to += interval;
			}
		}
		
		@Override
		protected void setupPoseStack(PoseStack poseStack, Camera camera, float partialTicks) {
			float x = (float)ModelPreviewer.this.xMove;
			float y = (float)ModelPreviewer.this.yMove;
			float z = (float)ModelPreviewer.this.zoom;
			float xRot = ModelPreviewer.this.xRot;
			float yRot = ModelPreviewer.this.yRot;
			
			poseStack.translate(x, y - 1.0D, z);
			poseStack.mulPose(QuaternionUtils.XP.rotationDegrees(xRot));
			poseStack.mulPose(QuaternionUtils.YP.rotationDegrees(yRot));
		}
	}
	
	/*******************************************************************
	 * @ResizableComponent variables                                   *
	 *******************************************************************/
	private int x1;
	private int x2;
	private int y1;
	private int y2;
	private final HorizontalSizing horizontalSizingOption;
	private final VerticalSizing verticalSizingOption;
	
	@Override
	public void setX1(int x1) {
		this.x1 = x1;
	}

	@Override
	public void setX2(int x2) {
		this.x2 = x2;
	}

	@Override
	public void setY1(int y1) {
		this.y1 = y1;
	}

	@Override
	public void setY2(int y2) {
		this.y2 = y2;
	}
	
	@Override
	public int getX1() {
		return this.x1;
	}

	@Override
	public int getX2() {
		return this.x2;
	}

	@Override
	public int getY1() {
		return this.y1;
	}

	@Override
	public int getY2() {
		return this.y2;
	}

	@Override
	public HorizontalSizing getHorizontalSizingOption() {
		return this.horizontalSizingOption;
	}

	@Override
	public VerticalSizing getVerticalSizingOption() {
		return this.verticalSizingOption;
	}

	@Override
	public void _setActive(boolean active) {
	}

	@Override
	public int _getX() {
		return this.getX();
	}

	@Override
	public int _getY() {
		return this.getY();
	}

	@Override
	public int _getWidth() {
		return this.getWidth();
	}

	@Override
	public int _getHeight() {
		return this.getHeight();
	}

	@Override
	public void _setX(int x) {
		this.setX(x);
	}

	@Override
	public void _setY(int y) {
		this.setY(y);
	}

	@Override
	public void _setWidth(int width) {
		this.setWidth(width);
	}

	@Override
	public void _setHeight(int height) {
		this.setHeight(height);
	}

	@Override
	public Component _getMessage() {
		return this.getMessage();
	}

	@Override
	public void _renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		this.renderWidget(guiGraphics, mouseX, mouseY, partialTicks);
	}
	
	public void sysoutTranslationInfo() {
		System.out.println("x move: " + this.xMove +" y move: "+ this.yMove +" x rot: "+ this.xRot +" y rot: "+ this.yRot +" zoom: "+ this.zoom);
	}
}
