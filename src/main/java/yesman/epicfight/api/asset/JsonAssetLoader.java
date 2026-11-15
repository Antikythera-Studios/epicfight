package yesman.epicfight.api.asset;
import yesman.epicfight.EpicFight;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonReader;
import io.netty.util.internal.StringUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;

import yesman.epicfight.api.animation.*;
import yesman.epicfight.api.animation.property.AnimationProperty.ActionAnimationProperty;
import yesman.epicfight.api.animation.types.ActionAnimation;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.api.animation.types.AttackAnimation.Phase;
import yesman.epicfight.api.animation.types.MainFrameAnimation;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.client.model.*;
import yesman.epicfight.api.client.model.Meshes.MeshContructor;
import yesman.epicfight.api.client.model.transformer.VanillaModelTransformer.VanillaMeshPartDefinition;
import yesman.epicfight.api.client.physics.cloth.ClothSimulator.ClothObject.ClothPart.ConstraintType;
import yesman.epicfight.api.exception.AssetLoadingException;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.api.utils.ParseUtil;
import yesman.epicfight.api.utils.math.MathUtils;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.api.utils.math.Vec3f;
import yesman.epicfight.api.utils.math.Vec4f;
import yesman.epicfight.gameasset.Armatures.ArmatureContructor;
import yesman.epicfight.main.EpicFightMod;
import yesman.epicfight.main.EpicFightSharedConstants;

import javax.annotation.Nullable;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class JsonAssetLoader {
    public static final OpenMatrix4f BLENDER_TO_MINECRAFT_COORD = OpenMatrix4f.createRotatorDeg(-90.0F, Vec3f.X_AXIS);
    public static final OpenMatrix4f MINECRAFT_TO_BLENDER_COORD = OpenMatrix4f.invert(BLENDER_TO_MINECRAFT_COORD, null);
    public static final String UNGROUPED_NAME = "noGroups";
    public static final String COORD_BONE = "Coord";
    public static final String ROOT_BONE = "Root";

    private JsonObject rootJson;

    // Used for deciding armature name, other resources are nullable
    @Nullable
    private ResourceLocation resourceLocation;
    private String filehash;

    public JsonAssetLoader(ResourceManager resourceManager, ResourceLocation resourceLocation) throws AssetLoadingException {
        JsonReader jsonReader = null;
        this.resourceLocation = resourceLocation;

        try {
            try {
                if (resourceManager == null) {
                    throw new NoSuchElementException();
                }

                Resource resource = resourceManager.getResource(resourceLocation).orElseThrow();
                InputStream inputStream = resource.open();
                InputStreamReader isr = new InputStreamReader(inputStream, StandardCharsets.UTF_8);

                jsonReader = new JsonReader(isr);
                jsonReader.setLenient(true);
                this.rootJson = Streams.parse(jsonReader).getAsJsonObject();
            } catch (NoSuchElementException e) {
                // In this case, reads the animation data from mod.jar (Especially in a server)
                // On Fabric, servers don't load assets/ via ResourceManager, so we read directly from the mod JAR
                // via the mod's own classloader. Each mod namespace maps to a mod container whose classes share
                // the same classpath as that mod's assets/ directory.
                InputStream inputstream = null;

                // Try the mod's classloader first. We pick a class from the namespace's mod by checking
                // the FabricLoader mod container, then use that container's classloader to find the asset.
                // As a robust fallback, also try the EpicFight mod class itself (covers the common case where
                // the asset is in the epicfight namespace).
                String assetPath = "/assets/" + resourceLocation.getNamespace() + "/" + resourceLocation.getPath();

                // Try via FabricLoader: find a class owned by the namespace's mod and use its classloader.
                try {
                    net.fabricmc.loader.api.ModContainer modContainer =
                        FabricLoader.getInstance().getModContainer(resourceLocation.getNamespace()).orElse(null);

                    if (modContainer != null) {
                        // Use the classloader that loaded JsonAssetLoader (same mod) — works for epicfight namespace.
                        // For other namespaces, the mod's own classes would be needed, but in practice all
                        // Epic Fight asset namespaces are served by the epicfight mod's classpath.
                        ClassLoader cl = JsonAssetLoader.class.getClassLoader();
                        inputstream = cl.getResourceAsStream(assetPath);
                    }
                } catch (Throwable ignored) {}

                // Final fallback: try EpicFight's own classloader.
                if (inputstream == null) {
                    inputstream = yesman.epicfight.EpicFight.class.getResourceAsStream(assetPath);
                }

                if (inputstream == null) {
                    throw new NoSuchElementException("No file named " + resourceLocation.toString());
                }

                BufferedInputStream bufferedInputStream = new BufferedInputStream(inputstream);
                InputStreamReader reader = new InputStreamReader(bufferedInputStream, StandardCharsets.UTF_8);

                jsonReader = new JsonReader(reader);
                jsonReader.setLenient(true);
                this.rootJson = Streams.parse(jsonReader).getAsJsonObject();
            }
        } catch (Exception e) {
            throw new AssetLoadingException("Can't read " + resourceLocation.toString() + " because of " + e);
        } finally {
            if (jsonReader != null) {
                try {
                    jsonReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        this.filehash = ParseUtil.getBytesSHA256Hash(this.rootJson.toString().getBytes());
    }

    public JsonAssetLoader(InputStream inputstream, ResourceLocation resourceLocation) throws AssetLoadingException {
        JsonReader jsonReader = null;
        this.resourceLocation = resourceLocation;

        jsonReader = new JsonReader(new InputStreamReader(inputstream, StandardCharsets.UTF_8));
        jsonReader.setLenient(true);
        this.rootJson = Streams.parse(jsonReader).getAsJsonObject();

        try {
            jsonReader.close();
        } catch (IOException e) {
            throw new AssetLoadingException("Can't read " + resourceLocation.toString() + ": " + e);
        }

        this.filehash = StringUtil.EMPTY_STRING;
    }

    public JsonAssetLoader(JsonObject rootJson, ResourceLocation rl) {
        this.rootJson = rootJson;
        this.resourceLocation = rl;
        this.filehash = StringUtil.EMPTY_STRING;
    }

    public static Mesh.RenderProperties getRenderProperties(JsonObject json) {
        if (!json.has("render_properties")) {
            return null;
        }

        JsonObject properties = json.getAsJsonObject("render_properties");
        Mesh.RenderProperties.Builder renderProperties = Mesh.RenderProperties.Builder.create();

        if (properties.has("transparent")) {
            renderProperties.transparency(properties.get("transparent").getAsBoolean());
        }

        if (properties.has("texture_path")) {
            renderProperties.customTexturePath(properties.get("texture_path").getAsString());
        }

        if (properties.has("color")) {
            JsonArray jsonarray = properties.getAsJsonArray("color");
            renderProperties.customColor(jsonarray.get(0).getAsFloat(), jsonarray.get(1).getAsFloat(), jsonarray.get(2).getAsFloat());
        }

        return renderProperties.build();
    }

    public ResourceLocation getParent() {
        return this.rootJson.has("parent") ? ResourceLocation.parse(this.rootJson.get("parent").getAsString()) : null;
    }

    private static final float DEFAULT_PARTICLE_MASS = 0.16F;
    private static final float DEFAULT_SELF_COLLISON = 0.05F;

    @Nullable
    public Map<String, SoftBodyTranslatable.ClothSimulationInfo> loadClothInformation(Float[] positionArray) {
        JsonObject obj = this.rootJson.getAsJsonObject("vertices");
        JsonObject clothInfoObj = obj.getAsJsonObject("cloth_info");

        if (clothInfoObj == null) {
            return null;
        }

        Map<String, SoftBodyTranslatable.ClothSimulationInfo> clothInfo = Maps.newHashMap();

        for (Map.Entry<String, JsonElement> e : clothInfoObj.entrySet()) {
            JsonObject clothObject = e.getValue().getAsJsonObject();
            int[] particlesArray = ParseUtil.toIntArrayPrimitive(clothObject.get("particles").getAsJsonObject().get("array").getAsJsonArray());
            float[] weightsArray = ParseUtil.toFloatArrayPrimitive(clothObject.get("weights").getAsJsonObject().get("array").getAsJsonArray());
            float particleMass = clothObject.has("particle_mass") ? clothObject.get("particle_mass").getAsFloat() : DEFAULT_PARTICLE_MASS;
            float selfCollision = clothObject.has("self_collision") ? clothObject.get("self_collision").getAsFloat() : DEFAULT_SELF_COLLISON;

            JsonArray constraintsArray = clothObject.get("constraints").getAsJsonArray();
            List<int[]> constraintsList = new ArrayList<> (constraintsArray.size());
            float[] compliances = new float[constraintsArray.size()];
            ConstraintType[] constraintType = new ConstraintType[constraintsArray.size()];
            float[] rootDistances = new float[particlesArray.length / 2];

            int i = 0;

            for (JsonElement element : constraintsArray) {
                JsonObject asJsonObject = element.getAsJsonObject();

                if (asJsonObject.has("unused") && GsonHelper.getAsBoolean(asJsonObject, "unused")) {
                    continue;
                }

                constraintType[i] = ConstraintType.valueOf(GsonHelper.getAsString(asJsonObject, "type").toUpperCase(Locale.ROOT));
                compliances[i] = GsonHelper.getAsFloat(asJsonObject, "compliance");
                constraintsList.add(ParseUtil.toIntArrayPrimitive(asJsonObject.get("array").getAsJsonArray()));
                element.getAsJsonObject().get("compliance");
                i++;
            }

            List<Vec3> rootParticles = Lists.newArrayList();

            for (int j = 0; j < particlesArray.length / 2; j++) {
                int weightIndex = particlesArray[j * 2 + 1];
                float weight = weightsArray[weightIndex];

                if (weight == 0.0F) {
                    int posId = particlesArray[j * 2];
                    rootParticles.add(new Vec3(positionArray[posId * 3 + 0], positionArray[posId * 3 + 1], positionArray[posId * 3 + 2]));
                }
            }

            for (int j = 0; j < particlesArray.length / 2; j++) {
                int posId = particlesArray[j * 2];
                Vec3 position = new Vec3(positionArray[posId * 3 + 0], positionArray[posId * 3 + 1], positionArray[posId * 3 + 2]);
                Vec3 nearest = MathUtils.getNearestVector(position, rootParticles);
                rootDistances[j] = (float)position.distanceTo(nearest);
            }

            int[] normalOffsetMappingArray = null;

            if (clothObject.has("normal_offsets")) {
                normalOffsetMappingArray = ParseUtil.toIntArrayPrimitive(clothObject.get("normal_offsets").getAsJsonObject().get("array").getAsJsonArray());
            }

            SoftBodyTranslatable.ClothSimulationInfo clothSimulInfo = new SoftBodyTranslatable.ClothSimulationInfo(particleMass, selfCollision, constraintsList, constraintType, compliances, particlesArray, weightsArray, rootDistances, normalOffsetMappingArray);
            clothInfo.put(e.getKey(), clothSimulInfo);
        }

        return clothInfo;
    }

    public <T extends ClassicMesh> T loadClassicMesh(MeshContructor<ClassicMesh.ClassicMeshPart, VertexBuilder, T> constructor) {
        ResourceLocation parent = this.getParent();

        if (parent != null) {
            T mesh = Meshes.getOrCreate(parent, (jsonLoader) -> jsonLoader.loadClassicMesh(constructor)).get();
            return constructor.invoke(null, null, mesh, getRenderProperties(this.rootJson));
        } else {
            JsonObject obj = this.rootJson.getAsJsonObject("vertices");
            JsonObject positions = obj.getAsJsonObject("positions");
            JsonObject normals = obj.getAsJsonObject("normals");
            JsonObject uvs = obj.getAsJsonObject("uvs");
            JsonObject parts = obj.getAsJsonObject("parts");
            JsonObject indices = obj.getAsJsonObject("indices");
            Float[] positionArray = ParseUtil.toFloatArray(positions.get("array").getAsJsonArray());

            for (int i = 0; i < positionArray.length / 3; i++) {
                int k = i * 3;
                Vec4f posVector = new Vec4f(positionArray[k], positionArray[k+1], positionArray[k+2], 1.0F);
                OpenMatrix4f.transform(BLENDER_TO_MINECRAFT_COORD, posVector, posVector);
                positionArray[k] = posVector.x;
                positionArray[k+1] = posVector.y;
                positionArray[k+2] = posVector.z;
            }

            Float[] normalArray = ParseUtil.toFloatArray(normals.get("array").getAsJsonArray());

            for (int i = 0; i < normalArray.length / 3; i++) {
                int k = i * 3;
                Vec4f normVector = new Vec4f(normalArray[k], normalArray[k+1], normalArray[k+2], 1.0F);
                OpenMatrix4f.transform(BLENDER_TO_MINECRAFT_COORD, normVector, normVector);
                normalArray[k] = normVector.x;
                normalArray[k+1] = normVector.y;
                normalArray[k+2] = normVector.z;
            }

            Float[] uvArray = ParseUtil.toFloatArray(uvs.get("array").getAsJsonArray());

            Map<String, Number[]> arrayMap = Maps.newHashMap();
            Map<MeshPartDefinition, List<VertexBuilder>> meshMap = Maps.newHashMap();

            arrayMap.put("positions", positionArray);
            arrayMap.put("normals", normalArray);
            arrayMap.put("uvs", uvArray);

            if (parts != null) {
                for (Map.Entry<String, JsonElement> e : parts.entrySet()) {
                    meshMap.put(VanillaMeshPartDefinition.of(e.getKey(), getRenderProperties(e.getValue().getAsJsonObject())), VertexBuilder.create(ParseUtil.toIntArrayPrimitive(e.getValue().getAsJsonObject().get("array").getAsJsonArray())));
                }
            }

            if (indices != null) {
                meshMap.put(VanillaMeshPartDefinition.of(UNGROUPED_NAME), VertexBuilder.create(ParseUtil.toIntArrayPrimitive(indices.get("array").getAsJsonArray())));
            }

            T mesh = constructor.invoke(arrayMap, meshMap, null, getRenderProperties(this.rootJson));
            mesh.putSoftBodySimulationInfo(this.loadClothInformation(positionArray));

            return mesh;
        }
    }

    public <T extends SkinnedMesh> T loadSkinnedMesh(MeshContructor<SkinnedMesh.SkinnedMeshPart, VertexBuilder, T> constructor) {
        ResourceLocation parent = this.getParent();

        if (parent != null) {
            T mesh = Meshes.getOrCreate(parent, (jsonLoader) -> jsonLoader.loadSkinnedMesh(constructor)).get();
            return constructor.invoke(null, null, mesh, getRenderProperties(this.rootJson));
        } else {
            JsonObject obj = this.rootJson.getAsJsonObject("vertices");
            JsonObject positions = obj.getAsJsonObject("positions");
            JsonObject normals = obj.getAsJsonObject("normals");
            JsonObject uvs = obj.getAsJsonObject("uvs");
            JsonObject vdincies = obj.getAsJsonObject("vindices");
            JsonObject weights = obj.getAsJsonObject("weights");
            JsonObject vcounts = obj.getAsJsonObject("vcounts");
            JsonObject parts = obj.getAsJsonObject("parts");
            JsonObject indices = obj.getAsJsonObject("indices");

            Float[] positionArray = ParseUtil.toFloatArray(positions.get("array").getAsJsonArray());

            for (int i = 0; i < positionArray.length / 3; i++) {
                int k = i * 3;
                Vec4f posVector = new Vec4f(positionArray[k], positionArray[k+1], positionArray[k+2], 1.0F);
                OpenMatrix4f.transform(BLENDER_TO_MINECRAFT_COORD, posVector, posVector);
                positionArray[k] = posVector.x;
                positionArray[k+1] = posVector.y;
                positionArray[k+2] = posVector.z;
            }

            Float[] normalArray = ParseUtil.toFloatArray(normals.get("array").getAsJsonArray());

            for (int i = 0; i < normalArray.length / 3; i++) {
                int k = i * 3;
                Vec4f normVector = new Vec4f(normalArray[k], normalArray[k+1], normalArray[k+2], 1.0F);
                OpenMatrix4f.transform(BLENDER_TO_MINECRAFT_COORD, normVector, normVector);
                normalArray[k] = normVector.x;
                normalArray[k+1] = normVector.y;
                normalArray[k+2] = normVector.z;
            }

            Float[] uvArray = ParseUtil.toFloatArray(uvs.get("array").getAsJsonArray());
            Float[] weightArray = ParseUtil.toFloatArray(weights.get("array").getAsJsonArray());
            Integer[] affectingJointCounts = ParseUtil.toIntArray(vcounts.get("array").getAsJsonArray());
            Integer[] affectingJointIndices = ParseUtil.toIntArray(vdincies.get("array").getAsJsonArray());

            Map<String, Number[]> arrayMap = Maps.newHashMap();
            Map<MeshPartDefinition, List<VertexBuilder>> meshMap = Maps.newHashMap();
            arrayMap.put("positions", positionArray);
            arrayMap.put("normals", normalArray);
            arrayMap.put("uvs", uvArray);
            arrayMap.put("weights", weightArray);
            arrayMap.put("vcounts", affectingJointCounts);
            arrayMap.put("vindices", affectingJointIndices);

            if (parts != null) {
                for (Map.Entry<String, JsonElement> e : parts.entrySet()) {
                    meshMap.put(VanillaMeshPartDefinition.of(e.getKey(), getRenderProperties(e.getValue().getAsJsonObject())), VertexBuilder.create(ParseUtil.toIntArrayPrimitive(e.getValue().getAsJsonObject().get("array").getAsJsonArray())));
                }
            }

            if (indices != null) {
                meshMap.put(VanillaMeshPartDefinition.of(UNGROUPED_NAME), VertexBuilder.create(ParseUtil.toIntArrayPrimitive(indices.get("array").getAsJsonArray())));
            }

            T mesh = constructor.invoke(arrayMap, meshMap, null, getRenderProperties(this.rootJson));
            mesh.putSoftBodySimulationInfo(this.loadClothInformation(positionArray));

            return mesh;
        }
    }

    public CompositeMesh loadCompositeMesh() throws AssetLoadingException {
        if (!this.rootJson.has("meshes")) {
            throw new AssetLoadingException("Composite mesh loading exception: lower meshes undefined");
        }

        JsonAssetLoader clothLoader = new JsonAssetLoader(this.rootJson.get("meshes").getAsJsonObject().get("cloth").getAsJsonObject(), null);
        JsonAssetLoader staticLoader = new JsonAssetLoader(this.rootJson.get("meshes").getAsJsonObject().get("static").getAsJsonObject(), null);
        SoftBodyTranslatable softBodyMesh = (SoftBodyTranslatable)clothLoader.loadMesh(false);
        StaticMesh<?> staticMesh = (StaticMesh<?>)staticLoader.loadMesh(false);

        if (!softBodyMesh.canStartSoftBodySimulation()) {
            throw new AssetLoadingException("Composite mesh loading exception: soft mesh doesn't have cloth info");
        }

        return new CompositeMesh(staticMesh, softBodyMesh);
    }

    public Mesh loadMesh() throws AssetLoadingException {
        return this.loadMesh(true);
    }

    private Mesh loadMesh(boolean allowCompositeMesh) throws AssetLoadingException {
        if (!this.rootJson.has("mesh_loader")) {
            throw new AssetLoadingException("Mesh loading exception: No mesh loader provided!");
        }

        String loader = this.rootJson.get("mesh_loader").getAsString();

        switch (loader) {
        case "classic_mesh" -> {
            return this.loadClassicMesh(ClassicMesh::new);
        }
        case "skinned_mesh" -> {
            return this.loadSkinnedMesh(SkinnedMesh::new);
        }
        case "composite_mesh" -> {
            if (!allowCompositeMesh) {
                throw new AssetLoadingException("Can't have a composite mesh inside another composite mesh");
            }

            return this.loadCompositeMesh();
        }
        default -> {
            throw new AssetLoadingException("Mesh loading exception: Unsupported mesh loader: " + loader);
        }
        }
    }

    public <T extends Armature> T loadArmature(ArmatureContructor<T> constructor) {
        if (this.resourceLocation == null) {
            throw new AssetLoadingException("Can't load armature: Resource location is null.");
        }

        JsonObject obj = this.rootJson.getAsJsonObject("armature");
        TransformFormat transformFormat = getAsTransformFormatOrDefault(obj, "armature_format");
        JsonObject hierarchy = obj.get("hierarchy").getAsJsonArray().get(0).getAsJsonObject();
        JsonArray nameAsVertexGroups = obj.getAsJsonArray("joints");
        Map<String, Integer> jointIds = Maps.newHashMap();

        int id = 0;

        for (int i = 0; i < nameAsVertexGroups.size(); i++) {
            String name = nameAsVertexGroups.get(i).getAsString();

            if (name.equals(COORD_BONE)) {
                continue;
            }

            jointIds.put(name, id);
            id++;
        }

        Map<String, Joint> jointMap = Maps.newHashMap();
        Joint joint = getJoint(hierarchy, jointIds, jointMap, transformFormat, true);
        joint.initOriginTransform(new OpenMatrix4f());

        String armatureName = this.resourceLocation.toString().replaceAll("(animmodels/|\\.json)", "");

        return constructor.invoke(armatureName, jointMap.size(), joint, jointMap);
    }

    private static Joint getJoint(JsonObject object, Map<String, Integer> jointIdMap, Map<String, Joint> jointMap, TransformFormat transformFormat, boolean root) {
        String name = object.get("name").getAsString();

        if (!jointIdMap.containsKey(name)) {
            throw new AssetLoadingException("Can't load joint: joint name " + name + " doesn't exist in armature hierarchy.");
        }

        // Skip Coord bone
        if (name.equals(COORD_BONE)) {
            JsonArray coordChildren = object.get("children").getAsJsonArray();

            if (coordChildren.isEmpty()) {
                throw new AssetLoadingException("No children for Coord bone");
            } else if (coordChildren.size() > 1) {
                throw new AssetLoadingException("Coord bone can't have multiple children");
            } else {
                return getJoint(coordChildren.get(0).getAsJsonObject(), jointIdMap, jointMap, transformFormat, false);
            }
        }

        JsonElement transform = GsonHelper.getNonNull(object, "transform");

        // WORKAROUND: The case when transform format is wrongly specified!
        if (transformFormat == TransformFormat.ATTRIBUTES && transform.isJsonArray()) {
            transformFormat = TransformFormat.MATRIX;
        } else if (transformFormat == TransformFormat.MATRIX && transform.isJsonObject()) {
            transformFormat = TransformFormat.ATTRIBUTES;
        }

        OpenMatrix4f localMatrix = null;

        switch (transformFormat) {
            case MATRIX -> {
                float[] matrixElements = ParseUtil.toFloatArrayPrimitive(GsonHelper.convertToJsonArray(transform, "transform"));
                localMatrix = OpenMatrix4f.load(null, matrixElements);
                localMatrix.transpose();

                if (root) {
                    localMatrix.mulFront(BLENDER_TO_MINECRAFT_COORD);
                }
            }
            case ATTRIBUTES -> {
                JsonObject transformObject = transform.getAsJsonObject();
                JsonArray locArray = transformObject.get("loc").getAsJsonArray();
                JsonArray rotArray = transformObject.get("rot").getAsJsonArray();
                JsonArray scaArray = transformObject.get("sca").getAsJsonArray();
                JointTransform jointTransform
                    = JointTransform.fromPrimitives(
                    locArray.get(0).getAsFloat()
                    , locArray.get(1).getAsFloat()
                    , locArray.get(2).getAsFloat()
                    , -rotArray.get(1).getAsFloat()
                    , -rotArray.get(2).getAsFloat()
                    , -rotArray.get(3).getAsFloat()
                    , rotArray.get(0).getAsFloat()
                    , scaArray.get(0).getAsFloat()
                    , scaArray.get(1).getAsFloat()
                    , scaArray.get(2).getAsFloat()
                );

                localMatrix = jointTransform.toMatrix();

                if (root) {
                    localMatrix.mulFront(BLENDER_TO_MINECRAFT_COORD);
                }
            }
        }

        Joint joint = new Joint(name, jointIdMap.get(name), localMatrix);
        jointMap.put(name, joint);

        if (object.has("children")) {
            for (JsonElement children : object.get("children").getAsJsonArray()) {
                joint.addSubJoints(getJoint(children.getAsJsonObject(), jointIdMap, jointMap, transformFormat, false));
            }
        }

        return joint;
    }

    public AnimationClip loadClipForAnimation(StaticAnimation animation) {
        if (this.rootJson == null) {
            throw new AssetLoadingException("Can't find animation in path: " + animation);
        }

        if (animation.getArmature() == null) {
            EpicFight.LOGGER.error("Animation " + animation + " doesn't have an armature.");
        }

        TransformFormat format = getAsTransformFormatOrDefault(this.rootJson, "format");
        JsonArray array = this.rootJson.get("animation").getAsJsonArray();
        boolean action = animation instanceof MainFrameAnimation;
        boolean attack = animation instanceof AttackAnimation;
        boolean dedicatedServer = FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER;
        boolean noTransformData = !action && !attack && dedicatedServer;
        boolean root = true;
        Armature armature = animation.getArmature().get();
        Set<String> allowedJoints = Sets.newLinkedHashSet();

        if (attack) {
            for (Phase phase : ((AttackAnimation)animation).phases) {
                for (AttackAnimation.JointColliderPair colliderInfo : phase.getColliders()) {
                    armature.gatherAllJointsInPathToTerminal(colliderInfo.getFirst().getName(), allowedJoints);
                }
            }
        } else if (action) {
            allowedJoints.add(ROOT_BONE);
        }

        AnimationClip clip = new AnimationClip();

        for (JsonElement element : array) {
            JsonObject jObject = element.getAsJsonObject();
            String name = jObject.get("name").getAsString();

            if (attack && dedicatedServer && !allowedJoints.contains(name)) {
                if (name.equals(COORD_BONE)) {
                    root = false;
                }

                continue;
            }

            Joint joint = armature.searchJointByName(name);

            if (joint == null) {
                if (name.equals(COORD_BONE)) {
                    TransformSheet sheet = getTransformSheet(jObject, new OpenMatrix4f(), true, format);

                    if (action) {
                        ((ActionAnimation)animation).addProperty(ActionAnimationProperty.COORD, sheet);
                    }

                    root = false;
                    continue;
                } else {
                    EpicFight.LOGGER.debug("[EpicFightMod] No joint named " + name + " in " + animation);
                    continue;
                }
            }

            TransformSheet sheet = getTransformSheet(jObject, OpenMatrix4f.invert(joint.getLocalTransform(), null), root, format);

            if (!noTransformData) {
                clip.addJointTransform(name, sheet);
            }

            float maxFrameTime = sheet.maxFrameTime();

            if (clip.getClipTime() < maxFrameTime) {
                clip.setClipTime(maxFrameTime);
            }

            root = false;
        }

        return clip;
    }

    public AnimationClip loadAllJointsClipForAnimation(StaticAnimation animation) {
        TransformFormat format = getAsTransformFormatOrDefault(this.rootJson, "format");
        JsonArray array = this.rootJson.get("animation").getAsJsonArray();
        boolean root = true;

        if (animation.getArmature() == null) {
            EpicFight.LOGGER.error("Animation " + animation + " doesn't have an armature.");
        }

        Armature armature = animation.getArmature().get();
        AnimationClip clip = new AnimationClip();

        for (JsonElement element : array) {
            JsonObject jObject = element.getAsJsonObject();
            String name = jObject.get("name").getAsString();
            Joint joint = armature.searchJointByName(name);

            if (joint == null) {
                if (EpicFightSharedConstants.IS_DEV_ENV) {
                    EpicFight.LOGGER.debug(animation.getRegistryName() + ": No joint named " + name + " in armature");
                }

                continue;
            }

            TransformSheet sheet = getTransformSheet(jObject, OpenMatrix4f.invert(joint.getLocalTransform(), null), root, format);
            clip.addJointTransform(name, sheet);
            float maxFrameTime = sheet.maxFrameTime();

            if (clip.getClipTime() < maxFrameTime) {
                clip.setClipTime(maxFrameTime);
            }

            root = false;
        }

        return clip;
    }

    public JsonObject getRootJson() {
        return this.rootJson;
    }

    public String getFileHash() {
        return this.filehash;
    }

    public static TransformFormat getAsTransformFormatOrDefault(JsonObject jsonObject, String propertyName) {
        return jsonObject.has(propertyName) ? ParseUtil.enumValueOfOrNull(TransformFormat.class, GsonHelper.getAsString(jsonObject, propertyName)) : TransformFormat.MATRIX;
    }

    public AnimationClip loadAnimationClip(Armature armature) {
        TransformFormat format = getAsTransformFormatOrDefault(this.rootJson, "format");
        JsonArray array = this.rootJson.get("animation").getAsJsonArray();
        AnimationClip clip = new AnimationClip();
        boolean root = true;

        for (JsonElement element : array) {
            JsonObject jObject = element.getAsJsonObject();
            String name = jObject.get("name").getAsString();
            Joint joint = armature.searchJointByName(name);

            if (joint == null) {
                continue;
            }

            TransformSheet sheet = getTransformSheet(element.getAsJsonObject(), OpenMatrix4f.invert(joint.getLocalTransform(), null), root, format);
            clip.addJointTransform(name, sheet);
            float maxFrameTime = sheet.maxFrameTime();

            if (clip.getClipTime() < maxFrameTime) {
                clip.setClipTime(maxFrameTime);
            }

            root = false;
        }

        return clip;
    }

    /// @param invLocalTransform    nullable if transformFormat == [TransformFormat#ATTRIBUTES]
    /// @param rootCorrection       no matter what the value is if transformFormat == [TransformFormat#ATTRIBUTES]
    public static TransformSheet getTransformSheet(JsonObject jObject, @Nullable OpenMatrix4f invLocalTransform, boolean rootCorrection, TransformFormat transformFormat) throws AssetLoadingException, JsonParseException {
        JsonArray timeArray = jObject.getAsJsonArray("time");
        JsonArray transformArray = jObject.getAsJsonArray("transform");

        if (timeArray.size() != transformArray.size()) {
            throw new AssetLoadingException(
                    "Can't read transform sheet: the size of timestamp and transform array is different."
                    + "timestamp array size: " + timeArray.size() + ", transform array size: " + transformArray.size()
            );
        }

        int timesCount = timeArray.size();
        List<Keyframe> keyframeList = Lists.newArrayList();

        for (int i = 0; i < timesCount; i++) {
            float timeStamp = timeArray.get(i).getAsFloat();

            if (timeStamp < 0.0F) {
                continue;
            }

            // WORKAROUND: The case when transform format is wrongly specified!
            if (transformFormat == TransformFormat.ATTRIBUTES && transformArray.get(i).isJsonArray()) {
                transformFormat = TransformFormat.MATRIX;
            } else if (transformFormat == TransformFormat.MATRIX && transformArray.get(i).isJsonObject()) {
                transformFormat = TransformFormat.ATTRIBUTES;
            }

            switch (transformFormat) {
            case MATRIX -> {
                JsonArray matrixArray = transformArray.get(i).getAsJsonArray();
                float[] matrixElements = new float[16];

                for (int j = 0; j < 16; j++) {
                    matrixElements[j] = matrixArray.get(j).getAsFloat();
                }

                OpenMatrix4f matrix = OpenMatrix4f.load(null, matrixElements);
                matrix.transpose();

                if (rootCorrection) {
                    matrix.mulFront(BLENDER_TO_MINECRAFT_COORD);
                }

                matrix.mulFront(invLocalTransform);

                JointTransform transform = JointTransform.fromMatrix(matrix);
                transform.rotation().normalize();
                keyframeList.add(new Keyframe(timeStamp, transform));
            }
            case ATTRIBUTES -> {
                JsonObject transformObject = transformArray.get(i).getAsJsonObject();
                JsonArray locArray = transformObject.get("loc").getAsJsonArray();
                JsonArray rotArray = transformObject.get("rot").getAsJsonArray();
                JsonArray scaArray = transformObject.get("sca").getAsJsonArray();
                JointTransform transform
                    = JointTransform.fromPrimitives(
                          locArray.get(0).getAsFloat()
                        , locArray.get(1).getAsFloat()
                        , locArray.get(2).getAsFloat()
                        , -rotArray.get(1).getAsFloat()
                        , -rotArray.get(2).getAsFloat()
                        , -rotArray.get(3).getAsFloat()
                        , rotArray.get(0).getAsFloat()
                        , scaArray.get(0).getAsFloat()
                        , scaArray.get(1).getAsFloat()
                        , scaArray.get(2).getAsFloat()
                    );

                keyframeList.add(new Keyframe(timeStamp, transform));
            }
            }
        }

        TransformSheet sheet = new TransformSheet(keyframeList);

        return sheet;
    }

    /// Determines how the transform is expressed in json
    ///
    /// [TransformFormat#MATRIX] be like,
    /// [0, 1, 2, ..., 15]
    ///
    /// [TransformFormat#ATTRIBUTES] be like,
    /// {
    ///   "loc": [0, 0, 0],
    ///   "rot": [0, 0, 0, 1],
    ///   "sca": [1, 1, 1],
    /// }
    public enum TransformFormat {
        MATRIX, ATTRIBUTES
    }
}