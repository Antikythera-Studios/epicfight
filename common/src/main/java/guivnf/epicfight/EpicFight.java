package guivnf.epicfight;

import com.mojang.brigadier.CommandDispatcher;
import com.yesman.akythera.api.world.patch.item.WeaponStance;
import com.yesman.akythera.core.animation.FSMAnimationState;
import guivnf.epicfight.akythera.datablocks.EpicfightFSMAnimationState;
import guivnf.epicfight.akythera.registries.EpicFightScriptTypes;
import guivnf.epicfight.world.item.datablock.EpicFightWeaponStance;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

public class EpicFight {
    public static final String MODID = "epicfight";

    /// Creates an identifier that points to an Epic Fight resource.
    public static @NonNull Identifier identifier(@NonNull final String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }

    /// Called on each mod loader's mod entry point class's constructor
    public static void onModConstructed() {
        EpicFightScriptTypes.REGISTRAR.notifyPresence();
    }

    public static void registerDataBlockEnumClasses() {
        WeaponStance.ENUM_MANAGER.registerEnumCls(MODID, EpicFightWeaponStance.class);
        FSMAnimationState.ENUM_MANAGER.registerEnumCls(MODID, EpicfightFSMAnimationState.class);
    }

    /// Called on mod loader lifecycle when the mod initializes in a dedicated server
    public static void onModInitializedInDedicatedServer() {
    }

    /// Register in-game commands
    public static void onRegisterCommands(final CommandDispatcher<CommandSourceStack> dispatcher) {
    }
}
