package yesman.epicfight.client.gui.datapack.screen;

import java.util.function.Consumer;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringUtil;
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.client.model.SkinnedMesh;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.client.gui.datapack.widgets.ModelPreviewer;

public class SelectAnimationScreen extends Screen {
	private final Screen parentScreen;
	private final AnimationList animationList;
	private final ModelPreviewer modelPreviewer;
	private final Consumer<AssetAccessor<? extends StaticAnimation>> selectCallback;
	private final Consumer<AssetAccessor<? extends StaticAnimation>> cancelCallback;
	private final Predicate<AssetAccessor<? extends StaticAnimation>> filter;
	private final EditBox searchBox;
	
	public SelectAnimationScreen(Screen parentScreen, Consumer<AssetAccessor<? extends StaticAnimation>> selectCallback, Consumer<AssetAccessor<? extends StaticAnimation>> cancelCallback, Predicate<AssetAccessor<? extends StaticAnimation>> filter, AssetAccessor<? extends Armature> armature, AssetAccessor<? extends SkinnedMesh> mesh) {
		super(Component.translatable("gui.epicfight.select.animations"));
		
		this.parentScreen = parentScreen;
		this.minecraft = Minecraft.getInstance();
		this.font = Minecraft.getInstance().font;
		
		this.modelPreviewer = new ModelPreviewer(10, 20, 36, 60, null, null, armature, mesh);
		this.animationList = new AnimationList(Minecraft.getInstance(), this.width, this.height - 52, 36, 21);
		this.selectCallback = selectCallback;
		this.cancelCallback = cancelCallback;
		this.filter = filter;
		this.searchBox = new EditBox(Minecraft.getInstance().font, this.width / 2, 12, this.width / 2 - 12, 16, Component.literal("datapack_edit.keyword"));
		this.searchBox.setResponder(this.animationList::refreshAniamtionList);
		
		this.animationList.refreshAniamtionList(null);
		
		if (armature != null) {
			this.searchBox.setValue(armature.get().toString().substring(armature.get().toString().indexOf("/") + 1));
			this.searchBox.moveCursorTo(0, true);
		}
	}
	
	public void refreshAnimationList() {
		this.animationList.refreshAniamtionList(this.searchBox.getValue());
	}
	
	@Override
	protected void init() {
		int split = this.width / 2 - 80;
		
		this.modelPreviewer._setWidth(split - 10);
		this.modelPreviewer._setHeight(this.height - 68);
		this.modelPreviewer.resize(null);
		
		this.animationList.updateSizeAndPosition(this.width - split, this.height - 68, 36);
		this.animationList.setX(split);
		
		this.searchBox.setX(this.width / 2);
		this.searchBox.setY(12);
		this.searchBox.setWidth(this.width / 2 - 12);
		this.searchBox.setHeight(16);
		
		this.addRenderableWidget(this.searchBox);
		this.addRenderableWidget(Button.builder(Component.translatable("datapack_edit.import_animation"), (button) -> {
			Minecraft.getInstance().setScreen(new ImportAnimationsScreen(this, this.modelPreviewer.getArmature(), this.modelPreviewer.getMesh()));
		}).pos(10, 10).size(100, 21).build());
		
		this.addRenderableWidget(this.modelPreviewer);
		this.addRenderableWidget(this.animationList);
		
		this.addRenderableWidget(Button.builder(CommonComponents.GUI_OK, (button) -> {
			if (this.animationList.getSelected() != null) {
				this.selectCallback.accept(this.animationList.getSelected().animation);
			}
			
			this.onClose();
		}).pos(this.width / 2 - 162, this.height - 28).size(160, 21).build());
		this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, (button) -> {
			this.cancelCallback.accept(null);
			this.onClose();
		}).pos(this.width / 2 + 2, this.height - 28).size(160, 21).build());
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
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
		if (this.modelPreviewer.mouseDragged(mouseX, mouseY, button, dx, dy)) {
			return true;
		}
		
		return super.mouseDragged(mouseX, mouseY, button, dx, dy);
	}
	
	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		this.renderBackground(guiGraphics, mouseX, mouseY, partialTicks);
		
		super.render(guiGraphics, mouseX, mouseY, partialTicks);
	}
	
	class AnimationList extends ObjectSelectionList<AnimationList.AnimationEntry> {
		public AnimationList(Minecraft minecraft, int width, int height, int y, int itemHeight) {
			super(minecraft, width, height, y, itemHeight);
		}
		
		@Override
		public void setSelected(@Nullable AnimationEntry selEntry) {
			super.setSelected(selEntry);
			
			SelectAnimationScreen.this.modelPreviewer.clearAnimations();
			SelectAnimationScreen.this.modelPreviewer.addAnimationToPlay(selEntry.animation);
		}
		
		@Override
		public int getRowWidth() {
			return this.width;
		}
		
		@Override
		protected int getScrollbarPosition() {
			return this.getRight() - 6;
		}
		
		@Override
	    protected void renderListBackground(GuiGraphics guiGraphics) {
	    }
		
	    @Override
	    protected void renderListSeparators(GuiGraphics guiGraphics) {
	    }
		
		public void refreshAniamtionList(String keyword) {
			this.setScrollAmount(0.0D);
			this.children().clear();
			
			AnimationManager.getInstance().getAnimations(SelectAnimationScreen.this.filter).values().stream()
				.filter((accessor) -> StringUtil.isNullOrEmpty(keyword) ? true : accessor.registryName().toString().contains(keyword))
				.map(AnimationEntry::new)
				.sorted((a1, a2) -> Integer.compare(a1.animation.get().getId(), a2.animation.get().getId()))
				.forEach(this::addEntry);
			
			DatapackEditScreen.getCurrentScreen().getUserAniamtions().values().stream()
				.map((packEntry) -> packEntry.getValue())
				.filter(SelectAnimationScreen.this.filter)
				.map(AnimationEntry::new)
				.sorted((a1, a2) -> a1.animation.registryName().compareTo(a2.animation.registryName()))
				.forEach(this::addEntry);
		}
		
		class AnimationEntry extends ObjectSelectionList.Entry<AnimationList.AnimationEntry> {
			private final AssetAccessor<? extends StaticAnimation> animation;
			
			public AnimationEntry(AssetAccessor<? extends StaticAnimation> animation) {
				this.animation = animation;
			}
			
			@Override
			public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean isMouseOver, float partialTicks) {
				guiGraphics.drawString(SelectAnimationScreen.this.minecraft.font, this.animation.registryName().toString(), left + 5, top + 5, 16777215, false);
			}
			
			@Override
			public Component getNarration() {
				return Component.translatable("narrator.select");
			}
			
			@Override
			public boolean mouseClicked(double mouseX, double mouseY, int button) {
				if (button == 0) {
					if (AnimationList.this.getSelected() == this) {
						SelectAnimationScreen.this.selectCallback.accept(this.animation);
						SelectAnimationScreen.this.minecraft.setScreen(SelectAnimationScreen.this.parentScreen);
						return true;
					}
					
					AnimationList.this.setSelected(this);
					
					return true;
				} else {
					return false;
				}
			}
		}
	}
}
