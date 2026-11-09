package guivnf.epicfight.client.akythera;

import com.yesman.akythera.api.data.modularize.ScriptFunction;
import com.yesman.akythera.api.world.patch.entity.EntityPatch;
import com.yesman.akythera.core.reflection.FunctionSignature;
import com.yesman.akythera.core.reflection.ScriptModularizeHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.ColorLerper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.animal.sheep.Sheep;

import java.util.List;
import java.util.function.Function;

public final class EpicFightClientScriptFunctions {
    private EpicFightClientScriptFunctions() {}

    public static ScriptFunction sheepWoolColor() {
        return ScriptModularizeHelper.makeCustomScript(
            (Function<EntityPatch<?>, Integer>) entityPatch -> {
                if (entityPatch == null || !(entityPatch.getOriginal() instanceof Sheep sheep)) return -1;

                Component customName = sheep.getCustomName();

                if (customName != null && "jeb_".equals(customName.getString())) {
                    float partialTick = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);

                    return ColorLerper.getLerpedColor(ColorLerper.Type.SHEEP, sheep.tickCount + partialTick);
                }

                return ColorLerper.Type.SHEEP.getColor(sheep.getColor());
            },
            FunctionSignature.create(Integer.class, List.of("entityPatch"), EntityPatch.class),
            "Wool Color",
            "Sheep",
            "The ARGB color the sheep's wool is tinted by, cycling for a sheep named jeb_. White for anything else",
            true,
            ScriptFunction.Usage.SCRIPT
        );
    }
}
