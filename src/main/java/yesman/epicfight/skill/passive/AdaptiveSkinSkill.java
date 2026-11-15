package yesman.epicfight.skill.passive;

import com.google.common.collect.ImmutableMap;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import yesman.epicfight.api.event.EntityEventListener;
import yesman.epicfight.api.event.EpicFightEventHooks;
import yesman.epicfight.api.utils.math.ValueModifier;
import yesman.epicfight.api.utils.math.Vec3f;
import yesman.epicfight.api.utils.side.ClientOnly;
import yesman.epicfight.client.gui.BattleModeGui;
import yesman.epicfight.client.renderer.EpicFightRenderTypes;
import yesman.epicfight.registry.entries.EpicFightSkillDataKeys;
import yesman.epicfight.registry.entries.EpicFightSounds;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillBuilder;
import yesman.epicfight.skill.SkillCategories;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.world.capabilities.entitypatch.EntityDecorations.DecorationOverlay;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.damagesource.EpicFightDamageTypeTags;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class AdaptiveSkinSkill extends PassiveSkill {
    public static class Builder extends SkillBuilder<AdaptiveSkinSkill.Builder> {
        protected final Map<TagKey<DamageType>, Vec3f> protectableDamageTypeTags = new LinkedHashMap<> ();

        public Builder(Function<AdaptiveSkinSkill.Builder, ? extends Skill> constructor) {
            super(constructor);
        }

        public Builder addProtectableDamageTypeTags(Map<TagKey<DamageType>, Vec3f> tags) {
            this.protectableDamageTypeTags.putAll(tags);
            return this;
        }
    }

    public static AdaptiveSkinSkill.Builder createAdaptiveSkinBuilder() {
        return new AdaptiveSkinSkill.Builder(AdaptiveSkinSkill::new)
                .addProtectableDamageTypeTags(
                    ImmutableMap.of(
                        EpicFightDamageTypeTags.IS_MELEE, new Vec3f(227 / 255.0F, 127 / 255.0F, 127 / 255.0F),
                        DamageTypeTags.IS_PROJECTILE, new Vec3f(102 / 255.0F, 197 / 255.0F, 255 / 255.0F),
                        DamageTypeTags.IS_FIRE, new Vec3f(229 / 255.0F, 143 / 255.0F, 66 / 255.0F),
                        EpicFightDamageTypeTags.IS_MAGIC, new Vec3f(226 / 255.0F, 154 / 255.0F, 234 / 255.0F),
                        DamageTypeTags.IS_EXPLOSION, new Vec3f(207 / 255.0F, 205 / 255.0F, 120 / 255.0F)
                    )
                )
                .setCategory(SkillCategories.PASSIVE)
                .setResource(Resource.NONE);
    }

    private final Map<TagKey<DamageType>, Vec3f> protectableDamageTypeTags;
    private float damageResistance;
    private int maxResistanceStack;

    public AdaptiveSkinSkill(AdaptiveSkinSkill.Builder builder) {
        super(builder);

        this.protectableDamageTypeTags = Collections.unmodifiableMap(builder.protectableDamageTypeTags);
    }

    @Override
    public void loadDatapackParameters(CompoundTag parameters) {
        super.loadDatapackParameters(parameters);

        this.damageResistance = parameters.getFloat("damage_resistance");
        this.maxResistanceStack = parameters.getInt("max_resistance_stack");
    }

    @Override
    public void onInitiate(SkillContainer container, EntityEventListener eventListener) {
        super.onInitiate(container, eventListener);

        eventListener.registerEvent(
            EpicFightEventHooks.Entity.TAKE_DAMAGE_PRE,
            event -> {
                TagKey<DamageType> currentResisting = container.getDataManager().getDataValue(EpicFightSkillDataKeys.RESISTING_DAMAGE_TYPE);
                int stacks = container.getDataManager().getDataValueOptional(EpicFightSkillDataKeys.STACKS).orElse(0);

                if (event.getDamageSource().is(currentResisting)) {
                    event.attachValueModifier(ValueModifier.multiplier(1.0F - this.damageResistance * stacks));

                    if (stacks < this.maxResistanceStack) {
                        container.getExecutor().playSound(EpicFightSounds.ADAPTIVE_SKIN_INCREASE, 1.0F, 0.0F, 0.0F);
                        container.getDataManager().setDataSyncF(EpicFightSkillDataKeys.STACKS, v -> v + 1);
                    }

                    container.getDataManager().setDataSync(EpicFightSkillDataKeys.TICK_RECORD, container.getExecutor().getOriginal().tickCount);
                } else {
                    if (stacks <= 1) {
                        for (TagKey<DamageType> protectableTag : this.protectableDamageTypeTags.keySet()) {
                            if (event.getDamageSource().is(protectableTag)) {
                                container.getDataManager().setDataSync(EpicFightSkillDataKeys.STACKS, 1);
                                container.getDataManager().setDataSync(EpicFightSkillDataKeys.RESISTING_DAMAGE_TYPE, protectableTag);
                                container.getDataManager().setDataSync(EpicFightSkillDataKeys.TICK_RECORD, container.getExecutor().getOriginal().tickCount);

                                if (stacks == 0) {
                                    container.getExecutor().playSound(EpicFightSounds.ADAPTIVE_SKIN_INCREASE, 1.0F, 0.0F, 0.0F);
                                } else {
                                    container.getExecutor().playSound(EpicFightSounds.ADAPTIVE_SKIN_DECREASE, 1.0F, 0.0F, 0.0F);
                                }

                                break;
                            }
                        }
                    } else {
                        boolean adaptableType = false;

                        for (TagKey<DamageType> protectableTag : this.protectableDamageTypeTags.keySet()) {
                            if (event.getDamageSource().is(protectableTag)) {
                                adaptableType = true;
                                break;
                            }
                        }

                        if (adaptableType) {
                            container.getExecutor().playSound(EpicFightSounds.ADAPTIVE_SKIN_DECREASE.get(), 1.0F, 0.0F, 0.0F);
                            container.getDataManager().setDataSyncF(EpicFightSkillDataKeys.STACKS, v -> v - 1);
                        }
                    }
                }
            },
            this
        );
    }

    @Override @ClientOnly
    public void onInitiateClient(SkillContainer container) {
        container.getExecutor().getEntityDecorations().addDecorationOverlay(this, new DecorationOverlay() {
            @Override
            public RenderType getRenderType() {
                TagKey<DamageType> resistingDamageTypeTagKey = container.getDataManager().getDataValue(EpicFightSkillDataKeys.RESISTING_DAMAGE_TYPE);
                Vec3f color = AdaptiveSkinSkill.this.getGlintColor(resistingDamageTypeTagKey);
                return EpicFightRenderTypes.coloredGlintWorldRendertype(container.getExecutor().getOriginal(), color.x, color.y, color.z);
            }

            @Override
            public boolean shouldRender() {
                return container.getDataManager().getDataValueOptional(EpicFightSkillDataKeys.RESISTING_DAMAGE_TYPE).orElse(EpicFightDamageTypeTags.NONE) != EpicFightDamageTypeTags.NONE;
            }
        });

        container.getExecutor().getEntityDecorations().addColorModifier(this, (resultColor, partialTick) -> {
            TagKey<DamageType> resistingDamageTypeTagKey = container.getDataManager().getDataValue(EpicFightSkillDataKeys.RESISTING_DAMAGE_TYPE);

            if (!EpicFightDamageTypeTags.NONE.equals(resistingDamageTypeTagKey)) {
                Vec3f color = AdaptiveSkinSkill.this.getGlintColor(resistingDamageTypeTagKey);
                if (color != null) {
                    resultColor.x = color.x;
                    resultColor.y = color.y;
                    resultColor.z = color.z;
                }
            }
        });
    }

    @Override
    public void updateContainer(SkillContainer container) {
        super.updateContainer(container);

        TagKey<DamageType> resistingDamageTypeTag = container.getDataManager().getDataValue(EpicFightSkillDataKeys.RESISTING_DAMAGE_TYPE);

        if (!EpicFightDamageTypeTags.NONE.equals(resistingDamageTypeTag)) {
            if (container.getExecutor().getOriginal().tickCount - container.getDataManager().getDataValueOptional(EpicFightSkillDataKeys.TICK_RECORD).orElse(0) > 300) {
                container.getDataManager().setDataSync(EpicFightSkillDataKeys.RESISTING_DAMAGE_TYPE, EpicFightDamageTypeTags.NONE);
                container.getDataManager().setDataSync(EpicFightSkillDataKeys.STACKS, 0);
            }
        }
    }

    private Vec3f getGlintColor(TagKey<DamageType> tagKey) {
        return this.protectableDamageTypeTags.get(tagKey);
    }

    @Override @ClientOnly
    public boolean shouldDraw(SkillContainer container) {
        TagKey<DamageType> resistingDamageTypeTag = container.getDataManager().getDataValue(EpicFightSkillDataKeys.RESISTING_DAMAGE_TYPE);
        return !EpicFightDamageTypeTags.NONE.equals(resistingDamageTypeTag) && this.protectableDamageTypeTags.containsKey(resistingDamageTypeTag);
    }

    @Override @ClientOnly
    public void drawOnGui(BattleModeGui gui, SkillContainer container, GuiGraphics guiGraphics, float x, float y, float partialTick) {
        Vec3f color = this.protectableDamageTypeTags.get(container.getDataManager().getDataValue(EpicFightSkillDataKeys.RESISTING_DAMAGE_TYPE));
        guiGraphics.innerBlit(this.getSkillTexture(), (int)x, (int)x + 24, (int)y, (int)y + 24, 0, 0.0F, 1.0F, 0.0F, 1.0F, color.x, color.y, color.z, 1.0F);
        int stacks = container.getDataManager().getDataValue(EpicFightSkillDataKeys.STACKS);

        if (stacks > 1) {
            guiGraphics.drawString(gui.getFont(), String.valueOf(stacks), (int)(x + 18), (int)(y + 16), 16777215, true);
        }

        int lastHitTick = container.getDataManager().getDataValueOptional(EpicFightSkillDataKeys.TICK_RECORD).orElse(0);

        if (container.getExecutor().getOriginal().tickCount - lastHitTick > 200) {
            int remainseconds = 1 + (100 - (container.getExecutor().getOriginal().tickCount - lastHitTick - 200)) / 20;
            guiGraphics.drawString(gui.getFont(), String.valueOf(remainseconds), (int)(x + 8), (int)(y + 8), 16777215, true);
        }
    }

    @Override @ClientOnly
    public List<Object> getTooltipArgsOfScreen(List<Object> list) {
        list.add(ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(this.damageResistance * 100.0F));
        list.add(this.maxResistanceStack);

        StringBuilder sb = new StringBuilder();

        for (TagKey<DamageType> tag : this.protectableDamageTypeTags.keySet()) {
            String tagKey = String.format("tag.%s.%s.%s", tag.registry().location().getPath(), tag.location().getNamespace(), tag.location().getPath());
            sb.append("- ").append(Component.translatable(tagKey).getString()).append("\n");
        }

        list.add(sb.toString());

        return list;
    }
}
