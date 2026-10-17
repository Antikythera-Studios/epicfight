package yesman.epicfight.client.gui.datapack.screen;
import net.minecraft.client.Minecraft;

import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import io.netty.util.internal.StringUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import yesman.epicfight.client.gui.datapack.widgets.DataBindingComponent;

public class MessageScreen<T> extends Screen {
	protected final Button.OnPress onOkPress;
	protected final Button.OnPress onCancelPress;
	protected final Screen parentScreen;
	protected final Component message;
	protected final Consumer<T> onOkPressWithInput;
	protected final DataBindingComponent<T, ?> inputWidget;
	protected int messageBoxWidth;
	protected int messageBoxHeight;
	protected Component okButtomTitle;
	protected Component cancelButtomTitle;
	
	protected int layerFarPlane = 100;
	
	public MessageScreen(String title, String message, Screen parentScreen, Button.OnPress onOkPres, int width, int height) {
		this(title, Component.literal(message), parentScreen, onOkPres, null, width, height);
	}
	
	public MessageScreen(String title, Component message, Screen parentScreen, Button.OnPress onOkPres, int width, int height) {
		this(title, message, parentScreen, onOkPres, null, width, height);
	}
	
	public MessageScreen(String title, String message, Screen parentScreen, Button.OnPress onOkPress, @Nullable Button.OnPress onCancelPress, int width, int height) {
		this(title, Component.literal(message), parentScreen, onOkPress, onCancelPress, width, height);
	}
	
	public MessageScreen(String title, Component message, Screen parentScreen, Button.OnPress onOkPress, @Nullable Button.OnPress onCancelPress, int width, int height) {
		super(Component.literal(title));
		
		this.onOkPress = onOkPress;
		this.onCancelPress = onCancelPress;
		this.parentScreen = parentScreen;
		this.message = message;
		this.messageBoxWidth = width;
		this.messageBoxHeight = height;
		this.onOkPressWithInput = null;
		this.inputWidget = null;
		this.minecraft = Minecraft.getInstance();
	}
	
	public MessageScreen(String title, String message, Screen parentScreen, Consumer<T> onOkPressWithInput, @Nullable Button.OnPress onCancelPress, DataBindingComponent<T, ?> inputWidget, int width, int height) {
		super(Component.literal(title));
		
		this.onOkPress = null;
		this.onCancelPress = onCancelPress;
		this.parentScreen = parentScreen;
		this.message = Component.literal(message);
		this.messageBoxWidth = width;
		this.messageBoxHeight = height;
		this.onOkPressWithInput = onOkPressWithInput;
		this.inputWidget = inputWidget;
		this.minecraft = Minecraft.getInstance();
	}
	
	public MessageScreen<T> autoCalculateHeight() {
		int titleLength = this.minecraft.font.split(this.title, this.messageBoxWidth - 16).size();
		int messageLength = this.minecraft.font.split(this.message, this.messageBoxWidth - 16).size();
		this.messageBoxHeight = 40 + messageLength * 15 + titleLength * 15;
		
		return this;
	}
	
	public MessageScreen<T> withOkTitle(Component okButtomTitle) {
		this.okButtomTitle = okButtomTitle;
		return this;
	}
	
	public MessageScreen<T> withCancelTitle(Component cancelButtomTitle) {
		this.cancelButtomTitle = cancelButtomTitle;
		return this;
	}
	
	@Override
	protected void init() {
		this.parentScreen.init(this.minecraft, this.width, this.height);
		int height = this.messageBoxHeight / 2;
		
		int stringWidthOk = this.minecraft.font.width(this.okButtomTitle != null ? this.okButtomTitle : CommonComponents.GUI_OK) + 10;
		int stringWidthCancel = this.minecraft.font.width(this.cancelButtomTitle != null ? this.cancelButtomTitle : CommonComponents.GUI_CANCEL) + 10;
		int buttonWidht = Math.max(Math.max(stringWidthOk, stringWidthCancel), 55);
		
		if (this.onOkPress != null || this.onOkPressWithInput != null) {
			this.addRenderableWidget(Button.builder(this.okButtomTitle != null ? this.okButtomTitle : CommonComponents.GUI_OK, (button) -> {
				if (this.onOkPress != null) {
					this.onOkPress.onPress(button);
				} else {
					this.onOkPressWithInput.accept(this.inputWidget._getValue());
				}
			}).bounds(this.width / 2 - (buttonWidht + 1), this.height / 2 + height - 20, buttonWidht + (this.onCancelPress == null ? (buttonWidht + 2) : 0), 16).build());
		}
		
		if (this.onCancelPress != null) {
			this.addRenderableWidget(Button.builder(this.cancelButtomTitle != null ? this.cancelButtomTitle : CommonComponents.GUI_CANCEL, this.onCancelPress).bounds(this.width / 2 + 1, this.height / 2 + height - 20, buttonWidht, 16).build());
		}
		
		if (this.inputWidget != null) {
			this.addRenderableWidget((AbstractWidget)this.inputWidget);
		}
	}
	
	@Override
	public void tick() {
		this.parentScreen.tick();
	}
	
	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		this.parentScreen.render(guiGraphics, mouseX, mouseY, partialTick);
		
		guiGraphics.pose().pushPose();
		guiGraphics.pose().translate(0, 0, this.layerFarPlane);
		guiGraphics.fillGradient(0, 0, this.width, this.height, -1072689136, -804253680);
		
		int width = this.messageBoxWidth / 2;
		int height = this.messageBoxHeight / 2;
		
		List<FormattedCharSequence> messageLines = this.minecraft.font.split(this.message, width * 2 - 16);
		int messageWidth = this.minecraft.font.width(this.title);
		
		guiGraphics.fill(this.width / 2 - width, this.height / 2 - height, this.width / 2 + width, this.height / 2 + height, -6250336);
		guiGraphics.fill(this.width / 2 - width + 1, this.height / 2 - height + 1, this.width / 2 + width - 1, this.height / 2 + height - 1, -16777215);
		
		int y = this.height / 2 - height + 10;
		
		if (!StringUtil.isNullOrEmpty(this.title.getString())) {
			guiGraphics.drawString(this.font, this.title, this.width / 2 - messageWidth / 2, y, 16777215);
			y += 20;
		}
		
		for (FormattedCharSequence charSequence : messageLines) {
			guiGraphics.drawString(this.font, charSequence, this.width / 2 - width + 8, y, 16777215);
			y += 15;
		}
		
		if (this.inputWidget != null) {
			this.inputWidget._setWidth((width - 20) * 2);
			this.inputWidget._setX(this.width / 2 - width + 20);
			this.inputWidget._setY(y + 8);
		}
		
		for (Renderable renderable : this.renderables) {
            renderable.render(guiGraphics, mouseX, mouseY, partialTick);
        }
		
		guiGraphics.pose().popPose();
	}
	
	public MessageScreen<T> setLayerFarPlane(int i) {
		this.layerFarPlane = i;
		return this;
	}
}
