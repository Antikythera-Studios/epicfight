package yesman.epicfight.mixin.client;

import net.minecraft.client.renderer.OutlineBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/// Yarn mappings: teamR/G/B/A are int (not float)
@Mixin(OutlineBufferSource.class)
public interface OutlineBufferSourceAccessor {
    @Accessor("teamR")
    int epicfight$getTeamR();
    @Accessor("teamR")
    void epicfight$setTeamR(int value);
    @Accessor("teamG")
    int epicfight$getTeamG();
    @Accessor("teamG")
    void epicfight$setTeamG(int value);
    @Accessor("teamB")
    int epicfight$getTeamB();
    @Accessor("teamB")
    void epicfight$setTeamB(int value);
    @Accessor("teamA")
    int epicfight$getTeamA();
    @Accessor("teamA")
    void epicfight$setTeamA(int value);
}
