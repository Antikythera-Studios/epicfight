package yesman.epicfight.epicskins.client.screen;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import org.apache.commons.lang3.exception.ExceptionUtils;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.client.model.Meshes;
import yesman.epicfight.api.client.model.SoftBodyTranslatable;
import yesman.epicfight.api.client.physics.cloth.ClothColliderPresets;
import yesman.epicfight.api.client.physics.cloth.ClothSimulator;
import yesman.epicfight.api.utils.ParseUtil;
import yesman.epicfight.api.utils.math.Vec4f;
import yesman.epicfight.client.gui.datapack.screen.MessageScreen;
import yesman.epicfight.client.gui.datapack.widgets.*;
import yesman.epicfight.client.gui.datapack.widgets.ResizableComponent.HorizontalSizing;
import yesman.epicfight.client.gui.datapack.widgets.ResizableComponent.VerticalSizing;
import yesman.epicfight.client.gui.widgets.ColorSlider;
import yesman.epicfight.epicskins.animation.EpicSkinsAnimations;
import yesman.epicfight.epicskins.client.widget.CapePopupBox;
import yesman.epicfight.epicskins.exception.HttpResponseException;
import yesman.epicfight.epicskins.user.AuthenticationHelperImpl;
import yesman.epicfight.epicskins.user.AuthenticationHelperImpl.CapeProperties;
import yesman.epicfight.epicskins.user.Cosmetic;
import yesman.epicfight.gameasset.Armatures;
import yesman.epicfight.main.AuthenticationHelper;
import yesman.epicfight.EpicFight;

import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

public class AvatarEditScreen extends Screen {
	private static final List<AssetAccessor<? extends StaticAnimation>> IDLE_ANIMATIONS = List.of(EpicSkinsAnimations.BIPED_IDLE1, EpicSkinsAnimations.BIPED_IDLE2);

	private final Screen parentScreen;
	private final Queue<Runnable> deferredWorks = new ConcurrentLinkedQueue<> ();
	private LayoutElements<?> capeOptionsLayout;
	private LayoutElements<?> capeSelectionLayout;
	private LayoutElements<?> activatedLayout;

	private CapePopupBox capePopupBox;
	private CapeList capeList;
	private InputComponentList<JsonElement> capeOptions;
	private ModelPreviewer mainModelPreviewer;
	private ColorSlider hueSlider;
	private ColorSlider saturationSlider;
	private ColorSlider brightnessSlider;
	private CheckBox useVanillTextureCheckBox;
	private int nextPlaying;
	
	private Button saveButton;
	private Button quitButton;
	private Button signOutButton;
	private Button backButton;
	
	public AvatarEditScreen(Screen screen) {
		super(Component.translatable("gui.epicskins.skin_config"));
		
		this.parentScreen = screen;
		this.minecraft = screen == null ? Minecraft.getInstance() : Minecraft.getInstance();
		this.font = this.minecraft.font;

		this.saveButton = Button.builder(Component.translatable("gui.epicskins.button.save"), (button) -> {
			this.popupResponseAwaiting();
			
			CapeProperties epicskinsInfo = AuthenticationHelperImpl.getInstance().capeProperties();
			epicskinsInfo.setCape(this.capePopupBox._getValue() != null ? this.capePopupBox._getValue().seq() : -1);
			epicskinsInfo.setHue(this.hueSlider.getPosition());
			epicskinsInfo.setSaturation(this.saturationSlider.getPosition());
			epicskinsInfo.setBrightness(this.brightnessSlider.getPosition());
			epicskinsInfo.setVanillaTextureUse(this.useVanillTextureCheckBox._getValue());
			
			AuthenticationHelperImpl.getInstance().sendSaveRequest((ex) -> {
				if (ex instanceof HttpResponseException httpFailResponseException) {
					httpFailResponseException.printStackTrace();
					this.popupHttpResponseException(httpFailResponseException.getHttpStatusCode(), httpFailResponseException.getResponseBody());
				} else if (ex != null) {
					this.popupConnectionFail(ex);
				} else {
					this.popupSavedSuccess();
				}
			});
		}).size(80, 20).build();
		
		this.saveButton.active = AuthenticationHelperImpl.getInstance().status() == AuthenticationHelper.Status.AUTHENTICATED;
		
		this.quitButton = Button.builder(Component.translatable("gui.epicskins.button.quit"), (button) -> Minecraft.getInstance().setScreen(this.parentScreen)).size(80, 20).build();
		this.signOutButton = Button.builder(Component.translatable("gui.epicskins.button.delete_account"), (button) -> this.popupSignOutAsk()).size(90, 20).build();
		this.backButton = Button.builder(Component.translatable("gui.epicskins.button.back"), (button) -> {
			this.deferredWorks.add(() -> {
				this.capeOptionsLayout.accept(this::addRenderableWidget);
				this.capeSelectionLayout.accept(this::removeWidget);
				this.activatedLayout = this.capeOptionsLayout;
				
				Cosmetic cosmetic = this.capePopupBox._getValue();
				
				if (cosmetic != null) {
					cosmetic.getAsMesh((mesh) -> {
						this.mainModelPreviewer.setCloakColor(cosmetic.useIntParam1() ? this.brightnessSlider.getColor() : 0xFFFFFFFF);
						this.mainModelPreviewer.initCloakInfo(
							  (SoftBodyTranslatable)mesh
							, cosmetic.textureLocation()
							, ClothSimulator.ClothObjectBuilder.create()
								.parentJoint(Armatures.BIPED.get().torso)
								.putAll(ClothColliderPresets.BIPED)
						);
						this.capePopupBox._setValue(cosmetic);
						this.refreshOptionComponents(cosmetic);
					});
				} else {
					this.mainModelPreviewer.removeCloak();
				}
				
			});
		}).size(200, 20).build();
		
		this.mainModelPreviewer = new ModelPreviewer(165, 10, 15, 34, HorizontalSizing.WIDTH_RIGHT, VerticalSizing.TOP_BOTTOM, Armatures.BIPED, Meshes.BIPED);
		this.mainModelPreviewer.setBackgroundClearColor(new Vec4f(0.0F, 0.0F, 0.0F, 0.0F));
		
		this.mainModelPreviewer.enableZoomingCamera(false);
		this.mainModelPreviewer.enableCameraMove(false);
		this.mainModelPreviewer.setCameraTransform(-3.0D, 28.0F, 160.0F, 0.0F, 0.0F);
		this.mainModelPreviewer.setFigureTexture(AuthenticationHelperImpl.getInstance().playerInfo().getSkin().texture());
		
		if (PlayerSkin.Model.SLIM.equals(AuthenticationHelperImpl.getInstance().playerInfo().getSkin().model())) {
			this.mainModelPreviewer.setMesh(Meshes.ALEX);
		} else {
			this.mainModelPreviewer.setMesh(Meshes.BIPED);
		}
		
		this.mainModelPreviewer.addAnimationToPlay(EpicSkinsAnimations.BIPED_STANDING);
		
		RandomSource randomSource = RandomSource.create(System.currentTimeMillis());
		this.nextPlaying = 160 + randomSource.nextInt(120);
		
		this.capeOptions = new InputComponentList<> (this, 0, 0, 0, 30) {
			@Override
			public void importTag(JsonElement tag) {
			}
		};
		
		this.capePopupBox = new CapePopupBox(this, this.font, 0, 10, 60, 15, HorizontalSizing.LEFT_RIGHT, null, Component.translatable("gui.epicskins.avatar.cape_type"),
													(cosmetic) -> ParseUtil.nullOrToString(cosmetic, Cosmetic::title),
													() -> {
														this.deferredWorks.add(() -> {
															OUTER:
															for (CapeList.CosmeticsEntry e : this.capeList.children()) {
																for (GuiEventListener block : e.children()) {
																	CapeList.CapeBlock capeBlock = (CapeList.CapeBlock)block;
																	
																	if (capeBlock.cosmetic == this.capePopupBox._getValue()) {
																		this.capeList.setFocused(e);
																		
																		if (this.capeList.focused != null) {
																			this.capeList.focused.setFocused(false);
																		}
																		
																		this.capeList.focused = capeBlock;
																		capeBlock.setFocused(true);
																		
																		break OUTER;
																	}
																}
															}
															
															this.capeList.moveScrollToFocus();
															this.capeOptionsLayout.accept(this::removeWidget);
															this.capeSelectionLayout.accept(this::addRenderableWidget);
															this.activatedLayout = this.capeSelectionLayout;
														});
													}, null
												);
		
		this.capeOptions.newRow();
		this.capeOptions.addComponentCurrentRow(new Static(this, this.capeOptions.nextStart(8), 80, 60, 15, HorizontalSizing.LEFT_WIDTH, null, "gui.epicskins.avatar.cape_type"));
		this.capeOptions.addComponentCurrentRow(this.capePopupBox.relocateX(this.getRectangle(), this.capeOptions.nextStart(8)));
		
		this.hueSlider = new ColorSlider(this.font, this.capeOptions.nextStart(8), 10, 60, 15, HorizontalSizing.LEFT_RIGHT, null, Component.translatable("gui.epicskins.cape_color"), ColorSlider.Style.SIMPLE, 0.0D,
												(position, color) -> {
													this.saturationSlider.changeColor(color, 0);
													this.brightnessSlider.changeColor(this.saturationSlider.getColor(), 0);

													if ((!this.capePopupBox._getValue().useBoolParam1() || !this.useVanillTextureCheckBox._getValue()) && this.capePopupBox._getValue().useIntParam1()) {
														this.mainModelPreviewer.setCloakColor(this.brightnessSlider.getColor());
													}
												});

		this.saturationSlider = new ColorSlider(this.font, 0, 10, 60, 15, HorizontalSizing.LEFT_RIGHT, null, Component.translatable("gui.epicskins.cape_color"), ColorSlider.Style.SIMPLE, 0.0D,
												(position, color) -> {
													this.brightnessSlider.changeColor(color, 0);

													if ((!this.capePopupBox._getValue().useBoolParam1() || !this.useVanillTextureCheckBox._getValue()) && this.capePopupBox._getValue().useIntParam1()) {
														this.mainModelPreviewer.setCloakColor(this.brightnessSlider.getColor());
													}
												}, 0xFFFF0000, 0xFFFFFFFF);

		this.brightnessSlider = new ColorSlider(this.font, 0, 10, 60, 15, HorizontalSizing.LEFT_RIGHT, null, Component.translatable("gui.epicskins.cape_color"), ColorSlider.Style.SIMPLE, 0.0D,
												(position, color) -> {
													if ((!this.capePopupBox._getValue().useBoolParam1() || !this.useVanillTextureCheckBox._getValue()) && this.capePopupBox._getValue().useIntParam1()) {
														this.mainModelPreviewer.setCloakColor(color);
													}
												}, 0xFFFF0000, 0xFF000000);

		this.useVanillTextureCheckBox = new CheckBox(this.font, this.capeOptions.nextStart(8), 60, 0, 10, HorizontalSizing.LEFT_WIDTH, null, false, Component.literal(""), null);

		Consumer<Boolean> responder = (val) -> {
			if (this.capeList != null) {
				this.mainModelPreviewer.setCloakTexture(val ? AuthenticationHelperImpl.getInstance().playerInfo().getSkin().capeTexture() : this.capePopupBox._getValue().textureLocation());
				this.mainModelPreviewer.setCloakColor(val || !this.capePopupBox._getValue().useIntParam1() ? 0xFFFFFFFF : this.brightnessSlider.getColor());
			}
		};

		CapeProperties epicskinsInfo = AuthenticationHelperImpl.getInstance().capeProperties();
		Cosmetic cosmetic = AuthenticationHelperImpl.getInstance().getCosmetic(epicskinsInfo.capeSeq());

		if (cosmetic != null) {
			cosmetic.getAsMesh((mesh) -> {
				this.mainModelPreviewer.setCloakColor(cosmetic.useIntParam1() && !epicskinsInfo.useVanillaTexture() ? this.brightnessSlider.getColor() : 0xFFFFFFFF);
				this.mainModelPreviewer.initCloakInfo(
					  (SoftBodyTranslatable)mesh
					, cosmetic.useBoolParam1() && epicskinsInfo.useVanillaTexture()
						? AuthenticationHelperImpl.getInstance().playerInfo().getSkin().capeTexture() : cosmetic.textureLocation()
					, ClothSimulator.ClothObjectBuilder.create()
						.parentJoint(Armatures.BIPED.get().torso)
						.putAll(ClothColliderPresets.BIPED)
				);
				this.refreshOptionComponents(cosmetic);
			});

			this.capePopupBox._setValue(cosmetic);
			this.useVanillTextureCheckBox._setResponder(responder);
			this.useVanillTextureCheckBox._setValue(epicskinsInfo.useVanillaTexture());
			this.hueSlider.setValue(epicskinsInfo.hue());
			this.saturationSlider.setValue(epicskinsInfo.saturation());
			this.brightnessSlider.setValue(epicskinsInfo.brightness());
		} else {
			this.useVanillTextureCheckBox._setValue(false);
			this.useVanillTextureCheckBox._setResponder(responder);
		}

		this.capeList = new CapeList(this.minecraft, (int)(this.width * 0.6F) - 20, this.height - 64, 30);
		this.capeOptionsLayout = new LayoutElements<> (this.capeOptions, this.saveButton, this.quitButton, this.signOutButton);
		this.capeSelectionLayout = new LayoutElements<> (this.capeList, this.backButton);
	}

	public void refreshOptionComponents(Cosmetic selectedCosmetic) {
		this.capeOptions.clearComponents();

		switch (selectedCosmetic.slot()) {
		case CAPE -> {
			this.capeOptions.newRow();
			this.capeOptions.addComponentCurrentRow(new Static(this, this.capeOptions.nextStart(8), 80, 60, 15, HorizontalSizing.LEFT_WIDTH, null, "gui.epicskins.avatar.cape_type"));
			this.capeOptions.addComponentCurrentRow(this.capePopupBox.relocateX(this.getRectangle(), this.capeOptions.nextStart(8)));

			if (selectedCosmetic.useIntParam1()) {
				this.capeOptions.newRow();
				this.capeOptions.addComponentCurrentRow(new Static(this, this.capeOptions.nextStart(8), 80, 60, 15, HorizontalSizing.LEFT_WIDTH, null, "gui.epicskins.avatar.cape_color"));
				this.capeOptions.addComponentCurrentRow(this.hueSlider.relocateX(this.getRectangle(), this.capeOptions.nextStart(8)));

				this.capeOptions.newRow();
				this.capeOptions.addComponentCurrentRow(this.saturationSlider.relocateX(this.getRectangle(), this.capeOptions.nextStart(96)));

				this.capeOptions.newRow();
				this.capeOptions.addComponentCurrentRow(this.brightnessSlider.relocateX(this.getRectangle(), this.capeOptions.nextStart(96)));
			}

			if (selectedCosmetic.useBoolParam1() && AuthenticationHelperImpl.getInstance().playerInfo().getSkin().capeTexture() != null) {
				this.capeOptions.newRow();
				this.capeOptions.addComponentCurrentRow(new Static(this, this.capeOptions.nextStart(8), 80, 60, 15, HorizontalSizing.LEFT_WIDTH, null, "gui.epicskins.avatar.vanilla_texture"));
				this.capeOptions.addComponentCurrentRow(this.useVanillTextureCheckBox.relocateX(this.getRectangle(), this.capeOptions.nextStart(8)));
			}
		}
		}

		this.capeOptions.updateSizeAndPosition((int)(this.width * 0.6F) - 20, this.height - 64, 30);
		this.capeOptions.setX(10);
	}

	@Override
	protected void init() {
		this.saveButton.setPosition(this.width / 2 - 125, this.height - 28);
		this.quitButton.setPosition(this.width / 2 - 40, this.height - 28);
		this.signOutButton.setPosition(this.width / 2 + 45, this.height - 28);
		this.signOutButton.active = AuthenticationHelperImpl.getInstance().status() == AuthenticationHelper.Status.AUTHENTICATED;
		this.backButton.setPosition(this.width / 2 - 100, this.height - 28);

		this.mainModelPreviewer.setX1((int)(this.width * 0.4F));
		this.mainModelPreviewer.resize(this.getRectangle());

		this.capeOptions.updateSizeAndPosition((int)(this.width * 0.6F) - 20, this.height - 64, 30);
		this.capeOptions.setX(10);

		this.capeList.updateSizeAndPosition((int)(this.width * 0.6F) - 20, this.height - 64, 30);
		this.capeList.setX(10);
		this.capeList.rearrangeCosmetics();

		this.addRenderableWidget(this.mainModelPreviewer);

		if (!this.initialized) {
			if (this.capePopupBox._getValue() != null && this.capePopupBox._getValue().useBoolParam1() && this.useVanillTextureCheckBox._getValue()) {
				this.mainModelPreviewer.setCloakTexture(AuthenticationHelperImpl.getInstance().playerInfo().getSkin().capeTexture());
			}

			this.initialized = true;
			this.capeOptionsLayout.accept(this::addRenderableWidget);
			this.activatedLayout = this.capeOptionsLayout;

			switch (AuthenticationHelperImpl.getInstance().status()) {
			case UNAUTHENTICATED -> {
				this.popupStartSigningIn();
			}
			case OFFLINE_MODE -> this.popupOfflineModeWarning();
			case AUTHENTICATED -> {}
			}
		} else {
			this.activatedLayout.accept(this::addRenderableWidget);
		}
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
		return this.getChildAt(mouseX, mouseY).filter((listener) -> {
			return listener.mouseDragged(mouseX, mouseY, button, dx, dy);
		}).isPresent();
	}

	@Override
	public void tick() {
		Runnable work = null;

		while ((work = this.deferredWorks.poll()) != null) {
			work.run();
		}

		if (--this.nextPlaying < 0) {
			RandomSource randomSource = RandomSource.create(System.currentTimeMillis());
			this.mainModelPreviewer.getAnimator().playAnimation(IDLE_ANIMATIONS.get(randomSource.nextInt(IDLE_ANIMATIONS.size())), 0.0F);
			this.nextPlaying = 160 + randomSource.nextInt(120);
		}

		this.mainModelPreviewer._tick();
	}

	public void onSigninSuccess() {
		this.deferredWorks.add(() -> {
			this.saveButton.active = true;
			this.minecraft.setScreen(this);
			this.capeList.rearrangeCosmetics();

			CapeProperties userskinInfo = AuthenticationHelperImpl.getInstance().capeProperties();
			Cosmetic cosmetic = AuthenticationHelperImpl.getInstance().getCosmetic(userskinInfo.capeSeq());

			if (cosmetic != null) {
				this.capePopupBox._setValue(cosmetic);
				this.hueSlider.setValue(userskinInfo.hue());
				this.saturationSlider.setValue(userskinInfo.saturation());
				this.brightnessSlider.setValue(userskinInfo.brightness());
				this.useVanillTextureCheckBox._setValue(userskinInfo.useVanillaTexture());
				this.refreshOptionComponents(cosmetic);
			}
		});
	}

	public void popupStartSigningIn() {
		this.deferredWorks.add(() -> {
			this.minecraft.setScreen(new MessageScreen<>("Sign up/in first before setting up cosmetics", "", this, (button) -> {
				this.popupAuthCodeEnter();
				AuthenticationHelperImpl.getInstance().openAuthenticateBrowser();
			}, 240, 70).autoCalculateHeight());
		});
	}

	public void popupAuthCodeEnter() {
		this.deferredWorks.add(() -> {
			ResizableEditBox verifyingBox = new ResizableEditBox(this.minecraft.font, 0, 0, 0, 16, Component.literal(""), null, null);
			verifyingBox.setMaxLength(6);

			this.minecraft.setScreen(new MessageScreen<>("Enter the 6-characters verifying code", "", this,
														(code) -> {
															this.popupResponseAwaiting();

															AuthenticationHelperImpl.getInstance().loginWithAuthCode(code,
																() -> this.onSigninSuccess(),
																(ex) -> {
																	ex.printStackTrace();

																	if (ex instanceof HttpResponseException httpFailResponseException) {
																		switch (httpFailResponseException.getResponseBody()) {
																		case "expired" -> {
																			this.popupVerifyCodeExpired();
																		}
																		case "invalid_code" -> {
																			this.popupInvalidVerifyingCode();
																		}
																		case "not_an_owner" -> {
																			this.popupInvalidVerifyingCode();
																		}
																		default -> {
																			this.popupHttpResponseException(httpFailResponseException.getHttpStatusCode(), httpFailResponseException.getResponseBody());
																		}
																		}
																	} else {
																		this.popupException(ex);
																	}
																}
															);
														},
														(button) -> this.minecraft.setScreen(this), verifyingBox, 300, 90));
		});
	}

	public void popupInvalidVerifyingCode() {
		this.deferredWorks.add(() -> {
			ResizableEditBox verifyingBox = new ResizableEditBox(this.minecraft.font, 0, 0, 0, 16, Component.literal(""), null, null);
			verifyingBox.setMaxLength(6);

			this.minecraft.setScreen(new MessageScreen<>("Invalid verifying code!", "Please try again", this,
														(code) -> {
															AuthenticationHelperImpl.getInstance().loginWithAuthCode(code, () -> this.onSigninSuccess(), (ex) -> {
																if (ex instanceof HttpResponseException httpFailResponseException) {
																	switch (httpFailResponseException.getResponseBody()) {
																	case "expired" -> {
																		this.popupVerifyCodeExpired();
																	}
																	case "invalid_code" -> {
																		this.popupInvalidVerifyingCode();
																	}
																	case "not_an_owner" -> {
																		this.popupInvalidVerifyingCode();
																	}
																	}
																} else {
																	this.popupException(ex);
																}
															});
														},
														(button) -> this.minecraft.setScreen(this), verifyingBox, 200, 100));
		});
	}

	public void popupVerifyCodeExpired() {
		this.deferredWorks.add(() -> {
			this.minecraft.setScreen(new MessageScreen<>("Verifying code expired!", "", this, (button2) -> this.minecraft.setScreen(this.parentScreen), 200, 70).autoCalculateHeight());
		});
	}

	public void popupOfflineModeWarning() {
		this.deferredWorks.add(() -> {
			this.minecraft.setScreen(new MessageScreen<>("Failed to verify your Minecraft account", "You may use cracked or unofficial launcher, please use official launcher to connect your Minecraft account.",
					this, (button2) -> this.minecraft.setScreen(this.parentScreen), 300, 70).autoCalculateHeight());
		});
	}

	public void popupConnectionFail(Throwable exception) {
		this.deferredWorks.add(() -> {
			this.minecraft.setScreen(new MessageScreen<>("Connection failed", ExceptionUtils.getRootCause(exception).getMessage() + "\n\nPlease try later"
						+ "\nIf the problem persists, please contact us by the links below:"
						+ "\nOfficial Discord Server: https://discord.com/invite/NbAJwj8RHg"
						+ "\nGithub Issue Trakcer: https://github.com/Yesssssman/epicfightmod/issues",
						this, (button2) -> this.minecraft.setScreen(this.parentScreen), 300, 70).autoCalculateHeight());
		});
	}

	public void popupException(Throwable exception) {
		this.deferredWorks.add(() -> {
			this.minecraft.setScreen(new MessageScreen<>("Connection failed", ExceptionUtils.getRootCause(exception) + ": " + ExceptionUtils.getRootCause(exception).getMessage()
						+ "\n\nPlease report the issue to the link below"
						+ "\nGithub Issue Trakcer: https://github.com/Yesssssman/epicfightmod/issues",
						this, (button2) -> this.minecraft.setScreen(this.parentScreen), 300, 70).autoCalculateHeight());
		});
	}

	public void popupHttpResponseException(int responseCode, String responseBody) {
		this.deferredWorks.add(() -> {
			this.minecraft.setScreen(new MessageScreen<>(String.valueOf(responseCode) + " Response", responseBody
						+ "\n\nPlease report the issue to the link below"
						+ "\nGithub Issue Trakcer: https://github.com/Yesssssman/epicfightmod/issues",
						this, (button2) -> this.minecraft.setScreen(this.parentScreen), 300, 70).autoCalculateHeight());
		});
	}

	public void popupAccessDenied() {
		this.deferredWorks.add(() -> {
			this.minecraft.setScreen(new MessageScreen<>("Failed in Discord authentication", "Access denied.", this, (button2) -> this.minecraft.setScreen(this), 250, 70).autoCalculateHeight());
		});
	}

	public void popupNoUserInfo() {
		this.deferredWorks.add(() -> {
			this.minecraft.setScreen(new MessageScreen<>("Failed in Discord authentication", "Can't find your account in Epic Fight discord server. Please join our Discord server to identify your account."
						+ "\nOfficial Discord Server: https://discord.com/invite/NbAJwj8RHg",
						this, (button2) -> this.minecraft.setScreen(this), 250, 70).autoCalculateHeight());
		});
	}

	public void popupDuplicatedDiscordID(String errMsg) {
		this.deferredWorks.add(() -> {
			this.minecraft.setScreen(new MessageScreen<>("Failed in Discord authentication", errMsg + "\nPlease contact our developer team to get any supports."
						+ "\nOfficial Discord Server: https://discord.com/invite/NbAJwj8RHg",
						this, (button2) -> this.minecraft.setScreen(this), 330, 70).autoCalculateHeight());
		});
	}

	public void popupSignOutAsk() {
		this.deferredWorks.add(() -> {
			this.minecraft.setScreen(new MessageScreen<> ("Do you want to sign out?", "", this, (ok) -> {
				this.popupResponseAwaiting();

				AuthenticationHelperImpl.getInstance().signOut(() -> {
					this.popupSignOutSuccess();
				}, (ex) -> {
					if (ex instanceof HttpResponseException httpFailResponseException) {
						if (httpFailResponseException.getHttpStatusCode() == 404) {
							AuthenticationHelperImpl.getInstance().onSignOut();
						}

						this.popupHttpResponseException(httpFailResponseException.getHttpStatusCode(), httpFailResponseException.getResponseBody());
					} else {
						this.popupException(ex);
					}
				});
			}, (cancel) -> this.minecraft.setScreen(this), 200, 90).autoCalculateHeight());
		});
	}

	public void popupSavedSuccess() {
		this.deferredWorks.add(() -> {
			this.minecraft.setScreen(new MessageScreen<>("Successfully saved!", "", this, (button2) -> this.minecraft.setScreen(this), 200, 70).autoCalculateHeight());
		});
	}

	public void popupSignOutSuccess() {
		this.deferredWorks.add(() -> {
			this.minecraft.setScreen(new MessageScreen<>("Successfully signed out!", "", this, (button2) -> this.minecraft.setScreen(new TitleScreen()), 200, 70).autoCalculateHeight());
		});
	}

	public void popupTimeout() {
		this.deferredWorks.add(() -> {
			this.minecraft.setScreen(new MessageScreen<>("Failed in Discord authentication", "Session timed out! Please try it later.", this, (button2) -> this.minecraft.setScreen(this), 300, 70).autoCalculateHeight());
		});
	}

	public void popupResponseAwaiting() {
		this.deferredWorks.add(() -> {
			this.minecraft.setScreen(new AwaitIconMessageScreen("Awaiting for response...", "", this, 200, 90).withOkTitle(CommonComponents.GUI_CANCEL));
		});
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		super.render(guiGraphics, mouseX, mouseY, partialTicks);
		guiGraphics.drawCenteredString(this.font, this.title, 56, 15, 16777215);
	}

	@Override
	public void onClose() {
		this.capeList.destroy();
		this.minecraft.setScreen(this.parentScreen);
	}

	public class CapeList extends ContainerObjectSelectionList<CapeList.CosmeticsEntry> {
		private CosmeticBlock focused;

		public CapeList(Minecraft pMinecraft, int pWidth, int pHeight, int pY0) {
			super(pMinecraft, pWidth, pHeight, pY0, 0);
		}

		@Override
	    protected void renderListBackground(GuiGraphics guiGraphics) {
	    }

	    @Override
	    protected void renderListSeparators(GuiGraphics guiGraphics) {
	    }

		public Cosmetic getSelectedCloak() {
			return this.focused != null ? this.focused.getCosmetic() : null;
		}

		public void moveScrollToFocus() {
			int idx = this.children().indexOf(this.getFocused());

			if (idx > -1) {
				this.setScrollAmount(this.itemHeight * idx);
			}
		}

		public void rearrangeCosmetics() {
			int blockLength = 0;
			int columns = 1;

			while (true) {
				int rowWidth = this.width / columns;

				if (rowWidth < 200) {
					blockLength = rowWidth;
					break;
				}

				columns++;
			}

			this.itemHeight = blockLength;
			this.clearEntries();

			int count = 0;
			int row = 0;
			ImmutableList.Builder<CosmeticBlock> builder = ImmutableList.builder();

			for (Cosmetic cosmetic : AuthenticationHelperImpl.getInstance().getAllCosmetics()) {
				CapeBlock capeButton =
					new CapeBlock(
						  this.getX() + count * blockLength + 3
						, this.getY() + row * blockLength + 3
						, blockLength - 6
						, blockLength - 6
						, cosmetic
					);

				capeButton.setTooltip(Tooltip.create(Component.literal(cosmetic.description())));

				builder.add(capeButton);

				count++;

				if (count == columns) {
					this.addEntry(new CosmeticsEntry(builder.build()));
					builder = ImmutableList.builder();
					count = 0;
					row++;
				}
			}

			List<CosmeticBlock> list = builder.build();

			if (!list.isEmpty()) {
				this.addEntry(new CosmeticsEntry(list));
			}
		}

		@Override
		protected int getScrollbarPosition() {
			return this.getRight() - 6;
		}

		@Override
		public int getRowWidth() {
			return this.width;
		}

		@Override
		protected void clearEntries() {
			this.destroy();
			super.clearEntries();
		}

		public void destroy() {
			this.children().forEach((entry) -> entry.cosmeticsBlock.forEach(CosmeticBlock::destroy));
		}

		@Override
		public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
			this.updateScrollingState(pMouseX, pMouseY, pButton);

			if (!this.isMouseOver(pMouseX, pMouseY)) {
				return false;
			} else {
				for (int i1 = 0; i1 < this.getItemCount(); ++i1) {
					int j1 = this.getRowTop(i1);
					int k1 = this.getRowBottom(i1);

					if (k1 >= this.getY() && j1 <= this.getBottom()) {
						CapeList.CosmeticsEntry entry = this.children().get(i1);

						for (CapeList.CosmeticBlock b : entry.cosmeticsBlock) {
							if (b.mouseClicked(pMouseX, pMouseY, pButton)) {
								CosmeticsEntry oEntry = this.getFocused();

								if (entry != oEntry && oEntry instanceof ContainerEventHandler) {
									ContainerEventHandler containereventhandler = (ContainerEventHandler) oEntry;
									containereventhandler.setFocused((GuiEventListener)null);
								}

								this.setFocused(entry);
								this.setDragging(true);
								b.onPress(b == this.focused);

								if (this.focused != null) {
									this.focused.setFocused(false);
								}

								this.focused = b;
								this.focused.setFocused(true);

								return true;
							}
						}
					}
				}

				return this.scrolling;
			}
		}

		public class CosmeticsEntry extends ContainerObjectSelectionList.Entry<CapeList.CosmeticsEntry> {
			private final List<CosmeticBlock> cosmeticsBlock;

			public CosmeticsEntry(List<CosmeticBlock> cosmeticBlocks) {
				this.cosmeticsBlock = cosmeticBlocks;
			}

			@Override
			public List<? extends GuiEventListener> children() {
				return this.cosmeticsBlock;
			}

			@Override
			public List<? extends NarratableEntry> narratables() {
				return this.cosmeticsBlock;
			}

			@Override
			public void render(GuiGraphics pGuiGraphics, int pIndex, int pTop, int pLeft, int pWidth, int pHeight, int pMouseX, int pMouseY, boolean pHovering, float pPartialTick) {
				for (CosmeticBlock cosmeticBlock : this.cosmeticsBlock) {
					cosmeticBlock.setY(pTop);
					cosmeticBlock.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
				}
			}
		}

		public abstract class CosmeticBlock extends AbstractWidget {
			public CosmeticBlock(int pX, int pY, int pWidth, int pHeight, Component pMessage) {
				super(pX, pY, pWidth, pHeight, pMessage);
			}

			public abstract void onPress(boolean pressedTwice);
			public abstract Cosmetic getCosmetic();
			public abstract void destroy();
		}

		public class CapeBlock extends CosmeticBlock {
            private static final ResourceLocation LOCK_ICON = EpicFight.identifier("textures/gui/lock.png");
			private final ModelPreviewer modelPreviewer;
			private Cosmetic cosmetic;

			public CapeBlock(int pX, int pY, int pWidth, int pHeight, Cosmetic cosmetic) {
				super(pX, pY, pWidth, pHeight, Component.empty());

				this.cosmetic = cosmetic;

				switch (cosmetic.slot()) {
				case CAPE -> {
					this.modelPreviewer = new ModelPreviewer(pX, pWidth, pY, pHeight, null, null, null, Meshes.BIPED);
					this.modelPreviewer.setBackgroundClearColor(new Vec4f(0.3552F, 0.3552F, 0.3552F, 0.3F));
					this.modelPreviewer.setCameraTransform(-2.5D, 18.75F, 16.25F, -1.1641532E-8F, -0.0F);

					cosmetic.getAsMesh((mesh) -> {
						this.modelPreviewer.initCloakInfo((SoftBodyTranslatable)mesh, cosmetic.textureLocation(), null);
					});
				}
				default -> {
					throw new UnsupportedOperationException("Unsupported slot type: " + cosmetic.slot());
				}
				}
			}

			@Override
			protected void renderWidget(GuiGraphics guiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
				if (this.isFocused()) {
					this.modelPreviewer.setBackgroundClearColor(new Vec4f(0.5552F, 0.5552F, 0.5552F, 0.8F));
				} else {
					this.modelPreviewer.setBackgroundClearColor(new Vec4f(0.3552F, 0.3552F, 0.3552F, 0.3F));
				}

				RenderSystem.enableBlend();
				this.modelPreviewer.render(guiGraphics, pMouseX, pMouseY, pPartialTick);

				if (!this.cosmetic.unlocked()) {
					guiGraphics.fill(this.modelPreviewer.getX(), this.modelPreviewer.getY(), this.modelPreviewer.getX() + this.modelPreviewer.getWidth(), this.modelPreviewer.getY() + this.modelPreviewer.getHeight(), 0xBB111111);
					guiGraphics.blit(LOCK_ICON, this.modelPreviewer.getX() + this.modelPreviewer.getWidth() / 2 - 8, this.modelPreviewer.getY() + this.modelPreviewer.getHeight() / 2 - 8, 16, 16, 0, 0, 16, 16, 16, 16);
				}

				RenderSystem.disableBlend();

				guiGraphics.drawCenteredString(CapeList.this.minecraft.font, this.cosmetic.title(), this.getX() + this.width / 2, this.getY() + this.height - 12, -1);
			}

			@Override
			public void onPress(boolean pressedTwice) {
				if (!pressedTwice) {
					if (this.modelPreviewer.getCloakMesh() != null) {
						boolean canConfigureColor = !AvatarEditScreen.this.useVanillTextureCheckBox._getValue() && this.cosmetic.useIntParam1() ;

						if (canConfigureColor) {
							AvatarEditScreen.this.mainModelPreviewer.setCloakColor(AvatarEditScreen.this.brightnessSlider.getColor());
						} else {
							AvatarEditScreen.this.mainModelPreviewer.setCloakColor(0xFFFFFFFF);
						}
						
						boolean useVanillaTexture = AvatarEditScreen.this.useVanillTextureCheckBox._getValue() && this.cosmetic.useBoolParam1();
						
						AvatarEditScreen.this.mainModelPreviewer.initCloakInfo(
							  this.modelPreviewer.getCloakMesh()
							, useVanillaTexture ? AuthenticationHelperImpl.getInstance().playerInfo().getSkin().capeTexture() : this.modelPreviewer.getCloakTexture()
							, ClothSimulator.ClothObjectBuilder.create()
								.parentJoint(Armatures.BIPED.get().torso)
								.putAll(ClothColliderPresets.BIPED)
						);
					} else {
						AvatarEditScreen.this.mainModelPreviewer.removeCloak();
					}
				}
				
				if (pressedTwice && this.cosmetic.unlocked()) {
					AvatarEditScreen.this.capePopupBox._setValue(this.cosmetic);
					AvatarEditScreen.this.refreshOptionComponents(this.cosmetic);
					
					AvatarEditScreen.this.capeSelectionLayout.accept(AvatarEditScreen.this::removeWidget);
					AvatarEditScreen.this.capeOptionsLayout.accept(AvatarEditScreen.this::addRenderableWidget);
					AvatarEditScreen.this.activatedLayout = AvatarEditScreen.this.capeOptionsLayout;
				}
			}
			
			@Override
			public void destroy() {
				this.modelPreviewer.onDestroy();
			}

			@Override
			public Cosmetic getCosmetic() {
				return this.cosmetic;
			}
			
			@Override
			protected void updateWidgetNarration(NarrationElementOutput pNarrationElementOutput) {
			}

			@Override
			public void setX(int x) {
				super.setX(x);
				this.modelPreviewer.setX(x);
			}

			@Override
			public void setY(int y) {
				super.setY(y);
				this.modelPreviewer.setY(y);
			}
		}
	}

	public static class LayoutElements<T extends GuiEventListener & Renderable & NarratableEntry> {
		private List<T> components;

		@SafeVarargs
		private LayoutElements(T... components) {
			this.components = Arrays.asList(components);
		}

		public void accept(Consumer<T> consumer) {
			this.components.forEach(consumer);
		}
	}
}
