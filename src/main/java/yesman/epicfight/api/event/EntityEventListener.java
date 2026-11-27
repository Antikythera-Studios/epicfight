package yesman.epicfight.api.event;

import org.jetbrains.annotations.ApiStatus;
import yesman.epicfight.api.event.subscription.ContextAwareEventSubscription;
import yesman.epicfight.api.event.subscription.EventSubscriptionType;
import yesman.epicfight.api.event.subscription.DefaultEventSubscription;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/// Set of Event listeners owned by [LivingEntityPatch]
///
/// Unlike the event subscribers that is applied globally and permanent, you can add and remove
/// listeners dynamically in runtime by this module.
///
/// All events are required a subscriber identifier to be registered, which is used when
/// removing existing subscribers
///
/// See [EventSubscriptionType] for the detailed explanation about each subscriptionType type
public class EntityEventListener {
    private final LivingEntityPatch<?> entityPatch;
    private final Map<EventHook<? extends LivingEntityPatchEvent>, List<EventListener<? extends LivingEntityPatchEvent>>> eventListeners;

    public EntityEventListener(LivingEntityPatch<?> entityPatch) {
        this.entityPatch = entityPatch;
        this.eventListeners = new HashMap<> ();
    }

    /// Returns subscribers for given even
    @ApiStatus.Internal
    @SuppressWarnings("unchecked")
    public <T extends Event> Stream<EventListener<T>> getListenersFor(EventHook<T> event) {
        return this.eventListeners.containsKey(event) ? this.eventListeners.get(event).stream().map(subscriber -> (EventListener<T>)subscriber) : Stream.empty();
    }

    /// Cheap emptiness check mirroring [EntityEventListener#getListenersFor] -
    /// lets callers skip merge/sort fast paths when no entity listeners exist.
    @ApiStatus.Internal
    public boolean hasListenersFor(EventHook<? extends Event> event) {
        List<EventListener<? extends LivingEntityPatchEvent>> list = this.eventListeners.get(event);
        return list != null && !list.isEmpty();
    }

    /// Registers an event with default priority
    /// @param identifier specify the subscriber identifier to be referenced by other events, it will be stored at [EventContext]
    ///                   use this subscriber identifier to remove the listener
    public <T extends LivingEntityPatchEvent> void registerEvent(EventHook<T> event, DefaultEventSubscription<T> subscription, IdentifierProvider identifier) {
        this.registerEvent(event, subscription, identifier, 0);
    }

    /// Registers an event with full parameters
    public <T extends LivingEntityPatchEvent> void registerEvent(EventHook<T> event, DefaultEventSubscription<T> subscription, IdentifierProvider identifier, int priority) {
        if (event.logicalSide().isEntityOnValidSide(this.entityPatch.getOriginal())) {
            var subscriberList = this.eventListeners.computeIfAbsent(event, key -> new ArrayList<> ());
            subscriberList.add(new EventListener<> (identifier.getStringId(), priority, subscription));
        }
    }

    /// Registers an event with default priority
    /// @param identifier specify the subscriber identifier to be referenced by other events, it will be stored at [EventContext]
    ///                   use this subscriber identifier to remove the listener
    public <T extends LivingEntityPatchEvent & CancelableEvent> void registerContextAwareEvent(EventHook<T> event, ContextAwareEventSubscription<T> subscription, IdentifierProvider identifier) {
        this.registerContextAwareEvent(event, subscription, identifier, 0);
    }

    /// Registers an event with full parameters
    public <T extends LivingEntityPatchEvent & CancelableEvent> void registerContextAwareEvent(EventHook<T> event, ContextAwareEventSubscription<T> subscription, IdentifierProvider identifier, int priority) {
        if (event.logicalSide().isEntityOnValidSide(this.entityPatch.getOriginal())) {
            var subscriberList = this.eventListeners.computeIfAbsent(event, key -> new ArrayList<>());
            subscriberList.add(new EventListener<> (identifier.getStringId(), priority, subscription));
        }
    }

    /// Removes all listeners matching with the given identifier
    public void removeListenersBelongTo(IdentifierProvider identifier) {
        if (IdentifierProvider.permanent().equals(identifier)) {
            throw new IllegalArgumentException("Can't remove permanent listener");
        }

        // remove matching subscribers with given identifier
        this.eventListeners.values().forEach(subscriberList -> subscriberList.removeIf(subscriber -> subscriber.identifier().equals(identifier.getStringId())));

        // remove empty events
        this.eventListeners.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }

    /// Returns the owner
    public LivingEntityPatch<?> getEntityPatch() {
        return this.entityPatch;
    }
}
