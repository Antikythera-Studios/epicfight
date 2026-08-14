# 🌱 Contributing

This guide is for developers interested in contributing to the Epic Fight project.

> [!NOTE]  
> While we welcome contributions, we highly
> encourage [creating issues on GitHub](https://github.com/Epic-Fight/epicfight//issues/new) before
> working on them—especially for medium to large changes.

## 📜 Code of Conduct

Please review our [Code of Conduct](./CODE_OF_CONDUCT.md) to understand the expected standards of behavior when
participating in this project.

## 📋 Prerequisites

- Linux, macOS, or Windows.
- A [Java JDK](https://adoptium.net/temurin/releases). The exact JDK version depends on the Minecraft
  version, for example:
    - **1.21.1**: [21](https://adoptium.net/temurin/releases?version=21)
    - **1.20.1**: [17](https://adoptium.net/temurin/releases?version=17&os=any&arch=any)
- [git](https://git-scm.com/) for version control.
- [IntelliJ IDEA Community Edition](https://www.jetbrains.com/idea/download/)
  with [Minecraft Development IDE plugin](https://plugins.jetbrains.com/plugin/8327-minecraft-development) (**optional
  but recommended**).
- [Commit signature verification](https://docs.github.com/en/authentication/managing-commit-signature-verification/about-commit-signature-verification)
  set up for your GitHub account (**optional but recommended**).
- [Minecraft account](https://www.minecraft.net/store/minecraft-deluxe-collection-pc) to launch the game and test
  cape physics and online features (**optional**).

## 🍴 Forking & cloning the repository

> [!TIP]
> If you want a **beginner-friendly** guide on how to contribute by sending **pull requests** (aka PRs),
> refer to [this video](https://youtu.be/8lGpZkjnkt4?si=X_OV0U6Jk3ox-DHY).
> This skill will benefit you **greatly** as a **professional developer**, not just for one PR.

- Fork the [GitHub repo](https://github.com/Epic-Fight/epicfight/) to your account. If you already have a fork,
  make sure it's up to date.

* Clone your fork:

    ```bash
    git clone git@github.com:YOUR_GITHUB_USERNAME_HERE/epicfight.git
    cd epicfight
    ```

* Add the upstream repo:

    ```bash
    git remote add upstream git@github.com:Epic-Fight/epicfight.git
    ```

  This allows you to fetch updates from the main repository.

* Create a local branch and checkout to it:

  ```bash
  git branch -b YOUR_BRANCH_NAME_HERE
  ```

* Make your changes and commit them (structured commits are a bonus):

  ```bash
  git add .
  git commit -m "YOUR_COMMIT_MESSAGE_HERE"
  ```

* Push local branch:

  ```bash
  git push origin YOUR_BRANCH_NAME_HERE
  ```

* GitHub will prompt you to open a GitHub pull request, if not, you can
  open [this link](http://github.com/Epic-Fight/epicfight/pull/new).

## 🧪 Testing

If this is the first time you're building the project, run the following commands
to build and generate IDE configurations:

```shell
$ ./gradlew ide
$ ./gradlew build
```

To test the game (client):

```shell
$ ./gradlew runClient
```

To test the dedicated server:

```shell
$ ./gradlew runServer
```

## ⚙️ Development Notes

- Update [`CHANGELOG.md`](CHANGELOG.md) whenever you make changes.
    - Follow [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) for format and weaponStance.
    - If the change is **breaking** or affects other mods/addons, document it in the `For Devs` section.

## ✏️ API Documentation

Clearly documenting your changes in a simple and understandable manner is one of the best ways to communicate updates to other developers and users.
Documentation helps new developers understand the API and contributes to a smoother, more efficient workflow overall.

In Epic Fight, we’ve created a system that allows developers to create new documentation pages explaining the API, while also having them automatically displayed on our [Official Wiki](https://epicfight-docs.readthedocs.io). 
Below are a few useful steps to help you make use of this system.

1. Getting Started<br>
The first step is to create your documentation file. All documentation files must be placed within the ``docs/`` directory of the repository.
You must decide whether your new documentation page is Universal (applies to all versions of the mod) or Version-Specific (applies only to the current branch).
Using the universal_docs folder simplifies maintenance by allowing you to update a single file that reflects across the entire wiki.<br>
Below is a guide on how files are mapped from the source repository to the public wiki:

| Source Path (in EpicFight) | Target Path (in EpicFight-Docs) |
|---------------------------|--------------------------------|
| docs/universal_docs/General_Info.en.md | docs/API/General_Info.en.md |
| docs/universal_docs/porting/Guide.en.md | docs/API/porting/Guide.en.md |
| docs/version_specific/Items.en.md (on branch 1.21.1) | docs/API/1.21.1/Items.en.md |
| docs/version_specific/Items.en.md (on branch 1.20.1) | docs/API/1.20.1/Items.en.md |
| docs/version_specific/Animations/Math.en.md (on branch 1.21.1) | docs/API/1.21.1/Animations/Math.en.md |

* **Universal Directory:** Files placed in ``docs/universal_docs/`` will appear at the root of the API section. This keeps the navigation clean and avoids deep nesting for general information.

* **Version Specific Directory:** Files placed in ``docs/version_specific/`` will be automatically nested inside a folder named after your current branch (e.g., main, 1.21.1, 1.20.1).

2. File name<br>
Naming your file is straightforward. Create the file and append the ``.en.md``.suffix when specifying the file extension.
This generates an English documentation page, which is the default language for our wiki.<br>
The list of available languages is shown below.

| Language | ISO codes |
|---------------------------|--------------------------------|
| English [Default] | ``.en.`` |
| Portuguease | ``.pt.`` |
| Spanish | ``.es.`` |
| Russian | ``.ru.`` |
| Polish | ``.pl.`` |
| Japanese | ``.ja.`` |
| Chinese | ``.zh.`` |
| Korean | ``.ko.`` |
| Croatian | ``.hr.`` |

Keep in mind that contributors are not required to translate their documentation pages. That said, ***we encourage writing documentation in English to ensure it is accessible to a wider audience***.

3. Page metadata (Hiding version warning)
As the last step, add the following metadata to the top of your documentation page.
```
---
hide:
  - announcement
---
```

This prevents the wiki from prompting users to search for a newer version of the page. It is a simple adjustment that should be included in all current documentation.

## 🧩 Code Style & Recommendations

### 1. Keep the code independent of mod loader APIs

Avoid unnecessary mod loader-specific APIs like `@OnlyIn`. This annotation exists because the separate Minecraft server
JAR lacks client-only classes, and calling a non-existent class can cause crashes that are hard to debug. `@OnlyIn`
prevents this by crashing the game when used on the wrong distribution.

For mods, this is usually unnecessary, as mods typically provide a single JAR for both server and client.

Relying on such APIs makes the project harder to maintain, port to newer Minecraft versions, or support multiple mod
loaders.

We're still working on fixing this issue, as the current system relies heavily on NeoForge/Forge.  
This makes it difficult to port to other mod loaders like Fabric or future NeoForge-based forks.

> [!IMPORTANT]
> Avoid designing code under the assumption that NeoForge will always be used.  
> NeoForge could be replaced or ported later, as happened with Forge—change is inevitable in software.  
> Design for adaptability and low-maintenance updates, even if it requires a higher initial implementation cost.

### 2. Keep the code design more independent of the Minecraft APIs

Try to not depend on Minecraft APIs directly in many files.

#### 🚫 Avoid

```java
// File1.java

Minecraft.getInstance().player.setGameMode(0);

// File2.java

Minecraft.getInstance().player.setGameMode(1);

// File3.java

Minecraft.getInstance().player.setGameMode(1);
```

#### ✅ Preferred

```java
// MyMinecraft.java

// An example only, we should find a better name depending on the problem we're trying to solve.
final class MyMinecraft {
    private static setPlayerGameMode(int gameMode) {
        Minecraft.getInstance().player.setGameMode(gameMode);
    }
}

// File1.java

MyMinecraft.setGameMode(0);

// File2.java

MyMinecraft.setGameMode(1);

// File3.java

MyMinecraft.setGameMode(1);
```

So if Minecraft ever changes the `setGameMode` method—for example, to use an `enum` (e.g., `GameMode.CREATIVE`) instead
of an `int`—we only need to update one place, which greatly reduces maintenance.

> [!NOTE]
> This is just an example. Do not consider it best practice—always look for better alternatives.
> It's not always a good idea to introduce abstractions around Minecraft APIs.

### 3. Avoid tight-coupling on third-party mods directly when implementing compatibility

When adding compatibility with a third-party mod, avoid using their imports everywhere across the codebase to implement
the compatibility. Always confirm that it's an optional dependency.

If you can't find an Epic Fight API that you could use,
like an event to register inside `ICompatModule`, or a callback,
try to develop an API or refactor the existing code to make it easier to solve
the problem, regardless of the dependency X.

For example, we tried to add controller
support ([full issue report](https://github.com/Epic-Fight/epicfight/issues/2116)). However, since many controller mods
exist and change over time, we introduced [major refactoring](https://github.com/Epic-Fight/epicfight/pull/2122), and
then added [Controlify integration](https://github.com/Epic-Fight/epicfight/pull/2133) with just one class—without any
compromise to support quality. This also made it easier to backport it to an older Minecraft version.

Keep the compatibility modular, if it's not possible to implement the compatibility in a modular way,
simplify the existing code before introducing any dependencies. This also makes it easier to replace the dependency.

It's preferable that any imports specific to a third-party dependency remain inside their own package, such as
`yesman.epicfight.compat.mod_name.x`.

> [!NOTE]
> Remember, this is a long-term project, so we take maintenance seriously,
> introducing changes that "just" work, hacks, workarounds, technical debt, may result in serious
> consequences in the long-term. 

### 4. Always prefer a supported public API

For example, these mods provide a supported API that you should consider whatever possible:

* [Shoulder Surfing](https://github.com/Exopandora/ShoulderSurfing/wiki/API-Documentation-Plugins)
* [Controlify Entrypoint](https://moddedmc.wiki/en/project/controlify/latest/docs/developers/controlify-entrypoint)
* [Iron's Spells](https://iron.wiki/developers/)
* [Curios Inventory](https://docs.illusivesoulworks.com/curios/getting-started)
* [GeckoLib](https://github.com/bernie-g/geckolib/wiki)

You could also refer to the mod's `api` package to see if they provide an event, use mixins as the last option.

If the mod does not provide any public API, then we should consider whether its worth depending on,
or if we could provide the compatibility in a separate mod.

The classic Minecraft modding solution is to use mixins, BUT any mixins on third-party classes should remain under
`yesman.epicfight.compat.mod_name.mixin`, and then create a dedicated mixin JSON file just for that mod,
and only register it if the mod is loaded.

Confirm that it does not spam the log, with or without the third-party mod loaded.

### 5. Keep code modular

Even if it's not a public API—or just a private method or field—keep the code modular.  
Avoid putting everything into a single large method that does it all, as this reduces reusability and leads to future
refactoring, cleanup, breaking changes, and extensive testing to fix later.

#### 🚫 Avoid

```java
private void handleKeyMappings() {
    if (isActionPressed()) {
        // Action implementation details
    }
    // ...
}
```

#### ✅ Preferred

```java
private void handleKeyMappings() {
    if (isActionPressed()) {
        doAction();
    }
    // ...
}

private doAction() {
    // Action implementation details
}
```

The first example mixes the trigger condition with the action logic, making it harder for other mods to inject or extend
behavior (even if they shouldn't, they should still have the option).
It also complicates adding new features, fixing bugs, porting to new versions, maintaining the code, and keeping it
readable.

Maybe the

> [!TIP]
> For a real example, refer to this:
> - [Before](https://github.com/Epic-Fight/epicfight/blob/c40ce6b00a2643927ba1b2f2ab27195cf23168f8/src/main/java/yesman/epicfight/client/events/engine/ControlEngine.java#L102-L393)
> - [After](https://github.com/Epic-Fight/epicfight/blob/1.20.1/src/main/java/yesman/epicfight/client/events/engine/ControlEngine.java#L139-L295)

### 6. Always test the game in a production environment

Even if it works on your development machine, that doesn't mean it will work in a production environment.  
When modifying mixins, always build the JAR file and test it in a fresh instance to ensure it truly works from the
user's perspective.

### 7. Don't rely on temporary understanding

Even if you fully understand the code now, others might not—and in the future, you might not either.

Keep it simple: write clear Javadocs, use meaningful names, and maintain a clear structure that makes the code easy to
revisit at any time. This habit will greatly benefit you as a professional software engineer, beyond a single pull
request.

Try to avoid depending on temporary context whenever possible.
Always take time to ensure your code is simple and easy to understand.

### 8. Document public APIs

Although our existing public APIs lack documentation, we're working to improve this going forward.

### 9. Keep code formatting consistent

Currently, we don't have a GitHub workflow or CI setup to enforce this, but that may change in future releases.

### 10. Avoid magical numbers

Avoid mysterious and magical numbers that lack a clear purpose or explanation.

```diff
- rect.left = 4.24264068712;
+ rect.left = 3.0 * Math.sqrt(2);
```

### 11. Use Markdown for Javadoc comments

Markdown support has been [added in Java 23+](https://openjdk.org/jeps/467); however,  
[IntelliJ IDEA supports it](https://blog.jetbrains.com/idea/2025/04/markdown-in-java-docs-shut-up-and-take-my-comments/)
in any Java version.

To adopt this change, new comments should be written using Markdown syntax,  
and existing comments should be migrated gradually.

#### 🚫 Avoid

```java
/**
 * This key mapping only applies {@link KeyConflictContext#IN_GAME} since it represents player moves
 */
```

#### ✅ Preferred

```java
/// This key mapping only applies [KeyConflictContext#IN_GAME] since it represents player moves.
```

### 12. Avoid hardcoding translation keys in the source code

Avoid referencing translation keys (from `en_us.json` resource file) directly in source code,
instead, use the generated `LangKeys` object:

```diff
- String key = "key." + EpicFightMod.MODID + ".switch_mode.description";
+ String key = LangKeys.KEY_SWITCH_MODE_DESCRIPTION;

Component.translatable(key);
```

Hardcoding is error-prone, more runtime crashes and bugs, and misleading behavior.
Where `LangKeys` is a generated object that updates when launching the game,
and automatically adapts the latest `en_us.json` changes, forcing all references to be fixed at compile time.

### 13. Avoid handling programming bugs at runtime or hiding them

Imagine a complex system with many features that suddenly stops working,
with no errors, no logs, no crashes.

That would be very hard to debug, right?

Which is why it's sometimes better to crash the game than allow misleading
behavior caused by programming bugs.

Example of good design that makes bugs easier to catch:

```java
/// Sets the maximum number of retries.
///
/// @param retries number of retries, must be >= 0
/// @throws IllegalArgumentException if retries is negative (invalid usage)
void setMaxRetries(int retries) {
  if (retries < 0) {
    throw new IllegalArgumentException("retries must be >= 0");
  }
  // ...
}
```

### Errors vs Failures

- **Errors:** Programming issues that must be fixed at the core,
  and **never** handled at runtime via `try-catch`
  (e.g., passing a `-1` or `null` to a parameter that requires a valid argument value).
  - `IllegalArgumentException` is almost always a programming bug.
  - Errors such as `NullPointerException` must never be caught via `try-catch`, as they usually indicate
    programming bugs that must be solved.
  - Avoid throwing errors for expected failures.
- **Failures:** Runtime issues that are not the
  developer's fault (e.g., bad image data on a user's device, disk full, incorrect password or email not found).

It's better to prevent it at the core, rather than accessing a nullable
fields and then using this workaround (try-catch `NullPointerException`).

Handling is only acceptable when coming from external libraries or JSON deserialization.
Even then, document the workaround clearly, map it to a cleaner exception,
or validate via (e.g., `assert`, `if`) and fail fast.

It's also acceptable to log errors instead of silently ignoring
bugs to save development time ([example PR](https://github.com/Epic-Fight/epicfight/pull/1935)).

> [!TIP]
> For more details about this topic,
> read [this comment](https://github.com/flutter/packages/pull/8079#discussion_r1902075152).
> However, it's unrelated to Epic Fight.
