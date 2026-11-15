package yesman.epicfight.client.gui.datapack.screen;
import net.minecraft.client.Minecraft;

import java.util.List;
import java.util.function.Consumer;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.GsonHelper;
import yesman.epicfight.api.animation.LivingMotion;
import yesman.epicfight.api.animation.LivingMotions;
import yesman.epicfight.api.animation.types.datapack.EditorAnimation;
import yesman.epicfight.api.client.animation.Layer;
import yesman.epicfight.api.client.animation.property.JointMask.JointMaskSet;
import yesman.epicfight.api.client.animation.property.JointMaskReloadListener;
import yesman.epicfight.api.utils.ParseUtil;
import yesman.epicfight.client.gui.datapack.widgets.ComboBox;
import yesman.epicfight.client.gui.datapack.widgets.Grid;
import yesman.epicfight.client.gui.datapack.widgets.Grid.GridBuilder.RowEditButton;
import yesman.epicfight.client.gui.datapack.widgets.InputComponentList;
import yesman.epicfight.client.gui.datapack.widgets.PopupBox;
import yesman.epicfight.client.gui.datapack.widgets.ResizableComponent.HorizontalSizing;
import yesman.epicfight.client.gui.datapack.widgets.Static;

public class StaticAnimationPropertyScreen extends Screen {
	private final InputComponentList<JsonObject> inputComponentsList;
	private final ComboBox<LayerOptions> layerTypeCombo;
	private final Consumer<LayerOptions> layerTypeResponder;
	private final Screen parentScreen;
	private final EditorAnimation animation;
	
	private Layer.Priority baseLayerPriority;
	private Layer.Priority compositeLayerPriority;
	private List<PackEntry<LivingMotion, JointMaskSet>> baseLayerMasks = Lists.newArrayList();
	private List<PackEntry<LivingMotion, JointMaskSet>> compositeLayerMasks = Lists.newArrayList();
	
	protected StaticAnimationPropertyScreen(Screen parentScreen, EditorAnimation animation) {
		super(Component.translatable("datapack_edit.import_animation.client_data"));
		
		this.parentScreen = parentScreen;
		this.minecraft = Minecraft.getInstance();
		this.font = Minecraft.getInstance().font;
		this.animation = animation;
		
		this.inputComponentsList = new InputComponentList<> (this, 0, 0, 0, 30) {
			@Override
			public void importTag(JsonObject tag) {
				this.clearComponents();
				this.setComponentsActive(true);
				
				LayerOptions layerOption = null;
				
				if (tag.has("layer")) {
					layerOption = LayerOptions.valueOf(GsonHelper.getAsString(tag, "layer"));
				} else if (tag.has("multilayer")) {
					layerOption = LayerOptions.MULTILAYER;
				}
				
				StaticAnimationPropertyScreen.this.rearrangeComponents(layerOption);
				
				Object[] data;
				
				if (layerOption == LayerOptions.BASE_LAYER) {
					StaticAnimationPropertyScreen.this.baseLayerPriority = Layer.Priority.valueOf(GsonHelper.getAsString(tag, "priority"));
					
					data = new Object[] {
						layerOption,
						StaticAnimationPropertyScreen.this.baseLayerPriority
					};
				} else if (layerOption == LayerOptions.COMPOSITE_LAYER) {
					StaticAnimationPropertyScreen.this.compositeLayerPriority = Layer.Priority.valueOf(GsonHelper.getAsString(tag, "priority"));
					Grid.PackImporter packImporter = new Grid.PackImporter();
					
					for (JsonElement maskTag : tag.getAsJsonArray("masks")) {
						JsonObject maskCompoundTag = maskTag.getAsJsonObject();
						LivingMotion livingMotion = LivingMotion.ENUM_MANAGER.get(GsonHelper.getAsString(maskCompoundTag, "livingmotion"));
						JointMaskSet jointMask = JointMaskReloadListener.getJointMaskEntry(GsonHelper.getAsString(maskCompoundTag, "type"));
						
						packImporter.newRow();
						packImporter.newValue("living_motion", livingMotion);
						packImporter.newValue("joint_mask", jointMask);
						
						StaticAnimationPropertyScreen.this.compositeLayerMasks.add(PackEntry.ofValue(livingMotion, jointMask));
					}
					
					data = new Object[] {
						layerOption,
						StaticAnimationPropertyScreen.this.compositeLayerPriority,
						packImporter
					};
					
				} else if (layerOption == LayerOptions.MULTILAYER) {
					JsonObject base = tag.getAsJsonObject("multilayer").getAsJsonObject("base");
					JsonObject composite = tag.getAsJsonObject("multilayer").getAsJsonObject("composite");
					
					StaticAnimationPropertyScreen.this.baseLayerPriority = Layer.Priority.valueOf(GsonHelper.getAsString(base, "priority"));
					Grid.PackImporter basePackImporter = new Grid.PackImporter();
					
					for (JsonElement maskTag : base.getAsJsonArray("masks")) {
						JsonObject maskCompoundTag = maskTag.getAsJsonObject();
						LivingMotion livingMotion = LivingMotion.ENUM_MANAGER.get(GsonHelper.getAsString(maskCompoundTag, "livingmotion"));
						JointMaskSet jointMask = JointMaskReloadListener.getJointMaskEntry(GsonHelper.getAsString(maskCompoundTag, "type"));
						
						basePackImporter.newRow();
						basePackImporter.newValue("living_motion", livingMotion);
						basePackImporter.newValue("joint_mask", jointMask);
						
						StaticAnimationPropertyScreen.this.baseLayerMasks.add(PackEntry.ofValue(livingMotion, jointMask));
					}
					
					StaticAnimationPropertyScreen.this.compositeLayerPriority = Layer.Priority.valueOf(GsonHelper.getAsString(composite, "priority"));
					Grid.PackImporter compositePackImporter = new Grid.PackImporter();
					
					for (JsonElement maskTag : composite.getAsJsonArray("masks")) {
						JsonObject maskCompoundTag = maskTag.getAsJsonObject();
						LivingMotion livingMotion = LivingMotion.ENUM_MANAGER.get(GsonHelper.getAsString(maskCompoundTag, "livingmotion"));
						JointMaskSet jointMask = JointMaskReloadListener.getJointMaskEntry(GsonHelper.getAsString(maskCompoundTag, "type"));
						
						compositePackImporter.newRow();
						compositePackImporter.newValue("living_motion", livingMotion);
						compositePackImporter.newValue("joint_mask", jointMask);
						
						StaticAnimationPropertyScreen.this.compositeLayerMasks.add(PackEntry.ofValue(livingMotion, jointMask));
					}
					
					data = new Object[] {
						layerOption,
						StaticAnimationPropertyScreen.this.baseLayerPriority,
						basePackImporter,
						StaticAnimationPropertyScreen.this.compositeLayerPriority,
						compositePackImporter
					};
				} else {
					data = new Object[] {
						null
					};
				}
				
				StaticAnimationPropertyScreen.this.layerTypeCombo._setResponder(null);
				this.setDataBindingComponenets(data);
				StaticAnimationPropertyScreen.this.layerTypeCombo._setResponder(StaticAnimationPropertyScreen.this.layerTypeResponder);
			}
		};
		
		this.layerTypeResponder = (layerType) -> this.rearrangeComponents(layerType);
		this.layerTypeCombo = new ComboBox<>(this, this.font, 0, 124, 100, 15, HorizontalSizing.LEFT_WIDTH, null, 8, Component.translatable("datapack_edit.import_animation.client_data.layer_type"),
												List.of(LayerOptions.values()), ParseUtil::snakeToSpacedCamel, this.layerTypeResponder);
		
		this.inputComponentsList.importTag(animation.getPropertiesJson());
	}
	
	protected void rearrangeComponents(LayerOptions layerType) {
		ScreenRectangle screenRect = this.getRectangle();
		this.inputComponentsList.clearComponents();
		
		this.baseLayerPriority = null;
		this.compositeLayerPriority = null;
		this.baseLayerMasks.clear();
		this.compositeLayerMasks.clear();
		
		this.inputComponentsList.newRow();
		this.inputComponentsList.addComponentCurrentRow(new Static(this, this.inputComponentsList.nextStart(4), 90, 60, 15, HorizontalSizing.LEFT_WIDTH, null, "datapack_edit.import_animation.client_data.layer_type"));
		this.inputComponentsList.addComponentCurrentRow(this.layerTypeCombo.relocateX(screenRect, this.inputComponentsList.nextStart(5)));
		
		if (layerType == LayerOptions.BASE_LAYER || layerType == LayerOptions.COMPOSITE_LAYER) {
			this.inputComponentsList.newRow();
			this.inputComponentsList.addComponentCurrentRow(new Static(this, this.inputComponentsList.nextStart(4), 90, 60, 15, HorizontalSizing.LEFT_WIDTH, null, "datapack_edit.import_animation.client_data.priority"));
			this.inputComponentsList.addComponentCurrentRow(new ComboBox<>(this, this.font, this.inputComponentsList.nextStart(5), 124, 100, 15, HorizontalSizing.LEFT_WIDTH, null, 8, Component.translatable("datapack_edit.import_animation.client_data.layer_type"),
																			List.of(Layer.Priority.values()), ParseUtil::snakeToSpacedCamel, (priority) -> {
																				if (layerType == LayerOptions.BASE_LAYER) {
																					this.baseLayerPriority = priority;
																				} else {
																					this.compositeLayerPriority = priority;
																				}
																			}));
			
			if (layerType == LayerOptions.COMPOSITE_LAYER) {
				this.inputComponentsList.newRow();
				this.inputComponentsList.addComponentCurrentRow(new Static(this, this.inputComponentsList.nextStart(4), 90, 60, 15, HorizontalSizing.LEFT_WIDTH, null, "datapack_edit.import_animation.client_data.mask"));
				
				this.inputComponentsList.newRow();
				this.inputComponentsList.newRow();
				this.inputComponentsList.addComponentCurrentRow(
					Grid.builder(this, Minecraft.getInstance())
						.xy1(5, 120)
						.xy2(16, 80)
						.horizontalSizing(HorizontalSizing.LEFT_RIGHT)
						.rowHeight(21)
						.rowEditable(RowEditButton.ADD_REMOVE)
						.addColumn(Grid.combo("living_motion", LivingMotion.ENUM_MANAGER.universalValues())
										.valueChanged((event) -> this.compositeLayerMasks.get(event.rowposition).setPackKey(event.postValue)).defaultVal(LivingMotions.IDLE))
						.addColumn(Grid.popup("joint_mask", PopupBox.JointMaskPopupBox::new)
										.valueChanged((event) -> this.compositeLayerMasks.get(event.rowposition).setValue(event.postValue))
										.toDisplayText((jointMask) -> ParseUtil.nullOrToString(jointMask, (m) -> JointMaskReloadListener.getKey(m).toString()))
										.width(150))
						.pressAdd((grid, button) -> {
							this.compositeLayerMasks.add(PackEntry.ofValue(LivingMotions.IDLE, null));
							int rowposition = grid.addRow();
							grid.setGridFocus(rowposition, "style");
						})
						.pressRemove((grid, button) -> {
							grid.removeRow((removedRow) -> this.compositeLayerMasks.remove(removedRow));
						})
						.build()
				);
				this.inputComponentsList.newRow();
			}
		} else if (layerType == LayerOptions.MULTILAYER) {
			this.inputComponentsList.newRow();
			this.inputComponentsList.addComponentCurrentRow(new Static(this, this.inputComponentsList.nextStart(12), 90, 60, 15, HorizontalSizing.LEFT_WIDTH, null, "datapack_edit.import_animation.client_data.base_layer"));
			
			this.inputComponentsList.newRow();
			this.inputComponentsList.addComponentCurrentRow(new Static(this, this.inputComponentsList.nextStart(26), 90, 60, 15, HorizontalSizing.LEFT_WIDTH, null, "datapack_edit.import_animation.client_data.priority"));
			this.inputComponentsList.addComponentCurrentRow(new ComboBox<>(this, this.font, this.inputComponentsList.nextStart(5), 124, 100, 15, HorizontalSizing.LEFT_WIDTH, null, 8, Component.translatable("datapack_edit.import_animation.client_data.layer_type"),
																			List.of(Layer.Priority.values()), ParseUtil::snakeToSpacedCamel, (priority) -> {
																				this.baseLayerPriority = priority;
																			}));
			
			this.inputComponentsList.newRow();
			this.inputComponentsList.addComponentCurrentRow(new Static(this, this.inputComponentsList.nextStart(26), 90, 60, 15, HorizontalSizing.LEFT_WIDTH, null, "datapack_edit.import_animation.client_data.mask"));
			this.inputComponentsList.newRow();
			this.inputComponentsList.newRow();
			this.inputComponentsList.addComponentCurrentRow(
				Grid.builder(this, Minecraft.getInstance())
					.xy1(26, 120)
					.xy2(16, 80)
					.horizontalSizing(HorizontalSizing.LEFT_RIGHT)
					.rowHeight(21)
					.rowEditable(RowEditButton.ADD_REMOVE)
					.addColumn(Grid.combo("living_motion", LivingMotion.ENUM_MANAGER.universalValues())
							.valueChanged((event) -> this.baseLayerMasks.get(event.rowposition).setPackKey(event.postValue)).defaultVal(LivingMotions.IDLE))
					.addColumn(Grid.popup("joint_mask", PopupBox.JointMaskPopupBox::new)
							.valueChanged((event) -> this.baseLayerMasks.get(event.rowposition).setValue(event.postValue))
							.toDisplayText((jointMask) -> ParseUtil.nullOrToString(jointMask, (m) -> JointMaskReloadListener.getKey(m).toString()))
							.width(150))
					.pressAdd((grid, button) -> {
						this.baseLayerMasks.add(PackEntry.ofValue(LivingMotions.IDLE, null));
						int rowposition = grid.addRow();
						grid.setGridFocus(rowposition, "style");
					})
					.pressRemove((grid, button) -> {
						grid.removeRow((removedRow) -> this.baseLayerMasks.remove(removedRow));
					})
					.build()
			);
			this.inputComponentsList.newRow();
			
			this.inputComponentsList.newRow();
			this.inputComponentsList.addComponentCurrentRow(new Static(this, this.inputComponentsList.nextStart(12), 90, 60, 15, HorizontalSizing.LEFT_WIDTH, null, Component.translatable("datapack_edit.import_animation.client_data.composite_layer")));
			
			this.inputComponentsList.newRow();
			this.inputComponentsList.addComponentCurrentRow(new Static(this, this.inputComponentsList.nextStart(26), 90, 60, 15, HorizontalSizing.LEFT_WIDTH, null, Component.translatable("datapack_edit.import_animation.client_data.priority")));
			this.inputComponentsList.addComponentCurrentRow(new ComboBox<>(this, this.font, this.inputComponentsList.nextStart(5), 124, 100, 15, HorizontalSizing.LEFT_WIDTH, null, 8, Component.translatable("datapack_edit.import_animation.client_data.layer_type"),
																			List.of(Layer.Priority.values()), ParseUtil::snakeToSpacedCamel, (priority) -> {
																				this.compositeLayerPriority = priority;
																			}));
			
			this.inputComponentsList.newRow();
			this.inputComponentsList.addComponentCurrentRow(new Static(this, this.inputComponentsList.nextStart(26), 90, 60, 15, HorizontalSizing.LEFT_WIDTH, null, "datapack_edit.import_animation.client_data.mask"));
			this.inputComponentsList.newRow();
			this.inputComponentsList.newRow();
			this.inputComponentsList.addComponentCurrentRow(
				Grid.builder(this, Minecraft.getInstance())
					.xy1(26, 120)
					.xy2(16, 80)
					.horizontalSizing(HorizontalSizing.LEFT_RIGHT)
					.rowHeight(21)
					.rowEditable(RowEditButton.ADD_REMOVE)
					.addColumn(Grid.combo("living_motion", LivingMotion.ENUM_MANAGER.universalValues())
									.valueChanged((event) -> this.compositeLayerMasks.get(event.rowposition).setPackKey(event.postValue)).defaultVal(LivingMotions.IDLE))
					.addColumn(Grid.popup("joint_mask", PopupBox.JointMaskPopupBox::new)
									.valueChanged((event) -> this.compositeLayerMasks.get(event.rowposition).setValue(event.postValue))
									.toDisplayText((jointMask) -> ParseUtil.nullOrToString(jointMask, (m) -> JointMaskReloadListener.getKey(m).toString()))
									.width(150))
					.pressAdd((grid, button) -> {
						this.compositeLayerMasks.add(PackEntry.ofValue(LivingMotions.IDLE, null));
						int rowposition = grid.addRow();
						grid.setGridFocus(rowposition, "style");
					})
					.pressRemove((grid, button) -> {
						grid.removeRow((removedRow) -> this.compositeLayerMasks.remove(removedRow));
					})
					.build()
			);
			this.inputComponentsList.newRow();
		}
		
		this.inputComponentsList.updateSizeAndPosition(screenRect.width() - 15, screenRect.bottom() - screenRect.top() - 80, screenRect.top() + 32);
		this.inputComponentsList.setX(15);
	}
	
	@Override
	protected void init() {
		ScreenRectangle screenRect = this.getRectangle();
		
		this.inputComponentsList.updateSizeAndPosition(screenRect.width() - 15, screenRect.bottom() - screenRect.top() - 80, screenRect.top() + 32);
		this.inputComponentsList.setX(15);
		
		this.addRenderableWidget(this.inputComponentsList);
		
		this.addRenderableWidget(Button.builder(CommonComponents.GUI_OK, (button) -> {
			try {
				this.save();
				this.onClose();
			} catch (IllegalStateException e) {
				this.minecraft.setScreen(new MessageScreen<>("Failed to save", e.getMessage(), this, (button3) -> this.minecraft.setScreen(this), 300, 70).autoCalculateHeight());
			}
		}).pos(this.width / 2 - 162, this.height - 28).size(160, 21).build());
		
		this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, (button) -> {
			this.minecraft.setScreen(new MessageScreen<>("", "Do you want to quit without saving changes?", this, (button2) -> this.onClose(), (button2) -> this.minecraft.setScreen(this), 180, 70));
		}).pos(this.width / 2 + 2, this.height - 28).size(160, 21).build());
	}
	
	public void save() throws IllegalStateException {
		this.animation.getPropertiesJson().asMap().clear();
		LayerOptions layerOption = this.layerTypeCombo._getValue();
		
		if (layerOption == null) {
			throw new IllegalStateException("Layer type is not defined!");
		} else if (layerOption == LayerOptions.MULTILAYER) {
			
			if (this.baseLayerPriority == null) {
				throw new IllegalStateException("Base layer priority is not defined!");
			}
			
			if (this.compositeLayerPriority == null) {
				throw new IllegalStateException("Composite layer priority is not defined!");
			}
			
			JsonObject multilayer = new JsonObject();
			JsonObject base = new JsonObject();
			JsonObject composite = new JsonObject();
			
			base.addProperty("priority", this.baseLayerPriority.toString());
			composite.addProperty("priority", this.compositeLayerPriority.toString());
			
			JsonArray baseMasks = new JsonArray();
			int idx = 0;
			
			for (PackEntry<LivingMotion, JointMaskSet> entry : this.baseLayerMasks) {
				idx++;
				
				JsonObject jointEntryTag = new JsonObject();
				
				if (entry.getKey() == null) {
					throw new IllegalStateException(String.format("Row %s: Living motion is not defined!", idx));
				}
				
				if (entry.getValue() == null) {
					throw new IllegalStateException(String.format("Row %s: Joint mask is not defined!", idx));
				}
				
				jointEntryTag.addProperty("livingmotion", entry.getKey().toString());
				jointEntryTag.addProperty("type", JointMaskReloadListener.getKey(entry.getValue()).toString());
				baseMasks.add(jointEntryTag);
			}
			
			JsonArray compositeMasks = new JsonArray();
			idx = 0;
			
			for (PackEntry<LivingMotion, JointMaskSet> entry : this.compositeLayerMasks) {
				idx++;
				
				JsonObject jointEntryTag = new JsonObject();
				
				if (entry.getKey() == null) {
					throw new IllegalStateException(String.format("Row %s: Living motion is not defined!", idx));
				}
				
				if (entry.getValue() == null) {
					throw new IllegalStateException(String.format("Row %s: Joint mask is not defined!", idx));
				}
				
				jointEntryTag.addProperty("livingmotion", entry.getKey().toString());
				jointEntryTag.addProperty("type", JointMaskReloadListener.getKey(entry.getValue()).toString());
				
				compositeMasks.add(jointEntryTag);
			}
			
			base.add("masks", baseMasks);
			composite.add("masks", compositeMasks);
			multilayer.add("base", base);
			multilayer.add("composite", composite);
			this.animation.getPropertiesJson().add("multilayer", multilayer);
		} else {
			if (layerOption == LayerOptions.BASE_LAYER && this.baseLayerPriority == null) {
				throw new IllegalStateException("Base layer priority is not defined!");
			}
			
			if (layerOption == LayerOptions.COMPOSITE_LAYER && this.compositeLayerPriority == null) {
				throw new IllegalStateException("Composite layer priority is not defined!");
			}
			
			this.animation.getPropertiesJson().addProperty("layer", layerOption.toString());
			this.animation.getPropertiesJson().addProperty("priority", layerOption == LayerOptions.BASE_LAYER ? this.baseLayerPriority.toString() : this.compositeLayerPriority.toString());
			
			JsonArray masks = new JsonArray();
			List<PackEntry<LivingMotion, JointMaskSet>> list = layerOption == LayerOptions.BASE_LAYER ? this.baseLayerMasks : this.compositeLayerMasks;
			int idx = 0;
			
			for (PackEntry<LivingMotion, JointMaskSet> entry : list) {
				idx++;
				
				JsonObject jointEntryTag = new JsonObject();
				
				if (entry.getKey() == null) {
					throw new IllegalStateException(String.format("Row %s: Living motion is not defined!", idx));
				}
				
				if (entry.getValue() == null) {
					throw new IllegalStateException(String.format("Row %s: Joint mask is not defined!", idx));
				}
				
				jointEntryTag.addProperty("livingmotion", entry.getKey().toString());
				jointEntryTag.addProperty("type", JointMaskReloadListener.getKey(entry.getValue()).toString());
				masks.add(jointEntryTag);
			}
			
			this.animation.getPropertiesJson().add("masks", masks);
		}
	}
	
	@Override
	public void onClose() {
		this.minecraft.setScreen(this.parentScreen);
	}
	
	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
		if (this.inputComponentsList.mouseDragged(mouseX, mouseY, button, dx, dy)) {
			return true;
		}
		
		return super.mouseDragged(mouseX, mouseY, button, dx, dy);
	}
	
	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		this.renderBackground(guiGraphics, mouseX, mouseY, partialTicks);
		
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
		
		super.render(guiGraphics, mouseX, mouseY, partialTicks);
	}
	
	public enum LayerOptions {
		BASE_LAYER, COMPOSITE_LAYER, MULTILAYER
	}
}