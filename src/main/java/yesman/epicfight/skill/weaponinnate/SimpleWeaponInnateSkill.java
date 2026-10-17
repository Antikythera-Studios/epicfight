package yesman.epicfight.skill.weaponinnate;

import java.util.List;
import java.util.function.Function;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.api.animation.types.AttackAnimation.Phase;
import yesman.epicfight.skill.SkillCategories;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;

public class SimpleWeaponInnateSkill extends WeaponInnateSkill {
	public static final class Builder extends WeaponInnateSkill.Builder<SimpleWeaponInnateSkill.Builder> {
		protected AnimationAccessor<? extends AttackAnimation> attackAnimation;
		
		public Builder(Function<Builder, ? extends SimpleWeaponInnateSkill> constructor) {
			super(constructor);
		}
		
		public SimpleWeaponInnateSkill.Builder setAnimations(AnimationAccessor<? extends AttackAnimation> attackAnimation) {
			this.attackAnimation = attackAnimation;
			return this;
		}
	}
	
	public static SimpleWeaponInnateSkill.Builder createSimpleWeaponInnateBuilder() {
		return new SimpleWeaponInnateSkill.Builder(SimpleWeaponInnateSkill::new).setCategory(SkillCategories.WEAPON_INNATE).setResource(Resource.WEAPON_CHARGE);
	}
	
	public static SimpleWeaponInnateSkill.Builder createSimpleWeaponInnateBuilder(Function<SimpleWeaponInnateSkill.Builder, ? extends SimpleWeaponInnateSkill> constructor) {
		return new SimpleWeaponInnateSkill.Builder(constructor).setCategory(SkillCategories.WEAPON_INNATE).setResource(Resource.WEAPON_CHARGE);
	}
	
	protected AnimationAccessor<? extends AttackAnimation> attackAnimation;
	
	public SimpleWeaponInnateSkill(SimpleWeaponInnateSkill.Builder builder) {
		super(builder);
		
		this.attackAnimation = builder.attackAnimation;
	}
	
	@Override
	public void executeOnServer(SkillContainer container, CompoundTag arguments) {
		container.getExecutor().playAnimationSynchronized(this.attackAnimation, 0);
		
		super.executeOnServer(container, arguments);
	}
	
	@Override
	public List<Component> getTooltipOnItem(ItemStack itemStack, CapabilityItem cap, PlayerPatch<?> playerCap) {
		List<Component> list = super.getTooltipOnItem(itemStack, cap, playerCap);
		this.generateTooltipforPhase(list, itemStack, cap, playerCap, this.properties.get(0), "Each Strike:");
		
		return list;
	}
	
	@Override
	public WeaponInnateSkill registerPropertiesToAnimation() {
		if (this.attackAnimation == null) {
			return this;
		}

		AttackAnimation anim = this.attackAnimation.get();

		if (anim == null) {
			return this;
		}

		for (Phase phase : anim.phases) {
			phase.addProperties(this.properties.get(0).entrySet());
		}

		return this;
	}
}