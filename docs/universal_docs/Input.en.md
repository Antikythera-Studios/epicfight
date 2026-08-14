---
icon: material/exit-to-app
hide:
  - announcement
---

# Input

This section covers input handling and the abstractions that Epic Fight introduces to support controllers.

!!! warning
    These APIs are currently marked as **experimental**.  
    This designation **does not imply** that the implementation is of an 'experimental' quality,  
    but rather indicates that classes, methods, and fields may be subject to renaming, relocation, or removal.  
    The Epic Fight team may introduce **breaking changes** to the existing API at any time without prior notice.

***

## Overview

Epic Fight introduces an input abstraction layer to unify keyboard and controller handling.  
This allows mods to interact with player input consistently, regardless of the input.

## Background Issue

Directly depending on the vanilla `KeyMapping` often does not work correctly with controller mods and usually requires workarounds or hacks.  
These abstractions were introduced because they are widely used across Epic Fight and help avoid direct dependencies on any specific controller mod.

For more details, refer to
issue [#2116](https://github.com/Epic-Fight/epicfight/issues/2116) and
pull request [#2122](https://github.com/Epic-Fight/epicfight/pull/2122), which include all relevant information.

## Input Actions

Input actions replace direct use of `KeyMapping` with controller-compatible APIs.

```diff
- EpicFightKeyMappings.DODGE; // Raw access to KeyMapping, does not support controllers.
+ EpicFightInputAction.DODGE; // Use raw ".keyMapping()" if really necessary; prefer new supported APIs.
```

## Handling Input Actions

Epic Fight separates input actions into two main types: **discrete** (triggered once per press) and **continuous** (active while held).

### Discrete (one-time)

Used for actions that should trigger once when pressed, such as attacking or dodging.

```diff
- while (isKeyPressed(EpicFightKeyMappings.ATTACK, true)) {
-    // ...
- }
+ InputManager.triggerOnPress(EpicFightInputAction.ATTACK, true, (context) -> {
+      boolean triggeredByController = context.triggeredByController() // Optional context
+     // ...
+ });
```

### Continuous

Used for actions that remain active while a key or button is held down, such as guard.

```diff
- if (minecraft.options.keyAttack.isDown()) {}
+ if (InputManager.isActionActive(MinecraftInputAction.ATTACK_DESTROY)) {}
```

## Player Movement State

Player movement input is handled through immutable state objects, allowing safer and more predictable updates compared 
to directly modifying vanilla `Input` fields.

### Updating state

```diff
- Minecraft.getInstance().player.input.jumping = true; // Bad: Mutable, unpredictable, and may not support controllers
+ final PlayerInputState updated = InputManager.getInputState(Minecraft.getInstance().player)
+        .withJumping(true);
+ InputManager.setInputState(updated);
```

### Retrieving Direction

```diff
- int forward = input.up ? 1 : 0;
- int backward = input.down ? -1 : 0;
- int left = input.left ? 1 : 0;
- int right = input.right ? -1 : 0;
- int vertic = forward + backward;
- int horizon = left + right;
+ final MovementDirection movementDirection = MovementDirection.fromInputState(InputManager.getInputState(input));
+ final int vertic = movementDirection.vertical();
+ final int horizon = movementDirection.horizontal();
```

## Comparing Bindings

Determines if two actions share the same physical key or button:

```diff
- if (EpicFightKeyMappings.WEAPON_INNATE_SKILL.getKey().equals(EpicFightKeyMappings.ATTACK.getKey())) {}
+ if (InputManager.isBoundToSamePhysicalInput(EpicFightInputAction.WEAPON_INNATE_SKILL, EpicFightInputAction.ATTACK)) {}
```

## Controller

These APIs are specific to controller mods and are not part of vanilla.

### Integrating a controller mod

To let Epic Fight detect custom keybinds, compare bindings, and access other controller mod-specific features, implement the
`IEpicFightControllerMod` interface and register your integration:

```java
// Controlify mod is used as an example here.
final class ControlifyModIntegration implements IEpicFightControllerMod {
    // Implement the required methods...
}

EpicFightControllerModProvider.set(YourMod.MODID, new ControlifyModIntegration());
```

!!! note
    This alone does not resolve all controller compatibility issues.  
    You may also need to disable controller player look inputs during target lock-on.  
    Fixes often require controller-specific APIs, via supported events or mixin injections.
    
    Even then, issues like cape physics may occur due to incorrect movement data 
    from the controller mod—these are generally not Epic Fight–specific.

### Low-level Controller access

```java
@Nullable IEpicFightControllerMod controllerModApi = EpicFightControllerModProvider.get();

if (controllerModApi == null {
    // Null if no controller mod integration is available
   // (i.e., the controller mod is not installed, or Epic Fight does not support it).
    return;
}

// Checking the input mode

InputMode inputMode = controllerModApi.getInputMode();
boolean supportsController = inputMode.supportsController();

// Getting raw controller binding

ControllerBinding moveForward = controllerModApi.getBinding(MinecraftInputAction.MOVE_FORWARD);
float analogue = moveForward.getAnalogueNow();

// OR

ControllerBinding jump = controllerModApi.getBinding(MinecraftInputAction.JUMP);
boolean justPressed = jump.isDigitalJustPressed();

```

!!! tip
    This documentation currently does not cover
    [
    `ControlEngine`](https://github.com/Epic-Fight/epicfight/blob/1.21.1/src/main/java/yesman/epicfight/client/events/engine/ControlEngine.java)
    or other internal APIs, as they are not intended for public use.
