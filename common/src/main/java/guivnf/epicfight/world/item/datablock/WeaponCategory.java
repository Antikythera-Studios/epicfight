package guivnf.epicfight.world.item.datablock;

import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.yesman.akythera.core.util.EnumerableDataBlock;
import com.yesman.akythera.core.util.EnumerableDataBlockManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

public interface WeaponCategory extends EnumerableDataBlock {
    EnumerableDataBlockManager<WeaponCategory> ENUM_MANAGER = new EnumerableDataBlockManager<>(
        "weapon_category",
        RecordCodecBuilder.mapCodec(
            instance ->
                instance.group(
                    BuiltInRegistries.ITEM.byNameCodec().fieldOf("icon_item").forGetter(WeaponCategory::iconItem)
                )
                .apply(instance, iconItem -> new WeaponCategory() {
                    @Override
                    public int universalOrdinal() {
                        return ENUM_MANAGER.derefOrdinal(this);
                    }

                    @Override
                    public Item iconItem() {
                        return iconItem;
                    }

                    @Override
                    public String toString() {
                        Identifier enumName = ENUM_MANAGER.derefIdentifier(this);
                        return enumName == null ? "WeaponCategory" : enumName.toString();
                    }
                })
        )
    );

    /// An item to shown on skill description screen
    Item iconItem();

    @Override
    default EnumerableDataBlockManager<WeaponCategory> getEnumManager() {
        return ENUM_MANAGER;
    }
}
