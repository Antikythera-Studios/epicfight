package yesman.epicfight.world.capabilities.entitypatch;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import org.joml.Vector4f;
import yesman.epicfight.api.client.animation.property.TrailInfo;
import yesman.epicfight.api.event.IdentifierProvider;
import yesman.epicfight.api.utils.math.Vec2i;
import yesman.epicfight.api.utils.side.ClientOnly;
import yesman.epicfight.client.renderer.EpicFightRenderTypes;
import yesman.epicfight.EpicFight;
import yesman.epicfight.world.capabilities.item.CapabilityItem;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public final class EntityDecorations {
    private final Map<ResourceLocation, RenderAttributeModifier<Vec2i>> overlay = new HashMap<> ();
    private final Map<ResourceLocation, RenderAttributeModifier<Vector4f>> colors = new HashMap<> ();
    private final Map<ResourceLocation, RenderAttributeModifier<Vec2i>> lights = new HashMap<> ();
    private final Map<ResourceLocation, AnimationPropertyModifier<SoundEvent, CapabilityItem>> swingSound = new HashMap<> ();
    private final Map<ResourceLocation, AnimationPropertyModifier<SoundEvent, CapabilityItem>> hurtSound = new HashMap<> ();
    private final Map<ResourceLocation, AnimationPropertyModifier<TrailInfo, CapabilityItem>> trail = new HashMap<> ();
    private final Map<ResourceLocation, ParticleGenerator> particleGenerator = new HashMap<> ();
    private final Map<ResourceLocation, DecorationOverlay> decorationOverlays = new HashMap<> ();

    public void addOverlayCoordModifier(IdentifierProvider identifierProvider, RenderAttributeModifier<Vec2i> overlayModifier) {
        this.overlay.put(identifierProvider.getId(), overlayModifier);
    }

    public boolean removeOverlayCoordModifier(IdentifierProvider identifierProvider) {
        return this.overlay.remove(identifierProvider.getId()) != null;
    }

    public void addColorModifier(IdentifierProvider identifierProvider, RenderAttributeModifier<Vector4f> colorModifier) {
        this.colors.put(identifierProvider.getId(), colorModifier);
    }

    public boolean removeColorModifier(IdentifierProvider identifierProvider) {
        return this.colors.remove(identifierProvider.getId()) != null;
    }

    public void addLightModifier(IdentifierProvider identifierProvider, RenderAttributeModifier<Vec2i> lightModifier) {
        this.lights.put(identifierProvider.getId(), lightModifier);
    }

    public boolean removeLightModifier(IdentifierProvider identifierProvider) {
        return this.lights.remove(identifierProvider.getId()) != null;
    }

    public void addSwingSoundModifier(IdentifierProvider identifierProvider, AnimationPropertyModifier<SoundEvent, CapabilityItem> swingSoundModifier) {
        this.swingSound.put(identifierProvider.getId(), swingSoundModifier);
    }

    public boolean removeSwingSoundModifier(IdentifierProvider identifierProvider) {
        return this.swingSound.remove(identifierProvider.getId()) != null;
    }

    public void addHurtSoundModifier(IdentifierProvider identifierProvider, AnimationPropertyModifier<SoundEvent, CapabilityItem> hurtSoundModifier) {
        this.hurtSound.put(identifierProvider.getId(), hurtSoundModifier);
    }

    public boolean removeHurtSoundModifier(IdentifierProvider identifierProvider) {
        return this.hurtSound.remove(identifierProvider.getId()) != null;
    }

    public void addTrailInfoModifier(IdentifierProvider identifierProvider, AnimationPropertyModifier<TrailInfo, CapabilityItem> trailInfoModifier) {
        this.trail.put(identifierProvider.getId(), trailInfoModifier);
    }

    public boolean removeTrailInfoModifier(IdentifierProvider identifierProvider) {
        return this.trail.remove(identifierProvider.getId()) != null;
    }

    public void addParticleGenerator(IdentifierProvider identifierProvider, ParticleGenerator particleGenerator) {
        this.particleGenerator.put(identifierProvider.getId(), particleGenerator);
    }

    public boolean removeParticleGenerator(IdentifierProvider identifierProvider) {
        return this.particleGenerator.remove(identifierProvider.getId()) != null;
    }

    public void addDecorationOverlay(IdentifierProvider identifierProvider, DecorationOverlay entityOverlay) {
        this.decorationOverlays.put(identifierProvider.getId(), entityOverlay);
    }

    public void removeDecorationOverlay(IdentifierProvider identifierProvider) {
        this.decorationOverlays.remove(identifierProvider.getId());
    }

    public void modifyOverlay(Vec2i overlayCoord, float partialTick) {
        for (RenderAttributeModifier<Vec2i> modifier : this.overlay.values()) {
            modifier.modifyValue(overlayCoord, partialTick);
        }

        overlayCoord.x = Mth.clamp(overlayCoord.x, 0, 15);
        overlayCoord.y = Mth.clamp(overlayCoord.y, 0, 15);
    }

    public void modifyColor(Vector4f vec, float partialTick) {
        for (RenderAttributeModifier<Vector4f> modifier : this.colors.values()) {
            modifier.modifyValue(vec, partialTick);
        }

        vec.x = Mth.clamp(vec.x, 0.0F, 1.0F);
        vec.y = Mth.clamp(vec.y, 0.0F, 1.0F);
        vec.z = Mth.clamp(vec.z, 0.0F, 1.0F);
        vec.w = Mth.clamp(vec.w, 0.0F, 1.0F);
    }

    public void modifyLight(Vec2i mi, float partialTick) {
        for (RenderAttributeModifier<Vec2i> modifier : this.lights.values()) {
            modifier.modifyValue(mi, partialTick);
        }

        mi.x = Mth.clamp(mi.x, 0, 15);
        mi.y = Mth.clamp(mi.y, 0, 15);
    }

    public Stream<DecorationOverlay> listDecorationOverlays() {
        return this.decorationOverlays.values().stream();
    }

    public SoundEvent getModifiedSwingSound(SoundEvent original, CapabilityItem item) {
        for (AnimationPropertyModifier<SoundEvent, CapabilityItem> v : this.swingSound.values()) {
            original = v.getModifiedValue(original, item);
        }

        return original;
    }

    public SoundEvent getModifiedHurtSound(SoundEvent original, CapabilityItem item) {
        for (AnimationPropertyModifier<SoundEvent, CapabilityItem> v : this.hurtSound.values()) {
            original = v.getModifiedValue(original, item);
        }

        return original;
    }

    public TrailInfo getModifiedTrailInfo(TrailInfo original, CapabilityItem item) {
        for (AnimationPropertyModifier<TrailInfo, CapabilityItem> v : this.trail.values()) {
            original = v.getModifiedValue(original, item);
        }

        return original;
    }

    public void removeAll(IdentifierProvider identifierProvider) {
        ResourceLocation identifier = identifierProvider.getId();

        this.colors.remove(identifier);
        this.overlay.remove(identifier);
        this.lights.remove(identifier);
        this.swingSound.remove(identifier);
        this.hurtSound.remove(identifier);
        this.trail.remove(identifier);
        this.particleGenerator.remove(identifier);
        this.decorationOverlays.remove(identifier);
    }

    public void tick() {
        this.colors.entrySet().removeIf(entry -> entry.getValue().tick());
        this.overlay.entrySet().removeIf(entry -> entry.getValue().tick());
        this.lights.entrySet().removeIf(entry -> entry.getValue().tick());
        this.particleGenerator.entrySet().removeIf(entry -> entry.getValue().generateParticles());
    }

    public interface RenderAttributeModifier<T> {
        void modifyValue(T val, float partialTick);

        /// @return whether this attribute is invalid or not
        default boolean tick() {
            return false;
        }
    }

    public interface AnimationPropertyModifier<T, O> {
        T getModifiedValue(T val, O object);
    }

    public interface ParticleGenerator {
        /// @return whether this attribute is invalid or not
        boolean generateParticles();
    }

    @ClientOnly
    public interface DecorationOverlay {
        ResourceLocation GENERIC = EpicFight.identifier("textures/common/white.png");
        Vector4f NO_COLOR = new Vector4f(1.0F);

        default Vector4f color(float partialTick) {
            return NO_COLOR;
        }

        default RenderType getRenderType() {
            return EpicFightRenderTypes.overlayModel(GENERIC);
        }

        default boolean shouldRender() {
            return true;
        }
    }
}
