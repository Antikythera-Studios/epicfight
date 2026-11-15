package yesman.epicfight.world.capabilities.item;
import yesman.epicfight.EpicFight;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import yesman.epicfight.api.utils.ParseUtil;
import yesman.epicfight.api.utils.side.ClientOnly;
import yesman.epicfight.main.EpicFightMod;
import yesman.epicfight.network.server.SPDatapackSync;

import java.io.IOException;
import java.io.Reader;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ItemKeywordReloadListener extends SimplePreparableReloadListener<Map<ResourceLocation, List<JsonElement>>> {
	private static final String DIRECTORY = "capabilities/weapons/item_keyword";
	private static final Gson GSON = new GsonBuilder().create();
	
	private static final Map<ResourceLocation, ItemRegex> REGEXES = new HashMap<> ();
	private static final List<CompoundTag> COMPOUNDS = new ArrayList<> ();
	
	public static void scanDirectory(ResourceManager pResourceManager, String pName, Gson pGson, Map<ResourceLocation, List<JsonElement>> pOutput) {
		FileToIdConverter filetoidconverter = FileToIdConverter.json(pName);
		
		for (Map.Entry<ResourceLocation, List<Resource>> entry : filetoidconverter.listMatchingResourceStacks(pResourceManager).entrySet()) {
			ResourceLocation resourcelocation = entry.getKey();
			ResourceLocation resourcelocation1 = filetoidconverter.fileToId(resourcelocation);
			
			for (Resource resource : entry.getValue()) {
				try (Reader reader = resource.openAsReader()) {
					JsonElement jsonelement = GsonHelper.fromJson(pGson, reader, JsonElement.class);
					List<JsonElement> list = pOutput.computeIfAbsent(resourcelocation1, k -> new ArrayList<> ());
					list.add(jsonelement);
				} catch (IllegalArgumentException | IOException | JsonParseException jsonparseexception) {
					EpicFight.LOGGER.error("Couldn't parse data file {} from {}", resourcelocation1, resourcelocation, jsonparseexception);
				}
			}
		}
	}
	
	@Override
	protected Map<ResourceLocation, List<JsonElement>> prepare(ResourceManager pResourceManager, ProfilerFiller pProfiler) {
		Map<ResourceLocation, List<JsonElement>> map = new HashMap<> ();
		scanDirectory(pResourceManager, DIRECTORY, GSON, map);
		return map;
	}
	
	@Override
	protected void apply(Map<ResourceLocation, List<JsonElement>> packEntry, ResourceManager resourceManager, ProfilerFiller profilerFiller) {
		REGEXES.clear();
		COMPOUNDS.clear();
		
		Map<ResourceLocation, List<String>> regexMap = new HashMap<> ();
		
		packEntry.entrySet().stream()
			.forEach(entry -> {
				for (JsonElement jsonelement : entry.getValue()) {
					CompoundTag compound = ParseUtil.parseTagOrThrow(jsonelement);
					
					if (!compound.contains("regexes", Tag.TAG_LIST)) {
						throw new NoSuchElementException("No regexes provided");
					}
					
					List<String> regexes = compound.getList("regexes", Tag.TAG_STRING).stream().map(Tag::getAsString).collect(Collectors.toList());
					List<String> list = regexMap.computeIfAbsent(entry.getKey(), k -> new ArrayList<> ());
					list.addAll(regexes);
				}
			});
		
		regexMap.forEach((k, v) -> {
			REGEXES.put(k, new ItemRegex(v));
		});
		
		REGEXES.forEach((k, v) -> {
			CompoundTag compound = new CompoundTag();
			compound.putString("registry_name", k.toString());
			ListTag list = new ListTag();
			v.regexes().stream().map(StringTag::valueOf).forEach(list::add);
			compound.put("regexes", list);
			COMPOUNDS.add(compound);
		});
	}
	
	public static Map<ResourceLocation, ItemRegex> getRegexes() {
		return ImmutableMap.copyOf(REGEXES);
	}
	
	public static Stream<CompoundTag> getCompounds() {
		return COMPOUNDS.stream();
	}

    @ClientOnly
	public static void handleClientBoundSyncPacket(SPDatapackSync packet) {
		if (packet.packetType() == SPDatapackSync.PacketType.WEAPON_TYPE) {
			REGEXES.clear();
			
			for (CompoundTag tag : packet.tags()) {
				ResourceLocation id = ResourceLocation.parse(tag.getString("registry_name"));
				REGEXES.put(id, ItemRegex.deserialize(tag));
			}
		}
	}
	
	public static record ItemRegex(List<String> regexes) {
		public static ItemRegex deserialize(CompoundTag compound) {
			if (!compound.contains("regexes", Tag.TAG_LIST)) {
				throw new NoSuchElementException("No regexes provided");
			}
			
			List<String> regexes = compound.getList("regexes", Tag.TAG_STRING).stream().map(Tag::getAsString).collect(Collectors.toList());
			return new ItemRegex(regexes);
		}
		
		public boolean matchesAny(String itemName) {
			for (String regex : this.regexes()) {
				if (itemName.matches(regex)) {
					return true;
				}
			}
			
			return false;
		}
	}
}