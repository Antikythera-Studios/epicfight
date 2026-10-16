package yesman.epicfight.api.event;

import com.google.common.collect.Lists;
import yesman.epicfight.api.client.event.EpicFightClientEventHooks;
import yesman.epicfight.api.event.subscription.DefaultEventSubscription;
import yesman.epicfight.api.utils.side.LogicalSide;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

/// An event bus that is dedicated to one [Event] type
///
/// Note that this object isn't created each time an event is fired. it
/// only defines event type and its subscriptions. Look at [Event]
/// which hold event states and arguments
///
/// To create custom events, follow these codebase: [EpicFightEventHooks]
/// and [EpicFightClientEventHooks] for client-side only events
public class EventHook<T extends Event> {
	/// Treemap to order subscribers in descending order
	final ConcurrentMap<Integer, List<EventListener<T>>> subscribers = new ConcurrentSkipListMap<>(Comparator.reverseOrder());

    /// Determines if the event is only called in logical side
    protected final LogicalSide logicalSide;

    protected EventHook(LogicalSide logicalSide) {
        this.logicalSide = logicalSide;
    }

    /// Post the event to subscribers and execute tasks by their priority in descending order
    public T post(T event) {
        for (var subscriber : subscribers.values()) {
            for (var listener : subscriber) {
                if (listener.subscriptionType() instanceof DefaultEventSubscription<T> passiveSubscription) {
                    try {
                        passiveSubscription.fire(event);
                    } catch (Throwable t) {
                        org.slf4j.LoggerFactory.getLogger(EventHook.class).warn(
                                "Event listener {} threw during post of {}: {}", listener.identifier(), event.getClass().getSimpleName(), t.toString());
                    }
                }
            }
        }
        return event;
    }

    /// Post the event to subscribers including from [EntityEventListener], and execute tasks by their priority in descending order
    public T postWithListener(T event, EntityEventListener eventListener) {
        if (!(event instanceof LivingEntityPatchEvent)) {
            throw new IllegalArgumentException("EventHook instance must be a subtype of LivingEntityPatchEvent to be posted with EntityEventListener");
        }

        List<EventListener<T>> buffer = Lists.newArrayList();
        this.subscribers.values().forEach(buffer::addAll);
        Stream.concat(buffer.stream(), eventListener.getListenersFor(this))
                .sorted(EventHook::reverseOrder)
                .forEach(subs -> {
                    if (subs.subscriptionType() instanceof DefaultEventSubscription<T> passiveSubscription) {
                        try {
                            passiveSubscription.fire(event);
                        } catch (Throwable t) {
                            org.slf4j.LoggerFactory.getLogger(EventHook.class).warn(
                                    "Event listener {} threw during postWithListener of {}: {}", subs.identifier(), event.getClass().getSimpleName(), t.toString());
                        }
                    }
                });
        return event;
    }

    public static int reverseOrder(EventListener<? extends Event> A, EventListener<? extends Event> B)
    {
        return Integer.compare(B.priority(), A.priority());
    }

	/// Registers an event with default identifier and priority
	public void registerEvent(DefaultEventSubscription<T> subscription) {
		this.registerEvent(subscription, getDefaultSubscriberName(), 0);
	}
	
	/// Registers an event with default identifier
	/// @param priority determines the order of the event in descending order
	public void registerEvent(DefaultEventSubscription<T> subscription, int priority) {
		this.registerEvent(subscription, getDefaultSubscriberName(), priority);
	}
	
	/// Registers an event with default priority
	/// @param name specify the subscriber identifier to be referenced by other events, it will be stored at {@link EventContext}
	public void registerEvent(DefaultEventSubscription<T> subscription, String name) {
		this.registerEvent(subscription, name, 0);
	}
	
	/// Registers an event with full parameters
	public void registerEvent(DefaultEventSubscription<T> subscription, String name, int priority) {
		this.subscribers.computeIfAbsent(priority, sub -> new CopyOnWriteArrayList<>()).add(new EventListener<>(name,priority,subscription));
	}

    public final LogicalSide logicalSide() {
        return this.logicalSide;
    }

	/// Returns a class identifier who called register_event methods
	protected static String getDefaultSubscriberName() {
		StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        StackTraceElement caller = stackTraceElements[2];
        return caller.getClassName();
	}
	
	/// Defines a default event type
	public static <T extends Event> EventHook<T> createEventHook() {
		return new EventHook<> (LogicalSide.BOTH);
	}

    /// Defines a sided default event type
    public static <T extends Event> EventHook<T> createSidedEventHook(LogicalSide logicalSide) {
        return new EventHook<> (logicalSide);
    }
}
