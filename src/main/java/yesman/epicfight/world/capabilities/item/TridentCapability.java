package yesman.epicfight.world.capabilities.item;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.enchantment.Enchantments;
import org.jetbrains.annotations.NotNull;
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.LivingMotion;
import yesman.epicfight.api.animation.LivingMotions;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.particle.HitParticleType;
import yesman.epicfight.registry.entries.EpicFightParticles;
import yesman.epicfight.registry.entries.EpicFightSkills;
import yesman.epicfight.registry.entries.EpicFightSounds;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

import javax.annotation.Nullable;
import java.util.List;

public class TridentCapability extends RangedWeaponCapability {
    private List<AnimationManager.AnimationAccessor<? extends AttackAnimation>> attackMotion;
    private List<AnimationManager.AnimationAccessor<? extends AttackAnimation>> mountAttackMotion;

    public TridentCapability(RangedWeaponCapability.Builder builder) {
        super(builder);

        this.attackMotion = List.of(Animations.TRIDENT_AUTO1, Animations.TRIDENT_AUTO2, Animations.TRIDENT_AUTO3, Animations.SPEAR_DASH, Animations.SPEAR_ONEHAND_AIR_SLASH);
        this.mountAttackMotion = List.of(Animations.SPEAR_MOUNT_ATTACK);
    }

    @Override @NotNull
    public Style getStyle(LivingEntityPatch<?> entitypatch) {
        return Styles.ONE_HAND;
    }

    @Override
    public SoundEvent getHitSound() {
        return EpicFightSounds.BLADE_HIT.get();
    }

    @Override
    public HitParticleType getHitParticle() {
        return EpicFightParticles.HIT_BLADE.get();
    }

    @Override
    public List<AnimationManager.AnimationAccessor<? extends AttackAnimation>> getAutoAttackMotion(PlayerPatch<?> playerpatch) {
        return this.attackMotion;
    }

    @Override
    public List<AnimationManager.AnimationAccessor<? extends AttackAnimation>> getMountAttackMotion(PlayerPatch<?>  livingentitypatch) {
        return this.mountAttackMotion;
    }

    @Override
    public LivingMotion getLivingMotion(LivingEntityPatch<?> entitypatch, InteractionHand hand) {
        return entitypatch.getOriginal().isUsingItem() && entitypatch.getOriginal().getUseItem().getUseAnimation() == UseAnim.SPEAR ? LivingMotions.AIM : null;
    }

    @Nullable
    @Override
    public Skill getInnateSkill(PlayerPatch<?> playerpatch, ItemStack itemstack) {
        if (EnchantmentHelper.getItemEnchantmentLevel(playerpatch.getLevel().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getHolder(Enchantments.RIPTIDE).orElseThrow(), itemstack) > 0) {
            return EpicFightSkills.TSUNAMI.get();
        } else if (EnchantmentHelper.getItemEnchantmentLevel(playerpatch.getLevel().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getHolder(Enchantments.CHANNELING).orElseThrow(), itemstack) > 0) {
            return EpicFightSkills.WRATHFUL_LIGHTING.get();
        } else if (EnchantmentHelper.getItemEnchantmentLevel(playerpatch.getLevel().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getHolder(Enchantments.LOYALTY).orElseThrow(), itemstack) > 0) {
            return EpicFightSkills.EVERLASTING_ALLEGIANCE.get();
        } else {
            return null;
        }
    }
}