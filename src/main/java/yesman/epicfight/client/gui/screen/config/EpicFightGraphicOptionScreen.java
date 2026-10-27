package yesman.epicfight.client.gui.screen.config;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import yesman.epicfight.api.utils.ParseUtil;
import yesman.epicfight.client.ClientEngine;
import yesman.epicfight.client.camera.EpicFightTpsCameraDisableState;
import yesman.epicfight.client.camera.EpicFightTpsCameraDisabledReason;
import yesman.epicfight.client.gui.datapack.screen.MessageScreen;
import yesman.epicfight.client.gui.widgets.ColorSlider;
import yesman.epicfight.client.gui.widgets.EpicFightOptionList;
import yesman.epicfight.client.renderer.shader.compute.loader.ComputeShaderProvider;
import yesman.epicfight.config.ClientConfig;
import yesman.epicfight.generated.LangKeys;
import yesman.epicfight.main.EpicFightMod;

import java.io.File;
import java.io.IOException;

public class EpicFightGraphicOptionScreen extends EpicFightOptionSubScreen {
	private EpicFightOptionList optionsList;
	
	public EpicFightGraphicOptionScreen(Screen parentScreen) {
		super(parentScreen, Component.translatable(EpicFightMod.format("gui.%s.graphic_options")));
	}
	
	@Override
	protected void init() {
		super.init();
		
		this.optionsList = new EpicFightOptionList(this.minecraft, this.width, this.height, 32, this.height - 32, 25);
		int buttonHeight = -32;
		
		Button showTargetIndicatorButton =
			Button.builder(
				Component.translatable(EpicFightMod.format("gui.%s.target_indicator." + (ClientConfig.showTargetIndicator ? "on" : "off"))),
				button -> {
					ClientConfig.showTargetIndicator = !ClientConfig.showTargetIndicator;
					button.setMessage(Component.translatable(EpicFightMod.format("gui.%s.target_indicator." + (ClientConfig.showTargetIndicator ? "on" : "off"))));
				}
			)
			.pos(this.width / 2 + 5, this.height / 4 - 8)
			.size(160, 20)
			.tooltip(Tooltip.create(Component.translatable(EpicFightMod.format("gui.%s.target_indicator.tooltip"))))
			.build();
		
		Button healthBarVisibilityOptionButton =
			Button.builder(
				Component.translatable(EpicFightMod.format("gui.%s.health_bar_show_option." + ClientConfig.healthBarVisibility.getSerializedName())),
				button -> {
					ClientConfig.healthBarVisibility = ClientConfig.healthBarVisibility.nextEnum();
					button.setMessage(Component.translatable(EpicFightMod.format("gui.%s.health_bar_show_option." + ClientConfig.healthBarVisibility.getSerializedName())));
				}
			)
			.pos(this.width / 2 - 165, this.height / 4 - 8)
			.size(160, 20)
			.tooltip(Tooltip.create(Component.translatable(EpicFightMod.format("gui.%s.health_bar_show_option.tooltip"))))
			.build();
		
		this.optionsList.addSmall(showTargetIndicatorButton, healthBarVisibilityOptionButton);
		buttonHeight += 24;
		
		Button cameraSetupButton = Button.builder(Component.translatable(EpicFightMod.format("gui.%s.tps_setup")), (button) -> {
			if (Minecraft.getInstance().level == null || Minecraft.getInstance().player == null) {
				Minecraft.getInstance().setScreen(new MessageScreen<> ("Warning", "You can open camera setup screen only after entering the world", this, (button2) -> Minecraft.getInstance().setScreen(this), 300, 70).autoCalculateHeight());
			} else {
				Minecraft.getInstance().setScreen(new TPSSettingScreen(this));
			}
		}).pos(this.width / 2 + 5, this.height / 4 - 8).size(160, 20).tooltip(Tooltip.create(Component.translatable(EpicFightMod.format("gui.%s.tps_setup.tooltip")))).build();
		
		Button cameraTypeButton = Button.builder(Component.translatable(EpicFightMod.format("gui.%s.tps_perspective." + ParseUtil.toLowerCase(ClientConfig.getCameraMode().name()))), (button) -> {
			ClientConfig.cameraMode = ClientConfig.getCameraMode().nextEnum();
			button.setMessage(Component.translatable(EpicFightMod.format("gui.%s.tps_perspective." + ParseUtil.toLowerCase(ClientConfig.getCameraMode().name()))));
			cameraSetupButton.active = ClientConfig.getCameraMode().hasTPSTransition();
		}).pos(this.width / 2 - 165, this.height / 4 + buttonHeight).size(160, 20).tooltip(Tooltip.create(Component.translatable(EpicFightMod.format("gui.%s.tps_perspective.tooltip")))).build();
		
		cameraSetupButton.active = ClientConfig.getCameraMode().hasTPSTransition();
		this.optionsList.addSmall(cameraTypeButton, cameraSetupButton);
		
		buttonHeight += 24;
		
		Button bloodEffectsButton =
			Button.builder(Component.translatable(EpicFightMod.format("gui.%s.blood_effects." + (ClientConfig.bloodEffects ? "on" : "off"))),
				button -> {
					ClientConfig.bloodEffects = !ClientConfig.bloodEffects;
					button.setMessage(Component.translatable(EpicFightMod.format("gui.%s.blood_effects." + (ClientConfig.bloodEffects ? "on" : "off"))));
				}
			)
			.pos(this.width / 2 - 165, this.height / 4 - 8)
			.size(160, 20)
			.tooltip(Tooltip.create(Component.translatable(EpicFightMod.format("gui.%s.blood_effects.tooltip"))))
			.build();
		
		Button exportCustomArmors =
			Button.builder(Component.translatable(EpicFightMod.format("gui.%s.export_custom_armor")),
				button -> {
					File resourcePackDirectory = Minecraft.getInstance().getResourcePackDirectory().toFile();
					try {
						yesman.epicfight.api.client.model.rebake.ArmorRebaker.exportModels(resourcePackDirectory);
						Util.getPlatform().openFile(resourcePackDirectory);
					} catch (IOException e) {
						EpicFightMod.LOGGER.info("Failed to export custom armor models");
						e.printStackTrace();
					}
				}
			)
			.pos(this.width / 2 + 5, this.height / 4 + buttonHeight)
			.size(160, 20)
			.tooltip(Tooltip.create(Component.translatable(EpicFightMod.format("gui.%s.export_custom_armor.tooltip"))))
			.build();
		
		this.optionsList.addSmall(bloodEffectsButton, exportCustomArmors);
		buttonHeight += 24;
		
		Button enablePovAction =
			Button.builder(Component.translatable(EpicFightMod.format("gui.%s.enable_pov_action." + (ClientConfig.enablePovAction ? "on" : "off"))),
				button -> {
					ClientConfig.enablePovAction = !ClientConfig.enablePovAction;
					button.setMessage(Component.translatable(EpicFightMod.format("gui.%s.enable_pov_action." + (ClientConfig.enablePovAction ? "on" : "off"))));
				}
			)
			.pos(this.width / 2 - 165, this.height / 4 + buttonHeight)
			.size(160, 20)
			.tooltip(Tooltip.create(Component.translatable(EpicFightMod.format("gui.%s.enable_pov_action.tooltip"))))
			.build();
		
		Button uiSetupButton =
			Button.builder(Component.translatable(EpicFightMod.format("gui.%s.ui_setup")),
				button -> {
					this.minecraft.setScreen(new UISetupScreen(this));
				}
			)
			.pos(this.width / 2 + 5, this.height / 4 + buttonHeight)
			.size(160, 20)
			.tooltip(Tooltip.create(Component.translatable(EpicFightMod.format("gui.%s.ui_setup.tooltip"))))
			.build();
		
		this.optionsList.addSmall(enablePovAction, uiSetupButton);
		buttonHeight += 24;
		
		Button showEpicfightAttributesButton =
			Button.builder(Component.translatable(EpicFightMod.format("gui.%s.show_attributes." + (ClientConfig.showEpicFightAttributesInTooltip ? "on" : "off"))),
				button -> {
					ClientConfig.showEpicFightAttributesInTooltip = !ClientConfig.showEpicFightAttributesInTooltip;
					button.setMessage(Component.translatable(EpicFightMod.format("gui.%s.show_attributes." + (ClientConfig.showEpicFightAttributesInTooltip ? "on" : "off"))));
				}
			)
			.pos(this.width / 2 - 165, this.height / 4 + buttonHeight)
			.size(160, 20)
			.tooltip(Tooltip.create(Component.translatable(EpicFightMod.format("gui.%s.show_attributes.tooltip"))))
			.build();
		
		Button maxHitProjectilesButton =
			Button.builder(Component.translatable(EpicFightMod.format("gui.%s.max_stuck_projectiles"), String.valueOf(ClientConfig.maxStuckProjectiles)),
				button -> {
					ClientConfig.maxStuckProjectiles = (ClientConfig.maxStuckProjectiles + 1) % 30;
					button.setMessage(Component.translatable(EpicFightMod.format("gui.%s.max_stuck_projectiles"), String.valueOf(ClientConfig.maxStuckProjectiles)));
				}
			)
			.pos(this.width / 2 + 5, this.height / 4 + buttonHeight)
			.size(160, 20)
			.tooltip(Tooltip.create(Component.translatable(EpicFightMod.format("gui.%s.max_stuck_projectiles.tooltip"))))
			.build();
		
		this.optionsList.addSmall(showEpicfightAttributesButton, maxHitProjectilesButton);
		buttonHeight += 24;
		
		Button enableMineBlockGuideButton =
			Button.builder(Component.translatable(EpicFightMod.format("gui.%s.mine_block_guide." + ClientConfig.mineBlockGuideOption.getSerializedName())),
				button -> {
					ClientConfig.mineBlockGuideOption = ClientConfig.mineBlockGuideOption.nextEnum();
					button.setMessage(Component.translatable(EpicFightMod.format("gui.%s.mine_block_guide." + ClientConfig.mineBlockGuideOption.getSerializedName())));
				}
			)
			.pos(this.width / 2 - 165, this.height / 4 + buttonHeight)
			.size(160, 20)
			.tooltip(Tooltip.create(Component.translatable(EpicFightMod.format("gui.%s.mine_block_guide.tooltip"))))
			.build();
		
		Button enableTargetEntityGuide =
			Button.builder(Component.translatable(EpicFightMod.format("gui.%s.enable_target_entity_guide." + (ClientConfig.enableTargetEntityGuide ? "on" : "off"))),
				button -> {
					ClientConfig.enableTargetEntityGuide = !ClientConfig.enableTargetEntityGuide;
					button.setMessage(Component.translatable(EpicFightMod.format("gui.%s.enable_target_entity_guide." + (ClientConfig.enableTargetEntityGuide ? "on" : "off"))));
				}
			)
			.pos(this.width / 2 + 5, this.height / 4 + buttonHeight)
			.size(160, 20)
			.tooltip(Tooltip.create(Component.translatable(EpicFightMod.format("gui.%s.enable_target_entity_guide.tooltip"))))
			.build();
		
		this.optionsList.addSmall(enableMineBlockGuideButton, enableTargetEntityGuide);
		buttonHeight += 24;
		
		Button firstPersonModelButton =
			Button.builder(Component.translatable(EpicFightMod.format("gui.%s.first_person_model." + (ClientConfig.enableAnimatedFirstPersonModel ? "on" : "off"))),
				button -> {
					ClientConfig.enableAnimatedFirstPersonModel = !ClientConfig.enableAnimatedFirstPersonModel;
					button.setMessage(Component.translatable(EpicFightMod.format("gui.%s.first_person_model." + (ClientConfig.enableAnimatedFirstPersonModel ? "on" : "off"))));
				}
			)
			.pos(this.width / 2 - 165, this.height / 4 + buttonHeight)
			.size(160, 20)
			.tooltip(Tooltip.create(Component.translatable(EpicFightMod.format("gui.%s.first_person_model.tooltip"))))
			.build();
		
		Button enablePlayerVanillaModelButton =
			Button.builder(Component.translatable(EpicFightMod.format("gui.%s.enable_player_vanilla_model." + (ClientConfig.enableOriginalModel ? "on" : "off"))),
				button -> {
					ClientConfig.enableOriginalModel = !ClientConfig.enableOriginalModel;
					button.setMessage(Component.translatable(EpicFightMod.format("gui.%s.enable_player_vanilla_model." + (ClientConfig.enableOriginalModel ? "on" : "off"))));
				}
			)
			.pos(this.width / 2 + 5, this.height / 4 + buttonHeight)
			.size(160, 20)
			.tooltip(Tooltip.create(Component.translatable(EpicFightMod.format("gui.%s.enable_player_vanilla_model.tooltip"))))
			.build();
		
		this.optionsList.addSmall(firstPersonModelButton, enablePlayerVanillaModelButton);
		buttonHeight += 24;
		
		Button enableCosmetics =
			Button.builder(Component.translatable(EpicFightMod.format("gui.%s.enable_cosmetics." + (ClientConfig.enableCosmetics ? "on" : "off"))),
				button -> {
					ClientConfig.enableCosmetics = !ClientConfig.enableCosmetics;
					button.setMessage(Component.translatable(EpicFightMod.format("gui.%s.enable_cosmetics." + (ClientConfig.enableCosmetics ? "on" : "off"))));
				}
			)
			.pos(this.width / 2 - 165, this.height / 4 + buttonHeight)
			.size(160, 20)
			.tooltip(Tooltip.create(Component.translatable(EpicFightMod.format("gui.%s.enable_cosmetics.tooltip"))))
			.build();
		
		Button useComputeShaderButton =
			Button.builder(Component.translatable(EpicFightMod.format("gui.%s.use_compute_shader." + (ClientConfig.activateComputeShader ? "on" : "off"))),
				button -> {
					ClientConfig.activateComputeShader = !ClientConfig.activateComputeShader;
					button.setMessage(Component.translatable(EpicFightMod.format("gui.%s.use_compute_shader." + (ClientConfig.activateComputeShader ? "on" : "off"))));
				}
			)
			.pos(this.width / 2 + 5, this.height / 4 + buttonHeight)
			.size(160, 20)
			.tooltip(Tooltip.create(Component.translatable(EpicFightMod.format("gui.%s.use_compute_shader.tooltip"))))
			.build();
		
		if (!ComputeShaderProvider.supportComputeShader()) {
			useComputeShaderButton.active = false;
			useComputeShaderButton.setTooltip(Tooltip.create(Component.translatable(EpicFightMod.format("gui.%s.use_compute_shader.locked.tooltip"))));
		}
		
		this.optionsList.addSmall(enableCosmetics, useComputeShaderButton);
		buttonHeight += 30;
		
		Button groundSlamsButton =
			Button.builder(Component.translatable(EpicFightMod.format("gui.%s.ground_slams." + (ClientConfig.groundSlams ? "on" : "off"))),
				button -> {
					ClientConfig.groundSlams = !ClientConfig.groundSlams;
					button.setMessage(Component.translatable(EpicFightMod.format("gui.%s.ground_slams." + (ClientConfig.groundSlams ? "on" : "off"))));
				}
			)
			.pos(this.width / 2 - 165, this.height / 4 + buttonHeight)
			.size(160, 20)
			.tooltip(Tooltip.create(Component.translatable(EpicFightMod.format("gui.%s.ground_slams.tooltip"))))
			.build();
		
		this.optionsList.addSmall(groundSlamsButton, null);
		buttonHeight += 30;
		
		this.optionsList.addBig(
			new ColorSlider(
				this.font,
				this.width / 2 - 150,
				this.height / 4 + buttonHeight,
				300,
				20,
				Component.translatable(EpicFightMod.format("gui.%s.target_outline_color")),
				ColorSlider.Style.CLASSIC,
				ClientConfig.targetOutlineColor,
				(position, color) -> ClientConfig.targetOutlineColor = position
			)
		);
		
		this.addWidget(this.optionsList);

        maybeDisableCameraButtons(cameraTypeButton, cameraSetupButton);
	}

    private void maybeDisableCameraButtons(Button cameraTypeButton, Button cameraSetupButton) {
        final EpicFightTpsCameraDisabledReason tpsDisabledReason = EpicFightTpsCameraDisableState.getReason();
        if (tpsDisabledReason != null) {
            cameraTypeButton.active = false;
            cameraSetupButton.active = false;
            final Tooltip disabledReasonTooltip = Tooltip.create(Component.translatable(LangKeys.GUI_TPS_PERSPECTIVE_DISABLED_DUE_TO_MOD_CONFLICT, tpsDisabledReason.getModName()));

            cameraTypeButton.setTooltip(disabledReasonTooltip);
            cameraSetupButton.setTooltip(disabledReasonTooltip);
        }
    }
	
	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		ClientEngine.getInstance().renderEngine.versionNotifier.render(guiGraphics, false);
		this.basicListRender(guiGraphics, this.optionsList, mouseX, mouseY, partialTicks);
	}
}
