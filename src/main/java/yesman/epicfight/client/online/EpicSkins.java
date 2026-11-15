package yesman.epicfight.client.online;
import yesman.epicfight.EpicFight;

import com.google.common.collect.Maps;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonReader;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.Items;
import yesman.epicfight.api.client.model.Meshes;
import yesman.epicfight.api.client.model.SoftBodyTranslatable;
import yesman.epicfight.api.client.physics.cloth.ClothColliderPresets;
import yesman.epicfight.api.client.physics.cloth.ClothSimulator;
import yesman.epicfight.client.gui.widgets.ColorDeterminator;
import yesman.epicfight.client.online.cosmetics.Cape;
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

public record EpicSkins(Supplier<ResourceLocation> capeTexture, float r, float g, float b) {
	public static void initEpicSkins(AbstractClientPlayerPatch<?> playerpatch) {
		if (EpicFightServerConnectionHelper.supported() && ClientConfig.enableCosmetics) {
			EpicFightServerConnectionHelper.getPlayerSkinInfo(EpicFightSharedConstants.webServerDomain(), playerpatch.getOriginal().getUUID().toString().replace("-", ""), (response, exception) -> {
				if (exception != null) {
					EpicFight.LOGGER.error("Failed at connecting Epic Fight web server: " + exception.getMessage());
				}
				
				if (response.statusCode() != 200) {
					EpicFight.LOGGER.error("Failed at connecting Epic Fight web server: " + response.body());
				}
				
				Map<Slot, Cape> cosmetics = Maps.newHashMap();
				
				try {
					JsonReader jsonReader = new JsonReader(new InputStreamReader(new ByteArrayInputStream(response.body().getBytes()), StandardCharsets.UTF_8));
					JsonArray cosmeticsArray = Streams.parse(jsonReader).getAsJsonArray();
					
					for (JsonElement cosmeticJson : cosmeticsArray) {
						JsonObject cosmeticObj = cosmeticJson.getAsJsonObject();
						
						try {
							Cape cosmetic = new Cape(cosmeticObj);
							cosmetics.put(cosmetic.slot(), cosmetic);
						} catch (JsonSyntaxException e) {
							e.printStackTrace();
						}
					}
				} catch (Exception e) {
				}
				
				if (cosmetics.containsKey(Slot.CAPE)) {
					Cape cosmetic = cosmetics.get(Slot.CAPE);
					Supplier<ResourceLocation> cloakTextureProvider = null;
					
					if (cosmetic.useBoolParam1() && cosmetic.boolParam1()) {
						cloakTextureProvider = () -> playerpatch.getOriginal().getSkin().capeTexture();
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
								.putAll(ClothColliderPresets.BIPED)
							, () -> {
								  return playerpatch.getOriginal().getSkin().capeTexture() != null
										  && !playerpatch.getOriginal().isInvisible()
										  && playerpatch.getOriginal().isModelPartShown(PlayerModelPart.CAPE)
										  && playerpatch.getOriginal().getItemBySlot(EquipmentSlot.CHEST).getItem() != Items.ELYTRA;
							  }
						);
						
						if (cosmetic.useIntParam1() && (!cosmetic.useBoolParam1() || !cosmetic.boolParam1())) {
							double brightness = (cosmetic.intParam1() & 255) / 255.0F;
							double saturation = ((cosmetic.intParam1() & 65280) >> 8) / 255.0F;
							double hue = ((cosmetic.intParam1() & 16711680) >> 16) / 255.0F;
							int hueColor = ColorDeterminator.positionToPackedRGBA(hue);
							int saturationApplied = ColorDeterminator.positionToPackedRGBA(saturation, new int[] { hueColor, 0xFFFFFFFF } );
							int brightnessApplied = ColorDeterminator.positionToPackedRGBA(brightness, new int[] { saturationApplied, 0xFF000000 } );
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
				.putAll(PlayerSkin.Model.WIDE.equals(playerpatch.getOriginal().getSkin().model())
							? ClothColliderPresets.BIPED : ClothColliderPresets.BIPED_SLIM)
			, () -> playerpatch.getOriginal().getSkin().capeTexture() != null
					&& !playerpatch.getOriginal().isInvisible()
					&& playerpatch.getOriginal().isModelPartShown(PlayerModelPart.CAPE)
					&& playerpatch.getOriginal().getItemBySlot(EquipmentSlot.CHEST).getItem() != Items.ELYTRA
		);
		
		playerpatch.setEpicSkinsInformation(new EpicSkins(() -> playerpatch.getOriginal().getSkin().capeTexture(), 1.0F, 1.0F, 1.0F));
	}

    public enum Slot {
		CAPE
	}
}
