package yesman.epicfight.api.client.event.impl;

import com.mojang.datafixers.util.Pair;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.item.CapabilityItem;

public final class VanillaGUIEventHooks {
    private static final Pair<ResourceLocation, ResourceLocation> OFFHAND_TEXTURE = Pair.of(InventoryMenu.BLOCK_ATLAS, InventoryMenu.EMPTY_ARMOR_SLOT_SHIELD);

    /// Called when the player presses mouse buttons in any [Screen]
    ///
    /// @return whether cancel the event
    ///
    /// @see MouseHandler#onPress
    public static boolean onMouseButtonPressedInScreen(Screen screen) {
        LocalPlayer localPlayer = Minecraft.getInstance().player;

        if (localPlayer != null && screen instanceof AbstractContainerScreen<?> abstractContainerScreen) {
            Slot slot = ((yesman.epicfight.mixin.client.AbstractContainerScreenAccessor) abstractContainerScreen).epicfight$getHoveredSlot();

            if (slot != null) {
                CapabilityItem cap = EpicFightCapabilities.getItemStackCapability(localPlayer.containerMenu.getCarried());

                if (!cap.canBePlacedOffhand()) {
                    return slot.getNoItemIcon() != null && slot.getNoItemIcon().equals(OFFHAND_TEXTURE);
                }
            }
        }

        return false;
    }

    /// Called when the player releases mouse buttons in any [Screen]
    ///
    /// @return whether cancel the event
    ///
    /// @see MouseHandler#onPress
    public static boolean onMouseButtonReleasedInScreen(Screen screen) {
        LocalPlayer localPlayer = Minecraft.getInstance().player;

        if (localPlayer != null && screen instanceof AbstractContainerScreen<?> abstractContainerScreen) {
            Slot slot = ((yesman.epicfight.mixin.client.AbstractContainerScreenAccessor) abstractContainerScreen).epicfight$getHoveredSlot();

            if (slot != null) {
                CapabilityItem cap = EpicFightCapabilities.getItemStackCapability(localPlayer.containerMenu.getCarried());

                if (!cap.canBePlacedOffhand()) {
                    return slot.getNoItemIcon() != null && slot.getNoItemIcon().equals(OFFHAND_TEXTURE);
                }
            }
        }

        return false;
    }

    /// Called when the player presses keyboard keys in any [Screen]
    ///
    /// @return whether cancel the event
    ///
    /// @see KeyboardHandler#keyPress
    public static boolean onKeyboardPressedInScreen(Screen screen, int keyCode) {
        Minecraft minecraft = Minecraft.getInstance();

        // TODO: (INPUT_SYSTEM_REFACTOR) This only disables putting the item to offhand inventory slot for key inputs (defaults to F).
        //  Explore a universal solution that also supports controllers and other input systems.
        //  https://github.com/Epic-Fight/epicfight/issues/2135
        if (keyCode == yesman.epicfight.client.input.InputUtils.getKey(minecraft.options.keySwapOffhand).getValue()) {
            if (screen instanceof AbstractContainerScreen<?> abstractContainerScreen) {
                Slot slot = ((yesman.epicfight.mixin.client.AbstractContainerScreenAccessor) abstractContainerScreen).epicfight$getHoveredSlot();

                if (slot != null && slot.hasItem()) {
                    CapabilityItem itemCapability = EpicFightCapabilities.getItemStackCapability(slot.getItem());
                    return !itemCapability.canBePlacedOffhand();
                }
            }
        } else if (keyCode >= 49 && keyCode <= 57) {
            if (screen instanceof AbstractContainerScreen<?> abstractContainerScreen) {
                Slot slot = ((yesman.epicfight.mixin.client.AbstractContainerScreenAccessor) abstractContainerScreen).epicfight$getHoveredSlot();

                if (minecraft.player != null && slot != null && slot.getNoItemIcon() != null && slot.getNoItemIcon().equals(OFFHAND_TEXTURE)) {
                    CapabilityItem itemCapability = EpicFightCapabilities.getItemStackCapability(minecraft.player.getInventory().getItem(keyCode - 49));
                    return !itemCapability.canBePlacedOffhand();
                }
            }
        }

        return false;
    }

    private VanillaGUIEventHooks() {}
}
