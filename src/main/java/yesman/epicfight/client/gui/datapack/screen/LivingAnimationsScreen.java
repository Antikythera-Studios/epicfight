package yesman.epicfight.client.gui.datapack.screen;
import net.minecraft.client.Minecraft;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import yesman.epicfight.api.animation.LivingMotion;
import yesman.epicfight.api.animation.LivingMotions;
import yesman.epicfight.api.animation.types.MainFrameAnimation;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.client.model.Meshes;
import yesman.epicfight.api.utils.ParseUtil;
import yesman.epicfight.api.utils.datastructure.ModifiablePair;
import yesman.epicfight.client.gui.datapack.widgets.Grid;
import yesman.epicfight.client.gui.datapack.widgets.Grid.GridBuilder.RowEditButton;
import yesman.epicfight.client.gui.datapack.widgets.ModelPreviewer;
import yesman.epicfight.client.gui.datapack.widgets.PopupBox;
import yesman.epicfight.client.gui.datapack.widgets.ResizableComponent.HorizontalSizing;
import yesman.epicfight.client.gui.datapack.widgets.ResizableComponent.VerticalSizing;
import yesman.epicfight.client.gui.datapack.widgets.Static;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.gameasset.Armatures;
import yesman.epicfight.world.capabilities.item.CapabilityItem.Styles;
import yesman.epicfight.world.capabilities.item.Style;

public class LivingAnimationsScreen extends Screen {
	private final Screen parentScreen;
	private final Grid stylesGrid;
	private Grid animationsGrid;
	private final ModelPreviewer modelPreviewer;
	
	private final List<PackEntry<String, List<ModifiablePair<String, String>>>> styles = Lists.newArrayList();
	private final CompoundTag rootTag;
	
	public LivingAnimationsScreen(Screen parentScreen, CompoundTag rootTag) {
		super(Component.translatable("datapack_edit.weapon_type.living_animations"));
		
		this.parentScreen = parentScreen;
		this.minecraft = Minecraft.getInstance();
		this.rootTag = rootTag;
		this.font = Minecraft.getInstance().font;
		
		this.modelPreviewer = new ModelPreviewer(106, 200, 60, 49, HorizontalSizing.LEFT_RIGHT, VerticalSizing.TOP_BOTTOM, Armatures.BIPED, Meshes.BIPED);
		
		this.stylesGrid = Grid.builder(this, Minecraft.getInstance())
								.xy1(12, 60)
								.xy2(85, 50)
								.horizontalSizing(HorizontalSizing.LEFT_WIDTH)
								.verticalSizing(VerticalSizing.TOP_BOTTOM)
								.rowHeight(21)
								.rowEditable(RowEditButton.ADD_REMOVE)
								.rowpositionChanged((rowposition, values) -> {
									Grid.PackImporter importer = new Grid.PackImporter();
									
									for (ModifiablePair<String, String> entry : this.styles.get(rowposition).getValue()) {
										importer.newRow().newValue("living_motion", LivingMotion.ENUM_MANAGER.get(entry.getFirst())).newValue("living_animation", DatapackEditScreen.animationByKey(entry.getSecond()));
									}
									
									this.animationsGrid._setActive(true);
									this.animationsGrid._setValue(importer);
								})
								.addColumn(Grid.combo("style", Style.ENUM_MANAGER.universalValues())
												.valueChanged((event) -> this.styles.get(event.rowposition).setPackKey(ParseUtil.nullParam(event.postValue).toLowerCase(Locale.ROOT)))
												.defaultVal(Styles.ONE_HAND))
								.pressAdd((grid, button) -> {
									this.styles.add(PackEntry.ofValue("", Lists.newArrayList()));
									int rowposition = grid.addRow();
									grid.setGridFocus(rowposition, "style");
								})
								.pressRemove((grid, button) -> {
									grid.removeRow((removedRow) -> this.styles.remove(removedRow));
									
									if (grid.children().size() == 0) {
										this.animationsGrid._setActive(false);
									}
								})
								.build();
		
		this.animationsGrid = Grid.builder(this, Minecraft.getInstance())
									.xy1(177, 60)
									.xy2(14, 49)
									.horizontalSizing(HorizontalSizing.WIDTH_RIGHT)
									.verticalSizing(VerticalSizing.TOP_BOTTOM)
									.rowHeight(21)
									.rowEditable(RowEditButton.ADD_REMOVE)
									.addColumn(Grid.combo("living_motion", List.of(LivingMotions.IDLE, LivingMotions.WALK, LivingMotions.RUN, LivingMotions.SNEAK, LivingMotions.SWIM, LivingMotions.FLOAT, LivingMotions.KNEEL, LivingMotions.FALL,
											LivingMotions.SIT, LivingMotions.FLY, LivingMotions.CREATIVE_FLY, LivingMotions.CREATIVE_IDLE, LivingMotions.BLOCK, LivingMotions.RELOAD, LivingMotions.AIM, LivingMotions.SHOT))
													.valueChanged((event) -> {
														List<ModifiablePair<String, String>> styleAnimations = this.styles.get(this.stylesGrid.getRowposition()).getValue();
														styleAnimations.get(event.rowposition).setFirst(ParseUtil.nullParam(event.postValue).toLowerCase(Locale.ROOT));
														
														this.modelPreviewer.clearAnimations();
														this.modelPreviewer.getAnimator().getEntityPatch().currentLivingMotion = event.postValue;
														this.modelPreviewer.getAnimator().getEntityPatch().currentCompositeMotion = event.postValue;
														
														if (LIVING_ANIMTIONS.containsKey(event.postValue)) {
															this.modelPreviewer.addAnimationToPlay(LIVING_ANIMTIONS.get(event.postValue));
														}
														
														AssetAccessor<? extends StaticAnimation> livingAnimation = this.animationsGrid.getValue(event.rowposition, "living_animation");
														
														if (livingAnimation != null) {
															this.modelPreviewer.getAnimator().playAnimation(livingAnimation, 0.0F);
														}
													}).defaultVal(LivingMotions.IDLE))
									.addColumn(Grid.popup("living_animation", PopupBox.AnimationPopupBox::new)
													.filter((accessor) -> !(accessor.checkType(MainFrameAnimation.class)))
													.editWidgetCreated((popupBox) -> popupBox.setModel(Armatures.BIPED, Meshes.BIPED))
													.valueChanged((event) -> {
														this.modelPreviewer.clearAnimations();
														
														LivingMotion livingMotion = event.grid.getValue(event.rowposition, "living_motion");
														List<ModifiablePair<String, String>> styleAnimations = this.styles.get(this.stylesGrid.getRowposition()).getValue();
														styleAnimations.get(event.rowposition).setSecond(ParseUtil.nullOrToString(event.postValue, (animation) -> animation.registryName().toString()));
														
														if (LIVING_ANIMTIONS.containsKey(livingMotion)) {
															this.modelPreviewer.addAnimationToPlay(LIVING_ANIMTIONS.get(livingMotion));
														}
														
														if (event.postValue != null) {
															this.modelPreviewer.addAnimationToPlay(event.postValue);
														}
													})
													.toDisplayText((animation) -> animation == null ? "" : animation.registryName().toString())
													.width(150))
									.pressAdd((grid, button) -> {
										this.styles.get(this.stylesGrid.getRowposition()).getValue().add(ModifiablePair.of("", ""));
										int rowposition = grid.addRow();
										grid.setGridFocus(rowposition, "living_animation");
									})
									.pressRemove((grid, button) -> {
										this.styles.get(this.stylesGrid.getRowposition()).getValue().remove(grid.getRowposition());
										grid.removeRow();
									})
									.rowpositionChanged((rowposition, values) -> {
										this.modelPreviewer.clearAnimations();
										LivingMotion livingMotion = (LivingMotion)values.get("living_motion");
										this.modelPreviewer.getAnimator().getEntityPatch().currentLivingMotion = livingMotion;
										this.modelPreviewer.getAnimator().getEntityPatch().currentCompositeMotion = livingMotion;
										
										if (LIVING_ANIMTIONS.containsKey(livingMotion)) {
											this.modelPreviewer.addAnimationToPlay(LIVING_ANIMTIONS.get(livingMotion));
										}
										
										@SuppressWarnings("unchecked")
										AssetAccessor<? extends StaticAnimation> animation = (AssetAccessor<? extends StaticAnimation>)values.get("living_animation");
										
										if (animation != null) {
											this.modelPreviewer.getAnimator().playAnimation(animation, 0.0F);
										}
									})
									.build();
		
		this.animationsGrid._setActive(false);
		
		Grid.PackImporter packImporter = new Grid.PackImporter();
		
		for (Map.Entry<String, Tag> entry : rootTag.tags.entrySet()) {
			List<ModifiablePair<String, String>> list = Lists.newArrayList();
			
			for (Map.Entry<String, Tag> entry$2 : ((CompoundTag)entry.getValue()).tags.entrySet()) {
				list.add(ModifiablePair.of(entry$2.getKey(), entry$2.getValue().getAsString()));
			}
			
			this.styles.add(PackEntry.ofValue(entry.getKey(), list));
			
			packImporter.newRow();
			packImporter.newValue("style", Style.ENUM_MANAGER.get(entry.getKey()));
		}
		
		this.stylesGrid._setValue(packImporter);
	}
	
	@Override
	protected void init() {
		ScreenRectangle screenRectangle = this.getRectangle();
		
		this.stylesGrid.resize(screenRectangle);
		this.animationsGrid.resize(screenRectangle);
		this.modelPreviewer.resize(screenRectangle);
		
		this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, (button) -> {
			Set<String> styles = Sets.newHashSet();
			
			for (PackEntry<String, List<ModifiablePair<String, String>>> entry : this.styles) {
				if (styles.contains(entry.getKey())) {
					this.minecraft.setScreen(new MessageScreen<>("Save Failed", "Unable to save because of duplicated style: " + entry.getKey(), this, (button2) -> {
						this.minecraft.setScreen(this);
					}, 180, 90));
					return;
				}
				styles.add(entry.getKey());
			}

			boolean allTagsNormal = true;
			String animation = null;
			String style = null;
			
			exit:
			for (PackEntry<String, List<ModifiablePair<String, String>>> entry : this.styles) {
				for (ModifiablePair<String, String> pair : entry.getValue()) {
					if (DatapackEditScreen.animationByKey(pair.getSecond()) == null) {
						animation = pair.getSecond();
						style = entry.getKey();
						allTagsNormal = false;
						break exit;
					}
				}
			}
			
			if (!allTagsNormal) {
				this.minecraft.setScreen(new MessageScreen<>("Save Failed", "No animation named: " + animation + " in " + style, this, (button2) -> {
					this.minecraft.setScreen(this);
				}, 180, 90));
			} else {
				this.rootTag.tags.clear();
				
				for (PackEntry<String, List<ModifiablePair<String, String>>> entry : this.styles) {
					CompoundTag compTag = new CompoundTag();
					
					for (ModifiablePair<String, String> pair : entry.getValue()) {
						compTag.putString(pair.getFirst(), pair.getSecond());
					}
					
					this.rootTag.put(entry.getKey(), compTag);
				}
				
				this.onClose();
			}
		}).pos(this.width / 2 - 162, this.height - 32).size(160, 21).build());
		
		this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, (button) -> {
			this.minecraft.setScreen(new MessageScreen<>("", "Do you want to quit without saving changes?", this,
														(button2) -> {
															this.onClose();
														}, (button2) -> {
															this.minecraft.setScreen(this);
														}, 180, 70));
		}).pos(this.width / 2 + 2, this.height - 32).size(160, 21).build());
		
		this.addRenderableWidget(new Static(this, 14, 100, 40, 15, HorizontalSizing.LEFT_WIDTH, null, Component.translatable("datapack_edit.styles"), Component.translatable("datapack_edit.styles.tooltip.mandatory")));
		this.addRenderableWidget(this.stylesGrid);
		this.addRenderableWidget(this.modelPreviewer);
		this.addRenderableWidget(new Static(this, this.width - 188, 100, 40, 15, HorizontalSizing.LEFT_WIDTH, null, "datapack_edit.weapon_type.living_animations.modifiers"));
		this.addRenderableWidget(this.animationsGrid);
	}
	
	@Override
	public void onClose() {
		this.minecraft.setScreen(this.parentScreen);
		this.modelPreviewer.onDestroy();
	}
	
	@Override
	public void tick() {
		this.modelPreviewer._tick();
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
	
	private static final Map<LivingMotion, AssetAccessor<? extends StaticAnimation>> LIVING_ANIMTIONS = Maps.newHashMap();
	
	static {
		LIVING_ANIMTIONS.put(LivingMotions.IDLE, Animations.BIPED_IDLE);
		LIVING_ANIMTIONS.put(LivingMotions.WALK, Animations.BIPED_WALK);
		LIVING_ANIMTIONS.put(LivingMotions.RUN, Animations.BIPED_RUN);
		LIVING_ANIMTIONS.put(LivingMotions.SNEAK, Animations.BIPED_SNEAK);
		LIVING_ANIMTIONS.put(LivingMotions.SWIM, Animations.BIPED_SWIM);
		LIVING_ANIMTIONS.put(LivingMotions.FLOAT, Animations.BIPED_FLOAT);
		LIVING_ANIMTIONS.put(LivingMotions.KNEEL, Animations.BIPED_KNEEL);
		LIVING_ANIMTIONS.put(LivingMotions.FALL, Animations.BIPED_FALL);
		LIVING_ANIMTIONS.put(LivingMotions.MOUNT, Animations.BIPED_MOUNT);
		LIVING_ANIMTIONS.put(LivingMotions.SIT, Animations.BIPED_SIT);
		LIVING_ANIMTIONS.put(LivingMotions.FLY, Animations.BIPED_FLYING);
		LIVING_ANIMTIONS.put(LivingMotions.DEATH, Animations.BIPED_DEATH);
		LIVING_ANIMTIONS.put(LivingMotions.JUMP, Animations.BIPED_JUMP);
		LIVING_ANIMTIONS.put(LivingMotions.CLIMB, Animations.BIPED_CLIMBING);
		LIVING_ANIMTIONS.put(LivingMotions.SLEEP, Animations.BIPED_SLEEPING);
		LIVING_ANIMTIONS.put(LivingMotions.CREATIVE_FLY, Animations.BIPED_CREATIVE_FLYING);
		LIVING_ANIMTIONS.put(LivingMotions.CREATIVE_IDLE, Animations.BIPED_CREATIVE_IDLE);
	}
}