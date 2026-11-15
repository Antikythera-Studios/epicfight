package yesman.epicfight.skill.identity;
import yesman.epicfight.EpicFight;

import com.google.common.collect.Maps;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.client.event.EpicFightClientEventHooks;
import yesman.epicfight.api.client.event.types.hud.TickTargetIndicatorEvent;
import yesman.epicfight.api.event.EntityEventListener;
import yesman.epicfight.api.event.EpicFightEventHooks;
import yesman.epicfight.api.utils.AttackResult.ResultType;
import yesman.epicfight.api.utils.side.ClientOnly;
import yesman.epicfight.client.events.engine.ControlEngine;
import yesman.epicfight.client.gui.BattleModeGui;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.main.EpicFightMod;
import yesman.epicfight.registry.entries.EpicFightSkillDataKeys;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillBuilder;
import yesman.epicfight.skill.SkillCategories;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.capabilities.item.CapabilityItem.WeaponCategories;
import yesman.epicfight.world.capabilities.item.WeaponCapability;
import yesman.epicfight.world.capabilities.item.WeaponCategory;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

public class RevelationSkill extends Skill {
    public static RevelationSkill.Builder createRevelationSkillBuilder() {
        return new Builder(RevelationSkill::new)
            .addMotion(WeaponCategories.LONGSWORD, (item, player) -> Animations.REVELATION_TWOHAND)
            .addMotion(WeaponCategories.GREATSWORD, (item, player) -> Animations.REVELATION_TWOHAND)
            .addMotion(WeaponCategories.TACHI, (item, player) -> Animations.REVELATION_TWOHAND)
            .setCategory(SkillCategories.IDENTITY)
            .setActivateType(ActivateType.DURATION)
            .setResource(Resource.NONE);
    }

    public static class Builder extends SkillBuilder<RevelationSkill.Builder> {
        protected final Map<WeaponCategory, BiFunction<CapabilityItem, PlayerPatch<?>, AnimationAccessor<? extends StaticAnimation>>> motions = Maps.newHashMap();

        public Builder(Function<RevelationSkill.Builder, ? extends Skill> constructor) {
            super(constructor);
        }

        public Builder addMotion(WeaponCategory weaponCategory, BiFunction<CapabilityItem, PlayerPatch<?>, AnimationAccessor<? extends StaticAnimation>> function) {
            this.motions.put(weaponCategory, function);
            return this;
        }
    }

    protected final Map<WeaponCategory, BiFunction<CapabilityItem, PlayerPatch<?>, AnimationAccessor<? extends StaticAnimation>>> motions;
    protected final Map<EntityType<?>, Integer> maxRevelationStacks = Maps.newHashMap();
    protected int blockStack;
    protected int parryStack;
    protected int dodgeStack;
    protected int defaultRevelationStacks;

    public RevelationSkill(Builder builder) {
        super(builder);

        this.motions = builder.motions;
    }

    @Override
    public void loadDatapackParameters(CompoundTag parameters) {
        super.loadDatapackParameters(parameters);

        this.maxRevelationStacks.clear();
        this.blockStack = parameters.getInt("block_stacks");
        this.parryStack = parameters.getInt("parry_stacks");
        this.dodgeStack = parameters.getInt("dodge_stacks");
        this.defaultRevelationStacks = parameters.getInt("default_revelation_stacks");

        CompoundTag maxStacks = parameters.getCompound("max_revelations");

        for (String registryName : maxStacks.getAllKeys()) {
            EntityType<?> entityType = EntityType.byString(registryName).orElse(null);

            if (entityType != null) {
                this.maxRevelationStacks.put(entityType, maxStacks.getInt(registryName));
            } else {
                EpicFight.LOGGER.warn("Revelation registry error: no entity type named {}", registryName);
            }
        }
    }

    @Override
    public void onInitiate(SkillContainer skillContainer, EntityEventListener eventListener) {
        super.onInitiate(skillContainer, eventListener);

        eventListener.registerEvent(
            EpicFightEventHooks.Player.CAST_SKILL,
            event -> {
                if (skillContainer.getExecutor().isLogicalClient()) {
                    Skill skill = event.getSkillContainer().getSkill();

                    if (skill.getCategory() != SkillCategories.WEAPON_INNATE) {
                        return;
                    }

                    if (skillContainer.getExecutor().getTarget() != null) {
                        EpicFightCapabilities.getUnparameterizedEntityPatch(skillContainer.getExecutor().getTarget(), LivingEntityPatch.class).ifPresent(entitypatch -> {
                            if (this.isActivated(skillContainer)) {
                                if (skillContainer.sendCastRequest(skillContainer.getClientExecutor(), ControlEngine.getInstance()).isExecutable()) {
                                    skillContainer.setDuration(0);
                                    event.cancel();
                                }
                            }
                        });
                    }
                }
            },
            this
        );

        eventListener.registerEvent(
            EpicFightEventHooks.Player.SET_TARGET,
            event -> skillContainer.getDataManager().setDataSync(EpicFightSkillDataKeys.STACKS, 0),
            this
        );

        eventListener.registerEvent(
            EpicFightEventHooks.Entity.ON_DODGE,
            event -> {
                LivingEntity target = skillContainer.getExecutor().getTarget();

                if (target != null && target.is(event.getDamageSource().getDirectEntity())) {
                    this.checkStackAndActivate(skillContainer, target, skillContainer.getDataManager().getDataValue(EpicFightSkillDataKeys.STACKS), this.dodgeStack);
                }
            },
            this
        );

        eventListener.registerContextAwareEvent(
            EpicFightEventHooks.Entity.TAKE_DAMAGE_INCOME,
            (event, eventContext) -> {
                if (event.getResult() == ResultType.BLOCKED) {
                    LivingEntity target = skillContainer.getExecutor().getTarget();

                    if (target != null && target.is(event.getDamageSource().getDirectEntity())) {
                        int stacks = event.isParried() ? this.parryStack : this.blockStack;

                        this.checkStackAndActivate(skillContainer, target, skillContainer.getDataManager().getDataValue(EpicFightSkillDataKeys.STACKS), stacks);
                    }
                }
            },
            this
        );

        eventListener.registerEvent(
            EpicFightClientEventHooks.HUD.TARGET_INDICATOR_TICK,
            event -> {
                if (this.isActivated(skillContainer)) {
                    event.setType(TickTargetIndicatorEvent.Type.FLASH);
                }
            },
            this
        );
    }

	@Override
	public void executeOnServer(SkillContainer container, CompoundTag arguments) {
		super.executeOnServer(container, arguments);
		
		// Source the revelation animation map from the active combat cap so a longsword in
		// the offhand resolves the correct category-specific motion.
		CapabilityItem holdingItem = container.getExecutor().getPrimaryItemCapability();
		AnimationAccessor<? extends StaticAnimation> animation = this.motions.containsKey(holdingItem.getWeaponCategory()) ? this.motions.get(holdingItem.getWeaponCategory()).apply(holdingItem, container.getExecutor()) : Animations.REVELATION_ONEHAND;
		if (holdingItem instanceof WeaponCapability weaponCap)
        {
            animation = weaponCap.getCurrentSet(container.getServerExecutor()).getRevelation() != null ? weaponCap.getCurrentSet(container.getServerExecutor()).getRevelation() : animation;
        }
		container.getExecutor().playAnimationSynchronized(animation, 0.0F);
	}

	public void checkStackAndActivate(SkillContainer container, LivingEntity target, int stacks, int addStacks) {
		int maxStackSize = this.maxRevelationStacks.getOrDefault(target.getType(), this.defaultRevelationStacks);
		int plusStack = stacks + addStacks;
		
		if (plusStack < maxStackSize) {
			container.getDataManager().setDataSync(EpicFightSkillDataKeys.STACKS, plusStack);
		} else {
			if (!this.isActivated(container)) {
				this.setDurationSynchronize(container, this.maxDuration);
			}
			
			container.getDataManager().setDataSync(EpicFightSkillDataKeys.STACKS, 0);
		}
	}

	@ClientOnly @Override
	public boolean shouldDraw(SkillContainer container) {
		return container.getExecutor().getTarget() != null;
	}

    @ClientOnly @Override
	public void drawOnGui(BattleModeGui gui, SkillContainer container, GuiGraphics guiGraphics, float x, float y, float partialTick) {
		guiGraphics.blit(this.getSkillTexture(), (int)x, (int)y, 24, 24, 0, 0, 1, 1, 1, 1);
		int stacks = container.getRemainDuration() > 0 ? 0 : this.maxRevelationStacks.getOrDefault(container.getExecutor().getTarget().getType(), this.defaultRevelationStacks) - container.getDataManager().getDataValue(EpicFightSkillDataKeys.STACKS);
		guiGraphics.drawString(gui.getFont(), String.format("%d", stacks), (int)(x + 18), (int)(y + 14), 16777215, true);
	}
}