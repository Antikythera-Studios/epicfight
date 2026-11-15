package yesman.epicfight.platform.neoforged.event;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;

/// Stub for NeoForge's EventHooks.
public class EventHooks {
    public static boolean onLivingTick(LivingEntity entity) { return false; }
    public static void onLivingJump(LivingEntity entity) {}
    public static boolean onLivingUseItem(LivingEntity entity, ItemStack stack, int duration) { return false; }
    public static boolean onUseItemStop(LivingEntity entity, ItemStack stack, int duration) { return false; }
    public static boolean onEntityDestroyBlock(Object boss, BlockPos pos, BlockState state) { return true; }
    public static boolean onEntityDestroyBlock(Level level, BlockPos pos, BlockState state) { return true; }
    public static boolean canEntityGrief(Level level, Mob mob) {
        return mob != null && level.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_MOBGRIEFING);
    }
}
