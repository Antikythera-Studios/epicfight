package yesman.epicfight.compat.controlify.screenop;

import dev.isxander.controlify.api.bind.InputBindingSupplier;
import dev.isxander.controlify.bindings.ControlifyBindings;
import dev.isxander.controlify.controller.ControllerEntity;
import dev.isxander.controlify.screenop.ScreenProcessor;
import net.minecraft.client.InputType;
import net.minecraft.client.Minecraft;
import yesman.epicfight.client.gui.screen.SkillEditScreen;

public class SkillEditScreenProcessor extends ScreenProcessor<SkillEditScreen> {
    public SkillEditScreenProcessor(SkillEditScreen screen) {
        super(screen);
    }

    private static final InputBindingSupplier OPEN_SKILL_INFO = ControlifyBindings.GUI_ABSTRACT_ACTION_1;

    public void handleButtons(ControllerEntity controller) {
        super.handleButtons(controller);

        if (this.screen.getFocused() instanceof SkillEditScreen.EquipSkillButton equipSkillButton &&
                OPEN_SKILL_INFO.on(controller).guiPressed().get()) {
            equipSkillButton.openSkillInfoScreen();
        }
    }

    protected void setInitialFocus() {
        // Intentionally empty. Do NOT call super.setInitialFocus().
    }

    public void onWidgetRebuild() {
        super.onWidgetRebuild();
        setInputTypeWorkaround();
    }

    /// Controlify intentionally avoids setting Minecraft's input type to
    /// [InputType#KEYBOARD_ARROW] because keyboard and controller inputs behave
    /// differently.
    /// However, [SkillEditScreen] was built mainly for mouse users,
    /// and some GUI elements—like skill slot names—are shown only via tooltips, which
    /// appear only when the input type is [InputType#KEYBOARD_ARROW].
    /// <p>
    /// To support controller users without extra rework, the input type is set here
    /// manually.
    /// As a result, [#setInitialFocus()] must remain empty, since
    /// Minecraft handles focus automatically whenever the input type is not
    /// [InputType#NONE], which is what Controlify normally uses.
    private void setInputTypeWorkaround() {
        Minecraft.getInstance().setLastInputType(InputType.KEYBOARD_ARROW);
    }
}
