package yesman.epicfight.client.gui.datapack.screen;
import net.minecraft.client.Minecraft;

import java.util.List;
import java.util.function.Supplier;

import com.google.common.collect.Lists;

import io.netty.util.internal.StringUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import yesman.epicfight.api.utils.ParseUtil;
import yesman.epicfight.client.gui.datapack.widgets.Grid;
import yesman.epicfight.client.gui.datapack.widgets.Grid.GridBuilder.RowEditButton;
import yesman.epicfight.client.gui.datapack.widgets.ResizableComponent.HorizontalSizing;
import yesman.epicfight.client.gui.datapack.widgets.ResizableComponent.VerticalSizing;
import yesman.epicfight.client.gui.datapack.widgets.ResizableEditBox;
import yesman.epicfight.data.conditions.Condition;
import yesman.epicfight.data.conditions.Condition.EntityPatchCondition;
import yesman.epicfight.data.conditions.Condition.ParameterEditor;
import yesman.epicfight.registry.EpicFightRegistries;
import yesman.epicfight.registry.entries.EpicFightConditions;

public class OffhandValidatorScreen extends Screen {
	private final Screen parentScreen;
	private final List<CompoundTag> conditionList = Lists.newLinkedList();
	private final CompoundTag rootTag;
	private Grid conditionGrid;
	private Grid parameterGrid;
	
	public OffhandValidatorScreen(Screen parentScreen, CompoundTag rootTag) {
		super(Component.translatable("datapack_edit.weapon_type.offhand_validator"));
		
		this.minecraft = Minecraft.getInstance();
		this.font = Minecraft.getInstance().font;
		this.rootTag = rootTag;
		this.parentScreen = parentScreen;
		
		this.conditionGrid = Grid.builder(this, Minecraft.getInstance())
									.xy1(15, 45)
									.xy2(100, 45)
									.horizontalSizing(HorizontalSizing.LEFT_WIDTH)
									.verticalSizing(VerticalSizing.TOP_BOTTOM)
									.rowHeight(21)
									.rowEditable(RowEditButton.ADD_REMOVE)
									.rowpositionChanged((rowposition, values) -> {
										this.parameterGrid.reset();
										
										@SuppressWarnings("unchecked")
										Supplier<Condition<?>> conditionProvider = (Supplier<Condition<?>>)values.get("condition");
										
										if (conditionProvider != null) {
											Condition<?> condition = conditionProvider.get();
											CompoundTag comp = this.conditionList.get(rowposition);
											Grid.PackImporter parameters = new Grid.PackImporter();
											
											for (ParameterEditor editor : condition.getAcceptingParameters(this)) {
												parameters.newRow();
												parameters.newValue("parameter_key", editor);
												parameters.newValue("parameter_value", editor.fromTag.apply(comp.get(editor.editWidget.getMessage().getString())));
											}
											
											this.parameterGrid._setValue(parameters);
										}
									})
									.addColumn(Grid.registryPopup("condition", EpicFightRegistries.CONDITION)
													.filter((condition) -> condition.get() instanceof EntityPatchCondition)
													.editable(true)
													.toDisplayText((condition) -> ParseUtil.getRegistryName(condition, EpicFightRegistries.CONDITION))
													.valueChanged((event) -> {
														CompoundTag comp = this.conditionList.get(event.rowposition);
														comp.putString("predicate", ParseUtil.getRegistryName(event.postValue, EpicFightRegistries.CONDITION));
														this.parameterGrid.reset();
														
														if (event.postValue != null) {
															Condition<?> condition = event.postValue.get();
															Grid.PackImporter parameters = new Grid.PackImporter();
															
															for (ParameterEditor editor : condition.getAcceptingParameters(this)) {
																parameters.newRow();
																parameters.newValue("parameter_key", editor);
																parameters.newValue("parameter_value", editor.fromTag.apply(comp.get(editor.editWidget.getMessage().getString())));
															}
															
															this.parameterGrid._setValue(parameters);
														}
													})
													.width(180))
									.pressAdd((grid, button) -> {
										grid.setValueChangeEnabled(false);
										int rowposition = grid.addRow();
										this.conditionList.add(rowposition, new CompoundTag());
										
										grid.setGridFocus(rowposition, "weapon_category");
										grid.setValueChangeEnabled(true);
									})
									.pressRemove((grid, button) -> {
										grid.removeRow((removedRow) -> {
											this.conditionList.remove(removedRow);
										});
										
										if (grid.children().size() == 0) {
											this.parameterGrid.reset();
										}
									})
									.build();
		
		this.parameterGrid = Grid.builder(this, Minecraft.getInstance())
									.xy1(125, 45)
									.xy2(12, 45)
									.horizontalSizing(HorizontalSizing.LEFT_RIGHT)
									.verticalSizing(VerticalSizing.TOP_BOTTOM)
									.rowHeight(21)
									.rowEditable(RowEditButton.NONE)
									.addColumn(Grid.<ParameterEditor, ResizableEditBox>wildcard("parameter_key")
													.editable(false)
													.toDisplayText((widget) -> widget.editWidget.getMessage().getString())
													.width(100))
									.addColumn(Grid.wildcard("parameter_value")
													.editWidgetProvider((row) -> {
														ParameterEditor editor = row.getValue("parameter_key");
														return editor.editWidget;
													})
													.toDisplayText(ParseUtil::snakeToSpacedCamel)
													.editable(true)
													.valueChanged((event) -> {
														CompoundTag predicate = this.conditionList.get(this.conditionGrid.getRowposition());
														ParameterEditor editor = event.grid.getValue(event.rowposition, "parameter_key");
														
														if (StringUtil.isNullOrEmpty(ParseUtil.nullParam(event.postValue))) {
															predicate.remove(editor.editWidget.getMessage().getString());
														} else {
															predicate.put(editor.editWidget.getMessage().getString(), editor.toTag.apply(event.postValue));
														}
													}).width(150))
									.build();
		
		if (rootTag.contains("offhand_item_compatible_predicate", Tag.TAG_LIST)) {
			ListTag conditionList = rootTag.getList("offhand_item_compatible_predicate", Tag.TAG_COMPOUND);
			
			Grid.PackImporter packImporter = new Grid.PackImporter();
			
			for (Tag tag : conditionList) {
				CompoundTag compTag = (CompoundTag)tag.copy();
				this.conditionList.add(compTag);
				
				packImporter.newRow();
				packImporter.newValue("condition", EpicFightConditions.getConditionOrNull(ResourceLocation.parse(compTag.getString("predicate"))));
			}
			
			this.conditionGrid._setValue(packImporter);
		}
	}
	
	@Override
	protected void init() {
		ScreenRectangle screenRectangle = this.getRectangle();
		
		this.conditionGrid.resize(screenRectangle);
		this.parameterGrid.resize(screenRectangle);
		
		this.addRenderableWidget(this.conditionGrid);
		this.addRenderableWidget(this.parameterGrid);
		
		this.addRenderableWidget(Button.builder(CommonComponents.GUI_OK, (button) -> {
			ListTag newListTag = new ListTag();
			int idx = 0;
			
			for (CompoundTag tag : this.conditionList) {
				try {
					this.validateTagSave(tag);
					newListTag.add(tag);
					idx++;
				} catch (Exception e) {
					this.minecraft.setScreen(new MessageScreen<>("Save Failed", "Failed to save row " + idx + ": " + e.getMessage(), this, (button2) -> {
						this.minecraft.setScreen(this);
					}, 180, 90).autoCalculateHeight());
					return;
				}
			}
			
			this.rootTag.put("offhand_item_compatible_predicate", newListTag);
			this.onClose();
		}).pos(this.width / 2 - 162, this.height - 32).size(160, 21).build());
		
		this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, (button) -> {
			this.minecraft.setScreen(new MessageScreen<>("", "Do you want to quit without saving changes?", this,
														(button2) -> {
															this.onClose();
														}, (button2) -> {
															this.minecraft.setScreen(this);
														}, 180, 70));
		}).pos(this.width / 2 + 2, this.height - 32).size(160, 21).build());
	}
	
	@Override
	public void onClose() {
		this.minecraft.setScreen(this.parentScreen);
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
			Supplier<Condition<?>> condition = EpicFightConditions.getConditionOrThrow(ResourceLocation.parse(tag.getString("predicate")));
			condition.get().read(tag);
		} catch (Exception e) {
			throw new IllegalStateException(e.getMessage());
		}
	}
}