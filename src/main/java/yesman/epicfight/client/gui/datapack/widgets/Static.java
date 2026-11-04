package yesman.epicfight.client.gui.datapack.widgets;
import net.minecraft.client.Minecraft;

import javax.annotation.Nullable;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.fabricmc.api.EnvType;


/**
 * We're refactoring UI codes, use {@link yesman.epicfight.client.gui.widgets.Static} the advanced one
 */
@Deprecated
public class Static extends AbstractWidget implements ResizableComponent {
	private final Screen owner;
	private final Font font;
	private final Component tooltip;
	private int fontColor = 0xFFFFFFFF;
	
	public Static(Screen owner, int x1, int x2, int y1, int y2, HorizontalSizing horizontal, VerticalSizing vertical, String translateKey) {
		this(owner, x1, x2, y1, y2, horizontal, vertical, Component.translatable(translateKey), Component.translatable(translateKey + ".tooltip"));
	}
	
	public Static(Screen owner, int x1, int x2, int y1, int y2, HorizontalSizing horizontal, VerticalSizing vertical, Component message) {
		this(owner, x1, x2, y1, y2, horizontal, vertical, message, null);
	}
	
	public Static(Screen owner, int x1, int x2, int y1, int y2, HorizontalSizing horizontal, VerticalSizing vertical, Component message, @Nullable Component tooltip) {
		super(x1, y1, x2, y2, message);
		
		this.owner = owner;
		this.font = Minecraft.getInstance().font;
		this.x1 = x1;
		this.x2 = x2;
		this.y1 = y1;
		this.y2 = y2;
		this.horizontalSizingOption = horizontal;
		this.verticalSizingOption = vertical;
		this.tooltip = tooltip;
		
		this.setTooltip(this.tooltip == null ? null : Tooltip.create(this.tooltip));
	}
	
	@Override
	public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		String correctedString = this._getMessage() == null ? "" : this.font.plainSubstrByWidth(this._getMessage().getString(), this._getWidth());
		guiGraphics.drawString(this.font, correctedString, this._getX(), this._getY() + this.height / 2 - this.font.lineHeight / 2, this.fontColor, false);
	}
	
	@Override
	protected void updateWidgetNarration(NarrationElementOutput p_259858_) {
	}
	
	@Override
	public boolean isMouseOver(double mouseX, double mouseY) {
		return mouseX >= (double) this.getX() && mouseY >= (double) this.getY() && mouseX < (double) (this.getX() + this.font.width(this._getMessage())) && mouseY < (double) (this.getY() + this.height);
	}
	
	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int action) {
		return false;
	}
	
	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int action) {
		return false;
	}
	
	public boolean mouseDragged(double mouseX, double mouseY, int action, double p_93648_, double p_93649_) {
		return false;
	}
	
	public void setColor(int r, int g, int b) {
		this.fontColor = 0xFF000000 | r << 24 | g << 16 | b << 8;
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
	public void _setX(int x) {
		this.setX(x);
	}

	@Override
	public void _setY(int y) {
		this.setY(y);
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
		this.render(guiGraphics, mouseX, mouseY, partialTicks);
	}
}