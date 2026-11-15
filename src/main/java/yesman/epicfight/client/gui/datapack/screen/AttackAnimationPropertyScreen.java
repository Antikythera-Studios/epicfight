package yesman.epicfight.client.gui.datapack.screen;
import net.minecraft.client.Minecraft;

import java.util.List;
import java.util.Locale;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.netty.util.internal.StringUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import yesman.epicfight.api.animation.Joint;
import yesman.epicfight.api.animation.types.datapack.EditorAnimation;
import yesman.epicfight.api.client.animation.property.TrailInfo;
import yesman.epicfight.api.utils.ParseUtil;
import yesman.epicfight.client.gui.datapack.widgets.ComboBox;
import yesman.epicfight.client.gui.datapack.widgets.Grid;
import yesman.epicfight.client.gui.datapack.widgets.Grid.GridBuilder.RowEditButton;
import yesman.epicfight.client.gui.datapack.widgets.InputComponentList;
import yesman.epicfight.client.gui.datapack.widgets.ModelPreviewer;
import yesman.epicfight.client.gui.datapack.widgets.ResizableComponent.HorizontalSizing;
import yesman.epicfight.client.gui.datapack.widgets.ResizableComponent.VerticalSizing;
import yesman.epicfight.client.gui.datapack.widgets.ResizableEditBox;
import yesman.epicfight.client.gui.datapack.widgets.Static;

public class AttackAnimationPropertyScreen extends Screen {
	private final Screen parentScreen;
	private final EditorAnimation animation;
	private final Grid trailGrid;
	private final InputComponentList<JsonObject> inputComponentsList;
	private final ResizableEditBox startTime;
	private final ResizableEditBox endTime;
	private final ComboBox<Joint> joint;
	private final ComboBox<InteractionHand> hand;
	private final ResizableEditBox interpolations;
	private final ResizableEditBox lifetime;
	private final ModelPreviewer modelPlayer;
	
	private JsonArray trailList = new JsonArray();
	
	protected AttackAnimationPropertyScreen(Screen parentScreen, EditorAnimation animation, List<Joint> joints, ModelPreviewer modelPlayer) {
		super(Component.translatable("datapack_edit.import_animation.client_data"));
		
		this.parentScreen = parentScreen;
		this.minecraft = Minecraft.getInstance();
		this.font = this.minecraft.font;
		
		this.animation = animation;
		this.modelPlayer = modelPlayer;
		
		this.inputComponentsList = new InputComponentList<> (this, 0, 0, 0, 30) {
			@Override
			public void importTag(JsonObject tag) {
				this.setComponentsActive(true);
				
				this.setDataBindingComponenets(new Object[] {
					ParseUtil.valueOfOmittingType(ParseUtil.nullOrToString(tag.get("start_time"), JsonElement::getAsString)),
					ParseUtil.valueOfOmittingType(ParseUtil.nullOrToString(tag.get("end_time"), JsonElement::getAsString)),
					ParseUtil.nullOrApply(tag.get("joint"), (jsonElement) -> AttackAnimationPropertyScreen.this.modelPlayer.getArmature().get().searchJointByName(jsonElement.getAsString())),
					ParseUtil.nullOrApply(tag.get("item_skin_hand"), (jsonElement) -> InteractionHand.valueOf(jsonElement.getAsString().toUpperCase(Locale.ROOT))),
					ParseUtil.nullParam(ParseUtil.nullOrToString(tag.get("interpolations"), JsonElement::getAsString)),
					ParseUtil.nullParam(ParseUtil.nullOrToString(tag.get("lifetime"), JsonElement::getAsString)),
				});
			}
		};
		
		ScreenRectangle screenRect = this.getRectangle();
		
		this.trailGrid = Grid.builder(parentScreen, Minecraft.getInstance())
								.xy1(15, 48)
								.xy2(100, 50)
								.verticalSizing(VerticalSizing.TOP_BOTTOM)
								.rowHeight(26)
								.rowEditable(RowEditButton.ADD_REMOVE)
								.rowpositionChanged((rowposition, values) -> {
									this.inputComponentsList.importTag(this.trailList.get(rowposition).getAsJsonObject());
								})
								.addColumn(Grid.editbox("trail")
												.editable(false)
												.width(200))
								.pressAdd((grid, button) -> {
									this.trailList.add(new JsonObject());
									int rowposition = grid.addRowWithDefaultValues("trail", String.format("Trail%d", grid.children().size()));
									grid.setGridFocus(rowposition, "trail");
								})
								.pressRemove((grid, button) -> {
									grid.removeRow((removedRow) -> {
										this.trailList.remove(removedRow);
									});
								})
								.build();
		
		this.startTime = new ResizableEditBox(this.font, 0, 35, 0, 15, Component.translatable("datapack_edit.import_animation.client_data.start_time"), HorizontalSizing.LEFT_WIDTH, null);
		this.endTime = new ResizableEditBox(this.font, 0, 35, 0, 15, Component.translatable("datapack_edit.import_animation.client_data.end_time"), HorizontalSizing.LEFT_WIDTH, null);
		this.interpolations = new ResizableEditBox(this.font, 0, 35, 0, 15, Component.translatable("datapack_edit.item_capability.interpolations"), HorizontalSizing.LEFT_WIDTH, null);
		this.lifetime = new ResizableEditBox(this.font, 0, 35, 0, 15, Component.translatable("datapack_edit.item_capability.lifetime"), HorizontalSizing.LEFT_WIDTH, null);
		this.joint = new ComboBox<>(this, this.font, 0, 124, 100, 15, HorizontalSizing.LEFT_WIDTH, null, 8, Component.translatable("datapack_edit.import_animation.joint"),
									joints, Joint::getName, (joint) -> {
										JsonObject trailObj = this.trailList.get(this.trailGrid.getRowposition()).getAsJsonObject();
										
										if (joint != null) {
											trailObj.addProperty("joint", joint.getName());
										} else {
											trailObj.remove("joint");
										}
									});
		this.hand = new ComboBox<>(this, this.font, 0, 124, 100, 15, HorizontalSizing.LEFT_WIDTH, null, 8,
									Component.translatable("datapack_edit.import_animation.hand"), List.of(InteractionHand.MAIN_HAND, InteractionHand.OFF_HAND),
									ParseUtil::snakeToSpacedCamel, (hand) -> {
										JsonObject trailObj = this.trailList.get(this.trailGrid.getRowposition()).getAsJsonObject();
										
										if (hand != null) {
											trailObj.addProperty("item_skin_hand", hand.toString().toLowerCase(Locale.ROOT));
										} else {
											trailObj.remove("item_skin_hand");
										}
									});
		
		this.startTime.setResponder((value) -> {
			JsonObject trailObj = this.trailList.get(this.trailGrid.getRowposition()).getAsJsonObject();
			
			if (!StringUtil.isNullOrEmpty(value)) {
				trailObj.addProperty("start_time", Float.parseFloat(value));
			} else {
				trailObj.remove("start_time");
			}
		});
		
		this.endTime.setResponder((value) -> {
			JsonObject trailObj = this.trailList.get(this.trailGrid.getRowposition()).getAsJsonObject();
			
			if (!StringUtil.isNullOrEmpty(value)) {
				trailObj.addProperty("end_time", Float.parseFloat(value));
			} else {
				trailObj.remove("end_time");
			}
		});
		
		this.interpolations.setResponder((value) -> {
			JsonObject trailObj = this.trailList.get(this.trailGrid.getRowposition()).getAsJsonObject();
			
			if (!StringUtil.isNullOrEmpty(value)) {
				trailObj.addProperty("interpolations", Integer.parseInt(value));
			} else {
				trailObj.remove("interpolations");
			}
		});
		
		this.lifetime.setResponder((value) -> {
			JsonObject trailObj = this.trailList.get(this.trailGrid.getRowposition()).getAsJsonObject();
			
			if (!StringUtil.isNullOrEmpty(value)) {
				trailObj.addProperty("lifetime", Integer.parseInt(value));
			} else {
				trailObj.remove("lifetime");
			}
		});
		
		this.startTime.setFilter((context) -> StringUtil.isNullOrEmpty(context) || ParseUtil.isParsable(context, Float::parseFloat));
		this.endTime.setFilter((context) -> StringUtil.isNullOrEmpty(context) || ParseUtil.isParsable(context, Float::parseFloat));
		this.interpolations.setFilter((context) -> StringUtil.isNullOrEmpty(context) || ParseUtil.isParsable(context, Integer::parseInt));
		this.lifetime.setFilter((context) -> StringUtil.isNullOrEmpty(context) || ParseUtil.isParsable(context, Integer::parseInt));
		
		this.inputComponentsList.newRow();
		this.inputComponentsList.addComponentCurrentRow(new Static(this, this.inputComponentsList.nextStart(4), 80, 60, 15, HorizontalSizing.LEFT_WIDTH, null, "datapack_edit.model_player.trail"));
		this.inputComponentsList.newRow();
		this.inputComponentsList.addComponentCurrentRow(new Static(this, this.inputComponentsList.nextStart(12), 80, 60, 15, HorizontalSizing.LEFT_WIDTH, null, "datapack_edit.import_animation.client_data.start_time"));
		this.inputComponentsList.addComponentCurrentRow(this.startTime.relocateX(screenRect, this.inputComponentsList.nextStart(5)));
		this.inputComponentsList.newRow();
		this.inputComponentsList.addComponentCurrentRow(new Static(this, this.inputComponentsList.nextStart(12), 80, 60, 15, HorizontalSizing.LEFT_WIDTH, null, "datapack_edit.import_animation.client_data.end_time"));
		this.inputComponentsList.addComponentCurrentRow(this.endTime.relocateX(screenRect, this.inputComponentsList.nextStart(5)));
		this.inputComponentsList.newRow();
		this.inputComponentsList.addComponentCurrentRow(new Static(this, this.inputComponentsList.nextStart(12), 80, 60, 15, HorizontalSizing.LEFT_WIDTH, null, "datapack_edit.import_animation.client_data.joint"));
		this.inputComponentsList.addComponentCurrentRow(this.joint.relocateX(screenRect, this.inputComponentsList.nextStart(5)));
		this.inputComponentsList.newRow();
		this.inputComponentsList.addComponentCurrentRow(new Static(this, this.inputComponentsList.nextStart(12), 80, 60, 15, HorizontalSizing.LEFT_WIDTH, null, "datapack_edit.import_animation.hand"));
		this.inputComponentsList.addComponentCurrentRow(this.hand.relocateX(screenRect, this.inputComponentsList.nextStart(5)));
		this.inputComponentsList.newRow();
		this.inputComponentsList.addComponentCurrentRow(new Static(this, this.inputComponentsList.nextStart(12), 80, 60, 15, HorizontalSizing.LEFT_WIDTH, null, "datapack_edit.item_capability.interpolations"));
		this.inputComponentsList.addComponentCurrentRow(this.interpolations.relocateX(screenRect, this.inputComponentsList.nextStart(5)));
		this.inputComponentsList.newRow();
		this.inputComponentsList.addComponentCurrentRow(new Static(this, this.inputComponentsList.nextStart(12), 80, 60, 15, HorizontalSizing.LEFT_WIDTH, null, "datapack_edit.item_capability.lifetime"));
		this.inputComponentsList.addComponentCurrentRow(this.lifetime.relocateX(screenRect, this.inputComponentsList.nextStart(5)));
		
		this.inputComponentsList.setComponentsActive(false);
		
		if (animation.getPropertiesJson().has("trail_effects")) {
			JsonArray array = animation.getPropertiesJson().get("trail_effects").getAsJsonArray();
			
			this.trailList = array;
			
			for (int i = 0; i < array.size(); i++) {
				this.trailGrid.addRowWithDefaultValues("trail", String.format("Trail%d", i));
			}
		}
	}
	
	@Override
	protected void init() {
		ScreenRectangle screenRect = this.getRectangle();
		
		this.inputComponentsList.updateSizeAndPosition(screenRect.width() - 125, screenRect.bottom() - screenRect.top() - 80, screenRect.top() + 32);
		this.inputComponentsList.setX(125);
		
		this.trailGrid.resize(screenRect);
		
		this.addRenderableWidget(this.trailGrid);
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
			this.minecraft.setScreen(new MessageScreen<>("", "Do you want to quit without saving changes?", this,
				(button2) -> {
					this.onClose();
				}, (button2) -> {
					this.minecraft.setScreen(this);
				}, 180, 70));
		}).pos(this.width / 2 + 2, this.height - 28).size(160, 21).build());
	}
	
	@Override
	public void onClose() {
		this.minecraft.setScreen(this.parentScreen);
	}
	
	public void save() throws IllegalStateException {
		int i = 0;
		
		JsonArray jsonArr = new JsonArray();
		TrailInfo[] trailArr = new TrailInfo[this.trailList.size()];
		
		for (JsonElement element : this.trailList) {
			JsonObject trailObj = element.getAsJsonObject();
			
			if (!trailObj.has("start_time")) {
				throw new IllegalStateException(String.format("Row %d: Start time undefined!", i+1));
			}
			
			if (!trailObj.has("end_time")) {
				throw new IllegalStateException(String.format("Row %d: End time undefined!", i+1));
			}
			
			if (!trailObj.has("joint")) {
				throw new IllegalStateException(String.format("Row %d: Joint undefined!", i+1));
			}
			
			if (this.modelPlayer.getArmature().get().searchJointByName(trailObj.get("joint").getAsString()) == null) {
				throw new IllegalStateException(String.format("Row %d: No joint named %s in %s!", i+1, trailObj.get("joint").getAsString(), this.modelPlayer.getArmature()));
			}
			
			if (!trailObj.has("item_skin_hand")) {
				throw new IllegalStateException(String.format("Row %d: Hand undefined!", i+1));
			}
			
			TrailInfo.Builder builder = TrailInfo.builder()
													.time(trailObj.get("start_time").getAsFloat(), trailObj.get("end_time").getAsFloat())
													.joint(trailObj.get("joint").getAsString())
													.itemSkinHand(InteractionHand.valueOf(trailObj.get("item_skin_hand").getAsString().toUpperCase(Locale.ROOT)));
			
			if (trailObj.has("lifetime")) {
				builder.lifetime(trailObj.get("lifetime").getAsInt());
			}
			
			if (trailObj.has("interpolations")) {
				builder.interpolations(trailObj.get("interpolations").getAsInt());
			}
			
			trailArr[i] = TrailInfo.PREVIEWER_DEFAULT_TRAIL.overwrite(builder.create());
			
			jsonArr.add(element);
			i++;
		}
		
		this.animation.getPropertiesJson().asMap().clear();
		this.animation.getPropertiesJson().add("trail_effects", jsonArr);
		
		this.modelPlayer.setTrailInfo(trailArr);
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
		
		ResourceLocation backgroundTexture = this.minecraft.level == null ? MENU_BACKGROUND : INWORLD_MENU_BACKGROUND;
		
		guiGraphics.setColor(0.125F, 0.125F, 0.125F, 1.0F);
        guiGraphics.blit(backgroundTexture, 0, yBegin, (float)this.width, (float)yEnd - yBegin, this.width, yEnd, 32, 32);
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
		
		guiGraphics.setColor(0.25F, 0.25F, 0.25F, 1.0F);
		guiGraphics.blit(backgroundTexture, 0, 0, 0.0F, 0.0F, this.width, yBegin, 32, 32);
        guiGraphics.blit(backgroundTexture, 0, yEnd, 0.0F, (float)yEnd - yBegin, this.width, yEnd, 32, 32);
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        
        guiGraphics.fillGradient(RenderType.guiOverlay(), 0, yBegin, this.width, yBegin + 4, -16777216, 0, 0);
		guiGraphics.fillGradient(RenderType.guiOverlay(), 0, yEnd, this.width, yEnd + 1, 0, -16777216, 0);
		
		super.render(guiGraphics, mouseX, mouseY, partialTicks);
	}
	
	public enum LayerOptions {
		BASE_LAYER, COMPOSITE_LAYER, MULTILAYER
	}
}