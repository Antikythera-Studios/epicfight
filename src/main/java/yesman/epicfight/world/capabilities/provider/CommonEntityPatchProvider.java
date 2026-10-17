package yesman.epicfight.world.capabilities.provider;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import yesman.epicfight.api.event.EpicFightEventHooks;
import yesman.epicfight.api.event.types.registry.EntityPatchRegistryEvent;
import yesman.epicfight.api.utils.side.ClientOnly;
import yesman.epicfight.main.EpicFightMod;
import yesman.epicfight.registry.entries.EpicFightEntityTypes;
import yesman.epicfight.world.capabilities.entitypatch.EntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.GlobalMobPatch;
import yesman.epicfight.world.capabilities.entitypatch.boss.WitherGhostPatch;
import yesman.epicfight.world.capabilities.entitypatch.boss.WitherPatch;
import yesman.epicfight.world.capabilities.entitypatch.boss.enderdragon.EnderDragonPatch;
import yesman.epicfight.world.capabilities.entitypatch.mob.*;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.world.capabilities.projectile.*;
import yesman.epicfight.world.gamerule.EpicFightGameRules;

import java.util.*;
import java.util.function.Function;

public final class CommonEntityPatchProvider {
    public static final CommonEntityPatchProvider INSTANCE = new CommonEntityPatchProvider();

    private CommonEntityPatchProvider() {}

    private final Map<EntityType<?>, Function<Entity, EntityPatch<?>>> capabilities = new HashMap<> ();
    private final Map<EntityType<?>, Function<Entity, EntityPatch<?>>> datapackCapabilities = new HashMap<> ();
    private final Map<Class<? extends Projectile>, Function<Projectile, ProjectilePatch<?>>> typedCapabilities = new HashMap<> ();

    public void registerVanillaEntityPatches() {
        Map<EntityType<?>, Function<Entity, EntityPatch<?>>> registry = new HashMap<> ();
        registerEntityPatchUnsafe(registry, EntityType.PLAYER, entity -> new ServerPlayerPatch((ServerPlayer) entity));
        registerEntityPatch(registry, EntityType.ZOMBIE, ZombiePatch::new);
        registerEntityPatch(registry, EntityType.CREEPER, CreeperPatch::new);
        registerEntityPatch(registry, EntityType.ENDERMAN, EndermanPatch::new);
        registerEntityPatch(registry, EntityType.SKELETON, SkeletonPatch::new);
        registerEntityPatch(registry, EntityType.WITHER_SKELETON, WitherSkeletonPatch::new);
        registerEntityPatch(registry, EntityType.STRAY, StrayPatch::new);
        registerEntityPatch(registry, EntityType.ZOMBIFIED_PIGLIN, ZombifiedPiglinPatch::new);
        registerEntityPatch(registry, EntityType.ZOMBIE_VILLAGER, ZombiePatch::new);
        registerEntityPatch(registry, EntityType.HUSK, ZombiePatch::new);
        registerEntityPatch(registry, EntityType.SPIDER, SpiderPatch::new);
        registerEntityPatch(registry, EntityType.CAVE_SPIDER, CaveSpiderPatch::new);
        registerEntityPatch(registry, EntityType.IRON_GOLEM, IronGolemPatch::new);
        registerEntityPatch(registry, EntityType.VINDICATOR, VindicatorPatch::new);
        registerEntityPatch(registry, EntityType.EVOKER, EvokerPatch::new);
        registerEntityPatch(registry, EntityType.WITCH, WitchPatch::new);
        registerEntityPatch(registry, EntityType.DROWNED, DrownedPatch::new);
        registerEntityPatch(registry, EntityType.PILLAGER, PillagerPatch::new);
        registerEntityPatch(registry, EntityType.RAVAGER, RavagerPatch::new);
        registerEntityPatch(registry, EntityType.VEX, VexPatch::new);
        registerEntityPatch(registry, EntityType.PIGLIN, PiglinPatch::new);
        registerEntityPatch(registry, EntityType.PIGLIN_BRUTE, PiglinBrutePatch::new);
        registerEntityPatch(registry, EntityType.HOGLIN, HoglinPatch::new);
        registerEntityPatch(registry, EntityType.ZOGLIN, ZoglinPatch::new);
        registerEntityPatch(registry, EntityType.ENDER_DRAGON, entity -> {
            if (entity instanceof EnderDragon enderdragon) {
                return new EnderDragonPatch(enderdragon);
            }
            return null;
        });
        registerEntityPatch(registry, EntityType.WITHER, WitherPatch::new);
        registerEntityPatch(registry, EpicFightEntityTypes.WITHER_SKELETON_MINION.get(), WitherSkeletonPatch::new);
        registerEntityPatch(registry, EpicFightEntityTypes.WITHER_GHOST_CLONE.get(), WitherGhostPatch::new);
        registerEntityPatch(registry, EntityType.ARROW, ArrowPatch::new);
        registerEntityPatch(registry, EntityType.SPECTRAL_ARROW, ArrowPatch::new);
        registerEntityPatch(registry, EntityType.WITHER_SKULL, WitherSkullPatch::new);
        registerEntityPatch(registry, EntityType.DRAGON_FIREBALL, DragonFireballPatch::new);
        registerEntityPatch(registry, EntityType.TRIDENT, ThrownTridentPatch::new);

        this.typedCapabilities.put(AbstractArrow.class, entity -> new ArrowPatch<>((AbstractArrow) entity));

        this.capabilities.putAll(registry);
    }

    /// Fired lazily at [DeferredInitPhase] — mod entity types are only bound after all
    /// Fabric entrypoints run, so addon holders (e.g. cdmoveset CORRUPTDOG) resolve to
    /// real types instead of defaulted-registry fallbacks (missing -> minecraft:pig).
    public void fireEntityPatchRegistryEvent() {
        Map<EntityType<?>, Function<Entity, EntityPatch<?>>> registry = new HashMap<> ();
        EntityPatchRegistryEvent entitypatchRegistryEvent = new EntityPatchRegistryEvent(registry);
        EpicFightEventHooks.Registry.ENTITY_PATCH.post(entitypatchRegistryEvent);
        this.capabilities.putAll(registry);
    }

    /// For more details, refer to [EntityPatchRegistryEvent#registerEntityPatch].
    @ApiStatus.Internal
    public static <T extends Entity> void registerEntityPatch(
            @NotNull Map<EntityType<?>, Function<Entity, EntityPatch<?>>> registry,
            @NotNull EntityType<T> entityType,
            @NotNull Function<T, EntityPatch<T>> entityPatchFactory
    ) {
        if (entityType == null) {
            EpicFightMod.LOGGER.warn("Skipping entity patch registration: entity type holder not bound yet");
            return;
        }
        //noinspection unchecked
        registry.put(
                entityType, (entity) -> entityPatchFactory.apply((T) entity)
        );
    }

    /// Strongly prefer [#registerEntityPatch] over this unsafe version
    /// for type-safety and strict design. Use this only as a last resort.
    /// For more details, refer to [EntityPatchRegistryEvent#registerEntityPatchUnsafe].
    @ApiStatus.Internal
    public static <T extends Entity> void registerEntityPatchUnsafe(
            @NotNull Map<EntityType<?>, Function<Entity, EntityPatch<?>>> registry,
            @NotNull EntityType<T> entityType,
            @NotNull Function<? super T, ? extends EntityPatch<? extends T>> entityPatchFactory
    ) {
        //noinspection unchecked
        registry.put(
                entityType,
                entity -> entityPatchFactory.apply((T) entity)
        );
    }

    public void clearDatapackEntities() {
        this.datapackCapabilities.clear();
    }

    public void putCustomEntityPatch(EntityType<?> entityType, Function<Entity, EntityPatch<?>> entitypatchProvider) {
        this.datapackCapabilities.put(entityType, entitypatchProvider);
    }

    public Function<Entity, EntityPatch<?>> get(String registryName) {
        ResourceLocation rl = ResourceLocation.parse(registryName);
        EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.get(rl);

        return this.capabilities.get(entityType);
    }

    public List<EntityType<?>> getPatchedEntities() {
        List<EntityType<?>> list = new ArrayList<> ();
        list.add(null);
        this.capabilities.keySet().stream().filter((type) -> type.getCategory() != MobCategory.MISC).sorted(Comparator.comparing(EntityType::getKey)).forEach(list::add);

        return list;
    }

    @SuppressWarnings("rawtypes")
    public @Nullable EntityPatch getCapability(Entity entity) {
        Function<Entity, EntityPatch<?>> provider = this.datapackCapabilities.getOrDefault(entity.getType(), this.capabilities.get(entity.getType()));

        if (provider != null) {
            try {
                return provider.apply(entity);
            } catch (Exception e) {
                EpicFightMod.stacktraceIfDevSide("Can't apply entity patch provider", (s) -> e);
            }
        } else if (entity instanceof Mob mob && EpicFightGameRules.GLOBAL_STUN.getRuleValue(entity.level())) {
            return new GlobalMobPatch(mob);
        }

        return null;
    }

    @ClientOnly
    public static class ClientModule {
        public static void registerClientPlayerPatches() {
            INSTANCE.capabilities.put(EntityType.PLAYER, entity -> switch (entity) {
                case net.minecraft.client.player.LocalPlayer localPlayer -> new yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch(localPlayer);
                case net.minecraft.client.player.RemotePlayer remotePlayer -> new yesman.epicfight.client.world.capabilites.entitypatch.player.AbstractClientPlayerPatch<>(remotePlayer);
                case ServerPlayer serverPlayer -> new ServerPlayerPatch(serverPlayer);
                case null, default -> null;
            });
        }
    }
}