package yesman.epicfight.mixin.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.world.BossEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yesman.epicfight.client.events.engine.RenderEngine;

@Mixin(value = BossHealthOverlay.class)
public abstract class MixinBossHealthOverlay {
    @Inject(
        method = "drawBar(Lnet/minecraft/client/gui/GuiGraphics;IILnet/minecraft/world/BossEvent;)V",
        at = @At("TAIL")
    )
    private void epicfight$drawBar(GuiGraphics guiGraphics, int x, int y, BossEvent bossEvent, CallbackInfo callbackInfo) {
        RenderEngine.getInstance().epicfight$bossEventProgress(guiGraphics, x, y, bossEvent);
    }
}
