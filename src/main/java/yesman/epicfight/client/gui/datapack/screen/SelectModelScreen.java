package yesman.epicfight.client.gui.datapack.screen;

import java.util.Set;
import java.util.function.BiConsumer;

import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringUtil;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.client.model.Meshes;
import yesman.epicfight.api.client.model.SkinnedMesh;
import yesman.epicfight.client.gui.datapack.widgets.ModelPreviewer;

public class SelectModelScreen extends Screen {
	private final Screen parentScreen;
	private final ModelList modelList;
	private final ModelPreviewer modelPreviewer;
	private final EditBox searchBox;
	private final BiConsumer<String, AssetAccessor<SkinnedMesh>> selectCallback;
	private final BiConsumer<String, AssetAccessor<SkinnedMesh>> cancelCallback;
	
	public SelectModelScreen(Screen parentScreen, BiConsumer<String, AssetAccessor<SkinnedMesh>> selectCallback, BiConsumer<String, AssetAccessor<SkinnedMesh>> cancelCallback) {
		super(Component.translatable("gui.epicfight.select.models"));
		
		this.parentScreen = parentScreen;
		this.minecraft = Minecraft.getInstance();
		this.font = Minecraft.getInstance().font;
		
		this.modelPreviewer = new ModelPreviewer(10, 20, 36, 60, null, null, null, null);
		this.modelList = new ModelList(this.minecraft, this.width, this.height - 52, 36, 21);
		this.searchBox = new EditBox(this.font, this.width / 2, 12, this.width / 2 - 12, 16, Component.literal("datapack_edit.keyword"));
		this.searchBox.setResponder(this.modelList::refreshModelList);
		this.selectCallback = selectCallback;
		this.cancelCallback = cancelCallback;
		
		this.modelList.refreshModelList(null);
	}
	
	public void refreshModelList() {
		this.modelList.refreshModelList(this.searchBox.getValue());
	}
	
	@Override
	protected void init() {
		int split = this.width / 2 - 80;
		
		this.modelPreviewer._setWidth(split - 10);
		this.modelPreviewer._setHeight(this.height - 68);
		this.modelPreviewer.resize(null);
		
		this.modelList.updateSizeAndPosition(this.width - split, this.height - 68, 36);
		this.modelList.setX(split);
		
		this.searchBox.setX(this.width / 2);
		this.searchBox.setY(12);
		this.searchBox.setWidth(this.width / 2 - 12);
		this.searchBox.setHeight(16);
		
		this.addRenderableWidget(this.searchBox);
		this.addRenderableWidget(Button.builder(Component.translatable("datapack_edit.import_model"), (button) -> Minecraft.getInstance().setScreen(new ImportModelScreen(this))).pos(10, 10).size(100, 21).build());
		
		this.addRenderableWidget(this.modelPreviewer);
		this.addRenderableWidget(this.modelList);
		
		this.addRenderableWidget(Button.builder(CommonComponents.GUI_OK, (button) -> {
			if (this.modelList.getSelected() == null) {
				this.minecraft.setScreen(new MessageScreen<>("", "Select an item from the list", this, (button$2) -> {
					this.minecraft.setScreen(this);
				}, 180, 60));
			} else {
				try {
					this.selectCallback.accept(this.modelList.getSelected().registryName, this.modelList.getSelected().mesh);
					this.onClose();
				} catch (Exception e) {
					this.minecraft.setScreen(new MessageScreen<>("", e.getMessage(), this.parentScreen, (button$2) -> this.minecraft.setScreen(this.parentScreen), 180, 70).autoCalculateHeight());
				}
			}
			
		}).pos(this.width / 2 - 162, this.height - 28).size(160, 21).build());
		this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, (button) -> {
			this.cancelCallback.accept(StringUtils.EMPTY, null);
			this.onClose();
		}).pos(this.width / 2 + 2, this.height - 28).size(160, 21).build());
	}
	
	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
		if (this.modelPreviewer.mouseDragged(mouseX, mouseY, button, dx, dy)) {
			return true;
		}
		
		return super.mouseDragged(mouseX, mouseY, button, dx, dy);
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
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		this.renderBackground(guiGraphics, mouseX, mouseY, partialTicks);
		super.render(guiGraphics, mouseX, mouseY, partialTicks);
	}
	
	class ModelList extends ObjectSelectionList<ModelList.ModelEntry> {
		public ModelList(Minecraft minecraft, int width, int height, int y, int itemHeight) {
			super(minecraft, width, height, y, itemHeight);
		}
		
		@Override
		public void setSelected(@Nullable ModelEntry selEntry) {
			super.setSelected(selEntry);
			
			SelectModelScreen.this.modelPreviewer.setMesh(selEntry.mesh);
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
		
		@SuppressWarnings("unchecked")
		public void refreshModelList(String keyward) {
			this.setScrollAmount(0.0D);
			this.children().clear();
			
			Set<AssetAccessor<SkinnedMesh>> skinnedMeshes = Meshes.entry(SkinnedMesh.class);
			DatapackEditScreen.getCurrentScreen().getUserMeshes().values().forEach((accessor) -> skinnedMeshes.add((AssetAccessor<SkinnedMesh>) accessor));
			
			skinnedMeshes.stream().filter((accessor) -> StringUtil.isNullOrEmpty(keyward) ? true : accessor.registryName().toString().contains(keyward)).map((accessor) -> new ModelEntry(accessor.registryName().toString(), accessor))
														.sorted((entry$1, entry$2) -> entry$1.registryName.compareTo(entry$2.registryName)).forEach(this::addEntry);
		}
		
		class ModelEntry extends ObjectSelectionList.Entry<ModelList.ModelEntry> {
			private final String registryName;
			private final AssetAccessor<SkinnedMesh> mesh;
			
			public ModelEntry(String registryName, AssetAccessor<SkinnedMesh> mesh) {
				this.registryName = registryName;
				this.mesh = mesh;
			}
			
			@Override
			public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean isMouseOver, float partialTicks) {
				guiGraphics.drawString(SelectModelScreen.this.minecraft.font, this.registryName, left + 5, top + 5, 16777215, false);
			}
			
			@Override
			public Component getNarration() {
				return Component.translatable("narrator.select");
			}
			
			@Override
			public boolean mouseClicked(double mouseX, double mouseY, int button) {
				if (button == 0) {
					if (ModelList.this.getSelected() == this) {
						try {
							SelectModelScreen.this.selectCallback.accept(this.registryName, this.mesh);
							SelectModelScreen.this.onClose();
						} catch (Exception e) {
							SelectModelScreen.this.minecraft.setScreen(new MessageScreen<>("", e.getMessage(), SelectModelScreen.this.parentScreen, (button$2) -> SelectModelScreen.this.minecraft.setScreen(SelectModelScreen.this.parentScreen), 180, 70).autoCalculateHeight());
						}
						
						return true;
					}
					
					ModelList.this.setSelected(this);
					
					return true;
				} else {
					return false;
				}
			}
		}
	}
}
