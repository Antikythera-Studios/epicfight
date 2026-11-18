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
	public static final IndependentVariableKey<Integer> STATE_STREAK = AnimationVariables.unsyncIndependent((animator) -> 0, true);

	/**
	 * Ticks a differing selection must persist before the sub animation actually
	 * switches. Selectors computed from per-tick values (e.g. the dot product of
	 * view and velocity used by creative flight) can oscillate across their
	 * boundary for several ticks — at high flight speeds a single camera turn
	 * holds a strongly negative dot for ~4-5 ticks while momentum lags the
	 * view. Ten ticks filters those transient crossings while keeping real
	 * direction changes responsive enough (0.5s).
	 */
	private static final int SWITCH_STABILITY_TICKS = 10;
	private static final boolean SAT_DEBUG = Boolean.getBoolean("epicfight.animDebug");

	/**
	 * Selector overrides keyed by the meta animation's registry name. Selective
	 * animations registered by addons (e.g. epicfightx's creative flight) carry
	 * their own selector bytecode that we cannot edit, but the instance still
	 * runs through this class — consulting this map first lets us substitute a
	 * better signal (e.g. player input instead of view-vs-velocity dot product,
	 * which falsely reads "backward" during fast turns as momentum lags view).
	 */
	private static final java.util.Map<net.minecraft.resources.ResourceLocation, Function<LivingEntityPatch<?>, Integer>> SELECTOR_OVERRIDES = new java.util.concurrent.ConcurrentHashMap<>();

	public static void registerSelectorOverride(net.minecraft.resources.ResourceLocation animationId, Function<LivingEntityPatch<?>, Integer> selector) {
		SELECTOR_OVERRIDES.put(animationId, selector);
	}

	private int select(LivingEntityPatch<?> entitypatch) {
		Function<LivingEntityPatch<?>, Integer> override = SELECTOR_OVERRIDES.get(this.getAccessor().registryName());
		return override != null ? override.apply(entitypatch) : this.selector.apply(entitypatch);
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
				int currentStateId = this.select(entitypatch);
				Optional<Integer> prevState = entitypatch.getAnimator().getVariables().get(PREVIOUS_STATE, this.getAccessor());

				if (SAT_DEBUG && entitypatch.getOriginal() == net.minecraft.client.Minecraft.getInstance().player) {
					double dbgDot = entitypatch.getOriginal().getViewVector(1.0F).dot(entitypatch.getOriginal().getDeltaMovement());
					yesman.epicfight.EpicFight.LOGGER.info("[SAT] {} cur={} prev={} dot={} |v|={}", this.getAccessor().registryName(), currentStateId, prevState, String.format("%.4f", dbgDot), String.format("%.3f", entitypatch.getOriginal().getDeltaMovement().length()));
				}

				prevState.ifPresentOrElse(prevStateId -> {
					if (prevStateId != currentStateId) {
						int streak = entitypatch.getAnimator().getVariables().getOrDefault(STATE_STREAK, this.getAccessor()) + 1;

						if (streak >= SWITCH_STABILITY_TICKS) {
							entitypatch.getAnimator().playAnimation(this.animationsInEachState.get(currentStateId), 0.0F);
							entitypatch.getAnimator().getVariables().put(PREVIOUS_STATE, this.getAccessor(), currentStateId);
							entitypatch.getAnimator().getVariables().put(STATE_STREAK, this.getAccessor(), 0);
						} else {
							entitypatch.getAnimator().getVariables().put(STATE_STREAK, this.getAccessor(), streak);
						}
					} else {
						entitypatch.getAnimator().getVariables().put(STATE_STREAK, this.getAccessor(), 0);
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

		int result = this.select(entitypatch);
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