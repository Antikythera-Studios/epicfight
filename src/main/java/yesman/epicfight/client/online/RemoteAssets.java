package yesman.epicfight.client.online;
import yesman.epicfight.EpicFight;

import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.client.model.Mesh;
import yesman.epicfight.client.online.texture.RemoteTexture;
import yesman.epicfight.main.EpicFightMod;
import yesman.epicfight.main.EpicFightSharedConstants;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class RemoteAssets {
	private static final RemoteAssets INSTANCE = new RemoteAssets();
	private static final TextureManager TEXTURE_MANAGER = Minecraft.getInstance().getTextureManager();
	
	public static RemoteAssets getInstance() {
		return INSTANCE;
	}
	
	private final Map<Integer, RemoteMeshAccessor> cachedMeshes = Maps.newConcurrentMap();
	
	public synchronized AssetAccessor<Mesh> getRemoteMesh(int seq, String path, @Nullable Consumer<Mesh> callback) {
		if (this.cachedMeshes.containsKey(seq)) {
			RemoteMeshAccessor cachedMesh = this.cachedMeshes.get(seq);
			
			if (callback != null) {
				if (cachedMesh.get() == null) {
					cachedMesh.addWork(callback);
				} else {
					callback.accept(cachedMesh.get());
				}
			}
			
			return cachedMesh;
		}
		
		RemoteMeshAccessor remoteMeshAccessor = new RemoteMeshAccessor();
		remoteMeshAccessor.addWork(callback);
		this.cachedMeshes.put(seq, remoteMeshAccessor);
		
		CompletableFuture.runAsync(() -> {
			EpicFightServerConnectionHelper.loadRemoteMesh(EpicFightSharedConstants.webServerDomain(), path, (mesh, exception) -> {
				if (exception != null) {
					EpicFight.LOGGER.error("Failed at loading remote mesh " + seq + ": " + exception.getMessage());
					exception.printStackTrace();
				} else {
					remoteMeshAccessor.load(mesh);
				}
			});
		});

		return this.cachedMeshes.get(seq);
	}
	
	public synchronized ResourceLocation getRemoteTexture(String fileName) {
		ResourceLocation textureLocation = ResourceLocation.fromNamespaceAndPath(EpicFight.EPICSKINS_MODID, "textures/remote/" + fileName);
		AbstractTexture texture = TEXTURE_MANAGER.getTexture(textureLocation, MissingTextureAtlasSprite.getTexture());
		
		if (texture == MissingTextureAtlasSprite.getTexture()) {
			AbstractTexture httptexture = new RemoteTexture(EpicFightSharedConstants.webServerDomain() + "/textures/" + fileName, MissingTextureAtlasSprite.getLocation());
			TEXTURE_MANAGER.register(textureLocation, httptexture);
		}
		
		return textureLocation;
	}

    private static class RemoteMeshAccessor implements AssetAccessor<Mesh> {
		private Queue<Consumer<Mesh>> callback = Queues.newArrayDeque();
		private Mesh mesh;
		
		public void addWork(Consumer<Mesh> callback) {
			this.callback.add(callback);
		}
		
		public void load(Mesh mesh) {
			this.mesh = mesh;
			
			Minecraft.getInstance().execute(() -> {
				this.callback.forEach((callback) -> callback.accept(mesh));
				this.callback.clear();
				this.callback = null;
			});
		}
		
		@Override
		public Mesh get() {
			return this.mesh;
		}
		
		@Override
		public ResourceLocation registryName() {
			return null;
		}
		
		@Override
		public boolean isPresent() {
			return this.mesh != null;
		}

		@Override
		public boolean inRegistry() {
			return false;
		}
	}
}
