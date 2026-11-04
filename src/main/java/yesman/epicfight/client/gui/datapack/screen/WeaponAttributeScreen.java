package yesman.epicfight.client.gui.datapack.screen;
import net.minecraft.client.Minecraft;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import io.netty.util.internal.StringUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import yesman.epicfight.api.utils.ParseUtil;
import yesman.epicfight.client.gui.datapack.screen.DatapackEditScreen.ItemCapabilityTab.ItemType;
import yesman.epicfight.client.gui.datapack.widgets.Grid;
import yesman.epicfight.client.gui.datapack.widgets.Grid.GridBuilder.RowEditButton;
import yesman.epicfight.client.gui.datapack.widgets.ResizableComponent.HorizontalSizing;
import yesman.epicfight.client.gui.datapack.widgets.ResizableComponent.VerticalSizing;
import yesman.epicfight.client.gui.datapack.widgets.ResizableEditBox;
import yesman.epicfight.client.gui.datapack.widgets.Static;
import yesman.epicfight.data.conditions.Condition.ParameterEditor;
import yesman.epicfight.world.capabilities.item.CapabilityItem.Styles;
import yesman.epicfight.world.capabilities.item.Style;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class WeaponAttributeScreen extends Screen {
	private final Map<String, ParameterEditor> weaponAttributeEditors = Maps.newLinkedHashMap();
	private final Map<String, ParameterEditor> armorAttributeEditors = Maps.newLinkedHashMap();
	
	private final Screen parentScreen;
	private final ItemType itemType;
	private Grid stylesGrid;
	private Grid attributesGrid;
	private final List<PackEntry<String, CompoundTag>> styles = Lists.newArrayList();
	private final CompoundTag rootTag;
	
	public WeaponAttributeScreen(Screen parentScreen, CompoundTag rootTag, ItemType itemType) {
		super(Component.translatable("datapack_edit.item_capability.attributes"));
		
		this.itemType = itemType;
		this.parentScreen = parentScreen;
		this.rootTag = rootTag;
		this.minecraft = Minecraft.getInstance();
		this.font = Minecraft.getInstance().font;
		
		final ResizableEditBox impactEditBox = new ResizableEditBox(this.font, 0, 0, 0, 0, Component.literal("impact"), null, null);
		final ResizableEditBox armorNegationEditBox = new ResizableEditBox(this.font, 0, 0, 0, 0, Component.literal("armor_negation"), null, null);
		final ResizableEditBox maxStrikesEditBox = new ResizableEditBox(this.font, 0, 0, 0, 0, Component.literal("max_strikes"), null, null);
		final ResizableEditBox damageBonusEditBox = new ResizableEditBox(this.font, 0, 0, 0, 0, Component.literal("damage_bonus"), null, null);
		final ResizableEditBox speedBonusEditBox = new ResizableEditBox(this.font, 0, 0, 0, 0, Component.literal("speed_bonus"), null, null);
		final ResizableEditBox stunArmorEditBox = new ResizableEditBox(this.font, 0, 0, 0, 0, Component.literal("stun_armor"), null, null);
		final ResizableEditBox weightEditBox = new ResizableEditBox(this.font, 0, 0, 0, 0, Component.literal("weight"), null, null);
		
		impactEditBox.setFilter((context) -> StringUtil.isNullOrEmpty(context) || ParseUtil.isParsableAllowingMinus(context, Double::parseDouble));
		armorNegationEditBox.setFilter((context) -> StringUtil.isNullOrEmpty(context) || ParseUtil.isParsableAllowingMinus(context, Double::parseDouble));
		maxStrikesEditBox.setFilter((context) -> StringUtil.isNullOrEmpty(context) || ParseUtil.isParsable(context, Integer::parseInt));
		damageBonusEditBox.setFilter((context) -> StringUtil.isNullOrEmpty(context) || ParseUtil.isParsableAllowingMinus(context, Double::parseDouble));
		speedBonusEditBox.setFilter((context) -> StringUtil.isNullOrEmpty(context) || ParseUtil.isParsableAllowingMinus(context, Double::parseDouble));
		stunArmorEditBox.setFilter((context) -> StringUtil.isNullOrEmpty(context) || ParseUtil.isParsableAllowingMinus(context, Double::parseDouble));
		weightEditBox.setFilter((context) -> StringUtil.isNullOrEmpty(context) || ParseUtil.isParsableAllowingMinus(context, Double::parseDouble));
		
		this.weaponAttributeEditors.put("armor_negation", ParameterEditor.of((value) -> DoubleTag.valueOf(ParseUtil.parseOrGet(value.toString(), Double::parseDouble, 0.0D)), (tag) -> ParseUtil.valueOfOmittingType(tag.getAsString()), armorNegationEditBox));
		this.weaponAttributeEditors.put("impact", ParameterEditor.of((value) -> DoubleTag.valueOf(ParseUtil.parseOrGet(value.toString(), Double::parseDouble, 0.0D)), (tag) -> ParseUtil.valueOfOmittingType(tag.getAsString()), impactEditBox));
		this.weaponAttributeEditors.put("max_strikes", ParameterEditor.of((value) -> IntTag.valueOf(ParseUtil.parseOrGet(value.toString(), Integer::parseInt, 0)), (tag) -> ParseUtil.valueOfOmittingType(tag.getAsString()), maxStrikesEditBox));
		this.weaponAttributeEditors.put("damage_bonus", ParameterEditor.of((value) -> DoubleTag.valueOf(ParseUtil.parseOrGet(value.toString(), Double::parseDouble, 0.0D)), (tag) -> ParseUtil.valueOfOmittingType(tag.getAsString()), damageBonusEditBox));
		this.weaponAttributeEditors.put("speed_bonus", ParameterEditor.of((value) -> DoubleTag.valueOf(ParseUtil.parseOrGet(value.toString(), Double::parseDouble, 0.0D)), (tag) -> ParseUtil.valueOfOmittingType(tag.getAsString()), speedBonusEditBox));
		this.armorAttributeEditors.put("stun_armor", ParameterEditor.of((value) -> DoubleTag.valueOf(ParseUtil.parseOrGet(value.toString(), Double::parseDouble, 0.0D)), (tag) -> ParseUtil.valueOfOmittingType(tag.getAsString()), stunArmorEditBox));
		this.armorAttributeEditors.put("weight", ParameterEditor.of((value) -> DoubleTag.valueOf(ParseUtil.parseOrGet(value.toString(), Double::parseDouble, 0.0D)), (tag) -> ParseUtil.valueOfOmittingType(tag.getAsString()), weightEditBox));
		
		if (itemType == ItemType.WEAPON) {
			this.stylesGrid = Grid.builder(this, Minecraft.getInstance())
									.xy1(20, 60)
									.xy2(90, 50)
									.horizontalSizing(HorizontalSizing.LEFT_WIDTH)
									.verticalSizing(VerticalSizing.TOP_BOTTOM)
									.rowHeight(21)
									.rowEditable(RowEditButton.ADD_REMOVE)
									.rowpositionChanged((rowposition, values) -> {
										Grid.PackImporter packImporter = new Grid.PackImporter();
										
										for (Map.Entry<String, Tag> entry : this.styles.get(rowposition).getValue().tags.entrySet()) {
											ParameterEditor paramEditor = this.weaponAttributeEditors.get(entry.getKey());
											packImporter.newRow().newValue("attribute", this.weaponAttributeEditors.get(entry.getKey())).newValue("amount", paramEditor == null ? "" : paramEditor.fromTag.apply(entry.getValue()));
										}
										
										this.attributesGrid._setActive(true);
										this.attributesGrid._setValue(packImporter);
									})
									.addColumn(Grid.combo("style", Style.ENUM_MANAGER.universalValues())
													.valueChanged((event) -> this.styles.get(event.rowposition).setPackKey(ParseUtil.nullParam(event.postValue).toLowerCase(Locale.ROOT)))
													.defaultVal(Styles.ONE_HAND))
									.pressAdd((grid, button) -> {
										this.styles.add(PackEntry.of("", CompoundTag::new));
										int rowposition = grid.addRow();
										grid.setGridFocus(rowposition, "style");
									})
									.pressRemove((grid, button) -> {
										grid.removeRow((removedRow) -> this.styles.remove(removedRow));
										
										if (grid.children().size() == 0) {
											this.attributesGrid._setActive(false);
										}
									})
									.build();
			
			this.attributesGrid = Grid.builder(this, Minecraft.getInstance())
										.xy1(120, 60)
										.xy2(20, 50)
										.horizontalSizing(HorizontalSizing.LEFT_RIGHT)
										.verticalSizing(VerticalSizing.TOP_BOTTOM)
										.rowHeight(21)
										.rowEditable(RowEditButton.ADD_REMOVE)
										.addColumn(Grid.combo("attribute", List.copyOf(this.weaponAttributeEditors.values()))
														.toDisplayText((editor) -> ParseUtil.nullOrToString(editor, (editor$1) -> ParseUtil.snakeToSpacedCamel(editor.editWidget.getMessage().getString())))
														.valueChanged((event) -> {
															CompoundTag attributesCompound = this.styles.get(this.stylesGrid.getRowposition()).getValue();
															
															if (event.prevValue != null) {
																attributesCompound.remove(event.prevValue.editWidget.getMessage().getString());
															} else {
																attributesCompound.remove("");
															}
															
															attributesCompound.putString(ParseUtil.nullParam(event.postValue.editWidget.getMessage().getString()), "");
														})
														.width(100))
										.addColumn(Grid.wildcard("amount")
														.editWidgetProvider((row) -> {
															ParameterEditor editor = row.getValue("attribute");
															return editor == null ? null : editor.editWidget;
														})
														.valueChanged((event) -> {
															CompoundTag attributesTag = this.styles.get(this.stylesGrid.getRowposition()).getValue();
															ParameterEditor editor = event.grid.getValue(event.rowposition, "attribute");
															
															if (!StringUtil.isNullOrEmpty(ParseUtil.nullParam(event.postValue))) {
																attributesTag.put(editor.editWidget.getMessage().getString(), editor.toTag.apply(event.postValue));
															} else {
																attributesTag.remove(editor.editWidget.getMessage().getString());
															}
														})
														.width(150))
										.pressAdd((grid, button) -> {
											this.styles.get(this.stylesGrid.getRowposition()).getValue().put("", StringTag.valueOf(""));
											int rowposition = grid.addRow();
											grid.setGridFocus(rowposition, "attribute");
										})
										.pressRemove((grid, button) -> {
                                            Object attributeGridRow = grid.getValue(grid.getRowposition(), "attribute");
											this.styles.get(this.stylesGrid.getRowposition()).getValue().remove(attributeGridRow == null ? "" : ((ParameterEditor)attributeGridRow).editWidget.getMessage().getString());
											grid.removeRow((removedRow) -> {});
										})
										.build();
			
			Grid.PackImporter packImporter = new Grid.PackImporter();
			
			for (Map.Entry<String, Tag> entry : rootTag.tags.entrySet()) {
				this.styles.add(PackEntry.of(entry.getKey(), () -> (CompoundTag)entry.getValue()));
				
				packImporter.newRow();
				packImporter.newValue("style", Style.ENUM_MANAGER.get(entry.getKey()));
			}
			
			this.stylesGrid._setValue(packImporter);
			this.attributesGrid._setActive(false);
		} else if (itemType == ItemType.ARMOR) {
			this.styles.add(PackEntry.of("armor", CompoundTag::new));
			
			this.attributesGrid = Grid.builder(this, Minecraft.getInstance())
										.xy1(20, 60)
										.xy2(20, 50)
										.horizontalSizing(HorizontalSizing.LEFT_RIGHT)
										.verticalSizing(VerticalSizing.TOP_BOTTOM)
										.rowHeight(21)
										.rowEditable(RowEditButton.ADD_REMOVE)
										.addColumn(Grid.combo("attribute", List.copyOf(this.armorAttributeEditors.values()))
														.toDisplayText((editor) -> ParseUtil.nullOrToString(editor, (editor$1) -> ParseUtil.snakeToSpacedCamel(editor.editWidget.getMessage().getString())))
														.valueChanged((event) -> {
															CompoundTag attributesCompound = this.styles.get(0).getValue();
															
															if (event.prevValue != null) {
																attributesCompound.remove(event.prevValue.editWidget.getMessage().getString());
															} else {
																attributesCompound.remove("");
															}
															
															attributesCompound.putString(ParseUtil.nullParam(event.postValue.editWidget.getMessage().getString()), "");
														})
														.width(100))
										.addColumn(Grid.wildcard("amount")
														.editWidgetProvider((row) -> {
															ParameterEditor editor = row.getValue("attribute");
															return editor == null ? null : editor.editWidget;
														})
														.valueChanged((event) -> {
															CompoundTag attributesTag = this.styles.get(0).getValue();
															ParameterEditor editor = event.grid.getValue(event.rowposition, "attribute");
															
															if (!StringUtil.isNullOrEmpty(ParseUtil.nullParam(event.postValue))) {
																attributesTag.put(editor.editWidget.getMessage().getString(), editor.toTag.apply(event.postValue));
															} else {
																attributesTag.remove(editor.editWidget.getMessage().getString());
															}
														})
														.width(150))
										.pressAdd((grid, button) -> {
											this.styles.get(0).getValue().put("", StringTag.valueOf(""));
											int rowposition = grid.addRow();
											grid.setGridFocus(rowposition, "attribute");
										})
										.pressRemove((grid, button) -> {
											this.styles.get(0).getValue().remove(grid.getValue(grid.getRowposition(), "attribute"));
											grid.removeRow((removedRow) -> {});
										})
										.build();
			
			this.styles.add(PackEntry.of("attributes", CompoundTag::new));
			
			Grid.PackImporter packImporter = new Grid.PackImporter();
			
			for (Map.Entry<String, Tag> entry : rootTag.tags.entrySet()) {
				ParameterEditor paramEditor = this.armorAttributeEditors.get(entry.getKey());
				
				packImporter.newRow();
				packImporter.newValue("attribute", this.armorAttributeEditors.get(entry.getKey()));
				packImporter.newValue("amount", paramEditor.fromTag.apply(entry.getValue()));
			}
			
			this.attributesGrid._setValue(packImporter);
		}
	}
	
	@Override
	protected void init() {
		if (this.itemType == ItemType.WEAPON) {
			this.stylesGrid.resize(this.getRectangle());
			this.addRenderableWidget(new Static(this, 20, 60, 40, 15, HorizontalSizing.LEFT_WIDTH, null, Component.translatable("datapack_edit.styles")));
			this.addRenderableWidget(this.stylesGrid);
		}
		
		this.attributesGrid.resize(this.getRectangle());
		this.addRenderableWidget(new Static(this, this.itemType == ItemType.WEAPON ? 120 : 20, 60, 40, 15, HorizontalSizing.LEFT_WIDTH, null, Component.translatable("datapack_edit.item_capability.attributes")));
		this.addRenderableWidget(this.attributesGrid);
		
		this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, (button) -> {
			if (this.itemType == ItemType.WEAPON) {
				Set<String> styles = Sets.newHashSet();
				
				for (PackEntry<String, CompoundTag> entry : this.styles) {
					if (styles.contains(entry.getKey())) {
						this.minecraft.setScreen(new MessageScreen<>("Save Failed", "Unable to save because of duplicated style: " + entry.getKey(), this, (button2) -> {
							this.minecraft.setScreen(this);
						}, 180, 90));
						return;
					}
					styles.add(entry.getKey());
				}
				
				this.rootTag.tags.clear();
				
				for (PackEntry<String, CompoundTag> entry : this.styles) {
					this.rootTag.put(entry.getKey(), entry.getValue());
				}
			} else if (this.itemType == ItemType.ARMOR) {
				CompoundTag attributesTag = this.styles.get(0).getValue();
				
				for (Map.Entry<String, Tag> tag : attributesTag.tags.entrySet()) {
					this.rootTag.put(tag.getKey(), tag.getValue());
				}
			}
			
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
}
