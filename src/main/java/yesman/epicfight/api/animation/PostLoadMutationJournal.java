package yesman.epicfight.api.animation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

/// Ordered per-key journal of post-load animation mutations. Each entry is
/// identified by a caller-chosen source object; identity (==) decides whether a
/// source has already been recorded under a key, so refired config passes that
/// reuse the same property objects become no-ops while structurally equal but
/// distinct sources stay distinct. Mutations that throw are never recorded.
final class PostLoadMutationJournal<K, T> {
	private final Map<K, List<Consumer<T>>> mutations = new HashMap<> ();
	private final Map<K, Set<Object>> recordedSources = new HashMap<> ();
	
	boolean isRecorded(K key, Object source) {
		Set<Object> sources = this.recordedSources.get(key);
		return sources != null && sources.contains(source);
	}
	
	/// Applies the mutation to the target, then records it for future replays.
	/// No-op when the source was already recorded under the key; a failed
	/// mutation propagates without being recorded.
	void apply(K key, Object source, T target, Consumer<T> mutation) {
		Objects.requireNonNull(key, "key");
		Objects.requireNonNull(source, "source");
		Objects.requireNonNull(target, "target");
		Objects.requireNonNull(mutation, "mutation");
		
		if (this.isRecorded(key, source)) {
			return;
		}
		
		mutation.accept(target);
		this.record(key, source, mutation);
	}
	
	/// Records a mutation already applied by the caller so it is re-applied by
	/// {@link #replay}. Deliberately additive: every call appends, even for a
	/// repeated (key, source, consumer) triple, while {@link #apply} alone
	/// enforces apply-once-per-source semantics.
	void record(K key, Object source, Consumer<T> mutation) {
		Objects.requireNonNull(key, "key");
		Objects.requireNonNull(source, "source");
		Objects.requireNonNull(mutation, "mutation");
		
		this.mutations.computeIfAbsent(key, k -> new ArrayList<> ()).add(mutation);
		this.recordedSources.computeIfAbsent(key, k -> Collections.newSetFromMap(new IdentityHashMap<> ())).add(source);
	}
	
	/// Re-applies every mutation recorded under the key, in record order.
	/// Iterates an ordered snapshot so a mutation that registers further
	/// mutations does not corrupt this pass; the new entries run on the next
	/// replay, while their {@link #apply} still executes immediately.
	void replay(K key, T target) {
		Objects.requireNonNull(key, "key");
		Objects.requireNonNull(target, "target");
		
		List<Consumer<T>> recorded = this.mutations.get(key);
		if (recorded != null) {
			List.copyOf(recorded).forEach(mutation -> mutation.accept(target));
		}
	}
}
