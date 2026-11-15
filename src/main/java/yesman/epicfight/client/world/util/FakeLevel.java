package yesman.epicfight.client.world.util;
import yesman.epicfight.EpicFight;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;
import yesman.epicfight.main.EpicFightMod;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class FakeLevel extends ClientLevel {
	private static FakeLevel instance;
	private static final Map<GameProfile, FakeClientPlayer> FAKE_PLAYERS = new HashMap<> ();
	
	public static FakeLevel getFakeLevel(ClientLevel refLevel) {
		if (instance == null || instance.registryAccess() != refLevel) {
			instance = new FakeLevel(refLevel, Minecraft.getInstance());
		}
		
		return instance;
	}
	
	public static void unloadFakeLevel() {
		instance = null;
	}
	
	public static FakeClientPlayer getFakePlayer(GameProfile playerprofile) {
		return FAKE_PLAYERS.computeIfAbsent(playerprofile, key -> new FakeClientPlayer(instance, key));
	}
	
	private final ClientLevel refLevel;
	
	public FakeLevel(ClientLevel refLevel, Minecraft minecraft) {
		super(
            // Copy the connection instance from original level due to the mixin crashes from
            // Fabric's network API (#2419), tho this may have side effect, possibly modify the world
            // data and send packet for the modification. (So in FakeLevel it overrides all methods where to use)
            ((yesman.epicfight.mixin.client.ClientLevelAccessor) refLevel).epicfight$getConnection(),
			// new FakeClientPacketListener(refLevel, minecraft), << the original approach, making a fake connection
			new ClientLevel.ClientLevelData(Difficulty.NORMAL, false, false),
			Level.OVERWORLD,
			refLevel.registryAccess().registryOrThrow(Registries.DIMENSION_TYPE).getHolderOrThrow(BuiltinDimensionTypes.OVERWORLD),
			0,
			0,
			minecraft::getProfiler,
			minecraft.levelRenderer,
			true,
			0
		);
		
		this.refLevel = refLevel;
	}

    /// These are method overrides where [#connection] is used to send packets,
    /// preventing payloads are shipping from fake level.
    ///
    /// Left only getter methods as is: [#enabledFeatures]. [#potionBrewing], [#getScoreboard], [#getRecipeManager]
    @Override
    public void disconnect() {
    }

    @Override
    public void sendPacketToServer(Packet<?> packet) {
    }
    /// **********************************************************************

	/**
	 * Accessor methods referencing original world
	 */
	@Nullable
	@Override
	public ChunkAccess getChunk(int pX, int pZ, ChunkStatus pRequiredStatus, boolean pNonnull) {
		return this.refLevel.getChunk(pX, pZ, pRequiredStatus, pNonnull);
	}
	
	@Override
	public boolean hasChunk(int pChunkX, int pChunkZ) {
		return this.refLevel.hasChunk(pChunkX, pChunkZ);
	}
	
	@Override
	public int getHeight(Heightmap.Types pHeightmapType, int pX, int pZ) {
		return this.refLevel.getHeight(pHeightmapType, pX, pZ);
	}
	
	@Override
	public int getSkyDarken() {
		return this.refLevel.getSkyDarken();
	}

    /**
     * A workaround to a crash when some mods are installed such as BadOptimizations.
     * <a href="https://github.com/imthosea/BadOptimizations/issues/108">Issue report</a>.
     *
     * @see FakeLevel#getBiomeManager()
     *
     */
    private boolean appliedGetBiomeManagerWorkaround;

    @Override
    public @NotNull BiomeManager getBiomeManager() {
        // The field "refLevel" could be null, which causes the game to freeze when
        // joining the world (100% and stuck) when some mods are installed.
        // For example: https://github.com/imthosea/BadOptimizations/issues/108
        // We work around this issue by checking for null and then fallback to the super method.
        final @Nullable ClientLevel level = this.refLevel;
        if (level == null) {
            if (!appliedGetBiomeManagerWorkaround) {
                EpicFight.LOGGER.warn(
                        """
                                FakeLevel.refLevel is null, so Epic Fight can't override getBiomeManager().
                                This issue may happens when some mods are installed, such as BadOptimizations.
                                For more technical details, refer to: https://github.com/imthosea/BadOptimizations/issues/108"""
                );
            }
            appliedGetBiomeManagerWorkaround = true;
            return super.getBiomeManager();
        }
        return level.getBiomeManager();
    }
	
	@Override
	public boolean isClientSide() {
		return true;
	}
	
	@Nullable
	@Override
	public BlockEntity getBlockEntity(BlockPos pPos) {
		return this.refLevel.getBlockEntity(pPos);
	}
	
	@Override
	public BlockState getBlockState(BlockPos bPos) {
		return this.refLevel.getBlockState(bPos);
	}
	
	@Override
	public FluidState getFluidState(BlockPos pPos) {
		return this.refLevel.getFluidState(pPos);
	}
	
	@Override
	public List<Entity> getEntities(@Nullable Entity pEntity, AABB pArea, Predicate<? super Entity> pPredicate) {
		return this.refLevel.getEntities(pEntity, pArea, pPredicate);
	}
	
	@Override
	public <T extends Entity> List<T> getEntities(EntityTypeTest<Entity, T> pEntityTypeTest, AABB pBounds, Predicate<? super T> pPredicate) {
		return this.refLevel.getEntities(pEntityTypeTest, pBounds, pPredicate);
	}
	
	@Override
	public List<AbstractClientPlayer> players() {
		return this.refLevel.players();
	}

	/**
	 * Writer methods does nothing in general
	 */
	@Override
	public boolean setBlock(BlockPos pPos, BlockState pState, int pFlags, int pRecursionLeft) {
		return false;
	}
	
	@Override
	public boolean removeBlock(BlockPos pPos, boolean pIsMoving) {
		return false;
	}
	
	@Override
	public boolean destroyBlock(BlockPos pPos, boolean pDropBlock, @Nullable Entity pEntity, int pRecursionLeft) {
		return false;
	}

    public static class FakeClientPlayer extends AbstractClientPlayer {
        public FakeClientPlayer(FakeLevel fakeLevel, GameProfile gameProfile) {
            super(fakeLevel, gameProfile);
        }

        @Override
        public boolean isLocalPlayer() {
            return true;
        }
    }

    /* Due to the workaround specified in constructor of FakeLevel this class is no longer needed.
     * Maybe revert the change when significant issue is found
     *
	private static class FakeClientPacketListener extends ClientPacketListener {
		private static final Connection DUMMY_CONNECTION = new Connection(PacketFlow.CLIENTBOUND);
		
        public FakeClientPacketListener(ClientLevel refLevel, Minecraft minecraft) {
            super(
            	minecraft,
            	DUMMY_CONNECTION,
            	new CommonListenerCookie(
            		(GameProfile)null,//((yesman.epicfight.mixin.client.ClientLevelAccessor) refLevel).epicfight$getConnection().getLocalGameProfile(),
            		(WorldSessionTelemetryManager)null,//((yesman.epicfight.mixin.client.ClientLevelAccessor) refLevel).epicfight$getConnection().telemetryManager,
            		(RegistryAccess.Frozen)refLevel.registryAccess(),
            		refLevel.enabledFeatures(),
            		(String)null,
            		(ServerData)null,
            		(Screen)null,
            		Map.of(),
            		(ChatComponent.State)null,
            		false,
            		Map.of(),
            		(ServerLinks)null,
            		(ConnectionType)null//((yesman.epicfight.mixin.client.ClientLevelAccessor) refLevel).epicfight$getConnection().getConnectionType()
            	)
            );
        }
		
		@Override
		public void close() {
		}
	}
	*/
}