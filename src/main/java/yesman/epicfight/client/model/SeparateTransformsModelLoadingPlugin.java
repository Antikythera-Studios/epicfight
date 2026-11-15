package yesman.epicfight.client.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelResolver;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import yesman.epicfight.EpicFight;

import java.io.InputStreamReader;
import java.util.Optional;

/// Fabric [ModelLoadingPlugin] that handles the NeoForge [neoforge:separate_transforms] model loader format.
///
/// The NeoForge format allows different models for different [ItemDisplayContext] perspectives (GUI, ground, fixed, etc.).
/// This plugin reads the raw model JSON, detects the [separate_transforms] loader, parses the [base] and [perspectives]
/// sub-models as vanilla [BlockModel]s, and wraps them in a [SeparateTransformsUnbakedModel].
///
/// For models without the [separate_transforms] loader, returns null so vanilla handles them.
public class SeparateTransformsModelLoadingPlugin implements ModelLoadingPlugin {
    public static final String SEPARATE_TRANSFORMS_LOADER = "neoforge:separate_transforms";
    private static final Gson GSON = new GsonBuilder().create();

    @Override
    public void onInitializeModelLoader(Context context) {
        context.resolveModel().register(new SeparateTransformsResolver());
    }

    private static class SeparateTransformsResolver implements ModelResolver {
        @Override
        public UnbakedModel resolveModel(Context context) {
            ResourceLocation id = context.id();

            // Only check models in the epicfight namespace to avoid unnecessary resource reads
            if (id == null || !EpicFight.MODID.equals(id.getNamespace())) {
                return null;
            }

            try {
                // The model id points to assets/<namespace>/models/<path>.json
                ResourceLocation modelResourceLocation = ResourceLocation.fromNamespaceAndPath(
                    id.getNamespace(),
                    "models/" + id.getPath() + ".json"
                );

                Optional<Resource> resourceOpt = Minecraft.getInstance().getResourceManager().getResource(modelResourceLocation);

                if (resourceOpt.isEmpty()) {
                    return null;
                }

                Resource resource = resourceOpt.get();
                try (InputStreamReader reader = new InputStreamReader(resource.open())) {
                    JsonElement jsonElement = JsonParser.parseReader(reader);

                    if (!jsonElement.isJsonObject()) {
                        return null;
                    }

                    JsonObject json = jsonElement.getAsJsonObject();

                    if (!json.has("loader") || !SEPARATE_TRANSFORMS_LOADER.equals(json.get("loader").getAsString())) {
                        return null;
                    }

                    // Parse base model
                    BlockModel baseModel = null;
                    if (json.has("base")) {
                        baseModel = parseBlockModel(json.get("base"), json);
                    }

                    // Parse perspectives
                    java.util.Map<net.minecraft.world.item.ItemDisplayContext, BlockModel> perspectives = new java.util.HashMap<>();

                    if (json.has("perspectives") && json.get("perspectives").isJsonObject()) {
                        JsonObject perspectivesJson = json.getAsJsonObject("perspectives");

                        for (String contextName : perspectivesJson.keySet()) {
                            try {
                                net.minecraft.world.item.ItemDisplayContext displayContext = net.minecraft.world.item.ItemDisplayContext.valueOf(contextName.toUpperCase());
                                BlockModel perspectiveModel = parseBlockModel(perspectivesJson.get(contextName), json);

                                if (perspectiveModel != null) {
                                    perspectives.put(displayContext, perspectiveModel);
                                }
                            } catch (IllegalArgumentException e) {
                                EpicFight.LOGGER.warn("Unknown ItemDisplayContext '{}' in separate_transforms model {}", contextName, id);
                            }
                        }
                    }

                    if (baseModel == null && perspectives.isEmpty()) {
                        return null;
                    }

                    return new SeparateTransformsUnbakedModel(baseModel, perspectives);
                }
            } catch (Exception e) {
                EpicFight.LOGGER.warn("Failed to load separate_transforms model {}: {}", id, e.getMessage());
                return null;
            }
        }

        private BlockModel parseBlockModel(JsonElement jsonElement, JsonObject topLevel) {
            if (jsonElement == null || !jsonElement.isJsonObject()) {
                return null;
            }

            JsonObject modelJson = jsonElement.getAsJsonObject();

            if (!modelJson.has("gui_light") && topLevel.has("gui_light")) {
                modelJson.addProperty("gui_light", topLevel.get("gui_light").getAsString());
            }

            String json = GSON.toJson(modelJson);
            return BlockModel.fromString(json);
        }
    }
}
