package yesman.epicfight.registry.entries;
import yesman.epicfight.EpicFight;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import yesman.epicfight.registry.deferred_shim.DeferredHolderShim;
import yesman.epicfight.registry.deferred_shim.DeferredRegisterShim;
import org.jetbrains.annotations.NotNull;
import yesman.epicfight.main.EpicFightMod;
import yesman.epicfight.particle.HitParticleType;

public final class EpicFightParticles {
	private EpicFightParticles() {}
	
	public static final DeferredRegisterShim<ParticleType<?>> REGISTRY = new DeferredRegisterShim<>(Registries.PARTICLE_TYPE, EpicFight.MODID);
	
	public static final DeferredHolderShim<ParticleType<?>, SimpleParticleType> ADRENALINE_PLAYER_BEATING = REGISTRY.register("adrenaline_player_beating", () -> new SimpleParticleType(true));
	public static final DeferredHolderShim<ParticleType<?>, SimpleParticleType> ASH_DIRECTIONAL = REGISTRY.register("ash_directional", () -> new SimpleParticleType(true));
	public static final DeferredHolderShim<ParticleType<?>, SimpleParticleType> BLOOD = REGISTRY.register("blood", () -> new SimpleParticleType(true));
	public static final DeferredHolderShim<ParticleType<?>, SimpleParticleType> CATHARSIS = REGISTRY.register("catharsis", () -> new SimpleParticleType(true));
	public static final DeferredHolderShim<ParticleType<?>, SimpleParticleType> CUT = REGISTRY.register("cut", () -> new SimpleParticleType(true));
	public static final DeferredHolderShim<ParticleType<?>, SimpleParticleType> DUST_EXPANSIVE = REGISTRY.register("dust_expansive", () -> new SimpleParticleType(true));
	public static final DeferredHolderShim<ParticleType<?>, SimpleParticleType> DUST_CONTRACTIVE = REGISTRY.register("dust_contractive", () -> new SimpleParticleType(true));
	public static final DeferredHolderShim<ParticleType<?>, SimpleParticleType> NORMAL_DUST = REGISTRY.register("dust_normal", () -> new SimpleParticleType(true));
	public static final DeferredHolderShim<ParticleType<?>, SimpleParticleType> ENDERMAN_DEATH_EMIT = REGISTRY.register("enderman_death_emit", () -> new SimpleParticleType(true));
	public static final DeferredHolderShim<ParticleType<?>, SimpleParticleType> GROUND_SLAM = REGISTRY.register("ground_slam", () -> new SimpleParticleType(true));
    public static final DeferredHolderShim<ParticleType<?>, ParticleType<BlockParticleOption>> GROUND_FRACTURE = REGISTRY.register("ground_fracture", () -> new ParticleType<BlockParticleOption>(false) {
        public @NotNull MapCodec<BlockParticleOption> codec() {
            return BlockParticleOption.codec(this);
        }

        public @NotNull StreamCodec<? super RegistryFriendlyByteBuf, BlockParticleOption> streamCodec() {
            return BlockParticleOption.streamCodec(this);
        }
    });
	public static final DeferredHolderShim<ParticleType<?>, HitParticleType> HIT_BLUNT = REGISTRY.register("hit_blunt", () -> new HitParticleType(true, HitParticleType.RANDOM_WITHIN_BOUNDING_BOX, HitParticleType.ZERO));
	public static final DeferredHolderShim<ParticleType<?>, HitParticleType> HIT_BLADE = REGISTRY.register("hit_blade", () -> new HitParticleType(true, HitParticleType.RANDOM_WITHIN_BOUNDING_BOX, HitParticleType.ZERO));
	public static final DeferredHolderShim<ParticleType<?>, HitParticleType> EVISCERATE = REGISTRY.register("eviscerate", () -> new HitParticleType(true, HitParticleType.CENTER_OF_TARGET, HitParticleType.ATTACKER_XY_ROTATION));
	public static final DeferredHolderShim<ParticleType<?>, HitParticleType> BLADE_RUSH_SKILL = REGISTRY.register("blade_rush", () -> new HitParticleType(true, HitParticleType.RANDOM_WITHIN_BOUNDING_BOX, HitParticleType.ZERO));
	public static final DeferredHolderShim<ParticleType<?>, SimpleParticleType> BREATH_FLAME = REGISTRY.register("breath_flame", () -> new SimpleParticleType(true));
	public static final DeferredHolderShim<ParticleType<?>, SimpleParticleType> FORCE_FIELD = REGISTRY.register("force_field", () -> new SimpleParticleType(true));
	public static final DeferredHolderShim<ParticleType<?>, SimpleParticleType> FORCE_FIELD_END = REGISTRY.register("force_field_end", () -> new SimpleParticleType(true));
	public static final DeferredHolderShim<ParticleType<?>, SimpleParticleType> ENTITY_AFTER_IMAGE = REGISTRY.register("after_image", () -> new SimpleParticleType(true));
	public static final DeferredHolderShim<ParticleType<?>, SimpleParticleType> LASER = REGISTRY.register("laser", () -> new SimpleParticleType(true));
	public static final DeferredHolderShim<ParticleType<?>, SimpleParticleType> NEUTRALIZE = REGISTRY.register("neutralize", () -> new SimpleParticleType(true));
	public static final DeferredHolderShim<ParticleType<?>, SimpleParticleType> BOSS_CASTING = REGISTRY.register("boss_casting", () -> new SimpleParticleType(true));
	public static final DeferredHolderShim<ParticleType<?>, SimpleParticleType> TSUNAMI_SPLASH = REGISTRY.register("tsunami_splash", () -> new SimpleParticleType(true));
	public static final DeferredHolderShim<ParticleType<?>, SimpleParticleType> FEATHER = REGISTRY.register("feather", () -> new SimpleParticleType(true));
	public static final DeferredHolderShim<ParticleType<?>, SimpleParticleType> WHITE_AFTERIMAGE = REGISTRY.register("white_afterimage", () -> new SimpleParticleType(true));

	public static final DeferredHolderShim<ParticleType<?>, HitParticleType> AIR_BURST = REGISTRY.register("air_burst", () -> new HitParticleType(true, HitParticleType.MIDDLE_OF_ENTITIES, HitParticleType.ATTACKER_Y_ROTATION));
	public static final DeferredHolderShim<ParticleType<?>, SimpleParticleType> SWING_TRAIL = REGISTRY.register("swing_trail", () -> new SimpleParticleType(true));
	public static final DeferredHolderShim<ParticleType<?>, SimpleParticleType> PROJECTILE_TRAIL = REGISTRY.register("projectile_trail", () -> new SimpleParticleType(true));
}