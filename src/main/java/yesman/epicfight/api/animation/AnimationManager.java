package yesman.epicfight.api.animation;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gson.*;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;


import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import yesman.epicfight.EpicFight;
import yesman.epicfight.api.animation.property.AnimationProperty;
import yesman.epicfight.api.animation.types.DynamicAnimation;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.asset.JsonAssetLoader;
import yesman.epicfight.api.client.animation.AnimationSubFileReader;
import yesman.epicfight.api.data.reloader.SkillReloadListener;
import yesman.epicfight.api.exception.AssetLoadingException;
import yesman.epicfight.api.utils.InstantiateInvoker;
import yesman.epicfight.api.utils.MutableBoolean;
import yesman.epicfight.api.utils.side.ClientOnly;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.gameasset.Armatures;
import yesman.epicfight.main.EpicFightMod;
import yesman.epicfight.main.EpicFightSharedConstants;
import yesman.epicfight.network.EpicFightNetworkManager;
import yesman.epicfight.network.client.CPPairingAnimationRegistry;
import yesman.epicfight.network.server.SPDatapackSync;

import java.io.IOException;
import java.io.Reader;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("unchecked")
public class AnimationManager extends SimplePreparableReloadListener<List<ResourceLocation>> {
	private static final AnimationManager INSTANCE = new AnimationManager();
	private static ResourceManager serverResourceManager = null;
    private static final Gson GSON = new GsonBuilder().create();
    private static final String DIRECTORY = "animmodels/animations";

	public static AnimationManager getInstance() {
		return INSTANCE;
	}
	
	private final Map<Integer, AnimationAccessor<? extends StaticAnimation>> animationById = new HashMap<> ();
	private final Map<ResourceLocation, AnimationAccessor<? extends StaticAnimation>> animationByName = new HashMap<> ();
	private final Map<AnimationAccessor<? extends StaticAnimation>, StaticAnimation> animations = new HashMap<> ();
	private final Map<AnimationAccessor<? extends StaticAnimation>, String> resourcepackAnimationCommands = new HashMap<> ();

	/// Mutations applied to an animation object after its construction (EDP config
	/// files, virtualization protocols) live only on that instance — but {@link #apply}
	/// clears {@link #animations} on every reload, and the accessor's {@code onLoad}
	/// only replays construction-time properties. Journaled mutations are replayed
	/// onto each freshly built instance so config-file events/properties survive
	/// reloads. Keyed by registry name, not accessor instance, because datapack
	/// animations get a fresh accessor (with a fresh id) on every reload.
	private final PostLoadMutationJournal<ResourceLocation, StaticAnimation> postLoadMutations = new PostLoadMutationJournal<> ();

	/// Records a mutation so it is re-applied every time the accessor's animation is
	/// rebuilt after a reload. Call after mutating a loaded animation. Additive:
	/// every call appends to the journal, so the consumer replays once per call.
	public <T extends StaticAnimation> void journalPostLoadMutation(AnimationAccessor<? extends T> accessor, Consumer<T> mutation) {
		this.postLoadMutations.record(accessor.registryName(), mutation, (Consumer<StaticAnimation>) (Consumer<?>) mutation);
	}

	/// Applies a post-load mutation to the accessor's current animation and records
	/// it for replay on rebuilds, unless the exact {@code source} object was already
	/// recorded under the accessor's registry name, in which case this is a no-op.
	/// {@code accessor.get()} runs first so journaled mutations replay onto a fresh
	/// instance before the new one applies. A failed mutation is never recorded.
	public <T extends StaticAnimation> void applyPostLoadMutation(AnimationAccessor<? extends T> accessor, Object source, Consumer<T> mutation) {
		this.postLoadMutations.apply(accessor.registryName(), source, accessor.get(), (Consumer<StaticAnimation>) (Consumer<?>) mutation);
	}

	private void replayPostLoadMutations(ResourceLocation registryName, StaticAnimation animation) {
		this.postLoadMutations.replay(registryName, animation);
	}

	/**
	 * Monotonic id source for animation accessors. Ids must never be reused while
	 * {@link #animationById} retains entries: {@link #apply} clears {@link #animations}
	 * on reload but keeps {@code inRegistry} accessors in the id map, so deriving ids
	 * from {@code animations.size()} would recycle ids already owned by surviving
	 * accessors and silently re-alias {@link #byId} to an unrelated animation.
	 */
	private final java.util.concurrent.atomic.AtomicInteger animationIdSource = new java.util.concurrent.atomic.AtomicInteger(0);

	public static int allocateAnimationId() {
		return INSTANCE.animationIdSource.incrementAndGet();
	}

	public static boolean checkNull(AssetAccessor<? extends StaticAnimation> animation) {
		if (animation == null || animation.isEmpty()) {
			if (animation != null) {
				EpicFightMod.stacktraceIfDevSide("Empty animation accessor: " + animation.registryName(), NoSuchElementException::new);
			} else {
				EpicFightMod.stacktraceIfDevSide("Null animation accessor", NoSuchElementException::new);
			}
			
			return true;
		}
		
		return false;
	}
	
	public static <T extends StaticAnimation> AnimationAccessor<T> byKey(String registryName) {
		return byKey(ResourceLocation.parse(registryName));
	}
	
	public static <T extends StaticAnimation> AnimationAccessor<T> byKey(ResourceLocation registryName) {
		return (AnimationAccessor<T>)getInstance().animationByName.get(registryName);
	}
	
	public static <T extends StaticAnimation> AnimationAccessor<T> byId(int animationId) {
		return (AnimationAccessor<T>)getInstance().animationById.get(animationId);
	}

    /// Use the transient accessor when you need to get accessors before animations are loaded, especailly a data pack loading stage
    public static <T extends StaticAnimation> AnimationAccessor<T> trasientAccessor(ResourceLocation registryName) {
        return new TransientAnimationAccessor<> (registryName);
    }

	public Map<ResourceLocation, AnimationAccessor<? extends StaticAnimation>> getAnimations(Predicate<AssetAccessor<? extends StaticAnimation>> filter) {
		Map<ResourceLocation, AnimationAccessor<? extends StaticAnimation>> filteredItems =
			this.animationByName.entrySet().stream()
				.filter(entry -> filter.test(entry.getValue()))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
		
		return ImmutableMap.copyOf(filteredItems);
	}
	
	public AnimationClip loadAnimationClip(StaticAnimation animation, BiFunction<JsonAssetLoader, StaticAnimation, AnimationClip> clipLoader) {
		try {
			if (getAnimationResourceManager() == null) {
				return null;
			}

			JsonAssetLoader modelLoader = new JsonAssetLoader(getAnimationResourceManager(), animation.getLocation());

            return clipLoader.apply(modelLoader, animation);
		} catch (AssetLoadingException e) {
			throw new AssetLoadingException("Failed to load animation clip from: " + animation, e);
		}
	}
	
	public static void readAnimationProperties(StaticAnimation animation) {
		ResourceLocation dataLocation = getSubAnimationFileLocation(animation.getLocation(), AnimationSubFileReader.SUBFILE_CLIENT_PROPERTY);
		ResourceLocation povLocation = getSubAnimationFileLocation(animation.getLocation(), AnimationSubFileReader.SUBFILE_POV_ANIMATION);

		getAnimationResourceManager().getResource(dataLocation).ifPresent((rs) -> {
			AnimationSubFileReader.readAndApply(animation, rs, AnimationSubFileReader.SUBFILE_CLIENT_PROPERTY);
		});

		getAnimationResourceManager().getResource(povLocation).ifPresent((rs) -> {
			AnimationSubFileReader.readAndApply(animation, rs, AnimationSubFileReader.SUBFILE_POV_ANIMATION);
		});
	}

	@Override
	protected @NotNull List<ResourceLocation> prepare(@NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profilerIn) {
		if (!EpicFightSharedConstants.isPhysicalClient() && serverResourceManager == null) {
			serverResourceManager = resourceManager;
		}

		this.animationById.entrySet().removeIf(entry -> !entry.getValue().inRegistry());
		this.animationByName.entrySet().removeIf(entry -> !entry.getValue().inRegistry());
		this.resourcepackAnimationCommands.clear();

        List<ResourceLocation> directories = new ArrayList<> ();
        scanDirectoryNames(resourceManager, directories);

		return directories;
	}

    private static void scanDirectoryNames(ResourceManager resourceManager, List<ResourceLocation> output) {
        FileToIdConverter filetoidconverter = FileToIdConverter.json(DIRECTORY);
        filetoidconverter.listMatchingResources(resourceManager).keySet().stream().map(AnimationManager::pathToId).forEach(output::add);
    }

	@Override
	protected void apply(List<ResourceLocation> objects, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profilerIn) {
		if (!objects.isEmpty() || !EpicFightSharedConstants.isPhysicalClient()) {
			this.animations.clear();
		}

		Armatures.reload(resourceManager);

		Set<ResourceLocation> registeredAnimation =
            this.animationById.values().stream()
                .reduce(
                    new HashSet<> (),
                    (set, accessor) -> {
                        set.add(accessor.registryName());

                        for (AssetAccessor<? extends StaticAnimation> subAnimAccessor : accessor.get().getSubAnimations()) {
                            set.add(subAnimAccessor.registryName());
                        }

                        return set;
                    },
                    (set1, set2) -> {
                        set1.addAll(set2);
                        return set1;
                    }
                );

		// Load animations that are not registered by AnimationRegistryEvent
        // Reads from /assets folder in physical client, /datapack in physical server.
        objects.stream()
            .filter(animId -> !registeredAnimation.contains(animId) && !animId.getPath().contains("/data/") && !animId.getPath().contains("/pov/"))
			.sorted(Comparator.comparing(ResourceLocation::toString))
			.forEach(animId -> {
                Optional<Resource> resource = resourceManager.getResource(idToPath(animId));

                try (Reader reader = resource.orElseThrow().openAsReader()) {
                    JsonElement jsonelement = GsonHelper.fromJson(GSON, reader, JsonElement.class);
                    this.readResourcepackAnimation(animId, jsonelement.getAsJsonObject());
                } catch (IOException | JsonParseException | IllegalArgumentException resourceReadException) {
                    EpicFight.LOGGER.error("Couldn't parse animation data from {}", animId, resourceReadException);
                } catch (Exception e) {
                    EpicFight.LOGGER.error("Failed at constructing {}", animId, e);
                }
            });

		SkillReloadListener.reloadAllSkillsAnimations();

		this.animations.entrySet().stream()
            .reduce(
                new ArrayList<AssetAccessor<? extends StaticAnimation>>(),
                (list, entry) -> {
                    MutableBoolean init = new MutableBoolean(true);

                    if (entry.getValue() == null || entry.getValue().getAccessor() == null) {
                        EpicFightMod.logAndStacktraceIfDevSide(Logger::error, "Invalid animation implementation: " + entry.getKey(), AssetLoadingException::new);
                        init.set(false);
                    }

                    entry.getValue().getSubAnimations().forEach((subAnimation) -> {
                        if (subAnimation == null || subAnimation.get() == null) {
                            EpicFightMod.logAndStacktraceIfDevSide(Logger::error, "Invalid sub animation implementation: " + entry.getKey(), AssetLoadingException::new);
                            init.set(false);
                        }
                    });

                    if (init.value()) {
                        list.add(entry.getValue().getAccessor());
                        list.addAll(entry.getValue().getSubAnimations());
                    }

                    return list;
                },
                (list1, list2) -> {
                    list1.addAll(list2);
                    return list1;
                }
            )
            .forEach(accessor -> {
                accessor.doOrThrow(StaticAnimation::postInit);

                if (EpicFightSharedConstants.isPhysicalClient()) {
                    AnimationManager.readAnimationProperties(accessor.get());
                }
            });
	}
	
	public static ResourceLocation getSubAnimationFileLocation(ResourceLocation location, AnimationSubFileReader.SubFileType<?> subFileType) {
		int splitIdx = location.getPath().lastIndexOf('/');
		
		if (splitIdx < 0) {
			splitIdx = 0;
		}
		
		return ResourceLocation.fromNamespaceAndPath(location.getNamespace(), String.format("%s/" + subFileType.getDirectory() + "%s", location.getPath().substring(0, splitIdx), location.getPath().substring(splitIdx)));
	}

    /// Converts animation id, acquired by [StaticAnimation#getRegistryName], to animation resource path acquired by [StaticAnimation#getLocation]
    public static ResourceLocation idToPath(ResourceLocation rl) {
        return rl.getPath().matches(DIRECTORY + "/.*\\.json") ? rl : ResourceLocation.fromNamespaceAndPath(rl.getNamespace(), DIRECTORY + "/" + rl.getPath() + ".json");
    }

    /// Converts animation resource path, acquired by [StaticAnimation#getLocation], to animation id acquired by [StaticAnimation#getRegistryName]
    public static ResourceLocation pathToId(ResourceLocation rl) {
        return ResourceLocation.fromNamespaceAndPath(rl.getNamespace(), rl.getPath().replace(DIRECTORY + "/", "").replace(".json", ""));
    }

	public static void setServerResourceManager(ResourceManager pResourceManager) {
		serverResourceManager = pResourceManager;
	}
	
	public static ResourceManager getAnimationResourceManager() {
		return EpicFightSharedConstants.isPhysicalClient() ? Minecraft.getInstance().getResourceManager() : serverResourceManager;
	}
	
	public Stream<CompoundTag> getResourcepackAnimationStream() {
		return this.resourcepackAnimationCommands.entrySet().stream().map((entry) -> {
			CompoundTag compTag = new CompoundTag();
			compTag.putString("registry_name", entry.getKey().registryName().toString());
			compTag.putInt("id", entry.getKey().id());
			compTag.putString("invoke_command", entry.getValue());
			
			return compTag;
		});
	}
	
	/**
	 * @param mandatoryPack : creates dummy animations for animations from the server without animation clips when the server has mandatory resource pack.
	 *                        custom weapon types & mob capabilities won't be created because they won't be able to find the animations from the server
	 *                        dummy animations will be automatically removed right after reloading resourced as the server forces using resource pack
	 */
    @ClientOnly
	public void processServerPacket(SPDatapackSync packet, boolean mandatoryPack) {
		if (mandatoryPack) {
			for (CompoundTag tag : packet.tags()) {
				String invocationCommand = tag.getString("invoke_command");
				ResourceLocation registryName = ResourceLocation.parse(tag.getString("registry_name"));
				int id = tag.getInt("id");
						
				if (this.animationByName.containsKey(registryName)) {
					continue;
				}
				
				AnimationAccessor<? extends StaticAnimation> accessor = AnimationAccessorImpl.create(registryName, this.resourcepackAnimationCommands.size(), false, (accessor$2) -> {
					try {
						return InstantiateInvoker.invoke(invocationCommand, StaticAnimation.class).getResult();
					} catch (Exception e) {
						EpicFight.LOGGER.warn("Failed at creating animation from server resource pack", e);
						return Animations.EMPTY_ANIMATION;
					}
				});
				
				this.animationById.put(id, accessor);
				this.animationByName.put(registryName, accessor);
			}
		}
		
		List<String> registryNames = this.animationById.values().stream()
			.sorted(Comparator.comparingInt(AnimationAccessor::id))
			.map(accessor -> accessor.registryName().toString())
			.collect(Collectors.toList());
		
		CPPairingAnimationRegistry registrySyncPacket = new CPPairingAnimationRegistry(registryNames);
		EpicFightNetworkManager.sendToServer(registrySyncPacket);
	}
	
	public void validateClientAnimationRegistry(CPPairingAnimationRegistry msg, ServerGamePacketListenerImpl connection) {
		StringBuilder messageBuilder = new StringBuilder();
		int count = 0;
		
		Set<String> clientAnimationRegistry = new HashSet<> (Set.copyOf(msg.registryNames()));
		
		for (String registryName : this.animations.keySet().stream().map(Object::toString).toList()) {
			if (!clientAnimationRegistry.contains(registryName)) {
				// Animations that don't exist in client
				if (count < 10) {
					messageBuilder.append(registryName);
					messageBuilder.append("\n");
				}
				
				count++;
			} else {
				clientAnimationRegistry.remove(registryName);
			}
		}
		
		// Animations that don't exist in server
		for (String registryName : clientAnimationRegistry) {
			if (registryName.equals("empty")) {
				continue;
			}
			
			if (count < 10) {
				messageBuilder.append(registryName);
				messageBuilder.append("\n");
			}
			
			count++;
		}
		
		if (count >= 10) {
			messageBuilder.append(Component.translatable("gui.epicfight.warn.animation_unsync.etc", (count - 9)).getString());
			messageBuilder.append("\n");
		}
		
		if (!messageBuilder.isEmpty()) {
			connection.disconnect(Component.translatable("gui.epicfight.warn.animation_unsync", messageBuilder.toString()));
		}
	}
	
	private static final Set<String> NO_WARNING_MODID = Sets.newHashSet();
	
	public static void addNoWarningModId(String modid) {
		NO_WARNING_MODID.add(modid);
	}
	
	/**************************************************
	 * User-animation loader
	 **************************************************/
	@SuppressWarnings({ "deprecation" })
	private void readResourcepackAnimation(ResourceLocation rl, JsonObject json) throws Exception {
		JsonElement constructorElement = json.get("constructor");
		
		if (constructorElement == null) {
			if (NO_WARNING_MODID.contains(rl.getNamespace())) {
				return;
			} else {
				EpicFightMod.logAndStacktraceIfDevSide(
					  Logger::error
					, "Datapack animation reading failed: No constructor information has provided: " + rl
					, IllegalStateException::new
					, "No constructor information has provided in User animation, " + rl + "\nPlease remove this resource if it's unnecessary to optimize your project."
				);
				return;
			}
		}
		
		JsonObject constructorObject = constructorElement.getAsJsonObject();
		String invocationCommand = constructorObject.get("invocation_command").getAsString();
		StaticAnimation animation = InstantiateInvoker.invoke(invocationCommand, StaticAnimation.class).getResult();
		JsonElement propertiesElement = json.getAsJsonObject().get("properties");
		
		if (propertiesElement != null) {
			JsonObject propertiesObject = propertiesElement.getAsJsonObject();
			
			for (Map.Entry<String, JsonElement> entry : propertiesObject.entrySet()) {
				AnimationProperty<?> propertyKey = AnimationProperty.getSerializableProperty(entry.getKey());
				Object value = propertyKey.parseFrom(entry.getValue());
				animation.addPropertyUnsafe(propertyKey, value);
			}
		}
		
		AnimationAccessor<StaticAnimation> accessor = AnimationAccessorImpl.create(rl, allocateAnimationId(), false, null);
		animation.setAccessor(accessor);
		
		this.resourcepackAnimationCommands.put(accessor, invocationCommand);
		this.animationById.put(accessor.id(), accessor);
		this.animationByName.put(accessor.registryName(), accessor);
		this.animations.put(accessor, animation);
		this.replayPostLoadMutations(rl, animation);
	}

	public interface AnimationAccessor<A extends DynamicAnimation> extends AssetAccessor<A> {
		int id();
		
		default boolean idBetween(AnimationAccessor<? extends StaticAnimation> a1, AnimationAccessor<? extends StaticAnimation> a2) {
			return a1.id() <= this.id() && a2.id() >= this.id();
		}
	}

    public record TransientAnimationAccessor<A extends StaticAnimation> (ResourceLocation registryName) implements AnimationAccessor<A> {
        @Override
        public A get() {
            return (A)INSTANCE.animationByName.get(registryName).get();
        }

        @Override
        public int id() {
            return INSTANCE.animationByName.get(registryName).id();
        }

        @Override
        public boolean inRegistry() {
            return false;
        }
    }

	public record AnimationAccessorImpl<A extends StaticAnimation> (ResourceLocation registryName, int id, boolean inRegistry, Function<AnimationAccessor<A>, A> onLoad) implements AnimationAccessor<A> {
		private static <A extends StaticAnimation> AnimationAccessor<A> create(ResourceLocation registryName, int id, boolean inRegistry, Function<AnimationAccessor<A>, A> onLoad) {
			return new AnimationAccessorImpl<A> (registryName, id, inRegistry, onLoad);
		}
		
		@Override
		public A get() {
			if (!INSTANCE.animations.containsKey(this) || INSTANCE.animations.get(this) == null) {
				A loaded = this.onLoad.apply(this);
				if (EpicFight.LOGGER.isDebugEnabled() && loaded != null && loaded.getLocation() != null && !INSTANCE.animations.containsValue(loaded)) {
					EpicFight.LOGGER.debug("[EFDBG] anim load: {} id={} loc={} obj={}", this.registryName(), this.id(), loaded.getLocation(), System.identityHashCode(loaded));
				}
				INSTANCE.animations.put(this, loaded);
				if (loaded != null) {
					try {
						INSTANCE.replayPostLoadMutations(this.registryName, loaded);
					} catch (RuntimeException | Error e) {
						INSTANCE.animations.remove(this, loaded);
						throw e;
					}
				}
			}

			return (A)INSTANCE.animations.get(this);
		}

        @Override
		public @NotNull String toString() {
			return this.registryName.toString();
		}

        @Override
		public int hashCode() {
			return this.registryName.hashCode();
		}
		
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			} else if (obj instanceof AnimationAccessor<?> animationAccessor) {
				return this.registryName.equals(animationAccessor.registryName());
			} else if (obj instanceof ResourceLocation rl) {
				return this.registryName.equals(rl);
			} else if (obj instanceof String name) {
				return this.registryName.toString().equals(name);
			} else {
				return false;
			}
		}
	}
	
	/// Posted on the shared mod bus once all mod construction is done — lets addon
	/// mods contribute builders via {@code @SubscribeEvent} handlers (upstream
	/// semantics), instead of only the hardcoded registrations in EpicFightFabric.
	public static class AnimationRegistryEvent extends net.neoforged.bus.api.Event {
		private final List<AnimationBuilder> builders = Lists.newArrayList();
		private final Set<String> namespaces = Sets.newHashSet();
		
		public void newBuilder(String namespace, Consumer<AnimationBuilder> build) {
			if (this.namespaces.contains(namespace)) {
				throw new IllegalArgumentException("Animation builder namespace '" + namespace + "' already exists!");
			}
			
			this.namespaces.add(namespace);
			this.builders.add(new AnimationBuilder(namespace, build));
		}
		
		public List<AnimationBuilder> getBuilders() {
			return this.builders;
		}
	}
	
	public record AnimationBuilder(String namespace, Consumer<AnimationBuilder> task) {
		public <T extends StaticAnimation> AnimationManager.AnimationAccessor<T> nextAccessor(String id, Function<AnimationManager.AnimationAccessor<T>, T> onLoad) {
			AnimationAccessor<T> accessor = AnimationAccessorImpl.create(ResourceLocation.fromNamespaceAndPath(this.namespace, id), allocateAnimationId(), true, onLoad);
			
			INSTANCE.animationById.put(accessor.id(), accessor);
			INSTANCE.animationByName.put(accessor.registryName(), accessor);
			INSTANCE.animations.put(accessor, null);
			
			return accessor; 
		}
	}
}
