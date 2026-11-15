package yesman.epicfight.registry.entries;
import yesman.epicfight.EpicFight;

import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.core.registries.Registries;
import yesman.epicfight.registry.deferred_shim.DeferredHolderShim;
import yesman.epicfight.registry.deferred_shim.DeferredRegisterShim;
import yesman.epicfight.main.EpicFightMod;
import yesman.epicfight.server.commands.arguments.AnimationArgument;
import yesman.epicfight.server.commands.arguments.SkillArgument;

public final class EpicFightCommandArgumentTypes {
	private EpicFightCommandArgumentTypes() {}
	
	public static final DeferredRegisterShim<ArgumentTypeInfo<?, ?>> REGISTRY = new DeferredRegisterShim<>(Registries.COMMAND_ARGUMENT_TYPE, EpicFight.MODID);
	
	public static final DeferredHolderShim<ArgumentTypeInfo<?, ?>, ArgumentTypeInfo<SkillArgument, ?>> SKILL = REGISTRY.register("skill", () -> SingletonArgumentInfo.contextFree(SkillArgument::skill));
	public static final DeferredHolderShim<ArgumentTypeInfo<?, ?>, ArgumentTypeInfo<AnimationArgument, ?>> ANIMATION = REGISTRY.register("animation", () -> SingletonArgumentInfo.contextFree(AnimationArgument::animation));
	
	public static void registerArgumentTypes() {
		ArgumentTypeInfos.BY_CLASS.put(SkillArgument.class, SKILL.get());
		ArgumentTypeInfos.BY_CLASS.put(AnimationArgument.class, ANIMATION.get());
	}
}