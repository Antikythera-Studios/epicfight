package yesman.epicfight.api.client.animation;
import yesman.epicfight.EpicFight;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonReader;
import com.mojang.datafixers.util.Pair;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.GsonHelper;
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.LivingMotion;
import yesman.epicfight.api.animation.TransformSheet;
import yesman.epicfight.api.animation.property.AnimationProperty.StaticAnimationProperty;
import yesman.epicfight.api.animation.types.ActionAnimation;
import yesman.epicfight.api.animation.types.DirectStaticAnimation;
import yesman.epicfight.api.animation.types.DynamicAnimation;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.asset.JsonAssetLoader;
import yesman.epicfight.api.client.animation.property.*;
import yesman.epicfight.api.exception.AssetLoadingException;
import yesman.epicfight.api.utils.ParseUtil;
import yesman.epicfight.main.EpicFightMod;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class AnimationSubFileReader {
	public static final SubFileType<ClientProperty> SUBFILE_CLIENT_PROPERTY = new ClientPropertyType();
	public static final SubFileType<PovSettings> SUBFILE_POV_ANIMATION = new PovAnimationType();
	
	public static void readAndApply(StaticAnimation animation, Resource iresource, SubFileType<?> subFileType) {
		InputStream inputstream = null;
		
		try {
			inputstream = iresource.open();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		assert inputstream != null : "Input stream is null";
		
		try {
			subFileType.apply(inputstream, animation);
		} catch (JsonParseException e) {
			EpicFight.LOGGER.warn("Can't read sub file " + subFileType.directory + " for " + animation);
			e.printStackTrace();
		}
	}
	
	public static abstract class SubFileType<T> {
		private final String directory;
		private final AnimationSubFileDeserializer<T> deserializer;
		
		private SubFileType(String directory,  AnimationSubFileDeserializer<T> deserializer) {
			this.directory = directory;
			this.deserializer = deserializer;
		}
		
		// Deserialize from input stream
		public void apply(InputStream inputstream, StaticAnimation animation) {
			Reader reader = new InputStreamReader(inputstream, StandardCharsets.UTF_8);
			JsonReader jsonReader = new JsonReader(reader);
			jsonReader.setLenient(true);
			T deserialized = this.deserializer.deserialize(animation, Streams.parse(jsonReader));
			this.applySubFileInfo(deserialized, animation);
		}
		
		// Deserialize from json object
		public void apply(JsonElement jsonElement, StaticAnimation animation) {
			T deserialized = this.deserializer.deserialize(animation, jsonElement);
			this.applySubFileInfo(deserialized, animation);
		}
		
		protected abstract void applySubFileInfo(T deserialized, StaticAnimation animation);
		
		public String getDirectory() {
			return this.directory;
		}
	}
	
	private record ClientProperty(LayerInfo layerInfo, LayerInfo multilayerInfo, List<TrailInfo> trailInfo) {
	}
	
	private static class ClientPropertyType extends SubFileType<ClientProperty> {
		private ClientPropertyType() {
			super("data", new AnimationSubFileReader.ClientAnimationPropertyDeserializer());
		}
		
		@Override
		public void applySubFileInfo(ClientProperty deserialized, StaticAnimation animation) {
			if (deserialized.layerInfo() != null) {
				if (deserialized.layerInfo().jointMaskEntry.isValid()) {
					animation.addProperty(ClientAnimationProperties.JOINT_MASK, deserialized.layerInfo().jointMaskEntry);
				}
				
	        	animation.addProperty(ClientAnimationProperties.LAYER_TYPE, deserialized.layerInfo().layerType);
	        	animation.addProperty(ClientAnimationProperties.PRIORITY, deserialized.layerInfo().priority);
	        }
			
			if (deserialized.multilayerInfo() != null) {
				DirectStaticAnimation multilayerAnimation = new DirectStaticAnimation(animation.getLocation(), animation.getTransitionTime(), animation.isRepeat(), animation.getRegistryName().toString() + "_multilayer", animation.getArmature());
				
				if (deserialized.multilayerInfo().jointMaskEntry.isValid()) {
					multilayerAnimation.addProperty(ClientAnimationProperties.JOINT_MASK, deserialized.multilayerInfo().jointMaskEntry);
				}
				
				multilayerAnimation.addProperty(ClientAnimationProperties.LAYER_TYPE, deserialized.multilayerInfo().layerType);
				multilayerAnimation.addProperty(ClientAnimationProperties.PRIORITY, deserialized.multilayerInfo().priority);
				multilayerAnimation.addProperty(StaticAnimationProperty.ELAPSED_TIME_MODIFIER, (self, entitypatch, speed, prevElapsedTime, elapsedTime) -> {
					Layer baseLayer = entitypatch.getClientAnimator().baseLayer;
					
					if (baseLayer.animationPlayer.getAnimation().get().getRealAnimation().get() != animation) {
						return Pair.of(prevElapsedTime, elapsedTime);
					}
					
					if (!self.isStaticAnimation() && baseLayer.animationPlayer.getAnimation().get().isStaticAnimation()) {
						return Pair.of(prevElapsedTime + speed, elapsedTime + speed);
					}
					
					return Pair.of(baseLayer.animationPlayer.getPrevElapsedTime(), baseLayer.animationPlayer.getElapsedTime());
				});
				
				animation.addProperty(ClientAnimationProperties.MULTILAYER_ANIMATION, multilayerAnimation);
			}
			
			if (deserialized.trailInfo().size() > 0) {
				animation.addProperty(ClientAnimationProperties.TRAIL_EFFECT, deserialized.trailInfo());
			}
		}
	}
	
	private static class ClientAnimationPropertyDeserializer implements AnimationSubFileDeserializer<ClientProperty> {
		private static LayerInfo deserializeLayerInfo(JsonObject jsonObject) {
			return deserializeLayerInfo(jsonObject, null);
		}
		
		private static LayerInfo deserializeLayerInfo(JsonObject jsonObject, @Nullable Layer.LayerType defaultLayerType) {
			JointMaskEntry.Builder builder = JointMaskEntry.builder();
			Layer.Priority priority = jsonObject.has("priority") ? Layer.Priority.valueOf(GsonHelper.getAsString(jsonObject, "priority")) : null;
			Layer.LayerType layerType = jsonObject.has("layer") ? Layer.LayerType.valueOf(GsonHelper.getAsString(jsonObject, "layer")) : Layer.LayerType.BASE_LAYER;
			
			if (jsonObject.has("masks")) {
				JsonArray maskArray = jsonObject.get("masks").getAsJsonArray();
				
				if (!maskArray.isEmpty()) {
					builder.defaultMask(JointMaskReloadListener.getNoneMask());
					
					maskArray.forEach(element -> {
						JsonObject jointMaskEntry = element.getAsJsonObject();
						String livingMotionName = GsonHelper.getAsString(jointMaskEntry, "livingmotion");
						String type = GsonHelper.getAsString(jointMaskEntry, "type");
						
						if (!type.contains(":")) {
							type = (new StringBuilder(EpicFight.MODID)).append(":").append(type).toString();
						}
						
						if (livingMotionName.equals("ALL")) {
							builder.defaultMask(JointMaskReloadListener.getJointMaskEntry(type));
						} else {
							builder.mask((LivingMotion) LivingMotion.ENUM_MANAGER.getOrThrow(livingMotionName), JointMaskReloadListener.getJointMaskEntry(type));
						}
					});
				}
			}
			
			return new LayerInfo(builder.create(), priority, (defaultLayerType == null) ? layerType : defaultLayerType);
		}
		
		@Override
		public ClientProperty deserialize(StaticAnimation animation, JsonElement json) throws JsonParseException {
			JsonObject jsonObject = json.getAsJsonObject();
			LayerInfo layerInfo = null;
			LayerInfo multilayerInfo = null;
			
			if (jsonObject.has("multilayer")) {
				JsonObject multiplayerJson = jsonObject.get("multilayer").getAsJsonObject();
				layerInfo = deserializeLayerInfo(multiplayerJson.get("base").getAsJsonObject());
				multilayerInfo = deserializeLayerInfo(multiplayerJson.get("composite").getAsJsonObject(), Layer.LayerType.COMPOSITE_LAYER);
			} else {
				layerInfo = deserializeLayerInfo(jsonObject);
			}
			
			List<TrailInfo> trailInfos = Lists.newArrayList();
			
			if (jsonObject.has("trail_effects")) {
				JsonArray trailArray = jsonObject.get("trail_effects").getAsJsonArray();
				trailArray.forEach(element -> trailInfos.add(TrailInfo.deserialize(element)));
			}
			
			return new ClientProperty(layerInfo, multilayerInfo, trailInfos);
		}
	}
	
	public record PovSettings (
		@Nullable TransformSheet cameraTransform,
		Map<String, Boolean> visibilities,
		RootTransformation rootTransformation,
		@Nullable ViewLimit viewLimit,
		boolean visibilityOthers,
		boolean hasUniqueAnimation,
		boolean syncFrame
	) {
		public enum RootTransformation {
			CAMERA, WORLD
		}
		
		public record ViewLimit(float xRotMin, float xRotMax, float yRotMin, float yRotMax) {
		}
	}
	
	private static class PovAnimationType extends SubFileType<PovSettings> {
		private PovAnimationType() {
			super("pov", new AnimationSubFileReader.PovAnimationDeserializer());
		}
		
		@Override
		public void applySubFileInfo(PovSettings deserialized, StaticAnimation animation) {
			ResourceLocation povAnimationLocation = deserialized.hasUniqueAnimation() ? AnimationManager.getSubAnimationFileLocation(animation.getLocation(), SUBFILE_POV_ANIMATION) : animation.getLocation();
			DirectStaticAnimation povAnimation = new DirectStaticAnimation(povAnimationLocation, animation.getTransitionTime(), animation.isRepeat(), animation.getRegistryName().toString() + "_pov", animation.getArmature()) {
				@Override
				public float getPlaySpeed(LivingEntityPatch<?> entitypatch, DynamicAnimation pAnimation) {
					return animation.getPlaySpeed(entitypatch, pAnimation);
				}
			};
			
			animation.getProperty(StaticAnimationProperty.PLAY_SPEED_MODIFIER).ifPresent(speedModifier -> {
				povAnimation.addProperty(StaticAnimationProperty.PLAY_SPEED_MODIFIER, speedModifier);
			});
			
			if (deserialized.syncFrame()) {
				animation.getProperty(StaticAnimationProperty.ELAPSED_TIME_MODIFIER).ifPresent(elapsedTimeModifier -> {
					povAnimation.addProperty(StaticAnimationProperty.ELAPSED_TIME_MODIFIER, elapsedTimeModifier);
				});
			}
			
			animation.addProperty(ClientAnimationProperties.POV_ANIMATION, povAnimation);
			animation.addProperty(ClientAnimationProperties.POV_SETTINGS, deserialized);
		}
	}
	
	private static class PovAnimationDeserializer implements AnimationSubFileDeserializer<PovSettings> {
		@Override
		public PovSettings deserialize(StaticAnimation animation, JsonElement json) throws AssetLoadingException, JsonParseException {
			JsonObject jObject = json.getAsJsonObject();
			TransformSheet cameraTransform = null;
			PovSettings.ViewLimit viewLimit = null;
			PovSettings.RootTransformation rootTrasnformation = null;
			
			if (jObject.has("root")) {
				rootTrasnformation = PovSettings.RootTransformation.valueOf(ParseUtil.toUpperCase(GsonHelper.getAsString(jObject, "root")));
			} else {
				if (animation instanceof ActionAnimation) {
					rootTrasnformation = PovSettings.RootTransformation.WORLD;
				} else {
					rootTrasnformation = PovSettings.RootTransformation.CAMERA;
				}
			}
			
			if (jObject.has("camera")) {
				JsonObject cameraTransformJObject = jObject.getAsJsonObject("camera");
				cameraTransform = JsonAssetLoader.getTransformSheet(cameraTransformJObject, null, false, JsonAssetLoader.TransformFormat.ATTRIBUTES);
			}
			
			ImmutableMap.Builder<String, Boolean> visibilitiesBuilder = ImmutableMap.builder();
			boolean others = false;
			
			if (jObject.has("visibilities")) {
				JsonObject visibilitiesObject = jObject.getAsJsonObject("visibilities");
				visibilitiesObject.entrySet().stream().filter((e) -> !"others".equals(e.getKey())).forEach((entry) -> visibilitiesBuilder.put(entry.getKey(), entry.getValue().getAsBoolean()));
				others = visibilitiesObject.get("others").getAsBoolean();
			} else {
				visibilitiesBuilder.put("leftArm", true);
				visibilitiesBuilder.put("leftSleeve", true);
				visibilitiesBuilder.put("rightArm", true);
				visibilitiesBuilder.put("rightSleeve", true);
			}
			
			if (jObject.has("limited_view_degrees")) {
				JsonObject limitedViewDegrees = jObject.getAsJsonObject("limited_view_degrees");
				JsonArray xRot = limitedViewDegrees.get("xRot").getAsJsonArray();
				JsonArray yRot = limitedViewDegrees.get("yRot").getAsJsonArray();
				
				float xRotMin = Math.min(xRot.get(0).getAsFloat(), xRot.get(1).getAsFloat());
				float xRotMax = Math.max(xRot.get(0).getAsFloat(), xRot.get(1).getAsFloat());
				float yRotMin = Math.min(yRot.get(0).getAsFloat(), yRot.get(1).getAsFloat());
				float yRotMax = Math.max(yRot.get(0).getAsFloat(), yRot.get(1).getAsFloat());
				viewLimit = new PovSettings.ViewLimit(xRotMin, xRotMax, yRotMin, yRotMax);
			}
			
			return new PovSettings(cameraTransform, visibilitiesBuilder.build(), rootTrasnformation, viewLimit, others, jObject.has("animation"), GsonHelper.getAsBoolean(jObject, "sync_frame", false));
		}
	}
	
	public interface AnimationSubFileDeserializer<T> {
		public T deserialize(StaticAnimation animation, JsonElement json) throws JsonParseException;
	}
}
