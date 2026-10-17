package yesman.epicfight.client.gui.widgets;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.CommonInputs;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import yesman.epicfight.client.gui.screen.SideBarScreen;
import yesman.epicfight.client.gui.widgets.common.AnchoredWidget;
import yesman.epicfight.client.gui.widgets.common.PressableWidget;
import yesman.epicfight.EpicFight;

import java.util.List;
import java.util.function.Function;

public class SideNavigationBarOpener extends AbstractWidget implements AnchoredWidget, PressableWidget {
    private static final ResourceLocation ARROW_SPRITE = EpicFight.identifier("widget/arrow");
    private static final ResourceLocation ARROW_HIGHLIGHTED_SPRITE = EpicFight.identifier("widget/arrow_highlighted");

    private final Screen owner;
    private final Screen twoStepsParent;
    private final Function<Screen, List<SideBarScreen.SideNavigationBarEntry>> entriesProvider;

    public SideNavigationBarOpener(Screen owner, Screen twoStepsParent, Function<Screen, List<SideBarScreen.SideNavigationBarEntry>> entriesProvider) {
        super(0, 0, 0, 21, Component.empty());

        this.owner = owner;
        this.twoStepsParent = twoStepsParent;
        this.entriesProvider = entriesProvider;
        this.x1 = 0;
        this.x2 = 8;
        this.y1 = 0;
        this.y2 = 34;
        this.horizontalAnchorType = HorizontalAnchorType.LEFT_WIDTH;
        this.verticalAnchorType = VerticalAnchorType.CENTER_HEIGHT;
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.setWidth(this.isHoveredOrFocused() ? 16 : 8);
        int outlineColor = this.isHoveredOrFocused() ? -6250336 : -12566463;
        int minX = this.getX();
        int minY = this.getY();
        int maxX = this.getX() + this.getWidth();
        int maxY = this.getY() + this.getHeight();

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 1000);

        guiGraphics.fill(minX, minY, maxX, maxY, outlineColor);
        guiGraphics.fill(minX, minY + 1, maxX - 1, maxY - 1, 0xFF000000);

        ResourceLocation resourcelocation = this.isHoveredOrFocused() ? ARROW_HIGHLIGHTED_SPRITE : ARROW_SPRITE;
        guiGraphics.blitSprite(resourcelocation, this.getX() + this.getWidth() / 2 - 3, this.getY() + this.getHeight() / 2 - 3, 4, 6);

        guiGraphics.pose().popPose();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        this.defaultButtonNarrationText(narrationElementOutput);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.active && this.visible) {
            if (this.isValidClickButton(button)) {
                boolean flag = this.clicked(mouseX, mouseY);
                if (flag) {
                    this.playDownSound(Minecraft.getInstance().getSoundManager());
                    this.onPressed();
                    return true;
                }
            }

            return false;
        } else {
            return false;
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.active && this.visible) {
            if (CommonInputs.selected(keyCode)) {
                this.playDownSound(Minecraft.getInstance().getSoundManager());
                this.onPressed();
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    @Override
    public void onPressed() {
        Minecraft.getInstance().setScreen(new SideBarScreen(this.owner, this.entriesProvider.apply(this.twoStepsParent), 110));
    }

    /*******************************************************************
     * {@link AnchoredWidget} implementations                          *
     *******************************************************************/
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
