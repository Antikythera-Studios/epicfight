package yesman.epicfight.world.entity.data;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.ApiStatus;

import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;

import io.netty.buffer.Unpooled;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import yesman.epicfight.registry.deferred_shim.DeferredHolderShim;
import yesman.epicfight.api.utils.datastructure.ParameterizedHolderHashMap;
import yesman.epicfight.network.EpicFightNetworkManager;
import yesman.epicfight.network.EpicFightNetworkManager.PayloadBundleBuilder;
import yesman.epicfight.network.server.SPModifyExpandedEntityData;

public final class ExpandedSyncedData {
	/// Stores registered keys by their ResourceKey to allow comparison between
	/// DeferredHolderShim (used during registration) and vanilla Holder.Reference
	/// (produced by packet decoders). Without this, HashSet.contains() fails because
	/// the two Holder implementations have incompatible equals() methods.
	private final Set<ResourceKey<ExpandedEntityDataAccessor<?>>> registeredKeys = new HashSet<> ();
	private final Map<ResourceKey<ExpandedEntityDataAccessor<?>>, Holder<ExpandedEntityDataAccessor<?>>> registeredHolders = new HashMap<> ();
	private final ParameterizedHolderHashMap<ExpandedEntityDataAccessor<?>> dataMap = new ParameterizedHolderHashMap<> ();
	private final Map<Holder<ExpandedEntityDataAccessor<?>>, Object> pendingDirtyData = new HashMap<> ();
	private final Supplier<Integer> entityIdProvider;
	private final boolean isLogicalServer;

	public ExpandedSyncedData(Supplier<Integer> entityIdProvider, boolean isServer) {
		this.entityIdProvider = entityIdProvider;
		this.isLogicalServer = isServer;
	}

	public void register(Holder<ExpandedEntityDataAccessor<?>> key) {
		ResourceKey<ExpandedEntityDataAccessor<?>> rkey = toResourceKey(key);
		this.registeredKeys.add(rkey);
		this.registeredHolders.put(rkey, key);
		this.dataMap.put(key, key.value().defaultValue());
	}

	@ApiStatus.Internal
	public void load(CompoundTag compound) {
		CompoundTag synchedDataCompound = compound.getCompound("expanded_sycned_data");

		if (synchedDataCompound != null) {
			MapLike<Tag> compoundConverted = NbtOps.INSTANCE.getMap(synchedDataCompound).result().orElseThrow();

			this.registeredHolders.values().stream().forEach(holder -> {
				Object data = holder.value().readFromTag(compoundConverted);

				if (data != null) {
					this.dataMap.put(holder, data);
					this.pendingDirtyData.put(holder, data);
				}
			});
		}
	}

	@SuppressWarnings("unchecked")
	@ApiStatus.Internal
	public void saveData(CompoundTag compound) {
		RecordBuilder<Tag> recordBuilder = NbtOps.INSTANCE.mapBuilder();


		this.dataMap.forEach((k, v) -> {
			((ExpandedEntityDataAccessor<Object>)k.value()).saveData(v, recordBuilder);
		});

		recordBuilder.build(new CompoundTag()).result().ifPresent(result -> {
			compound.put("expanded_sycned_data", result);
		});
	}

	@ApiStatus.Internal
	public <T> void setRaw(Holder<ExpandedEntityDataAccessor<?>> key, @NonNull Object val) {
		ResourceKey<ExpandedEntityDataAccessor<?>> rkey = toResourceKey(key);
		if (!this.registeredKeys.contains(rkey)) {
			throw new IllegalArgumentException("Unregistered key " + key.getRegisteredName());
		}

		// Normalize to the registered holder so the dataMap lookup works
		Holder<ExpandedEntityDataAccessor<?>> registered = this.registeredHolders.get(rkey);
		this.dataMap.put(registered != null ? registered : key, val);
	}

	public <T> void set(DeferredHolderShim<ExpandedEntityDataAccessor<?>, ExpandedEntityDataAccessor<T>> key, @NonNull T val) {
		ResourceKey<ExpandedEntityDataAccessor<?>> rkey = toResourceKey(key);
		if (!this.registeredKeys.contains(rkey)) {
			throw new IllegalArgumentException("Unregistered key " + key.getRegisteredName());
		}

		this.dataMap.put(key, val);

		if (this.isLogicalServer) {
			this.pendingDirtyData.put(key, val);
		}
	}

	public <T> T get(DeferredHolderShim<ExpandedEntityDataAccessor<?>, ExpandedEntityDataAccessor<T>> key) {
		ResourceKey<ExpandedEntityDataAccessor<?>> rkey = toResourceKey(key);
		if (!this.registeredKeys.contains(rkey)) {
			throw new IllegalArgumentException("Unregistered key " + key.getRegisteredName());
		}

		return this.dataMap.get(key);
	}

	@SuppressWarnings("unchecked")
	@Nullable
	public EpicFightNetworkManager.PayloadBundleBuilder prepareDataToSend() {
		if (this.pendingDirtyData.isEmpty()) {
			return null;
		}

		PayloadBundleBuilder payloadBundleBuilder = PayloadBundleBuilder.create();

		for (Map.Entry<Holder<ExpandedEntityDataAccessor<?>>, Object> dirtyData : this.pendingDirtyData.entrySet()) {
			FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
			((ExpandedEntityDataAccessor<Object>)dirtyData.getKey().value()).streamCodec().encode(buf, dirtyData.getValue());
			SPModifyExpandedEntityData payload = new SPModifyExpandedEntityData(this.entityIdProvider.get(), (Holder<ExpandedEntityDataAccessor<?>>)dirtyData.getKey(), buf);
			payloadBundleBuilder.and(payload);
		}

		this.pendingDirtyData.clear();

		return payloadBundleBuilder;
	}

	/// Extracts a ResourceKey from a Holder, supporting both DeferredHolderShim and vanilla Holder.Reference.
	/// This is necessary because DeferredHolderShim and vanilla Holder have incompatible equals() methods,
	/// so we compare by ResourceKey instead.
	@SuppressWarnings({"unchecked", "rawtypes"})
	private static ResourceKey<ExpandedEntityDataAccessor<?>> toResourceKey(Holder<ExpandedEntityDataAccessor<?>> holder) {
		if (holder instanceof DeferredHolderShim<?, ?> shim) {
			return (ResourceKey<ExpandedEntityDataAccessor<?>>) shim.getKey();
		}
		Optional<ResourceKey<ExpandedEntityDataAccessor<?>>> key = (Optional) holder.unwrapKey();
		return key.orElseThrow(() -> new IllegalArgumentException("Holder has no ResourceKey: " + holder));
	}
}
