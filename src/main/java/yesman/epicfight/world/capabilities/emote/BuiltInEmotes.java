package yesman.epicfight.world.capabilities.emote;

import net.minecraft.resources.ResourceKey;
import yesman.epicfight.client.online.cosmetics.Emote;
import yesman.epicfight.EpicFight;
import yesman.epicfight.registry.EpicFightRegistries;

public interface BuiltInEmotes {
    ResourceKey<Emote> FRUSTRATED = ResourceKey.create(EpicFightRegistries.Keys.EMOTE, EpicFight.identifier("frustrated"));
    ResourceKey<Emote> HOPAK = ResourceKey.create(EpicFightRegistries.Keys.EMOTE, EpicFight.identifier("hopak"));
    ResourceKey<Emote> LAUGH = ResourceKey.create(EpicFightRegistries.Keys.EMOTE, EpicFight.identifier("laugh"));
    ResourceKey<Emote> SALUTE = ResourceKey.create(EpicFightRegistries.Keys.EMOTE, EpicFight.identifier("salute"));
    ResourceKey<Emote> SLIT_THROAT = ResourceKey.create(EpicFightRegistries.Keys.EMOTE, EpicFight.identifier("slit_throat"));
    ResourceKey<Emote> WAVE_HAND = ResourceKey.create(EpicFightRegistries.Keys.EMOTE, EpicFight.identifier("wave_hand"));
}
