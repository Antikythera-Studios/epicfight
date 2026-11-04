package yesman.epicfight.api.animation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

class PostLoadMutationJournalTest {
	/// Stand-in for a StaticAnimation: an ordered event list plus a
	/// phase-property map, the two shapes post-load mutations touch.
	private static final class DummyAnimation {
		final List<String> events = new ArrayList<> ();
		final Map<String, Integer> phaseProperties = new LinkedHashMap<> ();
	}

	private static Consumer<DummyAnimation> addEvent(String name) {
		return animation -> animation.events.add(name);
	}

	private static Consumer<DummyAnimation> setPhaseProperty(String key, int value) {
		return animation -> animation.phaseProperties.put(key, value);
	}

	private static DummyAnimation freshBase() {
		DummyAnimation animation = new DummyAnimation();
		animation.events.add("base_tick");
		animation.phaseProperties.put("anticipation", 0);
		
		return animation;
	}

	@Test
	void appliesOncePerSourceObject() {
		PostLoadMutationJournal<String, DummyAnimation> journal = new PostLoadMutationJournal<> ();
		Object source = new Object();
		DummyAnimation animation = freshBase();
		
		journal.apply("anim:a", source, animation, addEvent("config_event"));
		journal.apply("anim:a", source, animation, addEvent("config_event"));
		
		assertEquals(List.of("base_tick", "config_event"), animation.events);
	}

	@Test
	void distinctEqualSourcesBothApplyInOrder() {
		PostLoadMutationJournal<String, DummyAnimation> journal = new PostLoadMutationJournal<> ();
		Object sourceA = new String("shared-source");
		Object sourceB = new String("shared-source");
		assertEquals(sourceA, sourceB);
		DummyAnimation animation = freshBase();
		
		journal.apply("anim:a", sourceA, animation, addEvent("first"));
		journal.apply("anim:a", sourceB, animation, addEvent("second"));
		
		assertEquals(List.of("base_tick", "first", "second"), animation.events);
	}

	@Test
	void replayReproducesConfigOnEveryRebuild() {
		PostLoadMutationJournal<String, DummyAnimation> journal = new PostLoadMutationJournal<> ();
		Object eventSource = new Object();
		Object phaseSource = new Object();
		DummyAnimation first = freshBase();
		
		journal.apply("anim:a", eventSource, first, addEvent("config_event"));
		journal.apply("anim:a", phaseSource, first, setPhaseProperty("anticipation", 5));
		
		assertEquals(List.of("base_tick", "config_event"), first.events);
		assertEquals(Map.of("anticipation", 5), first.phaseProperties);
		
		for (int cycle = 0; cycle < 3; cycle++) {
			DummyAnimation rebuilt = freshBase();
			journal.replay("anim:a", rebuilt);
			
			assertEquals(List.of("base_tick", "config_event"), rebuilt.events, "cycle " + cycle);
			assertEquals(Map.of("anticipation", 5), rebuilt.phaseProperties, "cycle " + cycle);
		}
	}

	@Test
	void applyAfterReplayIsNoOp() {
		PostLoadMutationJournal<String, DummyAnimation> journal = new PostLoadMutationJournal<> ();
		Object source = new Object();
		DummyAnimation animation = freshBase();
		
		journal.apply("anim:a", source, animation, addEvent("config_event"));
		
		DummyAnimation rebuilt = freshBase();
		journal.replay("anim:a", rebuilt);
		journal.apply("anim:a", source, rebuilt, addEvent("config_event"));
		
		assertEquals(List.of("base_tick", "config_event"), rebuilt.events);
	}

	@Test
	void registryKeysIsolateSharedClipConfigs() {
		PostLoadMutationJournal<String, DummyAnimation> journal = new PostLoadMutationJournal<> ();
		Object sharedSource = new Object();
		DummyAnimation targetA = freshBase();
		DummyAnimation targetB = freshBase();
		
		journal.apply("anim:a", sharedSource, targetA, addEvent("config_a"));
		journal.apply("anim:b", sharedSource, targetB, addEvent("config_b"));
		
		assertEquals(List.of("base_tick", "config_a"), targetA.events);
		assertEquals(List.of("base_tick", "config_b"), targetB.events);
		
		DummyAnimation rebuiltA = freshBase();
		journal.replay("anim:a", rebuiltA);
		assertEquals(List.of("base_tick", "config_a"), rebuiltA.events);
	}

	@Test
	void failedMutationIsNotJournaled() {
		PostLoadMutationJournal<String, DummyAnimation> journal = new PostLoadMutationJournal<> ();
		Object source = new Object();
		DummyAnimation animation = freshBase();
		Consumer<DummyAnimation> broken = anim -> {
			throw new IllegalStateException("boom");
		};
		
		assertThrows(IllegalStateException.class, () -> journal.apply("anim:a", source, animation, broken));
		journal.apply("anim:a", source, animation, addEvent("recovered"));
		
		assertEquals(List.of("base_tick", "recovered"), animation.events);
		
		DummyAnimation rebuilt = freshBase();
		journal.replay("anim:a", rebuilt);
		assertEquals(List.of("base_tick", "recovered"), rebuilt.events);
	}

	@Test
	void recordIsAdditiveWhileApplyDedups() {
		PostLoadMutationJournal<String, DummyAnimation> journal = new PostLoadMutationJournal<> ();
		Object source = new Object();
		Consumer<DummyAnimation> mutation = addEvent("config_event");
		DummyAnimation animation = freshBase();
		
		journal.record("anim:a", source, mutation);
		journal.record("anim:a", source, mutation);
		journal.replay("anim:a", animation);
		
		assertEquals(List.of("base_tick", "config_event", "config_event"), animation.events);
		
		DummyAnimation next = freshBase();
		journal.apply("anim:a", source, next, mutation);
		assertEquals(List.of("base_tick"), next.events);
	}

	@Test
	void reentrantApplyDuringReplayRunsOnce() {
		PostLoadMutationJournal<String, DummyAnimation> journal = new PostLoadMutationJournal<> ();
		Object firstSource = new Object();
		Object secondSource = new Object();
		
		journal.record("anim:a", firstSource, animation -> {
			animation.events.add("first");
			journal.apply("anim:a", secondSource, animation, addEvent("second"));
		});
		
		DummyAnimation rebuilt = freshBase();
		journal.replay("anim:a", rebuilt);
		assertEquals(List.of("base_tick", "first", "second"), rebuilt.events);
		
		DummyAnimation rebuiltAgain = freshBase();
		journal.replay("anim:a", rebuiltAgain);
		assertEquals(List.of("base_tick", "first", "second"), rebuiltAgain.events);
	}

	@Test
	void replayFollowsStableKeyAcrossReplacement() {
		PostLoadMutationJournal<String, DummyAnimation> journal = new PostLoadMutationJournal<> ();
		Object source = new Object();
		
		journal.record("anim:a", source, addEvent("config_event"));
		
		DummyAnimation replacement = freshBase();
		journal.replay("anim:a", replacement);
		
		assertEquals(List.of("base_tick", "config_event"), replacement.events);
	}

	@Test
	void nullArgumentsRejected() {
		PostLoadMutationJournal<String, DummyAnimation> journal = new PostLoadMutationJournal<> ();
		Object source = new Object();
		DummyAnimation animation = freshBase();
		Consumer<DummyAnimation> mutation = addEvent("x");
		
		assertThrows(NullPointerException.class, () -> journal.apply(null, source, animation, mutation));
		assertThrows(NullPointerException.class, () -> journal.apply("anim:a", null, animation, mutation));
		assertThrows(NullPointerException.class, () -> journal.apply("anim:a", source, null, mutation));
		assertThrows(NullPointerException.class, () -> journal.apply("anim:a", source, animation, null));
		assertThrows(NullPointerException.class, () -> journal.record("anim:a", null, mutation));
		assertThrows(NullPointerException.class, () -> journal.record("anim:a", source, null));
		assertThrows(NullPointerException.class, () -> journal.replay("anim:a", null));
		
		journal.replay("anim:a", animation);
		assertEquals(List.of("base_tick"), animation.events);
	}
}
