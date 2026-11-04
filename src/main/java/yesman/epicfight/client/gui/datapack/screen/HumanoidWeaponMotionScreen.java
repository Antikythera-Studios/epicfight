package yesman.epicfight.client.gui.datapack.screen;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.google.common.collect.Lists;

import io.netty.util.internal.StringUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import yesman.epicfight.api.animation.LivingMotion;
import yesman.epicfight.api.animation.LivingMotions;
import yesman.epicfight.api.animation.types.MainFrameAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.client.model.SkinnedMesh;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.api.utils.ParseUtil;
import yesman.epicfight.client.gui.datapack.widgets.ComboBox;
import yesman.epicfight.client.gui.datapack.widgets.Grid;
import yesman.epicfight.client.gui.datapack.widgets.Grid.GridBuilder.RowEditButton;
import yesman.epicfight.client.gui.datapack.widgets.InputComponentList;
import yesman.epicfight.client.gui.datapack.widgets.PopupBox;
import yesman.epicfight.client.gui.datapack.widgets.ResizableComponent.HorizontalSizing;
import yesman.epicfight.client.gui.datapack.widgets.ResizableComponent.VerticalSizing;
import yesman.epicfight.client.gui.datapack.widgets.Static;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.capabilities.item.Style;
import yesman.epicfight.world.capabilities.item.WeaponCategory;

public class HumanoidWeaponMotionScreen extends Screen {
	private Grid motionSetGrid;
	private InputComponentList<CompoundTag> inputComponentsList;
	private final List<CompoundTag> motionSetList = Lists.newLinkedList();
	private final Screen parentScreen;
	private final CompoundTag rootTag;
	
	protected HumanoidWeaponMotionScreen(Screen parentScreen, CompoundTag rootTag, AssetAccessor<? extends Armature> armature, AssetAccessor<? extends SkinnedMesh> mesh) {
		super(Component.translatable("datapack_edit.mob_patch.humanoid_weapon_motions"));
		
		this.parentScreen = parentScreen;
		this.minecraft = Minecraft.getInstance();
		this.font = Minecraft.getInstance().font;
		
		this.rootTag = rootTag;
		this.motionSetGrid = Grid.builder(this, Minecraft.getInstance())
									.xy1(8, 45)
									.xy2(150, 50)
									.horizontalSizing(HorizontalSizing.LEFT_WIDTH)
									.verticalSizing(VerticalSizing.TOP_BOTTOM)
									.rowHeight(26)
									.rowEditable(RowEditButton.ADD_REMOVE)
									.rowpositionChanged((rowposition, values) -> {
										this.inputComponentsList.importTag(this.motionSetList.get(rowposition));
									})
									.addColumn(Grid.editbox("motion_set")
													.editable(false)
													.width(180))
									.pressAdd((grid, button) -> {
										grid.setValueChangeEnabled(false);
										int rowposition = grid.addRowWithDefaultValues("motion_set", String.format("set%d", grid.children().size() + 1));
										this.motionSetList.add(rowposition, new CompoundTag());
										grid.setGridFocus(rowposition, "motion_set");
										grid.setValueChangeEnabled(true);
										this.setFocused(grid);
										
										if (grid.children().size() > 0) {
											this.inputComponentsList.setComponentsActive(true);
										}
									})
									.pressRemove((grid, button) -> {
										grid.removeRow((removedRow) -> this.motionSetList.remove(removedRow));
										
										if (this.motionSetList.size() == 0) {
											this.inputComponentsList.setComponentsActive(false);
										}
									})
									.build();
		
		this.inputComponentsList = new InputComponentList<>(this, 0, 0, 0, 28) {
			@Override
			public void importTag(CompoundTag tag) {
				Grid.PackImporter categories = new Grid.PackImporter();
				
				for (Tag weaponCategory : tag.getList("weapon_categories", Tag.TAG_STRING)) {
					categories.newRow();
					categories.newValue("weapon_category", WeaponCategory.ENUM_MANAGER.get(weaponCategory.getAsString()));
				}
				
				Grid.PackImporter livingMotions = new Grid.PackImporter();
				
				for (Map.Entry<String, Tag> entry : tag.getCompound("livingmotions").tags.entrySet()) {
					livingMotions.newRow();
					livingMotions.newValue("living_motion", LivingMotion.ENUM_MANAGER.get(entry.getKey()));
					livingMotions.newValue("animation", DatapackEditScreen.animationByKey(entry.getValue().getAsString()));
				}
				
				Style style = Style.ENUM_MANAGER.get(tag.getString("style"));
				
				this.setDataBindingComponenets(new Object[] {
					categories,
					style,
					livingMotions,
				});
				
				this.setComponentsActive(true);
			}
		};
		
		this.inputComponentsList.setX(164);
		
		this.inputComponentsList.newRow();
		this.inputComponentsList.addComponentCurrentRow(new Static(this, this.inputComponentsList.nextStart(5), 100, 0, 15, HorizontalSizing.LEFT_WIDTH, null, "datapack_edit.mob_patch.humanoid_weapon_motions.weapon_categories"));
		this.inputComponentsList.newRow();
		this.inputComponentsList.newRow();
		this.inputComponentsList.addComponentCurrentRow(Grid.builder(this, Minecraft.getInstance())
															.xy1(this.inputComponentsList.nextStart(5), 0)
															.xy2(10, 80)
															.horizontalSizing(HorizontalSizing.LEFT_RIGHT)
															.rowHeight(21)
															.rowEditable(RowEditButton.ADD_REMOVE)
															.addColumn(Grid.combo("weapon_category", List.of(CapabilityItem.WeaponCategories.values()))
																			.editable(true)
																			.valueChanged((event) -> {
																				ListTag listTag = ParseUtil.getOrDefaultTag(this.motionSetList.get(this.motionSetGrid.getRowposition()), "weapon_categories", new ListTag());
																				listTag.remove(event.rowposition);
																				listTag.add(event.rowposition, StringTag.valueOf(ParseUtil.nullParam(event.postValue).toLowerCase(Locale.ROOT)));
																			})
																			.width(180))
															.pressAdd((grid, button) -> {
																grid.setValueChangeEnabled(false);
																int rowposition = grid.addRow();
																ListTag listTag = ParseUtil.getOrDefaultTag(this.motionSetList.get(this.motionSetGrid.getRowposition()), "weapon_categories", new ListTag());
																listTag.add(rowposition, StringTag.valueOf(""));
																grid.setGridFocus(rowposition, "weapon_category");
																grid.setValueChangeEnabled(true);
															})
															.pressRemove((grid, button) -> {
																grid.removeRow((removedRow) -> {
																	ListTag listTag = ParseUtil.getOrDefaultTag(this.motionSetList.get(this.motionSetGrid.getRowposition()), "weapon_categories", new ListTag());
																	listTag.remove(removedRow);
																});
															})
															.build());
		this.inputComponentsList.newRow();
		
		this.inputComponentsList.newRow();
		this.inputComponentsList.addComponentCurrentRow(new Static(this, this.inputComponentsList.nextStart(5), 100, 0, 15, HorizontalSizing.LEFT_WIDTH, null, "datapack_edit.mob_patch.humanoid_weapon_motions.style"));
		this.inputComponentsList.addComponentCurrentRow(new ComboBox<>(this, this.font, this.inputComponentsList.nextStart(5), 116, 15, 53, HorizontalSizing.LEFT_WIDTH, VerticalSizing.HEIGHT_BOTTOM, 8, Component.translatable("datapack_edit.weapon_type.styles.default"),
				new ArrayList<>(Style.ENUM_MANAGER.universalValues()), ParseUtil::snakeToSpacedCamel, (style) -> {
					CompoundTag tag = this.motionSetList.get(this.motionSetGrid.getRowposition());
					tag.putString("style", ParseUtil.nullParam(style).toLowerCase(Locale.ROOT));
				}));
		
		this.inputComponentsList.newRow();
		this.inputComponentsList.addComponentCurrentRow(new Static(this, this.inputComponentsList.nextStart(5), 100, 0, 15, HorizontalSizing.LEFT_WIDTH, null, "datapack_edit.mob_patch.humanoid_weapon_motions.living_animations"));
		this.inputComponentsList.newRow();
		this.inputComponentsList.newRow();
		this.inputComponentsList.addComponentCurrentRow(Grid.builder(this, Minecraft.getInstance())
															.xy1(this.inputComponentsList.nextStart(5), 0)
															.xy2(10, 80)
															.horizontalSizing(HorizontalSizing.LEFT_RIGHT)
															.rowHeight(21)
															.rowEditable(RowEditButton.ADD_REMOVE)
															.addColumn(Grid.combo("living_motion", List.of(LivingMotions.IDLE, LivingMotions.WALK, LivingMotions.CHASE, LivingMotions.MOUNT, LivingMotions.FALL, LivingMotions.FLOAT, LivingMotions.DEATH, LivingMotions.RELOAD, LivingMotions.AIM))
																			.valueChanged((event) -> {
																				CompoundTag livingMotionTag = ParseUtil.getOrSupply(this.motionSetList.get(this.motionSetGrid.getRowposition()), "livingmotions", CompoundTag::new);
																				livingMotionTag.remove(ParseUtil.nullParam(event.prevValue));
																				livingMotionTag.putString(ParseUtil.nullOrToString(event.postValue, (livingmotion) -> livingmotion.name().toLowerCase(Locale.ROOT)), "");
																			}).editable(true).width(100))
															.addColumn(Grid.popup("animation", PopupBox.AnimationPopupBox::new).filter((accessor) -> !(accessor.checkType(MainFrameAnimation.class)))
																			.editWidgetCreated((popupBox) -> popupBox.setModel(armature, mesh))
																			.valueChanged((event) -> {
																				CompoundTag livingMotionTag = ParseUtil.getOrSupply(this.motionSetList.get(this.motionSetGrid.getRowposition()), "livingmotions", CompoundTag::new);
																				livingMotionTag.putString(ParseUtil.nullOrToString((LivingMotions)event.grid.getValue(event.rowposition, "living_motion"), (livingmotion) -> livingmotion.name().toLowerCase(Locale.ROOT)),
																											ParseUtil.nullOrToString(event.postValue, (animation) -> animation.registryName().toString()));
																			}).toDisplayText((item) -> item == null ? "" : item.registryName().toString()).width(150))
															.pressAdd((grid, button) -> {
																CompoundTag attributeTag = ParseUtil.getOrDefaultTag(this.motionSetList.get(this.motionSetGrid.getRowposition()), "livingmotions", new CompoundTag());
																attributeTag.putString("", "");
																
																int rowposition = grid.addRow();
																grid.setGridFocus(rowposition, "living_motion");
															})
															.pressRemove((grid, button) -> {
																int rowposition = grid.getRowposition();
																
																if (rowposition > -1) {
																	CompoundTag livingMotionTag = ParseUtil.getOrDefaultTag(this.motionSetList.get(this.motionSetGrid.getRowposition()), "livingmotions", new CompoundTag());
																	livingMotionTag.remove(ParseUtil.nullParam(grid.getValue(rowposition, "living_motion")).toLowerCase(Locale.ROOT));
																	grid.removeRow(rowposition);
																}
															})
															.build());
		this.inputComponentsList.newRow();
		
		if (this.rootTag.contains("humanoid_weapon_motions")) {
			Grid.PackImporter packImporter = new Grid.PackImporter();
			ListTag list = this.rootTag.getList("humanoid_weapon_motions", Tag.TAG_COMPOUND);
			
			for (int i = 0; i < list.size(); i++) {
				this.motionSetList.add(list.getCompound(i));
				packImporter.newRow();
				packImporter.newValue("motion_set", "set" + (i+1));
			}
			
			this.motionSetGrid._setValue(packImporter);
		}
		
		this.inputComponentsList.setComponentsActive(false);
	}
	
	@Override
	protected void init() {
		ScreenRectangle screenRectangle = this.getRectangle();
		this.motionSetGrid.resize(screenRectangle);
		
		this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, (button) -> {
			ListTag newListTag = new ListTag();
			int idx = 0;
			
			for (CompoundTag tag : this.motionSetList) {
				try {
					this.validateTagSave(tag);
					newListTag.add(tag);
					idx++;
				} catch (Exception e) {
					this.minecraft.setScreen(new MessageScreen<>("Save Failed", "Failed to save " + this.motionSetGrid.getValue(idx, "motion_set") + ": " + e.getMessage(), this, (button2) -> {
						this.minecraft.setScreen(this);
					}, 180, 90));
					return;
				}
			}
			
			this.rootTag.remove("humanoid_weapon_motions");
			this.rootTag.put("humanoid_weapon_motions", newListTag);
			
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
		
		this.inputComponentsList.updateSizeAndPosition(screenRectangle.width() - 172, screenRectangle.bottom() - screenRectangle.top() - 85, screenRectangle.top() + 35);
		this.inputComponentsList.setX(164);
		
		this.addRenderableWidget(this.motionSetGrid);
		this.addRenderableWidget(this.inputComponentsList);
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
		if (!tag.contains("weapon_categories") || tag.getList("weapon_categories", Tag.TAG_STRING).size() == 0) {
			throw new IllegalStateException("Define at least one weapon category");
		}
		
		if (!tag.contains("style") || StringUtil.isNullOrEmpty(tag.getString("style"))) {
			throw new IllegalStateException("Define a style");
		}
		
		if (!tag.contains("livingmotions") || tag.getCompound("livingmotions").tags.size() == 0) {
			throw new IllegalStateException("Define at least one living motion");
		}
		
		int idx = 1;
		
		for (Map.Entry<String, Tag> entry : tag.getCompound("livingmotions").tags.entrySet()) {
			if (StringUtil.isNullOrEmpty(entry.getKey())) {
				throw new IllegalStateException("Row " + idx + ": No living motion defined.");
			}
			
			if (StringUtil.isNullOrEmpty(entry.getValue().getAsString())) {
				throw new IllegalStateException("Row " + idx + ": No animation defined.");
			}
			
			idx++;
		}
	}
}