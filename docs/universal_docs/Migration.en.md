---
icon: material/file-multiple
hide:
  - announcement
---
# Migration

If you're a mod developer with a mod that targets older,
now-incompatible versions of Epic Fight, this guide will help you migrate to the latest version.

***
## :octicons-code-16: From `20.9.7` → `20.10.2.101` and Newer Versions

These breaking changes were introduced in an **Epic Fight** version that already supports **Forge 1.20.1**.  
However, the initial release of **NeoForge 1.21.1** already uses the **new system**,  
so this guide is **only relevant** for mod developers who previously supported **Epic Fight `20.9.7`**  
(which was available **only for Forge 1.20.1**).

### :material-file-code: Accessors

One of the **breaking changes** introduced in versions **newer than `20.9.7`**  
is the way **animations** and **armatures** are registered.

Previously, you would register animations **directly**, like this:

```java
@Mod.EventBusSubscriber(modid = YourMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class YourModAnimations {
    public static StaticAnimation CUSTOM;

    @SubscribeEvent
    public static void registerAnimations(AnimationRegistryEvent event) {
        event.getRegistryMap().put(YourMod.MOD_ID, Animation::build);
    }
    
    private static void build() {
        HumanoidArmature biped = Armatures.BIPED;

        CUSTOM = new StaticAnimation(true, "biped/living/custom", biped);
    }
}
```

But in versions **newer than `20.9.7`**, they are registered **like this**:

```java
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.AnimationManager.AnimationRegistryEvent;
import yesman.epicfight.gameasset.Armatures.ArmatureAccessor;

@Mod.EventBusSubscriber(modid = YourMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class YourModAnimations {
    public static AnimationAccessor<StaticAnimation> CUSTOM;

    @SubscribeEvent
    public static void registerAnimations(AnimationRegistryEvent event) {
        event.newBuilder(YourMod.MOD_ID, Animation::build);
    }
    
    private static void build(AnimationManager.AnimationBuilder builder) {
        ArmatureAccessor<HumanoidArmature> armatureAccessor = Armatures.BIPED;

        CUSTOM = builder.nextAccessor("biped/living/custom", (accessor) -> new StaticAnimation(true, accessor, armatureAccessor));
    }
}
```

The **same pattern** now applies to **armatures** as well.

!!! tip
    We understand that this is a **major breaking change** and may cause **inconvenience**,  
    as it affects every Epic Fight addon.

    However, this change was necessary to **resolve existing issues** with the previous system.  
    The new approach ensures **deferred registration**, helping prevent potential problems  
    with the **mod loading lifecycle**.
