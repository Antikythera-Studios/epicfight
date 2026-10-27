package yesman.epicfight.client.events.engine;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.ChatFormatting;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.StringUtil;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderHighlightEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import yesman.epicfight.api.animation.JointTransform;
import yesman.epicfight.api.client.animation.AnimationSubFileReader.PovSettings.ViewLimit;
import yesman.epicfight.api.client.camera.EpicFightCameraAPI;
import yesman.epicfight.api.client.forgeevent.PatchedRenderersEvent;
import yesman.epicfight.api.client.forgeevent.RenderEnderDragonEvent;
import yesman.epicfight.api.client.input.InputManager;
import yesman.epicfight.api.client.input.action.EpicFightInputAction;
import yesman.epicfight.api.client.model.Meshes;
import yesman.epicfight.api.utils.math.MathUtils;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.api.utils.math.Vec3f;
import yesman.epicfight.client.ClientEngine;
import yesman.epicfight.client.gui.BattleModeGui;
import yesman.epicfight.client.gui.EntityUI;
import yesman.epicfight.client.gui.VersionNotifier;
import yesman.epicfight.client.gui.screen.config.UISetupScreen;
import yesman.epicfight.client.gui.screen.overlay.OverlayManager;
import yesman.epicfight.client.input.EpicFightKeyMappings;
import yesman.epicfight.client.mesh.HumanoidMesh;
import yesman.epicfight.client.renderer.EpicFightRenderTypes;
import yesman.epicfight.client.renderer.FakeBlockRenderer;
import yesman.epicfight.client.renderer.FirstPersonRenderer;
import yesman.epicfight.client.renderer.VanillaFakeBlockRenderer;
import yesman.epicfight.client.renderer.patched.entity.PCreeperRenderer;
import yesman.epicfight.client.renderer.patched.entity.PCustomEntityRenderer;
import yesman.epicfight.client.renderer.patched.entity.PCustomHumanoidEntityRenderer;
import yesman.epicfight.client.renderer.patched.entity.PDrownedRenderer;
import yesman.epicfight.client.renderer.patched.entity.PEnderDragonRenderer;
import yesman.epicfight.client.renderer.patched.entity.PEndermanRenderer;
import yesman.epicfight.client.renderer.patched.entity.PHoglinRenderer;
import yesman.epicfight.client.renderer.patched.entity.PHumanoidRenderer;
import yesman.epicfight.client.renderer.patched.entity.PIllagerRenderer;
import yesman.epicfight.client.renderer.patched.entity.PIronGolemRenderer;
import yesman.epicfight.client.renderer.patched.entity.PPlayerRenderer;
import yesman.epicfight.client.renderer.patched.entity.PRavagerRenderer;
import yesman.epicfight.client.renderer.patched.entity.PSpiderRenderer;
import yesman.epicfight.client.renderer.patched.entity.PStrayRenderer;
import yesman.epicfight.client.renderer.patched.entity.PVexRenderer;
import yesman.epicfight.client.renderer.patched.entity.PVindicatorRenderer;
import yesman.epicfight.client.renderer.patched.entity.PWitchRenderer;
import yesman.epicfight.client.renderer.patched.entity.PWitherRenderer;
import yesman.epicfight.client.renderer.patched.entity.PWitherSkeletonMinionRenderer;
import yesman.epicfight.client.renderer.patched.entity.PZombieVillagerRenderer;
import yesman.epicfight.client.renderer.patched.entity.PatchedEntityRenderer;
import yesman.epicfight.client.renderer.patched.entity.PatchedLivingEntityRenderer;
import yesman.epicfight.client.renderer.patched.entity.PresetRenderer;
import yesman.epicfight.client.renderer.patched.entity.WitherGhostCloneRenderer;
import yesman.epicfight.client.renderer.patched.item.RenderFilledMap;
import yesman.epicfight.client.renderer.patched.item.RenderItemBase;
import yesman.epicfight.client.renderer.patched.item.RenderKatana;
import yesman.epicfight.client.renderer.patched.item.RenderShield;
import yesman.epicfight.client.renderer.patched.item.RenderTrident;
import yesman.epicfight.client.renderer.patched.item.RenderTwoHandedRangedWeapon;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;
import yesman.epicfight.config.ClientConfig;
import yesman.epicfight.main.EpicFightMod;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.boss.BossPatch;
import yesman.epicfight.world.capabilities.entitypatch.boss.enderdragon.EnderDragonPatch;
import yesman.epicfight.world.capabilities.item.BowCapability;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.capabilities.item.CrossbowCapability;
import yesman.epicfight.world.capabilities.item.MapCapability;
import yesman.epicfight.world.capabilities.item.ShieldCapability;
import yesman.epicfight.world.capabilities.item.TridentCapability;
import yesman.epicfight.world.entity.EpicFightEntities;
import yesman.epicfight.world.gamerule.EpicFightGameRules;

@SuppressWarnings("rawtypes")
public class RenderEngine {
	public final BattleModeGui battleModeUI;
	public final VersionNotifier versionNotifier;
	public final Minecraft minecraft;
	
	private final BiMap<EntityType<?>, Function<EntityType<?>, PatchedEntityRenderer>> entityRendererProvider;
	private final Map<EntityType<?>, PatchedEntityRenderer> entityRendererCache;
	private final Map<Item, RenderItemBase> itemRendererMapByInstance;
	private final Map<Class<?>, RenderItemBase> itemRendererMapByClass;
	private final Map<UUID, BossPatch> bossEventOwners = Maps.newConcurrentMap();
	private final OverlayManager overlayManager;
	private FakeBlockRenderer fakeBlockRenderer;
	
	private FirstPersonRenderer firstPersonRenderer;
	private PHumanoidRenderer<?, ?, ?, ?, ?> basicHumanoidRenderer;
	private int modelInitTimer;
	
	public RenderEngine() {
		Events.renderEngine = this;
		
		this.minecraft = Minecraft.getInstance();
		this.battleModeUI = new BattleModeGui(this.minecraft);
		this.versionNotifier = new VersionNotifier(this.minecraft);
		this.entityRendererProvider = HashBiMap.create();
		this.entityRendererCache = Maps.newHashMap();
		this.itemRendererMapByInstance = Maps.newHashMap();
		this.itemRendererMapByClass = Maps.newHashMap();
		this.overlayManager = new OverlayManager();
		this.fakeBlockRenderer = new VanillaFakeBlockRenderer();
	}
	
	/**
	 * Dummy method for backward compatibility (since 20.14.7)
	 * Originally existed for initialization of item renderer types ('type' property in 'item_skins'),
	 * but it's moved to {@link #reloadItemRenderers}
	 */
	@Deprecated(forRemoval = true)
	public void initialize() {
		
	}
	
	public void reloadFakeBlockRenderer(FakeBlockRenderer fakeBlockRenderer) {
		this.fakeBlockRenderer = fakeBlockRenderer;
	}
	
	public void reloadEntityRenderers(EntityRendererProvider.Context context) {
		this.entityRendererProvider.clear();
		this.entityRendererProvider.put(EntityType.CREEPER, (entityType) -> new PCreeperRenderer(context, entityType).initLayerLast(context, entityType));
		this.entityRendererProvider.put(EntityType.ENDERMAN, (entityType) -> new PEndermanRenderer(context, entityType).initLayerLast(context, entityType));
		this.entityRendererProvider.put(EntityType.ZOMBIE, (entityType) -> new PHumanoidRenderer<>(Meshes.BIPED_OLD_TEX, context, entityType).initLayerLast(context, entityType));
		this.entityRendererProvider.put(EntityType.ZOMBIE_VILLAGER, (entityType) -> new PZombieVillagerRenderer(context, entityType).initLayerLast(context, entityType));
		this.entityRendererProvider.put(EntityType.ZOMBIFIED_PIGLIN, (entityType) -> new PHumanoidRenderer<>(Meshes.PIGLIN, context, entityType).initLayerLast(context, entityType));
		this.entityRendererProvider.put(EntityType.HUSK, (entityType) -> new PHumanoidRenderer<>(Meshes.BIPED_OLD_TEX, context, entityType).initLayerLast(context, entityType));
		this.entityRendererProvider.put(EntityType.SKELETON, (entityType) -> new PHumanoidRenderer<>(Meshes.SKELETON, context, entityType).initLayerLast(context, entityType));
		this.entityRendererProvider.put(EntityType.WITHER_SKELETON, (entityType) -> new PHumanoidRenderer<>(Meshes.SKELETON, context, entityType).initLayerLast(context, entityType));
		this.entityRendererProvider.put(EntityType.STRAY, (entityType) -> new PStrayRenderer(context, entityType).initLayerLast(context, entityType));
		this.entityRendererProvider.put(EntityType.PLAYER, (entityType) -> new PPlayerRenderer(context, entityType).initLayerLast(context, entityType));
		this.entityRendererProvider.put(EntityType.SPIDER, (entityType) -> new PSpiderRenderer(context, entityType).initLayerLast(context, entityType));
		this.entityRendererProvider.put(EntityType.CAVE_SPIDER, (entityType) -> new PSpiderRenderer(context, entityType).initLayerLast(context, entityType));
		this.entityRendererProvider.put(EntityType.IRON_GOLEM, (entityType) -> new PIronGolemRenderer(context, entityType).initLayerLast(context, entityType));
		this.entityRendererProvider.put(EntityType.VINDICATOR, (entityType) -> new PVindicatorRenderer(context, entityType).initLayerLast(context, entityType));
		this.entityRendererProvider.put(EntityType.EVOKER, (entityType) -> new PIllagerRenderer<> (context, entityType).initLayerLast(context, entityType));
		this.entityRendererProvider.put(EntityType.WITCH, (entityType) -> new PWitchRenderer(context, entityType).initLayerLast(context, entityType));
		this.entityRendererProvider.put(EntityType.DROWNED, (entityType) -> new PDrownedRenderer(context, entityType).initLayerLast(context, entityType));
		this.entityRendererProvider.put(EntityType.PILLAGER, (entityType) -> new PIllagerRenderer<> (context, entityType).initLayerLast(context, entityType));
		this.entityRendererProvider.put(EntityType.RAVAGER, (entityType) -> new PRavagerRenderer(context, entityType).initLayerLast(context, entityType));
		this.entityRendererProvider.put(EntityType.VEX, (entityType) -> new PVexRenderer(context, entityType).initLayerLast(context, entityType));
		this.entityRendererProvider.put(EntityType.PIGLIN, (entityType) -> new PHumanoidRenderer<>(Meshes.PIGLIN, context, entityType).initLayerLast(context, entityType));
		this.entityRendererProvider.put(EntityType.PIGLIN_BRUTE, (entityType) -> new PHumanoidRenderer<>(Meshes.PIGLIN, context, entityType).initLayerLast(context, entityType));
		this.entityRendererProvider.put(EntityType.HOGLIN, (entityType) -> new PHoglinRenderer<> (context, entityType).initLayerLast(context, entityType));
		this.entityRendererProvider.put(EntityType.ZOGLIN, (entityType) -> new PHoglinRenderer<> (context, entityType).initLayerLast(context, entityType));
		this.entityRendererProvider.put(EntityType.ENDER_DRAGON, (entityType) -> new PEnderDragonRenderer());
		this.entityRendererProvider.put(EntityType.WITHER, (entityType) -> new PWitherRenderer(context, entityType).initLayerLast(context, entityType));
		this.entityRendererProvider.put(EpicFightEntities.WITHER_SKELETON_MINION.get(), (entityType) -> new PWitherSkeletonMinionRenderer(context, entityType).initLayerLast(context, entityType));
		this.entityRendererProvider.put(EpicFightEntities.WITHER_GHOST_CLONE.get(), (entityType) -> new WitherGhostCloneRenderer());
		
		this.firstPersonRenderer = new FirstPersonRenderer(context, EntityType.PLAYER);
		this.basicHumanoidRenderer = new PHumanoidRenderer<>(Meshes.BIPED, context, EntityType.PLAYER);
		
		ModLoader.get().postEvent(new PatchedRenderersEvent.Add(this.entityRendererProvider, context));
		
		this.resetRenderers();
	}
	
	public void reloadItemRenderers(Map<ResourceLocation, JsonElement> objects) {
		//Clear item renderers
		this.itemRendererMapByInstance.clear();
		this.itemRendererMapByClass.clear();
		
		// Build item renderers
		Map<ResourceLocation, Function<JsonElement, RenderItemBase>> itemRenderers = Maps.newHashMap();
		itemRenderers.put(ResourceLocation.withDefaultNamespace("base"), RenderItemBase::new);
		itemRenderers.put(ResourceLocation.withDefaultNamespace("ranged"), RenderTwoHandedRangedWeapon::new);
		itemRenderers.put(ResourceLocation.withDefaultNamespace("map"), RenderFilledMap::new);
		itemRenderers.put(ResourceLocation.withDefaultNamespace("shield"), RenderShield::new);
		itemRenderers.put(ResourceLocation.withDefaultNamespace("trident"), RenderTrident::new);
		itemRenderers.put(EpicFightMod.identifier("uchigatana"), RenderKatana::new);
		
		ModLoader.get().postEvent(new PatchedRenderersEvent.RegisterItemRenderer(itemRenderers));
		
		for (Map.Entry<ResourceLocation, JsonElement> entry : objects.entrySet()) {
			ResourceLocation rl = entry.getKey();
			String pathString = rl.getPath();
			ResourceLocation registryName = ResourceLocation.fromNamespaceAndPath(rl.getNamespace(), pathString);
			
			if (!ForgeRegistries.ITEMS.containsKey(registryName)) {
				EpicFightMod.LOGGER.warn("Failed to load item skin: no item named " + registryName);
				continue;
			}
			
			Item item = ForgeRegistries.ITEMS.getValue(registryName);
			Function<JsonElement, RenderItemBase> rendererProvider;
			
			if (entry.getValue().getAsJsonObject().has("renderer")) {
				ResourceLocation rendererName = ResourceLocation.parse(entry.getValue().getAsJsonObject().get("renderer").getAsString());
				
				if (itemRenderers.containsKey(rendererName)) {
					rendererProvider = itemRenderers.get(rendererName);
				} else {
					EpicFightMod.LOGGER.warn("No renderer named " + rendererName);
					rendererProvider = RenderItemBase::new;
				}
			} else {
				rendererProvider = RenderItemBase::new;
			}
			
			RenderItemBase itemRenderer = rendererProvider.apply(entry.getValue());
			this.itemRendererMapByInstance.put(item, itemRenderer);
		}
		
		RenderItemBase baseRenderer = new RenderItemBase(new JsonObject());
		RenderTwoHandedRangedWeapon bowRenderer = new RenderTwoHandedRangedWeapon(objects.get(ForgeRegistries.ITEMS.getKey(Items.BOW)).getAsJsonObject());
		RenderTwoHandedRangedWeapon crossbowRenderer = new RenderTwoHandedRangedWeapon(objects.get(ForgeRegistries.ITEMS.getKey(Items.CROSSBOW)).getAsJsonObject());
		RenderTrident tridentRenderer = new RenderTrident(objects.get(ForgeRegistries.ITEMS.getKey(Items.TRIDENT)).getAsJsonObject());
		RenderFilledMap mapRenderer = new RenderFilledMap(objects.get(ForgeRegistries.ITEMS.getKey(Items.FILLED_MAP)).getAsJsonObject());
		RenderShield shieldRenderer = new RenderShield(objects.get(ForgeRegistries.ITEMS.getKey(Items.SHIELD)).getAsJsonObject());
		
		// Render by item classes
		this.itemRendererMapByClass.put(BowItem.class, bowRenderer);
		this.itemRendererMapByClass.put(CrossbowItem.class, crossbowRenderer);
		this.itemRendererMapByClass.put(ShieldItem.class, baseRenderer);
		this.itemRendererMapByClass.put(TridentItem.class, tridentRenderer);
		this.itemRendererMapByClass.put(ShieldItem.class, shieldRenderer);
		
		// Render by capability classes
		this.itemRendererMapByClass.put(BowCapability.class, bowRenderer);
		this.itemRendererMapByClass.put(CrossbowCapability.class, crossbowRenderer);
		this.itemRendererMapByClass.put(TridentCapability.class, tridentRenderer);
		this.itemRendererMapByClass.put(MapCapability.class, mapRenderer);
		this.itemRendererMapByClass.put(ShieldCapability.class, shieldRenderer);
	}
	
	public void resetRenderers() {
		this.entityRendererCache.clear();
		
		for (Map.Entry<EntityType<?>, Function<EntityType<?>, PatchedEntityRenderer>> entry : this.entityRendererProvider.entrySet()) {
			this.entityRendererCache.put(entry.getKey(), entry.getValue().apply(entry.getKey()));
		}
		
		ModLoader.get().postEvent(new PatchedRenderersEvent.Modify(this.entityRendererCache));
	}
	
	@SuppressWarnings("unchecked")
	public void registerCustomEntityRenderer(EntityType<?> entityType, String rendererName, CompoundTag compound) {
		if (StringUtil.isNullOrEmpty(rendererName)) {
			return;
		}
		
		EntityRenderDispatcher erd = this.minecraft.getEntityRenderDispatcher();
		EntityRendererProvider.Context context = new EntityRendererProvider.Context(erd, this.minecraft.getItemRenderer(), this.minecraft.getBlockRenderer(), erd.getItemInHandRenderer(), this.minecraft.getResourceManager(), this.minecraft.getEntityModels(), this.minecraft.font);
		
		if ("player".equals(rendererName)) {
			this.entityRendererCache.put(entityType, this.basicHumanoidRenderer);
		} else if ("epicfight:custom".equals(rendererName)) {
			if (compound.getBoolean("humanoid")) {
				this.entityRendererCache.put(entityType, new PCustomHumanoidEntityRenderer<> (Meshes.getOrCreate(ResourceLocation.parse(compound.getString("model")), (jsonAssetLoader) -> jsonAssetLoader.loadSkinnedMesh(HumanoidMesh::new)), context, entityType));
			} else {
				this.entityRendererCache.put(entityType, new PCustomEntityRenderer(Meshes.getOrCreate(ResourceLocation.parse(compound.getString("model")), (jsonAssetLoader) -> jsonAssetLoader.loadSkinnedMesh(HumanoidMesh::new)), context));
			}
		} else {
			EntityType<?> presetEntityType = ForgeRegistries.ENTITY_TYPES.getValue(ResourceLocation.parse(rendererName));
			
			if (this.entityRendererProvider.containsKey(presetEntityType)) {
				PatchedEntityRenderer renderer = this.entityRendererProvider.get(presetEntityType).apply(entityType);
				
				if (!(this.minecraft.getEntityRenderDispatcher().renderers.get(entityType) instanceof LivingEntityRenderer) && (renderer instanceof PatchedLivingEntityRenderer patchedLivingEntityRenderer)) {
					this.entityRendererCache.put(entityType, new PresetRenderer(context, entityType, (LivingEntityRenderer<LivingEntity, EntityModel<LivingEntity>>)context.getEntityRenderDispatcher().renderers.get(presetEntityType), patchedLivingEntityRenderer.getDefaultMesh()));
				} else {
					this.entityRendererCache.put(entityType, this.entityRendererProvider.get(presetEntityType).apply(entityType));
				}
			} else {
				throw new IllegalArgumentException("Datapack Mob Patch Crash: Invalid Renderer type " + rendererName);
			}
		}
	}
	
	public RenderItemBase getItemRenderer(ItemStack itemstack) {
		RenderItemBase renderItem = this.itemRendererMapByInstance.get(itemstack.getItem());
		
		if (renderItem == null) {
			renderItem = this.findMatchingRendererByClass(itemstack.getItem().getClass());
			
			if (renderItem == null) {
				CapabilityItem itemCap = EpicFightCapabilities.getItemStackCapability(itemstack);
				renderItem = this.findMatchingRendererByClass(itemCap.getClass());
			}
			
			if (renderItem == null) {
				// Get generic renderer
				renderItem = this.itemRendererMapByInstance.get(Items.AIR);
			}
			
			this.itemRendererMapByInstance.put(itemstack.getItem(), renderItem);
		}
		
		return renderItem;
	}

	private RenderItemBase findMatchingRendererByClass(Class<?> clazz) {
		RenderItemBase renderer = null;
		
		for (; clazz != null && renderer == null; clazz = clazz.getSuperclass()) {
			renderer = this.itemRendererMapByClass.get(clazz);
		}
		
		return renderer;
	}
	
	@SuppressWarnings("unchecked")
	public void renderEntityArmatureModel(LivingEntity livingEntity, LivingEntityPatch<?> entitypatch, EntityRenderer<? extends Entity> renderer, MultiBufferSource buffer, PoseStack matStack, int packedLight, float partialTicks) {
		this.getEntityRenderer(livingEntity).render(livingEntity, entitypatch, renderer, buffer, matStack, packedLight, partialTicks);
	}
	
	public PatchedEntityRenderer getEntityRenderer(Entity entity) {
		return this.getEntityRenderer(entity.getType());
	}
	
	public PatchedEntityRenderer getEntityRenderer(EntityType entityType) {
		return this.entityRendererCache.get(entityType);
	}
	
	public boolean hasRendererFor(Entity entity) {
		return this.entityRendererCache.computeIfAbsent(entity.getType(), (key) -> this.entityRendererProvider.containsKey(key) ? this.entityRendererProvider.get(entity.getType()).apply(entity.getType()) : null) != null;
	}
	
	public Set<ResourceLocation> getRendererEntries() {
		Set<ResourceLocation> availableRendererEntities = this.entityRendererProvider.keySet().stream().map((entityType) -> EntityType.getKey(entityType)).collect(Collectors.toSet());
		availableRendererEntities.add(EpicFightMod.identifier("custom"));
		
		return availableRendererEntities;
	}
	
	public void setModelInitializerTimer(int tick) {
		this.modelInitTimer = tick;
	}
	
	// true when cancel the existing camera setup
	
	
	public OverlayManager getOverlayManager() {
		return this.overlayManager;
	}
	
	public FirstPersonRenderer getFirstPersonRenderer() {
		return firstPersonRenderer;
	}
	
	public void upSlideSkillUI() {
		this.battleModeUI.slideUp();
	}
	
	public void downSlideSkillUI() {
		this.battleModeUI.slideDown();
	}
	
	public boolean shouldRenderVanillaModel() {
		return ClientEngine.getInstance().isVanillaModelDebuggingMode() || this.modelInitTimer > 0;
	}
	
	public void addBossEventOwner(UUID uuid, BossPatch bosspatch) {
		this.bossEventOwners.put(uuid, bosspatch);
	}
	
	public void removeBossEventOwner(UUID uuid, BossPatch bosspatch) {
		this.bossEventOwners.remove(uuid);
	}
	
	public void initHUD() {
		this.battleModeUI.init();
		this.versionNotifier.init();
	}
	
	public void freeUnusedSources() {
		this.bossEventOwners.entrySet().removeIf((entry) -> {
			Entity entity = entry.getValue().cast().getOriginal();
			return !entity.isAlive() || entity.isRemoved();
		});
		
		if (!RenderSystem.isOnRenderThread()) {
			RenderSystem.recordRenderCall(() -> {
				EpicFightRenderTypes.freeUnusedWorldRenderTypes();
			});
		} else {
			EpicFightRenderTypes.freeUnusedWorldRenderTypes();
		}
	}
	
	public void clear() {
		EpicFightCameraAPI.getInstance().zoomOut(0);
		
		this.bossEventOwners.clear();
		
		if (!RenderSystem.isOnRenderThread()) {
			RenderSystem.recordRenderCall(() -> {
				this.resetRenderers();
				EpicFightRenderTypes.clearWorldRenderTypes();
			});
		} else {
			this.resetRenderers();
			EpicFightRenderTypes.clearWorldRenderTypes();
		}
	}
	
	public static boolean hitResultEquals(@Nullable HitResult hitResult, HitResult.Type hitType) {
		return hitResult == null ? false : hitType.equals(hitResult.getType());
	}
	
	public static boolean hitResultNotEquals(@Nullable HitResult hitResult, HitResult.Type hitType) {
		return hitResult == null ? true : !hitType.equals(hitResult.getType());
	}
	
	/// More strict type sensitive hit result getter by instanceof
    public static BlockHitResult asBlockHitResult(@Nullable HitResult hitResult) {
        if (hitResult == null) {
            return null;
        }

        if (hitResult.getType() == HitResult.Type.BLOCK && hitResult instanceof BlockHitResult blockHitResult) {
            return blockHitResult;
        }

        return null;
    }

    /// More strict type sensitive hit result getter by instanceof
    public static EntityHitResult asEntityHitResult(@Nullable HitResult hitResult) {
        if (hitResult == null) {
            return null;
        }

        if (hitResult.getType() == HitResult.Type.ENTITY && hitResult instanceof EntityHitResult entityHitResult) {
            return entityHitResult;
        }

        return null;
    }
	
	/** These methods will be removed in 1.21.1 **/
	@Deprecated
	public void correctCamera(ViewportEvent.ComputeCameraAngles event, float partialTicks) {
		// Leave this method for shoulder surfing
		event.getCamera().setRotation(1.0F, 1.0F);
	}
	
	@Deprecated
	public void setRangedWeaponThirdPerson(ViewportEvent.ComputeCameraAngles event, CameraType pov, double partialTicks) {}
	
	@Mod.EventBusSubscriber(modid = EpicFightMod.MODID, value = Dist.CLIENT)
	public static class Events {
		static RenderEngine renderEngine;
		
		@SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.LOWEST)
		public static void renderLivingEvent(RenderLivingEvent.Pre<? extends LivingEntity, ? extends EntityModel<? extends LivingEntity>> event) {
			LivingEntity livingentity = event.getEntity();
			
			if (livingentity.level() == null) {
				return;
			}
			
			if (event.isCanceled()) {
				return;
			}
			
			if (renderEngine.hasRendererFor(livingentity)) {
				LivingEntityPatch<?> entitypatch = EpicFightCapabilities.getEntityPatch(livingentity, LivingEntityPatch.class);
				float originalYRot = 0.0F;

				// Draw the player in inventory
				if ((event.getPartialTick() == 0.0F || event.getPartialTick() == 1.0F) && entitypatch instanceof LocalPlayerPatch localplayerpatch) {
					if (entitypatch.overrideRender()) {
						originalYRot = localplayerpatch.getModelYRot();
						localplayerpatch.setModelYRotInGui(livingentity.getYRot());
						event.getPoseStack().translate(0, 0.1D, 0);
						boolean compusteShaderSetting = ClientConfig.activateComputeShader;
						
						// Disable compute shader
						ClientConfig.activateComputeShader = false;
						renderEngine.renderEntityArmatureModel(livingentity, entitypatch, event.getRenderer(), event.getMultiBufferSource(), event.getPoseStack(), event.getPackedLight(), event.getPartialTick());
						ClientConfig.activateComputeShader = compusteShaderSetting;
						
						event.setCanceled(true);
						localplayerpatch.disableModelYRotInGui(originalYRot);
					}
					
					return;
				}
				
				if (entitypatch != null && entitypatch.overrideRender()) {
					renderEngine.renderEntityArmatureModel(livingentity, entitypatch, event.getRenderer(), event.getMultiBufferSource(), event.getPoseStack(), event.getPackedLight(), event.getPartialTick());
					
					if (renderEngine.shouldRenderVanillaModel()) {
						event.getPoseStack().translate(renderEngine.modelInitTimer > 0 ? 10000.0F : 1.5F, 0.0F, 0.0F);
						--renderEngine.modelInitTimer;
					} else {
						event.setCanceled(true);
						postRenderLiving(event);
					}
				}
			}
			
			if (!renderEngine.minecraft.options.hideGui && !EpicFightGameRules.DISABLE_ENTITY_UI.getRuleValue(livingentity.level())) {
				EpicFightCapabilities.getUnparameterizedEntityPatch(renderEngine.minecraft.player, LocalPlayerPatch.class).ifPresent(playerpatch -> {
					LivingEntityPatch<?> entityPatch = EpicFightCapabilities.getEntityPatch(livingentity, LivingEntityPatch.class);
					
					for (EntityUI entityIndicator : EntityUI.ENTITY_UI_LIST) {
						if (entityIndicator.shouldDraw(livingentity, entityPatch, playerpatch, event.getPartialTick())) {
							entityIndicator.draw(livingentity, entityPatch, playerpatch, event.getPoseStack(), event.getMultiBufferSource(), event.getPartialTick());
						}
					}
				});
			}
		}
		
		@SuppressWarnings({ "unchecked", "rawtypes" })
		private static void postRenderLiving(RenderLivingEvent.Pre<? extends LivingEntity, ? extends EntityModel<? extends LivingEntity>> event) {
			try {
				net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new RenderLivingEvent.Post(event.getEntity(), event.getRenderer(),
					event.getPartialTick(), event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight()));
			} catch (Exception e) {
				EpicFightMod.LOGGER.debug("[render] A RenderLivingEvent.Post listener threw: {}", e.toString());
			}
		}

		@SubscribeEvent
		public static void itemTooltip(ItemTooltipEvent event) {
			if (ClientConfig.showEpicFightAttributesInTooltip && event.getEntity() != null && event.getEntity().level().isClientSide) {
				EpicFightCapabilities.getUnparameterizedEntityPatch(event.getEntity(), LocalPlayerPatch.class).ifPresent(playerpatch -> {
					CapabilityItem cap = EpicFightCapabilities.getItemStackCapabilityOr(event.getItemStack(), null);
					
					if (cap != null) {
						if (InputManager.isActionPhysicallyActive(EpicFightInputAction.WEAPON_INNATE_SKILL_TOOLTIP)) {
							Skill weaponInnateSkill = cap.getInnateSkill(playerpatch, event.getItemStack());

							if (weaponInnateSkill != null) {
								event.getToolTip().clear();
								List<Component> skilltooltip = weaponInnateSkill.getTooltipOnItem(event.getItemStack(), cap, playerpatch);

								for (Component s : skilltooltip) {
									event.getToolTip().add(s);
								}
							}
						} else {
							List<Component> tooltip = event.getToolTip();
							cap.modifyItemTooltip(event.getItemStack(), event.getToolTip(), playerpatch);
							
							for (int i = 0; i < tooltip.size(); i++) {
								Component textComp = tooltip.get(i);
								
								if (!textComp.getSiblings().isEmpty()) {
									Component sibling = textComp.getSiblings().get(0);
									
									if (sibling instanceof MutableComponent mutableComponent && mutableComponent.getContents() instanceof TranslatableContents translatableContent) {
										if (translatableContent.getArgs().length > 1 && translatableContent.getArgs()[1] instanceof MutableComponent mutableComponent$2) {
											if (mutableComponent$2.getContents() instanceof TranslatableContents translatableContent$2) {
												if (translatableContent$2.getKey().equals(Attributes.ATTACK_SPEED.getDescriptionId())) {
													float weaponSpeed = (float)playerpatch.getWeaponAttribute(Attributes.ATTACK_SPEED, event.getItemStack());
													tooltip.remove(i);
													tooltip.add(i, Component.literal(String.format(" %.2f ", playerpatch.getModifiedAttackSpeed(cap, weaponSpeed)))
															.append(Component.translatable(Attributes.ATTACK_SPEED.getDescriptionId())));
													
												} else if (translatableContent$2.getKey().equals(Attributes.ATTACK_DAMAGE.getDescriptionId())) {
													float weaponDamage = (float)playerpatch.getWeaponAttribute(Attributes.ATTACK_DAMAGE, event.getItemStack());
													float damageBonus = EnchantmentHelper.getDamageBonus(event.getItemStack(), MobType.UNDEFINED);
													String damageFormat = ItemStack.ATTRIBUTE_MODIFIER_FORMAT.format(playerpatch.getModifiedBaseDamage(weaponDamage) + damageBonus);
													
													tooltip.remove(i);
													tooltip.add(i, Component.literal(String.format(" %s ", damageFormat))
																			.append(Component.translatable(Attributes.ATTACK_DAMAGE.getDescriptionId()))
																			.withStyle(ChatFormatting.DARK_GREEN));
												}
											}
										}
									}
								}
							}
							
							Skill weaponInnateSkill = cap.getInnateSkill(playerpatch, event.getItemStack());
							
							if (weaponInnateSkill != null) {
								event.getToolTip().add(Component.translatable("inventory.epicfight.guide_innate_tooltip", EpicFightKeyMappings.WEAPON_INNATE_SKILL_TOOLTIP.getKey().getDisplayName()).withStyle(ChatFormatting.DARK_GRAY));
							}
						}
					}
				});
			}
		}
		
		private static final Vector3f CAMERA_ROTATION_EULER = new Vector3f();
		private static final OpenMatrix4f PLAYER_ROTATION = new OpenMatrix4f();
		
		@SubscribeEvent
		public static void cameraSetupEvent(ViewportEvent.ComputeCameraAngles event) {
			EpicFightCapabilities.getUnparameterizedEntityPatch(renderEngine.minecraft.player, LocalPlayerPatch.class).ifPresent(playerpatch -> {
				// First person camera correction
				if (ClientConfig.enablePovAction && renderEngine.minecraft.options.getCameraType().isFirstPerson() && playerpatch.isEpicFightMode() && !playerpatch.getFirstPersonLayer().isOff()) {
					float partialTick = (float)event.getPartialTick();
					EpicFightCameraAPI cameraApi = EpicFightCameraAPI.getInstance();
					
					if (cameraApi.isLerpingFpv()) {
						float xRot = cameraApi.getLerpedFpvXRot(partialTick);
						float yRot = cameraApi.getLerpedFpvYRot(partialTick);
						renderEngine.minecraft.cameraEntity.setXRot(xRot);
						renderEngine.minecraft.cameraEntity.setYRot(yRot);
					} else {
						ViewLimit viewLimit = playerpatch.getPovSettings().viewLimit();
						
						if (viewLimit != null) {
							float clampedXRot = Mth.clamp(event.getPitch(), viewLimit.xRotMin(), viewLimit.xRotMax());
							float bodyY = MathUtils.findNearestRotation(event.getYaw(), playerpatch.getYRot());
							float clampedYRot = Mth.clamp(event.getYaw(), bodyY + viewLimit.yRotMin(), bodyY + viewLimit.yRotMax());
							
							if (Float.compare(clampedXRot, event.getPitch()) != 0 || Float.compare(clampedYRot, event.getYaw()) != 0) {
								cameraApi.fixFpvRotation(clampedXRot, playerpatch.getYRot(), 5);
							}
						}
					}
					
					if (playerpatch.hasCameraAnimation()) {
						float time = Mth.lerp(partialTick, playerpatch.getFirstPersonLayer().animationPlayer.getPrevElapsedTime(), playerpatch.getFirstPersonLayer().animationPlayer.getElapsedTime());
						JointTransform cameraTransform;
						
						if (playerpatch.getFirstPersonLayer().animationPlayer.getAnimation().get().isLinkAnimation() || playerpatch.getPovSettings() == null) {
							cameraTransform = playerpatch.getFirstPersonLayer().getLinkCameraTransform().getInterpolatedTransform(time);
						} else {
							cameraTransform = playerpatch.getPovSettings().cameraTransform().getInterpolatedTransform(time);
						}
						
						float xRot = playerpatch.getOriginal().getXRot();
						float yRot = playerpatch.getOriginal().getYRot();
						
						Vec3f translation = OpenMatrix4f.transform3v(OpenMatrix4f.ofRotationDegree(yRot, Vec3f.Y_AXIS, PLAYER_ROTATION).rotate(xRot, Vec3f.X_AXIS), cameraTransform.translation(), null);
						Quaternionf rot = cameraTransform.rotation();
						rot.getEulerAnglesXYZ(CAMERA_ROTATION_EULER);
						
						CAMERA_ROTATION_EULER.x = (float)Math.toDegrees(CAMERA_ROTATION_EULER.x);
						CAMERA_ROTATION_EULER.y = (float)Math.toDegrees(CAMERA_ROTATION_EULER.y);
						CAMERA_ROTATION_EULER.z = (float)Math.toDegrees(CAMERA_ROTATION_EULER.z);
						
						event.getCamera().move(translation.x, translation.y, translation.z);
						event.setPitch(event.getPitch() + CAMERA_ROTATION_EULER.x);
						event.setYaw(event.getYaw() + CAMERA_ROTATION_EULER.y);
						event.setRoll(event.getRoll() + CAMERA_ROTATION_EULER.z);
					}
				}
			});
		}
		
		@SubscribeEvent
		public static void renderGui(RenderGuiEvent.Pre event) {
			Window window = Minecraft.getInstance().getWindow();
			LocalPlayerPatch playerpatch = ClientEngine.getInstance().getPlayerPatch();
			
			if (playerpatch != null) {
				playerpatch.getSkillCapability().listSkillContainers().forEach(skillContainer -> {
					if (skillContainer.getSkill() != null) {
						skillContainer.getSkill().onScreen(playerpatch, window.getGuiScaledWidth(), window.getGuiScaledHeight());
					}
				});
				
				renderEngine.overlayManager.renderTick(window.getGuiScaledWidth(), window.getGuiScaledHeight());
				
				if (Minecraft.renderNames() && !(Minecraft.getInstance().screen instanceof UISetupScreen)) {
					renderEngine.battleModeUI.renderTick();
				}
				
				//Shows the epic fight version in beta
				renderEngine.versionNotifier.render(event.getGuiGraphics(), true);
			}
		}
		
		@SubscribeEvent
		public static void renderGameOverlayPost(CustomizeGuiOverlayEvent.BossEventProgress event) {
			if (event.getBossEvent().getName().getString().equals("Ender Dragon")) {
				if (renderEngine.bossEventOwners.containsKey(event.getBossEvent().getId())) {
					LivingEntityPatch<?> entitypatch = renderEngine.bossEventOwners.get(event.getBossEvent().getId()).cast();
					float stunShield = entitypatch.getStunShield();
					
					if (stunShield > 0) {
						float progression = stunShield / entitypatch.getMaxStunShield();
						int x = event.getX();
						int y = event.getY();
						
						RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
						event.getGuiGraphics().blit(BossHealthOverlay.GUI_BARS_LOCATION, x, y + 6, 183, 2, 0, 45.0F, 182, 6, 255, 255);
						event.getGuiGraphics().blit(BossHealthOverlay.GUI_BARS_LOCATION, x + (int)(183 * progression), y + 6, (int)(183 * (1.0F - progression)), 2, 0, 39.0F, 182, 6, 255, 255);
					}
				}
			}
		}
		
		@SuppressWarnings("unchecked")
		@SubscribeEvent(priority = EventPriority.HIGHEST)
		public static void renderHand(RenderHandEvent event) {
			LocalPlayerPatch playerpatch = ClientEngine.getInstance().getPlayerPatch();
			
			if (playerpatch != null) {
				boolean isBattleMode = playerpatch.isEpicFightMode();
				
				if (isBattleMode && ClientConfig.enableAnimatedFirstPersonModel) {
					RenderItemBase mainhandItemSkin = renderEngine.getItemRenderer(playerpatch.getOriginal().getMainHandItem());
					RenderItemBase offhandItemSkin = renderEngine.getItemRenderer(playerpatch.getOriginal().getOffhandItem());
					boolean useEpicFightModel = (mainhandItemSkin == null || !mainhandItemSkin.forceVanillaFirstPerson()) && (offhandItemSkin == null || !offhandItemSkin.forceVanillaFirstPerson());
					
					if (useEpicFightModel) {
						if (event.getHand() == InteractionHand.MAIN_HAND) {
							renderEngine.firstPersonRenderer.render(
								  playerpatch.getOriginal()
								, playerpatch
								, (LivingEntityRenderer)renderEngine.minecraft.getEntityRenderDispatcher().getRenderer(playerpatch.getOriginal())
								, event.getMultiBufferSource()
								, event.getPoseStack()
								, event.getPackedLight()
								, event.getPartialTick()
							);
						}
						
						event.setCanceled(true);
					}
				}
			}
		}
		
		@SubscribeEvent
		public static void renderWorldLast(RenderLevelStageEvent event) {
			if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_TRIPWIRE_BLOCKS) {
				BlockHitResult blockHitResult = asBlockHitResult(renderEngine.minecraft.hitResult);
				
				if (ClientConfig.mineBlockGuideOption.showBlockHighlight() && blockHitResult != null) {
					EpicFightCapabilities.getUnparameterizedEntityPatch(renderEngine.minecraft.player, LocalPlayerPatch.class).ifPresent(playerpatch -> {
						if (!playerpatch.canPlayAttackAnimation() && playerpatch.isEpicFightMode()) {
							renderEngine.fakeBlockRenderer.render(event.getCamera(), event.getPoseStack(), renderEngine.minecraft.renderBuffers().bufferSource(), renderEngine.minecraft.level, blockHitResult.getBlockPos(), 1.0F, 1.0F, 1.0F, 0.4F);					
						}
					});
				}
			}
		}
		
		@SuppressWarnings("unchecked")
		@SubscribeEvent
		public static void renderEnderDragonEvent(RenderEnderDragonEvent event) {
			EnderDragon livingentity = event.getEntity();
			
			if (renderEngine.hasRendererFor(livingentity)) {
				EpicFightCapabilities.getUnparameterizedEntityPatch(livingentity, EnderDragonPatch.class).ifPresent(enderdragonpatch -> {
					event.setCanceled(true);
					renderEngine.getEntityRenderer(livingentity).render(livingentity, enderdragonpatch, event.getRenderer(), event.getBuffers(), event.getPoseStack(), event.getLight(), event.getPartialRenderTick());
				});
			}
		}
		
		@SubscribeEvent
		public static void renderBlockHighlight(RenderHighlightEvent.Block event) {
			EpicFightCapabilities.getUnparameterizedEntityPatch(renderEngine.minecraft.player, LocalPlayerPatch.class).ifPresent(playerpatch -> {
				if (playerpatch.canPlayAttackAnimation()) {
					event.setCanceled(true);
				}
			});
		}
		
		@SubscribeEvent
		public static void renderTickEvent(TickEvent.RenderTickEvent event) {
			if (event.phase == TickEvent.Phase.START) {
				EntityUI.HEALTH_BAR.reset();
			} else {
				EntityUI.HEALTH_BAR.remove();
			}
		}
		
		@SubscribeEvent
		public static void clientTickEvent(TickEvent.ClientTickEvent event) {
			if (event.phase == TickEvent.Phase.START) {
				renderEngine.freeUnusedSources();
				EpicFightCameraAPI.getInstance().preClientTick();
			} else {
				EpicFightCameraAPI.getInstance().postClientTick();
			}
		}
		
		@SubscribeEvent
		public static void levelTickEvent(TickEvent.LevelTickEvent event) {
			if (event.level.isClientSide() && event.phase == TickEvent.Phase.END) {
				EntityUI.HEALTH_BAR.tick();
			}
		}
	}

    /**
     * @deprecated Use {@link EpicFightCameraAPI#zoomIn()} instead
     */
    @Deprecated(forRemoval = true)
    public void zoomIn() {
        EpicFightCameraAPI.getInstance().zoomIn();
    }

    /**
     * @deprecated Use {@link EpicFightCameraAPI#zoomOut(int)} instead
     */
    @Deprecated(forRemoval = true)
    public void zoomOut(int zoomOutTicks) {
        EpicFightCameraAPI.getInstance().zoomOut(zoomOutTicks);
    }
}
