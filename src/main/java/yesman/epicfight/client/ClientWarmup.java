package yesman.epicfight.client;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import yesman.epicfight.api.utils.LevelUtil;
import yesman.epicfight.registry.entries.EpicFightParticles;
import yesman.epicfight.registry.entries.EpicFightSounds;
import yesman.epicfight.world.level.block.FractureBlockState;

public final class ClientWarmup {
	/**
	 * Runs the terrain-fracture path once so its class loading, JIT
	 * compilation and buffer allocations don't happen mid-combat on the
	 * first real ground slam. Called once per session on the first tick
	 * with a loaded world; everything spawned is below the world floor.
	 */
	public static void warmTerrainFracture(Minecraft client) {
		ClientLevel level = client.level;

		if (level == null || client.player == null) {
			return;
		}

		int x = client.player.getBlockX();
		int z = client.player.getBlockZ();
		int floorY = level.getMinBuildHeight();

		// 1. Real fracture deep underground: exercises spreadShockwave,
		//    FractureBlockState creation, level.setBlock and
		//    FractureBlockEntity registration + lifetime tick, which
		//    restores the original state on its own afterwards.
		for (int dy = 1; dy <= 8; dy++) {
			BlockPos pos = new BlockPos(x, floorY + dy, z);
			BlockState state = level.getBlockState(pos);

			if (LevelUtil.canTransferShockWave(level, pos, state) && !(state instanceof FractureBlockState) && !(state.getBlock() instanceof EntityBlock)) {
				LevelUtil.circleSlamFracture(null, level, Vec3.atBottomCenterOf(pos), 0.6D, true, true, false);
				break;
			}
		}

		// 2. Tessellate one block through the breaking-texture path (what the
		//    fracture renderer runs per fractured block per frame) into a
		//    scratch buffer — pure CPU tessellation, nothing is drawn.
		try (ByteBufferBuilder byteBufferBuilder = new ByteBufferBuilder(8192)) {
			BufferBuilder bufferBuilder = new BufferBuilder(byteBufferBuilder, VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
			client.getBlockRenderer().renderBreakingTexture(Blocks.STONE.defaultBlockState(), BlockPos.ZERO, level, new PoseStack(), bufferBuilder);
			MeshData meshData = bufferBuilder.build();

			if (meshData != null) {
				meshData.close();
			}
		}

		// 3. Pre-allocate the shared cutout buffer the block-entity renderer
		//    draws into.
		client.renderBuffers().bufferSource().getBuffer(RenderType.cutout());

		// 4. Spawn the slam particles below the world floor so the creation
		//    path (GroundSlamParticle -> TerrainParticle + campfire smoke)
		//    is warm.
		double px = x + 0.5D;
		double py = floorY + 1.0D;
		double pz = z + 0.5D;

		client.particleEngine.createParticle(EpicFightParticles.GROUND_SLAM.get(), px, py, pz, 1.0D, 4.0D, 0.5D);
		LevelUtil.createParticle(level, new BlockPos(x, floorY + 1, z), level.getBlockState(new BlockPos(x, floorY + 1, z)));
		client.particleEngine.createParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, px, py, pz, 0.0D, 0.0D, 0.0D);

		// 5. Kick the slam sound's lazy decode at inaudible volume.
		level.playLocalSound(px, py, pz, EpicFightSounds.SLAM_HEAVY.get(), SoundSource.BLOCKS, 0.001F, 1.0F, false);
	}

	private ClientWarmup() {
	}
}
