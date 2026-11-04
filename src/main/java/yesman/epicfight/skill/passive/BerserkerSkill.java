package yesman.epicfight.skill.passive;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import org.joml.Vector4f;
import yesman.epicfight.api.event.EntityEventListener;
import yesman.epicfight.api.event.EpicFightEventHooks;
import yesman.epicfight.api.utils.math.ValueModifier;
import yesman.epicfight.api.utils.side.ClientOnly;
import yesman.epicfight.client.gui.BattleModeGui;
import yesman.epicfight.skill.SkillBuilder;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.world.capabilities.entitypatch.EntityDecorations.DecorationOverlay;

import java.util.List;

public class BerserkerSkill extends PassiveSkill {
    private float speedBonus;
    private float damageBonus;

    public BerserkerSkill(SkillBuilder<?> builder) {
        super(builder);
    }

    @Override
    public void loadDatapackParameters(CompoundTag parameters) {
        super.loadDatapackParameters(parameters);
        this.speedBonus = parameters.getFloat("speed_bonus");
        this.damageBonus = parameters.getFloat("damage_bonus");
    }

    @Override @ClientOnly
    public void onInitiateClient(SkillContainer container) {
        Player player = container.getExecutor().getOriginal();

        container.getExecutor().getEntityDecorations().addDecorationOverlay(this, new DecorationOverlay() {
            @Override
            public Vector4f color(float partialTick) {
                float alpha = Mth.clampedLerp(0.0F, 0.42F, 1.0F - (player.getHealth() / player.getMaxHealth()));
                return new Vector4f(0.66F, 0.06F, 0.07F, alpha);
            }
        });

        container.getExecutor().getEntityDecorations().addParticleGenerator(this, () -> {
            float healthRatio = player.getHealth() / player.getMaxHealth();
            RandomSource random = player.getRandom();
            float chance = Mth.clampedLerp(0.0F, 0.04F, (1.0F - healthRatio) - 0.2F);

            for (int i = 0; i < 4; i++) {
                if (random.nextFloat() < chance) {
                    player.level().addParticle(
                        ParticleTypes.POOF,
                        player.getX() + random.nextGaussian() * 0.4F,
                        player.getY() + player.getBbHeight() * 0.5D + random.nextGaussian() * 0.6F,
                        player.getZ() + random.nextGaussian() * 0.4F,
                        0.0F,
                        0.2F,
                        0.0F
                    );
                }
            }

            return false;
        });
    }

    @Override
    public void onInitiate(SkillContainer container, EntityEventListener eventListener) {
        super.onInitiate(container, eventListener);

        eventListener.registerEvent(
            EpicFightEventHooks.Entity.MODIFY_ATTACK_SPEED,
            event -> {
                Player player = container.getExecutor().getOriginal();
                float health = player.getHealth();
                float maxHealth = player.getMaxHealth();
                float lostHealthPercentage = (maxHealth - health) / maxHealth;
                lostHealthPercentage = (float)Math.floor(lostHealthPercentage * 100.0F) * 0.01F * this.speedBonus;
                float attackSpeed = event.getAttackSpeed();
                event.setAttackSpeed(Math.min(5.0F, attackSpeed * (1.0F + lostHealthPercentage)));
            },
            this
        );

        eventListener.registerEvent(
            EpicFightEventHooks.Entity.MODIFY_ATTACK_DAMAGE,
            event -> {
                Player player = container.getExecutor().getOriginal();
                float health = player.getHealth();
                float maxHealth = player.getMaxHealth();
                float lostHealthPercentage = (maxHealth - health) / maxHealth;
                lostHealthPercentage = (float)Math.floor(lostHealthPercentage * 100.0F) * 0.01F * this.damageBonus;
                event.attachValueModifier(ValueModifier.multiplier(1.0F + lostHealthPercentage));
            },
            this
        );
    }

    @Override @ClientOnly
    public boolean shouldDraw(SkillContainer container) {
        Player player = container.getExecutor().getOriginal();
        float health = player.getHealth();
        float maxHealth = player.getMaxHealth();
        return (maxHealth - health) > 0.0F;
    }

    @Override @ClientOnly
    public void drawOnGui(BattleModeGui gui, SkillContainer container, GuiGraphics guiGraphics, float x, float y, float partialTick) {
        guiGraphics.blit(this.getSkillTexture(), (int)x, (int)y, 24, 24, 0, 0, 1, 1, 1, 1);
        Player player = container.getExecutor().getOriginal();
        float health = player.getHealth();
        float maxHealth = player.getMaxHealth();
        float lostHealthPercentage = (maxHealth - health) / maxHealth;
        lostHealthPercentage = (float)Math.floor(lostHealthPercentage * 100.0F);
        guiGraphics.drawString(gui.getFont(), String.format("%.0f%%", lostHealthPercentage), (int)(x + 4), (int)(y + 6), 16777215, true);
    }

    @Override @ClientOnly
    public List<Object> getTooltipArgsOfScreen(List<Object> list) {
        list.add(String.format("%.1f", this.speedBonus));
        list.add(String.format("%.1f", this.damageBonus));

        return list;
    }
}