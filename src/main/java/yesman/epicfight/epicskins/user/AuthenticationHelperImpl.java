package yesman.epicfight.epicskins.user;
import yesman.epicfight.EpicFight;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonReader;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;
import yesman.epicfight.platform.neoforged.common.ModConfigSpec;
import yesman.epicfight.client.ClientEngine;
import yesman.epicfight.client.online.EpicFightServerConnectionHelper;
import yesman.epicfight.config.ClientConfig;
import yesman.epicfight.epicskins.client.screen.AvatarEditScreen;
import yesman.epicfight.epicskins.exception.HttpResponseException;
import yesman.epicfight.epicskins.exception.OfflineUserException;
import yesman.epicfight.epicskins.util.JsonConverter;
import yesman.epicfight.main.AuthenticationHelper;
import yesman.epicfight.main.EpicFightMod;
import yesman.epicfight.main.EpicFightSharedConstants;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

public class AuthenticationHelperImpl implements AuthenticationHelper {
	public static boolean checkOnlineUser(User user) {
		return !"0".equals(user.getAccessToken());
	}
	
	private static String profileIdToString(User user) {
		return user.getProfileId().toString().replace("-", "");
	}
	
	public synchronized static AuthenticationHelperImpl getInstance() {
		return (AuthenticationHelperImpl)(ClientEngine.getInstance().getAuthHelper());
	}
	
	private final Multimap<Cosmetic.Slot, Cosmetic> cosmeticsBySlot = HashMultimap.create();
	private final Map<Integer, Cosmetic> cosmetics = Maps.newHashMap();
	
	private PlayerInfo playerInfo;
	private String accessToken;
	private String refreshToken;
	private Status status = Status.UNAUTHENTICATED;
	private AuthenticationProvider authProvider;
	private final CapeProperties capeProperties = new CapeProperties();
	
	private AuthenticationHelperImpl() {
        // Initialize Auth Helper
        ClientEngine.getInstance().initAuthHelper(this);
	}
	
	@Override
	public void initialize(
		ModConfigSpec.ConfigValue<String> accessToken,
		ModConfigSpec.ConfigValue<String> refreshToken,
		ModConfigSpec.EnumValue<AuthenticationProvider> provider
	) {
        // Terminate when already initialized
        if (this.status != Status.UNAUTHENTICATED) {
            return;
        }

		this.accessToken = accessToken.get();
		this.refreshToken = refreshToken.get();
		this.authProvider = provider.get();
		
		User user = Minecraft.getInstance().getUser();
		
		if (checkOnlineUser(user)) {
			EpicFightServerConnectionHelper.autoLogin(EpicFightSharedConstants.webServerDomain(), profileIdToString(user), this.accessToken, this.refreshToken, this.authProvider.toString(), (response, exception) -> {
				if (exception != null) {
					EpicFight.LOGGER.warn("Exeception fired in automatic login", exception);
				} else {
					if (response.statusCode() == 200) {
						JsonObject responseJson = JsonConverter.parseJson(response.body()).getAsJsonObject();
						this.onAuthenticationSuccess(responseJson, () -> {}, (ex) -> {
							
						});
					} else {
						EpicFight.LOGGER.warn("Auto login failed with status code " + response.statusCode() + ": " + response.body());
					}
				}
			});
		} else {
			this.status = Status.OFFLINE_MODE;
		}
	}
	
	@Override
	public Screen getAvatarEditorScreen(Screen parentScreen) {
		return new AvatarEditScreen(parentScreen);
	}
	
	@Override
	public boolean valid() {
		return true;
	}
	
	public void openAuthenticateBrowser() {
		User user = Minecraft.getInstance().getUser();
		Util.getPlatform().openUri(URI.create(EpicFightSharedConstants.webServerDomain() + "/login?mc_uuid=" + profileIdToString(user) + "&mc_username=" + user.getName()));
	}
	
	public void loginWithAuthCode(String code, Runnable onSuccess, Consumer<Throwable> onFail) {
		User user = Minecraft.getInstance().getUser();
		
		EpicFightServerConnectionHelper.signIn(EpicFightSharedConstants.webServerDomain(), profileIdToString(user), code, (response, exception) -> {
			if (exception != null) {
				onFail.accept(exception);
				return;
			}
			
			if (response.statusCode() == 200) {
				JsonReader jsonReader = new JsonReader(new InputStreamReader(new ByteArrayInputStream(response.body().getBytes()), StandardCharsets.UTF_8));
				JsonObject jsonObject = Streams.parse(jsonReader).getAsJsonObject();
				this.onAuthenticationSuccess(jsonObject, onSuccess, onFail);
			} else {
				onFail.accept(new HttpResponseException("Invalid code", response.statusCode(), response.body()));
			}
		});
	}
	
	private void onAuthenticationSuccess(JsonObject authResponse, Runnable onSuccess, Consumer<Throwable> onFailed) {
		this.status = Status.AUTHENTICATED;
		this.authProvider = AuthenticationProvider.valueOf(GsonHelper.getAsString(authResponse, "provider").toUpperCase(Locale.ROOT));
		this.accessToken = GsonHelper.getAsString(authResponse, "access_token");
		this.refreshToken = GsonHelper.getAsString(authResponse, "refresh_token");
		
		ClientConfig.ACCESS_TOKEN.set(this.accessToken);
		ClientConfig.REFRESH_TOKNE.set(this.refreshToken);
		ClientConfig.PROVIDER.set(this.authProvider);
		
		JsonArray cosmeticsArray = GsonHelper.getAsJsonArray(authResponse, "cosmetics");
		
		for (JsonElement cosmeticElement : cosmeticsArray) {
			JsonObject cosemticObject = cosmeticElement.getAsJsonObject();
			
			if ("cape".equals(GsonHelper.getAsString(cosemticObject, "slot"))) {
				this.capeProperties.setCape(GsonHelper.getAsInt(cosemticObject, "cosmetic_seq"));
				this.capeProperties.unpackColorSliderPositions(GsonHelper.getAsInt(cosemticObject, "int_param1"));
				this.capeProperties.setVanillaTextureUse(GsonHelper.getAsBoolean(cosemticObject, "bool_param1"));
			}
		}
		
		this.cosmeticsBySlot.clear();
		this.cosmetics.clear();
		User user = Minecraft.getInstance().getUser();
		
		EpicFightServerConnectionHelper.getAvailableCosmetics(EpicFightSharedConstants.webServerDomain(), profileIdToString(user), this.accessToken, this.refreshToken, this.authProvider.toString(), (response, exception) -> {
			if (exception != null) {
				onFailed.accept(exception);
			}
			
			if (response.statusCode() == 200) {
				try {
					JsonObject responseJson = JsonConverter.parseJson(response.body()).getAsJsonObject();
					
					for (JsonElement json : responseJson.getAsJsonArray("object")) {
						Cosmetic cosmetic = new Cosmetic(json.getAsJsonObject());
						this.cosmeticsBySlot.put(cosmetic.slot(), cosmetic);
						this.cosmetics.put(cosmetic.seq(), cosmetic);
					}
					
					onSuccess.run();
				} catch (Exception e) {
                    EpicFight.LOGGER.error("Failed at deserializing json object from remote", e);
				}
			} else {
				onFailed.accept(new HttpResponseException("Failed at getting available cosmetics", response.statusCode(), response.body()));
			}
		});
	}
	
	public void signOut(Runnable onSuccess, Consumer<Throwable> onFailed) {
		User user = Minecraft.getInstance().getUser();
		
		EpicFightServerConnectionHelper.signOut(EpicFightSharedConstants.webServerDomain(), profileIdToString(user), this.accessToken, this.refreshToken, this.authProvider.toString(), (response, exception) -> {
			if (exception != null) {
				onFailed.accept(exception);
			} else {
				if (response.statusCode() == 200) {
					AuthenticationHelperImpl.getInstance().onSignOut();
					onSuccess.run();
				} else {
					onFailed.accept(new HttpResponseException("Sign out failed", response.statusCode(), response.body()));
				}
			}
		});
	}
	
	public void onSignOut() {
		try {
			this.status = Status.UNAUTHENTICATED;
			this.authProvider = AuthenticationProvider.NULL;
			this.accessToken = "";
			this.refreshToken = "";
			
			ClientConfig.ACCESS_TOKEN.set(this.accessToken);
			ClientConfig.REFRESH_TOKNE.set(this.refreshToken);
			ClientConfig.PROVIDER.set(this.authProvider);
			
			this.cosmeticsBySlot.clear();
			this.cosmetics.clear();
		} catch (Exception e) {
            EpicFight.LOGGER.error("Sign out failed", e);
		}
	}
	
	public void sendSaveRequest(Consumer<Throwable> callback) {
		User user = Minecraft.getInstance().getUser();
		
		if (!checkOnlineUser(user)) {
			this.status = AuthenticationHelper.Status.OFFLINE_MODE;
			throw new OfflineUserException("Offline mode user");
		}
		
		JsonObject postBody = new JsonObject();
		postBody.addProperty("provider", this.authProvider.toString());
		postBody.addProperty("access_token", this.accessToken);
		postBody.addProperty("refresh_token", this.refreshToken);
		postBody.addProperty("minecraft_uuid", profileIdToString(user));
		postBody.addProperty("cosmetic_seq", this.capeProperties.capeSeq());
		postBody.addProperty("slot", "cape");
		postBody.addProperty("int_param1", this.capeProperties.packColorSliderPositions());
		postBody.addProperty("bool_param1", this.capeProperties.useVanillaTexture());
		
		EpicFightServerConnectionHelper.saveConfiguration(EpicFightSharedConstants.webServerDomain(), postBody.toString(), (response, exception) -> {
			if (exception != null) {
				callback.accept(exception);
			}
			
			if (response.statusCode() == 200) {
				JsonObject responseJson = JsonConverter.parseJson(response.body()).getAsJsonObject();
				this.accessToken = GsonHelper.getAsString(responseJson, "accessToken");
				this.refreshToken = GsonHelper.getAsString(responseJson, "refreshToken");
				ClientConfig.ACCESS_TOKEN.set(this.accessToken);
				ClientConfig.REFRESH_TOKNE.set(this.refreshToken);
				
				callback.accept(null);
			} else {
				callback.accept(new HttpResponseException("Failed at updating cosmetic information", response.statusCode(), response.body()));
			}
		});
	}

    @Override
    public void loadPlayerSkin() {
        this.playerInfo = new PlayerInfo(Minecraft.getInstance().getGameProfile(), false);
        // Indirectly call registerTextures that loads the player's skin textures
        ResourceLocation rl = this.playerInfo.getSkin().texture();
    }

	public void setStatus(Status status) {
		this.status = status;
	}
	
	public AuthenticationProvider authProvider() {
		return this.authProvider;
	}
	
	public String getAccessToken() {
		return this.accessToken;
	}
	
	public String getRefreshToken() {
		return this.refreshToken;
	}

    @Override
	public Status status() {
		return this.status;
	}
	
	public PlayerInfo playerInfo() {
		return this.playerInfo;
	}
	
	public Collection<Cosmetic> getAllCosmetics() {
		return this.cosmetics.values();
	}
	
	public Collection<Cosmetic> getCosmeticsBySlot(Cosmetic.Slot cosmeticSlot) {
		return this.cosmeticsBySlot.get(cosmeticSlot);
	}
	
	public Cosmetic getCosmetic(int seq) {
		return this.cosmetics.get(seq);
	}
	
	public CapeProperties capeProperties() {
		return this.capeProperties;
	}
	
	public static class CapeProperties {
		// Cape information
		private int capeSeq;
		private boolean useVanillaTexture;
		private double hue;
		private double saturation;
		private double brightness;
		
		public void setCape(int capeSeq) {
			this.capeSeq = capeSeq;
		}
		
		public void setVanillaTextureUse(boolean useVanillaTexture) {
			this.useVanillaTexture = useVanillaTexture;
		}
		
		public void setHue(double hue) {
			this.hue = hue;
		}
		
		public void setSaturation(double saturation) {
			this.saturation = saturation;
		}
		
		public void setBrightness(double brightness) {
			this.brightness = brightness;
		}
		
		public int capeSeq() {
			return this.capeSeq;
		}
		
		public boolean useVanillaTexture() {
			return this.useVanillaTexture;
		}
		
		public double hue() {
			return this.hue;
		}
		
		public double saturation() {
			return this.saturation;
		}
		
		public double brightness() {
			return this.brightness;
		}
		
		public void unpackColorSliderPositions(int packedColorSlider) {
			this.brightness = (packedColorSlider & 255) / 255.0F;
			this.saturation = ((packedColorSlider & 65280) >> 8) / 255.0F;
			this.hue = ((packedColorSlider & 16711680) >> 16) / 255.0F;
		}
		
		public int packColorSliderPositions() {
			double huePos = Mth.clamp(this.hue, 0.0F, 1.0F);
			double saturationPos = Mth.clamp(this.saturation, 0.0F, 1.0F);
			double brightnessPos = Mth.clamp(this.brightness, 0.0F, 1.0F);
			
			return ((int)(huePos * 255.0D) << 16) | ((int)(saturationPos * 255.0D) << 8) | (int)(brightnessPos * 255.0D);
		}
	}
}