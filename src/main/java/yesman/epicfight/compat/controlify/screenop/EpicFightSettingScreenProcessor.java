package yesman.epicfight.compat.controlify.screenop;

import dev.isxander.controlify.api.bind.InputBindingSupplier;
import dev.isxander.controlify.api.buttonguide.ButtonGuideApi;
import dev.isxander.controlify.api.buttonguide.ButtonGuidePredicate;
import dev.isxander.controlify.bindings.ControlifyBindings;
import dev.isxander.controlify.controller.ControllerEntity;
import dev.isxander.controlify.screenop.ScreenProcessor;
import yesman.epicfight.client.gui.screen.config.EpicFightSettingScreen;

public class EpicFightSettingScreenProcessor extends ScreenProcessor<EpicFightSettingScreen> {
    public EpicFightSettingScreenProcessor(EpicFightSettingScreen screen) {
        super(screen);
    }

    private static final InputBindingSupplier SAVE = ControlifyBindings.GUI_ABSTRACT_ACTION_1;
    private static final InputBindingSupplier DISCARD = ControlifyBindings.GUI_ABSTRACT_ACTION_2;

    public void handleButtons(ControllerEntity controller) {
        final boolean savePressed = SAVE.on(controller).guiPressed().get();
        final boolean discardPressed = DISCARD.on(controller).guiPressed().get();
        if (savePressed || discardPressed) {
            if (savePressed) {
                screen.getSaveButton().onPress();
            } else {
                screen.getDiscardButton().onPress();
            }
            playClackSound();
        }
        super.handleButtons(controller);
    }

    public void onWidgetRebuild() {
        super.onWidgetRebuild();

        ButtonGuideApi.addGuideToButton(
            this.screen.getSaveButton(),
            SAVE,
            ButtonGuidePredicate.always()
        );
        ButtonGuideApi.addGuideToButton(
            this.screen.getDiscardButton(),
            DISCARD,
            ButtonGuidePredicate.always()
        );
    }
}