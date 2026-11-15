package yesman.epicfight.client.gui.datapack.screen;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Supplier;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import io.netty.util.internal.StringUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import yesman.epicfight.api.utils.ParseUtil;
import yesman.epicfight.client.gui.datapack.widgets.ComboBox;
import yesman.epicfight.client.gui.datapack.widgets.Grid;
import yesman.epicfight.client.gui.datapack.widgets.Grid.GridBuilder.RowEditButton;
import yesman.epicfight.client.gui.datapack.widgets.ResizableComponent.HorizontalSizing;
import yesman.epicfight.client.gui.datapack.widgets.ResizableComponent.VerticalSizing;
import yesman.epicfight.client.gui.datapack.widgets.ResizableEditBox;
import yesman.epicfight.client.gui.datapack.widgets.Static;
import yesman.epicfight.data.conditions.Condition;
import yesman.epicfight.data.conditions.Condition.EntityPatchCondition;
import yesman.epicfight.data.conditions.Condition.ParameterEditor;
import yesman.epicfight.registry.EpicFightRegistries;
import yesman.epicfight.registry.entries.EpicFightConditions;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.capabilities.item.CapabilityItem.Styles;
import yesman.epicfight.world.capabilities.item.Style;

public class StylesScreen extends Screen {
	private final Screen parentScreen;
	private final Grid stylesGrid;
	private Grid conditionGrid;
	private Grid parameterGrid;
	
	private final ComboBox<Style> defaultStyle;
	private final List<CompoundTag> cases = Lists.newArrayList();
	private final CompoundTag rootTag;
	
	public StylesScreen(Screen parentScreen, CompoundTag rootTag) {
		super(Component.translatable("datapack_edit.weapon_type.styles"));
		
		this.parentScreen = parentScreen;
		this.minecraft = Minecraft.getInstance();
		this.font = Minecraft.getInstance().font;
		this.rootTag = rootTag;
		
		this.stylesGrid = Grid.builder(this, Minecraft.getInstance())
								.xy1(12, 60)
								.xy2(160, 76)
								.horizontalSizing(HorizontalSizing.LEFT_WIDTH)
								.verticalSizing(VerticalSizing.TOP_BOTTOM)
								.rowHeight(26)
								.rowEditable(RowEditButton.ADD_REMOVE)
								.rowpositionChanged((rowposition, values) -> {
									CompoundTag caseCompound = this.cases.get(rowposition);
									Grid.PackImporter parameters = new Grid.PackImporter();
									
									for (Tag tag : caseCompound.getList("conditions", Tag.TAG_COMPOUND)) {
										CompoundTag conditionCompound = (CompoundTag)tag;
										parameters.newRow();
										parameters.newValue("condition", EpicFightConditions.getConditionOrNull(ResourceLocation.parse(conditionCompound.getString("predicate"))));
									}
									
									this.conditionGrid._setValue(parameters);
									
									if (this.conditionGrid.children().size() > 0) {
										this.conditionGrid.setGridFocus(0, "condition");
									}
									
									this.conditionGrid._setActive(true);
									this.parameterGrid._setActive(true);
								})
								.addColumn(Grid.combo("style", ParseUtil.remove(Style.ENUM_MANAGER.universalValues(), CapabilityItem.Styles.COMMON)).valueChanged((event) -> {
												this.cases.get(event.rowposition).put("style", StringTag.valueOf(ParseUtil.nullParam(event.postValue).toLowerCase(Locale.ROOT)));
											}).defaultVal(Styles.ONE_HAND))
								.pressAdd((grid, button) -> {
									int rowposition = grid.addRow((addedRow) -> this.cases.add(addedRow, new CompoundTag()));
									grid.setGridFocus(rowposition, "style");
									this.conditionGrid._setActive(true);
									this.parameterGrid._setActive(true);
								})
								.pressRemove((grid, button) -> {
									grid.removeRow((removedRow) -> {
										this.cases.remove(removedRow);
										
										if (this.cases.size() == 0) {
											this.conditionGrid._setActive(false);
											this.parameterGrid._setActive(false);
										}
									});
								})
								.build();
		
		this.defaultStyle = new ComboBox<>(parentScreen, this.font, 55, 116, 15, 53, HorizontalSizing.LEFT_WIDTH, VerticalSizing.HEIGHT_BOTTOM, 8, Component.translatable("datapack_edit.weapon_type.styles.default"),
											new ArrayList<>(ParseUtil.remove(Style.ENUM_MANAGER.universalValues(), CapabilityItem.Styles.COMMON)), ParseUtil::snakeToSpacedCamel, null);
		
		this.font = Minecraft.getInstance().font;
		
		this.conditionGrid = Grid.builder(this, Minecraft.getInstance())
									.xy1(187, 60)
									.xy2(15, 50)
									.horizontalSizing(HorizontalSizing.LEFT_RIGHT)
									.verticalSizing(VerticalSizing.TOP_HEIGHT)
									.rowHeight(21)
									.rowEditable(RowEditButton.ADD_REMOVE)
									.rowpositionChanged((rowposition, values) -> {
										this.parameterGrid.reset();
										
										@SuppressWarnings("unchecked")
										Supplier<Condition<?>> conditionProvider = (Supplier<Condition<?>>)values.get("condition");
										
										if (conditionProvider != null) {
											Condition<?> condition = conditionProvider.get();
											ListTag conditionList = ParseUtil.getOrDefaultTag(this.cases.get(this.stylesGrid.getRowposition()), "conditions", new ListTag());
											CompoundTag conditionCompound = conditionList.getCompound(rowposition);
											Grid.PackImporter parameters = new Grid.PackImporter();
											
											for (ParameterEditor editor : condition.getAcceptingParameters(this)) {
												parameters.newRow();
												parameters.newValue("parameter_key", editor);
												parameters.newValue("parameter_value", editor.fromTag.apply(conditionCompound.get(editor.editWidget.getMessage().getString())));
											}
											
											this.parameterGrid._setValue(parameters);
										}
									})
									.addColumn(Grid.registryPopup("condition", EpicFightRegistries.CONDITION)
													.filter((condition) -> condition.get() instanceof EntityPatchCondition)
													.editable(true)
													.toDisplayText((condition) -> ParseUtil.getRegistryName(condition, EpicFightRegistries.CONDITION))
													.valueChanged((event) -> {
														ListTag conditionList = ParseUtil.getOrDefaultTag(this.cases.get(this.stylesGrid.getRowposition()), "conditions", new ListTag());
														CompoundTag conditionCompound = conditionList.getCompound(event.rowposition);
														
														conditionCompound.putString("predicate", ParseUtil.getRegistryName(event.postValue, EpicFightRegistries.CONDITION));
														this.parameterGrid.reset();
														
														if (event.postValue != null) {
															Condition<?> condition = event.postValue.get();
															Grid.PackImporter parameters = new Grid.PackImporter();
															
															for (ParameterEditor editor : condition.getAcceptingParameters(this)) {
																parameters.newRow();
																parameters.newValue("parameter_key", editor);
																parameters.newValue("parameter_value", editor.fromTag.apply(conditionCompound.get(editor.editWidget.getMessage().getString())));
															}
															
															this.parameterGrid._setValue(parameters);
														}
													})
													.width(180))
									.pressAdd((grid, button) -> {
										grid.setValueChangeEnabled(false);
										int rowposition = grid.addRow();
										
										ListTag conditionList = ParseUtil.getOrDefaultTag(this.cases.get(this.stylesGrid.getRowposition()), "conditions", new ListTag());
										conditionList.add(rowposition, new CompoundTag());
										
										grid.setGridFocus(rowposition, "weapon_category");
										grid.setValueChangeEnabled(true);
									})
									.pressRemove((grid, button) -> {
										grid.removeRow((removedRow) -> {
											ListTag conditionList = ParseUtil.getOrDefaultTag(this.cases.get(this.stylesGrid.getRowposition()), "conditions", new ListTag());
											conditionList.remove(removedRow);
										});
										
										if (grid.children().size() == 0) {
											this.parameterGrid.reset();
										}
									})
									.build();
		
		this.parameterGrid = Grid.builder(this, Minecraft.getInstance())
									.xy1(187, 135)
									.xy2(15, 52)
									.horizontalSizing(HorizontalSizing.LEFT_RIGHT)
									.verticalSizing(VerticalSizing.TOP_BOTTOM)
									.rowHeight(21)
									.rowEditable(RowEditButton.NONE)
									.addColumn(Grid.<ParameterEditor, ResizableEditBox>wildcard("parameter_key")
													.editable(false)
													.toDisplayText((editor) -> editor.editWidget.getMessage().getString())
													.width(100))
									.addColumn(Grid.wildcard("parameter_value")
													.editWidgetProvider((row) -> {
														ParameterEditor editor = row.getValue("parameter_key");
														return editor.editWidget;
													})
													.toDisplayText(ParseUtil::snakeToSpacedCamel)
													.editable(true)
													.valueChanged((event) -> {
														ListTag conditionList = ParseUtil.getOrDefaultTag(this.cases.get(this.stylesGrid.getRowposition()), "conditions", new ListTag());
														CompoundTag conditionCompound = conditionList.getCompound(this.conditionGrid.getRowposition());
														
														ParameterEditor editor = event.grid.getValue(event.rowposition, "parameter_key");
														conditionCompound.put(editor.editWidget.getMessage().getString(), editor.toTag.apply(event.postValue));
													}).width(150))
									.build();
		
		this.conditionGrid._setActive(false);
		this.parameterGrid._setActive(false);
		
		if (rootTag.contains("styles")) {
			CompoundTag stylesCompound = rootTag.getCompound("styles");
			Grid.PackImporter packImporter = new Grid.PackImporter();
			
			for (Tag caseTag : stylesCompound.getList("cases", Tag.TAG_COMPOUND)) {
				CompoundTag caseComp = (CompoundTag)caseTag;
				CompoundTag caseComp$2 = new CompoundTag();
				
				if (caseComp.contains("style")) {
					caseComp$2.put("style", caseComp.get("style"));
				}
				
				if (caseComp.contains("conditions")) {
					caseComp$2.put("conditions", caseComp.get("conditions"));
				}
				
				packImporter.newRow();
				packImporter.newValue("style", Style.ENUM_MANAGER.get(caseComp$2.getString("style")));
				
				this.cases.add(caseComp$2);
			}
			
			this.stylesGrid._setValue(packImporter);
			this.defaultStyle._setValue(Style.ENUM_MANAGER.get(stylesCompound.getString("default")));
		}
	}
	
	@Override
	protected void init() {
		ScreenRectangle screenRectangle = this.getRectangle();
		
		this.stylesGrid.resize(screenRectangle);
		this.defaultStyle.resize(screenRectangle);
		this.conditionGrid.resize(screenRectangle);
		this.parameterGrid.resize(screenRectangle);
		
		this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, (button) -> {
			ListTag caseListTag = new ListTag();
			String currentStyle = "";
			
			if (this.defaultStyle._getValue() == null) {
				this.minecraft.setScreen(new MessageScreen<>("Save Failed", "Define a default style", this, (button2) -> {
					this.minecraft.setScreen(this);
				}, 180, 90).autoCalculateHeight());
				return;
			}
			
			Set<String> styleNames = Sets.newHashSet();
			
			try {
				for (CompoundTag tag : this.cases) {
					currentStyle = tag.getString("style");
					
					this.validateTagSave(tag);
					
					if (styleNames.contains(tag.getString("style"))) {
						throw new IllegalStateException("Duplicated style " + tag.getString("style"));
					}
					
					caseListTag.add(tag);
					styleNames.add(tag.getString("style"));
				}
				
				if (this.defaultStyle._getValue() == null) {
					throw new IllegalStateException("Define a default style");
				}
			} catch (Exception e) {
				this.minecraft.setScreen(new MessageScreen<>("Save Failed", "Failed to save style " + currentStyle + ": " + e.getMessage(), this, (button2) -> {
					this.minecraft.setScreen(this);
				}, 180, 90).autoCalculateHeight());
				
				return;
			}
			
			CompoundTag stylesCompound = new CompoundTag();
			stylesCompound.put("cases", caseListTag);
			stylesCompound.putString("default", ParseUtil.nullParam(this.defaultStyle._getValue()).toLowerCase(Locale.ROOT));
			
			this.rootTag.remove("styles");
			this.rootTag.put("styles", stylesCompound);
			
			this.onClose();
		}).pos(this.width / 2 - 162, this.height - 32).size(160, 21).build());
		
		this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, (button) -> {
			this.minecraft.setScreen(new MessageScreen<>("", "Do you want to quit without saving changes?", this, (button2) -> this.onClose(), (button2) -> this.minecraft.setScreen(this), 180, 70));
		}).pos(this.width / 2 + 2, this.height - 32).size(160, 21).build());
		
		Static defaultStyleTitle = new Static(this, 12, 100, 15, 53, HorizontalSizing.LEFT_WIDTH, VerticalSizing.HEIGHT_BOTTOM, "datapack_edit.weapon_type.styles.default");
		defaultStyleTitle.resize(screenRectangle);
		
		Static conditionTitle = new Static(this, 187, 100, 40, 15, HorizontalSizing.LEFT_WIDTH, VerticalSizing.TOP_HEIGHT, "datapack_edit.weapon_type.styles.condition");
		conditionTitle.resize(screenRectangle);
		
		Static parameterTitle = new Static(this, 187, 100, 115, 15, HorizontalSizing.LEFT_WIDTH, VerticalSizing.TOP_HEIGHT, "datapack_edit.weapon_type.styles.parameters");
		parameterTitle.resize(screenRectangle);
		
		this.addRenderableWidget(new Static(this, 12, 60, 40, 15, HorizontalSizing.LEFT_WIDTH, null, Component.translatable("datapack_edit.styles"), Component.translatable("datapack_edit.styles.tooltip.optional")));
		this.addRenderableWidget(this.stylesGrid);
		this.addRenderableWidget(defaultStyleTitle);
		this.addRenderableWidget(this.defaultStyle);
		this.addRenderableWidget(conditionTitle);
		this.addRenderableWidget(this.conditionGrid);
		this.addRenderableWidget(parameterTitle);
		this.addRenderableWidget(this.parameterGrid);
	}
	
	@Override
	public void onClose() {
		this.minecraft.setScreen(this.parentScreen);
	}
	
	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int action) {
		if (this.getFocused() != null) {
			if (this.getFocused().mouseClicked(mouseX, mouseY, action)) {
				if (action == 0) {
					this.setDragging(true);
				}

				return true;
			}
		}
		
		for (GuiEventListener guieventlistener : this.children()) {
			if (guieventlistener == this.getFocused()) {
				continue;
			}
			
			if (guieventlistener.mouseClicked(mouseX, mouseY, action)) {
				this.setFocused(guieventlistener);
				
				if (action == 0) {
					this.setDragging(true);
				}

				return true;
			}
		}

		return false;
	}
	
	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		int yBegin = 32;
		int yEnd = this.height - 45;
		
		guiGraphics.drawString(this.font, this.title, 20, 16, 16777215);
		
		guiGraphics.setColor(0.125F, 0.125F, 0.125F, 1.0F);
        guiGraphics.blit(Screen.MENU_BACKGROUND, 0, yBegin, (float)this.width, (float)yEnd - yBegin, this.width, yEnd, 32, 32);
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
		
		guiGraphics.setColor(0.25F, 0.25F, 0.25F, 1.0F);
		guiGraphics.blit(Screen.MENU_BACKGROUND, 0, 0, 0.0F, 0.0F, this.width, yBegin, 32, 32);
        guiGraphics.blit(Screen.MENU_BACKGROUND, 0, yEnd, 0.0F, (float)yEnd - yBegin, this.width, yEnd, 32, 32);
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        
        guiGraphics.fillGradient(RenderType.guiOverlay(), 0, yBegin, this.width, yBegin + 4, -16777216, 0, 0);
		guiGraphics.fillGradient(RenderType.guiOverlay(), 0, yEnd, this.width, yEnd + 1, 0, -16777216, 0);
		
		super.render(guiGraphics, mouseX, mouseY, partialTick);
	}
	
	private void validateTagSave(CompoundTag tag) throws IllegalStateException {
		try {
			if (!tag.contains("conditions")) {
				throw new IllegalStateException("Define conditions");
			}
			
			for (Tag conditionTag : tag.getList("conditions", Tag.TAG_COMPOUND)) {
				CompoundTag conditionCompound = (CompoundTag)conditionTag;
				Supplier<Condition<?>> condition = EpicFightConditions.getConditionOrThrow(ResourceLocation.parse(conditionCompound.getString("predicate")));
				condition.get().read(conditionCompound);
			}
			
			if (!tag.contains("style") || StringUtil.isNullOrEmpty(tag.getString("style"))) {
				throw new IllegalStateException("Define a style");
			}
		} catch (Exception e) {
			throw new IllegalStateException(e.getMessage());
		}
	}
}