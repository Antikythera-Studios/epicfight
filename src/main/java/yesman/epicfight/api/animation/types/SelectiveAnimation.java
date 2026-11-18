package yesman.epicfight.api.animation.types;

import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.AnimationVariables;
import yesman.epicfight.api.animation.AnimationVariables.IndependentVariableKey;
import yesman.epicfight.api.animation.property.AnimationEvent;
import yesman.epicfight.api.animation.property.AnimationEvent.SimpleEvent;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.client.animation.Layer;
import yesman.epicfight.api.utils.side.ClientOnly;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class SelectiveAnimation extends StaticAnimation {
	public static final IndependentVariableKey<Integer> PREVIOUS_STATE = AnimationVariables.unsyncIndependent((animator) -> 0, true);

	private static final java.util.Map<net.minecraft.resources.ResourceLocation, Function<LivingEntityPatch<?>, Integer>> SELECTOR_OVERRIDES = new java.util.concurrent.ConcurrentHashMap<>();

	public static void registerSelectorOverride(net.minecraft.resources.ResourceLocation animationId, Function<LivingEntityPatch<?>, Integer> selector) {
		SELECTOR_OVERRIDES.put(animationId, selector);
	}

	private final Function<LivingEntityPatch<?>, Integer> selector;
	private final List<AssetAccessor<? extends StaticAnimation>> animationsInEachState;

	/**
	 * All animations should have same priority and layer type
	 */
	@SafeVarargs
	public SelectiveAnimation(Function<LivingEntityPatch<?>, Integer> selector, AnimationAccessor<? extends SelectiveAnimation> accessor, AssetAccessor<? extends StaticAnimation>... selectOptions) {
		super(0.15F, false, accessor, null);

		this.selector = selector;
		this.animationsInEachState = List.of(selectOptions);

		for (AssetAccessor<? extends StaticAnimation> subAnimations : this.animationsInEachState) {
			subAnimations.get().addEvents(SimpleEvent.create((entitypatch, animation, params) -> {
				int currentStateId = SELECTOR_OVERRIDES.getOrDefault(this.getAccessor().registryName(), this.selector).apply(entitypatch);
				Optional<Integer> prevState = entitypatch.getAnimator().getVariables().get(PREVIOUS_STATE, this.getAccessor());

				prevState.ifPresentOrElse(prevStateId -> {
					if (prevStateId != currentStateId) {
						entitypatch.getAnimator().playAnimation(this.animationsInEachState.get(currentStateId), 0.0F);
						entitypatch.getAnimator().getVariables().put(PREVIOUS_STATE, this.getAccessor(), currentStateId);
					}
				}, () -> {
					entitypatch.getAnimator().playAnimation(this.animationsInEachState.get(0), 0.0F);
					entitypatch.getAnimator().getVariables().put(PREVIOUS_STATE, this.getAccessor(), 0);
				});
			}, AnimationEvent.Side.BOTH));
		}
	}

	@Override
	public void begin(LivingEntityPatch<?> entitypatch) {
		super.begin(entitypatch);

		int result = SELECTOR_OVERRIDES.getOrDefault(this.getAccessor().registryName(), this.selector).apply(entitypatch);
		entitypatch.getAnimator().getVariables().put(PREVIOUS_STATE, this.getAccessor(), result);
		entitypatch.getAnimator().playAnimation(this.animationsInEachState.get(result), 0.0F);
	}

	@Override
	public void tick(LivingEntityPatch<?> entitypatch) {
		super.tick(entitypatch);
	}

	@Override
	public boolean isMetaAnimation() {
		return true;
	}

	@Override
	public List<AssetAccessor<? extends StaticAnimation>> getSubAnimations() {
		return this.animationsInEachState;
	}

	@Override @ClientOnly
	public Layer.Priority getPriority() {
		return this.animationsInEachState.get(0).get().getPriority();
	}

	@Override @ClientOnly
	public Layer.LayerType getLayerType() {
		return this.animationsInEachState.get(0).get().getLayerType();
	}
}