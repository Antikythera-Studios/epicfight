package yesman.epicfight.world.level.block;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import com.mojang.serialization.MapCodec;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import yesman.epicfight.api.extension.BlockStateExtension;

public class FractureBlockState extends BlockState {
	private Vector3f translate;
	private Quaternionf rotation;
	private double bouncing;
	private int maxLifeTime;
	private BlockState originalBlockState;
	private static final Int2ObjectMap<BlockState> ORIGINAL_BLOCK_STATE_CACHE = new Int2ObjectOpenHashMap<>();
	
	public static void remove(BlockPos blockPos) {
		ORIGINAL_BLOCK_STATE_CACHE.remove(blockPos.hashCode());
	}
	
	public static void reset() {
		ORIGINAL_BLOCK_STATE_CACHE.clear();
	}
	
	public FractureBlockState(Block block, Reference2ObjectArrayMap<Property<?>, Comparable<?>> propertyMap, MapCodec<BlockState> mapCodec) {
		super(block, propertyMap, mapCodec);
	}
	
	public void setFractureInfo(BlockPos bp, BlockState originalState, Vector3f translate, Quaternionf rotation, double bouncing, int maxLifeTime) {
		ORIGINAL_BLOCK_STATE_CACHE.put(bp.hashCode(), originalState);
		this.originalBlockState = originalState;
		this.translate = translate;
		this.rotation = rotation;
		this.bouncing = bouncing;
		this.maxLifeTime = maxLifeTime;
	}
	
	public Vector3f getTranslate() {
		return this.translate;
	}
	
	public Quaternionf getRotation() {
		return this.rotation;
	}
	
	public BlockState getOriginalBlockState(BlockPos blockPos) {
		return ORIGINAL_BLOCK_STATE_CACHE.get(blockPos.hashCode());
	}
	
	public double getBouncing() {
		return this.bouncing;
	}
	
	public int getLifeTime() {
		return this.maxLifeTime;
	}
	
	public boolean hasBlockEntity() {
		return true;
	}
	
	public boolean hidesNeighborFace(BlockGetter level, BlockPos pos, BlockState neighborState, Direction dir) {
		return false;
	}
	
	public boolean supportsExternalFaceHiding() {
		return false;
	}
	
		public int getLightEmission() {
		return this.originalBlockState != null ? this.originalBlockState.getLightEmission() : 0;
	}

	public int getLightEmission(BlockGetter level, BlockPos blockPos) {
		return this.originalBlockState != null ? BlockStateExtension.of(this.originalBlockState).epicfight$getLightEmission(level, blockPos) : 0;
	}
	
	public VoxelShape getShape(BlockGetter level, BlockPos blockPos) {
		return Shapes.empty();
	}
	
	public VoxelShape getShape(BlockGetter level, BlockPos blockPos, CollisionContext collisionContext) {
		return Shapes.empty();
	}
	
	public VoxelShape getCollisionShape(BlockGetter level, BlockPos blockPos) {
		BlockState blockState = this.getOriginalBlockState(blockPos);
		
		if (blockState == null) {
			return Shapes.empty();
		}
		
		return blockState.getCollisionShape(level, blockPos, CollisionContext.empty());
	}
	
	public VoxelShape getCollisionShape(BlockGetter level, BlockPos blockPos, CollisionContext collisionContext) {
		BlockState blockState = this.getOriginalBlockState(blockPos);
		
		if (blockState == null) {
			return Shapes.empty();
		}
		
		return blockState.getCollisionShape(level, blockPos, collisionContext);
	}
	
	public VoxelShape getVisualShape(BlockGetter level, BlockPos blockPos, CollisionContext collisionContext) {
		return Shapes.empty();
	}
	
	public RenderShape getRenderShape() {
		return RenderShape.INVISIBLE;
	}
}