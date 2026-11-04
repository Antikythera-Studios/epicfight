package yesman.epicfight.client.gui.widgets;
import net.minecraft.client.Minecraft;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;
import yesman.epicfight.client.gui.widgets.common.AnchoredWidget;
import yesman.epicfight.client.gui.widgets.common.DataBoundWidget;
import yesman.epicfight.client.gui.widgets.common.WarningMarkableWidget;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;

/// A layout class for multiple widgets. Each widget must inherit [AbstractWidget] and [AnchoredWidget]
public class WidgetTable extends ContainerObjectSelectionList<WidgetTable.WidgetEntry> implements AnchoredWidget {
    @Nullable
    private WidgetTable.WidgetEntry lastEntry;

    public WidgetTable(Screen parentScreen, int x1, int x2, int y1, int y2, AnchoredWidget.HorizontalAnchorType horizontalAnchor, VerticalAnchorType verticalAnchor, int itemHeight) {
        super(Minecraft.getInstance(), 0, 0, 0, itemHeight);

        this.x1 = x1;
        this.x2 = x2;
        this.y1 = y1;
        this.y2 = y2;
        this.horizontalAnchorType = horizontalAnchor;
        this.verticalAnchorType = verticalAnchor;
    }

    @Override
    protected void renderListBackground(GuiGraphics guiGraphics) {
    }

    @Override
    protected void renderListSeparators(GuiGraphics guiGraphics) {
    }

    @Override
    public int getRowWidth() {
        return this.width;
    }

    @Override
    protected int getScrollbarPosition() {
        return this.getRight() - 6;
    }

    public int nextX(int spacing) {
        int xPos;

        if (this.lastEntry.widgets.isEmpty()) {
            xPos = 0;
        } else {
            AbstractWidget lastWidget = this.lastEntry.widgets.getLast();
            xPos = lastWidget.getX() + lastWidget.getWidth();
        }

        return xPos + spacing;
    }

    public WidgetTable newRow() {
        this.lastEntry = new WidgetEntry();
        this.addEntry(this.lastEntry);

        return this;
    }

    public <T extends AbstractWidget & AnchoredWidget> WidgetTable addWidget(T widget) {
        if (widget.getVerticalAnchorType() != VerticalAnchorType.TOP_HEIGHT) {
            throw new IllegalArgumentException("Only TOP_HEIGHT vertical anchor is allowed in WidgetTable");
        }

        Objects.requireNonNull(this.lastEntry);

        this.lastEntry.widgets.add(widget);
        this.lastEntry.anchored.add(widget);

        if (widget instanceof DataBoundWidget<?> dataBoundWidget) this.lastEntry.dataBound.add(dataBoundWidget);

        return this;
    }

    /// Initialize widget that inherits [DataBoundWidget]
    @SuppressWarnings("unchecked")
    public void initialize(boolean markNoValues) {
        for (WidgetEntry widgetEntry : this.children()) {
            for (DataBoundWidget<?> widget : widgetEntry.dataBound) {
                Callable<?> callable = widget.getDataProvider();

                try {
                    Object value = callable.call();
                    ((DataBoundWidget<Object>)widget).valueSetter().accept(value);
                } catch (Exception e) {
                    if (widget instanceof WarningMarkableWidget<?> warningMarkableWidget) warningMarkableWidget.mark();
                }
            }
        }
    }

    /// Resets widget that inherits [DataBoundWidget]
    /// See with the comment at [DataBoundWidget#reset]
    public void resetWidgets() {
        for (WidgetEntry widgetEntry : this.children()) {
            for (DataBoundWidget<?> dataBoundwidget : widgetEntry.dataBound()) {
                dataBoundwidget.reset();
            }
        }
    }

    /// Removes all widgets in table
    public void clearWidgets() {
        this.children().clear();
    }

    /// Returns the widget by index
    public AbstractWidget getComponent(int row, int column) {
        return this.children().get(row).widgets.get(column);
    }

    @Override
    public void relocate(ScreenRectangle screenRectangle) {
        AnchoredWidget.super.relocate(screenRectangle);

        ScreenRectangle tableRectangle = new ScreenRectangle(this.getX(), this.getY(), this.width, this.height);
        int widgetIndex = 0;

        for (WidgetEntry widgetEntry : this.children()) {
            for (AnchoredWidget widget : widgetEntry.anchored()) {
                int height = widget.getVerticalAnchorType().scaler().withSize(screenRectangle, widget);

                /// Locate the widget at the vertical center of the current row, as we don't allow anchoring
                /// rather than [VerticalAnchorType.TOP_HEIGHT] in [#addWidget], the
                /// operation is expected as same as [AbstractWidget#setY]
                int top = this.getRowTop(widgetIndex) + this.itemHeight / 2 - height / 2 - tableRectangle.top();
                widget.setY1(top);
                widget.relocate(tableRectangle);
            }

            widgetIndex++;
        }
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        this.updateScrollingState(x, y, button);

        if (!this.isMouseOver(x, y)) {
            return false;
        }

        // Checks the focusing entry first for the cases that widgets could be pressed out of the row bounds (e.g. combo box dropdown)
        if (this.getFocused() != null) {
            int index = this.children().indexOf(this.getFocused());
            int focusTop = this.getRowTop(index);
            int focusBottom = this.getRowBottom(index);

            // When the focused entry is in a visible area
            if (focusTop <= this.getBottom() && focusBottom >= this.getY()) {
                for (GuiEventListener e : this.getFocused().children()) {
                    if (e.mouseClicked(x, y, button)) {
                        if (this.getFocused() != null && this.getFocused() != e) {
                            this.getFocused().setFocused(false);
                            this.getFocused().setFocused(e);
                        }

                        return true;
                    }
                }
            }
        }

        return super.mouseClicked(x, y, button);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double xDelta, double yDelta) {
        // Checks the focusing entry first for the cases that widgets could be pressed out of the row bounds (e.g. combo box dropdown)
        if (this.getFocused() != null) {
            int index = this.children().indexOf(this.getFocused());
            int focusTop = this.getRowTop(index);
            int focusBottom = this.getRowBottom(index);

            // When the focused entry is in a visible area
            if (focusTop <= this.getBottom() && focusBottom >= this.getY()) {
                for (GuiEventListener e : this.getFocused().children()) {
                    if (e.mouseScrolled(x, y, xDelta, yDelta)) {
                        return true;
                    }
                }
            }
        }

        // Checks the entry in the mouse pointer location
        WidgetEntry e = this.getEntryAtPosition(x, y);

        if (e != null && e.mouseScrolled(x, y, xDelta, yDelta)) {
            return true;
        }

        return super.mouseScrolled(x, y, xDelta, yDelta);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        for (int i = 0; i < this.children().size(); i++) {
            WidgetEntry entry = this.children().get(i);
            int j1 = this.getRowTop(i);
            int k1 = this.getRowBottom(i);

            if (k1 >= this.getY() && j1 <= this.getBottom()) {
                if (entry.getChildAt(mouseX, mouseY).filter((component) -> component.mouseDragged(mouseX, mouseY, button, dx, dy)).isPresent()) {
                    return true;
                }
            }
        }

        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
    }

    public class WidgetEntry extends ContainerObjectSelectionList.Entry<WidgetTable.WidgetEntry> {
        final List<AbstractWidget> widgets = new ArrayList<> ();
        final List<AnchoredWidget> anchored = new ArrayList<> ();
        final List<DataBoundWidget<?>> dataBound = new ArrayList<> ();

        @Override
        public Optional<GuiEventListener> getChildAt(double x, double y) {
            for (GuiEventListener widget : this.widgets) {
                if (widget.isMouseOver(x, y)) {
                    return Optional.of(widget);
                }
            }

            return Optional.empty();
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return this.widgets;
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return this.widgets;
        }

        public List<? extends AnchoredWidget> anchored() {
            return this.anchored;
        }

        public List<? extends DataBoundWidget<?>> dataBound() {
            return this.dataBound;
        }

        @Override
        public void setFocused(@Nullable GuiEventListener listener) {
            if (this.getFocused() != listener) {
                if (this.getFocused() != null) this.getFocused().setFocused(false);
                if (listener != null) listener.setFocused(true);
            }

            ((yesman.epicfight.mixin.client.ContainerObjectSelectionListEntryAccessor) this).epicfight$setFocused(listener);
        }

        @Override
        public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovering, float partialTick) {
            for (AbstractWidget widget : this.widgets) {
                widget.setY(top + WidgetTable.this.itemHeight / 2 - widget.getHeight() / 2);
                widget.render(guiGraphics, mouseX, mouseY, partialTick);
            }
        }
    }

    /// *****************************************************************
    /// [AnchoredWidget] implementations                                *
    /// *****************************************************************
    private int x1;
    private int x2;
    private int y1;
    private int y2;
    private final AnchoredWidget.HorizontalAnchorType horizontalAnchorType;
    private final VerticalAnchorType verticalAnchorType;

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
    public void setX1(int i) {
        this.x1 = i;
    }

    @Override
    public void setX2(int i) {
        this.x2 = i;
    }

    @Override
    public void setY1(int i) {
        this.y1 = i;
    }

    @Override
    public void setY2(int i) {
        this.y2 = i;
    }

    @Override
    public HorizontalAnchorType getHorizontalAnchorType() {
        return this.horizontalAnchorType;
    }

    @Override
    public VerticalAnchorType getVerticalAnchorType() {
        return this.verticalAnchorType;
    }
}
