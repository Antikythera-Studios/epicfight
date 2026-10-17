package yesman.epicfight.client.gui.datapack.screen;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.client.model.SkinnedMesh;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.api.utils.ParseUtil;
import yesman.epicfight.client.gui.datapack.widgets.ComboBox;
import yesman.epicfight.client.gui.datapack.widgets.Grid;
import yesman.epicfight.client.gui.datapack.widgets.Grid.GridBuilder.RowEditButton;
import yesman.epicfight.client.gui.datapack.widgets.InputComponentList;
import yesman.epicfight.client.gui.datapack.widgets.ResizableComponent.HorizontalSizing;
import yesman.epicfight.client.gui.datapack.widgets.ResizableComponent.VerticalSizing;
import yesman.epicfight.client.gui.datapack.widgets.Static;
import yesman.epicfight.client.gui.datapack.widgets.SubScreenOpenButton;
import yesman.epicfight.world.capabilities.item.CapabilityItem.WeaponCategories;
import yesman.epicfight.world.capabilities.item.Style;
import yesman.epicfight.world.capabilities.item.WeaponCategory;

public class HumanoidCombatBehaviorScreen extends Screen {
	private final Screen parentScreen;
	private final InputComponentList<CompoundTag> inputComponentsList;
	private final CompoundTag rootTag;
	private final List<CompoundTag> weaponList = Lists.newLinkedList();
	
	private Grid weaponGrid;
	private Grid weaponCategoriesGrid;
	private ComboBox<Style> styleCombo;
	
	public HumanoidCombatBehaviorScreen(Screen parentScreen, CompoundTag rootTag, AssetAccessor<? extends Armature> armature, AssetAccessor<? extends SkinnedMesh> mesh) {
		super(Component.translatable("datapack_edit.mob_patch.humanoid_combat_behavior"));
		
		this.parentScreen = parentScreen;
		this.minecraft = Minecraft.getInstance();
		this.font = Minecraft.getInstance().font;
		
		this.rootTag = rootTag;
		this.inputComponentsList = new InputComponentList<>(this, 0, 0, 0, 26) {
			@Override
			public void importTag(CompoundTag tag) {
				this.setComponentsActive(true);
				
				ListTag weaponCategoriesList = tag.getList("weapon_categories", Tag.TAG_STRING);
				Grid.PackImporter packImporter = new Grid.PackImporter();
				
				for (Tag weaponCategory : weaponCategoriesList) {
					packImporter.newRow();
					packImporter.newValue("weapon_category", weaponCategory.getAsString());
				}
				
				this.setDataBindingComponenets(new Object[] {
					packImporter,
					Style.ENUM_MANAGER.get(tag.getString("style"))
				});
			}
		};
		this.inputComponentsList.setX(150);
		
		this.weaponGrid = Grid.builder(this, Minecraft.getInstance())
								.xy1(12, 50)
								.xy2(130, 50)
								.horizontalSizing(HorizontalSizing.LEFT_WIDTH)
								.verticalSizing(VerticalSizing.TOP_BOTTOM)
								.rowHeight(26)
								.rowEditable(RowEditButton.ADD_REMOVE)
								.rowpositionChanged((rowposition, values) -> {
									this.inputComponentsList.importTag(this.weaponList.get(rowposition));
								})
								.addColumn(Grid.editbox("weapon")
												.editable(false)
												.width(180))
								.pressAdd((grid, button) -> {
									grid.setValueChangeEnabled(false);
									int rowposition = grid.addRowWithDefaultValues("weapon", String.format("weapon%d", grid.children().size() + 1));
									this.weaponList.add(rowposition, new CompoundTag());
									grid.setGridFocus(rowposition, "weapon");
									grid.setValueChangeEnabled(true);
									this.setFocused(grid);
									
									if (grid.children().size() > 0) {
										this.inputComponentsList.setComponentsActive(true);
									}
								})
								.pressRemove((grid, button) -> {
									grid.removeRow((removedRow) -> this.weaponList.remove(removedRow));
									
									if (this.weaponList.size() == 0) {
										this.inputComponentsList.setComponentsActive(false);
									}
								})
								.build();
		
		this.weaponCategoriesGrid = Grid.builder(this, Minecraft.getInstance())
									.xy1(4, 40)
									.xy2(12, 120)
									.horizontalSizing(HorizontalSizing.LEFT_RIGHT)
									.rowHeight(21)
									.rowEditable(RowEditButton.ADD_REMOVE)
									.addColumn(Grid.combo("weapon_category", List.copyOf(WeaponCategories.ENUM_MANAGER.universalValues()))
													.editable(true)
													.valueChanged((event) -> {
														CompoundTag weaponBehaviorTag = this.weaponList.get(this.weaponGrid.getRowposition());
														ListTag weaponCategoriesList = weaponBehaviorTag.getList("weapon_categories", Tag.TAG_STRING);
														weaponCategoriesList.set(event.rowposition, StringTag.valueOf(ParseUtil.nullParam(event.postValue).toLowerCase(Locale.ROOT)));
													}).width(150))
									.pressAdd((grid, button) -> {
										grid.setValueChangeEnabled(false);
										int rowposition = grid.addRow();
										
										CompoundTag weaponBehaviorTag = this.weaponList.get(this.weaponGrid.getRowposition());
										ListTag weaponCategoriesList = ParseUtil.getOrDefaultTag(weaponBehaviorTag, "weapon_categories", new ListTag());
										weaponCategoriesList.add(rowposition, StringTag.valueOf(""));
										
										grid.setGridFocus(rowposition, "weapon_category");
										grid.setValueChangeEnabled(true);
									})
									.pressRemove((grid, button) -> {
										grid.removeRow((removedRow) -> {
											CompoundTag weaponBehaviorTag = this.weaponList.get(this.weaponGrid.getRowposition());
											ListTag weaponCategoriesList = weaponBehaviorTag.getList("weapon_categories", Tag.TAG_STRING);
											weaponCategoriesList.remove(removedRow);
										});
									})
									.build();
		
		this.styleCombo = new ComboBox<> (this, this.font, 55, 116, 0, 16, HorizontalSizing.LEFT_WIDTH, null, 8, Component.translatable("datapack_edit.style"),
				new ArrayList<>(Style.ENUM_MANAGER.universalValues()), ParseUtil::snakeToSpacedCamel, (style) -> {
					CompoundTag weaponBehaviorTag = this.weaponList.get(this.weaponGrid.getRowposition());
					weaponBehaviorTag.putString("style", ParseUtil.nullParam(style).toLowerCase(Locale.ROOT));
				});
		
		this.inputComponentsList.newRow();
		this.inputComponentsList.addComponentCurrentRow(new Static(this, this.inputComponentsList.nextStart(4), 100, 100, 15, HorizontalSizing.LEFT_WIDTH, null, Component.translatable("datapack_edit.mob_patch.humanoid_weapon_motions.weapon_categories")));
		this.inputComponentsList.newRow();
		this.inputComponentsList.newRow();
		this.inputComponentsList.newRow();
		this.inputComponentsList.addComponentCurrentRow(this.weaponCategoriesGrid);
		this.inputComponentsList.newRow();
		this.inputComponentsList.newRow();
		
		this.inputComponentsList.newRow();
		this.inputComponentsList.addComponentCurrentRow(new Static(this, this.inputComponentsList.nextStart(4), 100, 100, 15, HorizontalSizing.LEFT_WIDTH, null, Component.translatable("datapack_edit.style")));
		this.inputComponentsList.addComponentCurrentRow(this.styleCombo.relocateX(this.getRectangle(), this.inputComponentsList.nextStart(5)));
		
		this.inputComponentsList.newRow();
		this.inputComponentsList.addComponentCurrentRow(new Static(this, this.inputComponentsList.nextStart(4), 100, 100, 15, HorizontalSizing.LEFT_WIDTH, null, Component.translatable("datapack_edit.mob_patch.combat_behavior")));
		this.inputComponentsList.addComponentCurrentRow(SubScreenOpenButton.builder().subScreen(() -> {
			return new CombatBehaviorScreen(this, this.weaponList.get(this.weaponGrid.getRowposition()), armature, mesh, true);
		}).bounds(this.inputComponentsList.nextStart(4), 0, 15, 15).build());
		
		if (this.rootTag.contains("combat_behavior_humanoid")) {
			Grid.PackImporter packImporter = new Grid.PackImporter();
			ListTag list = this.rootTag.getList("combat_behavior_humanoid", Tag.TAG_COMPOUND);
			
			for (int i = 0; i < list.size(); i++) {
				this.weaponList.add(list.getCompound(i));
				packImporter.newRow();
				packImporter.newValue("weapon", "weapon" + (i+1));
			}
			
			this.weaponGrid._setValue(packImporter);
		}
		
		this.inputComponentsList.setComponentsActive(false);
	}
	
	@Override
	protected void init() {
		ScreenRectangle screenRectangle = this.getRectangle();
		
		this.inputComponentsList.updateSizeAndPosition(screenRectangle.width() - 150, screenRectangle.height() - screenRectangle.top() - 90, screenRectangle.top() + 45);
		this.inputComponentsList.setX(150);
		
		this.weaponGrid.resize(screenRectangle);
		
		this.addRenderableWidget(Button.builder(CommonComponents.GUI_OK, (button) -> {
			ListTag newListTag = new ListTag();
			int idx = 0;
			
			for (CompoundTag tag : this.weaponList) {
				try {
					this.validateTagSave(tag);
					newListTag.add(tag);
					idx++;
				} catch (Exception e) {
					this.minecraft.setScreen(new MessageScreen<>("Save Failed", "Failed to save " + this.weaponGrid.getValue(idx, "weapon") + ": " + e.getMessage(), this, (button2) -> {
						this.minecraft.setScreen(this);
					}, 180, 90).autoCalculateHeight());
					return;
				}
			}
			
			this.rootTag.remove("combat_behavior_humanoid");
			this.rootTag.put("combat_behavior_humanoid", newListTag);
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
		
		this.addRenderableWidget(this.weaponGrid);
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
		
		for (Tag weaponCategoryTag : tag.getList("weapon_categories", Tag.TAG_STRING)) {
			if (StringUtil.isNullOrEmpty(weaponCategoryTag.getAsString())) {
				throw new IllegalStateException("Invalid weapon category");
			}
			
			try {
				WeaponCategory.ENUM_MANAGER.getOrThrow(weaponCategoryTag.getAsString());
			} catch (IllegalArgumentException e) {
				throw new IllegalStateException("Invalid weapon category " + weaponCategoryTag.getAsString());
			}
		}
		
		if (!tag.contains("style") || StringUtil.isNullOrEmpty(tag.getString("style"))) {
			throw new IllegalStateException("Define a style");
		}
		
		if (!tag.contains("behavior_series")) {
			throw new IllegalStateException("Define combat behavior");
		}
	}
}