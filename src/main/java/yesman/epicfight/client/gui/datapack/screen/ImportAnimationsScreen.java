package yesman.epicfight.client.gui.datapack.screen;
import net.minecraft.client.Minecraft;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.netty.util.internal.StringUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import yesman.epicfight.api.animation.Joint;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.animation.types.datapack.DatapackAnimation;
import yesman.epicfight.api.animation.types.datapack.EditorAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.asset.JsonAssetLoader;
import yesman.epicfight.api.client.animation.property.TrailInfo;
import yesman.epicfight.api.client.model.SkinnedMesh;
import yesman.epicfight.api.collider.Collider;
import yesman.epicfight.api.collider.MultiOBBCollider;
import yesman.epicfight.api.collider.OBBCollider;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.api.utils.ParseUtil;
import yesman.epicfight.client.gui.datapack.widgets.*;
import yesman.epicfight.client.gui.datapack.widgets.Grid.GridBuilder.RowEditButton;
import yesman.epicfight.client.gui.datapack.widgets.ResizableComponent.HorizontalSizing;
import yesman.epicfight.gameasset.ColliderPreset;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

public class ImportAnimationsScreen extends Screen {
	private final SelectAnimationScreen parentScreen;
	private final Grid animationGrid;
	private final ModelPreviewer modelPreviewer;
	private final List<EditorAnimation> fakeAnimations = Lists.newArrayList();
	private InputComponentList<EditorAnimation> inputComponentsList;
	private ComboBox<EditorAnimation.AnimationType> animationType;
	private Consumer<EditorAnimation.AnimationType> responder;
	private Map<ResourceLocation, PackEntry<EditorAnimation, DatapackAnimation<? extends StaticAnimation>>> userAnimations;
	
	public ImportAnimationsScreen(SelectAnimationScreen parentScreen, AssetAccessor<? extends Armature> armature, AssetAccessor<? extends SkinnedMesh> mesh) {
		super(Component.literal("register_animation_screen"));
		
		this.parentScreen = parentScreen;
		this.minecraft = Minecraft.getInstance();
		this.font = Minecraft.getInstance().font;
		
		this.userAnimations = DatapackEditScreen.getCurrentScreen().getUserAniamtions();
		this.fakeAnimations.addAll(this.userAnimations.values().stream().map(PackEntry::getKey).map(EditorAnimation::deepCopy).toList());
		this.modelPreviewer = new ModelPreviewer(10, 15, 0, 140, HorizontalSizing.LEFT_RIGHT, null, armature, mesh);
		this.modelPreviewer.setCollider(ColliderPreset.FIST);
		
		ScreenRectangle screenRect = parentScreen.getRectangle();
		int split = screenRect.width() / 2 - 60;
		
		this.animationGrid = Grid.builder(this, Minecraft.getInstance())
									.xy1(8, screenRect.top() + 14)
									.xy2(split - 10, screenRect.height() - 21)
									.rowHeight(26)
									.rowEditable(RowEditButton.NONE)
									.rowpositionChanged((rowposition, values) -> {
										this.inputComponentsList.importTag(this.fakeAnimations.get(rowposition));
										this.modelPreviewer.setTrailInfo();
										
										EditorAnimation.AnimationType animationType = this.fakeAnimations.get(rowposition).getAnimationClass();
										
										if (animationType == EditorAnimation.AnimationType.ATTACK || animationType == EditorAnimation.AnimationType.BASIC_ATTACK || animationType == EditorAnimation.AnimationType.DASH_ATTACK || animationType == EditorAnimation.AnimationType.AIR_SLASH) {
											this.inputComponentsList.getComponent(7, 1)._setActive(false);
											this.inputComponentsList.getComponent(8, 1)._setActive(false);
											this.inputComponentsList.getComponent(9, 1)._setActive(false);
											this.inputComponentsList.getComponent(10, 1)._setActive(false);
											this.inputComponentsList.getComponent(11, 1)._setActive(false);
											this.inputComponentsList.getComponent(12, 1)._setActive(false);
											this.inputComponentsList.getComponent(13, 1)._setActive(false);
											this.inputComponentsList.getComponent(14, 2)._setActive(false);
											this.inputComponentsList.getComponent(14, 4)._setActive(false);
											this.inputComponentsList.getComponent(14, 6)._setActive(false);
											this.inputComponentsList.getComponent(15, 2)._setActive(false);
											this.inputComponentsList.getComponent(15, 4)._setActive(false);
											this.inputComponentsList.getComponent(15, 6)._setActive(false);
											this.inputComponentsList.getComponent(16, 1)._setActive(false);
											
											if (this.fakeAnimations.get(rowposition).getPropertiesJson().has("trail_effects")) {
												JsonArray trailArr = this.fakeAnimations.get(rowposition).getPropertiesJson().getAsJsonArray("trail_effects");
												TrailInfo[] trailInfos = new TrailInfo[trailArr.size()];
												
												int i = 0;
												
												for (JsonElement element : trailArr) {
													JsonObject trailObj = element.getAsJsonObject();
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
													
													trailInfos[i] = TrailInfo.PREVIEWER_DEFAULT_TRAIL.overwrite(builder.create());
													i++;
												}
												
												this.modelPreviewer.setTrailInfo(trailInfos);
											}
										}
									})
									.addColumn(Grid.editbox("animation_name")
													.editWidgetCreated((editbox) -> editbox.setFilter(rl -> ResourceLocation.isValidNamespace(rl) && ResourceLocation.isValidPath(rl)))
													.editable(true)
													.valueChanged((event) -> this.fakeAnimations.get(event.rowposition).setParameter("path", event.postValue))
													.width(180))
									.build();
		
		this.inputComponentsList = new InputComponentList<>(this, 0, 0, 0, 30) {
			@Override
			public void importTag(EditorAnimation fakeAnim) {
				ImportAnimationsScreen.this.rearrangeComponents(fakeAnim.getAnimationClass());
				this.setComponentsActive(true);
				
				if (fakeAnim.getAnimationClass() != null) {
				switch (fakeAnim.getAnimationClass()) {
				case STATIC, MOVEMENT, EMOTE:
					ImportAnimationsScreen.this.animationType._setResponder(null);
					
					this.setDataBindingComponenets(new Object[] {
						fakeAnim.getAnimationClass(),
						ParseUtil.nullParam(fakeAnim.getParameter("convertTime")),
						fakeAnim.getParameter("isRepeat")
					});
					
					ImportAnimationsScreen.this.animationType._setResponder(ImportAnimationsScreen.this.responder);
					break;
				case LONG_HIT, SHORT_HIT:
					ImportAnimationsScreen.this.animationType._setResponder(null);
					
					this.setDataBindingComponenets(new Object[] {
						fakeAnim.getAnimationClass(),
						ParseUtil.nullParam(fakeAnim.getParameter("convertTime")),
					});
					
					ImportAnimationsScreen.this.animationType._setResponder(ImportAnimationsScreen.this.responder);
					break;
				case ATTACK, BASIC_ATTACK, DASH_ATTACK, AIR_SLASH:
					CompoundTag colliderTag = new CompoundTag();
					Collider collider = (Collider)fakeAnim.getParameter("collider");
					
					if (collider != null) {
						collider.serialize(colliderTag);
					}
					
					ImportAnimationsScreen.this.animationType._setResponder(null);
					
					Grid.PackImporter packImporter = new Grid.PackImporter();
					ListTag phasesTag = fakeAnim.getParameter("phases");
					
					for (int i = 0; i < phasesTag.size(); i++) {
						packImporter.newRow();
						packImporter.newValue("phase", String.format("Phase%s", i));
					}
					
					this.setDataBindingComponenets(new Object[] {
						fakeAnim.getAnimationClass(),
						ParseUtil.nullParam(fakeAnim.getParameter("convertTime")),
						packImporter,
						ParseUtil.nullParam(fakeAnim.getParameter("antic")),
						ParseUtil.nullParam(fakeAnim.getParameter("preDelay")),
						ParseUtil.nullParam(fakeAnim.getParameter("contact")),
						ParseUtil.nullParam(fakeAnim.getParameter("recovery")),
						fakeAnim.getParameter("hand"),
						null,
						ParseUtil.nullParam(colliderTag.get("number")),
						colliderTag.contains("center") ? ParseUtil.nullParam(ParseUtil.nullOrToString(colliderTag.getList("center", Tag.TAG_DOUBLE).get(0), Tag::getAsString)) : "",
						colliderTag.contains("center") ? ParseUtil.nullParam(ParseUtil.nullOrToString(colliderTag.getList("center", Tag.TAG_DOUBLE).get(1), Tag::getAsString)) : "",
						colliderTag.contains("center") ? ParseUtil.nullParam(ParseUtil.nullOrToString(colliderTag.getList("center", Tag.TAG_DOUBLE).get(2), Tag::getAsString)) : "",
						colliderTag.contains("size") ? ParseUtil.nullParam(ParseUtil.nullOrToString(colliderTag.getList("size", Tag.TAG_DOUBLE).get(0), Tag::getAsString)) : "",
						colliderTag.contains("size") ? ParseUtil.nullParam(ParseUtil.nullOrToString(colliderTag.getList("size", Tag.TAG_DOUBLE).get(1), Tag::getAsString)) : "",
						colliderTag.contains("size") ? ParseUtil.nullParam(ParseUtil.nullOrToString(colliderTag.getList("size", Tag.TAG_DOUBLE).get(2), Tag::getAsString)) : "",
						fakeAnim.getParameter("colliderJoint")
					});
					
					ImportAnimationsScreen.this.animationType._setResponder(ImportAnimationsScreen.this.responder);
					
					if (fakeAnim.getPropertiesJson().has("trail_effects")) {
						JsonArray trailList = fakeAnim.getPropertiesJson().get("trail_effects").getAsJsonArray();
						TrailInfo[] trailArr = new TrailInfo[trailList.size()];
						
						int i = 0;
						
						for (JsonElement element : trailList) {
							JsonObject trailObj = element.getAsJsonObject();
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
							i++;
						}
						
						ImportAnimationsScreen.this.modelPreviewer.setTrailInfo(trailArr);
					}
					
					break;
				default:
					ImportAnimationsScreen.this.animationType._setValue(null);
					break;
				}
				} else {
					ImportAnimationsScreen.this.animationType._setResponder(null);
					ImportAnimationsScreen.this.animationType._setValue(null);
					ImportAnimationsScreen.this.animationType._setResponder(ImportAnimationsScreen.this.responder);
				}
			}
		};
		
		if (this.fakeAnimations.isEmpty()) {
			this.inputComponentsList.newRow();
			this.inputComponentsList.addComponentCurrentRow(new Static(this, this.inputComponentsList.nextStart(4), 5, 60, 15, HorizontalSizing.LEFT_RIGHT, null, "datapack_edit.import_animation.place_tooltip"));
		}
		
		this.responder = (clz) -> {
			if (clz != null) {
				EditorAnimation fakeAnim = this.fakeAnimations.get(this.animationGrid.getRowposition());
				fakeAnim.setAnimationClass(clz);
				this.rearrangeComponents(clz);
			}
		};
		
		this.animationType = new ComboBox<>(this, this.font, 0, 124, 100, 15, HorizontalSizing.LEFT_WIDTH, null, 8, Component.translatable("datapack_edit.import_animation.type"),
											List.of(EditorAnimation.AnimationType.values()), (type) -> type.toString(), this.responder);
		
		for (PackEntry<EditorAnimation, DatapackAnimation<? extends StaticAnimation>> entry : this.userAnimations.values()) {
			this.animationGrid.addRowWithDefaultValues("animation_name", entry.getKey().getParameter("path"));
		}
	}
	
	public void rearrangeComponents(EditorAnimation.AnimationType animationClass) {
		ScreenRectangle screenRect = this.getRectangle();
		
		this.modelPreviewer.setCollider(null);
		this.modelPreviewer.setColliderJoint(null);
		this.modelPreviewer.clearAnimations();
		this.modelPreviewer.addAnimationToPlay(this.fakeAnimations.get(this.animationGrid.getRowposition()));
		
		this.inputComponentsList.clearComponents();
		this.inputComponentsList.newRow();
		this.inputComponentsList.addComponentCurrentRow(new Static(this, this.inputComponentsList.nextStart(4), 85, 60, 15, HorizontalSizing.LEFT_WIDTH, null, "datapack_edit.import_animation.type"));
		this.inputComponentsList.addComponentCurrentRow(this.animationType.relocateX(screenRect, this.inputComponentsList.nextStart(5)));
		
		if (animationClass != null) {
		switch (animationClass) {
		case STATIC, MOVEMENT, EMOTE:
		{
			final ResizableEditBox convertTime = new ResizableEditBox(this.font, 0, 35, 0, 15, Component.translatable("datapack_edit.import_animation.convert_time"), HorizontalSizing.LEFT_WIDTH, null);
			Boolean isRepeat = this.fakeAnimations.get(this.animationGrid.getRowposition()).getParameter("isRepeat");
			
			if (isRepeat == null) {
				isRepeat = Boolean.valueOf(false);
			}
			
			final CheckBox repeat = new CheckBox(this.font, 0, 60, 0, 10, HorizontalSizing.LEFT_WIDTH, null, isRepeat, Component.literal(""), (value) -> this.fakeAnimations.get(this.animationGrid.getRowposition()).setParameter("isRepeat", value));
			
			convertTime.setResponder((input) -> {
				Object f = StringUtil.isNullOrEmpty(input) ? null : Float.valueOf(input);
				this.fakeAnimations.get(this.animationGrid.getRowposition()).setParameter("convertTime", f);
			});
			
			convertTime.setFilter((context) -> StringUtil.isNullOrEmpty(context) || ParseUtil.isParsable(context, Float::parseFloat));
			
			this.inputComponentsList.newRow();
			this.inputComponentsList.addComponentCurrentRow(new Static(this, this.inputComponentsList.nextStart(4), 85, 60, 15, HorizontalSizing.LEFT_WIDTH, null, "datapack_edit.import_animation.convert_time"));
			this.inputComponentsList.addComponentCurrentRow(convertTime.relocateX(screenRect, this.inputComponentsList.nextStart(5)));
			
			this.inputComponentsList.newRow();
			this.inputComponentsList.addComponentCurrentRow(new Static(this, this.inputComponentsList.nextStart(4), 85, 60, 15, HorizontalSizing.LEFT_WIDTH, null, "datapack_edit.import_animation.repeat"));
			this.inputComponentsList.addComponentCurrentRow(repeat.relocateX(screenRect, this.inputComponentsList.nextStart(5)));
			
			this.inputComponentsList.newRow();
			this.inputComponentsList.addComponentCurrentRow(new Static(this, this.inputComponentsList.nextStart(4), 85, 60, 15, HorizontalSizing.LEFT_WIDTH, null, "datapack_edit.import_animation.client_data"));
			this.inputComponentsList.addComponentCurrentRow(SubScreenOpenButton.builder().subScreen(() -> {
				return new StaticAnimationPropertyScreen(this, this.fakeAnimations.get(this.animationGrid.getRowposition()));
			}).bounds(this.inputComponentsList.nextStart(4), 0, 15, 15).build());
		}
		break;
		case SHORT_HIT, LONG_HIT, KNOCK_DOWN:
		{
			final ResizableEditBox convertTime = new ResizableEditBox(this.font, 0, 35, 0, 15, Component.translatable("datapack_edit.import_animation.convert_time"), HorizontalSizing.LEFT_WIDTH, null);
			
			convertTime.setResponder((input) -> {
				Object f = StringUtil.isNullOrEmpty(input) ? null : Float.valueOf(input);
				this.fakeAnimations.get(this.animationGrid.getRowposition()).setParameter("convertTime", f);
			});
			
			convertTime.setFilter((context) -> StringUtil.isNullOrEmpty(context) || ParseUtil.isParsable(context, Float::parseFloat));
			
			this.inputComponentsList.newRow();
			this.inputComponentsList.addComponentCurrentRow(new Static(this, this.inputComponentsList.nextStart(4), 85, 60, 15, HorizontalSizing.LEFT_WIDTH, null, "datapack_edit.import_animation.convert_time"));
			this.inputComponentsList.addComponentCurrentRow(convertTime.relocateX(screenRect, this.inputComponentsList.nextStart(5)));
		}
		break;
		case ATTACK, BASIC_ATTACK, DASH_ATTACK, AIR_SLASH:
		{
			this.modelPreviewer.setCollider(ColliderPreset.FIST);
			
			final ResizableEditBox convertTime = new ResizableEditBox(this.font, 0, 35, 0, 15, Component.translatable("datapack_edit.import_animation.convert_time"), HorizontalSizing.LEFT_WIDTH, null);
			
			convertTime.setResponder((input) -> {
				Object f = StringUtil.isNullOrEmpty(input) ? null : Float.valueOf(input);
				this.fakeAnimations.get(this.animationGrid.getRowposition()).setParameter("convertTime", f);
			});
			convertTime.setFilter((context) -> StringUtil.isNullOrEmpty(context) || ParseUtil.isParsable(context, Float::parseFloat));
			
			final ResizableEditBox antic = new ResizableEditBox(this.font, 0, 35, 0, 15, Component.translatable("datapack_edit.import_animation.antic"), HorizontalSizing.LEFT_WIDTH, null);
			final ResizableEditBox preDelay = new ResizableEditBox(this.font, 0, 35, 0, 15, Component.translatable("datapack_edit.import_animation.preDelay"), HorizontalSizing.LEFT_WIDTH, null);
			final ResizableEditBox contact = new ResizableEditBox(this.font, 0, 35, 0, 15, Component.translatable("datapack_edit.import_animation.contact"), HorizontalSizing.LEFT_WIDTH, null);
			final ResizableEditBox recovery = new ResizableEditBox(this.font, 0, 35, 0, 15, Component.translatable("datapack_edit.import_animation.recovery"), HorizontalSizing.LEFT_WIDTH, null);
			final ResizableEditBox colliderCount = new ResizableEditBox(this.font, 0, 40, 0, 15, Component.translatable("datapack_edit.collider.count"), HorizontalSizing.LEFT_WIDTH, null);
			final ResizableEditBox colliderCenterX = new ResizableEditBox(this.font, 0, 35, 0, 15, Component.translatable("datapack_edit.collider.center.x"), HorizontalSizing.LEFT_WIDTH, null);
			final ResizableEditBox colliderCenterY = new ResizableEditBox(this.font, 0, 35, 0, 15, Component.translatable("datapack_edit.collider.center.y"), HorizontalSizing.LEFT_WIDTH, null);
			final ResizableEditBox colliderCenterZ = new ResizableEditBox(this.font, 0, 35, 0, 15, Component.translatable("datapack_edit.collider.center.z"), HorizontalSizing.LEFT_WIDTH, null);
			final ResizableEditBox colliderSizeX = new ResizableEditBox(this.font, 0, 35, 0, 15, Component.translatable("datapack_edit.collider.size.x"), HorizontalSizing.LEFT_WIDTH, null);
			final ResizableEditBox colliderSizeY = new ResizableEditBox(this.font, 0, 35, 0, 15, Component.translatable("datapack_edit.collider.size.y"), HorizontalSizing.LEFT_WIDTH, null);
			final ResizableEditBox colliderSizeZ = new ResizableEditBox(this.font, 0, 35, 0, 15, Component.translatable("datapack_edit.collider.size.z"), HorizontalSizing.LEFT_WIDTH, null);
			
			final ComboBox<Joint> colliderJoint = new ComboBox<>(this, this.font, 0, 124, 100, 15, HorizontalSizing.LEFT_WIDTH, null, 8, Component.translatable("datapack_edit.import_animation.joint"),
																	this.modelPreviewer.getArmature().get().rootJoint.getAllJoints(), Joint::getName, null);
			final ComboBox<InteractionHand> interactionHand = new ComboBox<>(this, this.font, 0, 124, 100, 15, HorizontalSizing.LEFT_WIDTH, null, 8, Component.translatable("datapack_edit.import_animation.hand"),
																				List.of(InteractionHand.MAIN_HAND, InteractionHand.OFF_HAND), ParseUtil::snakeToSpacedCamel, null);
			final PopupBox.ColliderPopupBox colliderPopup = new PopupBox.ColliderPopupBox(this, font, 0, 30, 130, 15, HorizontalSizing.LEFT_RIGHT, null, Component.translatable("datapack_edit.collider"), null);
			
			antic._setActive(false);
			preDelay._setActive(false);
			contact._setActive(false);
			recovery._setActive(false);
			colliderCount._setActive(false);
			colliderCenterX._setActive(false);
			colliderCenterY._setActive(false);
			colliderCenterZ._setActive(false);
			colliderSizeX._setActive(false);
			colliderSizeY._setActive(false);
			colliderSizeZ._setActive(false);
			colliderJoint._setActive(false);
			interactionHand._setActive(false);
			colliderPopup._setActive(false);
			
			final Grid phasesGrid = Grid.builder(this, this.minecraft)
										.xy1(8, 0)
										.xy2(12, 80)
										.horizontalSizing(HorizontalSizing.LEFT_RIGHT)
										.rowHeight(26)
										.rowEditable(RowEditButton.ADD_REMOVE)
										.rowpositionChanged((rowposition, values) -> {
											EditorAnimation fakeAnimation = this.fakeAnimations.get(this.animationGrid.getRowposition());
											ListTag phases = fakeAnimation.getParameter("phases");
											CompoundTag tag = phases.getCompound(rowposition);
											
											antic.setValue(tag.contains("antic") ? ParseUtil.valueOfOmittingType(tag.get("antic").getAsString()) : "");
											preDelay.setValue(tag.contains("preDelay") ? ParseUtil.valueOfOmittingType(tag.get("preDelay").getAsString()) : "");
											contact.setValue(tag.contains("contact") ? ParseUtil.valueOfOmittingType(tag.get("contact").getAsString()) : "");
											recovery.setValue(tag.contains("recovery") ? ParseUtil.valueOfOmittingType(tag.get("recovery").getAsString()) : "");
											
											if (tag.contains("joint")) {
												String armature$joint = tag.getString("joint");
												String joinName = armature$joint.substring(armature$joint.lastIndexOf('.') + 1);
												colliderJoint._setValue(this.modelPreviewer.getArmature().get().searchJointByName(joinName));
											} else {
												colliderJoint._setValue(null);
											}
											
											interactionHand._setValue(tag.contains("hand") ? InteractionHand.valueOf(tag.getString("hand")) : null);
											
											if (tag.contains("collider")) {
												CompoundTag colliderTag = tag.getCompound("collider");
												colliderCount.setValue(ParseUtil.valueOfOmittingType(colliderTag.get("number")));
												colliderCenterX.setValue(ParseUtil.valueOfOmittingType(colliderTag.getList("center", Tag.TAG_DOUBLE).get(0)));
												colliderCenterY.setValue(ParseUtil.valueOfOmittingType(colliderTag.getList("center", Tag.TAG_DOUBLE).get(1)));
												colliderCenterZ.setValue(ParseUtil.valueOfOmittingType(colliderTag.getList("center", Tag.TAG_DOUBLE).get(2)));
												colliderSizeX.setValue(ParseUtil.valueOfOmittingType(colliderTag.getList("size", Tag.TAG_DOUBLE).get(0)));
												colliderSizeY.setValue(ParseUtil.valueOfOmittingType(colliderTag.getList("size", Tag.TAG_DOUBLE).get(1)));
												colliderSizeZ.setValue(ParseUtil.valueOfOmittingType(colliderTag.getList("size", Tag.TAG_DOUBLE).get(2)));
											} else {
												colliderPopup._setValue(null);
												colliderCount.setValue("");
												colliderCenterX.setValue("");
												colliderCenterY.setValue("");
												colliderCenterZ.setValue("");
												colliderSizeX.setValue("");
												colliderSizeY.setValue("");
												colliderSizeZ.setValue("");
											}
											
											if (rowposition > -1) {
												antic._setActive(true);
												preDelay._setActive(true);
												contact._setActive(true);
												recovery._setActive(true);
												colliderCount._setActive(true);
												colliderCenterX._setActive(true);
												colliderCenterY._setActive(true);
												colliderCenterZ._setActive(true);
												colliderSizeX._setActive(true);
												colliderSizeY._setActive(true);
												colliderSizeZ._setActive(true);
												colliderJoint._setActive(true);
												interactionHand._setActive(true);
												colliderPopup._setActive(true);
											}
										})
										.addColumn(Grid.editbox("phase")
														.editable(false)
														.width(200))
										.pressAdd((grid, button) -> {
											EditorAnimation fakeAnimation = this.fakeAnimations.get(this.animationGrid.getRowposition());
											ListTag phases = fakeAnimation.getParameter("phases");
											
											if (phases.isEmpty()) {
												antic._setActive(true);
												preDelay._setActive(true);
												contact._setActive(true);
												recovery._setActive(true);
												colliderCount._setActive(true);
												colliderCenterX._setActive(true);
												colliderCenterY._setActive(true);
												colliderCenterZ._setActive(true);
												colliderSizeX._setActive(true);
												colliderSizeY._setActive(true);
												colliderSizeZ._setActive(true);
												colliderJoint._setActive(true);
												interactionHand._setActive(true);
												colliderPopup._setActive(true);
											}
											
											phases.add(new CompoundTag());
											
											int rowposition = grid.addRowWithDefaultValues("phase", String.format("Phase%d", grid.children().size()));
											grid.setGridFocus(rowposition, "phase");
										})
										.pressRemove((grid, button) -> {
											grid.removeRow((removedRow) -> {
												ListTag phases = this.fakeAnimations.get(this.animationGrid.getRowposition()).getParameter("phases");
												
												//Rename rows
												for (int i = 0; i < grid.children().size(); i++) {
													grid.setValue(i, "phase", "Phase"+i);
												}
												
												phases.remove(removedRow);
												
												if (phases.isEmpty()) {
													antic._setActive(false);
													preDelay._setActive(false);
													contact._setActive(false);
													recovery._setActive(false);
													colliderCount._setActive(false);
													colliderCenterX._setActive(false);
													colliderCenterY._setActive(false);
													colliderCenterZ._setActive(false);
													colliderSizeX._setActive(false);
													colliderSizeY._setActive(false);
													colliderSizeZ._setActive(false);
													colliderJoint._setActive(false);
													interactionHand._setActive(false);
													colliderPopup._setActive(false);
												}
											});
										})
										.build();
			
			antic.setResponder((input) -> {
				if (!StringUtil.isNullOrEmpty(input)) {
					ListTag phases = this.fakeAnimations.get(this.animationGrid.getRowposition()).getParameter("phases");
					CompoundTag phaseTag = phases.getCompound(phasesGrid.getRowposition());
					phaseTag.putFloat("antic", Float.valueOf(input));
				}
			});
			
			preDelay.setResponder((input) -> {
				if (!StringUtil.isNullOrEmpty(input)) {
					ListTag phases = this.fakeAnimations.get(this.animationGrid.getRowposition()).getParameter("phases");
					CompoundTag phaseTag = phases.getCompound(phasesGrid.getRowposition());
					float f = Float.valueOf(input);
					
					phaseTag.putFloat("preDelay", f);
					this.modelPreviewer.setAttackTimeBegin(f);
				}
			});
			
			contact.setResponder((input) -> {
				if (!StringUtil.isNullOrEmpty(input)) {
					ListTag phases = this.fakeAnimations.get(this.animationGrid.getRowposition()).getParameter("phases");
					CompoundTag phaseTag = phases.getCompound(phasesGrid.getRowposition());
					float f = Float.valueOf(input);
					
					phaseTag.putFloat("contact", f);
					this.modelPreviewer.setAttackTimeEnd(f);
				}
			});
			
			recovery.setResponder((input) -> {
				if (!StringUtil.isNullOrEmpty(input)) {
					ListTag phases = this.fakeAnimations.get(this.animationGrid.getRowposition()).getParameter("phases");
					CompoundTag phaseTag = phases.getCompound(phasesGrid.getRowposition());
					phaseTag.putFloat("recovery", Float.valueOf(input));
				}
			});
			
			antic.setFilter((context) -> StringUtil.isNullOrEmpty(context) || ParseUtil.isParsable(context, Float::parseFloat));
			preDelay.setFilter((context) -> StringUtil.isNullOrEmpty(context) || ParseUtil.isParsable(context, Float::parseFloat));
			contact.setFilter((context) -> StringUtil.isNullOrEmpty(context) || ParseUtil.isParsable(context, Float::parseFloat));
			recovery.setFilter((context) -> StringUtil.isNullOrEmpty(context) || ParseUtil.isParsable(context, Float::parseFloat));
			
			final Runnable setCollider = () -> {
				CompoundTag tag = new CompoundTag();
				
				if (ParseUtil.isParsable(colliderCount.getValue(), Integer::parseInt)) {
					tag.putInt("number", Integer.parseInt(colliderCount.getValue()));
				}
				
				ListTag center = new ListTag();
				
				if (ParseUtil.isParsable(colliderCenterX.getValue(), Double::parseDouble)) {
					center.add(DoubleTag.valueOf(Double.parseDouble(colliderCenterX.getValue())));
				}
				if (ParseUtil.isParsable(colliderCenterY.getValue(), Double::parseDouble)) {
					center.add(DoubleTag.valueOf(Double.parseDouble(colliderCenterY.getValue())));
				}
				if (ParseUtil.isParsable(colliderCenterZ.getValue(), Double::parseDouble)) {
					center.add(DoubleTag.valueOf(Double.parseDouble(colliderCenterZ.getValue())));
				}
				
				tag.put("center", center);
				
				ListTag size = new ListTag();
				
				if (ParseUtil.isParsable(colliderSizeX.getValue(), Double::parseDouble)) {
					size.add(DoubleTag.valueOf(Double.parseDouble(colliderSizeX.getValue())));
				}
				if (ParseUtil.isParsable(colliderSizeY.getValue(), Double::parseDouble)) {
					size.add(DoubleTag.valueOf(Double.parseDouble(colliderSizeY.getValue())));
				}
				if (ParseUtil.isParsable(colliderSizeZ.getValue(), Double::parseDouble)) {
					size.add(DoubleTag.valueOf(Double.parseDouble(colliderSizeZ.getValue())));
				}
				
				tag.put("size", size);
				EditorAnimation fakeAnimation = this.fakeAnimations.get(this.animationGrid.getRowposition());
				ListTag phases = fakeAnimation.getParameter("phases");
				CompoundTag phaseTag = phases.getCompound(phasesGrid.getRowposition());
				
				try {
					Collider collider = ColliderPreset.deserializeSimpleCollider(tag);
					phaseTag.put("collider", tag);
					this.modelPreviewer.setCollider(collider);
				} catch (Exception e) {
					phaseTag.remove("collider");
					this.modelPreviewer.setCollider(ColliderPreset.FIST);
				}
			};
			
			colliderCount.setResponder((input) -> {
				setCollider.run();
			});
			
			colliderCenterX.setResponder((input) -> {
				setCollider.run();
			});
			
			colliderCenterY.setResponder((input) -> {
				setCollider.run();
			});
			
			colliderCenterZ.setResponder((input) -> {
				setCollider.run();
			});
			
			colliderSizeX.setResponder((input) -> {
				setCollider.run();
			});
			
			colliderSizeY.setResponder((input) -> {
				setCollider.run();
			});
			
			colliderSizeZ.setResponder((input) -> {
				setCollider.run();
			});
			
			colliderCount.setFilter((context) -> StringUtil.isNullOrEmpty(context) || ParseUtil.isParsable(context, Integer::parseInt));
			colliderCenterX.setFilter((context) -> StringUtil.isNullOrEmpty(context) || ParseUtil.isParsableAllowingMinus(context, Double::parseDouble));
			colliderCenterY.setFilter((context) -> StringUtil.isNullOrEmpty(context) || ParseUtil.isParsableAllowingMinus(context, Double::parseDouble));
			colliderCenterZ.setFilter((context) -> StringUtil.isNullOrEmpty(context) || ParseUtil.isParsableAllowingMinus(context, Double::parseDouble));
			colliderSizeX.setFilter((context) -> StringUtil.isNullOrEmpty(context) || ParseUtil.isParsable(context, Double::parseDouble));
			colliderSizeY.setFilter((context) -> StringUtil.isNullOrEmpty(context) || ParseUtil.isParsable(context, Double::parseDouble));
			colliderSizeZ.setFilter((context) -> StringUtil.isNullOrEmpty(context) || ParseUtil.isParsable(context, Double::parseDouble));
			
			colliderJoint._setResponder((joint) -> {
				if (joint != null) {
					ListTag phases = this.fakeAnimations.get(this.animationGrid.getRowposition()).getParameter("phases");
					CompoundTag phaseTag = phases.getCompound(phasesGrid.getRowposition());
					phaseTag.putString("joint", this.modelPreviewer.getArmature().toString() + "." + joint.getName());
					
					this.modelPreviewer.setColliderJoint(joint);
				}
			});
			
			interactionHand._setResponder((hand) -> {
				if (hand != null) {
					ListTag phases = this.fakeAnimations.get(this.animationGrid.getRowposition()).getParameter("phases");
					CompoundTag phaseTag = phases.getCompound(phasesGrid.getRowposition());
					phaseTag.putString("hand", hand.toString());
				}
			});
			
			colliderPopup._setResponder((pair) -> {
				if (pair.getSecond() != null) {
					ListTag phases = this.fakeAnimations.get(this.animationGrid.getRowposition()).getParameter("phases");
					CompoundTag phaseTag = phases.getCompound(phasesGrid.getRowposition());
					CompoundTag colliderTag = new CompoundTag();
					pair.getSecond().serialize(colliderTag);
					
					colliderCount.setValue(String.valueOf(colliderTag.getInt("number")));
					
					ListTag centerVec = colliderTag.getList("center", Tag.TAG_DOUBLE);
					colliderCenterX.setValue(String.valueOf(centerVec.getDouble(0)));
					colliderCenterY.setValue(String.valueOf(centerVec.getDouble(1)));
					colliderCenterZ.setValue(String.valueOf(centerVec.getDouble(2)));
					
					ListTag sizeVec = colliderTag.getList("size", Tag.TAG_DOUBLE);
					colliderSizeX.setValue(String.valueOf(sizeVec.getDouble(0)));
					colliderSizeY.setValue(String.valueOf(sizeVec.getDouble(1)));
					colliderSizeZ.setValue(String.valueOf(sizeVec.getDouble(2)));
					
					phaseTag.put("collider", colliderTag);
					
					this.modelPreviewer.setCollider(pair.getSecond(), colliderJoint._getValue());
				} else {
					ListTag phases = this.fakeAnimations.get(this.animationGrid.getRowposition()).getParameter("phases");
					CompoundTag phaseTag = phases.getCompound(phasesGrid.getRowposition());
					
					if (phaseTag.contains("collider")) {
						colliderCount.setValue("");
						colliderCenterX.setValue("");
						colliderCenterY.setValue("");
						colliderCenterZ.setValue("");
						colliderSizeX.setValue("");
						colliderSizeY.setValue("");
						colliderSizeZ.setValue("");
					}
					
					this.modelPreviewer.setCollider(null);
				}
			});
			
			colliderPopup.applyFilter((collider) -> collider instanceof OBBCollider || collider instanceof MultiOBBCollider);
			
			this.inputComponentsList.newRow();
			this.inputComponentsList.addComponentCurrentRow(new Static(this, this.inputComponentsList.nextStart(4), 85, 60, 15, HorizontalSizing.LEFT_WIDTH, null, "datapack_edit.import_animation.convert_time"));
			this.inputComponentsList.addComponentCurrentRow(convertTime.relocateX(screenRect, this.inputComponentsList.nextStart(5)));
			this.inputComponentsList.newRow();
			this.inputComponentsList.addComponentCurrentRow(new RowSpliter(this.inputComponentsList.nextStart(5), 10, 60, 15, HorizontalSizing.LEFT_RIGHT, null));
			
			this.inputComponentsList.newRow();
			this.inputComponentsList.addComponentCurrentRow(new Static(this, this.inputComponentsList.nextStart(4), 85, 60, 15, HorizontalSizing.LEFT_WIDTH, null, "datapack_edit.import_animation.phases"));
			this.inputComponentsList.newRow();
			this.inputComponentsList.newRow();
			this.inputComponentsList.addComponentCurrentRow(phasesGrid);
			this.inputComponentsList.newRow();
			
			this.inputComponentsList.newRow();
			this.inputComponentsList.addComponentCurrentRow(new Static(this, this.inputComponentsList.nextStart(4), 85, 60, 15, HorizontalSizing.LEFT_WIDTH, null, "datapack_edit.import_animation.antic"));
			this.inputComponentsList.addComponentCurrentRow(antic.relocateX(screenRect, this.inputComponentsList.nextStart(5)));
			this.inputComponentsList.newRow();
			this.inputComponentsList.addComponentCurrentRow(new Static(this, this.inputComponentsList.nextStart(4), 85, 60, 15, HorizontalSizing.LEFT_WIDTH, null, "datapack_edit.import_animation.pre_delay"));
			this.inputComponentsList.addComponentCurrentRow(preDelay.relocateX(screenRect, this.inputComponentsList.nextStart(5)));
			this.inputComponentsList.newRow();
			this.inputComponentsList.addComponentCurrentRow(new Static(this, this.inputComponentsList.nextStart(4), 85, 60, 15, HorizontalSizing.LEFT_WIDTH, null, "datapack_edit.import_animation.contact"));
			this.inputComponentsList.addComponentCurrentRow(contact.relocateX(screenRect, this.inputComponentsList.nextStart(5)));
			this.inputComponentsList.newRow();
			this.inputComponentsList.addComponentCurrentRow(new Static(this, this.inputComponentsList.nextStart(4), 85, 60, 15, HorizontalSizing.LEFT_WIDTH, null, "datapack_edit.import_animation.recovery"));
			this.inputComponentsList.addComponentCurrentRow(recovery.relocateX(screenRect, this.inputComponentsList.nextStart(5)));
			this.inputComponentsList.newRow();
			this.inputComponentsList.addComponentCurrentRow(new Static(this, this.inputComponentsList.nextStart(4), 85, 60, 15, HorizontalSizing.LEFT_WIDTH, null, "datapack_edit.import_animation.hand"));
			this.inputComponentsList.addComponentCurrentRow(interactionHand.relocateX(screenRect, this.inputComponentsList.nextStart(5)));
			this.inputComponentsList.newRow();
			this.inputComponentsList.addComponentCurrentRow(new Static(this, this.inputComponentsList.nextStart(4), 85, 60, 15, HorizontalSizing.LEFT_WIDTH, null, "datapack_edit.import_animation.collider"));
			this.inputComponentsList.addComponentCurrentRow(colliderPopup.relocateX(screenRect, this.inputComponentsList.nextStart(5)));
			
			this.inputComponentsList.newRow();
			this.inputComponentsList.addComponentCurrentRow(new Static(this, this.inputComponentsList.nextStart(20), 40, 60, 15, HorizontalSizing.LEFT_WIDTH, null, "datapack_edit.collider.count"));
			this.inputComponentsList.addComponentCurrentRow(colliderCount.relocateX(screenRect, this.inputComponentsList.nextStart(5)));
			
			this.inputComponentsList.newRow();
			this.inputComponentsList.addComponentCurrentRow(new Static(this, this.inputComponentsList.nextStart(20), 40, 60, 15, HorizontalSizing.LEFT_WIDTH, null, Component.translatable("datapack_edit.collider.center")));
			this.inputComponentsList.addComponentCurrentRow(new Static(this, this.inputComponentsList.nextStart(5), 8, 60, 15, HorizontalSizing.LEFT_WIDTH, null, Component.literal("X: ")));
			this.inputComponentsList.addComponentCurrentRow(colliderCenterX.relocateX(screenRect, this.inputComponentsList.nextStart(5)));
			this.inputComponentsList.addComponentCurrentRow(new Static(this, this.inputComponentsList.nextStart(8), 8, 60, 15, HorizontalSizing.LEFT_WIDTH, null, Component.literal("Y: ")));
			this.inputComponentsList.addComponentCurrentRow(colliderCenterY.relocateX(screenRect, this.inputComponentsList.nextStart(5)));
			this.inputComponentsList.addComponentCurrentRow(new Static(this, this.inputComponentsList.nextStart(8), 8, 60, 15, HorizontalSizing.LEFT_WIDTH, null, Component.literal("Z: ")));
			this.inputComponentsList.addComponentCurrentRow(colliderCenterZ.relocateX(screenRect, this.inputComponentsList.nextStart(5)));
			
			this.inputComponentsList.newRow();
			this.inputComponentsList.addComponentCurrentRow(new Static(this, this.inputComponentsList.nextStart(20), 40, 60, 15, HorizontalSizing.LEFT_WIDTH, null, Component.translatable("datapack_edit.collider.size")));
			this.inputComponentsList.addComponentCurrentRow(new Static(this, this.inputComponentsList.nextStart(5), 8, 60, 15, HorizontalSizing.LEFT_WIDTH, null, Component.literal("X: ")));
			this.inputComponentsList.addComponentCurrentRow(colliderSizeX.relocateX(screenRect, this.inputComponentsList.nextStart(5)));
			this.inputComponentsList.addComponentCurrentRow(new Static(this, this.inputComponentsList.nextStart(8), 8, 60, 15, HorizontalSizing.LEFT_WIDTH, null, Component.literal("Y: ")));
			this.inputComponentsList.addComponentCurrentRow(colliderSizeY.relocateX(screenRect, this.inputComponentsList.nextStart(5)));
			this.inputComponentsList.addComponentCurrentRow(new Static(this, this.inputComponentsList.nextStart(8), 8, 60, 15, HorizontalSizing.LEFT_WIDTH, null, Component.literal("Z: ")));
			this.inputComponentsList.addComponentCurrentRow(colliderSizeZ.relocateX(screenRect, this.inputComponentsList.nextStart(5)));
			
			this.inputComponentsList.newRow();
			this.inputComponentsList.addComponentCurrentRow(new Static(this, this.inputComponentsList.nextStart(4), 85, 60, 15, HorizontalSizing.LEFT_WIDTH, null, "datapack_edit.import_animation.joint"));
			this.inputComponentsList.addComponentCurrentRow(colliderJoint.relocateX(screenRect, this.inputComponentsList.nextStart(5)));
			
			this.inputComponentsList.newRow();
			this.inputComponentsList.addComponentCurrentRow(new RowSpliter(this.inputComponentsList.nextStart(5), 10, 60, 15, HorizontalSizing.LEFT_RIGHT, null));
			
			this.inputComponentsList.newRow();
			this.inputComponentsList.addComponentCurrentRow(new Static(this, this.inputComponentsList.nextStart(4), 85, 60, 15, HorizontalSizing.LEFT_WIDTH, null, "datapack_edit.import_animation.client_data"));
			this.inputComponentsList.addComponentCurrentRow(SubScreenOpenButton.builder().subScreen(() -> {
				return new AttackAnimationPropertyScreen(this, this.fakeAnimations.get(this.animationGrid.getRowposition()), this.modelPreviewer.getArmature().get().rootJoint.getAllJoints(), this.modelPreviewer);
			}).bounds(this.inputComponentsList.nextStart(4), 0, 15, 15).build());
		}
		break;
		}
		}
		
		this.inputComponentsList.newRow();
		this.inputComponentsList.addComponentCurrentRow(new Static(this, this.inputComponentsList.nextStart(4), 60, 60, 15, HorizontalSizing.LEFT_WIDTH, null, Component.translatable("datapack_edit.import_animation.preview")));
		this.inputComponentsList.newRow();
		this.inputComponentsList.newRow();
		this.inputComponentsList.newRow();
		
		int split = screenRect.width() / 2 - 60;
		
		this.inputComponentsList.addComponentCurrentRow(this.modelPreviewer);
		this.inputComponentsList.newRow();
		this.inputComponentsList.newRow();
		
		this.inputComponentsList.updateSizeAndPosition(screenRect.width() - (split + 8), screenRect.bottom() - screenRect.top() - 50, screenRect.top() + 14);
		this.inputComponentsList.setX(split + 2);
	}
	
	@Override
	protected void init() {
		ScreenRectangle screenRect = this.getRectangle();
		int split = screenRect.width() / 2 - 60;
		
		this.animationGrid.updateSizeAndPosition(split - 10, screenRect.bottom() - screenRect.top() - 50, screenRect.top() + 14);
		this.animationGrid.setX(8);
		this.animationGrid.resize(screenRect);
		this.inputComponentsList.updateSizeAndPosition(screenRect.width() - (split + 8), screenRect.bottom() - screenRect.top() - 50, screenRect.top() + 14);
		this.inputComponentsList.setX(split + 2);
		
		this.addRenderableWidget(this.animationGrid);
		this.addRenderableWidget(this.inputComponentsList);
		
		this.addRenderableWidget(Button.builder(CommonComponents.GUI_OK, (button) -> {
			String errorMessage = this.createRealAnimations(this.fakeAnimations);
			
			if (errorMessage != null) {
				this.minecraft.setScreen(new MessageScreen<>("Failed to import the animations", errorMessage, this, (button2) -> this.minecraft.setScreen(this), 300, 70).autoCalculateHeight());
			} else {
				this.onClose();
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
	public void tick() {
		this.inputComponentsList.tick();
	}
	
	@Override
	public void onClose() {
		this.parentScreen.refreshAnimationList();
		this.minecraft.setScreen(this.parentScreen);
		this.modelPreviewer.onDestroy();
	}
	
	@Override
	public void onFilesDrop(List<Path> paths) {
		this.minecraft.setScreen(new MessageScreen<>("", "Enter the mod id", this,
			(modid) -> {
				this.animationGrid.setValueChangeEnabled(false);
				
				for (Path path : paths) {
					InputStream stream = null;
					
					try {
						File file = path.toFile();
						stream = new FileInputStream(file);
						JsonAssetLoader jsonLoader = new JsonAssetLoader(stream, ResourceLocation.fromNamespaceAndPath(modid, file.getName()));
						String armatureName = this.modelPreviewer.getArmature().get().toString();
						armatureName = armatureName.substring(armatureName.indexOf(":") + 1);
						
						String animationPath = modid + ":" + armatureName.substring(armatureName.lastIndexOf("/") + 1) + "/" + file.getName().replace(".json", "");
						EditorAnimation animation = new EditorAnimation(
                            animationPath,
                            this.modelPreviewer.getArmature(),
                            jsonLoader.loadAnimationClip(this.modelPreviewer.getArmature().get()),
                            JsonAssetLoader.getAsTransformFormatOrDefault(jsonLoader.getRootJson(), "format"),
                            jsonLoader.getRootJson().getAsJsonArray("animation")
                        );
						
						this.fakeAnimations.add(animation);
						this.animationGrid.addRowWithDefaultValues("animation_name", animationPath);
					} catch (Exception e) {
						e.printStackTrace();
					} finally {
						try {
							stream.close();
						} catch (IOException e) {
						}
					}
				}
				
				this.animationGrid.setValueChangeEnabled(true);
				this.minecraft.setScreen(this);
			},
			(button) -> this.minecraft.setScreen(this),
			new ResizableEditBox(this.minecraft.font, 0, 0, 0, 16, Component.literal("datapack_edit.import_animation.input"), null, null), 120, 80));
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
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		this.renderBackground(guiGraphics, mouseX, mouseY, partialTicks);
		super.render(guiGraphics, mouseX, mouseY, partialTicks);
	}
	
	private String createRealAnimations(List<EditorAnimation> fakeAnimations) {
		this.userAnimations.clear();
		
		StringBuilder sb = new StringBuilder();
		boolean hasException = false;
		List<Object> uniquepaths = fakeAnimations.stream().map((fakeAnim) -> fakeAnim.getParameter("path")).distinct().toList();
		
		if (uniquepaths.size() != fakeAnimations.size()) {
			hasException = true;
			sb.append("Duplicated animation path.");
		} else {
			for (EditorAnimation fakeAnimation : fakeAnimations) {
				try {
					DatapackAnimation<? extends StaticAnimation> result = fakeAnimation.createAnimation();
					this.userAnimations.put(fakeAnimation.getRegistryName(), PackEntry.ofValue(fakeAnimation, result));
				} catch (Throwable e) {
					hasException = true;
					sb.append(String.format("%s : %s\n", fakeAnimation.getParameter("path"), e.getMessage()));
					e.printStackTrace();
				}
			}
		}
		
		if (hasException) {
			return sb.toString();
		} else {
			return null;
		}
	}
}