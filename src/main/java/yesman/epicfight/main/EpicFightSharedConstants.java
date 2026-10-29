package yesman.epicfight.main;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.api.EnvType;
import yesman.epicfight.api.animation.Animator;
import yesman.epicfight.api.animation.ServerAnimator;
import yesman.epicfight.api.client.animation.ClientAnimator;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

import java.util.function.Function;

public class EpicFightSharedConstants {
	// Model variables
	public static final int MAX_WEIGHTS = 3;
	public static final int MAX_JOINTS = 1000;

	// Ingame variables
	public static final float A_TICK = 0.05F;
	public static final float GENERAL_ANIMATION_TRANSITION_TIME = 0.15F;
	public static final float EXECUTION_DAMAGE = 2147483647F;

	// Environment variables
	public static final boolean IS_DEV_ENV = FabricLoader.getInstance().isDevelopmentEnvironment();

    // Public server domain in AWS
    public static final String PUBLIC_SERVER_DOMAIN = "https://epic-fight.com";

    // When you run Epic Fight web server on local, change the domain to this
    public static final String LOCAL_WEB_SERVER_DOMAIN = "http://127.0.0.1:8080";

	// Sided variables
	private static final Function<LivingEntityPatch<?>, Animator> ANIMATOR_PROVIDER;

	static {
		ANIMATOR_PROVIDER = isPhysicalClient() ? ClientAnimator::getAnimator : ServerAnimator::getAnimator;
	}

	public static Animator getAnimator(LivingEntityPatch<?> entitypatch) {
		return ANIMATOR_PROVIDER.apply(entitypatch);
	}

	public static boolean isPhysicalClient() {
		return FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT;
	}

    /**
     * Returns a domain of Epic Fight web server to access patron cosmetics
     */
    public static String webServerDomain() {
        return PUBLIC_SERVER_DOMAIN;
    }
}
