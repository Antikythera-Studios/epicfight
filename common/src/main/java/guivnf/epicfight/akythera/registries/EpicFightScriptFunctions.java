package guivnf.epicfight.akythera.registries;

import com.yesman.akythera.api.data.modularize.ScriptFunction;
import com.yesman.akythera.api.world.ability.AbilityResourceRef;
import com.yesman.akythera.api.world.ability.StoredAbilityResource;
import com.yesman.akythera.api.world.patch.entity.EntityPatch;
import com.yesman.akythera.api.world.patch.entity.LivingEntityPatch;
import com.yesman.akythera.core.reflection.FunctionSignature;
import com.yesman.akythera.core.reflection.ScriptModularizeHelper;
import com.yesman.akythera.core.registry.AkytheraRegistries;
import com.yesman.akythera.core.util.helper.MobUtils;
import com.yesman.akythera.platform.ModPlatform;
import com.yesman.akythera.platform.ModPlatformProvider;
import guivnf.epicfight.EpicFight;
import guivnf.epicfight.client.akythera.EpicFightClientScriptFunctions;
import guivnf.epicfight.akythera.providers.EpicfightAbilityResources;
import guivnf.epicfight.world.entity.ai.ResponsiveBodyRotationControl;
import guivnf.epicfight.world.entity.ai.SmoothMoveControl;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.sheep.Sheep;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public final class EpicFightScriptFunctions {
    private EpicFightScriptFunctions() {}

    public static final ModPlatform.Registrar<ScriptFunction> REGISTRAR =
        ModPlatformProvider.get().getRegistrar(AkytheraRegistries.Frozen.SCRIPT_FUNCTION_KEY, EpicFight.MODID);

    public static final Holder<ScriptFunction> SHEEP_IS_SHEARED = REGISTRAR.register("sheep_is_sheared", () ->
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

    public static final Holder<ScriptFunction> SHEEP_HEAD_EAT_POSITION_SCALE = REGISTRAR.register("sheep_head_eat_position_scale", () ->
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

    public static final Holder<ScriptFunction> STAMINA_RATIO = REGISTRAR.register("stamina_ratio", () ->
        ScriptModularizeHelper.makeCustomScript(
            (Function<EntityPatch<?>, Float>) entityPatch -> {
                StoredAbilityResource stamina = entityPatch == null
                    ? null
                    : StoredAbilityResource.get(new AbilityResourceRef.ByData(EpicfightAbilityResources.STAMINA));

                return entityPatch instanceof LivingEntityPatch<?> living && stamina != null && stamina.getMax() > 0.0F
                    ? stamina.read(living) / stamina.getMax()
                    : 0.0F;
            },
            FunctionSignature.create(Float.class, List.of("entityPatch"), EntityPatch.class),
            "Stamina Ratio",
            "Ability",
            "The share of its stamina the entity holds, from 0 to 1. 0 for an entity without an ability system",
            true,
            ScriptFunction.Usage.SCRIPT
        )
    );

    public static final Holder<ScriptFunction> SMOOTH_LOCOMOTION = REGISTRAR.register("smooth_locomotion", () ->
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

    public static final Holder<ScriptFunction> SHEEP_WOOL_COLOR = REGISTRAR.register("sheep_wool_color", () ->
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
