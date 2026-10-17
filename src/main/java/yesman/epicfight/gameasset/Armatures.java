package yesman.epicfight.gameasset;
import yesman.epicfight.EpicFight;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import com.google.common.collect.Sets;
import com.mojang.datafixers.util.Pair;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.EntityType;
import yesman.epicfight.api.animation.Joint;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.asset.JsonAssetLoader;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.main.EpicFightMod;
import yesman.epicfight.model.armature.CreeperArmature;
import yesman.epicfight.model.armature.DragonArmature;
import yesman.epicfight.model.armature.EndermanArmature;
import yesman.epicfight.model.armature.HoglinArmature;
import yesman.epicfight.model.armature.HumanoidArmature;
import yesman.epicfight.model.armature.IronGolemArmature;
import yesman.epicfight.model.armature.PiglinArmature;
import yesman.epicfight.model.armature.RavagerArmature;
import yesman.epicfight.model.armature.SpiderArmature;
import yesman.epicfight.model.armature.VexArmature;
import yesman.epicfight.model.armature.WitherArmature;
import yesman.epicfight.registry.entries.EpicFightEntityTypes;
import yesman.epicfight.world.capabilities.entitypatch.EntityPatch;

public class Armatures {
	public static final Armatures INSTANCE = new Armatures();
	private static ResourceManager resourceManager = null;
	
	@FunctionalInterface
	public interface ArmatureContructor<T extends Armature> {
		T invoke(String name, int jointNumber, Joint joint, Map<String, Joint> jointMap);
	}
	
	private static final Map<ResourceLocation, ArmatureAccessor<? extends Armature>> ACCESSORS = new HashMap<> ();
	private static final Map<ArmatureAccessor<? extends Armature>, Armature> ARMATURES = new HashMap<> ();
	private static final Map<EntityType<?>, AssetAccessor<? extends Armature>> ENTITY_TYPE_ARMATURE_MAPPER = new HashMap<> ();
	
	public static final ArmatureAccessor<HumanoidArmature> BIPED = ArmatureAccessor.create(EpicFight.MODID, "entity/biped", HumanoidArmature::new);
	public static final ArmatureAccessor<CreeperArmature> CREEPER = ArmatureAccessor.create(EpicFight.MODID, "entity/creeper", CreeperArmature::new);
	public static final ArmatureAccessor<EndermanArmature> ENDERMAN = ArmatureAccessor.create(EpicFight.MODID, "entity/enderman", EndermanArmature::new);
	public static final ArmatureAccessor<HumanoidArmature> SKELETON = ArmatureAccessor.create(EpicFight.MODID, "entity/skeleton", HumanoidArmature::new);
	public static final ArmatureAccessor<SpiderArmature> SPIDER = ArmatureAccessor.create(EpicFight.MODID, "entity/spider", SpiderArmature::new);
	public static final ArmatureAccessor<IronGolemArmature> IRON_GOLEM = ArmatureAccessor.create(EpicFight.MODID, "entity/iron_golem", IronGolemArmature::new);
	public static final ArmatureAccessor<RavagerArmature> RAVAGER = ArmatureAccessor.create(EpicFight.MODID, "entity/ravager", RavagerArmature::new);
	public static final ArmatureAccessor<VexArmature> VEX = ArmatureAccessor.create(EpicFight.MODID, "entity/vex", VexArmature::new);
	public static final ArmatureAccessor<PiglinArmature> PIGLIN = ArmatureAccessor.create(EpicFight.MODID, "entity/piglin", PiglinArmature::new);
	public static final ArmatureAccessor<HoglinArmature> HOGLIN = ArmatureAccessor.create(EpicFight.MODID, "entity/hoglin", HoglinArmature::new);
	public static final ArmatureAccessor<DragonArmature> DRAGON = ArmatureAccessor.create(EpicFight.MODID, "entity/dragon", DragonArmature::new);
	public static final ArmatureAccessor<WitherArmature> WITHER = ArmatureAccessor.create(EpicFight.MODID, "entity/wither", WitherArmature::new);
	
	public static void registerEntityTypes() {
		registerEntityTypeArmature(EntityType.CAVE_SPIDER, SPIDER);
		registerEntityTypeArmature(EntityType.CREEPER, CREEPER);
		registerEntityTypeArmature(EntityType.DROWNED, BIPED);
		registerEntityTypeArmature(EntityType.ENDERMAN, ENDERMAN);
		registerEntityTypeArmature(EntityType.EVOKER, BIPED);
		registerEntityTypeArmature(EntityType.HOGLIN, HOGLIN);
		registerEntityTypeArmature(EntityType.HUSK, BIPED);
		registerEntityTypeArmature(EntityType.IRON_GOLEM, IRON_GOLEM);
		registerEntityTypeArmature(EntityType.PIGLIN_BRUTE, PIGLIN);
		registerEntityTypeArmature(EntityType.PIGLIN, PIGLIN);
		registerEntityTypeArmature(EntityType.PILLAGER, BIPED);
		registerEntityTypeArmature(EntityType.RAVAGER, RAVAGER);
		registerEntityTypeArmature(EntityType.SKELETON, SKELETON);
		registerEntityTypeArmature(EntityType.SPIDER, SPIDER);
		registerEntityTypeArmature(EntityType.STRAY, SKELETON);
		registerEntityTypeArmature(EntityType.VEX, VEX);
		registerEntityTypeArmature(EntityType.VINDICATOR, BIPED);
		registerEntityTypeArmature(EntityType.WITCH, BIPED);
		registerEntityTypeArmature(EntityType.WITHER_SKELETON, SKELETON);
		registerEntityTypeArmature(EntityType.ZOGLIN, HOGLIN);
		registerEntityTypeArmature(EntityType.ZOMBIE, BIPED);
		registerEntityTypeArmature(EntityType.ZOMBIE_VILLAGER, BIPED);
		registerEntityTypeArmature(EntityType.ZOMBIFIED_PIGLIN, PIGLIN);
		registerEntityTypeArmature(EntityType.PLAYER, BIPED);
		registerEntityTypeArmature(EntityType.ENDER_DRAGON, DRAGON);
		registerEntityTypeArmature(EntityType.WITHER, WITHER);
		registerEntityTypeArmature(EpicFightEntityTypes.WITHER_SKELETON_MINION.get(), SKELETON);
		registerEntityTypeArmature(EpicFightEntityTypes.WITHER_GHOST_CLONE.get(), WITHER);
	}
	
	public static void reload(ResourceManager resourceManager) {
		Armatures.resourceManager = resourceManager;
		
		ACCESSORS.entrySet().removeIf(entry -> !entry.getValue().inRegistry);
		ARMATURES.clear();
	}
	
	public static void registerEntityTypeArmature(EntityType<?> entityType, AssetAccessor<? extends Armature> armatureAccessor) {
		if (entityType == null) {
			yesman.epicfight.EpicFight.LOGGER.warn("Skipping armature mapping: entity type holder not bound yet");
			return;
		}
		ENTITY_TYPE_ARMATURE_MAPPER.put(entityType, armatureAccessor);
	}
	
	//For presets
	public static void registerEntityTypeArmatureByPreset(EntityType<?> entityType, String presetName) {
		ResourceLocation presetKey = ResourceLocation.parse(presetName);
		if (!BuiltInRegistries.ENTITY_TYPE.containsKey(presetKey)) {
			yesman.epicfight.EpicFight.LOGGER.warn("No armature preset entity named {} — skipping", presetKey);
			return;
		}
		EntityType<?> presetEntityType = BuiltInRegistries.ENTITY_TYPE.get(presetKey);
		ENTITY_TYPE_ARMATURE_MAPPER.put(entityType, ENTITY_TYPE_ARMATURE_MAPPER.get(presetEntityType));
	}
	
	@SuppressWarnings("unchecked")
	public static <A extends Armature> A getArmatureFor(EntityPatch<?> entitypatch) {
		return (A)ENTITY_TYPE_ARMATURE_MAPPER.get(entitypatch.getOriginal().getType()).get().deepCopy();
	}
	
	@SuppressWarnings("unchecked")
	@Nullable
	public static <A extends Armature> AssetAccessor<A> get(ResourceLocation id) {
		return (AssetAccessor<A>) ACCESSORS.get(id);
	}
	
	@SuppressWarnings("unchecked")
	public static <A extends Armature> AssetAccessor<A> getOrCreate(ResourceLocation id, ArmatureContructor<A> armatureConstructor) {
		return ACCESSORS.containsKey(id) ? (AssetAccessor<A>)ACCESSORS.get(id) : ArmatureAccessor.create(id, armatureConstructor, false);
	}
	
	@SuppressWarnings("unchecked")
	public static <A extends Armature> Set<Pair<ResourceLocation, AssetAccessor<A>>> entry() {
		Set<Pair<ResourceLocation, AssetAccessor<A>>> newset = Sets.newHashSet();
		
		for (AssetAccessor<? extends Armature> accessor : ACCESSORS.values()) {
			try {
				AssetAccessor<A> casted = (AssetAccessor<A>)accessor;
				newset.add(Pair.of(casted.registryName(), casted));
			} catch(ClassCastException e) {
			}
		}
		
		return newset;
	}
	
	public static ResourceLocation wrapLocation(ResourceLocation rl) {
		return rl.getPath().matches("animmodels/.*\\.json") ? rl : ResourceLocation.fromNamespaceAndPath(rl.getNamespace(), "animmodels/" + rl.getPath() + ".json");
	}
	
	public static record ArmatureAccessor<A extends Armature> (ResourceLocation registryName, ArmatureContructor<A> armatureConstructor, boolean inRegistry) implements AssetAccessor<A> {
		public static <A extends Armature> ArmatureAccessor<A> create(String namespaceId, String path, ArmatureContructor<A> armatureConstructor) {
			return create(ResourceLocation.fromNamespaceAndPath(namespaceId, path), armatureConstructor, true);
		}
		
		private static <A extends Armature> ArmatureAccessor<A> create(ResourceLocation id, ArmatureContructor<A> armatureConstructor, boolean inRegistry) {
			ArmatureAccessor<A> accessor = new ArmatureAccessor<A> (id, armatureConstructor, inRegistry);
			ACCESSORS.put(id, accessor);
			return accessor;
		}
		
		@SuppressWarnings("unchecked")
		@Override
		public A get() {
			if (ARMATURES.get(this) == null) {
				JsonAssetLoader jsonAssetLoader = new JsonAssetLoader(resourceManager, wrapLocation(this.registryName()));
				ARMATURES.put(this, jsonAssetLoader.loadArmature(this.armatureConstructor));
			}
			
			return (A)ARMATURES.get(this);
		}
		
		public String toString() {
			return this.registryName.toString();
		}
		
		public int hashCode() {
			return this.registryName.hashCode();
		}
		
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			} else if (obj instanceof ArmatureAccessor armatureAccessor) {
				return this.registryName.equals(armatureAccessor.registryName());
			} else if (obj instanceof ResourceLocation rl) {
				return this.registryName.equals(rl);
			} else if (obj instanceof String name) {
				return this.registryName.toString().equals(name);
			} else {
				return false;
			}
		}
	}
}