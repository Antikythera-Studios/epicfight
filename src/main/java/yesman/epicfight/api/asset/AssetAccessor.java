package yesman.epicfight.api.asset;

import java.util.NoSuchElementException;
import java.util.function.Consumer;
import java.util.function.Supplier;

import net.minecraft.resources.ResourceLocation;

/**
 * An accessor class
 * @param <O> {@link Object} can be any object
 */
public interface AssetAccessor<O> extends Supplier<O> {
	O get();
	
	ResourceLocation registryName();
	
	default boolean isPresent() {
		return this.get() != null;
	}
	
	default boolean isEmpty() {
		return !this.isPresent();
	}
	
	boolean inRegistry();
	
	default boolean checkType(Class<?> cls) {
		return cls.isAssignableFrom(this.get().getClass());
	}
	
	default O orElse(O whenNull) {
		return this.isPresent() ? this.get() : whenNull;
	}
	
	default void ifPresent(Consumer<O> action) {
		if (this.isPresent()) {
			action.accept(this.get());
		}
	}
	
	default void ifPresentOrElse(Consumer<O> action, Runnable whenNull) {
		if (this.isPresent()) {
			action.accept(this.get());
		} else {
			whenNull.run();
		}
	}
	
	default void doOrThrow(Consumer<O> action) {
		if (this.isPresent()) {
			action.accept(this.get());
		} else {
			throw new NoSuchElementException("No asset " + this.registryName());
		}
	}
	
	default void checkNotNull() {
		if (!this.isPresent()) {
			throw new NoSuchElementException("No asset " + this.registryName());
		}
	}
}