package yesman.epicfight.client.gui.datapack.widgets;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import org.lwjgl.opengl.GL11;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import yesman.epicfight.api.utils.ParseUtil;
import yesman.epicfight.client.gui.datapack.widgets.PopupBox.PopupBoxProvider;
import yesman.epicfight.client.gui.datapack.widgets.PopupBox.RegistryPopupBox;

public class Grid extends ObjectSelectionList<Grid.Row> implements DataBindingComponent<Object, Object> {
	private final Screen owner;
	private final Map<String, Column<?, ?>> columns = Maps.newLinkedHashMap();
	private final List<ResizableButton> rowEditButtons = Lists.newArrayList();
	private final BiConsumer<Integer, Map<String, Object>> onRowpositionChanged;
	private final int columnSizeSum;
	
	private ResizableComponent editingWidget;
	private Column<?, ?> editingColumn;
	private boolean active = true;
	private boolean rowpositionChangeEnabled = true;
	private boolean valueChangeEnabled = true;
	private int rowposition = -1;
	
	public Grid(GridBuilder gb) {
		super(gb.minecraft, gb.x2, gb.y2, gb.y1, gb.rowHeight);
		
		this.owner = gb.owner;
		this.onRowpositionChanged = gb.onRowpositionChanged;
		this.horizontalSizingOption = gb.horizontalSizing;
		this.verticalSizingOption = gb.verticalSizing;
		
		this.xParam1 = gb.x1;
		this.yParam1 = gb.y1;
		this.xParam2 = gb.x2;
		this.yParam2 = gb.y2;
		
		this.columnSizeSum = gb.columnSizeTotal;
		gb.columns.forEach(this.columns::put);
		
		this.resize(gb.parentScreenRectangleProvider.get());
		this.setX(gb.x1);
		
		if (gb.rowEditButtons.add) {
			this.rowEditButtons.add(ResizableButton.builder(Component.literal("+"), (button) -> gb.onAddPress.accept(this, button)).pos(0, 0).size(12, 12).build());
		}
		
		if (gb.rowEditButtons.remove) {
			this.rowEditButtons.add(ResizableButton.builder(Component.literal("-"), (button) -> gb.onRemovePress.accept(this, button)).pos(0, 0).size(12, 12).build());
		}
		
		this.relocateButtons();
	}
	
	public int addRow() {
		return this.addRow(children().size());
	}
	
	public int addRow(IntConsumer onAdd) {
		return this.addRow(children().size(), onAdd);
	}
	
	public int addRow(int rowposition) {
		return this.addRow(rowposition, null);
	}
	
	public int addRowWithDefaultValues(Object... defaultValues) {
		return addRow(this.children().size(), null, defaultValues);
	}
	
	public int addRow(int rowposition, IntConsumer onAdd, Object... defaultValues) {
		this.editingColumn = null;
		this.editingWidget = null;
		
		Row row = new Row();
		this.children().add(rowposition, row);
		
		if (onAdd != null) {
			onAdd.accept(rowposition);
		}
		
		for (Map.Entry<String, Column<?, ?>> entry : this.columns.entrySet()) {
			row.setValue(entry.getKey(), entry.getValue().defaultVal);
		}
		
		for (int i = 0; i < defaultValues.length; i+=2) {
			row.setValue((String)defaultValues[i], defaultValues[i + 1]);
		}
		
		this.resizeColumnWidth();
		
		return rowposition;
	}
	
	public int removeRow() {
		return this.removeRow(this.rowposition);
	}
	
	public int removeRow(int row) {
		return this.removeRow(row, null);
	}
	
	public int removeRow(IntConsumer callback) {
		return this.removeRow(this.rowposition, callback);
	}
	
	public int removeRow(int row, IntConsumer callback) {
		if (row < 0) {
			return -1;
		}
		
		if (this.children().size() == 0) {
			return -1;
		}
		
		if (this.rowposition == row) {
			this.editingColumn = null;
			this.editingWidget = null;
		}
		
		this.children().remove(row);
		
		double scrollAmount = this.getScrollAmount();
		this.setScrollAmount(Math.min(scrollAmount, this.getMaxScroll()));
		
		this.resizeColumnWidth();
		
		if (callback != null) {
			callback.accept(row);
		}
		
		int newRow = Math.min(row, this.children().size() - 1);
		
		if (newRow >= 0) {
			int oldRowpos = this.rowposition;
			
			this.setSelected(newRow);
			
			if (newRow == oldRowpos) {
				if (this.onRowpositionChanged != null && this.rowposition > -1 && this.rowpositionChangeEnabled) {
					this.onRowpositionChanged.accept(this.rowposition, this.children().get(this.rowposition).values);
				}
			}
		} else {
			this.setSelected(null);
		}
		
		return row;
	}
	
	public Grid setValueChangeEnabled(boolean enabled) {
		this.valueChangeEnabled = enabled;
		return this;
	}
	
	public Grid setRowpositionChangeEnabled(boolean enabled) {
		this.rowpositionChangeEnabled = enabled;
		return this;
	}
	
	public void setSelected(int rowposition) {
		this.setSelected(this.children().get(rowposition));
	}
	
	@Override
	public void setSelected(@Nullable Grid.Row row) {
		super.setSelected(row);
		this.setRowposition(this.children().indexOf(row));
	}
	
	private void setRowposition(int position) {
		if (this.rowposition != position) {
			this.rowposition = position;
			
			if (this.onRowpositionChanged != null && this.rowposition > -1 && this.rowpositionChangeEnabled) {
				this.onRowpositionChanged.accept(this.rowposition, this.children().get(this.rowposition).values);
			}
		}
	}
	
	public int getRowposition() {
		return this.rowposition;
	}
	
	public <T> T getValue(int rowposition, String columnName) {
		return this.children().get(rowposition).getValue(columnName);
	}
	
	public <T> void setValue(int rowposition, String columnName, T value) {
		Row row = this.children().get(rowposition);
		row.setValue(columnName, value);
	}
	
	public void setGridFocus(int rowposition, String columnName) {
		if (this.owner.getFocused() != this) {
			this.owner.setFocused(this);
		}
		
		this.setSelected(rowposition);
		
		int startX = 0;
		this.editingColumn = null;
		
		for (Map.Entry<String, Column<?, ?>> entry : this.columns.entrySet()) {
			if (entry.getKey() == columnName) {
				this.editingColumn = entry.getValue();
				break;
			}
			
			startX += entry.getValue().width;
		}
		
		if (this.editingColumn == null) {
			this.editingWidget = null;
		} else {
			if (this.editingColumn.editable) {
				this.editingWidget =
					this.editingColumn.createEditWidget(
						  this.owner
						, Minecraft.getInstance().font
						, this.getX() + startX + 2
						, this.getRowTop(rowposition) + 2
						, this.itemHeight - 3
						, rowposition
						, this.getSelected()
						, columnName
						, this.getSelected().getValue(columnName)
					);
				
				if (this.editingWidget != null) {
					this.editingWidget.setFocused(true);
				}
			}
		}
	}
	
	public void visitRows(Consumer<Map<String, Object>> task) {
		this.children().forEach((row) -> task.accept(row.values));
	}
	
	public List<ResizableButton> getRowEditButtons() {
		return this.rowEditButtons;
	}
	
	private void relocateButtons() {
		int x = this.getRight() - 12;
		int y = this.getY() - 12;
		
		for (Button rowEditButton : Lists.reverse(this.rowEditButtons)) {
			rowEditButton.setPosition(x, y);
			x -= 12;
		}
	}
	
	@Override
	public void resize(ScreenRectangle screenRectangle) {
		if (this.getHorizontalSizingOption() != null) {
			this.getHorizontalSizingOption().resizeFunction.resize(this, screenRectangle, this.getX1(), this.getX2());
		}
		
		if (this.getVerticalSizingOption() != null) {
			this.getVerticalSizingOption().resizeFunction.resize(this, screenRectangle, this.getY1(), this.getY2());
		}
		
		this.relocateButtons();
		this.resizeColumnWidth();
	}
	
	protected void resizeColumnWidth() {
		int remainWidth = this.width - (this.getMaxScroll() > 0 ? 7 : 1);
		int idx = 0;
		int size = this.columns.size();
		
		for (Column<?, ?> col : this.columns.values()) {
			col.width = (int)((float)col.initialWidth * ((float)this.width / this.columnSizeSum));
			remainWidth -= col.width;
			idx++;
			
			if (idx == size && remainWidth != 0) {
				col.width += remainWidth;
			}
		}
		
		if (this.editingWidget != null) {
			int width = 0;
			
			for (Column<?, ?> column : this.columns.values()) {
				if (column == this.editingColumn) {
					break;
				}
				
				width += column.width + 1;
			}
			
			this.editingWidget._setX(this._getX() + width + 1);
			this.editingWidget._setWidth(this.editingColumn.width - 3);
		}
	}
	
	@Override
	public void setX(int x) {
		super.setX(x);
		this.relocateButtons();
	}

	@Override
	public int getRowLeft() {
		return this.getX() + this.width / 2 - this.getRowWidth() / 2 + 2;
	}
	
	@Override
	public int getRowRight() {
		return this.getRowLeft() + this.getRowWidth();
	}
	
	@Override
	protected int getRowTop(int p_93512_) {
		return this.getY() - (int) this.getScrollAmount() + p_93512_ * this.itemHeight + this.headerHeight;
	}
	
	@Override
	protected int getRowBottom(int p_93486_) {
		return this.getRowTop(p_93486_) + this.itemHeight;
	}
	
	@Override
	public void setFocused(boolean focused) {
		if (!focused) {
			this.editingColumn = null;
			this.editingWidget = null;
		}
	}
	
	@Override
	public boolean isFocused() {
		return this.owner.getFocused() == this;
	}
	
	@Override
	public int getMaxScroll() {
		return Math.max(0, this.getMaxPosition() - (this.height - 1));
	}
	
	@Override
    protected void renderListBackground(GuiGraphics guiGraphics) {
    }

    @Override
    protected void renderListSeparators(GuiGraphics guiGraphics) {
    }
	
    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		this.rowEditButtons.forEach((button) -> button.render(guiGraphics, mouseX, mouseY, partialTick));
		
		int color = this.isFocused() ? -1 : this.isActive() ? -6250336 : -12566463;
		GL11.glEnable(GL11.GL_STENCIL_TEST);
		
		RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE);
		RenderSystem.stencilFunc(GL11.GL_ALWAYS, 1, 0xFF);
		RenderSystem.stencilMask(0xFF);
		// Clear doesn't work if stencil mask is set to 0
		RenderSystem.clear(GL11.GL_STENCIL_BUFFER_BIT, true);
		
		guiGraphics.fill(this.getX(), this.getY(), this.getRight(), this.getBottom(), color);
		RenderSystem.stencilFunc(GL11.GL_EQUAL, 1, 0xFF);
		RenderSystem.stencilMask(0x00);
		
		this.renderListItems(guiGraphics, mouseX, mouseY, partialTick);
		
		GL11.glDisable(GL11.GL_STENCIL_TEST);
		
		if (this.editingWidget != null) {
			int rowTop = this.getRowTop(this.rowposition);
			int rowBottom = this.getRowTop(this.rowposition) + this.itemHeight;
			
			if (rowBottom >= this.getY() && rowTop <= this.getBottom()) {
				guiGraphics.pose().pushPose();
				guiGraphics.pose().translate(0, 0, 1);
				this.editingWidget._setY(this.getRowTop(this.rowposition) + 2);
				this.editingWidget.asWidget().render(guiGraphics, mouseX, mouseY, partialTick);
				guiGraphics.pose().popPose();
			}
		}
	}
	
	@Override
	protected void renderListItems(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		int rowLeft = this.getRowLeft() - 1;
		int rowWidth = this.getRowWidth();
		int itemHeight = this.itemHeight;
		int itemCount = this.getItemCount();
		int rowBottom = this.getY();
		
		for (int rowIndex = 0; rowIndex < itemCount; ++rowIndex) {
			int rowTop = this.getRowTop(rowIndex);
			rowBottom = this.getRowTop(rowIndex) + this.itemHeight;
			
			if (rowBottom >= this.getY() && rowTop <= this.getBottom()) {
				this.renderItem(guiGraphics, mouseX, mouseY, partialTicks, rowIndex, rowLeft, rowTop, rowWidth, itemHeight);
			}
		}
		
		if (rowBottom + 1 < this.getBottom() - 1) {
			guiGraphics.fill(rowLeft, rowBottom + 1, rowLeft + rowWidth - 2, this.getBottom() - 1, -16777216);
		}
		
		guiGraphics.pose().pushPose();
		guiGraphics.pose().translate(0, 0, 1);
		
		int i = this.getScrollbarPosition();
		int j = i + 6;
		int i2 = this.getMaxScroll();
		
		if (i2 > 0) {
			int j2 = (int) ((float) (this.height * this.height) / (float) this.getMaxPosition());
			j2 = Mth.clamp(j2, 32, this.height - 8);
			int k1 = (int) this.getScrollAmount() * (this.height - j2) / i2 + this.getY();
			
			if (k1 < this.getY()) {
				k1 = this.getY();
			}
			
			guiGraphics.fill(i, this.getY(), j, this.getBottom(), -16777216);
			guiGraphics.fill(i, k1, j, k1 + j2, -8355712);
			guiGraphics.fill(i, k1, j - 1, k1 + j2 - 1, -4144960);
		}
		
		guiGraphics.pose().popPose();
	}
	
	@Override
	protected void renderItem(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks, int rowPosition, int rowLeft, int rowTop, int rowRight, int itemHeight) {
		Row row = this.getEntry(rowPosition);
		
		if (this.isSelectedItem(rowPosition)) {
			guiGraphics.pose().pushPose();
			guiGraphics.pose().translate(0, 0, 1);
			this.renderSelection(guiGraphics, rowTop, rowRight, itemHeight, 0, 0); // color params are not used
			row.render(guiGraphics, rowPosition, rowTop, rowLeft, rowRight, itemHeight, mouseX, mouseY, false, partialTicks);
			guiGraphics.pose().popPose();
		} else {
			row.render(guiGraphics, rowPosition, rowTop, rowLeft, rowRight, itemHeight, mouseX, mouseY, false, partialTicks);
		}
	}
	
	@Override
	protected void renderSelection(GuiGraphics guiGraphics, int rowTop, int rowRight, int itemHeight, int color, int color2) {
		guiGraphics.fill(this.getX(), rowTop, this.getRight(), rowTop + itemHeight + 1, -1);
	}
	
	@Override
	public boolean isMouseOver(double x, double y) {
		double y0 = this.rowEditButtons.size() > 0 ? this.getY() - 12 : this.getY();
		return (this.editingWidget != null && this.editingWidget.isMouseOver(x, y)) || y >= y0 && y <= (double)this.getBottom() && x >= (double)this.getX() && x <= (double)this.getRight();
	}
	
	@Override
	public boolean mouseClicked(double x, double y, int button) {
		if (!this.isActive()) {
			return false;
		}
		
		for (Button editButton : this.rowEditButtons) {
			if (editButton.mouseClicked(x, y, button)) {
				return true;
			}
		}
		
		if (this.editingWidget != null) {
			if (this.editingWidget.mouseClicked(x, y, button)) {
				return true;
			}
		}
		
		if (!this.isMouseOver(x, y)) {
			return false;
		}
		
		if (super.mouseClicked(x, y, button)) {
			return true;
		}
		
		return false;
	}
	
	@Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (this.editingWidget != null) {
			if (this.editingWidget.isMouseOver(mouseX, mouseY) && this.editingWidget.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
				return true;
			}
		}
		
		if (this.isFocused() && this.getMaxScroll() > 0) {
			this.setScrollAmount(this.getScrollAmount() - scrollY * (double) this.itemHeight / 2.0D);
			return true;
		}
		
		return false;
    }
	
	@Override
	public boolean keyPressed(int keycode, int p_100876_, int p_100877_) {
		if (!this.isActive()) {
			return false;
		}
		
		if (this.editingWidget != null) {
			return this.editingWidget.keyPressed(keycode, p_100876_, p_100877_);
		}
		
		return super.keyPressed(keycode, p_100876_, p_100877_);
	}
	
	@Override
	public boolean charTyped(char c, int i) {
		if (!this.isActive()) {
			return false;
		}
		
		if (this.editingWidget != null) {
			return this.editingWidget.charTyped(c, i);
		}
		
		return super.charTyped(c, i);
	}

	@Override
	public boolean isActive() {
		return this.active;
	}
	
	@Override
	public int getRowWidth() {
		return this.width;
	}
	
	@Override
	protected int getScrollbarPosition() {
		return this.getRight() - 6;
	}
	
	public class Row extends ObjectSelectionList.Entry<Grid.Row> {
		private Map<String, Object> values = Maps.newLinkedHashMap();
		
		private Row() {
			for (String columnName : Grid.this.columns.keySet()) {
				this.values.put(columnName, null);
			}
		}
		
		@SuppressWarnings("unchecked")
		public <T> T getValue(String columnName) {
			return (T)this.values.get(columnName);
		}
		
		@SuppressWarnings("unchecked")
		public <T> void setValue(String columnName, T value) {
			if (!Grid.this.columns.containsKey(columnName)) {
				throw new IllegalArgumentException("There's no column named " + columnName + " in Grid");
			}
			
			T oldVal = (T)this.values.get(columnName);
			this.values.put(columnName, value);
			
			Column<T, ?> column = (Column<T, ?>) Grid.this.columns.get(columnName);
			
			if (column.onValueChanged != null && Grid.this.valueChangeEnabled && !ParseUtil.compareNullables(oldVal, value)) {
				int idx = Grid.this.children().indexOf(this);
				
				if (idx >= 0) {
					column.onValueChanged.accept(new ValueChangeEvent<> (Grid.this, Grid.this.children().indexOf(this), oldVal, value));
				}
			}
		}
		
		@Override
		public Component getNarration() {
			return Component.translatable("narrator.select");
		}
		
		private boolean rowHighlight() {
			return Grid.this.isFocused() && Grid.this.getSelected() == this;
		}
		
		@Override
		public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean isMouseOver, float partialTicks) {
			int startX = Grid.this.getX();
			int start = this.rowHighlight() ? 2 : 1;
			int size = this.values.size();
			int idx = 0;
			
			for (Map.Entry<String, Object> entry : this.values.entrySet()) {
				Column<?, ?> column = Grid.this.columns.get(entry.getKey());
				boolean first = idx == 0;
				boolean last = idx == (size - 1);
				int end = this.rowHighlight() ? 1 : 0;
				guiGraphics.fill(startX + (first ? start : 1), top + start, startX + column.width - (last ? end : 0), top + height - end, -16777216);
			
				String displayText = column.toDisplayText(entry.getValue());
				String correctedString = Grid.this.minecraft.font.plainSubstrByWidth(displayText, column.width - 1);
				guiGraphics.drawString(Grid.this.minecraft.font, correctedString, startX + 3, top + Grid.this.itemHeight / 2 - 4, 16777215, false);
				
				startX += column.width;
				idx++;
			}
		}
		
		@Override
		public boolean mouseClicked(double mouseX, double mouseY, int button) {
			if (button == 0) {
				int rowposition = Grid.this.children().indexOf(this);
				
				if (Grid.this.getSelected() == this) {
					if (Grid.this.editingColumn == null || Grid.this.editingColumn != this.getColumn(mouseX)) {
						Grid.this.setGridFocus(rowposition, this.getColumnName(mouseX));
					} else {
						Grid.this.setGridFocus(rowposition, null);
					}
				} else {
					Grid.this.editingWidget = null;
					Grid.this.setGridFocus(rowposition, this.getColumnName(mouseX));
				}
				
				Grid.this.setSelected(this);
				
				return true;
			} else {
				return false;
			}
		}
		
		public Column<?, ?> getColumn(double mouseX) {
			double x = Grid.this.getX();
			
			for (Column<?, ?> entry : Grid.this.columns.values()) {
				x += entry.width;
				
				if (mouseX < x) {
					return entry;
				}
			}
			
			return null;
		}
		
		public String getColumnName(double mouseX) {
			double x = Grid.this.getX();
			
			for (Map.Entry<String, Column<?, ?>> entry : Grid.this.columns.entrySet()) {
				x += entry.getValue().width;
				
				if (mouseX < x) {
					return entry.getKey();
				}
			}
			
			return null;
		}
	}
	
	public static GridBuilder builder(Screen owner) {
		return new GridBuilder(owner);
	}
	
	public static GridBuilder builder(Screen owner, Minecraft minecraft) {
		return new GridBuilder(owner, minecraft);
	}
	
	public static EditBoxColumnBuilder editbox(String string) {
		return new EditBoxColumnBuilder(string);
	}
	
	public static <T> ComboBoxColumnBuilder<T> combo(String string, Collection<T> selectionList) {
		return new ComboBoxColumnBuilder<>(string, selectionList);
	}
	
	public static <T> RegistryPopupColumnBuilder<T> registryPopup(String string, Registry<T> registry) {
		return new RegistryPopupColumnBuilder<>(string, registry);
	}
	
	public static <T, P extends PopupBox<T>> PopupColumnBuilder<T, P> popup(String string, PopupBoxProvider<T, P> popupBoxProvider) {
		return new PopupColumnBuilder<>(string, popupBoxProvider);
	}
	
	@SuppressWarnings("rawtypes")
	public static <T, W extends AbstractWidget & DataBindingComponent> WildcardColumnBuilder<T, W> wildcard(String string) {
		return new WildcardColumnBuilder<>(string);
	}
	
	public static class GridBuilder {
		private final Minecraft minecraft;
		private final Screen owner;
		private final Map<String, Column<?, ?>> columns = Maps.newLinkedHashMap();
		private int x1;
		private int y1;
		private int x2;
		private int y2;
		private int rowHeight;
		private int columnSizeTotal;
		private Supplier<ScreenRectangle> parentScreenRectangleProvider;
		private BiConsumer<Grid, Button> onAddPress;
		private BiConsumer<Grid, Button> onRemovePress;
		private BiConsumer<Integer, Map<String, Object>> onRowpositionChanged;
		private HorizontalSizing horizontalSizing = null;
		private VerticalSizing verticalSizing = null;
		private RowEditButton rowEditButtons = RowEditButton.NONE;
		
		private GridBuilder(Screen owner) {
			this(owner, Minecraft.getInstance());
		}
		
		private GridBuilder(Screen owner, Minecraft minecraft) {
			this.owner = owner;
			this.minecraft = minecraft;
			this.parentScreenRectangleProvider = owner::getRectangle;
		}
		
		public <T, C extends Column<T, W>, W extends AbstractWidget> GridBuilder addColumn(ColumnBuilder<T, C, W> builder) {
			this.columns.put(builder.name, builder.create());
			this.columnSizeTotal += builder.width;
			
			return this;
		}
		
		public GridBuilder xy1(int x1, int y1) {
			this.x1 = x1;
			this.y1 = y1;
			return this;
		}
		
		public GridBuilder xy2(int x2, int y2) {
			this.x2 = x2;
			this.y2 = y2;
			return this;
		}
		
		public GridBuilder rowHeight(int rowHeight) {
			this.rowHeight = rowHeight;
			return this;
		}
		
		public GridBuilder rowEditable(RowEditButton rowEditButtons) {
			this.rowEditButtons = rowEditButtons;
			return this;
		}
		
		public GridBuilder screenRectangleProvider(Supplier<ScreenRectangle> parentScreenRectangleProvider) {
			this.parentScreenRectangleProvider = parentScreenRectangleProvider;
			return this;
		}
		
		public GridBuilder pressAdd(BiConsumer<Grid, Button> onAddPress) {
			this.onAddPress = onAddPress;
			return this;
		}
		
		public GridBuilder pressRemove(BiConsumer<Grid, Button> OnRemovePress) {
			this.onRemovePress = OnRemovePress;
			return this;
		}
		
		public GridBuilder rowpositionChanged(BiConsumer<Integer, Map<String, Object>> onRowpositionChanged) {
			this.onRowpositionChanged = onRowpositionChanged;
			return this;
		}
		
		public GridBuilder horizontalSizing(HorizontalSizing horizontalSizing) {
			this.horizontalSizing = horizontalSizing;
			return this;
		}
		
		public GridBuilder verticalSizing(VerticalSizing verticalSizing) {
			this.verticalSizing = verticalSizing;
			return this;
		}
		
		public Grid build() {
			return new Grid(this);
		}
		
		public enum RowEditButton {
			ADD(true, false), REMOVE(false, true), ADD_REMOVE(true, true), NONE(false, false);
			
			public boolean add;
			public boolean remove;
			
			RowEditButton(boolean add, boolean remove) {
				this.add = add;
				this.remove = remove;
			}
		}
	}
	
	private static abstract class Column<T, W extends AbstractWidget> {
		final Function<T, String> toDisplayText;
		final Consumer<ValueChangeEvent<T>> onValueChanged;
		final Consumer<W> onEditWidgetCreate;
		final T defaultVal;
		final boolean editable;
		final int initialWidth;
		int width;
		
		private Column(Function<T, String> toDisplayText, Consumer<ValueChangeEvent<T>> onValueChanged, Consumer<W> onEditWidgetCreate, T defaultVal, boolean editable, int width) {
			this.toDisplayText = toDisplayText;
			this.onValueChanged = onValueChanged;
			this.onEditWidgetCreate = onEditWidgetCreate;
			this.defaultVal = defaultVal;
			this.initialWidth = width;
			this.editable = editable;
		}
		
		@SuppressWarnings("unchecked")
		public String toDisplayText(Object object) {
			return this.toDisplayText.apply((T)object);
		}
		
		public abstract ResizableComponent createEditWidget(Screen owner, Font font, int x, int y, int height, int rowposition, Row row, String colName, T value);
	}
	
	private static class EditBoxColumn extends Column<String, EditBox> {
		private EditBoxColumn(Function<String, String> toDisplayText, Consumer<ValueChangeEvent<String>> onValueChanged, Consumer<EditBox> onEditWidgetCreate, String defaultVal, boolean editable, int size) {
			super(toDisplayText, onValueChanged, onEditWidgetCreate, defaultVal, editable, size);
		}
		
		@Override
		public ResizableComponent createEditWidget(Screen owner, Font font, int x, int y, int height, int rowposition, Row row, String colName, String value) {
			ResizableEditBox editbox = new ResizableEditBox(font, x, this.width - 3, y, height, Component.literal("grid.editbox"), null, null);
			
			editbox.setMaxLength(100);
			editbox.setValue(value);
			editbox.setResponder((string) -> row.setValue(colName, string));
			
			if (this.onEditWidgetCreate != null) {
				this.onEditWidgetCreate.accept(editbox);
			}
			
			return editbox;
		}
	}
	
	private static class ComboColumn<T> extends Column<T, ComboBox<T>> {
		final Collection<T> comboItemCollection;
		
		private ComboColumn(Function<T, String> toDisplayText, Consumer<ValueChangeEvent<T>> onValueChanged, Consumer<ComboBox<T>> onEditWidgetCreate, T defaultVal, Collection<T> enums, boolean editable, int size) {
			super(toDisplayText, onValueChanged, onEditWidgetCreate, defaultVal, editable, size);
			
			this.comboItemCollection = enums;
		}
		
		@Override
		public ResizableComponent createEditWidget(Screen owner, Font font, int x, int y, int height, int rowposition, Row row, String colName, T value) {
			ComboBox<T> comboBox = new ComboBox<>(owner, font, x, this.width - 3, y, height, null, null, Math.min(this.comboItemCollection.size(), 8), Component.literal("grid.comboEdit"), this.comboItemCollection, this.toDisplayText, (item) -> row.setValue(colName, item));
			comboBox._setValue(value);
			
			if (this.onEditWidgetCreate != null) {
				this.onEditWidgetCreate.accept(comboBox);
			}
			
			return comboBox;
		}
	}
	
	private static class RegistryPopupColumn<T> extends Column<T, PopupBox.RegistryPopupBox<T>> {
		final Registry<T> registry;
		final Predicate<T> filter;
		
		private RegistryPopupColumn(Function<T, String> toDisplayText, Consumer<ValueChangeEvent<T>> onValueChanged, Consumer<PopupBox.RegistryPopupBox<T>> onEditWidgetCreate, T defaultVal, Registry<T> registry, Predicate<T> filter, boolean editable, int size) {
			super(toDisplayText, onValueChanged, onEditWidgetCreate, defaultVal, editable, size);
			
			this.registry = registry;
			this.filter = filter;
		}
		
		@Override
		public ResizableComponent createEditWidget(Screen owner, Font font, int x, int y, int height, int rowposition, Row row, String colName, T value) {
			PopupBox.RegistryPopupBox<T> popup = new PopupBox.RegistryPopupBox<>(owner, font, x, this.width - 3, y, height, null, null, Component.literal("grid.popupEdit"), this.registry, (pair) -> row.setValue(colName, pair.getSecond()));
			
			popup.applyFilter(this.filter);
			popup._setValue(value);
			
			if (this.onEditWidgetCreate != null) {
				this.onEditWidgetCreate.accept(popup);
			}
			
			return popup;
		}
	}
	
	private static class PopupColumn<T, P extends PopupBox<T>> extends Column<T, P> {
		final PopupBoxProvider<T, P> popupBoxProvider;
		final Predicate<T> filter;
		
		private PopupColumn(Function<T, String> toDisplayText, Consumer<ValueChangeEvent<T>> onValueChanged, Consumer<P> onEditWidgetCreate, T defaultVal, PopupBoxProvider<T, P> popupBoxProvider, Predicate<T> filter, Consumer<P> onCreate, boolean editable, int size) {
			super(toDisplayText, onValueChanged, onEditWidgetCreate, defaultVal, editable, size);
			
			this.popupBoxProvider = popupBoxProvider;
			this.filter = filter;
		}
		
		@Override
		public ResizableComponent createEditWidget(Screen owner, Font font, int x, int y, int height, int rowposition, Row row, String colName, T value) {
			P popup = this.popupBoxProvider.create(owner, font, x, this.width - 3, y, height, null, null, Component.literal("grid.popupEdit"), (pair) -> row.setValue(colName, pair.getSecond()));
			
			popup.applyFilter(this.filter);
			popup._setValue(value);
			
			if (this.onEditWidgetCreate != null) {
				this.onEditWidgetCreate.accept(popup);
			}
			
			return popup;
		}
	}
	
	@SuppressWarnings("rawtypes")
	private static class WildcardColumn<T, W extends AbstractWidget & DataBindingComponent> extends Column<T, W> {
		Function<Row, AbstractWidget> editWidgetProvider;
		
		private WildcardColumn(Function<T, String> toDisplayText, Consumer<ValueChangeEvent<T>> onValueChanged, Consumer<W> onEditWidgetCreate, T defaultVal, Function<Row, AbstractWidget> editWidgetProvider, boolean editable, int size) {
			super(toDisplayText, onValueChanged, onEditWidgetCreate, defaultVal, editable, size);
			this.editWidgetProvider = editWidgetProvider;
		}
		
		@SuppressWarnings("unchecked")
		@Override
		public W createEditWidget(Screen owner, Font font, int x, int y, int height, int rowposition, Row row, String colName, T value) {
			W editWidget = (W)this.editWidgetProvider.apply(row);
			
			if (editWidget == null) {
				return null;
			}
			
			editWidget.setX(x);
			editWidget.setY(y);
			editWidget.setWidth(this.width - 3);
			editWidget.setHeight(height);
			editWidget._setValue(value);
			editWidget._setResponder((val) -> row.setValue(colName, val));
			
			if (editWidget instanceof PopupBox<?> popupBox) {
				popupBox._setResponder((pair) -> row.setValue(colName, pair.getFirst()));
			}
			
			if (this.onEditWidgetCreate != null) {
				this.onEditWidgetCreate.accept(editWidget);
			}
			
			return editWidget;
		}
	}
	
	public abstract static class ColumnBuilder<T, C extends Column<T, W>, W extends AbstractWidget> {
		protected final String name;
		protected Function<T, String> toDisplayText = ParseUtil::nullParam;
		protected Consumer<ValueChangeEvent<T>> onValueChanged = null;
		protected Consumer<W> onEditWidgetCreated = null;
		protected T defaultValue = null;
		protected boolean editable = true;
		protected int width = 100;
		
		protected ColumnBuilder(String name) {
			this.name = name;
		}
		
		public ColumnBuilder<T, C, W> toDisplayText(Function<T, String> toDisplayText) {
			this.toDisplayText = toDisplayText;
			return this;
		}
		
		public ColumnBuilder<T, C, W> editable(boolean editable) {
			this.editable = editable;
			return this;
		}
		
		public ColumnBuilder<T, C, W> valueChanged(Consumer<ValueChangeEvent<T>> onValueChanged) {
			this.onValueChanged = onValueChanged;
			return this;
		}
		
		public ColumnBuilder<T, C, W> defaultVal(T value) {
			this.defaultValue = value;
			return this;
		}
		
		public ColumnBuilder<T, C, W> editWidgetCreated(Consumer<W> onCreate) {
			this.onEditWidgetCreated = onCreate;
			return this;
		}
		
		public ColumnBuilder<T, C, W> width(int width) {
			this.width = width;
			return this;
		}
		
		protected abstract C create();
	}
	
	public static class EditBoxColumnBuilder extends ColumnBuilder<String, EditBoxColumn, EditBox> {
		protected EditBoxColumnBuilder(String name) {
			super(name);
		}
		
		@Override
		protected EditBoxColumn create() {
			return new EditBoxColumn(this.toDisplayText, this.onValueChanged, this.onEditWidgetCreated, this.defaultValue, this.editable, this.width);
		}
	}
	
	public static class ComboBoxColumnBuilder<T> extends ColumnBuilder<T, ComboColumn<T>, ComboBox<T>> {
		private final Collection<T> enums;
		
		protected ComboBoxColumnBuilder(String name, Collection<T> enums) {
			super(name);
			
			this.enums = enums;
			this.toDisplayText = ParseUtil::snakeToSpacedCamel;
		}
		
		@Override
		protected ComboColumn<T> create() {
			return new ComboColumn<> (this.toDisplayText, this.onValueChanged, this.onEditWidgetCreated, this.defaultValue, this.enums, this.editable, this.width);
		}
	}
	
	public static class RegistryPopupColumnBuilder<T> extends ColumnBuilder<T, RegistryPopupColumn<T>, RegistryPopupBox<T>> {
		final Registry<T> registry;
		Predicate<T> filter = (item) -> true;
		
		protected RegistryPopupColumnBuilder(String name, Registry<T> registry) {
			super(name);
			
			this.registry = registry;
		}
		
		public RegistryPopupColumnBuilder<T> filter(Predicate<T> filter) {
			this.filter = filter;
			return this;
		}
		
		@Override
		protected RegistryPopupColumn<T> create() {
			return new RegistryPopupColumn<>(this.toDisplayText, this.onValueChanged, this.onEditWidgetCreated, this.defaultValue, this.registry, this.filter, this.editable, this.width);
		}
	}
	
	public static class PopupColumnBuilder<T, P extends PopupBox<T>> extends ColumnBuilder<T, PopupColumn<T, P>, P> {
		final PopupBoxProvider<T, P> popupProvider;
		Predicate<T> filter = (item) -> true;
		
		
		protected PopupColumnBuilder(String name, PopupBoxProvider<T, P> popupProvider) {
			super(name);
			
			this.popupProvider = popupProvider;
		}
		
		public PopupColumnBuilder<T, P> filter(Predicate<T> filter) {
			this.filter = filter;
			return this;
		}
		
		@Override
		protected PopupColumn<T, P> create() {
			return new PopupColumn<>(this.toDisplayText, this.onValueChanged, this.onEditWidgetCreated, this.defaultValue, this.popupProvider, this.filter, this.onEditWidgetCreated, this.editable, this.width);
		}
	}
	
	@SuppressWarnings("rawtypes")
	public static class WildcardColumnBuilder<T, W extends AbstractWidget & DataBindingComponent> extends ColumnBuilder<T, WildcardColumn<T, W>, W> {
		Function<Row, AbstractWidget> editWidgetProvider;
		
		protected WildcardColumnBuilder(String name) {
			super(name);
		}
		
		public WildcardColumnBuilder<T, W> editWidgetProvider(Function<Row, AbstractWidget> editWidgetProvider) {
			this.editWidgetProvider = editWidgetProvider;
			return this;
		}
		
		@Override
		protected WildcardColumn<T, W> create() {
			return new WildcardColumn<> (this.toDisplayText, this.onValueChanged, this.onEditWidgetCreated, this.defaultValue, this.editWidgetProvider, this.editable, this.width);
		}
	}
	
	public static class PackImporter {
		List<Map<String, Object>> rows = Lists.newArrayList();
		
		public PackImporter newRow() {
			this.rows.add(Maps.newHashMap());
			return this;
		}
		
		public PackImporter newValue(String column, Object value) {
			this.rows.get(this.rows.size() - 1).put(column, value);
			return this;
		}
	}
	
	public static class ValueChangeEvent<T> {
		public final Grid grid;
		public final int rowposition;
		public final T prevValue;
		public final T postValue;
		
		private ValueChangeEvent(Grid grid, int rowposition, T prevValue, T postValue) {
			this.grid = grid;
			this.rowposition = rowposition;
			this.prevValue = prevValue;
			this.postValue = postValue;
		}
	}
	
	/*******************************************************************
	 * @ResizableComponent                                             *
	 *******************************************************************/
	private int xParam1;
	private int yParam1;
	private int xParam2;
	private int yParam2;
	private final HorizontalSizing horizontalSizingOption;
	private final VerticalSizing verticalSizingOption;
	
	@Override
	public void setX1(int x1) {
		this.xParam1 = x1;
	}

	@Override
	public void setX2(int x2) {
		this.xParam2 = x2;
	}

	@Override
	public void setY1(int y1) {
		this.yParam1 = y1;
	}

	@Override
	public void setY2(int y2) {
		this.yParam2 = y2;
	}
	
	@Override
	public Grid relocateX(ScreenRectangle screenrect, int x) {
		this.setX(x);
		this.relocateButtons();
		
		return this;
	}
	
	@Override
	public Grid relocateY(ScreenRectangle screenrect, int y) {
		this.setY(y);
		this.relocateButtons();
		
		return this;
	}
	
	@Override
	public void _setX(int x) {
		this.setX(x);
		this.relocateButtons();
	}
	
	@Override
	public void _setY(int y) {
		this.setY(y);
		this.relocateButtons();
	}
	
	@Override
	public void _setWidth(int width) {
		this.width = width;
		this.relocateButtons();
	}
	
	@Override
	public void _setHeight(int height) {
		this.height = height;
		this.relocateButtons();
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
	public int getX1() {
		return this.xParam1;
	}

	@Override
	public int getX2() {
		return this.xParam2;
	}

	@Override
	public int getY1() {
		return this.yParam1;
	}

	@Override
	public int getY2() {
		return this.yParam2;
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
		this.rowEditButtons.forEach((button) -> button._setActive(active));
		
		if (!this.active) {
			this.reset();
		}
	}
	
	@Override
	public void _setResponder(Consumer<Object> responder) {
	}
	
	@Override
	public Consumer<Object> _getResponder() {
		return null;
	}
	
	@Override
	public void _setValue(Object value) {
		this.reset();
		
		if (value instanceof PackImporter packImporter) {
			this.setValueChangeEnabled(false);
			
			for (int i = 0; i < packImporter.rows.size(); i++) {
				this.addRow();
				
				Map<String, Object> map = packImporter.rows.get(i);
				Row row = this.children().get(i);
				map.forEach(row::setValue);
			}
			
			this.setValueChangeEnabled(true);
		}
	}
	
	@Override
	public Object _getValue() {
		return null;
	}
	
	@Override
	public void _renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		this.render(guiGraphics, mouseX, mouseY, partialTicks);
	}
	
	@Override
	public void reset() {
		this.rowposition = -1;
		this.children().clear();
		this.setScrollAmount(0.0D);
		this.setSelected(null);
		this.editingColumn = null;
		this.editingWidget = null;
	}
	
	@Override
	public Component _getMessage() {
		return Component.literal(this.toString());
	}
	
	@Override
	public int _getWidth() {
		return this.getWidth();
	}

	@Override
	public int _getHeight() {
		return this.getHeight();
	}
}