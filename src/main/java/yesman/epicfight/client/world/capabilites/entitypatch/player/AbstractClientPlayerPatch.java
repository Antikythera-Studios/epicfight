package yesman.epicfight.client.world.capabilites.entitypatch.player;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PlayerRideableJumping;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector4f;
import yesman.epicfight.api.animation.*;
import yesman.epicfight.api.animation.property.AnimationProperty.StaticAnimationProperty;
import yesman.epicfight.api.animation.types.ActionAnimation;
import yesman.epicfight.api.animation.types.DynamicAnimation;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.client.animation.ClientAnimator;
import yesman.epicfight.api.client.animation.Layer;
import yesman.epicfight.api.client.event.EpicFightClientEventHooks;
import yesman.epicfight.api.client.event.types.entity.ModifyPlayerLivingMotionEvent;
import yesman.epicfight.api.client.event.types.render.ValidatePlayerModelEvent;
import yesman.epicfight.api.client.physics.cloth.ClothSimulatable;
import yesman.epicfight.api.client.physics.cloth.ClothSimulator;
import yesman.epicfight.api.physics.PhysicsSimulator;
import yesman.epicfight.api.physics.SimulationTypes;
import yesman.epicfight.api.utils.EntitySnapshot;
import yesman.epicfight.api.utils.math.MathUtils;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.api.utils.math.Vec3f;
import yesman.epicfight.api.utils.side.ClientOnly;
import yesman.epicfight.client.online.EpicSkins;
import yesman.epicfight.config.ClientConfig;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.network.EntityPairingPacketTypes;
import yesman.epicfight.network.server.SPEntityPairingPacket;
import yesman.epicfight.registry.entries.EpicFightParticles;
import yesman.epicfight.registry.entries.EpicFightSkills;
import yesman.epicfight.registry.entries.EpicFightSounds;
import yesman.epicfight.world.capabilities.entitypatch.EntityDecorations.RenderAttributeModifier;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;

import java.util.Optional;

public class AbstractClientPlayerPatch<T extends AbstractClientPlayer> extends PlayerPatch<T> implements ClothSimulatable {
    private Item prevHeldItem;
    private Item prevHeldItemOffHand;
    protected EpicSkins epicSkinsInformation;

    public AbstractClientPlayerPatch(T entity) {
        super(entity);
    }

    @Override
    public void onJoinWorld(T entity, Level level, boolean worldgenSpawn) {
        super.onJoinWorld(entity, level, worldgenSpawn);

        this.prevHeldItem = Items.AIR;
        this.prevHeldItemOffHand = Items.AIR;

        EpicSkins.initEpicSkins(this);
    }

    @Override
    public void updateMotion(boolean considerInaction) {
        if (this.original.getHealth() <= 0.0F) {
            currentLivingMotion = LivingMotions.DEATH;
        } else if (!this.state.updateLivingMotion() && considerInaction) {
            currentLivingMotion = LivingMotions.INACTION;
        } else {
            if (original.isFallFlying() || original.isAutoSpinAttack()) {
                currentLivingMotion = LivingMotions.FLY;
            } else if (original.getVehicle() != null) {
                if (original.getVehicle() instanceof PlayerRideableJumping)
                    currentLivingMotion = LivingMotions.MOUNT;
                else
                    currentLivingMotion = LivingMotions.SIT;
            } else if (original.isVisuallySwimming()) {
                currentLivingMotion = LivingMotions.SWIM;
            } else if (original.isSleeping()) {
                currentLivingMotion = LivingMotions.SLEEP;
            } else if (!original.onGround() && original.onClimbable()) {
                currentLivingMotion = LivingMotions.CLIMB;
            } else if (!original.getAbilities().flying) {
                ClientAnimator animator = this.getClientAnimator();

                if (original.isUnderWater() && (original.getY() - this.yo) < -0.005)
                    currentLivingMotion = LivingMotions.FLOAT;
                else if (original.getY() - this.yo < -0.4F || this.isAirborneState())
                    currentLivingMotion = LivingMotions.FALL;
                else if (this.isMoving()) {
                    if (original.isCrouching())
                        currentLivingMotion = LivingMotions.SNEAK;
                    else if (original.isSprinting())
                        currentLivingMotion = LivingMotions.RUN;
                    else
                        currentLivingMotion = LivingMotions.WALK;

                    animator.baseLayer.animationPlayer.setReversed(this.dz < 0);

                } else {
                    animator.baseLayer.animationPlayer.setReversed(false);

                    if (original.isCrouching())
                        currentLivingMotion = LivingMotions.KNEEL;
                    else
                        currentLivingMotion = LivingMotions.IDLE;
                }
            } else {
                if (this.isMoving())
                    currentLivingMotion = LivingMotions.CREATIVE_FLY;
                else
                    currentLivingMotion = LivingMotions.CREATIVE_IDLE;
            }
        }

        ModifyPlayerLivingMotionEvent.BaseLayer baseLayerEvent = new ModifyPlayerLivingMotionEvent.BaseLayer(this, this.currentLivingMotion, !this.state.updateLivingMotion() && considerInaction);
        EpicFightClientEventHooks.Entity.MODIFY_PLAYER_LIVING_MOTION_BASE.postWithListener(baseLayerEvent, this.getEventListener());

        this.currentLivingMotion = baseLayerEvent.getMotion();

        if (!this.state.updateLivingMotion() && considerInaction) {
            this.currentCompositeMotion = LivingMotions.NONE;
        } else {
            CapabilityItem mainhandItemCap = this.getHoldingItemCapability(InteractionHand.MAIN_HAND);
            CapabilityItem offhandItemCap = this.getHoldingItemCapability(InteractionHand.OFF_HAND);
            LivingMotion customLivingMotion = mainhandItemCap.getLivingMotion(this, InteractionHand.MAIN_HAND);

            if (customLivingMotion == null) customLivingMotion = offhandItemCap.getLivingMotion(this, InteractionHand.OFF_HAND);

            // When item capabilities has custom living motion
            if (customLivingMotion != null)
                currentCompositeMotion = customLivingMotion;
            else if (this.original.isUsingItem()) {
                    UseAnim useAnim = this.original.getUseItem().getUseAnimation();
                if (useAnim == UseAnim.BLOCK)
                    currentCompositeMotion = LivingMotions.BLOCK_SHIELD;
                else if (useAnim == UseAnim.CROSSBOW)
                    currentCompositeMotion = LivingMotions.RELOAD;
                else if (useAnim == UseAnim.DRINK)
                    currentCompositeMotion = LivingMotions.DRINK;
                else if (useAnim == UseAnim.EAT)
                    currentCompositeMotion = LivingMotions.EAT;
                else if (useAnim == UseAnim.SPYGLASS)
                    currentCompositeMotion = LivingMotions.SPECTATE;
                else
                    currentCompositeMotion = currentLivingMotion;
            } else {
                if (this.getClientAnimator().getCompositeLayer(Layer.Priority.MIDDLE).animationPlayer.getRealAnimation().get().isReboundAnimation())
                    currentCompositeMotion = LivingMotions.SHOT;
                else if (this.original.swinging && this.original.getSleepingPos().isEmpty())
                    currentCompositeMotion = LivingMotions.DIGGING;
                else
                    currentCompositeMotion = currentLivingMotion;
            }

            ModifyPlayerLivingMotionEvent.CompositeLayer compositeLayerEvent = new ModifyPlayerLivingMotionEvent.CompositeLayer(this, this.currentCompositeMotion);
            EpicFightClientEventHooks.Entity.MODIFY_PLAYER_LIVING_MOTION_COMPOSITE.postWithListener(compositeLayerEvent, this.getEventListener());

            this.currentCompositeMotion = compositeLayerEvent.getMotion();
        }
    }

    @Override
    public void preTickClient() {
        this.xCloakO2 = this.original.xCloakO;
        this.yCloakO2 = this.original.yCloakO;
        this.zCloakO2 = this.original.zCloakO;

        super.preTickClient();

        if (!this.getEntityState().updateLivingMotion()) {
            this.original.yBodyRot = this.original.yHeadRot;
        }

        Item currentMain = this.original.getInventory().getSelected().getItem();
        Item currentOff = this.original.getInventory().offhand.getFirst().getItem();
        boolean isMainHandChanged = this.prevHeldItem != currentMain;
        boolean isOffHandChanged = this.prevHeldItemOffHand != currentOff;

        // F-swap during use-item (pressing F while scoped through a spyglass, eating, drinking,
        // blocking, ...). Both slots flip simultaneously and the items trade places, but the
        // vanilla {@code usedItemHand} stays on the original hand, so {@code MirrorAnimation}'s
        // begin-time routing keeps the animation glued to the now-empty side. Skip the
        // {@code updateHeldItem}/equip-flourish path (which would also call cancelItemUse) and
        // re-trigger whatever LivingMotion is currently driving the layer the meta animation
        // re-runs {@link MirrorAnimation#begin} and routes to the new hand via
        // {@code resolveActiveHand}.
        // Items literally trade slots the previous-frame mainhand item is now in the offhand
        // and vice versa. This catches the asymmetric case (using item in mainhand, empty offhand)
        // too: spyglass moves to offhand, AIR moves to mainhand, both prev/current still match
        // by reference. We deliberately don't require both hands to be non-empty.
        boolean isFSwapDuringUse = isMainHandChanged && isOffHandChanged
            && this.prevHeldItem == currentOff
            && this.prevHeldItemOffHand == currentMain
            && this.original.isUsingItem();

        if (isFSwapDuringUse) {
            ClientAnimator animator = this.getClientAnimator();
            yesman.epicfight.api.animation.LivingMotion compositeMotion = animator.currentCompositeMotion();
            AssetAccessor<? extends StaticAnimation> compositeAnim = animator.getCompositeLivingMotion(compositeMotion);
            if (compositeAnim != null && compositeAnim.isPresent()
                && compositeAnim.get() instanceof yesman.epicfight.api.animation.types.MirrorAnimation) {
                this.playAnimationInClientSide(compositeAnim, 0.0F);
            }
            this.prevHeldItem = currentMain;
            this.prevHeldItemOffHand = currentOff;
        } else if (isMainHandChanged || isOffHandChanged) {
            this.updateHeldItem(this.getHoldingItemCapability(InteractionHand.MAIN_HAND), this.getHoldingItemCapability(InteractionHand.OFF_HAND));

            if (isMainHandChanged) {
                AssetAccessor<? extends StaticAnimation> mainAnim =
                    currentMain == Items.AIR ? Animations.BIPED_UNEQUIP_ITEM : Animations.BIPED_EQUIP_ITEM;
                this.playAnimationInClientSide(mainAnim, 0.0F);
                this.prevHeldItem = currentMain;
            }

            if (isOffHandChanged) {
                AssetAccessor<? extends StaticAnimation> offAnim =
                    currentOff == Items.AIR ? Animations.BIPED_UNEQUIP_ITEM_OFFHAND : Animations.BIPED_EQUIP_ITEM_OFFHAND;
                this.playAnimationInClientSide(offAnim, 0.0F);
                this.prevHeldItemOffHand = currentOff;
            }
        }

        // Since LivingDeathEvent is never fired for client players, so we manually called death animation playing here
        if (this.original.deathTime == 1) {
            this.getClientAnimator().playDeathAnimation();
        }

        this.clothSimulator.tick(this);
    }

    @Override
    public void postTickClient() {
        this.modelYRotO2 = this.modelYRotO;
        this.xPosO2 = (float)this.original.xOld;
        this.yPosO2 = (float)this.original.yOld;
        this.zPosO2 = (float)this.original.zOld;
    }

    protected boolean isMoving() {
        return Math.abs(this.dx) > 0.01F || Math.abs(this.dz) > 0.01F;
    }

    public void updateHeldItem(CapabilityItem mainHandCap, CapabilityItem offHandCap) {
        this.cancelItemUse();

        this.getClientAnimator().iterAllLayers((layer) -> {
            if (layer.isOff()) {
                return;
            }

            layer.animationPlayer.getRealAnimation().get().getProperty(StaticAnimationProperty.ON_ITEM_CHANGE_EVENT).ifPresent((event) -> {
                event.params(mainHandCap, offHandCap);
                event.execute(this, layer.animationPlayer.getRealAnimation(), layer.animationPlayer.getPrevElapsedTime(), layer.animationPlayer.getElapsedTime());
            });
        });
    }

    @Override @ClientOnly
    public void entityPairing(SPEntityPairingPacket packet) {
        super.entityPairing(packet);

        if (packet.pairingPacketType().is(EntityPairingPacketTypes.class)) {
            switch (packet.pairingPacketType().toEnum(EntityPairingPacketTypes.class)) {
            case TECHNICIAN_ACTIVATED -> {
                this.original.level().addParticle(EpicFightParticles.WHITE_AFTERIMAGE.get(), this.original.getX(), this.original.getY(), this.original.getZ(), Double.longBitsToDouble(this.original.getId()), 0, 0);
            }
            case ADRENALINE_ACTIVATED -> {
                if (this.original.isLocalPlayer()) {
                    Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(EpicFightSounds.ADRENALINE.get(), 1.0F, 1.0F));
                } else {
                    this.original.playSound(EpicFightSounds.ADRENALINE.get());
                }

                this.original.level().addParticle(EpicFightParticles.ADRENALINE_PLAYER_BEATING.get(), this.original.getX(), this.original.getY(), this.original.getZ(), Double.longBitsToDouble(this.original.getId()), 0, 0);
            }
            case EMERGENCY_ESCAPE_ACTIVATED -> {
                float yRot = packet.buffer().readFloat();
                this.original.level().addParticle(EpicFightParticles.AIR_BURST.get(), this.original.getX(), this.original.getY() + this.original.getBbHeight() * 0.5F, this.original.getZ(), 90.0F, yRot, 0);

                this.entityDecorations.addColorModifier(EpicFightSkills.EMERGENCY_ESCAPE.get(), new RenderAttributeModifier<> () {
                    private int tickCount;

                    @Override
                    public void modifyValue(Vector4f val, float partialTick) {
                        val.w = (float)Math.pow((this.tickCount + partialTick) / 6.0D, 2.0D) - 0.4F;
                    }

                    @Override
                    public boolean tick() {
                        return ++this.tickCount > 6;
                    }
                });
            }
            default -> {}
            }
        }
    }

    @Override
    public boolean overrideRender() {
        ValidatePlayerModelEvent validatePlayerModelEvent = new ValidatePlayerModelEvent(this, !ClientConfig.enableOriginalModel || this.isEpicFightMode());
        EpicFightClientEventHooks.Render.VALIDATE_PLAYER_MODEL_TO_RENDER.post(validatePlayerModelEvent);
        return validatePlayerModelEvent.getShouldRender();
    }

    @Override
    public boolean shouldMoveOnCurrentSide(ActionAnimation actionAnimation) {
        return false;
    }

    @Override
    public void poseTick(DynamicAnimation animation, Pose pose, float elapsedTime, float partialTick) {
        if (pose.hasTransform("Head") && this.armature.hasJoint("Head")) {
            if (animation.doesHeadRotFollowEntityHead()) {
                float headRelativeRot = Mth.rotLerp(partialTick, Mth.wrapDegrees(this.modelYRotO - this.original.yHeadRotO), Mth.wrapDegrees(this.modelYRot - this.original.yHeadRot));

                OpenMatrix4f headTransform = this.armature.getBoundTransformFor(pose, this.armature.searchJointByName("Head"));
                OpenMatrix4f toOriginalRotation = headTransform.removeScale().removeTranslation().invert();
                Vec3f xAxis = OpenMatrix4f.transform3v(toOriginalRotation, Vec3f.X_AXIS, null);
                Vec3f yAxis = OpenMatrix4f.transform3v(toOriginalRotation, Vec3f.Y_AXIS, null);
                OpenMatrix4f headRotation = OpenMatrix4f.createRotatorDeg(headRelativeRot, yAxis).rotateDeg(-Mth.rotLerp(partialTick, this.original.xRotO, this.original.getXRot()), xAxis);
                pose.orElseEmpty("Head").frontResult(JointTransform.fromMatrix(headRotation), OpenMatrix4f::mul);
            }
        }
    }

    @Override
    public OpenMatrix4f getModelMatrix(float partialTick) {
        if (this.original.isAutoSpinAttack()) {
            OpenMatrix4f mat = MathUtils.getModelMatrixIntegral(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0, 0, 0, 0, partialTick, PLAYER_SCALE, PLAYER_SCALE, PLAYER_SCALE);
            float yRot = MathUtils.lerpBetween(this.original.yRotO, this.original.getYRot(), partialTick);
            float xRot = MathUtils.lerpBetween(this.original.xRotO, this.original.getXRot(), partialTick);

            mat.rotateDeg(-yRot, Vec3f.Y_AXIS)
               .rotateDeg(-xRot, Vec3f.X_AXIS)
               .rotateDeg((this.original.tickCount + partialTick) * -55.0F, Vec3f.Z_AXIS)
               .translate(0F, -0.39F, 0F);

            return mat;
        } else if (this.original.isFallFlying()) {
            OpenMatrix4f mat = MathUtils.getModelMatrixIntegral(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0, 0, 0, 0, partialTick, PLAYER_SCALE, PLAYER_SCALE, PLAYER_SCALE);
            float f1 = (float)this.original.getFallFlyingTicks() + partialTick;
            float f2 = Mth.clamp(f1 * f1 / 100.0F, 0.0F, 1.0F);

            mat.rotateDeg(-Mth.rotLerp(partialTick, this.original.yBodyRotO, this.original.yBodyRot), Vec3f.Y_AXIS).rotateDeg(f2 * (-this.original.getXRot()), Vec3f.X_AXIS);

            Vec3 vec3d = this.original.getViewVector(partialTick);
            Vec3 vec3d1 = this.original.getDeltaMovementLerped(partialTick);
            double d0 = vec3d1.horizontalDistanceSqr();
            double d1 = vec3d.horizontalDistanceSqr();

            if (d0 > 0.0D && d1 > 0.0D) {
                double d2 = (vec3d1.x * vec3d.x + vec3d1.z * vec3d.z) / (Math.sqrt(d0) * Math.sqrt(d1));
                double d3 = vec3d1.x * vec3d.z - vec3d1.z * vec3d.x;
                mat.rotate((float)-((Math.signum(d3) * Math.acos(d2))), Vec3f.Z_AXIS);
            }

            return mat;

        } else if (this.original.isSleeping()) {
            BlockState blockstate = this.original.getInBlockState();
            float yRot = 0.0F;

            if (blockstate.getBlock() instanceof net.minecraft.world.level.block.BedBlock) {
                if (blockstate.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
                    switch(blockstate.getValue(BlockStateProperties.HORIZONTAL_FACING)) {
                    case EAST:
                        yRot = 90.0F;
                        break;
                    case WEST:
                        yRot = -90.0F;
                        break;
                    case SOUTH:
                        yRot = 180.0F;
                        break;
                    default:
                        break;
                    }
                }
            }

            return MathUtils.getModelMatrixIntegral(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, yRot, yRot, 0, PLAYER_SCALE, PLAYER_SCALE, PLAYER_SCALE);
        } else {
            float yRotO;
            float yRot;
            float xRotO = 0;
            float xRot = 0;

            if (this.original.getVehicle() instanceof LivingEntity ridingEntity) {
                yRotO = ridingEntity.yBodyRotO;
                yRot = ridingEntity.yBodyRot;
            } else {
                yRotO = this.modelYRotO;
                yRot = this.modelYRot;
            }

            if (!this.getEntityState().inaction() && this.original.getPose() == net.minecraft.world.entity.Pose.SWIMMING) {
                float f = this.original.getSwimAmount(partialTick);
                float f3 = this.original.isInWater() ? this.original.getXRot() : 0;
                float f4 = Mth.lerp(f, 0.0F, f3);
                xRotO = f4;
                xRot = f4;
            }

            return MathUtils.getModelMatrixIntegral(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, xRotO, xRot, yRotO, yRot, partialTick, PLAYER_SCALE, PLAYER_SCALE, PLAYER_SCALE);
        }
    }

    public void setEpicSkinsInformation(EpicSkins epicSkinsInformation) {
        this.epicSkinsInformation = epicSkinsInformation;
    }

    public EpicSkins getEpicSkinsInformation() {
        return this.epicSkinsInformation;
    }

    public boolean isEpicSkinsLoaded() {
        return this.epicSkinsInformation != null;
    }

    @Override
    public EntitySnapshot<?> captureEntitySnapshot() {
        return EntitySnapshot.capturePlayer(this);
    }

    private final ClothSimulator clothSimulator = new ClothSimulator();
    public float modelYRotO2;
    public double xPosO2;
    public double yPosO2;
    public double zPosO2;
    public double xCloakO2;
    public double yCloakO2;
    public double zCloakO2;

    @SuppressWarnings("unchecked")
    @Override
    public <SIM extends PhysicsSimulator<?, ?, ?, ?, ?>> Optional<SIM> getSimulator(SimulationTypes<?, ?, ?, ?, ?, SIM> simulationType) {
        if (simulationType == SimulationTypes.CLOTH) {
            return Optional.of((SIM)this.clothSimulator);
        }

        return Optional.empty();
    }

    @Override
    public ClothSimulator getClothSimulator() {
        return this.clothSimulator;
    }

    @Override
    public Vec3 getAccurateCloakLocation(float partialFrame) {
        if (partialFrame < 0.0F) {
            partialFrame = 1.0F - partialFrame;

            double x = Mth.lerp((double)partialFrame, this.xCloakO2, this.original.xCloakO) - Mth.lerp((double)partialFrame, this.xPosO2, this.original.xo);
            double y = Mth.lerp((double)partialFrame, this.yCloakO2, this.original.yCloakO) - Mth.lerp((double)partialFrame, this.yPosO2, this.original.yo);
            double z = Mth.lerp((double)partialFrame, this.zCloakO2, this.original.zCloakO) - Mth.lerp((double)partialFrame, this.zPosO2, this.original.zo);

            return new Vec3(x, y, z);
        } else {
            double x = Mth.lerp((double)partialFrame, this.original.xCloakO, this.original.xCloak) - Mth.lerp((double)partialFrame, this.original.xo, this.original.getX());
            double y = Mth.lerp((double)partialFrame, this.original.yCloakO, this.original.yCloak) - Mth.lerp((double)partialFrame, this.original.yo, this.original.getY());
            double z = Mth.lerp((double)partialFrame, this.original.zCloakO, this.original.zCloak) - Mth.lerp((double)partialFrame, this.original.zo, this.original.getZ());

            return new Vec3(x, y, z);
        }
    }

    @Override
    public Vec3 getAccuratePartialLocation(float partialFrame) {
        if (partialFrame < 0.0F) {
            partialFrame = 1.0F + partialFrame;

            double x = Mth.lerp((double)partialFrame, this.xPosO2, this.original.xOld);
            double y = Mth.lerp((double)partialFrame, this.yPosO2, this.original.yOld);
            double z = Mth.lerp((double)partialFrame, this.zPosO2, this.original.zOld);

            return new Vec3(x, y, z);
        } else {
            double x = Mth.lerp((double)partialFrame, this.original.xOld, this.original.getX());
            double y = Mth.lerp((double)partialFrame, this.original.yOld, this.original.getY());
            double z = Mth.lerp((double)partialFrame, this.original.zOld, this.original.getZ());

            return new Vec3(x, y, z);
        }
    }

    @Override
    public Vec3 getObjectVelocity() {
        return new Vec3(this.original.getX() - this.original.xOld, this.original.getY() - this.original.yOld, this.original.getZ() - this.original.zOld);
    }

    @Override
    public float getAccurateYRot(float partialFrame) {
        if (partialFrame < 0.0F) {
            partialFrame = 1.0F + partialFrame;

            return Mth.rotLerp(partialFrame, this.modelYRotO2, this.getYRotO());
        } else {
            return Mth.rotLerp(partialFrame, this.getYRotO(), this.getYRot());
        }
    }

    @Override
    public float getYRotDelta(float partialFrame) {
        if (partialFrame < 0.0F) {
            partialFrame = 1.0F + partialFrame;

            return Mth.rotLerp(partialFrame, this.modelYRotO2, this.getYRotO()) - this.modelYRotO2;
        } else {
            return Mth.rotLerp(partialFrame, this.getYRotO(), this.getYRot()) - this.getYRotO();
        }
    }

    @Override
    public boolean invalid() {
        return this.original.isRemoved();
    }

    @Override
    public float getScale() {
        return PLAYER_SCALE;
    }

    @Override
    public Animator getSimulatableAnimator() {
        return this.animator;
    }

    @Override
    public float getGravity() {
        return this.getOriginal().isUnderWater() ? 0.98F : 9.8F;
    }
}
