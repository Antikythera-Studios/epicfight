package yesman.epicfight.compat.controlify.screenop;

import dev.isxander.controlify.api.bind.InputBindingSupplier;
import dev.isxander.controlify.api.buttonguide.ButtonGuideApi;
import dev.isxander.controlify.api.buttonguide.ButtonGuidePredicate;
import dev.isxander.controlify.bindings.ControlifyBindings;
import dev.isxander.controlify.controller.ControllerEntity;
import dev.isxander.controlify.screenop.ScreenProcessor;
import yesman.epicfight.client.gui.screen.SkillBookScreen;

public class SkillBookScreenProcessor extends ScreenProcessor<SkillBookScreen> {
    public SkillBookScreenProcessor(SkillBookScreen screen) {
        super(screen);
    }

    private static final InputBindingSupplier LEARN_SKILL = ControlifyBindings.GUI_PRESS;

    public void handleButtons(ControllerEntity controller) {
        if (LEARN_SKILL.on(controller).guiPressed().get()) {
            screen.getLearnButton().onPress();
            playClackSound();
        }
        super.handleButtons(controller);
    }

    // The Skill Book screen has a single actionable button (the "learn skill" button).
    // Controller navigation and focus are disabled, and only the primary controller
    // button (e.g., X on DualSense) is used to trigger the action.

    protected void setInitialFocus() {
        // Intentionally empty. Do NOT call super.setInitialFocus().
    }

    protected void handleComponentNavigation(ControllerEntity controller) {
        // Intentionally empty. Do NOT call super.handleComponentNavigation().
    }

    public void onWidgetRebuild() {
        super.onWidgetRebuild();

        ButtonGuideApi.addGuideToButton(
                this.screen.getLearnButton(),
                LEARN_SKILL,
                ButtonGuidePredicate.always()
        );
    }
}
