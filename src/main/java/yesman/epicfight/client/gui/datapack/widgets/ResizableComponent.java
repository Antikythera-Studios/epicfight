package yesman.epicfight.client.gui.datapack.widgets;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;
import net.fabricmc.api.EnvType;

import yesman.epicfight.client.gui.widgets.common.AnchoredWidget;

/**
 * @Deprecated We're refactoring ui code use {@link AnchoredWidget} the advanced one
 */
@Deprecated
public interface ResizableComponent extends GuiEventListener, NarratableEntry {
	default void resize(ScreenRectangle screenRectangle) {
		if (this.getHorizontalSizingOption() != null) {
			this.getHorizontalSizingOption().resizeFunction.resize(this, screenRectangle, this.getX1(), this.getX2());
		}
		
		if (this.getVerticalSizingOption() != null) {
			this.getVerticalSizingOption().resizeFunction.resize(this, screenRectangle, this.getY1(), this.getY2());
		}
	}
	
	default ResizableComponent relocateX(ScreenRectangle screenrect, int screenX) {
		this._setX(screenX);
		
		if (this.getHorizontalSizingOption() == HorizontalSizing.WIDTH_RIGHT) {
			this.setX2(screenrect.right() - (screenX + this._getWidth()));
		} else {
			this.setX1(screenX);
		}
		
		return this;
	}
	
	default ResizableComponent relocateY(ScreenRectangle screenrect, int screenY) {
		this._setY(screenY);
		
		if (this.getVerticalSizingOption() == VerticalSizing.HEIGHT_BOTTOM) {
			this.setY2(screenrect.bottom() - (screenY + this._getHeight()));
		} else {
			this.setY1(screenY);
		}
		
		return this;
	}
	
	int getX1();
	int getX2();
	int getY1();
	int getY2();
	void setX1(int x1);
	void setX2(int x2);
	void setY1(int y1);
	void setY2(int y2);
	HorizontalSizing getHorizontalSizingOption();
	VerticalSizing getVerticalSizingOption();

	public static enum HorizontalSizing {
		LEFT_WIDTH((component, screenRectangle, v1, v2) -> {
			component._setX(screenRectangle.left() + v1);
			component._setWidth(v2);
		}), LEFT_RIGHT((component, screenRectangle, v1, v2) -> {
			int end = screenRectangle.right() - v2;
			int width = Math.max(end - (screenRectangle.left() + v1), 0);
			component._setX(screenRectangle.left() + v1);
			component._setWidth(width);
		}), WIDTH_RIGHT((component, screenRectangle, v1, v2) -> {
			int end = screenRectangle.right() - v2;
			int start = Math.max(end - v1, 0);
			component._setX(start);
			component._setWidth(v1);
		});
		
		ResizeFunction resizeFunction;
		
		HorizontalSizing(ResizeFunction resizeFunction) {
			this.resizeFunction = resizeFunction;
		}
	}

	public static enum VerticalSizing {
		TOP_HEIGHT((component, screenRectangle, v1, v2) -> {
			component._setY(v1);
			component._setHeight(v2);
		}), TOP_BOTTOM((component, screenRectangle, v1, v2) -> {
			int end = screenRectangle.bottom() - v2;
			int height = Math.max(end - v1, 0);
			component._setY(v1);
			component._setHeight(height);
		}), HEIGHT_BOTTOM((component, screenRectangle, v1, v2) -> {
			int end = screenRectangle.bottom() - v2;
			int start = Math.max(end - v1, 0);
			component._setY(start);
			component._setHeight(v1);
		});
		
		ResizeFunction resizeFunction;
		
		VerticalSizing(ResizeFunction resizeFunction) {
			this.resizeFunction = resizeFunction;
		}
	}

	@FunctionalInterface
	interface ResizeFunction {
		public void resize(ResizableComponent component, ScreenRectangle screenRectangle, int v1, int v2);
	}
	
	default AbstractWidget asWidget() {
		return (AbstractWidget)this;
	}
	
	/*****************************************
	 *        Vanilla Widget Functions       *
	 *****************************************/
	void _setActive(boolean active);
	void _renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks);
	int _getX();
	int _getY();
	int _getWidth();
	int _getHeight();
	void _setX(int x);
	void _setY(int y);
	void _setWidth(int width);
	void _setHeight(int height);
	Component _getMessage();
}