package yesman.epicfight.compat.simplytooltips;

import com.google.common.collect.Lists;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.sweenus.simplytooltips.api.ModernTooltipModel;
import net.sweenus.simplytooltips.api.TooltipProvider;
import net.sweenus.simplytooltips.api.TooltipTheme;
import org.jetbrains.annotations.NotNull;
import yesman.epicfight.api.ex_cap.data.Moveset;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;
import yesman.epicfight.registry.entries.EpicFightAttributes;
import yesman.epicfight.registry.entries.EpicFightDataComponentTypes;
import yesman.epicfight.registry.entries.EpicFightItems;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.guard.GuardSkill;
import yesman.epicfight.skill.weaponinnate.WeaponInnateSkill;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.capabilities.item.ArmorCapability;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.capabilities.item.WeaponCapability;

import java.text.DecimalFormat;
import java.util.*;
import java.util.function.Predicate;

public class EpicFightTooltipProvider implements TooltipProvider
{
    @Override
    public boolean supports(ItemStack itemStack) {
        return itemStack != null && !itemStack.isEmpty() && (EpicFightCapabilities.getItemStackCapability(itemStack) instanceof WeaponCapability || EpicFightCapabilities.getItemStackCapability(itemStack) instanceof ArmorCapability || itemStack.is(EpicFightItems.SKILLBOOK));
    }

    private List<String> extractString(Component s) {
        return Arrays.asList(s.getString().split("\n"));
    }

    private boolean movesetDetails(Skill innateSkill, Moveset currentSet)
    {
        return innateSkill != null || (currentSet != null && (currentSet.languageExists(currentSet.getRawName()) || currentSet.languageExists(currentSet.getRawDescription())));
    }

    private void handleWeapon(ItemStack itemStack, WeaponCapability weapon, List<Component> filteredLines, List<String> movesetData, List<String> bodyLines, List<Component> extraLines, boolean altDown, boolean pastBlank) {
        if (EpicFightCapabilities.getEntityPatch(Minecraft.getInstance().player, PlayerPatch.class) instanceof LocalPlayerPatch patch)
        {
            Skill innateSkill = weapon.getInnateSkill(patch, itemStack);
            Moveset currentSet = weapon.getCurrentSet(patch);
            if (altDown && movesetDetails(innateSkill, currentSet)) {

                if (currentSet != null)
                {
                    if (currentSet.languageExists(currentSet.getRawName()))
                    {
                        List<String> fullMovesetName = extractString(currentSet.getTranslatedName());
                        movesetData.addAll(fullMovesetName);
                        movesetData.add("");
                    }
                    if (currentSet.languageExists(currentSet.getRawDescription()))
                    {
                        List<String> lines = extractString(currentSet.getDescription());
                        movesetData.addAll(lines);
                    }
                }
                if (innateSkill != null)
                {
                    bodyLines.add(Component.translatable("text.epicfight.innate_skill", innateSkill.getDisplayName()).withStyle(ChatFormatting.BOLD).getString());
                    if (innateSkill instanceof WeaponInnateSkill innateSkill1)
                    {
                        List<Component> innateLine = innateSkill1.getSimplyTooltips(itemStack, weapon, patch);
                        for (var i : innateLine)
                        {
                            bodyLines.add(i.getString());
                        }
                    }
                }
            }
            else
            {
                for(int i = 1; i < filteredLines.size(); ++i) {
                    String s = filteredLines.get(i).getString().trim();
                    if (!pastBlank && s.isEmpty()) {
                        pastBlank = true;
                    } else if (pastBlank) {
                        extraLines.add(filteredLines.get(i));
                    } else {
                        bodyLines.add(filteredLines.get(i).getString());
                    }
                }
                if (movesetDetails(innateSkill, currentSet))
                    bodyLines.add(Component.translatable("text.epicfight.alt_key").getString());

            }
        }
        else
        {
            for(int i = 1; i < filteredLines.size(); ++i) {
                String s = filteredLines.get(i).getString().trim();
                if (!pastBlank && s.isEmpty()) {
                    pastBlank = true;
                } else if (pastBlank) {
                    extraLines.add(filteredLines.get(i));
                } else {
                    bodyLines.add(filteredLines.get(i).getString());
                }
            }
        }
    }



    @Override
    public ModernTooltipModel build(ItemStack itemStack, List<Component> list, boolean altDown) {
        String title = list.isEmpty() ? itemStack.getHoverName().getString() : list.getFirst().getString();
        HeaderStats headerStats = extractMainHandStats(itemStack);
        Component hint = buildStatsHint(headerStats);
        List<Component> filteredLines = filterMainHandAttackLines(list);
        List<String> movesetData = new ArrayList<>();
        List<String> bodyLines = new ArrayList<>();
        boolean pastBlank = false;
        List<Component> extraLines = new ArrayList<>();
        CapabilityItem item = EpicFightCapabilities.getItemStackCapability(itemStack);
        List<String> badges = Lists.newArrayList();
        List<String> affixes = Lists.newArrayList();

        if (item instanceof ArmorCapability)
        {
            for(int i = 1; i < filteredLines.size(); ++i) {
                String s = filteredLines.get(i).getString().trim();
                if (!pastBlank && s.isEmpty()) {
                    pastBlank = true;
                } else if (pastBlank) {
                    extraLines.add(filteredLines.get(i));
                } else {
                    bodyLines.add(filteredLines.get(i).getString());
                }
            }
        }
        if (item instanceof WeaponCapability weaponCapability)
        {
            handleWeapon(itemStack, weaponCapability, filteredLines, movesetData, bodyLines, extraLines, altDown, pastBlank);
        }
        if (itemStack.is(EpicFightItems.SKILLBOOK))
        {
            var skill = itemStack.get(EpicFightDataComponentTypes.SKILL.get());
            if (skill != null)
            {
                badges.add(skill.value().getDisplayName().getString().toUpperCase(Locale.ROOT));
                for(int i = 1; i < filteredLines.size(); ++i) {
                    String s = filteredLines.get(i).getString().trim();
                    if (!pastBlank && s.isEmpty()) {
                        pastBlank = true;
                    } else if (pastBlank) {
                        extraLines.add(filteredLines.get(i));
                    } else {
                        bodyLines.add(filteredLines.get(i).getString());
                    }
                }
                movesetData.add(skill.value().getTranslatedTooltip(itemStack, null, null).getString());
                skill.value().getModfierEntry().forEach(
                        modifier -> {
                            String translationKey = modifier.getKey().value().getDescriptionId();
                            double rawAmount = modifier.getValue().amount();
                            String modifierAmount = formatValueWithOperation(rawAmount, modifier.getValue().operation());
                            affixes.add(Component.translatable(translationKey).append(Component.literal(" ")).append(Component.literal(modifierAmount)).getString());
                        }
                );
                if (skill.value() instanceof GuardSkill guardSkill)
                {
                    affixes.add(Component.translatable("text.epicfigt.guard_skill_consumption", guardSkill.getDisplayName()).append(Component.translatable("skill.epicfight.guard.consume.tooltip")).getString());
                }
            }
        }

        if (badges.isEmpty())
            badges.add("ITEM");

        List<String> resultingAffixes = null;

        if (!affixes.isEmpty())
        {
            resultingAffixes = Lists.newArrayList(affixes);
        }

        return new ModernTooltipModel(title, badges,
                0, movesetData, bodyLines,
                extraLines, TooltipTheme.defaultTheme(),
                null, null, null, hint, resultingAffixes);
    }

    private static @NotNull String formatValueWithOperation(double amount, AttributeModifier.Operation operation) {
        String prefix = amount >= 0 ? "+" : "";
        String formattedValue;

        if (operation == AttributeModifier.Operation.ADD_VALUE) {
            formattedValue = prefix + String.format("%.1f", amount);
        } else {
            formattedValue = prefix + String.format("%.1f%%", amount * 100.0);
        }
        return formattedValue;
    }

    private record HeaderStats(Double damage, Double speed, Double armorNegation, Double impact, Double maxStrikes) {
    }

    private static List<Component> filterMainHandAttackLines(List<Component> rawLines) {
        if (rawLines.size() <= 1) {
            return rawLines;
        } else {
            Set<Integer> remove = new HashSet<>();
            int lastIndex = rawLines.size() - 1;

            for(int i = 1; i <= lastIndex; ++i) {
                Component line = rawLines.get(i);
                if (hasTranslatableKey(line, "item.modifiers.mainhand"::equals)) {
                    List<Integer> attackLines = new ArrayList<>();
                    int nonAttackAttributeLines = 0;

                    int j;
                    for(j = i + 1; j <= lastIndex; ++j) {
                        Component candidate = rawLines.get(j);
                        if (isBlank(candidate) || hasTranslatableKey(candidate, (key) -> key.startsWith("item.modifiers."))) {
                            break;
                        }

                        if (isAttackAttributeLine(candidate)) {
                            attackLines.add(j);
                        } else if (isAttributeModifierLine(candidate)) {
                            ++nonAttackAttributeLines;
                        }
                    }

                    if (!attackLines.isEmpty()) {
                        remove.addAll(attackLines);
                        if (nonAttackAttributeLines == 0) {
                            remove.add(i);
                            if (i > 1 && isBlank(rawLines.get(i - 1))) {
                                remove.add(i - 1);
                            }
                        }
                    }

                    i = j - 1;
                }
            }

            List<Component> filtered = new ArrayList<>(rawLines.size());

            for(int i = 0; i <= lastIndex; ++i) {
                if (!remove.contains(i)) {
                    filtered.add(rawLines.get(i));
                }
            }

            return collapseBlankLines(filtered);
        }
    }

    private static List<Component> collapseBlankLines(List<Component> lines) {
        if (lines.size() <= 1) {
            return lines;
        } else {
            List<Component> compact = new ArrayList<>(lines.size());
            compact.add(lines.getFirst());

            for(int i = 1; i < lines.size(); ++i) {
                Component line = lines.get(i);
                if (!isBlank(line) || compact.size() != 1 && !isBlank(compact.getLast())) {
                    compact.add(line);
                }
            }
            while(compact.size() > 1 && isBlank(compact.getLast())) {
                compact.removeLast();
            }
            return compact;
        }
    }

    private static boolean isAttributeModifierLine(Component line) {
        return hasTranslatableKey(line, (key) -> key.startsWith("attribute.modifier."));
    }

    private static boolean isAttackAttributeLine(Component line) {
        return isAttributeModifierLine(line) && hasTranslatableKey(line, (key) -> "attribute.name.generic.attack_damage".equals(key) || "attribute.name.generic.attack_speed".equals(key));
    }

    private static boolean isBlank(Component line) {
        return line == null || line.getString().trim().isEmpty();
    }

    private static boolean hasTranslatableKey(Component text, Predicate<String> matcher) {
        return text != null && hasTranslatableKey0(text, matcher, 0);
    }

    private static boolean hasTranslatableKey0(Component text, Predicate<String> matcher, int depth) {
        if (depth > 8) {
            return false;
        } else {
            ComponentContents content = text.getContents();
            if (content instanceof TranslatableContents translatable) {
                if (matcher.test(translatable.getKey())) {
                    return true;
                }

                for(Object arg : translatable.getArgs()) {
                    if (arg instanceof Component nested) {
                        if (hasTranslatableKey0(nested, matcher, depth + 1)) {
                            return true;
                        }
                    }
                }
            }

            for(Component sibling : text.getSiblings()) {
                if (hasTranslatableKey0(sibling, matcher, depth + 1)) {
                    return true;
                }
            }

            return false;
        }
    }

    @SuppressWarnings("deprecation")
    private static HeaderStats extractMainHandStats(ItemStack stack) {
        HeaderStat attackDamage = HeaderStat.missing();
        HeaderStat attackSpeed = HeaderStat.missing();
        HeaderStat armorNegation = HeaderStat.missing();
        HeaderStat impact = HeaderStat.missing();
        HeaderStat maxStrikes = HeaderStat.missing();
        stack.forEachModifier(EquipmentSlotGroup.MAINHAND, (attribute, modifier) -> {
            if (attribute.is(Attributes.ATTACK_DAMAGE)) {
                double value = displayedAttributeValue(attribute.is(Attributes.ATTACK_DAMAGE), modifier);
                int priority = modifierPriority(modifier, Item.BASE_ATTACK_DAMAGE_ID);
                if (priority > attackDamage.priority()) {
                    attackDamage.replace(value, priority);
                }
            } else if (attribute.is(Attributes.ATTACK_SPEED)) {
                double value = displayedAttributeValue(attribute.is(Attributes.ATTACK_SPEED), modifier);
                int priority = modifierPriority(modifier, Item.BASE_ATTACK_SPEED_ID);
                if (priority > attackSpeed.priority()) {
                    attackSpeed.replace(value, priority);
                }
            } else if (attribute.is(EpicFightAttributes.ARMOR_NEGATION)) {
                double value = displayedAttributeValue(attribute.is(EpicFightAttributes.ARMOR_NEGATION), modifier);
                int priority = modifierPriority(modifier, EpicFightAttributes.ARMOR_NEGATION_MODIFIER);
                if (priority > armorNegation.priority()) {
                    armorNegation.replace(value, priority);
                }
            } else if (attribute.is(EpicFightAttributes.IMPACT)) {
                double value = displayedAttributeValue(attribute.is(EpicFightAttributes.IMPACT), modifier);
                int priority = modifierPriority(modifier, EpicFightAttributes.IMPACT_MODIFIER);
                if (priority > impact.priority()) {
                    impact.replace(value, priority);
                }
            } else if (attribute.is(EpicFightAttributes.MAX_STRIKES)) {
                double value = displayedAttributeValue(attribute.is(EpicFightAttributes.MAX_STRIKES), modifier);
                int priority = modifierPriority(modifier, EpicFightAttributes.MAX_STRIKE_MODIFIER);
                if (priority > maxStrikes.priority()) {
                    maxStrikes.replace(value, priority);
                }
            }

        });
        return !attackDamage.present() && !attackSpeed.present() && !armorNegation.present() && !impact.present() && !maxStrikes.present() ? null : new HeaderStats(attackDamage.present() ? attackDamage.value() : null, attackSpeed.present() ? attackSpeed.value() : null, armorNegation.present() ? armorNegation.value() : null, impact.present() ? impact.value() : null, maxStrikes.present() ? maxStrikes.value() : null);
    }

    private static Component buildStatsHint(HeaderStats stats) {
        if (stats == null) {
            return null;
        } else {
            List<String> parts = new ArrayList<>(5);

            if (stats.damage() != null) {
                DecimalFormat var10001 = ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT;
                parts.add("\ud83d\udde1 " + var10001.format(stats.damage));
            }

            if (stats.speed() != null) {
                DecimalFormat var2 = ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT;
                parts.add("⌛ " + var2.format(stats.speed()));
            }

            if (stats.armorNegation() != null) {
                DecimalFormat format = ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT;
                parts.add("AP " + format.format(stats.armorNegation()));
            }

            if (stats.impact() != null) {
                DecimalFormat format = ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT;
                parts.add("IMP " + format.format(stats.impact()));
            }

            if (stats.maxStrikes() != null) {
                DecimalFormat format = ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT;
                parts.add("MS " + format.format(stats.maxStrikes()));
            }

            return parts.isEmpty() ? null : Component.literal(String.join("   ", parts));
        }
    }

    private static int modifierPriority(AttributeModifier modifier, ResourceLocation baseModifierId) {
        if (modifier.is(baseModifierId)) {
            return 3;
        } else {
            return modifier.operation() == AttributeModifier.Operation.ADD_VALUE ? 2 : 1;
        }
    }


    private static double displayedAttributeValue(boolean isAttackAttribute, AttributeModifier modifier) {
        double value = modifier.amount();
        if (isAttackAttribute) {
            if (modifier.is(Item.BASE_ATTACK_DAMAGE_ID)) {
                value += Attributes.ATTACK_DAMAGE.value().getDefaultValue();
            } else if (modifier.is(Item.BASE_ATTACK_SPEED_ID)) {
                value += Attributes.ATTACK_SPEED.value().getDefaultValue();
            }
        }

        return modifier.operation() != AttributeModifier.Operation.ADD_MULTIPLIED_BASE && modifier.operation() != AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL ? value : value * (double)100.0F;
    }

    private static final class HeaderStat {
        private double value;
        private int priority;

        private HeaderStat(double value, int priority) {
            this.value = value;
            this.priority = priority;
        }

        static HeaderStat missing() {
            return new HeaderStat(0.0F, -1);
        }

        boolean present() {
            return this.priority >= 0;
        }

        double value() {
            return this.value;
        }

        int priority() {
            return this.priority;
        }

        void replace(double newValue, int newPriority) {
            this.value = newValue;
            this.priority = newPriority;
        }
    }
}
