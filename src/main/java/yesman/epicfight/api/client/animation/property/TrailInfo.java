package yesman.epicfight.api.client.animation.property;

import java.util.Locale;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.netty.util.internal.StringUtil;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;
import yesman.epicfight.api.utils.ParseUtil;
import yesman.epicfight.api.utils.math.Vec3f;
import yesman.epicfight.EpicFight;
import yesman.epicfight.registry.entries.EpicFightParticles;

public record TrailInfo(
	  Vec3 start
	, Vec3 end
	, String joint
	, SimpleParticleType particle
	, float startTime
	, float endTime
	, float fadeTime
	, float rCol
	, float gCol
	, float bCol
	, int interpolateCount
	, int trailLifetime
	, int updateInterval
	, int blockLight
	, int skyLight
	, ResourceLocation texturePath
	, InteractionHand hand
) {
    public static final ResourceLocation GENERIC_TRAIL_TEXTURE = EpicFight.identifier("textures/particle/swing_trail.png");
    public static final ResourceLocation SWORDMASTER_SWING_TRAIL_TEX = EpicFight.identifier("textures/particle/swordmaster_trail.png");
	
	public static final TrailInfo PREVIEWER_DEFAULT_TRAIL = TrailInfo.builder()
			.startPos(new Vec3(0.0D, 0.0D, 0.0D))
			.endPos(new Vec3(0.0D, 0.0D, -1.0D))
			.interpolations(4)
			.lifetime(4)
			.r(0.75F)
			.g(0.75F)
			.b(0.75F)
			.texture(GENERIC_TRAIL_TEXTURE)
			.type(EpicFightParticles.SWING_TRAIL.get())
			.create();
	
	public TrailInfo.Builder unpackAsBuilder() {
		return new TrailInfo.Builder()
				.startPos(this.start)
				.endPos(this.end)
				.joint(this.joint)
				.type(this.particle)
				.time(this.startTime, this.endTime)
				.fadeTime(this.fadeTime)
				.r(this.rCol)
				.g(this.gCol)
				.b(this.bCol)
				.interpolations(this.interpolateCount)
				.lifetime(this.trailLifetime)
				.updateInterval(this.updateInterval)
				.blockLight(this.blockLight)
				.skyLight(this.skyLight)
				.texture(this.texturePath)
				.itemSkinHand(this.hand)
		;
	}
	
	public static final TrailInfo ANIMATION_DEFAULT_TRAIL = TrailInfo.builder()
			.time(0.1F, 0.2F)
			.joint("Tool_R")
			.itemSkinHand(InteractionHand.MAIN_HAND)
			.create();
	
	private TrailInfo(TrailInfo.Builder builder) {
		this(
			  builder.start
			, builder.end
			, builder.joint
			, builder.particle
			, builder.startTime
			, builder.endTime
			, builder.fadeTime
			, builder.rCol
			, builder.gCol
			, builder.bCol
			, builder.interpolateCount
			, builder.trailLifetime
			, builder.updateInterval
			, builder.blockLight
			, builder.skyLight
			, builder.texturePath
			, builder.hand
		);
	}
	
	public TrailInfo overwrite(TrailInfo trailInfo) {
		boolean validTime = isValidTime(trailInfo.startTime) && isValidTime(trailInfo.endTime);
		boolean validColor = trailInfo.rCol >= 0.0F && trailInfo.gCol >= 0.0F && trailInfo.bCol >= 0.0F;
		TrailInfo.Builder builder = new TrailInfo.Builder();
		
		builder.startPos((trailInfo.start == null) ? this.start : trailInfo.start);
		builder.endPos((trailInfo.end == null) ? this.end : trailInfo.end);
		builder.joint((trailInfo.joint == null) ? this.joint : trailInfo.joint);
		builder.type((trailInfo.particle == null) ? this.particle : trailInfo.particle);
		builder.time(!validTime ? this.startTime : trailInfo.startTime, !validTime ? this.endTime : trailInfo.endTime);
		builder.fadeTime((!isValidTime(trailInfo.fadeTime)) ? this.fadeTime : trailInfo.fadeTime);
		builder.r(!validColor ? this.rCol : trailInfo.rCol);
		builder.g(!validColor ? this.gCol : trailInfo.gCol);
		builder.b(!validColor ? this.bCol : trailInfo.bCol);
		builder.blockLight(Math.max(this.blockLight, trailInfo.blockLight));
		builder.skyLight(Math.max(this.skyLight, trailInfo.skyLight));
		builder.interpolations((trailInfo.interpolateCount < 0) ? this.interpolateCount : trailInfo.interpolateCount);
		builder.lifetime((trailInfo.trailLifetime < 0) ? this.trailLifetime : trailInfo.trailLifetime);
		builder.texture((trailInfo.texturePath == null) ? this.texturePath : trailInfo.texturePath);
		builder.itemSkinHand((trailInfo.hand == null) ? this.hand : trailInfo.hand);
		
		return builder.create();
	}
	
	public static boolean isValidTime(float time) {
		return !Float.isNaN(time) && time >= 0.0F;
	}
	
	public boolean playable() {
		if (this.particle() == EpicFightParticles.SWING_TRAIL.get()) {
			return this.start != null
					&& this.end != null
					&& this.particle != null
					&& this.updateInterval > 0
					&& !StringUtil.isNullOrEmpty(this.joint)
					&& isValidTime(this.startTime)
					&& isValidTime(this.endTime)
					&& this.interpolateCount > 0
					&& this.trailLifetime > 0
					&& this.texturePath != null;
		} else {
			return this.start != null
					&& this.end != null
					&& this.particle != null
					&& this.updateInterval > 0
					&& this.interpolateCount > 0
					&& this.trailLifetime > 0
					&& this.texturePath != null;
		}
	}
	
	public static TrailInfo.Builder builder() {
		return new TrailInfo.Builder();
	}
	
	public static TrailInfo deserialize(JsonElement json) {
		JsonObject trailObj = json.getAsJsonObject();
		TrailInfo.Builder trailBuilder = TrailInfo.builder();
		
		if (trailObj.has("start_time") && trailObj.has("end_time")) {
			float startTime = GsonHelper.getAsFloat(trailObj, "start_time");
			float endTime = GsonHelper.getAsFloat(trailObj, "end_time");
			trailBuilder.time(startTime, endTime);
		}
		
		if (trailObj.has("fade_time")) {
			float fadeTime = trailObj.get("fade_time").getAsFloat();
			trailBuilder.fadeTime(fadeTime);
		}
		
		if (trailObj.has("lifetime")) {
			trailBuilder.lifetime(GsonHelper.getAsInt(trailObj, "lifetime"));
		}
		
		if (trailObj.has("interpolations")) {
			trailBuilder.interpolations(GsonHelper.getAsInt(trailObj, "interpolations"));
		}
		
		if (trailObj.has("joint")) {
			trailBuilder.joint(GsonHelper.getAsString(trailObj, "joint"));
		}
		
		if (trailObj.has("texture_path")) {
			trailBuilder.texture(GsonHelper.getAsString(trailObj, "texture_path"));
		}
		
		if (trailObj.has("particle_type")) {
			String particleTypeName = GsonHelper.getAsString(trailObj, "particle_type");
			SimpleParticleType particleType = (SimpleParticleType)BuiltInRegistries.PARTICLE_TYPE.get(ResourceLocation.parse(particleTypeName));
			trailBuilder.type(particleType);
		}
		
		if (trailObj.has("color")) {
			JsonArray color = trailObj.get("color").getAsJsonArray();
			Vec3f colorVec = ParseUtil.toVector3f(color);
			trailBuilder.r(colorVec.x / 255F);
			trailBuilder.g(colorVec.y / 255F);
			trailBuilder.b(colorVec.z / 255F);
		}
		
		if (trailObj.has("begin_pos")) {
			JsonArray beginPos = trailObj.get("begin_pos").getAsJsonArray();
			Vec3 begin = ParseUtil.toVector3d(beginPos);
			trailBuilder.startPos(begin);
		}
		
		if (trailObj.has("end_pos")) {
			JsonArray endPos = trailObj.get("end_pos").getAsJsonArray();
			Vec3 end = ParseUtil.toVector3d(endPos);
			trailBuilder.endPos(end);
		}
		
		if (trailObj.has("item_skin_hand")) {
			String itemSkinHand = trailObj.get("item_skin_hand").getAsString();
			InteractionHand hand = InteractionHand.valueOf(itemSkinHand.toUpperCase(Locale.ROOT));
			trailBuilder.itemSkinHand(hand);
		}
		
		if (trailObj.has("block_light")) {
			int blockLight = trailObj.get("block_light").getAsInt();
			trailBuilder.blockLight(blockLight);
		}
		
		if (trailObj.has("sky_light")) {
			int skyLight = trailObj.get("sky_light").getAsInt();
			trailBuilder.skyLight(skyLight);
		}
		
		return trailBuilder.create();
	}
	
	public static TrailInfo deserialize(CompoundTag compoundTag) {
		TrailInfo.Builder trailBuilder = TrailInfo.builder();
		
		if (compoundTag.contains("start_time") && compoundTag.contains("end_time")) {
			float startTime = compoundTag.getFloat("start_time");
			float endTime = compoundTag.getFloat("end_time");
			trailBuilder.time(startTime, endTime);
		}
		
		if (compoundTag.contains("fade_time")) {
			float fadeTime = compoundTag.getFloat("fade_time");
			trailBuilder.fadeTime(fadeTime);
		}
		
		if (compoundTag.contains("lifetime")) {
			trailBuilder.lifetime(compoundTag.getInt("lifetime"));
		}
		
		if (compoundTag.contains("interpolations")) {
			trailBuilder.interpolations(compoundTag.getInt("interpolations"));
		}
		
		if (compoundTag.contains("update_interval")) {
			trailBuilder.updateInterval(compoundTag.getInt("update_interval"));
		}
		
		if (compoundTag.contains("joint")) {
			trailBuilder.joint(compoundTag.getString("joint"));
		}
		
		if (compoundTag.contains("texture_path")) {
			trailBuilder.texture(compoundTag.getString("texture_path"));
		}
		
		if (compoundTag.contains("particle_type")) {
			String particleTypeName = compoundTag.getString("particle_type");
			
			SimpleParticleType particleType = (SimpleParticleType)BuiltInRegistries.PARTICLE_TYPE.get(ResourceLocation.parse(particleTypeName));
			trailBuilder.type(particleType);
		}
		
		if (compoundTag.contains("color")) {
			ListTag color = compoundTag.getList("color", Tag.TAG_INT);
			trailBuilder.r(color.getInt(0) / 255F);
			trailBuilder.g(color.getInt(1) / 255F);
			trailBuilder.b(color.getInt(2) / 255F);
		}
		
		if (compoundTag.contains("begin_pos")) {
			ListTag beginPos = compoundTag.getList("begin_pos", Tag.TAG_DOUBLE);
			trailBuilder.startPos(new Vec3(beginPos.getDouble(0), beginPos.getDouble(1), beginPos.getDouble(2)));
		}
		
		if (compoundTag.contains("end_pos")) {
			ListTag endPos = compoundTag.getList("end_pos", Tag.TAG_DOUBLE);
			trailBuilder.endPos(new Vec3(endPos.getDouble(0), endPos.getDouble(1), endPos.getDouble(2)));
		}
		
		if (compoundTag.contains("block_light")) {
			int blockLight = compoundTag.getInt("block_light");
			trailBuilder.blockLight(blockLight);
		}
		
		if (compoundTag.contains("sky_light")) {
			int skyLight = compoundTag.getInt("sky_light");
			trailBuilder.skyLight(skyLight);
		}
		
		if (compoundTag.contains("item_skin_hand")) {
			String itemSkinHand = compoundTag.getString("item_skin_hand");
			InteractionHand hand = InteractionHand.valueOf(itemSkinHand.toUpperCase(Locale.ROOT));
			trailBuilder.itemSkinHand(hand);
		}
		
		return trailBuilder.create();
	}
	
	public static class Builder {
		private Vec3 start;
		private Vec3 end;
		private SimpleParticleType particle;
		private String joint;
		private float startTime = Float.NaN;
		private float endTime = Float.NaN;
		private float fadeTime = Float.NaN;
		private float rCol = -1.0F;
		private float gCol = -1.0F;
		private float bCol = -1.0F;
		private int interpolateCount = -1;
		private int trailLifetime = -1;
		private int updateInterval = 1;
		private int blockLight;
		private int skyLight;
		private ResourceLocation texturePath;
		private InteractionHand hand = InteractionHand.MAIN_HAND;
		
		public TrailInfo.Builder startPos(Vec3 start) {
			this.start = start;
			return this;
		}
		
		public TrailInfo.Builder endPos(Vec3 end) {
			this.end = end;
			return this;
		}
		
		public TrailInfo.Builder type(SimpleParticleType particle) {
			this.particle = particle;
			return this;
		}
		
		public TrailInfo.Builder joint(String joint) {
			this.joint = joint;
			return this;
		}
		
		public TrailInfo.Builder time(float startTime, float endTime) {
			this.startTime = startTime;
			this.endTime = endTime;
			return this;
		}
		
		public TrailInfo.Builder fadeTime(float fadeTime) {
			this.fadeTime = fadeTime;
			return this;
		}
		
		public TrailInfo.Builder r(float rCol) {
			this.rCol = rCol;
			return this;
		}
		
		public TrailInfo.Builder g(float gCol) {
			this.gCol = gCol;
			return this;
		}
		
		public TrailInfo.Builder b(float bCol) {
			this.bCol = bCol;
			return this;
		}
		
		public TrailInfo.Builder interpolations(int interpolateCount) {
			this.interpolateCount = interpolateCount;
			return this;
		}
		
		public TrailInfo.Builder lifetime(int trailLifetime) {
			this.trailLifetime = trailLifetime;
			return this;
		}
		
		public TrailInfo.Builder updateInterval(int updateInterval) {
			this.updateInterval = updateInterval;
			return this;
		}
		
		public TrailInfo.Builder blockLight(int blockLight) {
			this.blockLight = blockLight;
			return this;
		}
		
		public TrailInfo.Builder skyLight(int skyLight) {
			this.skyLight = skyLight;
			return this;
		}
		
		public TrailInfo.Builder texture(String texturePath) {
			this.texturePath = ResourceLocation.parse(texturePath);
			return this;
		}
		
		public TrailInfo.Builder texture(ResourceLocation texturePath) {
			this.texturePath = texturePath;
			return this;
		}
		
		public TrailInfo.Builder itemSkinHand(InteractionHand itemSkinHand) {
			this.hand = itemSkinHand;
			return this;
		}
		
		public TrailInfo create() {
			return new TrailInfo(this);
		}
	}
}