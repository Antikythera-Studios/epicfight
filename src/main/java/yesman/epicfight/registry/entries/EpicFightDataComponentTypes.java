package yesman.epicfight.registry.entries;
import yesman.epicfight.EpicFight;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import yesman.epicfight.registry.deferred_shim.DeferredHolderShim;
import yesman.epicfight.registry.deferred_shim.DeferredRegisterShim;
import yesman.epicfight.main.EpicFightMod;
import yesman.epicfight.skill.Skill;

public final class EpicFightDataComponentTypes {
	private EpicFightDataComponentTypes() {}
	
	public static final DeferredRegisterShim<DataComponentType<?>> REGISTRY = new DeferredRegisterShim<>(Registries.DATA_COMPONENT_TYPE, EpicFight.MODID);
	
	public static final DeferredHolderShim<DataComponentType<?>, DataComponentType<Holder<Skill>>> SKILL = REGISTRY.register("skill", () ->
		DataComponentType.<Holder<Skill>>builder().persistent(Skill.CODEC).networkSynchronized(Skill.STREAM_CODEC).cacheEncoding().build()
	);
}
