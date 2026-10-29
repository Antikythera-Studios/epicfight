package yesman.epicfight.client.gui.screen.config;
import yesman.epicfight.EpicFight;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.components.tabs.Tab;
import net.minecraft.client.gui.components.tabs.TabManager;
import net.minecraft.client.gui.components.tabs.TabNavigationBar;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;

import net.fabricmc.loader.api.FabricLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import yesman.epicfight.api.client.model.transformer.HumanoidModelBaker;
import yesman.epicfight.client.gui.datapack.screen.MessageScreen;
import yesman.epicfight.client.gui.screen.SideBarScreen;
import yesman.epicfight.client.gui.widgets.*;
import yesman.epicfight.client.gui.widgets.common.AnchoredWidget;
import yesman.epicfight.config.ClientConfig;
import yesman.epicfight.config.ClientConfig.HealthBarVisibility;
import yesman.epicfight.main.EpicFightMod;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static yesman.epicfight.generated.LangKeys.*;

public class EpicFightSettingScreen extends Screen {
    @Nullable
    private final Screen parentScreen;
    private final TabManager tabManager = new TabManager(this::addRenderableWidget, this::removeWidget);
    @Nullable
    private TabNavigationBar tabNavigationBar;

    private final SideNavigationBarOpener sideBarOpener;
    private final AnchoredButton saveButton;
    private final AnchoredButton discardButton;

    public EpicFightSettingScreen(@Nullable final Object mod, @Nullable Screen parentScreen) {
        super(Component.translatable(GUI_TITLE_SETTINGS));

        this.minecraft = parentScreen == null ? Minecraft.getInstance() : Minecraft.getInstance();
        this.font = this.minecraft.font;
        this.parentScreen = parentScreen;

        this.saveButton = AnchoredButton.buttonBuilder(
            Component.translatable(GUI_WIDGET_COMMON_SAVE),
            button -> {
                List<Runnable> toSave = new ArrayList<>();
                List<Runnable> toDiscard = new ArrayList<>();
                ClientConfig.checkUnsaved(toSave, toDiscard);

                for (Runnable runnable : toSave) {
                    runnable.run();
                }

                this.minecraft.setScreen(this.parentScreen);
            }
        )
        .xParams(115, 80)
        .yParams(10, 20)
        .horizontalAnchorType(AnchoredWidget.HorizontalAnchorType.RIGHT_WIDTH)
        .verticalAnchorType(AnchoredWidget.VerticalAnchorType.BOTTOM_HEGIHT)
        .theme(AnchoredButton.BuiltInTheme.BLACK)
        .highlihgtFontWhen(AbstractWidget::isHoveredOrFocused)
        .alpha(0.6F)
        .build();

        this.discardButton = AnchoredButton.buttonBuilder(
            Component.translatable(GUI_WIDGET_COMMON_DISCARD),
            button -> {
                List<Runnable> toSave = new ArrayList<>();
                List<Runnable> toDiscard = new ArrayList<> ();
                ClientConfig.checkUnsaved(toSave, toDiscard);

                if (!toSave.isEmpty() && !toDiscard.isEmpty()) {
                    this.minecraft.setScreen(new MessageScreen<> (
                        "",
                        Component.translatable(GUI_MESSAGE_SETTINGS_DISCARD_CHANGES_NOTIFICATION),
                        this,
                        button$2 -> {
                            toDiscard.forEach(Runnable::run);
                            this.minecraft.setScreen(this.parentScreen);
                        },
                        button$2 -> {
                            this.minecraft.setScreen(this);
                        },
                        180,
                        0
                    ).autoCalculateHeight());
                } else {
                    this.minecraft.setScreen(this.parentScreen);
                }
            }
        )
        .xParams(10, 100)
        .yParams(10, 20)
        .horizontalAnchorType(AnchoredWidget.HorizontalAnchorType.RIGHT_WIDTH)
        .verticalAnchorType(AnchoredWidget.VerticalAnchorType.BOTTOM_HEGIHT)
        .theme(AnchoredButton.BuiltInTheme.BLACK)
        .highlihgtFontWhen(AbstractWidget::isHoveredOrFocused)
        .alpha(0.6F)
        .build();

        this.sideBarOpener = new SideNavigationBarOpener(this, this.parentScreen, SideBarScreen::createConfigScreenSideNavBar);
    }

    @Override
    public void onClose() {
        List<Runnable> toSave = new ArrayList<>();
        List<Runnable> toDiscard = new ArrayList<>();
        ClientConfig.checkUnsaved(toSave, toDiscard);

        if (!toSave.isEmpty() && !toDiscard.isEmpty()) {
            this.minecraft.setScreen(new MessageScreen<> (
                "",
                Component.translatable(GUI_MESSAGE_SETTINGS_UNSAVED_CHANGES_NOTIFICATION),
                this,
                button -> {
                    toDiscard.forEach(Runnable::run);
                    this.minecraft.setScreen(this.parentScreen);
                },
                button -> {
                    this.minecraft.setScreen(this);
                },
                180,
                0
            ).autoCalculateHeight());
        } else {
            this.minecraft.setScreen(this.parentScreen);
        }
    }

    @Override
    protected void init() {
        this.tabNavigationBar = TabNavigationBar.builder(this.tabManager, this.width).addTabs(new EpicFightSettingScreen.UiTab(), new EpicFightSettingScreen.GraphicsTab(), new EpicFightSettingScreen.ModelTab(), new EpicFightSettingScreen.ControlsTab(), new EpicFightSettingScreen.CameraTab()).build();
        this.addRenderableWidget(this.tabNavigationBar);
        this.addRenderableWidget(this.saveButton);
        this.addRenderableWidget(this.discardButton);
        this.tabNavigationBar.selectTab(0, false);
        this.addRenderableWidget(this.sideBarOpener);
        this.repositionElements();
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawString(this.font, "EpicFight " + FabricLoader.getInstance().getModContainer(EpicFight.MODID).map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("0.0.0.0"), 4, this.height - 16, 0xFF9F9F9F);
    }

    @Override
    public void repositionElements() {
        if (this.tabNavigationBar != null) {
            this.tabNavigationBar.setWidth(this.width);
            this.tabNavigationBar.arrangeElements();
            int i = this.tabNavigationBar.getRectangle().bottom();
            ScreenRectangle screenrectangle = new ScreenRectangle(0, i, this.width, this.height - 36 - i);
            this.tabManager.setTabArea(screenrectangle);
        }

        this.saveButton.relocate(this.getRectangle());
        this.discardButton.relocate(this.getRectangle());
        this.sideBarOpener.relocate(this.getRectangle());
    }

    @Nullable
    public GuiEventListener getFocusedWidgetFromTable() {
        if (this.getFocused() instanceof WidgetTable widgetTable && widgetTable.getSelected() instanceof WidgetTable.WidgetEntry widgetEntry) {
            return widgetEntry.getFocused();
        }

        return null;
    }

    public AnchoredButton getSaveButton() {
        return this.saveButton;
    }

    public AnchoredButton getDiscardButton() {
        return this.discardButton;
    }

    abstract class SettingTabPage implements Tab {
        private final Component title;
        protected final WidgetTable widgetTable = new WidgetTable(EpicFightSettingScreen.this, 10, 0, 5, 10, AnchoredWidget.HorizontalAnchorType.LEFT_RIGHT, AnchoredWidget.VerticalAnchorType.TOP_BOTTOM, 21);
        protected final TextBox textBox = new TextBox(minecraft.font, 10, 0, 5, 10, AnchoredWidget.HorizontalAnchorType.RIGHT_WIDTH, AnchoredWidget.VerticalAnchorType.TOP_BOTTOM, Component.empty());
        @Nullable
        protected SettingTitle hoveringSettingTitle = null;

        public SettingTabPage(Component title) {
            this.title = title;
        }

        @Override
        public @NotNull Component getTabTitle() {
            return this.title;
        }

        @Override
        public void doLayout(ScreenRectangle rectangle) {
            this.widgetTable.setX2(Math.round((rectangle.right() - rectangle.left()) * 0.3F));
            this.widgetTable.relocate(rectangle);

            this.textBox.setX2(Math.round((rectangle.right() - rectangle.left()) * 0.27F));
            this.textBox.relocate(rectangle);
        }

        @Override
        public void visitChildren(Consumer<AbstractWidget> consumer) {
            consumer.accept(this.widgetTable);
            consumer.accept(this.textBox);
        }

        @Nullable
        protected SettingTitle getHoveringSettingTitle() {
            return this.hoveringSettingTitle;
        }

        protected void setHoveringSettingTitle(@Nullable SettingTitle settingTitle) {
            this.hoveringSettingTitle = settingTitle;
        }
    }

    class UiTab extends SettingTabPage {
        private static final Component TITLE = Component.translatable(GUI_TITLE_SETTINGS_TAB_UI);

        public UiTab() {
            super(TITLE);

            this.widgetTable
                .newRow()
                .addWidget(
                    new SettingTitle(
                        minecraft.font,
                        this.widgetTable.nextX(4),
                        125,
                        0,
                        15,
                        AnchoredWidget.HorizontalAnchorType.LEFT_RIGHT,
                        AnchoredWidget.VerticalAnchorType.TOP_HEIGHT,
                        GUI_WIDGET_SETTINGS_UI_HEALTH_BAR,
                        this.textBox::setMessage,
                        this::getHoveringSettingTitle,
                        this::setHoveringSettingTitle,
                        EpicFightSettingScreen.this::getFocusedWidgetFromTable
                    )
                )
                .addWidget(
                    new ComboBox<> (
                        EpicFightSettingScreen.this,
                        minecraft.font,
                        10,
                        100,
                        0,
                        15,
                        AnchoredWidget.HorizontalAnchorType.RIGHT_WIDTH,
                        AnchoredWidget.VerticalAnchorType.TOP_HEIGHT,
                        () -> ClientConfig.healthBarVisibility,
                        value -> ClientConfig.healthBarVisibility = value,
                        8,
                        Component.translatable(GUI_WIDGET_SETTINGS_UI_HEALTH_BAR),
                        List.of(HealthBarVisibility.values()),
                        StringRepresentable::getSerializedName
                    )
                )
                .newRow()
                .addWidget(
                    new SettingTitle(
                        minecraft.font,
                        this.widgetTable.nextX(4),
                        125,
                        0,
                        15,
                        AnchoredWidget.HorizontalAnchorType.LEFT_RIGHT,
                        AnchoredWidget.VerticalAnchorType.TOP_HEIGHT,
                        GUI_WIDGET_SETTINGS_UI_TARGET_INDICATOR,
                        this.textBox::setMessage,
                        this::getHoveringSettingTitle,
                        this::setHoveringSettingTitle,
                        EpicFightSettingScreen.this::getFocusedWidgetFromTable
                    )
                )
                .addWidget(
                    new CheckBox(
                        minecraft.font,
                        9,
                        12,
                        0,
                        12,
                        AnchoredWidget.HorizontalAnchorType.RIGHT_WIDTH,
                        AnchoredWidget.VerticalAnchorType.TOP_HEIGHT,
                        () -> ClientConfig.showTargetIndicator,
                        value -> ClientConfig.showTargetIndicator = value,
                        Component.translatable(GUI_WIDGET_SETTINGS_UI_TARGET_INDICATOR),
                        false
                    )
                )
                .newRow()
                .addWidget(
                    new SettingTitle(
                        minecraft.font,
                        this.widgetTable.nextX(4),
                        125,
                        0,
                        15,
                        AnchoredWidget.HorizontalAnchorType.LEFT_RIGHT,
                        AnchoredWidget.VerticalAnchorType.TOP_HEIGHT,
                        GUI_WIDGET_SETTINGS_UI_TARGET_OUTLINER,
                        this.textBox::setMessage,
                        this::getHoveringSettingTitle,
                        this::setHoveringSettingTitle,
                        EpicFightSettingScreen.this::getFocusedWidgetFromTable
                    )
                )
                .addWidget(
                    new CheckBox(
                        minecraft.font,
                        9,
                        12,
                        0,
                        12,
                        AnchoredWidget.HorizontalAnchorType.RIGHT_WIDTH,
                        AnchoredWidget.VerticalAnchorType.TOP_HEIGHT,
                        () -> ClientConfig.enableTargetEntityGuide,
                        value -> ClientConfig.enableTargetEntityGuide = value,
                        Component.translatable(GUI_WIDGET_SETTINGS_UI_TARGET_OUTLINER),
                        false
                    )
                )
                .newRow()
                .addWidget(
                    new SettingTitle(
                        minecraft.font,
                        this.widgetTable.nextX(4),
                        125,
                        0,
                        15,
                        AnchoredWidget.HorizontalAnchorType.LEFT_RIGHT,
                        AnchoredWidget.VerticalAnchorType.TOP_HEIGHT,
                        GUI_WIDGET_SETTINGS_UI_TARGET_OUTLINER_COLOR,
                        this.textBox::setMessage,
                        this::getHoveringSettingTitle,
                        this::setHoveringSettingTitle,
                        EpicFightSettingScreen.this::getFocusedWidgetFromTable
                    )
                )
                .newRow()
                .addWidget(
                    new ColorDeterminator(
                        minecraft.font,
                        this.widgetTable.nextX(10),
                        12,
                        0,
                        15,
                        AnchoredWidget.HorizontalAnchorType.LEFT_RIGHT,
                        AnchoredWidget.VerticalAnchorType.TOP_HEIGHT,
                        () -> ClientConfig.targetOutlineColor,
                        value -> ClientConfig.targetOutlineColor = value,
                        Component.translatable(GUI_WIDGET_SETTINGS_UI_TARGET_OUTLINER),
                        ColorDeterminator.Theme.SIMPLE,
                        0.0D
                    )
                )
                .newRow()
                .addWidget(
                    new SettingTitle(
                        minecraft.font,
                        this.widgetTable.nextX(4),
                        125,
                        0,
                        15,
                        AnchoredWidget.HorizontalAnchorType.LEFT_RIGHT,
                        AnchoredWidget.VerticalAnchorType.TOP_HEIGHT,
                        GUI_WIDGET_SETTINGS_UI_MINE_BLOCK_GUIDE,
                        this.textBox::setMessage,
                        this::getHoveringSettingTitle,
                        this::setHoveringSettingTitle,
                        EpicFightSettingScreen.this::getFocusedWidgetFromTable
                    )
                )
                .addWidget(
                    new ComboBox<> (
                        EpicFightSettingScreen.this,
                        minecraft.font,
                        10,
                        100,
                        0,
                        15,
                        AnchoredWidget.HorizontalAnchorType.RIGHT_WIDTH,
                        AnchoredWidget.VerticalAnchorType.TOP_HEIGHT,
                        () -> ClientConfig.mineBlockGuideOption,
                        value -> ClientConfig.mineBlockGuideOption = value,
                        8,
                        Component.translatable(GUI_WIDGET_SETTINGS_UI_MINE_BLOCK_GUIDE),
                        List.of(ClientConfig.BlockGuideOptions.values()),
                        StringRepresentable::getSerializedName
                    )
                )
                .newRow()
                .addWidget(
                    new SettingTitle(
                        minecraft.font,
                        this.widgetTable.nextX(4),
                        125,
                        0,
                        15,
                        AnchoredWidget.HorizontalAnchorType.LEFT_RIGHT,
                        AnchoredWidget.VerticalAnchorType.TOP_HEIGHT,
                        GUI_WIDGET_SETTINGS_UI_SHOW_EPICFIGHT_ATTRIBUTES,
                        this.textBox::setMessage,
                        this::getHoveringSettingTitle,
                        this::setHoveringSettingTitle,
                        EpicFightSettingScreen.this::getFocusedWidgetFromTable
                    )
                )
                .addWidget(
                    new CheckBox(
                        minecraft.font,
                        9,
                        12,
                        0,
                        12,
                        AnchoredWidget.HorizontalAnchorType.RIGHT_WIDTH,
                        AnchoredWidget.VerticalAnchorType.TOP_HEIGHT,
                        () -> ClientConfig.showEpicFightAttributesInTooltip,
                        value -> ClientConfig.showEpicFightAttributesInTooltip = value,
                        Component.translatable(GUI_WIDGET_SETTINGS_UI_SHOW_EPICFIGHT_ATTRIBUTES),
                        false
                    )
                )
                .newRow()
                .addWidget(
                    new SettingTitle(
                        minecraft.font,
                        this.widgetTable.nextX(4),
                        125,
                        0,
                        15,
                        AnchoredWidget.HorizontalAnchorType.LEFT_RIGHT,
                        AnchoredWidget.VerticalAnchorType.TOP_HEIGHT,
                        GUI_WIDGET_SETTINGS_UI_HUD_LOCATIONS,
                        this.textBox::setMessage,
                        this::getHoveringSettingTitle,
                        this::setHoveringSettingTitle,
                        EpicFightSettingScreen.this::getFocusedWidgetFromTable
                    )
                )
                .addWidget(
                    new SubscreenButton(
                        8,
                        15,
                        0,
                        15,
                        AnchoredWidget.HorizontalAnchorType.RIGHT_WIDTH,
                        AnchoredWidget.VerticalAnchorType.TOP_HEIGHT,
                        button -> {},
                        () -> new HUDLocationsScreen(EpicFightSettingScreen.this)
                    )
                )
            ;

            this.widgetTable.initialize(false);
        }
    }

    class GraphicsTab extends SettingTabPage {
        private static final Component TITLE = Component.translatable(GUI_TITLE_SETTINGS_TAB_GRAPHICS);

        public GraphicsTab() {
            super(TITLE);

            this.widgetTable
                .newRow()
                .addWidget(
                    new SettingTitle(
                        minecraft.font,
                        this.widgetTable.nextX(4),
                        125,
                        0,
                        15,
                        AnchoredWidget.HorizontalAnchorType.LEFT_RIGHT,
                        AnchoredWidget.VerticalAnchorType.TOP_HEIGHT,
                        GUI_WIDGET_SETTINGS_GRAPHICS_COMPUTE_SHADER,
                        this.textBox::setMessage,
                        this::getHoveringSettingTitle,
                        this::setHoveringSettingTitle,
                        EpicFightSettingScreen.this::getFocusedWidgetFromTable
                    )
                )
                .addWidget(
                    new CheckBox(
                        minecraft.font,
                        9,
                        12,
                        0,
                        12,
                        AnchoredWidget.HorizontalAnchorType.RIGHT_WIDTH,
                        AnchoredWidget.VerticalAnchorType.TOP_HEIGHT,
                        () -> ClientConfig.activateComputeShader,
                        value -> ClientConfig.activateComputeShader = value,
                        Component.translatable(GUI_WIDGET_SETTINGS_GRAPHICS_COMPUTE_SHADER),
                        false
                    )
                )
                .newRow()
                .addWidget(
                    new SettingTitle(
                        minecraft.font,
                        this.widgetTable.nextX(4),
                        125,
                        0,
                        15,
                        AnchoredWidget.HorizontalAnchorType.LEFT_RIGHT,
                        AnchoredWidget.VerticalAnchorType.TOP_HEIGHT,
                        GUI_WIDGET_SETTINGS_GRAPHICS_BLOOD_PARTICLES,
                        this.textBox::setMessage,
                        this::getHoveringSettingTitle,
                        this::setHoveringSettingTitle,
                        EpicFightSettingScreen.this::getFocusedWidgetFromTable
                    )
                )
                .addWidget(
                    new CheckBox(
                        minecraft.font,
                        9,
                        12,
                        0,
                        12,
                        AnchoredWidget.HorizontalAnchorType.RIGHT_WIDTH,
                        AnchoredWidget.VerticalAnchorType.TOP_HEIGHT,
                        () -> ClientConfig.bloodEffects,
                        value -> ClientConfig.bloodEffects = value,
                        Component.translatable(GUI_WIDGET_SETTINGS_GRAPHICS_BLOOD_PARTICLES),
                        false
                    )
                )
                .newRow()
                .addWidget(
                    new SettingTitle(
                        minecraft.font,
                        this.widgetTable.nextX(4),
                        125,
                        0,
                        15,
                        AnchoredWidget.HorizontalAnchorType.LEFT_RIGHT,
                        AnchoredWidget.VerticalAnchorType.TOP_HEIGHT,
                        GUI_WIDGET_SETTINGS_GRAPHICS_PERSISTENT_BUFFER,
                        this.textBox::setMessage,
                        this::getHoveringSettingTitle,
                        this::setHoveringSettingTitle,
                        EpicFightSettingScreen.this::getFocusedWidgetFromTable
                    )
                )
                .addWidget(
                    new CheckBox(
                        minecraft.font,
                        9,
                        12,
                        0,
                        12,
                        AnchoredWidget.HorizontalAnchorType.RIGHT_WIDTH,
                        AnchoredWidget.VerticalAnchorType.TOP_HEIGHT,
                        () -> ClientConfig.activatePersistentBuffer,
                        value -> ClientConfig.activatePersistentBuffer = value,
                        Component.translatable(GUI_TOOLTIP_SETTINGS_GRAPHICS_PERSISTENT_BUFFER),
                        false
                    )
                )
                .newRow()
                .addWidget(
                    new SettingTitle(
                        minecraft.font,
                        this.widgetTable.nextX(4),
                        125,
                        0,
                        15,
                        AnchoredWidget.HorizontalAnchorType.LEFT_RIGHT,
                        AnchoredWidget.VerticalAnchorType.TOP_HEIGHT,
                        GUI_WIDGET_SETTINGS_GRAPHICS_GROUND_SLAMS,
                        this.textBox::setMessage,
                        this::getHoveringSettingTitle,
                        this::setHoveringSettingTitle,
                        EpicFightSettingScreen.this::getFocusedWidgetFromTable
                    )
                )
                .addWidget(
                    new CheckBox(
                        minecraft.font,
                        9,
                        12,
                        0,
                        12,
                        AnchoredWidget.HorizontalAnchorType.RIGHT_WIDTH,
                        AnchoredWidget.VerticalAnchorType.TOP_HEIGHT,
                        () -> ClientConfig.groundSlams,
                        value -> ClientConfig.groundSlams = value,
                        Component.translatable(GUI_TOOLTIP_SETTINGS_GRAPHICS_GROUND_SLAMS),
                        false
                    )
                );

            this.widgetTable.initialize(false);
        }
    }

    class ModelTab extends SettingTabPage {
        private static final Component TITLE = Component.translatable(GUI_TITLE_SETTINGS_TAB_MODEL);

        public ModelTab() {
            super(TITLE);

            this.widgetTable
                .newRow()
                .addWidget(
                    new SettingTitle(
                        minecraft.font,
                        this.widgetTable.nextX(4),
                        125,
                        0,
                        15,
                        AnchoredWidget.HorizontalAnchorType.LEFT_RIGHT,
                        AnchoredWidget.VerticalAnchorType.TOP_HEIGHT,
                        GUI_WIDGET_SETTINGS_MODEL_VANILLA_MODEL,
                        this.textBox::setMessage,
                        this::getHoveringSettingTitle,
                        this::setHoveringSettingTitle,
                        EpicFightSettingScreen.this::getFocusedWidgetFromTable
                    )
                )
                .addWidget(
                    new CheckBox(
                        minecraft.font,
                        9,
                        12,
                        0,
                        12,
                        AnchoredWidget.HorizontalAnchorType.RIGHT_WIDTH,
                        AnchoredWidget.VerticalAnchorType.TOP_HEIGHT,
                        () -> ClientConfig.enableOriginalModel,
                        value -> ClientConfig.enableOriginalModel = value,
                        Component.translatable(GUI_WIDGET_SETTINGS_MODEL_VANILLA_MODEL),
                        false
                    )
                )
                .newRow()
                .addWidget(
                    new SettingTitle(
                        minecraft.font,
                        this.widgetTable.nextX(4),
                        125,
                        0,
                        15,
                        AnchoredWidget.HorizontalAnchorType.LEFT_RIGHT,
                        AnchoredWidget.VerticalAnchorType.TOP_HEIGHT,
                        GUI_WIDGET_SETTINGS_MODEL_FIRST_PERSON_MODEL,
                        this.textBox::setMessage,
                        this::getHoveringSettingTitle,
                        this::setHoveringSettingTitle,
                        EpicFightSettingScreen.this::getFocusedWidgetFromTable
                    )
                )
                .addWidget(
                    new CheckBox(
                        minecraft.font,
                        9,
                        12,
                        0,
                        12,
                        AnchoredWidget.HorizontalAnchorType.RIGHT_WIDTH,
                        AnchoredWidget.VerticalAnchorType.TOP_HEIGHT,
                        () -> ClientConfig.enableAnimatedFirstPersonModel,
                        value -> ClientConfig.enableAnimatedFirstPersonModel = value,
                        Component.translatable(GUI_WIDGET_SETTINGS_MODEL_FIRST_PERSON_MODEL),
                        false
                    )
                )
                .newRow()
                .addWidget(
                    new SettingTitle(
                        minecraft.font,
                        this.widgetTable.nextX(4),
                        125,
                        0,
                        15,
                        AnchoredWidget.HorizontalAnchorType.LEFT_RIGHT,
                        AnchoredWidget.VerticalAnchorType.TOP_HEIGHT,
                        GUI_WIDGET_SETTINGS_MODEL_MAX_STUCK_PROJECTILES,
                        this.textBox::setMessage,
                        this::getHoveringSettingTitle,
                        this::setHoveringSettingTitle,
                        EpicFightSettingScreen.this::getFocusedWidgetFromTable
                    )
                )
                .addWidget(
                    new ClampedNumberBox<> (
                        minecraft.font,
                        9,
                        60,
                        0,
                        12,
                        AnchoredWidget.HorizontalAnchorType.RIGHT_WIDTH,
                        AnchoredWidget.VerticalAnchorType.TOP_HEIGHT,
                        () -> ClientConfig.maxStuckProjectiles,
                        value -> ClientConfig.maxStuckProjectiles = value,
                        Component.translatable(GUI_WIDGET_SETTINGS_MODEL_MAX_STUCK_PROJECTILES),
                        0,
                        30,
                        1,
                        -1,
                        Integer::valueOf,
                        String::valueOf,
                        Integer::compare,
                        Integer::sum
                    )
                )
                .newRow()
                .addWidget(
                    new SettingTitle(
                        minecraft.font,
                        this.widgetTable.nextX(4),
                        125,
                        0,
                        15,
                        AnchoredWidget.HorizontalAnchorType.LEFT_RIGHT,
                        AnchoredWidget.VerticalAnchorType.TOP_HEIGHT,
                        GUI_WIDGET_SETTINGS_MODEL_COSMETICS,
                        this.textBox::setMessage,
                        this::getHoveringSettingTitle,
                        this::setHoveringSettingTitle,
                        EpicFightSettingScreen.this::getFocusedWidgetFromTable
                    )
                )
                .addWidget(
                    new CheckBox(
                        minecraft.font,
                        9,
                        12,
                        0,
                        12,
                        AnchoredWidget.HorizontalAnchorType.RIGHT_WIDTH,
                        AnchoredWidget.VerticalAnchorType.TOP_HEIGHT,
                        () -> ClientConfig.enableCosmetics,
                        value -> ClientConfig.enableCosmetics = value,
                        Component.translatable(GUI_WIDGET_SETTINGS_MODEL_COSMETICS),
                        false
                    )
                )
                .newRow()
                .addWidget(
                    new SettingTitle(
                        minecraft.font,
                        this.widgetTable.nextX(4),
                        125,
                        0,
                        15,
                        AnchoredWidget.HorizontalAnchorType.LEFT_RIGHT,
                        AnchoredWidget.VerticalAnchorType.TOP_HEIGHT,
                        GUI_WIDGET_SETTINGS_MODEL_EXPORT_ARMOR_MODELS,
                        this.textBox::setMessage,
                        this::getHoveringSettingTitle,
                        this::setHoveringSettingTitle,
                        EpicFightSettingScreen.this::getFocusedWidgetFromTable
                    )
                )
                .addWidget(
                    AnchoredButton
                        .buttonBuilder(
                            Component.translatable(GUI_WIDGET_SETTINGS_MODEL_EXPORT_ARMOR_MODELS),
                            button -> {
                                File resourcePackDirectory = Minecraft.getInstance().getResourcePackDirectory().toFile();
                                try {
                                    HumanoidModelBaker.exportModels(resourcePackDirectory);
                                    Util.getPlatform().openFile(resourcePackDirectory);
                                } catch (IOException e) {
                                    EpicFight.LOGGER.info("Failed to export custom armor models.", e);
                                }
                            }
                        )
                        .xParams(8, 60)
                        .yParams(0, 18)
                        .horizontalAnchorType(AnchoredWidget.HorizontalAnchorType.RIGHT_WIDTH)
                        .verticalAnchorType(AnchoredWidget.VerticalAnchorType.TOP_HEIGHT)
                        .theme(AnchoredButton.BuiltInTheme.VANILLA)
                        .build()
                );

            this.widgetTable.initialize(false);
        }
    }

    class ControlsTab extends SettingTabPage {
        private static final Component TITLE = Component.translatable(GUI_TITLE_SETTINGS_TAB_CONTROLS);

        public ControlsTab() {
            super(TITLE);

            this.widgetTable
                .newRow()
                .addWidget(
                    new SettingTitle(
                        minecraft.font,
                        this.widgetTable.nextX(4),
                        125,
                        0,
                        15,
                        AnchoredWidget.HorizontalAnchorType.LEFT_RIGHT,
                        AnchoredWidget.VerticalAnchorType.TOP_HEIGHT,
                        GUI_WIDGET_SETTINGS_CONTROLS_HOLDING_THRESHOLD,
                        this.textBox::setMessage,
                        this::getHoveringSettingTitle,
                        this::setHoveringSettingTitle,
                        EpicFightSettingScreen.this::getFocusedWidgetFromTable
                    )
                )
                .addWidget(
                    new ClampedNumberBox<> (
                        minecraft.font,
                        9,
                        60,
                        0,
                        12,
                        AnchoredWidget.HorizontalAnchorType.RIGHT_WIDTH,
                        AnchoredWidget.VerticalAnchorType.TOP_HEIGHT,
                        () -> ClientConfig.holdingThreshold,
                        value -> ClientConfig.holdingThreshold = value,
                        Component.translatable(GUI_WIDGET_SETTINGS_CONTROLS_HOLDING_THRESHOLD),
                        1,
                        10,
                        1,
                        -1,
                        Integer::valueOf,
                        String::valueOf,
                        Integer::compare,
                        Integer::sum
                    )
                )
                .newRow()
                .addWidget(
                    new SettingTitle(
                        minecraft.font,
                        this.widgetTable.nextX(4),
                        125,
                        0,
                        15,
                        AnchoredWidget.HorizontalAnchorType.LEFT_RIGHT,
                        AnchoredWidget.VerticalAnchorType.TOP_HEIGHT,
                        GUI_WIDGET_SETTINGS_CONTROLS_AUTO_PERSPECTIVE_SWITHING,
                        this.textBox::setMessage,
                        this::getHoveringSettingTitle,
                        this::setHoveringSettingTitle,
                        EpicFightSettingScreen.this::getFocusedWidgetFromTable
                    )
                )
                .addWidget(
                    new CheckBox(
                        minecraft.font,
                        9,
                        12,
                        0,
                        12,
                        AnchoredWidget.HorizontalAnchorType.RIGHT_WIDTH,
                        AnchoredWidget.VerticalAnchorType.TOP_HEIGHT,
                        () -> ClientConfig.autoPerspectiveSwithing,
                        value -> ClientConfig.autoPerspectiveSwithing = value,
                        Component.translatable(GUI_WIDGET_SETTINGS_CONTROLS_AUTO_PERSPECTIVE_SWITHING),
                        false
                    )
                )
                .newRow()
                .addWidget(
                    new SettingTitle(
                        minecraft.font,
                        this.widgetTable.nextX(4),
                        125,
                        0,
                        15,
                        AnchoredWidget.HorizontalAnchorType.LEFT_RIGHT,
                        AnchoredWidget.VerticalAnchorType.TOP_HEIGHT,
                        GUI_WIDGET_SETTINGS_CONTROLS_CANCELED_VANILLA_ACTIONS,
                        this.textBox::setMessage,
                        this::getHoveringSettingTitle,
                        this::setHoveringSettingTitle,
                        EpicFightSettingScreen.this::getFocusedWidgetFromTable
                    )
                )
                .addWidget(
                    new ComboBox<> (
                        EpicFightSettingScreen.this,
                        minecraft.font,
                        10,
                        100,
                        0,
                        15,
                        AnchoredWidget.HorizontalAnchorType.RIGHT_WIDTH,
                        AnchoredWidget.VerticalAnchorType.TOP_HEIGHT,
                        () -> ClientConfig.canceledVanillaActions,
                        value -> ClientConfig.canceledVanillaActions = value,
                        8,
                        Component.translatable(GUI_WIDGET_SETTINGS_CONTROLS_CANCELED_VANILLA_ACTIONS),
                        List.of(ClientConfig.CanceledVanillaActions.values()),
                        StringRepresentable::getSerializedName
                    )
                )
                .newRow()
                .addWidget(
                    new SettingTitle(
                        minecraft.font,
                        this.widgetTable.nextX(4),
                        125,
                        0,
                        15,
                        AnchoredWidget.HorizontalAnchorType.LEFT_RIGHT,
                        AnchoredWidget.VerticalAnchorType.TOP_HEIGHT,
                        GUI_WIDGET_SETTINGS_CONTROLS_PLAYER_BAHAVIOR_STRATEGY,
                        this.textBox::setMessage,
                        this::getHoveringSettingTitle,
                        this::setHoveringSettingTitle,
                        EpicFightSettingScreen.this::getFocusedWidgetFromTable
                    )
                )
                .addWidget(
                    new ComboBox<> (
                        EpicFightSettingScreen.this,
                        minecraft.font,
                        10,
                        100,
                        0,
                        15,
                        AnchoredWidget.HorizontalAnchorType.RIGHT_WIDTH,
                        AnchoredWidget.VerticalAnchorType.TOP_HEIGHT,
                        () -> ClientConfig.playerBehaviorStrategy,
                        value -> ClientConfig.playerBehaviorStrategy = value,
                        8,
                        Component.translatable(GUI_WIDGET_SETTINGS_CONTROLS_PLAYER_BAHAVIOR_STRATEGY),
                        List.of(ClientConfig.PlayerBehaviorStrategy.values()),
                        StringRepresentable::getSerializedName
                    )
                )
                .newRow()
                .addWidget(
                    new SettingTitle(
                        minecraft.font,
                        this.widgetTable.nextX(4),
                        125,
                        0,
                        15,
                        AnchoredWidget.HorizontalAnchorType.LEFT_RIGHT,
                        AnchoredWidget.VerticalAnchorType.TOP_HEIGHT,
                        GUI_WIDGET_SETTINGS_CONTROLS_ITEM_CATEGORY,
                        this.textBox::setMessage,
                        this::getHoveringSettingTitle,
                        this::setHoveringSettingTitle,
                        EpicFightSettingScreen.this::getFocusedWidgetFromTable
                    )
                )
                .addWidget(
                    new SubscreenButton(
                        8,
                        15,
                        0,
                        15,
                        AnchoredWidget.HorizontalAnchorType.RIGHT_WIDTH,
                        AnchoredWidget.VerticalAnchorType.TOP_HEIGHT,
                        button -> {},
                        () -> new ItemsPreferenceScreen(EpicFightSettingScreen.this)
                    )
                );

            this.widgetTable.initialize(false);
        }
    }

    class CameraTab extends SettingTabPage {
        private static final Component TITLE = Component.translatable(GUI_TITLE_SETTINGS_TAB_CAMERA);

        public CameraTab() {
            super(TITLE);

            this.widgetTable
                .newRow()
                .addWidget(
                    new SettingTitle(
                        minecraft.font,
                        this.widgetTable.nextX(4),
                        125,
                        0,
                        15,
                        AnchoredWidget.HorizontalAnchorType.LEFT_RIGHT,
                        AnchoredWidget.VerticalAnchorType.TOP_HEIGHT,
                        GUI_WIDGET_SETTINGS_CAMERA_FIRST_PERSON_CAMERA_MOVE,
                        this.textBox::setMessage,
                        this::getHoveringSettingTitle,
                        this::setHoveringSettingTitle,
                        EpicFightSettingScreen.this::getFocusedWidgetFromTable
                    )
                )
                .addWidget(
                    new CheckBox(
                        minecraft.font,
                        9,
                        12,
                        0,
                        12,
                        AnchoredWidget.HorizontalAnchorType.RIGHT_WIDTH,
                        AnchoredWidget.VerticalAnchorType.TOP_HEIGHT,
                        () -> ClientConfig.enableFirstPersonCameraMove,
                        value -> ClientConfig.enableFirstPersonCameraMove = value,
                        Component.translatable(GUI_WIDGET_SETTINGS_CAMERA_FIRST_PERSON_CAMERA_MOVE),
                        false
                    )
                )
                .newRow()
                .addWidget(
                    new SettingTitle(
                        minecraft.font,
                        this.widgetTable.nextX(4),
                        125,
                        0,
                        15,
                        AnchoredWidget.HorizontalAnchorType.LEFT_RIGHT,
                        AnchoredWidget.VerticalAnchorType.TOP_HEIGHT,
                        GUI_WIDGET_SETTINGS_CAMERA_TPS_PERSPECTIVE,
                        this.textBox::setMessage,
                        this::getHoveringSettingTitle,
                        this::setHoveringSettingTitle,
                        EpicFightSettingScreen.this::getFocusedWidgetFromTable
                    )
                )
                .addWidget(
                    new ComboBox<> (
                        EpicFightSettingScreen.this,
                        minecraft.font,
                        10,
                        100,
                        0,
                        15,
                        AnchoredWidget.HorizontalAnchorType.RIGHT_WIDTH,
                        AnchoredWidget.VerticalAnchorType.TOP_HEIGHT,
                        ClientConfig::getTpsActivationType,
                        value -> ClientConfig.tpsType = value,
                        8,
                        Component.translatable(GUI_WIDGET_SETTINGS_CAMERA_TPS_PERSPECTIVE),
                        List.of(ClientConfig.TPSActivationType.values()),
                        StringRepresentable::getSerializedName
                    )
                )
                .newRow()
                .addWidget(
                    new SettingTitle(
                        minecraft.font,
                        this.widgetTable.nextX(4),
                        125,
                        0,
                        15,
                        AnchoredWidget.HorizontalAnchorType.LEFT_RIGHT,
                        AnchoredWidget.VerticalAnchorType.TOP_HEIGHT,
                        GUI_WIDGET_SETTINGS_CAMERA_TPS_POSITION,
                        this.textBox::setMessage,
                        this::getHoveringSettingTitle,
                        this::setHoveringSettingTitle,
                        EpicFightSettingScreen.this::getFocusedWidgetFromTable
                    )
                )
                .addWidget(
                    new SubscreenButton(
                        8,
                        15,
                        0,
                        15,
                        AnchoredWidget.HorizontalAnchorType.RIGHT_WIDTH,
                        AnchoredWidget.VerticalAnchorType.TOP_HEIGHT,
                        button -> {},
                        () -> new TPSSettingScreen(EpicFightSettingScreen.this)
                    )
                )
                .newRow()
                .addWidget(
                    new SettingTitle(
                        minecraft.font,
                        this.widgetTable.nextX(4),
                        125,
                        0,
                        15,
                        AnchoredWidget.HorizontalAnchorType.LEFT_RIGHT,
                        AnchoredWidget.VerticalAnchorType.TOP_HEIGHT,
                        GUI_WIDGET_SETTINGS_CAMERA_LOCK_ON_SNAPPING,
                        this.textBox::setMessage,
                        this::getHoveringSettingTitle,
                        this::setHoveringSettingTitle,
                        EpicFightSettingScreen.this::getFocusedWidgetFromTable
                    )
                )
                .addWidget(
                    new CheckBox(
                        minecraft.font,
                        9,
                        12,
                        0,
                        12,
                        AnchoredWidget.HorizontalAnchorType.RIGHT_WIDTH,
                        AnchoredWidget.VerticalAnchorType.TOP_HEIGHT,
                        () -> ClientConfig.lockOnSnapping,
                        value -> ClientConfig.lockOnSnapping = value,
                        Component.translatable(GUI_WIDGET_SETTINGS_CAMERA_LOCK_ON_SNAPPING),
                        false
                    )
                )
                .newRow()
                .addWidget(
                    new SettingTitle(
                        minecraft.font,
                        this.widgetTable.nextX(4),
                        125,
                        0,
                        15,
                        AnchoredWidget.HorizontalAnchorType.LEFT_RIGHT,
                        AnchoredWidget.VerticalAnchorType.TOP_HEIGHT,
                        GUI_WIDGET_SETTINGS_CAMERA_ENTITY_FOCUSING_RANGE,
                        this.textBox::setMessage,
                        this::getHoveringSettingTitle,
                        this::setHoveringSettingTitle,
                        EpicFightSettingScreen.this::getFocusedWidgetFromTable
                    )
                )
                .addWidget(
                    new ClampedNumberBox<> (
                        minecraft.font,
                        9,
                        60,
                        0,
                        12,
                        AnchoredWidget.HorizontalAnchorType.RIGHT_WIDTH,
                        AnchoredWidget.VerticalAnchorType.TOP_HEIGHT,
                        () -> ClientConfig.entityFocusingRange,
                        value -> ClientConfig.entityFocusingRange = value,
                        Component.translatable(GUI_WIDGET_SETTINGS_CAMERA_ENTITY_FOCUSING_RANGE),
                        5,
                        25,
                        1,
                        -1,
                        Integer::valueOf,
                        String::valueOf,
                        Integer::compare,
                        Integer::sum
                    )
                )
                .newRow()
                .addWidget(
                    new SettingTitle(
                        minecraft.font,
                        this.widgetTable.nextX(4),
                        125,
                        0,
                        15,
                        AnchoredWidget.HorizontalAnchorType.LEFT_RIGHT,
                        AnchoredWidget.VerticalAnchorType.TOP_HEIGHT,
                        GUI_WIDGET_SETTINGS_CAMERA_PERSPECTIVE_TOGGLE_MODE,
                        this.textBox::setMessage,
                        this::getHoveringSettingTitle,
                        this::setHoveringSettingTitle,
                        EpicFightSettingScreen.this::getFocusedWidgetFromTable
                    )
                )
                .addWidget(
                    new ComboBox<> (
                        EpicFightSettingScreen.this,
                        minecraft.font,
                        10,
                        100,
                        0,
                        15,
                        AnchoredWidget.HorizontalAnchorType.RIGHT_WIDTH,
                        AnchoredWidget.VerticalAnchorType.TOP_HEIGHT,
                        () -> ClientConfig.cameraPerspectiveToggleMode,
                        value -> ClientConfig.cameraPerspectiveToggleMode = value,
                        8,
                        Component.translatable(GUI_WIDGET_SETTINGS_CAMERA_PERSPECTIVE_TOGGLE_MODE),
                        List.of(ClientConfig.CameraPerspectiveToggleMode.values()),
                        StringRepresentable::getSerializedName
                    )
                );

            this.widgetTable.initialize(false);
        }
    }
}
