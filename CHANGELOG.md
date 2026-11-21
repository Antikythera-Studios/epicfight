# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).
## [21.17.4] - 2026-06-09
### Additions
- Added 4 new enchantments that improve the player's combat experience.
  - Lightweight: Reduces the weight of a player's armor.
  - Unshakable: Increases the stun armor
  - Impactful: Increases the impact of the weapon.
  - Armor Piercing: Increases the Armor Negation of a weapon. Similar to Breach but is non-exclusive to the Mace.
### Fixes
- Fixed the client-server desync regarding Guard and HoldableSkill
## [21.17.3] - 2026-05-14
### Additions
- Added Item Specific Overrides to Builder to allow any explicit tier configurations.
- Added Simply Tooltips Compatibility with a plethora of Epic Fight's own features.
### Changes
- Used `@WrapOperation` instead of `@Redirect` to improve compatibility with other mods' mixins.
- Updated GeckoLib compatibility 
### Deprecations
- Deprecated `setWeaponData()` as `modifyBuilder()` made the former method redundant.
### Fixes
- More inheritance issues regarding `WeaponCapability`
- Custom Data in Item Presets was not working due to a logic error.
- Skills not using `isWeaponCategory()`

## [21.17.2] - 2026-05-12
### Additions
- Added builder modifiers to `WeaponModifier` just a small addition that gives more power to addon developers.
### Hotfixes
- Fixed Issue regarding dual wielding not working. Caused by a weapon category match being that just passing the entire weapon cap instead of just the category.
- Fixed weapon category and collider inheritance by setting them to null and only adding the first categories if it stayed null from inheritance.

## [21.17.1] - 2026-04-29
### Changes
**ExCap**

---
 - Introduced the Hybrid Registry System
   - The hybrid Registry system uses the DeferredRegister to register new data statically, while also allowing Datapacks to override it down the line.
     - DeferredRegister registers it upon static compile-time while datapacks then override it upon a world load or running /reload
 - Added Modifiers – A replacement of ExCap's system for Dynamic Overrides
 - Existing ExCap registration for Core Epic Fight content has been turned from event driven into Registry Driven via DeferredRegister.
 - Added the option to register a Weapon Capability monolithically via just the Builder. Modularity is no longer forced.
 - Implemented Inheiritance for `WeaponCapability.Builder` allowing even faster prototyping and building of new weapons.
 - Implemented `CustomData` a new way for addons to extend on weapon data using datapacks and code. All is done in DeferredRegister. Either as weapon data or as moveset data. Java can be used to extend both Weapon and Moveset data. Datapacks and JSON can only be used to extend Weapon data.
 - **Experimental Change:** Introduced `WeaponCategory` inheiritance which uses multiple inheiritance for its weapon categories.
 - Mass deprecation of as many old `WeaponCapabilityPresets` weapons and Old ExCap methods that were deemed extremely difficult to work with and extremely tedious to use.

---

- Added Hand switching item animation
- Added max passive skills gamerule. `/gamerule maxPassiveSkills <value>`
- Added animation mirroring for combo weapons and living animations.
- Made it so roll/dodge skills cannot be performed midair.
- Made it so you cannot spam dash attacks.
- Tweaks to the Skill equipping window GUI. No longer closes when equipping a skill and some other tweaks here and there.

### Fixes
- Fixed biped entities mounted atop horses & other entities rendering all weird. The skeletons on top of spiders (spider jockeys) should be more visually pleasing. Same goes for the Chicken jockey.
- Fixed the tools being misplaced on skeletons when they sit/mount on other entities
- Fixed chicken jockey behavior, now it attacks the player and targets it properly.


## [21.16.4] - 2026-04-21

### Fixed
- Fixed Emote data pack crashes with custom animations in dedicated server [#2534](https://github.com/Antikythera-Studios/epicfight/issues/2534)
- Fixed a crash when opening datapack editor [#2546](https://github.com/Antikythera-Studios/epicfight/issues/2546)

## [21.16.3] - 2026-04-13

### Added
- Added a Tag system for item capability data pack [#2526](https://github.com/Antikythera-Studios/epicfight/pull/2526)
- Added Wildfire's Female Gender mod compatibility

### Fixed
- Fixed the player head jittering in TPS mode while blocking & looking at a nearby block at the same time [#2524](https://github.com/Antikythera-Studios/epicfight/issues/2524)
- Fixed Skill Books not dropped by mobs [#2528](https://github.com/Antikythera-Studios/epicfight/issues/2528)

## [21.16.2] - 2026-04-05

### Fixed
- Fixed the `EventHook` overwriting event listeners with the same priority [#2511](https://github.com/Antikythera-Studios/epicfight/pull/2511)
- Fixed the damage still being applied after cancelling `TAKE_DAMAGE_INCOME` event
- Fixed the EF featured attributes (stamina, impact, armor negation, ...) base values being reset on rejoining a world [#2507](https://github.com/Antikythera-Studios/epicfight/issues/2507)
- Fixed an issue where shields couldn't block attacks [#2519](https://github.com/Antikythera-Studios/epicfight/issues/2519)
- Fixed Ender Dragon can't be damaged by melee attacks even `allowVanillaMelee` gamerule is set to true [#2458](https://github.com/Antikythera-Studios/epicfight/issues/2458)

### Changed
- Changed the Phantom Ascent not to play damage overlay when jumping and landing at the same height [#2509](https://github.com/Antikythera-Studios/epicfight/issues/2509)

## [21.16.1] - 2026-03-29

### Added
- ExCap (an Epic Fight addon for dynamic attack moveset) is now built-in
- Added MacOS support to access Patreon capes

## [21.15.8] - 2026-03-21

### Fixed
- Fixed the players can't interact with non attackable entities in TPS mode [#2504](https://github.com/Epic-Fight/epicfight/issues/2504)
- Fixed a crash when datapack item capability has no combo animation [#2496](https://github.com/Epic-Fight/epicfight/issues/2496)
- Fixed the Netherite weapons not enchantable [#2501](https://github.com/Epic-Fight/epicfight/issues/2501)

### Changed
- Changed the default key of **Lock-on Free Shift** to mouse middle button

## [21.15.7] - 2026-03-09

### Fixed
- Fixed a crash caused by incompatible hit result type. [#2474](https://github.com/Epic-Fight/epicfight/issues/2474)
- Fixed downed players attacking or casting skills (player revive) [#2464](https://github.com/Epic-Fight/epicfight/issues/2464)
- Fixed the Stun Armor and Weight attributes from armors not applied for players [#2479](https://github.com/Epic-Fight/epicfight/issues/2479)
- Fixed the damage modifiers not working [#2488](https://github.com/Epic-Fight/epicfight/issues/2488)

## [21.15.6] - 2026-02-16

### Added
- Added Linux support to access Patreon capes [#2462](https://github.com/Epic-Fight/epicfight/pull/2462)
- Added a (loc, rot, sca) based transformation import for joint local transforms [#2465](https://github.com/Epic-Fight/epicfight/pull/2465)

### Fixed
- Fixed a crash when casting Wrathful Lighting to Llama [#2459](https://github.com/Epic-Fight/epicfight/issues/2459)

## [21.15.5] - 2026-02-13

### Added
- Added a config option to disable ground slam effects

### Fixed
- Fixed a crash when a nearby player casts Demolition Leap and Meteor Slam in a dedicated server
- Fixed modded HUDs not rendering

## [21.15.4] - 2026-02-06

### Fixed

- Fixed a crash when changing hand items from entities by commands [#2437](https://github.com/Epic-Fight/epicfight/issues/2437)
- Fixed a crash when removing an empty row in weapon attribute screen [#2439](https://github.com/Epic-Fight/epicfight/issues/2439)
- Fixed the overlapped tab buttons are created in the datapack editor when resizing the screen [#2443](https://github.com/Epic-Fight/epicfight/issues/2443)
- Fixed the player still moves forward even when the 'stiffComboAttack' gamerule is set to false [#2444](https://github.com/Epic-Fight/epicfight/issues/2444)
- Fixed an error due to Ender Dragon spawning [#2445](https://github.com/Epic-Fight/epicfight/issues/2445)
- Fixed a crash when killing Ender Dragon [#2446](https://github.com/Epic-Fight/epicfight/issues/2446)
- Fixed an issue where you can't type namespace separator ":" for weapon type entries in datapack editor
- Fixed a crash from wrong animation formatting [#2441](https://github.com/Epic-Fight/epicfight/issues/2441)

### Added

- Made the Datapack Editor detect the classname change from 1.20.1 packs
  - BasicAttackAnimation(1.20.1) -> ComboAttackAnimation(1.21.1)

## [21.15.3] - 2026-01-31

### Fixed

- Fixed Phantom Ascent being disabled if a player lands without taking damage [#2421](https://github.com/Epic-Fight/epicfight/issues/2421)
- Fixed casting Meteor slam immediately crashes remote players and kicks them from a dedicated server [#2422](https://github.com/Epic-Fight/epicfight/issues/2422)
- Fixed a wither animation crash [#2428](https://github.com/Epic-Fight/epicfight/issues/2428)
- Fixed a crash by null sound object [#2432](https://github.com/Epic-Fight/epicfight/issues/2432)
- Fixed a crash when initializing item renderers. [#2430](https://github.com/Epic-Fight/epicfight/issues/2430)
- Fixed the 'stiffComboAttack' game rule breaking player motions and trail effects [#2369](https://github.com/Epic-Fight/epicfight/issues/2369)
- Fixed a crash when loading non-existent animations in datapack editor [#2317](https://github.com/Epic-Fight/epicfight/issues/2317)
- Relieved the crosshair hit entity detection [#2431](https://github.com/Epic-Fight/epicfight/issues/2431)
- Fixed an infinite combo attacks looping for some attack movesets [#2429](https://github.com/Epic-Fight/epicfight/issues/2429)
- Fixed a crash when trying to render a preview model's trail particle in Datapack Editor [#2452](https://github.com/Epic-Fight/epicfight/issues/2452)

## [21.15.2] - 2026-01-21

### Fixed

- Fixed players can't switch from vanilla to Epic Fight mode
- Fixed a crash when Geckolib installed

## [21.15.1] - 2026-01-20

### Added

- Emote System added
    - Holding emote wheel key (default 'Y') will open Emote screen, where you can select a emote to play
    - Emote system is data-driven. Meaning that users can add custom emotes
    - Similar to other datapack systems, it requires each client to load emote animation provided as resource pack
- Persistent Mapped Buffer (by @dfdyz)
    - Boosts frame rate by removing the uploading process of buffers to shader
    - Requires OpenGL version over 4.6
- Added "swing_sound", "hit_sound", and "hit_particle" that is configurable in ItemCapability
    - This is not supported by data pack editor, as we're going to refactor the data pack screen

### Changed

- Configuration screen reboot
    - The design of configuration screen is clean and modernized
    - Splitted configurations with more specific categories, Ui, Graphics, Model, Controls, and Camera
    - Some configurations' names have been changed to more standard and in common use terminologies
    - Added a side bar to switch Settings, Data Pack Editor, Cosemtics screen

### Fixed
- Fixed the player can't play the same attack animation, until it fully ends in the server side

### For Devs

- Renamed the JAR file to be consistent with the Modrinth project slug URL, to support automatic sources download.
- Added `EntityPatchRegistryEvent#registerEntityPatch` and `EntityPatchRegistryEvent#registerEntityPatchUnsafe`, which
  are more type-safe and recommended over `EntityPatchRegistryEvent#getTypeEntry`. 
- Deprecated `EpicFight.format` since we added `LangKeys` for words translation
- ComboCounterHandler
    - A more modularized and parameter sensitive way to handle combo counter of `ComboAttacks`
    - CapabilityItem.shouldCancelCombo is now deprecated, as more parameter sensitive version introduced

## [21.14.4] - 2025-12-17

### Fixed

- Fixed broken Epic Fight entity patches due to `Vindicator` being cast to `Spider`.

### For Devs

- Deprecated `EpicFightMod.rl` and added `EpicFightMod.identifier` since
  [Mojang renamed `ResourceLocation` to
  `Identifier` in 1.21.11](https://neoforged.net/news/21.11release/#renaming-of-resourcelocation-to-identifier).
- Removed `MixinControlEngine` to avoid future breaking changes

## [21.14.3] - 2025-12-11

### Fixed

- Fixed datapack animations not loading properly

### Changed

- Changed the asset license to All Rights Reserved, whereas the source code license keeps GNU GPLv3.
    - This means, we still allow forks of our project, but they're required to replace assets into custom and
      their original-made ones to redistribute.
    - You're still allowed to use assets via datapack editor or addons. Nevertheless, we can claim copyright
      of our assets if we find some abuse, or your behaviors that don't respect our efforts on it.

## [21.14.2] - 2025-12-10

### Fixed

- Fixed the target indicator invisible for non Epic Fight patched entities
- Fixed the lock-on target not being synced to the server when using mouse snap to change the target
- Fixed the player being dark in inventory screen
- Fixed the camera jittering in TPS mode

### Changed

- Now Lock-on automatically searches a new target if there is no currently focusing entity
- Now TPS mode applies 8 directional movement to the player

## [21.14.1] - 2025-12-07

### Added

- Added a client config to allow skipping the third-person front perspective when
  toggling the camera perspective (i.e., F5).
  [#2205](https://github.com/Epic-Fight/epicfight/issues/2205)
- Improved arrow key navigation (`↑`, `↓`, `→`, `←`) in the skill editor screen, including proper scrolling support. [#2203](https://github.com/Epic-Fight/epicfight/issues/2203)
- **[Controlify]** Added native controller support for the skill editor screen and disabled virtual mouse behavior.
- An option that you can always activate the TPS perspective, which was only activated when aiming with ranged weapons.
- An option screen where you can set up the camera position in TPS perspective
- An auto-tracking functionality that aligns the player's look to the crosshair when aiming and striking entities.
- A new keybind that moves the camera freely while locking on any entity to search another target
- A lock-on snapping feature that cycles lock-on entities in the screen by snapping mouse left or right
- An auto-targeting functionality that searches a next target when the current lock-on entity is dead
- An option to toggle lock-on snapping and auto target
- An option to set the maximum distance that the player can focus on entities
- See the devlog [here](https://www.patreon.com/posts/tps-camera-and-141028682)
- Epic Fight's TPS perspective will be automatically disabled when a conflicting mod, such
  as [Shoulder Surfing Reloaded](https://modrinth.com/mod/shoulder-surfing-reloaded)
  or [Better Third Person](https://modrinth.com/mod/better-third-person), is detected to prevent issues.
- Explicit Shoulder Surfing compatibility with the new
  enhanced lock-on (credit [Exopandora](https://github.com/Exopandora)).
  [#2258](https://github.com/Epic-Fight/epicfight/issues/2258)

### Fixed

- Fixed a bug that allowed the player to replace the current skill slot even during cooldown.
- **Fixed:** The **Shoulder Surfing compatibility module** was not being registered,
  causing issues with its intended functionality.
- Fixed the mining crosshair not to show in vanilla mode
- Fixed the massive memory consume on loading the game caused by animation loads
- Disabled the `AzureLib` and `AzureLibArmor` compatibility modules for version `3.X.X` and newer as a workaround to
  address breaking changes and prevent crashes.
- Fixed the player kicked from a dedicated server when other players are equipping Adaptive Skin skill

### For Devs

- Rename the experimental enum `EpicFightInputActions` to `EpicFightInputAction` to follow Java naming
  conventions. [#2194](https://github.com/Epic-Fight/epicfight/issues/2194)
- Removed AirSlash and its related fields (SkillCategory, SkillSlot) to merge air slash and combo attacks as one skill
- Updated the experimental Epic Fight's input API to support using custom input actions that are not a
  `EpicFightInputAction`.
- Extracted vanilla input actions from `EpicFightInputAction` into `MinecraftInputAction`.
  [#2194](https://github.com/Epic-Fight/epicfight/issues/2194)
  [#2194](https://github.com/Epic-Fight/epicfight/issues/2194)
- New API feature: Hooks
    - Replace the mod-loader event system into Epic Fight API, as we're planning to support multi-loader developer environment
    - The feature is still WIP, supporting only events for EpicFightCameraAPI
    - We will eventually replace all Forge/NeoForge events owned by Epic Fight into Hooks
- Added API JAR file, which includes classes under `yesman/epicfight/api/**` only, to allow consumers to compile against
  Epic Fight public API only.
    - **Note:** Keep in mind that Epic Fight public APIs are still being stabilized, and breaking changes may occur. 

## [21.13.5] - 2025-11-12

### Fixed

- Fixed a regression where the Phantom Ascent skill was triggered when
  pressing the jump key while any screen was open (e.g., inventory, chat).
  [#2170](https://github.com/Epic-Fight/epicfight/issues/2170)
- Fixed a regression where the weapon's innate skill tooltip did not trigger.
  [#2198](https://github.com/Epic-Fight/epicfight/pull/2197)
- Fixed a crash when right click some blocks from Supplementaries
  [#2187](https://github.com/Epic-Fight/epicfight/issues/2187)
- Fixed a cosmetic configuration button always inactivated
- Fixed an unintended mechanism where you weren't able to attack in Epic Fight
  mode while Preference Work is set to Switch Mode, so it works in the same way
  as Item Auto Switching option, an old config where automatically sets player
  mode depending on the item that player holds

### Changed

- Refactored the code to eliminate unnecessary native GLFW calls,
  optimizing whether key down checks are performed per tick, by adapting
  Minecraft vanilla `KeyMapping`, which may also potentially fix other compatibility issues with other mods.
- Avoid registering mixins for non-existing third-party mods to avoid spamming the console log and prevent unnecessary operations.

### For Devs
- Adopted KeyConflictContext for each keybind as documented by [Neoforge](https://docs.neoforged.net/docs/misc/keymappings/#ikeyconflictcontext) to avoid potential problem from inconsistency
- Made GUARD and DODGE CombatKeyMapping, to activate only in Epic Fight mode
- Adjusted `MixinWitherBoss` to follow best practices and prevent potential conflicts with other mods in production environments.

## [21.13.4] - 2025-11-04

### Changed
- Updated the default config to disable the mine block highlight guide.

### Fixed
- Fixed a crash when joining a world with BadOptimizations installed. [BadOptimizations#108](https://github.com/imthosea/BadOptimizations/issues/108), [#2160](https://github.com/Epic-Fight/epicfight/issues/2160).
- Fixed a crash when joining a world with [Ecliptic Seasons](https://www.curseforge.com/minecraft/mc-mods/ecliptic-seasons) installed.

## [21.13.3] - 2025-11-01

### Added
- Added data-driven button guides for improved flexibility and customization.

### Changed
- All changes from Epic Fight 20.13.3
- Refined the dodge guide and introduced a new guide for weapon innate skill tooltips.

### Fixed
- Fixed the camera not switching when aiming ranged weapons
- Fixed an issue where sneaking movement speed was incorrect when using a controller.
- Resolved a crash that occurred when loading on dedicated servers.

## [21.13.1] - 2025-10-21

### Fixed
- Ported from Epic Fight 20.13.1
- Fixed the crash when equipping Geckolib armors
- Fixed the armor's texture to follow the render property first
- Restored Skin layer 3d compatibility
- Fixed the first-person player model transform broken when using a shaderpack
