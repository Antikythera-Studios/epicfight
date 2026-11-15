package yesman.epicfight.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;

import yesman.epicfight.platform.neoforged.common.ModConfigSpec;
import yesman.epicfight.client.particle.EpicFightParticleRenderTypes;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;
import yesman.epicfight.main.AuthenticationHelper;
import yesman.epicfight.network.server.SPPlayUISound;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;

public class ClientEngine {
    private static ClientEngine instance = new ClientEngine();

    public static ClientEngine getInstance() {
        return instance;
    }

    public Minecraft minecraft;

    private boolean vanillaModelDebuggingMode = false;
    private AuthenticationHelper authenticationHelper = new AuthenticationHelper() {
        @Override
        public void initialize(
            ModConfigSpec.ConfigValue<String> accessToken,
            ModConfigSpec.ConfigValue<String> refreshToken,
            ModConfigSpec.EnumValue<AuthenticationProvider> provider
        ) {
        }

        @Override
        public boolean valid() {
            return false;
        }

        @Override
        public Status status() {
            return Status.OFFLINE_MODE;
        }

        @Override
        public void loadPlayerSkin() {
        }
    };

    public ClientEngine() {
        instance = this;
        this.minecraft = Minecraft.getInstance();
    }

    public boolean switchVanillaModelDebuggingMode() {
        this.vanillaModelDebuggingMode = !this.vanillaModelDebuggingMode;
        return this.vanillaModelDebuggingMode;
    }

    public boolean isVanillaModelDebuggingMode() {
        return this.vanillaModelDebuggingMode;
    }

    public void initAuthHelper(AuthenticationHelper authHelper) {
        this.authenticationHelper = authHelper;
    }

    public AuthenticationHelper getAuthHelper() {
        return this.authenticationHelper;
    }

    public void playUISound(SPPlayUISound msg) {
        SoundInstance soundinstance = SimpleSoundInstance.forUI(msg.sound().value(), msg.pitch(), msg.volume());

        // Playing a sound twice corrects volume issue...
        Minecraft.getInstance().getSoundManager().play(soundinstance);
        Minecraft.getInstance().getSoundManager().play(soundinstance);
    }

    public boolean isEpicFightMode() {
        LocalPlayerPatch localPlayerPatch = EpicFightCapabilities.getEntityPatch(this.minecraft.player, LocalPlayerPatch.class);

        if (localPlayerPatch == null) {
            return false;
        }

        return localPlayerPatch.isEpicFightMode();
    }

    /// Copy from [ClientHooks#makeParticleRenderTypeComparator] but prioritize [ParticleRenderType#CUSTOM] lowest since it resets GL parameters setup
    public static Comparator<ParticleRenderType> makeCustomLowestParticleRenderTypeComparator(List<ParticleRenderType> renderOrder) {
        Comparator<ParticleRenderType> vanillaComparator = Comparator.comparingInt(renderOrder::indexOf);

        return (t1, t2) -> {
            boolean vanillaType1 = renderOrder.contains(t1);
            boolean vanillaType2 = renderOrder.contains(t2);

            if (vanillaType1 && vanillaType2) {
                return vanillaComparator.compare(t1, t2);
            }

            if (t1.equals(t2)) {
                return 0;
            }

            if (t1 == ParticleRenderType.CUSTOM || t1 == EpicFightParticleRenderTypes.ENTITY_PARTICLE) {
                return 1;
            } else if (t2 == ParticleRenderType.CUSTOM || t2 == EpicFightParticleRenderTypes.ENTITY_PARTICLE) {
                return -1;
            }

            if (!vanillaType1 && !vanillaType2) {
                return Integer.compare(System.identityHashCode(t1), System.identityHashCode(t2));
            }

            return vanillaType1 ? -1 : 1;
        };
    }
}