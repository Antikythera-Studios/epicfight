package yesman.epicfight.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleProvider;

@Mixin(ParticleEngine.class)
public interface ParticleEngineProvidersAccessor {

	@Accessor("providers")
	Int2ObjectMap<ParticleProvider<?>> getProviders();
}
