package yesman.epicfight.compat.controlify;
import yesman.epicfight.EpicFight;

import dev.isxander.controlify.api.ControlifyApi;
import dev.isxander.controlify.api.bind.ControlifyBindApi;
import dev.isxander.controlify.api.bind.InputBinding;
import dev.isxander.controlify.api.bind.InputBindingBuilder;
import dev.isxander.controlify.api.bind.InputBindingSupplier;
import dev.isxander.controlify.api.entrypoint.ControlifyEntrypoint;
import dev.isxander.controlify.api.entrypoint.InitContext;
import dev.isxander.controlify.api.entrypoint.PreInitContext;
import dev.isxander.controlify.api.event.ControlifyEvents;
import dev.isxander.controlify.api.guide.ContainerCtx;
import dev.isxander.controlify.api.guide.Fact;
import dev.isxander.controlify.api.guide.GuideDomainRegistry;
import dev.isxander.controlify.api.guide.InGameCtx;
import dev.isxander.controlify.bindings.ControlifyBindings;
import dev.isxander.controlify.bindings.RadialIcons;
import dev.isxander.controlify.controller.ControllerEntity;
import dev.isxander.controlify.screenop.ScreenProcessorProvider;
import dev.isxander.controlify.utils.render.Blit;
import dev.isxander.controlify.utils.render.CGuiPose;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;
import yesman.epicfight.api.client.camera.EpicFightCameraAPI;
import yesman.epicfight.api.client.input.InputMode;
import yesman.epicfight.api.client.input.action.EpicFightInputAction;
import yesman.epicfight.api.client.input.action.MinecraftInputAction;
import yesman.epicfight.api.client.input.controller.EpicFightControllerModProvider;
import yesman.epicfight.client.gui.screen.SkillBookScreen;
import yesman.epicfight.client.gui.screen.SkillEditScreen;
import yesman.epicfight.client.gui.screen.config.EpicFightSettingScreen;
import yesman.epicfight.client.input.EpicFightInputCategories;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;
import yesman.epicfight.compat.controlify.screenop.EpicFightSettingScreenProcessor;
import yesman.epicfight.compat.controlify.screenop.SkillBookScreenProcessor;
import yesman.epicfight.compat.controlify.screenop.SkillEditScreenProcessor;
import yesman.epicfight.generated.LangKeys;
import yesman.epicfight.skill.SkillCategories;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.item.CapabilityItem;

import java.util.Objects;
import java.util.Optional;

// Important for maintainers: Be careful when using Epic Fight classes here,
// as the Epic Fight mod might not be loaded yet. For example, avoid referencing
// EpicFightItems.UCHIGATANA.get() in onControlifyPreInit.
@ApiStatus.Internal
public class EpicFightControlifyEntrypoint implements ControlifyEntrypoint {
    public void onControllersDiscovered(ControlifyApi controlify) {
    }

    public void onControlifyInit(InitContext context) {
        // It's best to call this method in onControlifyInit,
        // ensuring that Epic Fight can use Controlify input bindings
        // only after they have been registered.
        registerModIntegration();
    }

    public void onControlifyPreInit(PreInitContext context) {
        final ControlifyBindApi registrar = ControlifyBindApi.get();
        registerCustomRadialIcons();
        EpicFightControlifyBindContexts.EpicFight.register(registrar);
        registerInputBindings(registrar);
        registerEvents();
        registerGuides(context.guideRegistries().inGame(), context.guideRegistries().container());
        registerScreenProcessors();
    }

    private static InputBindingSupplier attack;
    private static InputBindingSupplier mobility;
    private static InputBindingSupplier guard;
    private static InputBindingSupplier dodge;
    private static InputBindingSupplier switchMode;
    private static InputBindingSupplier weaponInnateSkill;
    private static InputBindingSupplier weaponInnateSkillTooltip;
    private static InputBindingSupplier openSkillEditorScreen;
    private static InputBindingSupplier openConfigScreen;
    private static InputBindingSupplier openEmoteWheelScreen;
    private static InputBindingSupplier switchVanillaModeDebugging;

    private static InputBindingSupplier lockOn;
    private static InputBindingSupplier lockOnShiftLeft;
    private static InputBindingSupplier lockOnShiftRight;
    private static InputBindingSupplier lockOnShiftFreely;

    private record TranslationKeys(@NotNull String name, @NotNull String description) {
        private @NotNull Component getNameComponent() {
            return Component.translatable(name());
        }

        private @NotNull Component getDescriptionComponent() {
            return Component.translatable(description());
        }

        /// Maps a non-vanilla [EpicFightInputAction] to its corresponding translation keys.
        ///
        /// @param action the non-vanilla action to get the translation key for
        /// @return a [TranslationKeys] instance containing the translation keys for the name and description
        private static @NotNull TranslationKeys fromAction(@NotNull EpicFightInputAction action) {
            return switch (action) {
                case ATTACK -> new TranslationKeys(LangKeys.KEY_ATTACK, LangKeys.KEY_ATTACK_DESCRIPTION);
                case DODGE -> new TranslationKeys(LangKeys.KEY_DODGE, LangKeys.KEY_DODGE_DESCRIPTION);
                case GUARD -> new TranslationKeys(LangKeys.KEY_GUARD, LangKeys.KEY_GUARD_DESCRIPTION);
                case LOCK_ON -> new TranslationKeys(LangKeys.KEY_LOCK_ON, LangKeys.KEY_LOCK_ON_DESCRIPTION);
                case LOCK_ON_SHIFT_LEFT ->
                        new TranslationKeys(LangKeys.KEY_LOCK_ON_SHIFT_LEFT, LangKeys.KEY_LOCK_ON_SHIFT_LEFT_DESCRIPTION);
                case LOCK_ON_SHIFT_RIGHT ->
                        new TranslationKeys(LangKeys.KEY_LOCK_ON_SHIFT_RIGHT, LangKeys.KEY_LOCK_ON_SHIFT_RIGHT_DESCRIPTION);
                case LOCK_ON_SHIFT_FREELY ->
                        new TranslationKeys(LangKeys.KEY_LOCK_ON_SHIFT_FREELY, LangKeys.KEY_LOCK_ON_SHIFT_FREELY_DESCRIPTION);
                case SWITCH_MODE -> new TranslationKeys(LangKeys.KEY_SWITCH_MODE, LangKeys.KEY_SWITCH_MODE_DESCRIPTION);
                case WEAPON_INNATE_SKILL ->
                        new TranslationKeys(LangKeys.KEY_WEAPON_INNATE_SKILL, LangKeys.KEY_WEAPON_INNATE_SKILL_DESCRIPTION);
                case WEAPON_INNATE_SKILL_TOOLTIP ->
                        new TranslationKeys(LangKeys.KEY_SHOW_TOOLTIP, LangKeys.KEY_SHOW_TOOLTIP_DESCRIPTION);
                case OPEN_SKILL_SCREEN ->
                        new TranslationKeys(LangKeys.KEY_SKILL_GUI, LangKeys.KEY_SKILL_GUI_DESCRIPTION);
                case OPEN_CONFIG_SCREEN -> new TranslationKeys(LangKeys.KEY_CONFIG, LangKeys.KEY_CONFIG_DESCRIPTION);
                case OPEN_EMOTE_WHEEL_SCREEN ->
                    new TranslationKeys(LangKeys.KEY_EMOTE, LangKeys.KEY_EMOTE_DESCRIPTION);
                case SWITCH_VANILLA_MODEL_DEBUGGING ->
                        new TranslationKeys(LangKeys.KEY_SWITCH_VANILLA_MODEL_DEBUG, LangKeys.KEY_SWITCH_VANILLA_MODEL_DEBUG_DESCRIPTION);
                case MOBILITY -> new TranslationKeys(LangKeys.KEY_MOVER_SKILL, LangKeys.KEY_MOVER_SKILL_DESCRIPTION);
            };
        }
    }

    private enum EpicFightRadialIcons {
        UCHIGATANA(EpicFight.identifier("textures/item/uchigatana_gui.png")),
        SKILL_BOOK(EpicFight.identifier("textures/item/skillbook.png"));

        private final @NotNull ResourceLocation id;

        EpicFightRadialIcons(@NotNull ResourceLocation id) {
            this.id = id;
        }

        public @NotNull ResourceLocation getId() {
            return id;
        }
    }

    private static void registerCustomRadialIcons() {
        for (EpicFightRadialIcons icon : EpicFightRadialIcons.values()) {
            final ResourceLocation location = icon.getId();

            // For consistency with the current Controlify radial icons,
            // this code is equivalent to:
            // https://github.com/isXander/Controlify/blob/f5c94c57d5e0d4954e413624a0d7ead937b6e8ab/src/main/java/dev/isxander/controlify/bindings/RadialIcons.java#L106-L112
            RadialIcons.registerIcon(location, (graphics, x, y, tickDelta) -> {
                var pose = CGuiPose.ofPush(graphics);
                pose.translate(x, y);
                pose.scale(0.5f, 0.5f);
                Blit.tex(graphics, location, 0, 0, 0, 0, 32, 32, 32, 32);
                pose.pop();
            });
        }
    }

    private static void registerInputBindings(ControlifyBindApi registrar) {
        for (EpicFightInputAction action : EpicFightInputAction.values()) {
            registerInputBinding(registrar, action);
        }
    }

    /// Registers a non-vanilla input binding with Controlify.
    ///
    /// Must **only** be called for non-vanilla [EpicFightInputAction].
    ///
    /// ### **Type-safety and exhaustive checking:**
    ///
    /// Returns an [InputBindingSupplier] via a *switch expression* over all enum constants.
    /// The returned value is a dummy, used only
    /// to satisfy the Java compiler and enforce exhaustive handling.
    /// It is **never used** and has no effect on behavior.
    ///
    /// @param registrar the Controlify API used to register the binding
    /// @param action    the non-vanilla input action to register
    /// @return a dummy [InputBindingSupplier] for type-safety only
    @SuppressWarnings("UnusedReturnValue") // Read Javadocs of this method before removing.
    private static @NotNull InputBindingSupplier registerInputBinding(
            @NotNull ControlifyBindApi registrar,
            @NotNull EpicFightInputAction action
    ) {
        final Component combatCategory = Component.translatable(EpicFightInputCategories.COMBAT);
        final Component guiCategory = Component.translatable(EpicFightInputCategories.GUI);
        final Component cameraCategory = Component.translatable(EpicFightInputCategories.CAMERA);
        final Component systemCategory = Component.translatable(EpicFightInputCategories.SYSTEM);

        // Using a switch expression to enforce compile-time exhaustive checking.
        // The returned value is a dummy and does nothing; its only purpose is to
        // satisfy the compiler and ensure all enum constants are handled.
        return switch (action) {
            case ATTACK -> attack = registrar.registerBinding(
                    builder -> applyCommonBindingProperties(action, builder)
                            .category(combatCategory)
                            .allowedContexts(EpicFightControlifyBindContexts.EpicFight.COMBAT_MODE)
            );
            case MOBILITY -> mobility = registrar.registerBinding(
                    builder -> applyCommonBindingProperties(action, builder)
                            .category(combatCategory)
                            .allowedContexts(EpicFightControlifyBindContexts.EpicFight.COMBAT_MODE)
            );
            case GUARD -> guard = registrar.registerBinding(
                    builder -> applyCommonBindingProperties(action, builder)
                            .category(combatCategory)
                            .allowedContexts(EpicFightControlifyBindContexts.EpicFight.COMBAT_MODE)
            );
            case DODGE -> dodge = registrar.registerBinding(
                    builder -> applyCommonBindingProperties(action, builder)
                            .category(combatCategory)
                            .allowedContexts(EpicFightControlifyBindContexts.EpicFight.COMBAT_MODE)
            );
            case LOCK_ON -> lockOn = registrar.registerBinding(
                    builder -> applyCommonBindingProperties(action, builder)
                            .category(cameraCategory)
                            .allowedContexts(EpicFightControlifyBindContexts.EpicFight.COMBAT_MODE)
            );
            case LOCK_ON_SHIFT_LEFT -> lockOnShiftLeft = registrar.registerBinding(
                    builder -> applyCommonBindingProperties(action, builder)
                            .category(cameraCategory)
                            .allowedContexts(EpicFightControlifyBindContexts.EpicFight.LOCK_ON)
            );
            case LOCK_ON_SHIFT_RIGHT -> lockOnShiftRight = registrar.registerBinding(
                    builder -> applyCommonBindingProperties(action, builder)
                            .category(cameraCategory)
                            .allowedContexts(EpicFightControlifyBindContexts.EpicFight.LOCK_ON)
            );
            case LOCK_ON_SHIFT_FREELY -> lockOnShiftFreely = registrar.registerBinding(
                    builder -> applyCommonBindingProperties(action, builder)
                            .category(cameraCategory)
                            .allowedContexts(EpicFightControlifyBindContexts.EpicFight.LOCK_ON)
            );
            case SWITCH_MODE -> switchMode = registrar.registerBinding(
                    builder -> applyCommonBindingProperties(action, builder)
                            .category(systemCategory)
                            .allowedContexts(EpicFightControlifyBindContexts.IN_GAME)
                            .radialCandidate(EpicFightRadialIcons.UCHIGATANA.getId())
            );
            case WEAPON_INNATE_SKILL -> weaponInnateSkill = registrar.registerBinding(
                    builder -> applyCommonBindingProperties(action, builder)
                            .category(combatCategory)
                            .allowedContexts(EpicFightControlifyBindContexts.EpicFight.COMBAT_MODE)
            );
            case WEAPON_INNATE_SKILL_TOOLTIP -> weaponInnateSkillTooltip = registrar.registerBinding(
                    builder -> applyCommonBindingProperties(action, builder)
                            .category(guiCategory)
                            .allowedContexts(EpicFightControlifyBindContexts.ANY_SCREEN)
            );
            case OPEN_SKILL_SCREEN -> openSkillEditorScreen = registrar.registerBinding(
                    builder -> applyCommonBindingProperties(action, builder)
                            .category(guiCategory)
                            .allowedContexts(EpicFightControlifyBindContexts.IN_GAME)
                            .radialCandidate(EpicFightRadialIcons.SKILL_BOOK.getId())
            );
            case OPEN_CONFIG_SCREEN -> openConfigScreen = registrar.registerBinding(
                    builder -> applyCommonBindingProperties(action, builder)
                            .category(guiCategory)
                            .allowedContexts(EpicFightControlifyBindContexts.IN_GAME)
                            .radialCandidate(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(Items.REDSTONE))
            );
            case OPEN_EMOTE_WHEEL_SCREEN -> openEmoteWheelScreen = registrar.registerBinding(
                    builder -> applyCommonBindingProperties(action, builder)
                            .category(guiCategory)
                            .allowedContexts(EpicFightControlifyBindContexts.IN_GAME)
            );
            case SWITCH_VANILLA_MODEL_DEBUGGING -> switchVanillaModeDebugging = registrar.registerBinding(
                    builder -> applyCommonBindingProperties(action, builder)
                            .category(systemCategory)
                            .allowedContexts(EpicFightControlifyBindContexts.IN_GAME)
            );
        };
    }

    private static @NotNull InputBindingBuilder applyCommonBindingProperties(
            @NotNull EpicFightInputAction action,
            @NotNull InputBindingBuilder builder
    ) {
        final TranslationKeys translationKeys = TranslationKeys.fromAction(action);
        final KeyMapping keyMappingToIgnore = action.keyMapping();
        return builder
                .id(getBindingId(action))
                .name(translationKeys.getNameComponent())
                .description(translationKeys.getDescriptionComponent())
                // Prevents Controlify from auto-registering controller bindings for Epic Fight's
                // vanilla key mappings, since Epic Fight already provides explicit native support.
                .addKeyCorrelation(keyMappingToIgnore);
    }

    private static @NotNull ResourceLocation getBindingId(@NotNull EpicFightInputAction action) {
        final String path = switch (action) {
            // Project maintainers: if you change any ID (e.g., "attack"), update assets/controlify too.
            case ATTACK -> "attack";
            case MOBILITY -> "mobility";
            case GUARD -> "guard";
            case DODGE -> "dodge";
            case LOCK_ON -> "lock_on";
            case LOCK_ON_SHIFT_LEFT -> "lock_on_shift_left";
            case LOCK_ON_SHIFT_RIGHT -> "lock_on_shift_right";
            case LOCK_ON_SHIFT_FREELY -> "lock_on_shift_freely";
            case SWITCH_MODE -> "switch_mode";
            case WEAPON_INNATE_SKILL -> "weapon_innate_skill";
            case WEAPON_INNATE_SKILL_TOOLTIP -> "weapon_innate_skill_tooltip";
            case OPEN_SKILL_SCREEN -> "open_skill_editor_screen";
            case OPEN_CONFIG_SCREEN -> "open_config_screen";
            case OPEN_EMOTE_WHEEL_SCREEN -> "open_emote_wheel_screen";
            case SWITCH_VANILLA_MODEL_DEBUGGING -> "switch_vanilla_mode_debugging";
        };
        return EpicFight.identifier(path);
    }

    private static void registerModIntegration() {
        EpicFightControllerModProvider.set(EpicFight.MODID, new EpicFightControlifyControllerMod());
    }

    private static void registerEvents() {
        // Registers a look-input modifier so Epic Fight's camera can take over
        // the player's look input when in Epic Fight combat mode.
        // Note: Controlify is currently disabled as a dependency (malformed access widener),
        // so the stub's register() is a no-op. When Controlify is re-enabled, this will
        // function identically to the NeoForge version.
        ControlifyEvents.LOOK_INPUT_MODIFIER.register(event -> {
            // Workaround: Since these values are normalized
            // (e.g., x = -10 with default sensitivity or -20 when sensitivity is maxed),
            // while mouse values are not normalized (e.g., around 110.00000983476669),
            // handle the difference by scaling the values by 10.
            final double multiplier = 10;

            final Vector2f lookInput = event.lookInput();
            final double dy = lookInput.x * multiplier;
            final double dx = lookInput.y * multiplier;

            if (EpicFightCameraAPI.getInstance().turnCamera(dy, dx)) {
                lookInput.zero();
            }
        });
    }

    private static void registerGuides(GuideDomainRegistry<InGameCtx> inGameRegistry, GuideDomainRegistry<ContainerCtx> containerRegistry) {
        // Facts are registered here; rules in "assets/controlify/guides/in_game.json" reference these facts.
        inGameRegistry.registerFact(new Fact<>(EpicFight.identifier("can_perform_dodge"), ctx -> {
            final LocalPlayerPatch localPlayerPatch = EpicFightCapabilities.ClientModule.getCachedLocalPlayerPatch();
            if (localPlayerPatch == null || !localPlayerPatch.isEpicFightMode()) {
                return false;
            }
            return localPlayerPatch.getPlayerSkills().hasCategory(SkillCategories.DODGE);
        }));
        containerRegistry.registerFact(new Fact<>(EpicFight.identifier("can_show_weapon_innate_skill_tooltip"), ctx -> {
            final Slot hoveredSlot = ctx.hoveredSlot();
            if (hoveredSlot == null || !ctx.hoveredSlot().hasItem()) {
                return false;
            }
            final Optional<CapabilityItem> maybeCapabilityItem = EpicFightCapabilities.getItemCapability(hoveredSlot.getItem());
            return maybeCapabilityItem.isPresent();
        }));
    }

    public static @NotNull InputBinding getControlifyBinding(@NotNull EpicFightInputAction action) {
        final InputBindingSupplier bindingSupplier = switch (action) {
            case ATTACK -> attack;
            case MOBILITY -> mobility;
            case GUARD -> guard;
            case DODGE -> dodge;
            case LOCK_ON -> lockOn;
            case LOCK_ON_SHIFT_LEFT -> lockOnShiftLeft;
            case LOCK_ON_SHIFT_RIGHT -> lockOnShiftRight;
            case LOCK_ON_SHIFT_FREELY -> lockOnShiftFreely;
            case SWITCH_MODE -> switchMode;
            case WEAPON_INNATE_SKILL -> weaponInnateSkill;
            case WEAPON_INNATE_SKILL_TOOLTIP -> weaponInnateSkillTooltip;
            case OPEN_SKILL_SCREEN -> openSkillEditorScreen;
            case OPEN_CONFIG_SCREEN -> openConfigScreen;
            case OPEN_EMOTE_WHEEL_SCREEN -> openEmoteWheelScreen;
            case SWITCH_VANILLA_MODEL_DEBUGGING -> switchVanillaModeDebugging;
        };
        final @Nullable InputBinding binding = bindingSupplier.onOrNull(requireControllerEntity());
        return Objects.requireNonNull(binding, "The binding for the action " + action.name() + " is not yet registered.");
    }

    public static @NotNull InputBinding getControlifyBinding(@NotNull MinecraftInputAction action) {
        final InputBindingSupplier bindingSupplier = switch (action) {
            case ATTACK_DESTROY -> ControlifyBindings.ATTACK;
            case MOVE_FORWARD -> ControlifyBindings.WALK_FORWARD;
            case MOVE_BACKWARD -> ControlifyBindings.WALK_BACKWARD;
            case MOVE_LEFT -> ControlifyBindings.WALK_LEFT;
            case MOVE_RIGHT -> ControlifyBindings.WALK_RIGHT;
            case SPRINT -> ControlifyBindings.SPRINT;
            case SNEAK -> ControlifyBindings.SNEAK;
            case USE -> ControlifyBindings.USE;
            case SWAP_OFF_HAND -> ControlifyBindings.SWAP_HANDS;
            case DROP -> ControlifyBindings.DROP_INGAME;
            case TOGGLE_PERSPECTIVE -> ControlifyBindings.CHANGE_PERSPECTIVE;
            case JUMP -> ControlifyBindings.JUMP;
        };
        final @Nullable InputBinding binding = bindingSupplier.onOrNull(requireControllerEntity());
        return Objects.requireNonNull(binding, "The binding for the action " + action.name() + " is not yet registered.");
    }

    public static @NotNull ControlifyApi getApi() {
        return ControlifyApi.get();
    }

    public static @NotNull ControllerEntity requireControllerEntity() {
        Optional<ControllerEntity> optionalControllerEntity = getApi().getCurrentController();

        if (optionalControllerEntity.isEmpty()) {
            final String message = String.format(
                    "The method IEpicFightControllerMod#getInputState must not be called when the input mode is not %s",
                    InputMode.CONTROLLER.name()
            );
            EpicFight.LOGGER.error(message);
            throw new IllegalStateException(message);
        }

        return optionalControllerEntity.get();
    }

    private static void registerScreenProcessors() {
        ScreenProcessorProvider.registerProvider(
                SkillEditScreen.class,
                SkillEditScreenProcessor::new
        );
        ScreenProcessorProvider.registerProvider(
                SkillBookScreen.class,
                SkillBookScreenProcessor::new
        );
        ScreenProcessorProvider.registerProvider(
                EpicFightSettingScreen.class,
                EpicFightSettingScreenProcessor::new
        );
    }
}
