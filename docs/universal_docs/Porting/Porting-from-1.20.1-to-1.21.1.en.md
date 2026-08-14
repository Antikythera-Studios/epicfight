---
icon: material/vector-line
hide:
  - announcement
---

# Porting 1.20.1 :material-arrow-right: 1.21.1

In this article, I'll introduce the most breaking changes from 1.20.1 and help add-on developers to figure out
how to properly adapt the changes for their project.

***
## Skill Registration

Epic Fight now uses the Deferred register from NeoForge. This gets rid of the incongruous registration of skills
and provides a more integrated system with mod loader.

### Affections
- `yesman.epicfight.api.forgeevent.SkillBuildEvent` is removed as the skill registration is now through
  Deferred Register.

### Migration Example

This is a comparison of BasicAttack(ComboAttacks in 1.21.1) registration

*In 1.20.1*

```java
BASIC_ATTACK = modRegistry.build("basic_attack", BasicAttack::new, BasicAttack.createBasicAttackBuilder());
```

*In 1.21.1*

```java
public static final DeferredRegister<Skill> REGISTRY = DeferredRegister.create(EpicFightRegistries.Keys.SKILL, EpicFightMod.MODID);

...

public static final DeferredHolder<Skill, ComboAttacks> COMBO_ATTACKS = REGISTRY.register("combo_attacks", key ->
    ComboAttacks.createComboAttackBuilder().build(key)
);
```

You can keep the old builder pattern, but the method that constructs skill by builder is significantly different.
It receives the registration key as `ResourceLocation`, which you need to register skills via consumer,
`(key) -> {}` not, `() -> {}`

This is a more complicated skill registration example with a damage-source property pattern.

*In 1.20.1*

```java
WeaponInnateSkill eviscerate = modRegistry.build("eviscerate", EviscerateSkill::new, WeaponInnateSkill.createWeaponInnateBuilder());
    eviscerate.newProperty()
        .addProperty(AttackPhaseProperty.MAX_STRIKES_MODIFIER, ValueModifier.setter(1))
        .addProperty(AttackPhaseProperty.IMPACT_MODIFIER, ValueModifier.setter(2.0F))
        ...
        .newProperty()
        .addProperty(AttackPhaseProperty.MAX_STRIKES_MODIFIER, ValueModifier.setter(1))
        .addProperty(AttackPhaseProperty.EXTRA_DAMAGE, Set.of(ExtraDamageInstance.SWEEPING_EDGE_ENCHANTMENT.create(), ExtraDamageInstance.EVISCERATE_LOST_HEALTH.create(0.1F)))
        ...
    EVISCERATE = eviscerate;
```

*In 1.21.1*

```java
public static final DeferredRegister<Skill> REGISTRY = DeferredRegister.create(EpicFightRegistries.Keys.SKILL, EpicFightMod.MODID);
...
public static final DeferredHolder<Skill, EviscerateSkill> EVISCERATE = REGISTRY.register("eviscerate", key ->
    WeaponInnateSkill.createWeaponInnateBuilder(EviscerateSkill::new)
        .newProperty()
            .addProperty(AttackPhaseProperty.MAX_STRIKES_MODIFIER, ValueModifier.setter(1))
            .addProperty(AttackPhaseProperty.IMPACT_MODIFIER, ValueModifier.setter(2.0F))
        ...
        .newProperty()
            .addProperty(AttackPhaseProperty.MAX_STRIKES_MODIFIER, ValueModifier.setter(1))
            .addProperty(AttackPhaseProperty.EXTRA_DAMAGE, Set.of(ExtraDamageInstance.SWEEPING_EDGE_ENCHANTMENT.create(), ExtraDamageInstance.EVISCERATE_LOST_HEALTH.create(0.1F)))
        ...
        .build(key)
    );
```

**Deferred Register** is Forge and NeoForge's registration system to add more entries to
Minecraft's frozen registries have control over registration timing and freezing status. Check out
[Neoforge's Deffered Register Document] for a more in-depth explanation.

***
## Adding & removing Skill Events to the listener

When the `Skill` is equipped to players, we registered listeners for specific event hooks and manually removed them when
the skill is unequipped. In 1.21.1, Epic Fight provides more streamlined registration without `UUID` and needs to remove
listeners manually.

### Example

*In 1.20.1*

```java
public class BerserkerSkill extends PassiveSkill {
    ...
    public void onInitiate(SkillContainer container) {
        super.onInitiate(container);

        container.getExecutor().getEventListener().addEventListener(EventType.MODIFY_ATTACK_SPEED_EVENT, EVENT_UUID, (event) -> {
            // Do event tasks...
        });
        ...
    }

    public void onRemoved(SkillContainer container) {
        super.onRemoved(container);

        container.getExecutor().getEventListener().removeListener(EventType.MODIFY_ATTACK_SPEED_EVENT, EVENT_UUID);
        ...
    }
    ...

```

```java
public class BerserkerSkill extends PassiveSkill {
    public void onInitiate(SkillContainer container, EntityEventListener eventListener) {
        super.onInitiate(container, eventListener);

        eventListener.registerEvent(
            EpicFightEventHooks.Entity.MODIFY_ATTACK_SPEED,
            event -> {
                // Do event tasks...
            },
            this
        );
    }
    
    // public void onRemoved(SkillContainer container), No need to call remove methods, it's fully automated!
}
```
***
## Method Parameters Overview

Below is an explanation of each parameter used by the `registerEvent` method.

* Param1: `EventHook<T>`
  Replaces `EventType` in `1.20.1`, but it uses our newer Event API system, as described at [Independent Event API].
  You can check the type of event hooks both in `yesman.epicfight.api.event.EpicFightEventHooks`
  and `yesman.epicfight.api.client.event.EpicFightClientEventHooks`

* Param2: `DefaultEventSubscription` or `ContextAwareEventSubscription` for `registerContextAwareEvent`
  A functional interface where you set tasks when the event is triggered. See [Independent Event API] for details.

* Param3: `IdentifierProvider`
  You'll always give the skill instance itself since the skill inherits the `IdentifierProvider` interface. This replaces
  the work for what `UUID` did in the older system, but automatically when the skill is unequipped.

* Param4(Skipped in example snippet): Priority(int)
  For the cases that you need to intercept other event subscribers, I made it to be ordered in descending order. Which
  means you can cancel the events with lower priority. (This feature was already in `1.20.1`)

***
## Epic Fight now seeks Multi-loader structure
After extensive discussion, our team concluded that Epic Fight should become more future-proof by
adopting a more open, multi-loader architecture. This shift aims to better support additional mod loaders
such as Fabric, which the community has requested for years, and to align Epic Fight with newer APIs and modern Minecraft modding standards.

!!! info "Prototype Status"

	At present, the multi-loader structure exists only as a prototype, and Epic Fight has not yet been ported to Fabric.
	However, this prototype already demonstrates that Epic Fight can function as a multi-platform project.


!!! tip

	This change will not be introduced in ``1.21.1``, meaning addon developers do not need to migrate anything for this version.
	That said, if you plan to support Fabric in the future, you should begin decoupling NeoForge-specific code in your project.



***
## Epic Fight now seeks a more independent event system from mod-loaders
Driven by the [move toward a multi-loader architecture], we needed to decouple the codebase from
as we need to create multiple event instances for the number of mod-loaders that we depend on. Instead, Epic Fight now has
a unique Event system independent of whatever mod loader.

The main target that is affected by this change is the skill event listener, which was introduced at the
[Adding & removing Skill Events to listener] section.

### Example
This is a simple example of adding a subscriber that prints a message when the innate skill is set to Sweeping Edge.

```java
EpicFightEventHooks.Player.CHANGE_INNATE_SKILL.registerEvent(event -> {
    if (event.getToItemCapability().getInnateSkill(event.getPlayerPatch(), event.getTo()) == EpicFightSkills.SWEEPING_EDGE.get()) {
        System.out.println("Changed to Sweeping Edge skill.");
    }
});
```
Basically, you can call this registration event anywhere, but I strongly recommend registering events in the mod
initializing stage.

### Example with full arguments

```java
EpicFightEventHooks.Player.CHANGE_INNATE_SKILL.registerEvent(
    event -> {
        if (event.getToItemCapability().getInnateSkill(event.getPlayerPatch(), event.getTo()) == EpicFightSkills.SWEEPING_EDGE.get()) {
            System.out.println("Changed to Sweeping Edge skill.");
        }
    },
    "MySubscriber", // Set the name of the subscriber
    10              // Set priority
);
```

You can also set the name of your subscriber, which is mainly used by `ContextAwareEventSubscription`, which will
be explained in the following section.

The priority will organize the subscribers in descending order. Any event subscriber who cancels the event will prevent
triggering events with lower priorities.

### Event Cancelation, and Context Awaring Subscriber
The new event system also provides ways to interrupt the following process. Some event hooks are instances of
`CancelableEventHook`, that means, you can cancel the event to intercept the following process.

Even though an event is canceled, you're still able to trigger your task. Registering as `ContextAwareEventSubscription`
will still visit your subscriber even if it's canceled by another event with a higher priority.

```java
EpicFightEventHooks.Entity.TAKE_DAMAGE_INCOME.registerEvent(
    event -> {
        if (event.getEntityPatch().getOriginal().getHealth() < 1.0F) {
            event.cancel(); // Cancel damage incoming event when the targetted entity's health is lower than 1.0
        }
    },
    "MyRegistrar",
    10 // set priority 10
);

EpicFightEventHooks.Entity.TAKE_DAMAGE_INCOME.registerContextAwareEvent(
    (event, eventContext) -> {
        if (event.getEntityPatch().getOriginal().getHealth() < 1.0F && eventContext.isCanceledBy("MyRegistrar")) {
            event.getEntityPatch().getOriginal().kill(); // Kill the entity if the event is canceled by "MyRegistrar"
        }
    },
    1 // set priority 1
);
```

The first subscriber in the example will make entities never die if their health is below 1.0. However, the
following subscriber kills the entity by checking the subscriber name. The first subscriber will be triggered before
the second one is triggered and cancel the event, but the second subscriber will still be triggered since it's
`ContextAwareEventSubscription`.

### Creating Custom Event Hooks

You can create custom event hooks for your usage. You first need a class that will contain all your event hook instances.
If you make an event hook as *sided*, they will only trigger events in the given **logical** side.

```java
public final class MyEventHooks {
    // parameterize with your event class
    public static final EventHook<MyEventHook> MY_EVENT_HOOK = EventHook.createEventHook();
    
    // subscribers only be triggered in server side even tho it's called in client
    public static final EventHook<MyServerEventHook> MY_SERVER_SIDE_EVENT_HOOK = EventHook.createSidedEventHook(LogicalSide.CLIENT);
    
    // Cancelable events allow interrupting the final task of the event
    public static final CancelableEventHook<MyCancelableEventHook> MY_CANCELABLE_EVENT_HOOK = CancelableEventHook.createEventHook();
    
    // MyLivingEntityEventHook inherits `LivingEntityPatchEvent`, where you can trigger subscribers in `EntityEventListener`
    public static final EventHook<MyLivingEntityEventHook> MY_LIVING_ENTITY_EVENT_HOOK = EventHook.createEventHook();
    
    private MyEventHooks() {} // prevents instantiation
} 
```

Then, you can trigger the event as:
```java
MyEventHooks.MY_EVENT_HOOK.post(new MyEventHook());
```

You can also check if the event is canceled:
```java
if (!MyEventHooks.MY_CANCELABLE_EVENT_HOOK.post(new MyCancelableEventHook()).isCanceled()) { // Checks if event is canceled
    // Do tasks when the event is not canceled
}
```

### Using `EntityEventListener` to trigger attachable event listeners
As described in [Adding & removing Skill Events to listener] section,
there are some event types that developers can attach and detach listeners dynamically. Those event types all inherit
`LivingEntityPatchEvent`.

You can trigger the event hooks via `postWithListener`.

```java
Entity entity;
LivingEntityPatch<?> entityPatch = EpicFightCapabilities.getEntityPatch(entity, LivingEntityPatch.class); // Extracts entity patch from entity

MyEventHooks.MY_LIVING_ENTITY_EVENT_HOOK.postWithListener(new MyLivingEntityEventHook(entityPatch), entityPatch.getEventListener());
```
