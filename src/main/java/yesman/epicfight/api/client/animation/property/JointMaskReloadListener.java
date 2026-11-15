package yesman.epicfight.api.client.animation.property;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import yesman.epicfight.api.client.animation.property.JointMask.BindModifier;
import yesman.epicfight.api.client.animation.property.JointMask.JointMaskSet;
import yesman.epicfight.EpicFight;

public class JointMaskReloadListener extends SimpleJsonResourceReloadListener {
	private static final BiMap<ResourceLocation, JointMaskSet> JOINT_MASKS = HashBiMap.create();
	private static final Map<String, JointMask.BindModifier> BIND_MODIFIERS = Maps.newHashMap();
    private static final ResourceLocation NONE_MASK = EpicFight.identifier("none");
	
	static {
		BIND_MODIFIERS.put("keep_child_locrot", JointMask.KEEP_CHILD_LOCROT);
	}
	
	public static JointMaskSet getJointMaskEntry(String type) {
		ResourceLocation rl = ResourceLocation.parse(type);
		return JOINT_MASKS.getOrDefault(rl, JOINT_MASKS.get(NONE_MASK));
	}
	
	public static JointMaskSet getNoneMask() {
		return JOINT_MASKS.get(NONE_MASK);
	}
	
	public static ResourceLocation getKey(JointMaskSet type) {
		return JOINT_MASKS.inverse().get(type);
	}
	
	public static Set<Map.Entry<ResourceLocation, JointMaskSet>> entries() {
		return JOINT_MASKS.entrySet();
	}
	
	public JointMaskReloadListener() {
		super((new GsonBuilder()).create(), "animmodels/joint_mask");
	}
	
	@Override
	protected void apply(Map<ResourceLocation, JsonElement> objectIn, ResourceManager resourceManager, ProfilerFiller profileFiller) {
		JOINT_MASKS.clear();
		
		for (Map.Entry<ResourceLocation, JsonElement> entry : objectIn.entrySet()) {
			Set<JointMask> masks = Sets.newHashSet();
			JsonObject object = entry.getValue().getAsJsonObject();
			JsonArray joints = object.getAsJsonArray("joints");
			JsonObject bindModifiers = object.has("bind_modifiers") ? object.getAsJsonObject("bind_modifiers") : null;
			
			for (JsonElement joint : joints) {
				String jointName = joint.getAsString();
				BindModifier modifier = null;
				
				if (bindModifiers != null) {
					String modifierName = bindModifiers.has(jointName) ? bindModifiers.get(jointName).getAsString() : null;
					modifier = BIND_MODIFIERS.get(modifierName);
				}
				
				masks.add(JointMask.of(jointName, modifier));
			}
			
			String path = entry.getKey().toString();
			ResourceLocation key = ResourceLocation.fromNamespaceAndPath(entry.getKey().getNamespace(), path.substring(path.lastIndexOf("/") + 1));
			
			JOINT_MASKS.put(key, JointMaskSet.of(masks));
		}
	}
}