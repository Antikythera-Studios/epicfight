package yesman.epicfight.skill;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import yesman.epicfight.registry.deferred_shim.DeferredHolderShim;
import org.jetbrains.annotations.ApiStatus;
import yesman.epicfight.api.utils.side.ClientOnly;
import yesman.epicfight.network.EpicFightNetworkManager;
import yesman.epicfight.network.client.CPHandleSkillData;
import yesman.epicfight.network.server.SPHandleSkillData;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

public class SkillDataManager {
	private final Map<Holder<SkillDataKey<?>>, Object> data = new HashMap<> ();
	private final SkillContainer container;

	public SkillDataManager(SkillContainer container) {
		this.container = container;
	}

	/// Normalizes a Holder key to its underlying vanilla Holder.Reference.
	/// This is necessary because registerData() receives vanilla Holder.Reference instances
	/// (from registry callbacks), while setData()/getDataValue() receive DeferredHolderShim instances.
	/// Without normalization, HashMap lookups fail due to mismatched equals()/hashCode().
	@SuppressWarnings("unchecked")
	private Holder<SkillDataKey<?>> normalizeKey(Holder<SkillDataKey<?>> key) {
		if (key instanceof DeferredHolderShim<?, ?> shim) {
			Holder<?> underlying = shim.asHolder();
			if (underlying != null) {
				return (Holder<SkillDataKey<?>>) underlying;
			}
		}
		return key;
	}

	public void registerData(Holder<SkillDataKey<?>> key) {
		key = normalizeKey(key);
		if (this.hasData(key)) {
			throw new IllegalStateException(key + " is already registered!");
		}

		this.data.put(key, key.value().defaultValue());
	}

	public void transferDataTo(SkillDataManager dest) {
		dest.data.putAll(this.data);
	}

	public void removeData(Holder<SkillDataKey<?>> key) {
		this.data.remove(normalizeKey(key));
	}

	public Set<Holder<SkillDataKey<?>>> keySet() {
		return this.data.keySet();
	}

	/**
	 * Use setData() or setDataSync() which is type-safe
	 */
	@ApiStatus.Internal
	public void setDataRawtype(Holder<SkillDataKey<?>> key, Object data) {
		key = normalizeKey(key);
		if (!this.data.containsKey(key)) {
			throw new IllegalStateException(key + " is unregistered.");
		}

		this.data.put(key, data);
	}

	public <T> void setData(DeferredHolderShim<SkillDataKey<?>, ? extends SkillDataKey<T>> key, T data) {
		this.setDataRawtype(key, data);
	}

	public <T> void setDataF(DeferredHolderShim<SkillDataKey<?>, ? extends SkillDataKey<T>> key, Function<T, T> dataMapper) {
		this.setDataRawtype(key, dataMapper.apply(this.getDataValue(key)));
	}

	public <T> void setDataSync(DeferredHolderShim<SkillDataKey<?>, ? extends SkillDataKey<T>> key, T data) {
		this.setData(key, data);

		if (!this.container.getExecutor().isLogicalClient()) {
			this.syncServerPlayerData(key, this.container.getServerExecutor().getOriginal());
		} else {
			this.syncLocalPlayerData(key, this.container.getClientExecutor().getOriginal());
		}
	}

	public <T> void setDataSyncF(DeferredHolderShim<SkillDataKey<?>, ? extends SkillDataKey<T>> key, Function<T, T> dataManipulator) {
		this.setDataF(key, dataManipulator);

		if (!this.container.getExecutor().isLogicalClient()) {
			this.syncServerPlayerData(key, this.container.getServerExecutor().getOriginal());
		} else {
			this.syncLocalPlayerData(key, this.container.getClientExecutor().getOriginal());
		}
	}
	
	private <T> void syncServerPlayerData(DeferredHolderShim<SkillDataKey<?>, ? extends SkillDataKey<T>> key, ServerPlayer serverplayer) {
		SPHandleSkillData msg = new SPHandleSkillData(SPHandleSkillData.WorkType.MODIFY, this.container.getSlot(), serverplayer.getId(), key);
		@SuppressWarnings("unchecked") Object dataValue = this.getDataValue((DeferredHolderShim) key); ((SkillDataKey) key.value()).encode(msg.buffer(), dataValue);
		EpicFightNetworkManager.sendToPlayer(msg, serverplayer);
		
		if (key.value().syncronizeToRemotePlayers()) {
			EpicFightNetworkManager.sendToAllPlayerTrackingThisEntity(msg, serverplayer);
		}
	}

    @ClientOnly
	private <T> void syncLocalPlayerData(DeferredHolderShim<SkillDataKey<?>, ? extends SkillDataKey<T>> key, LocalPlayer player) {
		CPHandleSkillData msg = new CPHandleSkillData(this.container.getSlot(), key);
		@SuppressWarnings("unchecked") Object dataValue = this.getDataValue((DeferredHolderShim) key); ((SkillDataKey) key.value()).encode(msg.buffer(), dataValue);
		EpicFightNetworkManager.sendToServer(msg);
	}
	
	@SuppressWarnings("unchecked")
	public void onTracked(EpicFightNetworkManager.PayloadBundleBuilder bundleBuilder) {
		this.data.forEach((key, val) -> {
			if (key.value().syncronizeToRemotePlayers()) {
				SPHandleSkillData msg = new SPHandleSkillData(SPHandleSkillData.WorkType.MODIFY, this.container.getSlot(), this.container.executor.getOriginal().getId(), key);
				((SkillDataKey<Object>)key.value()).encode(msg.buffer(), val);
				
				bundleBuilder.and(msg);
			}
		});
	}
	
	@SuppressWarnings("unchecked")
	public <T> T getDataValue(DeferredHolderShim<SkillDataKey<?>, ? extends SkillDataKey<T>> key) {
		Holder<SkillDataKey<?>> normalized = normalizeKey(key);
		return this.hasData(normalized) ? (T)this.data.get(normalized) : null;
	}

	@SuppressWarnings("unchecked")
	public <T> Optional<T> getDataValueOptional(DeferredHolderShim<SkillDataKey<?>, ? extends SkillDataKey<T>> key) {
		return Optional.ofNullable((T)this.data.get(normalizeKey(key)));
	}

	@ApiStatus.Internal
	public Object getRawDataValue(Holder<SkillDataKey<?>> key) {
		return this.data.get(normalizeKey(key));
	}

	public boolean hasData(Holder<SkillDataKey<?>> key) {
		return this.data.containsKey(normalizeKey(key));
	}
	
	public void clearData() {
		this.data.clear();
	}
}