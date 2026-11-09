package guivnf.epicfight.world.entity.ai;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.BodyRotationControl;

public class ResponsiveBodyRotationControl extends BodyRotationControl {
    private static final float TURN_START = 15.0F;
    private static final float TURN_STOP = 3.0F;
    private static final float STAND_TURN_SPEED = 6.0F;
    private static final float MOVE_TURN_SPEED = 15.0F;

    private final Mob mob;
    private boolean turning;

    public ResponsiveBodyRotationControl(final Mob mob) {
        super(mob);
        this.mob = mob;
    }

    @Override
    public void clientTick() {
        double xd = this.mob.getX() - this.mob.xo;
        double zd = this.mob.getZ() - this.mob.zo;

        if (xd * xd + zd * zd > 2.5000003E-7F) {
            this.mob.yBodyRot = Mth.approachDegrees(this.mob.yBodyRot, this.mob.getYRot(), MOVE_TURN_SPEED);
            this.mob.yHeadRot = Mth.rotateIfNecessary(this.mob.yHeadRot, this.mob.yBodyRot, this.mob.getMaxHeadYRot());
            this.turning = false;
            return;
        }

        float offset = Mth.wrapDegrees(this.mob.yHeadRot - this.mob.yBodyRot);

        if (Math.abs(offset) > TURN_START) this.turning = true;

        if (this.turning) {
            this.mob.yBodyRot = Mth.approachDegrees(this.mob.yBodyRot, this.mob.yHeadRot, STAND_TURN_SPEED);
            this.turning = Math.abs(offset) > TURN_STOP;
        }

        this.mob.yHeadRot = Mth.rotateIfNecessary(this.mob.yHeadRot, this.mob.yBodyRot, this.mob.getMaxHeadYRot());
    }
}
