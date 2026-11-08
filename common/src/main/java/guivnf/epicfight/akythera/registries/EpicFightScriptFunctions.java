package guivnf.epicfight.akythera.registries;

import com.yesman.akythera.api.data.modularize.ScriptFunction;
import com.yesman.akythera.api.world.patch.entity.EntityPatch;
import com.yesman.akythera.core.reflection.FunctionSignature;
import com.yesman.akythera.core.reflection.ScriptModularizeHelper;
import com.yesman.akythera.core.util.helper.MobUtils;
import com.yesman.akythera.platform.ModPlatformProvider;
import guivnf.epicfight.client.akythera.EpicFightClientScriptFunctions;
import guivnf.epicfight.world.entity.ai.ResponsiveBodyRotationControl;
import guivnf.epicfight.world.entity.ai.SmoothMoveControl;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.sheep.Sheep;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public final class EpicFightScriptFunctions {
    private EpicFightScriptFunctions() {}

    public static void register(final BiConsumer<String, Supplier<ScriptFunction>> registry) {
        registry.accept("sheep_is_sheared", () ->
            ScriptModularizeHelper.makeCustomScript(
                (Function<EntityPatch<?>, Boolean>) entityPatch -> entityPatch != null && entityPatch.getOriginal() instanceof Sheep sheep && sheep.isSheared(),
                FunctionSignature.create(Boolean.class, List.of("entityPatch"), EntityPatch.class),
                "Is Sheared",
                "Sheep",
                "Whether the sheep is sheared. False for anything else",
                true,
                ScriptFunction.Usage.SCRIPT
            )
        );

        registry.accept("sheep_head_eat_position_scale", () ->
            ScriptModularizeHelper.makeCustomScript(
                (BiFunction<EntityPatch<?>, Float, Float>) (entityPatch, partialTick) ->
                    entityPatch != null && entityPatch.getOriginal() instanceof Sheep sheep ? sheep.getHeadEatPositionScale(partialTick == null ? 0.0F : partialTick) : 0.0F,
                FunctionSignature.create(Float.class, List.of("entityPatch", "partialTick"), EntityPatch.class, Float.class),
                "Head Eat Position Scale",
                "Sheep",
                "How far the sheep's head is lowered to eat grass, from 0 to 1",
                true,
                ScriptFunction.Usage.SCRIPT
            )
        );

        registry.accept("smooth_locomotion", () ->
            ScriptModularizeHelper.makeCustomScript(
                (Consumer<EntityPatch<?>>) entityPatch -> {
                    if (entityPatch != null && entityPatch.getOriginal() instanceof Mob mob) {
                        MobUtils.setMoveControl(mob, new SmoothMoveControl(mob));
                        MobUtils.setBodyRotationControl(mob, new ResponsiveBodyRotationControl(mob));
                    }
                },
                FunctionSignature.create(Void.class, List.of("entityPatch"), EntityPatch.class),
                "Smooth Locomotion",
                "Entity",
                "Makes the mob follow its path along a smooth curve at a natural turning speed, and turn its body toward its head while standing",
                false,
                ScriptFunction.Usage.SCRIPT
            )
        );

        registry.accept("sheep_wool_color", () ->
            ModPlatformProvider.get().getPhysicalSide().isClient()
                ? EpicFightClientScriptFunctions.sheepWoolColor()
                : ScriptModularizeHelper.makeCustomScript(
                    (Function<EntityPatch<?>, Integer>) entityPatch -> -1,
                    FunctionSignature.create(Integer.class, List.of("entityPatch"), EntityPatch.class),
                    "Wool Color",
                    "Sheep",
                    "The ARGB color the sheep's wool is tinted by",
                    true,
                    ScriptFunction.Usage.SCRIPT
                )
        );
    }
}
