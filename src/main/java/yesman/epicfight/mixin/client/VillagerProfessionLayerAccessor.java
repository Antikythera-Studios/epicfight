package yesman.epicfight.mixin.client;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import net.minecraft.client.renderer.entity.layers.VillagerProfessionLayer;
import net.minecraft.client.resources.metadata.animation.VillagerMetaDataSection;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/// Yarn mappings: typeHatCache is Object2ObjectMap<VillagerType, Hat>, professionHatCache is Object2ObjectMap<VillagerProfession, Hat>
@Mixin(VillagerProfessionLayer.class)
public interface VillagerProfessionLayerAccessor {
    @Accessor("typeHatCache")
    Object2ObjectMap<VillagerType, VillagerMetaDataSection.Hat> epicfight$getTypeHatCache();
    @Accessor("professionHatCache")
    Object2ObjectMap<VillagerProfession, VillagerMetaDataSection.Hat> epicfight$getProfessionHatCache();
}
