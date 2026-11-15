package yesman.epicfight.data.loot.function;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.floats.FloatObjectPair;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import yesman.epicfight.platform.ModPlatformProvider;
import yesman.epicfight.registry.EpicFightRegistries;
import yesman.epicfight.registry.entries.EpicFightDataComponentTypes;
import yesman.epicfight.registry.entries.EpicFightLootItemFunctions;
import yesman.epicfight.skill.Skill;

import java.util.ArrayList;
import java.util.List;

public class SetSkillFunction extends LootItemConditionalFunction {
	/**
	 * A codec for skill modifier
	 * 
	 * e.g.
	 * 
	 * {
	 * 	"skills": ["epicfight:roll", "epicfight:step"],
	 *  "weights": [0.1, 0.2],
	 * }
	 * 
	 **/
	public static final MapCodec<SetSkillFunction> CODEC = RecordCodecBuilder.mapCodec(
		instance -> commonFields(instance)
            .and(
	            instance.group(
	            	Skill.CODEC.listOf().fieldOf("skills").forGetter(setskillfunction -> setskillfunction.skillSource.stream().map(FloatObjectPair::right).toList()),
	                Codec.FLOAT.listOf().fieldOf("weights").forGetter(setskillfunction -> setskillfunction.skillSource.stream().map(FloatObjectPair::leftFloat).toList())
	            )
		    )
            .apply(instance, SetSkillFunction::new)
    );
	
	private final List<FloatObjectPair<Holder<Skill>>> skillSource;
	
	public SetSkillFunction(List<LootItemCondition> predicates, List<Holder<Skill>> skills, List<Float> weights) {
		super(predicates);
		
		if (skills.size() != weights.size()) {
			throw new IllegalArgumentException("skills and weights number unmatches");
		}
		
		ImmutableList.Builder<FloatObjectPair<Holder<Skill>>> builder = ImmutableList.builder();
		
		for (int i = 0; i < skills.size(); i++) {
			builder.add(FloatObjectPair.of(weights.get(i), normalizeSkill(skills.get(i))));
		}
		
		this.skillSource = builder.build();
	}
	
	/**
	 * DeferredHolderShim implements Holder but is not a registry Reference, so
	 * it fails canSerializeIn during NBT save ("Unregistered holder"). Resolve
	 * the canonical holder straight from the skill registry by key — works for
	 * shims and any other non-reference holder alike.
	 */
	private static Holder<Skill> normalizeSkill(Holder<Skill> holder) {
		return holder.unwrapKey()
				.flatMap(EpicFightRegistries.SKILL::getHolder)
				.map(reference -> (Holder<Skill>) reference)
				.orElse(holder);
	}
	
	private Holder<Skill> selectRandomSkillFromSource(RandomSource randomSource) {
		for (FloatObjectPair<Holder<Skill>> pair : this.skillSource) {
			if (randomSource.nextFloat() < pair.firstFloat()) {
				return pair.second();
			}
		}
		
		return this.skillSource.isEmpty() ? null : this.skillSource.get(0).second();
	}
	
	@Override
	protected ItemStack run(ItemStack itemstack, LootContext context) {
		if (ModPlatformProvider.get().isModLoaded("epicskills")) {
			return ItemStack.EMPTY;
		}
		
		Holder<Skill> skill = this.selectRandomSkillFromSource(context.getRandom());
		
		if (skill != null) {
			itemstack.set(EpicFightDataComponentTypes.SKILL.get(), skill);
		}
		
		return itemstack;
	}
	
	@Override
	public LootItemFunctionType<? extends LootItemConditionalFunction> getType() {
		return EpicFightLootItemFunctions.SKILLS.get();
	}
	
	@SafeVarargs
	public static LootItemFunction.Builder builder(Holder<Skill>... skills) {
		return new LootItemFunction.Builder() {
			public LootItemFunction build() {
				List<Holder<Skill>> list1 = new ArrayList<> ();
				List<Float> list2 = new ArrayList<> ();
				float weight = 1.0F / skills.length;
				float weightSum = 0.0F;
				
				for (Holder<Skill> skill : skills) {
					weightSum += weight;
					list1.add(skill);
					list2.add(weightSum);
				}
				
				return new SetSkillFunction(List.of(), list1, list2);
			}
		};
	}
	
	public static LootItemFunction.Builder builder(Object... skillAndWeight) {
		return new LootItemFunction.Builder() {
			@SuppressWarnings("unchecked")
			public LootItemFunction build() {
				List<Holder<Skill>> list1 = new ArrayList<> ();
				List<Float> list2 = new ArrayList<> ();
				
				float weightTotal = 0.0F;
				float weightSum = 0.0F;
				
				for (int i = 0; i < skillAndWeight.length / 2; i++) {
					weightTotal += (float)skillAndWeight[i * 2];
				}
				
				for (int i = 0; i < skillAndWeight.length / 2; i++) {
					weightSum += (float)skillAndWeight[i * 2];
					list1.add((Holder<Skill>)skillAndWeight[i * 2 + 1]);
					list2.add(weightSum / weightTotal);
				}
				
				return new SetSkillFunction(List.of(), list1, list2);
			}
		};
	}
}