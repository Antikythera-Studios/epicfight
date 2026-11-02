package guivnf.epicfight.akythera.scripts.animator;

import com.yesman.akythera.api.animation.animator.AnimationLayer;
import com.yesman.akythera.core.animation.AnimationLayerSignature;
import com.yesman.akythera.core.animation.animator.BlendingAnimator;
import com.yesman.akythera.core.animation.driver.AnimationDriver;
import com.yesman.akythera.core.animation.keyframe.Pose;
import com.yesman.akythera.core.blueprint.script.AnimatorScript;
import com.yesman.akythera.core.registry.AkytheraRegistries;
import guivnf.epicfight.EpicFight;
import guivnf.epicfight.akythera.providers.EpicfightAnimationSequences;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

import java.util.Map;

/// Loops the humanoid idle on the base layer. Needs nothing of its owner.
public final class IdleAnimatorScript implements AnimatorScript {
    private static final AnimationLayerSignature BASE = AnimationLayerSignature.ENUM_MANAGER.getOrThrow(Identifier.parse("akythera:base"));

    @Override
    public void construct(final Map<AnimationLayerSignature, AnimationLayer> animLayerMap, final Map<String, AnimationDriver> poseCaches, final BlendingAnimator animator) {
        animLayerMap.put(
            BASE,
            AnimationLayer.create(
                AnimatorScript.instantiateAnimSequence(
                    ResourceKey.create(
                        AkytheraRegistries.DataPack.ANIM_SEQUENCE_KEY,
                        Identifier.fromNamespaceAndPath(EpicFight.MODID, "humanoid/locomotion/common/idle")
                    )
                )
            )
        );
    }

    @Override
    public Pose getFinalPose(final Map<AnimationLayerSignature, AnimationLayer> layers) {
        return layers.get(BASE).getPose();
    }

    @Override
    public void update(final float frameTime, final float partialTick) {
    }
}
