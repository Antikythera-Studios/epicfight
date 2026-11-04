package yesman.epicfight.world.capabilities;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import yesman.epicfight.EpicFight;
import yesman.epicfight.api.utils.side.ClientOnly;
import yesman.epicfight.world.capabilities.entitypatch.EntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.capabilities.provider.AttachmentEntityPatchProvider;
import yesman.epicfight.world.capabilities.provider.CommonEntityPatchProvider;
import yesman.epicfight.world.capabilities.provider.CommonItemCapabilityProvider;

import java.util.Optional;

@SuppressWarnings("unchecked")
public class EpicFightCapabilities {
	public static final CommonEntityPatchProvider ENTITY_PATCH_PROVIDER = CommonEntityPatchProvider.INSTANCE;
	public static final CommonItemCapabilityProvider ITEM_CAPABILITY_PROVIDER = CommonItemCapabilityProvider.INSTANCE;

	/// Returns [CapabilityItem] from [ItemStack] instance
	public static @NotNull CapabilityItem getItemStackCapability(ItemStack stack) {
		return getItemCapability(stack).orElse(CapabilityItem.EMPTY);
	}

    /// Returns [CapabilityItem] from [ItemStack] instance wrapped by [Optional]
	public static Optional<CapabilityItem> getItemCapability(ItemStack stack) {
		CapabilityItem cap = ITEM_CAPABILITY_PROVIDER.getCapability(stack, null);
		return Optional.ofNullable(cap);
	}

	/// This method should remain as the secondary option, especially when you can't fix local variables inside lambda expression.
    ///
    /// @param entity An entity object to extract an entity patch
    /// @param type A class type to cast
	public static <T extends EntityPatch<?>> T getEntityPatch(@Nullable Entity entity, Class<T> type) {
		if (entity != null) {
			AttachmentEntityPatchProvider attachmentEntitypatchProvider = getEntityPatchProvider(entity);
			EntityPatch<?> entitypatch = attachmentEntitypatchProvider.getCapability();

			if (entitypatch != null && type.isAssignableFrom(entitypatch.getClass())) {
				return (T)entitypatch;
			}
		}

		return null;
	}

    /// A compact version of [#getEntityPatch(Entity, Class)] to extract [PlayerPatch] from [Player]
    /// Conducts null checking
    public static @Nullable PlayerPatch<?> getPlayerPatch(@Nullable Player player) {
        if (player != null) {
            AttachmentEntityPatchProvider attachmentEntitypatchProvider = getEntityPatchProvider(player);
            EntityPatch<?> entitypatch = attachmentEntitypatchProvider.getCapability();

            if (entitypatch != null && PlayerPatch.class.isAssignableFrom(entitypatch.getClass())) {
                return (PlayerPatch<?>)entitypatch;
            }
        }

        return null;
    }

    /// A compact version of [#getEntityPatch(Entity, Class)] to extract [ServerPlayerPatch] from [ServerPlayer]
    /// Conducts null checking
    public static @Nullable ServerPlayerPatch getServerPlayerPatch(@Nullable ServerPlayer serverPlayer) {
        if (serverPlayer != null) {
            AttachmentEntityPatchProvider attachmentEntitypatchProvider = getEntityPatchProvider(serverPlayer);
            EntityPatch<?> entitypatch = attachmentEntitypatchProvider.getCapability();

            if (entitypatch != null && ServerPlayerPatch.class.isAssignableFrom(entitypatch.getClass())) {
                return (ServerPlayerPatch)entitypatch;
            }
        }

        return null;
    }

	/// Returns entity patch with unparameterized original entity
	/// This is useful to reduce the amount of code when type-casting for [EntityPatch#getOriginal] is unnecessary.
    ///
    /// @param entity An entity object to extract an entity patch
    /// @param type A class type to cast
	public static <T extends EntityPatch<?>> Optional<T> getUnparameterizedEntityPatch(@Nullable Entity entity, Class<T> type) {
		if (entity != null) {
			AttachmentEntityPatchProvider attachmentEntitypatchProvider = getEntityPatchProvider(entity);
			EntityPatch<?> entitypatch = attachmentEntitypatchProvider.getCapability();

			if (entitypatch != null && type.isAssignableFrom(entitypatch.getClass())) {
				return Optional.of((T)entitypatch);
			}
		}

		return Optional.empty();

	}

	/// Returns entity patch with parameterized original entity
	/// This method is used when you need parameterized return value of [EntityPatch#getOriginal].
    ///
    /// @param entity       An entity object to extract an entity patch
    /// @param entitytype   An entity type to cast
    /// @param patchtype    A class type to cast
	public static <E extends Entity, T extends EntityPatch<E>> Optional<T> getParameterizedEntityPatch(@Nullable Entity entity, Class<E> entitytype, Class<?> patchtype) {
		if (entity != null && entitytype.isAssignableFrom(entity.getClass())) {
			AttachmentEntityPatchProvider attachmentEntitypatchProvider = getEntityPatchProvider(entity);
			EntityPatch<?> entitypatch = attachmentEntitypatchProvider.getCapability();

			if (entitypatch != null && patchtype.isAssignableFrom(entitypatch.getClass())) {
				return Optional.of((T)entitypatch);
			}
		}

		return Optional.empty();
	}

    /// Returns [PlayerPatch] from a player
    ///
    /// @param entity A player to extract the entity patch
    public static Optional<PlayerPatch<?>> getPlayerPatchAsOptional(@Nullable Entity entity) {
        if (entity != null) {
            AttachmentEntityPatchProvider attachmentEntitypatchProvider = getEntityPatchProvider(entity);
            EntityPatch<?> entitypatch = attachmentEntitypatchProvider.getCapability();

            if (entitypatch instanceof PlayerPatch<?> playerpatch) {
                return Optional.of(playerpatch);
            }
        }

        return Optional.empty();
    }

    /// Returns [ServerPlayerPatch] from a server player
    /// Warning: developers must check logical side before calling this method
    ///
    /// @param entity A player to extract the entity patch
    public static Optional<ServerPlayerPatch> getServerPlayerPatchAsOptional(@Nullable Entity entity) {
        if (entity != null) {
            AttachmentEntityPatchProvider attachmentEntitypatchProvider = getEntityPatchProvider(entity);
            EntityPatch<?> entitypatch = attachmentEntitypatchProvider.getCapability();

            if (entitypatch instanceof ServerPlayerPatch serverplayerpatch) {
                return Optional.of(serverplayerpatch);
            }
        }

        return Optional.empty();
    }

    /// Gets or creates the entity patch provider for an entity.
    /// On Fabric, this uses a mixin-injected field on Entity.
    /// Package-private: ClientModule needs access and Fabric's classloader breaks nestmate visibility.
    static AttachmentEntityPatchProvider getEntityPatchProvider(Entity entity) {
        if (entity instanceof IEpicFightEntityPatchHolder holder) {
            AttachmentEntityPatchProvider provider = holder.epicfight$getEntityPatchProvider();
            if (provider == null) {
                provider = new AttachmentEntityPatchProvider(entity);
                holder.epicfight$setEntityPatchProvider(provider);
            }
            return provider;
        }
        EpicFight.LOGGER.warn("[EpicFight] getEntityPatchProvider: entity {} is not IEpicFightEntityPatchHolder", entity.getClass().getName());
        return new AttachmentEntityPatchProvider(entity);
    }

    // Client-only methods live in a separate class file to avoid loading
    // client types (LocalPlayer, Minecraft) on a dedicated server.
    @ClientOnly
    public static class ClientModule {
        public static @Nullable yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch getLocalPlayerPatch(@Nullable net.minecraft.client.player.LocalPlayer localPlayer) {
            if (localPlayer != null) {
                AttachmentEntityPatchProvider attachmentEntitypatchProvider = getEntityPatchProvider(localPlayer);
                EntityPatch<?> entitypatch = attachmentEntitypatchProvider.getCapability();

                if (entitypatch != null && yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch.class.isAssignableFrom(entitypatch.getClass())) {
                    return (yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch) entitypatch;
                }
            }

            return null;
        }

        public static @Nullable yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch getCachedLocalPlayerPatch() {
            if (net.minecraft.client.Minecraft.getInstance().player == null) {
                return null;
            }

            AttachmentEntityPatchProvider attachmentEntitypatchProvider = getEntityPatchProvider(net.minecraft.client.Minecraft.getInstance().player);
            EntityPatch<?> entitypatch = attachmentEntitypatchProvider.getCapability();

            if (entitypatch instanceof yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch localplayerpatch) {
                return localplayerpatch;
            }

            return null;
        }

        public static Optional<yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch> getCachedLocalPlayerPatchAsOptional() {
            if (net.minecraft.client.Minecraft.getInstance().player == null) {
                return Optional.empty();
            }

            return Optional.ofNullable(getLocalPlayerPatch(net.minecraft.client.Minecraft.getInstance().player));
        }

        public static Optional<yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch> getLocalPlayerPatchAsOptional(@Nullable Entity entity) {
            if (entity != null) {
                AttachmentEntityPatchProvider attachmentEntitypatchProvider = getEntityPatchProvider(entity);
                EntityPatch<?> entitypatch = attachmentEntitypatchProvider.getCapability();

                if (entitypatch instanceof yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch localplayerpatch) {
                    return Optional.of(localplayerpatch);
                }
            }

            return Optional.empty();
        }
    }
}
