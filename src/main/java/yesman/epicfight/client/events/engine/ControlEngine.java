package yesman.epicfight.client.events.engine;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import yesman.epicfight.EpicFight;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;







import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import yesman.epicfight.api.animation.types.EntityState;
import yesman.epicfight.api.client.camera.EpicFightCameraAPI;
import yesman.epicfight.api.client.event.EpicFightClientEventHooks;
import yesman.epicfight.api.client.event.types.control.MappedMovementInputUpdateEvent;
import yesman.epicfight.api.client.input.InputManager;
import yesman.epicfight.api.client.input.PlayerInputState;
import yesman.epicfight.api.client.input.action.EpicFightInputAction;
import yesman.epicfight.api.client.input.action.InputAction;
import yesman.epicfight.api.client.input.action.MinecraftInputAction;
import yesman.epicfight.api.event.types.player.SkillCastEvent;
import yesman.epicfight.client.ClientEngine;
import yesman.epicfight.client.camera.EpicFightTpsCameraDisableState;
import yesman.epicfight.client.gui.screen.EmoteWheelScreen;
import yesman.epicfight.client.gui.screen.SkillEditScreen;
import yesman.epicfight.client.gui.screen.config.EpicFightSettingScreen;
import yesman.epicfight.client.input.InputUtils;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;
import yesman.epicfight.client.world.util.FakeLevel;
import yesman.epicfight.config.ClientConfig;
import yesman.epicfight.network.EpicFightNetworkManager;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.SkillSlot;
import yesman.epicfight.skill.SkillSlots;
import yesman.epicfight.skill.modules.ChargeableSkill;
import yesman.epicfight.skill.modules.HoldableSkill;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.capabilities.skill.PlayerSkills;
import yesman.epicfight.world.gamerule.EpicFightGameRules;
import yesman.epicfight.world.item.SkillBookItem;

import java.util.HashSet;
import java.util.Set;

public class ControlEngine implements IEventBasedEngine {
	private static final ControlEngine INSTANCE = new ControlEngine();

	public static ControlEngine getInstance() {
		return INSTANCE;
	}

	private final Set<CustomPacketPayload> packetsToSend = new HashSet<> ();
	private final Minecraft minecraft;
	private LocalPlayer player;
	private LocalPlayerPatch playerpatch;
	private int weaponInnatePressCounter = 0;
	private int sneakPressCounter = 0;
	private int moverPressCounter = 0;
	private int tickSinceLastJump = 0;
	private int lastHotbarLockedTime;
	private boolean weaponInnatePressToggle = false;
	private boolean sneakPressToggle = false;
	private boolean moverPressToggle = false;
	private boolean attackLightPressToggle = false;
	private boolean hotbarLocked;
	private boolean holdingFinished;
	private int reserveCounter;
    /**
     * <b>DEPRECATED:</b> This field is retained for backward compatibility and should not be used
     * for comparisons or method calls on this instance. In future updates, {@link EpicFightInputAction}
     * will be stored directly instead of a vanilla {@link KeyMapping}.
     *
     * <p>Do not rely on this field for new functionality. For mapping a {@link KeyMapping} to an
     * action, use {@link ControlEngine#mapKeyMappingToAction} instead (temporary solution).</p>
     *
     * @see ControlEngine#mapKeyMappingToAction
     */
    @SuppressWarnings("DeprecatedIsStillUsed")
    @Deprecated
	private KeyMapping reservedKey;
	private SkillSlot reservedOrHoldingSkillSlot;
    /**
     * <b>DEPRECATED:</b> Consider using {@link ControlEngine#isCurrentHoldingAction} or
     * {@link ControlEngine#isCurrentHoldingActionActive} instead of directly
     * accessing or comparing this field. This field is retained for backward
     * compatibility; in future updates, {@link EpicFightInputAction} will be
     * stored directly instead of a vanilla {@link KeyMapping}.
     *
     * @see ControlEngine#mapKeyMappingToAction
     */
    @SuppressWarnings("DeprecatedIsStillUsed")
    @Deprecated
    private KeyMapping currentHoldingKey;
	public Options options;

	private ControlEngine() {
		this.minecraft = Minecraft.getInstance();

		if (this.minecraft != null) {
			this.options = this.minecraft.options;
		}
	}

	public void reloadPlayerPatch(LocalPlayerPatch playerpatch) {
		this.weaponInnatePressCounter = 0;
		this.weaponInnatePressToggle = false;
		this.sneakPressCounter = 0;
		this.sneakPressToggle = false;
		this.attackLightPressToggle = false;
		this.player = playerpatch.getOriginal();
		this.playerpatch = playerpatch;
	}

	public LocalPlayerPatch getPlayerPatch() {
		return this.playerpatch;
	}

	public boolean canPlayerMove(EntityState playerState) {
		return !playerState.movementLocked() || this.player.jumpableVehicle() != null;
    }

    public boolean canPlayerRotate(EntityState playerState) {
        return !playerState.turningLocked() || this.player.jumpableVehicle() != null;
    }

    public void handleEpicFightKeyMappings() {
        // Pause here if playerpatch is null
        if (this.playerpatch == null) {
            return;
        }

        InputManager.triggerOnPress(EpicFightInputAction.OPEN_SKILL_SCREEN, this::openSkillEditor);

        InputManager.triggerOnPress(EpicFightInputAction.OPEN_CONFIG_SCREEN, this::openConfig);

        // TODO: We need another way to detect when the key first pressed especailly for UI open key mappings
        InputManager.triggerOnPress(EpicFightInputAction.OPEN_EMOTE_WHEEL_SCREEN, this::mayOpenEmoteWheel);

        InputManager.triggerOnPress(EpicFightInputAction.SWITCH_VANILLA_MODEL_DEBUGGING, this::switchVanillaModelDebugging);

        // The "runKeyboardMouseEvent" is used as a workaround to this issue:
        // * https://github.com/Epic-Fight/epicfight/issues/1771
        // * https://github.com/Creators-of-Create/Create/issues/6901

        InputManager.triggerOnPress(
            EpicFightInputAction.ATTACK,
            () -> InputUtils.runKeyboardMouseEvent(EpicFightInputAction.ATTACK, this::maybeAttack)
        );

        InputManager.triggerOnPress(
            EpicFightInputAction.DODGE,
            () -> InputUtils.runKeyboardMouseEvent(EpicFightInputAction.DODGE, this::maybeDodge)
        );

        if (InputManager.isActionActive(EpicFightInputAction.GUARD)) this.maybeGuard();

        InputManager.triggerOnPress(
            EpicFightInputAction.WEAPON_INNATE_SKILL,
            () -> InputUtils.runKeyboardMouseEvent(EpicFightInputAction.WEAPON_INNATE_SKILL, this::handleSeparateWeaponInnateSkill)
        );

        InputManager.triggerOnPress(
            EpicFightInputAction.MOBILITY,
            () -> InputUtils.runKeyboardMouseEvent(EpicFightInputAction.MOBILITY, this::maybePerformMoverSkill)
        );

        InputManager.triggerOnPress(EpicFightInputAction.SWITCH_MODE, this::switchMode);

        InputManager.triggerOnPress(EpicFightInputAction.LOCK_ON, this::toggleLockOnState);

        InputManager.triggerOnPress(EpicFightInputAction.LOCK_ON_SHIFT_LEFT, this::searchNewTargetFromLeft);

        InputManager.triggerOnPress(EpicFightInputAction.LOCK_ON_SHIFT_RIGHT, this::searchNewTargetFromRight);

        if (shouldDisableSwapHandItems()) consumeSwapOffhandKeyClicks();

        // Pause here if player is not in battle mode
        if (!this.playerpatch.isEpicFightMode() || Minecraft.getInstance().isPaused()) {
            return;
        }

        if (this.player.tickCount - this.lastHotbarLockedTime > 20 && this.hotbarLocked) {
            this.unlockHotkeys();
        }

        if (this.weaponInnatePressToggle) {
            if (!InputManager.isActionActive(EpicFightInputAction.WEAPON_INNATE_SKILL)) {
                this.attackLightPressToggle = true;
                this.weaponInnatePressToggle = false;
                this.weaponInnatePressCounter = 0;
            } else {
                if (InputManager.isBoundToSamePhysicalInput(EpicFightInputAction.WEAPON_INNATE_SKILL, EpicFightInputAction.ATTACK)) {
                    if (this.weaponInnatePressCounter > ClientConfig.holdingThreshold) {
                        if (this.playerpatch.getSkill(SkillSlots.WEAPON_INNATE).sendCastRequest(this.playerpatch, this).shouldReserveKey()) {
                            if (!this.player.isSpectator()) {
                                this.reserveKey(SkillSlots.WEAPON_INNATE, EpicFightInputAction.WEAPON_INNATE_SKILL);
                            }
                        } else {
                            this.lockHotkeys();
                        }

                        this.weaponInnatePressToggle = false;
                        this.weaponInnatePressCounter = 0;
                    } else {
                        this.weaponInnatePressCounter++;
                    }
                }
            }
        }

        if (this.attackLightPressToggle) {
            SkillCastEvent skillCastEvent = this.playerpatch.getSkill(SkillSlots.COMBO_ATTACKS).sendCastRequest(this.playerpatch, this);

            if (skillCastEvent.isExecutable()) {
                this.player.resetAttackStrengthTicker();
                this.releaseAllServedKeys();
            } else {
                if (!this.player.isSpectator()) {
                    this.reserveKey(SkillSlots.COMBO_ATTACKS, EpicFightInputAction.ATTACK);
                }
            }

            this.lockHotkeys();

            this.attackLightPressToggle = false;
            this.weaponInnatePressToggle = false;
            this.weaponInnatePressCounter = 0;
        }

        if (this.sneakPressToggle) {
            if (!InputManager.isActionActive(MinecraftInputAction.SNEAK)) {
                SkillSlot skillSlot = (this.playerpatch.getEntityState().knockDown()) ? SkillSlots.KNOCKDOWN_WAKEUP : SkillSlots.DODGE;
                SkillContainer skill = this.playerpatch.getSkill(skillSlot);

                if (skill.sendCastRequest(this.playerpatch, this).shouldReserveKey()) {
                    this.reserveKey(skillSlot, MinecraftInputAction.SNEAK);
                }

                this.sneakPressToggle = false;
                this.sneakPressCounter = 0;
            } else {
                if (this.sneakPressCounter > ClientConfig.holdingThreshold) {
                    this.sneakPressToggle = false;
                    this.sneakPressCounter = 0;
                } else {
                    this.sneakPressCounter++;
                }
            }
        }

        if (this.currentHoldingKey != null) {
            SkillContainer container = this.playerpatch.getSkill(this.reservedOrHoldingSkillSlot);

            if (!container.isEmpty()) {
                if (container.getSkill() instanceof HoldableSkill) {
                    if (!this.isCurrentHoldingActionActive()) {
                        this.holdingFinished = true;
                    }

                    if (container.getSkill() instanceof ChargeableSkill chargingSkill) {
                        if (this.holdingFinished) {
                            if (this.playerpatch.getSkillChargingTicks() > chargingSkill.getMinChargingTicks()) {
                                container.sendCastRequest(this.playerpatch, this);
                                this.releaseAllServedKeys();
                            }
                        } else if (this.playerpatch.getSkillChargingTicks() >= chargingSkill.getAllowedMaxChargingTicks()) {
                            this.releaseAllServedKeys();
                        }
                    } else {
                        if (this.holdingFinished) {
                            // Note: Holdable skills are canceled in client first
                            this.playerpatch.resetHolding();
                            CompoundTag arguments = new CompoundTag();
                            container.getSkill().gatherArguments(container, this, arguments);
                            container.getSkill().cancelOnClient(container, arguments);
                            container.sendCancelRequest(this.playerpatch, this);
                            this.releaseAllServedKeys();
                        }
                    }
                } else {
                    this.releaseAllServedKeys();
                }
            }
        }

        if (this.reservedKey != null) {
            if (this.reserveCounter > 0) {
                SkillContainer skill = this.playerpatch.getSkill(this.reservedOrHoldingSkillSlot);
                this.reserveCounter--;

                if (skill.getSkill() != null) {
                    if (skill.sendCastRequest(this.playerpatch, this).isExecutable()) {
                        this.releaseAllServedKeys();
                        this.lockHotkeys();
                    }
                }
            } else {
                this.releaseAllServedKeys();
			}
		}

		if (isSwitchOrDropBlocked()) {
			disableHotbarSlotPresses();
			consumeDropKeyClicks();
		}

        maybeCorrectTPSPosition();
    }

    private void maybeCorrectTPSPosition() {
        if (this.minecraft.level == null || !EpicFightCameraAPI.getInstance().isTPSMode()) {
            return;
        }

        if (!InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), InputConstants.KEY_LCONTROL)) {
            return;
        }

        if (InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), InputConstants.KEY_LEFT)) {
            ClientConfig.cameraHorizontalLocation = Math.min(10, ClientConfig.cameraHorizontalLocation + 1);
        }

        if (InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), InputConstants.KEY_RIGHT)) {
            ClientConfig.cameraHorizontalLocation = Math.max(-10, ClientConfig.cameraHorizontalLocation - 1);
        }

        if (InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), InputConstants.KEY_UP)) {
            ClientConfig.cameraVerticalLocation = Math.min(5, ClientConfig.cameraVerticalLocation + 1);
        }

        if (InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), InputConstants.KEY_DOWN)) {
            ClientConfig.cameraVerticalLocation = Math.max(-2, ClientConfig.cameraVerticalLocation - 1);
        }
	}

    private void openSkillEditor() {
        final PlayerSkills playerSkills = this.playerpatch.getPlayerSkills();
        if (playerSkills == null) {
            return;
        }

        this.minecraft.setScreen(new SkillEditScreen(this.player, playerSkills));
    }

    private void openConfig() {
    	this.minecraft.setScreen(new EpicFightSettingScreen(null, null));
    }

    private void mayOpenEmoteWheel() {
        if (this.minecraft.screen == null) {
            this.minecraft.setScreen(new EmoteWheelScreen(this.playerpatch));
        }
    }

    private void switchVanillaModelDebugging() {
        boolean flag = ClientEngine.getInstance().switchVanillaModelDebuggingMode();
        this.minecraft.keyboardHandler.debugFeedbackTranslated(flag ? "debug.vanilla_model_debugging.on" : "debug.vanilla_model_debugging.off");
    }

    private void maybeAttack() {
        if (!this.playerpatch.isEpicFightMode() || isCurrentHoldingAction(EpicFightInputAction.ATTACK)) {
            return;
        }

        if (EpicFightTpsCameraDisableState.deferAction(this::maybeAttack)) {
            return;
        }

        final MinecraftInputAction vanillaAttack = MinecraftInputAction.ATTACK_DESTROY;
        final EpicFightInputAction epicFightAttack = EpicFightInputAction.ATTACK;

        boolean shouldPlayAttackAnimation = this.playerpatch.canPlayAttackAnimation();
        if (InputUtils.getKey(vanillaAttack.keyMapping()) == InputUtils.getKey(epicFightAttack.keyMapping()) &&
                Minecraft.getInstance().hitResult != null && shouldPlayAttackAnimation) {
            // Not needed for controller inputs.
            // This is called for keyboard/mouse inputs to just reset the internal keymapping counter.
            // It does not cancel the attack input, as that is handled in a mixin on the Minecraft class.
            consumeVanillaAttackKeyClicks();
        }

        if (shouldPlayAttackAnimation) {
            if (!InputManager.isBoundToSamePhysicalInput(epicFightAttack, EpicFightInputAction.WEAPON_INNATE_SKILL)) {
                SkillCastEvent skillCastEvent = this.playerpatch.getSkill(SkillSlots.COMBO_ATTACKS).sendCastRequest(this.playerpatch, this);

                if (skillCastEvent.isExecutable()) {
                    this.player.resetAttackStrengthTicker();
                    this.attackLightPressToggle = false;
                    this.releaseAllServedKeys();
                } else {
                    if (!this.player.isSpectator()) {
                        this.reserveKey(SkillSlots.COMBO_ATTACKS, epicFightAttack);
                    }
                }

                this.lockHotkeys();
                this.attackLightPressToggle = false;
                this.weaponInnatePressToggle = false;
                this.weaponInnatePressCounter = 0;
            } else {
                if (!this.weaponInnatePressToggle) {
                    this.weaponInnatePressToggle = true;
                }
            }
        }
    }

    private void maybeDodge() {
        if (!this.playerpatch.isEpicFightMode() || isCurrentHoldingAction(EpicFightInputAction.DODGE)) {
            return;
        }
        if (InputManager.isBoundToSamePhysicalInput(EpicFightInputAction.DODGE, MinecraftInputAction.SNEAK)) {
            if (this.player.getVehicle() == null) {
                if (!this.sneakPressToggle) {
                    this.sneakPressToggle = true;
                }
            }
        } else {
            SkillSlot skillCategory = (this.playerpatch.getEntityState().knockDown()) ? SkillSlots.KNOCKDOWN_WAKEUP : SkillSlots.DODGE;
            SkillContainer skill = this.playerpatch.getSkill(skillCategory);

            if (!skill.isEmpty() && skill.sendCastRequest(this.playerpatch, this).shouldReserveKey()) {
                this.reserveKey(SkillSlots.DODGE, EpicFightInputAction.DODGE);
            }
        }
    }

    private void maybeGuard() {
        if (!this.playerpatch.isEpicFightMode() || isCurrentHoldingAction(EpicFightInputAction.GUARD)) {
            return;
        }
        boolean shouldCancelGuard = false;

        if (this.playerpatch.isHoldingAny()) {
            shouldCancelGuard = true;
        } else if (ShieldItem.class.isAssignableFrom(this.player.getMainHandItem().getItem().getClass())
                || ShieldItem.class.isAssignableFrom(this.player.getOffhandItem().getItem().getClass())
                || this.player.getMainHandItem().getItem() instanceof SkillBookItem
                || this.player.getOffhandItem().getItem() instanceof SkillBookItem) {
            shouldCancelGuard = true;
        }

        if (!shouldCancelGuard) {
            SkillCastEvent skillCastEvent = this.playerpatch.getSkill(SkillSlots.GUARD).sendCastRequest(this.playerpatch, this);

            if (skillCastEvent.shouldReserveKey()) {
                if (!this.player.isSpectator()) {
                    this.reserveKey(SkillSlots.GUARD, EpicFightInputAction.GUARD);
                }
            } else {
                this.lockHotkeys();
            }
        }
    }

    private void handleSeparateWeaponInnateSkill() {
        if (!this.playerpatch.isEpicFightMode() || isCurrentHoldingAction(EpicFightInputAction.WEAPON_INNATE_SKILL)) {
            return;
        }
        if (!InputManager.isBoundToSamePhysicalInput(EpicFightInputAction.ATTACK, EpicFightInputAction.WEAPON_INNATE_SKILL)) {
            if (this.playerpatch.getSkill(SkillSlots.WEAPON_INNATE).sendCastRequest(this.playerpatch, this).shouldReserveKey()) {
                if (!this.player.isSpectator()) {
                    this.reserveKey(SkillSlots.WEAPON_INNATE, EpicFightInputAction.WEAPON_INNATE_SKILL);
                }
            } else {
                this.lockHotkeys();
            }
        }
    }

    private void maybePerformMoverSkill() {
        if (!this.playerpatch.isEpicFightMode() || this.playerpatch.isHoldingAny()) {
            return;
        }

        if (InputManager.isBoundToSamePhysicalInput(EpicFightInputAction.MOBILITY, MinecraftInputAction.JUMP)) {
            SkillContainer skillContainer = this.playerpatch.getSkill(SkillSlots.MOVER);

            if (!skillContainer.isEmpty()) {
                SkillCastEvent event = new SkillCastEvent(this.playerpatch, skillContainer, null);

                if (skillContainer.canUse(this.playerpatch, event) && this.player.getVehicle() == null) {
                    if (!this.moverPressToggle) {
                        this.moverPressToggle = true;
                    }
                }
            }
        } else {
            // Immediately trigger the skill cast if Mover and Jump are bound to different keys/buttons.
            SkillContainer skill = this.playerpatch.getSkill(SkillSlots.MOVER);
            skill.sendCastRequest(this.playerpatch, this);
        }
    }

    private void switchMode() {
        final boolean canSwitch = EpicFightGameRules.CAN_SWITCH_PLAYER_MODE.getRuleValue(this.playerpatch.getOriginal().level());
        if (!canSwitch) {
            this.minecraft.gui.getChat().addMessage(Component.translatable("epicfight.messages.mode_switching_disabled").withStyle(ChatFormatting.RED));
            return;
        }
        this.playerpatch.toggleMode();
    }

    /// Called from [MixinLocalPlayerInput] after [Input#tick] during [LocalPlayer#aiStep].
    /// Replaces the NeoForge [MovementInputUpdateEvent] handler [epicfight$moveInputEvent].
    public void handleMovementInput(Input input) {
        if (this.playerpatch == null) {
            return;
        }

        PlayerInputState playerinputstate = this.inputTick(input);

        MappedMovementInputUpdateEvent mappedEvent = new MappedMovementInputUpdateEvent(this.playerpatch, playerinputstate);
        EpicFightClientEventHooks.Control.MAPPED_MOVEMENT_INPUT_UPDATE.postWithListener(mappedEvent, this.playerpatch.getEventListener());
    }

    /// Called from [MixinLivingEntityJump] when the local player jumps.
    /// Replaces the NeoForge [LivingJumpEvent] handler [epicfight$livingJumpEvent].
    public void setTickSinceLastJump(int value) {
        this.tickSinceLastJump = value;
    }

    private void toggleLockOnState() {
        EpicFightCameraAPI.getInstance().toggleLockOn();
    }

    private void searchNewTargetFromLeft() {
        EpicFightCameraAPI.getInstance().setNextLockOnTarget(1, true, true);
    }

    private void searchNewTargetFromRight() {
        EpicFightCameraAPI.getInstance().setNextLockOnTarget(-1, true, true);
    }

    private PlayerInputState inputTick(Input input) {
		if (this.tickSinceLastJump > 0) this.tickSinceLastJump--;
		PlayerInputState inputState = InputManager.getInputState(input);

		if (this.moverPressToggle) {
			if (!InputManager.isActionActive(MinecraftInputAction.JUMP)) {
				this.moverPressToggle = false;
				this.moverPressCounter = 0;

				if (this.player.onGround()) {
					this.player.noJumpDelay = 0;
                    inputState = inputState.withJumping(true);
                    InputManager.setInputState(inputState);
				}
			} else {
				if (this.moverPressCounter > ClientConfig.holdingThreshold) {
					SkillContainer skill = this.playerpatch.getSkill(SkillSlots.MOVER);
					skill.sendCastRequest(this.playerpatch, this);

					this.moverPressToggle = false;
					this.moverPressCounter = 0;
				} else {
					this.player.noJumpDelay = 2;
					this.moverPressCounter++;
				}
			}
		}

		if (!this.canPlayerMove(this.playerpatch.getEntityState())) {
            inputState = inputState.copyWith(0F, 0F, false, false, false, false, false, false);
            InputManager.setInputState(inputState);
			this.player.sprintTriggerTime = -1;
			this.player.setSprinting(false);
		}

		return inputState;
	}

    /**
     * <b>DEPRECATED:</b> This method is retained for backward compatibility and will
     * be removed in a future release. Do not use it for new code.
     * <p>Instead of using this method, use
     * {@link #reserveKey(SkillSlot, InputAction)}, which works directly
     * with {@link EpicFightInputAction}.</p>
     */
    @SuppressWarnings("DeprecatedIsStillUsed")
    @Deprecated(forRemoval = true)
	private void reserveKey(SkillSlot slot, KeyMapping keyMapping) {
		this.reservedKey = keyMapping;
		this.reservedOrHoldingSkillSlot = slot;
		this.reserveCounter = 8;
	}

    private void reserveKey(SkillSlot slot, InputAction action) {
        reserveKey(slot, action.keyMapping());
    }

	public void releaseAllServedKeys() {
		this.holdingFinished = true;
		this.currentHoldingKey = null;
		this.reservedOrHoldingSkillSlot = null;
		this.reserveCounter = -1;
		this.reservedKey = null;
	}

	public void setHoldingKey(SkillSlot chargingSkillSlot, KeyMapping keyMapping) {
		this.holdingFinished = false;
		this.currentHoldingKey = keyMapping;
		this.reservedOrHoldingSkillSlot = chargingSkillSlot;
		this.reserveCounter = -1;
		this.reservedKey = null;
	}

	public void lockHotkeys() {
		this.hotbarLocked = true;
		this.lastHotbarLockedTime = this.player.tickCount;
        disableHotbarSlotPresses();
	}

	public void unlockHotkeys() {
		this.hotbarLocked = false;
	}

	public void addPacketToSend(CustomPacketPayload packet) {
		this.packetsToSend.add(packet);
	}

    /**
     * <b>DEPRECATED:</b> Use {@link InputManager#isActionActive} instead for controller support,
     * though it only handles Epic Fight supported actions. For checking a custom mod keybind,
     * use the vanilla {@link KeyMapping#isDown}.
     * <p>
     * Please note that there is a difference between vanilla {@link KeyMapping#isDown} and this method,
     * {@link KeyMapping#isDown} may be <code>false</code> in some cases even if the physical key
     * is actually down, for example, if a screen is open.
     * <p>
     * Even though this is a private method, it is retained in case an Epic Fight addon
     * accesses it by bypassing Java private access modifier restriction.
     */
	@SuppressWarnings({"JavadocReference", "DeprecatedIsStillUsed"})
    @Deprecated(forRemoval = true)
    public static boolean isKeyDown(KeyMapping key) {
        if (!RenderSystem.isOnRenderThread()) {
            return key.isDown();
        }
		InputConstants.Key inputKey = InputUtils.getKey(key);
		if (inputKey.getType() == InputConstants.Type.KEYSYM) {
			return key.isDown() || (inputKey.getValue() != GLFW.GLFW_KEY_UNKNOWN && GLFW.glfwGetKey(Minecraft.getInstance().getWindow().getWindow(), inputKey.getValue()) > 0);
		} else if(inputKey.getType() == InputConstants.Type.MOUSE) {
			return key.isDown() || GLFW.glfwGetMouseButton(Minecraft.getInstance().getWindow().getWindow(), inputKey.getValue()) > 0;
		} else {
			return false;
		}
	}

    /**
     * <b>DEPRECATED:</b> Use {@link InputManager#triggerOnPress} instead for controller support,
     * though it only handles Epic Fight supported actions. For checking a custom mod keybind,
     * use the vanilla {@link KeyMapping#consumeClick} with a {@code while} statement.
     * <p>
     * Even though this is a private method, it is retained in case an Epic Fight addon
     * accesses it by bypassing Java private access modifier restriction.
     * @see InputManager#isPhysicalKeyDownInternalWorkaround(KeyMapping)
     * @see InputManager#isKeyDown(KeyMapping)
     * @deprecated Consider adapting the Minecraft's {@link KeyMapping#isDown()}, keep in mind that
     *  this may return false in some cases, such as when a screen or chat is open.
     */
    @SuppressWarnings({"JavadocReference", "removal"})
    @Deprecated(forRemoval = true)
	private static boolean isKeyPressed(KeyMapping key, boolean eventCheck) {
		return key.consumeClick();
	}

    /**
     * <b>DISCOURAGED:</b> Does not support controller mods or other input systems.
     * <p>
     * This method was previously called before {@link Minecraft#handleKeybinds} to disable some vanilla
     * input actions.
     * <p>
     * Rather than relying on this method, consider fixing the problem at the core.
     * For example, to disable vanilla attack:
     * <pre>
     * {@code
     * @Mixin(value = Minecraft.class)
     * public class MixinMinecraft {
     *     @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
     *     private void onVanillaAttack(CallbackInfoReturnable<Boolean> cir) {
     *         cir.cancel();
     *     }
     * }
     * }
     * </pre>
     * <b>NOTE:</b> You may still want to call this method to reset the internal {@link KeyMapping#clickCount} state,
     * as a safety guard to prevent potential issues with other mods.
     * Refer to {@link ControlEngine#consumeVanillaAttackKeyClicks} and {@link ControlEngine#shouldDisableVanillaAttack} as an example.
     *
     * @see ControlEngine#shouldDisableVanillaAttack
     * @see ControlEngine#shouldDisableSwapHandItems
     */
    @SuppressWarnings({"JavadocReference", "DeprecatedIsStillUsed", "StatementWithEmptyBody", "DefaultAnnotationParam"})
    @Deprecated(forRemoval = false)
    public static void makeUnpressed(KeyMapping keyMapping) {
        while (keyMapping.consumeClick()) {
        }
        keyMapping.setDown(false);
    }

    /**
     * <b>DISCOURAGED:</b> Does not allow Epic Fight to perform additional
     * logic that is independent of the Minecraft vanilla {@link KeyMapping}.
     * Previously used to set the {@link KeyMapping#isDown} state for sprint keybind,
     * but that is now handled in {@link ControlEngine#setSprintingKeyStateNotDown}.
     * Alternatively, you could use the vanilla @{@link KeyMapping#set} directly.
     *
     * @see ControlEngine#makeUnpressed
     * @see ControlEngine#setSprintingKeyStateNotDown
     */
    @SuppressWarnings("JavadocReference")
    @Deprecated(forRemoval = true)
    public static void setKeyBind(KeyMapping key, boolean setter) {
        key.setDown(setter);
    }

    /**
     * Sets the state of the sprint keybind ({@link KeyMapping#isDown}) to `false`.
     * <p>
     * This only changes the internal {@link KeyMapping} state and does not actually disable sprinting.
     * To actually stop sprinting, this should usually be called alongside
     * {@link LocalPlayer#setSprinting}.
     * <p>
     * This is intended to support mods and other systems that rely on {@link KeyMapping#isDown()}
     * to check whether the player is sprinting, rather than {@link LocalPlayer#isSprinting()}.
     * This is not needed for controller mods.
     */
    @SuppressWarnings("JavadocReference")
    public static void setSprintingKeyStateNotDown() {
        MinecraftInputAction.SPRINT.keyMapping().setDown(false);
    }

    /**
     * Disables the player's vanilla attacks while in Epic Fight mode.
     * <p>
     * Previously, we injected into the vanilla {@link Minecraft#handleKeybinds} method
     * and used this workaround to reset the internal counter for the vanilla attack keybind:
     * <pre>
     * {@code
     * // Called before Minecraft#handleKeybinds is called.
     * KeyMapping attack = Minecraft.getInstance().options.keyAttack;
     * while (attack.consumeClick()) {}
     * KeyMapping.set(attack.getKey(), false);
     * }
     * </pre>
     * <p>
     * However, that approach relied on assumptions and did not support other mods, inputs, or systems.
     * The problem is now solved by injecting into {@link Minecraft#startAttack} and
     * canceling the call when the player is in Epic Fight mode.
     * This also means, the player must be always in vanilla mode to perform vanilla attacks, which is as intended.
     *
     * @see ControlEngine#consumeVanillaAttackKeyClicks
     */
    @SuppressWarnings("JavadocReference")
    @ApiStatus.Internal
    public static boolean shouldDisableVanillaAttack() {
        final LocalPlayerPatch playerpatch = EpicFightCapabilities.ClientModule.getCachedLocalPlayerPatch();
        if (playerpatch == null) {
            return false;
        }
        return playerpatch.isEpicFightMode() && playerpatch.canPlayAttackAnimation();
    }

    /**
     * Disables the swap offhand items while the player is in action.
     * <p>
     * Previously, we injected into the vanilla {@link Minecraft#handleKeybinds} method
     * and used this workaround to reset the internal counter for the vanilla swap offhand keybind:
     * <pre>
     * {@code
     * // Called before Minecraft#handleKeybinds is called.
     * KeyMapping swapOffhand = Minecraft.getInstance().options.keySwapOffhand;
     * while (swapOffhand.consumeClick()) {}
     * KeyMapping.set(swapOffhand.getKey(), false);
     * }
     * </pre>
     * <p>
     * However, that approach relied on assumptions and did not support other mods, inputs, or systems.
     * The problem is now solved by injecting into {@link ClientPacketListener#send} and
     * canceling the call when the player is in action and trying to swap offhand items.
     *
     * @see ControlEngine#consumeSwapOffhandKeyClicks
     */
    @SuppressWarnings("JavadocReference")
    @ApiStatus.Internal
    public static boolean shouldDisableSwapHandItems() {
        final LocalPlayerPatch playerpatch = EpicFightCapabilities.ClientModule.getCachedLocalPlayerPatch();
        if (playerpatch == null) {
            return false;
        }
        return playerpatch.getEntityState().inaction() || (!playerpatch.getHoldingItemCapability(InteractionHand.MAIN_HAND).canBePlacedOffhand());
    }

    /// Determines whether the vanilla use-item action should be cancelled.
    ///
    /// Replaces the NeoForge [InputEvent.InteractionKeyMappingTriggered] handler logic for USE action.
    /// Called from [MixinMinecraft] when the player attempts to use an item.
    ///
    /// @return `true` to cancel the vanilla use-item action; `false` to allow it
    @ApiStatus.Internal
    public static boolean shouldCancelVanillaUseItem() {
        final LocalPlayerPatch playerpatch = EpicFightCapabilities.ClientModule.getCachedLocalPlayerPatch();
        if (playerpatch == null) {
            return false;
        }

        // Only cancel when in EpicFight mode and the use key is bound to the same physical input as guard
        if (!playerpatch.isEpicFightMode()) {
            return false;
        }

        if (!InputManager.isBoundToSamePhysicalInput(MinecraftInputAction.USE, EpicFightInputAction.GUARD)) {
            return false;
        }

        // Don't cancel vanilla use when the player is holding a shield.
        // maybeGuard() already skips the guard skill for ShieldItem, so if we also cancel
        // vanilla use here, the player can neither guard nor block — a deadlock.
        // Letting vanilla blocking proceed matches the intent: shields use vanilla blocking,
        // the guard skill is for weapons with guard motions.
        // Same for SkillBookItem: right-clicking it opens the skill book screen, and
        // cancelling use here would leave the book silently dead while in EpicFight mode.
        LocalPlayer player = playerpatch.getOriginal();
        if (ShieldItem.class.isAssignableFrom(player.getMainHandItem().getItem().getClass())
                || ShieldItem.class.isAssignableFrom(player.getOffhandItem().getItem().getClass())
                || player.getMainHandItem().getItem() instanceof SkillBookItem
                || player.getOffhandItem().getItem() instanceof SkillBookItem) {
            return false;
        }

        SkillContainer skillContainer = playerpatch.getSkill(SkillSlots.GUARD);
        boolean canGuard = skillContainer.getSkill() != null && skillContainer.getSkill().canExecute(skillContainer);

        if (!canGuard) {
            return false;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.hitResult == null) {
            return false;
        }

        if (mc.hitResult.getType() == HitResult.Type.MISS) {
            return ClientConfig.canceledVanillaActions.cancelItemUse();
        } else {
            // For entity/block hits, simulate the interaction to decide
            try {
                InteractionResult interactionResult;
                if (mc.hitResult instanceof EntityHitResult entityHitResult) {
                    interactionResult = entityHitResult.getEntity().interact(mc.player, InteractionHand.MAIN_HAND);
                } else if (mc.hitResult instanceof BlockHitResult blockHitResult) {
                    BlockPos blockpos = blockHitResult.getBlockPos();
                    BlockState blockstate = mc.level.getBlockState(blockpos);
                    FakeLevel fakeLevel = FakeLevel.getFakeLevel(mc.level);
                    FakeLevel.FakeClientPlayer fakePlayer = FakeLevel.getFakePlayer(mc.player.getGameProfile());
                    InteractionResult useItemResult = blockstate.useItemOn(mc.player.getItemInHand(InteractionHand.MAIN_HAND), fakeLevel, fakePlayer, InteractionHand.MAIN_HAND, blockHitResult).result();
                    if (useItemResult != InteractionResult.PASS) {
                        interactionResult = useItemResult;
                    } else {
                        interactionResult = blockstate.useWithoutItem(fakeLevel, fakePlayer, blockHitResult);
                    }
                } else {
                    return false;
                }

                if (interactionResult != InteractionResult.PASS) {
                    return ClientConfig.canceledVanillaActions.cancelInteraction();
                } else {
                    return ClientConfig.canceledVanillaActions.cancelItemUse();
                }
            } catch (Throwable e) {
                return false;
            }
        }
    }

    /// Determines whether the vanilla attack action should be cancelled when targeting a living entity in vanilla mode.
    ///
    /// Replaces the NeoForge [InputEvent.InteractionKeyMappingTriggered] handler logic for ATTACK action.
    /// Called from [MixinMinecraft] when the player attempts to attack.
    ///
    /// @return `true` to cancel the vanilla attack action; `false` to allow it
    @ApiStatus.Internal
    public static boolean shouldCancelVanillaAttackOnEntity() {
        final LocalPlayerPatch playerpatch = EpicFightCapabilities.ClientModule.getCachedLocalPlayerPatch();
        if (playerpatch == null) {
            return false;
        }

        // Only applies in vanilla mode
        if (!playerpatch.isVanillaMode()) {
            return false;
        }

        if (EpicFightGameRules.ALLOW_VANILLA_MELEE.getRuleValue(playerpatch.getOriginal().level())) {
            return false;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.hitResult instanceof EntityHitResult entityHitResult) {
            return entityHitResult.getEntity() instanceof LivingEntity || entityHitResult.getEntity() instanceof net.minecraft.world.entity.boss.EnderDragonPart;
        }

        return false;
    }

    /// Determines whether the vanilla attack action should be cancelled when targeting a block with a combat-categorized item.
    ///
    /// Replaces the NeoForge [InputEvent.InteractionKeyMappingTriggered] handler logic for ATTACK_DESTROY + BLOCK hit.
    /// Called from [MixinMinecraft] when the player attempts to attack.
    ///
    /// @return `true` to cancel the vanilla attack action; `false` to allow it
    @ApiStatus.Internal
    public static boolean shouldCancelVanillaAttackOnBlock() {
        final LocalPlayerPatch playerpatch = EpicFightCapabilities.ClientModule.getCachedLocalPlayerPatch();
        if (playerpatch == null) {
            return false;
        }

        if (!InputManager.isBoundToSamePhysicalInput(EpicFightInputAction.ATTACK, MinecraftInputAction.ATTACK_DESTROY)) {
            return false;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.hitResult == null || mc.hitResult.getType() != HitResult.Type.BLOCK) {
            return false;
        }

        ItemStack combatItemForSwing;
        if (ClientConfig.combatCategorizedItems.contains(playerpatch.getOriginal().getMainHandItem().getItem())) {
            combatItemForSwing = playerpatch.getOriginal().getMainHandItem();
        } else if (ClientConfig.combatCategorizedItems.contains(playerpatch.getOriginal().getOffhandItem().getItem())) {
            combatItemForSwing = playerpatch.getOriginal().getOffhandItem();
        } else {
            return false;
        }

        BlockHitResult blockHitResult = (BlockHitResult) mc.hitResult;
        BlockPos bp = blockHitResult.getBlockPos();
        BlockState bs = mc.level.getBlockState(bp);

        return !combatItemForSwing.getItem().canAttackBlock(bs, playerpatch.getOriginal().level(), bp, playerpatch.getOriginal()) || combatItemForSwing.getDestroySpeed(bs) <= 1.0F;
    }


    /**
     * Previously used to disable the vanilla attack key so the player
     * can't attack entities or break grass when in Epic Fight mode, but that is now handled by
     * {@link yesman.epicfight.mixin.client.MixinMinecraft#onStartVanillaAttack} and
     * {@link yesman.epicfight.mixin.client.MixinMinecraft#onContinueVanillaAttack}.
     * <p>
     * This method now only decrements the internal counter of the vanilla {@link KeyMapping#clickCount}
     * to prevent potential conflicts with other mods. It acts as a safety measure;
     * removing it should no longer cause issues.
     * <p>
     * This method does not rely on {@link InputManager} because it operates solely on
     * the vanilla {@link KeyMapping} behavior.
     * @see ControlEngine#shouldDisableVanillaAttack
     */
    @SuppressWarnings("JavadocReference")
    private static void consumeVanillaAttackKeyClicks() {
        makeUnpressed(MinecraftInputAction.ATTACK_DESTROY.keyMapping());
    }

    /// Previously used to temporarily disable the vanilla swap-offhand key while the player
    /// was performing an action or attacking, but this is now handled by
    /// [yesman.epicfight.mixin.client.MixinClientCommonPacketListenerImpl#onBeforeSendPacket].
    ///
    /// This method now only decrements the internal counter of the vanilla [KeyMapping#clickCount]
    /// to prevent potential conflicts with other mods.
    ///
    /// It serves as a safety measure; removing it should no longer cause any issues.
    ///
    /// This method does not rely on [InputManager] because it operates solely on
    /// the vanilla [KeyMapping] behavior.
    ///
    /// @see ControlEngine#shouldDisableSwapHandItems
    @SuppressWarnings("JavadocReference")
    private static void consumeSwapOffhandKeyClicks() {
        makeUnpressed(MinecraftInputAction.SWAP_OFF_HAND.keyMapping());
    }

    /// Disables hotbar slot key presses (keyboard only).
    ///
    /// This feature is strictly for keyboards and will not support controllers,
    /// as controllers have limited buttons.
    ///
    /// Keyboard users can switch slots via number keys or the mouse wheel.
    /// Controller users can only switch using forward/backward buttons.
    ///
    /// @see ControlEngine#isHotbarCyclingDisabled
    private static void disableHotbarSlotPresses() {
        final KeyMapping[] hotbarSlots = Minecraft.getInstance().options.keyHotbarSlots;
        for (int i = 0; i < 9; ++i) {
            final KeyMapping hotbarSlot = hotbarSlots[i];
            makeUnpressed(hotbarSlot);
        }
    }

    /// Previously used to temporarily disable the vanilla item drop key while the player
    /// was performing an action or attacking, but this is now handled by
    /// [yesman.epicfight.mixin.client.MixinLocalPlayer#onDrop].
    ///
    /// This method now only decrements the internal counter of the vanilla {@link KeyMapping#clickCount}
    /// to prevent potential conflicts with other mods.
    ///
    /// It serves as a safety measure; removing it should no longer cause any issues.
    @SuppressWarnings("JavadocReference")
    private static void consumeDropKeyClicks() {
        makeUnpressed(MinecraftInputAction.DROP.keyMapping());
    }

    /// Maps a [KeyMapping] to its corresponding input action, if defined.
    ///
    /// Each [InputAction] enum constant has an associated [KeyMapping],
    /// but not every [KeyMapping] corresponds to an [EpicFightInputAction],
    /// so this may return `null`.
    /// Using [KeyMapping] directly does not support controllers.
    ///
    /// Ideally, this workaround should not exist.
    /// The code should depend on [EpicFightInputAction] directly
    /// instead of storing [KeyMapping] instances.
    ///
    /// However, since some classes and Epic Fight addons still rely on:
    ///
    /// - [HoldableSkill#getKeyMapping]
    /// - [ControlEngine#currentHoldingKey]
    /// - [ControlEngine#reservedKey]
    ///
    /// this method remains temporarily for backward compatibility. Future updates should refactor these dependencies
    /// to remove reliance on [KeyMapping], allowing this method to be fully removed.
    ///
    /// Sometimes, it makes sense to use this method, for example, if you're using an event such as [Object.InteractionKeyMappingTriggered],
    /// which provides only a [KeyMapping].
    private static @Nullable InputAction mapKeyMappingToAction(@NotNull KeyMapping keyMapping) {
        return InputAction.fromKeyMapping(keyMapping);
    }

    /// Checks if the specified input action is currently being held.
    ///
    /// @param other the input action to check
    /// @return `true` if the given action is currently held; otherwise `false`
    /// @see ControlEngine#mapKeyMappingToAction
    private boolean isCurrentHoldingAction(@NotNull InputAction other) {
        if (currentHoldingKey == null) {
            return false;
        }
        final InputAction currentHoldingAction = mapKeyMappingToAction(currentHoldingKey);
        if (currentHoldingAction == null) {
            // Fallback for legacy or custom key mappings.
            // This is IMPORTANT to prevent addon breakage; this allows custom keybinds from other mods,
            // but controller inputs will not support those custom keybinds.
            return currentHoldingKey == other.keyMapping();
        }
        return other == currentHoldingAction;
    }

    private boolean isCurrentHoldingActionActive() {
        if (currentHoldingKey == null) {
            return false;
        }
        final InputAction currentHoldingAction = mapKeyMappingToAction(currentHoldingKey);
        if (currentHoldingAction == null) {
            // Fallback for legacy or custom key mappings.
            // This is IMPORTANT to prevent addon breakage; this allows custom keybinds from other mods,
            // but controller inputs will not support those custom keybinds.
            return isKeyDown(currentHoldingKey);
        }
        // On Fabric 1.21.1, KeyMapping#isDown can briefly report false for keyboard keys
        // that share a physical binding with a vanilla key mapping (e.g. MOVER_SKILL and
        // JUMP both bound to Space). This causes the chargeable-skill holding loop to
        // think the player released the key, prematurely ending the charge. Fall back to
        // the physical key state, which is reliable and safe here because this method is
        // only called during in-game ticks (no screen open).
        if (InputManager.isActionActive(currentHoldingAction)) {
            return true;
        }
        return InputManager.isActionPhysicallyActive(currentHoldingAction);
    }

    /// Determines whether hotbar cycling should be disabled.
    ///
    /// Used internally in [Object.MouseScrollingEvent] and
    /// [yesman.epicfight.mixin.client.MixinInventory].
    ///
    /// Cancelling the mouse scroll event disables cycling for vanilla mouse input, but other input
    /// systems (e.g., controllers) still call {@link Inventory#swapPaint}, so we
    /// cancel those calls as well.
    ///
    /// This ensures universal behavior while
    /// maximizing compatibility.
    ///
    /// @return `true` if hotbar item cycling should be disabled; `false` otherwise.
    @ApiStatus.Internal
    public static boolean isHotbarCyclingDisabled() {
        final Minecraft minecraft = Minecraft.getInstance();
        final LocalPlayerPatch localPlayerPatch = EpicFightCapabilities.ClientModule.getCachedLocalPlayerPatch();
        return minecraft.player != null && localPlayerPatch != null && !localPlayerPatch.getEntityState().canSwitchHoldingItem() && minecraft.screen == null;
    }

     /// Checks whether the player is blocked from switching or dropping the held item.
     ///
     /// @return `true` if switching or dropping is blocked, `false` otherwise
    public boolean isSwitchOrDropBlocked() {
        return !this.playerpatch.getEntityState().canSwitchHoldingItem() || this.hotbarLocked;
    }

    public boolean moverToggling() {
		return this.moverPressToggle;
	}

	public boolean sneakToggling() {
		return this.sneakPressToggle;
	}

	public boolean attackToggling() {
		return this.attackLightPressToggle;
	}

	public boolean weaponInnateToggling() {
		return this.weaponInnatePressToggle;
	}

	/******************
	 * Event listeners
	 ******************/
	// Handled by MixinInventory.onCycleHotbarSlot (cancels swapPaint for all input methods)
	// NeoForge's InputEvent.MouseScrollingEvent was an extra safety for mouse input only
	private void epicfight$mouseScrollEvent(Object event) {
	}

	// Handled by MixinLocalPlayerInput.epicfight$onMovementInputUpdate
	private void epicfight$moveObject(Object event) {
	}

	// Handled by gameEventBus() callback — ClientTickEvents.END_CLIENT_TICK
	private void epicfight$clientTickEndEvent(Object event) {
	}

	// Handled by MixinMinecraft.epicfight$startUseItem and MixinMinecraft.onStartVanillaAttack
	private void epicfight$interactionKeyMappingTriggered(Object event) {
	}

	// Handled by MixinLivingEntityJump.epicfight$onJump
	private void epicfight$livingJumpEvent(Object event) {
	}

	/**********************
	 * Event listeners end
	 **********************/
	@Override
	public void gameEventBus(Object gameEventBus) {
		// Client tick end — flush queued packets (was epicfight$clientTickEndEvent / ClientTickEvent.Post)
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (this.player == null) {
				return;
			}

			this.packetsToSend.forEach(EpicFightNetworkManager::sendToServer);
			this.packetsToSend.clear();
		});
	}

	@Override
	public void modEventBus(Object modEventBus) {
	}
}