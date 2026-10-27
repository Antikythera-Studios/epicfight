package yesman.epicfight.api.client.model.transformer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import net.minecraft.SharedConstants;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import yesman.epicfight.api.client.model.SkinnedMesh;
import yesman.epicfight.client.mesh.HumanoidMesh;
import yesman.epicfight.main.EpicFightMod;

public class HumanoidModelBaker {
	static final Map<ResourceLocation, SkinnedMesh> BAKED_MODELS = Maps.newHashMap();
	static final List<HumanoidModelTransformer> MODEL_TRANSFORMERS = Lists.newArrayList();
	
	static final Set<ArmorItem> EXCEPTIONAL_MODELS = Sets.newHashSet();
	static final Set<ModelPart> MODEL_PARTS = Sets.newHashSet();
	
	public static final HumanoidModelTransformer VANILLA_TRANSFORMER = new VanillaModelTransformer();
	
	public interface ModelProvider {
		public Model get(LivingEntity entityLiving, ItemStack itemStack, EquipmentSlot slot, HumanoidModel<?> _default);
	}
	
	public static void registerNewTransformer(HumanoidModelTransformer transformer) {
		MODEL_TRANSFORMERS.add(transformer);
	}
	
	public static void exportModels(File resourcePackDirectory) throws IOException {
		File zipFile = new File(resourcePackDirectory, "epicfight_custom_armors.zip");
		ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipFile));
		
		for (Map.Entry<ResourceLocation, SkinnedMesh> entry : BAKED_MODELS.entrySet()) {
			ZipEntry zipEntry = new ZipEntry(String.format("assets/%s/animmodels/armor/%s.json", entry.getKey().getNamespace(), entry.getKey().getPath()));
			Gson gson = new GsonBuilder().create();
			out.putNextEntry(zipEntry);
			out.write(gson.toJson(entry.getValue().toJsonObject()).getBytes());
			out.closeEntry();
			EpicFightMod.LOGGER.info("Exported custom armor model : " + entry.getKey());
		}
		
		ZipEntry zipEntry = new ZipEntry("pack.mcmeta");
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		JsonObject root = new JsonObject();
		JsonObject pack = new JsonObject();
		pack.addProperty("description", "epicfight_custom_armor_models");
		pack.addProperty("pack_format", SharedConstants.getCurrentVersion().getPackVersion(PackType.CLIENT_RESOURCES));
		root.add("pack", pack);
		out.putNextEntry(zipEntry);
		out.write(gson.toJson(root).getBytes());
		out.closeEntry();
		out.close();
	}
	
	public static SkinnedMesh bakeArmor(LivingEntity entityLiving, ItemStack itemstack, ArmorItem armorItem, EquipmentSlot slot, HumanoidModel<?> originalModel, Model forgeModel, HumanoidModel<?> entityModel, HumanoidMesh entityMesh) {
		SkinnedMesh skinnedArmorModel = null;
		
		if (!EXCEPTIONAL_MODELS.contains(armorItem)) {
			if (forgeModel == originalModel || !(forgeModel instanceof HumanoidModel humanoidModel)) {
				return entityMesh.getHumanoidArmorModel(slot).get();
			}
			
			for (HumanoidModelTransformer modelTransformer : MODEL_TRANSFORMERS) {
				try {
					skinnedArmorModel = modelTransformer.transformArmorModel(humanoidModel);
				} catch (Exception e) {
					EpicFightMod.LOGGER.warn("Can't transform the model of " + ForgeRegistries.ITEMS.getKey(armorItem) + " because of :");
					e.printStackTrace();
					EXCEPTIONAL_MODELS.add(armorItem);
				}
				
				if (skinnedArmorModel != null) {
					break;
				}
			}
			
			if (skinnedArmorModel == null) {
				skinnedArmorModel = VANILLA_TRANSFORMER.transformArmorModel(humanoidModel);
			}
		}
		
		BAKED_MODELS.put(ForgeRegistries.ITEMS.getKey(armorItem), skinnedArmorModel);
		
		return skinnedArmorModel;
	}
}