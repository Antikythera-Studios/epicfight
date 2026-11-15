package yesman.epicfight.client.gui.widgets;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.CommonInputs;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import yesman.epicfight.client.gui.widgets.common.AnchoredWidget;
import yesman.epicfight.client.gui.widgets.common.DataBoundWidget;
import yesman.epicfight.client.gui.widgets.common.PressableWidget;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;

public class ComboBox<T> extends AbstractWidget implements AnchoredWidget, DataBoundWidget<T>, PressableWidget {
    private final ComboItemList comboItemList;
    private final Font font;
    private final int maxRows;

    public ComboBox(
        Screen parent,
        Font font,
        int x1,
        int x2,
        int y1,
        int y2,
        AnchoredWidget.HorizontalAnchorType horizontalAnchor,
        AnchoredWidget.VerticalAnchorType verticalAnchor,
        Callable<T> dataProvider,
        Consumer<T> onWidgetChanged,
        int maxRows,
        Component title,
        Collection<T> items,
        Function<T, String> displayStringMapper
    ) {
        super(0, 0, 0, 0, title);

        this.font = font;
        this.maxRows = Math.min(maxRows, items.size());
        this.dataProvider = dataProvider;
        this.onWidgetChanged = onWidgetChanged;

        this.comboItemList = new ComboItemList(Minecraft.getInstance(), this.maxRows, 15);

        for (T item : items) {
            this.comboItemList.addEntry(item, displayStringMapper.apply(item));
        }

        this.comboItemList.visible = false;
        this.comboItemList.active = false;

        this.x1 = x1;
        this.x2 = x2;
        this.y1 = y1;
        this.y2 = y2;
        this.horizontalAnchorType = horizontalAnchor;
        this.verticalAnchorType = verticalAnchor;

        this.relocateComboList();
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        if (this.active && this.visible) {
            if (this.comboItemList.isActive() && this.comboItemList.mouseClicked(x, y, button)) {
                if (x < this.comboItemList.getScrollbarPosition() || x > this.comboItemList.getScrollbarPosition() + 6) {
                    this.comboItemList.active = false;
                    this.comboItemList.visible = false;
                }

                this.playDownSound(Minecraft.getInstance().getSoundManager());

                return true;
            } else {
                if (this.isValidClickButton(button)) {
                    boolean flag = this.clicked(x, y);

                    if (flag) {
                        if (this.arrowClicked(x, y)) {
                            this.onPressed();
                        } else {
                            this.closeSelectionListIfActive();
                        }

                        return true;
                    }
                }
            }

            return false;
        }

        return false;
    }

    @Override
    public boolean mouseScrolled(double x, double y, double xDelta, double yDelta) {
        if (this.comboItemList.isActive()) {
            return this.comboItemList.mouseScrolled(x, y, xDelta, yDelta);
        }

        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (this.comboItemList.isActive()) {
            return this.comboItemList.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        }

        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.active && this.visible) {
            if (CommonInputs.selected(keyCode)) {
                this.onPressed();
                return true;
            } else if (this.isSelectionListVisible()) {
                switch (keyCode) {
                    case 264 -> {
                        int i = Math.min(this.comboItemList.children().size() - 1, this.comboItemList.children().indexOf(this.comboItemList.getSelected()) + 1);
                        this.comboItemList.setSelected(this.comboItemList.children().get(i));
                        return true;
                    }
                    case 265 -> {
                        int i = Math.max(0, this.comboItemList.children().indexOf(this.comboItemList.getSelected()) - 1);
                        this.comboItemList.setSelected(this.comboItemList.children().get(i));
                        return true;
                    }
                }

                return false;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public boolean isSelectionListVisible() {
        return this.comboItemList.isActive() && this.comboItemList.visible;
    }

    public void setSelectionListVisible(boolean flag) {
        this.comboItemList.active = flag;
        this.comboItemList.visible = flag;
    }

    @Override
    protected boolean clicked(double x, double y) {
        return this.active && this.visible && x >= (double)this.getX() && y >= (double) this.getY() && x < (double) (this.getX() + this.width) && y < (double) (this.getY() + this.height);
    }

    @Override
    public boolean isMouseOver(double x, double y) {
        if (this.isSelectionListVisible() && this.comboItemList.isMouseOver(x, y)) {
            return true;
        }

        return this.active && this.visible && x >= (double)this.getX() && y >= (double) this.getY() && x < (double) (this.getX() + this.width) && y < (double) (this.getY() + this.height * (this.maxRows + 1));
    }

    @Override
    public void setWidth(int width) {
        this.width = width;

        int left = this.comboItemList.getX();
        this.comboItemList.setWidth(width);
        this.comboItemList.setX(left);
    }

    @Override
    public void relocate(ScreenRectangle screenRectangle) {
        AnchoredWidget.super.relocate(screenRectangle);
        this.relocateComboList();
    }

    private void relocateComboList() {
        int entryHeight = 15;
        int possibleTopPosition = this.getY() - (this.height * this.maxRows + 1);
        int possibleBottomPosition = this.getY() + this.height + this.height * this.maxRows + 1;
        int bottomSpace = Minecraft.getInstance().getWindow().getGuiScaledHeight() - possibleBottomPosition;

        if (bottomSpace < possibleTopPosition) {
            this.comboItemList.updateSizeAndPosition(this.width, entryHeight * this.maxRows, this.getY() - (entryHeight * this.maxRows + 1));
        } else {
            this.comboItemList.updateSizeAndPosition(this.width, entryHeight * this.maxRows, this.getY() + this.height + 1);
        }

        this.comboItemList.setX(this.getX());
    }

    private void openSelectionList() {
        this.playDownSound(Minecraft.getInstance().getSoundManager());
        this.setSelectionListVisible(!this.isSelectionListVisible());
    }

    private void closeSelectionListIfActive() {
        if (!this.isSelectionListVisible()) {
            return;
        }

        this.setSelectionListVisible(false);
        this.playDownSound(Minecraft.getInstance().getSoundManager());
    }

    @Override
    public void onPressed() {
        if (this.isSelectionListVisible()) {
            this.closeSelectionListIfActive();
        } else {
            this.openSelectionList();
        }
    }

    private boolean arrowClicked(double x, double y) {
        int openPressed = this.getX() + this.width - 14;
        return this.active && this.visible && x >= (double)openPressed && y >= (double) this.getY() && x < (double) (this.getX() + this.width) && y < (double) (this.getY() + this.height);
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        int outlineColor = this.isFocused() ? -1 : this.isActive() ? -6250336 : -12566463;

        guiGraphics.fill(this.getX() - 1, this.getY() - 1, this.getX() + this.width + 1, this.getY() + this.height + 1, outlineColor);
        guiGraphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, -16777216);

        String correctedString = this.font.plainSubstrByWidth(this.comboItemList.getSelected() == null ? "" : this.comboItemList.getSelected().displayName, this.width - 10);
        int fontColor = this.isActive() ? 16777215 : 4210752;

        guiGraphics.drawString(this.font, Component.literal(correctedString), this.getX() + 4, this.getY() + this.height / 2 - this.font.lineHeight / 2 + 1, fontColor, false);
        guiGraphics.drawString(this.font, Component.literal("▼"), this.getX() + this.width - 8, this.getY() + this.height / 2 - this.font.lineHeight / 2 + 1, fontColor, false);

        if (this.comboItemList.visible) {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0, 0, 10);
            this.comboItemList.render(guiGraphics, mouseX, mouseY, partialTicks);
            guiGraphics.pose().popPose();
        }
    }

    public void setValue(T value) {
        this.comboItemList.setSelected(value);
        this.valueChangeCallback().accept(value);
    }

    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
        this.comboItemList.setFocused(focused);

        if (!focused) {
            this.setSelectionListVisible(false);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementInput) {
        narrationElementInput.add(NarratedElementType.TITLE, this.createNarrationMessage());
    }

    class ComboItemList extends ObjectSelectionList<ComboItemList.ComboItemEntry> {
        private final Map<T, ComboItemEntry> entryMap = new HashMap<>();

        public ComboItemList(Minecraft minecraft, int maxRows, int itemHeight) {
            super(minecraft, ComboBox.this.width, itemHeight * maxRows, 0, itemHeight);
            this.setRenderHeader(false, 0);
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

        public void setSelected(@Nullable T item) {
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

        @Override
        public void setFocused(boolean focused) {
            super.setFocused(focused);

            if (!focused) {
                this.active = false;
                this.visible = false;
            }
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
                    setSelected(this);
                    ComboBox.this.setValue(this.item);
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
     * {@link AnchoredWidget} implementations                          *
     *******************************************************************/
    private int x1;
    private int x2;
    private int y1;
    private int y2;
    private final AnchoredWidget.HorizontalAnchorType horizontalAnchorType;
    private final AnchoredWidget.VerticalAnchorType verticalAnchorType;

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

    /*******************************************************************
     * {@link DataBoundWidget} implementations                         *
     *******************************************************************/
    private final Callable<T> dataProvider;
    private final Consumer<T> onWidgetChanged;

    @Override
    public Callable<T> getDataProvider() {
        return this.dataProvider;
    }

    @Override
    public Consumer<T> valueChangeCallback() {
        return this.onWidgetChanged;
    }

    @Override
    public Consumer<T> valueSetter() {
        return this::setValue;
    }

    @Override
    public void reset() {
        this.comboItemList.setSelected((T)null);
    }
}
