<#-- epicfight:smooth_locomotion (entityPatch) -> void --><#t>
if (${params[0]} != null && ${params[0]}.getOriginal() instanceof net.minecraft.world.entity.Mob) {<#t>
    com.yesman.akythera.core.util.helper.MobUtils.setMoveControl((net.minecraft.world.entity.Mob) ${params[0]}.getOriginal(), new guivnf.epicfight.world.entity.ai.SmoothMoveControl((net.minecraft.world.entity.Mob) ${params[0]}.getOriginal()));<#t>
    com.yesman.akythera.core.util.helper.MobUtils.setBodyRotationControl((net.minecraft.world.entity.Mob) ${params[0]}.getOriginal(), new guivnf.epicfight.world.entity.ai.ResponsiveBodyRotationControl((net.minecraft.world.entity.Mob) ${params[0]}.getOriginal()));<#t>
}<#t>