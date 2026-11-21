package yesman.epicfight.world.capabilities.item;

import java.util.Map;

import com.google.common.collect.Maps;

import net.minecraft.world.InteractionHand;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.LivingMotion;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

public class RangedWeaponCapability extends CapabilityItem {
    protected Map<LivingMotion, AnimationAccessor<? extends StaticAnimation>> rangeAnimationModifiers;
    protected ZoomInType zoomInType;

    protected RangedWeaponCapability(RangedWeaponCapability.Builder builder) {
        super(builder);

        RangedWeaponCapability.Builder rangedBuilder = builder;
        this.rangeAnimationModifiers = rangedBuilder.rangeAnimationModifiers;
        this.zoomInType = rangedBuilder.zoomInType;
    }

    @Override
    public Map<LivingMotion, AnimationAccessor<? extends StaticAnimation>> getLivingMotionModifier(LivingEntityPatch<?> playerdata, InteractionHand hand) {
        if (hand == InteractionHand.MAIN_HAND) {
            return this.rangeAnimationModifiers;
        }

        return super.getLivingMotionModifier(playerdata, hand);
    }

    @Override
    public boolean canBePlacedOffhand() {
        return false;
    }

    @Override
    public ZoomInType getZoomInType() {
        return this.zoomInType;
    }

    public static RangedWeaponCapability.Builder builder() {
        return new RangedWeaponCapability.Builder();
    }

    public static class Builder extends CapabilityItem.Builder<Builder> {
        private Map<LivingMotion, AnimationAccessor<? extends StaticAnimation>> rangeAnimationModifiers;
        private ZoomInType zoomInType = ZoomInType.USE_TICK;

        protected Builder() {
            this.category = WeaponCategories.RANGED;
            this.constructor = RangedWeaponCapability::new;
            this.rangeAnimationModifiers = Maps.newHashMap();
        }

        public Builder addAnimationsModifier(LivingMotion livingMotion, AnimationAccessor<? extends StaticAnimation> animations) {
            this.rangeAnimationModifiers.put(livingMotion, animations);
            return this;
        }

        public Builder zoomInType(ZoomInType zoomInType) {
            this.zoomInType = zoomInType;
            return this;
        }
    }
}