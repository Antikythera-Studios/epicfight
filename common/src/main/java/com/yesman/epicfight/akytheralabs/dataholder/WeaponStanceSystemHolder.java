package com.yesman.epicfight.akytheralabs.dataholder;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.yesman.akythera.api.component.PatchComponent;
import com.yesman.akythera.api.data.modularize.node.PureNode;
import com.yesman.akythera.api.world.patch.entity.EntityPatch;
import com.yesman.akythera.api.world.patch.item.ItemPatch;
import com.yesman.akythera.core.util.EnumerableDataBlock;
import com.yesman.akytheralabs.NodeTreeSourceRenderer;
import com.yesman.akytheralabs.akythera.animation.ConditionGraphCompiler;
import com.yesman.akytheralabs.akythera.entitypatch.component.ComponentDataHolder;
import com.yesman.akytheralabs.akythera.entitypatch.component.RootAttachableComponent;
import com.yesman.akytheralabs.dataholder.EditorReference;
import com.yesman.akytheralabs.editor.EditorEditable;
import com.yesman.akytheralabs.editor.EditorNodeGraph;
import com.yesman.akytheralabs.editor.NodeGraphTitleProvider;
import com.yesman.akytheralabs.gui.widget.script.ScriptGraphData;
import com.yesman.epicfight.world.item.component.EpicFightWeaponProperties;
import com.yesman.epicfight.world.item.component.WeaponStanceSystem;
import com.yesman.epicfight.world.item.datablock.WeaponStance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/// Editor holder for [WeaponStanceSystem]: the default stance (data block combo) with its
/// [EpicFightWeaponPropertiesHolder] embedded right below as a nested editor block, plus a
/// "Conditional Stances" list whose elements pair a stance, its condition canvas, **and** its
/// properties block — a stance and its properties are one set, added and removed together.
///
/// Conditional stance **order is priority order** (first matching condition wins at runtime) —
/// the list order is preserved into the compiled `LinkedHashMap`. The default block's properties
/// compile first, so a conditional block sharing the default stance overrides them.
public final class WeaponStanceSystemHolder extends ComponentDataHolder<WeaponStanceSystem>
    implements RootAttachableComponent
{
    /// The context local [WeaponStanceSystem#getCurrentStance] binds before executing a condition.
    /// Referenced by [ConditionalStanceHolder]'s [EditorNodeGraph] declaration — must stay a
    /// compile-time constant.
    static final String EQUIPPER_PARAM = "equipper";

    public static final MapCodec<WeaponStanceSystemHolder> EDITOR_CODEC = RecordCodecBuilder.mapCodec(
        instance -> instance.group(
            WeaponStance.ENUM_MANAGER.getNameCodec().fieldOf("default_stance").forGetter(h -> h.defaultStance),
            EpicFightWeaponPropertiesHolder.CODEC.optionalFieldOf("default_properties")
                .forGetter(h -> Optional.of(h.defaultProperties)),
            ConditionalStanceHolder.CODEC.listOf().optionalFieldOf("conditional_stances", List.of())
                .forGetter(h -> h.conditionalStances)
        ).apply(instance, (defaultStance, defaultProperties, conditions) -> {
            WeaponStanceSystemHolder h = new WeaponStanceSystemHolder();
            h.defaultStance = defaultStance;
            h.defaultProperties = defaultProperties.orElseGet(EpicFightWeaponPropertiesHolder::createDefault);
            h.conditionalStances.addAll(conditions);
            return h;
        })
    );

    @EditorEditable(label = "gui.epicfight.item_patch_screen.row.default_stance", order = 0)
    private WeaponStance defaultStance = firstStance();

    /// Nested editor block ([EpicFightWeaponPropertiesHolder] has no widget producer but declares
    /// its own rows) — renders indented under a header, its own list rows included.
    @EditorEditable(label = "gui.epicfight.item_patch_screen.row.default_properties", order = 1)
    private EpicFightWeaponPropertiesHolder defaultProperties = EpicFightWeaponPropertiesHolder.createDefault();

    /// Live list the Details panel's "Conditional Stances" row mutates.
    @EditorEditable(
        label = "gui.epicfight.item_patch_screen.section.conditional_stances",
        defaultElement = "defaultConditionalStance",
        order = 2
    )
    private final List<ConditionalStanceHolder> conditionalStances = new ArrayList<>();

    public static WeaponStanceSystemHolder createDefault() {
        return new WeaponStanceSystemHolder();
    }

    /// ⚠ Requires at least one weapon stance to be loaded (`EpicFight.onModInitialized`).
    private static WeaponStance firstStance() {
        return WeaponStance.ENUM_MANAGER.universalValues().iterator().next();
    }

    /// The `+` button's fresh block, see [EditorEditable#defaultElement].
    static ConditionalStanceHolder defaultConditionalStance() {
        return new ConditionalStanceHolder(
            firstStance(), ScriptGraphData.EMPTY, EpicFightWeaponPropertiesHolder.createDefault()
        );
    }

    @Override
    public Stream<EditorReference> referencedReferences() {
        return Stream.concat(
            defaultProperties.referencedReferences(),
            conditionalStances.stream().flatMap(d -> d.properties.referencedReferences()));
    }

    @Override
    public void readFrom(WeaponStanceSystem object) {
        Map<WeaponStance, EpicFightWeaponProperties> properties = object.weaponProperties();

        defaultStance = object.defaultStance();
        defaultProperties = holderOf(properties.get(defaultStance));

        // Runtime PureNode trees cannot be decompiled back into editor canvases (same limitation
        // as the entity-patch script surface): stances import with empty condition graphs. And
        // since every properties entry lives inside a stance block here, properties for stances
        // that are neither the default nor conditional have no block to land in and are dropped.
        conditionalStances.clear();
        for (Map.Entry<WeaponStance, PureNode> entry : object.conditionalStances().entrySet()) {
            conditionalStances.add(new ConditionalStanceHolder(
                entry.getKey(), ScriptGraphData.EMPTY, holderOf(properties.get(entry.getKey()))));
        }
    }

    private static EpicFightWeaponPropertiesHolder holderOf(EpicFightWeaponProperties properties) {
        return properties == null
            ? EpicFightWeaponPropertiesHolder.createDefault()
            : EpicFightWeaponPropertiesHolder.from(properties);
    }

    @Override
    public WeaponStanceSystem makeObject() {
        Map<WeaponStance, PureNode> conditions = new LinkedHashMap<>();
        Map<WeaponStance, EpicFightWeaponProperties> properties = new LinkedHashMap<>();

        // Default block first — a conditional block for the same stance overrides its properties.
        properties.put(defaultStance, defaultProperties.makeProperties());

        for (ConditionalStanceHolder stance : conditionalStances) {
            conditions.put(stance.stance, ConditionGraphCompiler.compile(stance.condition, ItemPatch.class, EQUIPPER_PARAM));
            properties.put(stance.stance, stance.properties.makeProperties());
        }

        return new WeaponStanceSystem(conditions, defaultStance, properties);
    }

    /// [WeaponStanceSystem] is its own blueprint — it builds itself.
    @Override
    public Class<? extends PatchComponent> generateEventually() {
        return WeaponStanceSystem.class;
    }

    // ***************************************************************
    // Source export (File > Export > As Source Code)
    // ***************************************************************

    @Override
    public String sourceTemplate() {
        return "components/weapon_stance_system.ftl";
    }

    /// Tokens for `components/weapon_stance_system.ftl`. Conditions render through the compiled
    /// runtime tree ([NodeTreeSourceRenderer#pure]) — the same lowering `makeObject` performs.
    /// The properties list is the default block followed by the conditional blocks; stances
    /// duplicated across blocks would collide in the exported `ImmutableMap` (a compile-time
    /// error in the exported source, not an export failure).
    @Override
    public Map<String, Object> getDTO() {
        List<Map<String, Object>> conditionRows = conditionalStances.stream()
            .<Map<String, Object>>map(d -> Map.of(
                "stance", identifierOf(d.stance),
                "source", NodeTreeSourceRenderer.pure(
                    ConditionGraphCompiler.compile(d.condition, ItemPatch.class, EQUIPPER_PARAM), 5)
            ))
            .toList();

        List<Map<String, Object>> propertyRows = new ArrayList<>();
        propertyRows.add(propertyRow(defaultStance, defaultProperties));
        conditionalStances.forEach(d -> propertyRows.add(propertyRow(d.stance, d.properties)));

        return Map.of(
            "default_stance", identifierOf(defaultStance),
            "conditions", conditionRows,
            "properties", propertyRows
        );
    }

    private static Map<String, Object> propertyRow(WeaponStance stance, EpicFightWeaponPropertiesHolder d) {
        return Map.of(
            "stance", identifierOf(stance),
            "categories", d.categories.stream().map(WeaponStanceSystemHolder::identifierOf).toList(),
            "combo", d.comboAttacks.stream().map(EpicFightWeaponPropertiesHolder::montageId).toList(),
            "dash", d.dashIdForExport(),
            "air_slash", d.airSlashIdForExport(),
            "swing_sound", d.swingSoundIdForExport(),
            "hit_sound", d.hitSoundIdForExport(),
            "hit_particle", d.hitParticleIdForExport(),
            "offhand", d.equippableInOffhandForExport()
        );
    }

    private static String identifierOf(EnumerableDataBlock block) {
        Identifier id = block.getEnumManager().derefIdentifier(block);
        return id == null ? "" : id.toString();
    }

    /// One conditional stance block: the stance (data block combo), its boolean condition canvas —
    /// a raw [ScriptGraphData] field (the [com.yesman.akytheralabs.akythera.animation.AnimatorScriptHolder]
    /// storage convention) whose [EditorNodeGraph] declaration renders the "Edit Node Graph"
    /// button → center tab — and the stance's properties as a nested editor block. The graph tab
    /// titles itself "&lt;stance&gt; condition" through [NodeGraphTitleProvider].
    public static final class ConditionalStanceHolder implements NodeGraphTitleProvider {
        static final Codec<ConditionalStanceHolder> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                WeaponStance.ENUM_MANAGER.getNameCodec().fieldOf("stance").forGetter(d -> d.stance),
                ScriptGraphData.CODEC.optionalFieldOf("condition", ScriptGraphData.EMPTY).forGetter(d -> d.condition),
                EpicFightWeaponPropertiesHolder.CODEC.optionalFieldOf("properties")
                    .forGetter(d -> Optional.of(d.properties))
            ).apply(instance, (stance, condition, properties) -> new ConditionalStanceHolder(
                stance, condition, properties.orElseGet(EpicFightWeaponPropertiesHolder::createDefault)))
        );

        @EditorEditable(label = "gui.epicfight.item_patch_screen.row.stance", order = 0)
        private WeaponStance stance;

        @EditorEditable(label = "gui.epicfight.item_patch_screen.row.condition", order = 1)
        @EditorNodeGraph(contextParams = EQUIPPER_PARAM, contextParamTypes = EntityPatch.class)
        private ScriptGraphData condition;

        @EditorEditable(label = "gui.epicfight.item_patch_screen.row.properties", order = 2)
        private EpicFightWeaponPropertiesHolder properties;

        ConditionalStanceHolder(WeaponStance stance, ScriptGraphData condition, EpicFightWeaponPropertiesHolder properties) {
            this.stance = stance;
            this.condition = condition;
            this.properties = properties;
        }

        @Override
        public Component nodeGraphTabTitle(String fieldName, Component fallback) {
            Identifier id = WeaponStance.ENUM_MANAGER.derefIdentifier(stance);
            return Component.literal(id == null ? "?" : id.getPath() + " condition");
        }
    }
}
