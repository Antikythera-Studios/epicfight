package yesman.epicfight.main;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.Rarity;

import java.util.function.UnaryOperator;

public class EpicFightExtensibleEnums {

    /// The style modifier NeoForge's `EnumProxy` passes to the UNIQUE rarity.
    public static final UnaryOperator<Style> UNIQUE_STYLE = style -> style.withColor(ChatFormatting.GREEN);

    /// Aliased to [Rarity#EPIC] on Fabric — see [#initExtensibleEnums()].
    public static Rarity UNIQUE;

    /// NeoForge adds a green `epicfight:unique` rarity through its `EnumProxy` enum-extension
    /// mechanism. Fabric has no equivalent, and injecting the constant at runtime does not work
    /// here: `Rarity.<clinit>` snapshots `$VALUES` into `CODEC`, then into `BY_ID`
    /// (a fixed `ByIdMap.continuous` array over ids 0-3), then into `STREAM_CODEC`. Any constant
    /// added after `<clinit>` is missing from all three, so an item carrying it would fail to
    /// encode its `DataComponents.RARITY` when synced to the client.
    ///
    /// Nothing in Epic Fight — or in upstream NeoForge — actually reads UNIQUE, so it is aliased
    /// to the closest vanilla rarity instead of registering a constant that cannot round-trip.
    public static void initExtensibleEnums() {
        UNIQUE = Rarity.EPIC;
    }
}
