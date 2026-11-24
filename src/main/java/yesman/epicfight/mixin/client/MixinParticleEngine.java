package yesman.epicfight.mixin.client;

import java.util.List;
import java.util.Map;
import java.util.Queue;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;

import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import yesman.epicfight.api.client.particle.ParticleRenderTypeExtension;
import yesman.epicfight.client.ClientEngine;

@Mixin(value = ParticleEngine.class)
public class MixinParticleEngine {
	@Shadow
	@Final
	private static List<ParticleRenderType> RENDER_ORDER;

	@Shadow
	@Mutable
	private Map<ParticleRenderType, Queue<Particle>> particles;

	@Shadow
	@Final
	private TextureManager textureManager;

	@Shadow
	@Final
	private Int2ObjectMap<net.minecraft.client.particle.ParticleProvider<?>> providers;

	private static int trailDebugCounter;

	@Inject(at = @At(value = "HEAD"), method = "createParticle")
	private void epicfight$probeCreateParticle(net.minecraft.core.particles.ParticleOptions options, double x, double y, double z, double xs, double ys, double zs, CallbackInfoReturnable<Particle> cir) {
		if (yesman.epicfight.api.animation.types.StaticAnimation.TRAIL_DEBUG) {
			net.minecraft.resources.ResourceLocation key = net.minecraft.core.registries.BuiltInRegistries.PARTICLE_TYPE.getKey(options.getType());
			if (!"minecraft".equals(key.getNamespace())) {
				int rawId = net.minecraft.core.registries.BuiltInRegistries.PARTICLE_TYPE.getId(options.getType());
				yesman.epicfight.EpicFight.LOGGER.info("[TRAILDBG] createParticle type={} rawId={} provider={}", key, rawId, providers.get(rawId));
			}
		}
	}

	@Inject(at = @At(value = "HEAD"), method = "makeParticle")
	private void epicfight$probeMakeParticle(net.minecraft.core.particles.ParticleOptions options, double x, double y, double z, double xs, double ys, double zs, CallbackInfoReturnable<Particle> cir) {
		if (yesman.epicfight.api.animation.types.StaticAnimation.TRAIL_DEBUG) {
			net.minecraft.resources.ResourceLocation key = net.minecraft.core.registries.BuiltInRegistries.PARTICLE_TYPE.getKey(options.getType());
			if (!"minecraft".equals(key.getNamespace())) {
				int rawId = net.minecraft.core.registries.BuiltInRegistries.PARTICLE_TYPE.getId(options.getType());
				yesman.epicfight.EpicFight.LOGGER.info("[TRAILDBG]   makeParticle type={} rawId={} providerFound={}", key, rawId, providers.get(rawId) != null);
			}
		}
	}

	@Inject(at = @At(value = "HEAD"), method = "add")
	private void epicfight$probeAdd(Particle particle, CallbackInfo ci) {
		if (yesman.epicfight.api.animation.types.StaticAnimation.TRAIL_DEBUG && particle != null && !particle.getClass().getName().startsWith("net.minecraft")) {
			yesman.epicfight.EpicFight.LOGGER.info("[TRAILDBG]   add particle {}", particle.getClass().getName());
		}
	}

	@Inject(at = @At(value = "TAIL"), method = "<init>")
	private void epicfight$constructor(ClientLevel pLevel, TextureManager pTextureManager, CallbackInfo callbackInfo) {
		this.particles = Maps.newTreeMap(ClientEngine.makeCustomLowestParticleRenderTypeComparator(RENDER_ORDER));
	}

	/// Vanilla's render() only iterates the fixed RENDER_ORDER list, so custom
	/// ParticleRenderTypes (trail arcs, slash effects, glowing particles) are
	/// queued but never drawn. NeoForge patches the same loop upstream; on Fabric
	/// we draw every non-vanilla type after the vanilla pass, in the TreeMap's
	/// comparator order (custom types already sort last). Layers implementing
	/// {@link ParticleRenderTypeExtension} get end() exactly once per rendered
	/// layer in finally, so a post-pass type can restore framebuffer state even
	/// when rendering or the mesh upload throws.
	@Inject(at = @At(value = "TAIL"), method = "render(Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;F)V")
	private void epicfight$renderCustomParticleTypes(LightTexture lightTexture, Camera camera, float partialTick, CallbackInfo ci) {
		boolean drew = false;

		try {
			for (Map.Entry<ParticleRenderType, Queue<Particle>> entry : this.particles.entrySet()) {
				ParticleRenderType type = entry.getKey();
				Queue<Particle> queue = entry.getValue();

				if (RENDER_ORDER.contains(type) || queue == null || queue.isEmpty()) {
					continue;
				}

				if (yesman.epicfight.api.animation.types.StaticAnimation.TRAIL_DEBUG && trailDebugCounter++ % 40 == 0) {
					yesman.epicfight.EpicFight.LOGGER.info("[TRAILDBG] render pass: custom type {} queue={} totalTypes={}", type, queue.size(), this.particles.size());
				}

				if (!drew) {
					lightTexture.turnOnLightLayer();
					RenderSystem.enableDepthTest();
					drew = true;
				}

				try {
					Tesselator tesselator = Tesselator.getInstance();
					BufferBuilder builder = type.begin(tesselator, this.textureManager);

					if (builder != null) {
						for (Particle particle : queue) {
							try {
								particle.render(builder, camera, partialTick);
							} catch (Throwable throwable) {
								CrashReport crashReport = CrashReport.forThrowable(throwable, "Ticking Particle");
								CrashReportCategory crashReportCategory = crashReport.addCategory("Particle being rendered");
								crashReportCategory.setDetail("Particle", particle::toString);
								crashReportCategory.setDetail("Particle Type", type::toString);
								throw new ReportedException(crashReport);
							}
						}

						MeshData meshData = builder.build();

						if (meshData != null) {
							BufferUploader.drawWithShader(meshData);
						}
					}
				} finally {
					if (type instanceof ParticleRenderTypeExtension extension) {
						extension.end();
					}
				}
			}
		} finally {
			if (drew) {
				RenderSystem.depthMask(true);
				RenderSystem.disableBlend();
				RenderSystem.defaultBlendFunc();
				RenderSystem.enableCull();
				RenderSystem.disablePolygonOffset();
				lightTexture.turnOffLightLayer();
			}
		}
	}
}
