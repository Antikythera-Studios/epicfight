package yesman.epicfight.mixin.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(KeyMapping.class)
public interface KeyMappingAccessor {
    @Accessor("clickCount")
    int epicfight$getClickCount();

    @Accessor("clickCount")
    void epicfight$setClickCount(int value);

    @Accessor("key")
    InputConstants.Key epicfight$getKey();

    @Accessor("ALL")
    static Map<String, KeyMapping> epicfight$getAll() {
        throw new RuntimeException("Mixin stub");
    }

    @Accessor("MAP")
    static Map<InputConstants.Key, KeyMapping> epicfight$getMap() {
        throw new RuntimeException("Mixin stub");
    }
}
