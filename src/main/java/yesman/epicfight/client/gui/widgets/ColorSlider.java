package yesman.epicfight.client.gui.widgets;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import yesman.epicfight.api.utils.ColorUtil;
import yesman.epicfight.client.gui.datapack.widgets.ResizableComponent;
import yesman.epicfight.client.renderer.EpicFightRenderTypes;

import java.util.function.BiConsumer;

/// We're refactoring UI codes, use [ColorDeterminator] the advanced one
@Deprecated
public class ColorSlider extends AbstractSliderButton implements ResizableComponent {
	public static final int[] RGB_COMBINATIONS = { 0xFFFF0000, 0xFFFFFF00, 0xFF00FF00, 0xFF00FFFF, 0xFF0000FF, 0xFFFF00FF, 0xFFFF0000 };
	
	private final BiConsumer<Double, Integer> valueChangeCallback;
	private final int[] colors;
	private final Font font;
	private final Style style;
	
	public ColorSlider(Font font, int x, int y, int width, int height, Component message, ColorSlider.Style style, double initColor, BiConsumer<Double, Integer> valueChangeCallback) {
		this(font, x, y, width, height, message, style, initColor, valueChangeCallback, RGB_COMBINATIONS);
	}
	
	public ColorSlider(Font font, int x, int y, int width, int height, Component message, ColorSlider.Style style, double initColor, BiConsumer<Double, Integer> valueChangeCallback, int... colors) {
		super(x, y, width, height, message, initColor);
		
		this.valueChangeCallback = valueChangeCallback;
		this.font = font;
		this.colors = colors;
		this.style = style;
		this.horizontalSizingOption = null;
		this.verticalSizingOption = null;
	}
	
	public ColorSlider(Font font, int x1, int x2, int y1, int y2, HorizontalSizing horizontalSizing, VerticalSizing verticalSizing, Component message, ColorSlider.Style style, double initColor, BiConsumer<Double, Integer> valueChangeCallback) {
		this(font, x1, x2, y1, y2, horizontalSizing, verticalSizing, message, style, initColor, valueChangeCallback, RGB_COMBINATIONS);
	}
	
	public ColorSlider(Font font, int x1, int x2, int y1, int y2, HorizontalSizing horizontalSizing, VerticalSizing verticalSizing, Component message, ColorSlider.Style style, double initColor, BiConsumer<Double, Integer> valueChangeCallback, int... colors) {
		super(x1, y1, x2, y2, message, initColor);
		
		this.x1 = x1;
		this.x2 = x2;
		this.y1 = y1;
		this.y2 = y2;
		this.valueChangeCallback = valueChangeCallback;
		this.font = font;
		this.colors = colors;
		this.style = style;
		this.horizontalSizingOption = horizontalSizing;
		this.verticalSizingOption = verticalSizing;
	}
	
	@Override
	public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		this.style.backgroundRenderer.render(this, guiGraphics);
		this.style.selectorRenderer.render(this, guiGraphics);
		this.style.titleRenderer.render(this, guiGraphics, this.font);
	}
	
	public void changeColor(int color, int index) {
		this.colors[index] = color;
	}
	
	@Override
	protected void applyValue() {
		this.valueChangeCallback.accept(this.value, this.getColor());
	}
	
	@Override
	protected void updateMessage() {
	}
	
	public double getPosition() {
		return this.value;
	}
	
	public int getColor() {
		return sliderPositionToColor(this.value, this.colors);
	}
	
	public static int sliderPositionToColor(double value, int[] colors) {
		int packedColor = 0;
		int colorBlocks = colors.length - 1;
		
		for (int i = 0; i < colorBlocks; i++) {
			double min = 1.0D / colorBlocks * i;
			double max = 1.0D / colorBlocks * (i + 1);
			
			if (value >= min && value <= max) {
				double lerpFactor = (value - min) / (max - min);
				int startColor = colors[i];
				int endColor = colors[i + 1];
				packedColor = ColorUtil.mixPackedARGB(lerpFactor, startColor, endColor);
			}
		}
		
		return packedColor;
	}
	
	public static int rgbColor(double position) {
		return sliderPositionToColor(position, RGB_COMBINATIONS);
	}
	
	private static void fillGradient(GuiGraphics guiGraphics, int x1, int y1, int x2, int y2, int startColor, int endColor) {
		float f = (float) FastColor.ARGB32.alpha(startColor) / 255.0F;
		float f1 = (float) FastColor.ARGB32.red(startColor) / 255.0F;
		float f2 = (float) FastColor.ARGB32.green(startColor) / 255.0F;
		float f3 = (float) FastColor.ARGB32.blue(startColor) / 255.0F;
		float f4 = (float) FastColor.ARGB32.alpha(endColor) / 255.0F;
		float f5 = (float) FastColor.ARGB32.red(endColor) / 255.0F;
		float f6 = (float) FastColor.ARGB32.green(endColor) / 255.0F;
		float f7 = (float) FastColor.ARGB32.blue(endColor) / 255.0F;
		
		Matrix4f matrix4f = guiGraphics.pose().last().pose();
		VertexConsumer consumer = guiGraphics.bufferSource().getBuffer(RenderType.gui());
		
		consumer.addVertex(matrix4f, (float)x1, (float)y2, 0.0F).setColor(f1, f2, f3, f);
		consumer.addVertex(matrix4f, (float)x2, (float)y2, 0.0F).setColor(f5, f6, f7, f4);
		consumer.addVertex(matrix4f, (float)x2, (float)y1, 0.0F).setColor(f5, f6, f7, f4);
		consumer.addVertex(matrix4f, (float)x1, (float)y1, 0.0F).setColor(f1, f2, f3, f);
	}
	
	private static void fillRhombus(GuiGraphics guiGraphics, int minX, int minY, int maxX, int maxY, int pColor) {
		Matrix4f matrix4f = guiGraphics.pose().last().pose();
		
		float a = (float)FastColor.ARGB32.alpha(pColor) / 255.0F;
		float r = (float)FastColor.ARGB32.red(pColor) / 255.0F;
		float g = (float)FastColor.ARGB32.green(pColor) / 255.0F;
		float b = (float)FastColor.ARGB32.blue(pColor) / 255.0F;
		int centerX = minX + (maxX - minX) / 2;
		int centerY = minY + (maxY - minY) / 2;
		
		VertexConsumer vertexconsumer = guiGraphics.bufferSource().getBuffer(EpicFightRenderTypes.guiTriangle());
		vertexconsumer.addVertex(matrix4f, (float) centerX, (float) minY, (float) 0).setColor(r, g, b, a);
		vertexconsumer.addVertex(matrix4f, (float) minX, (float) centerY, (float) 0).setColor(r, g, b, a);
		vertexconsumer.addVertex(matrix4f, (float) centerX, (float) maxY, (float) 0).setColor(r, g, b, a);
		vertexconsumer.addVertex(matrix4f, (float) centerX, (float) maxY, (float) 0).setColor(r, g, b, a);
		vertexconsumer.addVertex(matrix4f, (float) maxX, (float) centerY, (float) 0).setColor(r, g, b, a);
		vertexconsumer.addVertex(matrix4f, (float) centerX, (float) minY, (float) 0).setColor(r, g, b, a);
		
		guiGraphics.flush();
	}

	@FunctionalInterface
	interface Selector {
		public void render(ColorSlider widget, GuiGraphics guiGraphics);
	}

	@FunctionalInterface
	interface Background {
		public void render(ColorSlider widget, GuiGraphics guiGraphics);
	}

	@FunctionalInterface
	interface Title {
		public void render(ColorSlider widget, GuiGraphics guiGraphics, Font font);
	}

	public enum Style {
		CLASSIC(
			(ColorSlider widget, GuiGraphics guiGraphics) -> {
				int minX = widget.getX() + (int) (widget.value * (double) (widget.width - 8));
				guiGraphics.fill(minX, widget.getY(), minX + 8, widget.getY() + 20, 0xFFFFFFFF);
				guiGraphics.fill(minX + 1, widget.getY() + 1, minX + 7, widget.getY() + 19, sliderPositionToColor(widget.value, widget.colors));
			},
			(ColorSlider widget, GuiGraphics guiGraphics) -> {
				int y1 = widget.getY();
				int y2 = widget.getY() + widget.height;
				int blocks = widget.colors.length - 1;
				int prevEnd = widget.getX();
				
				for (int i = 0; i < widget.colors.length - 1; i++) {
					int nextX = widget.getX() + (widget.width * (i + 1) / blocks);
					fillGradient(guiGraphics, prevEnd, y1, nextX, y2, widget.colors[i], widget.colors[i+1]);
					prevEnd = nextX;
				}
			},
			(ColorSlider widget, GuiGraphics guiGraphics, Font font) -> {
				int j = 0xFFFFFFFF;
				guiGraphics.drawCenteredString(widget.font, widget.getMessage(), widget.getX() + widget.width / 2, widget.getY() + (widget.height - 8) / 2, j | Mth.ceil(widget.alpha * 255.0F) << 24);
			}
		),
		SIMPLE(
			(ColorSlider widget, GuiGraphics guiGraphics) -> {
				int outlineColor = widget.isHoveredOrFocused() ? 0xFFFFFFFF : 0xFF000000;
				int minX = widget.getX() + (int)(widget.value * (widget.width - 8));
				int centerY = widget.getY() + widget.getHeight() / 2;
				fillRhombus(guiGraphics, minX, centerY - 5, minX + 10, centerY + 5, outlineColor);
				fillRhombus(guiGraphics, minX + 1, centerY - 4, minX + 9, centerY + 4, sliderPositionToColor(widget.value, widget.colors));
			},
			(ColorSlider widget, GuiGraphics guiGraphics) -> {
				int y1 = widget.getY() + widget.height / 2 - 1;
				int y2 = widget.getY() + widget.height / 2 + 1;
				int blocks = widget.colors.length - 1;
				int prevEnd = widget.getX();
				
				guiGraphics.fill(widget.getX() - 1, y1 - 1, widget.getX() + widget.width + 1, y2 + 1, 0xFF000000);
				
				for (int i = 0; i < widget.colors.length - 1; i++) {
					int nextX = widget.getX() + (widget.width * (i + 1) / blocks);
					fillGradient(guiGraphics, prevEnd, y1, nextX, y2, widget.colors[i], widget.colors[i+1]);
					prevEnd = nextX;
				}
			},
			(ColorSlider widget, GuiGraphics guiGraphics, Font font) -> {
			}
		);
		
		Selector selectorRenderer;
		Background backgroundRenderer;
		Title titleRenderer;
		
		Style(Selector selectorRenderer, Background backgroundRenderer, Title titleRenderer) {
			this.selectorRenderer = selectorRenderer;
			this.backgroundRenderer = backgroundRenderer;
			this.titleRenderer = titleRenderer;
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