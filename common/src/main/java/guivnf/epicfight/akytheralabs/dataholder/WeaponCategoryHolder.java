package guivnf.epicfight.akytheralabs.dataholder;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.yesman.akythera.core.util.EnumerableDataBlockManager;
import com.yesman.akytheralabs.dataholder.DataBlockHolder;
import com.yesman.akytheralabs.editor.EditorEditable;
import com.yesman.akytheralabs.editor.EditorWidgetFactory;
import guivnf.epicfight.world.item.datablock.WeaponCategory;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.List;

/// [DataBlockHolder] for the `WeaponCategory` editable data block. See [WeaponStanceHolder] — same
/// shape, with the skill-screen icon item as its one property.
public final class WeaponCategoryHolder extends DataBlockHolder<WeaponCategory> {
    public static final Codec<WeaponCategoryHolder> CODEC = RecordCodecBuilder.create(
        instance -> instance.group(
            BuiltInRegistries.ITEM.byNameCodec().optionalFieldOf("icon_item", Items.AIR).forGetter(h -> h.iconItem)
        ).apply(instance, iconItem -> {
            WeaponCategoryHolder holder = new WeaponCategoryHolder();
            holder.iconItem = iconItem;
            return holder;
        })
    );

    @EditorEditable(label = "gui.epicfight.data_block.row.icon_item")
    private Item iconItem = Items.AIR;

    /// Written reflectively by [EditorWidgetFactory] when the item picker commits.
    public void setIconItem(Item iconItem) {
        this.iconItem = iconItem;
    }

    @Override
    public void readFrom(final WeaponCategory object) {
        iconItem = object.iconItem();
    }

    /// The icon resolves through the item registry rather than an `Items` constant, a project being
    /// free to point at a modded item. The generated constants are built by
    /// [EnumerableDataBlockManager#loadEnum], which both loaders call after the item registry is
    /// frozen.
    @Override
    public List<SourceProperty> sourceProperties() {
        Identifier id = BuiltInRegistries.ITEM.getKey(iconItem);

        return List.of(new SourceProperty("Item", "iconItem", "iconItem",
            "BuiltInRegistries.ITEM.getValue(Identifier.parse(\"" + id + "\"))"));
    }

    @Override
    protected WeaponCategory instantiate() {
        return new WeaponCategory() {
            @Override
            public int universalOrdinal() {
                return -1;
            }

            @Override
            public Item iconItem() {
                return iconItem;
            }

            @Override
            public Identifier identifier() {
                return currentId();
            }

            @Override
            public String toString() {
                return currentId().toString();
            }
        };
    }
}
