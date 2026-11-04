package yesman.epicfight.mixin.common;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yesman.epicfight.api.event.impl.VanillaPlayerEventHooks;

/// Mixin for [BowItem] to wire [ArrowLooseEvent].
///
/// In NeoForge, ArrowLooseEvent fires when a player releases a bow.
/// In Fabric, we intercept [BowItem.releaseUsing] at HEAD.
@Mixin(value = BowItem.class)
public abstract class MixinBowItem {

    @Inject(
        at = @At(value = "HEAD"),
        method = "releaseUsing(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;I)V"
    )
    private void epicfight$arrowLoose(ItemStack stack, Level level, LivingEntity entity, int timeCharged, CallbackInfo info) {
        if (entity instanceof Player player) {
            VanillaPlayerEventHooks.onLooseArrow(player);
        }
    }
}
