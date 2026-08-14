---
icon: material/tools
hide:
  - announcement
---
# Utilities

Here are the utility modules used throughout the Epic Fight API.

***
## Asset Accessor
Asset accessor is Epic Fight's resource handler that dynamically loads game assets at runtime. Since it doesn't ensure the specified mesh actually exists, it is provided as an Optional format.
Accessors don't actually hold the object in their implementations. Instead, they always reference an object in the matching registry, creating a new one if there are none. And it ensures they always reference the latest after reloading assets.

### Subtypes

| Type               | Usage                            |
|--------------------|----------------------------------|
| MeshAccessor       | Accessor for Mesh resources      |
| ArmatureAccessor   | Accessor for Armature resources  |
| AnimationAccessor  | Accessor for Animation resources |

### Methods

Common

| Methods                                                     | Usage                                                                                                                                                                    |
|-------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| @Nullable O get()                                           | Returns the object that the accessor references. It will try to create an object if there is no matching entry in the registry name.                                     |
| ResourceLocation registryName()                             | Returns the registry name of the accessor.                                                                                                                               |
| boolean isPresent()                                         | Returns if the accessor references any object. Beware that this method also tries to create an asset object and returns false only if it fails or has previously failed. |
| boolean isEmpty()                                           | Vice versa of isPresent()                                                                                                                                                |
| boolean inRegistry()                                        | Returns if the accessor references a built-in object.                                                                                                                    |
| boolean checkType(Class&lt;?&gt;)                           | Returns if the referencing value can be assigned to the given class.                                                                                                     |
| O orElse(O)                                                 | Returns the referencing value, or O if it's null.                                                                                                                        |
| void ifPresent(Consumer&lt;O&gt;)                           | Runs task if the referencing value is not null                                                                                                                           |
| void ifPresentOrElse(Consumer&lt;O&gt; action, Runnable whenNull) | Runs task if the referencing value is not null, or runs whenNull                                                                                                   |
| void doOrThrow(Consumer&lt;O&gt;)                           | Runs task if the referencing value is not null, or throws an error                                                                                                       |
| void checkNotNull()                                         | Checks and throws an error if the referencing value is null                                                                                                              |


Animation Accessor

| Methods                                                 | Usage                                                     |
|---------------------------------------------------------|-----------------------------------------------------------|
| int id()                                                | Returns the numerical id of the referencing animation     |
| boolean idBetween(AnimationAccessor, AnimationAccessor) | Checks if the animation id is between each parameter's id |

### Creating Built-in Asset Accessors
You'd create accessors for the assets in your mod. For these types of assets, you can create static final accessors that ensure the assets are present in the resource manager.

For Meshes,
```java
public static final MeshAccessor<MyMesh> MY_MESH = MeshAccessor.create(MyMod.MODID, "entity/my_entity", (jsonModelLoader) -> jsonModelLoader.loadSkinnedMesh(MyMesh::new));
```

For Armatures,
```java
public static final ArmatureAccessor<MyArmature> MY_ARMATURE = ArmatureAccessor.create(MyMod.MODID, "entity/my_entity", MyArmature::new);
```
*Beware that a mesh and an armature are usually stored in one model file. You need to match their path for a single model file*

For Animations,
```java
public static AnimationAccessor<StaticAnimation> MY_ENTITY_IDLE_ANIMATION;

@SubscribeEvent
public static void registerAnimations(AnimationRegistryEvent event) {
  event.newBuilder(MyMod.MODID, MyAnimations::build);
}

public static void build(AnimationBuilder builder) {
  MY_ENTITY_IDLE_ANIMATION = builder.nextAccessor("my_entity/idle", accessor -> new StaticAnimation(true, accessor, Armatures.MY_ENTITY));
}
```
*For Animation accessors, the final modifier is not valid*

### Creating dynamic Asset Accessor
You may need accessors for the resources whose presence is unclear, especially when you have a soft dependency. Under these circumstances, you can create an accessor and check if the resource actually exists.

For Meshes,
```java
public static final AssetAccessor<MyMesh> MAY_EXIST_MESH = Meshes.getOrCreate(ResourceLocation.fromNamespaceAndPath(MyMod.MODID, "entity/may_exist_entity"), jsonModelLoader -> jsonModelLoader.loadSkinnedMesh(MyMesh::new));

// Check if the resource exists
if (MAY_EXIST_MESH.isPresent()) {
  // do task when the resource exists
}

// Or use ifPresent
MAY_EXIST_MESH.ifPresent(theMesh -> {
  // do task when the resource exists
});
```

For Armatures,
```java
public static final AssetAccessor<MyArmature> MAY_EXIST_ARMATURE = Armatures.getOrCreate(ResourceLocation.fromNamespaceAndPath(MyMod.MODID, "entity/may_exist_entity"), MyArmature::new);

// Check if the resource exists
if (MAY_EXIST_ARMATURE.isPresent()) {
  // do task when the resource exists
}

// Or use ifPresent
MAY_EXIST_ARMATURE.ifPresent(theArmature -> {
  // do task when the resource exists
});
```

For Animations,
```java
public static final AssetAccessor<StaticAnimation> MAY_EXIST_ANIMATION = .byKey(ResourceLocation.fromNamespaceAndPath(EpicFightMod.MODID, "my_entity/may_exist_idle_animation"));

// Check if the resource exists
if (MAY_EXIST_ANIMATION.isPresent()) {
  // do task when the resource exists
}

// Or use ifPresent
MAY_EXIST_ANIMATION.ifPresent(theAnimation -> {
  // do task when the resource exists
});
```

***
## Extensible Enum
Extensible Enum is an Enum type that implements `ExtensibleEnum` (`ExtendableEnum` in 1.20.1), which has scalability in the intermod area. Developers can add more enums as their requirements or even create a new Extensible enum to open their API to others.

### Subtypes

| Type                    | Explanation                                                |
|-------------------------|------------------------------------------------------------|
| EntityPairngPakcetTypes | For the pairing packets of entity states                   |
| Faction                 | For the entity's faction                                   |
| LivingMotion            | For the Cycling animations depending on the player's state |
| SkillCategory           | For the skills' categorization                             |
| SkillSlot               | For the skill containers                                   |
| Style                   | For the moveset of weapons depending on the owner's state  |
| WeaponCategory          | For the weapons' categorization                            |

### Expanding Epic Fight extensible enums
First, you'll create an Enum class for custom enums
```java
public enum MyLivingMotions implements LivingMotion {
	CRAWL, SKYDIVE, BULLET_RUN;
	
	final int id;
	
	MyLivingMotions() {
		// Ids are automatically assigned by Enum Manager
		this.id = LivingMotion.ENUM_MANAGER.assign(this);
	}
	
	@Override
	public int universalOrdinal() {
		// Return universal id for all enums extending LivingMotion
		return this.id;
	}
}
```
Then, you register your enum classes on the mod construction stage.
```java
@Mod(MyMod.MODID)
public class MyMod {
  ...
  public MyMod(FMLJavaModLoadingContext context) {
    ...
		LivingMotion.ENUM_MANAGER.registerEnumCls(MyMod.MODID, MyLivingMotions.class);
    ...
  }
}
```
It's done. Now you can use `MyLivingMotions` without any conflicts.

### Adding custom extensible enums
If you want to create an extensible enum for other developers, you'd follow these steps.
First, create an interface with `EnumManager` in it.
```java
public interface MyExtensibleEnum extends ExtendableEnum {
	ExtendableEnumManager<MyExtensibleEnum> ENUM_MANAGER = new ExtendableEnumManager<> ("my_extensible_enum");
}
```
Then follow the first step introduced in **Expanding Epic Fight extensible enums** to create enum objects.
```java
public enum MyExtensibleEnums implements MyExtensibleEnum {
	ENUM1, ENUM2, ENUM3;
	
	final int id;
	
	MyExtensibleEnums() {
		this.id = MyExtensibleEnum.ENUM_MANAGER.assign(this);
	}
	
	@Override
	public int universalOrdinal() {
		return this.id;
	}
}
```
Lastly, you register the enum class and enum manager in the mod loading stage.
```java
@Mod(MyMod.MODID)
public class MyMod {
  ...
  public MyMod(FMLJavaModLoadingContext context) {
    ...
		MyExtensibleEnum.ENUM_MANAGER.registerEnumCls(MyMod.MODID, MyExtensibleEnums.class);
    ...
  }
  '''
  private void constructMod(final FMLConstructModEvent event) {
      '''
      // This loads all enum classes specified by "registerEnumCls"
    	event.enqueueWork(MyExtensibleEnum.ENUM_MANAGER::loadEnum);
      '''
  }
  '''
}
```