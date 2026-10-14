package yesman.epicfight.api.animation.types;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.netty.util.internal.StringUtil;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import yesman.epicfight.api.animation.*;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.AnimationVariables.IndependentVariableKey;
import yesman.epicfight.api.animation.property.AnimationEvent;
import yesman.epicfight.api.animation.property.AnimationEvent.SimpleEvent;
import yesman.epicfight.api.animation.property.AnimationParameters;
import yesman.epicfight.api.animation.property.AnimationProperty;
import yesman.epicfight.api.animation.property.AnimationProperty.ActionAnimationProperty;
import yesman.epicfight.api.animation.property.AnimationProperty.PlaybackSpeedModifier;
import yesman.epicfight.api.animation.property.AnimationProperty.StaticAnimationProperty;
import yesman.epicfight.api.animation.types.EntityState.StateFactor;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.asset.JsonAssetLoader;
import yesman.epicfight.api.client.animation.Layer;
import yesman.epicfight.api.client.animation.Layer.LayerType;
import yesman.epicfight.api.client.animation.property.ClientAnimationProperties;
import yesman.epicfight.api.client.animation.property.JointMaskEntry;
import yesman.epicfight.api.client.animation.property.TrailInfo;
import yesman.epicfight.api.event.EpicFightEventHooks;
import yesman.epicfight.api.event.types.animation.AnimationBeginEvent;
import yesman.epicfight.api.event.types.animation.AnimationEndEvent;
import yesman.epicfight.api.exception.AssetLoadingException;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.api.physics.ik.InverseKinematicsProvider;
import yesman.epicfight.api.physics.ik.InverseKinematicsSimulatable;
import yesman.epicfight.api.physics.ik.InverseKinematicsSimulator;
import yesman.epicfight.api.physics.ik.InverseKinematicsSimulator.BakedInverseKinematicsDefinition;
import yesman.epicfight.api.physics.ik.InverseKinematicsSimulator.InverseKinematicsObject;
import yesman.epicfight.api.utils.datastructure.ParameterizedHashMap;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.api.utils.math.Vec3f;
import yesman.epicfight.api.utils.side.ClientOnly;
import yesman.epicfight.client.events.engine.RenderEngine;
import yesman.epicfight.client.renderer.EpicFightRenderTypes;
import yesman.epicfight.client.renderer.RenderingTool;
import yesman.epicfight.client.renderer.patched.item.RenderItemBase;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.EpicFight;
import yesman.epicfight.main.EpicFightSharedConstants;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

public class StaticAnimation extends DynamicAnimation implements InverseKinematicsProvider {
    public static final IndependentVariableKey<Boolean> NO_PHYSICS = AnimationVariables.unsyncIndependent(animator -> false, true);

    public static String getFileHash(ResourceLocation rl) {
        String fileHash;

        try {
            JsonAssetLoader jsonfile = new JsonAssetLoader(AnimationManager.getAnimationResourceManager(), rl);
            fileHash = jsonfile.getFileHash();
        } catch (AssetLoadingException e) {
            fileHash = StringUtil.EMPTY_STRING;
        }

        return fileHash;
    }

    protected final Map<AnimationProperty<?>, Object> properties = Maps.newHashMap();

    /**
     * States will bind into animation on {@link AnimationManager#apply}
     */
    protected final StateSpectrum.Blueprint stateSpectrumBlueprint = new StateSpectrum.Blueprint();
    protected final StateSpectrum stateSpectrum = new StateSpectrum();
    protected final AssetAccessor<? extends Armature> armature;
    protected ResourceLocation resourceLocation;
    protected AnimationAccessor<? extends StaticAnimation> accessor;
    private final String filehash;

    public StaticAnimation() {
        super(0.0F, true);

        this.resourceLocation = EpicFight.identifier("emtpy");
        this.armature = null;
        this.filehash = StringUtil.EMPTY_STRING;
    }

    public StaticAnimation(boolean isRepeat, AnimationAccessor<? extends StaticAnimation> accessor, AssetAccessor<? extends Armature> armature) {
        this(EpicFightSharedConstants.GENERAL_ANIMATION_TRANSITION_TIME, isRepeat, accessor, armature);
    }

    public StaticAnimation(float transitionTime, boolean isRepeat, AnimationAccessor<? extends StaticAnimation> accessor, AssetAccessor<? extends Armature> armature) {
        super(transitionTime, isRepeat);

        this.resourceLocation = ResourceLocation.fromNamespaceAndPath(accessor.registryName().getNamespace(), "animmodels/animations/" + accessor.registryName().getPath() + ".json");

        this.armature = armature;
        this.accessor = accessor;
        this.filehash = getFileHash(this.resourceLocation);
    }

    /* Resourcepack animations */
    public StaticAnimation(float transitionTime, boolean isRepeat, String path, AssetAccessor<? extends Armature> armature) {
        super(transitionTime, isRepeat);

        ResourceLocation registryName = ResourceLocation.parse(path);
        this.resourceLocation = ResourceLocation.fromNamespaceAndPath(registryName.getNamespace(), "animmodels/animations/" + registryName.getPath() + ".json");
        this.armature = armature;
        this.filehash = StringUtil.EMPTY_STRING;
    }

    /* Multilayer Constructor */
    public StaticAnimation(ResourceLocation fileLocation, float transitionTime, boolean isRepeat, String registryName, AssetAccessor<? extends Armature> armature) {
        super(transitionTime, isRepeat);

        this.resourceLocation = fileLocation;
        this.armature = armature;
        this.filehash = StringUtil.EMPTY_STRING;
    }

    public void loadAnimation() {
        if (!this.isMetaAnimation()) {
            if (this.properties.containsKey(StaticAnimationProperty.IK_DEFINITION)) {
                this.animationClip = AnimationManager.getInstance().loadAnimationClip(this, JsonAssetLoader::loadAllJointsClipForAnimation);

                this.getProperty(StaticAnimationProperty.IK_DEFINITION).ifPresent(ikDefinitions -> {
                    boolean correctY = this.getProperty(ActionAnimationProperty.MOVE_VERTICAL).orElse(false);
                    boolean correctZ = this.isMainFrameAnimation();

                    List<BakedInverseKinematicsDefinition> bakedIKDefinitionList = ikDefinitions.stream().map(ikDefinition -> ikDefinition.bake(this.armature, this.animationClip.getJointTransforms(), correctY, correctZ)).toList();
                    this.addProperty(StaticAnimationProperty.BAKED_IK_DEFINITION, bakedIKDefinitionList);

                    // Remove the unbaked data
                    this.properties.remove(StaticAnimationProperty.IK_DEFINITION);
                });
            } else {
                this.animationClip = AnimationManager.getInstance().loadAnimationClip(this, JsonAssetLoader::loadClipForAnimation);
            }

            this.animationClip.bakeKeyframes();
        }
    }

    public void postInit() {
        this.stateSpectrum.readFrom(this.stateSpectrumBlueprint);
    }

    @Override
    public AnimationClip getAnimationClip() {
        if (this.animationClip == null) {
            this.loadAnimation();
        }

        return this.animationClip;
    }

    public void setLinkAnimation(final AssetAccessor<? extends DynamicAnimation> fromAnimation, Pose startPose, boolean isOnSameLayer, float transitionTimeModifier, LivingEntityPatch<?> entitypatch, LinkAnimation dest) {
        if (!entitypatch.isLogicalClient()) {
            startPose = Animations.EMPTY_ANIMATION.getPoseByTime(entitypatch, 0.0F, 1.0F);
        }

        dest.resetNextStartTime();

        float playTime = this.getPlaySpeed(entitypatch, dest);
        PlaybackSpeedModifier playSpeedModifier = this.getRealAnimation().get().getProperty(StaticAnimationProperty.PLAY_SPEED_MODIFIER).orElse(null);

        if (playSpeedModifier != null) {
            playTime = playSpeedModifier.modify(dest, entitypatch, playTime, 0.0F, playTime);
        }

        playTime = Math.abs(playTime);
        playTime *= EpicFightSharedConstants.A_TICK;

        float linkTime = transitionTimeModifier > 0.0F ? transitionTimeModifier + this.transitionTime : this.transitionTime;
        float totalTime = playTime * (int)Math.ceil(linkTime / playTime);
        float nextStartTime = Math.max(0.0F, -transitionTimeModifier);
        nextStartTime += totalTime - linkTime;

        dest.setNextStartTime(nextStartTime);
        dest.getAnimationClip().reset();
        dest.setTotalTime(totalTime);
        dest.setConnectedAnimations(fromAnimation, this.getAccessor());

        Map<String, JointTransform> data1 = startPose.getJointTransformData();
        Map<String, JointTransform> data2 = this.getPoseByTime(entitypatch, nextStartTime, 0.0F).getJointTransformData();
        Set<String> joint1 = new HashSet<> (isOnSameLayer ? data1.keySet() : Set.of());
        Set<String> joint2 = new HashSet<> (data2.keySet());

        if (entitypatch.isLogicalClient()) {
            JointMaskEntry entry = fromAnimation.get().getJointMaskEntry(entitypatch, false).orElse(null);
            JointMaskEntry entry2 = this.getJointMaskEntry(entitypatch, true).orElse(null);

            if (entry != null) {
                joint1.removeIf((jointName) -> entry.isMasked(fromAnimation.get().getProperty(ClientAnimationProperties.LAYER_TYPE).orElse(Layer.LayerType.BASE_LAYER) == Layer.LayerType.BASE_LAYER ?
                        entitypatch.getClientAnimator().currentMotion() : entitypatch.getClientAnimator().currentCompositeMotion(), jointName));
            }

            if (entry2 != null) {
                joint2.removeIf((jointName) -> entry2.isMasked(this.getProperty(ClientAnimationProperties.LAYER_TYPE).orElse(Layer.LayerType.BASE_LAYER) == Layer.LayerType.BASE_LAYER ?
                        entitypatch.getCurrentLivingMotion() : entitypatch.currentCompositeMotion, jointName));
            }
        }

        joint1.addAll(joint2);

        if (linkTime != totalTime) {
            Map<String, JointTransform> firstPose = this.getPoseByTime(entitypatch, 0.0F, 0.0F).getJointTransformData();

            for (String jointName : joint1) {
                Keyframe[] keyframes = new Keyframe[3];
                keyframes[0] = new Keyframe(0.0F, data1.get(jointName));
                keyframes[1] = new Keyframe(linkTime, firstPose.get(jointName));
                keyframes[2] = new Keyframe(totalTime, data2.get(jointName));
                TransformSheet sheet = new TransformSheet(keyframes);
                dest.getAnimationClip().addJointTransform(jointName, sheet);
            }
        } else {
            for (String jointName : joint1) {
                Keyframe[] keyframes = new Keyframe[2];
                keyframes[0] = new Keyframe(0.0F, data1.get(jointName));
                keyframes[1] = new Keyframe(totalTime, data2.get(jointName));
                TransformSheet sheet = new TransformSheet(keyframes);
                dest.getAnimationClip().addJointTransform(jointName, sheet);
            }
        }
    }

    @Override
    public void begin(LivingEntityPatch<?> entitypatch) {
        // Load if null
        this.getAnimationClip();

        // Please fix this implementation when minecraft supports any mixinable method that returns noPhysics variable
        this.getProperty(StaticAnimationProperty.NO_PHYSICS).ifPresent(val -> {
            if (val) {
                entitypatch.getAnimator().getVariables().put(NO_PHYSICS, this.getAccessor(), entitypatch.getOriginal().noPhysics);
                entitypatch.getOriginal().noPhysics = true;
            }
        });

        if (entitypatch.isLogicalClient()) {
            this.getProperty(ClientAnimationProperties.TRAIL_EFFECT).ifPresent(trailInfos -> {
                int idx = 0;

                for (TrailInfo trailInfo : trailInfos) {
                    double eid = Double.longBitsToDouble((long)entitypatch.getOriginal().getId());
                    double animid = Double.longBitsToDouble((long)this.getId());
                    double jointId = Double.longBitsToDouble((long)entitypatch.getArmature().searchJointByName(trailInfo.joint()).getId());
                    double index = Double.longBitsToDouble((long)idx++);

                    if (trailInfo.hand() != null) {
                        // Trails are authored against MAIN_HAND. In offhand-only mirror mode the
                        // weapon lives in OFF_HAND, so resolve the item from the active combat
                        // hand instead -- otherwise the lookup hits the bare mainhand, the
                        // weapon-specific trail config never applies, and trailInfo.playable()
                        // fails so no trail particle is spawned at all.
                        net.minecraft.world.InteractionHand resolvedHand =
                            entitypatch.isMirrorMode() && trailInfo.hand() == net.minecraft.world.InteractionHand.MAIN_HAND
                                ? net.minecraft.world.InteractionHand.OFF_HAND
                                : trailInfo.hand();
                        ItemStack stack = entitypatch.getAdvancedHoldingItemStack(resolvedHand);
                        RenderItemBase renderitembase = RenderEngine.getInstance().getItemRenderer(stack);

                        if (renderitembase != null && renderitembase.trailInfo() != null) {
                            trailInfo = renderitembase.trailInfo().overwrite(trailInfo);
                        } else {
                            EpicFight.LOGGER.info("[trail-debug] {} no item-skin trail (renderer={}, stack={})", this.getRegistryName(), renderitembase, stack.getItem());
                        }
                    }

                    if (!trailInfo.playable()) {
                        EpicFight.LOGGER.info("[trail-debug] {} trail NOT playable: joint={} type={} tex={} lifetime={} interpol={} interval={}", this.getRegistryName(), trailInfo.joint(), trailInfo.particle(), trailInfo.texturePath(), trailInfo.trailLifetime(), trailInfo.interpolateCount(), trailInfo.updateInterval());
                        continue;
                    }
                    EpicFight.LOGGER.info("[trail-debug] {} spawning trail joint={}", this.getRegistryName(), trailInfo.joint());

                    entitypatch.getOriginal().level().addParticle(trailInfo.particle(), eid, 0, animid, jointId, index, 0);
                }
            });
        }

        this.getProperty(StaticAnimationProperty.ON_BEGIN_EVENTS).ifPresent(events -> {
            for (SimpleEvent<?> event : events) {
                event.execute(entitypatch, this.getAccessor(), 0.0F, 0.0F);
            }
        });

        EpicFightEventHooks.Animation.BEGIN.postWithListener(new AnimationBeginEvent(entitypatch, this.getAccessor()), entitypatch.getEventListener());
    }

    @Override
    public void end(LivingEntityPatch<?> entitypatch, AssetAccessor<? extends DynamicAnimation> nextAnimation, boolean isEnd) {
        this.getProperty(StaticAnimationProperty.NO_PHYSICS).ifPresent((val) -> {
            if (val) entitypatch.getOriginal().noPhysics = entitypatch.getAnimator().getVariables().getOrDefault(NO_PHYSICS, this.getAccessor());
        });

        this.getProperty(StaticAnimationProperty.ON_END_EVENTS).ifPresent((events) -> {
            events.forEach(event -> event.executeWithNewParams(entitypatch, this.getAccessor(), this.getTotalTime(), this.getTotalTime(), event.getParameters() == null ? AnimationParameters.of(isEnd) : AnimationParameters.addParameter(event.getParameters(), isEnd)));
        });

        EpicFightEventHooks.Animation.END.postWithListener(new AnimationEndEvent(entitypatch, this.getAccessor(), isEnd), entitypatch.getEventListener());
        entitypatch.getAnimator().getVariables().removeAll(this.getAccessor());
    }

    @Override
    public void tick(LivingEntityPatch<?> entitypatch) {
        this.getProperty(StaticAnimationProperty.NO_PHYSICS).ifPresent((val) -> {
            if (val) {
                entitypatch.getOriginal().noPhysics = true;
            }
        });

        this.getProperty(StaticAnimationProperty.TICK_EVENTS).ifPresent((events) -> {
            entitypatch.getAnimator().getPlayer(this.getAccessor()).ifPresent(player -> {
                for (AnimationEvent<?, ?> event : events) {
                    float prevElapsed = player.getPrevElapsedTime();
                    float elapsed = player.getElapsedTime();

                    event.execute(entitypatch, this.getAccessor(), prevElapsed, elapsed);
                }
            });
        });
    }

    @Override
    public EntityState getState(LivingEntityPatch<?> entitypatch, float time) {
        return new EntityState(this.getStatesMap(entitypatch, time));
    }

    @Override
    public ParameterizedHashMap<StateFactor<?>> getStatesMap(LivingEntityPatch<?> entitypatch, float time) {
        return this.stateSpectrum.getStateMap(entitypatch, time);
    }

    @Override
    public <T> T getState(StateFactor<T> stateFactor, LivingEntityPatch<?> entitypatch, float time) {
        return this.stateSpectrum.getSingleState(stateFactor, entitypatch, time);
    }

    @Override
    public Optional<JointMaskEntry> getJointMaskEntry(LivingEntityPatch<?> entitypatch, boolean useCurrentMotion) {
        return this.getProperty(ClientAnimationProperties.JOINT_MASK);
    }

    @Override
    public void modifyPose(DynamicAnimation animation, Pose pose, LivingEntityPatch<?> entitypatch, float time, float partialTicks) {
        entitypatch.poseTick(animation, pose, time, partialTicks);

        this.getProperty(StaticAnimationProperty.POSE_MODIFIER).ifPresent((poseModifier) -> {
            poseModifier.modify(animation, pose, entitypatch, time, partialTicks);
        });
    }

    @Override
    public boolean isStaticAnimation() {
        return true;
    }

    @Override
    public boolean doesHeadRotFollowEntityHead() {
        return !this.getProperty(StaticAnimationProperty.FIXED_HEAD_ROTATION).orElse(false);
    }

    @Override
    public int getId() {
        return this.accessor.id();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof StaticAnimation staticAnimation) {
            if (this.accessor != null && staticAnimation.accessor != null) {
                return this.getId() == staticAnimation.getId();
            }
        }

        return super.equals(obj);
    }

    public boolean idBetween(StaticAnimation a1, StaticAnimation a2) {
        return a1.getId() <= this.getId() && a2.getId() >= this.getId();
    }

    public boolean in(StaticAnimation[] animations) {
        for (StaticAnimation animation : animations) {
            if (this.equals(animation)) {
                return true;
            }
        }

        return false;
    }

    public boolean in(AnimationAccessor<? extends DynamicAnimation>[] animationProviders) {
        for (AnimationAccessor<? extends DynamicAnimation> animationProvider : animationProviders) {
            if (this.equals(animationProvider.get())) {
                return true;
            }
        }

        return false;
    }

    @SuppressWarnings("unchecked")
    public <A extends StaticAnimation> A setResourceLocation(String namespace, String path) {
        this.resourceLocation = ResourceLocation.fromNamespaceAndPath(namespace, "animmodels/animations/" + path + ".json");
        return (A)this;
    }

    public ResourceLocation getLocation() {
        return this.resourceLocation;
    }

    @Override
    public ResourceLocation getRegistryName() {
        return this.accessor.registryName();
    }

    public AssetAccessor<? extends Armature> getArmature() {
        return this.armature;
    }

    public String getFileHash() {
        return this.filehash;
    }

    @Override
    public float getPlaySpeed(LivingEntityPatch<?> entitypatch, DynamicAnimation animation) {
        return 1.0F;
    }

    @Override
    public TransformSheet getCoord() {
        return this.getProperty(ActionAnimationProperty.COORD).orElse(super.getCoord());
    }

    @Override
    public String toString() {
        String classPath = this.getClass().toString();
        return classPath.substring(classPath.lastIndexOf(".") + 1) + " " + this.getLocation();
    }

    /**
     * Internal use only
     */
    @Deprecated
    public StaticAnimation addPropertyUnsafe(AnimationProperty<?> propertyType, Object value) {
        this.properties.put(propertyType, value);
        this.getSubAnimations().forEach((subAnimation) -> subAnimation.get().addPropertyUnsafe(propertyType, value));
        return this;
    }

    @SuppressWarnings("unchecked")
    public <A extends StaticAnimation, V> A addProperty(StaticAnimationProperty<V> propertyType, V value) {
        this.properties.put(propertyType, value);
        this.getSubAnimations().forEach((subAnimation) -> subAnimation.get().addProperty(propertyType, value));
        return (A)this;
    }

    @SuppressWarnings("unchecked")
    public <A extends StaticAnimation> A removeProperty(StaticAnimationProperty<?> propertyType) {
        this.properties.remove(propertyType);
        this.getSubAnimations().forEach((subAnimation) -> subAnimation.get().removeProperty(propertyType));
        return (A)this;
    }

    @SafeVarargs
    @SuppressWarnings("unchecked")
    public final <A extends StaticAnimation> A addEvents(StaticAnimationProperty<?> key, AnimationEvent<?, ?>... events) {
        this.properties.computeIfPresent(key, (k, v) -> {
            return Stream.concat(((Collection<?>)v).stream(), List.of(events).stream()).toList();
        });

        this.properties.computeIfAbsent(key, (k) -> {
            return List.of(events);
        });

        this.getSubAnimations().forEach((subAnimation) -> subAnimation.get().addEvents(key, events));

        return (A)this;
    }

    @SuppressWarnings("unchecked")
    public <A extends StaticAnimation> A addEvents(AnimationEvent<?, ?>... events) {
        this.properties.computeIfPresent(StaticAnimationProperty.TICK_EVENTS, (k, v) -> {
            return Stream.concat(((Collection<?>)v).stream(), List.of(events).stream()).toList();
        });

        this.properties.computeIfAbsent(StaticAnimationProperty.TICK_EVENTS, (k) -> {
            return List.of(events);
        });

        this.getSubAnimations().forEach((subAnimation) -> subAnimation.get().addEvents(events));

        return (A)this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <V> Optional<V> getProperty(AnimationProperty<V> propertyType) {
        return (Optional<V>) Optional.ofNullable(this.properties.get(propertyType));
    }

    @ClientOnly
    public Layer.Priority getPriority() {
        return this.getProperty(ClientAnimationProperties.PRIORITY).orElse(Layer.Priority.LOWEST);
    }

    @ClientOnly
    public Layer.LayerType getLayerType() {
        return this.getProperty(ClientAnimationProperties.LAYER_TYPE).orElse(LayerType.BASE_LAYER);
    }

    @SuppressWarnings("unchecked")
    public <A extends StaticAnimation> A newTimePair(float start, float end) {
        this.stateSpectrumBlueprint.newTimePair(start, end);
        return (A)this;
    }

    @SuppressWarnings("unchecked")
    public <A extends StaticAnimation> A newConditionalTimePair(Function<LivingEntityPatch<?>, Integer> condition, float start, float end) {
        this.stateSpectrumBlueprint.newConditionalTimePair(condition, start, end);
        return (A)this;
    }

    @SuppressWarnings("unchecked")
    public <T, A extends StaticAnimation> A addState(StateFactor<T> factor, T val) {
        this.stateSpectrumBlueprint.addState(factor, val);
        return (A)this;
    }

    @SuppressWarnings("unchecked")
    public <T, A extends StaticAnimation> A removeState(StateFactor<T> factor) {
        this.stateSpectrumBlueprint.removeState(factor);
        return (A)this;
    }

    @SuppressWarnings("unchecked")
    public <T, A extends StaticAnimation> A addConditionalState(int metadata, StateFactor<T> factor, T val) {
        this.stateSpectrumBlueprint.addConditionalState(metadata, factor, val);
        return (A)this;
    }

    @SuppressWarnings("unchecked")
    public <T, A extends StaticAnimation> A addStateRemoveOld(StateFactor<T> factor, T val) {
        this.stateSpectrumBlueprint.addStateRemoveOld(factor, val);
        return (A)this;
    }

    @SuppressWarnings("unchecked")
    public <T, A extends StaticAnimation> A addStateIfNotExist(StateFactor<T> factor, T val) {
        this.stateSpectrumBlueprint.addStateIfNotExist(factor, val);
        return (A)this;
    }

    public Object getModifiedLinkState(StateFactor<?> factor, Object val, LivingEntityPatch<?> entitypatch, float elapsedTime) {
        return val;
    }

    public List<AssetAccessor<? extends StaticAnimation>> getSubAnimations() {
        return List.of();
    }

    @Override
    public AnimationAccessor<? extends StaticAnimation> getRealAnimation() {
        return this.getAccessor();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <A extends DynamicAnimation> AnimationAccessor<A> getAccessor() {
        return (AnimationAccessor<A>)this.accessor;
    }

    public void setAccessor(AnimationAccessor<? extends StaticAnimation> accessor) {
        this.accessor = accessor;
    }

    public void invalidate() {
        this.accessor = null;
    }

    public boolean isInvalid() {
        return this.accessor == null;
    }

    @ClientOnly
    public void renderDebugging(PoseStack poseStack, MultiBufferSource buffer, LivingEntityPatch<?> entitypatch, float playTime, float partialTicks) {
        if (entitypatch instanceof InverseKinematicsSimulatable ikSimulatable) {
            this.getProperty(StaticAnimationProperty.BAKED_IK_DEFINITION).ifPresent((ikDefinitions) -> {
                OpenMatrix4f modelmat = ikSimulatable.getModelMatrix(partialTicks);
                LivingEntity originalEntity = entitypatch.getOriginal();
                Vec3 entitypos = originalEntity.position();
                float x = (float)entitypos.x;
                float y = (float)entitypos.y;
                float z = (float)entitypos.z;
                float xo = (float)originalEntity.xo;
                float yo = (float)originalEntity.yo;
                float zo = (float)originalEntity.zo;
                OpenMatrix4f toModelPos = OpenMatrix4f.mul(OpenMatrix4f.createTranslation(xo + (x - xo) * partialTicks, yo + (y - yo) * partialTicks, zo + (z - zo) * partialTicks), modelmat, null).invert();

                for (BakedInverseKinematicsDefinition bakedIKInfo : this.getProperty(StaticAnimationProperty.BAKED_IK_DEFINITION).orElse(null)) {
                    ikSimulatable.getIKSimulator().getRunningObject(bakedIKInfo.endJoint()).ifPresent((ikObjet) -> {
                        VertexConsumer vertexBuilder = buffer.getBuffer(EpicFightRenderTypes.debugQuads());
                        Vec3f worldtargetpos = ikObjet.getDestination();
                        Vec3f modeltargetpos = OpenMatrix4f.transform3v(toModelPos, worldtargetpos, null).multiply(-1.0F, 1.0F, -1.0F);
                        RenderingTool.drawQuad(poseStack, vertexBuilder, modeltargetpos, 0.5F, 1.0F, 0.0F, 0.0F);
                        Vec3f jointWorldPos = ikObjet.getTipPosition(partialTicks);
                        Vec3f jointModelpos = OpenMatrix4f.transform3v(toModelPos, jointWorldPos, null);
                        RenderingTool.drawQuad(poseStack, vertexBuilder, jointModelpos.multiply(-1.0F, 1.0F, -1.0F), 0.4F, 0.0F, 0.0F, 1.0F);

                        Pose pose = new Pose();

                        for (String jointName : this.getTransfroms().keySet()) {
                            pose.putJointData(jointName, this.getTransfroms().get(jointName).getInterpolatedTransform(playTime));
                        }
                    });
                }
            });
        }
    }

    @Override
    public InverseKinematicsObject createSimulationData(InverseKinematicsProvider provider, InverseKinematicsSimulatable simOwner, InverseKinematicsSimulator.InverseKinematicsBuilder simBuilder) {
        return new InverseKinematicsObject(simBuilder);
    }
}
