package yesman.epicfight.world.item;
import yesman.epicfight.EpicFight;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class WeaponItem extends Item {
    public WeaponItem(Item.Properties properties) {
        super(properties);
    }
    
    /**
     * Below code are copies from {@link SwordItem.class} to share the common properties of swords
     */
    public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, Player player) {
        return !player.isCreative();
    }

    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        return true;
    }

    public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        stack.hurtAndBreak(1, attacker, EquipmentSlot.MAINHAND);
    }

    public boolean canPerformAction(ItemStack stack, yesman.epicfight.platform.neoforged.common.ItemAbility itemAbility) {
        return yesman.epicfight.platform.neoforged.common.ItemAbilities.DEFAULT_SWORD_ACTIONS.contains(itemAbility);
    }
}
