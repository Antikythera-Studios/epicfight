package dev.isxander.controlify.api.entrypoint;
import dev.isxander.controlify.api.guide.GuideDomainRegistry;
import dev.isxander.controlify.api.guide.InGameCtx;
import dev.isxander.controlify.api.guide.ContainerCtx;
public class PreInitContext {
    public PreInitContext guideRegistries() { return this; }
    public PreInitContext registerIcon(net.minecraft.resources.ResourceLocation id, java.util.function.Consumer<dev.isxander.controlify.utils.render.CGuiPose> consumer) { return this; }
    public GuideDomainRegistry<InGameCtx> inGame() { return new GuideDomainRegistry<>(); }
    public GuideDomainRegistry<ContainerCtx> container() { return new GuideDomainRegistry<>(); }
}
