package guivnf.epicfight.world.entity.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

public class SmoothMoveControl extends MoveControl<Mob> {
    private static final float TURN_SPEED = 12.0F;
    private static final double LOOKAHEAD = 1.25D;

    public SmoothMoveControl(final Mob mob) {
        super(mob);
    }

    @Override
    public void tick() {
        if (this.operation != Operation.MOVE_TO) {
            super.tick();
            return;
        }

        this.operation = Operation.WAIT;
        double xd = this.wantedX - this.mob.getX();
        double yd = this.wantedY - this.mob.getY();
        double zd = this.wantedZ - this.mob.getZ();

        if (xd * xd + yd * yd + zd * zd < 2.5000003E-7F) {
            this.mob.setZza(0.0F);
            return;
        }

        Vec3 aim = this.aimPoint();
        float aimYaw = (float) (Mth.atan2(aim.z - this.mob.getZ(), aim.x - this.mob.getX()) * Mth.RAD_TO_DEG) - 90.0F;
        float yaw = this.rotlerp(this.mob.getYRot(), aimYaw, TURN_SPEED);
        float facing = Math.max(Mth.cos(Mth.wrapDegrees(aimYaw - yaw) * Mth.DEG_TO_RAD), 0.0F);

        this.mob.setYRot(yaw);
        this.mob.setSpeed((float) (this.speedModifier * this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED)) * facing);

        BlockPos pos = this.mob.blockPosition();
        BlockState state = this.mob.level().getBlockState(pos);
        VoxelShape shape = state.getCollisionShape(this.mob.level(), pos);
        boolean climb = yd > this.mob.maxUpStep() && xd * xd + zd * zd < Math.max(1.0F, this.mob.getBbWidth());
        boolean blocked = !shape.isEmpty() && this.mob.getY() < shape.max(Direction.Axis.Y) + pos.getY() && !state.is(BlockTags.DOORS) && !state.is(BlockTags.FENCES);

        if (climb || blocked) {
            this.mob.getJumpControl().jump();
            this.operation = Operation.JUMPING;
        }
    }

    private Vec3 aimPoint() {
        Path path = this.mob.getNavigation().getPath();
        Vec3 from = this.mob.position();

        if (path == null || path.isDone()) return new Vec3(this.wantedX, this.wantedY, this.wantedZ);

        double left = LOOKAHEAD;

        for (int i = path.getNextNodeIndex(); i < path.getNodeCount(); i++) {
            Vec3 node = path.getEntityPosAtNode(this.mob, i);
            double length = Math.hypot(node.x - from.x, node.z - from.z);

            if (length >= left) return from.lerp(node, left / length);

            left -= length;
            from = node;
        }

        return from;
    }
}
