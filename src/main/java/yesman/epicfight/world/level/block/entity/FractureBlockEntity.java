package yesman.epicfight.world.level.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import yesman.epicfight.api.utils.side.ClientOnly;
import yesman.epicfight.registry.entries.EpicFightBlockEntities;
import yesman.epicfight.registry.entries.EpicFightParticles;
import yesman.epicfight.world.level.block.FractureBlockState;

public class FractureBlockEntity extends BlockEntity {
    private Vector3f translate;
    private Quaternionf rotation;
    private BlockState originalBlockState;
    private double bouncing;
    private int maxLifeTime;
    private int lifeTime = 0;

    public FractureBlockEntity(BlockPos blockPos, BlockState originalBlockState) {
        super(EpicFightBlockEntities.FRACTURE.get(), blockPos, originalBlockState);
    }

    public FractureBlockEntity(BlockPos blockPos, BlockState blockState, FractureBlockState fractureBlockState) {
        super(EpicFightBlockEntities.FRACTURE.get(), blockPos, blockState);

        this.originalBlockState = fractureBlockState.getOriginalBlockState(blockPos);
        this.bouncing = fractureBlockState.getBouncing();
        this.translate = fractureBlockState.getTranslate();
        this.rotation = fractureBlockState.getRotation();
        this.maxLifeTime = fractureBlockState.getLifeTime();
    }

    public BlockState getOriginalBlockState() {
        return this.originalBlockState;
    }

    public Vector3f getTranslate() {
        return this.translate;
    }

    public Quaternionf getRotation() {
        return this.rotation;
    }

    public double getBouncing() {
        return this.bouncing;
    }

    public int getMaxLifeTime() {
        return this.maxLifeTime;
    }

    public int getLifeTime() {
        return this.lifeTime;
    }

    public int increaseAndGetLifeTime() {
        return ++this.lifeTime;
    }

    @ClientOnly
    public static void lifeTimeTick(Level level, BlockPos blockPos, BlockState blockState, FractureBlockEntity fractureBlockEntity) {
        if (fractureBlockEntity.getOriginalBlockState().shouldSpawnTerrainParticles() && fractureBlockEntity.getMaxLifeTime() - fractureBlockEntity.getLifeTime() < 10) {
            level.addParticle(new BlockParticleOption(EpicFightParticles.GROUND_FRACTURE.get(), fractureBlockEntity.getOriginalBlockState()), blockPos.getX(), blockPos.getY(), blockPos.getZ(), 0.0, 0.0, 0.0);
        }

        if (fractureBlockEntity.increaseAndGetLifeTime() > fractureBlockEntity.getMaxLifeTime()) {
            level.removeBlockEntity(blockPos);
            FractureBlockState.remove(blockPos);
            level.setBlock(blockPos, fractureBlockEntity.getOriginalBlockState(), 0);
        }
    }
}