package yesman.epicfight.world.capabilities.emote;
import yesman.epicfight.EpicFight;

import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.lang3.mutable.MutableInt;
import org.jetbrains.annotations.Nullable;
import yesman.epicfight.client.online.cosmetics.Emote;
import yesman.epicfight.main.EpicFightMod;
import yesman.epicfight.registry.EpicFightRegistries;
import yesman.epicfight.util.TagUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PlayerEmoteSlots {
    private final List<EmoteTab> emoteTabs = new ArrayList<>();

    public void serialize(CompoundTag compoundTag) {
        ListTag listTag = TagUtils.getOrCreateTag(compoundTag, "emotes", ListTag::new);

        for (EmoteTab emoteTab : this.emoteTabs) {
            ListTag page = new ListTag();

            for (Holder.Reference<Emote> emote : emoteTab.emotes) {
                page.add(StringTag.valueOf(emote != null ? emote.getRegisteredName() : ""));
            }

            listTag.add(page);
        }
    }

    public CompoundTag getSerialized() {
        CompoundTag compoundTag = new CompoundTag();
        this.serialize(compoundTag);
        return compoundTag;
    }

    public void deserialize(CompoundTag compoundTag, RegistryAccess registryAccess) {
        ListTag listTag = compoundTag.getList("emotes", CompoundTag.TAG_LIST);
        this.emoteTabs.clear();

        for (Tag pageTag : listTag) {
            EmoteTab page = new EmoteTab();
            MutableInt slotIndex = new MutableInt();

            for (Tag registyNameTag : (ListTag)pageTag) {
                String emoteName = registyNameTag.getAsString();

                if (emoteName.isEmpty()) {
                    page.emotes[slotIndex.getValue()] = null;
                } else {
                    ResourceKey<Emote> emoteResourceKey = ResourceKey.create(EpicFightRegistries.Keys.EMOTE, ResourceLocation.parse(emoteName));
                    Optional<Holder.Reference<Emote>> emoteHolder = registryAccess.registryOrThrow(yesman.epicfight.registry.EpicFightRegistries.Keys.EMOTE).getHolder(emoteResourceKey);

                    emoteHolder.ifPresentOrElse(emote -> {
                        page.emotes[slotIndex.getValue()] = emote;
                    }, () -> {
                        EpicFight.LOGGER.error("Unknown emote: {}. Ignored.", registyNameTag.getAsString());
                    });
                }

                slotIndex.increment();
            }

            if (page.hasAny()) {
                this.emoteTabs.add(page);
            }
        }

        // When emote tab is empty, add default emotes
        if (this.emoteTabs.isEmpty()) {
            EmoteTab defaultPage = new EmoteTab();
            registryAccess.registryOrThrow(yesman.epicfight.registry.EpicFightRegistries.Keys.EMOTE).getHolder(BuiltInEmotes.FRUSTRATED).ifPresent(emote -> defaultPage.emotes[0] = emote);
            registryAccess.registryOrThrow(yesman.epicfight.registry.EpicFightRegistries.Keys.EMOTE).getHolder(BuiltInEmotes.HOPAK).ifPresent(emote -> defaultPage.emotes[1] = emote);
            registryAccess.registryOrThrow(yesman.epicfight.registry.EpicFightRegistries.Keys.EMOTE).getHolder(BuiltInEmotes.LAUGH).ifPresent(emote -> defaultPage.emotes[2] = emote);
            registryAccess.registryOrThrow(yesman.epicfight.registry.EpicFightRegistries.Keys.EMOTE).getHolder(BuiltInEmotes.SALUTE).ifPresent(emote -> defaultPage.emotes[3] = emote);
            registryAccess.registryOrThrow(yesman.epicfight.registry.EpicFightRegistries.Keys.EMOTE).getHolder(BuiltInEmotes.SLIT_THROAT).ifPresent(emote -> defaultPage.emotes[4] = emote);
            registryAccess.registryOrThrow(yesman.epicfight.registry.EpicFightRegistries.Keys.EMOTE).getHolder(BuiltInEmotes.WAVE_HAND).ifPresent(emote -> defaultPage.emotes[5] = emote);

            this.emoteTabs.add(defaultPage);
        }
    }

    public void copyFrom(PlayerEmoteSlots oldData) {
        this.emoteTabs.clear();
        this.emoteTabs.addAll(oldData.emoteTabs);
    }

    public void reset(int pages) {
        this.emoteTabs.clear();

        for (int i = 0; i < pages; i++) {
            this.emoteTabs.add(new EmoteTab());
        }
    }

    public void setEmote(int pageIndex, int slotIndex, @Nullable Holder.Reference<Emote> emote) {
        if (this.emoteTabs.size() <= pageIndex) {
            EpicFight.LOGGER.error("Emote page index {} is out of bound {}", pageIndex, this.emoteTabs.size());
            return;
        }

        if (slotIndex < 0 || slotIndex >= 6) {
            EpicFight.LOGGER.error("Emote slot {} is not a valid index (0~6)", slotIndex);
            return;
        }

        this.emoteTabs.get(pageIndex).emotes[slotIndex] = emote;
    }

    public int tabs() {
        return this.emoteTabs.size();
    }

    public void listEmotes(EmoteIterator task) {
        int pageIndex = 0;

        for (EmoteTab emoteTab : this.emoteTabs) {
            int wheelIndex = 0;

            for (Holder.Reference<Emote> emote : emoteTab.emotes) {
                task.doWork(pageIndex, wheelIndex, emote);
                wheelIndex++;
            }

            pageIndex++;
        }
    }

    private static class EmoteTab {
        public Holder.Reference<Emote>[] emotes = new Holder.Reference[6];

        boolean hasAny() {
            for (Holder.Reference<Emote> emote : this.emotes) {
                if (emote != null) return true;
            }

            return false;
        }
    }
}
