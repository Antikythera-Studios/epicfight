package yesman.epicfight.client.gui;

import com.mojang.blaze3d.platform.GlStateManager.DestFactor;
import com.mojang.blaze3d.platform.GlStateManager.SourceFactor;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import yesman.epicfight.api.utils.math.Vec2i;
import yesman.epicfight.client.gui.ScreenCalculations.AlignDirection;
import yesman.epicfight.client.gui.ScreenCalculations.HorizontalBasis;
import yesman.epicfight.client.gui.ScreenCalculations.VerticalBasis;
import yesman.epicfight.client.gui.screen.config.HUDLocationsScreen;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;
import yesman.epicfight.config.ClientConfig;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.SkillSlot;
import yesman.epicfight.skill.SkillSlots;
import yesman.epicfight.skill.modules.ChargeableSkill;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;

import java.util.ArrayList;
import java.util.List;

public class BattleModeGui {
	private final List<SkillContainer> skillIcons = new ArrayList<> ();
	private Minecraft minecraft;
	private boolean isVisible;
	private int slidingO;
	private int sliding;
	private float staminaO;
	private float stamina;
	
	public BattleModeGui(Minecraft minecraft) {
		this.sliding = 7;
		this.isVisible = false;
		this.minecraft = minecraft;
	}
	
	public void tick(LocalPlayerPatch playerpatch) {
		this.staminaO = this.stamina;
		this.stamina = playerpatch.getStamina();
		
		this.slidingO = this.sliding;
		
		if (this.sliding > 6) {
			return;
		} else if (this.sliding > 0) {
			if (this.isVisible) {
				--this.sliding;
			} else {
				++this.sliding;
			}
		}
	}
	
	public void renderStaminaBar(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
		if (Minecraft.getInstance().screen instanceof HUDLocationsScreen) {
			return;
		}
		
		LocalPlayerPatch playerpatch = EpicFightCapabilities.ClientModule.getCachedLocalPlayerPatch();
		
		if (playerpatch == null || !playerpatch.getOriginal().isAlive() || playerpatch.getOriginal().isSpectator()) {
			return;
		}
		
		PoseStack poseStack = guiGraphics.pose();
		poseStack.pushPose();
		
		float partialTick = deltaTracker.getGameTimeDeltaPartialTick(false);
		float sliding = this.getSliding(partialTick);
		poseStack.translate(0, sliding, 0);
		
		float maxStamina = playerpatch.getMaxStamina();
		
		if (maxStamina > 0.0F && this.stamina < maxStamina) {
			Vec2i pos = ClientConfig.getStaminaPosition();
			float ratio = (this.staminaO + (this.stamina - this.staminaO) * partialTick) / maxStamina;
			RenderSystem.setShaderColor(1.0F, ratio, 0.25F, 1.0F);
			guiGraphics.blit(EntityUI.BATTLE_ICON, pos.x, pos.y, 118, 4, 2, 38, 237, 9, 255, 255);
			guiGraphics.blit(EntityUI.BATTLE_ICON, pos.x, pos.y, (int)(118 * ratio), 4, 2, 47, (int)(237 * ratio), 9, 255, 255);
			RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		}
		
		poseStack.popPose();
	}
	
	public void renderChargingBar(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
		if (Minecraft.getInstance().screen instanceof HUDLocationsScreen) {
			return;
		}
		
		LocalPlayerPatch playerpatch = EpicFightCapabilities.ClientModule.getCachedLocalPlayerPatch();;
		
		if (playerpatch == null || !playerpatch.getOriginal().isAlive() || playerpatch.getOriginal().isSpectator()) {
			return;
		}
		
		if (playerpatch.isHoldingAny() && playerpatch.getHoldingSkill() instanceof ChargeableSkill chageableSkill) {
			float partialTick = deltaTracker.getGameTimeDeltaPartialTick(false);
			int chargeTicks = playerpatch.getChargingTicks();
			int prevChargingAmount = playerpatch.getChargingTicksO();
			float ratio = Math.min((prevChargingAmount + (chargeTicks - prevChargingAmount) * partialTick) / chageableSkill.getMaxChargingTicks(), 1.0F);
			Vec2i pos = ClientConfig.getChargingBarPosition();
			RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
			
			guiGraphics.blit(EntityUI.BATTLE_ICON, pos.x, pos.y, 1, 71, 238, 13, 255, 255);
			guiGraphics.blit(EntityUI.BATTLE_ICON, pos.x, pos.y, 1, 57, (int)(238 * ratio), 13, 255, 255);
			
			ResourceLocation rl = ResourceLocation.parse(chageableSkill.toString());
			String skillName = Component.translatable(String.format("skill.%s.%s", rl.getNamespace(), rl.getPath())).getString();
			
			int stringWidth = this.minecraft.font.width(skillName);
			guiGraphics.drawString(this.minecraft.font, skillName, (int)(pos.x + 120 - stringWidth * 0.5F), pos.y - 12, 16777215, true);
		}
	}
	
	public void renderNormalSkills(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
		if (Minecraft.getInstance().screen instanceof HUDLocationsScreen) {
			return;
		}
		
		LocalPlayerPatch playerpatch = EpicFightCapabilities.ClientModule.getCachedLocalPlayerPatch();;
		
		if (playerpatch == null || !playerpatch.getOriginal().isAlive() || playerpatch.getOriginal().isSpectator()) {
			return;
		}
		
		for (SkillSlot slot : SkillSlot.ENUM_MANAGER.universalValues()) {
			if (slot == SkillSlots.WEAPON_INNATE) {
				continue;
			}

			SkillContainer container = playerpatch.getSkill(slot);

			if (!container.isEmpty()) {
				if (!this.skillIcons.contains(container) && container.getSkill().shouldDraw(container)) {
					this.skillIcons.add(container);
				}
			}
		}
		
		this.skillIcons.removeIf(skillContainer -> skillContainer.isEmpty() || !skillContainer.getSkill().shouldDraw(skillContainer));
		
		AlignDirection alignDirection = ClientConfig.passiveAlignDirection;
		HorizontalBasis horBasis = ClientConfig.passiveBaseX;
		VerticalBasis verBasis = ClientConfig.passiveBaseY;
		Window window = Minecraft.getInstance().getWindow();
		int passiveX = horBasis.positionGetter.apply(window.getGuiScaledWidth(), ClientConfig.passiveX);
		int passiveY = verBasis.positionGetter.apply(window.getGuiScaledHeight(), ClientConfig.passiveY);
		int icons = this.skillIcons.size();
		Vec2i slotCoord = alignDirection.startCoordGetter.get(passiveX, passiveY, 24, 24, icons, horBasis, verBasis);
		
		RenderSystem.enableBlend();
		RenderSystem.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA);
		float partialTick = deltaTracker.getGameTimeDeltaPartialTick(false);
		
		PoseStack poseStack = guiGraphics.pose();
		poseStack.pushPose();
		
		float sliding = this.getSliding(partialTick);
		poseStack.translate(0, sliding, 0);
		
		for (SkillContainer container : this.skillIcons) {
			if (!container.isEmpty()) {
				RenderSystem.enableBlend();
				RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
				RenderSystem.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA);
				container.getSkill().drawOnGui(this, container, guiGraphics, slotCoord.x, slotCoord.y, partialTick);
				slotCoord = alignDirection.nextPositionGetter.getNext(horBasis, verBasis, slotCoord, 24, 24);
			}
		}
		
		poseStack.popPose();
		
		RenderSystem.disableBlend();
	}
	
	public void renderWeaponInnateSkill(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
		if (Minecraft.getInstance().screen instanceof HUDLocationsScreen) {
			return;
		}
		
		LocalPlayerPatch playerpatch = EpicFightCapabilities.ClientModule.getCachedLocalPlayerPatch();;
		
		if (playerpatch == null || !playerpatch.getOriginal().isAlive() || playerpatch.getOriginal().isSpectator()) {
			return;
		}
		
		SkillContainer container = playerpatch.getSkill(SkillSlots.WEAPON_INNATE);

		if (!container.isEmpty() && container.getSkill().shouldDraw(container)) {
			float partialTick = deltaTracker.getGameTimeDeltaPartialTick(false);
			Vec2i pos = ClientConfig.getWeaponInnatePosition();
			
			PoseStack poseStack = guiGraphics.pose();
			poseStack.pushPose();
			
			float sliding = this.getSliding(partialTick);
			poseStack.translate(0, sliding, 0);
			container.getSkill().drawOnGui(this, container, guiGraphics, pos.x, pos.y, partialTick);
			poseStack.popPose();
		}
	}
	
	public void slideUp() {
		if (!this.isVisible) {
			this.sliding = 6;
			this.isVisible = true;
		}
	}
	
	public void slideDown() {
		if (this.isVisible) {
			this.sliding = 1;
			this.isVisible = false;
		}
	}
	
	public void init(LocalPlayerPatch playerpatch) {
		this.skillIcons.clear();
		this.staminaO = playerpatch.getStamina();
		this.stamina = playerpatch.getStamina();
	}
	
	public Font getFont() {
		return this.minecraft.font;
	}
	
	private float getSliding(float partialTick) {
		return Mth.lerp(partialTick, this.slidingO, this.sliding) * 8.0F + 4.0F;
	}
}
