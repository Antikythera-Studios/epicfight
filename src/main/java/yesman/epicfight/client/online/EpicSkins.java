package yesman.epicfight.client.online;

import com.google.common.collect.Maps;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonReader;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.Items;
import yesman.epicfight.api.client.model.Meshes;
import yesman.epicfight.api.client.model.SoftBodyTranslatable;
import yesman.epicfight.api.client.physics.cloth.ClothColliderPresets;
import yesman.epicfight.api.client.physics.cloth.ClothSimulator;
import yesman.epicfight.api.utils.ParseUtil;
import yesman.epicfight.client.gui.widgets.ColorSlider;
import yesman.epicfight.client.world.capabilites.entitypatch.player.AbstractClientPlayerPatch;
import yesman.epicfight.config.ClientConfig;
import yesman.epicfight.gameasset.Armatures;
import yesman.epicfight.main.EpicFightMod;
import yesman.epicfight.main.EpicFightSharedConstants;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.function.Supplier;

public record EpicSkins(Supplier<ResourceLocation> cloakTexture, float r, float g, float b) {
	public static void initEpicSkins(AbstractClientPlayerPatch<?> playerpatch) {
		if (EpicFightServerConnectionHelper.supported() && ClientConfig.enableCosmetics) {
			EpicFightServerConnectionHelper.getPlayerSkinInfo(EpicFightSharedConstants.webServerDomain(), playerpatch.getOriginal().getUUID().toString().replace("-", ""), (response, exception) -> {
				if (exception != null) {
					EpicFightMod.LOGGER.error("Failed at connecting Epic Fight web server: " + exception.getMessage());
				}
				
				if (response.statusCode() != 200) {
					EpicFightMod.LOGGER.error("Error code from Epic Fight web server: " + response.body());
				}
				
				Map<Slot, Cosmetic> cosmetics = Maps.newHashMap();
				
				try {
					JsonReader jsonReader = new JsonReader(new InputStreamReader(new ByteArrayInputStream(response.body().getBytes()), StandardCharsets.UTF_8));
					JsonArray cosmeticsArray = Streams.parse(jsonReader).getAsJsonArray();
					
					for (JsonElement cosmeticJson : cosmeticsArray) {
						JsonObject cosmeticObj = cosmeticJson.getAsJsonObject();
						
						try {
							Cosmetic cosmetic = new Cosmetic(cosmeticObj);
							cosmetics.put(cosmetic.slot(), cosmetic);
						} catch (JsonSyntaxException e) {
							e.printStackTrace();
						}
					}
				} catch (Exception e) {
				}
				
				if (cosmetics.containsKey(Slot.CAPE)) {
					Cosmetic cosmetic = cosmetics.get(Slot.CAPE);
					Supplier<ResourceLocation> cloakTextureProvider = null;
					
					if (cosmetic.useBoolParam1() && cosmetic.boolParam1()) {
						cloakTextureProvider = () -> playerpatch.getOriginal().getCloakTextureLocation();
					} else {
						cloakTextureProvider = () -> cosmetic.textureLocation();
					}
					
					final Supplier<ResourceLocation> fCloakTextureProvider = cloakTextureProvider;
					
					RemoteAssets.getInstance().getRemoteMesh(cosmetic.seq(), cosmetic.fileLocation(), (mesh) -> {
						SoftBodyTranslatable.TRACKING_SIMULATION_SUBJECTS.add(playerpatch);
							
						playerpatch.getClothSimulator().runWhen(
							  ClothSimulator.PLAYER_CLOAK
							, (SoftBodyTranslatable)mesh
							, ClothSimulator.ClothObjectBuilder.create()
								.parentJoint(Armatures.BIPED.get().torso)
								.putAll("default".equals(playerpatch.getOriginal().getModelName())
											? ClothColliderPresets.BIPED : ClothColliderPresets.BIPED)
							, () -> {
								  return playerpatch.getOriginal().isCapeLoaded() && !playerpatch.getOriginal().isInvisible() && playerpatch.getOriginal().isModelPartShown(PlayerModelPart.CAPE)
										 && playerpatch.getOriginal().getItemBySlot(EquipmentSlot.CHEST).getItem() != Items.ELYTRA;
							  }
						);
						
						if (cosmetic.useIntParam1() && (!cosmetic.useBoolParam1() || !cosmetic.boolParam1())) {
							double brightness = (cosmetic.intParam1() & 255) / 255.0F;
							double saturation = ((cosmetic.intParam1() & 65280) >> 8) / 255.0F;
							double hue = ((cosmetic.intParam1() & 16711680) >> 16) / 255.0F;
							int hueColor = ColorSlider.rgbColor(hue);
							int saturationApplied = ColorSlider.sliderPositionToColor(saturation, new int[] { hueColor, 0xFFFFFFFF } );
							int brightnessApplied = ColorSlider.sliderPositionToColor(brightness, new int[] { saturationApplied, 0xFF000000 } );
							float r = ((brightnessApplied & 16711680) >> 16) / 255.0F;
							float g = ((brightnessApplied & 65280) >> 8) / 255.0F;
							float b = (brightnessApplied & 255) / 255.0F;
							
							playerpatch.setEpicSkinsInformation(new EpicSkins(fCloakTextureProvider, r, g, b));
						} else {
							playerpatch.setEpicSkinsInformation(new EpicSkins(fCloakTextureProvider, 1.0F, 1.0F, 1.0F));
						}
					});
				} else {
					initDefaultCape(playerpatch);
				}
			});
		} else {
			initDefaultCape(playerpatch);
		}
	}
	
	public static void initDefaultCape(AbstractClientPlayerPatch<?> playerpatch) {
		SoftBodyTranslatable.TRACKING_SIMULATION_SUBJECTS.add(playerpatch);
		
		playerpatch.getClothSimulator().runWhen(
			  ClothSimulator.PLAYER_CLOAK
			, Meshes.CAPE_DEFAULT
			, ClothSimulator.ClothObjectBuilder.create()
				.parentJoint(Armatures.BIPED.get().torso)
				.putAll("default".equals(playerpatch.getOriginal().getModelName()) ? ClothColliderPresets.BIPED : ClothColliderPresets.BIPED_SLIM)
			, () -> playerpatch.getOriginal().getCloakTextureLocation() != null && !playerpatch.getOriginal().isInvisible() && playerpatch.getOriginal().isModelPartShown(PlayerModelPart.CAPE) && playerpatch.getOriginal().getItemBySlot(EquipmentSlot.CHEST).getItem() != Items.ELYTRA
		);
		
		playerpatch.setEpicSkinsInformation(new EpicSkins(() -> playerpatch.getOriginal().getCloakTextureLocation(), 1.0F, 1.0F, 1.0F));
	}
	
	public record Cosmetic(int seq, Slot slot, int intParam1, boolean boolParam1, boolean useIntParam1, boolean useBoolParam1, String fileLocation, ResourceLocation textureLocation) {
		/**
		 * intParam1 is usually used to color
		 * boolParam1 is usually used to decide cape's vanilla texture
		 */
		public Cosmetic(JsonObject json) throws JsonSyntaxException {
			this(
				  GsonHelper.getAsInt(json, "cosmeticSeq")
				, Slot.valueOf(ParseUtil.toUpperCase(GsonHelper.getAsString(json, "slot")))
				, GsonHelper.getAsInt(json, "intParam1")
				, GsonHelper.getAsBoolean(json, "boolParam1")
				, GsonHelper.getAsBoolean(json, "useIntParam1")
				, GsonHelper.getAsBoolean(json, "useBoolParam1")
				, GsonHelper.getAsString(json, "fileLocation")
				, RemoteAssets.getInstance().getRemoteTexture(GsonHelper.getAsString(json, "textureLocation"))
			);
		}
	}
	
	public enum Slot {
		CAPE
	}
}
