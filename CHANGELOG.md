# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [20.15.0] - 2026-10-26
### Added
- Armor rebaker.

## [20.14.17] - 2026-04-29
### Removed
- Removed [ExCap](https://www.curseforge.com/minecraft/mc-mods/ex-cap). Delegating the change to be exclusive to 1.21.1

## [20.14.16.1] - 2026-04-16
### Added
- Revelation for ExCap making it complete with 1.21.1.
### Fixed
- Shields not rendering offhand alone.
- Weapon Passive Skill still using the legacy method.
- Mount Attacks still using the legacy method.

## [20.14.16] - 2026-04-13

### Added
- Added [ExCap](https://www.curseforge.com/minecraft/mc-mods/ex-cap) as built-in into Epic Fight
- Added Wildfire's Female Gender mod compatibility

## [20.14.15.1] - 2026-04-09

### Fixed
- [Hotfix] Fixed TPS correction is also triggered in first person too

## [20.14.15] - 2026-04-09
### Added
- Added a Tag system for item capability data pack [#2525](https://github.com/Antikythera-Studios/epicfight/pull/2525)

### Fixed
- Fixed the player head jittering in TPS mode while blocking & looking at a nearby block at the same time [#2524](https://github.com/Antikythera-Studios/epicfight/issues/2524)

## [20.14.14] - 2026-04-05

### Fixed
- Fixed the damage still being applied after cancelling `TAKE_DAMAGE_EVENT_ATTACK` event
- Fixed an issue where shields couldn't block attacks [#2519](https://github.com/Antikythera-Studios/epicfight/issues/2519)
- Fixed Ender Dragon can't be damaged by melee attacks even `allowVanillaMelee` gamerule is set to true [#2458](https://github.com/Antikythera-Studios/epicfight/issues/2458)

### Changed 
- Changed the Phantom Ascent not to play damage overlay when jumping and landing at the same height [#2509](https://github.com/Antikythera-Studios/epicfight/issues/2509)

## [20.14.13] - 2026-03-29
- Added MacOS support to access Patreon capes

## [20.14.12] - 2026-03-21

### Fixed
- Fixed the players can't interact with non attackable entities in TPS mode
- Fixed a crash when datapack item capability has no combo animation [#2496](https://github.com/Epic-Fight/epicfight/issues/2496)

### Changed
- Changed the default key of **Lock-on Free Shift** to mouse middle button

## [20.14.11] - 2026-03-09

### Fixed
- Fixed a crash caused by incompatible hit result type. [#2474](https://github.com/Epic-Fight/epicfight/issues/2474)
- Fixed downed players attacking or casting skills (player revive) [#2464](https://github.com/Epic-Fight/epicfight/issues/2464)
- Fixed the Stun Armor and Weight attributes from armors not applied for players [#2479](https://github.com/Epic-Fight/epicfight/issues/2479)
- Fixed Mixin injection failure when spawning Piglin [#2491](https://github.com/Epic-Fight/epicfight/issues/2491)

## [20.14.10] - 2026-02-16

### Added
- Added Linux support to access Patreon capes [#2462](https://github.com/Epic-Fight/epicfight/pull/2462)
- Added a (loc, rot, sca) based transformation import for joint local transforms [#2465](https://github.com/Epic-Fight/epicfight/pull/2465)

### Fixed
- Fixed a crash when casting Wrathful Lighting to Llama [#2459](https://github.com/Epic-Fight/epicfight/issues/2459)

## [20.14.9] - 2026-02-13

### Added
- Added a config option to disable ground slam effects

### Fixed
- Fixed a crash when a nearby player casts Demolition Leap and Meteor Slam in a dedicated server
- Fixed modded HUDs not rendering

## [20.14.8] - 2026-02-06

### Fixed

- Fixed the player still moves forward even when the 'stiffComboAttack' gamerule is set to false [#2444](https://github.com/Epic-Fight/epicfight/issues/2444)
- Fixed a crash when removing an empty row in weapon attribute screen [#2439](https://github.com/Epic-Fight/epicfight/issues/2439)
- Fixed a crash from wrong animation formatting [#2441](https://github.com/Epic-Fight/epicfight/issues/2441)

## [20.14.7] - 2026-01-31

### Fixed

- Fixed Phantom Ascent being disabled if a player lands without taking damage [#2421](https://github.com/Epic-Fight/epicfight/issues/2421)
- Fixed casting Meteor slam immediately crashes remote players and kicks them from a dedicated server [#2422](https://github.com/Epic-Fight/epicfight/issues/2422)
- Fixed a wither animation crash [#2428](https://github.com/Epic-Fight/epicfight/issues/2428)
- Fixed a crash by null sound object [#2432](https://github.com/Epic-Fight/epicfight/issues/2432)
- Fixed a crash when initializing item renderers [#2430](https://github.com/Epic-Fight/epicfight/issues/2430)
- Fixed the 'stiffComboAttack' game rule breaking player motions and trail effects [#2369](https://github.com/Epic-Fight/epicfight/issues/2369)
- Fixed a crash when loading non-existent animations in datapack editor [#2317](https://github.com/Epic-Fight/epicfight/issues/2317)
- Relieved the crosshair hit entity detection [#2431](https://github.com/Epic-Fight/epicfight/issues/2431)
- Fixed an infinite combo attacks looping for some attack movesets [#2429](https://github.com/Epic-Fight/epicfight/issues/2429)

## [20.14.6] - 2026-01-17

### Fixed
- Fixed Forbidden Strength triggered via basic attacks
- Fixed an issue where you can cast skills with holding TaCZ firearms #2413
- Fixed skills not casting with exact same amount of stamina required (reported by @Reascer)
- Fixed a crash: https://mclo.gs/nehnpG3
- Fixed a crash when hover items to show its tooltips #2406
- Fixed a crash when Fabric Networking API is installed through multi-platform mods #2399

### Added
- A game rule that blocked vanilla melee attacks is now rolled back with a new name, "allowVanillaMelee"
- A maximum damage limitation is applied to Eviscerate(dagger skill, default is 20) #2364

### Changed
- Now Epic Fight's action animations have higher priority than Player Animator's animations

### For Devs
- Now `BuildCameraTransform.Post` is applied to TPS mode too (https://github.com/Epic-Fight/epicfight/commit/e0f3daab8d61b5bc8a4c14e3051b34c3ff599080)

## [20.14.5] - 2026-01-11

### Fixed
- Fixed the player can't play the same attack animation, until it fully ends in the server side

### For Devs
- ComboCounterHandler
    - A more modularized and parameter sensitive way to handle combo counter of `BasicAttack`
    - CapabilityItem.shouldCancelCombo is now deprecated, as more parameter sensitive version introduced

## [20.14.4] - 2025-12-17

### Changed

- Internal changes for better code quality

### For Devs

- Deprecated `EpicFightMod.rl` and added `EpicFightMod.identifier` since
  [Mojang renamed `ResourceLocation` to
  `Identifier` in 1.21.11](https://neoforged.net/news/21.11release/#renaming-of-resourcelocation-to-identifier).

## [20.14.3] - 2025-12-11

### Fixed

- Fixed datapack animations not loading properly

### Changed

- Changed the asset license to All Rights Reserved, whereas the source code license keeps GNU GPLv3.
    - This means, we still allow forks of our project, but they're required to replace assets into custom and
      their original-made ones to redistribute.
    - You're still allowed to use assets via datapack editor or addons. Nevertheless, we can claim copyright
      of our assets if we find some abuse, or your behaviors that don't respect our efforts on it.

## [20.14.2] - 2025-12-10

### Fixed

- Fixed the target indicator invisible for non Epic Fight patched entities
- Fixed the lock-on target not being synced to the server when using mouse snap to change the target

### Changed

- Now Lock-on automatically searches a new target if there is no currently focusing entity
- Now TPS mode applies 8 directional movement to the player

## [20.14.1] - 2025-12-07

### Added

- Added a client config to allow skipping the third-person front perspective when
  toggling the camera perspective (i.e., F5).
  [#2205](https://github.com/Epic-Fight/epicfight/issues/2205)
- Improved arrow key navigation (`↑`, `↓`, `→`, `←`) in the skill editor screen, including proper scrolling support. [#2203](https://github.com/Epic-Fight/epicfight/issues/2203)
- **[Controlify]** Added native controller support for the skill editor screen and disabled virtual mouse behavior.
- An option that you can always activate the TPS perspective, which was only activated when aiming with ranged weapons.
- An option screen where you can set up the camera position in TPS perspective
- An auto-tracking functionality that aligns the player's look to the crosshair when aiming and striking entities.
- A new keybind that moves camera freely while locking on any entity to search another target
- A lock-on snapping feature that cycles lock-on entities in the screen by snapping mouse left or right
- An auto-targeting functionality that searches a next target when the current lock-on entity is dead
- An option to toggle lock-on snapping and auto target
- An option to set the maximum distance that the player can focus entities
- See the devlog [here](https://www.patreon.com/posts/tps-camera-and-141028682)
- Epic Fight's TPS perspective will be automatically disabled when a conflicting mod, such
  as [Shoulder Surfing Reloaded](https://modrinth.com/mod/shoulder-surfing-reloaded)
  or [Better Third Person](https://modrinth.com/mod/better-third-person), is detected to prevent issues.
- Explicit Shoulder Surfing compatibility with the new enhanced lock-on (credit [Exopandora](https://github.com/Exopandora)).

### Fixed

- Fixed a bug that allowed the player to replace the current skill slot even during cooldown.
- Fixed the mining crosshair not to show in vanilla mode
- Fixed the massive memory consume on loading the game caused by animation loads

### For Devs

- New API feature: Events
    - Replaces the mod-loader event system into Epic Fight API, as we planning to support multi-loader developer environment
    - The feautre is still WIP, supporting only events for EpicFightCameraAPI
    - We will eventually replace all Forge/Neoforge events owned by Epic Fight into Events
- Rename the experimental enum `EpicFightInputActions` to `EpicFightInputAction` to follow Java naming
  conventions. [#2194](https://github.com/Epic-Fight/epicfight/issues/2194)
- Removed AirAttack and its related fields (SkillCategory, SkillSlot) to merge air slash and combo attacks as one skill
- Updated the experimental Epic Fight's input API to support using custom input actions that are not a
  `EpicFightInputAction`.
  [#2194](https://github.com/Epic-Fight/epicfight/issues/2194)
- Deprecated `ClientEngine#isBattleMode` and added `ClientEngine#isEpicFightMode` for a smoother migration when porting
  from MC 1.20.1 to 1.21.1
- Added API JAR file, which includes classes under `yesman/epicfight/api/**` only, to allow consumers to compile against
  Epic Fight public API only.
    - **Note:** Keep in mind that Epic Fight public APIs are still being stabilized, and breaking changes may occur.

## [20.13.6] - 2025-11-12

### Fixed

- Fixed a regression where the weapon’s innate skill tooltip did not trigger.
  [#2198](https://github.com/Epic-Fight/epicfight/pull/2198)

## [20.13.5] - 2025-11-11

### Fixed

- Fixed a regression where the Phantom Ascent skill was triggered when
  pressing the jump key while any screen was open (e.g., inventory, chat).
  [#2170](https://github.com/Epic-Fight/epicfight/issues/2170)
- Fixed a crash when right click some blocks from Supplementaries
  [#2187](https://github.com/Epic-Fight/epicfight/issues/2187)
- Fixed patron capes always being default
- Fixed an unintended mechanism where you weren't able to attack in Epic Fight
  mode while Preference Work is set to Switch Mode, so it works in the same way
  as Item Auto Switching option, an old config where automatically sets player
  mode depending on the item that player holds

### Changed

- Refactored the code to eliminate unnecessary native GLFW calls,
  optimizing whether key down checks are performed per tick, by adapting
  Minecraft vanilla `KeyMapping`, which may also potentially fix other compatibility issues with other mods.

### Added

- Built-in Controlify integration for controller support.
  No need to install
  [Epic Fight: Controlify](https://www.curseforge.com/minecraft/mc-mods/epic-fight-controlify) anymore.
  Install only
  [Controlify: Forgified](https://www.curseforge.com/minecraft/mc-mods/controlify-forgified-unofficial) on 1.20.1

### For Devs

- Adjusted `MixinWitherBoss` to follow best practices and prevent potential conflicts with other mods in production
  environments.

## [20.13.4] - 2025-11-04

### Changed
- Updated the default config to disable the mine block highlight guide.

### Fixed
- Fixed a crash when joining a world with BadOptimizations installed. [BadOptimizations#108](https://github.com/imthosea/BadOptimizations/issues/108), [#2160](https://github.com/Epic-Fight/epicfight/issues/2160).
- Fixed a crash when joining a world with [Ecliptic Seasons](https://www.curseforge.com/minecraft/mc-mods/ecliptic-seasons) installed.

## [20.13.3] - 2025-11-01

### Bugfix
- Fixed Dedicated Server Crash
- Fixed Epic Fight potentially breaking other controller mods
- Fixed the player can't take blocking when holding weapons from Simplyswords
- Fixed the player's blocking and digging animations not removing under certain conditions (usually when it's combined with dodge skills)
- Blocked the player editing signs when 'resolve_key_conflicts' is set to 'interaction'
- Optimized texture files so that save 23% of size from the original

### For Devs
- Refactored the input system to be less dependent on vanilla Minecraft, allowing other controller mods to provide Epic Fight integration.

## [20.13.2] - 2025-10-24

### Bugfix
- Fixed the 'resolve_key_conflicts' option not to be applied when the player is vanilla mode
- Fixed the 'resolve_key_conflicts' messing up the door state
- Fixed the Ender dragon can't hurt the player

### Configuration
- Added a new config option that you can disable Minecraft model while in vanilla mode (Same as old 'filter animation' option)
- Added a new config option that determines how 'item_preference' works
> - Adaptive: same as current work
> - Switch mode: like the old Epic Fight's 'auto_switching' items, it switches the player mode when the player changes a main hand item, forcing the next behavior depending on the player mode

### Changes
- Made the stun shield persistent

## [20.13.1] - 2025-10-20

### Bugfix
- Fixed the players unable to turn camera in first-person on ladders when y rotation is 180 (or -180)
- Fixed the Technician not rewarding stamina

### Configuration
- Expanded the mining block guide option to configure both crosshair and block highlight overlay
- The config option 'resolve_key_conflicts' has been changed to `key_conflict_resolve_scope` which can cancel the vanilla actions when guard key conflicts with item use key

### Skill and Skill UI
- Added a replace cooldown for each skill slot (#2021)
- Added scrolling to the skill editor and slot selector to further enhance addon extensibility

### Shoulder surfing compatibility
- Players now follow camera when they're taking specific Epic Fight actions (attacks, blocking)

### Etc
- Now players can suppress movements of combo attacks by pressing the sneak key
- Enhanced the block highlight so that it only stains an opaque part