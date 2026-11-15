package yesman.epicfight.compat;

import net.fabricmc.loader.api.FabricLoader;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/// Enables registering mixins for other mods only when those mods are installed.
///
/// Maintainers: Do not reference any game or Epic Fight classes here.
/// This runs during early game loading.
/// [LoadingModList] is used because the mod loader is already initialized and is loading all mods,
/// including Epic Fight.
///
/// @see IMixinConfigPlugin
@ApiStatus.Internal
public abstract class ModMixinPlugin implements IMixinConfigPlugin {
    /// Whether the is installed and is currently loading.
    private final boolean isModInstalled;

    protected ModMixinPlugin() {
        isModInstalled = FabricLoader.getInstance().isModLoaded(this.getModId());
    }

    public abstract @NotNull String getModId();

    @Override
    public void onLoad(String mixinPackage) {

    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return isModInstalled;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {

    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
