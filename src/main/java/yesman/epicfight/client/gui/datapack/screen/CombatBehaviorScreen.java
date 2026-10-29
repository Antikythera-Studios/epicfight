package yesman.epicfight.client.gui.datapack.screen;
import net.minecraft.client.Minecraft;

import java.util.List;
import java.util.function.Supplier;

import com.google.common.collect.Lists;

import io.netty.util.internal.StringUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.client.model.SkinnedMesh;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.api.utils.ParseUtil;
import yesman.epicfight.client.gui.datapack.widgets.CheckBox;
import yesman.epicfight.client.gui.datapack.widgets.Grid;
import yesman.epicfight.client.gui.datapack.widgets.Grid.GridBuilder.RowEditButton;
import yesman.epicfight.client.gui.datapack.widgets.InputComponentList;
import yesman.epicfight.client.gui.datapack.widgets.ModelPreviewer;
import yesman.epicfight.client.gui.datapack.widgets.PopupBox;
import yesman.epicfight.client.gui.datapack.widgets.ResizableComponent.HorizontalSizing;
import yesman.epicfight.client.gui.datapack.widgets.ResizableComponent.VerticalSizing;
import yesman.epicfight.client.gui.datapack.widgets.ResizableEditBox;
import yesman.epicfight.client.gui.datapack.widgets.RowSpliter;
import yesman.epicfight.client.gui.datapack.widgets.Static;
import yesman.epicfight.data.conditions.Condition;
import yesman.epicfight.data.conditions.Condition.EntityPatchCondition;
import yesman.epicfight.data.conditions.Condition.ParameterEditor;
import yesman.epicfight.main.EpicFightMod;
import yesman.epicfight.registry.EpicFightRegistries;
import yesman.epicfight.registry.entries.EpicFightConditions;

public class CombatBehaviorScreen extends Screen {
	private InputComponentList<CompoundTag> inputComponentsList;
	private final List<CompoundTag> movesetList = Lists.newLinkedList();
	private final Screen parentScreen;
	private final CompoundTag rootTag;
	private final ModelPreviewer modelPreviewer;
	private final boolean isHumanoidSubTag;
	
	private Grid movesetGrid;
	private Grid behaviorGrid;
	private Grid conditionGrid;
	private Grid parameterGrid;
	
	protected CombatBehaviorScreen(Screen parentScreen, CompoundTag rootTag, AssetAccessor<? extends Armature> armature, AssetAccessor<? extends SkinnedMesh> mesh, boolean isHumanoidSubTag) {
		super(Component.translatable("datapack_edit.mob_patch.combat_behavior"));
		
		this.isHumanoidSubTag = isHumanoidSubTag;
		this.parentScreen = parentScreen;
		this.minecraft = Minecraft.getInstance();
		this.font = Minecraft.getInstance().font;
		
		this.rootTag = rootTag;
		this.modelPreviewer = new ModelPreviewer(0, 10, 35, 50, HorizontalSizing.LEFT_RIGHT, VerticalSizing.TOP_BOTTOM, armature, mesh);
		
		final PopupBox.AnimationPopupBox animationPopupBox = new PopupBox.AnimationPopupBox(this, this.font, 0, 11, 0, 15, HorizontalSizing.LEFT_RIGHT, null, Component.literal("datapack_edit.mob_patch.combat_behavior.animation"), (pair) -> {
			ListTag behaviorListTag = ParseUtil.getOrDefaultTag(this.movesetList.get(this.movesetGrid.getRowposition()), "behaviors", new ListTag());
			CompoundTag behaviorTag = behaviorListTag.getCompound(this.behaviorGrid.getRowposition());
			behaviorTag.putString("animation", pair.getFirst());
			
			this.rearrangeAttackAnimation();
		});
		
		animationPopupBox.applyFilter(animation -> animation.checkType(AttackAnimation.class));
		animationPopupBox.setModel(armature, mesh);
		
		this.movesetGrid = Grid.builder(this, Minecraft.getInstance())
									.xy1(8, 45)
									.xy2(55, 50)
									.horizontalSizing(HorizontalSizing.LEFT_WIDTH)
									.verticalSizing(VerticalSizing.TOP_BOTTOM)
									.rowHeight(26)
									.rowEditable(RowEditButton.ADD_REMOVE)
									.rowpositionChanged((rowposition, values) -> {
										this.inputComponentsList.importTag(this.movesetList.get(rowposition));
										this.conditionGrid._setActive(false);
										this.parameterGrid._setActive(false);
										animationPopupBox._setActive(false);
										
										this.rearrangeAttackAnimation();
									})
									.addColumn(Grid.editbox("movement_set")
													.editable(false)
													.width(180))
									.pressAdd((grid, button) -> {
										grid.setValueChangeEnabled(false);
										int rowposition = grid.addRowWithDefaultValues("movement_set", String.format("moveset%d", grid.children().size() + 1));
										this.movesetList.add(rowposition, new CompoundTag());
										grid.setGridFocus(rowposition, "movement_set");
										grid.setValueChangeEnabled(true);
										this.setFocused(grid);
										
										if (grid.children().size() > 0) {
											this.inputComponentsList.setComponentsActive(true);
											this.conditionGrid._setActive(false);
											this.parameterGrid._setActive(false);
											animationPopupBox._setActive(false);
										}
									})
									.pressRemove((grid, button) -> {
										grid.removeRow((removedRow) -> this.movesetList.remove(removedRow));
										
										if (this.movesetList.size() == 0) {
											this.inputComponentsList.setComponentsActive(false);
										}
									})
									.build();
		
		this.behaviorGrid = Grid.builder(this, Minecraft.getInstance())
				.xy1(2, 0)
				.xy2(55, 245)
				.horizontalSizing(HorizontalSizing.LEFT_WIDTH)
				.rowHeight(21)
				.rowEditable(RowEditButton.ADD_REMOVE)
				.rowpositionChanged((rowposition, values) -> {
					ListTag behaviorListTag = ParseUtil.getOrDefaultTag(this.movesetList.get(this.movesetGrid.getRowposition()), "behaviors", new ListTag());
					CompoundTag tag = behaviorListTag.getCompound(rowposition);
					
					ListTag conditions = tag.getList("conditions", Tag.TAG_COMPOUND);
					String animation = tag.getString("animation");
					
					Grid.PackImporter conditionImporter = new Grid.PackImporter();
					
					for (Tag conditionTag : conditions) {
						String condtionName = ((CompoundTag)conditionTag).getString("predicate");
						
						if (!condtionName.contains(":")) {
							condtionName = EpicFightMod.prefix(condtionName);
							((CompoundTag)conditionTag).putString("predicate", condtionName);
						}
						
						conditionImporter.newRow();
						conditionImporter.newValue("condition", EpicFightConditions.getConditionOrNull(ResourceLocation.parse(condtionName)));
					}
					
					this.parameterGrid.reset();
					this.conditionGrid._setValue(conditionImporter);
					
					if (this.conditionGrid.children().size() > 0) {
						this.conditionGrid.setGridFocus(0, "condition");
					}
					
					animationPopupBox._setValue(DatapackEditScreen.animationByKey(animation));
					
					this.conditionGrid._setActive(true);
					this.parameterGrid._setActive(true);
					animationPopupBox._setActive(true);
				})
				.addColumn(Grid.editbox("behavior")
								.editable(false)
								.width(180))
				.pressAdd((grid, button) -> {
					grid.setValueChangeEnabled(false);
					int rowposition = grid.addRowWithDefaultValues("behavior", String.format("behavior%d", grid.children().size() + 1));
					ListTag behaviorListTag = ParseUtil.getOrDefaultTag(this.movesetList.get(this.movesetGrid.getRowposition()), "behaviors", new ListTag());
					behaviorListTag.add(rowposition, new CompoundTag());
					grid.setGridFocus(rowposition, "behavior");
					grid.setValueChangeEnabled(true);
					this.setFocused(grid);
					
					if (grid.children().size() > 0) {
						this.conditionGrid._setActive(true);
						this.parameterGrid._setActive(true);
						animationPopupBox._setActive(true);
					}
				})
				.pressRemove((grid, button) -> {
					grid.removeRow((removedRow) -> {
						ListTag behaviorListTag = ParseUtil.getOrDefaultTag(this.movesetList.get(this.movesetGrid.getRowposition()), "behaviors", new ListTag());
						behaviorListTag.remove(removedRow);
					});
					
					if (grid.children().size() == 0) {
						this.conditionGrid._setActive(false);
						this.parameterGrid._setActive(false);
						animationPopupBox._setActive(false);
					}
				})
				.build();
		
		this.inputComponentsList = new InputComponentList<>(this, 0, 0, 0, 28) {
			@Override
			public void importTag(CompoundTag tag) {
				this.resetComponents();
				
				Grid.PackImporter behaviorSet = new Grid.PackImporter();
				
				for (int i = 0; i < tag.getList("behaviors", Tag.TAG_COMPOUND).size(); i++) {
					behaviorSet.newRow();
					behaviorSet.newValue("behavior", "behavior" + (i+1));
				}
				
				if (!tag.contains("canBeInterrupted")) {
					tag.putBoolean("canBeInterrupted", false);
				}
				
				if (!tag.contains("looping")) {
					tag.putBoolean("looping", false);
				}
				
				this.setDataBindingComponenets(new Object[] {
					tag.contains("weight") ? ParseUtil.valueOfOmittingType(tag.getDouble("weight")) : "",
					tag.contains("cooldown") ? ParseUtil.valueOfOmittingType(tag.getInt("cooldown")) : "",
					tag.getBoolean("canBeInterrupted"),
					tag.getBoolean("looping"),
					null,
					behaviorSet,
				});
				
				this.setComponentsActive(true);
			}
		};
		
		this.inputComponentsList.setX(164);
		
		final ResizableEditBox weightEditBox = new ResizableEditBox(this.font, 0, 50, 0, 15, Component.translatable("datapack_edit.mob_patch.combat_behavior.weight"), HorizontalSizing.LEFT_WIDTH, null);
		
		weightEditBox.setResponder((value) -> {
			if (StringUtil.isNullOrEmpty(value)) {
				this.movesetList.get(this.movesetGrid.getRowposition()).remove("weight");
			} else {
				this.movesetList.get(this.movesetGrid.getRowposition()).putDouble("weight", Double.parseDouble(value));
			}
		});
		
		weightEditBox.setFilter((context) -> StringUtil.isNullOrEmpty(context) || ParseUtil.isParsableAllowingMinus(context, Double::parseDouble));
		
		final ResizableEditBox cooldownEditBox = new ResizableEditBox(this.font, 0, 50, 0, 15, Component.translatable("datapack_edit.mob_patch.combat_behavior.weight"), HorizontalSizing.LEFT_WIDTH, null);
		
		cooldownEditBox.setResponder((value) -> {
			if (StringUtil.isNullOrEmpty(value)) {
				this.movesetList.get(this.movesetGrid.getRowposition()).remove("cooldown");
			} else {
				this.movesetList.get(this.movesetGrid.getRowposition()).putInt("cooldown", Integer.parseInt(value));
			}
		});
		
		cooldownEditBox.setFilter((context) -> StringUtil.isNullOrEmpty(context) || ParseUtil.isParsable(context, Integer::parseInt));
		
		this.conditionGrid = Grid.builder(this, Minecraft.getInstance())
									.xy1(63, 0)
									.xy2(10, 80)
									.horizontalSizing(HorizontalSizing.LEFT_RIGHT)
									.rowHeight(21)
									.rowEditable(RowEditButton.ADD_REMOVE)
									.rowpositionChanged((rowposition, values) -> {
										this.parameterGrid.reset();
										
										@SuppressWarnings("unchecked")
										Supplier<Condition<?>> conditionProvider = (Supplier<Condition<?>>)values.get("condition");
										
										if (conditionProvider != null) {
											Condition<?> condition = conditionProvider.get();
											ListTag behaviorListTag = ParseUtil.getOrDefaultTag(this.movesetList.get(this.movesetGrid.getRowposition()), "behaviors", new ListTag());
											ListTag conditionsList = ParseUtil.getOrDefaultTag(behaviorListTag.getCompound(this.behaviorGrid.getRowposition()), "conditions", new ListTag());
											CompoundTag comp = (CompoundTag)conditionsList.get(rowposition);
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
														ListTag behaviorListTag = ParseUtil.getOrDefaultTag(this.movesetList.get(this.movesetGrid.getRowposition()), "behaviors", new ListTag());
														ListTag conditionsList = ParseUtil.getOrDefaultTag(behaviorListTag.getCompound(this.behaviorGrid.getRowposition()), "conditions", new ListTag());
														CompoundTag comp = (CompoundTag)conditionsList.get(event.rowposition);
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
										
										ListTag behaviorListTag = ParseUtil.getOrDefaultTag(this.movesetList.get(this.movesetGrid.getRowposition()), "behaviors", new ListTag());
										ListTag conditionsList = ParseUtil.getOrDefaultTag(behaviorListTag.getCompound(this.behaviorGrid.getRowposition()), "conditions", new ListTag());
										conditionsList.add(rowposition, new CompoundTag());
										
										grid.setGridFocus(rowposition, "weapon_category");
										grid.setValueChangeEnabled(true);
									})
									.pressRemove((grid, button) -> {
										grid.removeRow((removedRow) -> {
											ListTag behaviorListTag = ParseUtil.getOrDefaultTag(this.movesetList.get(this.movesetGrid.getRowposition()), "behaviors", new ListTag());
											ListTag conditionsList = ParseUtil.getOrDefaultTag(behaviorListTag.getCompound(this.behaviorGrid.getRowposition()), "conditions", new ListTag());
											conditionsList.remove(removedRow);
										});
										
										if (grid.children().size() == 0) {
											this.parameterGrid.reset();
										}
									})
									.build();
		
		this.parameterGrid = Grid.builder(this, Minecraft.getInstance())
									.xy1(63, 0)
									.xy2(10, 80)
									.horizontalSizing(HorizontalSizing.LEFT_RIGHT)
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
														ListTag behaviorListTag = ParseUtil.getOrDefaultTag(this.movesetList.get(this.movesetGrid.getRowposition()), "behaviors", new ListTag());
														ListTag conditionsList = ParseUtil.getOrDefaultTag(behaviorListTag.getCompound(this.behaviorGrid.getRowposition()), "conditions", new ListTag());
														CompoundTag conditionTag = conditionsList.getCompound(this.conditionGrid.getRowposition());
														ParameterEditor editor = event.grid.getValue(event.rowposition, "parameter_key");
														
														if (StringUtil.isNullOrEmpty(ParseUtil.nullParam(event.postValue))) {
															conditionTag.remove(editor.editWidget.getMessage().getString());
														} else {
															conditionTag.put(editor.editWidget.getMessage().getString(), editor.toTag.apply(event.postValue));
														}
													})
													.width(150))
									.build();
		
		this.inputComponentsList.newRow();
		this.inputComponentsList.addComponentCurrentRow(new Static(this, this.inputComponentsList.nextStart(4), 100, 0, 15, HorizontalSizing.LEFT_WIDTH, null, "datapack_edit.mob_patch.combat_behavior.weight"));
		this.inputComponentsList.addComponentCurrentRow(weightEditBox.relocateX(parentScreen.getRectangle(), this.inputComponentsList.nextStart(5)));
		
		this.inputComponentsList.newRow();
		this.inputComponentsList.addComponentCurrentRow(new Static(this, this.inputComponentsList.nextStart(4), 100, 0, 15, HorizontalSizing.LEFT_WIDTH, null, "datapack_edit.mob_patch.combat_behavior.cooldown"));
		this.inputComponentsList.addComponentCurrentRow(cooldownEditBox.relocateX(parentScreen.getRectangle(), this.inputComponentsList.nextStart(5)));
		
		this.inputComponentsList.newRow();
		this.inputComponentsList.addComponentCurrentRow(new Static(this, this.inputComponentsList.nextStart(4), 100, 0, 15, HorizontalSizing.LEFT_WIDTH, null, "datapack_edit.mob_patch.combat_behavior.interceptable"));
		this.inputComponentsList.addComponentCurrentRow(new CheckBox(this.font, this.inputComponentsList.nextStart(5), 60, 0, 10, HorizontalSizing.LEFT_WIDTH, null, null, Component.literal(""), (value) -> {
			this.movesetList.get(this.movesetGrid.getRowposition()).putBoolean("canBeInterrupted", value);
		}));
		
		this.inputComponentsList.newRow();
		this.inputComponentsList.addComponentCurrentRow(new Static(this, this.inputComponentsList.nextStart(4), 100, 0, 15, HorizontalSizing.LEFT_WIDTH, null, "datapack_edit.mob_patch.combat_behavior.loop"));
		this.inputComponentsList.addComponentCurrentRow(new CheckBox(this.font, this.inputComponentsList.nextStart(5), 60, 0, 10, HorizontalSizing.LEFT_WIDTH, null, null, Component.literal(""), (value) -> {
			this.movesetList.get(this.movesetGrid.getRowposition()).putBoolean("looping", value);
		}));
		
		this.inputComponentsList.newRow();
		this.inputComponentsList.addComponentCurrentRow(new RowSpliter(this.inputComponentsList.nextStart(0), 10, 60, 15, HorizontalSizing.LEFT_RIGHT, null));
		
		this.inputComponentsList.newRow();
		this.inputComponentsList.addComponentCurrentRow(new Static(this, this.inputComponentsList.nextStart(63), 100, 0, 15, HorizontalSizing.LEFT_WIDTH, null, "datapack_edit.mob_patch.combat_behavior.conditions"));
		this.inputComponentsList.newRow();
		this.inputComponentsList.newRow();
		this.inputComponentsList.addComponentCurrentRow(this.conditionGrid.relocateX(parentScreen.getRectangle(), this.inputComponentsList.nextStart(60)));
		this.inputComponentsList.newRow();
		
		this.inputComponentsList.newRow();
		this.inputComponentsList.addComponentCurrentRow(this.behaviorGrid);
		
		this.inputComponentsList.addComponentCurrentRow(new Static(this, this.inputComponentsList.nextStart(7), 100, 0, 15, HorizontalSizing.LEFT_WIDTH, null, "datapack_edit.mob_patch.combat_behavior.parameters"));
		this.inputComponentsList.newRow();
		this.inputComponentsList.newRow();
		this.inputComponentsList.addComponentCurrentRow(this.parameterGrid.relocateX(parentScreen.getRectangle(), this.inputComponentsList.nextStart(60)));
		this.inputComponentsList.newRow();
		
		this.inputComponentsList.newRow();
		this.inputComponentsList.addComponentCurrentRow(new Static(this, this.inputComponentsList.nextStart(63), 55, 0, 15, HorizontalSizing.LEFT_WIDTH, null, "datapack_edit.mob_patch.combat_behavior.animation"));
		this.inputComponentsList.addComponentCurrentRow(animationPopupBox.relocateX(parentScreen.getRectangle(), this.inputComponentsList.nextStart(5)));
		
		if (this.rootTag.contains("combat_behavior") || this.isHumanoidSubTag && this.rootTag.contains("behavior_series")) {
			Grid.PackImporter packImporter = new Grid.PackImporter();
			ListTag list = this.rootTag.getList(this.isHumanoidSubTag ? "behavior_series" : "combat_behavior", Tag.TAG_COMPOUND);
			
			for (int i = 0; i < list.size(); i++) {
				this.movesetList.add(list.getCompound(i));
				packImporter.newRow();
				packImporter.newValue("movement_set", "movement set" + (i+1));
			}
			
			this.movesetGrid._setValue(packImporter);
		}
		
		this.inputComponentsList.setComponentsActive(false);
	}
	
	@Override
	protected void init() {
		ScreenRectangle screenRectangle = this.getRectangle();
		this.movesetGrid.resize(screenRectangle);
		
		this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, (button) -> {
			ListTag newListTag = new ListTag();
			int idx = 0;
			
			for (CompoundTag tag : this.movesetList) {
				try {
					this.validateTagSave(tag);
					newListTag.add(tag);
					idx++;
				} catch (Exception e) {
					this.minecraft.setScreen(new MessageScreen<>("Save Failed", "Failed to save " + this.movesetGrid.getValue(idx, "movement_set") + ": " + e.getMessage(), this, (button2) -> {
						this.minecraft.setScreen(this);
					}, 180, 90).autoCalculateHeight());
					return;
				}
			}
			
			if (this.isHumanoidSubTag) {
				this.rootTag.remove("behavior_series");
				this.rootTag.put("behavior_series", newListTag);
			} else {
				this.rootTag.remove("combat_behavior");
				this.rootTag.put("combat_behavior", newListTag);
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
		
		int splitPos = (int)(this.width * 0.6F);
		
		this.inputComponentsList.updateSizeAndPosition(splitPos - 70, screenRectangle.bottom() - screenRectangle.top() - 85, screenRectangle.top() + 35);
		this.inputComponentsList.setX(70);
		
		this.modelPreviewer.setX1(splitPos + 6);
		this.modelPreviewer.resize(screenRectangle);
		
		this.addRenderableWidget(this.movesetGrid);
		this.addRenderableWidget(this.inputComponentsList);
		this.addRenderableWidget(this.modelPreviewer);
	}
	
	@Override
	public void onClose() {
		this.minecraft.setScreen(this.parentScreen);
		this.modelPreviewer.onDestroy();
	}
	
	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
		if (this.modelPreviewer.mouseDragged(mouseX, mouseY, button, dx, dy)) {
			return true;
		}
		
		if (this.inputComponentsList.mouseDragged(mouseX, mouseY, button, dx, dy)) {
			return true;
		}
		
		return super.mouseDragged(mouseX, mouseY, button, dx, dy);
	}
	
	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		super.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
		
		guiGraphics.drawString(this.font, this.title, 20, 16, 16777215);
		
		for (Renderable renderable : this.renderables) {
            renderable.render(guiGraphics, mouseX, mouseY, partialTick);
        }
	}
	
	@Override
	public void tick() {
		this.modelPreviewer._tick();
	}
	
	private void rearrangeAttackAnimation() {
		this.modelPreviewer.clearAnimations();
		CompoundTag tag = this.movesetList.get(this.movesetGrid.getRowposition());
		
		for (Tag behaviorTag : tag.getList("behaviors", Tag.TAG_COMPOUND)) {
			CompoundTag behaviorCompound = (CompoundTag)behaviorTag;
			AssetAccessor<? extends StaticAnimation> animation = DatapackEditScreen.animationByKey(behaviorCompound.getString("animation"));
			
			if (animation != null) {
				this.modelPreviewer.addAnimationToPlay(animation);
			}
		}
	}
	
	private void validateTagSave(CompoundTag tag) throws IllegalStateException {
		if (!tag.contains("weight")) {
			throw new IllegalStateException("Define a weight value");
		}
		
		if (tag.getInt("cooldown") < 0) {
			throw new IllegalStateException("Cooldown can't be a negative value");
		}
		
		if (!tag.contains("canBeInterrupted")) {
			throw new IllegalStateException("Define interceptability");
		}
		
		if (!tag.contains("looping")) {
			throw new IllegalStateException("Define looping");
		}
		
		if (!tag.contains("behaviors") || tag.getList("behaviors", Tag.TAG_COMPOUND).size() == 0) {
			throw new IllegalStateException("Define at least one behavior");
		}
		
		int idx = 1;
		
		for (Tag behaviorTag : tag.getList("behaviors", Tag.TAG_COMPOUND)) {
			CompoundTag behaviorCompound = (CompoundTag)behaviorTag;
			
			if (!behaviorCompound.contains("animation") || StringUtil.isNullOrEmpty(behaviorCompound.getString("animation"))) {
				throw new IllegalStateException("Behavior" + idx + ": No animation defined.");
			}
			
			if (!behaviorCompound.contains("conditions") || behaviorCompound.getList("conditions", Tag.TAG_COMPOUND).size() == 0) {
				throw new IllegalStateException("Behavior" + idx + ": Define at least one condition");
			}
			
			for (Tag conditionTag : behaviorCompound.getList("conditions", Tag.TAG_COMPOUND)) {
				CompoundTag conditionCompound = (CompoundTag)conditionTag;
				
				if (!conditionCompound.contains("predicate") || StringUtil.isNullOrEmpty(conditionCompound.getString("predicate"))) {
					throw new IllegalStateException("Behavior" + idx + ": Condition not specified");
				}
				
				try {
					Supplier<Condition<?>> condition = EpicFightConditions.getConditionOrThrow(ResourceLocation.parse(conditionCompound.getString("predicate")));
					condition.get().read(conditionCompound);
				} catch (Exception e) {
					throw new IllegalStateException("Behavior" + idx + ": " + e.getMessage());
				}
			}
			
			idx++;
		}
	}
}