package yesman.epicfight.api.client.event.types.render;
import yesman.epicfight.EpicFight;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import yesman.epicfight.api.event.Event;
import yesman.epicfight.main.EpicFightMod;

public class AnimatedArmorTextureEvent extends Event {
	private final LivingEntity livingentity;
    private final ItemStack itemstack;
    private final EquipmentSlot equipmentSlot;
    private final HumanoidModel<?> originalModel;
    private ResourceLocation resultLocation;
    
	public AnimatedArmorTextureEvent(LivingEntity livingentity, ItemStack itemstack, EquipmentSlot equipmentSlot, HumanoidModel<?> originalModel) {
		this.livingentity = livingentity;
        this.itemstack = itemstack;
        this.equipmentSlot = equipmentSlot;
        this.originalModel = originalModel;
	}
	
	public ResourceLocation getResultLocation() {
		return this.resultLocation;
	}
	
	public void setResultLocation(ResourceLocation resultLocation) {
		if (this.resultLocation != null) {
			EpicFight.LOGGER.debug("AnimatedArmorTextureEvent: You've overriden the existing texutre location " + this.resultLocation);
		}
		
		this.resultLocation = resultLocation;
	}
	
	public LivingEntity getLivingEntity() {
		return this.livingentity;
	}
	
	public ItemStack getItemStack() {
		return this.itemstack;
	}
	
	public EquipmentSlot getEquipmentSlot() {
		return this.equipmentSlot;
	}
	
	public HumanoidModel<?> getOriginalModel() {
		return this.originalModel;
	}
}