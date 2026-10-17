package yesman.epicfight.api.animation.property;

import java.util.function.Predicate;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

@SuppressWarnings({"rawtypes", "unchecked"})
public abstract class AnimationEvent<EVENT extends AnimationEvent.Event<?, ?, ?, ?, ?, ?, ?, ?, ?, ?>, T extends AnimationEvent<EVENT, T>> {
	protected final AnimationEvent.Side side;
	protected final EVENT event;
	protected AnimationParameters params;
	
	private AnimationEvent(AnimationEvent.Side executionSide, EVENT event) {
		this.side = executionSide;
		this.event = event;
	}
	
	protected abstract boolean checkCondition(LivingEntityPatch<?> entitypatch, AssetAccessor<? extends StaticAnimation> animation, float prevElapsed, float elapsed);
	
	public void execute(LivingEntityPatch<?> entitypatch, AssetAccessor<? extends StaticAnimation> animation, float prevElapsed, float elapsed) {
		if (this.side.predicate.test(entitypatch.getOriginal()) && this.checkCondition(entitypatch, animation, prevElapsed, elapsed)) {
			this.event.fire(entitypatch, animation, this.params);
		}
	}
	
	public void executeWithNewParams(LivingEntityPatch<?> entitypatch, AssetAccessor<? extends StaticAnimation> animation, float prevElapsed, float elapsed, AnimationParameters parameters) {
		if (this.side.predicate.test(entitypatch.getOriginal()) && this.checkCondition(entitypatch, animation, prevElapsed, elapsed)) {
			this.event.fire(entitypatch, animation, parameters);
		}
	}
	
	public static class SimpleEvent<EVENT extends AnimationEvent.Event<?, ?, ?, ?, ?, ?, ?, ?, ?, ?>> extends AnimationEvent<EVENT, SimpleEvent<EVENT>> {
		private SimpleEvent(AnimationEvent.Side executionSide, EVENT event) {
			super(executionSide, event);
		}
		
		@Override
		protected boolean checkCondition(LivingEntityPatch<?> entitypatch, AssetAccessor<? extends StaticAnimation> animation, float prevElapsed, float elapsed) {
			return true;
		}
		
		public static <E extends Event<?, ?, ?, ?, ?, ?, ?, ?, ?, ?>> SimpleEvent<E> create(E event, AnimationEvent.Side isRemote) {
			return new SimpleEvent<> (isRemote, event);
		}
	}
	
	public static class InTimeEvent<EVENT extends AnimationEvent.Event<?, ?, ?, ?, ?, ?, ?, ?, ?, ?>> extends AnimationEvent<EVENT, InTimeEvent<EVENT>> implements Comparable<InTimeEvent<EVENT>> {
		final float time;
		
		private InTimeEvent(float time, AnimationEvent.Side executionSide, EVENT event) {
			super(executionSide, event);
			this.time = time;
		}
		
		@Override
		public boolean checkCondition(LivingEntityPatch<?> entitypatch, AssetAccessor<? extends StaticAnimation> animation, float prevElapsed, float elapsed) {
			return this.time >= prevElapsed && this.time < elapsed;
		}
		
		@Override
		public int compareTo(InTimeEvent<EVENT> arg0) {
			if(this.time == arg0.time) {
				return 0;
			} else {
				return this.time > arg0.time ? 1 : -1;
			}
		}
		
		public static <E extends Event<?, ?, ?, ?, ?, ?, ?, ?, ?, ?>> InTimeEvent<E> create(float time, E event, AnimationEvent.Side isRemote) {
			return new InTimeEvent<> (time, isRemote, event);
		}
	}
	
	public static class InPeriodEvent<EVENT extends AnimationEvent.Event<?, ?, ?, ?, ?, ?, ?, ?, ?, ?>> extends AnimationEvent<EVENT, InPeriodEvent<EVENT>> implements Comparable<InPeriodEvent<EVENT>> {
		final float start;
		final float end;
		
		private InPeriodEvent(float start, float end, AnimationEvent.Side executionSide, EVENT event) {
			super(executionSide, event);
			this.start = start;
			this.end = end;
		}
		
		@Override
		public boolean checkCondition(LivingEntityPatch<?> entitypatch, AssetAccessor<? extends StaticAnimation> animation, float prevElapsed, float elapsed) {
			return this.start <= elapsed && this.end > elapsed;
		}
		
		@Override
		public int compareTo(InPeriodEvent<EVENT> arg0) {
			if (this.start == arg0.start) {
				return 0;
			} else {
				return this.start > arg0.start ? 1 : -1;
			}
		}
		
		public static <E extends Event<?, ?, ?, ?, ?, ?, ?, ?, ?, ?>> InPeriodEvent<E> create(float start, float end, E event, AnimationEvent.Side isRemote) {
			return new InPeriodEvent<> (start, end, isRemote, event);
		}
	}
	
	public enum Side {
		CLIENT((entity) -> entity.level().isClientSide),
		SERVER((entity) -> !entity.level().isClientSide), BOTH((entity) -> true),
		LOCAL_CLIENT((entity) -> {
			if (entity instanceof Player player) {
				return player.isLocalPlayer();
			}
			
			return false;
		});
		
		Predicate<Entity> predicate;
		
		Side(Predicate<Entity> predicate) {
			this.predicate = predicate;
		}
	}
	
	public AnimationParameters<?, ?, ?, ?, ?, ?, ?, ?, ?, ?> getParameters() {
		return this.params;
	}
	
	public <A> T params(A p1) {
		this.params = AnimationParameters.of(p1);
		return (T)this;
	}
	
	public <A, B> T params(A p1, B p2) {
		this.params = AnimationParameters.of(p1, p2);
		return (T)this;
	}
	
	public <A, B, C> T params(A p1, B p2, C p3) {
		this.params = AnimationParameters.of(p1, p2, p3);
		return (T)this;
	}
	
	public <A, B, C, D> T params(A p1, B p2, C p3, D p4) {
		this.params = AnimationParameters.of(p1, p2, p3, p4);
		return (T)this;
	}
	
	public <A, B, C, D, E> T params(A p1, B p2, C p3, D p4, E p5) {
		this.params = AnimationParameters.of(p1, p2, p3, p4, p5);
		return (T)this;
	}
	
	public <A, B, C, D, E, F> T params(A p1, B p2, C p3, D p4, E p5, F p6) {
		this.params = AnimationParameters.of(p1, p2, p3, p4, p5, p6);
		return (T)this;
	}
	
	public <A, B, C, D, E, F, G> T params(A p1, B p2, C p3, D p4, E p5, F p6, G p7) {
		this.params = AnimationParameters.of(p1, p2, p3, p4, p5, p6, p7);
		return (T)this;
	}
	
	public <A, B, C, D, E, F, G, H> T params(A p1, B p2, C p3, D p4, E p5, F p6, G p7, H p8) {
		this.params = AnimationParameters.of(p1, p2, p3, p4, p5, p6, p7, p8);
		return (T)this;
	}
	
	public <A, B, C, D, E, F, G, H, I> T params(A p1, B p2, C p3, D p4, E p5, F p6, G p7, H p8, I p9) {
		this.params = AnimationParameters.of(p1, p2, p3, p4, p5, p6, p7, p8, p9);
		return (T)this;
	}
	
	public <A, B, C, D, E, F, G, H, I, J> T params(A p1, B p2, C p3, D p4, E p5, F p6, G p7, H p8, I p9, J p10) {
		this.params = AnimationParameters.of(p1, p2, p3, p4, p5, p6, p7, p8, p9, p10);
		return (T)this;
	}
	
	@FunctionalInterface
	public interface Event<A, B, C, D, E, F, G, H, I, J> {
		void fire(LivingEntityPatch<?> entitypatch, AssetAccessor<? extends StaticAnimation> animation, AnimationParameters<A, B, C, D, E, F, G, H, I, J> params);
	}
	
	@FunctionalInterface
	public interface E0 extends Event<Void, Void, Void, Void, Void, Void, Void, Void, Void, Void> {
		void fire(LivingEntityPatch<?> entitypatch, AssetAccessor<? extends StaticAnimation> animation, AnimationParameters<Void, Void, Void, Void, Void, Void, Void, Void, Void, Void> params);
	}
	
	@FunctionalInterface
	public interface E1<A> extends Event<A, Void, Void, Void, Void, Void, Void, Void, Void, Void> {
		void fire(LivingEntityPatch<?> entitypatch, AssetAccessor<? extends StaticAnimation> animation, AnimationParameters<A, Void, Void, Void, Void, Void, Void, Void, Void, Void> params);
	}
	
	@FunctionalInterface
	public interface E2<A, B> extends Event<A, B, Void, Void, Void, Void, Void, Void, Void, Void> {
		void fire(LivingEntityPatch<?> entitypatch, AssetAccessor<? extends StaticAnimation> animation, AnimationParameters<A, B, Void, Void, Void, Void, Void, Void, Void, Void> params);
	}
	
	@FunctionalInterface
	public interface E3<A, B, C> extends Event<A, B, C, Void, Void, Void, Void, Void, Void, Void> {
		void fire(LivingEntityPatch<?> entitypatch, AssetAccessor<? extends StaticAnimation> animation, AnimationParameters<A, B, C, Void, Void, Void, Void, Void, Void, Void> params);
	}
	
	@FunctionalInterface
	public interface E4<A, B, C, D> extends Event<A, B, C, D, Void, Void, Void, Void, Void, Void> {
		void fire(LivingEntityPatch<?> entitypatch, AssetAccessor<? extends StaticAnimation> animation, AnimationParameters<A, B, C, D, Void, Void, Void, Void, Void, Void> params);
	}
	
	@FunctionalInterface
	public interface E5<A, B, C, D, E> extends Event<A, B, C, D, E, Void, Void, Void, Void, Void> {
		void fire(LivingEntityPatch<?> entitypatch, AssetAccessor<? extends StaticAnimation> animation, AnimationParameters<A, B, C, D, E, Void, Void, Void, Void, Void> params);
	}
	
	@FunctionalInterface
	public interface E6<A, B, C, D, E, F> extends Event<A, B, C, D, E, F, Void, Void, Void, Void> {
		void fire(LivingEntityPatch<?> entitypatch, AssetAccessor<? extends StaticAnimation> animation, AnimationParameters<A, B, C, D, E, F, Void, Void, Void, Void> params);
	}
	
	@FunctionalInterface
	public interface E7<A, B, C, D, E, F, G> extends Event<A, B, C, D, E, F, G, Void, Void, Void> {
		void fire(LivingEntityPatch<?> entitypatch, AssetAccessor<? extends StaticAnimation> animation, AnimationParameters<A, B, C, D, E, F, G, Void, Void, Void> params);
	}
	
	@FunctionalInterface
	public interface E8<A, B, C, D, E, F, G, H> extends Event<A, B, C, D, E, F, G, H, Void, Void> {
		void fire(LivingEntityPatch<?> entitypatch, AssetAccessor<? extends StaticAnimation> animation, AnimationParameters<A, B, C, D, E, F, G, H, Void, Void> params);
	}
	
	@FunctionalInterface
	public interface E9<A, B, C, D, E, F, G, H, I> extends Event<A, B, C, D, E, F, G, H, I, Void> {
		void fire(LivingEntityPatch<?> entitypatch, AssetAccessor<? extends StaticAnimation> animation, AnimationParameters<A, B, C, D, E, F, G, H, I, Void> params);
	}
	
	@FunctionalInterface
	public interface E10<A, B, C, D, E, F, G, H, I, J> extends Event<A, B, C, D, E, F, G, H, I, J> {
		void fire(LivingEntityPatch<?> entitypatch, AssetAccessor<? extends StaticAnimation> animation, AnimationParameters<A, B, C, D, E, F, G, H, I, J> params);
	}
}