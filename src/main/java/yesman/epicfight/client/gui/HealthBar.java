package yesman.epicfight.client.gui;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import yesman.epicfight.EpicFight;
import yesman.epicfight.config.ClientConfig;
import yesman.epicfight.config.ClientConfig.HealthBarVisibility;
import yesman.epicfight.world.capabilities.entitypatch.Faction;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.effect.VisibleMobEffect;

public class HealthBar extends EntityUI {
    public static final ResourceLocation HEALTHBARS1 = EpicFight.identifier("textures/gui/healthbars1.png");
    public static final ResourceLocation HEALTHBARS2 = EpicFight.identifier("textures/gui/healthbars2.png");
    public static final TagKey<EntityType<?>> BOSSES = TagKey.create(BuiltInRegistries.ENTITY_TYPE.key(), EpicFight.identifier("bosses"));
	
	private final Map<LivingEntity, EntityAttributeTracker> trackingEntities = Maps.newConcurrentMap();
	
	@Override
	public boolean shouldDraw(LivingEntity entity, @Nullable LivingEntityPatch<?> entitypatch, LocalPlayerPatch playerpatch, float partialTicks) {
		HealthBarVisibility healthBarVisibility = ClientConfig.healthBarVisibility;
		Minecraft mc = Minecraft.getInstance();
		
		if (healthBarVisibility == HealthBarVisibility.NONE) {
			return false;
		} else if (entity.getType().is(BOSSES)) {
			return false;
		}
		
		EntityAttributeTracker healthTracker = this.trackingEntities.computeIfAbsent(entity, (key) -> new EntityAttributeTracker(key, entitypatch));
		healthTracker.checkChange(partialTicks);
		healthTracker.setKeepUp(true);
		
		if (entity.isInvisibleTo(playerpatch.getOriginal()) || entity == playerpatch.getOriginal().getVehicle()) {
			return false;
		} else if (entity.distanceToSqr(mc.getCameraEntity()) >= 400) {
			return false;
		} else if (entity instanceof Player player) {
			// Target == myself, stun shield is 0
			if (player == playerpatch.getOriginal() && playerpatch.getMaxStunShield() <= 0.0F) {
				return false;
			} else if (player.isCreative() || player.isSpectator()) {
				return false;
			}
		}
		
		boolean showTarget = false;
		
		if (healthBarVisibility == HealthBarVisibility.TARGET) {
			return playerpatch.getTarget() == entity;
		} else if (healthBarVisibility == HealthBarVisibility.TARGET_AND_HURT) {
			showTarget = playerpatch.getTarget() == entity;
		}
		
		return (!entity.getActiveEffects().isEmpty() || entity.getHealth() < entity.getMaxHealth() || showTarget) && !entity.isRemoved();
	}
	
	@Override
	public void draw(LivingEntity entity, @Nullable LivingEntityPatch<?> entitypatch, LocalPlayerPatch playerpatch, PoseStack poseStack, MultiBufferSource buffers, float partialTicks) {
		poseStack.pushPose();
		
		setupPoseStack(poseStack, entity, 0.0F, entity.getBbHeight() + 0.25F, 0.0F, true, partialTicks);
		Collection<MobEffectInstance> activeEffects = entity.getActiveEffects(); 
		
		if (!activeEffects.isEmpty() && !entity.is(playerpatch.getOriginal())) {
			Iterator<MobEffectInstance> iter = activeEffects.iterator();
			int acives = activeEffects.size();
			int row = acives > 1 ? 1 : 0;
			int column = ((acives-1) / 2);
			float startX = -0.8F + -0.3F * row;
			float startY = -0.15F + 0.15F * column;
			
			for (int i = 0; i <= column; i++) {
				for (int j = 0; j <= row; j++) {
					MobEffectInstance effectInstance = iter.next();
					MobEffect effect = effectInstance.getEffect().value();
					ResourceLocation rl;
					
					if (effect instanceof VisibleMobEffect visibleMobEffect) {
						rl = visibleMobEffect.getIcon(effectInstance);
					} else {
						rl = ResourceLocation.fromNamespaceAndPath(BuiltInRegistries.MOB_EFFECT.getKey(effect).getNamespace(), "textures/mob_effect/" + BuiltInRegistries.MOB_EFFECT.getKey(effect).getPath() + ".png");
					}
					
					float x = startX + 0.3F * j;
					float y = startY + -0.3F * i;
					drawUIAsLevelModel(poseStack.last(), rl, buffers, x, y, x + 0.3F, y + 0.3F, 0, 0, 256, 256, 256);
					
					if (!iter.hasNext()) {
						break;
					}
				}
			}
		}
		
		EntityAttributeTracker attributeTracker = this.trackingEntities.get(entity);
		ResourceLocation healthBarTexture;
		int damageColor;
		int textureIndex;
		
		if (entitypatch != null) {
			Faction faction = entitypatch.getFaction();
			healthBarTexture = faction.healthBarTexture();
			textureIndex = faction.healthBarIndex();
			damageColor = faction.damageColor();
		} else {
			healthBarTexture = HEALTHBARS2;
			textureIndex = 0;
			damageColor = 0xFFFF0000;
		}
		
		int texV = textureIndex * 10;
		final float healthBarHeight = 0.048828125F;
		final float innerHealthBarHeight = 0.029296875F;
		final float maxHealth = entity.getMaxHealth();
		float partialHealth = attributeTracker.healthState.getAnimatedValue(entity.tickCount, partialTicks);
		float partialAbsorption = attributeTracker.absorptionState.getAnimatedValue(entity.tickCount, partialTicks);
		float healthEnd;
		
		// Draw health bar background
		drawUIAsLevelModel(poseStack.last(), healthBarTexture, buffers, -0.5F, -healthBarHeight, 0.5F, healthBarHeight, 0, (texV) / 64.0F, 1.0F, (texV + 5) / 64.0F);
		
		if (partialAbsorption > 0.0F || attributeTracker.absorptionState.hasAnimation()) {
			boolean isTotalOverMaxHealth = (partialHealth + partialAbsorption) > maxHealth;
			float absorptionStart = isTotalOverMaxHealth ? Mth.clamp(partialHealth / (partialHealth + partialAbsorption), 0.0F, 1.0F) : partialHealth / maxHealth;
			float absorptionEnd = isTotalOverMaxHealth ? Mth.clamp((partialHealth + partialAbsorption) / maxHealth, 0.0F, 1.0F) : (partialHealth + partialAbsorption) / maxHealth;
			
			// Draw absorption amount
			drawUIAsLevelModel(poseStack.last(), HEALTHBARS2, buffers, absorptionStart - 0.5F, -healthBarHeight, absorptionEnd - 0.5F, healthBarHeight, absorptionStart, 0.921875F, absorptionEnd, 1.0F);
			
			if (attributeTracker.absorptionState.hasAnimation()) {
				float lostAbsorptionStart = Mth.clamp(entity.getAbsorptionAmount() / partialAbsorption, 0.0F, 1.0F);
				lostAbsorptionStart = absorptionStart + (absorptionEnd - absorptionStart) * lostAbsorptionStart;
				// Draw lost absorption amount
				drawColoredQuadAsLevelModel(poseStack.last(), buffers, lostAbsorptionStart - 0.5F, -innerHealthBarHeight, absorptionEnd - 0.5F, innerHealthBarHeight, 0x64FF1200);
			}
			
			healthEnd = isTotalOverMaxHealth ? absorptionStart : 1.0F;
		} else {
			healthEnd = 1.0F;
		}
		
		float ratio = Mth.clamp(entity.getHealth() / maxHealth, 0.0F, 1.0F);
		float filledHealthEnd = Math.max(-0.5F + ratio * healthEnd, -0.46875F);
		
		// Draw health amount
		drawUIAsLevelModel(poseStack.last(), healthBarTexture, buffers, -0.5F, -healthBarHeight, filledHealthEnd, healthBarHeight, 0.0F, (texV + 5) / 64.0F, ratio * healthEnd, (texV + 10) / 64.0F);
		
		if (attributeTracker.healthState.hasAnimation()) {
			float animatedHealthRatio = Mth.clamp(partialHealth / maxHealth, 0.0F, 1.0F);
			float animatedHealthModelX = Math.min(-0.5F + animatedHealthRatio, 0.46875F);
			
			// Draw lost health amount
			drawColoredQuadAsLevelModel(poseStack.last(), buffers, filledHealthEnd, -innerHealthBarHeight, animatedHealthModelX, innerHealthBarHeight, damageColor);
		}
		
		if (attributeTracker.stunShieldState != null) {
			if (Float.compare(entitypatch.getStunShield(), 0.0F) != 0) {
				// Draw stun shield background
				drawUIAsLevelModel(poseStack.last(), BATTLE_ICON, buffers, -0.5F, -0.08F, 0.5F, -healthBarHeight, 1, 0, 63, 5, 256);
				
				float stunShieldRatio = Mth.clamp(entitypatch.getStunShield() / entitypatch.getMaxStunShield(), 0.0F, 1.0F);
				float shieldRatio = -0.5F + stunShieldRatio;
				int stunShieldTextureRatio = (int) (62 * stunShieldRatio);
				
				// Draw stun shield amount
				drawUIAsLevelModel(poseStack.last(), BATTLE_ICON, buffers, -0.5F, -0.08F, shieldRatio, -healthBarHeight, 1, 5, stunShieldTextureRatio, 10, 256);
				
				float animatedStunShieldPosition = Mth.clamp(attributeTracker.stunShieldState.getAnimatedValue(entity.tickCount, partialTicks) / entitypatch.getMaxStunShield(), 0.0F, 1.0F);
				
				// Draw lost stun shield amount
				drawColoredQuadAsLevelModel(poseStack.last(), buffers, shieldRatio, -0.08F, animatedStunShieldPosition - 0.5F, -healthBarHeight, 0x88FF1200);
			}
		}
		
		poseStack.popPose();
	}
	
	public void reset() {
		this.trackingEntities.values().forEach(tracker -> tracker.setKeepUp(false));
	}
	
	public void remove() {
		this.trackingEntities.entrySet().removeIf(entry -> !entry.getValue().canKeepUp());
	}
	
	public void tick() {
		this.trackingEntities.values().forEach(EntityAttributeTracker::tick);
	}
	
	public class EntityAttributeTracker {
		private final LivingEntity entity;
		private final AttributeState healthState;
		private final AttributeState absorptionState;
		@Nullable
		private final AttributeState stunShieldState;
		
		private boolean canKeepUp;
		
		public EntityAttributeTracker(LivingEntity entity, @Nullable LivingEntityPatch<?> entitypatch) {
			this.entity = entity;
			this.healthState = new AttributeState(() -> entity.getMaxHealth(), () -> entity.getHealth());
			this.absorptionState = new AttributeState(() -> entity.getMaxHealth(), () -> entity.getAbsorptionAmount());
			this.stunShieldState = entitypatch != null ? new AttributeState(() -> entitypatch.getMaxStunShield(), () -> entitypatch.getStunShield()) : null;
		}
		
		public void setKeepUp(boolean canKeepUp) {
			this.canKeepUp = canKeepUp;
		}
		
		public boolean canKeepUp() {
			return this.canKeepUp;
		}
		
		public void checkChange(float partialTick) {
			this.healthState.checkState(this.entity.tickCount, partialTick);
			this.absorptionState.checkState(this.entity.tickCount, partialTick);
			if (this.stunShieldState != null) this.stunShieldState.checkState(this.entity.tickCount, partialTick);
		}
		
		public void tick() {
			this.healthState.tick(this.entity.tickCount);
			this.absorptionState.tick(this.entity.tickCount);
			if (this.stunShieldState != null) this.stunShieldState.tick(this.entity.tickCount);
		}
		
		class AttributeState {
			final Supplier<Float> maxValueGetter;
			final Supplier<Float> currentValueGetter;
			float value;
			float valueO;
			int lastChangeTick;
			int animationFrames;
			
			AttributeState(Supplier<Float> maxValueGetter, Supplier<Float> currentValueGetter) {
				this.maxValueGetter = maxValueGetter;
				this.currentValueGetter = currentValueGetter;
				
				float initValue = currentValueGetter.get();
				this.value = initValue;
				this.valueO = initValue;
				this.lastChangeTick = 0;
				this.animationFrames = 0;
			}
			
			void checkState(int tickCount, float partialTick) {
				float currentValue = this.currentValueGetter.get();
				
				if (this.value != currentValue) {
					if (this.animationFrames > 0) {
						this.valueO = this.getAnimatedValue(tickCount, partialTick);
					}
					
					this.lastChangeTick = tickCount + 3;
					this.animationFrames = currentValue == 0 ? 4 : Math.max(4, (int)((Math.abs(this.valueO - currentValue) / this.maxValueGetter.get()) * 10.0F));
					this.value = currentValue;
				}
			}
			
			boolean hasAnimation() {
				return this.animationFrames > 0;
			}
			
			void tick(int tickCount) {
				if (tickCount - this.lastChangeTick >= this.animationFrames) {
					this.animationFrames = 0;
					this.valueO = this.value;
				}
			}
			
			float getAnimatedValue(int tickCount, float partialTick) {
				if (this.animationFrames == 0) {
					return this.value;
				} else {
					if (tickCount < this.lastChangeTick) {
						return this.valueO;
					} else {
						float divide = (float)(tickCount - this.lastChangeTick) / (float)this.animationFrames;
						float partial = divide + partialTick / this.animationFrames;
						
						return this.valueO + (this.value - this.valueO) * partial;
					}
				}
			}
		}
	}
}