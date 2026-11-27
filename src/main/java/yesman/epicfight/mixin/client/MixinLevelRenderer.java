package yesman.epicfight.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import yesman.epicfight.api.client.camera.EpicFightCameraAPI;
import yesman.epicfight.client.renderer.EpicFightFrustumHolder;
import yesman.epicfight.config.ClientConfig;

import java.util.Iterator;

@Mixin(value = LevelRenderer.class)
public class MixinLevelRenderer {
	@Shadow @Final
	private RenderBuffers renderBuffers;

	@Shadow @Final
	private Minecraft minecraft;

	/// Stores the current render frustum so it can be accessed outside the render loop
	/// (e.g., by [EpicFightCameraAPI.setNextLockOnTarget]). On NeoForge, [LevelRenderer.getFrustum()]
	/// provides this; on Fabric, the frustum is a local variable in [renderLevel].
	@Unique
	private Frustum epicfight$currentFrustum;

	@Inject(
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/OutlineBufferSource;setColor(IIII)V",
			shift = Shift.AFTER
		),
		method = "renderLevel(Lnet/minecraft/client/DeltaTracker;ZLnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;)V",
		locals = LocalCapture.CAPTURE_FAILHARD
	)
	private void epicfight$renderLevel(
		DeltaTracker deltaTracker, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f frustumMatrix, Matrix4f projectionMatrix, CallbackInfo callbackInfo,
		// local variables
		TickRateManager local1,
		float local2,
		ProfilerFiller local3,
		Vec3 local4,
		double local5,
		double local6,
		double local7,
		boolean local8,
		Frustum local9,
		float local10,
		boolean local11,
		Matrix4fStack local12,
		boolean local13,
		PoseStack local14,
		MultiBufferSource.BufferSource local15,
		Iterator<?> local16,
		Entity local17,
		BlockPos local18,
		MultiBufferSource local19,
		OutlineBufferSource local20,
		int local21
	) {
		// Capture the frustum for use outside the render loop
		EpicFightFrustumHolder.set(local9);

        int color = ClientConfig.packedTargetOutlineColor;
        int r = color >> 16 & 255;
        int g = color >> 8 & 255;
        int b = color & 255;
        if (EpicFightCameraAPI.getInstance().shouldHighlightTarget(local17)) this.renderBuffers.outlineBufferSource().setColor(r, g, b, 255);
	}

	@Inject(at = @At(value = "HEAD"), method = "addParticleInternal(Lnet/minecraft/core/particles/ParticleOptions;ZZDDDDDD)Lnet/minecraft/client/particle/Particle;")
	private void epicfight$probeAddParticleInternal(net.minecraft.core.particles.ParticleOptions options, boolean force, boolean decreased, double x, double y, double z, double xs, double ys, double zs, CallbackInfoReturnable<net.minecraft.client.particle.Particle> cir) {
		if (yesman.epicfight.api.animation.types.StaticAnimation.TRAIL_DEBUG) {
			net.minecraft.resources.ResourceLocation key = net.minecraft.core.registries.BuiltInRegistries.PARTICLE_TYPE.getKey(options.getType());
			if (key != null && !"minecraft".equals(key.getNamespace())) {
				net.minecraft.world.phys.Vec3 cam = this.minecraft.gameRenderer.getMainCamera().getPosition();
				double dist2 = cam.distanceToSqr(x, y, z);
				yesman.epicfight.EpicFight.LOGGER.info("[TRAILDBG]   addParticleInternal type={} force={} decreased={} spawnPos=({},{},{}) camDist2={} dropByDist={}", key, force, decreased, x, y, z, (long)dist2, !force && dist2 > 1024.0);
			}
		}
	}
}
