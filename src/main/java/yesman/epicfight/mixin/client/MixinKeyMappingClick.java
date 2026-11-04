package yesman.epicfight.mixin.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashSet;
import java.util.Set;

/// Restores multi-binding key dispatch, which Epic Fight relies on but vanilla Fabric lacks.
///
/// Vanilla [KeyMapping#click] and [KeyMapping#set] both resolve a *single* mapping via the
/// static `MAP`, and `MAP` keeps only the last [KeyMapping] constructed for a given key.
/// Epic Fight binds both `ATTACK` and `WEAPON_INNATE_SKILL` to the left mouse button, so
/// `MAP[LMB]` ends up owned by `key.epicfight.weapon_innate_skill` and vanilla's `key.attack`
/// stops receiving clicks *and* press state entirely — which silently breaks block breaking,
/// because [net.minecraft.client.Minecraft#continueAttack] is driven by `keyAttack.isDown()`
/// and calls `stopDestroyBlock()` every tick while it reads `false`.
///
/// NeoForge does not have this problem: it replaces `MAP` with a `KeyBindingMap` whose
/// `lookupAll` returns *every* mapping bound to a key, so vanilla and modded mappings both
/// observe the input. This mixin reproduces that behaviour — after vanilla has updated the
/// single `MAP` owner, the same event is propagated to every other mapping bound to the key.
///
/// Suppressing vanilla actions in Epic Fight mode is *not* this class's job; that is handled
/// by [yesman.epicfight.mixin.client.MixinMinecraft], matching upstream.
@Mixin(KeyMapping.class)
public abstract class MixinKeyMappingClick {

    /// Keys currently held, used to tell a GLFW PRESS from the REPEAT events that follow it.
    /// Vanilla [net.minecraft.client.KeyboardHandler] calls [KeyMapping#set] before
    /// [KeyMapping#click], so [KeyMapping#isDown] cannot distinguish the two on its own.
    private static final Set<InputConstants.Key> EPICFIGHT_PRESSED_KEYS = new HashSet<>();

    private static boolean epicfight$boundTo(KeyMapping mapping, InputConstants.Key key) {
        // Compare against the live "key" field rather than getDefaultKey(): MAP is never
        // updated on rebind, so the default key would match the wrong binding afterwards.
        return ((KeyMappingAccessor) mapping).epicfight$getKey().equals(key);
    }

    @Inject(method = "click(Lcom/mojang/blaze3d/platform/InputConstants$Key;)V", at = @At("TAIL"))
    private static void epicfight$distributeClick(InputConstants.Key key, CallbackInfo ci) {
        KeyMapping vanillaClicked = KeyMappingAccessor.epicfight$getMap().get(key);
        boolean isRepeat = EPICFIGHT_PRESSED_KEYS.contains(key);

        for (KeyMapping mapping : KeyMappingAccessor.epicfight$getAll().values()) {
            KeyMappingAccessor accessor = (KeyMappingAccessor) mapping;

            if (mapping == vanillaClicked) {
                // Vanilla already incremented this one. Undo it when the increment was wrong:
                // either MAP is stale (this mapping was rebound away), or this is a REPEAT.
                if (!epicfight$boundTo(mapping, key) || isRepeat) {
                    accessor.epicfight$setClickCount(accessor.epicfight$getClickCount() - 1);
                }
            } else if (epicfight$boundTo(mapping, key) && !isRepeat) {
                accessor.epicfight$setClickCount(accessor.epicfight$getClickCount() + 1);
            }
        }

        if (!isRepeat) {
            EPICFIGHT_PRESSED_KEYS.add(key);
        }
    }

    /// Propagates press state, so a mapping displaced from `MAP` still reports [KeyMapping#isDown].
    @Inject(method = "set(Lcom/mojang/blaze3d/platform/InputConstants$Key;Z)V", at = @At("TAIL"))
    private static void epicfight$distributeSet(InputConstants.Key key, boolean pressed, CallbackInfo ci) {
        KeyMapping vanillaSet = KeyMappingAccessor.epicfight$getMap().get(key);

        for (KeyMapping mapping : KeyMappingAccessor.epicfight$getAll().values()) {
            if (mapping != vanillaSet && epicfight$boundTo(mapping, key)) {
                mapping.setDown(pressed);
            }
        }

        if (!pressed) {
            EPICFIGHT_PRESSED_KEYS.remove(key);
        }
    }
}
