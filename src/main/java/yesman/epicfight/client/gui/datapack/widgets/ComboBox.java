package yesman.epicfight.client.gui.datapack.widgets;

import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import com.google.common.collect.Maps;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * We're refactoring UI codes, use {@link yesman.epicfight.client.gui.widgets.ComboBox} the advanced one
 */
@Deprecated
public class ComboBox<T> extends AbstractWidget implements DataBindingComponent<T, T> {
	private final ComboItemList comboItemList;
	private final Font font;
	private final int maxRows;
	
	private Consumer<T> responder;
	private boolean useResponder = true;
	private boolean listOpened;
	
	public ComboBox(Screen parent, Font font, int x1, int x2, int y1, int y2, HorizontalSizing horizontal, VerticalSizing vertical, int maxRows, Component title, Collection<T> items, Function<T, String> displayStringMapper, Consumer<T> responder) {
		super(x1, y1, x2, y2, title);
		
		this.font = font;
		this.maxRows = Math.min(maxRows, items.size());
		
		this.responder = responder;
		this.comboItemList = new ComboItemList(Minecraft.getInstance(), this.maxRows, 15);
		
		for (T item : items) {
			this.comboItemList.addEntry(item, displayStringMapper.apply(item));
		}
		
		this.x1 = x1;
		this.x2 = x2;
		this.y1 = y1;
		this.y2 = y2;
		this.horizontalSizingOption = horizontal;
		this.verticalSizingOption = vertical;
		
		this.relocateComboList();
	}
	
	@Override
	public boolean mouseClicked(double x, double y, int button) {
		if (this.active && this.visible) {
			
			if (this.listOpened && this.comboItemList.mouseClicked(x, y, button)) {
				if (x < this.comboItemList.getScrollbarPosition() || x > this.comboItemList.getScrollbarPosition() + 6) {
					this.listOpened = false;
				}
				
				this.playDownSound(Minecraft.getInstance().getSoundManager());
				
				return true;
			} else {
				if (this.isValidClickButton(button)) {
					boolean flag = this.clicked(x, y);
					
					if (flag) {
						this.onClick(x, y);
						return true;
					}
				}
			}
			
			return false;
		}
		
		return false;
	}
	
	@Override
	public boolean mouseScrolled(double x, double y, double xScroll, double yScroll) {
		if (this.listOpened) {
			return this.comboItemList.mouseScrolled(x, y, xScroll, yScroll);
		}
		
		return false;
	}
	
	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
		if (this.listOpened) {
			return this.comboItemList.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
		}
		
		return false;
	}

	@Override
	protected boolean clicked(double x, double y) {
		return this.active && this.visible && x >= (double)this._getX() && y >= (double) this._getY() && x < (double) (this._getX() + this.width) && y < (double) (this._getY() + this.height);
	}
	
	@Override
	public boolean isMouseOver(double x, double y) {
		if (this.listOpened) {
			if (this.comboItemList.isMouseOver(x, y)) {
				return true;
			}
		}
		
		return this.active && this.visible && x >= (double)this._getX() && y >= (double) this._getY() && x < (double) (this._getX() + this.width) && y < (double) (this._getY() + this.height * (this.maxRows + 1));
	}
	
	@Override
	public void _setX(int x) {
		super.setX(x);
		this.relocateComboList();
	}
	
	@Override
	public void _setY(int y) {
		super.setY(y);
		this.relocateComboList();
	}
	
	@Override
	public void setWidth(int width) {
		this.width = width;
		
		int left = this.comboItemList.getX();
		this.comboItemList.updateSizeAndPosition(width, this.comboItemList.getBottom() - this.comboItemList.getY(), this.comboItemList.getY());
		this.comboItemList.setX(left);
	}
	
	private void relocateComboList() {
		int entryHeight = 15;
		int possibleTopPosition = this._getY() - (this.height * this.maxRows + 1);
		int possibleBottomPosition = this._getY() + this.height + this.height * this.maxRows + 1;
		int bottomSpace = Minecraft.getInstance().getWindow().getGuiScaledHeight() - possibleBottomPosition;
		int topSpace = possibleTopPosition;
		
		if (bottomSpace < topSpace) {
			this.comboItemList.updateSizeAndPosition(this.width, entryHeight * this.maxRows, this._getY() - (entryHeight * this.maxRows + 1));
		} else {
			this.comboItemList.updateSizeAndPosition(this.width, entryHeight * this.maxRows, this._getY() + this.height + 1);
		}
		
		this.comboItemList.setX(this._getX());
	}
	
	@Override
	public void onClick(double x, double y) {
		if (this.arrowClicked(x, y)) {
			this.playDownSound(Minecraft.getInstance().getSoundManager());
			this.listOpened = !this.listOpened;
		} else {
			if (this.listOpened) {
				this.listOpened = false;
				this.playDownSound(Minecraft.getInstance().getSoundManager());
			}
		}
	}
	
	private boolean arrowClicked(double x, double y) {
		int openPressed = this._getX() + this.width - 14;
		
		return this.active && this.visible && x >= (double)openPressed && y >= (double) this._getY() && x < (double) (this._getX() + this.width) && y < (double) (this._getY() + this.height);
	}
	
	@Override
	public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		int outlineColor = this.isFocused() ? -1 : this.isActive() ? -6250336 : -12566463;
		
		guiGraphics.fill(this._getX() - 1, this._getY() - 1, this._getX() + this.width + 1, this._getY() + this.height + 1, outlineColor);
		guiGraphics.fill(this._getX(), this._getY(), this._getX() + this.width, this._getY() + this.height, -16777216);
		
		String correctedString = this.font.plainSubstrByWidth(this.comboItemList.getSelected() == null ? "" : this.comboItemList.getSelected().displayName, this.width - 10);
		int fontColor = this.isActive() ? 16777215 : 4210752;
		
		guiGraphics.drawString(this.font, Component.literal(correctedString), this._getX() + 4, this._getY() + this.height / 2 - this.font.lineHeight / 2 + 1, fontColor, false);
		guiGraphics.drawString(this.font, Component.literal("▼"), this._getX() + this.width - 8, this._getY() + this.height / 2 - this.font.lineHeight / 2 + 1, fontColor, false);
		
		if (this.listOpened) {
			guiGraphics.pose().pushPose();
			guiGraphics.pose().translate(0, 0, 10);
			this.comboItemList.render(guiGraphics, mouseX, mouseY, partialTicks);
			guiGraphics.pose().popPose();
		}
	}
	
	@Override
	protected MutableComponent createNarrationMessage() {
		Component component = this._getMessage();
		return Component.translatable("gui.epicfight.narrate.comboBox", component);
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput narrationElementInput) {
		narrationElementInput.add(NarratedElementType.TITLE, this.createNarrationMessage());
	}
	
	@Override
	public void setFocused(boolean focused) {
		super.setFocused(focused);

		if (!focused) {
			this.listOpened = false;
		}
	}

	class ComboItemList extends ObjectSelectionList<ComboItemList.ComboItemEntry> {
		private final Map<T, ComboItemEntry> entryMap = Maps.newHashMap();
		
		public ComboItemList(Minecraft minecraft, int maxRows, int itemHeight) {
			super(minecraft, ComboBox.this.width, itemHeight * maxRows, 0, itemHeight);
			this.setRenderHeader(false, 0);
		}
		
		@Override
	    protected void renderListBackground(GuiGraphics guiGraphics) {
	    }

	    @Override
	    protected void renderListSeparators(GuiGraphics guiGraphics) {
	    }
		
		public void addEntry(T item, String displayName) {
			ComboItemEntry entry = new ComboItemEntry(item, displayName);
			this.entryMap.put(item, entry);
			this.addEntry(entry);
		}
		
		@Override
		public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
			guiGraphics.fill(this.getX() - 1, this.getY() - 1, this.getRight() + 1, this.getBottom() + 1, -1);
			guiGraphics.fill(this.getX(), this.getY(), this.getRight(), this.getBottom(), -16777216);
			
			super.renderWidget(guiGraphics, mouseX, mouseY, partialTicks);
		}
		
		public void setSelected(T item) {
			this.setSelected(this.entryMap.get(item));
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
		public int getMaxScroll() {
			return Math.max(0, this.getMaxPosition() - (this.getBottom() - this.getY()));
		}
		
		@Override
		protected int getRowTop(int row) {
			return this.getY() + 2 - (int) this.getScrollAmount() + row * this.itemHeight;
		}

		class ComboItemEntry extends ObjectSelectionList.Entry<ComboItemList.ComboItemEntry> {
			private final T item;
			private final String displayName;
			
			protected ComboItemEntry(T item, String displayName) {
				this.item = item;
				this.displayName = displayName;
			}
			
			@Override
			public boolean mouseClicked(double mouseX, double mouseY, int button) {
				if (button == 0) {
					ComboItemList.this.setSelected(this);
					
					if (ComboBox.this.responder != null) {
						ComboBox.this.responder.accept(this.item);
					}
					
					return true;
				} else {
					return false;
				}
			}
			
			@Override
			public Component getNarration() {
				return Component.empty();
			}
			
			@Override
			public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean isMouseOver, float partialTicks) {
				guiGraphics.drawString(ComboBox.this.font, this.displayName, left + 2, top + 1, 16777215, false);
			}
			
			public T getItem() {
				return this.item;
			}
		}
	}
	
	/*******************************************************************
	 * @ResizableComponent variables                                   *
	 *******************************************************************/
	private int x1;
	private int x2;
	private int y1;
	private int y2;
	private final HorizontalSizing horizontalSizingOption;
	private final VerticalSizing verticalSizingOption;
	
	@Override
	public void setX1(int x1) {
		this.x1 = x1;
	}

	@Override
	public void setX2(int x2) {
		this.x2 = x2;
	}

	@Override
	public void setY1(int y1) {
		this.y1 = y1;
	}

	@Override
	public void setY2(int y2) {
		this.y2 = y2;
	}
	
	@Override
	public int getX1() {
		return this.x1;
	}

	@Override
	public int getX2() {
		return this.x2;
	}

	@Override
	public int getY1() {
		return this.y1;
	}

	@Override
	public int getY2() {
		return this.y2;
	}

	@Override
	public HorizontalSizing getHorizontalSizingOption() {
		return this.horizontalSizingOption;
	}

	@Override
	public VerticalSizing getVerticalSizingOption() {
		return this.verticalSizingOption;
	}
	
	@Override
	public void _setActive(boolean active) {
		this.active = active;
	}
	
	@Override
	public void _setResponder(Consumer<T> responder) {
		this.responder = responder;
	}
	
	@Override
	public Consumer<T> _getResponder() {
		return this.responder;
	}
	
	@Override
	public void _setValue(T value) {
		this.comboItemList.setSelected(value);
		
		if (this.responder != null && this.useResponder) {
			this.responder.accept(value);
		}
	}
	
	@Override
	public T _getValue() {
		return this.comboItemList.getSelected() == null ? null : this.comboItemList.getSelected().item;
	}
	
	@Override
	public void reset() {
		this.comboItemList.setSelected((T)null);
	}
	
	@Override
	public int _getX() {
		return this.getX();
	}

	@Override
	public int _getY() {
		return this.getY();
	}

	@Override
	public int _getWidth() {
		return this.getWidth();
	}

	@Override
	public int _getHeight() {
		return this.getHeight();
	}

	@Override
	public void _setWidth(int width) {
		this.setWidth(width);
	}

	@Override
	public void _setHeight(int height) {
		this.setHeight(height);
	}

	@Override
	public Component _getMessage() {
		return this.getMessage();
	}

	@Override
	public void _renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		this.renderWidget(guiGraphics, mouseX, mouseY, partialTicks);
	}
}
